<!--
	=============================================================
	프로그램명: 지불계정관리 (haba510u)
	작성일자	: 2025.03.14
	작성자    : AI Assistant
	설명        : 지불유형별 연결 계정과목 관리 (표준 UI 적용)
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container">
		<!-- [헤더] 상단 액션 바 -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-credit-card-2-back me-2 text-primary" style="font-size: 18px;"></i>
				기본정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
				<span class="text-primary fw-bolder">지불계정관리 (HABA510U)</span>
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
				<button v-if="masterForm.actkind === 'U0'" class="btn-erp btn-danger" @click="deleteData">
					<i class="bi bi-trash"></i> 삭제
				</button>
			</div>
		</div>

		<!-- [상세] 입력 영역 -->
		<div class="p-2 pb-0 flex-shrink-0">
			<div class="card border shadow-sm bg-white overflow-hidden">
				<div class="card-header py-1 px-2 bg-light border-bottom">
					<span class="small fw-bold text-secondary"><i class="bi bi-pencil-square me-1"></i> 지불계정 상세 정보 [{{ masterForm.actkind === 'A0' ? '신규' : '수정' }}]</span>
				</div>
				<table class="erp-table-full small">
					<colgroup>
						<col style="width: 100px;" /><col style="width: 15%;" />
						<col style="width: 100px;" /><col style="width: 25%;" />
						<col style="width: 100px;" /><col />
					</colgroup>
					<tbody>
						<tr>
							<th class="text-center bg-light-subtle border-end">지불유형</th>
							<td class="bg-white border-end px-2 py-1">
								<input v-model="masterForm.paygbn" type="text" class="form-control form-control-sm text-center" maxlength="3" :readonly="masterForm.actkind === 'U0'" placeholder="3자리" />
							</td>
							<th class="text-center bg-light-subtle border-end">지불유형명</th>
							<td class="bg-white border-end px-2 py-1">
								<input v-model="masterForm.paygbnm" type="text" class="form-control form-control-sm" maxlength="20" />
							</td>
							<th class="text-center bg-light-subtle border-end">계정과목</th>
							<td class="bg-white px-2 py-1">
								<div class="input-group input-group-sm" style="max-width: 300px;">
									<input v-model="masterForm.acctcd" type="text" class="form-control text-center bg-light" style="max-width: 70px;" readonly />
									<input v-model="masterForm.acctnm" type="text" class="form-control" placeholder="계정 선택" @keydown.enter="openHelp" />
									<button class="btn btn-outline-secondary px-2" @click="openHelp"><i class="bi bi-search"></i></button>
								</div>
							</td>
						</tr>
					</tbody>
				</table>
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
import { ref, reactive, onMounted, nextTick } from 'vue'
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

// 마스터 데이터
const masterForm = reactive({
	actkind: 'A0',
	paygbn: '',
	paygbnm: '',
	acctcd: '',
	acctnm: ''
})

const mainGridRef = ref<HTMLDivElement | null>(null)
let mainGrid: Tabulator | null = null

const search = async () => {
	try {
		const res = await api.post('/haba/HABA_510U_STR', {
			actkind: 'S0',
			cmpycd: authStore.cmpycd
		})

		const processedData = res.data || []
		mainGrid?.setData(processedData)
		if (processedData.length > 0) vAlert('조회되었습니다.')
	} catch (e) { vAlertError('조회 중 오류 발생') }
}

const save = async () => {
	if (!masterForm.paygbn) return vAlert('지불유형을 기재해 주십시오.')
	if (masterForm.paygbn.length < 3) return vAlert('지불유형은 3자리로 입력하셔야 합니다.')
	if (!masterForm.paygbnm) return vAlert('지불유형명을 기재해 주십시오.')
	if (!masterForm.acctcd) return vAlert('계정과목을 선택해 주십시오.')

	try {
		const payload = {
			actkind: masterForm.actkind,
			cmpycd: authStore.cmpycd,
			paygbn: masterForm.paygbn,
			paygbnm: masterForm.paygbnm,
			acctcd: masterForm.acctcd,
			userid: authStore.userid
		}

		const res = await api.post('/haba/HABA_510U_STR', payload)
		const resData = res.data?.[0] || {};

		if (resData.ret_yn === 'Y' || resData.res === 'FAIL') {
			vAlertError(resData.ret_msg || '저장 중 오류가 발생했습니다.')
		} else {
			vAlert('정상적으로 저장되었습니다.')
			search()
			initialize()
		}
	} catch (e) { vAlertError('저장 실패') }
}

const deleteData = async () => {
	if (!masterForm.paygbn) return vAlert('삭제할 지불유형을 선택해 주십시오.')
	if (!confirm('정말로 삭제하시겠습니까?')) return

	try {
		const res = await api.post('/haba/HABA_510U_STR', {
			actkind: 'D0',
			cmpycd: authStore.cmpycd,
			paygbn: masterForm.paygbn,
			userid: authStore.userid
		})
		const resData = res.data?.[0] || {};

		if (resData.ret_yn === 'Y') {
			vAlertError(resData.ret_msg)
		} else {
			vAlert('성공적으로 삭제되었습니다.')
			search()
			initialize()
		}
	} catch (e) { vAlertError('삭제 중 오류 발생') }
}

const initialize = () => {
	resetForm(masterForm)
	masterForm.actkind = 'A0'
}

// 팝업 설정
const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function openHelp() {
	Object.assign(modalProps, {
		title: '계정과목 선택',
		path: '/ha00/HA00_00P_STR',
		data: { gubun: 'A0', cmpycd: authStore.cmpycd, code: masterForm.acctnm },
		columns: [
			{ title: '코드', field: 'acctcd', width: 100, hozAlign: 'center' },
			{ title: '계정명', field: 'acctnm', width: 250 }
		],
		onConfirm: (d: any) => {
			masterForm.acctcd = d.acctcd
			masterForm.acctnm = d.acctnm
		}
	})
	modalVisible.value = true
}

onMounted(() => {
	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns',
			height: '100%',
			columnDefaults: { headerSort: false, vertAlign: "middle" },
			columns: [
				{ title: "유형", field: "paygbn", width: 100, hozAlign: "center", cssClass: "fw-bold text-primary" },
				{ title: "유형명", field: "paygbnm", width: 250 },
				{ title: "계정과목", field: "acctcd", width: 120, hozAlign: "center" },
				{ title: "계정과목명", field: "acctnm", minWidth: 250 }
			]
		})

		mainGrid.on("rowClick", (e, row) => {
			const d = row.getData()
			Object.assign(masterForm, d)
			masterForm.actkind = 'U0'
		})

		nextTick(() => search())
	}
})
</script>

<style scoped>
.erp-label { min-width: 80px; font-weight: 500; font-size: 13px; }
.bg-light-subtle { background-color: #f8f9fa !important; }
:deep(.tabulator-row) { cursor: pointer; }
:deep(.tabulator-row:hover) { background-color: #f0f7ff !important; }
</style>
