<!--	=============================================================
	프로그램명 : 매출장
	작성일자	: 2025.02.24
	작성자    : AI Assistant
	설명        : 사업장별 매출 세금계산서 상세 내역 조회 (표준 UI 적용)
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container">
		<!-- [헤더] 1. 상단 액션 바 -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-journal-check me-2 text-primary" style="font-size: 18px;"></i>
				재무관리<i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				<span class="text-primary fw-bolder">매출장(HATX040S)</span>
			</div>
			<div class="btn-group-erp d-flex gap-1">
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

		<!-- [검색] 검색 조건 영역 -->
		<div class="p-2 pb-0 flex-shrink-0">
			<div class="card border shadow-sm bg-white overflow-hidden">
				<div class="card-body p-2 bg-light">
					<div class="d-flex flex-column gap-2 small">
						<div class="d-flex align-items-center flex-wrap gap-3">
							<div class="d-flex align-items-center">
								<span class="erp-label"><i class="bi bi-dot"></i>사업장</span>
								<select v-model="searchForm.saupcd" class="form-select form-select-sm" style="width: 150px;">
									<option v-for="opt in taxUnitOptions" :key="opt.code" :value="opt.code">{{ opt.name }}</option>
								</select>
							</div>
							<div class="d-flex align-items-center">
								<span class="erp-label"><i class="bi bi-dot"></i>유형</span>
								<div class="d-flex align-items-center gap-1">
									<select v-model="searchForm.gubun1" class="form-select form-select-sm" style="width: 110px;">
										<option v-for="opt in taxTypeOptions" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
									</select>
									<span>~</span>
									<select v-model="searchForm.gubun2" class="form-select form-select-sm" style="width: 110px;">
										<option v-for="opt in taxTypeOptions" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
									</select>
								</div>
							</div>
							<div class="d-flex align-items-center">
								<span class="erp-label"><i class="bi bi-dot"></i>발행일</span>
								<div class="d-flex align-items-center gap-1">
									<input v-model="searchForm.stdymd" type="date" class="form-control form-control-sm" style="width: 135px;" />
									<span>~</span>
									<input v-model="searchForm.endymd" type="date" class="form-control form-control-sm" style="width: 135px;" />
								</div>
							</div>
						</div>
						<div class="d-flex align-items-center">
							<span class="erp-label"><i class="bi bi-dot"></i>거래처</span>
							<div class="d-flex align-items-center gap-1">
								<div class="input-group input-group-sm" style="width: 220px;">
									<input v-model="searchForm.fcustcd" type="text" class="form-control text-center bg-light" style="max-width: 70px;" readonly />
									<input v-model="searchForm.fcustnm" type="text" class="form-control" @keydown.enter="openHelp('CUST', 'F')" placeholder="시작 거래처" />
									<button class="btn btn-outline-secondary px-2" @click="openHelp('CUST', 'F')"><i class="bi bi-search"></i></button>
								</div>
								<span class="mx-1">-</span>
								<div class="input-group input-group-sm" style="width: 220px;">
									<input v-model="searchForm.tcustcd" type="text" class="form-control text-center bg-light" style="max-width: 70px;" readonly />
									<input v-model="searchForm.tcustnm" type="text" class="form-control" @keydown.enter="openHelp('CUST', 'T')" placeholder="종료 거래처" />
									<button class="btn btn-outline-secondary px-2" @click="openHelp('CUST', 'T')"><i class="bi bi-search"></i></button>
								</div>
							</div>
						</div>
					</div>
				</div>
			</div>
		</div>

		<!-- [그리드] 그리드 영역 -->
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
import AppAlert from '@/components/AppAlert.vue'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useTabStore } from '@/stores/tabStore'
import Modal from '@/components/Modal.vue'
import type { ModalProps } from '@/types/modal'

const authStore = useAuthStore()
const tabStore = useTabStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()

const today = new Date().toISOString().slice(0, 10)
const firstDay = today.slice(0, 8) + '01'

const taxUnitOptions = ref<any[]>([])
const taxTypeOptions = ref<any[]>([])

const searchForm = reactive({
	saupcd: '',
	gubun1: '',
	gubun2: '',
	stdymd: firstDay,
	endymd: today,
	fcustcd: '',
	fcustnm: '',
	tcustcd: '',
	tcustnm: ''
})

const mainGridRef = ref<HTMLDivElement | null>(null)
let mainGrid: Tabulator | null = null

const fetchOptions = async () => {
	try {
		const [resU, resT] = await Promise.all([
			api.post('/ha00/HA00_00P_STR', { gubun: 'SA', cmpycd: authStore.cmpycd }),
			api.post('/ha00/HA00_00P_STR', { gubun: 'E0', cmpycd: authStore.cmpycd, gbncd: '130' })
		])
		taxUnitOptions.value = (resU.data || []).map((i: any) => ({ code: i.taxunit, name: i.unitnm }))
		if (taxUnitOptions.value.length > 0) searchForm.saupcd = taxUnitOptions.value[0].code

        taxTypeOptions.value = (resT.data || []).map((t: any) => ({ codecd: t.codecd, codenm: t.codenm }))

		if (taxTypeOptions.value.length > 0) {
			searchForm.gubun1 = taxTypeOptions.value[0].codecd
			searchForm.gubun2 = taxTypeOptions.value[taxTypeOptions.value.length - 1].codecd
		}
	} catch (e) { console.error(e) }
}

const search = async () => {
	if (!searchForm.saupcd) return vAlertError('사업장을 선택하세요.')
	if (searchForm.gubun1 > searchForm.gubun2) return vAlertError('유형 범위를 확인하세요.')

	try {
		const res = await api.post('/hatx/HATX_040S_STR', {
			cmpycd: authStore.cmpycd,
			saupcd: searchForm.saupcd,
			gubun1: searchForm.gubun1,
			gubun2: searchForm.gubun2,
			stdymd: searchForm.stdymd.replace(/-/g, ''),
			endymd: searchForm.endymd.replace(/-/g, ''),
			fcustcd: searchForm.fcustcd,
			tcustcd: searchForm.tcustcd
		})

		const data = (res.data || []).map((row: any) => ({
			pubymd: row.col0,
			custno: row.col1,
			custnm: row.col2,
			descnm: row.col3,
			supyamt: Number(row.col4 || 0),
			vatamt: Number(row.col5 || 0),
			totamt: Number(row.col6 || 0),
			tax_ref: row.col7,
			slip_ref: row.col8,
			typenm: row.col9,
			slipgu: row.slipgu,
			deptcd: row.deptcd
		}))

		mainGrid?.setData(data)
		vAlert('조회되었습니다.')
	} catch (e) { vAlertError('조회 중 오류 발생') }
}

const excel = () => {
	mainGrid?.download("xlsx", `매출장_${searchForm.stdymd}_${searchForm.endymd}.xlsx`)
}

const print = () => {
	const params = `taxunit=${searchForm.saupcd}&gubun1=${searchForm.gubun1}&gubun2=${searchForm.gubun2}&stdymd=${searchForm.stdymd}&endymd=${searchForm.endymd}&fcustcd=${searchForm.fcustcd}&tcustcd=${searchForm.tcustcd}&PRTGU=1`
	window.open(`/hatx/HATX_040P?${params}`, 'SalesRegister', 'width=1000,height=800,scrollbars=yes')
}

// --- 팝업 설정 ---
const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function openHelp(type: string, target?: string) {
	if (type === 'CUST') {
		Object.assign(modalProps, {
			title: '거래처 선택', path: '/ha00/HA00_010S_STR',
			data: { gubun: 'C0', search_gubun: '010', cmpycd: authStore.cmpycd, search: '' },
			columns: [{ title: '코드', field: 'custcd', width: 80 }, { title: '거래처명', field: 'custnm', width: 180 }],
			onConfirm: (d: any) => {
				if (target === 'F') { searchForm.fcustcd = d.custcd; searchForm.fcustnm = d.custnm }
				else { searchForm.tcustcd = d.custcd; searchForm.tcustnm = d.custnm }
			}
		})
	}
	modalVisible.value = true
}

onMounted(() => {
	fetchOptions()
	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns',
			height: '100%',
			columnDefaults: { headerSort: false, vertAlign: "middle" },
			columns: [
				{
					title: "발행일", field: "pubymd", width: 100, hozAlign: "center", frozen: true,
					formatter: (cell) => { const v = cell.getValue(); return v ? `${v.slice(0,4)}-${v.slice(4,6)}-${v.slice(6,8)}` : '' }
				},
				{ title: "사업자번호", field: "custno", width: 120, hozAlign: "center", frozen: true },
				{ title: "상호", field: "custnm", width: 250, frozen: true, cssClass: "fw-bold" },
				{ title: "유형", field: "typenm", width: 150, hozAlign: "center" },
				{ title: "적요", field: "descnm", widthGrow: 1.5 },
				{
					title: "공급가", field: "supyamt", width: 120, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 },
					bottomCalc: "sum", bottomCalcFormatter: "money", bottomCalcFormatterParams: { precision: 0 }
				},
				{
					title: "부가세", field: "vatamt", width: 120, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 },
					bottomCalc: "sum", bottomCalcFormatter: "money", bottomCalcFormatterParams: { precision: 0 }
				},
				{
					title: "합계", field: "totamt", width: 120, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 },
					bottomCalc: "sum", bottomCalcFormatter: "money", bottomCalcFormatterParams: { precision: 0 },
					cssClass: "text-primary fw-bold"
				},
				{
					title: "번호", field: "tax_ref", width: 150, hozAlign: "center",
					formatter: (cell) => cell.getValue() ? `<span class="text-primary text-decoration-underline" style="cursor:pointer;">${cell.getValue()}</span>` : '',
					cellClick: (e, cell) => {
						const d = cell.getData()
						if (!d.tax_ref) return
						const ym = d.tax_ref.substring(0, 6)
						const no = d.tax_ref.substring(7, 11)
						tabStore.addTab({
							name: '매출부가세등록',
							path: '/HATX/HATX010U',
							params: { taxym: ym, taxno: no, taxkind: '20' }
						})
					}
				},
				{
					title: "전표번호", field: "slip_ref", width: 150, hozAlign: "center",
					formatter: (cell) => cell.getValue() ? `<span class="text-primary text-decoration-underline" style="cursor:pointer;">${cell.getValue()}</span>` : '',
					cellClick: (e, cell) => {
						const d = cell.getData()
						if (!d.slip_ref) return
						const parts = d.slip_ref.split('-')
						const path = d.slipgu === '010' ? '/HASL/HASL010U' : '/HASL/HASL110U'
						tabStore.addTab({
							name: d.slipgu === '010' ? '회계전표관리' : '일반전표관리',
							path: path,
							params: { slipymd: parts[0], slipno: parts[1], deptcd: d.deptcd }
						})
					}
				}
			]
		})
	}
})
</script>

<style scoped>
.erp-label { min-width: 80px; font-weight: 500; font-size: 13px; }
:deep(.tabulator-cell) { border-right: 1px solid #dee2e6 !important; font-size: 12px; }
:deep(.tabulator-header .tabulator-col) { border-right: 1px solid #dee2e6 !important; background-color: #f8f9fa !important; font-size: 12px; }
:deep(.tabulator-footer .tabulator-calcs) { background-color: #f0f7ff !important; font-weight: bold; font-size: 12px; }
</style>
