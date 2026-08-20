<!--
	=============================================================
	프로그램명	: 대차대조표 (HACL020S)
	작성일자	: 2025.02.24
	설명        : HSOD100U 표준 그리드 정의를 준수하는 대차대조표 조회
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-file-earmark-spreadsheet me-2 text-primary" style="font-size: 18px;"></i>
        재무제표 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">대차대조표 (HACL020S)</span>
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
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white">
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="tableRef" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import * as XLSX from 'xlsx'
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

const sNUM = ['', 'Ⅰ', 'Ⅱ', 'Ⅲ', 'Ⅳ', 'Ⅴ', 'Ⅵ', 'Ⅶ', 'Ⅷ', 'Ⅸ']

const initGrid = () => {
  if (!tableRef.value) return;
  grid = new Tabulator(tableRef.value, {
    layout: "fitColumns",
    height: "100%",
    placeholder: "데이터 없음",
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      {
        title: "과 목", field: "formatted_nm", widthGrow: 2,
        formatter: "html",
        cssClass: "border-end bg-light fw-bold"
      },
      {
        title: "당 기",
        columns: [
          { title: "금 액", field: "camtl", width: 160, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
          { title: "합 계", field: "camtr", width: 160, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "fw-bold border-start bg-light-subtle" },
          { title: "%", field: "rate", width: 70, hozAlign: "center" }
        ]
      },
      {
        title: "전 기",
        columns: [
          { title: "금 액", field: "bamtl", width: 160, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "border-start" },
          { title: "합 계", field: "bamtr", width: 160, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "fw-bold border-start bg-light-subtle" },
          { title: "%", field: "rate_b", width: 70, hozAlign: "center", cssClass: "border-start" }
        ]
      }
    ],
  });
}

const search = async () => {
  try {
    const res = await api.post('/hacl/HACL_020S_STR', {
      cmpycd: authStore.cmpycd,
      yymm: `${searchForm.yy}${searchForm.mm}`
    });

    const rawData = res.data || []

    let i = 1, j = 1, k = 1
    const processedData = rawData.map((row: any, idx: number) => {
      const acctcd = String(row.acctcd || '').trim()
      const acctnm = String(row.acctnm || '')
      const camtl = Number(row.camtl || 0), camtr = Number(row.camtr || 0)
      const bamtl = Number(row.bamtl || 0), bamtr = Number(row.bamtr || 0)
      const assettot = Number(row.assettot || 0), assettotb = Number(row.assettotb || 0)

      let formatted_nm = acctnm, rate: any = "", rate_b: any = "", spaces = ""

      if (acctcd.substring(1, 7) === "000000" || ["1990000", "2980000", "3980000", "3990000"].includes(acctcd)) {
        formatted_nm = `[ ${acctnm} ]`
      } else if (acctcd.substring(2, 7) === "00000") {
        formatted_nm = `${sNUM[i] || i}. ${acctnm}`; i++;
        if (assettot !== 0) rate = ((camtr / assettot) * 100).toFixed(1);
        if (assettotb !== 0) rate_b = ((bamtr / assettotb) * 100).toFixed(1);
      } else if (acctcd.substring(3, 7) === "0000") {
        formatted_nm = `${j}). ${acctnm}`; j++; spaces = "&nbsp;&nbsp;";
        if (assettot !== 0) rate = ((camtr / assettot) * 100).toFixed(1);
        if (assettotb !== 0) rate_b = ((bamtr / assettotb) * 100).toFixed(1);
      } else if (acctcd.substring(5, 7) === "00") {
        formatted_nm = (row.rstyn === 'Y') ? acctnm : `${k}. ${acctnm}`;
        if (row.rstyn !== 'Y') k++;
        spaces = "&nbsp;&nbsp;&nbsp;&nbsp;";
      } else {
        formatted_nm = acctnm;
        spaces = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
      }

      const next = rawData[idx + 1]
      if (next) {
        const nCd = String(next.acctcd || '').trim()
        if (acctcd.substring(0, 3) !== nCd.substring(0, 3)) k = 1
        if (acctcd.substring(0, 2) !== nCd.substring(0, 2)) j = 1
        if (acctcd.substring(0, 1) !== nCd.substring(0, 1)) i = 1
      }

      return { ...row, formatted_nm: spaces + formatted_nm, camtl, camtr, rate, bamtl, bamtr, rate_b }
    })

    grid?.setData(processedData)
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

const excel = () => grid?.download("xlsx", `대차대조표_${searchForm.yy}${searchForm.mm}.xlsx`)
const print = () => window.open(`/hacl/HACL_020P?yy=${searchForm.yy}&mm=${searchForm.mm}&PRTGU=1`)

onMounted(() => {
  if (typeof window !== 'undefined') (window as any).XLSX = XLSX
  nextTick(initGrid)
  search()
})
</script>

<style scoped>
.erp-label { min-width: 80px; font-weight: 500; font-size: 13px; }
.tabulator-instance { border: 1px solid #dee2e6 !important; width: 100% !important; background-color: #fff; }
</style>
