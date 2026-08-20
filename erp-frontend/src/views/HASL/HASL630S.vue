<!--
	=============================================================
	프로그램명: 관리번호별 증감잔액명세서 (HASL630S)
	작성일자	: 2025.03.14
	작성자    : AI Assistant
	설명        : 특정 계정과목에 대하여 관리번호별 전기이월, 증가, 감소, 잔액 조회
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container d-flex flex-column h-100 bg-white">
		<!-- [헤더] 상단 액션 바 -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-tag me-2 text-primary" style="font-size: 18px;"></i>
				회계관리 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				<span class="text-primary fw-bolder">관리번호별 증감잔액명세서 (HASL630S)</span>
			</div>
			<div class="btn-group-erp d-flex gap-1 pe-3">
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
						<!-- 관리번호 범위 -->
						<div class="d-flex align-items-center">
							<span class="erp-label"><i class="bi bi-dot"></i>관리번호</span>
							<div class="d-flex align-items-center gap-1">
								<div class="input-group input-group-sm" style="width: 220px;">
									<input v-model="searchForm.mgtnofr" type="text" class="form-control" @keydown.enter="openHelp('MGTFR')" placeholder="시작 관리번호" />
									<button class="btn btn-outline-secondary px-2" @click="openHelp('MGTFR')"><i class="bi bi-search"></i></button>
								</div>
								<span class="text-muted">~</span>
								<div class="input-group input-group-sm" style="width: 220px;">
									<input v-model="searchForm.mgtnoto" type="text" class="form-control" @keydown.enter="openHelp('MGTTO')" placeholder="종료 관리번호" />
									<button class="btn btn-outline-secondary px-2" @click="openHelp('MGTTO')"><i class="bi bi-search"></i></button>
								</div>
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
import { addDynamicRoute } from '@/router/dynamicRoute'
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
	mgtgbn: '',
	mgtnofr: '',
	mgtnmfr: '',
	mgtnoto: '',
	mgtnmto: '',
	fromdt: (route.query.fromdt as string) || firstDay,
	todt: (route.query.todt as string) || today
})

const mainGridRef = ref<HTMLDivElement | null>(null)
let mainGrid: Tabulator | null = null



const search = async () => {
	if (!searchForm.acctcd) return vAlertError('계정과목을 선택해 주십시오.')
	if (!searchForm.fromdt || !searchForm.todt) return vAlertError('회계일자를 선택해 주십시오.')

	try {
		const res = await api.post('/hasl/HASL_630S_STR', {
			cmpycd: authStore.cmpycd,
			fromdt: searchForm.fromdt.replace(/-/g, ''),
			todt: searchForm.todt.replace(/-/g, ''),
			acctcd: searchForm.acctcd,
			mgtnofr: searchForm.mgtnofr,
			mgtnoto: searchForm.mgtnoto,
			gubun: '2'
		})

		const data = (res.data || []).map((row: any) => {
            const item = row
            return {
                ...item,
                mgtno: item.col0 || item.mgtno,
                mgtnm: item.col1 || item.mgtnm,
                bjanamt: Number(item.bjanamt || 0),
                dbamt: Number(item.dbamt || 0),
                cramt: Number(item.cramt || 0),
                cjanamt: Number(item.cjanamt || 0)
            }
        })

		mainGrid?.setData(data)
		if (data.length > 0) vAlert('조회되었습니다.')
		else vAlert('데이터가 존재하지 않습니다.')
	} catch (e) {
		vAlertError('조회 중 오류 발생')
	}
}

// 팝업 설정
const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function openHelp(type: 'ACCT' | 'MGTFR' | 'MGTTO') {
	if (type === 'ACCT') {
		Object.assign(modalProps, {
			title: '계정과목 선택', path: '/ha00/HA00_00P_STR', defaultField: 'acctnm',
			data: { gubun: 'A0', cmpycd: authStore.cmpycd, search: searchForm.acctnm },
			columns: [{ title: '코드', field: 'acctcd', width: 80 }, { title: '계정명', field: 'acctnm', width: 180 }, { title: '관리구분', field: 'typemgt', visible: false }],
			onConfirm: (d: any) => {
                const item = d
				searchForm.acctcd = item.acctcd
				searchForm.acctnm = item.acctnm
				searchForm.mgtgbn = item.typemgt || ''
				searchForm.mgtnofr = ''; searchForm.mgtnmfr = ''; searchForm.mgtnoto = ''; searchForm.mgtnmto = ''
			}
		})
	} else {
		if (!searchForm.acctcd) return vAlertError('계정과목을 먼저 선택해 주십시오.')
		const isFr = type === 'MGTFR'
		Object.assign(modalProps, {
			title: '관리번호 선택',
			path: '/ha00/HA00_00P_STR',
			defaultField: 'mgtno',
			data: {
				gubun: 'M0',
				cmpycd: authStore.cmpycd,
				gbncd: searchForm.mgtgbn,
				code: isFr ? searchForm.mgtnofr : searchForm.mgtnoto,
				remark: searchForm.acctcd
			},
			columns: [
				{ title: '관리번호', field: 'mgtno', width: 150 },
				{ title: '명칭/적요', field: 'mgtnm', width: 250 }
			],
			onConfirm: (d: any) => {
                const item = d
				if (isFr) {
					searchForm.mgtnofr = item.mgtno
					searchForm.mgtnmfr = item.mgtnm
				} else {
					searchForm.mgtnoto = item.mgtno
					searchForm.mgtnmto = item.mgtnm
				}
			}
		})
	}
	modalVisible.value = true
}

const print = () => {
	if (!searchForm.acctcd) return vAlertError('계정과목을 선택해 주세요.')
	const params = `acctcd=${searchForm.acctcd}&acctnm=${searchForm.acctnm}&mgtnofr=${searchForm.mgtnofr}&mgtnoto=${searchForm.mgtnoto}&fromdt=${searchForm.fromdt.replace(/-/g, '')}&todt=${searchForm.todt.replace(/-/g, '')}&PRTGU=1`
	window.open(`/hasl/HASL_630P?${params}`, 'ManagementStatementPrint', 'width=800,height=800,scrollbars=yes')
}

const goDetail = (mgtNo: string, mgtNm: string) => {
	if (!mgtNo) return
    const pgmid = 'HASL560S'
    addDynamicRoute(pgmid, '보조원장-관리번호', 'HASL')

	router.push({
		path: `/${pgmid}`,
		query: {
			fromdt: searchForm.fromdt,
			todt: searchForm.todt,
			acctcd: searchForm.acctcd,
			mgtno: mgtNo,
			mgtnm: mgtNm
		}
	})
}

onMounted(async () => {
	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns',
			height: '100%',
			columnDefaults: { headerSort: false, vertAlign: "middle" },
			columns: [
				{ title: "관리번호", field: "mgtno", width: 150, hozAlign: "center" },
				{
					title: "관리번호명 / 적요", field: "mgtnm", widthGrow: 2,
					formatter: (cell) => `<span class="text-primary text-decoration-underline cursor-pointer fw-bold">${cell.getValue()}</span>`,
					cellClick: (e, cell) => {
						const d = cell.getData()
						goDetail(d.mgtno, d.mgtnm)
					}
				},
				{
					title: "전기이월", field: "bjanamt", width: 130, hozAlign: "right",
					formatter: "money", formatterParams: { precision: 0 },
					bottomCalc: "sum", bottomCalcFormatter: "money", bottomCalcFormatterParams: { precision: 0 }
				},
				{
					title: "증가액", field: "dbamt", width: 130, hozAlign: "right",
					formatter: "money", formatterParams: { precision: 0 },
					bottomCalc: "sum", bottomCalcFormatter: "money", bottomCalcFormatterParams: { precision: 0 }
				},
				{
					title: "감소액", field: "cramt", width: 130, hozAlign: "right",
					formatter: "money", formatterParams: { precision: 0 },
					bottomCalc: "sum", bottomCalcFormatter: "money", bottomCalcFormatterParams: { precision: 0 }
				},
				{
					title: "잔액", field: "cjanamt", width: 130, hozAlign: "right",
					formatter: "money", formatterParams: { precision: 0 },
					bottomCalc: "sum", bottomCalcFormatter: "money", bottomCalcFormatterParams: { precision: 0 },
                    cssClass: "text-primary fw-bold"
				}
			]
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
				search()
			}
		} catch (e) {}
	}
})
</script>

<style scoped>
.erp-label { min-width: 80px; font-weight: 500; font-size: 13px; }
.tabulator-instance { width: 100% !important; background-color: #fff; }
:deep(.tabulator-cell) { border-right: 1px solid #dee2e6 !important; }
:deep(.tabulator-header .tabulator-col) { border-right: 1px solid #dee2e6 !important; background-color: #f8f9fa !important; }
</style>
