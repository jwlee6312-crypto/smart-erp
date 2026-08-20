<!--
	=============================================================
	프로그램명	: 품목별 판매현황 [디자인 원칙 13가지 + 검색영역 단일행 균등배분]
	작성일자	: 25.02.24
	작성자	    : AI Assistant
	설명        : 부서별/기간별 품목 매출 현황(당월/누계) 조회 및 상세 이동 기능
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container">
		<!-- 🚀 1, 11, 12. 상단 액션 바: 버튼 그룹 우측 상단 정렬 및 표준 색상 -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-bar-chart-line-fill me-2 text-primary" style="font-size: 18px;"></i>
				매출분석 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				<span class="text-primary fw-bolder">품목별 판매현황 (HSST320S)</span>
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

		<!-- 🔍 9. 최상단 검색 항목 영역 (단일행 25% 균등 배분 적용) -->
		<div class="p-2 pb-0 flex-shrink-0">
			<div class="card border shadow-sm overflow-hidden">
				<table class="erp-table-full" style="table-layout: fixed;">
					<colgroup>
						<col style="width: 25%;" />
						<col style="width: 25%;" />
						<col style="width: 25%;" />
						<col style="width: 25%;" />
					</colgroup>
					<tbody>
						<tr>
							<td>
								<div class="d-flex align-items-center px-2">
									<span class="erp-label me-2">판매부서</span>
									<div class="input-group input-group-sm flex-nowrap">
										<input v-model="searchForm.deptcd" type="text" class="form-control text-center bg-white" style="max-width: 60px;" readonly />
										<input v-model="searchForm.deptnm" type="text" class="form-control" placeholder="부서 선택" />
										<button class="btn btn-outline-secondary px-2" @click="openHelp('DEPT')"><i class="bi bi-search"></i></button>
									</div>
								</div>
							</td>
							<td>
								<div class="d-flex align-items-center px-2">
									<span class="erp-label me-2">판매일자</span>
									<div class="d-flex align-items-center gap-1 flex-grow-1">
										<input v-model="searchForm.fymd" type="date" class="form-control form-control-sm" />
										<span class="text-muted">~</span>
										<input v-model="searchForm.tymd" type="date" class="form-control form-control-sm" />
									</div>
								</div>
							</td>
							<td><!-- 균등 배분 공간 --></td>
							<td><!-- 균등 배분 공간 --></td>
						</tr>
					</tbody>
				</table>
			</div>
		</div>

		<!-- 📊 6, 8. 중앙 그리드 영역 (중앙 정렬 표준 적용) -->
		<div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column">
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
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { useRouter } from 'vue-router'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import type { ModalProps } from '@/types/modal'

const authStore = useAuthStore()
const router = useRouter()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

const now = new Date();
const firstDay = new Date(now.getFullYear(), now.getMonth(), 1).toISOString().substring(0, 10);
const today = now.toISOString().substring(0, 10);

// 13. 모든 변수명 대문자 고수
const searchForm = reactive({
	deptcd: authStore.deptcd,
	deptnm: authStore.deptnm,
	fymd: firstDay,
	tymd: today
})

const rowCount = ref(0)
const totals = reactive({ tmsum: 0, tysum: 0 })
const mainGridRef = ref<HTMLDivElement | null>(null); let mainGrid: Tabulator | null = null

const search = async () => {
	try {
		const res = await api.post('/hsst/HSST_320S_STR', {
			...searchForm,
			cmpycd: authStore.cmpycd,
			fymd: searchForm.fymd.replace(/-/g, ''),
			tymd: searchForm.tymd.replace(/-/g, '')
		})
		const data = res.data || []
		mainGrid?.setData(data)
		rowCount.value = data.length

		totals.tmsum = data.reduce((acc: number, cur: any) => acc + (Number(cur.tmamt) || 0), 0)
		totals.tysum = data.reduce((acc: number, cur: any) => acc + (Number(cur.tyamt) || 0), 0)

		vAlert('조회되었습니다.')
	} catch (e) { vAlertError('조회 실패') }
}

const initialize = () => {
	resetForm(searchForm);
	searchForm.deptcd = authStore.deptcd; searchForm.deptnm = authStore.deptnm;
	searchForm.fymd = firstDay; searchForm.tymd = today;
	mainGrid?.clearData(); rowCount.value = 0;
	totals.tmsum = 0; totals.tysum = 0;
}

const excel = () => mainGrid?.download("xlsx", "품목별판매현황.xlsx")
const print = () => vAlert('인쇄 기능을 준비 중입니다.')

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
                    headerVertAlign: "middle",
                    hozAlign: "center",
                    vertAlign: "middle",
                    minWidth: 80
                },
        		columns: [
				{
                    title: "품목명", field: "itemnm", minWidth: 200, widthGrow: 2, hozAlign: "left",
                    cssClass: "fw-bold text-primary cursor-pointer", frozen: true,
                    cellClick: (e, cell) => {
                        const d = cell.getData();
                        router.push({ path: '/HSST/HSST330S', query: { fymd: searchForm.fymd, tymd: searchForm.tymd, deptcd: searchForm.deptcd, deptnm: searchForm.deptnm, itemcd: d.itemcd } });
                    }
                },
				{
					title: "당월 실적 (Current Month)",
					columns: [
						{ title: "수량", field: "mqty", hozAlign: "right", width: 150, formatter: "money", formatterParams: { precision: 0 } },
						{ title: "매출액", field: "smamt", hozAlign: "right", width: 150, formatter: "money" },
						{ title: "부가세", field: "vmamt", hozAlign: "right", width: 150, formatter: "money" },
						{ title: "합계", field: "tmamt", hozAlign: "right", width: 150, formatter: "money", cssClass: "bg-light text-primary fw-bold" }
					]
				},
				{
					title: "누계 실적 (Cumulative)",
					columns: [
						{ title: "수량", field: "yqty", hozAlign: "right", width: 150, formatter: "money", formatterParams: { precision: 0 } },
						{ title: "매출액", field: "syamt", hozAlign: "right", width: 150, formatter: "money" },
						{ title: "부가세", field: "vyamt", hozAlign: "right", width: 150, formatter: "money" },
						{ title: "합계", field: "tyamt", hozAlign: "right", width: 150, formatter: "money", cssClass: "bg-light text-warning fw-bold" }
					]
				}
			]
		})
	}
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; border-bottom: 3px solid #005a9f !important; }

/* 🚀 2단 헤더에서 단일 컬럼의 타이틀을 수직 중앙 정렬 */
:deep(.tabulator-header .tabulator-col:not(.tabulator-col-group) .tabulator-col-content) {
	height: 100% !important;
	display: flex !important;
	align-items: center !important;
	justify-content: center !important;
}
</style>

