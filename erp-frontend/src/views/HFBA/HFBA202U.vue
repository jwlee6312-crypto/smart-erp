<!--
	=============================================================
	프로그램명	: 제조원가 명세서 편집 (HFBA202U)
	작성일자	: 2025.03.14
	작성자	    : AI Assistant
	설명        : 제조원가 명세서의 계정 항목 및 순서 편집 관리 (조회 조건 추가)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-pencil-square me-2 text-primary" style="font-size: 18px;"></i>
        회계기준 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">제조원가 명세서 편집 (HFBA202U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-save" @click="save">저장</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [검색] Search 필터 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-2 bg-white">
          <table class="erp-table-dense w-100">
            <tbody>
              <tr>
                <th class="bg-light text-center" style="width: 100px;">적용연도</th>
                <td style="width: 150px;">
                  <select v-model="searchForm.yyyy" class="form-select form-select-sm fw-bold text-center" @change="search">
                    <option v-for="y in yearOptions" :key="y" :value="y">{{ y }}년</option>
                  </select>
                </td>
                <th class="bg-light text-center border-start" style="width: 100px;">재무제표</th>
                <td style="width: 200px;">
                  <select v-model="searchForm.gubun" class="form-select form-select-sm" disabled>
                    <option value="030">제조원가명세서</option>
                  </select>
                </td>
                <td class="px-3">
                    <button class="btn btn-sm btn-secondary px-3" @click="search"><i class="bi bi-search me-1"></i>조회</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [그리드] 계정 항목 구성 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex justify-content-between align-items-center">
          <span class="small fw-bold text-dark"><i class="bi bi-list-ol me-1 text-primary"></i>계정 항목 구성 ({{ searchForm.yyyy }}년)</span>
          <div class="d-flex gap-1">
            <button class="btn btn-sm btn-outline-primary py-0 px-2 fw-bold" @click="addRow" style="font-size: 11px;">+ 행추가</button>
          </div>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="mainGridRef" class="tabulator-instance flex-grow-1"></div>
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
import AppAlert from '@/components/AppAlert.vue'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()

const currentYear = new Date().getFullYear()
const yearOptions = ref<string[]>([])
const searchForm = reactive({
  yyyy: String(currentYear),
  gubun: '030'
})

const mainGridRef = ref<HTMLDivElement | null>(null)
let mainGrid: Tabulator | null = null

const initGrid = () => {
  if (!mainGridRef.value) return
  mainGrid = new Tabulator(mainGridRef.value, {
    layout: "fitColumns", height: "100%", placeholder: "데이터 없음",
    columnDefaults: { headerSort: false, headerHozAlign: "center", vertAlign: "middle" },
    columns: [
      { title: "순서", field: "rowno", width: 60, editor: "number" },
      { title: "계정코드", field: "prtacct", width: 100, editor: "input" },
      { title: "계정명칭", field: "prtacctnm", widthGrow: 1, editor: "input", hozAlign: "left" },
      { title: "구분", field: "dc", width: 100, editor: "list", editorParams: { values: { "1": "항목", "2": "소계", "3": "합계" } } },
      { title: "사용", field: "dspyn", width: 70, editor: "tickCross", formatter: "tickCross" },
      { title: "삭제", width: 40, hozAlign: "center", formatter: (c) => "<i class='bi bi-trash text-danger'></i>", cellClick: (e, cell) => cell.getRow().delete() }
    ]
  })
}

const fetchYears = async () => {
  try {
    const res = await api.post('/hfba/FBA2020U_SEL', {
        cmpycd: authStore.cmpycd,
        actkind: 'S9',
        gubun: searchForm.gubun
    })
    if (res.data && res.data.length > 0) {
      yearOptions.value = res.data.map((r: any) => String(r.yyyy))
      searchForm.yyyy = yearOptions.value[0]
    } else {
      yearOptions.value = [String(currentYear), String(currentYear - 1)]
    }
  } catch (e) {
    yearOptions.value = [String(currentYear), String(currentYear - 1)]
  }
}

const search = async () => {
  try {
    const res = await api.post('/hfba/FBA2020U_SEL', {
        cmpycd: authStore.cmpycd,
        actkind: 'S0',
        gubun: searchForm.gubun,
        yyyy: searchForm.yyyy
    })
    mainGrid?.setData(res.data)
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

const save = async () => {
  const data = mainGrid?.getData() || []
  if (!confirm('저장하시겠습니까?')) return
  try {
    await api.post('/hfba/FBA2020U_MOD', {
      cmpycd: authStore.cmpycd,
      yyyy: searchForm.yyyy,
      gubun: searchForm.gubun,
      items: data,
      userid: authStore.userid
    })
    vAlert('저장되었습니다.')
    search()
  } catch (e) { vAlertError('저장 실패') }
}

const addRow = () => mainGrid?.addRow({ useyn: "Y", gubun: "1", yyyy: searchForm.yyyy, gubun_val: searchForm.gubun }, true)
const initialize = () => { mainGrid?.clearData(); search() }

onMounted(async () => {
  await fetchYears()
  nextTick(() => { initGrid(); search() })
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
