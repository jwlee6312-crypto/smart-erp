<!--
	=============================================================
	프로그램명 : 구좌정보관리 (HABA110U)
	작성일자	: 2025.03.14
	작성자    : AI Assistant
	설명        : 계정과목별 금융 계좌(구좌) 상세 정보 관리 (표준 UI 적용)
	=============================================================
-->

<template>
	<AppAlert :show="showalert" :error="showerror" :message="alertmessage" />

	<div class="erp-container">
		<!-- [헤더] 상단 액션 바 -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-wallet2 me-2 text-primary" style="font-size: 18px;"></i>
				기본정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				<span class="text-primary fw-bolder">구좌정보관리 (HABA110U)</span>
			</div>
			<div class="btn-group-erp d-flex gap-1">
				<button class="btn-erp btn-init" @click="initialize">초기화</button>
				<button class="btn-erp btn-search" @click="search">조회</button>
				<button class="btn-erp btn-save" @click="save">저장</button>
			</div>
		</div>

		<!-- [조회] 검색 조건 영역 -->
		<div class="p-2 pb-0 flex-shrink-0 bg-light">
			<div class="card border shadow-sm overflow-hidden bg-white">
				<table class="erp-table-full" style="table-layout: fixed;">
					<colgroup>
						<col style="width: 100px;" /><col style="width: 250px;" />
						<col />
					</colgroup>
					<tbody>
						<tr>
							<th class="text-center border-end bg-light">계정과목</th>
							<td class="px-2">
								<div class="input-group input-group-sm">
									<input v-model="searchform.acctcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
									<input v-model="searchform.acctcd_t" type="text" class="form-control" placeholder="계정 선택" @keydown.enter="openhelp('search_acct')" />
									<button class="btn btn-outline-secondary px-2" @click="openhelp('search_acct')"><i class="bi bi-search"></i></button>
								</div>
							</td>
							<td class="px-3 text-muted small">
								<i class="bi bi-info-circle me-1"></i> 검색하고자 하는 계정과목을 선택해 주십시오.
							</td>
						</tr>
					</tbody>
				</table>
			</div>
		</div>

		<!-- [상세] 상세 정보 입력 영역 -->
		<div class="p-2 pb-0 flex-shrink-0 bg-light">
			<div class="card border shadow-sm bg-white overflow-hidden">
				<div class="card-header py-1 px-2 bg-light border-bottom">
					<span class="small fw-bold text-secondary"><i class="bi bi-pencil-square me-1"></i> 구좌 상세 정보 [{{ masterdata.actkind === 'I1' ? '신규' : '수정' }}]</span>
				</div>
				<table class="erp-table-full border-0 small">
					<colgroup>
						<col style="width: 100px;" /><col style="width: 20%;" />
						<col style="width: 100px;" /><col style="width: 20%;" />
						<col style="width: 100px;" /><col />
					</colgroup>
					<tbody>
						<tr>
							<th class="text-center bg-light-subtle border-end">계정과목</th>
							<td class="bg-white border-end px-2 py-1">
								<input v-model="masterdata.acctcd_t" type="text" class="form-control form-control-sm bg-light" readonly />
							</td>
							<th class="text-center bg-light-subtle border-end border-top">계좌번호</th>
							<td class="bg-white border-end border-top px-2 py-1">
								<input v-model="masterdata.gujano" type="text" class="form-control form-control-sm" maxlength="20" />
							</td>
							<th class="text-center bg-light-subtle border-end border-top">개설기관</th>
							<td class="bg-white border-top px-2 py-1">
								<div class="input-group input-group-sm">
									<input v-model="masterdata.bankcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
									<input v-model="masterdata.bankcd_t" type="text" class="form-control" placeholder="금융기관 선택" @keydown.enter="openhelp('bank')" />
									<button class="btn btn-outline-secondary px-2" @click="openhelp('bank')"><i class="bi bi-search"></i></button>
								</div>
							</td>
						</tr>
						<tr>
							<th class="text-center bg-light-subtle border-end border-top">개설일</th>
							<td class="bg-white border-end border-top px-2 py-1">
								<input v-model="masterdata.stdymd" type="date" class="form-control form-control-sm" />
							</td>
							<th class="text-center bg-light-subtle border-end border-top">만기일</th>
							<td class="bg-white border-end border-top px-2 py-1">
								<input v-model="masterdata.endymd" type="date" class="form-control form-control-sm" />
							</td>
							<th class="text-center bg-light-subtle border-end border-top">계약금액</th>
							<td class="bg-white border-top px-2 py-1">
								<div class="input-group input-group-sm">
									<input v-model="masterdata.wonamt" type="number" class="form-control text-end" />
									<span class="input-group-text">원</span>
								</div>
							</td>
						</tr>
						<tr>
							<th class="text-center bg-light-subtle border-end border-top">이율</th>
							<td class="bg-white border-end border-top px-2 py-1">
								<div class="input-group input-group-sm">
									<input v-model="masterdata.rate" type="number" class="form-control text-end" step="0.01" />
									<span class="input-group-text">%</span>
								</div>
							</td>
							<th class="text-center bg-light-subtle border-end border-top">불입액</th>
							<td class="bg-white border-end border-top px-2 py-1">
								<div class="input-group input-group-sm">
									<input v-model="masterdata.payamt" type="number" class="form-control text-end" />
									<span class="input-group-text">원</span>
								</div>
							</td>
							<th class="text-center bg-light-subtle border-end border-top">불입일</th>
							<td class="bg-white border-top px-2 py-1">
								<div class="d-flex align-items-center gap-2">
									<input v-model="masterdata.paydd" type="text" class="form-control form-control-sm text-end" style="max-width: 60px;" maxlength="2" />
									<span class="fw-bold">일</span>
								</div>
							</td>
						</tr>
						<tr>
							<th class="text-center bg-light-subtle border-end border-top">비   고</th>
							<td colspan="3" class="bg-white border-end border-top px-2 py-1">
								<input v-model="masterdata.remark" type="text" class="form-control form-control-sm" maxlength="40" />
							</td>
							<th class="text-center bg-light-subtle border-end border-top">사용여부</th>
							<td class="bg-white border-top px-3">
								<div class="form-check form-switch m-0 pt-1">
									<input v-model="masterdata.useyn" class="form-check-input" type="checkbox" id="usecheck" true-value="Y" false-value="N">
									<label class="form-check-label small fw-bold" for="usecheck">사용</label>
								</div>
							</td>
						</tr>
					</tbody>
				</table>
			</div>
		</div>

		<!-- [그리드] 데이터 그리드 영역 -->
		<div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column bg-light">
			<div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white">
                <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
                  <div ref="maingridelement" class="tabulator-instance flex-grow-1"></div>
                </div>
			</div>
		</div>
	</div>

	<Modal v-model:visible="modalvisible" :modalProps="modalprops" />
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

const authstore = useAuthStore()
const { showAlert: showalert, showError: showerror, alertMessage: alertmessage, vAlert: valert, vAlertError: valerterror } = useAlerts()
const { resetForm: resetform } = useFormReset()

const searchform = reactive({ acctcd: '', acctcd_t: '', custgbn: '' })
const masterdata = reactive<any>({
	actkind: 'I1', acctcd: '', acctcd_t: '', custgbn: '', gujano: '', bankcd: '', bankcd_t: '',
	stdymd: '', endymd: '', wonamt: 0, rate: 0, payamt: 0, paydd: '', remark: '', useyn: 'Y', cmpycd: authstore.cmpycd
})

const maingridelement = ref<HTMLDivElement | null>(null)
let maingrid: Tabulator | null = null

const formatymd = (v: string) => v && v.length === 8 ? `${v.substring(0, 4)}-${v.substring(4, 6)}-${v.substring(6, 8)}` : v

const search = async () => {
	if (!searchform.acctcd_t) return valert('검색하고자 하는 계정과목을 선택해 주십시오.')
	try {
		const res = await api.post('/haba/haba_110u_str', {
			actkind: 'S1', cmpycd: authstore.cmpycd, acctcd: searchform.acctcd,
			wonamt: 0, rate: 0, payamt: 0
		})
		const processed = (res.data || []).map((n: any) => {
			return {
				...n,
				stdymd_fmt: formatymd(n.stdymd),
				endymd_fmt: formatymd(n.endymd)
			}
		})
		maingrid?.setData(processed)
		valert('조회되었습니다.')
	} catch (e) { valerterror('조회 중 오류 발생') }
}

const save = async () => {
	if (!masterdata.acctcd) return valert('계정코드를 기재해 주십시오.')
	if (!masterdata.gujano) return valert('계좌번호를 기재해 주십시오.')
	if (!masterdata.bankcd) return valert('개설기관을 선택해 주십시오.')

	if (!confirm('저장하시겠습니까?')) return
	try {
		const payload = {
			...masterdata,
			cmpycd: authstore.cmpycd,
			userid: authstore.userid,
			stdymd: (masterdata.stdymd || '').replace(/-/g, ''),
			endymd: (masterdata.endymd || '').replace(/-/g, '')
		}
		const res = await api.post('/haba/haba_110u_str', payload)
		const resdata = res.data?.[0] || {};
		if (resdata.ret_yn === 'Y' || resdata.result === 'N') valerterror(resdata.ret_msg || resdata.msg || '저장 실패')
		else { valert('정상적으로 처리되었습니다.'); search(); initialize() }
	} catch (e) { valerterror('저장 실패') }
}

const initialize = () => {
	resetform(masterdata)
	Object.assign(masterdata, { actkind: 'I1', cmpycd: authstore.cmpycd, useyn: 'Y', acctcd: searchform.acctcd, acctcd_t: searchform.acctcd_t, custgbn: searchform.custgbn })
}

const modalvisible = ref(false)
const modalprops = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function openhelp(type: string) {
	if (type === 'search_acct') {
		Object.assign(modalprops, {
			title: '계정과목 선택', path: '/ha00/ha00_00p_str',
			data: { gubun: 'A6', cmpycd: authstore.cmpycd, gbncd:'023' },
			columns: [{ title: '코드', field: 'acctcd', width: 80 }, { title: '계정명', field: 'acctnm', width: 180 }],
			onConfirm: (d: any) => {
				searchform.acctcd = d.acctcd; searchform.acctcd_t = d.acctnm;
				initialize();
			}
		})
	} else if (type === 'bank') {
		if (!masterdata.acctcd) return valert('조회 후 입력하시기 바랍니다.')
		Object.assign(modalprops, {
			title: '금융기관 선택', path: '/ha00/ha00_00p_str',
			data: { gubun: 'C3', cmpycd: authstore.cmpycd },
			columns: [{ title: '코드', field: 'custcd', width: 80 }, { title: '금융기관명', field: 'custnm', width: 180 }],
			onConfirm: (d: any) => {
				masterdata.bankcd = d.custcd; masterdata.bankcd_t = d.custnm
			}
		})
	}
	modalvisible.value = true
}

onMounted(() => {
	if (maingridelement.value) {
		maingrid = new Tabulator(maingridelement.value, {
			layout: 'fitColumns', height: '100%', selectable: 1,
			columnDefaults: { headerSort: false, vertAlign: "middle", headerHozAlign: "center", hozAlign: "center" },
			columns: [
				{ title: "계좌번호", field: "gujano", width: 200, hozAlign: "center", cssClass: "fw-bold" },
				{ title: "개설기관", field: "banknm", minWidth: 180, hozAlign: "left" },
				{ title: "개설일", field: "stdymd_fmt", width: 120, hozAlign: "center" },
				{ title: "만기일", field: "endymd_fmt", width: 120, hozAlign: "center" },
				{ title: "계약금액", field: "wonamt", width: 130, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
				{ title: "불입금액", field: "payamt", width: 130, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
				{ title: "이율", field: "rate", width: 100, hozAlign: "right", formatter: (c) => c.getValue() ? c.getValue() + "%" : "" },
                { title: "사용", field: "useyn", width: 80, hozAlign: "center",
                  formatter: (cell) => {
                    const val = String(cell.getValue() || '').trim().toUpperCase();
                    return val === 'Y' ? '<b class="text-primary">사용</b>' : '';
                  }
                }
			]
		})
		maingrid.on("rowClick", (e, row) => {
			const d = row.getData()
			Object.assign(masterdata, d);
			masterdata.actkind = 'U1';
			masterdata.acctcd_t = d.acctnm; masterdata.bankcd_t = d.banknm;
            // 날짜 포맷팅 (YYYY-MM-DD 형태로 변환하여 input[type=date]에 바인딩)
			masterdata.stdymd = d.stdymd ? `${d.stdymd.substring(0,4)}-${d.stdymd.substring(4,6)}-${d.stdymd.substring(6,8)}` : '';
            masterdata.endymd = d.endymd ? `${d.endymd.substring(0,4)}-${d.endymd.substring(4,6)}-${d.endymd.substring(6,8)}` : '';
		})
	}
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
