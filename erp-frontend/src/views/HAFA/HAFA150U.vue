<!--
	=============================================================
	프로그램명: 감가상각전표 발행 (hafa150u)
	작성일자	: 2025.03.14
	작성자    : AI Assistant
	설명        : 감가상각 계산 내역을 바탕으로 회계 전표를 일괄 또는 선택 발행
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container d-flex flex-column h-100 bg-white">
		<!-- 🚀 1. 상단 액션 바 -->
		<div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
			<div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-receipt-cutoff me-2 text-primary" style="font-size: 18px;"></i>
				고정자산 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
				기말처리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
				<span class="text-primary fw-bolder">감가상각전표 발행(HAFA150U)</span>
			</div>
			<div class="btn-group-erp d-flex gap-1 pe-3">
				<button class="btn-erp btn-init" @click="initialize">초기화</button>
				<button class="btn-erp btn-search" @click="search">조회</button>
				<button class="btn-erp btn-save" @click="save">전표발행</button>
			</div>
		</div>

		<!-- 💡 2. 메인 컨텐츠 영역 -->
		<div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

			<!-- [상단] 조회 필터 영역 -->
			<div class="card border shadow-sm flex-shrink-0 overflow-hidden">
				<div class="card-body p-0 bg-white">
					<table class="erp-table-dense" width="100%">
						<colgroup>
							<col style="width: 10%" /><col style="width: 90%" />
						</colgroup>
						<tbody>
							<tr>
								<th class="text-center bg-light">대상년월</th>
								<td>
									<div class="d-flex align-items-center gap-1 px-2">
										<select v-model="searchForm.yy" class="form-select form-select-sm" style="width: 100px;">
											<option v-for="y in yearOptions" :key="y" :value="y">{{ y }}년</option>
										</select>
										<select v-model="searchForm.mm" class="form-select form-select-sm" style="width: 80px;">
											<option v-for="m in monthOptions" :key="m" :value="m">{{ m }}월</option>
										</select>
										<span class="small fw-bold ms-2 text-secondary">월 감가상각 내역 조회</span>
									</div>
								</td>
							</tr>
						</tbody>
					</table>
				</div>
			</div>

			<!-- [중간] 전표 발행 정보 설정 -->
			<div class="card border shadow-sm flex-shrink-0 overflow-hidden">
				<div class="card-header bg-white py-1 px-3 border-bottom d-flex justify-content-between align-items-center">
					<div class="fw-bold small text-dark"><i class="bi bi-pencil-square me-2 text-primary"></i>전표 발행 정보 설정</div>
					<div class="text-danger small fw-bold" style="font-size: 11px;">※ 전표 발행 시 선택한 내역에 대해 전표가 자동 생성됩니다.</div>
				</div>
				<div class="card-body p-0 bg-white">
					<table class="erp-table-full">
						<colgroup>
							<col style="width: 110px;" /><col style="width: 300px;" />
							<col style="width: 110px;" /><col style="width: 200px;" />
							<col />
						</colgroup>
						<tbody>
							<tr>
								<th class="required bg-light">발행부서</th>
								<td>
									<div class="input-group input-group-sm px-1">
										<input v-model="issuingForm.deptcd" type="text" class="form-control text-center bg-light fw-bold" style="max-width: 65px;" readonly />
										<input v-model="issuingForm.deptnm" type="text" class="form-control" @keydown.enter="openHelp('DEPT')" placeholder="부서 선택" />
										<button class="btn btn-outline-secondary" @click="openHelp('DEPT')"><i class="bi bi-search"></i></button>
									</div>
								</td>
								<th class="required bg-light">발행일자</th>
								<td class="px-1"><input v-model="issuingForm.acctymd" type="date" class="form-control form-control-sm" /></td>
								<td class="px-3">
									<div class="d-flex justify-content-end gap-2">
										<div class="d-flex align-items-center bg-light border rounded px-2 py-1">
											<span class="text-muted small me-2">총 상각액</span>
											<span class="fw-bold text-dark small">{{ formatMoney(summary.totalAmt) }}</span>
										</div>
										<div class="d-flex align-items-center bg-primary-subtle border border-primary-subtle rounded px-2 py-1">
											<span class="text-primary-emphasis small me-2 fw-bold">발행예정</span>
											<span class="fw-bold text-primary">{{ formatMoney(summary.issuingAmt) }}</span>
										</div>
									</div>
								</td>
							</tr>
						</tbody>
					</table>
				</div>
			</div>

			<!-- 📊 3. 그리드 영역 -->
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
import { useTabStore } from '@/stores/tabStore'
import Modal from '@/components/Modal.vue'
import type { ModalProps } from '@/types/modal'

const authStore = useAuthStore()
const tabStore = useTabStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()

const currentYear = new Date().getFullYear()
const currentMonth = (new Date().getMonth() + 1).toString().padStart(2, '0')
const yearOptions = Array.from({ length: 5 }, (_, i) => String(currentYear - i))
const monthOptions = Array.from({ length: 12 }, (_, i) => String(i + 1).padStart(2, '0'))

const searchForm = reactive({ yy: String(currentYear), mm: currentMonth })
const issuingForm = reactive({ deptcd: authStore.deptcd || '', deptnm: authStore.deptnm || '', acctymd: new Date().toISOString().slice(0, 10) })
const summary = reactive({ totalAmt: 0, issuingAmt: 0 })

const mainGridRef = ref<HTMLDivElement | null>(null); let mainGrid: Tabulator | null = null

const formatMoney = (val: any) => Number(val || 0).toLocaleString()

async function search() {
	try {
		const res = await api.post('/hafa/HAFA_150U_STR',
		{   cmpycd: authStore.cmpycd,
		    baseym: searchForm.yy + searchForm.mm
		})
		const data = (res.data || []).map((row: any) => ({
			deptcd: row.deptcd,
			deptnm: row.deptnm,
			acctcd: row.acctcd,
			acctnm: row.acctnm,
			costtype: row.costtype,
			costtypenm: row.costtypenm,
			dprsamt: Number(row.dprsamt || 0),
			cacctcd: row.cacctcd,
			cacctnm: row.cacctnm,
			macctcd: row.macctcd,
			macctnm: row.macctnm,
			sacctcd: row.sacctcd,
			sacctnm: row.sacctnm,
			slipymd: row.slipymd,
			slipno: row.slipno,
			srowno: row.srowno
		}))
		summary.totalAmt = data.reduce((acc: number, cur: any) => acc + cur.dprsamt, 0)
		summary.issuingAmt = 0; mainGrid?.setData(data); vAlert('조회되었습니다.')
	} catch (e) { vAlertError('조회 중 오류 발생') }
}

async function save() {
	const selectedRows = mainGrid?.getSelectedData() || []
	if (selectedRows.length === 0) return vAlertError('전표 발행할 대상을 선택하세요.')
	if (!issuingForm.deptcd) return vAlertError('발행부서를 선택하세요.')
	if (!confirm('선택한 내역에 대해 감가상각 전표를 자동 발행하시겠습니까?')) return
	try {
		await api.post('/hafa/HAFA_150U_SAVE',
		{ cmpycd: authStore.cmpycd,
		  yy: searchForm.yy,
		  mm: searchForm.mm,
		  deptcd: issuingForm.deptcd,
		  acctymd: issuingForm.acctymd.replace(/-/g, ''),
		  items: selectedRows,
		  userid: authStore.userid })
		vAlert('전표가 정상적으로 발행되었습니다.');
		search()
	} catch (e) { vAlertError('전표 발행 중 오류 발생') }
}

function initialize() {
	searchForm.yy = String(currentYear); searchForm.mm = currentMonth;
	issuingForm.deptcd = authStore.deptcd || ''; issuingForm.deptnm = authStore.deptnm || '';
	issuingForm.acctymd = new Date().toISOString().slice(0, 10);
	mainGrid?.clearData(); summary.totalAmt = 0; summary.issuingAmt = 0;
}

const modalVisible = ref(false); const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })
function openHelp(type: string) {
	if (type === 'DEPT') {
		Object.assign(modalProps, { title: '부서 선택', path: '/ha00/HA00_00P_STR', data: { gubun: 'D0', cmpycd: authStore.cmpycd, code: issuingForm.deptnm }, columns: [{ title: '코드', field: 'deptcd', width: 80 }, { title: '부서명', field: 'deptnm', width: 180 }],
			onConfirm: (d: any) => { issuingForm.deptcd = d.deptcd; issuingForm.deptnm = d.deptnm }
		})
	}
	modalVisible.value = true
}

onMounted(() => {
	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns', height: '100%', selectable: true,
			columnDefaults: { headerSort: false, vertAlign: "middle" },
			columns: [
				{ formatter: "rowSelection", titleFormatter: "rowSelection", width: 40, hozAlign: "center", headerSort: false, cellClick: (e, cell) => cell.getRow().toggleSelect() },
				{ title: "부서코드", field: "deptcd", width: 80, hozAlign: "center" },
				{ title: "부서명", field: "deptnm", widthGrow: 1 },
				{ title: "계정코드", field: "acctcd", width: 80, hozAlign: "center" },
				{ title: "계정과목명", field: "acctnm", widthGrow: 1, cssClass: "fw-bold" },
				{ title: "비용구분", field: "costtypenm", widthGrow: 1, hozAlign: "center" },
				{ title: "감가상각액", field: "dprsamt", widthGrow: 1, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "text-primary fw-bold" },
				{ title: "전표번호", field: "slipno", widthGrow: 1, hozAlign: "center",
					formatter: (cell: any) => {
						const d = cell.getData(); if (!d.slipymd || d.slipymd === '00000000') return '';
						return `<span class="text-primary text-decoration-underline cursor-pointer">${d.slipymd}-${d.slipno}</span>`;
					},
					cellClick: (e, cell) => {
						const d = cell.getData(); if (!d.slipno) return;
						tabStore.addTab({ name: '일반전표관리', path: '/HASL/HASL110U', params: { slipymd: d.slipymd, slipno: d.slipno } });
					}
				}
			]
		})
		mainGrid.on("rowSelectionChanged", (data) => summary.issuingAmt = data.reduce((acc: number, cur: any) => acc + cur.dprsamt, 0))
	}
    search()
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
