<!--수입관리/수입발주현황-->
<!--
	=============================================================
	프로그램명	  : 수입발주현황 (시스템 최종 디자인 표준 모델)
    프로그램 ID	: HSIP200S
	작성일자	    : 25.02.24
	작성자	      : AI Assistant (Full-Expansion Layout)
    설명         : 미선적량/미입고량 자동 계산 및 우측 정렬 보정
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
	<Modal v-model:visible="modalVisible" :modalProps="modalProps" />

	<div class="erp-container">
		<!-- 🚀 1. 상단 액션 바 -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom shadow-sm bg-white py-1">
			<div class="fw-bold ps-3 text-dark d-flex align-items-center" style="font-size: 13px;">
				<i class="bi bi-file-earmark-bar-graph-fill me-2 text-primary"></i>
				수입관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
				<span class="text-primary fw-bolder">수입발주현황</span>
			</div>
			<div class="btn-group-erp pe-2">
				<button class="btn-erp btn-init" @click="initialize">초기화</button>
				<button class="btn-erp btn-search" @click="fetchList">조회</button>
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
					<th class="required border-end text-nowrap text-center">발주부서</th>
					<td>
						<div class="input-group input-group-sm flex-nowrap ms-1">
							<input v-model="searchForm.deptcd" type="text" class="form-control bg-light" style="max-width: 60px;" readonly />
							<input v-model="searchForm.deptnm" type="text" class="form-control" />
							<button class="btn btn-outline-secondary px-2" @click="openDeptPopup"><i class="bi bi-search"></i></button>
						</div>
					</td>
					<th class="border-end text-nowrap text-center">PO No</th>
					<td>
						<div class="input-group input-group-sm flex-nowrap ms-1">
							<input v-model="searchForm.fileno" type="text" class="form-control border-primary-subtle" placeholder="PO 번호 입력" @keyup.enter="fetchList" />
							<button class="btn btn-outline-secondary px-2" @click="openPoPopup"><i class="bi bi-search"></i></button>
						</div>
					</td>
					<td colspan="4" class="bg-light text-center small text-muted">※ 부서와 PO 번호로 수입 진행 현황을 조회합니다.</td>
				</tr>
			</tbody>
		</table>

		<!-- 📊 3. 하단 그리드 -->
		<div class="flex-grow-1 overflow-hidden bg-white">
			<div class="grid-title py-1 px-3 border-bottom fw-bold small text-primary bg-light">
				<i class="bi bi-list-check me-1"></i> 수입 발주별 진행 현황 리스트
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

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

// [도움창 상태 정의]
const modalVisible = ref(false)
const modalProps = reactive<any>({
	title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table'
})

const searchForm = reactive({
	deptcd: authStore.deptcd, deptnm: authStore.deptnm,
	fileno: ''
})

const mainGridRef = ref<HTMLDivElement | null>(null)
let mainGrid: Tabulator | null = null

async function fetchList() {
	if (!searchForm.deptcd) return vAlertError('발주부서를 선택하세요.')
	try {
		const res = await api.post('/hsip/HSIP_200S_STR', {
			...searchForm,
			cmpycd: authStore.cmpycd,
            actkind: 'S0'
		})
		mainGrid?.setData(res.data)
		vAlert('조회되었습니다.')
	} catch (e) { vAlertError('조회 실패') }
}

function initialize() {
	resetForm(searchForm)
	searchForm.deptcd = authStore.deptcd
	searchForm.deptnm = authStore.deptnm
	mainGrid?.clearData()
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

/** 🚀 PO 도움창 */
async function openPoPopup() {
	Object.assign(modalProps, {
		title: 'PO 선택',
		path: '/hs00/HS00_000S_STR',
		data: { gubun: 'F0', cmpycd: authStore.cmpycd, gbncd: '1', code: '', codenm: searchForm.fileno, etcval: '' },
		columns: [
			{ title: 'PO No', field: 'fileno', width: 150, hozAlign: 'center' },
			{ title: '거래처', field: 'custnm', minWidth: 200, widthGrow: 1 },
			{ title: '발주일자', field: 'issymd', width: 120, hozAlign: 'center' }
		],
		onConfirm: (d: any) => {
			searchForm.fileno = d.fileno
			fetchList()
		}
	})
	await nextTick()
	modalVisible.value = true
}

onMounted(async () => {
	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns',
			height: '100%',
			selectable: 1,
			placeholder: '표시할 데이터가 없습니다.',
			columnDefaults: {
				headerSort: false,
				headerHozAlign: 'center',
				hozAlign: 'right', // 🚀 기본값을 우측 정렬로 설정
				vertAlign: 'middle',
				minWidth: 100
			},
			columns: [
				{ title: '품목코드', field: 'itemcd', hozAlign: 'center', width: 110, cssClass: 'fw-bold text-primary border-end' },
				{ title: '품목명', field: 'itemnm', minWidth: 250, widthGrow: 10, cssClass: 'fw-bold' },
				{ title: '규격', field: 'itsize', width: 250 },
				{ title: '단위', field: 'unit', hozAlign: 'center', width: 80 },
				{ title: '발주량', field: 'balqty', hozAlign: 'right', width: 120, formatter: 'money', formatterParams: { precision: 0 } },
				{ title: '선적량', field: 'shipqty', hozAlign: 'right', width: 120, formatter: 'money', formatterParams: { precision: 0 }, cssClass: 'text-success' },
				{
					title: '미선적량', field: 'unship_calc', hozAlign: 'right', width: 120, cssClass: 'fw-bold',
					formatter: (c: any) => {
						const d = c.getData();
						const val = Number(d.balqty || 0) - Number(d.shipqty || 0);
						return val.toLocaleString();
					}
				},
				{ title: '통관량', field: 'passqty', hozAlign: 'right', width: 120, formatter: 'money', formatterParams: { precision: 0 }, cssClass: 'text-primary' },
				{
					title: '미입고량', field: 'unpass_calc', hozAlign: 'right', width: 120, cssClass: 'text-danger fw-bold',
					formatter: (c: any) => {
						const d = c.getData();
						const val = Number(d.balqty || 0) - Number(d.passqty || 0);
						return val.toLocaleString();
					}
				}
			]
		})
	}

	fetchList()
})
</script>

<style scoped>
.erp-table-full th, .erp-table-full td { height: 36px !important; padding: 0 8px !important; font-size: 13px; border: 1px solid #dee2e6; }
.tabulator-full-height { width: 100% !important; background-color: #fff; border-bottom: 3px solid #005a9f !important; }
</style>
