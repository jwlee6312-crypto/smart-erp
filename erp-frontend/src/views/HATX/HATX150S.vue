<!--	=============================================================
	프로그램명 : 거래처별 전자세금계산서 합계표
	작성일자	: 2025.02.24
	작성자    : AI Assistant
	설명        : 거래처별 전자세금계산서(매입/매출) 합계 및 상세 내역 조회 (HSOD100U 표준 UI 적용)
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container">
		<!-- [Header] 액션 바 -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-file-earmark-bar-graph me-2 text-primary" style="font-size: 18px;"></i>
				세무관리<i class="bi bi-chevron-right mx-1 small opacity-50"></i>
				<span class="text-primary fw-bolder">거래처별 전자세금계산서 합계표 (HATX150S)</span>
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
							<col style="width: 80px;" /><col style="width: 200px;" />
							<col style="width: 80px;" /><col />
						</colgroup>
						<tbody>
							<tr>
								<th class="bg-light text-center">구 분</th>
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

		<!-- [Grid] 메인 데이터 그리드 -->
		<div class="flex-grow-1 overflow-auto p-2 d-flex flex-column bg-light main-content-wrapper">
			<div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white">
				<div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
					<span class="fw-bold small text-dark">
						<i class="bi bi-list-check me-1 text-primary"></i>
						전자세금계산서 집계 리스트
						<span v-if="searchForm.gubun === '100'" class="badge bg-info-subtle text-info ms-2">매입</span>
						<span v-else class="badge bg-primary-subtle text-primary ms-2">매출</span>
					</span>
					<div class="small text-muted">
						총 <span class="fw-bold text-primary">{{ gridDataCount }}</span>건
					</div>
				</div>
				<div class="card-body p-0 flex-grow-1 overflow-hidden">
					<div ref="gridRef" class="tabulator-instance h-100"></div>
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

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()

const now = new Date()
const currentYear = now.getFullYear()
const currentMonth = String(now.getMonth() + 1).padStart(2, '0')

const taxUnitOptions = ref<any[]>([])
const yearOptions = Array.from({ length: 6 }, (_, i) => String(currentYear - i))
const monthOptions = Array.from({ length: 12 }, (_, i) => String(i + 1).padStart(2, '0'))

const searchForm = reactive({
	gubun: '100', // 100: 매입처, 200: 매출처
	taxunit: '000',
	yy: String(currentYear),
	fmm: currentMonth,
	tmm: currentMonth
})

const gridRef = ref<HTMLElement | null>(null)
let grid: Tabulator | null = null
const gridDataCount = ref(0)

const fetchOptions = async () => {
	try {
		const res = await api.post('/ha00/HA00_00P_STR', { gubun: 'SA', cmpycd: authStore.cmpycd })
		taxUnitOptions.value = (res.data || []).map((i: any) => ({ code: i.taxunit, name: i.unitnm }))
		if (taxUnitOptions.value.length > 0) searchForm.taxunit = '000' // 기본값 전체
	} catch (e) { console.error('사업장 옵션 로드 실패', e) }
}

async function search() {
	try {
		const ymfr = searchForm.yy + searchForm.fmm
		const ymto = searchForm.yy + searchForm.tmm

		const res = await api.post('/hatx/HATX_150S_STR', {
			cmpycd: authStore.cmpycd,
			gubun: searchForm.gubun,
			taxunit: searchForm.taxunit,
			ymfr: ymfr,
			ymto: ymto
		})

		const list = res.data || []
		grid?.setData(list)
		gridDataCount.value = list.length
		vAlert('조회되었습니다.')
	} catch (e) {
		vAlertError('조회 중 오류가 발생했습니다.')
		console.error(e)
	}
}

const initialize = () => {
	searchForm.gubun = '100'
	searchForm.taxunit = '000'
	searchForm.yy = String(currentYear)
	searchForm.fmm = currentMonth
	searchForm.tmm = currentMonth
	grid?.clearData()
	gridDataCount.value = 0
}

const initGrid = () => {
	if (!gridRef.value) return
	grid = new Tabulator(gridRef.value, {
		layout: "fitColumns",
		height: "100%",
		placeholder: "데이터가 존재하지 않습니다.",
		columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
		columns: [
			{ title: "거래처 상호명", field: "custnm", widthGrow: 1, cssClass: "fw-bold" },
			{
				title: "사업자번호", field: "custno", width: 180, hozAlign: "center",
				formatter: (cell) => {
					const v = cell.getValue() || ''
					return v.length === 10 ? `${v.slice(0,3)}-${v.slice(3,5)}-${v.slice(5)}` : v
				}
			},
			{ title: "매수", field: "cnt", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
			{ title: "공급가액", field: "supyamt", width: 180, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "text-primary fw-bold" },
			{ title: "세액", field: "vatamt", width: 180, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } }
		]
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
