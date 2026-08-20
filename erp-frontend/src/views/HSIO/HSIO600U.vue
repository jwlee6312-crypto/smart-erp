<!--
	=============================================================
	프로그램명	: 자재불출 출고처리 (HSIO600U)
	작성일자	: 2025.02.24
	설명        : 생산 자재불출요청 내역을 기반으로 영업 이동전표 자동 생성 (물류 전용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" v-bind="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-box-arrow-right me-2 text-primary" style="font-size: 18px;"></i>
        영업관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        출고관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">자재불출 출고처리 (HSIO600U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">불출요청조회</button>
        <button class="btn-erp btn-save" @click="handleConfirmIssue">출고등록</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 조회 필터 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
                <col style="width: 100px" /><col style="width: 250px" />
                <col style="width: 100px" /><col />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light">출고창고</th>
                <td>
                  <select v-model="searchForm.whcd" class="form-select form-select-sm" style="width: 200px;">
                    <option v-for="wh in whData" :key="wh.whcd" :value="wh.whcd">{{ wh.whnm }}</option>
                  </select>
                </td>
                <th class="text-center bg-light">요청기간</th>
                <td class="d-flex align-items-center gap-3">
                  <div class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                    <DateForm v-model:fromdt="searchForm.frymd" v-model:todt="searchForm.toymd" />
                  </div>
                  <div class="form-check form-check-sm mb-0 ms-3">
                    <input v-model="searchForm.isPending" class="form-check-input" type="checkbox" id="chkPending">
                    <label class="form-check-label small fw-bold text-primary" for="chkPending">미결건만 조회</label>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden border-top border-3 border-primary">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex justify-content-between align-items-center">
          <div class="d-flex align-items-center gap-2">
            <span class="fw-bold small text-dark"><i class="bi bi-card-list me-1 text-primary"></i>출고 대상 자재 목록</span>
            <button class="btn btn-xs btn-outline-success py-0 px-2" @click="fillIssueQty" style="font-size: 11px;">잔량 자동채우기</button>
          </div>
          <span class="badge bg-secondary" style="font-size: 10px;">{{ gridCount }}건</span>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white">
          <div ref="tableRef" class="tabulator-instance h-100"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import { useCommonHelp } from '@/composables/useCommonHelp'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { getDate } from '@/composables/useDate'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import DateForm from '@/components/DateForm.vue'

const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const authStore = useAuthStore()
const { modalVisible, modalProps, openHelp } = useCommonHelp()
const { firstDay, today } = getDate()

const searchForm = reactive({ frymd: firstDay, toymd: today, whcd: '100', isPending: true })
const whData = ref<any[]>([])
const gridCount = ref(0)
const tableRef = ref<HTMLElement | null>(null)
let tableInstance: Tabulator | null = null

const initGrid = () => {
  if (tableInstance) return;
  tableInstance = new Tabulator(tableRef.value!, {
    layout: 'fitColumns', height: '100%', selectable: true,
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "선택", width: 40, hozAlign: "center", formatter: "rowSelection", titleFormatter: "rowSelection" },
      { title: '요청번호', field: 'outymno', width: 120, hozAlign: 'center', mutator: (v,d) => d.outym && d.outno ? `${d.outym}-${d.outno}` : '' },
      { title: '자재명', field: 'mitemnm', minWidth: 200, widthGrow: 1, cssClass: 'text-dark fw-bold' },
      { title: '규격', field: 'mitsize', width: 130 },
      { title: '현재고', field: 'stkqty', width: 90, hozAlign: 'right', formatter: 'money', cssClass: 'text-danger fw-bolder' },
      { title: '요청수량', field: 'reqqty', width: 90, hozAlign: 'right', formatter: 'money' },
      { title: '기출고', field: 'acc_outqty', width: 80, hozAlign: 'right', formatter: 'money' },
      { title: '미출고잔량', field: 'remqty', width: 100, hozAlign: 'right', formatter: 'money', mutator: (v,d) => (d.reqqty || 0) - (d.acc_outqty || 0), cssClass: 'bg-light' },
      { title: '금일출고량', field: 'outqty', width: 110, hozAlign: 'right', editor: 'number', cssClass: 'bg-light-yellow fw-bold text-primary' },
      { title: '출고번호', field: 'ioymno', width: 120, hozAlign: 'center', mutator: (v,d) => d.ioym && d.iono ? `${d.ioym}-${d.iono}` : '' },
      { title: '입고번호', field: 'inymno', width: 120, hozAlign: 'center', mutator: (v,d) => d.inym && d.inno ? `${d.inym}-${d.inno}` : '' },
      { title: '비고', field: 'bigo', widthGrow: 1, editor: 'input' }
    ]
  });
}

const search = async () => {
  try {
    const params = {
      actkind: 'S',
      cmpycd: authStore.cmpycd,
      fromdt: searchForm.frymd.replace(/-/g, ''),
      todt: searchForm.toymd.replace(/-/g, ''),
      whcd: searchForm.whcd,
      outym: '', outno: '', mitemcd: '', ioym: '', iono: '', outqty: 0, updemp: authStore.userid
    }
    const { data } = await api.post('/hsio/HSIO_600U_STR', params)

    const gridData = (data || []).map((i: any) => ({
        ...i,
        acc_outqty: i.outqty || 0,
        outqty: (i.reqqty || 0) - (i.outqty || 0)
    }));

    tableInstance?.setData(gridData);
    gridCount.value = gridData.length;
    vAlert('조회되었습니다.');
  } catch (e) { vAlertError('조회 실패') }
}

const fillIssueQty = () => {
  const selectedRows = tableInstance?.getSelectedRows();
  if (!selectedRows?.length) return vAlertError('행을 먼저 선택하세요.');
  selectedRows.forEach(row => {
    const d = row.getData();
    row.update({ outqty: d.remqty });
  });
}

const handleConfirmIssue = async () => {
  const selectedData = tableInstance?.getSelectedData().filter(d => (d.outqty || 0) > 0);
  if (!selectedData?.length) return vAlertError('출고 수량이 입력된 행이 없습니다.');

  // 🚀 유효성 검사 추가
  for (const d of selectedData) {
    // 1. 이미 완료된 건 체크 (출고번호 존재 & 잔량 없음)
    if (d.ioym && d.iono && Number(d.reqqty || 0) <= Number(d.acc_outqty || 0)) {
        return vAlertError(`요청번호 [${d.outym}-${d.outno}]는 이미 완료된 건입니다.`);
    }

    // 2. 재고수량 부족 체크 (현재고 < 출고수량)
    if (Number(d.stkqty || 0) < Number(d.outqty || 0)) {
        return vAlertError(`자재 [${d.mitemnm}]의 재고가 부족합니다.\n(현재고: ${d.stkqty}, 입력수량: ${d.outqty})`);
    }
  }

  if (!confirm(`${selectedData.length}건의 자재를 현장으로 출고 확정하시겠습니까?`)) return;

  try {
    const payload = {
      cmpycd: authStore.cmpycd,
      updemp: authStore.userid,
      ioymd: today.replace(/-/g, ''),
      whcd: searchForm.whcd, // 출고창고
      items: selectedData.map(d => ({
        ...d,
        ordym: d.outym,
        ordno: d.outno
      }))
    }

    // 🚀 영업 수불 생성 + 생산 연동 통합 API 호출
    await api.post('/hsio/HSIO_600U_SAVE', payload);
    vAlert('출고 처리가 완료되었습니다.');
    search(); // 재조회를 통해 조인된 출고번호 확인
  } catch (e) { vAlertError('출고 처리 실패') }
}

const initialize = () => {
  Object.assign(searchForm, { frymd: firstDay, toymd: today, whcd: '100', isPending: true });
  tableInstance?.clearData(); gridCount.value = 0;
}

onMounted(async () => {
    nextTick(initGrid);
    try {
        const { data } = await api.post('/hs00/HS00_000S_STR', { gubun: 'W0', cmpycd: authStore.cmpycd });
        whData.value = data || [];
        if (whData.value.length > 0 && !searchForm.whcd) searchForm.whcd = whData.value[0].whcd;
    } catch (e) { console.error('창고 목록 로드 실패') }
});
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; font-size: 12px; }
.bg-light-yellow { background-color: #fffdec !important; }
</style>
