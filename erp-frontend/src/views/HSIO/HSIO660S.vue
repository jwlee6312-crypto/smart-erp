<!--
	=============================================================
	프로그램명	: 배송자별 상차현황 (Loading Status by Delivery)
	작성일자	: 25.02.24
	작성자	    : AI Assistant
	설명        : 배송담당자별 상차 내역을 상세 조회하는 표준 화면
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container">
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-truck me-2 text-primary" style="font-size: 18px;"></i>
				영업정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				출고관리 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				<span class="text-primary fw-bolder">배송자별 상차현황 (HSIO660S)</span>
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
						<col style="width: 50%;" />
						<col style="width: 50%;" />
					</colgroup>
					<tbody>
						<tr>
							<td>
								<div class="d-flex align-items-center px-2">
									<span class="erp-label me-2">배송담당</span>
									<div class="input-group input-group-sm flex-nowrap" style="max-width: 300px;">
										<input v-model="searchForm.TRNEMP" type="text" class="form-control text-center bg-white" style="max-width: 80px;" readonly />
										<input v-model="searchForm.TRNempnm" type="text" class="form-control" placeholder="담당자 선택" @keyup.enter="openHelp('TRNEMP')" />
										<button class="btn btn-outline-secondary px-2" @click="openHelp('TRNEMP')"><i class="bi bi-search"></i></button>
									</div>
								</div>
							</td>
							<td>
								<div class="d-flex align-items-center px-2">
									<span class="erp-label me-2">출고일자</span>
									<div class="d-flex align-items-center gap-1" style="max-width: 200px;">
										<input v-model="searchForm.OUtymd" type="date" class="form-control form-control-sm" />
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
	TRNEMP: authStore.userid,
	TRNempnm: authStore.usernm,
	OUtymd: new Date().toISOString().substring(0, 10)
})

const mainGridRef = ref<HTMLDivElement | null>(null); let mainGrid: Tabulator | null = null

const search = async () => {
	try {
		const res = await api.post('/hsio/HSIO_660S_STR', {
			...searchForm,
			cmpycd: authStore.cmpycd,
			OUtymd: searchForm.OUtymd?.replace(/-/g, '') || ''
		})
		mainGrid?.setData(res.data || [])
		vAlert('조회되었습니다.')
	} catch (e) { vAlertError('조회 실패') }
}

const initialize = () => {
	resetForm(searchForm);
	searchForm.TRNEMP = authStore.userid;
	searchForm.TRNempnm = authStore.usernm;
	searchForm.OUtymd = new Date().toISOString().substring(0, 10);
	mainGrid?.clearData();
}

const excel = () => mainGrid?.download("xlsx", "배송자별상차현황.xlsx")

const modalVisible = ref(false);
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function openHelp(type: string) {
	if (type === 'TRNEMP') {
		Object.assign(modalProps, {
			title: '배송담당자 선택', path: '/ha00/HA00_00P_STR', defaultField: 'cdnm',
			data: { gubun: 'U1', cmpycd: authStore.cmpycd },
			columns: [{ title: '코드', field: 'code', width: 100 }, { title: '성명', field: 'cdnm', width: 200 }],
			onConfirm: (d: any) => { searchForm.TRNEMP = d.code; searchForm.TRNempnm = d.cdnm }
		})
	}
	modalVisible.value = true
}

onMounted(() => {
	if (typeof window !== 'undefined') (window as any).XLSX = XLSX;
	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns', height: '100%',
			placeholder: "조회된 자료가 없습니다.",
			columnDefaults: { headerSort: false, headerHozAlign: "center", hozAlign: 'right', vertAlign: "middle" },
			columns: [
				{ title: "품목명", field: "itemnm", minWidth: 250, widthGrow: 2, hozAlign: "left", cssClass: "fw-bold" },
				{ title: "규격", field: "itsize", width: 150, hozAlign: "left" },
				{ title: "단위", field: "unit", width: 80, hozAlign: "center" },
				{ title: "배송지역", field: "areanm", width: 180, hozAlign: "left" },
				{ title: "수량", field: "ioqty", width: 110, formatter: "money", formatterParams: { precision: 0 } },
				{ title: "비고", field: "remark", minWidth: 200, hozAlign: "left" }
			]
		})
	}
})
</script>

<style scoped>
.tabulator-full-height { width: 100% !important; background-color: #fff; border-bottom: 3px solid #005a9f !important; }
</style>
