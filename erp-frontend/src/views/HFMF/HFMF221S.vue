<!--
	=============================================================
	프로그램명	: 제조원가명세서 (HFMF221S)
	작성일자	: 2025.02.24
	설명        : 제조원가 명세 (전월누계 / 당월 / 당월누계) 조회 (HSOD100U 표준 UI 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-file-earmark-ruled me-2 text-primary" style="font-size: 18px;"></i>
        원가관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        현황조회 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">제조원가명세서 (HFMF221S)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-search" @click="handleSearch">조회</button>
        <button class="btn-erp btn-excel" @click="handleExport">엑셀저장</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 조회 필터 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
                <col style="width: 10%" /><col style="width: 90%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light required">마감년월</th>
                <td>
                  <input v-model="searchForm.ym" type="month" class="form-control form-control-sm ms-2" style="max-width: 150px;" @change="handleSearch" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column grid-container-right">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>제조원가 명세 (전월누계 / 당월 / 당월누계)</span>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="mainGridRef" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import AppAlert from '@/components/AppAlert.vue'

const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()

const searchForm = reactive({ ym: new Date().toISOString().substring(0, 7) })
const mainGridRef = ref<HTMLElement | null>(null)
let mainGrid: Tabulator | null = null

const handleSearch = async () => {
	try {
		const ym = searchForm.ym.replace('-', '')
		const { data } = await api.post('/hfmf/FMF2210R_STR', { ym: ym, actkind: 'S0' })
		if (data.data) {
			mainGrid?.setData(data.data)
			vAlert('조회되었습니다.')
		}
	} catch (e) { vAlertError('조회 실패') }
}

const handleExport = () => {
	mainGrid?.download('xlsx', `제조원가명세서_${searchForm.ym}.xlsx`)
}

onMounted(() => {
	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns', height: '100%', placeholder: "데이터 없음",
      columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
			columns: [
				{
            title: '계정과목', field: 'acctnm', widthGrow: 3, hozAlign: 'left', frozen: true,
            formatter: (cell) => {
                const row = cell.getData();
                const acct = row.ACCT;
                const acctnm = row.acctnm;
                if (acct === '415-0000') return `<b>Ⅱ. ${acctnm}</b>`;
                if (acct === '418-0000') return `<b>Ⅲ. ${acctnm}</b>`;
                if (acct === '419-0000') return `<b>Ⅳ. ${acctnm}</b>`;
                if (acct === '411-0000') return `&nbsp;&nbsp;&nbsp;&nbsp;(1) ${acctnm}`;
                if (acct === '412-0000') return `&nbsp;&nbsp;&nbsp;&nbsp;(2) ${acctnm}`;
                if (acct === '413-0000') return `&nbsp;&nbsp;&nbsp;&nbsp;(3) ${acctnm}`;
                if (acct === '414-0000' || acct === '416-0000' || acct === '417-0000') return `&nbsp;&nbsp;&nbsp;&nbsp;${acctnm}`;
                return `&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;${acctnm}`;
            }
        },
				{ title: '전월누계', field: 'BEFosum', hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 } },
				{ title: '당월', field: 'CURRamt', hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 }, cssClass: 'bg-light fw-bold text-primary' },
				{ title: '당월누계', field: 'currsum', hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 } }
			]
		})
	}
	handleSearch()
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
