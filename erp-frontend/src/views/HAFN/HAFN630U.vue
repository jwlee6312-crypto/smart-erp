<!--
	=============================================================
	프로그램명: 선급비용 자동전표발행 (HAFN630U)
	작성일자	: 2025.03.14
	작성자    : AI Assistant
	설명        : 선급비용 내역을 조회하여 비용 상계(결산) 전표 자동 발행
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container d-flex flex-column h-100 bg-white">
		<!-- 🚀 1. 상단 액션 바 -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-magic me-2 text-primary" style="font-size: 18px;"></i>
				자금관리 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				<span class="text-primary fw-bolder">선급비용 자동전표발행 (HAFN630U)</span>
			</div>
			<div class="btn-group-erp d-flex gap-1">
				<button class="btn-erp btn-init" @click="initialize">
					<i class="bi bi-arrow-clockwise"></i> 초기화
				</button>
				<button class="btn-erp btn-search" @click="search">
					<i class="bi bi-search"></i> 조회
				</button>
				<button class="btn-erp btn-save" @click="save" :disabled="selectedRows.length === 0">
					<i class="bi bi-check-lg"></i> 전표발행
				</button>
			</div>
		</div>

		<!-- 💡 2. 검색 조건 영역 -->
		<div class="p-2 pb-0 flex-shrink-0 bg-light">
			<div class="card border shadow-sm overflow-hidden bg-white">
				<table class="erp-table-dense w-100">
					<colgroup>
						<col style="width: 100px;" /><col />
						<col style="width: 100px;" /><col />
					</colgroup>
					<tbody>
						<tr>
							<th class="text-center bg-light border-end">기준일자</th>
							<td class="bg-white border-end px-2">
								<input v-model="searchForm.payymd" type="date" class="form-control form-control-sm" style="max-width: 150px;" />
							</td>
							<th class="text-center bg-light border-end">계정과목</th>
							<td class="bg-white px-2">
								<select v-model="searchForm.acctcd" class="form-select form-select-sm" style="max-width: 200px;" @change="search">
									<option v-for="opt in acctOptions" :key="opt.cd" :value="opt.cd">{{ opt.nm }}</option>
								</select>
							</td>
						</tr>
					</tbody>
				</table>
			</div>
		</div>

		<!-- 📊 3. 그리드 영역 -->
		<div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column bg-light" style="min-height: 250px;">
			<div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white">
				<div class="card-header py-1 px-3 bg-light d-flex justify-content-between align-items-center border-bottom text-secondary">
					<span class="fw-bold small"><i class="bi bi-list-check me-1"></i> 상계 대상 선급비용 리스트</span>
					<span class="badge bg-primary">총 상계액: {{ formatMoney(totalSelectedAmt) }}</span>
				</div>
                <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
                  <div ref="mainGridRef" class="tabulator-instance flex-grow-1"></div>
                </div>
			</div>
		</div>

		<!-- 📝 4. 발행 정보 입력 영역 -->
		<div class="p-2 pt-0 flex-shrink-0 bg-light">
			<div class="card border shadow-sm bg-white overflow-hidden">
				<div class="card-header py-1 px-3 bg-dark text-white">
					<span class="small fw-bold"><i class="bi bi-pencil-square me-1"></i> 전표 발행 정보</span>
				</div>
				<table class="erp-table-full" style="table-layout: fixed;">
					<colgroup>
						<col style="width: 100px;" /><col />
						<col style="width: 100px;" /><col />
						<col style="width: 100px;" /><col />
					</colgroup>
					<tbody>
						<tr>
							<th class="text-center border-end bg-light-subtle">발행부서</th>
							<td class="bg-white border-end px-2 py-1">
								<div class="input-group input-group-sm">
									<input v-model="voucherForm.deptcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
									<input v-model="voucherForm.deptnm" type="text" class="form-control" placeholder="부서 선택" @keydown.enter="openHelp('DEPT')" />
									<button class="btn btn-outline-secondary px-2" @click="openHelp('DEPT')"><i class="bi bi-search"></i></button>
								</div>
							</td>
							<th class="text-center border-end bg-light-subtle">상계계정</th>
							<td class="bg-white border-end px-2 py-1">
								<div class="input-group input-group-sm">
									<input v-model="voucherForm.sacctcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
									<input v-model="voucherForm.sacctnm" type="text" class="form-control" placeholder="비용계정 선택" @keydown.enter="openHelp('ACCT')" />
									<button class="btn btn-outline-secondary px-2" @click="openHelp('ACCT')"><i class="bi bi-search"></i></button>
								</div>
							</td>
							<th class="text-center border-end bg-light-subtle">적요</th>
							<td class="bg-white px-2 py-1">
								<input v-model="voucherForm.remark" type="text" class="form-control form-control-sm" placeholder="전표 적요 입력" />
							</td>
						</tr>
					</tbody>
				</table>
			</div>
			<div class="mt-2 text-center text-muted small fw-bold">
				<i class="bi bi-info-circle me-1"></i> 저장 시 분개 전표가 자동으로 발행됩니다. 인쇄는 [경리전표출력] 메뉴에서 가능합니다.
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

const today = new Date().toISOString().substring(0, 10)

// 검색 데이터
const searchForm = reactive({
	payymd: today,
	acctcd: ''
})

const acctOptions = ref<any[]>([])

// 전표 발행 정보
const voucherForm = reactive({
	deptcd: authStore.deptcd,
	deptnm: authStore.deptnm,
	sacctcd: '',
	sacctnm: '',
	remark: '',
	clsymd: '00000000'
})

const mainGridRef = ref<HTMLDivElement | null>(null)
let mainGrid: Tabulator | null = null
const selectedRows = ref<any[]>([])

const totalSelectedAmt = computed(() => {
	return selectedRows.value.reduce((acc, row) => acc + (Number(row.payamt) || 0), 0)
})

const formatMoney = (val: any) => Number(val || 0).toLocaleString()
const formatYmdShort = (v: string) => v && v.length === 8 ? `${v.substring(2, 4)}.${v.substring(4, 6)}.${v.substring(6, 8)}` : v

// 초기 데이터 로드 (선급비용 계정 목록)
const loadInitData = async () => {
	try {
		const res = await api.post('/ha00/HA00_00P_STR', { gubun: 'A7', search: '110' })
		acctOptions.value = res.data.map((item: any) => {
            return { cd: item.col0, nm: item.col1 }
        })
		if (acctOptions.value.length > 0) searchForm.acctcd = acctOptions.value[0].cd

		const resCls = await api.post('/ha00/HA00_010S_STR', { gubun: 'P1', cmpycd: authStore.cmpycd })
		if (resCls.data && resCls.data.length > 0) {
            const cls = resCls.data[0]
            voucherForm.clsymd = cls.clsymd || '00000000'
        }
	} catch (e) { console.error('초기 데이터 로드 실패') }
}

const search = async () => {
	if (!searchForm.acctcd) return
	try {
		const res = await api.post('/hafn/HAFN_630U_STR', {
			actkind: 'S0',
			cmpycd: authStore.cmpycd,
			payymd: searchForm.payymd.replace(/-/g, ''),
			acctcd: searchForm.acctcd
		})

		const data = (res.data || []).map((item: any) => {
			return {
				slipymd: item.col0,
				slipno: item.col1,
				srowno: item.col2,
				remark: item.col3,
				custcd: item.col4,
				period: `${formatYmdShort(item.col6)} ~ ${formatYmdShort(item.col7)}`,
				total_amt: Number(item.col8 || 0),
				repay_amt: Number(item.col9 || 0),
				jan_amt: Number(item.col10 || 0),
				payamt: Number(item.col11 || 0),
				SELECT: true
			}
		})

		mainGrid?.setData(data)
		updateTotal()
		vAlert('조회되었습니다.')
	} catch (e) { vAlertError('조회 실패') }
}

const updateTotal = () => {
	const data = mainGrid?.getData() || []
	selectedRows.value = data.filter((r: any) => r.SELECT)
}

const save = async () => {
	if (searchForm.payymd.replace(/-/g, '') <= voucherForm.clsymd) return vAlert('회계 마감된 일자입니다.')
	if (!voucherForm.sacctcd) return vAlert('상계계정(비용)을 선택하십시오.')
	if (!voucherForm.remark) return vAlert('적요를 입력하십시오.')

	if (!confirm('선택한 내역으로 회계 전표를 발행하시겠습니까?')) return

	try {
		const details = []

		// 1. 차변 (Debits): 비용 계정 (상계계정)
		details.push({
			upkind: 'A', dbcr: 'D',
			acctcd: voucherForm.sacctcd,
			remark: voucherForm.remark,
			amount: totalSelectedAmt.value,
			usedeptcd: voucherForm.deptcd
		})

		// 2. 대변 (Credits): 선급비용 계정들
		selectedRows.value.forEach(row => {
			details.push({
				upkind: 'A', dbcr: 'C',
				acctcd: searchForm.acctcd,
				remark: voucherForm.remark,
				amount: row.payamt,
				custcd: row.custcd,
				usedeptcd: voucherForm.deptcd,
				sslipno: `${row.slipymd}${row.slipno}${row.srowno}`
			})
		})

		const payload = {
			actkind: 'A',
			MASTER: {
				cmpycd: authStore.cmpycd,
				slipymd: searchForm.payymd.replace(/-/g, ''),
				acctymd: searchForm.payymd.replace(/-/g, ''),
				deptcd: voucherForm.deptcd,
				business: voucherForm.remark,
				slipgu: '020'
			},
			DETAILS: details
		}

		const res = await api.post('/hasl/HASL_110U_SAVE', payload)
		vAlert('전표가 발행되었습니다.')
		if (res.data.slipno) {
			window.open(`/hasl/HASL_SLIP_PRINT?slipgu=020&slipymd=${payload.MASTER.slipymd}&slipno=${res.data.slipno}&deptcd=${voucherForm.deptcd}`)
		}
		search()
	} catch (e) { vAlertError('저장 실패') }
}

const initialize = () => {
	resetForm(searchForm)
	resetForm(voucherForm)
	searchForm.payymd = today
	voucherForm.deptcd = authStore.deptcd
	voucherForm.deptnm = authStore.deptnm
	loadInitData()
	mainGrid?.clearData()
}

// 팝업 설정
const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function openHelp(type: string) {
	if (type === 'DEPT') {
		Object.assign(modalProps, {
			title: '부서 선택', path: '/ha00/HA00_00P_STR',
			data: { gubun: 'D0', cmpycd: authStore.cmpycd, code: voucherForm.deptnm },
			columns: [{ title: '코드', field: 'deptcd', width: 80 }, { title: '부서명', field: 'deptnm', width: 180 }],
			onConfirm: (d: any) => { voucherForm.deptcd = d.deptcd; voucherForm.deptnm = d.deptnm }
		})
	} else if (type === 'ACCT') {
		Object.assign(modalProps, {
			title: '비용계정 선택', path: '/ha00/HA00_00P_STR',
			data: { gubun: 'A0', search: '62', cmpycd: authStore.cmpycd },
			columns: [{ title: '코드', field: 'acctcd', width: 80 }, { title: '계정명', field: 'acctnm', width: 180 }],
			onConfirm: (d: any) => { voucherForm.sacctcd = d.acctcd; voucherForm.sacctnm = d.acctnm }
		})
	}
	modalVisible.value = true
}

onMounted(() => {
	loadInitData()
	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns', height: '100%',
			columnDefaults: { headerSort: false, vertAlign: "middle" },
			columns: [
				{ title: "선택", field: "SELECT", width: 40, hozAlign: "center", formatter: "tickCross", editor: true, cellClick: (e, cell) => { cell.setValue(!cell.getValue()); updateTotal() } },
				{
					title: "발생전표", field: "slip_key", width: 130, hozAlign: "center",
					formatter: (c) => { const d = c.getData(); return `${d.slipymd}-${d.slipno}-${d.srowno}` }
				},
				{ title: "적요", field: "remark", minWidth: 200 },
				{ title: "기간", field: "period", width: 150, hozAlign: "center" },
				{ title: "선급비용", field: "total_amt", width: 100, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
				{ title: "상계액(누계)", field: "repay_amt", width: 100, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
				{ title: "잔액", field: "jan_amt", width: 100, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
				{ title: "금차상계액", field: "payamt", width: 110, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "bg-warning-subtle fw-bold" }
			]
		})
	}
})
</script>

<style scoped>
.erp-label { min-width: 80px; font-weight: 500; font-size: 13px; }
.bg-warning-subtle { background-color: #fffcf0 !important; }
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
