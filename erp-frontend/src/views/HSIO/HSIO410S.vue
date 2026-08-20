<!--
	=============================================================
	프로그램명	: 거래처별 입금현황
	작성일자	: 2025.02.24
	작성자	    : AI Assistant
	설명        : 거래처별 입금 내역 조회 및 상세 이동
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container">
		<!-- 🚀 상단 액션 바 -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-cash-stack me-2 text-primary" style="font-size: 18px;"></i>
				영업정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				입금관리 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				<span class="text-primary fw-bolder">거래처별 입금현황 (HSIO410S)</span>
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

		<!-- 🔍 검색 항목 영역 -->
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
									<span class="erp-label me-2">입금부서</span>
									<div class="input-group input-group-sm flex-nowrap">
										<input v-model="searchForm.deptcd" type="text" class="form-control text-center bg-white" style="max-width: 60px;" readonly />
										<input v-model="searchForm.deptnm" type="text" class="form-control" placeholder="부서 선택" @input="searchForm.deptcd = ''" />
										<button class="btn btn-outline-secondary px-2" @click="openHelp('DEPT')"><i class="bi bi-search"></i></button>
									</div>
								</div>
							</td>
							<td>
								<div class="d-flex align-items-center px-2">
									<span class="erp-label me-2">입금일자</span>
									<div class="d-flex align-items-center gap-1 flex-grow-1">
										<input v-model="searchForm.fromdt" type="date" class="form-control form-control-sm" />
										<span class="text-muted">~</span>
										<input v-model="searchForm.todt" type="date" class="form-control form-control-sm" />
									</div>
								</div>
							</td>
							<td>
								<div class="d-flex align-items-center px-2">
									<span class="erp-label me-2">거 래 처</span>
									<div class="input-group input-group-sm flex-nowrap">
										<input v-model="searchForm.custcd" type="text" class="form-control text-center bg-white" style="max-width: 60px;" readonly />
										<input v-model="searchForm.custnm" type="text" class="form-control" placeholder="거래처 선택" @input="searchForm.custcd = ''" />
										<button class="btn btn-outline-secondary px-2" @click="openHelp('CUST')"><i class="bi bi-search"></i></button>
									</div>
								</div>
							</td>
							<td><!-- 균등 배분 공간 --></td>
						</tr>
					</tbody>
				</table>
			</div>
		</div>

		<!-- 📊 중앙 그리드 영역 -->
		<div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column">
			<div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white">
                <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column" style="min-height: 0;">
                  <div ref="mainGridRef" class="tabulator-full-height" />
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

const searchForm = reactive({
	deptcd: authStore.deptcd,
	deptnm: authStore.deptnm,
	fromdt: firstDay,
	todt: today,
	custcd: '',
	custnm: ''
})

const mainGridRef = ref<HTMLDivElement | null>(null);
let mainGrid: Tabulator | null = null

const search = async () => {
	if (!searchForm.deptcd) {
		vAlert('입금부서를 선택해 주십시오.');
		return;
	}
	try {
		const res = await api.post('/hsio/HSIO_410S_STR', {
			cmpycd: authStore.cmpycd,
			deptcd: searchForm.deptcd,
			custcd: searchForm.custcd,
			fromdt: searchForm.fromdt.replace(/-/g, ''),
			todt: searchForm.todt.replace(/-/g, '')
		})
		const data = res.data || []
		mainGrid?.setData(data)
		vAlert('조회되었습니다.')
	} catch (e) { vAlertError('조회 실패') }
}

const initialize = () => {
	resetForm(searchForm);
	searchForm.deptcd = authStore.deptcd;
	searchForm.deptnm = authStore.deptnm;
	searchForm.fromdt = firstDay;
	searchForm.todt = today;
	mainGrid?.clearData();
}

const excel = () => mainGrid?.download("xlsx", "거래처별입금현황.xlsx")
const print = () => {
	const params = `deptcd=${searchForm.deptcd}&deptnm=${searchForm.deptnm}&custcd=${searchForm.custcd}&custnm=${searchForm.custnm}&fromdt=${searchForm.fromdt}&todt=${searchForm.todt}`;
	window.open(`/hsio/HSIO_410P?${params}`, 'Print', 'width=1000,height=800');
}

const modalVisible = ref(false);
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function openHelp(type: string) {
	if (type === 'DEPT') {
		Object.assign(modalProps, {
			title: '부서 선택', path: '/ha00/HA00_00P_STR', defaultField: 'deptnm',
			data: { gubun: 'D0', cmpycd: authStore.cmpycd, code: searchForm.deptnm },
			columns: [{ title: '코드', field: 'deptcd', width: 80 }, { title: '부서명', field: 'deptnm', width: 180 }],
			onConfirm: (d: any) => { searchForm.deptcd = d.deptcd; searchForm.deptnm = d.deptnm }
		})
	} else if (type === 'CUST') {
		Object.assign(modalProps, {
			title: '거래처 선택', path: '/ha00/HA00_00P_STR', defaultField: 'custnm',
			data: { gubun: 'C0', cmpycd: authStore.cmpycd, search: searchForm.custnm },
			columns: [{ title: '코드', field: 'custcd', width: 80 }, { title: '거래처명', field: 'custnm', width: 180 }],
			onConfirm: (d: any) => { searchForm.custcd = d.custcd; searchForm.custnm = d.custnm }
		})
	}
	modalVisible.value = true
}

onMounted(() => {
	if (typeof window !== 'undefined') (window as any).XLSX = XLSX;
	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns', height: '100%',
			columnDefaults: {
				headerSort: false,
				headerHozAlign: "center",
				hozAlign: 'right', // 🚀 기본값을 우측 정렬로 설정
				vertAlign: 'middle',
				minWidth: 100
			},
			columns: [
				{
					title: "입금일", field: "imymd", width: 100, formatter: (cell) => {
						const val = cell.getValue();
						return val && val.length === 8 ? `${val.substring(0, 4)}-${val.substring(4, 6)}-${val.substring(6, 8)}` : val;
					}
				},
				{
					title: "거래처", field: "custnm", minWidth: 150, hozAlign: "left", cssClass: "text-primary cursor-pointer fw-bold",
					cellClick: (e, cell) => {
						const d = cell.getData();
						router.push({ path: '/HSIO/HSIO300U', query: { IMym: d.IMym, IMNO: d.IMNO, deptcd: d.deptcd } });
					}
				},
				{
					title: "전표", field: "slipyn", width: 80, formatter: (cell) => cell.getValue() === 'Y' ? '발행' : '미발행'
				},
				{ title: "현금", field: "cashamt", hozAlign: "right", width: 100, formatter: "money", formatterParams: { precision: 0 } },
				{ title: "예금", field: "bankamt", hozAlign: "right", width: 100, formatter: "money", formatterParams: { precision: 0 } },
				{ title: "카드", field: "cardamt", hozAlign: "right", width: 100, formatter: "money", formatterParams: { precision: 0 } },
				{ title: "어음", field: "billamt", hozAlign: "right", width: 100, formatter: "money", formatterParams: { precision: 0 } },
				{ title: "대체", field: "sangamt", hozAlign: "right", width: 100, formatter: "money", formatterParams: { precision: 0 } },
				{ title: "기타", field: "etcamt", hozAlign: "right", width: 100, formatter: "money", formatterParams: { precision: 0 } },
				{
					title: "입금계", field: "amttot", hozAlign: "right", width: 110, formatter: "money", formatterParams: { precision: 0 },
					cssClass: "bg-light fw-bold text-primary",
					mutatorData: (v, d) => Number(d.cashamt || 0) + Number(d.bankamt || 0) + Number(d.cardamt || 0) + Number(d.billamt || 0) + Number(d.sangamt || 0) + Number(d.etcamt || 0)
				},
				{
					title: "세부내역", field: "remark", hozAlign: "left", minWidth: 200,
					formatter: (cell) => {
						const d = cell.getData();
						const type = String(d.imtype || '');
						if (type.startsWith('2') || type.startsWith('3') || type.startsWith('6') || type.startsWith('7')) {
							return [d.mgtno, d.mgtnm].filter(Boolean).join(' ');
						} else if (type.startsWith('4')) {
							const pubYmd = d.pubymd ? `${d.pubymd.substring(0, 4)}-${d.pubymd.substring(4, 6)}-${d.pubymd.substring(6, 8)}` : '';
							const endYmd = d.endymd ? `${d.endymd.substring(0, 4)}-${d.endymd.substring(4, 6)}-${d.endymd.substring(6, 8)}` : '';
							const amt = Number(d.billamt || 0).toLocaleString();
							return [d.mgtno, d.cdnm, pubYmd, endYmd, amt, d.pubbank, d.pubman].filter(Boolean).join(' | ');
						} else if (type.startsWith('5')) {
							return d.mgtno || d.remark || '';
						}
						return d.remark || '';
					}
				}
			],
			columnCalcs: "table"
		})

		mainGrid.on("dataLoaded", () => {
			mainGrid?.setColumns(mainGrid.getColumnDefinitions().map(col => {
				if (['cashamt', 'bankamt', 'cardamt', 'billamt', 'sangamt', 'etcamt', 'amttot'].includes(col.field as string)) {
					return { ...col, bottomCalc: "sum", bottomCalcFormatter: "money", bottomCalcFormatterParams: { precision: 0 } };
				}
				if (col.field === 'imymd') {
					return { ...col, bottomCalc: () => "합 계" };
				}
				return col;
			}));
		});
	}
})
</script>

<style scoped>
.tabulator-full-height { width: 100% !important; background-color: #fff; border-bottom: 3px solid #005a9f !important; }
</style>
