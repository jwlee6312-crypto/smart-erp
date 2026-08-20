<!--	=============================================================
	프로그램명 : 매출-수정세금계산서
	작성일자	: 2025.02.24
	작성자    : AI Assistant
	설명        : 매출 세금계산서의 수정 발행 및 전표 연계 처리 (HSOD100U 표준 UI 적용)
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container">
		<!-- [Header] 액션 바 -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-file-earmark-diff me-2 text-primary" style="font-size: 18px;"></i>
				세무관리<i class="bi bi-chevron-right mx-1 small opacity-50"></i>
				<span class="text-primary fw-bolder">매출-수정세금계산서 (HATX210U)</span>
			</div>
			<div class="btn-group-erp d-flex gap-1">
				<button class="btn-erp btn-init" @click="initialize">초기화</button>
				<button class="btn-erp btn-search" @click="search">조회</button>
				<button v-if="masterForm.taxno" class="btn-erp btn-delete" @click="handleDelete">삭제</button>
				<button class="btn-erp btn-save" @click="save">저장</button>
				<button v-if="masterForm.taxno" class="btn-erp btn-print" @click="print">인쇄</button>
			</div>
		</div>

		<!-- [Search] 검색 조건 -->
		<div class="p-2 pb-0 flex-shrink-0">
			<div class="card border shadow-sm bg-white overflow-hidden">
				<div class="card-body p-0">
					<table class="erp-table-dense w-100">
						<colgroup>
							<col style="width: 80px;" /><col style="width: 180px;" />
							<col style="width: 80px;" /><col style="width: 250px;" />
							<col style="width: 80px;" /><col />
						</colgroup>
						<tbody>
							<tr>
								<th class="bg-light text-center">사업장</th>
								<td>
									<select v-model="searchForm.taxunit" class="form-select form-select-sm">
										<option v-for="opt in taxUnitOptions" :key="opt.code" :value="opt.code">{{ opt.name }}</option>
									</select>
								</td>
								<th class="bg-light text-center">발행일</th>
								<td>
									<div class="d-flex align-items-center gap-1">
										<input type="date" v-model="searchForm.frymd" class="form-control form-control-sm" />
										<span>~</span>
										<input type="date" v-model="searchForm.toymd" class="form-control form-control-sm" />
									</div>
								</td>
								<th class="bg-light text-center">거래처</th>
								<td>
									<div class="input-group input-group-sm">
										<input type="text" v-model="searchForm.custnm" class="form-control" placeholder="거래처 검색" @keydown.enter="openCustHelp('search')" />
										<button class="btn btn-outline-secondary" @click="openCustHelp('search')"><i class="bi bi-search"></i></button>
									</div>
								</td>
							</tr>
						</tbody>
					</table>
				</div>
			</div>
		</div>

		<!-- [Content] 메인 폼 영역 -->
		<div class="flex-grow-1 overflow-auto p-2 d-flex flex-column gap-3 bg-light main-content-wrapper">

			<!-- 1. 마스터 정보 -->
			<div class="card border shadow-sm bg-white overflow-hidden">
				<div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
					<span class="fw-bold small text-dark"><i class="bi bi-info-circle me-1 text-primary"></i> 세금계산서 기본 정보</span>
					<div v-if="masterForm.sslipno" class="small text-danger fw-bold">
						연계 전표: {{ masterForm.sslipno }}
						<button class="btn btn-xs btn-outline-danger ms-2" @click="deleteSlip">전표삭제</button>
					</div>
				</div>
				<div class="card-body p-0">
					<table class="erp-table-dense w-100">
						<colgroup>
							<col style="width: 100px;" /><col style="width: 220px;" />
							<col style="width: 100px;" /><col style="width: 220px;" />
							<col style="width: 100px;" /><col style="width: 220px;" />
							<col style="width: 100px;" /><col />
						</colgroup>
						<tbody>
							<tr>
								<th class="bg-light text-center required">사업장</th>
								<td>
									<select v-model="masterForm.taxunit" class="form-select form-select-sm" disabled>
										<option v-for="opt in taxUnitOptions" :key="opt.code" :value="opt.code">{{ opt.name }}</option>
									</select>
								</td>
								<th class="bg-light text-center required">거래처</th>
								<td>
									<div class="input-group input-group-sm">
										<input type="text" v-model="masterForm.custnm" class="form-control" @keydown.enter="openCustHelp('master')" />
										<button class="btn btn-outline-secondary" @click="openCustHelp('master')"><i class="bi bi-search"></i></button>
									</div>
								</td>
								<th class="bg-light text-center">사업자번호</th>
								<td><input type="text" v-model="masterForm.custno" class="form-control form-control-sm bg-light" readonly /></td>
								<th class="bg-light text-center required">발행일</th>
								<td><input type="date" v-model="masterForm.pubymd" class="form-control form-control-sm" /></td>
							</tr>
							<tr>
								<th class="bg-light text-center required">유 형</th>
								<td>
									<select v-model="masterForm.taxtype" class="form-select form-select-sm">
										<option value="0">선택</option>
										<option v-for="opt in taxTypeOptions" :key="opt.code" :value="opt.code">{{ opt.name }}</option>
									</select>
								</td>
								<th class="bg-light text-center">공급가액</th>
								<td><input type="text" :value="nf(masterForm.supyamt)" class="form-control form-control-sm text-end bg-light fw-bold" readonly /></td>
								<th class="bg-light text-center">부가세</th>
								<td><input type="text" :value="nf(masterForm.vatamt)" class="form-control form-control-sm text-end bg-light text-danger" readonly /></td>
								<th class="bg-light text-center">합 계</th>
								<td><input type="text" :value="nf(masterForm.amtsum)" class="form-control form-control-sm text-end bg-light text-primary fw-bolder" readonly /></td>
							</tr>
							<tr>
								<th class="bg-light text-center required">적 요</th>
								<td colspan="3"><input type="text" v-model="masterForm.descnm" class="form-control form-control-sm" maxlength="50" /></td>
								<th class="bg-light text-center">대표자</th>
								<td><input type="text" v-model="masterForm.bossnm" class="form-control form-control-sm bg-light" readonly /></td>
								<th class="bg-light text-center">접수확인</th>
								<td>
									<div class="form-check form-check-inline mt-1">
										<input type="checkbox" v-model="masterForm.useyn" class="form-check-input" true-value="Y" false-value="N" />
										<label class="form-check-label small">접수 완료</label>
									</div>
								</td>
							</tr>
							<tr>
								<th class="bg-light text-center">업 태</th>
								<td colspan="3"><input type="text" v-model="masterForm.custtype" class="form-control form-control-sm" /></td>
								<th class="bg-light text-center">종 목</th>
								<td colspan="3"><input type="text" v-model="masterForm.custkind" class="form-control form-control-sm" /></td>
							</tr>
							<tr>
								<th class="bg-light text-center">주 소</th>
								<td colspan="7"><input type="text" v-model="masterForm.adrs" class="form-control form-control-sm" /></td>
							</tr>
						</tbody>
					</table>
				</div>
			</div>

			<!-- 2. 수정 사유 및 발행일 -->
			<div class="card border shadow-sm bg-white overflow-hidden border-primary border-opacity-25">
				<div class="card-header bg-primary bg-opacity-10 py-1 px-3 border-bottom border-primary border-opacity-25">
					<span class="fw-bold small text-primary"><i class="bi bi-pencil-square me-1"></i> 수정 발행 정보</span>
				</div>
				<div class="card-body p-0">
					<table class="erp-table-dense w-100">
						<colgroup>
							<col style="width: 100px;" /><col style="width: 250px;" />
							<col style="width: 100px;" /><col />
						</colgroup>
						<tbody>
							<tr>
								<th class="bg-light text-center required">수정사유</th>
								<td>
									<select v-model="masterForm.taxupdcd" class="form-select form-select-sm border-primary border-opacity-50">
										<option v-for="opt in updateReasonOptions" :key="opt.code" :value="opt.code">{{ opt.name }}</option>
									</select>
								</td>
								<th class="bg-light text-center required">당초 발행일</th>
								<td>
									<div class="d-flex align-items-center gap-3">
										<input type="date" v-model="masterForm.bfymd" class="form-control form-control-sm border-primary border-opacity-50" style="width: 150px;" />
										<span class="text-muted x-small">(환입/공급가차감: 당초 발행일, 내국신용장: 개설일, 계약해제: 해제일)</span>
									</div>
								</td>
							</tr>
						</tbody>
					</table>
				</div>
			</div>

			<!-- 3. 상세 내역 (그리드) -->
			<div class="card border shadow-sm flex-shrink-0 bg-white" style="min-height: 250px;">
				<div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
					<span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-1 text-primary"></i> 상세 품목 내역</span>
					<div class="small text-muted">※ 수정 품목 및 금액을 입력하세요.</div>
				</div>
				<div class="card-body p-0 overflow-hidden">
					<div ref="itemGridRef" class="tabulator-instance"></div>
				</div>
			</div>

			<!-- 4. 전표 생성 정보 -->
			<div class="card border shadow-sm bg-white overflow-hidden" v-if="!masterForm.sslipno">
				<div class="card-header bg-light-subtle py-1 px-3 border-bottom d-flex align-items-center gap-3">
					<span class="fw-bold small text-dark"><i class="bi bi-journal-plus me-1 text-success"></i> 전표 생성 옵션</span>
					<div class="form-check form-check-inline mb-0">
						<input type="checkbox" v-model="masterForm.slipyn" class="form-check-input" true-value="Y" false-value="N" />
						<label class="form-check-label small fw-bold text-success">자동 전표 생성</label>
					</div>
				</div>
				<div class="card-body p-0" v-if="masterForm.slipyn === 'Y'">
					<table class="erp-table-dense w-100">
						<colgroup>
							<col style="width: 100px;" /><col style="width: 250px;" />
							<col style="width: 100px;" /><col />
						</colgroup>
						<tbody>
							<tr>
								<th class="bg-light text-center required">발행부서</th>
								<td>
									<div class="input-group input-group-sm">
										<input type="text" v-model="slipForm.deptnm" class="form-control" @keydown.enter="openDeptHelp" />
										<button class="btn btn-outline-secondary" @click="openDeptHelp"><i class="bi bi-search"></i></button>
									</div>
								</td>
								<th class="bg-light text-center required">매입계정</th>
								<td>
									<div class="input-group input-group-sm">
										<input type="text" v-model="slipForm.acctnm" class="form-control" @keydown.enter="openAcctHelp" />
										<button class="btn btn-outline-secondary" @click="openAcctHelp"><i class="bi bi-search"></i></button>
									</div>
								</td>
							</tr>
						</tbody>
					</table>
					<!-- 지불 정보 그리드 (2행 고정) -->
					<div ref="payGridRef" class="tabulator-instance border-top"></div>
				</div>
			</div>

		</div>
	</div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick, watch } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import AppAlert from '@/components/AppAlert.vue'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useCommonHelp } from '@/composables/useCommonHelp'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { openHelp } = useCommonHelp()

const now = new Date()
const currentYear = now.getFullYear()
const currentMonth = String(now.getMonth() + 1).padStart(2, '0')
const today = now.toISOString().split('T')[0]

const taxUnitOptions = ref<any[]>([])
const taxTypeOptions = ref<any[]>([])
const updateReasonOptions = ref<any[]>([])
const yearOptions = Array.from({ length: 6 }, (_, i) => String(currentYear - i))
const monthOptions = Array.from({ length: 12 }, (_, i) => String(i + 1).padStart(2, '0'))

const searchForm = reactive({
	taxunit: '000',
	frymd: `${currentYear}-${currentMonth}-01`,
	toymd: today,
	custnm: '',
	custcd: ''
})

const masterForm = reactive({
	taxno: '', taxym: '', actkind: 'I1',
	taxunit: '000', custcd: '', custnm: '', custno: '', pubymd: today,
	taxtype: '0', supyamt: 0, vatamt: 0, amtsum: 0,
	descnm: '', bossnm: '', useyn: 'N', custtype: '', custkind: '', adrs: '',
	taxupdcd: '01', bfymd: today,
	slipyn: 'N', sslipno: '', sexists: '', slipdelyn: 'N'
})

const slipForm = reactive({
	deptcd: authStore.deptcd || '', deptnm: authStore.deptnm || '',
	acctcd: '', acctnm: ''
})

const itemGridRef = ref<HTMLElement | null>(null)
const payGridRef = ref<HTMLElement | null>(null)
let itemGrid: Tabulator | null = null
let payGrid: Tabulator | null = null

const fetchOptions = async () => {
	try {
		const resUnit = await api.post('/ha00/HA00_00P_STR', { gubun: 'SA', cmpycd: authStore.cmpycd })
		taxUnitOptions.value = (resUnit.data || []).map((i: any) => ({ code: i.taxunit, name: i.unitnm }))
		if (taxUnitOptions.value.length > 0) {
			searchForm.taxunit = taxUnitOptions.value[0].code
			masterForm.taxunit = taxUnitOptions.value[0].code
		}

		const resType = await api.post('/ha00/HA00_00P_STR', { gubun: 'E0', cmpycd: authStore.cmpycd, gbncd: '120' })
		taxTypeOptions.value = (resType.data || []).map((i: any) => ({ code: i.codecd, name: i.codenm }))

		const resReason = await api.post('/ha00/HA00_00P_STR', { gubun: 'E0', cmpycd: authStore.cmpycd, gbncd: '131' })
		updateReasonOptions.value = (resReason.data || []).filter((i:any) => i.codecd !== '00').map((i: any) => ({ code: i.codecd, name: i.codenm }))
	} catch (e) { console.error(e) }
}

async function search() {
	// 💡 실제로는 목록 팝업을 띄우거나 첫 번째 데이터를 로드하는 로직이 필요함 (ASP S2 기반)
	vAlert('조회 조건에 맞는 데이터를 선택하세요. (기능 준비 중)')
}

const save = async () => {
	if (!masterForm.custcd) return vAlertError('거래처를 선택하세요.')
	if (masterForm.taxtype === '0') return vAlertError('세무 유형을 선택하세요.')
	if (!masterForm.descnm.trim()) return vAlertError('적요를 입력하세요.')

	try {
		const items = itemGrid?.getData().filter((i:any) => i.itemnm) || []
		if (items.length === 0) return vAlertError('최소 하나 이상의 품목 내역이 필요합니다.')

		// 🚀 수정세금계산서 통합 저장 루틴
		const payload = {
			slipyn: masterForm.slipyn,
			master: {
				...masterForm,
				pubymd: masterForm.pubymd.replace(/-/g, ''),
				bfymd: masterForm.bfymd.replace(/-/g, ''),
				supyamt: String(masterForm.supyamt).replace(/,/g, ''),
				vatamt: String(masterForm.vatamt).replace(/,/g, ''),
				amtsum: String(masterForm.amtsum).replace(/,/g, ''),
				// 전표 연동 데이터 (매출과 동일 구조)
				deptcd: slipForm.deptcd,
				acctcd: slipForm.acctcd,
				payItems: payGrid?.getData() || []
			},
			items: items.map((it: any) => ({
				...it,
				ymd: it.ymd.replace(/-/g, ''),
				amt: String(it.amt).replace(/,/g, ''),
				vat: String(it.vat).replace(/,/g, '')
			}))
		}

		const res = await api.post('/hatx/save-corrected-sales', payload)
		if (res.data?.res === 'OK') {
			vAlert('성공적으로 저장되었습니다.')
			initialize()
			search()
		}
	} catch (e: any) {
		vAlertError(e.message || '저장 중 오류 발생')
	}
}

const handleDelete = async () => {
	if (!confirm('해당 수정 세금계산서를 삭제하시겠습니까?')) return
	try {
		await api.post('/hatx/HATX_210U_STR', { actkind: 'D1', taxno: masterForm.taxno, taxym: masterForm.taxym })
		vAlert('삭제되었습니다.')
		initialize()
	} catch (e) { vAlertError('삭제 실패') }
}

const initialize = () => {
	Object.assign(masterForm, {
		taxno: '', taxym: '', actkind: 'I1',
		custcd: '', custnm: '', custno: '', pubymd: today,
		taxtype: '0', supyamt: 0, vatamt: 0, amtsum: 0,
		descnm: '', bossnm: '', useyn: 'N', custtype: '', custkind: '', adrs: '',
		taxupdcd: '01', bfymd: today,
		slipyn: 'N', sslipno: '', sexists: '', slipdelyn: 'N'
	})
	itemGrid?.setData(Array.from({ length: 4 }, () => ({ ymd: today, itemnm: '', qty: '', price: '', amt: 0, vat: 0, bigo: '' })))
}

const openCustHelp = (target: 'search'|'master') => {
	openHelp({
		title: '거래처 선택', path: '/ha00/HA00_00P_STR',
		params: { gubun: '010', SEARCH: target === 'search' ? searchForm.custnm : masterForm.custnm },
		columns: [{ title: '코드', field: 'custcd', width: 100 }, { title: '거래처명', field: 'custnm', width: 200 }, { title: '대표자', field: 'bossnm', width: 100 }],
		onSelect: (row) => {
			if (target === 'search') {
				searchForm.custcd = row.custcd; searchForm.custnm = row.custnm
			} else {
				masterForm.custcd = row.custcd; masterForm.custnm = row.custnm;
				masterForm.custno = row.saupno; masterForm.bossnm = row.bossnm;
				masterForm.adrs = row.address; masterForm.custtype = row.uptae; masterForm.custkind = row.upjong;
			}
		}
	})
}

const openDeptHelp = () => {
	openHelp({
		title: '부서 선택', path: '/ha00/HA00_00P_STR', params: { gubun: '030', SEARCH: slipForm.deptnm },
		onSelect: (row) => { slipForm.deptcd = row.deptcd; slipForm.deptnm = row.deptnm }
	})
}

const openAcctHelp = () => {
	openHelp({
		title: '계정 선택', path: '/ha00/HA00_00P_STR', params: { gubun: 'ACCT', acct: '112', SEARCH: slipForm.acctnm },
		onSelect: (row) => { slipForm.acctcd = row.acctcd; slipForm.acctnm = row.acctnm }
	})
}

const initGrids = () => {
	itemGrid = new Tabulator(itemGridRef.value!, {
		layout: "fitColumns", height: 180, headerSort: false,
		data: Array.from({ length: 4 }, () => ({ ymd: today, itemnm: '', qty: '', price: '', amt: 0, vat: 0, bigo: '' })),
		columns: [
			{ title: "일자", field: "ymd", width: 120, editor: "input", hozAlign: "center" },
			{ title: "품명", field: "itemnm", widthGrow: 2, editor: "input" },
			{ title: "수량", field: "qty", width: 80, editor: "input", hozAlign: "right" },
			{ title: "단가", field: "price", width: 120, editor: "input", hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
			{ title: "공급가", field: "amt", width: 130, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
			{ title: "세액", field: "vat", width: 110, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
			{ title: "비고", field: "bigo", widthGrow: 1, editor: "input" }
		]
	})

	itemGrid.on("cellEdited", (cell) => {
		const field = cell.getField()
		if (['qty', 'price'].includes(field)) {
			const row = cell.getRow().getData()
			const amt = Math.floor(Number(row.qty || 0) * Number(row.price || 0))
			const vat = Math.floor(amt * 0.1)
			cell.getRow().update({ amt, vat })
			updateTotals()
		}
	})
}

const updateTotals = () => {
	const data = itemGrid?.getData() || []
	masterForm.supyamt = data.reduce((sum, i) => sum + Number(i.amt || 0), 0)
	masterForm.vatamt = data.reduce((sum, i) => sum + Number(i.vat || 0), 0)
	masterForm.amtsum = masterForm.supyamt + masterForm.vatamt
}

const nf = (v: any) => v ? Number(v).toLocaleString() : '0'

onMounted(() => {
	nextTick(() => {
		fetchOptions()
		initGrids()
	})
})
</script>

<style scoped>
:deep(.tabulator-cell) { border-right: 1px solid #dee2e6 !important; font-size: 12px; }
:deep(.tabulator-header .tabulator-col) { border-right: 1px solid #dee2e6 !important; background-color: #f8f9fa !important; font-size: 12px; }
.main-content-wrapper { min-height: 0; }
.x-small { font-size: 10px; }
</style>
