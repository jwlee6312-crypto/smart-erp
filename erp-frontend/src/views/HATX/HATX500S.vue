<!--	=============================================================
	프로그램명 : 접대비명세서
	작성일자	: 2025.02.24
	작성자    : AI Assistant
	설명        : 유형별/기간별 접대비 발생 내역 및 전표 연계 현황 조회 (HSOD100U 표준 UI 적용)
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container">
		<!-- [Header] 액션 바 -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-briefcase-fill me-2 text-primary" style="font-size: 18px;"></i>
				세무관리<i class="bi bi-chevron-right mx-1 small opacity-50"></i>
				<span class="text-primary fw-bolder">접대비명세서 (HATX500S)</span>
			</div>
			<div class="btn-group-erp d-flex gap-1">
				<button class="btn-erp btn-init" @click="initialize">초기화</button>
				<button class="btn-erp btn-search" @click="search">조회</button>
				<button class="btn-erp btn-print" @click="print">인쇄</button>
			</div>
		</div>

		<!-- [Search] 검색 조건 영역 -->
		<div class="p-2 pb-0 flex-shrink-0">
			<div class="card border shadow-sm bg-white overflow-hidden">
				<div class="card-body p-0">
					<table class="erp-table-dense w-100">
						<colgroup>
							<col style="width: 80px;" /><col style="width: 320px;" />
							<col style="width: 80px;" /><col />
						</colgroup>
						<tbody>
							<tr>
								<th class="bg-light text-center">유 형</th>
								<td>
									<div class="d-flex align-items-center gap-1 px-1">
										<select v-model="searchForm.gubun1" class="form-select form-select-sm" style="width: 140px;">
											<option v-for="opt in typeOptions" :key="opt.code" :value="opt.code">{{ opt.name }}</option>
										</select>
										<span>~</span>
										<select v-model="searchForm.gubun2" class="form-select form-select-sm" style="width: 140px;">
											<option v-for="opt in typeOptions" :key="opt.code" :value="opt.code">{{ opt.name }}</option>
										</select>
									</div>
								</td>
								<th class="bg-light text-center">접대일</th>
								<td>
									<div class="d-flex align-items-center gap-1 px-1">
										<input type="date" v-model="searchForm.stdymd" class="form-control form-control-sm" style="width: 140px;" />
										<span>~</span>
										<input type="date" v-model="searchForm.endymd" class="form-control form-control-sm" style="width: 140px;" />
									</div>
								</td>
							</tr>
						</tbody>
					</table>
				</div>
			</div>
		</div>

		<!-- [Grid] 데이터 그리드 영역 -->
		<div class="flex-grow-1 overflow-auto p-2 d-flex flex-column bg-light main-content-wrapper">
			<div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white">
				<div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
					<span class="fw-bold small text-dark"><i class="bi bi-list-ul me-1 text-primary"></i> 접대비 발생 명세</span>
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
const lastDay = new Date(currentYear, now.getMonth() + 1, 0).getDate()

const typeOptions = ref<any[]>([])

const searchForm = reactive({
	gubun1: '',
	gubun2: '',
	stdymd: `${currentYear}-${currentMonth}-01`,
	endymd: `${currentYear}-${currentMonth}-${String(lastDay).padStart(2, '0')}`
})

const gridRef = ref<HTMLElement | null>(null)
let grid: Tabulator | null = null
const gridDataCount = ref(0)

const fetchOptions = async () => {
	try {
		// 유형 옵션 로드 (E0, 230)
		const res = await api.post('/ha00/HA00_00P_STR', { gubun: 'E0', code1: ' ', code2: '230' })
		typeOptions.value = (res.data || []).map((i: any) => ({ code: i.col0, name: i.col1 }))

		if (typeOptions.value.length > 0) {
			searchForm.gubun1 = typeOptions.value[0].code
			searchForm.gubun2 = typeOptions.value[typeOptions.value.length - 1].code
		}
	} catch (e) { console.error('옵션 로드 실패', e) }
}

async function search() {
	if (searchForm.gubun1 > searchForm.gubun2) {
		return vAlertError('유형 범위를 확인하세요 (시작 > 종료).')
	}

	try {
		const res = await api.post('/hatx/HATX_500S_STR', {
			cmpycd: authStore.cmpycd,
			gubun1: searchForm.gubun1,
			gubun2: searchForm.gubun2,
			stdymd: searchForm.stdymd.replace(/-/g, ''),
			endymd: searchForm.endymd.replace(/-/g, '')
		})

		const list = res.data || []
		grid?.setData(list)
		gridDataCount.value = list.length
		vAlert('조회되었습니다.')
	} catch (e) { vAlertError('조회 중 오류 발생') }
}

const initialize = () => {
	searchForm.stdymd = `${currentYear}-${currentMonth}-01`
	searchForm.endymd = `${currentYear}-${currentMonth}-${String(lastDay).padStart(2, '0')}`
	if (typeOptions.value.length > 0) {
		searchForm.gubun1 = typeOptions.value[0].code
		searchForm.gubun2 = typeOptions.value[typeOptions.value.length - 1].code
	}
	grid?.clearData()
	gridDataCount.value = 0
}

const print = () => {
	const params = new URLSearchParams({
		GUBUN1: searchForm.gubun1,
		GUBUN2: searchForm.gubun2,
		STDYMD: searchForm.stdymd,
		ENDYMD: searchForm.endymd,
		PRTGU: 'Print'
	}).toString()
	window.open(`/hatx/HATX_500P?${params}`, 'EntertainmentReportPrint', 'width=1000,height=800,scrollbars=yes')
}

const initGrid = () => {
	grid = new Tabulator(gridRef.value!, {
		layout: "fitColumns",
		height: "100%",
		placeholder: "데이터가 존재하지 않습니다.",
		columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
		columns: [
			{
				title: "접대일", field: "srvymd", width: 100, hozAlign: "center",
				formatter: (cell) => {
					const v = cell.getValue() || ''
					return v.length === 8 ? `${v.slice(0,4)}-${v.slice(4,6)}-${v.slice(6,8)}` : v
				}
			},
			{ title: "사업자번호", field: "custno", width: 120, hozAlign: "center" },
			{ title: "유형", field: "typenm", width: 100, hozAlign: "center" },
			{ title: "상호", field: "custnm", width: 150 },
			{ title: "접대상대", field: "sname", width: 120 },
			{ title: "접대목적", field: "sobjct", widthGrow: 1.5 },
			{ title: "적요", field: "remark", widthGrow: 1.5 },
			{ title: "부서", field: "deptnm", width: 100, hozAlign: "center" },
			{
				title: "접대비", field: "srvamt", width: 110, hozAlign: "right",
				formatter: "money", formatterParams: { precision: 0 },
				bottomCalc: "sum", bottomCalcFormatter: "money"
			},
			{
				title: "봉사료", field: "bongamt", width: 100, hozAlign: "right",
				formatter: "money", formatterParams: { precision: 0 },
				bottomCalc: "sum", bottomCalcFormatter: "money"
			},
			{
				title: "합계", field: "totamt", width: 120, hozAlign: "right",
				formatter: "money", formatterParams: { precision: 0 },
				cssClass: "text-primary fw-bold",
				bottomCalc: "sum", bottomCalcFormatter: "money"
			},
			{
				title: "전표번호", field: "slipno", width: 140, hozAlign: "center",
				formatter: (cell) => {
					const data = cell.getRow().getData()
					return data.slipymd && data.slipno ? `${data.slipymd}-${data.slipno}` : ''
				}
			}
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
:deep(.tabulator-cell) { border-right: 1px solid #dee2e6 !important; font-size: 12px; }
:deep(.tabulator-header .tabulator-col) { border-right: 1px solid #dee2e6 !important; background-color: #f8f9fa !important; font-size: 12px; }
:deep(.tabulator-footer) { background-color: #e9ecef !important; font-weight: bold; }
.main-content-wrapper { min-height: 0; }
</style>
