<!--
	=============================================================
	프로그램명	: 예산현황 (HABG210S)
	작성일자	: 2025.02.24
	작성자	    : AI Assistant
	설명        : 부서별 예산 대비 실적/잔액 현황 조회
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container d-flex flex-column h-100 bg-white">
		<!-- 🚀 상단 액션 바 (표준 규격) -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-grid-3x3-gap me-2 text-primary" style="font-size: 18px;"></i>
				예산관리 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				<span class="text-primary fw-bolder">예산현황 (HABG210S)</span>
			</div>
			<div class="btn-group-erp d-flex gap-1">
				<button class="btn-erp btn-search" @click="search">
					<i class="bi bi-search"></i> 조회
				</button>
				<button class="btn-erp btn-excel" @click="excel">
					<i class="bi bi-file-earmark-excel"></i> 엑셀
				</button>
			</div>
		</div>

		<!-- 💡 메인 컨텐츠 영역 -->
		<div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

			<!-- 🔍 조회 영역 (HSOD100U 표준 패턴) -->
			<div class="card border shadow-sm flex-shrink-0 overflow-hidden">
				<div class="card-body p-0 bg-white">
					<table class="erp-table-dense" width="100%">
						<colgroup>
							<col style="width: 12%" /><col style="width: 38%" />
							<col style="width: 12%" /><col style="width: 38%" />
						</colgroup>
						<tbody>
							<tr>
								<th class="text-center bg-light">예산년월</th>
								<td>
									<input v-model="searchForm.bugtym" type="month" class="form-control form-control-sm" style="width: 150px;" @change="search" />
								</td>
								<th class="text-center bg-light">예산부서</th>
								<td>
									<div class="input-group input-group-sm" style="width: 250px;">
										<input v-model="searchForm.deptcd" type="text" class="form-control text-center bg-light" style="max-width: 65px;" readonly />
										<input v-model="searchForm.deptnm" type="text" class="form-control" @keydown.enter="openHelp('DEPT')" placeholder="부서 선택" />
										<button class="btn btn-outline-secondary" @click="openHelp('DEPT')"><i class="bi bi-search"></i></button>
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

const searchForm = reactive({
	bugtym: new Date().toISOString().slice(0, 7),
	deptcd: authStore.deptcd || '',
	deptnm: authStore.deptnm || ''
})

const mainGridRef = ref<HTMLDivElement | null>(null)
let mainGrid: Tabulator | null = null

const search = async () => {
	if (!searchForm.deptcd) return vAlertError('부서를 선택하세요.')

	try {
		const res = await api.post('/habg/HABG_210S_STR', {
			cmpycd: authStore.cmpycd,
			bugtym: searchForm.bugtym.replace('-', ''),
			deptcd: searchForm.deptcd
		})

		const data = (res.data || []).map((row: any) => ({
			bugtcd: row.bugtcd,
			bugtnm: row.bugtnm,
			bugtamt: Number(row.asgnamt || 0),
			rsltamt: Number(row.janamt || 0),
			balamt: Number(row.bugtrstamt || 0),
			rate: Number(row.asgnamt) > 0 ? (Number(row.janamt) / Number(row.asgnamt) * 100).toFixed(1) : '0.0'
		}))

		mainGrid?.setData(data)
		vAlert('조회되었습니다.')
	} catch (e) { vAlertError('조회 중 오류 발생') }
}

const excel = () => {
	mainGrid?.download("xlsx", `예산현황_${searchForm.deptnm}_${searchForm.bugtym}.xlsx`)
}

const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function openHelp(type: string) {
    Object.assign(modalProps, {
        title: '부서 선택', path: '/ha00/HA00_00P_STR', data: { gubun: 'D0', cmpycd: authStore.cmpycd, code: searchForm.deptnm },
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

			columns: [
				{ title: "예산코드", field: "bugtcd", widthGrow: 1, hozAlign: "center" },
				{ title: "예산명", field: "bugtnm", widthGrow: 1 },
				{ title: "예산금액", field: "bugtamt", widthGrow: 1, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, bottomCalc: "sum" },
				{ title: "실적금액", field: "rsltamt", widthGrow: 1, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, bottomCalc: "sum" },
				{ title: "잔액", field: "balamt", widthGrow: 1, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, bottomCalc: "sum" },
				{ title: "집행율(%)", field: "rate", widthGrow: 1, hozAlign: "right" }
			]
		})
	}
	if (searchForm.deptcd) search()
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
