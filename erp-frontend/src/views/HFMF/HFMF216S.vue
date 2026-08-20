<!--
	=============================================================
	프로그램명	: 제조원가명세서 (HFMF216S)
	작성일자	: 2025.02.24
	설명        : 기간별 제조원가 명세서 조회 및 출력 (HSOD100U 표준 UI 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 섹션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-file-earmark-bar-graph me-2 text-primary" style="font-size: 18px;"></i>
        원가관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        현황조회 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">제조원가명세서 (HFMF216S)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-excel" @click="excel">엑셀</button>
        <button class="btn-erp btn-print" @click="print">인쇄</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 조회 필터 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
                <col style="width: 8%" /><col style="width: 17%" />
                <col style="width: 8%" /><col style="width: 17%" />
                <col style="width: 8%" /><col style="width: 17%" />
                <col style="width: 25%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light required">조회년월</th>
                <td>
                  <div class="d-flex align-items-center gap-1 px-1">
                    <select v-model="searchForm.yy" class="form-select form-select-sm" style="width: 90px;">
                      <option v-for="y in yearOptions" :key="y" :value="y">{{ y }}년</option>
                    </select>
                    <select v-model="searchForm.mm" class="form-select form-select-sm" style="width: 75px;">
                      <option v-for="m in monthOptions" :key="m" :value="m">{{ m }}월</option>
                    </select>
                  </div>
                </td>
                <th class="text-center bg-light">제품구분</th>
                <td>
                  <div class="d-flex align-items-center gap-2 px-2">
                    <div class="form-check form-check-inline mb-0 me-1">
                      <input v-model="searchForm.jsgbn" class="form-check-input" type="radio" name="jsgbn" id="jsgbn1" value="200" @change="search">
                      <label class="form-check-label small fw-bold" for="jsgbn1">제품</label>
                    </div>
                    <div class="form-check form-check-inline mb-0 me-0">
                      <input v-model="searchForm.jsgbn" class="form-check-input" type="radio" name="jsgbn" id="jsgbn2" value="210" @change="search">
                      <label class="form-check-label small fw-bold" for="jsgbn2">반제품</label>
                    </div>
                  </div>
                </td>
                <th class="text-center bg-light">조회구분</th>
                <td>
                  <div class="d-flex align-items-center gap-2 px-2">
                    <div class="form-check form-check-inline mb-0 me-1">
                      <input v-model="searchForm.mmgbn" class="form-check-input" type="radio" name="mmgbn" id="mmgbn1" value="M" @change="search">
                      <label class="form-check-label small fw-bold" for="mmgbn1">월계</label>
                    </div>
                    <div class="form-check form-check-inline mb-0 me-0">
                      <input v-model="searchForm.mmgbn" class="form-check-input" type="radio" name="mmgbn" id="mmgbn2" value="N" @change="search">
                      <label class="form-check-label small fw-bold" for="mmgbn2">누계</label>
                    </div>
                  </div>
                </td>
                <td class="text-muted small ps-3 border-start-0" style="font-size: 11px;">
                  <i class="bi bi-info-circle me-1"></i> 당월 발생 금액과 당기 누계 금액을 비교 조회합니다.
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column grid-container-right">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark"><i class="bi bi-table me-2 text-primary"></i>제조원가 상세명세 리스트</span>
        </div>
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
import { useAlerts } from '@/composables/useAlerts'
import AppAlert from '@/components/AppAlert.vue'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import * as XLSX from 'xlsx'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()

const now = new Date()
const currentYear = now.getFullYear()
const currentMonth = (now.getMonth() + 1).toString().padStart(2, '0')

const yearOptions = Array.from({ length: 6 }, (_, i) => String(currentYear - i))
const monthOptions = Array.from({ length: 12 }, (_, i) => String(i + 1).padStart(2, '0'))

const searchForm = reactive({
  yy: String(currentYear),
  mm: currentMonth,
  jsgbn: '200',
  mmgbn: 'M'
})

const tableRef = ref<HTMLDivElement | null>(null)
let grid: Tabulator | null = null

const initGrids = () => {
  if (!tableRef.value) return
  grid = new Tabulator(tableRef.value, {
    layout: "fitColumns", height: "100%", placeholder: "데이터 없음",
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "과목코드", field: "acctcd", width: 100, hozAlign: "center" },
      { title: "계정과목", field: "acctnm", minWidth: 250, widthGrow: 1, cssClass: "fw-bold text-primary" },
      { title: "당월금액", field: "cur_amt", hozAlign: "right", width: 180, formatter: "money", formatterParams: { precision: 0 } },
      { title: "당기누계", field: "sum_amt", hozAlign: "right", width: 180, formatter: "money", formatterParams: { precision: 0 }, cssClass: "text-primary fw-bold bg-light" },
      { title: "비고", field: "remark", minWidth: 200 }
    ],
  });
}

async function search() {
  try {
    const res = await api.post('/hfmf/FMF2160R_STR', {
          cmpycd: authStore.cmpycd,
          yymm: searchForm.yy + searchForm.mm,
          costcd: '10000',
          jasancd: searchForm.jsgbn,
          mmgbn: searchForm.mmgbn
    })
    grid?.setData(res.data || [])
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

const excel = () => {
    if (!grid) return vAlertError('조회된 데이터가 없습니다.')
    grid.download("xlsx", `제조원가명세서_${searchForm.yy}_${searchForm.mm}.xlsx`, { sheetName: '제조원가명세서' })
}

const print = () => {
  const params = `yy=${searchForm.yy}&mm=${searchForm.mm}&PRTGU=1`
  window.open(`/hfmf/HFMF_216P?${params}`, 'CostStatement', 'width=1000,height=800,scrollbars=yes')
}

onMounted(() => {
  if (typeof window !== 'undefined') (window as any).XLSX = XLSX;
  nextTick(() => {
    initGrids();
    search();
  });
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
