<!--
	=============================================================
	프로그램명 : 고정자산원장(HAFA040S)
	작성일자	: 2025.02.24
	작성자    : AI Assistant
	설명        : 계정과목별 고정자산 마스터 및 상각/처분 현황 조회 (표준 UI 적용)
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container d-flex flex-column h-100 bg-white">
		<div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
			<div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-book me-2 text-primary" style="font-size: 18px;"></i>
				고정자산 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
				자산조회 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
				<span class="text-primary fw-bolder">고정자산원장(HAFA040S)</span>
			</div>
			<div class="btn-group-erp d-flex gap-1 pe-3">
				<button class="btn-erp btn-search" @click="search">조회</button>
				<button class="btn-erp btn-print" @click="print">인쇄</button>
				<button class="btn-erp btn-excel" @click="excel">엑셀</button>
			</div>
		</div>

		<div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">
			<!-- [상단] 조회 필터 -->
			<div class="card border shadow-sm flex-shrink-0 overflow-hidden">
				<div class="card-body p-0 bg-white">
					<table class="erp-table-dense" width="100%">
						<colgroup>
							<col style="width: 10%" /><col style="width: 40%" />
							<col style="width: 10%" /><col style="width: 40%" />
						</colgroup>
						<tbody>
							<tr>
								<th class="text-center bg-light">계정과목</th>
								<td>
									<div class="input-group input-group-sm" style="width: 300px;">
										<input v-model="searchForm.acctcd" type="text" class="form-control text-center bg-light fw-bold" style="max-width: 65px;" readonly />
										<input v-model="searchForm.acctnm" type="text" class="form-control" @keydown.enter="openHelp('ACCT')" placeholder="계정 선택" />
										<button class="btn btn-outline-secondary" @click="openHelp('ACCT')"><i class="bi bi-search"></i></button>
									</div>
								</td>
								<th class="text-center bg-light">기준년월</th>
								<td>
									<div class="d-flex align-items-center gap-1">
										<select v-model="searchForm.yy" class="form-select form-select-sm" style="width: 100px;">
											<option v-for="y in yearOptions" :key="y" :value="y">{{ y }}년</option>
										</select>
										<select v-model="searchForm.mm" class="form-select form-select-sm" style="width: 80px;">
											<option v-for="m in monthOptions" :key="m" :value="m">{{ m }}월</option>
										</select>
										<span class="small fw-bold ms-1 text-secondary">현재</span>
									</div>
								</td>
							</tr>
						</tbody>
					</table>
				</div>
			</div>

			<!-- [그리드] 데이터 그리드 영역 -->
			<div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white">
				<div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
					<span class="fw-bold small text-dark"><i class="bi bi-list-ul me-2 text-primary"></i>고정자산 변동상세 내역</span>
				</div>
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
import { useTabStore } from '@/stores/tabStore'
import Modal from '@/components/Modal.vue'
import type { ModalProps } from '@/types/modal'

const authStore = useAuthStore()
const tabStore = useTabStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()

const currentYear = new Date().getFullYear()
const currentMonth = (new Date().getMonth() + 1).toString().padStart(2, '0')
const yearOptions = Array.from({ length: 6 }, (_, i) => String(currentYear - i))
const monthOptions = Array.from({ length: 12 }, (_, i) => String(i + 1).padStart(2, '0'))

const searchForm = reactive({ acctcd: '', acctnm: '', yy: String(currentYear), mm: currentMonth })
const mainGridRef = ref<HTMLDivElement | null>(null); let mainGrid: Tabulator | null = null

async function search() {
	if (!searchForm.acctcd) return vAlertError('계정과목을 선택하세요.')
	try {
		const res = await api.post('/hafa/HAFA_040S_STR', { cmpycd: authStore.cmpycd, baseym: searchForm.yy + searchForm.mm, acctcd: searchForm.acctcd })
		const data = (res.data || []).map((row: any) => {
			const pchamt = Number(row.pchamt || 0);
			const baseamt = Number(row.baseamt || 0);
			const dprssum = Number(row.dprssum || 0);

			return {
				...row,
				pchamt,
				baseamt,
				dprssum,
				incramt: Number(row.incramt || 0),
				dprsamt: Number(row.dprsamt || 0),
				dspamt: Number(row.dspamt || 0),
				pchymd_disp: row.pchymd ? `${row.pchymd.slice(2,4)}.${row.pchymd.slice(4,6)}.${row.pchymd.slice(6,8)}` : '',
				dspymd: (row.dspymd && row.dspymd > '00000000') ? `${row.dspymd.slice(2,4)}.${row.dspymd.slice(4,6)}.${row.dspymd.slice(6,8)}` : '',
				// 미상각잔액 계산 (기초가액이 없으면 취득가액 기준)
				janamt: baseamt === 0 ? pchamt - dprssum : baseamt - dprssum
			}
		})
		mainGrid?.setData(data); vAlert('조회되었습니다.')
	} catch (e) { vAlertError('조회 중 오류 발생') }
}

function excel() { mainGrid?.download("xlsx", `고정자산원장_${searchForm.acctnm}_${searchForm.yy}${searchForm.mm}.xlsx`) }
function print() { const params = `yy=${searchForm.yy}&mm=${searchForm.mm}&acctcd=${searchForm.acctcd}&acctnm=${searchForm.acctnm}&PRTGU=1`; window.open(`/hafa/HAFA_040P?${params}`, 'FixedAssetLedger', 'width=1000,height=800,scrollbars=yes') }

const modalVisible = ref(false); const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })
function openHelp(type: string) {
	if (type === 'ACCT') {
		Object.assign(modalProps, { title: '계정과목 선택', path: '/ha00/HA00_00P_STR', data: { gubun: 'A8', cmpycd: authStore.cmpycd, gbncd: '020', code: '' }, columns: [{ title: '코드', field: 'acctcd', width: 80 }, { title: '계정명', field: 'acctnm', width: 200 }],
			onConfirm: (rawData: any) => {
                const d = rawData;
                searchForm.acctcd = d.acctcd || d.code;
                searchForm.acctnm = d.acctnm || d.cdnm || d.name;
                search()
            }
		})
	}
	modalVisible.value = true
}

onMounted(() => {
	if (typeof window !== 'undefined') (window as any).XLSX = XLSX
	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns', height: '100%', columnDefaults: { headerSort: false, vertAlign: "middle" },
			columns: [
				{ title: "자산정보", columns: [
					{ title: "코드", field: "asetcd", width: 70, hozAlign: "center" },
					{ title: "자산명", field: "asetnm", minWidth: 150, cssClass: "text-primary fw-bold" },
					{ title: "취득일", field: "pchymd_disp", width: 80, hozAlign: "center" },
					{ title: "수량", field: "pchqty", width: 60, hozAlign: "center" },
					{
                        title: "취득가액", field: "pchamt", width: 110, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 },
                        bottomCalc: "sum", bottomCalcFormatter: "money", bottomCalcFormatterParams: { precision: 0 }
                    },
				]},
				{ title: "상각정보", columns: [
					{ title: "방법", field: "dprstypenm", width: 120, hozAlign: "center" },
					{ title: "연수", field: "legalyy", width: 60, hozAlign: "center" },
					{ title: "율", field: "asetrate", width: 60, hozAlign: "center" },
					{
                        title: "기초가액", field: "baseamt", width: 110, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 },
                        bottomCalc: "sum", bottomCalcFormatter: "money", bottomCalcFormatterParams: { precision: 0 }
                    },
				]},
				{ title: "변동 및 감액", columns: [
					{
                        title: "당기증감", field: "incramt", width: 110, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 },
                        bottomCalc: "sum", bottomCalcFormatter: "money", bottomCalcFormatterParams: { precision: 0 }
                    },
					{
                        title: "당기상각", field: "dprsamt", width: 110, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 },
                        bottomCalc: "sum", bottomCalcFormatter: "money", bottomCalcFormatterParams: { precision: 0 }
                    },
					{
                        title: "상각누계", field: "dprssum", width: 110, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 },
                        bottomCalc: "sum", bottomCalcFormatter: "money", bottomCalcFormatterParams: { precision: 0 }
                    },
					{
                        title: "미상각잔액", field: "janamt", width: 120, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 },
                        bottomCalc: "sum", bottomCalcFormatter: "money", bottomCalcFormatterParams: { precision: 0 },
                        cssClass: "fw-bold text-primary"
                    },
				]},
				{ title: "기타", columns: [
					{ title: "보유부서", field: "deptnm", width: 150 },
					{ title: "처분일", field: "dspymd", width: 100, hozAlign: "center", cssClass: "text-danger" },
					{
                        title: "처분금액", field: "dspamt", width: 110, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 },
                        bottomCalc: "sum", bottomCalcFormatter: "money", bottomCalcFormatterParams: { precision: 0 }
                    },
				]}
			]
		})
	}
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
