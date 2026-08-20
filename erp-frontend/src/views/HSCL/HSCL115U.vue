<!--
	=============================================================
	프로그램명	: 외부 타계정전표발행 (HSCL115U)
	작성일자	: 2025.02.24
	설명        : 외부 타계정 출고 내역 선택 발행 및 관리 (HSOD100U 디자인 표준 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-journal-arrow-up me-2 text-primary" style="font-size: 18px;"></i>
        마감관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        전표관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">외부 타계정전표발행 (HSCL115U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-save" @click="handleAction('A')">전표발행</button>
        <button class="btn-erp btn-delete" @click="handleAction('D')">전표삭제</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 조회 조건 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
                <col style="width: 10%" /><col style="width: 30%" />
                <col style="width: 10%" /><col style="width: 20%" />
                <col style="width: 10%" /><col style="width: 20%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="required text-center bg-light">출고부서</th>
                <td>
                  <div class="input-group input-group-sm">
                    <input v-model="form.deptnm" class="form-control fw-bold text-primary" readonly />
                    <button class="btn btn-outline-secondary" @click="openHelp('DEPT')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="required text-center bg-light">출고연월</th>
                <td>
                  <input v-model="form.yymm" type="month" class="form-control form-control-sm" @change="search" />
                </td>
                <td colspan="2" class="text-end pe-3 bg-white">
                   <span v-if="form.sclsym" class="badge bg-primary px-2 me-2">마감: {{ formatYM(form.sclsym) }}</span>
                  <span class="small text-muted fw-bold">※ 전표 미결 내역을 선택하여 발행하십시오.</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 데이터 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>외부 타계정 출고 내역</span>
          <div class="d-flex gap-3 small fw-bold">
            <span class="text-dark">선택수량: <span class="text-primary">{{ formatNumber(selectedTotals.qty) }}</span></span>
            <span class="text-dark pe-2">선택금액: <span class="text-danger">{{ formatNumber(selectedTotals.amt) }}</span></span>
          </div>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="gridElement" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, nextTick } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { useCommonHelp } from '@/composables/useCommonHelp'
import { getDate } from '@/composables/useDate'

const authStore = useAuthStore()
const { today } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps } = useCommonHelp()

// 1. 상태 관리
const form = reactive<any>({
  deptcd: authStore.deptcd,
  deptnm: authStore.deptnm,
  yymm: today.substring(0, 7),
  clsymd: '',
  sclsym: ''
})

const selectedTotals = reactive({ qty: 0, amt: 0 })
const gridElement = ref<HTMLElement | null>(null)
const grid = ref<Tabulator | null>(null)

// 2. 그리드 초기화
const initGrid = () => {
  if (!gridElement.value) return
  grid.value = new Tabulator(gridElement.value, {
    layout: "fitColumns",
    height: "100%",
    placeholder: "조회된 내역이 없습니다.",
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "선택", width: 40, hozAlign: "center", formatter: "rowSelection", titleFormatter: "rowSelection", cellClick: (e, cell) => cell.getRow().toggleSelect() },
      { title: "출고번호", field: "io_full", width: 150, hozAlign: "center", cssClass: "fw-bold" },
      { title: "유형", field: "tatypenm", width: 150 },
      { title: "출고수량", field: "ioqty", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "출고금액", field: "ioamt", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, editor: "number" },
      { title: "거래처명", field: "custnm", minWidth: 150 },
      { title: "차변계정", field: "dacct_full", width: 150 },
      { title: "대변계정", field: "cacct_full", width: 150 },
      {
        title: "전송여부", field: "cofmyn", width: 150, hozAlign: "center",
        formatter: (c) => c.getValue() === 'Y' ? '<span class="badge bg-success">전송완료</span>' : '<span class="badge bg-secondary">전송미결</span>'
      },
      {
        title: "전표번호", field: "slip_full", width: 150, hozAlign: "center",
        formatter: (c) => {
          const d = c.getData();
          return d.slipno && d.slipno !== '000' ? `<span class="text-primary cursor-pointer text-decoration-underline fw-bold">${d.slipymd}-${d.slipno}</span>` : '';
        },
        cellClick: (e, cell) => {
          const d = cell.getData();
          if (d.slipno && d.slipno !== '000') handlePrint(d);
        }
      }
    ],
    selectableCheck: (row) => row.getData().cofmyn !== 'Y'
  })

  grid.value.on("selectionChanged", () => {
    const selectedRows = grid.value?.getSelectedData() || [];
    selectedTotals.qty = selectedRows.reduce((acc, cur) => acc + (Number(cur.ioqty) || 0), 0);
    selectedTotals.amt = selectedRows.reduce((acc, cur) => acc + (Number(cur.ioamt) || 0), 0);
  });
}

// 3. 기능 구현
const search = async () => {
  if (!form.deptcd) return vAlertError('출고부서를 선택해 주십시오.')

  try {
    const res = await api.post('/hscl/HSCL_115U_STR', {
      actkind: 'S',
      cmpycd: authStore.cmpycd,
      gbn: '200',
      yymm: form.yymm.replace(/-/g, '')
    })

    const data = (res.data || []).map((i: any) => {
      const item = Object.fromEntries(Object.entries(i).map(([k, v]) => [k.toLowerCase(), v]));
      return {
        ...item,
        io_full: `${item.ioym}-${item.iono}`,
        dacct_full: `${item.dacctcd}-${item.dacctnm}`,
        cacct_full: `${item.cacctcd}-${item.cacctnm}`,
        slip_full: item.slipno && item.slipno !== '000' ? `${item.slipymd}-${item.slipno}` : ''
      }
    });

    grid.value?.setData(data);
    vAlert('조회되었습니다.');
  } catch (e) { vAlertError('조회 중 오류 발생'); }
}

const handleAction = async (actKind: 'A' | 'D') => {
  const selectedRows = grid.value?.getSelectedRows();
  if (!selectedRows || selectedRows.length === 0) return vAlertError('처리할 내역을 선택하십시오.');

  const confirmMsg = actKind === 'A' ? '선택한 내역을 전표 발행하시겠습니까?' : '발행된 전표를 삭제하시겠습니까?';
  if (!confirm(confirmMsg)) return;

  try {
    const list = selectedRows.map(row => ({
      ...row.getData(),
      cmpycd: authStore.cmpycd,
      updemp: authStore.userid,
      slipkind: '040'
    }));

    await api.post('/hscl/HSCL_115U_SAVE', { actkind: actKind, list: list });

    vAlert(`정상적으로 ${actKind === 'A' ? '발행' : '삭제'}되었습니다.`);
    search();
  } catch (e: any) {
    vAlertError(e.response?.data?.message || '처리 중 오류 발생');
  }
}

const handlePrint = (data: any) => {
  const url = `/report/HASL_SLIP_PRINT_OUT?slipgu=010&slipymd=${data.slipymd}&slipno=${data.slipno}&deptcd=${data.deptcd}`;
  window.open(url, 'Print', 'width=800,height=700,scrollbars=yes');
}

const initialize = () => {
  resetForm(form);
  Object.assign(form, { deptcd: authStore.deptcd, deptnm: authStore.deptnm, yymm: today.substring(0, 7) });
  grid.value?.clearData();
  selectedTotals.qty = 0; selectedTotals.amt = 0;
  fetchInitialSettings();
}

const openHelp = (type: string) => {
  if (type === 'DEPT') {
    Object.assign(modalProps, {
      title: '부서 선택',
      path: '/ha00/HA00_00P_STR',
      data: { gubun: 'D0', cmpycd: authStore.cmpycd },
      columns: [
        { title: '코드', field: 'deptcd', width: 100 },
        { title: '부서명', field: 'deptnm', width: 200 }
      ],
      onConfirm: (d: any) => { form.deptcd = d.deptcd; form.deptnm = d.deptnm; search(); }
    });
    modalVisible.value = true;
  }
}

const fetchInitialSettings = async () => {
  try {
    const resCL = await api.post('/hs00/HS00_000S_STR', { gubun: 'CL', cmpycd: authStore.cmpycd });
    if (resCL.data?.length) {
      form.clsymd = resCL.data[0].clsymd;
      form.sclsym = resCL.data[0].sclsym;
    }
  } catch (e) { console.error('설정 로드 실패'); }
}

const formatNumber = (v: any) => new Intl.NumberFormat().format(v || 0);
const formatYM = (v: string) => v ? `${v.substring(0, 4)}-${v.substring(4, 6)}` : '';

onMounted(() => {
  nextTick(() => {
    initGrid();
    fetchInitialSettings();
  });
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
