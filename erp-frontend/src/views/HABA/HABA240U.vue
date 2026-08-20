<!--
	=============================================================
	프로그램명: 미지급금 잔액등록
	작성일자	: 2025.02.24
	작성자    : AI Assistant
	설명        : 계정과목 및 거래처별 미지급금 기초 잔액 관리
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container">
		<!-- [헤더] 상단 액션 바 -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-wallet2 me-2 text-primary" style="font-size: 18px;"></i>
				기본정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				<span class="text-primary fw-bolder">미지급금 잔액등록 (HABA240U)</span>
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
							<span class="erp-label"><i class="bi bi-dot"></i>계정과목</span>
							<div class="input-group input-group-sm" style="width: 220px;">
								<input v-model="searchForm.acctcd" type="text" class="form-control text-center bg-light" style="max-width: 65px;" readonly />
								<input v-model="searchForm.acctnm" type="text" class="form-control" placeholder="계정명" @keydown.enter="openHelp('S_ACCT')" />
								<button class="btn btn-outline-secondary px-2" @click="openHelp('S_ACCT')"><i class="bi bi-search"></i></button>
							</div>
						</div>
						<div class="d-flex align-items-center">
							<span class="erp-label"><i class="bi bi-dot"></i>발생일</span>
							<div class="d-flex align-items-center gap-1">
								<input v-model="searchForm.fromdt" type="date" class="form-control form-control-sm" style="width: 140px;" />
								<span>~</span>
								<input v-model="searchForm.todt" type="date" class="form-control form-control-sm" style="width: 140px;" />
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
					<span class="small fw-bold text-secondary"><i class="bi bi-pencil-square me-1"></i> 미지급금 상세 정보 [{{ masterForm.actkind === 'I1' ? '신규' : '수정' }}]</span>
				</div>
				<table class="erp-table-full small">
					<colgroup>
						<col style="width: 100px;" /><col style="width: 20%;" />
						<col style="width: 100px;" /><col style="width: 20%;" />
						<col style="width: 100px;" /><col />
					</colgroup>
					<tbody>
						<tr>
							<th class="text-center bg-light-subtle border-end">계정과목</th>
							<td class="bg-white border-end px-2 py-1">
								<div class="input-group input-group-sm">
									<input v-model="masterForm.acctcd" type="text" class="form-control text-center bg-light" style="max-width: 70px;" readonly />
									<input v-model="masterForm.acctnm" type="text" class="form-control" @keydown.enter="openHelp('M_ACCT')" />
									<button class="btn btn-outline-secondary px-2" @click="openHelp('M_ACCT')"><i class="bi bi-search"></i></button>
								</div>
							</td>
							<th class="text-center bg-light-subtle border-end">거 래 처</th>
							<td class="bg-white border-end px-2 py-1" colspan="3">
								<div class="input-group input-group-sm" style="max-width: 300px;">
									<input v-model="masterForm.custcd" type="text" class="form-control text-center bg-light" style="max-width: 70px;" readonly />
									<input v-model="masterForm.custnm" type="text" class="form-control" @keydown.enter="openHelp('M_CUST')" />
									<button class="btn btn-outline-secondary px-2" @click="openHelp('M_CUST')"><i class="bi bi-search"></i></button>
								</div>
							</td>
						</tr>
						<tr>
							<th class="text-center bg-light-subtle border-end">발 생 일</th>
							<td class="bg-white border-end px-2 py-1">
								<input v-model="masterForm.slipymd" type="date" class="form-control form-control-sm" />
							</td>
							<th class="text-center bg-light-subtle border-end">미지급금</th>
							<td class="bg-white border-end px-2 py-1">
								<div class="d-flex align-items-center gap-1">
									<input v-model="masterForm.Upayamt" type="number" class="form-control form-control-sm text-end" />
									<span>(원)</span>
								</div>
							</td>
							<td colspan="2" class="bg-white px-2 py-1"></td>
						</tr>
						<tr>
							<th class="text-center bg-light-subtle border-end">지불조건</th>
							<td class="bg-white border-end px-2 py-1">
								<select v-model="masterForm.paycndt" class="form-select form-select-sm">
									<option v-for="opt in payConditionOptions" :key="opt.value" :value="opt.value">{{ opt.text }}</option>
								</select>
							</td>
							<th class="text-center bg-light-subtle border-end">지불예정일</th>
							<td class="bg-white border-end px-2 py-1">
								<input v-model="masterForm.reqymd" type="date" class="form-control form-control-sm" />
							</td>
							<td colspan="2" class="bg-white px-2 py-1"></td>
						</tr>
						<tr>
							<th class="text-center bg-light-subtle border-end">비 고</th>
							<td class="bg-white border-end px-2 py-1">
								<input v-model="masterForm.remark" type="text" class="form-control form-control-sm" />
							</td>
							<th class="text-center bg-light-subtle border-end">사용여부</th>
							<td class="bg-white border-end px-2 py-1">
								<div class="form-check form-check-inline mb-0">
									<input v-model="masterForm.useyn" class="form-check-input" type="checkbox" id="useYnCheck" true-value="Y" false-value="N" />
									<label class="form-check-label" for="useYnCheck">사용</label>
								</div>
							</td>
							<td colspan="2" class="bg-white px-2 py-1"></td>
						</tr>
					</tbody>
				</table>
			</div>
		</div>

		<!-- [그리드] 메인 그리드 영역 -->
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
import { useFormReset } from '@/composables/useFormReset'
import Modal from '@/components/Modal.vue'
import type { ModalProps } from '@/types/modal'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

// 날짜 초기화 (현재 월 1일 ~ 오늘)
const today = new Date().toISOString().split('T')[0]
const firstDay = new Date(new Date().getFullYear(), new Date().getMonth(), 2).toISOString().split('T')[0]

// 검색 조건 데이터
const searchForm = reactive({
	acctcd: '',
	acctnm: '',
	fromdt: firstDay,
	todt: today
})

// 마스터 데이터
const masterForm = reactive({
	actkind: 'I1',
	slipno: '',
	srowno: '',
	acctcd: '',
	acctnm: '',
	custcd: '',
	custnm: '',
	slipymd: today,
	Upayamt: 0,
	paycndt: '',
	reqymd: '',
	remark: '',
	useyn: 'Y'
})

const payConditionOptions = ref<{ value: string; text: string }[]>([])

const mainGridRef = ref<HTMLDivElement | null>(null)
let mainGrid: Tabulator | null = null

const fetchOptions = async () => {
	try {
		const res = await api.post('/ha00/HA00_00P_STR', {
			gubun: 'E0',
			cmpycd: '',
			search: '190'
		})
		payConditionOptions.value = res.data?.map((item: any) => ({
			value: item.col0,
			text: item.col1
		})) || []
	} catch (e) { console.error('지불조건 로드 실패', e) }
}

const search = async () => {
	if (!searchForm.acctcd) return vAlert('검색할 계정과목을 선택해 주십시오.')

	try {
		const res = await api.post('/haba/HABA_240U_STR', {
			actkind: 'SR',
			cmpycd: authStore.cmpycd,
			fromdt: searchForm.fromdt.replace(/-/g, ''),
			todt: searchForm.todt.replace(/-/g, ''),
			slipymd: '',
			slipno: '',
			srowno: '',
			acctcd: searchForm.acctcd
		})

		mainGrid?.setData(res.data || [])
		vAlert('조회되었습니다.')
	} catch (e) { vAlertError('조회 중 오류 발생') }
}

const save = async () => {
	if (!masterForm.acctcd) return vAlert('계정과목을 선택해 주십시오.')
	if (!masterForm.custcd) return vAlert('거래처를 선택해 주십시오.')
	if (!masterForm.Upayamt) return vAlert('미지급액을 기재해 주십시오.')

	try {
		const payload = {
			actkind: masterForm.actkind,
			cmpycd: authStore.cmpycd,
			fromdt: searchForm.fromdt.replace(/-/g, ''),
			todt: searchForm.todt.replace(/-/g, ''),
			slipymd: masterForm.slipymd.replace(/-/g, ''),
			slipno: masterForm.slipno,
			srowno: masterForm.srowno,
			acctcd: masterForm.acctcd,
			custcd: masterForm.custcd,
			Upayamt: masterForm.Upayamt,
			paycndt: masterForm.paycndt,
			reqymd: masterForm.reqymd.replace(/-/g, ''),
			remark: masterForm.remark,
			useyn: masterForm.useyn,
			userid: authStore.userid
		}

		const res = await api.post('/haba/HABA_240U_STR', payload)

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
	const prevacctcd = searchForm.acctcd
	const prevacctnm = searchForm.acctnm

	resetForm(masterForm)

	masterForm.actkind = 'I1'
	masterForm.acctcd = prevacctcd
	masterForm.acctnm = prevacctnm
	masterForm.slipymd = today
	masterForm.useyn = 'Y'
	masterForm.Upayamt = 0
}

// 팝업 설정
const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function openHelp(type: string) {
	let searchVal = ''
	if (type === 'S_ACCT') searchVal = searchForm.acctnm
	if (type === 'M_ACCT') searchVal = masterForm.acctnm
	if (type === 'M_CUST') searchVal = masterForm.custnm

	if (type === 'M_CUST' && !masterForm.acctcd) {
		return vAlert("계정과목을 우선 선택해 주십시오.")
	}

	if (type.includes('ACCT')) {
		Object.assign(modalProps, {
			title: '계정과목 선택',
			path: '/ha00/HA00_00P_STR',
			data: { gbn: 'CRS', cmpycd: authStore.cmpycd, acctgbn: '010', search: searchVal },
			columns: [
				{ title: '코드', field: 'col0', width: 100, hozAlign: 'center' },
				{ title: '계정명', field: 'col1', width: 250 }
			],
			onConfirm: (d: any) => {
				if (type === 'S_ACCT') {
					searchForm.acctcd = d.col0
					searchForm.acctnm = d.col1
					masterForm.acctcd = d.col0
					masterForm.acctnm = d.col1
				} else {
					masterForm.acctcd = d.col0
					masterForm.acctnm = d.col1
				}
			}
		})
	} else {
		Object.assign(modalProps, {
			title: '거래처 선택',
			path: '/ha00/HA00_03P_STR',
			data: { custgbn: '010', cmpycd: authStore.cmpycd, search: searchVal },
			columns: [
				{ title: '코드', field: 'col0', width: 100, hozAlign: 'center' },
				{ title: '거래처명', field: 'col1', width: 250 }
			],
			onConfirm: (d: any) => {
				masterForm.custcd = d.col0
				masterForm.custnm = d.col1
			}
		})
	}
	modalVisible.value = true
}

// yyyymmDD -> yyyy-mm-DD
const formatDate = (val: string) => {
	if (!val || val.length !== 8) return ''
	return `${val.substring(0, 4)}-${val.substring(4, 6)}-${val.substring(6, 8)}`
}

onMounted(() => {
	fetchOptions()
	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns',
			height: '100%',
			columnDefaults: { headerSort: false, vertAlign: "middle" },
			columns: [
				{ title: "거래처", field: "col5", width: 100, hozAlign: "center" },
				{ title: "거래처명", field: "col6", minWidth: 200 },
				{ title: "발생일", field: "col0", width: 110, hozAlign: "center", formatter: (cell) => formatDate(cell.getValue()) },
				{ title: "미지급액", field: "col7", width: 120, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
				{ title: "지불조건", field: "col8", width: 110, hozAlign: "center" },
				{ title: "지불예정일", field: "col10", width: 110, hozAlign: "center", formatter: (cell) => formatDate(cell.getValue()) },
				{ title: "사용", field: "col12", width: 80, hozAlign: "center", formatter: "tickCross" }
			],
			rowClick: (e, row) => {
				const d = row.getData()
				masterForm.actkind = 'U1'
				masterForm.acctcd = d.col3
				masterForm.acctnm = d.col4
				masterForm.custcd = d.col5
				masterForm.custnm = d.col6
				masterForm.slipymd = formatDate(d.col0)
				masterForm.Upayamt = Number(d.col7)
				masterForm.paycndt = d.col9
				masterForm.reqymd = formatDate(d.col10)
				masterForm.remark = d.col11
				masterForm.useyn = d.col12
				masterForm.slipno = d.col1
				masterForm.srowno = d.col2
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
</style>
