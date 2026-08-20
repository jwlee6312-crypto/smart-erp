<!--
	=============================================================
	프로그램명	: 매출 총이익 명세서 (Statement of Gross Sales Profit) [디자인 원칙 13가지 완벽 준수]
	작성일자	: 25.02.24
	작성자	    : AI Assistant
	설명        : 부서별/품목별 매출액, 원가, 이익 및 이익률 현황 조회 (당월/누계)
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container">
		<!-- 🚀 1, 11, 12. 상단 액션 바: 버튼 그룹 우측 상단 정렬 및 표준 색상 -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-graph-up-arrow me-2 text-primary" style="font-size: 18px;"></i>
				매출분석 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				<span class="text-primary fw-bolder">매출 총이익 명세서 (HSST340S)</span>
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
				<button class="btn-erp btn-print" @click="print">
					<i class="bi bi-printer"></i> 인쇄
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
									<span class="erp-label me-2">매출부서</span>
									<div class="input-group input-group-sm flex-nowrap" style="max-width: 300px;">
										<input v-model="searchForm.deptcd" type="text" class="form-control text-center bg-white" style="max-width: 60px;" readonly />
										<input v-model="searchForm.deptnm" type="text" class="form-control" placeholder="부서 선택" />
										<button class="btn btn-outline-secondary px-2" @click="openHelp('DEPT')"><i class="bi bi-search"></i></button>
									</div>
								</div>
							</td>
							<td>
								<div class="d-flex align-items-center px-2">
									<span class="erp-label me-2">조회연월</span>
									<div class="d-flex align-items-center gap-1 flex-grow-1" style="max-width: 250px;">
										<select v-model="searchForm.yy" class="form-select form-select-sm">
											<option v-for="y in yearOptions" :key="y" :value="y">{{ y }}년</option>
										</select>
										<select v-model="searchForm.mm" class="form-select form-select-sm">
											<option v-for="m in monthOptions" :key="m" :value="m">{{ m }}월</option>
										</select>
										<span class="text-muted small ms-1">현재</span>
									</div>
								</div>
							</td>
							<td><!-- 균등 배분을 위한 빈 영역 --></td>
						</tr>
					</tbody>
				</table>
			</div>
		</div>

		<!-- 📊 6, 8. 중앙 그리드 영역 (상하좌우 중앙 정렬 표준) -->
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
import { ref, reactive, onMounted, nextTick } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
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
const currentyy = String(now.getFullYear());
const currentmm = String(now.getMonth() + 1).padStart(2, '0');

// 13. 모든 변수명 대문자 고수
const searchForm = reactive({
	deptcd: authStore.deptcd,
	deptnm: authStore.deptnm,
	yy: currentyy,
    mm: currentmm
})

const yearOptions = ref<string[]>(Array.from({ length: 5 }, (_, i) => String(now.getFullYear() - i)));
const monthOptions = ref<string[]>(Array.from({ length: 12 }, (_, i) => String(i + 1).padStart(2, '0')));

const rowCount = ref(0)
const totals = reactive({ amtsum: 0, costsum: 0, profsum: 0, amtsum_t: 0, costsum_t: 0, profsum_t: 0 })
const mainGridRef = ref<HTMLDivElement | null>(null); let mainGrid: Tabulator | null = null

const search = async () => {
	try {
		const res = await api.post('/hsst/HSST_340S_STR', {
			...searchForm,
			cmpycd: authStore.cmpycd,
			ym: searchForm.yy + searchForm.mm
		})
		const data = res.data || []
		mainGrid?.setData(data)
		rowCount.value = data.length

		totals.amtsum = data.reduce((acc: number, cur: any) => acc + (Number(cur.salsamt) || 0), 0)
		totals.costsum = data.reduce((acc: number, cur: any) => acc + (Number(cur.salscost) || 0), 0)
		totals.profsum = totals.amtsum - totals.costsum

		totals.amtsum_t = data.reduce((acc: number, cur: any) => acc + (Number(cur.salsamt_t) || 0), 0)
		totals.costsum_t = data.reduce((acc: number, cur: any) => acc + (Number(cur.salscost_t) || 0), 0)
		totals.profsum_t = totals.amtsum_t - totals.costsum_t

		vAlert('조회되었습니다.')
	} catch (e) { vAlertError('조회 실패') }
}

const initialize = () => {
	resetForm(searchForm);
	searchForm.deptcd = authStore.deptcd; searchForm.deptnm = authStore.deptnm;
	searchForm.yy = currentyy; searchForm.mm = currentmm;
	mainGrid?.clearData(); rowCount.value = 0;
	Object.assign(totals, { amtsum: 0, COstsUM: 0, profsum: 0, amtsum_T: 0, COstsUM_T: 0, profsum_T: 0 });
}

const excel = () => mainGrid?.download("xlsx", "매출총이익명세서.xlsx")
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
	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns', height: '100%',
			columnDefaults: { headerSort: false, headerHozAlign: "center", hozAlign: "center", vertAlign: "middle", minWidth: 80 },
			columns: [
				{
					title: "품목명", field: "itemnm", minWidth: 200, widthGrow: 2, hozAlign: "left", cssClass: "fw-bold text-primary cursor-pointer", frozen: true,
					cellClick: (e, cell) => {
						const d = cell.getData();
						router.push({ path: '/HSST/HSST350S', query: { deptcd: searchForm.deptcd, yy: searchForm.yy, mm: searchForm.mm, itemcd: d.itemcd } });
					}
				},
				{
					title: "당월 실적 (Current Month)",
					columns: [
						{ title: "매출액", field: "salsamt", hozAlign: "right", width: 150, formatter: "money", formatterParams: { precision: 0 } },
						{ title: "매출원가", field: "salscost", hozAlign: "right", width: 150, formatter: "money", formatterParams: { precision: 0 } },
						{ title: "매총이익", field: "salsprof", hozAlign: "right", width: 150, formatter: "money", mutatorData: (v,d) => (Number(d.salsamt)||0) - (Number(d.SALSCOST)||0), cssClass: "text-info fw-bold" },
						{ title: "이익률(%)", field: "prof_rate", hozAlign: "right", width: 150, formatter: "money", formatterParams: { precision: 1 }, mutatorData: (v,d) => {
							const s = Number(d.salsamt)||0;
							return s !== 0 ? ((s - (Number(d.salscost)||0)) / s * 100) : 0;
						}}
					]
				},
				{
					title: "누계 실적 (Cumulative)",
					columns: [
						{ title: "매출액", field: "salsamt_t", hozAlign: "right", width: 150, formatter: "money", formatterParams: { precision: 0 } },
						{ title: "매출원가", field: "salscost_t", hozAlign: "right", width: 150, formatter: "money", formatterParams: { precision: 0 } },
						{ title: "매총이익", field: "salsprof_t", hozAlign: "right", width: 150, formatter: "money", mutatorData: (v,d) => (Number(d.salsamt_T)||0) - (Number(d.SALSCOST_T)||0), cssClass: "text-warning fw-bold" },
						{ title: "이익률(%)", field: "prof_rate_t", hozAlign: "right", width: 150, formatter: "money", formatterParams: { precision: 1 }, mutatorData: (v,d) => {
							const s = Number(d.salsamt_t)||0;
							return s !== 0 ? ((s - (Number(d.salscost_t)||0)) / s * 100) : 0;
						}}
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
