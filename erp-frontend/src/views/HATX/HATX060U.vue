<!--	=============================================================
	프로그램명 : 매출부가세접수
	작성일자: 2025.02.24
	작성자    : AI Assistant
	설명        : 매출 부가세 내역의 접수 확인 및 전자발행 여부 일괄 관리
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container">
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-check2-square me-2 text-primary" style="font-size: 18px;"></i>
				재무관리<i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				<span class="text-primary fw-bolder">매출부가세접수(HATX060U)</span>
			</div>
			<div class="btn-group-erp d-flex gap-1">
				<button class="btn-erp btn-save" @click="save">
					<i class="bi bi-check-lg"></i> {{ searchForm.cfmyn === 'N' ? '접수처리' : '취소처리' }}
				</button>
				<button class="btn-erp btn-search" @click="search">
					<i class="bi bi-search"></i> 조회
				</button>
			</div>
		</div>

		<!-- [검색] 검색 조건 영역 -->
		<div class="p-2 pb-0 flex-shrink-0">
			<div class="card border shadow-sm bg-white overflow-hidden">
				<div class="card-body p-2 bg-light">
					<div class="d-flex align-items-center flex-wrap gap-3">
						<div class="d-flex align-items-center">
							<span class="erp-label">사업장</span>
							<select v-model="searchForm.taxunit" class="form-select" @change="search">
								<option v-for="opt in taxUnitOptions" :key="opt.code" :value="opt.code">{{ opt.name }}</option>
							</select>
						</div>
						<div class="d-flex align-items-center">
							<span class="erp-label">발행일</span>
							<div class="d-flex align-items-center gap-1">
								<input v-model="searchForm.fromdt" type="date" class="form-control" style="width: 135px;" />
								<span>~</span>
								<input v-model="searchForm.todt" type="date" class="form-control" style="width: 135px;" />
							</div>
						</div>
						<div class="d-flex align-items-center">
							<span class="erp-label">조회상태</span>
							<select v-model="searchForm.cfmyn" class="form-select" style="width: 100px;" @change="search">
								<option value="N">미접수</option>
								<option value="Y">접수</option>
							</select>
						</div>
					</div>
				</div>
			</div>
		</div>

		<!-- [그리드] 그리드 영역 -->
		<div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column">
			<div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white">
                <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
                  <div ref="mainGridRef" class="tabulator-instance flex-grow-1"></div>
                </div>
			</div>
		</div>
	</div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import AppAlert from '@/components/AppAlert.vue'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()

const today = new Date().toISOString().slice(0, 10)
const firstDay = today.slice(0, 8) + '01'

const taxUnitOptions = ref<any[]>([])

const searchForm = reactive({
	taxunit: '',
	fromdt: firstDay,
	todt: today,
	cfmyn: 'N'
})

const mainGridRef = ref<HTMLDivElement | null>(null)
let mainGrid: Tabulator | null = null

const fetchOptions = async () => {
	try {
		const res = await api.post('/ha00/HA00_00P_STR', { gubun: 'SA', cmpycd: authStore.cmpycd })
		taxUnitOptions.value = (res.data || []).map((i: any) => ({ code: i.taxunit, name: i.unitnm }))
		if (taxUnitOptions.value.length > 0) searchForm.taxunit = taxUnitOptions.value[0].code
	} catch (e) { console.error(e) }
}

async function search() {
	if (!searchForm.taxunit) return vAlertError('사업장을 선택하세요.')

	try {
		const res = await api.post('/hatx/HATX_010U_STR', {
			actkind: 'S0',
			cmpycd: authStore.cmpycd,
			taxunit: searchForm.taxunit,
			fromdt: searchForm.fromdt.replace(/-/g, ''),
			todt: searchForm.todt.replace(/-/g, ''),
			cfmyn: searchForm.cfmyn,
			taxkind: '200'
		})

		const data = (res.data || []).map((row: any) => ({
			pubymd: row.pubymd,
			custnm: row.custnm,
			typenm: row.typenm,
			supyamt: Number(row.supyamt || 0),
			vatamt: Number(row.vatamt || 0),
			totamt: Number(row.totamt || 0),
			descnm: row.descnm,
			taxym: row.taxym,
			taxno: row.taxno,
			elcyn: row.elcyn === 'Y',
			_selected: searchForm.cfmyn === 'N'
		}))

		mainGrid?.setData(data)
		if (searchForm.cfmyn === 'N') {
			mainGrid?.selectRow()
		}
		vAlert('조회되었습니다.')
	} catch (e) { vAlertError('조회 중 오류 발생') }
}

async function save() {
	const selectedRows = mainGrid?.getSelectedData() || []
	if (selectedRows.length === 0) return vAlertError('처리할 대상을 선택해 주십시오.')

	const msg = searchForm.cfmyn === 'N' ? '선택한 부가세를 접수처리 하시겠습니까?' : '선택한 부가세를 취소처리 하시겠습니까?'
	if (!confirm(msg)) return

	try {
		for (const row of selectedRows) {
			await api.post('/hatx/HATX_060U_STR', {
				actkind: 'U0',
				cmpycd: authStore.cmpycd,
				taxunit: searchForm.taxunit,
				fromdt: searchForm.fromdt.replace(/-/g, ''),
				todt: searchForm.todt.replace(/-/g, ''),
				cfmyn: searchForm.cfmyn,
				taxkind: '200',
				taxym: row.taxym,
				taxno: row.taxno,
				elcyn: row.elcyn ? 'Y' : 'N',
				userid: authStore.userid
			})
		}
		vAlert('정상적으로 처리되었습니다.')
		search()
	} catch (e) { vAlertError('처리 중 오류 발생') }
}

onMounted(() => {
	fetchOptions()
	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns',
			height: '100%',
			selectable: true,
			columnDefaults: { headerSort: false, vertAlign: "middle" },
			columns: [
				{ formatter: "rowSelection", titleFormatter: "rowSelection", width: 40, hozAlign: "center", headerSort: false },
				{
					title: "전자", field: "elcyn", width: 50, hozAlign: "center",
					formatter: "tickCross", formatterParams: { tickElement: '<i class="bi bi-check-square-fill text-primary"></i>', crossElement: '<i class="bi bi-square text-muted"></i>' },
					cellClick: (e, cell) => { cell.setValue(!cell.getValue()) }
				},
				{
					title: "발행일", field: "pubymd", width: 110, hozAlign: "center",
					formatter: (cell) => { const v = cell.getValue(); return v ? `${v.slice(0,4)}-${v.slice(4,6)}-${v.slice(6,8)}` : '' }
				},
				{ title: "상호", field: "custnm", width: 250 },
				{ title: "유형", field: "typenm", width: 150, hozAlign: "center" },
				{ title: "공급가", field: "supyamt", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
				{ title: "부가세", field: "vatamt", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
				{ title: "합계", field: "totamt", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "fw-bold" },
				{ title: "적요", field: "descnm", widthGrow: 1 }
			]
		})
	}
})
</script>
