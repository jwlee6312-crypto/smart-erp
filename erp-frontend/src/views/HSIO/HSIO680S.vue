<!--
	=============================================================
	프로그램명	: 미정산현황 (Unsettled Status) [디자인 원칙 13가지 + 검색영역 단일행 균등배분]
	작성일자	: 25.02.24
	작성자	    : AI Assistant
	설명        : 거래처별 출고/정산/미정산 내역 비교 현황 조회
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container">
		<!-- 🚀 1, 12. 상단 액션 바: 버튼 그룹 우측 상단 정렬 및 표준 색상 -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-file-earmark-diff me-2 text-primary" style="font-size: 18px;"></i>
				영업정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				매출관리 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				<span class="text-primary fw-bolder">미정산현황 (HSIO680S)</span>
			</div>
			<div class="btn-group-erp d-flex gap-1">
				<button class="btn-erp btn-init" @click="initialize">
					<i class="bi bi-arrow-clockwise"></i> 초기화
				</button>
				<button class="btn-erp btn-search" @click="search">
					<i class="bi bi-search"></i> 조회
				</button>
				<button class="btn-erp btn-excel" @click="excel">
					<i class="bi bi-file-earmark-excel"></i> 엑셀
				</button>
			</div>
		</div>

		<!-- 🔍 9. 최상단 검색 항목 영역 (단일행 균등 배분 적용) -->
		<div class="p-2 pb-0 flex-shrink-0">
			<div class="card border shadow-sm overflow-hidden">
				<table class="erp-table-full" style="table-layout: fixed;">
					<colgroup>
						<col style="width: 33.3%;" />
						<col style="width: 33.3%;" />
						<col style="width: 33.4%;" />
					</colgroup>
					<tbody>
						<tr>
							<td>
								<div class="d-flex align-items-center px-2">
									<span class="erp-label me-2">판매부서</span>
									<div class="input-group input-group-sm flex-nowrap" style="max-width: 300px;">
										<input v-model="searchForm.deptcd" type="text" class="form-control text-center bg-white" style="max-width: 60px;" readonly />
										<input v-model="searchForm.deptnm" type="text" class="form-control" placeholder="부서 선택" />
										<button class="btn btn-outline-secondary px-2" @click="openHelp('DEPT')"><i class="bi bi-search"></i></button>
									</div>
								</div>
							</td>
							<td>
								<div class="d-flex align-items-center px-2">
									<span class="erp-label me-2">출고일자</span>
									<div class="d-flex align-items-center gap-1 flex-grow-1" style="max-width: 320px;">
										<input v-model="searchForm.fymd" type="date" class="form-control form-control-sm" />
										<span class="text-muted">~</span>
										<input v-model="searchForm.tymd" type="date" class="form-control form-control-sm" />
									</div>
								</div>
							</td>
							<td>
								<!-- 균등 배분을 위한 빈 영역 또는 추가 필드 가능 -->
							</td>
						</tr>
					</tbody>
				</table>
			</div>
		</div>

		<!-- 📊 6. 중앙 그리드 영역 (상하좌우 중앙 정렬 표준) -->
		<div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column">
			<div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white">
				<div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column" style="min-height: 0;">
					<div ref="mainGridRef" class="tabulator-full-height" />
				</div>
				<!-- 🚀 하단 요약 정보 (표준 추가) -->
				<div class="card-footer bg-light border-top py-1 px-3 d-flex justify-content-between align-items-center flex-shrink-0">
					<div class="small text-muted">
						[ 총 <span class="fw-bold text-primary">{{ rowCount }}</span> 건 ]
					</div>
					<div class="d-flex gap-4 small">
						<div class="d-flex align-items-center">
							<span class="me-2 text-muted">출고합계:</span>
							<span class="fw-bold text-dark">{{ formatNumber(totals.osum) }}</span>
						</div>
						<div class="d-flex align-items-center border-start ps-4">
							<span class="me-2 text-muted text-info">정산합계:</span>
							<span class="fw-bold text-info">{{ formatNumber(totals.jsum) }}</span>
						</div>
						<div class="d-flex align-items-center border-start ps-4">
							<span class="me-2 text-muted text-danger">미정산합계:</span>
							<span class="fw-bold text-danger">{{ formatNumber(totals.nsum) }}</span>
						</div>
					</div>
				</div>
			</div>
		</div>

	</div>

	<Modal v-model:visible="modalVisible" :modalProps="modalProps" />
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import * as XLSX from 'xlsx'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import type { ModalProps } from '@/types/modal'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

// 13. 모든 변수명 대문자 고수
const searchForm = reactive({
	deptcd: authStore.deptcd,
	deptnm: authStore.deptnm,
	fymd: new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString().substring(0, 10),
	tymd: new Date().toISOString().substring(0, 10)
})

const rowCount = ref(0)
const totals = reactive({ osum: 0, jsum: 0, nsum: 0 })
const mainGridRef = ref<HTMLDivElement | null>(null); let mainGrid: Tabulator | null = null

const search = async () => {
	try {
		const res = await api.post('/hsio/HSIO_680S_STR', {
			cmpycd: authStore.cmpycd,
			deptcd: searchForm.deptcd,
			fromdt: searchForm.fymd.replace(/-/g, ''),
			todt: searchForm.tymd.replace(/-/g, '')
		})
		const data = (res.data || []).map((row: any) => {
			return Object.fromEntries(Object.entries(row).map(([k, v]) => [k.toLowerCase(), v]))
		})
		mainGrid?.setData(data)
		rowCount.value = data.length

		totals.osum = data.reduce((acc: number, cur: any) => acc + (Number(cur.oamt||0) + Number(cur.ovat||0)), 0)
		totals.jsum = data.reduce((acc: number, cur: any) => acc + (Number(cur.jamt||0) + Number(cur.jvat||0)), 0)
		totals.nsum = data.reduce((acc: number, cur: any) => acc + (Number(cur.namt||0) + Number(cur.nvat||0)), 0)

		vAlert('조회되었습니다.')
	} catch (e) { vAlertError('조회 실패') }
}

const initialize = () => {
	resetForm(searchForm);
	searchForm.deptcd = authStore.deptcd; searchForm.deptnm = authStore.deptnm;
	searchForm.fymd = new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString().substring(0, 10);
	searchForm.tymd = new Date().toISOString().substring(0, 10);
	mainGrid?.clearData(); rowCount.value = 0;
	totals.osum = 0; totals.jsum = 0; totals.nsum = 0;
}

const excel = () => mainGrid?.download("xlsx", "미정산현황.xlsx")

const modalVisible = ref(false);
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function openHelp(type: string) {
	if (type === 'DEPT') {
		Object.assign(modalProps, {
			title: '부서 선택', path: '/ha00/HA00_00P_STR', defaultField: 'deptnm',
			data: { gubun: 'D0', cmpycd: authStore.cmpycd },
			columns: [{ title: '코드', field: 'deptcd', width: 80 }, { title: '부서명', field: 'deptnm', width: 180 }],
			onConfirm: (d: any) => { searchForm.deptcd = d.deptcd; searchForm.deptnm = d.deptnm }
		})
	}
	modalVisible.value = true
}

const formatNumber = (val: any) => Number(val || 0).toLocaleString()

onMounted(() => {
	if (typeof window !== 'undefined') (window as any).XLSX = XLSX;
	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns', height: '100%',
			columnDefaults: {
				headerSort: false,
				headerHozAlign: "center",
				hozAlign: 'right', // 🚀 기본값 우측 정렬
				vertAlign: "middle",
				minWidth: 80
			},
			columns: [
				{
					title: "거래처명",
					field: "custnm",
					minWidth: 180,
					hozAlign: "center",
					vertAlign: "middle",
					cssClass: "fw-bold",
					frozen: true
				},
				{
					title: "출고 정보",
					columns: [
						{ title: "수량", field: "oqty", hozAlign: "right", width: 80, formatter: "money", formatterParams: { precision: 0 } },
						{ title: "공급가", field: "oamt", hozAlign: "right", width: 110, formatter: "money" },
						{ title: "부가세", field: "ovat", hozAlign: "right", width: 100, formatter: "money" },
						{ title: "합계", field: "osum", hozAlign: "right", width: 120, formatter: "money", cssClass: "bg-light text-dark", mutatorData: (v,d) => Number(d.oamt||0) + Number(d.ovat||0) }
					]
				},
				{
					title: "정산 정보",
					columns: [
						{ title: "수량", field: "jqty", hozAlign: "right", width: 80, formatter: "money", formatterParams: { precision: 0 } },
						{ title: "공급가", field: "jamt", hozAlign: "right", width: 110, formatter: "money" },
						{ title: "부가세", field: "jvat", hozAlign: "right", width: 100, formatter: "money" },
						{ title: "합계", field: "jsum", hozAlign: "right", width: 120, formatter: "money", cssClass: "bg-light text-info", mutatorData: (v,d) => Number(d.jamt||0) + Number(d.jvat||0) }
					]
				},
				{
					title: "미정산 내역",
					columns: [
						{ title: "수량", field: "nqty", hozAlign: "right", width: 80, formatter: "money", formatterParams: { precision: 0 }, cssClass: "text-danger" },
						{ title: "공급가", field: "namt", hozAlign: "right", width: 110, formatter: "money", cssClass: "text-danger" },
						{ title: "부가세", field: "nvat", hozAlign: "right", width: 100, formatter: "money", cssClass: "text-danger" },
						{ title: "합계", field: "nsum", hozAlign: "right", width: 120, formatter: "money", cssClass: "bg-light-danger fw-bold text-danger", mutatorData: (v,d) => Number(d.namt||0) + Number(d.nvat||0) }
					]
				}
			]
		})
	}
})


</script>

<style scoped>
.tabulator-full-height { width: 100% !important; background-color: #fff; border-bottom: 3px solid #005a9f !important; }

/* 🚀 2단 헤더에서 단일 컬럼(거래처명 등)의 타이틀을 수직 중앙 정렬 */
:deep(.tabulator-header .tabulator-col:not(.tabulator-col-group) .tabulator-col-content) {
	height: 100% !important;
	display: flex !important;
	align-items: center !important;
	justify-content: center !important;
}
</style>
