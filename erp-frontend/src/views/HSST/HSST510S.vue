<!--
	=============================================================
	프로그램명	: 품목별 거래처별 판매 현황 [디자인 원칙 13가지 + 검색영역 단일행 균등배분]
	작성일자	: 25.02.24
	작성자	    : AI Assistant
	설명        : 특정 품목에 대한 거래처별 매출 현황(당월/누계) 조회 및 상세 내역 이동
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container">
		<!-- 🚀 1, 11, 12. 상단 액션 바: 버튼 그룹 우측 상단 정렬 -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-box-seam-fill me-2 text-primary" style="font-size: 18px;"></i>
				매출분석 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				<span class="text-primary fw-bolder">품목별 거래처별 판매 현황 (HSST510S)</span>
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

		<!-- 🔍 9. 최상단 검색 항목 영역 (단일행 33.3% 균등 배분 적용) -->
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
							<td>
								<div class="d-flex align-items-center px-2">
									<span class="erp-label me-2">판매품목</span>
									<div class="input-group input-group-sm flex-nowrap">
										<input v-model="searchForm.itemcd" type="text" class="form-control text-center bg-white" style="max-width: 80px;" readonly />
										<input v-model="searchForm.itemnm" type="text" class="form-control" placeholder="품목 선택" />
										<button class="btn btn-outline-secondary px-2" @click="openHelp('ITEM')"><i class="bi bi-search"></i></button>
									</div>
								</div>
							</td>
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
	tymd: today,
	itemcd: '',
	itemnm: ''
})

const rowCount = ref(0)
const totals = reactive({ smtot: 0, sytot: 0 })
const mainGridRef = ref<HTMLDivElement | null>(null); let mainGrid: Tabulator | null = null

const search = async () => {
	if (!searchForm.itemcd) return vAlertError('품목을 선택하세요.');
	try {
		const res = await api.post('/hsst/HSST_510S_STR', {
			...searchForm,
			cmpycd: authStore.cmpycd,
			fymd: searchForm.fymd.replace(/-/g, ''),
			tymd: searchForm.tymd.replace(/-/g, '')
		})
		const data = res.data || []
		mainGrid?.setData(data)
		rowCount.value = data.length

		totals.smtot = data.reduce((acc: number, cur: any) => acc + (Number(cur.mamt || 0) + Number(cur.mvat || 0)), 0)
		totals.sytot = data.reduce((acc: number, cur: any) => acc + (Number(cur.tamt || 0) + Number(cur.tvat || 0)), 0)

		vAlert('조회되었습니다.')
	} catch (e) { vAlertError('조회 실패') }
}

const initialize = () => {
	resetForm(searchForm);
	searchForm.deptcd = authStore.deptcd; searchForm.deptnm = authStore.deptnm;
	searchForm.fymd = firstDay; searchForm.tymd = today;
	mainGrid?.clearData(); rowCount.value = 0;
	totals.smtot = 0; totals.sytot = 0;
}

const excel = () => mainGrid?.download("xlsx", "품목별거래처판매현황.xlsx")
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
	} else if (type === 'ITEM') {
		Object.assign(modalProps, {
			title: '품목 선택',
			path: '/hs00/HS00_000S_STR',
			defaultField: 'itemnm',
			data: { gubun: 'I1', cmpycd: authStore.cmpycd, gbncd: '2', code: '', remark: '' },
			columns: [
				{ title: '품목코드', field: 'itemcd', width: 100, hozAlign: 'center' },
				{ title: '품목명', field: 'itemnm', width: 200 },
				{ title: '규격', field: 'itsize', width: 150 },
				{ title: '단위', field: 'unit', width: 80, hozAlign: 'center' }
			],
			onConfirm: (d: any) => { searchForm.itemcd = d.itemcd; searchForm.itemnm = d.itemnm }
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
					title: "거래처명", field: "custnm", minWidth: 200, widthGrow: 2, hozAlign: "left", cssClass: "fw-bold text-primary cursor-pointer", frozen: true,
					formatter: (cell) => {
						const d = cell.getData();
						let name = d.custnm;
						if ((d.clsymd && d.clsymd !== '00000000' && d.clsymd <= searchForm.tymd.replace(/-/g, '')) || d.status === '030') {
							return `<span style="color: blue;">${name}</span>`;
						}
						return name;
					},
					cellClick: (e, cell) => {
						const d = cell.getData();
						router.push({ path: '/hsst/HSST140S', query: { fymd: searchForm.fymd, tymd: searchForm.tymd, deptcd: searchForm.deptcd, custcd: d.custcd, itemcd: searchForm.itemcd } });
					}
				},
				{
					title: "당월 실적 (Current Month)",
					columns: [
						{ title: "수량", field: "mqty", hozAlign: "right", width: 150, formatter: "money", formatterParams: { precision: (c:any) => c.getData().qtypnt || 0 } },
						{ title: "매출액", field: "mamt", hozAlign: "right", width: 150, formatter: "money", formatterParams: { precision: 0 } },
						{ title: "부가세", field: "mvat", hozAlign: "right", width: 150, formatter: "money", formatterParams: { precision: 0 } },
						{ title: "합계", field: "msum", hozAlign: "right", width: 150, formatter: "money", cssClass: "bg-light text-primary fw-bold",
						  mutatorData: (v,d) => Number(d.mamt||0) + Number(d.mvat||0) }
					]
				},
				{
					title: "누계 실적 (Cumulative)",
					columns: [
						{ title: "수량", field: "tqty", hozAlign: "right", width: 150, formatter: "money", formatterParams: { precision: (c:any) => c.getData().qtypnt || 0 } },
						{ title: "매출액", field: "tamt", hozAlign: "right", width: 150, formatter: "money", formatterParams: { precision: 0 } },
						{ title: "부가세", field: "tvat", hozAlign: "right", width: 150, formatter: "money", formatterParams: { precision: 0 } },
						{ title: "합계", field: "tsum", hozAlign: "right", width: 150, formatter: "money", cssClass: "bg-light text-warning fw-bold",
						  mutatorData: (v,d) => Number(d.tamt||0) + Number(d.tvat||0) }
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

