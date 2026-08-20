<!--	=============================================================
	프로그램명 : 신용카드매출전표등발행금액집계표
	작성일자	: 2025.02.24
	작성자    : AI Assistant
	설명        : 신용카드 및 현금영수증 발행 내역의 과세/면세별 총괄 집계 (표준 UI 적용)
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container">
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-collection-fill me-2 text-primary" style="font-size: 18px;"></i>
				재무관리<i class="bi bi-chevron-right mx-1 small opacity-50"></i>
				<span class="text-primary fw-bolder">신용카드매출전표등발행금액집계표 (HATX140S)</span>
			</div>
			<div class="btn-group-erp d-flex gap-1">
				<button class="btn-erp btn-init" @click="initialize">
					초기화
				</button>
				<button class="btn-erp btn-search" @click="search">
					<i class="bi bi-search"></i> 조회
				</button>
				<button class="btn-erp btn-print" @click="print">
					<i class="bi bi-printer"></i> 인쇄
				</button>
			</div>
		</div>

		<!-- [검색] 검색 조건 영역 -->
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
									</div>
								</td>
							</tr>
						</tbody>
					</table>
				</div>
			</div>
		</div>

		<!-- [콘텐츠] 메인 콘텐츠 영역 -->
		<div class="flex-grow-1 overflow-auto p-2 d-flex flex-column gap-3 bg-light main-content-wrapper">

			<!-- [그리드1] 메인 발행금액 현황 그리드 -->
			<div class="card border shadow-sm flex-shrink-0 overflow-hidden d-flex flex-column bg-white">
				<div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
					<span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-1 text-primary"></i> 발행금액 현황</span>
				</div>
				<div class="card-body p-0 overflow-hidden">
					<div ref="mainGridRef" class="tabulator-instance border-0"></div>
				</div>
			</div>

			<!-- [그리드2] 세금계산서/계산서 교부 내역 그리드 -->
			<div class="card border shadow-sm flex-shrink-0 overflow-hidden d-flex flex-column bg-white" style="max-width: 600px;">
				<div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
					<span class="fw-bold small text-dark"><i class="bi bi-journal-text me-1 text-primary"></i> 세금계산서/계산서 교부 내역</span>
				</div>
				<div class="card-body p-0 overflow-hidden">
					<div ref="subGridRef" class="tabulator-instance border-0"></div>
				</div>
			</div>

			<!-- [안내] 작성방법 안내 -->
			<div class="alert alert-secondary border-0 shadow-sm mb-0 p-3 flex-shrink-0">
				<div class="small text-muted">
					<p class="mb-2 fw-bold text-dark">※ 작성방법</p>
					<p class="mb-1">1. <strong>신용카드매출전표 등 발행금액 현황</strong>: 부가가치세 과세 매출분, 면세 매출분 및 봉사료로 각각 구분하여 기입하고, 과세 매출분은 공급대가(부가가치세를 포함합니다)를 기입합니다.</p>
					<p class="mb-0">2. <strong>세금계산서/계산서 교부 내역</strong>: 위 현황 중 합계 금액 중 세금계산서 또는 계산서를 교부한 금액을 기입합니다.</p>
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
	taxunit: '000',
	yy: String(currentYear),
	fmm: currentMonth,
	tmm: currentMonth
})

const mainGridRef = ref<HTMLElement | null>(null)
const subGridRef = ref<HTMLElement | null>(null)
let mainGrid: Tabulator | null = null
let subGrid: Tabulator | null = null

const fetchOptions = async () => {
	try {
        const res = await api.post('/ha00/HA00_00P_STR', { gubun: 'SA', cmpycd: authStore.cmpycd })
        taxUnitOptions.value = (res.data || []).map((i: any) => ({ code: i.taxunit, name: i.unitnm }))
        if (taxUnitOptions.value.length > 0) searchForm.taxunit = taxUnitOptions.value[0].code

	} catch (e) { console.error(e) }
}

async function search() {
	try {
		const ymfr = searchForm.yy + searchForm.fmm;
		const ymto = searchForm.yy + searchForm.tmm;

		const res = await api.post('/hatx/HATX_140S_STR', {
			actkind: 'S0',
			cmpycd: authStore.cmpycd,
			taxunit: searchForm.taxunit,
			ymfr: ymfr,
			ymto: ymto
		});



		if (res.data?.length) {
			const d = res.data[0];
			const mainData = [
				{ gubun: '합   계', tot: d.amt0, card: d.amt1, cash: d.amt2 },
				{ gubun: '과세 매출분', tot: d.amt3, card: d.amt4, cash: d.amt5 },
				{ gubun: '면세 매출분', tot: d.amt6, card: d.amt7, cash: d.amt8 },
				{ gubun: '봉 사 료', tot: d.amt9, card: d.amt10, cash: d.amt11 }
			];
			mainGrid?.setData(mainData);

			const subData = [
				{ item: '세금계산서 교부 금액', amt: d.amt12 },
				{ item: '계산서 교부 금액', amt: d.amt13 }
			];
			subGrid?.setData(subData);
		} else {
			mainGrid?.clearData();
			subGrid?.clearData();
		}

		vAlert('조회되었습니다.');
	} catch (e) { vAlertError('조회 실패'); }
}

const initialize = () => {
	searchForm.taxunit = '000';
	searchForm.yy = String(currentYear);
	searchForm.fmm = currentMonth;
	searchForm.tmm = currentMonth;
	mainGrid?.clearData();
	subGrid?.clearData();
}

const initGrids = () => {
	mainGrid = new Tabulator(mainGridRef.value!, {
		layout: "fitColumns",
		height: "auto",
		columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
		columns: [
			{ title: "구 분", field: "gubun", width: 200, cssClass: "bg-light fw-bold" },
			{ title: "합 계", field: "tot", hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "fw-bold" },
			{ title: "신용/직불/기명식선불카드", field: "card", hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
			{ title: "현금영수증", field: "cash", hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } }
		]
	});

	subGrid = new Tabulator(subGridRef.value!, {
		layout: "fitColumns",
		height: "auto",
		columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
		columns: [
			{ title: "구 분", field: "item", width: 300, cssClass: "bg-light fw-bold" },
			{ title: "금 액", field: "amt", hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "text-primary fw-bold" }
		]
	});
}

onMounted(() => {
	nextTick(() => {
		initGrids();
		fetchOptions();
		search();
	});
})
</script>

<style scoped>
:deep(.tabulator-cell) { border-right: 1px solid #dee2e6 !important; font-size: 13px; }
:deep(.tabulator-header .tabulator-col) { border-right: 1px solid #dee2e6 !important; background-color: #f8f9fa !important; font-size: 13px; }
</style>
