<!--수입관리/수입원가계산서-->
<!--
	=============================================================
	프로그램명	  : 수입원가계산서 (좌우 대칭 대조형)
    프로그램 ID	: HSIP170S
	작성일자	    : 2025.02.24
	설명         : 좌측(품목 상세 및 소계/합계) vs 우측(비용종류별 요약) 구조
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
	<Modal v-model:visible="modalVisible" :modalProps="modalProps" />

	<div class="erp-container d-flex flex-column h-100 bg-white">
		<!-- 🚀 1. 상단 액션 바 -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom shadow-sm bg-white py-2 px-3 sticky-top">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-file-earmark-ruled-fill me-2 text-primary" style="font-size: 18px;"></i>
				수입관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
				<span class="text-primary fw-bolder">수입원가계산서 (HSIP170S)</span>
			</div>
			<div class="btn-group-erp d-flex gap-1 pe-2">
				<button class="btn-erp btn-init" @click="initialize">초기화</button>
				<button class="btn-erp btn-search" @click="search">조회</button>
				<button class="btn-erp btn-print" @click="print">인쇄</button>
			</div>
		</div>

		<!-- 🔍 2. 검색 조건 영역 -->
		<div class="p-2 bg-light flex-shrink-0">
			<div class="card border shadow-sm">
				<div class="card-body p-0 bg-white">
					<table class="erp-table-dense w-100">
						<colgroup>
							<col style="width: 100px;" /> <col style="width: 18%" />
							<col style="width: 100px;" /> <col style="width: 12%" />
							<col style="width: 100px;" /> <col style="width: 12%" />
							<col style="width: 100px;" /> <col />
						</colgroup>
						<tbody>
							<tr>
								<th class="required bg-light text-center">PO No</th>
								<td>
									<div class="input-group input-group-sm">
										<input v-model="searchForm.fileno" type="text" class="form-control fw-bold text-primary" placeholder="PO 번호" />
										<button class="btn btn-outline-secondary px-2" @click="openHelp('PO')"><i class="bi bi-search"></i></button>
									</div>
								</td>
								<th class="required bg-light text-center">선적차수</th>
								<td>
									<div class="input-group input-group-sm">
										<input v-model="searchForm.shipseq" type="text" class="form-control text-center" />
										<button class="btn btn-outline-secondary px-2" @click="openHelp('SHIP')"><i class="bi bi-search"></i></button>
									</div>
								</td>
								<th class="required bg-light text-center">통관차수</th>
								<td>
									<div class="input-group input-group-sm">
										<input v-model="searchForm.passseq" type="text" class="form-control text-center" />
										<button class="btn btn-outline-secondary px-2" @click="openHelp('PASS')"><i class="bi bi-search"></i></button>
									</div>
								</td>
								<th class="bg-light text-center">품 목</th>
								<td>
									<div class="input-group input-group-sm">
										<input v-model="searchForm.itemcd" type="text" class="form-control bg-light" style="max-width: 80px;" readonly />
										<input v-model="searchForm.itemnm" type="text" class="form-control" />
										<button class="btn btn-outline-secondary px-2" @click="openHelp('ITEM')"><i class="bi bi-search"></i></button>
									</div>
								</td>
							</tr>
						</tbody>
					</table>
				</div>
			</div>
		</div>

		<!-- 📊 3. 메인 작업 영역 (좌우 분할) -->
		<div class="flex-grow-1 overflow-hidden p-2 d-flex gap-2 bg-light pt-0">

			<!-- ⬅️ 좌측: 품목별 상세 비용 명세 (S0) -->
			<div class="card border shadow-sm flex-grow-1 d-flex flex-column overflow-hidden bg-white">
				<div class="card-header py-1 px-3 border-bottom fw-bold small text-primary bg-white d-flex align-items-center">
					<i class="bi bi-box-seam-fill me-2"></i> 품목별 상세 수입비용 및 소계
				</div>
				<div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
					<div ref="mainGridRef" class="tabulator-instance flex-grow-1" />
				</div>
			</div>

			<!-- ➡️ 우측: 비용 종류별 요약 (S1) -->
			<div class="card border shadow-sm d-flex flex-column overflow-hidden bg-white" style="width: 500px; min-width: 500px;">
				<div class="card-header py-1 px-3 border-bottom fw-bold small text-secondary bg-white d-flex align-items-center justify-content-between">
					<span><i class="bi bi-list-stars me-2"></i> 수입 비용종류별 총괄 요약</span>
				</div>
				<div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
                    <div ref="summaryGridRef" class="tabulator-instance flex-grow-1"></div>
                </div>
			</div>

		</div>
	</div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
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

const searchForm = reactive({
	fileno: '', shipseq: '10', passseq: '10', itemcd: '', itemnm: '', itsize: '', unit: ''
})

const mainGridRef = ref<HTMLDivElement | null>(null); const summaryGridRef = ref<HTMLDivElement | null>(null);
let mainGrid: Tabulator | null = null; let summaryGrid: Tabulator | null = null;

/** 🚀 데이터 조회 (S0, S1 동시 호출) */
async function search() {
	if (!searchForm.fileno) return vAlertError('PO No를 입력하세요.');
	try {
        // 1. 좌측 상세 그리드 조회 (S0)
		const resS0 = await api.post('/hsip/HSIP_161S_STR', {
			actkind: 'S0', cmpycd: authStore.cmpycd, fileno: searchForm.fileno,
            shipseq: searchForm.shipseq, passseq: searchForm.passseq, divyn: 'Y', itemcd: searchForm.itemcd
		})
		mainGrid?.setData(resS0.data || [])

        // 2. 우측 요약 그리드 조회 (S1)
        const resS1 = await api.post('/hsip/HSIP_161S_STR', {
			actkind: 'S1', cmpycd: authStore.cmpycd, fileno: searchForm.fileno,
            shipseq: searchForm.shipseq, passseq: searchForm.passseq, divyn: 'Y', itemcd: searchForm.itemcd
		})
        summaryGrid?.setData(resS1.data || [])

		vAlert('조회되었습니다.')
	} catch (e) { vAlertError('조회 실패') }
}

/** 🚀 도움창 핸들러 */
const modalVisible = ref(false);
const modalProps = reactive<any>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function openHelp(type: string) {
	if (type === 'PO') {
		Object.assign(modalProps, {
			title: 'PO 선택', path: '/hs00/HS00_000S_STR',
			data: { gubun: 'F0', cmpycd: authStore.cmpycd, gbncd: '1', code: '', codenm: '', etcval: '' },
			columns: [{ title: 'PO No', field: 'fileno', width: 120, hozAlign: 'center' }, { title: '거래처', field: 'custnm', minWidth: 200 }, { title: '발주일자', field: 'issymd', width: 120, hozAlign: 'center' }],
			onConfirm: (d: any) => { searchForm.fileno = d.fileno }
		})
	} else if (type === 'SHIP') {
        if (!searchForm.fileno) return vAlertError('먼저 PO No를 선택하세요.')
		Object.assign(modalProps, {
			title: '선적차수 선택', path: '/hs00/HS00_000S_STR',
			data: { gubun: 'F0', cmpycd: authStore.cmpycd, gbncd: '2', codegbn_1: searchForm.fileno, code: '', codenm: '', etcval: '' },
			columns: [{ title: '차수', field: 'code', width: 100, hozAlign: 'center' }, { title: '명칭', field: 'codenm', width: 200 }],
			onConfirm: (d: any) => { searchForm.shipseq = d.code }
		})
	} else if (type === 'PASS') {
        if (!searchForm.shipseq) return vAlertError('먼저 선적차수를 선택하세요.')
		Object.assign(modalProps, {
			title: '통관차수 선택', path: '/hs00/HS00_000S_STR',
			data: { gubun: 'F0', cmpycd: authStore.cmpycd, gbncd: '3', codegbn_1: searchForm.fileno, code: '', codenm: searchForm.shipseq, etcval: '' },
			columns: [{ title: '차수', field: 'code', width: 100, hozAlign: 'center' }, { title: '명칭', field: 'codenm', width: 200 }],
			onConfirm: (d: any) => { searchForm.passseq = d.code }
		})
	} else if (type === 'ITEM') {
		Object.assign(modalProps, {
			title: '품목 선택', path: '/ha00/HA00_00P_STR', defaultField: 'itemnm',
			data: { gubun: 'I1', cmpycd: authStore.cmpycd, gbncd: '1', code: '', codenm: '', etcval: '' },
			columns: [{ title: '코드', field: 'itemcd', width: 100, hozAlign: 'center' }, { title: '품목명', field: 'itemnm', width: 200 }, { title: '규격', field: 'itsize', width: 120 }],
			onConfirm: (d: any) => { searchForm.itemcd = d.itemcd; searchForm.itemnm = d.itemnm; searchForm.itsize = d.itsize; searchForm.unit = d.unit }
		})
	}
	modalVisible.value = true
}

const print = () => { vAlert('인쇄 기능을 준비 중입니다.') }
function initialize() {
	resetForm(searchForm); mainGrid?.clearData(); summaryGrid?.clearData()
	searchForm.shipseq = '10'; searchForm.passseq = '10'
}

onMounted(() => {
	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns', height: '100%',
			columnDefaults: { headerSort: false, headerHozAlign: 'center', vertAlign: 'middle' },
            groupBy: 'itemcd', // 🚀 품목별 그룹화 (소계)
            groupHeader: (value, count, data) => {
                return `<span class='fw-bold text-primary'>[${data[0].itemcd}] ${data[0].itemnm}</span> <span class='ms-2 badge bg-light text-dark'>규격: ${data[0].itsize} | 입고수량: ${Number(data[0].inqty).toLocaleString()}</span>`;
            },
			columns: [
				{ title: '규격', field: 'itsize', width: 100 },
				{ title: '단위', field: 'unit', hozAlign: 'center', width: 60 },
				{ title: '비용명', field: 'costnm', widthGrow: 1.2, hozAlign: 'left', bottomCalc: () => 'POS 전체 합계' },
				{
					title: '단가', field: 'calc_price', hozAlign: 'right', width: 100,
					formatter: (cell) => {
						const d = cell.getData(); const amt = Number(d.costamt || 0); const qty = Number(d.inqty || 1);
						if (qty === 0) return '0.00';
						return (amt / qty).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
					}
				},
				{ title: '금액', field: 'costamt', hozAlign: 'right', width: 120, formatter: 'money', formatterParams: { precision: 0 }, cssClass: 'fw-bold',
					bottomCalc: 'sum', groupCalc: 'sum' // 🚀 전체 합계 및 그룹 소계
				}
			]
		})
	}

	if (summaryGridRef.value) {
		summaryGrid = new Tabulator(summaryGridRef.value, {
			layout: 'fitColumns', height: '100%',
			columnDefaults: { headerSort: false, headerHozAlign: 'center', vertAlign: 'middle' },
			columns: [
				{ title: '비용종류', field: 'costnm', widthGrow: 1, hozAlign: 'left', bottomCalc: () => '배부액 총계' },
				{ title: '배부기준', field: 'divnm', width: 120, hozAlign: 'center' },
				{ title: '금액', field: 'costamt', hozAlign: 'right', width: 140, formatter: 'money', formatterParams: { precision: 0 }, cssClass: 'text-danger fw-bold', bottomCalc: 'sum' }
			]
		})
	}
})
</script>

<style scoped>
.erp-table-dense th, .erp-table-dense td { height: 32px !important; padding: 0 8px !important; font-size: 12px; border: 1px solid #dee2e6; }
.tabulator-instance { width: 100% !important; background-color: #fff; border-bottom: 3px solid #005a9f !important; }
:deep(.tabulator-group) { background: #f8f9fa !important; border-top: 1px solid #dee2e6 !important; border-bottom: 1px solid #dee2e6 !important; }
:deep(.tabulator-group-header) { padding: 4px 10px !important; }
:deep(.tabulator-calcs-bottom) { background: #fff3cd !important; font-weight: bold !important; border-top: 2px solid #dee2e6 !important; }
</style>
