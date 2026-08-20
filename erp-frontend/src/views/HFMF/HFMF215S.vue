<!--
	=============================================================
	프로그램명	: 제조경비 배부명세서 (HFMF215S)
	작성일자	: 2025.02.24
	설명        : 공정별 제조경비 배부 현황 조회 (HSOD100U 표준 UI 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <DeptHelpModal :visible="deptHelpVisible" :cmpycd="authStore.cmpycd" @close="deptHelpVisible = false" @confirm="onSelectDept" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-file-earmark-spreadsheet me-2 text-primary" style="font-size: 18px;"></i>
        원가관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        현황조회 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">제조경비 배부명세서 (HFMF215S)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-search" @click="handleSearch">조회</button>
        <button class="btn-erp btn-excel" @click="handleExport">Excel</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 조회 필터 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
                <col style="width: 10%" /><col style="width: 25%" />
                <col style="width: 10%" /><col style="width: 55%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light required">마감년월</th>
                <td>
                  <input v-model="searchForm.ym" type="month" class="form-control form-control-sm" style="max-width: 150px;" @change="handleSearch" />
                </td>
                <th class="text-center bg-light">부서선택</th>
                <td>
                  <div class="input-group input-group-sm" style="max-width: 400px;">
                    <input v-model="searchForm.deptcd" class="form-control text-center bg-light" style="max-width: 100px;" placeholder="코드" readonly />
                    <input v-model="searchForm.deptnm" class="form-control bg-light" placeholder="전체 (선택 안할 시)" readonly @click="openDeptHelp" />
                    <button class="btn btn-outline-secondary" type="button" @click="openDeptHelp"><i class="bi bi-search"></i></button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column grid-container-right">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>공정별 배부 명세</span>
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
import { useAuthStore } from '@/stores/authStore'
import AppAlert from '@/components/AppAlert.vue'
import DeptHelpModal from '@/components/DeptHelpModal.vue'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()

const searchForm = reactive({
	ym: new Date().toISOString().substring(0, 7),
	deptcd: '',
	deptnm: ''
})

const deptHelpVisible = ref(false)
const mainGridRef = ref<HTMLElement | null>(null)
let mainGrid: Tabulator | null = null

const openDeptHelp = () => { deptHelpVisible.value = true }
const onSelectDept = (data: any) => {
	searchForm.deptcd = String(data.deptcd || '').trim()
	searchForm.deptnm = String(data.deptnm || '').trim()
	deptHelpVisible.value = false
	handleSearch()
}

const handleSearch = async () => {
	try {
		const ym = searchForm.ym.replace('-', '')
		const { data } = await api.post('/hfmf/FMF2150R_STR', {
			cmpycd: String(authStore.cmpycd),
			yymm: String(ym),
			deptcd: String(searchForm.deptcd)
		})
		mainGrid?.setData(data)
    vAlert('조회되었습니다.')
	} catch (e) { vAlertError('데이터 조회 실패') }
}

const handleExport = () => {
	mainGrid?.download('xlsx', `제조경비배부명세서_${searchForm.ym}.xlsx`)
}

onMounted(() => {
	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns', height: '100%',
      columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
			columns: [
				{ title: "No", formatter: "rownum", width: 40, hozAlign: "center" },
				{ title: '계정과목', field: 'acctnm', width: 200, frozen: true, hozAlign: 'left', cssClass: 'fw-bold' },
				{ title: '가공공정', field: 'amt1', hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 } },
				{ title: '조립공정', field: 'amt2', hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 } },
				{ title: '포장공정', field: 'amt3', hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 } },
				{ title: '공정4', field: 'amt4', hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 } },
				{ title: '공정5', field: 'amt5', hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 } },
				{ title: '공정6', field: 'amt6', hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 } },
				{ title: '공정7', field: 'amt7', hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 } },
				{ title: '합계', field: 'sumamt', width: 120, hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 }, cssClass: 'fw-bold text-primary bg-light' }
			]
		})
	}
	handleSearch()
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
