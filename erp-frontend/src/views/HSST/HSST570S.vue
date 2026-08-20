<!--
	=============================================================
	프로그램명	: 부서별 출고현황 (Inquiry by Department)
	작성일자	: 25.02.24
	작성자	    : AI Assistant
	설명        : 부서별 출고 내역을 상세 조회하는 표준 화면
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container">
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-diagram-3-fill me-2 text-primary" style="font-size: 18px;"></i>
				영업정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				출고관리 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				<span class="text-primary fw-bolder">부서별 출고현황 (HSST570S)</span>
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
									<span class="erp-label me-2">판매부서</span>
									<div class="input-group input-group-sm flex-nowrap">
										<input v-model="searchForm.deptcd" type="text" class="form-control text-center bg-white" style="max-width: 60px;" readonly />
										<input v-model="searchForm.deptnm" type="text" class="form-control" />
										<button class="btn btn-outline-secondary px-2" @click="openHelp('DEPT')"><i class="bi bi-search"></i></button>
									</div>
								</div>
							</td>
							<td>
								<div class="d-flex align-items-center px-2">
									<span class="erp-label me-2">출고일자</span>
									<div class="d-flex align-items-center gap-1 flex-grow-1">
										<input v-model="searchForm.fromdt" type="date" class="form-control form-control-sm" />
										<span class="text-muted">~</span>
										<input v-model="searchForm.todt" type="date" class="form-control form-control-sm" />
									</div>
								</div>
							</td>
							<td>
								<div class="d-flex align-items-center px-2">
									<span class="erp-label me-2">거래처</span>
									<div class="input-group input-group-sm flex-nowrap">
										<input v-model="searchForm.custnm" type="text" class="form-control" placeholder="거래처 선택" @keyup.enter="openHelp('CUST')" />
										<button class="btn btn-outline-secondary px-2" @click="openHelp('CUST')"><i class="bi bi-search"></i></button>
									</div>
								</div>
							</td>
							<td>
								<div class="d-flex align-items-center px-2">
									<span class="erp-label me-2">품목명</span>
									<div class="input-group input-group-sm flex-nowrap">
										<input v-model="searchForm.itemnm" type="text" class="form-control" placeholder="품목명 입력" @keyup.enter="openHelp('ITEM')" />
										<button class="btn btn-outline-secondary px-2" @click="openHelp('ITEM')"><i class="bi bi-search"></i></button>
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
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { useRouter } from 'vue-router'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import type { ModalProps } from '@/types/modal'

const authStore = useAuthStore(); const router = useRouter()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

const searchForm = reactive({
	deptcd: authStore.deptcd, deptnm: authStore.deptnm,
	fromdt: new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString().substring(0, 10),
	todt: new Date().toISOString().substring(0, 10),
	custcd: '', custnm: '', itemcd: '', itemnm: ''
})

const mainGridRef = ref<HTMLDivElement | null>(null); let mainGrid: Tabulator | null = null

const search = async () => {
	try {
		const res = await api.post('/hsst/HSST_570S_STR', {
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
	searchForm.deptcd = authStore.deptcd; searchForm.deptnm = authStore.usernm;
	searchForm.fromdt = new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString().substring(0, 10);
	searchForm.todt = new Date().toISOString().substring(0, 10);
	mainGrid?.clearData();
}

const excel = () => mainGrid?.download("xlsx", "부서별출고현황.xlsx")

const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function openHelp(type: string) {
	const props: any = { gubun: type === 'DEPT' ? 'D0' : (type === 'CUST' ? 'C0' : 'I1'), cmpycd: authStore.cmpycd };
	Object.assign(modalProps, {
		title: type === 'DEPT' ? '부서 선택' : (type === 'CUST' ? '거래처 선택' : '품목 선택'),
		path: '/ha00/HA00_00P_STR',
		data: props,
		columns: [{ title: '코드', field: 'code', width: 100 }, { title: '명칭', field: 'cdnm', width: 200 }],
		onConfirm: (d: any) => { if (type === 'DEPT') { searchForm.deptcd = d.code; searchForm.deptnm = d.cdnm }
                                 else if (type === 'CUST') { searchForm.custcd = d.code; searchForm.custnm = d.cdnm }
                                 else { searchForm.itemcd = d.code; searchForm.itemnm = d.cdnm } }
	}); modalVisible.value = true
}

onMounted(() => {
	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns', height: '100%',
			placeholder: "조회된 자료가 없습니다.",
			columnDefaults: { headerSort: false, headerHozAlign: "center", hozAlign: "center", vertAlign: "middle" },
			columns: [
				{
					title: "출고번호", field: "iono_full", width: 140, cssClass: "fw-bold text-primary cursor-pointer",
					formatter: (cell) => { const d = cell.getData(); return d.ioym && d.iono ? `${d.ioym}-${d.iono}` : ''; },
					cellClick: (e, cell) => {
						const d = cell.getData(); if (!d.ioym) return;
						if (d.giotype === "100") router.push({ path: '/HSIO/HSIO500U', query: { ioym: d.ioym, iono: d.iono, deptcd: d.deptcd } });
						else if (d.giotype === "200") router.push({ path: '/HSIO/HSIO580U', query: { ioym: d.ioym, iono: d.iono, deptcd: d.deptcd } });
                        else router.push({ path: '/HSIO/HSIO570U', query: { ioym: d.ioym, iono: d.iono, deptcd: d.deptcd } });
					}
				},
				{ title: "거래처명", field: "custnm", minWidth: 150, hozAlign: "left" },
				{ title: "출고유형", field: "iotypenm", width: 100 },
				{ title: "출고일자", field: "ioymd", width: 110, formatter: (c) => { const v = c.getValue(); return v ? `${v.substring(0,4)}-${v.substring(4,6)}-${v.substring(6,8)}` : '' } },
				{ title: "품목명", field: "itemnm", minWidth: 200, widthGrow: 2, hozAlign: "left", cssClass: "fw-bold" },
				{ title: "수량", field: "ioqty", hozAlign: "right", width: 90, formatter: "money", formatterParams: { precision: 0 } },
				{ title: "공급가", field: "ioamt", hozAlign: "right", width: 120, formatter: "money", formatterParams: { precision: 0 } },
				{ title: "비고", field: "remark", minWidth: 150, hozAlign: "left" }
			]
		})
	}
})
</script>
