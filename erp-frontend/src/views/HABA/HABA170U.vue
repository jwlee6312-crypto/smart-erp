<!--
	=============================================================
	프로그램명: 지급어음장 관리 (haba170u)
	작성일자	: 2025.03.14
	작성자    : AI Assistant
	설명        : 지급어음장의 등록 및 관리 (표준 UI 적용)
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container">
		<!-- 🚀 상단 액션 바 -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-journal-text me-2 text-primary" style="font-size: 18px;"></i>
				기본정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				<span class="text-primary fw-bolder">지급어음장 관리 (haba170u)</span>
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

		<!-- 🔍 검색 조건 영역 -->
		<div class="p-2 pb-0 flex-shrink-0 bg-light">
			<div class="card border shadow-sm bg-white overflow-hidden">
				<div class="card-body p-2">
					<div class="d-flex align-items-center flex-wrap gap-3 small">
						<div class="d-flex align-items-center">
							<span class="erp-label"><i class="bi bi-dot"></i>어음번호</span>
							<div class="d-flex align-items-center gap-1">
								<input v-model="searchForm.billno" type="text" class="form-control form-control-sm" maxlength="12" placeholder="시작 번호" @keydown.enter="search" />
								<span class="text-muted">~</span>
								<input v-model="searchForm.billno_TO" type="text" class="form-control form-control-sm" maxlength="12" placeholder="종료 번호" @keydown.enter="search" />
							</div>
						</div>
                        <div class="text-muted small italic ms-auto">
                            <i class="bi bi-info-circle me-1"></i> 조회하시려는 어음번호 범위를 입력하십시오.
                        </div>
					</div>
				</div>
			</div>
		</div>

		<!-- 📝 상세 정보 입력 영역 -->
		<div class="p-2 pb-0 flex-shrink-0 bg-light">
			<div class="card border shadow-sm bg-white overflow-hidden">
				<div class="card-header py-1 px-2 bg-light border-bottom">
					<span class="small fw-bold text-secondary"><i class="bi bi-pencil-square me-1"></i> 어음 상세 정보 [{{ masterForm.actkind === 'U1' ? '수정' : '신규' }}]</span>
				</div>
				<table class="erp-table-full small border-0">
					<colgroup>
						<col style="width: 100px;" /><col style="width: 18%;" />
						<col style="width: 100px;" /><col style="width: 120px;" />
						<col style="width: 100px;" /><col style="width: 25%;" />
						<col style="width: 100px;" /><col style="width: 15%;" />
						<col style="width: 100px;" /><col />
					</colgroup>
					<tbody>
						<tr>
							<th class="text-center bg-light-subtle border-end">어음번호</th>
							<td class="bg-white border-end px-2 py-1">
								<input v-model="masterForm.billno" type="text" class="form-control form-control-sm fw-bold text-primary" maxlength="12" placeholder="번호 입력" />
							</td>
							<th class="text-center bg-light-subtle border-end border-top">등록매수</th>
							<td class="bg-white border-end border-top px-2 py-1">
								<div class="input-group input-group-sm">
									<input v-model="masterForm.billcnt" type="number" class="form-control form-control-sm text-end" />
									<span class="input-group-text bg-light border-0 small">매</span>
								</div>
							</td>
							<th class="text-center bg-light-subtle border-end border-top">발행기관</th>
							<td class="bg-white border-end border-top px-2 py-1">
								<div class="input-group input-group-sm">
									<input v-model="masterForm.bankcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
									<input v-model="masterForm.banknm" type="text" class="form-control" placeholder="금융기관 선택" @keydown.enter="openHelp('BANK')" />
									<button class="btn btn-outline-secondary px-2" @click="openHelp('BANK')"><i class="bi bi-search"></i></button>
								</div>
							</td>
							<th class="text-center bg-light-subtle border-end border-top">발행인</th>
							<td class="bg-white border-end border-top px-2 py-1">
								<input v-model="masterForm.issuman" type="text" class="form-control form-control-sm" maxlength="20" />
							</td>
							<th class="text-center bg-light-subtle border-end border-top">상태</th>
							<td class="bg-white border-top px-3 py-1">
								<div class="form-check form-switch pt-1">
									<input v-model="masterForm.useyn" class="form-check-input" type="checkbox" id="deleteCheck" true-value="N" false-value="Y">
									<label class="form-check-label small fw-bold text-danger" for="deleteCheck">폐기</label>
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
	BILLGU: '100',
	billno: '',
	billno_TO: '',
	billcnt: 1,
	bankcd: '',
	banknm: '',
	issuman: '',
	useyn: 'Y'
})

const mainGridRef = ref<HTMLDivElement | null>(null)
let mainGrid: Tabulator | null = null

const formatYmd = (v: string) => v && v.length === 8 ? `${v.substring(2, 4)}.${v.substring(4, 6)}.${v.substring(6, 8)}` : v

const search = async () => {
	try {
		const res = await api.post('/haba/HABA_170U_STR', {
			actkind: 'S1',
			cmpycd: authStore.cmpycd,
			BILLGU: masterForm.BILLGU,
			billno: searchForm.billno,
			billno_TO: searchForm.billno_TO
		})

		const list = (res.data || []).map((n: any) => {
			return {
				billno: n.col0 || n.billno,
				bankcd: n.col1 || n.bankcd,
				banknm: n.col2 || n.banknm,
				issuman: n.col3 || n.issuman,
				regdate: formatYmd(n.col4 || n.regdate),
				wonamt: Number(n.col5 || n.wonamt || 0),
				billgu_nm: n.col7 || n.billgu_nm,
				useyn: n.col8 || n.useyn
			}
		})

		mainGrid?.setData(list)
		if (list.length > 0) vAlert('조회되었습니다.')
		else vAlert('조회된 데이터가 없습니다.')
	} catch (e) { vAlertError('조회 중 오류 발생') }
}

const save = async () => {
	if (!masterForm.billno) return vAlert('어음번호를 정확히 입력하세요.')
	if (isNaN(masterForm.billcnt) || masterForm.billcnt < 1) return vAlert('등록매수를 확인해 주십시오.')
	if (!masterForm.banknm) return vAlert('발행기관을 선택해 주십시오.')
	if (!masterForm.issuman) return vAlert('발행인을 입력해 주십시오.')

    if (!confirm('저장하시겠습니까?')) return
	try {
		const payload = {
			...masterForm,
			actkind: masterForm.actkind || 'U1',
			cmpycd: authStore.cmpycd,
			userid: authStore.userid
		}

		const res = await api.post('/haba/HABA_170U_STR', payload)
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

const initialize = () => {
	resetForm(masterForm)
	masterForm.actkind = 'I1'
	masterForm.BILLGU = '100'
	masterForm.billcnt = 1
	masterForm.useyn = 'Y'
    masterForm.cmpycd = authStore.cmpycd
}

const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function openHelp(type: string) {
	if (type === 'BANK') {
		Object.assign(modalProps, {
			title: '발행기관 선택', path: '/ha00/HA00_00P_STR',
			data: { gubun: 'C3', cmpycd: authStore.cmpycd, gbncd: '', code: masterForm.banknm },
			columns: [
				{ title: '코드', field: 'bankcd', width: 80 },
				{ title: '금융기관명', field: 'banknm', width: 180 }
			],
			onConfirm: (d: any) => {
				masterForm.bankcd = d.bankcd;
				masterForm.banknm = d.banknm
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
			columnDefaults: { headerSort: false, vertAlign: "middle", headerHozAlign: "center", hozAlign: "center" },
			columns: [
				{ title: "어음번호", field: "billno", width: 150, hozAlign: "center", cssClass: "fw-bold" },
				{ title: "발행기관", field: "banknm", minWidth: 200, hozAlign: "left" },
				{ title: "발행인", field: "issuman", width: 120 },
				{ title: "등록일", field: "regdate", width: 110 },
				{ title: "어음금액", field: "wonamt", width: 120, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
				{ title: "상태", field: "billgu_nm", width: 90 },
                { title: "폐기", field: "useyn", width: 70,
                  formatter: (cell) => {
                    const val = String(cell.getValue() || '').trim().toUpperCase();
                    return val === 'N' ? '<span class="badge bg-danger">폐기</span>' : '';
                  }
                }
			]
		})
        mainGrid.on("rowClick", (e, row) => {
            const d = row.getData()
            Object.assign(masterForm, {
                actkind: 'U1',
                billno: d.billno,
                bankcd: d.bankcd,
                banknm: d.banknm,
                issuman: d.issuman,
                useyn: d.useyn,
                billcnt: 1
            })
        })
	}
    search()
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
.bg-light-subtle { background-color: #f8f9fa !important; }
</style>
