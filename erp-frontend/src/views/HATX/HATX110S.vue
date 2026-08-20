<!--	=============================================================
	프로그램명 : 세금계산서합계표
	작성일자	: 2025.02.24
	작성자    : AI Assistant
	설명        : 매입/매출 세금계산서의 전자/종이 합계 및 거래처별 집계 현황 조회
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container">
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-calculator-fill me-2 text-primary" style="font-size: 18px;"></i>
				재무관리<i class="bi bi-chevron-right mx-1 small opacity-50"></i>
				<span class="text-primary fw-bolder">세금계산서합계표 (HATX110S)</span>
			</div>
			<div class="btn-group-erp d-flex gap-1">
				<button class="btn-erp btn-search" @click="search">
					<i class="bi bi-search"></i> 조회
				</button>
				<button class="btn-erp btn-print" @click="print">
					<i class="bi bi-printer"></i> 인쇄
				</button>
			</div>
		</div>

		<!-- [검색] 검색 조건 영역 -->
		<div class="p-2 pb-0 flex-shrink-0">
			<div class="card border shadow-sm bg-white overflow-hidden">
				<div class="card-body p-0">
					<table class="erp-table-dense w-100">
						<colgroup>
							<col style="width: 80px;" /><col style="width: 120px;" />
							<col style="width: 80px;" /><col style="width: 150px;" />
							<col style="width: 80px;" /><col />
						</colgroup>
						<tbody>
							<tr>
								<th class="bg-light text-center">구   분</th>
								<td>
									<select v-model="searchForm.gubun" class="form-select form-select-sm">
										<option value="100">매입처</option>
										<option value="200">매출처</option>
									</select>
								</td>
								<th class="bg-light text-center">사업장</th>
								<td>
									<select v-model="searchForm.taxunit" class="form-select form-select-sm">
										<option value="000">전체</option>
										<option v-for="opt in taxUnitOptions" :key="opt.code" :value="opt.code">{{ opt.name }}</option>
									</select>
								</td>
								<th class="bg-light text-center">기   간</th>
								<td>
									<div class="d-flex align-items-center gap-1">
										<select v-model="searchForm.yy" class="form-select form-select-sm" style="width: 90px;">
											<option v-for="y in yearOptions" :key="y" :value="y">{{ y }}년</option>
										</select>
										<select v-model="searchForm.fmm" class="form-select form-select-sm" style="width: 70px;">
											<option v-for="m in monthOptions" :key="m" :value="m">{{ m }}월</option>
										</select>
										<span>~</span>
										<select v-model="searchForm.tmm" class="form-select form-select-sm" style="width: 70px;">
											<option v-for="m in monthOptions" :key="m" :value="m">{{ m }}월</option>
										</select>
									</div>
								</td>
							</tr>
						</tbody>
					</table>
				</div>
			</div>
		</div>

		<!-- [콘텐츠] 메인 콘텐츠 영역 (상하 분할) -->
		<div class="flex-grow-1 overflow-auto p-2 d-flex flex-column gap-2 bg-light">

			<!-- [그리드1] 세금계산서 총괄 요약 그리드 -->
			<div class="card border shadow-sm flex-shrink-0 overflow-hidden d-flex flex-column bg-white">
				<div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
					<span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-1 text-primary"></i> 세금계산서 총괄 합계</span>
				</div>
				<div class="card-body p-0 overflow-hidden">
					<div ref="summaryGridRef" class="tabulator-instance"></div>
				</div>
			</div>

			<!-- [그리드2] 거래처별 상세 목록 그리드 -->
			<div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white">
				<div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
					<span class="fw-bold small text-dark"><i class="bi bi-list-ul me-1 text-primary"></i> 거래처별 집계 상세</span>
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
import AppAlert from '@/components/AppAlert.vue'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()

const now = new Date()
const currentYear = now.getFullYear()
const currentMonth = String(now.getMonth() + 1).padStart(2, '0')

const taxUnitOptions = ref<any[]>([])
const yearOptions = Array.from({ length: 6 }, (_, i) => String(currentYear - i))
const monthOptions = Array.from({ length: 12 }, (_, i) => String(i + 1).padStart(2, '0'))

const searchForm = reactive({
	gubun: '100',
	taxunit: '000',
	yy: String(currentYear),
	fmm: currentMonth,
	tmm: currentMonth
})

const summaryGridRef = ref<HTMLDivElement | null>(null)
const mainGridRef = ref<HTMLDivElement | null>(null)
let summaryGrid: Tabulator | null = null
let mainGrid: Tabulator | null = null

const fetchOptions = async () => {
	try {
        const res = await api.post('/ha00/HA00_00P_STR', { gubun: 'SA', cmpycd: authStore.cmpycd })
        taxUnitOptions.value = (res.data || []).map((i: any) => ({ code: i.taxunit, name: i.unitnm }))
        if (taxUnitOptions.value.length > 0) searchForm.taxunit = taxUnitOptions.value[0].code

	} catch (e) { console.error(e) }
}

const search = async () => {
	try {
		const ymfr = searchForm.yy + searchForm.fmm
		const ymto = searchForm.yy + searchForm.tmm

		// 1. 합계 정보 조회 (iogbn: 11 for Purchase Sum, 21 for Sales Sum)
		const iogbnSum = searchForm.gubun === '100' ? '11' : '21'
		const resSum = await api.post('/hatx/HATX_110S_STR', {
			cmpycd: authStore.cmpycd,
			iogbn: iogbnSum,
			taxunit: searchForm.taxunit,
			ymfr: ymfr,
			ymto: ymto
		})



		const rawSum = resSum.data?.[0] || {}
		const sumData = [
			{ category: '합계', detail: '전체 합계', count: rawSum.amt0, sheets: rawSum.amt1, supply: rawSum.amt2, tax: rawSum.amt3 },
			{ category: '전자세금계산서', detail: '사업자등록번호 발취분', count: rawSum.amt4, sheets: rawSum.amt5, supply: rawSum.amt6, tax: rawSum.amt7 },
			{ category: '전자세금계산서', detail: '주민등록번호 발취분', count: rawSum.amt8, sheets: rawSum.amt9, supply: rawSum.amt10, tax: rawSum.amt11 },
			{ category: '전자세금계산서', detail: '전자 소계', count: rawSum.amt12, sheets: rawSum.amt13, supply: rawSum.amt14, tax: rawSum.amt15 },
			{ category: '전자 이외', detail: '사업자등록번호 발취분', count: rawSum.amt16, sheets: rawSum.amt17, supply: rawSum.amt18, tax: rawSum.amt19 },
			{ category: '전자 이외', detail: '주민등록번호 발취분', count: rawSum.amt20, sheets: rawSum.amt21, supply: rawSum.amt22, tax: rawSum.amt23 },
			{ category: '전자 이외', detail: '이외 소계', count: rawSum.amt24, sheets: rawSum.amt25, supply: rawSum.amt26, tax: rawSum.amt27 }
		]
		summaryGrid?.setData(sumData)

		// 2. 상세 목록 조회 (iogbn: 12 for Purchase List, 22 for Sales List)
		const iogbnList = searchForm.gubun === '100' ? '12' : '22'
		const resList = await api.post('/hatx/HATX_110S_STR', {
			cmpycd: authStore.cmpycd,
			iogbn: iogbnList,
			taxunit: searchForm.taxunit,
			ymfr: ymfr,
			ymto: ymto
		})

		const listData = (resList.data || []).map((row: any) => ({
			custno: row.custno,
			custnm: row.custnm,
			custtype: row.custtype,
			sheets: Number(row.cnt || 0),
			supyamt: Number(row.supyamt || 0),
			vatamt: Number(row.vatamt || 0)
		}))
		mainGrid?.setData(listData)

		vAlert('조회되었습니다.')
	} catch (e) { vAlertError('조회 중 오류 발생') }
}

const print = () => {
	const params = `taxunit=${searchForm.taxunit}&GUBUN=${searchForm.gubun}&YY=${searchForm.yy}&FMM=${searchForm.fmm}&TMM=${searchForm.tmm}&PRTGU=1`
	window.open(`/hatx/HATX_110P?${params}`, 'TaxInvoiceSummaryPrint', 'width=1000,height=800,scrollbars=yes')
}

onMounted(() => {
	fetchOptions()

	if (summaryGridRef.value) {
		summaryGrid = new Tabulator(summaryGridRef.value, {
			layout: 'fitColumns',
			height: 'auto',
			columnDefaults: { headerSort: false, vertAlign: "middle" },
			columns: [
				{ title: "대분류", field: "category", width: 250, hozAlign: "center", cssClass: "bg-light fw-bold" },
				{ title: "상세구분", field: "detail", widthGrow: 1, hozAlign: "left" },
				{ title: "처수", field: "count", width: 120, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
				{ title: "매수", field: "sheets", width: 120, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
				{ title: "공급가액", field: "supply", width: 200, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
				{ title: "세액", field: "tax", width: 200, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } }
			]
		})
	}

	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns',
			height: '100%',
			columnDefaults: { headerSort: false, vertAlign: "middle" },
			columns: [
				{ title: "No", formatter: "rownum", width: 40, hozAlign: "center" },
				{
					title: "사업자등록번호", field: "custno", width: 150, hozAlign: "center",
					formatter: (cell) => { const v = cell.getValue(); return v && v.length === 10 ? `${v.slice(0,3)}-${v.slice(3,5)}-${v.slice(5)}` : v }
				},
				{ title: "상호", field: "custnm", widthGrow: 1, cssClass: "fw-bold text-primary" },
				{ title: "업태", field: "custkind", width: 180 },
				{ title: "종목", field: "custtype", width: 180 },
				{ title: "매수", field: "sheets", width: 100, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
				{ title: "공급가액", field: "supyamt", width: 180, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
				{ title: "세액", field: "vatamt", width: 180, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } }
			]
		})
	}
})
</script>

<style scoped>
.erp-table-dense th { min-width: 80px; }
:deep(.tabulator-cell) { border-right: 1px solid #dee2e6 !important; font-size: 12px; }
:deep(.tabulator-header .tabulator-col) { border-right: 1px solid #dee2e6 !important; background-color: #f8f9fa !important; font-size: 12px; }
</style>
