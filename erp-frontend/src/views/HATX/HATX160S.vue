<!--	=============================================================
	프로그램명 : 공제받지 못할 매입세액 명세서
	작성일자	: 2025.02.24
	작성자    : AI Assistant
	설명        : 부가가치세 신고 시 공제받지 못할 매입세액의 사유별 집계 및 명세 (HSOD100U 표준 UI 적용)
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container">
		<!-- [Header] 액션 바 -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-file-earmark-x me-2 text-primary" style="font-size: 18px;"></i>
				세무관리<i class="bi bi-chevron-right mx-1 small opacity-50"></i>
				<span class="text-primary fw-bolder">공제받지 못할 매입세액 명세서 (HATX160S)</span>
			</div>
			<div class="btn-group-erp d-flex gap-1">
				<button class="btn-erp btn-init" @click="initialize">
					초기화
				</button>
				<button class="btn-erp btn-search" @click="search">
					<i class="bi bi-search"></i> 조회
				</button>
			</div>
		</div>

		<!-- [Search] 검색 조건 영역 -->
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
		<div class="flex-grow-1 overflow-auto p-2 d-flex flex-column gap-3 bg-light main-content-wrapper">

			<!-- 1. 인적사항 카드 -->
			<div class="card border shadow-sm flex-shrink-0 bg-white">
				<div class="card-header bg-white py-1 px-3 border-bottom">
					<span class="fw-bold small text-dark"><i class="bi bi-person-vcard me-1 text-primary"></i> 1. 인적사항</span>
				</div>
				<div class="card-body p-0">
					<table class="erp-table-dense w-100">
						<colgroup>
							<col style="width: 120px;" /><col style="width: 200px;" />
							<col style="width: 120px;" /><col style="width: 150px;" />
							<col style="width: 120px;" /><col />
						</colgroup>
						<tbody>
							<tr>
								<th class="bg-light text-center">상호(법인명)</th>
								<td class="px-2 fw-bold text-dark">{{ bizInfo.ltdnm || '-' }}</td>
								<th class="bg-light text-center">성명(대표자)</th>
								<td class="px-2">{{ bizInfo.bossnm || '-' }}</td>
								<th class="bg-light text-center">사업자등록번호</th>
								<td class="px-2">{{ formatSaupNo(bizInfo.saupno) }}</td>
							</tr>
						</tbody>
					</table>
				</div>
			</div>

			<!-- 2. 공제받지 못할 매입세액 내역 (그리드) -->
			<div class="card border shadow-sm flex-shrink-0 bg-white">
				<div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
					<span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-1 text-primary"></i> 2. 공제받지 못할 매입세액 내역</span>
				</div>
				<div class="card-body p-0 overflow-hidden">
					<div ref="mainGridRef" class="tabulator-instance"></div>
				</div>
			</div>

			<!-- 3~5. 기타 명세 (아코디언 형태 또는 단순 섹션) -->
			<div class="card border shadow-sm flex-shrink-0 bg-white">
				<div class="card-header bg-white py-1 px-3 border-bottom">
					<span class="fw-bold small text-dark"><i class="bi bi-calculator me-1 text-primary"></i> 3. 공제매입세액 안분계산 ~ 5. 재계산 내역</span>
				</div>
				<div class="card-body p-3 text-center text-muted small">
					<i class="bi bi-info-circle me-1"></i> 현재 3~5번 항목은 확정 신고 기간 데이터에 한해 활성화됩니다.
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

const taxUnitOptions = ref<any[]>([])
const yearOptions = Array.from({ length: 6 }, (_, i) => String(currentYear - i))
const monthOptions = Array.from({ length: 12 }, (_, i) => String(i + 1).padStart(2, '0'))

const searchForm = reactive({
	taxunit: '000',
	yy: String(currentYear),
	fmm: currentMonth,
	tmm: currentMonth
})

const bizInfo = reactive({ ltdnm: '', bossnm: '', saupno: '' })
const mainGridRef = ref<HTMLElement | null>(null)
let mainGrid: Tabulator | null = null

const periodGubun = computed(() => {
	const m = parseInt(searchForm.tmm)
	if (m <= 3) return '1기 예정'
	if (m <= 6) return '1기 확정'
	if (m <= 9) return '2기 예정'
	return '2기 확정'
})

const fetchOptions = async () => {
	try {
		const res = await api.post('/ha00/HA00_00P_STR', { gubun: 'SA', cmpycd: authStore.cmpycd })
		taxUnitOptions.value = (res.data || []).map((i: any) => ({ code: i.taxunit, name: i.unitnm }))
		if (taxUnitOptions.value.length > 0) searchForm.taxunit = taxUnitOptions.value[0].code
	} catch (e) { console.error(e) }
}

async function search() {
	try {
		const ymfr = searchForm.yy + searchForm.fmm
		const ymto = searchForm.yy + searchForm.tmm

		// 1. 인적사항 조회
		const bizRes = await api.post('/haba/HABA_030U_STR', {
			actkind: 'TX', cmpycd: authStore.cmpycd, unitcd: searchForm.taxunit
		})
		if (bizRes.data?.length) {
			Object.assign(bizInfo, bizRes.data[0])
		}

		// 2. 내역 조회
		const res = await api.post('/hatx/HATX_160S_STR', {
			cmpycd: authStore.cmpycd,
			taxunit: searchForm.taxunit,
			ymfr: ymfr,
			ymto: ymto
		})

		const rawList = res.data || []
		const mappedData = buildGridData(rawList)
		mainGrid?.setData(mappedData)

		vAlert('조회되었습니다.')
	} catch (e) { vAlertError('조회 실패') }
}

const buildGridData = (raw: any[]) => {
	const reasons = [
		{ code: '01', nm: '① 필요적 기재사항 누락 등' },
		{ code: '02', nm: '② 사업과 직접관련없는 지출' },
		{ code: '03', nm: '③ 비영업용소형승용차구입·유지및임차' },
		{ code: '04', nm: '④ 접대비 및 이와 유사한 비용 관련' },
		{ code: '05', nm: '⑤ 면세사업 관련' },
		{ code: '06', nm: '⑥ 토지 및 자본적지출 관련' },
		{ code: '07', nm: '⑦ 사업자등록 전 매입세액' },
		{ code: '08', nm: '⑧ 금거래계좌 미사용관련 매입세액' }
	]

	let totalCnt = 0, totalAmt = 0, totalTax = 0
	const data = reasons.map(r => {
		const row = raw.find(i => i.bgongcd === r.code) || { cnt: 0, amt: 0, tax: 0 }
		totalCnt += Number(row.cnt || 0)
		totalAmt += Number(row.amt || 0)
		totalTax += Number(row.tax || 0)
		return { reason: r.nm, cnt: row.cnt, amt: row.amt, tax: row.tax }
	})

	data.push({ reason: '⑨ 합 계', cnt: totalCnt, amt: totalAmt, tax: totalTax, isTotal: true })
	return data
}

const initialize = () => {
	searchForm.taxunit = taxUnitOptions.value.length > 0 ? taxUnitOptions.value[0].code : '000'
	searchForm.yy = String(currentYear)
	searchForm.fmm = currentMonth
	searchForm.tmm = currentMonth
	mainGrid?.clearData()
	Object.assign(bizInfo, { ltdnm: '', bossnm: '', saupno: '' })
}

const formatSaupNo = (v: string) => v && v.length === 10 ? `${v.slice(0,3)}-${v.slice(3,5)}-${v.slice(5)}` : v

const initGrid = () => {
	mainGrid = new Tabulator(mainGridRef.value!, {
		layout: "fitColumns",
		height: "auto",
		columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
		columns: [
			{ title: "매입세액 불공제 사유", field: "reason", widthGrow: 2, cssClass: "bg-light-subtle fw-bold" },
			{ title: "매수", field: "cnt", width: 100, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
			{ title: "공급가액", field: "amt", width: 180, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
			{ title: "매입세액", field: "tax", width: 180, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "text-danger fw-bold" },
			{ title: "비고", field: "remark", widthGrow: 1 }
		],
		rowFormatter: (row) => { if (row.getData().isTotal) row.getElement().classList.add("table-primary", "fw-bold") }
	})
}

onMounted(() => {
	nextTick(() => {
		initGrid()
		fetchOptions()
		search()
	})
})
</script>

<style scoped>
:deep(.tabulator-cell) { border-right: 1px solid #dee2e6 !important; font-size: 13px; }
:deep(.tabulator-header .tabulator-col) { border-right: 1px solid #dee2e6 !important; background-color: #f8f9fa !important; font-size: 13px; }
.main-content-wrapper { min-height: 0; }
</style>
