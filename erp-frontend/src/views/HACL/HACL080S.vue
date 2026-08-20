<!--
	=============================================================
	프로그램명	: 현금흐름표 (Statement of Cash Flows)
	작성일자	: 2025.02.24
	설명        : HSOD100U 표준 그리드 패턴을 준수하여 현금흐름표(당월 vs 전월) 조회
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-cash-stack me-2 text-primary" style="font-size: 18px;"></i>
        재무제표 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">현금흐름표 (HACL080S)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-print" @click="print">인쇄</button>
        <button class="btn-erp btn-excel" @click="excel">엑셀</button>
      </div>
    </div>

    <!-- 🔍 2. 조회 조건 영역 -->
    <div class="p-2 pb-0 flex-shrink-0 bg-light">
      <div class="card border shadow-sm overflow-hidden">
        <div class="card-body p-2 bg-white">
          <div class="d-flex align-items-center gap-3 small">
            <div class="d-flex align-items-center">
              <span class="erp-label" style="min-width: 70px;"><i class="bi bi-dot text-primary"></i>회계일자</span>
              <div class="d-flex align-items-center gap-1">
                <select v-model="searchForm.yy" class="form-select form-select-sm" style="width: 100px;" @change="search">
                  <option v-for="year in yearOptions" :key="year" :value="year">{{ year }}년</option>
                </select>
                <select v-model="searchForm.mm" class="form-select form-select-sm" style="width: 80px;" @change="search">
                  <option v-for="month in monthOptions" :key="month" :value="month">{{ month }}</option>
                </select>
                <span class="ms-1 fw-bold">월 현재</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 📊 3. 그리드 영역 (HSOD100U 표준 패턴) -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column bg-light">
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white rounded-0">
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="tableRef" class="tabulator-instance flex-grow-1 border-0"></div>
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

const yearOptions = Array.from({ length: 16 }, (_, i) => String(2025 - i))
const monthOptions = Array.from({ length: 12 }, (_, i) => String(i + 1).padStart(2, '0'))

const searchForm = reactive({ yy: "2011", mm: "06" })

const tableRef = ref<HTMLDivElement | null>(null)
let grid: Tabulator | null = null

const sNUM = ['', 'Ⅰ', 'Ⅱ', 'Ⅲ', 'Ⅳ', 'Ⅴ', 'Ⅵ', 'Ⅶ', 'Ⅷ', 'Ⅸ', 'Ⅹ']

const initGrid = () => {
  if (!tableRef.value) return;
  grid = new Tabulator(tableRef.value, {
    layout: "fitColumns",
    height: "100%",
    placeholder: "데이터 없음",
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      {
        title: "과 목", field: "formatted_nm", widthGrow: 1.5,
        formatter: "html",
        cssClass: "border-end bg-light fw-bold",
        cellClick: (e, cell) => goDrillDown(cell.getData())
      },
      {
        title: "당 기",
        columns: [
          { title: "금 액(원)", field: "camtl", width: 160, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
          { title: "합 계(원)", field: "camtr", width: 160, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "border-start" }
        ]
      },
      {
        title: "전 기",
        columns: [
          { title: "금 액(원)", field: "bamtl", width: 160, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "border-start" },
          { title: "합 계(원)", field: "bamtr", width: 160, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "border-start" }
        ]
      }
    ],
  });
}

const search = async () => {
  try {
    const res = await api.post('/hacl/HACL_080S_STR', {
      cmpycd: authStore.cmpycd,
      yymm: `${searchForm.yy}${searchForm.mm}`
    });

    const rawData = res.data || []

    let i = 1, j = 1, k = 1
    let prev_acctcd = ""

    const processedData = rawData.map((row: any) => {
      const acctcd = String(row.acctcd || row.col0 || '').trim()
      const acctnm = String(row.acctnm || row.col1 || '')
      const camtl = Number(row.c_amt_l || row.camtl || 0)
      const camtr = Number(row.c_amt_r || row.camtr || 0)
      const bamtl = Number(row.b_amt_l || row.bamtl || 0)
      const bamtr = Number(row.b_amt_r || row.bamtr || 0)

      if (prev_acctcd) {
        if (acctcd.substring(0, 3) !== prev_acctcd.substring(0, 3)) k = 1
        if (acctcd.substring(0, 1) !== prev_acctcd.substring(0, 1)) j = 1
      }
      prev_acctcd = acctcd

      let formatted_nm = acctnm, spaces = "", is_header = false

      if (acctcd.substring(1, 7) === "000000") {
        formatted_nm = `${sNUM[i] || i}. ${acctnm}`; i++; is_header = true;
      } else if (acctcd.substring(2, 7) === "00000") {
        formatted_nm = `${j}. ${acctnm}`; j++; spaces = "&nbsp;&nbsp;"; is_header = true;
      } else {
        formatted_nm = `${k}) ${acctnm}`; k++; spaces = "&nbsp;&nbsp;&nbsp;&nbsp;";
      }

      const drill_amt = camtl !== 0 ? camtl : camtr

      return {
        ...row,
        formatted_nm: `<span class="text-primary text-decoration-underline cursor-pointer">${spaces + formatted_nm}</span>`,
        camtl, camtr, bamtl, bamtr,
        drill_amt, acctcd, acctnm,
        _is_header: is_header
      }
    })

    grid?.setData(processedData)
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

const goDrillDown = (data: any) => {
  if (!data.acctcd) return
  const params = `yy=${searchForm.yy}&mm=${searchForm.mm}&acctcd=${data.acctcd}&acctnm=${data.acctnm}&amt=${data.drill_amt}`
  window.open(`/hacl/HACL_081U?${params}`, 'CashFlowDetail', 'width=500,height=600,scrollbars=yes')
}

const excel = () => grid?.download("xlsx", `현금흐름표_${searchForm.yy}${searchForm.mm}.xlsx`)
const print = () => window.open(`/hacl/HACL_080P?yy=${searchForm.yy}&mm=${searchForm.mm}&PRTGU=1`)

onMounted(() => {
  nextTick(initGrid)
  search()
})
</script>

<style scoped>
.erp-label { min-width: 80px; font-weight: 500; font-size: 13px; }
.tabulator-instance { border: 1px solid #dee2e6 !important; width: 100% !important; background-color: #fff; }
</style>
