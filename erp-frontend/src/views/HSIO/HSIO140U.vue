<!--
	=============================================================
	프로그램명	: 매입전표취소 (Purchase Slip Cancellation)
	작성일자	: 25.02.24
	작성자	    : AI Assistant
	설명        : ASP(HSIO_140U) 소스 기반 재작성 (단일 그리드 표준 + 팝업 표준화)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-file-earmark-x-fill me-2 text-danger" style="font-size: 18px;"></i>
        구매정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        매입정산 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        <span class="text-primary fw-bolder">매입전표취소 (HSIO140U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchList">조회</button>
        <button class="btn-erp btn-danger" @click="handleCancel">전표취소</button>
      </div>
    </div>

    <!-- 🔍 2. 상단 조회 필터 -->
    <div class="search-bar bg-light border-bottom p-2 px-3 d-flex align-items-center shadow-sm flex-shrink-0">
      <div class="d-flex align-items-center gap-4 flex-grow-1">
        <div class="d-flex align-items-center gap-2">
          <span class="fw-bold small text-secondary">▶ 입고부서:</span>
          <div class="input-group input-group-sm" style="width: 250px;">
            <input v-model="searchForm.deptcd" type="text" class="form-control text-center bg-white" style="max-width: 60px;" readonly />
            <input v-model="searchForm.deptnm" type="text" class="form-control border-start-0" placeholder="부서 선택" @keyup.enter="handleOpenHelp('DEPT')" />
            <button class="btn btn-outline-secondary px-2" @click="handleOpenHelp('DEPT')"><i class="bi bi-search"></i></button>
          </div>
        </div>
        <div class="d-flex align-items-center gap-2">
          <span class="fw-bold small text-secondary">▶ 발행일자:</span>
          <div class="d-flex align-items-center gap-1">
            <input v-model="searchForm.fromdt" type="date" class="form-control form-control-sm" style="width: 140px;" />
            <span class="text-muted mx-1">~</span>
            <input v-model="searchForm.todt" type="date" class="form-control form-control-sm" style="width: 140px;" />
          </div>
        </div>
        <div class="ms-auto d-flex align-items-center gap-2 bg-white border rounded px-2 py-1 shadow-sm">
          <div class="form-check form-switch mb-0">
            <input v-model="autoSlip" class="form-check-input" type="checkbox" id="autoSlipCheck" true-value="Y" false-value="N">
            <label class="form-check-label small fw-bold text-primary" for="autoSlipCheck">자동전표취소</label>
          </div>
        </div>
      </div>
    </div>

    <!-- 📊 3. 메인 그리드 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2">
      <div class="card border shadow-sm h-100 d-flex flex-column bg-white">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
          <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i> 취소 대상 매입 전표 내역</span>
          <button class="btn btn-outline-primary btn-sm px-2 py-0" style="font-size: 11px;" @click="toggleAllRows">전체선택/해제</button>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="mainGridRef" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>
    </div>
  </div>

  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, nextTick, onUnmounted } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { useCommonHelp } from '@/composables/useCommonHelp'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps, openHelp } = useCommonHelp()

const now = new Date()
const searchForm = reactive<any>({
  deptcd: authStore.deptcd,
  deptnm: authStore.deptnm,
  fromdt: new Date(now.getFullYear(), now.getMonth(), 1).toISOString().substring(0, 10),
  todt: now.toISOString().substring(0, 10)
})

const autoSlip = ref('N')
// 마감일 정보
const clsInfo = reactive({ clsymd: '', sclsym: '' })
const mainGridRef = ref<HTMLDivElement | null>(null);
let grid: Tabulator | null = null;

async function fetchClosingInfo() {
  try {
    const res = await api.post('/hs00/HS00_000S_STR', { gubun: 'CL', cmpycd: authStore.cmpycd })
    if (res.data?.[0]) {
      clsInfo.clsymd = res.data[0].clsymd || ''
      clsInfo.sclsym = res.data[0].sclsym || ''
    }
  } catch (e) {}
}

async function fetchList() {
  try {
    const res = await api.post('/hsio/HSIO_140U_STR', {
      actkind: 'S0',
      cmpycd: authStore.cmpycd,
      iogbn: '100',
      fromdt: searchForm.fromdt.replace(/-/g, ''),
      todt: searchForm.todt.replace(/-/g, ''),
      deptcd: searchForm.deptcd
    });
    grid?.setData(res.data.map((i: any) => ({ ...i, procyn: null })));
  } catch (e) { vAlertError('조회 실패') }
}

async function handleCancel() {
  const items = grid?.getSelectedData() || []
  if (items.length === 0) return vAlertError('취소할 전표를 선택하세요.')

  for (const item of items) {
    if (item.cfmyn === 'Y' && autoSlip.value === 'N') return vAlertError(`확정처리된 전표입니다. 자동전표취소를 선택하거나 확정취소 후 작업하세요. [전표:${item.slipymd}-${item.slipno}]`);
    const slipYm = String(item.jsanymd || '').substring(0, 6);
    if (clsInfo.sclsym && slipYm <= clsInfo.sclsym) return vAlertError(`영업정보가 마감되었습니다. [전표:${item.slipymd}-${item.slipno}]`);
    if (item.jsanymd <= clsInfo.clsymd) return vAlertError(`회계정보가 마감되었습니다. [전표:${item.slipymd}-${item.slipno}]`);
  }

  if (!confirm(`선택한 ${items.length}건의 전표를 취소하시겠습니까?`)) return

  try {
    const res = await api.post('/hsio/HSIO_140U_CANCEL', {
        cmpycd: authStore.cmpycd,
        fromdt: searchForm.fromdt,
        todt: searchForm.todt,
        autoSlip: autoSlip.value,
        items: items.map(item => ({
          slipymd: item.slipymd,
          slipno: item.slipno,
          deptcd: item.deptcd
        }))
    })
    if (res.data?.status === 'SUCCESS' || res.data?.res === 'OK') {
        vAlert('정상적으로 취소되었습니다.');
        fetchList();
    } else {
        vAlertError(res.data?.message || '취소 실패');
    }
  } catch (e: any) { vAlertError(e.response?.data?.message || '오류 발생') }
}

const handleOpenHelp = (type: string) => {
  if (type === 'DEPT') {
    openHelp('DEPT', (d) => {
      searchForm.deptcd = d.deptcd;
      searchForm.deptnm = d.deptnm;
    });
  }
}

const toggleAllRows = () => {
  const rows = grid?.getRows(); if (!rows) return
  const selectedRows = grid?.getSelectedRows();
  if (rows.length === selectedRows?.length) grid?.deselectRow();
  else grid?.selectRow();
}

function initialize() {
  resetForm(searchForm);
  searchForm.deptcd = authStore.deptcd; searchForm.deptnm = authStore.deptnm;
  searchForm.fromdt = new Date().toISOString().substring(0, 7) + '-01';
  searchForm.todt = now.toISOString().substring(0, 10);
  grid?.clearData();
}

onUnmounted(() => {
  if (grid) grid.destroy();
});

onMounted(async () => {
  await fetchClosingInfo();
  if (mainGridRef.value) {
    grid = new Tabulator(mainGridRef.value, {
      layout: 'fitColumns', height: '100%', selectable: true,
      columnDefaults: { headerSort: false, headerHozAlign: "center", vertAlign: "middle" },
      columns: [
        { title: '선택', width: 50, hozAlign: 'center', formatter: 'rowSelection', titleFormatter: 'rowSelection', headerSort: false },
        { title: '전표번호', field: 'slipno_full', width: 150, hozAlign: 'center', cssClass: 'fw-bold text-primary cursor-pointer',
          mutatorData: (v, d) => d.slipymd && d.slipno ? `${d.slipymd}-${d.slipno}` : '',
          cellClick: (e, cell) => {
            const d = cell.getRow().getData();
            window.open(`../HASL/HASL_SLIP_PRINT.asp?slipgu=010&slipymd=${d.slipymd}&slipno=${d.slipno}&deptcd=${d.deptcd}`, '전표인쇄', 'left=10,top=10,width=700,height=650,scrollbars=yes');
          }
        },
        { title: '발행부서', field: 'deptnm', width: 140, hozAlign: 'left' },
        { title: '거래처', field: 'custnm', minWidth: 150, widthGrow: 1, hozAlign: 'left', cssClass: 'fw-bold' },
        { title: '유형', field: 'vattypenm', width: 100 },
        { title: '공급가', field: 'spyamt', width: 110, hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 } },
        { title: '부가세', field: 'vatamt', width: 100, hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 } },
        { title: '합계', field: 'jsansum', width: 120, hozAlign: 'right', formatter: 'money', cssClass: 'bg-light fw-bold', mutatorData: (v, d) => Number(d.spyamt || 0) + Number(d.vatamt || 0) }
      ]
    })
  }
  nextTick(() => fetchList())
})
</script>

