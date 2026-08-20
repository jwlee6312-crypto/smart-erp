<!--	=============================================================
	프로그램명 : 사업장별 부가가치세 과세표준 및 납부세액 신고명세서
	작성일자	: 2025.02.24
	작성자    : AI Assistant
	설명        : 사업장별 매출/매입 세액 및 납부세액 총괄 현황 조회 (HSOD100U 표준 UI 적용)
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container">
		<!-- [Header] 액션 바 -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-file-earmark-ruled me-2 text-primary" style="font-size: 18px;"></i>
				세무관리<i class="bi bi-chevron-right mx-1 small opacity-50"></i>
				<span class="text-primary fw-bolder">사업장별 부가가치세 신고명세서 (HATX170S)</span>
			</div>
			<div class="btn-group-erp d-flex gap-1">
				<button class="btn-erp btn-init" @click="initialize">
					초기화
				</button>
				<button class="btn-erp btn-search" @click="search">
					<i class="bi bi-search"></i> 조회
				</button>
				<button class="btn-erp btn-print" @click="print">
					<i class="bi bi-printer"></i> 인쇄
				</button>
			</div>
		</div>

		<!-- [Search] 검색 조건 영역 -->
		<div class="p-2 pb-0 flex-shrink-0">
			<div class="card border shadow-sm bg-white overflow-hidden">
				<div class="card-body p-0">
					<table class="erp-table-dense w-100">
						<colgroup>
							<col style="width: 80px;" /><col />
						</colgroup>
						<tbody>
							<tr>
								<th class="bg-light text-center">기   간</th>
								<td>
									<div class="d-flex align-items-center gap-1 px-1">
										<select v-model="searchForm.yy" class="form-select form-select-sm" style="width: 100px;">
											<option v-for="y in yearOptions" :key="y" :value="y">{{ y }}년</option>
										</select>
										<select v-model="searchForm.fmm" class="form-select form-select-sm" style="width: 70px;">
											<option v-for="m in monthOptions" :key="m" :value="m">{{ m }}월</option>
										</select>
										<span>~</span>
										<select v-model="searchForm.tmm" class="form-select form-select-sm" style="width: 70px;">
											<option v-for="m in monthOptions" :key="m" :value="m">{{ m }}월</option>
										</select>
										<span class="ms-2 badge bg-primary-subtle text-primary border-0">{{ periodGubun }}</span>
									</div>
								</td>
							</tr>
						</tbody>
					</table>
				</div>
			</div>
		</div>

		<!-- [Content] 메인 콘텐츠 -->
		<div class="flex-grow-1 overflow-auto p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">
			<div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white">
				<div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
					<span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-1 text-primary"></i> 사업장별 신고 명세</span>
					<div class="small text-muted">※ 과세/영세/의제 매입세액 상세 포함</div>
				</div>
				<div class="card-body p-0 flex-grow-1 overflow-hidden">
					<div ref="gridRef" class="tabulator-instance h-100"></div>
				</div>
			</div>
		</div>
	</div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, nextTick } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import AppAlert from '@/components/AppAlert.vue'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()

const now = new Date()
const currentYear = now.getFullYear()
const currentMonth = String(now.getMonth() + 1).padStart(2, '0')

const yearOptions = Array.from({ length: 6 }, (_, i) => String(currentYear - i))
const monthOptions = Array.from({ length: 12 }, (_, i) => String(i + 1).padStart(2, '0'))

const searchForm = reactive({
	yy: String(currentYear),
	fmm: currentMonth,
	tmm: currentMonth
})

const gridRef = ref<HTMLElement | null>(null)
let grid: Tabulator | null = null

const periodGubun = computed(() => {
	const m = parseInt(searchForm.tmm)
	if (m <= 3) return '1기 예정'
	if (m <= 6) return '1기 확정'
	if (m <= 9) return '2기 예정'
	return '2기 확정'
})

async function search() {
	try {
		const ymfr = searchForm.yy + searchForm.fmm
		const ymto = searchForm.yy + searchForm.tmm

		const res = await api.post('/hatx/HATX_170S_STR', {
			cmpycd: authStore.cmpycd,
			ymfr: ymfr,
			ymto: ymto
		})

		grid?.setData(res.data || [])
		vAlert('조회되었습니다.')
	} catch (e) { vAlertError('조회 중 오류 발생') }
}

const initialize = () => {
	searchForm.yy = String(currentYear)
	searchForm.fmm = currentMonth
	searchForm.tmm = currentMonth
	grid?.clearData()
}

const print = () => {
	const params = new URLSearchParams({
		YY: searchForm.yy, fmm: searchForm.fmm, tmm: searchForm.tmm, PRTGU: 'Print'
	}).toString()
	window.open(`/hatx/HATX_170P?${params}`, 'TaxReportPrint', 'width=1200,height=800,scrollbars=yes')
}

const initGrid = () => {
	grid = new Tabulator(gridRef.value!, {
		layout: "fitColumns",
		height: "100%",
		columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
		columns: [
			{ title: "사업장 주소", field: "address", width: 350, cssClass: "small" },
			{
				title: "사업자번호", field: "saupno", width: 150, hozAlign: "center",
				formatter: (cell) => {
					const v = cell.getValue() || ''
					return v.length === 10 ? `${v.slice(0,3)}-${v.slice(3,5)}-${v.slice(5)}` : v
				}
			},
			{
				title: "매출세액",
				columns: [
					{
						title: "과세표준", field: "outamt", width: 150, hozAlign: "right",
						formatter: "money", formatterParams: { precision: 0 },
						bottomCalc: "sum", bottomCalcFormatter: "money"
					},
					{
						title: "세액", field: "outtax", width: 150, hozAlign: "right",
						formatter: "money", formatterParams: { precision: 0 },
						bottomCalc: "sum", bottomCalcFormatter: "money"
					},
					{
						title: "영세율", field: "outyamt", width: 150, hozAlign: "right",
						formatter: "money", formatterParams: { precision: 0 },
						bottomCalc: "sum", bottomCalcFormatter: "money"
					}
				]
			},
			{
				title: "매입세액",
				columns: [
					{
						title: "과세표준", field: "inamt", width: 150, hozAlign: "right",
						formatter: "money", formatterParams: { precision: 0 },
						bottomCalc: "sum", bottomCalcFormatter: "money"
					},
					{
						title: "세액", field: "intax", width: 150, hozAlign: "right",
						formatter: "money", formatterParams: { precision: 0 },
						bottomCalc: "sum", bottomCalcFormatter: "money"
					},
					{
						title: "의제등", field: "inyamt", width: 150, hozAlign: "right",
						formatter: "money", formatterParams: { precision: 0 },
						bottomCalc: "sum", bottomCalcFormatter: "money"
					}
				]
			},
			{
				title: "납부(환급)세액", field: "napbuamt", width: 150, hozAlign: "right",
				formatter: "money", formatterParams: { precision: 0 },
				cssClass: "text-primary fw-bold",
				bottomCalc: "sum", bottomCalcFormatter: "money"
			}
		]
	})
}

onMounted(() => {
	nextTick(() => {
		initGrid()
		search()
	})
})
</script>

<style scoped>
:deep(.tabulator-cell) { border-right: 1px solid #dee2e6 !important; font-size: 12px; }
:deep(.tabulator-header .tabulator-col) { border-right: 1px solid #dee2e6 !important; background-color: #f8f9fa !important; font-size: 12px; }
:deep(.tabulator-footer) { background-color: #e9ecef !important; font-weight: bold; }
.main-content-wrapper { min-height: 0; }
</style>
