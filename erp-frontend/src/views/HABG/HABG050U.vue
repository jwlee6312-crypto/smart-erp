<!--
	=============================================================
	프로그램명	: 예산배정 (HABG050U)
	작성일자	: 2025.02.24
	작성자	    : AI Assistant
	설명        : 부서별 예산 조정액을 확인하고 배정액을 설정/저장
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container d-flex flex-column h-100 bg-white">
		<!-- 🚀 1. 상단 액션 바 (표준 규격 적용) -->
		<div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
			<div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-calendar-check me-2 text-primary" style="font-size: 18px;"></i>
				예산관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
				예산배정 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
				<span class="text-primary fw-bolder">예산배정 (HABG050U)</span>
			</div>
			<div class="btn-group-erp d-flex gap-1 pe-3">
				<button class="btn-erp btn-init" @click="initialize">초기화</button>
				<button class="btn-erp btn-search" @click="search">조회</button>
				<button class="btn-erp btn-save" @click="save">저장</button>
			</div>
		</div>

		<!-- 💡 2. 메인 컨텐츠 영역 -->
		<div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

			<!-- [상단] 조회 필터 영역 (HSOD100U 표준 패턴) -->
			<div class="card border shadow-sm flex-shrink-0 overflow-hidden">
				<div class="card-body p-0 bg-white">
					<table class="erp-table-dense" width="100%">
						<colgroup>
							<col style="width: 10%" /><col style="width: 40%" />
							<col style="width: 10%" /><col style="width: 40%" />
						</colgroup>
						<tbody>
							<tr>
								<th class="text-center bg-light">예산년월</th>
								<td>
									<div class="d-flex gap-1">
										<select v-model="searchForm.bugtyy" class="form-select form-select-sm" style="width: 100px;">
											<option v-for="year in yearOptions" :key="year" :value="year">{{ year }}년</option>
										</select>
										<select v-model="searchForm.bugtmm" class="form-select form-select-sm" style="width: 100px;">
											<option v-for="opt in periodOptions" :key="opt.value" :value="opt.value">{{ opt.text }}</option>
										</select>
									</div>
								</td>
								<th class="text-center bg-light">예산부서</th>
								<td>
									<div class="input-group input-group-sm" style="width: 250px;">
										<input v-model="searchForm.deptcd" type="text" class="form-control text-center bg-light" style="max-width: 65px;" readonly />
										<input v-model="searchForm.deptnm" type="text" class="form-control" @keydown.enter="openHelp('DEPT')" placeholder="부서 선택" />
										<button class="btn btn-outline-secondary px-2" @click="openHelp('DEPT')"><i class="bi bi-search"></i></button>
									</div>
								</td>
							</tr>
						</tbody>
					</table>
				</div>
			</div>

			<!-- 📊 3. 그리드 영역 -->
			<div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white">
				<div class="card-header py-1 px-3 bg-white border-bottom d-flex justify-content-between align-items-center">
					<div class="form-check form-check-inline mb-0">
						<input v-model="searchForm.allyn" class="form-check-input" type="checkbox" id="checkAdjApply" true-value="Y" false-value="N">
						<label class="form-check-label small fw-bold text-primary" for="checkAdjApply">조정액을 배정액에 일괄 적용 합니다.</label>
					</div>
					<span class="small text-muted fw-bold"><i class="bi bi-info-circle me-1"></i>배정 작업은 조정된 예산을 확정하는 단계입니다.</span>
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
import { ref, reactive, onMounted, computed } from 'vue'
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
const yearOptions = Array.from({ length: 7 }, (_, i) => String(currentYear + 1 - i))

const bgType = ref('010')

const searchForm = reactive({
	bugtyy: String(currentYear),
	bugtmm: String(new Date().getMonth() + 1).padStart(2, '0'),
	deptcd: authStore.deptcd || '',
	deptnm: authStore.deptnm || '',
	allyn: 'N'
})

const periodOptions = computed(() => {
	if (bgType.value === '020') {
		return [
			{ value: '01', text: '01 분기' }, { value: '04', text: '02 분기' },
			{ value: '07', text: '03 분기' }, { value: '10', text: '04 분기' }
		]
	} else if (bgType.value === '030') {
		return [{ value: '01', text: '상반기' }, { value: '07', text: '하반기' }]
	} else if (bgType.value === '040') {
		return [{ value: '01', text: `${searchForm.bugtyy} 년` }]
	}
	return Array.from({ length: 12 }, (_, i) => ({
		value: String(i + 1).padStart(2, '0'),
		text: `${String(i + 1).padStart(2, '0')} 월`
	}))
})

const mainGridRef = ref<HTMLDivElement | null>(null)
let mainGrid: Tabulator | null = null

const search = async () => {
	if (!searchForm.deptcd) return vAlertError('예산부서를 선택해 주십시오.')

	try {
		const res = await api.post('/habg/HABG_050U_STR', {
			actkind: 'S0',
			cmpycd: authStore.cmpycd,
			bugtym: `${searchForm.bugtyy}${searchForm.bugtmm}`,
			deptcd: searchForm.deptcd
		})

		const data = (res.data || []).map((row: any) => ({
			bugtcd: row.bugtcd || row.col0,
			bugtnm: row.bugtnm || row.col1,
			req_amt: Number(row.rqstamt || row.col2 || 0),
			adj_amt: Number(row.adstamt || row.col3 || 0),
			asgnamt: Number(row.asgnamt || row.col4 || 0)
		}))

		mainGrid?.setData(data)
		vAlert('조회되었습니다.')
	} catch (e) { vAlertError('조회 오류') }
}

const save = async () => {
	if (!searchForm.deptcd) return vAlertError('예산부서를 선택하세요.')
	const gridData = mainGrid?.getData() || []
	if (gridData.length === 0) return

	try {
		for (const row of gridData) {
			const res = await api.post('/habg/HABG_050U_STR', {
				actkind: 'A0',
				cmpycd: authStore.cmpycd,
				bugtym: `${searchForm.bugtyy}${searchForm.bugtmm}`,
				deptcd: searchForm.deptcd,
				bugtcd: row.bugtcd,
				asgnamt: row.asgnamt,
				allyn: searchForm.allyn,
				userid: authStore.userid
			})

			const resData = res.data?.[0];
			if (resData) {
				const rtnCode = String(resData.col0 || resData.COL0 || '').trim();
				const rtnMsg = resData.col1 || resData.COL1;

				if (rtnCode !== '' && rtnCode !== '000' && rtnCode !== '0') {
					vAlertError(rtnMsg || '저장 중 오류 발생');
					return;
				}
			}
		}
		vAlert('정상적으로 저장되었습니다.')
		search()
	} catch (e) { vAlertError('저장 실패') }
}

const initialize = () => {
	mainGrid?.clearData()
	searchForm.allyn = 'N'
}

const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function openHelp(type: 'DEPT') {
    Object.assign(modalProps, {
        title: '부서 선택', path: '/ha00/HA00_00P_STR', defaultField: 'deptnm',
        data: { gubun: 'D0', cmpycd: authStore.cmpycd, code: searchForm.deptnm },
        columns: [{ title: '코드', field: 'deptcd', width: 80 }, { title: '부서명', field: 'deptnm', width: 180 }],
        onConfirm: (d: any) => { searchForm.deptcd = d.deptcd; searchForm.deptnm = d.deptnm; search() }
    })
	modalVisible.value = true
}

const fetchBgType = async () => {
	try {
		const res = await api.post('/ha00/HA00_00P_STR', { gubun: 'E0', cmpycd: ' ', gbncd: '200' })
		if (res.data && res.data.length > 0) {
			bgType.value = res.data[0].remark || '010'
		} else {
			bgType.value = '010'
		}
	} catch (e) { bgType.value = '010' }
}

onMounted(async () => {
	await fetchBgType()

	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns',
			height: '100%',
			columnDefaults: { headerSort: false, vertAlign: "middle" },
			columns: [
				{ title: "예산코드", field: "bugtcd", widthGrow: 1, hozAlign: "center" },
				{ title: "예산명", field: "bugtnm", widthGrow: 2, hozAlign: "left" },
				{ title: "신청액", field: "req_amt", widthGrow: 1.5, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
				{ title: "조정액", field: "adj_amt", widthGrow: 1.5, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
				{
					title: "배정액", field: "asgnamt", widthGrow: 1.5, hozAlign: "right",
					editor: "number", formatter: "money", formatterParams: { precision: 0 },
					cssClass: "bg-warning-subtle fw-bold"
				}
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
