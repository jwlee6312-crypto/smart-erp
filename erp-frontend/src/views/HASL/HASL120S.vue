<!--
	=============================================================
	프로그램명	: 전표조회 (HASL120S)
	작성일자	: 2025.02.24
	설명        : 회계 전표 상세 내역 조회 및 검색
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container">
		<!-- 🚀 상단 액션 바 -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-search me-2 text-primary" style="font-size: 18px;"></i>
				전표관리 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				<span class="text-primary fw-bolder">전표조회 (HASL120S)</span>
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
								<div class="input-group input-group-sm">
									<input v-model="searchForm.acctcd" type="text" class="form-control text-center bg-light" style="max-width: 70px;" readonly />
									<input v-model="searchForm.acctnm" type="text" class="form-control" @keydown.enter="openHelp('ACCT')" />
									<button class="btn btn-outline-secondary px-2" @click="openHelp('ACCT')"><i class="bi bi-search"></i></button>
								</div>
							</td>
						</tr>
						<tr>
							<th class="text-center border-end border-top">사용부서</th>
							<td class="bg-white border-end border-top">
								<div class="input-group input-group-sm">
									<input v-model="searchForm.deptcd_h" type="text" class="form-control text-center bg-light" style="max-width: 70px;" readonly />
									<input v-model="searchForm.deptnm_h" type="text" class="form-control" @keydown.enter="openHelp('DEPT')" />
									<button class="btn btn-outline-secondary px-2" @click="openHelp('DEPT')"><i class="bi bi-search"></i></button>
								</div>
							</td>
							<th class="text-center border-end border-top">전표금액</th>
							<td class="bg-white border-top">
								<div class="d-flex align-items-center gap-1">
									<input v-model="searchForm.slipamt_f" type="number" class="form-control form-control-sm text-end" placeholder="최소" />
									<span class="text-muted">~</span>
									<input v-model="searchForm.slipamt_t" type="number" class="form-control form-control-sm text-end" placeholder="최대" />
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

// 🔍 검색 조건
const searchForm = reactive({
	fromdt: firstDay,
	todt: today,
	acctcd: '',
	acctnm: '',
	deptcd_h: authStore.deptcd,
	deptnm_h: authStore.deptnm,
	slipamt_f: '',
	slipamt_t: ''
})

const mainGridRef = ref<HTMLDivElement | null>(null)
let mainGrid: Tabulator | null = null

const search = async () => {
	try {
		const res = await api.post('/hasl/HASL_120S_STR', {
			cmpycd: authStore.cmpycd,
			fromdt: searchForm.fromdt.replace(/-/g, ''),
			todt: searchForm.todt.replace(/-/g, ''),
			acctcd: searchForm.acctcd,
			deptcd: searchForm.deptcd_h,
			slipamt_f: searchForm.slipamt_f,
			slipamt_t: searchForm.slipamt_t
		})

		const data = (res.data || []).map((row: any) => {
			return Object.fromEntries(Object.entries(row).map(([k, v]) => [k.toLowerCase(), v]))
		})
		mainGrid?.setData(data)
		vAlert('조회되었습니다.')
	} catch (e) { vAlertError('조회 실패') }
}

const initialize = () => {
	resetForm(searchForm)
	searchForm.fromdt = firstDay
	searchForm.todt = today
	searchForm.deptcd_h = authStore.deptcd
	searchForm.deptnm_h = authStore.deptnm
	mainGrid?.clearData()
}

const excel = () => mainGrid?.download("xlsx", `전표조회_${today}.xlsx`)

const goSlipDetail = (slipYmd: string, slipNo: string) => {
	router.push({
		path: '/HASL/HASL110U',
		query: { slipymd: slipYmd.replace(/-/g, ''), slipno: slipNo }
	})
}

// 팝업 설정
const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function openHelp(type: string) {
	if (type === 'ACCT') {
		Object.assign(modalProps, {
			title: '계정과목 선택', path: '/ha00/HA00_00P_STR',
			data: { gubun: 'A0', cmpycd: authStore.cmpycd, search: searchForm.acctnm },
			columns: [{ title: '코드', field: 'acctcd', width: 80 }, { title: '계정명', field: 'acctnm', width: 180 }],
			onConfirm: (d: any) => { searchForm.acctcd = d.acctcd; searchForm.acctnm = d.acctnm }
		})
	} else if (type === 'DEPT') {
		Object.assign(modalProps, {
			title: '부서 선택', path: '/ha00/HA00_00P_STR',
			data: { gubun: 'D0', cmpycd: authStore.cmpycd, code: searchForm.deptnm_h },
			columns: [{ title: '코드', field: 'deptcd', width: 80 }, { title: '부서명', field: 'deptnm', width: 180 }],
			onConfirm: (d: any) => { searchForm.deptcd_h = d.deptcd; searchForm.deptnm_h = d.deptnm }
		})
	}
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
		query: {
			slipymd: ymd,
			slipno: no,
			deptcd: searchForm.deptcd_h
		}
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
			columnDefaults: {
				headerSort: false,
				hozAlign: 'right',
				vertAlign: "middle"
			},
			columns: [
				{
					title: "전표번호", field: "slipno", width: 150, cssClass: "fw-bold", hozAlign: "center",
					formatter: (cell) => {
						const value = cell.getValue();
						if (!value) return "";
						const prevrow = cell.getRow().getPrevRow();
						if (prevrow && prevrow.getData().slipno === value) {
							return "";
						}
						return `<span class="text-primary cursor-pointer text-decoration-underline">${value}</span>`;
					},
					cellClick: (e, cell) => {
						const value = cell.getData().slipno;
						if(value) go_to_slip(String(value));
					}
				},
				{ title: "순번", field: "srowno", width: 65, hozAlign: "center" },
				{ title: "계정코드", field: "acctcd", width: 80, hozAlign: "center" },
				{ title: "계정명", field: "acctnm", width: 150, hozAlign: "left" },
				{ title: "적요", field: "descnm", minWidth: 200, hozAlign: "left" },
				{ title: "거래처", field: "subnm", width: 150, hozAlign: "left" },
				{ title: "관리번호", field: "mgtno", width: 120, hozAlign: "center" },
				{ title: "사용부서", field: "deptnm", width: 120, hozAlign: "center" },
				{ title: "차변", field: "dbamt", width: 110, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
				{ title: "대변", field: "cramt", width: 110, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } }
			]
		})
	}
})
</script>

<style scoped>
.tabulator-full-height { width: 100% !important; background-color: #fff; border-bottom: 3px solid #005a9f !important; }
.erp-label { min-width: 80px; font-weight: 500; font-size: 13px; }
.cursor-pointer { cursor: pointer; }
</style>
