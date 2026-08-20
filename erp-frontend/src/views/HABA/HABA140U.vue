<!--
	=============================================================
	프로그램명	: 법인카드관리 (haba140u)
	작성일자	: 2025.03.14
	설명        : 법인카드 정보 상세 관리 (완전 소문자 원칙 적용)
	=============================================================
-->

<template>
	<AppAlert :show="showalert" :error="showerror" :message="alertmessage" />

	<div class="erp-container">
		<!-- 🚀 상단 액션 바 -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-credit-card me-2 text-primary" style="font-size: 18px;"></i>
				기본정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
				<span class="text-primary fw-bolder">법인카드관리 (haba140u)</span>
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
			<div class="card border shadow-sm overflow-hidden bg-white">
				<table class="erp-table-full" style="table-layout: fixed;">
					<colgroup>
						<col style="width: 100px;" /><col style="width: 250px;" />
						<col />
					</colgroup>
					<tbody>
						<tr>
							<th class="text-center bg-light border-end">발행기관</th>
							<td class="px-2">
								<div class="input-group input-group-sm">
									<input v-model="searchform.bankcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
									<input v-model="searchform.bankcd_t" type="text" class="form-control" placeholder="금융기관 선택" @keydown.enter="openhelp('search_bank')" />
									<button class="btn btn-outline-secondary px-2" @click="openhelp('search_bank')"><i class="bi bi-search"></i></button>
								</div>
							</td>
							<td class="px-3 text-muted small">
								<i class="bi bi-info-circle me-1"></i> 검색하시려는 카드 발행 금융기관을 선택해 주십시오.
							</td>
						</tr>
					</tbody>
				</table>
			</div>
		</div>

		<!-- 📝 상세 정보 입력 영역 -->
		<div class="p-2 pb-0 flex-shrink-0 bg-light">
			<div class="card border shadow-sm bg-white overflow-hidden">
				<div class="card-header py-1 px-2 bg-light border-bottom">
					<span class="small fw-bold text-secondary"><i class="bi bi-pencil-square me-1"></i> 법인카드 상세 정보 [{{ masterform.actkind === 'i1' ? '신규' : '수정' }}]</span>
				</div>
				<table class="erp-table-full small border-0">
					<colgroup>
						<col style="width: 100px;" /><col style="width: 22%;" />
						<col style="width: 100px;" /><col style="width: 22%;" />
						<col style="width: 100px;" /><col />
					</colgroup>
					<tbody>
						<tr>
							<th class="text-center bg-light-subtle border-end">카드번호</th>
							<td class="bg-white border-end px-2 py-1">
								<input v-model="masterform.cardno" type="text" class="form-control form-control-sm" maxlength="19" :readonly="masterform.actkind === 'u1'" />
							</td>
							<th class="text-center bg-light-subtle border-end border-top">발행기관</th>
							<td class="bg-white border-end border-top px-2 py-1">
								<div class="input-group input-group-sm">
									<input v-model="masterform.bankcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
									<input v-model="masterform.bankcd_t" type="text" class="form-control" @keydown.enter="openhelp('bank')" />
									<button class="btn btn-outline-secondary px-2" @click="openhelp('bank')"><i class="bi bi-search"></i></button>
								</div>
							</td>
							<th class="text-center bg-light-subtle border-end border-top">카드명칭</th>
							<td class="bg-white border-top px-2 py-1">
								<input v-model="masterform.cardnm" type="text" class="form-control form-control-sm" maxlength="50" />
							</td>
						</tr>
						<tr>
							<th class="text-center bg-light-subtle border-end border-top">발행일자</th>
							<td class="bg-white border-end border-top px-2 py-1">
								<input v-model="masterform.stdymd" type="date" class="form-control form-control-sm" />
							</td>
							<th class="text-center bg-light-subtle border-end border-top">유효년월</th>
							<td class="bg-white border-end border-top px-2 py-1">
								<div class="d-flex align-items-center gap-1">
									<input v-model="masterform.sndymd_yy" type="text" class="form-control form-control-sm text-center" maxlength="4" style="width: 65px;" placeholder="YYYY" />
									<span class="small fw-bold">년</span>
									<select v-model="masterform.sndymd_mm" class="form-select form-select-sm" style="width: 70px;">
										<option value="00">선택</option>
										<option v-for="m in 12" :key="m" :value="String(m).padStart(2, '0')">{{ String(m).padStart(2, '0') }}</option>
									</select>
									<span class="small fw-bold">월</span>
								</div>
							</td>
							<th class="text-center bg-light-subtle border-end border-top">결제일자</th>
							<td class="bg-white border-top px-2 py-1">
								<div class="d-flex align-items-center gap-1">
									<select v-model="masterform.chkdd" class="form-select form-select-sm" style="width: 80px;">
										<option value="00">선택</option>
										<option v-for="d in 31" :key="d" :value="String(d).padStart(2, '0')">{{ String(d).padStart(2, '0') }}</option>
									</select>
									<span class="small fw-bold">일</span>
								</div>
							</td>
						</tr>
						<tr>
							<th class="text-center bg-light-subtle border-end border-top">소유자</th>
							<td class="bg-white border-end border-top px-2 py-1">
								<input v-model="masterform.soname" type="text" class="form-control form-control-sm" maxlength="20" />
							</td>
							<th class="text-center bg-light-subtle border-end border-top">결제계좌</th>
							<td class="bg-white border-end border-top px-2 py-1">
								<div class="input-group input-group-sm">
									<input v-model="masterform.gujano" type="text" class="form-control" @keydown.enter="openhelp('guja')" />
									<button class="btn btn-outline-secondary px-2" @click="openhelp('guja')"><i class="bi bi-search"></i></button>
								</div>
							</td>
							<th class="text-center bg-light-subtle border-end border-top">사용여부</th>
							<td class="bg-white border-top px-3">
								<div class="form-check form-switch pt-1">
									<input v-model="masterform.useyn" class="form-check-input" type="checkbox" id="usecheck" true-value="Y" false-value="N">
									<label class="form-check-label small fw-bold" for="usecheck">사용</label>
								</div>
							</td>
						</tr>
						<tr>
							<th class="text-center bg-light-subtle border-end border-top">비&nbsp;&nbsp;&nbsp;&nbsp;고</th>
							<td colspan="5" class="bg-white border-top px-2 py-1">
								<input v-model="masterform.remark" type="text" class="form-control form-control-sm" maxlength="50" />
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

// 🔍 검색 데이터
const searchform = reactive({
	bankcd: '',
	bankcd_t: ''
})

// 📝 마스터 데이터
const masterform = reactive({
	actkind: 'i1',
	cardno: '',
	bankcd: '',
	bankcd_t: '',
	cardnm: '',
	stdymd: '',
	sndymd_yy: '',
	sndymd_mm: '00',
	chkdd: '00',
	soname: '',
	gujano: '',
	remark: '',
	useyn: 'Y'
})

const maingridelement = ref<HTMLDivElement | null>(null)
let maingrid: Tabulator | null = null

const formatymd = (v: string) => v && v.length === 8 ? `${v.substring(0, 4)}-${v.substring(4, 6)}-${v.substring(6, 8)}` : v

const search = async () => {
	try {
		const res = await api.post('/haba/HABA_140U_STR', {
			actkind: searchform.bankcd ? 'SR' : 'S1',
			cmpycd: authstore.cmpycd,
			bankcd: searchform.bankcd
		})

		const processeddata = (res.data || []).map((n: any) => {
			return {
				cardno: n.cardno,
				bankcd: n.bankcd,
				banknm: n.banknm,
				cardnm: n.cardnm,
				stdymd: formatymd(n.stdymd),
				sndymd_yy: n.sndymd_yy,
				sndymd_mm: n.sndymd_mm,
				chkdd: n.chkdd,
				soname: n.soname,
				gujano: n.gujano,
				remark: n.remark,
				useyn: n.useyn
			}
		})

		maingrid?.setData(processeddata)
		if (processeddata.length > 0) valert('조회되었습니다.')
		else valert('조회된 데이터가 없습니다.')
	} catch (e) { valerterror('조회 중 오류 발생') }
}

const save = async () => {
	if (!masterform.cardno) return valert('카드번호를 정확히 입력하세요.')
	if (!masterform.bankcd) return valert('발행기관을 선택해 주십시오.')
	if (!masterform.cardnm) return valert('카드명을 입력해 주십시오.')
	if (!masterform.stdymd) return valert('발행일자를 선택해 주십시오.')
	if (!masterform.sndymd_yy || isNaN(Number(masterform.sndymd_yy))) return valert('유효년도를 숫자로 입력해 주십시오.')
	if (masterform.sndymd_mm === '00') return valert('유효월을 선택해 주십시오.')
	if (masterform.chkdd === '00') return valert('결제일을 선택해 주십시오.')
	if (!masterform.soname) return valert('소유자를 입력해 주십시오.')
	if (!masterform.gujano) return valert('결제계좌를 입력해 주십시오.')

    if (!confirm('저장하시겠습니까?')) return
	try {
		const payload = {
			...masterform,
			cmpycd: authstore.cmpycd,
			userid: authstore.userid,
			stdymd: masterform.stdymd.replace(/-/g, ''),
			sndymd: masterform.sndymd_yy + masterform.sndymd_mm
		}

		const res = await api.post('/haba/HABA_140U_STR', payload)
		const resdata = res.data?.[0] || {};

		if (resdata.ret_yn === 'Y' || resdata.result === 'N') {
			valerterror(resdata.ret_msg || resdata.msg || '저장 실패')
		} else {
			valert('성공적으로 저장되었습니다.')
			search()
			initialize()
		}
	} catch (e) { valerterror('저장 중 오류 발생') }
}

const initialize = () => {
	resetform(masterform)
	masterform.actkind = 'i1'
	masterform.sndymd_mm = '00'
	masterform.chkdd = '00'
	masterform.useyn = 'Y'
	masterform.cmpycd = authstore.cmpycd
}

// 팝업 설정
const modalvisible = ref(false)
const modalprops = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function openhelp(type: string) {
	if (type === 'search_bank') {
		Object.assign(modalprops, {
			title: '발행기관 선택', path: '/ha00/HA00_00P_STR',
			data: { gubun: 'C3', cmpycd: authstore.cmpycd, code: searchform.bankcd_t },
			columns: [{ title: '코드', field: 'bankcd', width: 80 }, { title: '금융기관명', field: 'banknm', width: 180 }],
			onConfirm: (d: any) => {
				searchform.bankcd = d.bankcd; searchform.bankcd_t = d.banknm; search();
			}
		})
	} else if (type === 'bank') {
		Object.assign(modalprops, {
			title: '발행기관 선택', path: '/ha00/HA00_00P_STR',
			data: { gubun: 'C3', cmpycd: authstore.cmpycd, code: masterform.bankcd_t },
			columns: [{ title: '코드', field: 'bankcd', width: 80 }, { title: '금융기관명', field: 'banknm', width: 180 }],
			onConfirm: (d: any) => {
				masterform.bankcd = d.bankcd; masterform.bankcd_t = d.banknm
			}
		})
	} else if (type === 'guja') {
		Object.assign(modalprops, {
			title: '결제계좌 선택', path: '/ha00/HA00_00P_STR',
			data: { gubun: 'M0', cmpycd: authstore.cmpycd, code: masterform.gujano, gbncd: '010', remark: '1120' },
			columns: [{ title: '계좌번호', field: 'mgtno', width: 150 }, { title: '계좌명칭', field: 'mgtnm', width: 150 }],
			onConfirm: (d: any) => {
				masterform.gujano = d.mgtno
			}
		})
	}
	modalvisible.value = true
}

onMounted(() => {
	if (maingridelement.value) {
		maingrid = new Tabulator(maingridelement.value, {
			layout: 'fitColumns',
			height: '100%',
			columnDefaults: { headerSort: false, vertAlign: "middle", headerHozAlign: "center", hozAlign: "center" },
			columns: [
				{ title: "카드번호", field: "cardno", width: 170, hozAlign: "center", cssClass: "fw-bold" },
				{ title: "발행기관", field: "banknm", width: 150, hozAlign: "left" },
				{ title: "발행일", field: "stdymd", width: 110 },
				{
					title: "유효년월", field: "sndymd", width: 100,
					formatter: (c) => {
						const d = c.getData()
						return d.sndymd_yy ? `${d.sndymd_yy}-${d.sndymd_mm}` : ''
					}
				},
				{ title: "결제일", field: "chkdd", width: 70, formatter: (c) => c.getValue() ? c.getValue() + "일" : "" },
				{ title: "소유자", field: "soname", width: 100 },
				{ title: "결제계좌", field: "gujano", width: 160, hozAlign: "left" },
				{ title: "비고", field: "remark", minWidth: 150, hozAlign: "left" },
                { title: "사용", field: "useyn", width: 70, hozAlign: "center",
                  formatter: (cell) => {
                    const val = String(cell.getValue() || '').trim().toUpperCase();
                    return val === 'Y' ? '<span class="badge bg-primary">Y</span>' : '<span class="badge bg-light text-dark">N</span>';
                  }
                }
			]
		})
		maingrid.on('rowClick', (e, row) => {
			const d = row.getData()
			Object.assign(masterform, d)
			masterform.actkind = 'u1'
			masterform.bankcd_t = d.banknm
		})
	}
	nextTick(search);
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
