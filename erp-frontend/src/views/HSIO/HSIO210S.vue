<!--
	=============================================================
	프로그램명	: 입고현황 (Purchase Receipt Status Report)
	작성일자	: 25.02.24
	작성자	    : AI Assistant
	설명        : [최종완성] HSIP110U 표준 UI 및 상하정중앙 정렬 완벽 보정
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container">
		<!-- 🚀 1. 상단 액션 바 (표준 버튼 배치 및 색상) -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-file-earmark-bar-graph me-2 text-primary" style="font-size: 18px;"></i>
				구매정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				입고관리 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				<span class="text-primary fw-bolder">입고현황</span>
			</div>
			<div class="btn-group-erp d-flex gap-1">
				<button class="btn-erp btn-init" @click="initialize">초기화</button>
				<button class="btn-erp btn-search" @click="fetchList">조회</button>
				<button class="btn-erp btn-print" @click="print">인쇄</button>
			</div>
		</div>

		<!-- 🔍 2. 검색 조건 영역 (2행 3열 고밀도 표준) -->
		<div class="p-2 pb-0">
			<div class="card border shadow-sm">
				<div class="card-body p-0">
					<table class="erp-table-full">
						<colgroup>
							<col style="width: 100px;"><col />
							<col style="width: 100px;"><col />
							<col style="width: 100px;"><col />
						</colgroup>
						<tbody>
							<tr>
								<th>입고창고</th>
								<td>
									<select v-model="searchForm.whcd" class="form-select form-select-sm">
										<option value="000">전체</option>
										<option v-for="opt in whOptions" :key="opt.code" :value="opt.code">{{ opt.cdnm }}</option>
									</select>
								</td>
								<th class="required">입고일자</th>
								<td>
									<div class="d-flex align-items-center gap-1">
										<input v-model="searchForm.fromdt" type="date" class="form-control form-control-sm" />
										<span class="text-muted">~</span>
										<input v-model="searchForm.todt" type="date" class="form-control form-control-sm" />
									</div>
								</td>
								<th>입고유형</th>
								<td>
									<select v-model="searchForm.iotype" class="form-select form-select-sm">
										<option value="000">전체</option>
										<option v-for="opt in typeOptions" :key="opt.code" :value="opt.code">{{ opt.cdnm }}</option>
									</select>
								</td>
							</tr>
							<tr>
								<th>거&nbsp;&nbsp;래&nbsp;&nbsp;처</th>
								<td>
									<div class="input-group input-group-sm">
										<input v-model="searchForm.custcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
										<input v-model="searchForm.custnm" type="text" class="form-control" placeholder="거래처 검색" />
										<button class="btn btn-outline-secondary px-2" @click="openHelp('CUST')"><i class="bi bi-search"></i></button>
									</div>
								</td>
								<th>품&nbsp;&nbsp;목&nbsp;&nbsp;명</th>
								<td colspan="3">
									<div class="input-group input-group-sm">
										<input v-model="searchForm.itemcd" type="text" class="form-control text-center bg-light" style="max-width: 80px;" readonly />
										<input v-model="searchForm.itemnm" type="text" class="form-control" placeholder="품목 검색" />
										<button class="btn btn-outline-secondary px-2" @click="openHelp('ITEM')"><i class="bi bi-search"></i></button>
                                        <input v-model="searchForm.itsize" type="text" class="form-control bg-light" style="max-width: 150px;" readonly placeholder="규격" />
                                        <input v-model="searchForm.unit" type="text" class="form-control bg-light" style="max-width: 60px;" readonly placeholder="단위" />
									</div>
								</td>
							</tr>
						</tbody>
					</table>
				</div>
			</div>
		</div>

		<!-- 📊 3. 그리드 영역 -->
		<div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2">
			<div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white">
				<div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center" style="height: 40px;">
					<span class="fw-bold small text-dark d-flex align-items-center">
						<i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i> 입고 상세 내역 현황
					</span>
				</div>
                <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column" style="min-height: 0;">
                  <div ref="mainGridRef" class="tabulator-full-height" />
                </div>
			</div>
		</div>
	</div>

	<Modal v-model:visible="modalVisible" :modalProps="modalProps" />
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, onUnmounted } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { getDate } from '@/composables/useDate'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { firstDay, today } = getDate()

const searchForm = reactive<any>({
	whcd: '000',
	iotype: '000',
	fromdt: firstDay,
	todt: today,
	custcd: '', custnm: '',
	itemcd: '', itemnm: '', itsize: '', unit: ''
})

const whOptions = ref<any[]>([])
const typeOptions = ref<any[]>([])
const mainGridRef = ref<HTMLDivElement | null>(null); let mainGrid: Tabulator | null = null

async function loadInitData() {
	try {
		const [resWh, resType] = await Promise.all([
			api.get('/hs00/HS00_000S_STR', { params: { gubun: 'W0', cmpycd: authStore.cmpycd } }),
			api.get('/hs00/HS00_000S_STR', { params: { gubun: 'E0', cmpycd: authStore.cmpycd, gbncd: '120' } })
		])
		whOptions.value = resWh.data.map((i: any) => ({ code: i.code || i.whcd, cdnm: i.cdnm || i.whnm }))
		typeOptions.value = resType.data.map((i: any) => ({ code: i.code, cdnm: i.cdnm }))
	} catch (e) {}
}

async function fetchList() {
	try {
		const res = await api.post('/hsio/HSIO_210S_STR', {
			...searchForm,
			cmpycd: authStore.cmpycd,
			fromdt: searchForm.fromdt.replace(/-/g, ''),
			todt: searchForm.todt.replace(/-/g, '')
		})
		mainGrid?.setData(res.data || [])
		vAlert('조회되었습니다.')
	} catch (e) { vAlertError('조회 실패') }
}

function initialize() {
	resetForm(searchForm)
	searchForm.whcd = '000'; searchForm.iotype = '000'
	searchForm.fromdt = firstDay
	searchForm.todt = today
	mainGrid?.clearData()
}

const print = () => { vAlert('인쇄 기능을 준비 중입니다.') }

const modalVisible = ref(false); const modalProps = reactive<any>({ title: '', path: '', onConfirm: () => {} })

const openHelp = (type: string) => {
    if (type === 'CUST') {
        Object.assign(modalProps, {
            title: '거래처 선택',
            path: '/ha00/HA00_00P_STR',
            data: { gubun: 'C4', cmpycd: authStore.cmpycd, code: searchForm.custnm, codenm: '', remark: '' },
            columns: [
                { title: '코드', field: 'custcd', width: 100, hozAlign: 'center' },
                { title: '거래처명', field: 'custnm', width: 200 }
            ],
            onConfirm: (d: any) => {
                searchForm.custcd = d.custcd;
                searchForm.custnm = d.custnm;
            }
        })
        modalVisible.value = true
    } else if (type === 'ITEM') {
        Object.assign(modalProps, {
            title: '품목 선택',
            path: '/hs00/HS00_000S_STR',
            data: { gubun: 'I1', cmpycd: authStore.cmpycd, gbncd: '2', code: '', codenm: searchForm.itemnm, remark: '' },
            columns: [
                { title: '코드', field: 'itemcd', width: 100, hozAlign: 'center' },
                { title: '품목명', field: 'itemnm', width: 200 },
                { title: '규격', field: 'itsize', width: 150 },
                { title: '단위', field: 'unitnm', width: 80, hozAlign: 'center' }
            ],
            onConfirm: (d: any) => {
                searchForm.itemcd = d.itemcd;
                searchForm.itemnm = d.itemnm;
                searchForm.itsize = d.itsize;
                searchForm.unit = d.unitnm || d.unit;
            }
        })
        modalVisible.value = true
    }
}

onUnmounted(() => {
  if (mainGrid) mainGrid.destroy();
});

onMounted(async () => {
	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns', height: '100%',
			columnDefaults: {
				headerSort: false,
				headerHozAlign: "center",
				hozAlign: 'right', // 🚀 기본값을 우측 정렬로 설정
				vertAlign: 'middle',
				minWidth: 100
			},
			columns: [
				{ title: '일자', field: 'ioymd', width: 100, hozAlign: 'center',
                  formatter: (c) => { const v = c.getValue(); return v && v.length === 8 ? `${v.substring(0,4)}-${v.substring(4,6)}-${v.substring(6,8)}` : v } },
				{ title: '품명', field: 'itemnm', minWidth: 200, widthGrow: 1, cssClass: 'fw-bold' },
				{ title: '규격', field: 'itsize', width: 150 },
				{ title: '단위', field: 'unit', width: 60, hozAlign: 'center' },
				{ title: '거래처', field: 'custnm', minWidth: 180, widthGrow: 1 },
				{ title: '수량', field: 'ioqty', hozAlign: 'right', width: 90, formatter: 'money', formatterParams: { precision: 0 } },
				{ title: '단가', field: 'price', hozAlign: 'right', width: 100, formatter: 'money', formatterParams: { precision: 0 },
                  mutatorData: (v, d) => (Number(d.jsanamt) / (Number(d.ioqty) || 1)) },
				{ title: '공급가', field: 'jsanamt', hozAlign: 'right', width: 120, formatter: 'money', formatterParams: { precision: 0 } },
				{ title: '부가세', field: 'jsanvat', hozAlign: 'right', width: 110, formatter: 'money', formatterParams: { precision: 0 } },
				{ title: '합계', field: 'iosum', hozAlign: 'right', width: 120, formatter: 'money', formatterParams: { precision: 0 },
                  mutatorData: (v, d) => Number(d.jsanamt || 0) + Number(d.jsanvat || 0), cssClass: 'fw-bold bg-light' },
				{ title: '비고', field: 'scustnm', width: 150 }
			]
		})
	}
	await loadInitData()
})
</script>

<style scoped>
.tabulator-full-height { width: 100% !important; background-color: #fff; border-bottom: 3px solid #005a9f !important; }
</style>
