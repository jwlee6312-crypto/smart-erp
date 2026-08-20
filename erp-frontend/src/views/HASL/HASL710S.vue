<!--
	=============================================================
	프로그램명: 부서별 비용명세서 (HASL710S)
	작성일자	: 2025.03.14
	작성자    : AI Assistant
	설명        : 부서별 월별 비용 발생 내역을 12개월간 조회
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container d-flex flex-column h-100 bg-white">
		<!-- [헤더] 상단 액션 바 -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-diagram-3 me-2 text-primary" style="font-size: 18px;"></i>
				회계관리 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				<span class="text-primary fw-bolder">부서별 비용명세서 (HASL710S)</span>
			</div>
			<div class="btn-group-erp d-flex gap-1 pe-3">
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

		<!-- [조회] 검색 조건 영역 -->
		<div class="p-2 pb-0 flex-shrink-0 bg-light">
			<div class="card border shadow-sm bg-white overflow-hidden">
				<div class="card-body p-2">
					<div class="d-flex align-items-center flex-wrap gap-3 small">
						<!-- 부서 -->
						<div class="d-flex align-items-center">
							<span class="erp-label"><i class="bi bi-dot"></i>부&nbsp;&nbsp;&nbsp;&nbsp;서</span>
							<div class="input-group input-group-sm" style="width: 250px;">
								<input v-model="searchForm.deptcd" type="text" class="form-control text-center bg-light fw-bold" style="max-width: 60px;" readonly />
								<input v-model="searchForm.deptnm" type="text" class="form-control" @keydown.enter="openHelp('DEPT')" placeholder="부서명 입력" />
								<button class="btn btn-outline-secondary px-2" @click="openHelp('DEPT')"><i class="bi bi-search"></i></button>
							</div>
						</div>
						<!-- 기준년월 -->
						<div class="d-flex align-items-center">
							<span class="erp-label"><i class="bi bi-dot"></i>기준년월</span>
							<div class="d-flex align-items-center gap-1">
								<select v-model="searchForm.yyyy" class="form-select form-select-sm" style="width: 90px;">
									<option v-for="year in yearOptions" :key="year" :value="year">{{ year }}년</option>
								</select>
								<select v-model="searchForm.mm" class="form-select form-select-sm" style="width: 70px;">
									<option v-for="month in monthOptions" :key="month" :value="month">{{ month }}</option>
								</select>
								<span class="small fw-bold ms-1">현재</span>
							</div>
						</div>
					</div>
				</div>
			</div>
		</div>

		<!-- [그리드] 데이터 영역 -->
		<div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column bg-light">
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
import { useAlerts } from '@/composables/useAlerts'
import AppAlert from '@/components/AppAlert.vue'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import Modal from '@/components/Modal.vue'
import type { ModalProps } from '@/types/modal'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()

const now = new Date()
const currentYear = now.getFullYear()
const currentMonth = String(now.getMonth() + 1).padStart(2, '0')

// 검색 조건
const searchForm = reactive({
	deptcd: '',
	deptnm: '',
	yyyy: String(currentYear),
    mm: currentMonth
})

const yearOptions = Array.from({ length: 6 }, (_, i) => String(currentYear - i))
const monthOptions = Array.from({ length: 12 }, (_, i) => String(i + 1).padStart(2, '0'))

const mainGridRef = ref<HTMLDivElement | null>(null)
let mainGrid: Tabulator | null = null

const monthHeaders = ref<string[]>([])



const fetchMonthHeaders = async () => {
	try {
		const res = await api.post('/hasl/HA00_150S_STR', {
			cmpycd: authStore.cmpycd,
			yyyymm: `${searchForm.yyyy}${searchForm.mm}`
		})
		if (res.data && res.data.length > 0) {
            const row = res.data[0]
			monthHeaders.value = Array.from({ length: 12 }, (_, i) => {
				const val = row['col' + i]
				return val ? String(val).substring(4, 6) + '월' : `${i + 1}월`
			})
		} else {
			monthHeaders.value = monthOptions.map(m => `${m}월`)
		}
	} catch (e) {
		monthHeaders.value = monthOptions.map(m => `${m}월`)
	}
}

const search = async () => {
	if (!searchForm.deptcd) {
		vAlertError('부서를 선택해 주십시오.')
		return
	}

	try {
		await fetchMonthHeaders()

		const res = await api.post('/hasl/HASL_710S_STR', {
			cmpycd: authStore.cmpycd,
			deptcd: searchForm.deptcd,
			yyyymm: `${searchForm.yyyy}${searchForm.mm}`
		})

		const rawData = res.data || []
		const processedData: any[] = []

		if (rawData.length > 0) {
			let i = 0
			let totalSum = Array(13).fill(0)

			while (i < rawData.length) {
				const groupKey = String(rawData[i].col2 || '').substring(0, 2)
				let groupSum = Array(13).fill(0)

				while (i < rawData.length && String(rawData[i].col2 || '').substring(0, 2) === groupKey) {
					const row = rawData[i]
					let rowMonthlySum = 0
					const monthlyValues = []

					for (let m = 1; m <= 12; m++) {
						const val = Number(row['col' + (m + 2)] || 0)
						monthlyValues.push(val)
						rowMonthlySum += val
						groupSum[m] += val
						totalSum[m] += val
					}

					groupSum[0] += rowMonthlySum
					totalSum[0] += rowMonthlySum

					processedData.push({
						acctcd: row.col0,
						acctnm: row.col1,
						total: rowMonthlySum,
						...Object.fromEntries(monthlyValues.map((v, idx) => [`M${idx + 1}`, v])),
						is_data: true
					})
					i++
				}

				processedData.push({
					acctcd: '',
					acctnm: '소  계',
					total: groupSum[0],
					...Object.fromEntries(groupSum.slice(1).map((v, idx) => [`M${idx + 1}`, v])),
					is_subtotal: true
				})
			}

			processedData.push({
				acctcd: '',
				acctnm: '총  계',
				total: totalSum[0],
				...Object.fromEntries(totalSum.slice(1).map((v, idx) => [`M${idx + 1}`, v])),
				is_total: true
			})
		}

		updateGridColumns()
		mainGrid?.setData(processedData)
		vAlert('조회되었습니다.')
	} catch (e) {
		vAlertError('조회 중 오류 발생')
	}
}

const updateGridColumns = () => {
	const baseColumns = [
		{ title: "계정코드", field: "acctcd", width: 90, hozAlign: "center", headerSort: false },
		{ title: "계정과목명", field: "acctnm", width: 180, headerSort: false },
		{
			title: "합계", field: "total", width: 110, hozAlign: "right",
			formatter: "money", formatterParams: { precision: 0 }, headerSort: false,
			cssClass: "fw-bold text-primary"
		}
	]

	const dynamicColumns = monthHeaders.value.map((label, idx) => ({
		title: label, field: `M${idx + 1}`, width: 95, hozAlign: "right",
		formatter: "money", formatterParams: { precision: 0 }, headerSort: false
	}))

	mainGrid?.setColumns([...baseColumns, ...dynamicColumns])
}

const excel = () => {
	mainGrid?.download("xlsx", `부서별비용명세서_${searchForm.yyyy}${searchForm.mm}.xlsx`)
}

const print = () => {
	if (!searchForm.deptcd) return vAlertError('부서를 먼저 선택해 주세요.')
	const params = `deptcd=${searchForm.deptcd}&deptnm=${searchForm.deptnm}&ymTO=${searchForm.yyyy}${searchForm.mm}&PRTGU=1`
	window.open(`/hasl/HASL_710P?${params}`, 'ExpensePrint', 'width=800,height=800,scrollbars=yes')
}

// 팝업 설정
const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function openHelp(type: string) {
	Object.assign(modalProps, {
		title: '부서 선택',
		path: '/ha00/HA00_00P_STR',
		defaultField: 'deptnm',
		data: {
			gubun: 'D0',
			cmpycd: authStore.cmpycd,
			code: searchForm.deptnm
		},
		columns: [
			{ title: '코드', field: 'deptcd', width: 80 },
			{ title: '부서명', field: 'deptnm', width: 180 }
		],
		onConfirm: (d: any) => {
			const item = d
			searchForm.deptcd = item.deptcd
			searchForm.deptnm = item.deptnm
			search()
		}
	})
	modalVisible.value = true
}

onMounted(() => {
	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns',
			height: '100%',
			columnDefaults: { headerSort: false, vertAlign: "middle" },
			columns: [],
			rowFormatter: (row) => {
				const d = row.getData()
				if (d.is_subtotal) {
                    row.getElement().style.backgroundColor = "#f8f9fa"
                    row.getElement().style.fontWeight = "bold"
                }
				if (d.is_total) {
					row.getElement().style.backgroundColor = "#e7f1ff"
					row.getElement().style.fontWeight = "bold"
				}
			}
		})
	}
	fetchMonthHeaders().then(() => updateGridColumns())
})
</script>

<style scoped>
.erp-label { min-width: 80px; font-weight: 500; font-size: 13px; }
.tabulator-instance { width: 100% !important; background-color: #fff; }
:deep(.tabulator-cell) { border-right: 1px solid #dee2e6 !important; }
:deep(.tabulator-header .tabulator-col) { border-right: 1px solid #dee2e6 !important; background-color: #f8f9fa !important; }
</style>
