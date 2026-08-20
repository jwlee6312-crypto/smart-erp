<!--
	=============================================================
	프로그램명	: 품목별단가관리 (HSBA060U)
	작성일자	: 2025.02.27
	설명        : 품목별 매입/매출 단가 관리 (내비게이션 및 체크버튼 표준화 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 (Navigation & Buttons) -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-currency-dollar me-2 text-primary" style="font-size: 18px;"></i>
        기본정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        품목관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">품목별단가관리 (HSBA060U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-save" @click="save">저장</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 조회 필터 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
              <col style="width: 100px;" /><col style="width: 250px;" />
              <col style="width: 100px;" /><col style="width: 250px;" />
              <col />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light">매입/매출</th>
                <td>
                  <select v-model="form_01.iogbn" class="form-select form-select-sm" @change="onIogbnChange" style="max-width: 300px;">
                    <option v-for="opt in iogbnOptions" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                  </select>
                </td>
                <th class="text-center bg-light">단가구분</th>
                <td>
                  <select v-model="form_01.prcgbn" class="form-select form-select-sm" style="max-width: 300px;">
                    <option v-for="opt in prcgbnOptions" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                  </select>
                </td>
                <td></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 데이터 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column" style="min-height: 0;">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>품목별 단가 목록</span>
          <div class="d-flex gap-1">
            <button class="btn btn-sm btn-outline-primary py-0 px-2 fw-bold" @click="applyBulkStd(true)" style="font-size: 11px;">기본단가 지정</button>
            <button class="btn btn-sm btn-outline-secondary py-0 px-2 fw-bold" @click="applyBulkStd(false)" style="font-size: 11px;">지정 해제</button>
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
import { reactive, ref, onMounted, nextTick, onUnmounted } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import AppAlert from '@/components/AppAlert.vue'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { useSearchStore } from '@/stores/useSearchStore'
import { useRoute } from 'vue-router'

const authStore = useAuthStore()
const searchStore = useSearchStore()
const route = useRoute()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

// [1] 데이터 모델링
const form_01 = reactive({ iogbn: '', prcgbn: '' })
const iogbnOptions = ref<any[]>([]); const prcgbnOptions = ref<any[]>([])
const gridElement = ref<HTMLElement | null>(null); const grid = ref<Tabulator | null>(null)

// [2] 그리드 초기화 (내비게이션 및 체크버튼 표준 설정)
const initGrid = () => {
  if (!gridElement.value) return
  grid.value = new Tabulator(gridElement.value, {
    layout: "fitColumns",
    height: "100%",
    placeholder: "조회된 데이터가 없습니다.",
    selectable: true,
    pagination: "local",
    paginationSize: 100,
    paginationButtonCount: 10,
    paginationSizeSelector: [50, 100, 200, 500],
    columnDefaults: { headerHozAlign: "center", headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "", width: 40, hozAlign: "center", headerHozAlign: "center", formatter: "rowSelection", titleFormatter: "rowSelection", resizable: false },
      { title: "품목코드", field: "itemcd", width: 100, hozAlign: "center", cssClass: "fw-bold text-primary" },
      { title: "품목명", field: "itemnm", minWidth: 250, hozAlign: "left" },
      { title: "규격", field: "itsize", width: 200, hozAlign: "left" },
      { title: "단위", field: "unit", width: 80, hozAlign: "center" },
      { title: "단가", field: "price", width: 150, hozAlign: "right", editor: "number", formatter: "money", formatterParams: { precision: 0 } },
      // 🚀 [해결] 시각적으로 정돈된 커스텀 체크박스 포맷터 (크기 조정: 15px)
      {
        title: "기본단가", field: "stdyn", width: 100, hozAlign: "center",
        formatter: (cell) => {
          const val = cell.getValue();
          return val === 'Y'
            ? '<i class="bi bi-check-square-fill text-primary" style="font-size: 15px;"></i>'
            : '<i class="bi bi-square text-secondary opacity-50" style="font-size: 15px;"></i>';
        },
        cellClick: (e, cell) => {
          const val = cell.getValue() === 'Y' ? 'N' : 'Y';
          cell.setValue(val);
        }
      },
      { title: "비고", field: "remark", minWidth: 200, editor: "input", hozAlign: "left" }
    ]
  })
}

// [3] 옵션 로드 로직
async function fetchOptions() {
  try {
    const resIo = await api.post('/hs00/HS00_000S_STR', { gubun: 'E0', gbncd: '210' })
    iogbnOptions.value = (resIo.data || []).map((i: any) => ({
      codecd: String(i.code || i.codecd || '').trim(),
      codenm: String(i.cdnm || i.codenm || '').trim()
    }))
    if (iogbnOptions.value.length) form_01.iogbn = iogbnOptions.value[0].codecd
    await updatePrcGbnOptions()
  } catch (e) { console.error('공통 옵션 로드 실패') }
}

async function updatePrcGbnOptions() {
  try {
    const resPrc = await api.post('/hs00/HS00_000S_STR', { gubun: 'E0', gbncd: '200' })
    prcgbnOptions.value = (resPrc.data || []).map((i: any) => ({
      codecd: String(i.code || i.codecd || '').trim(),
      codenm: String(i.cdnm || i.codenm || '').trim()
    }))
    if (prcgbnOptions.value.length) form_01.prcgbn = prcgbnOptions.value[0].codecd
  } catch (e) { console.error('단가구분 옵션 로드 실패') }
}

async function onIogbnChange() { await updatePrcGbnOptions() }

async function search() {
  if (!form_01.iogbn || !form_01.prcgbn) return;
  try {
    const res = await api.post('/hsba/HSBA_060U_STR', {
      actkind: 'S0', cmpycd: authStore.cmpycd, iogbn: form_01.iogbn,
      itemcd: '', prcgbn: form_01.prcgbn, price: 0, stdyn: '',
      remark: '', useyn: '', userid: authStore.userid
    })
    if (grid.value) {
      grid.value.setData((res.data || []).map((i: any) => ({
        ...i,
        stdyn: String(i.stdyn || i.STDYN || 'N').trim().toUpperCase() === 'Y' ? 'Y' : 'N',
        price: Number(i.price) || 0
      })))
      grid.value.selectRow();
    }
    vAlert('조회되었습니다.');
  } catch (e) { vAlertError('조회 실패') }
}

async function save() {
  const selectedData = grid.value?.getSelectedData();
  if (!selectedData || selectedData.length === 0) return vAlertError('저장할 항목을 선택해 주십시오.')
  if (!confirm('선택한 품목의 단가 정보를 저장하시겠습니까?')) return

  try {
    for (const row of selectedData) {
      await api.post('/hsba/HSBA_060U_STR', {
        actkind: 'A0', cmpycd: authStore.cmpycd, iogbn: form_01.iogbn,
        itemcd: row.itemcd, prcgbn: form_01.prcgbn, price: row.price,
        stdyn: row.stdyn === 'Y' ? 'Y' : 'N',
        remark: row.remark, useyn: 'Y', userid: authStore.userid
      })
    }
    vAlert('정상적으로 저장되었습니다.'); search()
  } catch (e) { vAlertError('저장 실패') }
}

function initialize() {
  if (grid.value) grid.value.clearData();
  resetForm(form_01);
  if (iogbnOptions.value.length) form_01.iogbn = iogbnOptions.value[0].codecd;
  updatePrcGbnOptions();
}

const applyBulkStd = (val: boolean) => {
  const selectedRows = grid.value?.getSelectedRows();
  if (!selectedRows?.length) return vAlertError('대상 항목을 먼저 선택하세요.');
  selectedRows.forEach(row => row.update({ stdyn: val ? 'Y' : 'N' }));
}

onMounted(async () => {
  await fetchOptions();
  nextTick(() => {
    initGrid();
    if (form_01.iogbn) search()
  })
})

onUnmounted(() => { searchStore.removeTab(route.name as string) });
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
/* 🚀 [해결] 첫 번째 컬럼 선택 체크박스가 확실히 보이도록 스타일 강제 적용 */
:deep(.tabulator-cell[tabulator-field=""]) {
  display: flex !important; align-items: center !important; justify-content: center !important;
}

:deep(.tabulator-footer) {
  background-color: #f8f9fa !important;
  border-top: 1px solid #dee2e6 !important;
  font-size: 12px !important;
  padding: 5px !important;
}
:deep(.tabulator-page) {
  padding: 2px 8px !important;
  margin: 0 2px !important;
  border: 1px solid #ddd !important;
  border-radius: 3px !important;
  background: #fff !important;
}
:deep(.tabulator-page.active) {
  background: #0d6efd !important;
  color: #fff !important;
  border-color: #0d6efd !important;
}
</style>
