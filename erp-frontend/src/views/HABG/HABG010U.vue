<!--
	=============================================================
	프로그램명	: 예산신청 (HABG010U)
	작성일자	: 2025.02.24
	작성자	    : AI Assistant
	설명        : 부서별/계정별 연간 예산을 월별로 신청 및 수정
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container d-flex flex-column h-100 bg-white">
		<!-- 🚀 상단 액션 바 (표준 규격) -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-pencil-square me-2 text-primary" style="font-size: 18px;"></i>
				예산관리 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				<span class="text-primary fw-bolder">예산신청 (HABG010U)</span>
			</div>
			<div class="btn-group-erp d-flex gap-1 pe-3">
				<button class="btn-erp btn-init" @click="initialize">초기화</button>
				<button class="btn-erp btn-search" @click="search">조회</button>
				<button class="btn-erp btn-save" @click="save">저장</button>
			</div>
		</div>

		<!-- 💡 메인 컨텐츠 영역 -->
		<div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

			<!-- 🔍 조회 조건 영역 -->
			<div class="card border shadow-sm flex-shrink-0 overflow-hidden">
				<div class="card-body p-0 bg-white">
					<table class="erp-table-dense" width="100%">
						<colgroup>
							<col style="width: 10%" /><col style="width: 23%" />
							<col style="width: 10%" /><col style="width: 23%" />
							<col style="width: 10%" /><col style="width: 24%" />
						</colgroup>
						<tbody>
							<tr>
								<th class="text-center bg-light">예산년도</th>
								<td>
									<select v-model="searchForm.bugtyy" class="form-select form-select-sm" style="width: 100px;" @change="search">
										<option v-for="year in yearOptions" :key="year" :value="year">{{ year }}년</option>
									</select>
								</td>
								<th class="text-center bg-light">예산부서</th>
								<td>
									<div class="input-group input-group-sm">
										<input v-model="searchForm.deptcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
										<input v-model="searchForm.deptnm" type="text" class="form-control" @keydown.enter="openHelp('DEPT')" placeholder="부서 선택" />
										<button class="btn btn-outline-secondary px-2" @click="openHelp('DEPT')"><i class="bi bi-search"></i></button>
									</div>
								</td>
								<th class="text-center bg-light">예산코드</th>
								<td>
									<div class="input-group input-group-sm">
										<input v-model="searchForm.bugtcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
										<input v-model="searchForm.bugtnm" type="text" class="form-control" @keydown.enter="openHelp('BUGT')" placeholder="예산코드 선택" />
										<button class="btn btn-outline-secondary px-2" @click="openHelp('BUGT')"><i class="bi bi-search"></i></button>
									</div>
								</td>
							</tr>
						</tbody>
					</table>
				</div>
			</div>

			<!-- 📊 그리드 영역 -->
			<div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white">
				<div class="card-header py-1 px-3 bg-white border-bottom d-flex justify-content-between align-items-center">
					<span class="small text-danger fw-bold"><i class="bi bi-exclamation-circle me-1"></i>조정 및 배정액이 존재하면 수정이 불가합니다.</span>
				</div>
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

//const currentYear = new Date().getFullYear()
const currentYear = 2011
const yearOptions = Array.from({ length: 6 }, (_, i) => String(currentYear + 1 - i))

const searchForm = reactive({
	bugtyy: String(currentYear),
	deptcd: authStore.deptcd || '',
	deptnm: authStore.deptnm || '',
	bugtcd: '',
	bugtnm: ''
})

const mainGridRef = ref<HTMLDivElement | null>(null)
let mainGrid: Tabulator | null = null

const search = async () => {
	if (!searchForm.deptcd || !searchForm.bugtcd) return

	try {
		const res = await api.post('/habg/HABG_010U_STR', {
			actkind: 'S0',
			cmpycd: authStore.cmpycd,
			bugtym: searchForm.bugtyy,
			deptcd: searchForm.deptcd,
			bugtcd: searchForm.bugtcd
		})

		const data = (res.data || []).map((row: any) => ({
		    mm: row.bugtym,
			gubun: row.bugtymnm,
			last_amt: Number(row.buseamt || 0),
			req_amt: Number(row.creqamt || 0),
			adj_amt: Number(row.cadstamt || 0),
			alloc_amt: Number(row.casgnamt || 0)
		}))

		mainGrid?.setData(data)
		vAlert('조회되었습니다.')
	} catch (e) { vAlertError('조회 오류') }
}

const save = async () => {
	if (!searchForm.deptcd || !searchForm.bugtcd) {
		return vAlertError('부서와 예산코드를 선택하세요.')
	}

	const gridData = mainGrid?.getData() || []
	if (gridData.length === 0) return

	try {
		for (const row of gridData) {
			await api.post('/habg/HABG_010U_STR', {
				actkind: 'A0',
				cmpycd: authStore.cmpycd,
				bugtym: `${searchForm.bugtyy}${row.mm}`,
				deptcd: searchForm.deptcd,
				bugtcd: searchForm.bugtcd,
				reqamt: row.req_amt,
				userid: authStore.userid
			})
		}
		vAlert('정상적으로 저장되었습니다.')
		search()
	} catch (e) { vAlertError('저장 중 오류 발생') }
}

const initialize = () => {
	searchForm.bugtcd = ''; searchForm.bugtnm = ''
	mainGrid?.clearData()
}

const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function openHelp(type: 'DEPT' | 'BUGT') {
	if (type === 'DEPT') {
		Object.assign(modalProps, {
			title: '부서 선택', path: '/ha00/HA00_00P_STR', defaultField: 'deptnm',
			data: { gubun: 'D0', cmpycd: authStore.cmpycd, code: searchForm.deptnm },
			columns: [{ title: '코드', field: 'deptcd', width: 80 }, { title: '부서명', field: 'deptnm', width: 180 }],
			onConfirm: (d: any) => { searchForm.deptcd = d.deptcd; searchForm.deptnm = d.deptnm; search() }
		})
	} else {
		Object.assign(modalProps, {
			title: '예산코드 선택', path: '/ha00/HA00_00P_STR', defaultField: 'bugtnm',
			data: { gubun: 'B0', cmpycd: authStore.cmpycd, code: searchForm.bugtnm },
			columns: [{ title: '코드', field: 'bugtcd', width: 80 }, { title: '예산명', field: 'bugtnm', width: 180 }],
			onConfirm: (d: any) => { searchForm.bugtcd = d.bugtcd; searchForm.bugtnm = d.bugtnm; search() }
		})
	}
	modalVisible.value = true
}

onMounted(() => {
	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns',
			height: '100%',
			columnDefaults: { headerSort: false, vertAlign: "middle" },
			columns: [
				{ title: "구분", field: "gubun", widthGrow: 1, hozAlign: "center" },
				{ title: "전년사용액", field: "last_amt", widthGrow: 1, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
				{
					title: "신청액", field: "req_amt", widthGrow: 1, hozAlign: "right",
					editor: "number", formatter: "money", formatterParams: { precision: 0 },
					cssClass: "fw-bold text-primary"
				},
				{ title: "조정액", field: "adj_amt", widthGrow: 1, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
				{ title: "배정액", field: "alloc_amt", widthGrow: 1, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
				{ title: "", field: "empty", widthGrow: 1 }
			],
			columnCalcs: "both",
			bottomCalc: "sum"
		})
	}
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
