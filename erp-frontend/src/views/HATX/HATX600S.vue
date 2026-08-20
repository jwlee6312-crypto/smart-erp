<!--	=============================================================
	프로그램명: 수출실적명세서
	작성일자	: 2025.02.24
	작성자    : AI Assistant
	설명        : 사업장별 수출 실적(수출재화, 기타영세율) 상세 현황 조회 (HSOD100U 기반 UI 적용)
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container">
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-airplane-engines me-2 text-primary" style="font-size: 18px;"></i>
				세무관리<i class="bi bi-chevron-right mx-1 small opacity-50"></i>
				<span class="text-primary fw-bolder">수출실적명세서(HATX600S)</span>
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

		<!-- 상단 검색 조건 영역 -->
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
										<option v-for="opt in taxUnitOptions" :key="opt.code" :value="opt.code">{{ opt.codenm }}</option>
									</select>
								</td>
								<th class="bg-light text-center">기  간</th>
								<td>
									<div class="d-flex align-items-center gap-1 px-1">
										<select v-model="searchForm.YY" class="form-select form-select-sm" style="width: 100px;">
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

		<!-- 하단 메인 컨텐츠 영역 (상하 분할) -->
		<div class="flex-grow-1 overflow-auto p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

			<!-- 상단: 수출실적 합계 그리드 -->
			<div class="card border shadow-sm flex-shrink-0 overflow-hidden d-flex flex-column bg-white">
				<div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
					<span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-1 text-primary"></i> 수출실적 합계</span>
				</div>
				<div class="card-body p-0 overflow-hidden">
					<div ref="summaryGridRef" class="tabulator-instance"></div>
				</div>
			</div>

			<!-- 하단: 수출실적 상세 목록 그리드 -->
			<div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white">
				<div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
					<span class="fw-bold small text-dark"><i class="bi bi-list-ul me-1 text-primary"></i> 수출실적 상세 내역</span>
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
	taxunit: '000',
	YY: String(currentYear),
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
		taxUnitOptions.value = (res.data || []).map((i: any) => ({ code: i.taxunit, codenm: i.unitnm }))
        if (taxUnitOptions.value.length > 0) searchForm.taxunit = taxUnitOptions.value[0].code

	} catch (e) { console.error(e) }
}

const search = async () => {
	try {
		const ymfr = searchForm.yy + searchForm.fmm
		const ymto = searchForm.yy + searchForm.tmm

		// 1. 합계 정보 조회 (actkind: S0)
		const resSum = await api.post('/hatx/HATX_600S_STR', {
			actkind: 'S0',
			cmpycd: authStore.cmpycd,
			taxunit: searchForm.taxunit,
			ymfr: ymfr,
			ymto: ymto
		})

		const rawSum = resSum.data?.[0] || {}
		const sumData = [
			{ category: '합계', count: rawSum.cnt1, frgn_amt: rawSum.frgnamt1, won_amt: rawSum.supyamt1, remark: '' },
			{ category: '수출재화', count: rawSum.cnt2, frgn_amt: rawSum.frgnamt2, won_amt: rawSum.supyamt2, remark: '' },
			{ category: '기타영세율적용', count: rawSum.cnt3, frgn_amt: rawSum.frgnamt3, won_amt: rawSum.supyamt3, remark: '' }
		]
		summaryGrid?.setData(sumData)

		// 2. 상세 목록 조회 (actkind: S1)
		const resList = await api.post('/hatx/HATX_600S_STR', {
			actkind: 'S1',
			cmpycd: authStore.cmpycd,
			taxunit: searchForm.taxunit,
			ymfr: ymfr,
			ymto: ymto
		})

		const listData = (resList.data || []).map((row: any) => ({
			mgtno: row.mgtno,
			pubymd: row.pubymd, // 8 digits
			currcd: row.currcd,
			frgnrate: Number(row.frgnrate || 0),
			frgnamt: Number(row.frgnamt || 0),
			supyamt: Number(row.supyamt || 0)
		}))
		mainGrid?.setData(listData)

		vAlert('조회되었습니다.')
	} catch (e) { vAlertError('조회 중 오류 발생') }
}

const initialize = () => {
	searchForm.taxunit = taxUnitOptions.value.length > 0 ? taxUnitOptions.value[0].code : '000'
	searchForm.yy = String(currentYear)
	searchForm.fmm = currentMonth
	searchForm.tmm = currentMonth
	summaryGrid?.clearData()
	mainGrid?.clearData()
}

onMounted(() => {
	fetchOptions()

	if (summaryGridRef.value) {
		summaryGrid = new Tabulator(summaryGridRef.value, {
			layout: 'fitColumns',
			height: 'auto',
			columnDefaults: { headerSort: false, vertAlign: "middle" },
			columns: [
				{ title: "구분", field: "category", widthGrow: 1, hozAlign: "center", cssClass: "bg-light fw-bold" },
				{ title: "건수", field: "count", width: 120, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
				{ title: "외화금액", field: "frgn_amt", width: 200, hozAlign: "right", formatter: "money", formatterParams: { precision: 2 } },
				{ title: "원화금액", field: "won_amt", width: 200, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "fw-bold" },
				{ title: "비고", field: "remark", widthGrow: 1 }
			]
		})
	}

	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns',
			height: '100%',
			columnDefaults: { headerSort: false, vertAlign: "middle" },
			columns: [
				{ title: "일련번호", formatter: "rownum", width: 80, hozAlign: "center" },
				{ title: "수출신고번호", field: "mgtno", width: 180, hozAlign: "center", cssClass: "fw-bold" },
				{
					title: "선적일자", field: "pubymd", width: 120, hozAlign: "center",
					formatter: (cell) => { const v = cell.getValue(); return v && v.length === 8 ? `${v.slice(0,4)}-${v.slice(4,6)}-${v.slice(6,8)}` : v }
				},
				{ title: "통화코드", field: "currcd", width: 100, hozAlign: "center" },
				{ title: "환율", field: "frgnrate", width: 120, hozAlign: "right", formatter: "money", formatterParams: { precision: 4 } },
				{ title: "외화금액", field: "frgnamt", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 2 } },
				{ title: "원화금액", field: "supyamt", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "text-primary fw-bold" }
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
