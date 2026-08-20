<!--
	=============================================================
	프로그램명: 관리번호 잔액등록 (haba230u)
	작성일자	: 2025.03.14
	작성자    : AI Assistant
	설명        : 계정과목 및 관리번호별 기초 잔액(이월/본월) 등록 및 관리
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container">
		<!-- [헤더] 상단 액션 바 -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-tag-fill me-2 text-primary" style="font-size: 18px;"></i>
				기초자료 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				<span class="text-primary fw-bolder">관리번호 잔액등록 (HABA230U)</span>
			</div>
			<div class="btn-group-erp d-flex gap-1">
				<button class="btn-erp btn-init" @click="initialize">
					<i class="bi bi-plus-lg"></i> 신규
				</button>
				<button class="btn-erp btn-search" @click="search">
					<i class="bi bi-search"></i> 조회
				</button>
				<button class="btn-erp btn-save" @click="save">
					<i class="bi bi-check-lg"></i> 저장
				</button>
			</div>
		</div>

		<!-- [조회] 검색 조건 영역 -->
		<div class="p-2 pb-0 flex-shrink-0">
			<div class="card border shadow-sm bg-white overflow-hidden">
				<div class="card-body p-2 bg-light">
					<div class="d-flex align-items-center flex-wrap gap-3 small">
						<div class="d-flex align-items-center">
							<span class="erp-label"><i class="bi bi-dot"></i>회계년월</span>
							<div class="d-flex align-items-center gap-1">
								<select v-model="searchForm.yyyy" class="form-select form-select-sm" style="width: 80px;" @change="syncSearchToMaster">
									<option v-for="year in years" :key="year" :value="year">{{ year }}</option>
								</select>
								<span class="fw-bold">년</span>
								<select v-model="searchForm.mm" class="form-select form-select-sm" style="width: 65px;" @change="syncSearchToMaster">
									<option v-for="month in months" :key="month" :value="month">{{ month }}</option>
								</select>
								<span class="fw-bold">월</span>
							</div>
						</div>
						<div class="d-flex align-items-center">
							<span class="erp-label"><i class="bi bi-dot"></i>구 분</span>
							<select v-model="searchForm.mmgbn" class="form-select form-select-sm" style="width: 80px;" @change="syncSearchToMaster">
								<option value="00">이월</option>
								<option value="88">본월</option>
							</select>
						</div>
						<div class="d-flex align-items-center">
							<span class="erp-label"><i class="bi bi-dot"></i>계정과목</span>
							<div class="input-group input-group-sm" style="width: 200px;">
								<input v-model="searchForm.acctcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
								<input v-model="searchForm.acctnm" type="text" class="form-control" placeholder="계정명" @keydown.enter="openHelp('S_ACCT')" />
								<button class="btn btn-outline-secondary px-2" @click="openHelp('S_ACCT')"><i class="bi bi-search"></i></button>
							</div>
						</div>
						<div class="d-flex align-items-center">
							<span class="erp-label"><i class="bi bi-dot"></i>관리번호</span>
							<div class="d-flex align-items-center gap-1">
								<div class="input-group input-group-sm" style="width: 180px;">
									<input v-model="searchForm.mgtno1" type="text" class="form-control" placeholder="시작" @keydown.enter="openHelp('S_MGT1')" />
									<button class="btn btn-outline-secondary px-2" @click="openHelp('S_MGT1')"><i class="bi bi-search"></i></button>
								</div>
								<span>~</span>
								<div class="input-group input-group-sm" style="width: 180px;">
									<input v-model="searchForm.mgtno2" type="text" class="form-control" placeholder="종료" @keydown.enter="openHelp('S_MGT2')" />
									<button class="btn btn-outline-secondary px-2" @click="openHelp('S_MGT2')"><i class="bi bi-search"></i></button>
								</div>
							</div>
						</div>
					</div>
				</div>
			</div>
		</div>

		<!-- [상세] 입력 영역 -->
		<div class="p-2 pb-0 flex-shrink-0">
			<div class="card border shadow-sm bg-white overflow-hidden">
				<div class="card-header py-1 px-2 bg-light border-bottom">
					<span class="small fw-bold text-secondary"><i class="bi bi-pencil-square me-1"></i> 관리번호 잔액 상세 정보 [{{ masterForm.actkind === 'I1' ? '신규' : '수정' }}]</span>
				</div>
				<table class="erp-table-full small">
					<colgroup>
						<col style="width: 100px;" /><col style="width: 20%;" />
						<col style="width: 100px;" /><col style="width: 20%;" />
						<col style="width: 100px;" /><col />
					</colgroup>
					<tbody>
						<tr>
							<th class="text-center bg-light-subtle border-end">회계년월</th>
							<td class="bg-white border-end px-2 py-1">
								<div class="d-flex align-items-center gap-1">
									<select v-model="masterForm.yyyy" class="form-select form-select-sm" style="width: 80px;">
										<option v-for="year in years" :key="year" :value="year">{{ year }}</option>
									</select>
									<span class="fw-bold">년</span>
									<select v-model="masterForm.mm" class="form-select form-select-sm" style="width: 65px;">
										<option v-for="month in months" :key="month" :value="month">{{ month }}</option>
									</select>
									<span class="fw-bold">월</span>
								</div>
							</td>
							<th class="text-center bg-light-subtle border-end">구 분</th>
							<td class="bg-white border-end px-2 py-1" colspan="3">
								<select v-model="masterForm.mmgbn" class="form-select form-select-sm" style="max-width: 100px;">
									<option value="88">본월</option>
									<option value="00">이월</option>
								</select>
							</td>
						</tr>
						<tr>
							<th class="text-center bg-light-subtle border-end">계정과목</th>
							<td class="bg-white border-end px-2 py-1">
								<div class="input-group input-group-sm">
									<input v-model="masterForm.acctcd" type="text" class="form-control text-center bg-light" style="max-width: 70px;" readonly />
									<input v-model="masterForm.acctnm" type="text" class="form-control" @keydown.enter="openHelp('M_ACCT')" />
									<button class="btn btn-outline-secondary px-2" @click="openHelp('M_ACCT')"><i class="bi bi-search"></i></button>
								</div>
							</td>
							<th class="text-center bg-light-subtle border-end">관리번호</th>
							<td class="bg-white border-end px-2 py-1" colspan="3">
								<div class="input-group input-group-sm" style="max-width: 350px;">
									<input v-model="masterForm.mgtno" type="text" class="form-control text-center bg-light" style="max-width: 120px;" readonly />
									<input v-model="masterForm.mgtnm" type="text" class="form-control" @keydown.enter="openHelp('M_MGT')" />
									<button class="btn btn-outline-secondary px-2" @click="openHelp('M_MGT')"><i class="bi bi-search"></i></button>
								</div>
							</td>
						</tr>
						<tr>
							<th class="text-center bg-light-subtle border-end">차변금액</th>
							<td class="bg-white border-end px-2 py-1">
								<div class="d-flex align-items-center gap-1">
									<input v-model="masterForm.dbamt" type="number" class="form-control form-control-sm text-end" step="0" />
									<span>(원)</span>
								</div>
							</td>
							<th class="text-center bg-light-subtle border-end">대변금액</th>
							<td class="bg-white border-end px-2 py-1">
								<div class="d-flex align-items-center gap-1">
									<input v-model="masterForm.cramt" type="number" class="form-control form-control-sm text-end" step="0" />
									<span>(원)</span>
								</div>
							</td>
							<td colspan="2" class="bg-white px-2 py-1"></td>
						</tr>
					</tbody>
				</table>
			</div>
			<div class="px-2 py-1 text-muted small">
				<i class="bi bi-info-circle me-1"></i> 이월 자료는 전기이월 잔액을, 본월 자료는 당월 기초 발생 금액을 의미합니다.
			</div>
		</div>

		<!-- [그리드] 데이터 그리드 영역 -->
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
import { ref, reactive, onMounted, computed } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import AppAlert from '@/components/AppAlert.vue'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import Modal from '@/components/Modal.vue'
import type { ModalProps } from '@/types/modal'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

// 년도/월 옵션
const currentYear = new Date().getFullYear()
const years = computed(() => Array.from({ length: 10 }, (_, i) => String(currentYear - i)))
const months = computed(() => Array.from({ length: 12 }, (_, i) => String(i + 1).padStart(2, '0')))

// 검색 조건
const searchForm = reactive({
	yyyy: String(currentYear),
    mm: String(new Date().getMonth() + 1).padStart(2, '0'),
    mmgbn: '00',
	acctcd: '',
	acctnm: '',
	mgtgbn: '',
	mgtno1: '',
	mgtnm1: '',
	mgtno2: '',
	mgtnm2: ''
})

// 마스터 데이터
const masterForm = reactive({
	actkind: 'I1',
	yyyy: String(currentYear),
    mm: String(new Date().getMonth() + 1).padStart(2, '0'),
    mmgbn: '88',
	acctcd: '',
	acctnm: '',
	mgtno: '',
	mgtnm: '',
	mgtgbn: '',
	dbamt: 0,
	cramt: 0
})

const mainGridRef = ref<HTMLDivElement | null>(null)
let mainGrid: Tabulator | null = null

const search = async () => {
	if (!searchForm.acctcd) return vAlert('검색할 계정과목을 선택해 주십시오.')

	try {
		const res = await api.post('/haba/HABA_230U_STR', {
			actkind: 'SR',
			cmpycd: authStore.cmpycd,
			acctym: searchForm.yyyy + searchForm.mm,
		    mmgbn: searchForm.mmgbn,
			acctcd: searchForm.acctcd,
			mgtno: '',
			mgtno1: searchForm.mgtno1,
			mgtno2: searchForm.mgtno2
		})

		mainGrid?.setData(res.data || [])
		vAlert('조회되었습니다.')
	} catch (e) { vAlertError('조회 중 오류 발생') }
}

const save = async () => {
	if (!masterForm.acctcd) return vAlert('계정과목을 선택해 주십시오.')
	if (!masterForm.mgtno) return vAlert('관리번호를 선택해 주십시오.')

	try {
		const payload = {
			actkind: masterForm.actkind,
			cmpycd: authStore.cmpycd,
			acctym: masterForm.yyyy + masterForm.mm,
		    mmgbn: masterForm.mmgbn,
			acctcd: masterForm.acctcd,
			mgtno: masterForm.mgtno,
			mgtno1: searchForm.mgtno1,
			mgtno2: searchForm.mgtno2,
			dbamt: masterForm.dbamt || 0,
			cramt: masterForm.cramt || 0,
			mgtnm: masterForm.mgtnm
		}

		const res = await api.post('/haba/HABA_230U_STR', payload)

		if (res.data?.[0]?.ret_yn === 'Y') {
			vAlertError(res.data[0].ret_msg)
		} else {
			vAlert('정상적으로 저장되었습니다.')
			search()
			initialize()
		}
	} catch (e) { vAlertError('저장 실패') }
}

const initialize = () => {
	const prevyyyy = masterForm.yyyy
	const prevmm = masterForm.mm
	const prevmmgbn = masterForm.mmgbn
	const prevacctcd = masterForm.acctcd
	const prevacctnm = masterForm.acctnm
	const prevmgtgbn = masterForm.mgtgbn

	resetForm(masterForm)

	masterForm.actkind = 'I1'
	masterForm.yyyy = prevyyyy
	masterForm.mm = prevmm
	masterForm.mmgbn = prevmmgbn
	masterForm.acctcd = prevacctcd
	masterForm.acctnm = prevacctnm
	masterForm.mgtgbn = prevmgtgbn
	masterForm.dbamt = 0
	masterForm.cramt = 0
}

const syncSearchToMaster = () => {
	masterForm.yyyy = searchForm.yyyy
	masterForm.mm = searchForm.mm
	masterForm.mmgbn = searchForm.mmgbn
}

// 팝업 설정
const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function openHelp(type: string) {
	let searchVal = ''
	if (type === 'S_ACCT') searchVal = searchForm.acctnm
	if (type === 'S_MGT1') searchVal = searchForm.mgtno1
	if (type === 'S_MGT2') searchVal = searchForm.mgtno2
	if (type === 'M_ACCT') searchVal = masterForm.acctnm
	if (type === 'M_MGT') searchVal = masterForm.mgtnm

	if (type.includes('MGT') && (!searchForm.acctcd && !masterForm.acctcd)) {
		return vAlert("계정과목을 우선 선택해 주십시오.")
	}

	if (type.includes('ACCT')) {
		Object.assign(modalProps, {
			title: '계정과목 선택',
			path: '/ha00/HA00_00P_STR',
			data: { gubun: 'MGT', cmpycd: authStore.cmpycd, search: searchVal },
			columns: [
				{ title: '코드', field: 'col0', width: 100, hozAlign: 'center' },
				{ title: '계정명', field: 'col1', width: 250 },
				{ title: '관리구분', field: 'col2', visible: false }
			],
			onConfirm: (d: any) => {
				if (type === 'S_ACCT') {
					searchForm.acctcd = d.col0
					searchForm.acctnm = d.col1
					searchForm.mgtgbn = d.col2
				} else {
					masterForm.acctcd = d.col0
					masterForm.acctnm = d.col1
					masterForm.mgtgbn = d.col2
				}
			}
		})
	} else {
		const mgtGbn = type.startsWith('S') ? searchForm.mgtgbn : masterForm.mgtgbn
		const acctCd = type.startsWith('S') ? searchForm.acctcd : masterForm.acctcd
		Object.assign(modalProps, {
			title: '관리번호 선택',
			path: '/ha00/HA00_05P_STR',
			data: { mgtgbn: mgtGbn, acctcd: acctCd, cmpycd: authStore.cmpycd, search: searchVal },
			columns: [
				{ title: '관리번호', field: 'col0', width: 150, hozAlign: 'center' },
				{ title: '관리번호명', field: 'col1', width: 250 }
			],
			onConfirm: (d: any) => {
				if (type === 'S_MGT1') {
					searchForm.mgtno1 = d.col0
					searchForm.mgtnm1 = d.col1
				} else if (type === 'S_MGT2') {
					searchForm.mgtno2 = d.col0
					searchForm.mgtnm2 = d.col1
				} else {
					masterForm.mgtno = d.col0
					masterForm.mgtnm = d.col1
				}
			}
		})
	}
	modalVisible.value = true
}

onMounted(() => {
	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns',
			height: '100%',
			columnDefaults: { headerSort: false, vertAlign: "middle" },
			columns: [
				{ title: "관리번호", field: "col4", width: 150, hozAlign: "center" },
				{ title: "관리번호명", field: "col5", minWidth: 200 },
				{ title: "차변", field: "col7", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, bottomCalc: "sum", bottomCalcFormatter: "money", bottomCalcFormatterParams: { precision: 0 } },
				{ title: "대변", field: "col8", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, bottomCalc: "sum", bottomCalcFormatter: "money", bottomCalcFormatterParams: { precision: 0 } },
				{ title: "잔액", field: "col9", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, bottomCalc: "sum", bottomCalcFormatter: "money", bottomCalcFormatterParams: { precision: 0 } }
			],
			rowClick: (e, row) => {
				const d = row.getData()
				masterForm.actkind = 'U1'
				masterForm.yyyy = d.col0
				masterForm.mm = d.col1
				masterForm.mmgbn = d.col6
				masterForm.acctcd = d.col2
				masterForm.acctnm = d.col3
				masterForm.mgtno = d.col4
				masterForm.mgtnm = d.col5
				masterForm.dbamt = Number(d.col7)
				masterForm.cramt = Number(d.col8)
			}
		})
	}
})
</script>

<style scoped>
.erp-label { min-width: 70px; font-weight: 500; font-size: 13px; }
.bg-light-subtle { background-color: #f8f9fa !important; }
:deep(.tabulator-row) { cursor: pointer; }
:deep(.tabulator-row:hover) { background-color: #f0f7ff !important; }
:deep(.tabulator-footer) { font-weight: bold; background-color: #dfd9bd !important; }
</style>
