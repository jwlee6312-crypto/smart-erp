<!--	=============================================================
	프로그램명 : 신용카드매출전표수취명세서
	작성일자	: 2025.02.24
	작성자    : AI Assistant
	설명        : 신용카드 및 현금영수증 매입 세액 공제를 위한 수취 명세 현황 조회
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container">
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-credit-card-2-front me-2 text-primary" style="font-size: 18px;"></i>
				재무관리<i class="bi bi-chevron-right mx-1 small opacity-50"></i>
				<span class="text-primary fw-bolder">신용카드매출전표수취명세서(HATX130S)</span>
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

		<!-- [검색] 검색 조건 영역 -->
		<div class="p-2 pb-0 flex-shrink-0">
			<div class="card border shadow-sm bg-white overflow-hidden">
				<div class="card-body p-0">
					<table class="erp-table-dense w-100">
						<colgroup>
							<col style="width: 80px;" /><col style="width: 150px;" />
							<col style="width: 80px;" /><col />
						</colgroup>
						<tbody>
							<tr>
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
		<div class="flex-grow-1 overflow-auto p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

			<!-- [그리드1] 수취 명세 총괄 합계 -->
			<div class="card border shadow-sm flex-shrink-0 overflow-hidden d-flex flex-column bg-white">
				<div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
					<span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-1 text-primary"></i> 수취 명세 총괄 합계</span>
					<span class="text-muted small" style="font-size: 11px;">* 구분을 클릭하면 거래 상세 내역이 표시됩니다.</span>
				</div>
				<div class="card-body p-0 overflow-hidden">
					<div ref="summaryGridRef" class="tabulator-instance"></div>
				</div>
			</div>

			<!-- [그리드2] 카드회원/가맹점별 집계 상세 -->
			<div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white">
				<div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
					<span class="fw-bold small text-dark"><i class="bi bi-list-ul me-1 text-primary"></i> 카드회원/가맹점별 집계 상세</span>
				</div>
				<div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
					<div ref="mainGridRef" class="tabulator-instance flex-grow-1"></div>
				</div>
			</div>
		</div>
	</div>

	<!-- [팝업] 거래 상세 내역 팝업 모달 -->
	<div class="modal fade" :class="{ show: showDetailModal }" :style="{ display: showDetailModal ? 'block' : 'none' }" tabindex="-1" @click.self="showDetailModal = false">
		<div class="modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable">
			<div class="modal-content shadow-lg border-0">
				<div class="modal-header bg-primary text-white py-2">
					<h6 class="modal-title fw-bold"><i class="bi bi-search me-2"></i> [{{ detailModalTitle }}] 거래 상세 내역 현황</h6>
					<button type="button" class="btn-close btn-close-white" @click="showDetailModal = false"></button>
				</div>
				<div class="modal-body p-0" style="height: 600px;">
					<div ref="detailGridRef" class="tabulator-instance h-100"></div>
				</div>
				<div class="modal-footer py-1 bg-light">
					<button type="button" class="btn btn-sm btn-secondary" @click="showDetailModal = false">닫기</button>
				</div>
			</div>
		</div>
	</div>
	<div v-if="showDetailModal" class="modal-backdrop fade show"></div>

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

const now = new Date()
const currentYear = now.getFullYear()
const currentMonth = String(now.getMonth() + 1).padStart(2, '0')

const taxUnitOptions = ref<any[]>([])
const yearOptions = Array.from({ length: 6 }, (_, i) => String(currentYear - i))
const monthOptions = Array.from({ length: 12 }, (_, i) => String(i + 1).padStart(2, '0'))

const searchForm = reactive({
	taxunit: '000',
	yy: String(currentYear),
	fmm: currentMonth,
	tmm: currentMonth
})

const summaryGridRef = ref<HTMLDivElement | null>(null)
const mainGridRef = ref<HTMLDivElement | null>(null)
const detailGridRef = ref<HTMLDivElement | null>(null)
let summaryGrid: Tabulator | null = null
let mainGrid: Tabulator | null = null
let detailGrid: Tabulator | null = null

const showDetailModal = ref(false)
const detailModalTitle = ref('')

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

		// 1. 합계 정보 조회 (actkind: S0)
		const resSum = await api.post('/hatx/HATX_130S_STR', {
			actkind: 'S0',
			cmpycd: authStore.cmpycd,
			taxunit: searchForm.taxunit,
			ymfr: ymfr,
			ymto: ymto
		})

		const d = resSum.data?.[0] || {}
		const sumData = [
			{ category: '합계', count: d.col0, supply: d.col1, tax: d.col2, taxtype: '05' },
			{ category: '현금영수증', count: d.col3, supply: d.col4, tax: d.col5, taxtype: '051' },
			{ category: '화물운전자복지카드', count: d.col6, supply: d.col7, tax: d.col8, taxtype: '052' },
			{ category: '사업용신용카드', count: d.col12, supply: d.col13, tax: d.col14, taxtype: '053' },
			{ category: '기타신용카드 등', count: d.col9, supply: d.col10, tax: d.col11, taxtype: '050' }
		]
		summaryGrid?.setData(sumData)

		// 2. 카드/가맹점별 목록 조회 (actkind: S1)
		const resList = await api.post('/hatx/HATX_130S_STR', {
			actkind: 'S1',
			cmpycd: authStore.cmpycd,
			taxunit: searchForm.taxunit,
			ymfr: ymfr,
			ymto: ymto
		})

		const listData = (resList.data || []).map((row: any) => ({
			cardno: row.mgtno,
			custno: row.custno,
			custnm: row.custnm,
			count: Number(row.trncnt || 0),
			supply: Number(row.spyamt || 0),
			tax: Number(row.vatamt || 0)
		}))
		mainGrid?.setData(listData)

		vAlert('조회되었습니다.')
	} catch (e) { vAlertError('조회 중 오류 발생') }
}

const openDetailView = async (data: any) => {
	detailModalTitle.value = data.category
	showDetailModal.value = true

	await nextTick()
	initDetailGrid()

	try {
		const ymfr = searchForm.yy + searchForm.fmm
		const ymto = searchForm.yy + searchForm.tmm
		const res = await api.post('/hatx/HATX_130S_STR', {
			actkind: 'S2',
			cmpycd: authStore.cmpycd,
			taxunit: searchForm.taxunit,
			ymfr: ymfr,
			ymto: ymto,
			taxtype: data.taxtype
		})

		const list = (res.data || []).map((i: any) => ({
			ymd: i.cltymd,
			custno: i.custno,
			custnm: i.custnm,
			supply: i.supyamt,
			tax: i.vatamt,
			cardno: i.mgtno,
			typenm: i.taxtypenm
		}))
		detailGrid?.setData(list)
	} catch (e) { vAlertError('상세 내역 조회 실패') }
}

const print = () => {
	const params = `taxunit=${searchForm.taxunit}&YY=${searchForm.yy}&FMM=${searchForm.fmm}&TMM=${searchForm.tmm}&PRTGU=1`
	window.open(`/hatx/HATX_130P?${params}`, 'CreditCardReceiptPrint', 'width=1000,height=800,scrollbars=yes')
}

const initialize = () => {
	searchForm.taxunit = taxUnitOptions.value.length > 0 ? taxUnitOptions.value[0].code : '000'
	searchForm.yy = String(currentYear)
	searchForm.fmm = currentMonth
	searchForm.tmm = currentMonth
	summaryGrid?.clearData()
	mainGrid?.clearData()
}

const initDetailGrid = () => {
	if (detailGrid || !detailGridRef.value) return
	detailGrid = new Tabulator(detailGridRef.value, {
		layout: 'fitColumns',
		height: '100%',
		columnDefaults: { headerSort: false, vertAlign: "middle" },
		columns: [
			{
				title: "거래일자", field: "ymd", width: 110, hozAlign: "center",
				formatter: (cell) => { const v = cell.getValue(); return v && v.length === 8 ? `${v.slice(0,4)}-${v.slice(4,6)}-${v.slice(6,8)}` : v }
			},
			{
				title: "사업자번호", field: "custno", width: 150, hozAlign: "center",
				formatter: (cell) => { const v = cell.getValue(); return v && v.length === 10 ? `${v.slice(0,3)}-${v.slice(3,5)}-${v.slice(5)}` : v }
			},
			{ title: "상호", field: "custnm", widthGrow: 1 },
			{ title: "공급가액", field: "supply", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
			{ title: "세액", field: "tax", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
			{ title: "카드(확인)번호", field: "cardno", width: 200, hozAlign: "center" },
			{ title: "구분", field: "typenm", width: 120, hozAlign: "center" }
		]
	})
}

onMounted(() => {
	fetchOptions()

	if (summaryGridRef.value) {
		summaryGrid = new Tabulator(summaryGridRef.value, {
			layout: 'fitColumns',
			height: 'auto',
			columnDefaults: { headerSort: false, vertAlign: "middle" },
			columns: [
				{
					title: "구분", field: "category", widthGrow: 1, hozAlign: "center", cssClass: "bg-light fw-bold",
					formatter: (cell) => `<span class="text-primary text-decoration-underline" style="cursor:pointer;">${cell.getValue()}</span>`,
					cellClick: (e, cell) => openDetailView(cell.getData())
				},
				{ title: "거래건수", field: "count", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
				{ title: "공급가액", field: "supply", width: 200, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
				{ title: "세액", field: "tax", width: 180, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } }
			]
		})
	}

	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns',
			height: '100%',
			columnDefaults: { headerSort: false, vertAlign: "middle" },
			columns: [
				{ title: "련번호", formatter: "rownum", width: 60, hozAlign: "center" },
				{ title: "카드회원번호", field: "cardno", width: 150, hozAlign: "center" },
				{
					title: "사업자등록번호", field: "custno", width: 130, hozAlign: "center",
					formatter: (cell) => { const v = cell.getValue(); return v && v.length === 10 ? `${v.slice(0,3)}-${v.slice(3,5)}-${v.slice(5)}` : v }
				},
				{ title: "공급자(가맹점) 상호", field: "custnm", widthGrow: 1, cssClass: "fw-bold text-dark" },
				{ title: "거래건수", field: "count", width: 100, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
				{ title: "공급가액", field: "supply", width: 130, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
				{ title: "세액", field: "tax", width: 110, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "text-primary fw-bold" }
			]
		})
	}
})
</script>

<style scoped>
:deep(.tabulator-cell) { border-right: 1px solid #dee2e6 !important; font-size: 12px; }
:deep(.tabulator-header .tabulator-col) { border-right: 1px solid #dee2e6 !important; background-color: #f8f9fa !important; font-size: 12px; }
.main-content-wrapper { min-height: 0; }
</style>
