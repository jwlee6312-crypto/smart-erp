<!--
	=============================================================
	프로그램명 : 차입금관리
	작성일자	: 2025.02.24
	작성자    : AI Assistant
	설명        : 계정과목별 차입금 상세 내역 관리
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container">
		<!-- [헤더] 상단 액션 바 -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-bank me-2 text-primary" style="font-size: 18px;"></i>
				기본정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				<span class="text-primary fw-bolder">차입금관리(HABA130U)</span>
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
			<div class="card border shadow-sm overflow-hidden bg-light">
				<table class="erp-table-full" style="table-layout: fixed;">
					<colgroup>
						<col style="width: 100px;" /><col style="width: 250px;" />
						<col />
					</colgroup>
					<tbody>
						<tr>
							<th class="text-center border-end">계정과목</th>
							<td class="bg-white border-end px-2">
								<div class="input-group input-group-sm">
									<input v-model="searchForm.acctcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
									<input v-model="searchForm.acctcd_t" type="text" class="form-control" placeholder="계정 선택" @keydown.enter="openHelp('search_acct')" />
									<button class="btn btn-outline-secondary px-2" @click="openHelp('search_acct')"><i class="bi bi-search"></i></button>
								</div>
							</td>
							<td class="bg-white px-3 text-muted small">
								<i class="bi bi-info-circle me-1"></i> 검색하시려는 계정과목을 선택해 주십시오.
							</td>
						</tr>
					</tbody>
				</table>
			</div>
		</div>

		<!-- [상세] 상세 정보 입력 영역 -->
		<div class="p-2 pb-0 flex-shrink-0">
			<div class="card border shadow-sm bg-white overflow-hidden">
				<div class="card-header py-1 px-2 bg-light border-bottom">
					<span class="small fw-bold text-secondary"><i class="bi bi-pencil-square me-1"></i> 차입금 상세 정보 [{{ masterForm.actkind === 'I1' ? '신규' : '수정' }}]</span>
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
								<input v-model="masterForm.acctcd_t" type="text" class="form-control form-control-sm bg-light" readonly />
							</td>
							<th class="text-center bg-light-subtle border-end border-top">차입번호</th>
							<td class="bg-white border-end border-top px-2 py-1">
								<input v-model="masterForm.loanno" type="text" class="form-control form-control-sm" maxlength="20" />
							</td>
							<th class="text-center bg-light-subtle border-end border-top">차 입 처</th>
							<td class="bg-white border-top px-2 py-1">
								<div class="input-group input-group-sm">
									<input v-model="masterForm.bankcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
									<input v-model="masterForm.bankcd_t" type="text" class="form-control" @keydown.enter="openHelp('BANK')" />
									<button class="btn btn-outline-secondary px-2" @click="openHelp('BANK')"><i class="bi bi-search"></i></button>
								</div>
							</td>
						</tr>
						<tr>
							<th class="text-center bg-light-subtle border-end border-top">차 입 일</th>
							<td class="bg-white border-end border-top px-2 py-1">
								<input v-model="masterForm.stdymd" type="date" class="form-control form-control-sm" />
							</td>
							<th class="text-center bg-light-subtle border-end border-top">만 기 일</th>
							<td class="bg-white border-end border-top px-2 py-1">
								<input v-model="masterForm.endymd" type="date" class="form-control form-control-sm" />
							</td>
							<th class="text-center bg-light-subtle border-end border-top">차 입 금</th>
							<td class="bg-white border-top px-2 py-1">
								<div class="input-group input-group-sm">
									<input v-model="masterForm.loanamt" type="number" class="form-control text-end" />
									<span class="input-group-text">원</span>
								</div>
							</td>
						</tr>
						<tr>
							<th class="text-center bg-light-subtle border-end border-top">이 율</th>
							<td class="bg-white border-end border-top px-2 py-1">
								<div class="input-group input-group-sm">
									<input v-model="masterForm.rate" type="number" class="form-control text-end" step="0.01" />
									<span class="input-group-text">%</span>
								</div>
							</td>
							<th class="text-center bg-light-subtle border-end border-top">비 고</th>
							<td class="bg-white border-end border-top px-2 py-1">
								<input v-model="masterForm.remark" type="text" class="form-control form-control-sm" maxlength="100" />
							</td>
							<th class="text-center bg-light-subtle border-end border-top">사용여부</th>
							<td class="bg-white border-top px-3">
								<div class="form-check pt-1">
									<input v-model="masterForm.useyn" class="form-check-input" type="checkbox" id="useCheck" true-value="Y" false-value="N">
									<label class="form-check-label small fw-bold" for="useCheck">사용</label>
								</div>
							</td>
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

// 검색 폼 데이터
const searchForm = reactive({
	acctcd: '',
	acctcd_t: '',
	custgbn: ''
})

// 마스터 폼 데이터
const masterForm = reactive({
	actkind: 'I1',
	acctcd: '',
	acctcd_t: '',
	custgbn: '',
	loanno: '',
	bankcd: '',
	bankcd_t: '',
	stdymd: '',
	endymd: '',
	loanamt: 0,
	rate: 0,
	remark: '',
	useyn: 'Y'
})

const mainGridRef = ref<HTMLDivElement | null>(null)
let mainGrid: Tabulator | null = null

const formatYmd = (v: string) => v && v.length === 8 ? `${v.substring(0, 4)}-${v.substring(4, 6)}-${v.substring(6, 8)}` : v

const search = async () => {
	if (!searchForm.acctcd_t) return vAlert('검색하시려는 계정과목을 선택해 주십시오.')
	try {
		const res = await api.post('/haba/HABA_130U_STR', {
			actkind: 'S1',
			cmpycd: authStore.cmpycd,
			acctcd: searchForm.acctcd,
			loanamt: 0,
            rate: 0
		})

		const processedData = (res.data || []).map((r: any) => ({
			acctcd: r.acctcd,
			acctnm: r.acctnm,
			loanno: r.loanno,
			bankcd: r.bankcd,
			banknm: r.banknm,
			stdymd: formatYmd(r.stdymd),
			endymd: formatYmd(r.endymd),
			loanamt: Number(r.loanamt || 0),
			rate: Number(r.rate || 0),
			remark: r.remark,
			useyn: r.useyn
		}))

		mainGrid?.setData(processedData)
		vAlert('조회되었습니다.')
	} catch (e) { vAlertError('조회 중 오류 발생') }
}

const save = async () => {
	if (!masterForm.acctcd) return vAlert('계정코드를 기재해 주십시오.')
	if (!masterForm.loanno) return vAlert('차입번호를 기재해 주십시오.')
	if (!masterForm.bankcd) return vAlert('차입처를 기재해 주십시오.')

	try {
		const payload = {
			...masterForm,
			cmpycd: authStore.cmpycd,
			userid: authStore.userid,
			stdymd: masterForm.stdymd.replace(/-/g, ''),
			endymd: masterForm.endymd.replace(/-/g, '')
		}

		const res = await api.post('/haba/HABA_130U_STR', payload)

		if (res.data?.[0]?.ret_yn === 'Y') {
			vAlertError(res.data[0].ret_msg)
		} else {
			vAlert('정상적으로 작업되었습니다.')
			search()
			initialize()
		}
	} catch (e) { vAlertError('저장 실패') }
}

const initialize = () => {
	resetForm(masterForm)
	masterForm.actkind = 'I1'
	masterForm.acctcd = searchForm.acctcd
	masterForm.acctcd_t = searchForm.acctcd_t
	masterForm.custgbn = searchForm.custgbn
	masterForm.useyn = 'Y'
}

// 팝업 설정
const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function openHelp(type: string) {
	if (type === 'search_acct') {
		Object.assign(modalProps, {
			title: '계정과목 선택', path: '/ha00/HA00_00P_STR',
			data: { gubun: 'A6', cmpycd: authStore.cmpycd, code: searchForm.acctcd_t, gbncd: '080' },
			columns: [{ title: '코드', field: 'acctcd', width: 80 }, { title: '계정명', field: 'acctnm', width: 180 }],
			onConfirm: (d: any) => {
				searchForm.acctcd = d.acctcd;
				searchForm.acctcd_t = d.acctnm;
				searchForm.custgbn = d.typesub;
				initialize();
			}
		})
	} else if (type === 'BANK') {
		if (!masterForm.acctcd) return vAlert('조회 후 입력하시기 바랍니다.')
		Object.assign(modalProps, {
			title: '차입처 선택', path: '/ha00/HA00_00P_STR',
			data: { gubun: 'C3', cmpycd: authStore.cmpycd, code: masterForm.bankcd_t, gbncd: '' },
			columns: [{ title: '코드', field: 'bankcd', width: 80 }, { title: '차입처명', field: 'banknm', width: 180 }],
			onConfirm: (d: any) => { masterForm.bankcd = d.bankcd; masterForm.bankcd_t = d.banknm }
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
				{ title: "차입번호", field: "loanno", width: 180 },
				{ title: "차입처", field: "banknm", width: 300 },
				{
					title: "차입/만기일", field: "stdymd", width: 150, hozAlign: "center",
					formatter: (cell) => {
						const d = cell.getData()
						return `<div>${d.stdymd}</div><div class='text-muted'>${d.endymd}</div>`
					}
				},
				{ title: "차입금", field: "loanamt", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
				{ title: "이율", field: "rate", width: 150, hozAlign: "right", formatter: (c) => c.getValue() ? c.getValue() + "%" : "-" },
				{ title: "비고", field: "remark", minWidth: 150 },
                {
                  title: "사용",
                  field: "useyn",
                  width: 80,
                  hozAlign: "center",
                  formatter: (cell) => {
                    const val = String(cell.getValue() || '').trim().toUpperCase();
                    return val === 'Y' ? '<b class="text-primary">사용</b>' : '';
                  }
                }
			],
			rowClick: (e, row) => {
				const d = row.getData()
				Object.assign(masterForm, d)
				masterForm.actkind = 'U1'
				masterForm.acctcd_t = d.acctnm
				masterForm.bankcd_t = d.banknm
			}
		})
	}
})
</script>

<style scoped>
.erp-label { min-width: 80px; font-weight: 500; font-size: 13px; }
.bg-light-subtle { background-color: #f8f9fa !important; }
:deep(.tabulator-row) { cursor: pointer; }
:deep(.tabulator-row:hover) { background-color: #f0f7ff !important; }
</style>
