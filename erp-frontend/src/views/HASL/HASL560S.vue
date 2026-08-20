<!--
	=============================================================
	프로그램명: 보조원장-관리번호 (HASL560S)
	작성일자	: 2025.03.14
	작성자    : AI Assistant
	설명        : 특정 계정과목 및 관리번호별 이월, 발생 상세 내역 조회
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container d-flex flex-column h-100 bg-white">
		<!-- [헤더] 상단 액션 바 -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-tags me-2 text-primary" style="font-size: 18px;"></i>
				회계관리 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				<span class="text-primary fw-bolder">보조원장-관리번호 (HASL560S)</span>
			</div>
			<div class="btn-group-erp d-flex gap-1">
				<button class="btn-erp btn-search" @click="search">
					<i class="bi bi-search"></i> 조회
				</button>
				<button class="btn-erp btn-print" @click="print">
					<i class="bi bi-printer"></i> 인쇄
				</button>
			</div>
		</div>

		<!-- [조회] 검색 조건 영역 -->
		<div class="p-2 pb-0 flex-shrink-0 bg-light">
			<div class="card border shadow-sm bg-white overflow-hidden">
				<div class="card-body p-2">
					<div class="d-flex align-items-center flex-wrap gap-3 small">
						<!-- 계정과목 -->
						<div class="d-flex align-items-center">
							<span class="erp-label"><i class="bi bi-dot"></i>계정과목</span>
							<div class="input-group input-group-sm" style="width: 250px;">
								<input v-model="searchForm.acctcd" type="text" class="form-control text-center bg-light fw-bold" style="max-width: 70px;" readonly />
								<input v-model="searchForm.acctnm" type="text" class="form-control" @keydown.enter="openHelp('ACCT')" placeholder="계정명 입력" />
								<button class="btn btn-outline-secondary px-2" @click="openHelp('ACCT')"><i class="bi bi-search"></i></button>
							</div>
						</div>
						<!-- 관리번호 -->
						<div class="d-flex align-items-center">
							<span class="erp-label"><i class="bi bi-dot"></i>관리번호</span>
							<div class="input-group input-group-sm" style="width: 250px;">
								<input v-model="searchForm.mgtno" type="text" class="form-control" @keydown.enter="openHelp('MGT')" placeholder="관리번호 입력" />
								<button class="btn btn-outline-secondary px-2" @click="openHelp('MGT')"><i class="bi bi-search"></i></button>
							</div>
						</div>
						<!-- 회계일자 -->
						<div class="d-flex align-items-center">
							<span class="erp-label"><i class="bi bi-dot"></i>회계일자</span>
							<div class="d-flex align-items-center gap-1">
								<input v-model="searchForm.fromdt" type="date" class="form-control form-control-sm" style="width: 140px;" />
								<span class="text-muted">~</span>
								<input v-model="searchForm.todt" type="date" class="form-control form-control-sm" style="width: 140px;" />
							</div>
						</div>
					</div>
				</div>
			</div>
		</div>

		<!-- [그리드] 데이터 영역 -->
		<div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column bg-light">
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
import { useRouter, useRoute } from 'vue-router'
import Modal from '@/components/Modal.vue'
import type { ModalProps } from '@/types/modal'

const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()

const now = new Date()
const firstDay = new Date(now.getFullYear(), now.getMonth(), 1).toISOString().substring(0, 10)
const today = now.toISOString().substring(0, 10)

// 검색 조건
const searchForm = reactive({
	acctcd: (route.query.acctcd as string) || '',
	acctnm: '',
	mgtno: (route.query.mgtno as string) || '',
	mgtnm: '',
	mgtgbn: '',
	fromdt: (route.query.fromdt as string) || firstDay,
	todt: (route.query.todt as string) || today
})

const mainGridRef = ref<HTMLDivElement | null>(null)
let mainGrid: Tabulator | null = null



const search = async () => {
	if (!searchForm.acctcd) return vAlertError('계정과목을 선택해 주십시오.')
	if (!searchForm.mgtno) return vAlertError('관리번호를 입력해 주십시오.')

	try {
		const res = await api.post('/hasl/HASL_560S_STR', {
			cmpycd: authStore.cmpycd,
			acctcd: searchForm.acctcd,
			mgtno: searchForm.mgtno,
			fromdt: searchForm.fromdt.replace(/-/g, ''),
			todt: searchForm.todt.replace(/-/g, '')
		})

		const rawData = res.data || []
		const processedData: any[] = []

		if (rawData.length > 0) {
			let carryRow = rawData[0]
			let dbMmTot = Number(carryRow.dbamt || 0)
			let crMmTot = Number(carryRow.cramt || 0)
			let dbcr = carryRow.dbcr // 'D' or 'C'
			let janAmt = dbcr === 'D' ? (dbMmTot - crMmTot) : (crMmTot - dbMmTot)

			processedData.push({
				acctymd: '',
				descnm: carryRow.descnm,
				slipno: '',
				dbamt: dbMmTot,
				cramt: crMmTot,
				janamt: janAmt,
				is_summary: true
			})

			let i = 1
			while (i < rawData.length) {
				let currentYm = String(rawData[i].acctymd).substring(0, 6)
				let dbMmAmt = 0
				let crMmAmt = 0

				while (i < rawData.length && String(rawData[i].acctymd).substring(0, 6) === currentYm) {
					const row = rawData[i]
					const db = Number(row.dbamt || 0)
					const cr = Number(row.cramt || 0)

					if (dbcr === 'D') janAmt += (db - cr)
					else janAmt += (cr - db)

					let details: string[] = []
					if (row.deptnm) details.push(String(row.deptnm).trim())
					if (row.subnm) details.push(String(row.subnm).trim())
					if (row.mgtno) details.push(String(row.mgtno).trim())
					if (row.bugtnm) details.push(String(row.bugtnm).trim())
					if (row.prjnm) details.push(String(row.prjnm).trim())

					const mgtFields = ['issubank', 'issuman', 'stdymd', 'endymd', 'sname']
					mgtFields.forEach(f => {
						const val = row[f]
						if (val && String(val).trim().length > 0 && val !== '00000000') details.push(String(val).trim())
					})

					if (Number(row.srvamt) !== 0) details.push(new Intl.NumberFormat().format(row.srvamt))
					if (Number(row.bongamt) !== 0) details.push(new Intl.NumberFormat().format(row.bongamt))

					if (row.frgnyn === 'Y') {
						if (row.frgnnm) details.push(String(row.frgnnm).trim())
						if (Number(row.frgnrate) !== 0) details.push(new Intl.NumberFormat().format(row.frgnrate))
						if (Number(row.frgnamt) !== 0) details.push(new Intl.NumberFormat().format(row.frgnamt))
					}

					processedData.push({
						acctymd: row.acctymd,
						descnm: row.descnm + (row.sslipno && String(row.sslipno).trim() !== '' ? ` (${row.sslipno})` : ''),
						slipno: row.slipno,
						dbamt: db,
						cramt: cr,
						janamt: janAmt,
						detail_str: details.join(' | '),
						is_data: true
					})

					dbMmAmt += db
					crMmAmt += cr
					i++
				}

				processedData.push({
					acctymd: '',
					descnm: '월  계',
					dbamt: dbMmAmt,
					cramt: crMmAmt,
					janamt: null,
					is_monthly: true
				})

				dbMmTot += dbMmAmt
				crMmTot += crMmAmt

				processedData.push({
					acctymd: '',
					descnm: '누  계',
					dbamt: dbMmTot,
					cramt: crMmTot,
					janamt: janAmt,
					is_total: true
				})
			}
		}

		mainGrid?.setData(processedData)
		if (processedData.length > 0) vAlert('조회되었습니다.')
		else vAlert('데이터가 존재하지 않습니다.')
	} catch (e) {
		vAlertError('조회 중 오류 발생')
	}
}

// 팝업 설정
const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function openHelp(type: 'ACCT' | 'MGT') {
	if (type === 'ACCT') {
		Object.assign(modalProps, {
			title: '계정과목 선택', path: '/ha00/HA00_00P_STR', defaultField: 'acctnm',
			data: { gubun: 'A2', cmpycd: authStore.cmpycd, code: searchForm.acctnm },
			columns: [{ title: '코드', field: 'acctcd', width: 80 }, { title: '계정명', field: 'acctnm', width: 180 }, { title: '구분', field: 'typemgt', width: 50, visible: false }],
			onConfirm: (d: any) => {
                const item = d
				searchForm.acctcd = item.acctcd
				searchForm.acctnm = item.acctnm
				searchForm.mgtgbn = item.typemgt || ''
				searchForm.mgtno = ''
			}
		})
	} else {
		if (!searchForm.acctcd) return vAlertError('계정과목을 먼저 선택해 주세요.')
		Object.assign(modalProps, {
			title: '관리번호 선택', path: '/ha00/HA00_00P_STR', defaultField: 'mgtno',
			data: { gubun: 'M0', cmpycd: authStore.cmpycd, gbncd: searchForm.mgtgbn, code: searchForm.mgtnm, remark: searchForm.acctcd },
			columns: [{ title: '관리번호', field: 'mgtno', width: 150 }, { title: '명칭/적요', field: 'mgtnm', width: 250 }],
			onConfirm: (d: any) => {
                const item = d
				searchForm.mgtno = item.mgtno
				search()
			}
		})
	}
	modalVisible.value = true
}

const print = () => {
	if (!searchForm.acctcd || !searchForm.mgtno) return vAlertError('계정 및 관리번호를 먼저 선택해 주세요.')
	const params = `acctcd=${searchForm.acctcd}&acctnm=${searchForm.acctnm}&mgtno=${searchForm.mgtno}&fromdt=${searchForm.fromdt.replace(/-/g, '')}&todt=${searchForm.todt.replace(/-/g, '')}&PRTGU=1`
	window.open(`/hasl/HASL_560P?${params}`, 'ManagementLedgerPrint', 'width=800,height=800,scrollbars=yes')
}

const goSlipDetail = (slipNo: string) => {
	if (!slipNo || slipNo.length < 10) return
	const ymd = slipNo.substring(0, 8)
	const no = slipNo.substring(9)
	router.push({
		path: '/HASL/HASL110U',
		query: { slipymd: ymd, slipno: no }
	})
}

onMounted(async () => {
	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns',
			height: '100%',
			columnDefaults: { headerSort: false, vertAlign: "middle" },
			columns: [
				{
					title: "일자", field: "acctymd", width: 90, hozAlign: "center",
					formatter: (cell) => {
						const v = cell.getValue()
						return v && v.length === 8 ? `${v.substring(2, 4)}.${v.substring(4, 6)}.${v.substring(6, 8)}` : ''
					}
				},
				{
					title: "적요 / 상세내역", field: "descnm", widthGrow: 2.5,
					formatter: (cell) => {
						const d = cell.getData()
						if (d.is_data) {
							return `<div>${cell.getValue()}</div><div class="small text-secondary fw-normal mt-1" style="font-size: 11px;">${d.detail_str}</div>`
						}
						return cell.getValue()
					}
				},
				{
					title: "전표번호", field: "slipno", width: 120, hozAlign: "center",
					formatter: (cell) => {
						const v = cell.getValue()
						return v ? `<span class="text-primary text-decoration-underline cursor-pointer">${v}</span>` : ''
					},
					cellClick: (e, cell) => {
						goSlipDetail(cell.getValue())
					}
				},
				{
					title: "차변", field: "dbamt", width: 120, hozAlign: "right",
					formatter: "money", formatterParams: { precision: 0 }
				},
				{
					title: "대변", field: "cramt", width: 120, hozAlign: "right",
					formatter: "money", formatterParams: { precision: 0 }
				},
				{
					title: "잔액", field: "janamt", width: 120, hozAlign: "right",
					formatter: "money", formatterParams: { precision: 0 }
				}
			],
			rowFormatter: (row) => {
				const d = row.getData()
				if (d.is_monthly || d.is_total || d.is_summary) {
					row.getElement().style.backgroundColor = "#f8f9fa"
					row.getElement().style.fontWeight = "bold"
				}
				if (d.is_total) {
					row.getElement().style.borderBottom = "2px solid #dee2e6"
				}
			}
		})
	}

	if (searchForm.acctcd) {
		try {
			const res = await api.post('/haba/HABA_010T_GET', {
				cmpycd: authStore.cmpycd,
				acctcd: searchForm.acctcd
			})
			if (res.data?.length > 0) {
                const item = res.data[0]
				searchForm.acctnm = item.acctnm
				searchForm.mgtgbn = item.typemgt
				if (searchForm.mgtno) search()
			}
		} catch (e) {}
	}
})
</script>

<style scoped>
.erp-label { min-width: 80px; font-weight: 500; font-size: 13px; }
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
