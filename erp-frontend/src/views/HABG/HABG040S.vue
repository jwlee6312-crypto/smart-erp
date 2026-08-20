<!--
	=============================================================
	프로그램명	: 예산조정현황 (HABG040S)
	작성일자	: 2025.02.24
	작성자	    : AI Assistant
	설명        : 예산 년도 및 부서별 월별 예산 조정 내역 조회
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container d-flex flex-column h-100 bg-white">
		<!-- 🚀 상단 액션 바 -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-file-earmark-bar-graph me-2 text-primary" style="font-size: 18px;"></i>
				예산관리 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				<span class="text-primary fw-bolder">예산조정현황 (HABG040S)</span>
			</div>
			<div class="btn-group-erp d-flex gap-1">
				<button class="btn-erp btn-init" @click="initialize">
					<i class="bi bi-arrow-clockwise"></i> 초기화
				</button>
				<button class="btn-erp btn-search" @click="search">
					<i class="bi bi-search"></i> 조회
				</button>
				<button class="btn-erp btn-print" @click="print">
					<i class="bi bi-printer"></i> 인쇄
				</button>
				<button class="btn-erp btn-excel" @click="excel">
					<i class="bi bi-file-earmark-excel"></i> 엑셀
				</button>
			</div>
		</div>

		<!-- 💡 메인 컨텐츠 영역 -->
		<div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

			<!-- 🔍 조회 조건 영역 -->
			<div class="card border shadow-sm flex-shrink-0 overflow-hidden">
				<div class="card-body p-0 bg-white">
					<table class="erp-table-dense" width="100%">
						<colgroup>
							<col style="width: 12%" /><col style="width: 38%" />
							<col style="width: 12%" /><col style="width: 38%" />
						</colgroup>
						<tbody>
							<tr>
								<th class="text-center bg-light">예산년도</th>
								<td>
									<select v-model="searchForm.bugtyy" class="form-select form-select-sm" style="width: 110px;">
										<option v-for="year in yearOptions" :key="year" :value="year">{{ year }} 년</option>
									</select>
								</td>
								<th class="text-center bg-light">예산부서</th>
								<td>
									<div class="input-group input-group-sm" style="width: 250px;">
										<input v-model="searchForm.deptcd" type="text" class="form-control text-center bg-white" style="max-width: 65px;" readonly />
										<input v-model="searchForm.deptnm" type="text" class="form-control" @keydown.enter="openHelp('DEPT')" placeholder="부서 선택" />
										<button class="btn btn-outline-secondary px-2" @click="openHelp('DEPT')"><i class="bi bi-search"></i></button>
									</div>
								</td>
							</tr>
						</tbody>
					</table>
				</div>
			</div>

			<!-- 📊 그리드 영역 -->
			<div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white">
				<div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
					<div ref="mainGridRef" class="tabulator-instance flex-grow-1"></div>
				</div>
			</div>
		</div>
	</div>

	<Modal v-model:visible="modalVisible" :modalProps="modalProps" />
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import * as XLSX from 'xlsx'
import { useAlerts } from '@/composables/useAlerts'
import AppAlert from '@/components/AppAlert.vue'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import Modal from '@/components/Modal.vue'
import type { ModalProps } from '@/types/modal'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()

// 년도 옵션 설정 (ASP 로직: 현재년도+1 ~ 현재년도-3)
//const currentYear = new Date().getFullYear()
const currentYear = 2011
const yearOptions = Array.from({ length: 5 }, (_, i) => String(currentYear + 1 - i))

const searchForm = reactive({
	bugtyy: String(currentYear),
	deptcd: authStore.deptcd || '',
	deptnm: authStore.deptnm || ''
})

const mainGridRef = ref<HTMLDivElement | null>(null)
let mainGrid: Tabulator | null = null

const search = async () => {
	if (!searchForm.bugtyy) return vAlertError('예산년도를 선택하세요.')
	if (!searchForm.deptcd) return vAlertError('예산부서코드를 선택하세요.')

	try {
		const res = await api.post('/habg/HABG_040S_STR', {
			cmpycd: authStore.cmpycd,
			bugtyy: searchForm.bugtyy,
			deptcd: searchForm.deptcd
		})

		const data = (res.data || []).map((row: any) => {
			const m01 = Number(row.m01 || 0); const m02 = Number(row.m02 || 0);
			const m03 = Number(row.m03 || 0); const m04 = Number(row.m04 || 0);
			const m05 = Number(row.m05 || 0); const m06 = Number(row.m06 || 0);
			const m07 = Number(row.m07 || 0); const m08 = Number(row.m08 || 0);
			const m09 = Number(row.m09 || 0); const m10 = Number(row.m10 || 0);
			const m11 = Number(row.m11 || 0); const m12 = Number(row.m12 || 0);

			return {
				bugtcd: row.col0,
				bugtnm: row.col1,
				m01, m02, m03, m04, m05, m06,
				m07, m08, m09, m10, m11, m12,
				total: m01 + m02 + m03 + m04 + m05 + m06 + m07 + m08 + m09 + m10 + m11 + m12
			}
		})

		mainGrid?.setData(data)
		vAlert('조회되었습니다.')
	} catch (e) { vAlertError('조회 중 오류가 발생했습니다.') }
}

const initialize = () => {
	searchForm.bugtyy = String(currentYear)
	searchForm.deptcd = authStore.deptcd
	searchForm.deptnm = authStore.deptnm
	mainGrid?.clearData()
}

const print = () => {
	const params = `bugtyy=${searchForm.bugtyy}&deptcd_h=${searchForm.deptcd}&deptnm_h=${searchForm.deptnm}`
	window.open(`/habg/HABG_040P?${params}`, 'BudgetAdjustmentPrint', 'width=1000,height=800,scrollbars=yes')
}

const excel = () => {
	mainGrid?.download("xlsx", `예산조정현황_${searchForm.bugtyy}_${searchForm.deptnm}.xlsx`)
}

// 팝업 설정
const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function openHelp(type: 'DEPT') {
    Object.assign(modalProps, {
        title: '부서 선택', path: '/ha00/HA00_00P_STR', defaultField: 'deptnm',
        data: { gubun: 'D0', cmpycd: authStore.cmpycd, search: searchForm.deptnm },
        columns: [{ title: '코드', field: 'deptcd', width: 80 }, { title: '부서명', field: 'deptnm', width: 180 }],
        onConfirm: (d: any) => { searchForm.deptcd = d.deptcd; searchForm.deptnm = d.deptnm; search() }
    })
	modalVisible.value = true
}

onMounted(() => {
	if (typeof window !== 'undefined') (window as any).XLSX = XLSX
	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns',
			height: '100%',
			columnDefaults: { headerSort: false, vertAlign: "middle", hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
			columns: [
				{ title: "예산코드", field: "bugtcd", width: 90, hozAlign: "center", formatter: "plaintext", frozen: true },
				{ title: "예산명", field: "bugtnm", width: 150, hozAlign: "left", formatter: "plaintext", frozen: true },
				{
					title: "상반기",
					columns: [
						{ title: "1월", field: "m01", widthGrow: 1 },
						{ title: "2월", field: "m02", widthGrow: 1 },
						{ title: "3월", field: "m03", widthGrow: 1 },
						{ title: "4월", field: "m04", widthGrow: 1 },
						{ title: "5월", field: "m05", widthGrow: 1 },
						{ title: "6월", field: "m06", widthGrow: 1 },
					]
				},
				{
					title: "하반기",
					columns: [
						{ title: "7월", field: "m07", widthGrow: 1 },
						{ title: "8월", field: "m08", widthGrow: 1 },
						{ title: "9월", field: "m09", widthGrow: 1 },
						{ title: "10월", field: "m10", widthGrow: 1 },
						{ title: "11월", field: "m11", widthGrow: 1 },
						{ title: "12월", field: "m12", widthGrow: 1 },
					]
				},
				{ title: "합계", field: "total", widthGrow: 1, cssClass: "bg-light fw-bold text-primary" }
			],
			columnCalcs: "both"
		})
	}
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
:deep(.tabulator-cell) { border-right: 1px solid #dee2e6 !important; font-size: 12px; }
:deep(.tabulator-header .tabulator-col) { border-right: 1px solid #dee2e6 !important; background-color: #f8f9fa !important; font-size: 12px; }
:deep(.tabulator-header .tabulator-col-group) { border-bottom: 1px solid #dee2e6 !important; }
:deep(.bg-light) { background-color: #f1f5f9 !important; }
</style>
