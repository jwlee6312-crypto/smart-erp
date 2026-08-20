<!--
	=============================================================
	프로그램명	: 보조원장-거래처 (HASL550S)
	작성일자	: 2025.02.24
	작성자	    : AI Assistant
	설명        : 계정과목 및 거래처별 보조원장 조회
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container">
		<!-- 🚀 상단 액션 바 -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-journal-text me-2 text-primary" style="font-size: 18px;"></i>
				회계관리 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				장부관리 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				<span class="text-primary fw-bolder">보조원장-거래처 (HASL550S)</span>
			</div>
			<div class="btn-group-erp d-flex gap-1">
				<button class="btn-erp btn-init" @click="initialize">
					<i class="bi bi-arrow-clockwise"></i> 초기화
				</button>
				<button class="btn-erp btn-search" @click="search">
					<i class="bi bi-search"></i> 조회
				</button>
				<button class="btn-erp btn-print" @click="printReport('Print')">
					<i class="bi bi-printer"></i> 인쇄
				</button>
				<button class="btn-erp btn-excel" @click="exportExcel">
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
						<col style="width: 100px;" /><col />
					</colgroup>
					<tbody>
						<tr>
							<th class="text-center border-end required">계정과목</th>
							<td class="bg-white border-end px-2">
								<div class="input-group input-group-sm">
									<input v-model="searchForm.acctcd" type="text" class="form-control text-center bg-light" style="max-width: 70px;" readonly />
									<input v-model="searchForm.acctnm" type="text" class="form-control" placeholder="계정 선택" @keydown.enter="handleOpenHelp('ACCT')" />
									<button class="btn btn-outline-secondary px-2" @click="handleOpenHelp('ACCT')"><i class="bi bi-search"></i></button>
								</div>
							</td>
							<th class="text-center border-end required">거래처</th>
							<td class="bg-white border-end px-2">
								<div class="input-group input-group-sm">
									<input v-model="searchForm.custcd" type="text" class="form-control text-center bg-light" style="max-width: 80px;" readonly />
									<input v-model="searchForm.custnm" type="text" class="form-control" placeholder="거래처 선택" @keydown.enter="handleOpenHelp('CUST')" />
									<button class="btn btn-outline-secondary px-2" @click="handleOpenHelp('CUST')"><i class="bi bi-search"></i></button>
								</div>
							</td>
							<th class="text-center border-end required">회계일자</th>
							<td class="bg-white px-2">
								<div class="d-flex align-items-center gap-1">
									<input v-model="searchForm.fromdt" type="date" class="form-control form-control-sm" />
									<span class="text-muted">~</span>
									<input v-model="searchForm.todt" type="date" class="form-control form-control-sm" />
								</div>
							</td>
						</tr>
					</tbody>
				</table>
			</div>
		</div>

		<!-- 📊 조회 그리드 -->
		<div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column">
			<div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white">
				<div class="card-header py-1 px-2 bg-light d-flex justify-content-between align-items-center border-bottom text-secondary">
					<span class="fw-bold small"><i class="bi bi-list-ul me-1"></i> 원장 내역</span>
					<span class="badge bg-primary" v-if="rowCount > 0">총 {{ rowCount }} 건</span>
				</div>
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
import { useFormReset } from '@/composables/useFormReset'
import { useCommonHelp } from '@/composables/useCommonHelp'
import { getDate } from '@/composables/useDate'
import Modal from '@/components/Modal.vue'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps, openHelp } = useCommonHelp()
const { today, firstDayOfMonth } = getDate()

const searchForm = reactive({
	acctcd: '',
	acctnm: '',
	custcd: '',
	custnm: '',
	custgbn: '',
	fromdt: firstDayOfMonth,
	todt: today
})

const mainGridRef = ref<HTMLDivElement | null>(null)
let mainGrid: Tabulator | null = null
const rowCount = ref(0)

const formatMoney = (val: any) => Number(val || 0).toLocaleString()
const formatYmdShort = (v: string) => v && v.length === 8 ? `${v.substring(2, 4)}.${v.substring(4, 6)}.${v.substring(6, 8)}` : v



const search = async () => {
	if (!searchForm.acctcd) return vAlertError('계정과목을 선택하십시오.')
	if (!searchForm.custcd) return vAlertError('거래처를 선택하십시오.')

	try {
		const res = await api.post('/hasl/HASL_550S_STR', {
			cmpycd: authStore.cmpycd,
			acctcd: searchForm.acctcd,
			custcd: searchForm.custcd,
			fromdt: searchForm.fromdt.replace(/-/g, ''),
			todt: searchForm.todt.replace(/-/g, '')
		})

		const rawData = res.data || []
		const processedData: any[] = []
		let janAmt = 0

		if (rawData.length > 0) {
			const baseRow = rawData[0]
			const dbMmTot = Number(baseRow.col8 || 0)
			const crMmTot = Number(baseRow.col9 || 0)
			const dbcr = baseRow.col10
			janAmt = (dbcr === 'D') ? (dbMmTot - crMmTot) : (crMmTot - dbMmTot)

			processedData.push({
				acctymd: '',
				descnm: baseRow.col1,
				slipno: '',
				dbamt: dbMmTot,
				cramt: crMmTot,
				janamt: janAmt,
				temp: '',
				is_special: true
			})

			let monthlyDb = 0
			let monthlyCr = 0
			let currentYm = rawData[0].col0 ? String(rawData[0].col0).substring(0, 6) : ''

			for (let i = 0; i < rawData.length; i++) {
				const row = rawData[i]
				if (!row.col0) continue

				const rowYm = String(row.col0).substring(0, 6)

				if (currentYm !== rowYm) {
					processedData.push({ descnm: '월 계', dbamt: monthlyDb, cramt: monthlyCr, is_subtotal: true })
					monthlyDb = 0; monthlyCr = 0
					currentYm = rowYm
				}

				const dbamt = Number(row.col8 || 0)
				const cramt = Number(row.col9 || 0)

				if (row.col10 === 'D') janAmt += (dbamt - cramt)
				else janAmt += (cramt - dbamt)

				let temp = row.col4 || ''
				if (row.col3) temp += ` | ${row.col3}`
				if (row.col7) temp += ` | ${row.col7}`
				if (row.col5) temp += ` | ${row.col5}`
				if (row.col6) temp += ` | ${row.col6}`

				processedData.push({
					acctymd: row.col0,
					descnm: row.col1 + (row.col11 ? ` (${row.col11})` : ''),
					slipno: row.col2,
					dbamt: dbamt,
					cramt: cramt,
					janamt: janAmt,
					temp: temp
				})

				monthlyDb += dbamt
				monthlyCr += cramt
			}

			processedData.push({ descnm: '월 계', dbamt: monthlyDb, cramt: monthlyCr, is_subtotal: true })
		}

		mainGrid?.setData(processedData)
		rowCount.value = rawData.length
		vAlert('조회되었습니다.')
	} catch (e) { vAlertError('조회 실패') }
}

const handleOpenHelp = (type: string) => {
	if (type === 'ACCT') {
		openHelp('ACCT', (d: any) => {
			searchForm.acctcd = d.acctcd;
			searchForm.acctnm = d.acctnm;
			searchForm.custgbn = d.typesub || '';
			searchForm.custcd = ''; searchForm.custnm = '';
		}, { code: searchForm.acctnm })
	} else if (type === 'CUST') {
		if (!searchForm.acctcd) return vAlertError('계정과목을 먼저 선택하십시오.')
		openHelp('CUST', (d: any) => {
			searchForm.custcd = d.custcd;
			searchForm.custnm = d.custnm;
		}, { custgbn: searchForm.custgbn, code: searchForm.custnm })
	}
}

const initialize = () => {
	resetForm(searchForm)
	searchForm.fromdt = firstDayOfMonth
	searchForm.todt = today
	mainGrid?.clearData()
	rowCount.value = 0
}

const printReport = (prtgu: string) => {
	if (!searchForm.acctcd || !searchForm.custcd) return vAlertError('조회 조건을 완료하십시오.')
	const url = `/hasl/HASL_550P?ACCTCD=${searchForm.acctcd}&ACCTNM=${searchForm.acctnm}&custcd=${searchForm.custcd}&custnm=${searchForm.custnm}&FRYMD=${searchForm.fromdt.replace(/-/g, '')}&TOYMD=${searchForm.todt.replace(/-/g, '')}&PRTGU=${prtgu}`
	window.open(url, 'ledger_print', 'width=800,height=800,scrollbars=yes')
}

const exportExcel = () => {
	mainGrid?.download("xlsx", `보조원장_거래처_${today}.xlsx`)
}

onMounted(() => {
	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns', height: '100%',
			columnDefaults: { headerSort: false, vertAlign: "middle" },
			columns: [
				{ title: "일자", field: "acctymd", width: 100, hozAlign: "center", formatter: (c) => formatYmdShort(c.getValue()) },
				{
					title: "적요 / 상세내역", field: "descnm", minWidth: 300,
					formatter: (c) => {
						const d = c.getData();
						if (d.is_special || d.is_subtotal) return `<span class="fw-bold">${d.descnm}</span>`;
						return `<div class="d-flex flex-column">
									<div class="fw-bold text-dark">${d.descnm}</div>
									<div class="small text-muted opacity-75">${d.temp || ''}</div>
								</div>`
					}
				},
				{ title: "전표번호", field: "slipno", width: 130, hozAlign: "center", cssClass: "text-primary" },
				{ title: "차변", field: "dbamt", width: 120, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
				{ title: "대변", field: "cramt", width: 120, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
				{ title: "잔액", field: "janamt", width: 130, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "bg-light-subtle fw-bold" }
			],
			rowFormatter: (row) => {
				const d = row.getData()
				if (d.is_special) row.getElement().style.backgroundColor = "#f8f9fa"
				if (d.is_subtotal) row.getElement().style.backgroundColor = "#eef2ff"
			}
		})
	}
})
</script>

<style scoped>
.tabulator-instance { font-size: 13px; }
.bg-light-subtle { background-color: #f8f9fa !important; }
</style>
