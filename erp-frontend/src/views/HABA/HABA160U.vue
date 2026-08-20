<!--
	=============================================================
	프로그램명: 지급어음기초자료 (haba160u)
	작성일자	: 2025.03.14
	작성자    : AI Assistant
	설명        : 지급어음의 기초 정보를 등록하고 관리하는 화면 (표준 UI 적용)
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container">
		<!-- 🚀 상단 액션 바 -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-file-earmark-ruled me-2 text-primary" style="font-size: 18px;"></i>
				기본정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				<span class="text-primary fw-bolder">지급어음기초자료 (haba160u)</span>
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
				<button v-if="masterForm.actkind === 'U1'" class="btn-erp btn-danger" @click="deleteData">
					<i class="bi bi-trash"></i> 삭제
				</button>
			</div>
		</div>

		<!-- 🔍 검색 조건 영역 -->
		<div class="p-2 pb-0 flex-shrink-0 bg-light">
			<div class="card border shadow-sm bg-white overflow-hidden">
				<div class="card-body p-2">
					<div class="d-flex align-items-center flex-wrap gap-3 small">
						<div class="d-flex align-items-center">
							<span class="erp-label"><i class="bi bi-dot"></i>어음번호</span>
							<div class="d-flex align-items-center gap-1">
								<input v-model="searchForm.billno" type="text" class="form-control form-control-sm" style="width: 150px;" maxlength="14" @keydown.enter="search" />
								<span class="text-muted">~</span>
								<input v-model="searchForm.billno_TO" type="text" class="form-control form-control-sm" style="width: 150px;" maxlength="14" @keydown.enter="search" />
							</div>
						</div>
                        <div class="ms-auto">
                            <button class="btn btn-outline-success btn-sm" @click="excel"><i class="bi bi-file-earmark-excel me-1"></i>엑셀</button>
                            <button class="btn btn-outline-dark btn-sm ms-1" @click="print"><i class="bi bi-printer me-1"></i>인쇄</button>
                        </div>
					</div>
				</div>
			</div>
		</div>

		<!-- 📝 상세 정보 입력 영역 -->
		<div class="p-2 pb-0 flex-shrink-0 bg-light">
			<div class="card border shadow-sm bg-white overflow-hidden">
				<div class="card-header py-1 px-2 bg-light border-bottom">
					<span class="small fw-bold text-secondary"><i class="bi bi-pencil-square me-1"></i> 어음 상세 정보 [{{ masterForm.actkind === 'I1' ? '신규' : '수정' }}]</span>
				</div>
				<table class="erp-table-full small border-0">
					<colgroup>
						<col style="width: 100px;" /><col style="width: 20%;" />
						<col style="width: 100px;" /><col style="width: 25%;" />
						<col style="width: 100px;" /><col />
					</colgroup>
					<tbody>
						<tr>
							<th class="text-center bg-light-subtle border-end">어음번호</th>
							<td class="bg-white border-end px-2 py-1">
								<input v-model="masterForm.billno" type="text" class="form-control form-control-sm fw-bold text-primary" maxlength="14" :readonly="masterForm.actkind === 'U1'" />
							</td>
							<th class="text-center bg-light-subtle border-end border-top">발행기관</th>
							<td class="bg-white border-end border-top px-2 py-1">
								<div class="input-group input-group-sm">
									<input v-model="masterForm.bankcd" type="text" class="form-control text-center bg-light" style="max-width: 65px;" readonly />
									<input v-model="masterForm.banknm" type="text" class="form-control" placeholder="금융기관 선택" @keydown.enter="openHelp('BANK')" />
									<button class="btn btn-outline-secondary px-2" @click="openHelp('BANK')"><i class="bi bi-search"></i></button>
								</div>
							</td>
							<th class="text-center bg-light-subtle border-end border-top">발행인</th>
							<td class="bg-white border-top px-2 py-1">
								<input v-model="masterForm.issuman" type="text" class="form-control form-control-sm" maxlength="20" />
							</td>
						</tr>
						<tr>
							<th class="text-center bg-light-subtle border-end border-top">발행일자</th>
							<td class="bg-white border-end border-top px-2 py-1">
								<input v-model="masterForm.stdymd" type="date" class="form-control form-control-sm" />
							</td>
							<th class="text-center bg-light-subtle border-end border-top">만기일자</th>
							<td class="bg-white border-end border-top px-2 py-1">
								<input v-model="masterForm.endymd" type="date" class="form-control form-control-sm" />
							</td>
							<th class="text-center bg-light-subtle border-end border-top">금&nbsp;&nbsp;&nbsp;&nbsp;액</th>
							<td class="bg-white border-top px-2 py-1">
								<div class="input-group input-group-sm">
									<input v-model="masterForm.billamt" type="number" class="form-control form-control-sm text-end fw-bold" step="0" />
									<span class="input-group-text bg-light border-0 small">원</span>
								</div>
							</td>
						</tr>
						<tr>
							<th class="text-center bg-light-subtle border-end border-top">어음유형</th>
							<td class="bg-white border-end border-top px-2 py-1">
								<select v-model="masterForm.billtype" class="form-select form-select-sm">
									<option v-for="opt in billTypeOptions" :key="opt.value" :value="opt.value">{{ opt.text }}</option>
								</select>
							</td>
							<th class="text-center bg-light-subtle border-end border-top">지급거래처</th>
							<td class="bg-white border-end border-top px-2 py-1">
								<div class="input-group input-group-sm">
									<input v-model="masterForm.custcd" type="text" class="form-control text-center bg-light" style="max-width: 80px;" readonly />
									<input v-model="masterForm.custnm" type="text" class="form-control" placeholder="거래처 선택" @keydown.enter="openHelp('CUST')" />
									<button class="btn btn-outline-secondary px-2" @click="openHelp('CUST')"><i class="bi bi-search"></i></button>
								</div>
							</td>
							<th class="text-center bg-light-subtle border-end border-top">사용여부</th>
							<td class="bg-white border-top px-3 py-1">
								<div class="form-check form-switch pt-1">
									<input v-model="masterForm.useyn" class="form-check-input" type="checkbox" id="useYnCheck" true-value="Y" false-value="N" />
									<label class="form-check-label small fw-bold" for="useYnCheck">사용</label>
								</div>
							</td>
						</tr>
					</tbody>
				</table>
			</div>
		</div>

		<!-- 📊 그리드 영역 -->
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
import * as XLSX from 'xlsx'
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

// 🔍 검색 데이터
const searchForm = reactive({
	billno: '',
	billno_TO: ''
})

// 📝 마스터 데이터
const masterForm = reactive({
	actkind: 'I1',
	billno: '',
	bankcd: '',
	banknm: '',
	issuman: '',
	stdymd: '',
	endymd: '',
	billamt: 0,
	billtype: '',
	custcd: '',
	custnm: '',
	useyn: 'Y'
})

const billTypeOptions = ref<{ value: string; text: string }[]>([])

const mainGridRef = ref<HTMLDivElement | null>(null)
let mainGrid: Tabulator | null = null

const fetchOptions = async () => {
	try {
		// 어음유형 (170)
		const resType = await api.post('/ha00/HA00_00P_STR', { gubun: 'E0', cmpycd: authStore.cmpycd, search: '170' })
		billTypeOptions.value = resType.data?.map((i: any) => ({ value: i.col0, text: i.col1 })) || []
		if (billTypeOptions.value.length > 0) masterForm.billtype = billTypeOptions.value[0].value
	} catch (e) { console.error('기초 데이터 로드 실패', e) }
}

const search = async () => {
	try {
		const res = await api.post('/haba/HABA_160U_STR', {
			actkind: 'S1',
			cmpycd: authStore.cmpycd,
			BILLGU: '100',
			billno: searchForm.billno,
			billno_TO: searchForm.billno_TO
		})
		const list = res.data || []
		mainGrid?.setData(list)
		if (list.length > 0) vAlert('조회되었습니다.')
		else vAlert('조회된 데이터가 없습니다.')
	} catch (e) { vAlertError('조회 중 오류 발생') }
}

const save = async () => {
	if (!masterForm.billno) return vAlert('어음번호를 입력하세요.')
	if (!masterForm.banknm) return vAlert('발행기관을 선택하세요.')
	if (!masterForm.issuman) return vAlert('발행인을 입력하세요.')
	if (!masterForm.stdymd) return vAlert('발행일자를 선택하세요.')
	if (!masterForm.endymd) return vAlert('만기일자를 선택하세요.')
	if (Number(masterForm.billamt || 0) <= 0) return vAlert('어음금액을 입력하세요.')
	if (!masterForm.custcd) return vAlert('지급거래처를 선택하세요.')

    if (!confirm('저장하시겠습니까?')) return
	try {
		const payload = {
			...masterForm,
			cmpycd: authStore.cmpycd,
			BILLGU: '100',
			stdymd: masterForm.stdymd.replace(/-/g, ''),
			endymd: masterForm.endymd.replace(/-/g, ''),
			userid: authStore.userid
		}

		const res = await api.post('/haba/HABA_160U_STR', payload)
		const resdata = res.data?.[0] || {};
		if (resdata.ret_yn === 'Y' || resdata.result === 'N') {
			vAlertError(resdata.ret_msg || resdata.msg || '저장 실패')
		} else {
			vAlert('정상적으로 처리되었습니다.')
			search()
			initialize()
		}
	} catch (e) { vAlertError('저장 실패') }
}

const deleteData = async () => {
	if (masterForm.actkind !== 'U1' || !masterForm.billno) return
	if (!confirm('해당 자료를 삭제하시겠습니까?')) return

	try {
		const res = await api.post('/haba/HABA_160U_STR', {
			actkind: 'D1',
			cmpycd: authStore.cmpycd,
			BILLGU: '100',
			billno: masterForm.billno,
			userid: authStore.userid
		})
        const resdata = res.data?.[0] || {};
		if (resdata.ret_yn === 'Y' || resdata.result === 'N') {
			vAlertError(resdata.ret_msg || resdata.msg || '삭제 실패')
		} else {
			vAlert('삭제되었습니다.')
			search()
			initialize()
		}
	} catch (e) { vAlertError('삭제 중 오류 발생') }
}

const excel = () => {
	const today = new Date().toISOString().substring(0, 10)
	mainGrid?.download("xlsx", `지급어음기초자료_${today}.xlsx`)
}

const print = () => {
	const params = `billno=${searchForm.billno}&billno_TO=${searchForm.billno_TO}`
	window.open(`/haba/HABA_160P?${params}`, 'NotesPrint', 'width=800,height=800,scrollbars=yes')
}

const initialize = () => {
	resetForm(masterForm)
	masterForm.actkind = 'I1'
	masterForm.useyn = 'Y'
    masterForm.cmpycd = authStore.cmpycd
	if (billTypeOptions.value.length > 0) masterForm.billtype = billTypeOptions.value[0].value
}

const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function openHelp(type: 'BANK' | 'CUST') {
	const isBank = type === 'BANK'
	Object.assign(modalProps, {
		title: isBank ? '발행기관 선택' : '거래처 선택',
		path: '/ha00/HA00_03P_STR',
		data: { custgbn: isBank ? '020' : '010', cmpycd: authStore.cmpycd, search: isBank ? masterForm.banknm : masterForm.custnm },
		columns: [
			{ title: '코드', field: 'col0', width: 100, hozAlign: 'center' },
			{ title: isBank ? '기관명' : '거래처명', field: 'col1', width: 250 }
		],
		onConfirm: (d: any) => {
			if (isBank) {
				masterForm.bankcd = d.col0; masterForm.banknm = d.col1
			} else {
				masterForm.custcd = d.col0; masterForm.custnm = d.col1
			}
		}
	})
	modalVisible.value = true
}

const formatDate = (val: string) => {
	if (!val || val.length !== 8) return ''
	return `${val.substring(0, 4)}-${val.substring(4, 6)}-${val.substring(6, 8)}`
}

onMounted(async () => {
	if (typeof window !== 'undefined') (window as any).XLSX = XLSX
	await fetchOptions()
	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns',
			height: '100%',
			columnDefaults: { headerSort: false, vertAlign: "middle", headerHozAlign: "center", hozAlign: "center" },
			columns: [
				{ title: "어음번호", field: "col0", width: 130, hozAlign: "center", cssClass: "fw-bold" },
				{ title: "발행기관", field: "col11", width: 150, hozAlign: "left" },
				{ title: "발행인", field: "col1", width: 120 },
				{ title: "발행일", field: "col3", width: 100, formatter: (c) => formatDate(c.getValue()) },
				{ title: "만기일", field: "col4", width: 100, formatter: (c) => formatDate(c.getValue()) },
				{ title: "금액", field: "col5", width: 120, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
				{ title: "유형", field: "col10", width: 100 },
				{ title: "지급거래처", field: "col9", minWidth: 200, hozAlign: "left" },
                { title: "사용", field: "col8", width: 70, formatter: "tickCross" }
			]
		})
        mainGrid.on("rowClick", (e, row) => {
            const d = row.getData()
            Object.assign(masterForm, {
                actkind: 'U1',
                billno: d.col0,
                issuman: d.col1,
                bankcd: d.col2,
                banknm: d.col11,
                stdymd: formatDate(d.col3),
                endymd: formatDate(d.col4),
                billamt: Number(d.col5),
                billtype: d.col7,
                custnm: d.col9,
                custcd: d.col6,
                useyn: d.col8
            })
        })
	}
    search()
})
</script>

<style scoped>
.erp-label { min-width: 80px; font-weight: 500; font-size: 13px; }
.bg-light-subtle { background-color: #f8f9fa !important; }
</style>
