<!--
	=============================================================
	프로그램명	: 외부매입전표처리 (HSIO131U)
	작성일자	: 2025.02.24
	설명        : 외부 정산 내역을 기반으로 회계 전표 발행 (ASP 로직 - A0/U0 순차 저장 이식)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom bg-white py-2 px-3 sticky-top shadow-sm">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-file-earmark-diff-fill me-2 text-primary" style="font-size: 18px;"></i>
        구매정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        매입정산 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        <span class="text-primary fw-bolder">외부매입전표처리 (HSIO131U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-2">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchList">조회</button>
        <button class="btn-erp btn-save" @click="saveSlip">전표발행</button>
      </div>
    </div>

    <!-- 🔍 2. 상단 조회 필터 -->
    <div class="p-2 pb-0 flex-shrink-0 bg-light">
      <div class="card border shadow-sm overflow-hidden">
        <div class="card-body p-2 bg-white">
          <div class="d-flex align-items-center gap-4">
            <div class="d-flex align-items-center gap-2">
              <span class="fw-bold small text-dark" style="min-width: 60px;">입고부서</span>
              <div class="input-group input-group-sm" style="width: 250px;">
                <input v-model="searchForm.deptcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
                <input v-model="searchForm.deptnm" type="text" class="form-control" placeholder="부서 선택" />
                <button class="btn btn-outline-secondary px-2" @click="handleOpenHelp('S_DEPT')"><i class="bi bi-search"></i></button>
              </div>
            </div>
            <div class="d-flex align-items-center gap-2">
              <span class="fw-bold small text-dark" style="min-width: 60px;">정산일자</span>
              <div class="d-flex align-items-center gap-1">
                <DateForm v-model:fromdt="searchForm.fromdt" v-model:todt="searchForm.todt" />
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="flex-grow-1 d-flex flex-column overflow-hidden p-2 gap-2 bg-light">
      <!-- 🅰️ 전표 발행 설정 폼 (ASP 원본 UI 준수: 발행일, 부서만 표시) -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense w-100">
            <colgroup>
              <col style="width: 120px;" /><col />
              <col style="width: 120px;" /><col />
            </colgroup>
            <tbody>
              <tr>
                <th class="required bg-light text-center">전표일자</th>
                <td><input v-model="formData.slipymd" type="date" class="form-control" /></td>
                <th class="required bg-light text-center">발행부서</th>
                <td>
                  <div class="input-group input-group-sm">
                    <input v-model="formData.deptcd" type="text" class="form-control text-center bg-light fw-bold" style="max-width: 60px;" readonly />
                    <input v-model="formData.deptnm" type="text" class="form-control" readonly />
                    <button class="btn btn-outline-secondary px-2" @click="handleOpenHelp('DEPT')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 🅱️ 정산 내역 그리드 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white grid-container-right">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
          <span class="fw-bold small text-dark d-flex align-items-center">
            <i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i> 외부 정산 대상 내역
          </span>
          <div class="d-flex align-items-center gap-2">
            <span class="text-danger fw-bold small">선택 합계: {{ formatNumber(totalSelectedAmt) }}</span>
            <button class="btn btn-xs btn-outline-secondary px-2" style="height: 24px; font-size: 11px;" @click="toggleAllRows">전체선택</button>
          </div>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column" style="min-height: 0;">
          <div ref="mainGridRef" class="tabulator-full-height" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick, computed, onUnmounted } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { getDate } from '@/composables/useDate'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import DateForm from '@/components/DateForm.vue'
import type { ModalProps } from '@/types/modal'

const authStore = useAuthStore()
const { firstDay, today } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

const searchForm = reactive({
  deptcd: authStore.deptcd,
  deptnm: authStore.deptnm,
  fromdt: firstDay,
  todt: today
})

const formData = reactive({
  slipymd: today,
  deptcd: authStore.deptcd,
  deptnm: authStore.deptnm,
  cardyn: 'Y',
  cardno: '',
  cardnm: '',
  sclsym: '', // 마감년월
  pclsym: ''
})

const mainGridRef = ref<HTMLDivElement | null>(null);
let grid: Tabulator | null = null;
const totalSelectedAmt = ref(0)

/** 🚀 팝업 직관화 구현 (ASP 규칙 적용) */
const handleOpenHelp = (type: string) => {
  if (type === 'S_DEPT') {
    Object.assign(modalProps, {
      title: '입고부서 선택',
      path: '/ha00/HA00_00P_STR',
      data: { gubun: 'D0', cmpycd: authStore.cmpycd, code: '', codenm: searchForm.deptnm },
      columns: [
        { title: '부서코드', field: 'deptcd', width: 100, hozAlign: 'center' },
        { title: '부서명', field: 'deptnm', width: 200 }
      ],
      onConfirm: (d: any) => { searchForm.deptcd = d.deptcd; searchForm.deptnm = d.deptnm }
    })
    modalVisible.value = true
  } else if (type === 'DEPT') {
    Object.assign(modalProps, {
      title: '발행부서 선택',
      path: '/ha00/HA00_00P_STR',
      data: { gubun: 'D0', cmpycd: authStore.cmpycd, code: '', codenm: formData.deptnm },
      columns: [
        { title: '부서코드', field: 'deptcd', width: 100, hozAlign: 'center' },
        { title: '부서명', field: 'deptnm', width: 200 }
      ],
      onConfirm: (d: any) => { formData.deptcd = d.deptcd; formData.deptnm = d.deptnm }
    })
    modalVisible.value = true
  }
}

async function fetchClosingInfo() {
  try {
    const res = await api.post('/hs00/HS00_000S_STR', { gubun: 'CL', cmpycd: authStore.cmpycd })
    if (res.data?.length) {
      formData.sclsym = res.data[0].sclsym || '';
      formData.pclsym = res.data[0].pclsym || '';
    }
  } catch (e) {}
}

async function fetchList() {
  try {
    const res = await api.post('/hsio/HSIO_131U_STR', {
      actkind: 'S0',
      cmpycd: authStore.cmpycd,
      iogbn: '100',
      fromdt: searchForm.fromdt.replace(/-/g, ''),
      todt: searchForm.todt.replace(/-/g, ''),
      deptcd: searchForm.deptcd
    });
    console.log(res.data)
    grid?.setData(res.data || []);
    calcTotalSum();
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

/**
 * 🚀 전표 발행 로직 (통합 백엔드 서비스 방식 - HSIP140U 표준 모델 준수)
 */
async function saveSlip() {
  const selectedRows = grid?.getSelectedData().filter((r: any) => r.procyn === true) || []
  if (selectedRows.length === 0) return vAlertError('발행할 항목을 선택하세요.')

  const slipymd = formData.slipymd.replace(/-/g, '');
  const slipym = slipymd.substring(0, 6);

  if (formData.sclsym && slipym <= formData.sclsym) return vAlertError('영업정보가 마감이 되었습니다. 해당 전표일자로 작업할 수 없습니다.');

  if (!confirm(`${selectedRows.length}건의 외부 매입전표를 일괄 발행하시겠습니까?`)) return

  const payload = {
    mst: {
      pubymd: formData.slipymd,
      deptcd: formData.deptcd,
      cmpycd: authStore.cmpycd,
      usernm: authStore.usernm,
      cardyn: formData.cardyn,
      cardno: formData.cardno,
      fromdt: searchForm.fromdt,
      todt: searchForm.todt
    },
    items: selectedRows.map(item => ({
      jsanym: item.jsanym,
      jsanno: item.jsanno,
      jsanymd: item.jsanymd,
      spyamt: item.spyamt,
      vatamt: item.vatamt,
      custcd: item.custcd,
      deptcd: item.deptcd,
      taxunit: item.taxunit,
      vattype: item.vattype,
      bigo: item.bigo
    }))
  }

  try {
    const res = await api.post('/hsio/HSIO_131U_SAVE', payload)
    if (res.data) {
      vAlert('외부매입전표가 성공적으로 발행되었습니다.')
      fetchList()
      initialize()
    }
  } catch (e: any) {
    vAlertError('발행 실패: ' + (e.response?.data?.message || e.message))
  }
}

const toggleAllRows = () => {
  const rows = grid?.getRows(); if (!rows) return
  const allSelected = rows.every(r => r.getData().procyn === true)
  rows.forEach(r => r.update({ procyn: !allSelected }))
  calcTotalSum();
}

const calcTotalSum = () => {
    const data = grid?.getData() || [];
    totalSelectedAmt.value = data.filter((r: any) => r.procyn === true)
                                .reduce((acc, cur: any) => acc + (Number(cur.spyamt) + Number(cur.vatamt)), 0);
}

function initialize() {
  resetForm(formData);
  formData.slipymd = today;
  formData.deptcd = authStore.deptcd; formData.deptnm = authStore.deptnm;
  grid?.clearData(); totalSelectedAmt.value = 0;
}

const formatNumber = (val: any) => Number(val || 0).toLocaleString();

onUnmounted(() => {
  if (grid) grid.destroy();
});

onMounted(async () => {
  await fetchClosingInfo();
  if (mainGridRef.value) {
    grid = new Tabulator(mainGridRef.value, {
      layout: 'fitColumns', height: '100%',
      columnDefaults: { headerSort: false, headerHozAlign: "center", hozAlign: 'right', vertAlign: "middle", minWidth: 100 },
      columns: [
        { title: '선택', field: 'procyn', hozAlign: 'center', width: 60, formatter: 'tickCross', editor: true,
          cellClick: () => { nextTick(calcTotalSum); }
        },
        { title: '발행일', field: 'jsanymd', width: 110, hozAlign: 'center',
          formatter: (c) => { const v = c.getValue(); return v && v.length === 8 ? `${v.substring(0,4)}-${v.substring(4,6)}-${v.substring(6,8)}` : v }
        },
        { title: '부서', field: 'deptnm', width: 150, hozAlign: 'left' },
        { title: '거래처', field: 'custnm', minWidth: 200, widthGrow: 2, hozAlign: 'left', cssClass: 'text-dark' },
        { title: '사업장', field: 'unitnm', width: 120, hozAlign: 'left' },
        { title: '유형', field: 'vattypenm', width: 120, hozAlign: 'left' },
        { title: '공급가', field: 'spyamt', width: 120, formatter: 'money', formatterParams: { precision: 0 } },
        { title: '부가세', field: 'vatamt', width: 110, formatter: 'money', formatterParams: { precision: 0 } },
        { title: '합계', field: 'jsansum', width: 130, formatter: 'money', cssClass: 'bg-light fw-bold',
          mutatorData: (v, d) => Number(d.spyamt || 0) + Number(d.vatamt || 0) }
      ]
    })
  }
  fetchList();
})
</script>

<style scoped>
.tabulator-full-height { width: 100% !important; background-color: #fff; border-bottom: 3px solid #005a9f !important; }
.erp-table-dense th, .erp-table-dense td {
  height: 32px !important; padding: 0 8px !important; font-size: 12px; vertical-align: middle; border: 1px solid #dee2e6;
}
.erp-table-dense th { font-weight: 600; color: #495057; }
</style>
