<!--
	=============================================================
	프로그램명	: 기간별 출고현황 (Inquiry by Period)
	작성일자	: 25.02.24
	작성자	    : AI Assistant
	설명        : 기간별/창고별 출고 내역을 상세 조회하는 표준 화면
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container">
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-calendar-check me-2 text-primary" style="font-size: 18px;"></i>
				영업정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				출고관리 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				<span class="text-primary fw-bolder">기간별 출고현황 (HSIO600S)</span>
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
						<col style="width: 25%;" /><col style="width: 25%;" /><col style="width: 25%;" /><col style="width: 25%;" />
					</colgroup>
					<tbody>
						<tr>
							<td>
								<div class="d-flex align-items-center px-2">
									<span class="erp-label me-2" style="width: 80px;">출고창고</span>
									<select v-model="searchForm.whcd" class="form-select form-select-sm">
										<option value="000">전체</option>
										<option v-for="opt in whOptions" :key="opt.code" :value="opt.code">{{ opt.cdnm }}</option>
									</select>
								</div>
							</td>
							<td>
								<div class="d-flex align-items-center px-2">
									<span class="erp-label me-2" style="width: 80px;">출고일자</span>
									<div class="d-flex align-items-center gap-1 flex-grow-1">
										<input v-model="searchForm.fromdt" type="date" class="form-control form-control-sm" />
										<span class="text-muted">~</span>
										<input v-model="searchForm.todt" type="date" class="form-control form-control-sm" />
									</div>
								</div>
							</td>
							<td>
								<div class="d-flex align-items-center px-2">
									<span class="erp-label me-2" style="width: 80px;">출고유형</span>
									<select v-model="searchForm.iotype" class="form-select form-select-sm">
										<option value="000">전체</option>
										<option v-for="opt in ioTypeOptions" :key="opt.code" :value="opt.code">{{ opt.cdnm }}</option>
									</select>
								</div>
							</td>
							<td>
								<div class="d-flex align-items-center px-2">
									<span class="erp-label me-2" style="width: 80px;">거래처</span>
									<div class="input-group input-group-sm flex-nowrap flex-grow-1">
										<input v-model="searchForm.custnm" type="text" class="form-control" placeholder="거래처 선택" @keyup.enter="handleOpenHelp('CUST')" />
										<button class="btn btn-outline-secondary px-2" @click="handleOpenHelp('CUST')"><i class="bi bi-search"></i></button>
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
import { getDate } from '@/composables/useDate'
import { useRouter } from 'vue-router'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import type { ModalProps } from '@/types/modal'

const authStore = useAuthStore(); const router = useRouter()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset(); const { firstDay, today } = getDate()

const searchForm = reactive({ whcd: '000', fromdt: firstDay, todt: today, iotype: '000', custcd: '', custnm: '' })
const whOptions = ref<any[]>([]); const ioTypeOptions = ref<any[]>([])
const mainGridRef = ref<HTMLDivElement | null>(null); let mainGrid: Tabulator | null = null

const search = async () => {
	try {
		const res = await api.post('/hsio/HSIO_600S_STR', {
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
	searchForm.fromdt = firstDay; searchForm.todt = today;
	mainGrid?.clearData();
}

const excel = () => mainGrid?.download("xlsx", "기간별출고현황.xlsx")

const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function handleOpenHelp(type: string) {
	if (type === 'CUST') {
		Object.assign(modalProps, {
			title: '거래처 선택', path: '/ha00/HA00_00P_STR', data: { gubun: 'C0', cmpycd: authStore.cmpycd, codenm: searchForm.custnm },
			columns: [{ title: '코드', field: 'custcd', width: 100 }, { title: '거래처명', field: 'custnm', width: 200 }],
			onConfirm: (d: any) => { searchForm.custcd = d.custcd; searchForm.custnm = d.custnm }
		}); modalVisible.value = true
	}
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
				{
					title: "출고번호", field: "iono_full", width: 140, hozAlign: "center", cssClass: "fw-bold text-primary cursor-pointer",
					formatter: (cell) => { const d = cell.getData(); return d.ioym && d.iono ? `${d.ioym}-${d.iono}` : ''; },
					cellClick: (e, cell) => {
						const d = cell.getData(); if (!d.ioym || !d.iono) return;
						if (d.iotype === "100" && d.gubun === "1" && d.spyamt >= 0) router.push({ path: '/HSIO/HSIO500U', query: { ioym: d.ioym, iono: d.iono, deptcd: d.deptcd } });
						else if (d.iotype === "100" && d.gubun === "1" && d.spyamt < 0) router.push({ path: '/HSIO/HSIO490U', query: { ioym: d.ioym, iono: d.iono, deptcd: d.deptcd } });
						else if (d.iotype === "100" && d.gubun === "2") router.push({ path: '/HSOD/HSOD200S', query: { ioym: d.ioym, iono: d.iono, deptcd: d.deptcd } });
						else if (d.iotype === "200") router.push({ path: '/HSIO/HSIO580U', query: { ioym: d.ioym, iono: d.iono, deptcd: d.deptcd } });
						else router.push({ path: '/HSIO/HSIO570U', query: { ioym: d.ioym, iono: d.iono, deptcd: d.deptcd } });
					}
				},
				{ title: "출고유형", field: "iotypenm", width: 120, hozAlign: "center" },
				{ title: "출고일자", field: "ioymd", width: 110, hozAlign: "center", formatter: (c) => { const v = c.getValue(); return v ? `${v.substring(0,4)}-${v.substring(4,6)}-${v.substring(6,8)}` : '' } },
				{ title: "품목명", field: "itemnm", minWidth: 200, widthGrow: 2, hozAlign: "left", cssClass: "fw-bold" },
				{ title: "수량", field: "ioqty", width: 90, formatter: "money", formatterParams: { precision: 0 } },
				{ title: "공급가", field: "jsanamt", width: 130, formatter: "money", formatterParams: { precision: 0 } },
				{ title: "특기사항", field: "remark", minWidth: 200, hozAlign: "left" }
			]
		})
	}
})
</script>

<style scoped>
.tabulator-full-height { width: 100% !important; background-color: #fff; border-bottom: 3px solid #005a9f !important; }
</style>
