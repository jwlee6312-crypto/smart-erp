<!--
	=============================================================
	프로그램명	: 분개장(전체) (HASL130S)
	작성일자	: 2025.02.24
	작성자	    : AI Assistant
	설명        : 회계 전표의 분개 내역을 전표별로 그룹화하여 조회
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container">
		<!-- 🚀 상단 액션 바 -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-journal-text me-2 text-primary" style="font-size: 18px;"></i>
				전표관리 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				<span class="text-primary fw-bolder">분개장(전체) (HASL130S)</span>
			</div>
			<div class="btn-group-erp d-flex gap-1">
				<button class="btn-erp btn-init" @click="initialize">
					<i class="bi bi-arrow-clockwise"></i> 초기화
				</button>
				<button class="btn-erp btn-search" @click="search">
					<i class="bi bi-search"></i> 조회
				</button>
				<button class="btn-erp btn-print" @click="print">
					<i class="bi bi-printer"></i> 인쇄
				</button>
				<button class="btn-erp btn-excel" @click="excel">
					<i class="bi bi-file-earmark-excel"></i> 엑셀
				</button>
			</div>
		</div>

		<!-- 🔍 검색 조건 영역 -->
		<div class="p-2 pb-0 flex-shrink-0">
			<div class="card border shadow-sm overflow-hidden bg-light">
				<table class="erp-table-full" style="table-layout: fixed;">
					<colgroup>
						<col style="width: 100px;" /><col />
						<col style="width: 100px;" /><col />
					</colgroup>
					<tbody>
						<tr>
							<th class="text-center border-end">회계일자</th>
							<td class="bg-white border-end">
								<div class="d-flex align-items-center gap-1">
									<input v-model="searchForm.fromdt" type="date" class="form-control form-control-sm" />
									<span class="text-muted">~</span>
									<input v-model="searchForm.todt" type="date" class="form-control form-control-sm" />
								</div>
							</td>
							<th class="text-center border-end">계정과목</th>
							<td class="bg-white">
								<div class="d-flex align-items-center gap-1">
									<div class="input-group input-group-sm" style="width: 48%;">
										<input v-model="searchForm.acctcd1" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
										<input v-model="searchForm.acctnm1" type="text" class="form-control" @keydown.enter="openHelp('ACCT1')" />
										<button class="btn btn-outline-secondary px-2" @click="openHelp('ACCT1')"><i class="bi bi-search"></i></button>
									</div>
									<span class="text-muted">~</span>
									<div class="input-group input-group-sm" style="width: 48%;">
										<input v-model="searchForm.acctcd2" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
										<input v-model="searchForm.acctnm2" type="text" class="form-control" @keydown.enter="openHelp('ACCT2')" />
										<button class="btn btn-outline-secondary px-2" @click="openHelp('ACCT2')"><i class="bi bi-search"></i></button>
									</div>
								</div>
							</td>
						</tr>
					</tbody>
				</table>
			</div>
		</div>

		<!-- 📊 그리드 영역 -->
		<div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column">
			<div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white">
                <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column" style="min-height: 0;">
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
import { addDynamicRoute as add_dynamic_route } from '@/router/dynamicRoute'
import Modal from '@/components/Modal.vue'
import AppAlert from '@/components/AppAlert.vue'
import type { ModalProps } from '@/types/modal'

const authStore = useAuthStore()
const router = useRouter()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

const now = new Date()
const firstDay = new Date(now.getFullYear(), now.getMonth(), 1).toISOString().substring(0, 10)
const today = now.toISOString().substring(0, 10)

const searchForm = reactive({
	fromdt: firstDay,
	todt: today,
	acctcd1: '',
	acctnm1: '',
	acctcd2: '',
	acctnm2: ''
})

const mainGridRef = ref<HTMLDivElement | null>(null)
let mainGrid: Tabulator | null = null



const search = async () => {
	try {
		const res = await api.post('/hasl/HASL_130S_STR', {
			cmpycd: authStore.cmpycd,
			fromdt: searchForm.fromdt.replace(/-/g, ''),
			todt: searchForm.todt.replace(/-/g, ''),
			acctcd1: searchForm.acctcd1,
			acctcd2: searchForm.acctcd2
		})

		mainGrid?.setData(res.data || [])
		vAlert('조회되었습니다.')
	} catch (e) { vAlertError('조회 실패') }
}

const initialize = () => {
	resetForm(searchForm)
	searchForm.fromdt = firstDay
	searchForm.todt = today
	mainGrid?.clearData()
}

const excel = () => mainGrid?.download("xlsx", `분개장_${today}.xlsx`)
const print = () => {
	const params = `fromdt=${searchForm.fromdt}&todt=${searchForm.todt}&acctcd1=${searchForm.acctcd1}&acctcd2=${searchForm.acctcd2}`
	window.open(`/hasl/HASL_130P?${params}`, 'JournalPrint', 'width=800,height=800,scrollbars=yes')
}

const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function openHelp(type: string) {
	const isFirst = type === 'ACCT1'
	Object.assign(modalProps, {
		title: '계정과목 선택', path: '/ha00/HA00_00P_STR', defaultField: 'acctnm',
		data: { gubun: 'A0', cmpycd: authStore.cmpycd, search: isFirst ? searchForm.acctnm1 : searchForm.acctnm2 },
		columns: [{ title: '코드', field: 'acctcd', width: 80 }, { title: '계정명', field: 'acctnm', width: 180 }],
		onConfirm: (d: any) => {
			if (isFirst) { searchForm.acctcd1 = d.acctcd; searchForm.acctnm1 = d.acctnm }
			else { searchForm.acctcd2 = d.acctcd; searchForm.acctnm2 = d.acctnm }
		}
	})
	modalVisible.value = true
}

const go_to_slip = (slipkey: string) => {
	if (!slipkey) return;
	let ymd = ""; let no = "";
	if (slipkey.includes('-')) {
		const parts = slipkey.split('-');
		ymd = parts[0]; no = parts[1];
	} else if (slipkey.length >= 11) {
		ymd = slipkey.substring(0, 8); no = slipkey.substring(8);
	} else return;

	const pgmid = 'HASL110U'
	add_dynamic_route(pgmid, '경리전표등록', 'HASL')
	router.push({
		path: `/${pgmid}`,
		query: { slipymd: ymd, slipno: no, deptcd: authStore.deptcd }
	})
}

onMounted(() => {
	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns',
			height: '100%',
			pagination: "local",
			paginationSize: 50,
			paginationSizeSelector: [20, 50, 100, 500],
			paginationCounter: "rows",
			groupBy: "slipno",
			groupHeader: function(value, count, data, group) {
				return `<span class="text-primary fw-bold cursor-pointer" onclick="window.dispatchEvent(new CustomEvent('go-slip', {detail: '${value}'}))">전표번호: ${value}</span> <span class="ms-3 small text-muted">(${count}개 항목)</span>`
			},
			columnDefaults: { headerSort: false, vertAlign: "middle" },
			columns: [
				{
					title: "전표번호", field: "slipno", width: 150, cssClass: "fw-bold",
					formatter: (cell) => {
						const value = cell.getValue() || '';
						if (!value) return "";
						const prevrow = cell.getRow().getPrevRow();
						if (prevrow && prevrow.getData().slipno === value) return "";
						return `<span class="text-primary cursor-pointer text-decoration-underline">${value}</span>`;
					},
					cellClick: (e, cell) => {
						const value = cell.getData().slipno || '';
						if(value) go_to_slip(String(value));
					}
				},
				{ title: "순번", field: "srowno", width: 65, hozAlign: "center" },
				{ title: "계정코드", field: "acctcd", width: 90, hozAlign: "center" },
				{ title: "계정명", field: "acctnm", width: 150 },
				{
					title: "적요 / 세부내역", field: "remark", minWidth: 300,
					formatter: (cell) => {
						const d = cell.getData();
						let details = [];
						const fields = [7, 5, 6, 11, 12, 10, 14, 15, 16, 17, 18, 19, 21, 22, 23, 24, 25, 26];
						fields.forEach(idx => {
							const val = d['col' + idx];
							if (val && String(val).trim() !== '' && String(val).trim() !== '00000000') {
								details.push(String(val).trim());
							}
						});
						const detailStr = details.join(' | ');
						return `<div>${d.remark || ''}</div><div class="small text-secondary fw-normal mt-1" style="font-size: 11px;">${detailStr}</div>`
					}
				},
				{
					title: "회계일", field: "acctymd", width: 90, hozAlign: "center",
					formatter: (cell) => {
						const v = cell.getValue() || ''
						return v.length === 8 ? `${v.substring(2, 4)}.${v.substring(4, 6)}.${v.substring(6, 8)}` : v
					}
				},
				{ title: "차변", field: "dbamt", width: 120, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, bottomCalc: "sum" },
				{ title: "대변", field: "cramt", width: 120, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, bottomCalc: "sum" }
			]
		})

		window.addEventListener('go-slip', ((e: CustomEvent) => {
			go_to_slip(e.detail)
		}) as EventListener)
	}
})
</script>

<style scoped>
.erp-label { min-width: 80px; font-weight: 500; font-size: 13px; }
:deep(.tabulator-group) { background-color: #f8f9fa !important; border-top: 1px solid #dee2e6 !important; }
.cursor-pointer { cursor: pointer; }
</style>
