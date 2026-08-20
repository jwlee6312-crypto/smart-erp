<!--
	=============================================================
	프로그램명: 월별 감가상각명세서 (HAFA140S)
	작성일자	: 2025.03.14
	작성자    : AI Assistant
	설명        : 계정과목별 개별 자산의 월별 감가상각 현황 조회
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container d-flex flex-column h-100 bg-white">
		<!-- 🚀 1. 상단 액션 바 -->
		<div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
			<div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-calendar-range me-2 text-primary" style="font-size: 18px;"></i>
				고정자산 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
				상각현황 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
				<span class="text-primary fw-bolder">월별 감가상각명세서(HAFA140S)</span>
			</div>
			<div class="btn-group-erp d-flex gap-1 pe-3">
				<button class="btn-erp btn-search" @click="search">조회</button>
				<button class="btn-erp btn-print" @click="print">인쇄</button>
				<button class="btn-erp btn-excel" @click="excel">엑셀</button>
			</div>
		</div>

		<!-- 💡 2. 메인 컨텐츠 영역 -->
		<div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

			<!-- [상단] 조회 필터 영역 -->
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
									<div class="input-group input-group-sm px-2" style="width: 300px;">
										<input v-model="searchForm.acctcd" type="text" class="form-control text-center bg-light fw-bold" style="max-width: 65px;" readonly />
										<input v-model="searchForm.acctnm" type="text" class="form-control" @keydown.enter="openHelp('ACCT')" placeholder="계정 선택" />
										<button class="btn btn-outline-secondary" @click="openHelp('ACCT')"><i class="bi bi-search"></i></button>
									</div>
								</td>
								<th class="text-center bg-light">기준년월</th>
								<td>
									<div class="d-flex align-items-center gap-1 px-2">
										<select v-model="searchForm.yy" class="form-select form-select-sm" style="width: 100px;" @change="updateHeader">
											<option v-for="y in yearOptions" :key="y" :value="y">{{ y }}년</option>
										</select>
										<select v-model="searchForm.mm" class="form-select form-select-sm" style="width: 80px;" @change="updateHeader">
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
const yearOptions = Array.from({ length: 6 }, (_, i) => String(currentYear - i))
const monthOptions = Array.from({ length: 12 }, (_, i) => String(i + 1).padStart(2, '0'))

const searchForm = reactive({ acctcd: '', acctnm: '', yy: String(currentYear), mm: currentMonth })
const monthTitles = ref<string[]>(Array(12).fill(''))
const mainGridRef = ref<HTMLDivElement | null>(null); let mainGrid: Tabulator | null = null

const updateHeader = async () => {
	try {
		const res = await api.post('/hafa/HA00_150S_STR', { cmpycd: authStore.cmpycd, baseym: searchForm.yy + searchForm.mm })
		if (res.data && res.data[0]) {
			const header = res.data[0];
			monthTitles.value = Array.from({ length: 12 }, (_, i) => {
                const keyM = 'm' + String(i + 1).padStart(2, '0');
                const keyC = 'col' + i;
				const val = header[keyM] || header[keyC] || '';
                return val ? val.substring(4, 6) + '월' : `${i + 1}월`
			})
			if (mainGrid) {
				const columns = mainGrid.getColumns()
				monthTitles.value.forEach((title, i) => { if(columns[i + 2]) columns[i + 2].updateDefinition({ title }) })
			}
		}
	} catch (e) { console.error('Header fetch error', e) }
}

async function search() {
	if (!searchForm.acctcd) return vAlertError('계정과목을 선택하세요.')
	try {
		await updateHeader()
		const res = await api.post('/hafa/HAFA_140S_STR', { cmpycd: authStore.cmpycd, baseym: searchForm.yy + searchForm.mm, acctcd: searchForm.acctcd })
		const data = (res.data || []).map((row: any) => {
			const months = Array.from({ length: 12 }, (_, i) => {
                const key = 'm' + String(i + 1).padStart(2, '0');
                return Number(row[key] || 0);
            })
			return {
                ...row,
                ...Object.fromEntries(months.map((m, i) => [`m${String(i + 1).padStart(2, '0')}`, m])),
                total: months.reduce((a, b) => a + b, 0),
                acctcd: searchForm.acctcd
            }
		})
		mainGrid?.setData(data);
        vAlert('조회되었습니다.')
	} catch (e) { vAlertError('조회 중 오류 발생') }
}

function excel() { mainGrid?.download("xlsx", `월별감가상각명세서_${searchForm.acctnm}_${searchForm.yy}${searchForm.mm}.xlsx`) }
function print() { const params = `yy=${searchForm.yy}&mm=${searchForm.mm}&acctcd=${searchForm.acctcd}&acctnm=${searchForm.acctnm}&PRTGU=1`; window.open(`/hafa/HAFA_140P?${params}`, 'MonthlyDepreciation', 'width=1000,height=800,scrollbars=yes') }

const modalVisible = ref(false); const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })
function openHelp(type: string) {
	if (type === 'ACCT') {
		Object.assign(modalProps, {
            title: '계정과목 선택',
            path: '/ha00/HA00_00P_STR',
            data: { gubun: 'A8', cmpycd: authStore.cmpycd, gbncd: '020', code: '' },
            columns: [{ title: '코드', field: 'acctcd', width: 80 }, { title: '계정명', field: 'acctnm', width: 200 }],
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
	if (mainGridRef.value) {
		const monthCols = Array.from({ length: 12 }, (_, i) => ({
            title: `${i + 1}월`,
            field: `m${String(i + 1).padStart(2, '0')}`,
            width: 100,
            hozAlign: "right",
            formatter: "money",
            formatterParams: { precision: 0 },
            bottomCalc: "sum",
            bottomCalcFormatter: "money",
            bottomCalcFormatterParams: { precision: 0 }
        }))
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns', height: '100%', columnDefaults: { headerSort: false, vertAlign: "middle" },
			columns: [
				{ title: "자산코드", field: "asetcd", width: 80, hozAlign: "center", frozen: true, cssClass: "text-primary fw-bold" },
				{ title: "자산명", field: "asetnm", minWidth: 150, frozen: true, cssClass: "fw-bold" },
				...monthCols,
				{
                    title: "합계",
                    field: "total",
                    width: 110,
                    hozAlign: "right",
                    formatter: "money",
                    formatterParams: { precision: 0 },
                    bottomCalc: "sum",
                    bottomCalcFormatter: "money",
                    bottomCalcFormatterParams: { precision: 0 },
                    cssClass: "fw-bold text-primary bg-light"
                }
			]
		})
	}
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
