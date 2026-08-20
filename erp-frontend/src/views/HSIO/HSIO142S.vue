<!--
	=============================================================
	프로그램명	: 외부매입전표출력 (HSIO142S)
	작성일자	: 2025.02.24
	설명        : HSOD100U 디자인 패턴 이식 및 그리드 스크롤 최적화
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-printer-fill me-2 text-primary" style="font-size: 18px;"></i>
        구매정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        매입정산 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        <span class="text-primary fw-bolder">외부매입전표출력 (HSIO142S)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-2">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchList">조회</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- 🔍 [상단] 조회 필터 영역 (HSOD100U 디자인 패턴 적용) -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
              <col style="width: 10%" /><col style="width: 40%" />
              <col style="width: 10%" /><col style="width: 40%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light">입고부서</th>
                <td>
                  <div class="input-group input-group-sm w-75">
                    <input v-model="searchForm.deptcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
                    <input v-model="searchForm.deptnm" type="text" class="form-control" placeholder="부서 선택" @keyup.enter="fetchList" />
                    <button class="btn btn-outline-secondary px-2" @click="openHelp('DEPT')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="text-center bg-light">발행일자</th>
                <td class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                  <input v-model="searchForm.fromdt" type="date" class="form-control form-control-sm" style="width: 140px;" />
                  <span class="text-muted mx-1">~</span>
                  <input v-model="searchForm.todt" type="date" class="form-control form-control-sm" style="width: 140px;" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 📊 3. 메인 그리드 영역 (스크롤 최적화) -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center">
          <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i> 전표 발행 내역</span>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="mainGridRef" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>
    </div>
    <Modal v-model:visible="modalVisible" :modalProps="modalProps" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick, onUnmounted } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { useCommonHelp } from '@/composables/useCommonHelp'
import { getDate } from '@/composables/useDate'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import type { ModalProps } from '@/types/modal'

const authStore = useAuthStore()
const { firstDay, today } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps, openHelp: openCommonHelp } = useCommonHelp()

const searchForm = reactive<any>({
  deptcd: authStore.deptcd,
  deptnm: authStore.deptnm,
  fromdt: firstDay,
  todt: today
})

const mainGridRef = ref<HTMLDivElement | null>(null);
let grid: Tabulator | null = null;

async function fetchList() {
  try {
    const res = await api.post('/hsio/HSIO_141U_STR', {
      actkind: 'S0',
      cmpycd: authStore.cmpycd,
      iogbn: '100',
      fromdt: searchForm.fromdt.replace(/-/g, ''),
      todt: searchForm.todt.replace(/-/g, ''),
      deptcd: searchForm.deptcd
    });
    grid?.setData(res.data || []);
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

function initialize() {
  resetForm(searchForm);
  searchForm.deptcd = authStore.deptcd;
  searchForm.deptnm = authStore.deptnm;
  searchForm.fromdt = firstDay
  searchForm.todt = today
  grid?.clearData();
}

function slipPrint(type: 'SLIP' | 'PUM', data: any) {
  const slipGu = '010';
  const url = type === 'SLIP'
    ? `../HASL/HASL_SLIP_PRINT_OUT.asp?slipgu=${slipGu}&slipymd=${data.slipymd}&slipno=${data.slipno}&deptcd=${data.deptcd}`
    : `../HSIR/HSIR_SLIP_PRINT.asp?slipgu=${slipGu}&slipymd=${data.slipymd}&slipno=${data.slipno}&deptcd=${data.deptcd}`;
  window.open(url, '전표인쇄', 'left=10,top=10,width=800,height=700,scrollbars=yes');
}

function openHelp(type: string) {
  if (type === 'DEPT') {
    openCommonHelp('DEPT', (d) => { searchForm.deptcd = d.deptcd; searchForm.deptnm = d.deptnm });
  }
}

onUnmounted(() => {
  if (grid) grid.destroy();
});

onMounted(() => {
  if (mainGridRef.value) {
    grid = new Tabulator(mainGridRef.value, {
      layout: 'fitColumns', height: '100%',
      columnDefaults: { headerSort: false, headerHozAlign: "center", vertAlign: "middle" },
      columns: [
        { title: '전표번호', field: 'slipno_full', width: 140, hozAlign: 'center', cssClass: 'fw-bold text-primary cursor-pointer',
          mutatorData: (v, d) => d.slipymd && d.slipno ? `${d.slipymd}-${d.slipno}` : '',
          cellClick: (e, cell) => slipPrint('SLIP', cell.getRow().getData())
        },
        { title: '발행부서', field: 'deptnm', width: 130, hozAlign: 'left' },
        { title: '거래처', field: 'custnm', minWidth: 200, widthGrow: 1, hozAlign: 'left', cssClass: 'fw-bold' },
        { title: '유형', field: 'vattypenm', width: 100 },
        { title: '공급가', field: 'spyamt', width: 110, hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 } },
        { title: '부가세', field: 'vatamt', width: 100, hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 } },
        { title: '합계', field: 'jsansum', width: 120, hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 }, cssClass: 'bg-light fw-bold',
          mutatorData: (v, d) => Number(d.spyamt || 0) + Number(d.vatamt || 0)
        },
        { title: '회계전표', width: 80, hozAlign: 'center',
          formatter: () => `<button class="btn btn-xs btn-outline-dark">인쇄</button>`,
          cellClick: (e, cell) => slipPrint('SLIP', cell.getRow().getData())
        },
        { title: '지출결의', width: 80, hozAlign: 'center',
          formatter: () => `<button class="btn btn-xs btn-outline-primary">인쇄</button>`,
          cellClick: (e, cell) => slipPrint('PUM', cell.getRow().getData())
        }
      ]
    })
  }
  nextTick(() => fetchList())
})
</script>

<style scoped>
.main-content-wrapper { padding-bottom: 0vh !important; }
.erp-table-dense th, .erp-table-dense td {
  height: 32px !important; padding: 0 8px !important; font-size: 12px; vertical-align: middle; border: 1px solid #dee2e6;
}
.erp-table-dense .form-control, .erp-table-dense .form-select, .erp-table-dense .btn {
  height: 26px !important; font-size: 12px !important; border-radius: 2px;
}
.erp-table-dense th { font-weight: 600; color: #495057; }
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
