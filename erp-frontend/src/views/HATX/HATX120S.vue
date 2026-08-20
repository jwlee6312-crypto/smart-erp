<!--	=============================================================
	프로그램명 : 계산서합계표
	작성일자	: 2025.02.24
	작성자    : AI Assistant
	설명        : 면세 계산서의 매입/매출 합계 및 거래처별 집계 현황 조회
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container">
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-file-earmark-bar-graph me-2 text-primary" style="font-size: 18px;"></i>
				재무관리<i class="bi bi-chevron-right mx-1 small opacity-50"></i>
				<span class="text-primary fw-bolder">계산서합계표 (HATX120S)</span>
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

			<!-- [그리드1] 계산서 총괄 요약 그리드 -->
			<div class="card border shadow-sm flex-shrink-0 overflow-hidden d-flex flex-column bg-white">
				<div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark">
					<i class="bi bi-grid-3x3-gap-fill me-1 text-primary"></i> 계산서 총괄 합계
				</div>
				<div class="card-body p-0 overflow-hidden">
					<div ref="summaryGridRef" class="tabulator-instance"></div>
				</div>
			</div>

			<!-- [그리드2] 거래처별 상세 목록 그리드 -->
			<div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white">
				<div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark">
					<i class="bi bi-list-ul me-1 text-primary"></i> 거래처별 집계 상세
				</div>
				<div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
					<div ref="mainGridRef" class="tabulator-instance flex-grow-1"></div>
				</div>
			</div>
		</div>
	</div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, watch } from 'vue'
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

const titleGbn = computed(() => searchForm.gubun === '100' ? '입' : '출')

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

		// 1. 합계 정보 조회 (iogbn: 41 for Purchase Sum, 31 for Sales Sum)
		const iogbnSum = searchForm.gubun === '100' ? '41' : '31'
		const resSum = await api.post('/hatx/HATX_110S_STR', {
			cmpycd: authStore.cmpycd,
			iogbn: iogbnSum,
			taxunit: searchForm.taxunit,
			ymfr: ymfr,
			ymto: ymto
		})

		const rawSum = resSum.data?.[0] || {}
		let sumData: any[] = []

		if (searchForm.gubun === '200') {
			sumData = [
				{ category: '합계', count: rawSum.col0, sheets: rawSum.col1, amount: rawSum.col2 },
				{ category: '사업자등록번호 발행분', count: rawSum.col3, sheets: rawSum.col4, amount: rawSum.col5 },
				{ category: '주민등록번호 발행분', count: rawSum.col6, sheets: rawSum.col7, amount: rawSum.col8 }
			]
		} else {
			sumData = [
				{ category: '사업자등록번호 발행분', count: rawSum.col0, sheets: rawSum.col1, amount: rawSum.col2 }
			]
		}
		summaryGrid?.setData(sumData)

		// 2. 상세 목록 조회 (iogbn: 42 for Purchase List, 32 for Sales List)
		const iogbnList = searchForm.gubun === '100' ? '42' : '32'
		const resList = await api.post('/hatx/HATX_110S_STR', {
			cmpycd: authStore.cmpycd,
			iogbn: iogbnList,
			taxunit: searchForm.taxunit,
			ymfr: ymfr,
			ymto: ymto
		})

		const listData = (resList.data || []).map((row: any) => ({
			custno: row.col0,
			custnm: row.col1,
			custkind: row.col2,
			custtype: row.col3,
			address: row.col4,
			sheets: Number(row.col5 || 0),
			amt: Number(row.col6 || 0)
		}))
		mainGrid?.setData(listData)

		vAlert('조회되었습니다.')
	} catch (e) { vAlertError('조회 중 오류 발생') }
}

const print = () => {
	const params = `taxunit=${searchForm.taxunit}&GUBUN=${searchForm.gubun}&YY=${searchForm.yy}&FMM=${searchForm.fmm}&TMM=${searchForm.tmm}&PRTGU=1`
	window.open(`/hatx/HATX_120P?${params}`, 'InvoiceSummaryPrint', 'width=1000,height=800,scrollbars=yes')
}

function updateGridTitles() {
	if (summaryGrid) {
		summaryGrid.updateColumnDefinition("count", { title: `매${titleGbn.value}처수` })
		summaryGrid.updateColumnDefinition("amount", { title: `매${titleGbn.value}금액` })
	}
	if (mainGrid) {
		mainGrid.updateColumnDefinition("amt", { title: `매${titleGbn.value}금액` })
	}
}

watch(() => searchForm.gubun, () => {
	updateGridTitles()
})

onMounted(() => {
	fetchOptions()

	if (summaryGridRef.value) {
		summaryGrid = new Tabulator(summaryGridRef.value, {
			layout: 'fitColumns',
			height: 'auto',
			columnDefaults: { headerSort: false, vertAlign: "middle" },
			columns: [
				{ title: "구분", field: "category", widthGrow: 1, hozAlign: "center", cssClass: "bg-light fw-bold" },
				{ title: `매입처수`, field: "count", width: 200, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
				{ title: "매수", field: "sheets", width: 200, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
				{ title: `매입금액`, field: "amount", width: 300, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } }
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
				{ title: "사업자번호", field: "custno", width: 200, hozAlign: "center" },
				{ title: "상호", field: "custnm", width: 300, cssClass: "fw-bold text-primary" },
				{ title: "업태", field: "custkind", width: 200 },
				{ title: "종목", field: "custtype", width: 200 },
				{ title: "주소", field: "address", widthGrow: 1 },
				{ title: "매수", field: "sheets", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
				{ title: `매입금액`, field: "amt", width: 200, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } }
			]
		})
	}
	updateGridTitles()
})
</script>

<style scoped>
:deep(.tabulator-cell) { border-right: 1px solid #dee2e6 !important; font-size: 12px; }
:deep(.tabulator-header .tabulator-col) { border-right: 1px solid #dee2e6 !important; background-color: #f8f9fa !important; font-size: 12px; }
</style>
