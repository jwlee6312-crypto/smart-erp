<!--
	=============================================================
	프로그램명	: 유형별 출고현황 (Inquiry by Type)
	작성일자	: 25.02.24
	작성자	    : AI Assistant
	설명        : 출고 유형별 내역을 상세 조회하는 표준 화면
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container">
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-grid-3x3-gap-fill me-2 text-primary" style="font-size: 18px;"></i>
				영업정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				출고관리 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				<span class="text-primary fw-bolder">유형별 출고현황 (HSIO610S)</span>
			</div>
			<div class="btn-group-erp d-flex gap-1">
				<button class="btn-erp btn-init" @click="initialize"><i class="bi bi-arrow-clockwise"></i> 초기화</button>
				<button class="btn-erp btn-search" @click="search"><i class="bi bi-search"></i> 조회</button>
				<button class="btn-erp btn-excel" @click="excel"><i class="bi bi-file-earmark-excel"></i> 엑셀</button>
			</div>
		</div>

		<div class="p-2 pb-0 flex-shrink-0">
			<div class="card border shadow-sm overflow-hidden">
				<table class="erp-table-full" style="table-layout: fixed;">
					<colgroup>
						<col style="width: 20%;" /><col style="width: 20%;" /><col style="width: 25%;" /><col style="width: 17.5%;" /><col style="width: 17.5%;" />
					</colgroup>
					<tbody>
						<tr>
							<td>
								<div class="d-flex align-items-center px-2">
									<span class="erp-label">출고창고</span>
									<select v-model="searchForm.whcd" class="form-select">
										<option value="000">전체</option>
										<option v-for="opt in whOptions" :key="opt.code" :value="opt.code">{{ opt.cdnm }}</option>
									</select>
								</div>
							</td>
							<td>
								<div class="d-flex align-items-center px-2">
									<span class="erp-label">출고유형</span>
									<select v-model="searchForm.iotype" class="form-select">
										<option value="000">전체</option>
										<option v-for="opt in ioTypeOptions" :key="opt.code" :value="opt.code">{{ opt.cdnm }}</option>
									</select>
								</div>
							</td>
							<td>
								<div class="d-flex align-items-center px-2">
									<span class="erp-label">출고일자</span>
									<div class="d-flex align-items-center gap-1 flex-grow-1">
										<input v-model="searchForm.fromdt" type="date" class="form-control" />
										<span class="text-muted">~</span>
										<input v-model="searchForm.todt" type="date" class="form-control" />
									</div>
								</div>
							</td>
							<td>
								<div class="d-flex align-items-center px-2">
									<span class="erp-label">거래처</span>
									<div class="input-group flex-nowrap">
										<input v-model="searchForm.custnm" type="text" class="form-control" placeholder="거래처" @keyup.enter="handleOpenHelp('CUST')" />
										<button class="btn btn-outline-secondary" @click="handleOpenHelp('CUST')"><i class="bi bi-search"></i></button>
									</div>
								</div>
							</td>
							<td>
								<div class="d-flex align-items-center px-2">
									<span class="erp-label">품목명</span>
									<div class="input-group flex-nowrap">
										<input v-model="searchForm.itemnm" type="text" class="form-control" placeholder="품목명" @keyup.enter="handleOpenHelp('ITEM')" />
										<button class="btn btn-outline-secondary" @click="handleOpenHelp('ITEM')"><i class="bi bi-search"></i></button>
									</div>
								</div>
							</td>
						</tr>
					</tbody>
				</table>
			</div>
		</div>

		<div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column">
			<div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white">
                <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
                  <div ref="mainGridRef" class="tabulator-full-height" />
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
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import type { ModalProps } from '@/types/modal'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

const searchForm = reactive({
	whcd: '000', iotype: '000',
	fromdt: new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString().substring(0, 10),
	todt: new Date().toISOString().substring(0, 10),
	custcd: '', custnm: '', itemcd: '', itemnm: ''
})

const whOptions = ref<any[]>([]); const ioTypeOptions = ref<any[]>([])
const mainGridRef = ref<HTMLDivElement | null>(null); let mainGrid: Tabulator | null = null

const search = async () => {
	try {
		const res = await api.post('/hsio/HSIO_610S_STR', {
			...searchForm, cmpycd: authStore.cmpycd,
			fromdt: searchForm.fromdt?.replace(/-/g, '') || '',
			todt: searchForm.todt?.replace(/-/g, '') || ''
		})
		mainGrid?.setData(res.data || [])
		vAlert('조회되었습니다.')
	} catch (e) { vAlertError('조회 실패') }
}

const initialize = () => {
	resetForm(searchForm);
	searchForm.whcd = '000'; searchForm.iotype = '000';
	searchForm.fromdt = new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString().substring(0, 10);
	searchForm.todt = new Date().toISOString().substring(0, 10);
	mainGrid?.clearData();
}

const excel = () => mainGrid?.download("xlsx", "유형별출고현황.xlsx")

const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function handleOpenHelp(type: string) {
	const props: any = { gubun: type === 'CUST' ? 'C0' : 'I1', cmpycd: authStore.cmpycd, codenm: type === 'CUST' ? searchForm.custnm : searchForm.itemnm };
	Object.assign(modalProps, {
		title: type === 'CUST' ? '거래처 선택' : '품목 선택',
		path: type === 'CUST' ? '/ha00/HA00_00P_STR' : '/hs00/HS00_000S_STR',
		data: props,
		columns: type === 'CUST' ? [{ title: '코드', field: 'custcd', width: 100 }, { title: '명칭', field: 'custnm', width: 200 }]
                                : [{ title: '코드', field: 'itemcd', width: 100 }, { title: '명칭', field: 'itemnm', width: 200 }],
		onConfirm: (d: any) => { if (type === 'CUST') { searchForm.custcd = d.custcd; searchForm.custnm = d.custnm } else { searchForm.itemcd = d.itemcd; searchForm.itemnm = d.itemnm } }
	}); modalVisible.value = true
}

onMounted(async () => {
	if (typeof window !== 'undefined') (window as any).XLSX = XLSX;
	try {
		const resWh = await api.post('/hs00/HS00_000S_STR', { gubun: 'W0', cmpycd: authStore.cmpycd })
		whOptions.value = resWh.data || []
		const resType = await api.post('/hs00/HS00_000S_STR', { gubun: 'E0', cmpycd: authStore.cmpycd, gbncd: '130' })
		ioTypeOptions.value = resType.data || []
	} catch (e) {}

	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns', height: '100%',
			placeholder: "조회된 자료가 없습니다.",
			columnDefaults: { headerSort: false, headerHozAlign: "center", hozAlign: 'right', vertAlign: "middle" },
			columns: [
				{ title: "출고일", field: "ioymd", width: 110, hozAlign: "center", formatter: (c) => { const v = c.getValue(); return v ? `${v.substring(0,4)}-${v.substring(4,6)}-${v.substring(6,8)}` : '' } },
				{ title: "거래처", field: "custnm", minWidth: 180, hozAlign: "left", cssClass: "fw-bold" },
				{ title: "품목명", field: "itemnm", minWidth: 200, widthGrow: 2, hozAlign: "left" },
				{ title: "수량", field: "ioqty", width: 100, formatter: "money", formatterParams: { precision: 0 } },
				{ title: "공급가", field: "jsanamt", width: 120, formatter: "money", formatterParams: { precision: 0 } },
				{ title: "특기사항", field: "remark", minWidth: 200, hozAlign: "left" }
			]
		})
	}
})
</script>

<style scoped>
.tabulator-full-height { width: 100% !important; background-color: #fff; border-bottom: 3px solid #005a9f !important; }
</style>
