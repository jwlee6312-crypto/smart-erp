<!--
	=============================================================
	프로그램명	: 입금전표 발행 (HSIO320U)
	작성일자	: 2025.02.24
	설명        : 입금 내역을 조회하여 회계 전표를 일괄 발행 처리
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-journal-check me-2 text-primary" style="font-size: 18px;"></i>
        영업정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        입금관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">입금전표 (HSIO320U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-save" @click="handleGenerateSlip" :disabled="isClosed">전표발행</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 조회 필터 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
                <col style="width: 10%" /><col style="width: 30%" />
                <col style="width: 10%" /><col style="width: 25%" />
                <col style="width: 10%" /><col style="width: 15%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light">입금부서</th>
                <td>
                  <div class="input-group input-group-sm" style="width: 250px;">
                    <input v-model="form_01.deptcd" class="form-control text-center bg-light" style="width: 60px;" readonly />
                    <input v-model="form_01.deptnm" class="form-control" placeholder="부서 선택" @keyup.enter="handleOpenHelp('DEPT_S')" />
                    <button class="btn btn-outline-secondary" @click="handleOpenHelp('DEPT_S')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="text-center bg-light">입금일자</th>
                <td class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                  <DateForm v-model:fromdt="form_01.fromdt" v-model:todt="form_01.todt" />
                </td>
                <th class="text-center bg-light">영업사원</th>
                <td>
                   <select v-model="form_01.salsemp" class="form-select form-select-sm">
                     <option value="">전체</option>
                     <option v-for="opt in salsempOptions" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                   </select>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [중앙] 발행 설정 정보 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden border-primary-subtle">
        <div class="card-header py-1 px-3 bg-primary-subtle border-bottom d-flex align-items-center">
            <span class="small fw-bold text-primary"><i class="bi bi-info-circle-fill me-1"></i> 전표 발행 설정</span>
        </div>
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense w-100">
            <colgroup>
              <col style="width: 110px;" /><col />
              <col style="width: 110px;" /><col />
              <col style="width: 110px;" /><col />
            </colgroup>
            <tbody>
              <tr>
                <th class="required bg-light text-center">전표일자</th>
                <td><input v-model="form_02.slipymd" type="date" class="form-control form-control-sm" style="width: 150px;" /></td>
                <th class="required bg-light text-center">발행부서</th>
                <td>
                  <div class="input-group input-group-sm" style="width: 250px;">
                    <input v-model="form_02.deptcd" class="form-control text-center bg-light" style="width: 60px;" readonly />
                    <input v-model="form_02.deptnm" class="form-control" readonly />
                    <button class="btn btn-outline-secondary" @click="handleOpenHelp('DEPT_G')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="bg-light text-center">선택합계</th>
                <td>
                    <input :value="formatNumber(selectedSum)" class="form-control form-control-sm bg-light text-end fw-bold text-primary" readonly />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 d-flex flex-column overflow-hidden">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>입금 내역 목록</span>
          <div class="small text-muted">선택: <span class="fw-bold text-primary">{{ selectedCount }}</span> 건</div>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="gridRef" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, computed, nextTick, onUnmounted } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import DateForm from '@/components/DateForm.vue'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { getDate } from '@/composables/useDate'

const authStore = useAuthStore()
const { firstDay, today } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

const modalVisible = ref(false)
const modalProps = reactive<any>({
  title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table'
})

const form_01 = reactive({ fromdt: firstDay, todt: today, deptcd: authStore.deptcd, deptnm: authStore.deptnm, salsemp: '' })
const form_02 = reactive({ slipymd: today, deptcd: authStore.deptcd, deptnm: authStore.deptnm })

const closingInfo = reactive({ clsymd: '', sclsym: '' })
const salsempOptions = ref<any[]>([])
const selectedSum = ref(0)
const selectedCount = ref(0)

const gridRef = ref<HTMLDivElement | null>(null)
let grid: Tabulator | null = null

const isClosed = computed(() => {
  if (!closingInfo.clsymd || !form_02.slipymd) return false
  const slipDate = form_02.slipymd.replace(/-/g, '')
  return slipDate <= closingInfo.clsymd || slipDate.substring(0, 6) <= closingInfo.sclsym
})

const initGrid = () => {
  grid = new Tabulator(gridRef.value!, {
    layout: "fitColumns", height: "100%", placeholder: "조회된 내역이 없습니다.",
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "선택", width: 40, hozAlign: "center", formatter: "rowSelection", titleFormatter: "rowSelection", cellClick: (e, cell) => cell.getRow().toggleSelect() },
      { title: "입금번호", field: "iono_full", hozAlign: "center", width: 130,
        formatter: (c) => `${c.getData().imym || ''}-${c.getData().imno || ''}`, cssClass: "fw-bold"
      },
      { title: "입금일자", field: "imymd", hozAlign: "center", width: 100, formatter: (c) => formatDate(c.getValue()) },
      { title: "입금부서", field: "deptnm", hozAlign: "left", width: 150 },
      { title: "거래처", field: "custnm", hozAlign: "left", minWidth: 200 },
      { title: "건수", field: "rowcnt", hozAlign: "right", width: 80, formatter: "money", formatterParams: { precision: 0 } },
      { title: "입금액", field: "imamt", hozAlign: "right", width: 130, formatter: "money", formatterParams: { precision: 0 }, cssClass: "text-primary fw-bold" }
    ],
  });

  grid.on("rowSelectionChanged", () => {
    const selectedData = grid?.getSelectedData() || [];
    selectedCount.value = selectedData.length;
    selectedSum.value = selectedData.reduce((acc, cur) => acc + (Number(cur.imamt) || 0), 0);
  });
}

const handleOpenHelp = (type: string) => {
  if (type.startsWith('DEPT')) {
    Object.assign(modalProps, {
      title: '부서 선택', path: '/ha00/HA00_00P_STR', defaultField: 'deptnm',
      data: { gubun: 'D0', cmpycd: authStore.cmpycd, gbncd: '', code: '', remark: '' },
      columns: [{ title: '코드', field: 'deptcd', width: 80, hozAlign: 'center' }, { title: '부서명', field: 'deptnm', width: 200 }],
      onConfirm: (d: any) => {
        if (type === 'DEPT_S') { form_01.deptcd = d.deptcd; form_01.deptnm = d.deptnm; }
        else { form_02.deptcd = d.deptcd; form_02.deptnm = d.deptnm; }
      }
    });
    modalVisible.value = true;
  }
}

async function search() {
  try {
    const res = await api.post('/hsio/HSIO_320U_STR', {
        actkind: 'S0',
        cmpycd: authStore.cmpycd,
        deptcd: form_01.deptcd,
        fromdt: form_01.fromdt.replace(/-/g, ''),
        todt: form_01.todt.replace(/-/g, ''),
        salsemp: form_01.salsemp
    });
    grid?.setData(res.data || []);
    vAlert('조회되었습니다.');
  } catch (e) { vAlertError('조회 실패'); }
}

async function handleGenerateSlip() {
    const selectedRows = grid?.getSelectedData();
    if (!selectedRows || selectedRows.length === 0) return vAlertError('발행할 대상을 선택하세요.');

    if (isClosed.value) return vAlertError('해당 일자는 이미 마감되어 전표를 발행할 수 없습니다.');
    if (!confirm('선택한 내역으로 전표를 발행하시겠습니까?')) return;

    try {
        const payload = {
            mst: {
                actkind: 'A',
                cmpycd: authStore.cmpycd,
                slipymd: form_02.slipymd.replace(/-/g, ''),
                deptcd: form_02.deptcd,
                updemp: authStore.userid,
                empnm: authStore.usernm,
                fromdt: form_01.fromdt.replace(/-/g, ''),
                todt: form_01.todt.replace(/-/g, ''),
                salsemp: form_01.salsemp,
                imsum: selectedSum.value
            },
            dtl: selectedRows.map(r => ({
                imym: r.imym,
                imno: r.imno,
                imymd: r.imymd,
                custcd: r.custcd,
                imamt: r.imamt
            }))
        };
        // 🚀 실제 백엔드 저장 API 호출 (ASP의 HSIO_320U_IF_AUD 로직 통합)
        await api.post('/hsio/HSIO_320U_SAVE', payload);
        vAlert('전표가 발행되었습니다.');
        search();
    } catch (e: any) {
        vAlertError(e.response?.data?.message || '전표 발행 중 오류가 발생했습니다.');
    }
}

function initialize() {
  resetForm(form_01);
  Object.assign(form_01, { fromdt: firstDay, todt: today, deptcd: authStore.deptcd, deptnm: authStore.deptnm, salsemp: '' });
  Object.assign(form_02, { slipymd: today, deptcd: authStore.deptcd, deptnm: authStore.deptnm });
  grid?.clearData(); selectedSum.value = 0; selectedCount.value = 0;
}

const formatNumber = (n: any) => Number(n || 0).toLocaleString();
const formatDate = (d: any) => (d && d.length === 8) ? `${d.substring(0,4)}-${d.substring(4,6)}-${d.substring(6,8)}` : d;

onUnmounted(() => {
  if (grid) grid.destroy();
});

onMounted(() => {
  // 영업사원 코드 로드
  api.post('/ha00/HA00_00P_STR', { gubun: 'SD', cmpycd: authStore.cmpycd, gbncd: '1' }).then(r => {
    salsempOptions.value = r.data || [];
  });
  // 마감 정보 로드
  api.get('/hp00/hp00_000s_str', { params: { gubun: 'CL', cmpycd: authStore.cmpycd } }).then(r => {
    if (r.data?.length) {
        closingInfo.clsymd = r.data[0].clsymd || ''; // 회계마감일
        closingInfo.sclsym = r.data[0].sclsym || ''; // 영업마감월
    }
  });
  nextTick(initGrid);
});
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; font-size: 12px; border: 1px solid #dee2e6; }
.erp-table-dense th, .erp-table-dense td { height: 32px !important; padding: 0 8px !important; font-size: 12px; vertical-align: middle; border: 1px solid #dee2e6; }
.erp-table-dense .form-control, .erp-table-dense .form-select, .erp-table-dense .btn { height: 26px !important; font-size: 12px !important; border-radius: 2px; }
.border-primary-subtle { border-color: #cfe2ff !important; }
</style>
