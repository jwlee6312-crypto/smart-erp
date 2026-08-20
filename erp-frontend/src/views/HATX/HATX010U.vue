<!--	=============================================================
	프로그램명	: 매입부가세관리
	작성일자	: 2025.02.24
	작성자	    : AI Assistant
	설명        : 사업장별 매입 부가세 내역 조회 및 관리 (E0 표준 및 단순화 적용)
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container d-flex flex-column vh-100 bg-light">
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-receipt me-2 text-primary" style="font-size: 18px;"></i>
				세무관리 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				<span class="text-primary fw-bolder">매입부가세관리 (HATX010U)</span>
			</div>
			<div class="btn-group-erp d-flex gap-1">
				<button class="btn-erp btn-new" @click="handleNew">
					<i class="bi bi-plus-lg"></i> 신규
				</button>
				<button class="btn-erp btn-search" @click="search">
					<i class="bi bi-search"></i> 조회
				</button>
			</div>
		</div>

		<!-- 🔍 검색 조건 영역 -->
		<div class="p-2 pb-0 flex-shrink-0">
			<div class="card border shadow-sm bg-white overflow-hidden">
				<div class="card-body p-2 bg-light">
					<div class="d-flex align-items-center flex-wrap gap-3 small">
						<div class="d-flex align-items-center">
							<span class="erp-label"><i class="bi bi-dot"></i>사업장</span>
							<select v-model="searchForm.taxunit" class="form-select form-select-sm" style="width: 150px;">
								<option v-for="opt in taxUnitOptions" :key="opt.code" :value="opt.code">{{ opt.name }}</option>
							</select>
						</div>
						<div class="d-flex align-items-center">
							<span class="erp-label"><i class="bi bi-dot"></i>유형</span>
							<select v-model="searchForm.taxtype" class="form-select form-select-sm" style="width: 120px;">
								<option value="000">전체</option>
								<option v-for="opt in taxTypeOptions" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
							</select>
						</div>
						<div class="d-flex align-items-center">
							<span class="erp-label"><i class="bi bi-dot"></i>발행일</span>
							<div class="d-flex align-items-center gap-1">
								<input v-model="searchForm.fromdt" type="date" class="form-control form-control-sm" style="width: 135px;" />
								<span>~</span>
								<input v-model="searchForm.todt" type="date" class="form-control form-control-sm" style="width: 135px;" />
							</div>
						</div>
					</div>
				</div>
			</div>
		</div>

		<!-- 📊 메인 영역 (그리드 + 우측 상세) -->
		<div class="flex-grow-1 overflow-hidden p-2 d-flex gap-2">
			<!-- 그리드 영역 -->
			<div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white">
				<div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
					<div ref="mainGridRef" class="tabulator-instance flex-grow-1"></div>
				</div>
			</div>

			<!-- 📝 상세 정보 영역 (우측 패널) -->
			<div v-if="detailModalVisible" class="card border shadow-sm flex-shrink-0 d-flex flex-column bg-white side-detail-panel" style="width: 650px;">
				<div class="card-header bg-white py-2 border-bottom shadow-sm d-flex justify-content-between align-items-center sticky-top">
					<h5 class="fw-bold text-dark d-flex align-items-center m-0" style="font-size: 15px;">
						<i class="bi bi-pencil-square text-primary me-2"></i>매입부가세 상세정보
					</h5>
					<button type="button" class="btn-close small" @click="detailModalVisible = false"></button>
				</div>
				<div class="card-body p-3 bg-light overflow-auto">
					<div class="p-1 small">
						<div class="row g-2 mb-3">
							<div class="col-md-6">
								<div class="d-flex align-items-center">
									<span class="erp-label-form required">사업장</span>
									<select v-model="formData.taxunit" class="form-select form-select-sm">
										<option v-for="opt in taxUnitOptions" :key="opt.code" :value="opt.code">{{ opt.name }}</option>
									</select>
								</div>
							</div>
							<div class="col-md-6">
								<div class="d-flex align-items-center">
									<span class="erp-label-form required">거래처</span>
									<div class="input-group input-group-sm">
										<input v-model="formData.custcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
										<input v-model="formData.custnm" type="text" class="form-control" @keydown.enter="openHelp('CUST')" />
										<button class="btn btn-outline-secondary px-2" @click="openHelp('CUST')"><i class="bi bi-search"></i></button>
									</div>
								</div>
							</div>
							<div class="col-md-6">
								<div class="d-flex align-items-center">
									<span class="erp-label-form">사업자번호</span>
									<input v-model="formData.custno" type="text" class="form-control form-control-sm bg-light" readonly />
								</div>
							</div>
							<div class="col-md-6 text-end">
								<div v-if="formData.sslipno" class="badge bg-info p-2 w-100">전표: {{ formData.sslipno }}</div>
							</div>

							<div class="col-md-6">
								<div class="d-flex align-items-center">
									<span class="erp-label-form required">발행일</span>
									<input v-model="formData.pubymd" type="date" class="form-control form-control-sm" />
								</div>
							</div>
							<div class="col-md-6">
								<div class="d-flex align-items-center">
									<span class="erp-label-form required">유형</span>
									<select v-model="formData.taxtype_val" class="form-select form-select-sm" @change="handleTaxTypeChange">
										<option value="0">선택</option>
										<option v-for="opt in taxTypeOptions" :key="opt.codecd" :value="opt.codecd + '|' + opt.codenm">{{ opt.codenm }}</option>
									</select>
								</div>
							</div>
							<div class="col-md-6">
								<div class="d-flex align-items-center">
									<span class="erp-label-form">공급가</span>
									<input v-model="formData.supyamt" type="text" class="form-control form-control-sm text-end bg-light" readonly />
								</div>
							</div>
							<div class="col-md-6">
								<div class="d-flex align-items-center">
									<span class="erp-label-form">부가세</span>
									<input v-model="formData.vatamt" type="text" class="form-control form-control-sm text-end bg-light" readonly />
								</div>
							</div>

							<div class="col-md-6">
								<div class="d-flex align-items-center">
									<span class="erp-label-form">대표자</span>
									<input v-model="formData.bossnm" type="text" class="form-control form-control-sm bg-light" readonly />
								</div>
							</div>
							<div class="col-md-6">
								<div class="d-flex align-items-center">
									<span class="erp-label-form">합계</span>
									<input v-model="formData.amtsum" type="text" class="form-control form-control-sm text-end bg-light fw-bold" readonly />
								</div>
							</div>
							<div class="col-12">
								<div class="d-flex align-items-center">
									<span class="erp-label-form required">적요</span>
									<input v-model="formData.descnm" type="text" class="form-control form-control-sm" maxlength="50" />
								</div>
							</div>

							<div class="col-md-6">
								<div class="d-flex align-items-center">
									<span class="erp-label-form">업태</span>
									<input v-model="formData.custtype" type="text" class="form-control form-control-sm" />
								</div>
							</div>
							<div class="col-md-6">
								<div class="d-flex align-items-center">
									<span class="erp-label-form">종목</span>
									<input v-model="formData.custkind" type="text" class="form-control form-control-sm" />
								</div>
							</div>
							<div class="col-12">
								<div class="d-flex align-items-center">
									<span class="erp-label-form">주소</span>
									<input v-model="formData.adrs" type="text" class="form-control form-control-sm" />
								</div>
							</div>

							<div v-if="showNonDeductible" class="col-12">
								<div class="d-flex align-items-center bg-warning-subtle p-2 rounded">
									<span class="erp-label-form fw-bold text-danger">불공제사유</span>
									<select v-model="formData.bgongcd" class="form-select form-select-sm">
										<option value="01">필요적기재사항누락</option>
										<option value="02">사업과 직접 관련 없는 지출</option>
										<option value="03">비영업용 소형승용차 구입·유지 및 임차</option>
										<option value="04">접대비 및 이와 유사한 비용 관련</option>
										<option value="05">면세사업 관련</option>
										<option value="06">토지의 자본적 지출 관련</option>
										<option value="07">사업자등록 전 매입세액</option>
										<option value="08">금거래계좌 미사용 관련 매입세액</option>
									</select>
								</div>
							</div>
						</div>

						<!-- 📋 품목 세부 내역 -->
						<div class="table-responsive mb-3 border rounded shadow-sm bg-white">
							<table class="table table-sm table-bordered mb-0 align-middle" style="font-size: 11px;">
								<thead class="table-light text-center">
									<tr>
										<th style="width: 105px;">일자</th>
										<th>품명</th>
										<th style="width: 50px;">수량</th>
										<th style="width: 75px;">단가</th>
										<th style="width: 85px;">공급가</th>
										<th style="width: 75px;">세액</th>
									</tr>
								</thead>
								<tbody>
									<tr v-for="(item, idx) in formData.items" :key="idx">
										<td class="p-0"><input v-model="item.ymd" type="date" class="form-control form-control-sm border-0 bg-transparent px-1" /></td>
										<td class="p-0">
											<div class="d-flex align-items-center gap-1 px-1">
												<input v-model="item.itemnm" type="text" class="form-control form-control-sm border-0 bg-transparent flex-grow-1" />
												<button class="btn btn-xs btn-outline-secondary p-0 border-0" @click="openHelp('ITEM', idx)" style="width:18px;height:18px;"><i class="bi bi-search" style="font-size:10px;"></i></button>
											</div>
										</td>
										<td class="p-0"><input v-model="item.qty" type="number" class="form-control form-control-sm border-0 bg-transparent px-1 text-end" @input="calculateItem(idx)" /></td>
										<td class="p-0"><input v-model="item.price" type="number" class="form-control form-control-sm border-0 bg-transparent px-1 text-end" @input="calculateItem(idx)" /></td>
										<td class="p-0"><input v-model="item.amt" type="text" class="form-control form-control-sm border-0 bg-light px-1 text-end" readonly /></td>
										<td class="p-0"><input v-model="item.vat" type="text" class="form-control form-control-sm border-0 bg-light px-1 text-end" readonly /></td>
									</tr>
								</tbody>
							</table>
						</div>

						<!-- 💳 전표 생성 정보 -->
						<div class="card erp-slip-card border-info-subtle mb-0 shadow-none">
							<div class="card-header bg-info-subtle p-2 d-flex justify-content-between align-items-center shadow-none border-info-subtle">
								<div class="form-check mb-0">
									<input v-model="formData.slipyn" class="form-check-input border-primary" type="checkbox" id="slipCheck" :disabled="!!formData.sslipno" true-value="Y" false-value="N">
									<label class="form-check-label fw-bold text-primary" for="slipCheck">전표 생성 정보</label>
								</div>
								<div class="form-check mb-0">
									<input v-model="formData.useyn" class="form-check-input" type="checkbox" id="receiptCheck" true-value="Y" false-value="N">
									<label class="form-check-label fw-bold text-success" for="receiptCheck">접수확인</label>
								</div>
							</div>
							<div v-if="formData.slipyn === 'Y'" class="card-body p-2 bg-white border border-top-0 border-info-subtle rounded-bottom">
								<div class="row g-2 small">
									<div class="col-md-6">
										<div class="d-flex align-items-center">
											<span class="erp-label-form required">발행부서</span>
											<div class="input-group input-group-sm">
												<input v-model="formData.deptcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
												<input v-model="formData.deptnm" type="text" class="form-control" @keydown.enter="openHelp('DEPT')" />
												<button class="btn btn-outline-secondary px-2" @click="openHelp('DEPT')"><i class="bi bi-search"></i></button>
											</div>
										</div>
									</div>
									<div class="col-md-6">
										<div class="d-flex align-items-center">
											<span class="erp-label-form required">매입계정</span>
											<div class="input-group input-group-sm">
												<input v-model="formData.acctcd" type="text" class="form-control text-center bg-light" style="max-width: 70px;" readonly />
												<input v-model="formData.acctnm" type="text" class="form-control" @keydown.enter="openHelp('ACCT_PURCHASE')" />
												<button class="btn btn-outline-secondary px-2" @click="openHelp('ACCT_PURCHASE')"><i class="bi bi-search"></i></button>
											</div>
										</div>
									</div>
									<div class="col-12 mt-2">
										<table class="table table-sm table-bordered mb-0 bg-light" style="font-size: 10px;">
											<thead class="text-center bg-secondary-subtle">
												<tr>
													<th>지불계정</th><th>지불액</th><th>관리번호</th><th>발행/만기일</th>
												</tr>
											</thead>
											<tbody>
												<tr>
													<td>
														<select v-model="formData.cpaycndt1" class="form-select form-select-sm" style="font-size: 10px;" @change="handlePaymentChange(1)">
															<option value="000">선택</option>
															<option v-for="opt in paymentOptions" :key="opt.VAL" :value="opt.VAL">{{ opt.codenm }}</option>
														</select>
													</td>
													<td><input v-model="formData.cinamt1" type="number" class="form-control form-control-sm text-end" style="font-size: 10px;" /></td>
													<td>
														<div class="input-group input-group-sm">
															<input v-model="formData.cmgtno1" type="text" class="form-control" style="font-size: 10px;" @keydown.enter="openHelp('MGT', 1)" />
															<button class="btn btn-outline-secondary px-1" @click="openHelp('MGT', 1)"><i class="bi bi-search"></i></button>
														</div>
													</td>
													<td>
														<input v-model="formData.cstdymd1" type="date" class="form-control form-control-sm mb-1 px-1" style="font-size: 10px;" />
														<input v-model="formData.cendymd1" type="date" class="form-control form-control-sm px-1" style="font-size: 10px;" />
													</td>
												</tr>
												<tr>
													<td>
														<select v-model="formData.cpaycndt2" class="form-select form-select-sm" style="font-size: 10px;" @change="handlePaymentChange(2)">
															<option value="000">선택</option>
															<option v-for="opt in paymentOptions" :key="opt.VAL" :value="opt.VAL">{{ opt.codenm }}</option>
														</select>
													</td>
													<td><input v-model="formData.cinamt2" type="number" class="form-control form-control-sm text-end" style="font-size: 10px;" /></td>
													<td>
														<div class="input-group input-group-sm">
															<input v-model="formData.cmgtno2" type="text" class="form-control" style="font-size: 10px;" @keydown.enter="openHelp('MGT', 2)" />
															<button class="btn btn-outline-secondary px-1" @click="openHelp('MGT', 2)"><i class="bi bi-search"></i></button>
														</div>
													</td>
													<td>
														<input v-model="formData.cstdymd2" type="date" class="form-control form-control-sm mb-1 px-1" style="font-size: 10px;" />
														<input v-model="formData.cendymd2" type="date" class="form-control form-control-sm px-1" style="font-size: 10px;" />
													</td>
												</tr>
											</tbody>
										</table>
									</div>
								</div>
							</div>
						</div>
					</div>
				</div>
				<div class="card-footer py-2 bg-white border-top shadow-sm mt-auto">
					<div class="d-flex justify-content-between w-100">
						<div class="btn-group gap-1">
							<button v-if="formData.sslipno" class="btn btn-sm btn-outline-info" @click="handleSlipPrint" title="전표 인쇄">
								<i class="bi bi-printer"></i>
							</button>
							<button v-if="formData.sslipno" class="btn btn-sm btn-outline-danger" @click="handleSlipDelete" title="전표 삭제">
								<i class="bi bi-trash"></i> 전표삭제
							</button>
						</div>
						<div class="btn-group gap-1">
							<button class="btn btn-sm btn-primary px-3" @click="save">
								<i class="bi bi-check-lg me-1"></i> 저장
							</button>
							<button v-if="formData.taxno" class="btn btn-sm btn-danger px-2" @click="handleTaxDelete" title="세금계산서 삭제">
								<i class="bi bi-trash"></i>
							</button>
							<button class="btn btn-sm btn-secondary px-3" @click="detailModalVisible = false">
								닫기
							</button>
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>

	<!-- 팝업 공통 모달 (검색용) -->
	<Modal v-model:visible="commonModalVisible" :modalProps="modalProps" />
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch, nextTick } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import Modal from '@/components/Modal.vue'
import AppAlert from '@/components/AppAlert.vue'
import type { ModalProps } from '@/types/modal'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()

const today = new Date().toISOString().slice(0, 10)
const firstDay = today.slice(0, 8) + '01'

// --- 옵션 데이터 ---
const taxUnitOptions = ref<any[]>([])
const taxTypeOptions = ref<any[]>([])
const paymentOptions = ref<any[]>([])

const searchForm = reactive({
	taxunit: '',
	taxtype: '000',
	fromdt: firstDay,
	todt: today
})

// --- 폼 데이터 ---
const formData = reactive<any>({
	taxunit: '', custcd: '', custnm: '', custno: '', taxno: '', taxym: '',
	pubymd: today, taxtype_val: '0', taxtype: '', typenm: '', supyamt: '0', vatamt: '0', amtsum: '0',
	bossnm: '', descnm: '', custtype: '', custkind: '', adrs: '', bgongcd: '01',
	useyn: 'N', slipyn: 'N', sslipno: '',
	deptcd: authStore.deptcd || '', deptnm: authStore.deptnm || '', acctcd: '', acctnm: '',
	cpaycndt1: '000', cinamt1: 0, cmgtno1: '', cacctcd1: '', cstdymd1: today, cendymd1: today, cpaytype1: '000',
	cpaycndt2: '000', cinamt2: 0, cmgtno2: '', cacctcd2: '', cstdymd2: today, cendymd2: today, cpaytype2: '000',
	items: Array.from({ length: 4 }, () => ({ rowno: '', ymd: today, itemnm: '', qty: 0, price: 0, amt: '0', vat: '0', bigo: '' }))
})

const showNonDeductible = ref(false)
const detailModalVisible = ref(false)
const mainGridRef = ref<HTMLDivElement | null>(null)
let mainGrid: Tabulator | null = null

watch(detailModalVisible, () => {
    nextTick(() => { setTimeout(() => { mainGrid?.redraw() }, 100) })
})

const fetchOptions = async () => {
	try {
		const [resU, resT, resP] = await Promise.all([
			api.post('/ha00/HA00_00P_STR', { gubun: 'SA', cmpycd: authStore.cmpycd }),
			api.post('/ha00/HA00_00P_STR', { gubun: 'E0', cmpycd: authStore.cmpycd, gbncd: '120' }), // 매입 유형
			api.post('/ha00/HA00_00P_STR', { gubun: 'E0', cmpycd: authStore.cmpycd, gbncd: '190' })  // 지불 수단
		])
		taxUnitOptions.value = (resU.data || []).map((i: any) => ({ code: i.taxunit, name: i.unitnm }))
		if (taxUnitOptions.value.length > 0) searchForm.taxunit = taxUnitOptions.value[0].code

		// 🚀 'E0' 표준 패턴 적용
		taxTypeOptions.value = (resT.data || []).map((i: any) => ({
			codecd: i.codecd,
			codenm: i.codenm
		}))

		paymentOptions.value = (resP.data || []).map((o: any) => ({
			VAL: `${o.codecd}${o.typemgt}${o.acctcd}`,
			codecd: o.codecd,
			codenm: o.codenm,
			mgtgbn: o.typemgt,
			acctcd: o.acctcd
		}))
	} catch (e) { console.error(e) }
}

const search = async () => {
	if (!searchForm.taxunit) return vAlertError('사업장을 선택하세요.')
	try {
		const res = await api.post('/hatx/HATX_010U_STR', {
			actkind: 'SR', cmpycd: authStore.cmpycd, taxunit: searchForm.taxunit,
			fromdt: searchForm.fromdt.replace(/-/g, ''), todt: searchForm.todt.replace(/-/g, ''),
			taxtype: searchForm.taxtype, taxkind: '100'
		})
		mainGrid?.setData((res.data || []).map((d: any) => ({
			...d, supyamt: Number(d.supyamt || 0), vatamt: Number(d.vatamt || 0), amtsum: Number(d.amtsum || 0)
		})))
		vAlert('조회되었습니다.')
	} catch (e) { vAlertError('조회 중 오류 발생') }
}

const handleNew = () => {
	Object.assign(formData, {
		taxunit: searchForm.taxunit, custcd: '', custnm: '', custno: '', taxno: '', taxym: '',
		pubymd: today, taxtype_val: '0', taxtype: '', typenm: '', supyamt: '0', vatamt: '0', amtsum: '0',
		bossnm: '', descnm: '', custtype: '', custkind: '', adrs: '', bgongcd: '01', useyn: 'N', slipyn: 'N', sslipno: '',
		deptcd: authStore.deptcd || '', deptnm: authStore.deptnm || '', acctcd: '', acctnm: '',
		cpaycndt1: '000', cinamt1: 0, cpaycndt2: '000', cinamt2: 0,
		items: Array.from({ length: 4 }, () => ({ rowno: '', ymd: today, itemnm: '', qty: 0, price: 0, amt: '0', vat: '0', bigo: '' }))
	})
	showNonDeductible.value = false; detailModalVisible.value = true
}

const handleTaxTypeChange = () => {
	if (formData.taxtype_val === '0') {
		formData.taxtype = ''; formData.typenm = ''; showNonDeductible.value = false
	} else {
		const [code, name] = formData.taxtype_val.split('|')
		formData.taxtype = code; formData.typenm = name
		showNonDeductible.value = code === '060'
	}
}

const handlePaymentChange = (num: number) => {
	const val = formData['cpaycndt' + num]
	if (val === '000') return
	const opt = paymentOptions.value.find(o => o.VAL === val)
	if (opt) {
		formData['cmgtgbn' + num] = opt.mgtgbn; formData['cacctcd' + num] = opt.acctcd;
		formData['cpaytype' + num] = opt.codecd; formData['cinamt' + num] = Number(formData.amtsum.replace(/,/g, ''))
	}
}

const calculateItem = (idx: number) => {
	const item = formData.items[idx]
	const amt = (item.qty || 0) * (item.price || 0)
	const vat = Math.floor(amt * 0.1)
	item.amt = amt.toLocaleString(); item.vat = vat.toLocaleString()
	let totalSup = 0, totalVat = 0
	formData.items.forEach(it => { totalSup += Number(it.amt.replace(/,/g, '')); totalVat += Number(it.vat.replace(/,/g, '')) })
	formData.supyamt = totalSup.toLocaleString(); formData.vatamt = totalVat.toLocaleString(); formData.amtsum = (totalSup + totalVat).toLocaleString()
}

const save = async () => {
	if (!formData.custcd) return vAlertError('거래처를 선택하세요.')
	if (formData.taxtype_val === '0') return vAlertError('유형을 선택하세요.')
	if (!formData.descnm.trim()) return vAlertError('적요를 입력하세요.')
	try {
		if (formData.slipyn === 'Y') {
			if (!formData.deptcd) return vAlertError('발행부서를 선택하세요.')
			if (!formData.acctcd) return vAlertError('매입계정을 선택하세요.')
			const inAmt = Number(String(formData.cinamt1).replace(/,/g, '')) + Number(String(formData.cinamt2).replace(/,/g, ''))
			const totAmt = Math.abs(Number(formData.amtsum.replace(/,/g, '')))
			if (inAmt !== totAmt) return vAlertError('매입액과 지불액이 일치하지 않습니다.')
		}

		// 🚀 통합 저장 루틴 (Vat + Slip)
		const payload = {
			slipyn: formData.slipyn,
			master: {
				...formData,
				actkind: formData.taxno ? 'U1' : 'I1',
				pubymd: formData.pubymd.replace(/-/g, ''),
				supyamt: formData.supyamt.replace(/,/g, ''),
				vatamt: formData.vatamt.replace(/,/g, ''),
				// 전표용 날짜 보정
				cstdymd1: formData.cstdymd1.replace(/-/g, ''),
				cendymd1: formData.cendymd1.replace(/-/g, ''),
				cstdymd2: formData.cstdymd2.replace(/-/g, ''),
				cendymd2: formData.cendymd2.replace(/-/g, ''),
				cpayymd1: formData.cpayymd1?.replace(/-/g, '') || '',
				cpayymd2: formData.cpayymd2?.replace(/-/g, '') || '',
			},
			items: formData.items.filter((it: any) => it.itemnm).map((it: any) => ({
				...it,
				ymd: it.ymd.replace(/-/g, ''),
				amt: it.amt.replace(/,/g, ''),
				vat: it.vat.replace(/,/g, '')
			}))
		}

		const res = await api.post('/hatx/save-purchase', payload)
		if (res.data?.res === 'OK') {
			vAlert('성공적으로 저장되었습니다.')
			detailModalVisible.value = false
			search()
		}
	} catch (e: any) {
		vAlertError(e.message || '저장 중 오류 발생')
	}
}

const handleTaxDelete = async () => {
	if (!confirm('해당 세금계산서를 삭제하시겠습니까?')) return
	try {
		await api.post('/hatx/HATX_010U_STR', { actkind: 'D1', cmpycd: authStore.cmpycd, taxkind: '100', taxym: formData.taxym, taxno: formData.taxno })
		vAlert('삭제되었습니다.'); detailModalVisible.value = false; search()
	} catch (e) { vAlertError('삭제 중 오류 발생') }
}

const handleSlipDelete = async () => {
	if (!confirm('해당 전표를 삭제하시겠습니까?')) return
	try {
		await api.post('/hatx/HATX_010U_STR', { SLIPACT: 'D', cmpycd: authStore.cmpycd, sslipno: formData.sslipno, deptcd: formData.deptcd, userid: authStore.userid })
		vAlert('전표가 삭제되었습니다.'); detailModalVisible.value = false; search()
	} catch (e) { vAlertError('전표 삭제 중 오류 발생') }
}

const handleSlipPrint = () => {
	if (!formData.sslipno) return
	const [ymd, no] = formData.sslipno.split('-')
	window.open(`/hasl/HASL_SLIP_PRINT?slipgu=010&slipymd=${ymd}&slipno=${no}`, 'SlipPrint', 'width=800,height=700,scrollbars=yes')
}

const loadDetail = async (row: any) => {
	try {
		const res = await api.post('/hatx/HATX_010U_STR', {
			actkind: 'S2', cmpycd: row.cmpycd || authStore.cmpycd, taxkind: '100', taxym: row.taxym, taxno: row.taxno
		})
		if (res.data?.[0]) {
			const d = res.data[0]
			Object.assign(formData, d)
			formData.taxym = row.taxym; formData.taxno = row.taxno
			formData.pubymd = d.pubymd ? `${d.pubymd.slice(0, 4)}-${d.pubymd.slice(4, 6)}-${d.pubymd.slice(6, 8)}` : ''
			formData.taxtype_val = `${d.taxtype}|${d.typenm}`
			formData.supyamt = Number(d.supyamt || 0).toLocaleString()
			formData.vatamt = Number(d.vatamt || 0).toLocaleString()
			formData.amtsum = (Number(d.supyamt || 0) + Number(d.vatamt || 0)).toLocaleString()
			formData.sslipno = (d.slipymd && d.slipymd > '00000000') ? `${d.slipymd}-${d.slipno}-${d.srowno}` : ''
			formData.bgongcd = d.bgongcd || '01'; formData.slipyn = 'N'

			showNonDeductible.value = d.taxtype === '060'
			const resItems = await api.post('/hatx/HATX_011U_STR', { actkind: 'S1', cmpycd: row.cmpycd, taxkind: '100', taxym: row.taxym, taxno: row.taxno })
			formData.items = Array.from({ length: 4 }, (_, i) => {
				const it = resItems.data?.[i]
				return it ? {
					...it, qty: Number(it.qty || 0), price: Number(it.price || 0),
					amt: Number(it.amt || 0).toLocaleString(), vat: Number(it.vat || 0).toLocaleString(),
					ymd: it.ymd ? `${it.ymd.slice(0, 4)}-${it.ymd.slice(4, 6)}-${it.ymd.slice(6, 8)}` : today
				} : { rowno: '', ymd: today, itemnm: '', qty: 0, price: 0, amt: '0', vat: '0', bigo: '' }
			})
			detailModalVisible.value = true
		}
	} catch (e) { vAlertError('상세 정보 로드 중 오류 발생') }
}

const commonModalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, large: true })

function openHelp(type: string, idx?: number) {
	if (type === 'CUST') {
		Object.assign(modalProps, {
			title: '거래처 선택', path: '/ha00/HA00_00P_STR', data: { gubun: 'C4', cmpycd: authStore.cmpycd, code: formData.custnm },
			columns: [{ title: '코드', field: 'custcd', width: 80 }, { title: '거래처명', field: 'custnm', width: 180 }, { title: '사업자번호', field: 'custno', width: 120 }],
			onConfirm: (d: any) => { formData.custcd = d.custcd; formData.custnm = d.custnm; formData.custno = d.custno; formData.bossnm = d.bossnm; formData.adrs = d.adrs; formData.custtype = d.custtype; formData.custkind = d.custkind }
		})
	} else if (type === 'ITEM' && idx !== undefined) {
		Object.assign(modalProps, {
			title: '품목 선택', path: '/hs00/HS00_000S_STR', data: { gubun: 'I1', cmpycd: authStore.cmpycd, gbncd: '2', codenm: formData.items[idx].itemnm },
			columns: [{ title: '코드', field: 'itemcd', width: 80 }, { title: '품목명', field: 'itemnm', width: 180 }],
			onConfirm: (d: any) => { formData.items[idx].itemnm = d.itemnm }
		})
	} else if (type === 'DEPT') {
		Object.assign(modalProps, {
			title: '부서 선택', path: '/ha00/HA00_00P_STR', data: { gubun: 'D0', cmpycd: authStore.cmpycd, code: formData.deptnm },
			columns: [{ title: '코드', field: 'deptcd', width: 80 }, { title: '부서명', field: 'deptnm', width: 180 }],
			onConfirm: (d: any) => { formData.deptcd = d.deptcd; formData.deptnm = d.deptnm }
		})
	} else if (type === 'ACCT_PURCHASE') {
		Object.assign(modalProps, {
			title: '계정과목 선택', path: '/ha00/HA00_00P_STR', data: { gubun: 'A0', cmpycd: authStore.cmpycd, code: '13' },
			columns: [{ title: '코드', field: 'acctcd', width: 80 }, { title: '계정명', field: 'acctnm', width: 180 }],
			onConfirm: (d: any) => { formData.acctcd = d.acctcd; formData.acctnm = d.acctnm }
		})
	} else if (type === 'MGT' && idx !== undefined) {
		const mgtgbn = formData['cmgtgbn' + idx]; const acctcd = formData['cacctcd' + idx]
		if (!mgtgbn || mgtgbn === '000') return vAlertError('관리번호를 관리하지 않는 계정입니다.')
		Object.assign(modalProps, {
			title: '관리번호 선택', path: '/ha00/HA00_00P_STR', data: { gubun: 'E0', cmpycd: authStore.cmpycd, gbncd: '190' },
			columns: [{ title: '번호', field: 'mgtno', width: 120 }, { title: '명칭', field: 'mgtnm', width: 180 }],
			onConfirm: (d: any) => { formData['cmgtno' + idx] = d.mgtno }
		})
	}
	commonModalVisible.value = true
}

onMounted(() => {
	fetchOptions()
	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns', height: '100%', columnDefaults: { headerSort: false, vertAlign: "middle" },
			columns: [
				{ title: "발행일", field: "pubymd", width: 100, hozAlign: "center", formatter: (cell) => { const v = String(cell.getValue() || ''); return v.length === 8 ? `${v.slice(0,4)}-${v.slice(4,6)}-${v.slice(6,8)}` : v } },
				{ title: "상호", field: "custnm", width: 250, cssClass: "text-primary fw-bold", cellClick: (e, cell) => loadDetail(cell.getData()) },
				{ title: "유형", field: "typenm", width: 100, hozAlign: "center" },
				{ title: "공급가", field: "supyamt", width: 110, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
				{ title: "부가세", field: "vatamt", width: 100, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
				{ title: "합계", field: "amtsum", width: 110, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "fw-bold" },
				{ title: "적요", field: "descnm", widthGrow: 1 },
				{ title: "접수", field: "useyn", width: 80, hozAlign: "center",
                  formatter: (cell) => {
                    const val = String(cell.getValue() || '').trim().toUpperCase();
                    return val === 'Y' ? '<b class="text-primary">접수</b>' : '<span class="text-danger">미접수</span>';
                  }
                }
			]
		})
	}
})
</script>

<style scoped>
.erp-label { min-width: 80px; font-weight: 500; font-size: 13px; }
.erp-label-form { min-width: 80px; font-weight: 500; font-size: 12px; color: #555; }
.erp-label-form.required::after { content: " *"; color: red; }
.btn-xs { padding: 1px 5px; font-size: 10px; }
.erp-slip-card { border: 1px dashed #0dcaf0; }
:deep(.tabulator-cell) { border-right: 1px solid #dee2e6 !important; font-size: 12px; cursor: pointer; }
:deep(.tabulator-header .tabulator-col) { border-right: 1px solid #dee2e6 !important; background-color: #f8f9fa !important; font-size: 12px; }
.side-detail-panel { animation: slideInRight 0.3s ease-out; z-index: 10; }
@keyframes slideInRight { from { transform: translateX(100%); } to { transform: translateX(0); } }
</style>
