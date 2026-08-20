<!--수입관리/수입단가추이현황-->
<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
	<Modal v-model:visible="modalVisible" :modalProps="modalProps" />

	<div class="erp-container">
		<!-- 🚀 1. 상단 액션 바 -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom shadow-sm bg-white py-1 sticky-top">
			<div class="fw-bold ps-3 text-dark d-flex align-items-center" style="font-size: 13px;">
				<i class="bi bi-graph-up-arrow me-2 text-primary"></i>
				수입관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
				<span class="text-primary fw-bolder">수입단가추이현황</span>
			</div>
			<div class="btn-group-erp pe-2">
				<button class="btn-erp btn-init" @click="initialize">초기화</button>
				<button class="btn-erp btn-search" @click="fetchTrendList">조회</button>
			</div>
		</div>

		<!-- 🔍 2. 검색 조건 -->
		<table class="erp-table-full border-bottom" style="table-layout: fixed;">
			<colgroup>
				<col style="width: 130px;" /> <col />
				<col style="width: 130px;" /> <col />
				<col style="width: 130px;" /> <col />
				<col style="width: 130px;" /> <col />
			</colgroup>
			<tbody>
				<tr>
					<th class="required border-end text-nowrap text-center">수입부서</th>
					<td>
						<div class="input-group input-group-sm flex-nowrap ms-1">
							<input v-model="searchForm.deptcd" type="text" class="form-control bg-light" style="max-width: 60px;" readonly />
							<input v-model="searchForm.deptnm" type="text" class="form-control" />
							<button class="btn btn-outline-secondary px-2" @click="openDeptPopup"><i class="bi bi-search"></i></button>
						</div>
					</td>
					<th class="required border-end text-nowrap text-center">수입연도</th>
					<td>
						<select v-model="searchForm.yy" class="form-select form-select-sm ms-1" style="width: 120px;">
							<option v-for="year in yearOptions" :key="year" :value="year">{{ year }}년</option>
						</select>
					</td>
					<td colspan="4" class="bg-light text-center small text-muted">
						※ 수입 연도별 월별 단가 및 금액 추이를 분석합니다.
					</td>
				</tr>
			</tbody>
		</table>

		<!-- 📊 3. 추이 분석 그리드 -->
		<div class="flex-grow-1 overflow-hidden bg-white d-flex flex-column">
			<div class="grid-title py-1 px-3 border-bottom fw-bold small text-primary d-flex justify-content-between align-items-center bg-light">
				<span><i class="bi bi-table me-1"></i> 월별 품목 수입 추이 데이터</span>
				<span class="badge bg-primary-subtle text-primary border border-primary-subtle px-2">단위: 원화(KRW)</span>
			</div>
			<div ref="mainGridRef" class="tabulator-full-height border-top" />
		</div>
	</div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, nextTick } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { getDate } from '@/composables/useDate'

const authStore = useAuthStore()
const { firstDay, today } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

// [도움창 상태 정의]
const modalVisible = ref(false)
const modalProps = reactive<any>({
	title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table'
})

const currentYear = new Date().getFullYear()
const searchForm = reactive({
	deptcd: authStore.deptcd, deptnm: authStore.deptnm,
	yy: currentYear.toString()
})

const yearOptions = ref<string[]>([])
const mainGridRef = ref<HTMLDivElement | null>(null)
let mainGrid: Tabulator | null = null

const initYearOptions = () => {
	const years = []
	for (let i = 0; i < 6; i++) { years.push((currentYear - i).toString()) }
	yearOptions.value = years
}

async function fetchTrendList() {
	try {
		const res = await api.post('/hsip/HSIP_210S_STR', {
			...searchForm,
			cmpycd: authStore.cmpycd
		})
		mainGrid?.setData(res.data)
		vAlert('조회되었습니다.')
	} catch (e) { vAlertError('조회 실패') }
}

/** 🚀 부서 도움창 */
async function openDeptPopup() {
	Object.assign(modalProps, {
		title: '부서 선택',
		path: '/ha00/HA00_00P_STR',
		data: { gubun: 'D0', cmpycd: authStore.cmpycd, gbncd: '', code: '', codenm: searchForm.deptnm, etcval: '' },
		columns: [
			{ title: '부서코드', field: 'deptcd', width: 100, hozAlign: 'center' },
			{ title: '부서명', field: 'deptnm', width: 200 }
		],
		onConfirm: (d: any) => {
			searchForm.deptcd = d.deptcd
			searchForm.deptnm = d.deptnm
		}
	})
	await nextTick()
	modalVisible.value = true
}

function initialize() {
	resetForm(searchForm)
	searchForm.deptcd = authStore.deptcd
	searchForm.deptnm = authStore.deptnm
	searchForm.yy = currentYear.toString()
	mainGrid?.clearData()
}

onMounted(async () => {
	initYearOptions()
	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns', height: '100%',
			placeholder: '데이터 없음',
			columnDefaults: {
				headerSort: false,
				headerHozAlign: 'center',
				hozAlign: 'right', // 🚀 기본값을 우측 정렬로 설정
				vertAlign: 'middle',
				minWidth: 70
			},
			columns: [
				{ title: '품목 명칭', field: 'itemnm', minWidth: 250, widthGrow: 1, hozAlign: 'left', cssClass: 'fw-bold text-dark border-end' },
				{ title: '합 계', field: 'amtsum', width: 110, formatter: 'money', formatterParams: { precision: 0 }, cssClass: 'bg-info-subtle fw-bold' },
				{ title: '1월', field: 'amt01', width: 90, formatter: 'money' },
				{ title: '2월', field: 'amt02', width: 90, formatter: 'money' },
				{ title: '3월', field: 'amt03', width: 90, formatter: 'money' },
				{ title: '4월', field: 'amt04', width: 90, formatter: 'money' },
				{ title: '5월', field: 'amt05', width: 90, formatter: 'money' },
				{ title: '6월', field: 'amt06', width: 90, formatter: 'money' },
				{ title: '7월', field: 'amt07', width: 90, formatter: 'money' },
				{ title: '8월', field: 'amt08', width: 90, formatter: 'money' },
				{ title: '9월', field: 'amt09', width: 90, formatter: 'money' },
				{ title: '10월', field: 'amt10', width: 90, formatter: 'money' },
				{ title: '11월', field: 'amt11', width: 90, formatter: 'money' },
				{ title: '12월', field: 'amt12', width: 90, formatter: 'money' }
			]
		})
	}
	fetchTrendList()
})
</script>

<style scoped>
.erp-table-full th, .erp-table-full td { height: 36px !important; padding: 0 8px !important; font-size: 13px; border: 1px solid #dee2e6; }
.tabulator-full-height { width: 100% !important; background-color: #fff; border-bottom: 3px solid #005a9f !important; }
</style>
