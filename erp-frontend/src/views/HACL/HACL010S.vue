<!--
	=============================================================
	프로그램명	: 시산표 (HACL010S)
	작성일자	: 2025.02.24
	설명        : HSOD100U 표준 그리드 패턴을 사용하여 시산표 조회
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-file-earmark-bar-graph me-2 text-primary" style="font-size: 18px;"></i>
        재무제표 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">시산표 (HACL010S)</span>
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
          <div class="d-flex align-items-center flex-wrap gap-3 small">
            <div class="d-flex align-items-center">
              <span class="erp-label" style="min-width: 70px;"><i class="bi bi-dot text-primary"></i>회계일자</span>
              <div class="d-flex align-items-center gap-1">
                <select v-model="searchForm.yy" class="form-select form-select-sm" style="width: 100px;" @change="search">
                  <option v-for="year in yearOptions" :key="year" :value="year">{{ year }}년</option>
                </select>
                <select v-model="searchForm.mm" class="form-select form-select-sm" style="width: 80px;" @change="search">
                  <option v-for="month in monthOptions" :key="month" :value="month">{{ month }}</option>
                </select>
                <span class="ms-1 fw-bold">월</span>
              </div>
            </div>
            <div class="d-flex align-items-center">
              <span class="erp-label" style="min-width: 50px;"><i class="bi bi-dot text-primary"></i>구분</span>
              <select v-model="searchForm.actkind" class="form-select form-select-sm" style="width: 120px;" @change="search">
                <option value="88">당월시산표</option>
                <option value="00">누계시산표</option>
              </select>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 📊 3. 그리드 영역 -->
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
import * as XLSX from 'xlsx'
import { useAlerts } from '@/composables/useAlerts'
import AppAlert from '@/components/AppAlert.vue'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()

const yearOptions = Array.from({ length: 16 }, (_, i) => String(2025 - i))
const monthOptions = Array.from({ length: 12 }, (_, i) => String(i + 1).padStart(2, '0'))

const searchForm = reactive({
  yy: "2011",
  mm: "06",
  actkind: '88'
})

const tableRef = ref<HTMLDivElement | null>(null)
let grid: Tabulator | null = null

const initGrid = () => {
  if (!tableRef.value) return;
  grid = new Tabulator(tableRef.value, {
    layout: "fitColumns",
    height: "100%",
    placeholder: "데이터 없음",
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      {
        title: "차  변 (Debit)",
        columns: [
          { title: "잔액", field: "jdbamt", width: 180, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "border-end" },
          { title: "합계", field: "dbamt", width: 180, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } }
        ]
      },
      {
        title: "계정과목", field: "acctnm", widthGrow: 1, hozAlign: "center",
        formatter: "html",
        cssClass: "border-start border-end bg-light fw-bold"
      },
      {
        title: "대  변 (Credit)",
        columns: [
          { title: "합계", field: "cramt", width: 180, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "border-end" },
          { title: "잔액", field: "jcramt", width: 180, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } }
        ]
      }
    ],
  });
}

const search = async () => {
  try {
    const res = await api.post('/hacl/HACL_010S_STR', {
      cmpycd: authStore.cmpycd,
      yymm: `${searchForm.yy}${searchForm.mm}`,
      gbn: searchForm.actkind
    });

    const rawData = res.data || []
    const processedData = rawData.map((row: any) => {
      const is_total = String(row.acctcd) === '9999999'
      return {
        ...row,
        acctnm: is_total ? `<strong>${row.acctnm}</strong>` : row.acctnm,
        jdbamt: Number(row.jdbamt || 0),
        dbamt: Number(row.dbamt || 0),
        cramt: Number(row.cramt || 0),
        jcramt: Number(row.jcramt || 0),
        _is_total: is_total
      }
    })

    grid?.setData(processedData)
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

const excel = () => grid?.download("xlsx", `시산표_${searchForm.yy}${searchForm.mm}.xlsx`)
const print = () => window.open(`/hacl/HACL_010P?yy=${searchForm.yy}&mm=${searchForm.mm}&actkind=${searchForm.actkind}&PRTGU=1`)

onMounted(() => {
  if (typeof window !== 'undefined') (window as any).XLSX = XLSX
  nextTick(initGrid)
  search()
})
</script>

<style scoped>
.erp-label { min-width: 50px; font-weight: 500; font-size: 13px; }
.tabulator-instance { border: 1px solid #dee2e6 !important; width: 100% !important; background-color: #fff; }
</style>
