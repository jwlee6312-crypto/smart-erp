<!--
	=============================================================
	프로그램명	: 손익계산서 (Income Statement)
	작성일자	: 2025.02.24
	설명        : HSOD100U 표준 그리드 정의에 따른 손익계산서 조회
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-graph-up-arrow me-2 text-primary" style="font-size: 18px;"></i>
        재무제표 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">손익계산서 (HACL030S)</span>
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
                <span class="ms-1 fw-bold">월 까지</span>
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

const yearOptions = Array.from({ length: 16 }, (_, i) => String(2025 - i))
const monthOptions = Array.from({ length: 12 }, (_, i) => String(i + 1).padStart(2, '0'))

const searchForm = reactive({ yy: "2011", mm: "06" })

const mainGridRef = ref<HTMLDivElement | null>(null)
let grid: Tabulator | null = null

const sNUM = ['', 'Ⅰ', 'Ⅱ', 'Ⅲ', 'Ⅳ', 'Ⅴ', 'Ⅵ', 'Ⅶ', 'Ⅷ', 'Ⅸ', 'Ⅹ', 'Ⅺ', 'Ⅻ', 'XIII', 'XIV']

const initGrid = () => {
  if (!mainGridRef.value) return;
  grid = new Tabulator(mainGridRef.value, {
    layout: "fitColumns",
    height: "100%",
    placeholder: "데이터 없음",
    columnDefaults: { headerSort: false, vertAlign: "middle", headerHozAlign: "center" },
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
          { title: "합 계", field: "camtr", width: 160, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "bg-light-subtle fw-bold" },
          { title: "%", field: "rate", width: 60, hozAlign: "center", formatter: (c) => c.getValue() ? Number(c.getValue()).toFixed(1) : "" }
        ]
      },
      {
        title: "전 기",
        columns: [
          { title: "금 액", field: "bamtl", width: 160, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
          { title: "합 계", field: "bamtr", width: 160, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "bg-light-subtle fw-bold" },
          { title: "%", field: "rate_b", width: 60, hozAlign: "center", formatter: (c) => c.getValue() ? Number(c.getValue()).toFixed(1) : "" }
        ]
      }
    ],
  });
}

const search = async () => {
  try {
    const res = await api.post('/hacl/HACL_030S_STR', {
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
      const salestot = Number(row.salestot || 0), salsamt_b = Number(row.salsamt_b || 0)

      let formatted_nm = acctnm, rate = 0, rate_b = 0, padding = 0

      if (acctcd.substring(1, 7) === "000000" || ["1990000", "2980000", "3980000", "3990000"].includes(acctcd)) {
        formatted_nm = `[ ${acctnm} ]`
      } else if (acctcd.substring(2, 7) === "00000") {
        formatted_nm = `${sNUM[i] || i}. ${acctnm}`; i++
        if (salestot !== 0) rate = (camtr / salestot) * 100
        if (salsamt_b !== 0) rate_b = (bamtr / salsamt_b) * 100
      } else if (acctcd.substring(3, 7) === "0000") {
        formatted_nm = `&nbsp;&nbsp;${j}). ${acctnm}`; j++
        if (salestot !== 0) rate = (camtr / salestot) * 100
        if (salsamt_b !== 0) rate_b = (bamtr / salsamt_b) * 100
        padding = 10
      } else if (acctcd.substring(5, 7) === "00") {
        if (row.rstyn === 'Y' || acctcd === "5213000" || acctcd === "5223000") {
          formatted_nm = `&nbsp;&nbsp;&nbsp;&nbsp;${acctnm}`
          if (salestot !== 0) rate = (camtr / salestot) * 100
          if (salsamt_b !== 0) rate_b = (bamtr / salsamt_b) * 100
        } else {
          formatted_nm = `&nbsp;&nbsp;&nbsp;&nbsp;${k}. ${acctnm}`; k++
        }
        padding = 20
      } else {
        formatted_nm = `&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;${acctnm}`; padding = 30
      }

      const next = rawData[idx + 1]
      if (next) {
        const nCd = String(next.acctcd || '')
        if (acctcd.substring(0, 3) !== nCd.substring(0, 3)) { if (acctcd.substring(0, 2) !== "51") k = 1 }
        if (acctcd.substring(0, 2) !== nCd.substring(0, 2)) j = 1
        if (acctcd.substring(0, 1) !== nCd.substring(0, 1)) i = 1
      }

      return { ...row, formatted_nm, camtl, camtr, rate, bamtl, bamtr, rate_b, is_header: padding === 0 }
    })

    grid?.setData(processedData)
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

const excel = () => grid?.download("xlsx", `손익계산서_${searchForm.yy}${searchForm.mm}.xlsx`)
const print = () => window.open(`/hacl/HACL_030P?yy=${searchForm.yy}&mm=${searchForm.mm}&PRTGU=1`)

onMounted(() => {
  nextTick(initGrid)
  search()
})
</script>

<style scoped>
.erp-label { min-width: 80px; font-weight: 500; font-size: 13px; }
.tabulator-instance { border: 1px solid #dee2e6 !important; width: 100% !important; background-color: #fff; }
</style>
