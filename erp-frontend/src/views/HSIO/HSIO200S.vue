<!--
	=============================================================
	프로그램명	: 입고현황 (Purchase Receipt Status)
	작성일자	: 25.02.24
	작성자	    : AI Assistant
	설명        : [디자인정상화] 라벨 겹침 및 글자 일그러짐 해결, 고밀도 표준 적용
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container">
		<!-- 🚀 1. 상단 액션 바 -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-clipboard-data me-2 text-primary" style="font-size: 18px;"></i>
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

		<!-- 🔍 2. 검색 조건 영역 (라벨 폭 고정으로 일그러짐 방지) -->
		<div class="p-2 pb-0 flex-shrink-0">
			<div class="card border shadow-sm">
				<div class="card-body p-0">
					<table class="erp-table-full">
						<colgroup>
							<col style="width: 100px;"><col style="width: 23%">
							<col style="width: 100px;"><col style="width: 23%">
							<col style="width: 100px;"><col style="width: 24%">
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
								<th>입고일자</th>
								<td>
									<div class="d-flex align-items-center gap-1">
										<input v-model="searchForm.fromdt" type="date" class="form-control form-control-sm" />
										<span class="text-muted">~</span>
										<input v-model="searchForm.todt" type="date" class="form-control form-control-sm" />
									</div>
								</td>
								<th>거&nbsp;&nbsp;래&nbsp;&nbsp;처</th>
								<td>
									<div class="input-group input-group-sm">
										<input v-model="searchForm.custcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
										<input v-model="searchForm.custnm" type="text" class="form-control" placeholder="거래처 검색" />
										<button class="btn btn-outline-secondary px-2" @click="openHelp('CUST')"><i class="bi bi-search"></i></button>
									</div>
								</td>
							</tr>
							<tr>
								<th>입고유형</th>
								<td>
									<select v-model="searchForm.iotype" class="form-select form-select-sm">
										<option value="000">전체</option>
										<option v-for="opt in typeOptions" :key="opt.code" :value="opt.code">{{ opt.cdnm }}</option>
									</select>
								</td>
								<th>주문번호</th>
								<td>
									<div class="d-flex align-items-center gap-1">
										<input v-model="searchForm.ordym" type="month" class="form-control form-control-sm" style="width: 130px;" />
										<input v-model="searchForm.ordno" type="text" class="form-control form-control-sm" style="width: 60px;" placeholder="0000" />
									</div>
								</td>
								<th>영업담당</th>
								<td>
									<select v-model="searchForm.schuserid" class="form-select form-select-sm">
										<option value="">전체</option>
										<option v-for="emp in empOptions" :key="emp.userid" :value="emp.userid">{{ emp.usernm }}</option>
									</select>
								</td>
							</tr>
						</tbody>
					</table>
				</div>
			</div>
		</div>

		<!-- 📊 3. 그리드 영역 (Card 레이아웃 유지 + HSIP210S 구조적 해법 적용) -->
		<div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2">
			<div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white">
				<div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between" style="height: 40px;">
					<span class="fw-bold small text-dark d-flex align-items-center">
						<i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i> 입고 현황 상세 목록
					</span>
                    <span class="badge bg-primary-subtle text-primary border border-primary-subtle px-2" style="font-size: 11px;">단위: 원화(KRW)</span>
				</div>
                <!-- 🚀 card-body는 flex-grow-1을 유지하되, 내부 그리드는 절대 높이 100%만 가짐 -->
                <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column" style="min-height: 0;">
                  <div ref="mainGridRef" class="tabulator-full-height" />
                </div>
			</div>
		</div>
	</div>

	<Modal v-model:visible="modalVisible" :modalProps="modalProps" />
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
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
	ordym: '', ordno: '',
	schuserid: ''
})

const whOptions = ref<any[]>([])
const typeOptions = ref<any[]>([])
const empOptions = ref<any[]>([])
const mainGridRef = ref<HTMLDivElement | null>(null); let mainGrid: Tabulator | null = null

async function loadInitData() {
	try {
		const [resWh, resType, resEmp] = await Promise.all([
			api.get('/hs00/HS00_000S_STR', { params: { gubun: 'W0', cmpycd: authStore.cmpycd } }),
			api.get('/hs00/HS00_000S_STR', { params: { gubun: 'E0', cmpycd: authStore.cmpycd, gbncd: '120' } }),
			api.post('/ha00/HA00_00P_STR', { gubun: 'SD', cmpycd: authStore.cmpycd, gbncd: '', code: '', remark: '' })
		])
		whOptions.value = resWh.data.map((i: any) => ({ code: i.code || i.whcd, cdnm: i.cdnm || i.whnm }))
		typeOptions.value = resType.data.map((i: any) => ({ code: i.code, cdnm: i.cdnm }))
		empOptions.value = resEmp.data
	} catch (e) {}
}

async function fetchList() {
	try {
		const res = await api.post('/hsio/HSIO_200S_STR', {
			...searchForm,
			cmpycd: authStore.cmpycd,
			fromdt: searchForm.fromdt.replace(/-/g, ''),
			todt: searchForm.todt.replace(/-/g, ''),
			ordym: searchForm.ordym.replace(/-/g, ''),
			ordno: searchForm.ordno,
			schuserid: searchForm.schuserid
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
const openHelp = (type: string) => { /* 팝업 로직 */ }
const modalVisible = ref(false); const modalProps = reactive<any>({ title: '', path: '', onConfirm: () => {} })

onUnmounted(() => {
  if (mainGrid) mainGrid.destroy();
});

onMounted(async () => {
	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns',
            height: '100%',
			columnDefaults: {
				headerSort: false,
				headerHozAlign: 'center',
				hozAlign: 'right', // 🚀 HSIP210S 표준: 기본 우측 정렬
				vertAlign: 'middle', // 🚀 HSIP210S 표준: 기본 세로 중앙 정렬
				minWidth: 100
			},
			columns: [
				{ title: '입고번호', field: 'iono_full', width: 130, hozAlign: 'center', cssClass: 'text-primary',
                  formatter: (c) => { const d = c.getData(); return `${d.ioym || ''}-${d.iono || ''}` } },
				{ title: '거래처', field: 'custnm', minWidth: 180, widthGrow: 1, hozAlign: 'left' },
				{ title: '입고유형', field: 'iotypenm', width: 100, hozAlign: 'center' },
				{ title: '입고일자', field: 'ioymd', width: 100, hozAlign: 'center',
                  formatter: (c) => { const v = c.getValue(); return v && v.length === 8 ? `${v.substring(0,4)}-${v.substring(4,6)}-${v.substring(6,8)}` : v } },
				{ title: '품명', field: 'itemnm', minWidth: 200, widthGrow: 1, hozAlign: 'left' },
				{ title: '규격', field: 'itsize', width: 150, hozAlign: 'left' },
				{ title: '단위', field: 'unit', width: 60, hozAlign: 'center' },
				{ title: '수량', field: 'ioqty', width: 80, formatter: 'money', formatterParams: { precision: 0 } },
				{
                    title: '단가', field: 'price', width: 100,
                    formatter: (c) => {
                        const d = c.getData(); const amt = Number(d.jsanamt || 0); const qty = Number(d.ioqty || 1);
                        const val = qty === 0 ? 0 : (amt / qty);
                        return val.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
                    }
                },
				{ title: '공급가', field: 'jsanamt', width: 110, formatter: 'money', formatterParams: { precision: 0 } },
				{ title: '부가세', field: 'jsanvat', width: 100, formatter: 'money', formatterParams: { precision: 0 } },
				{ title: '합계', field: 'totamt', width: 120, cssClass: 'bg-light',
                  formatter: (c) => {
                      const d = c.getData();
                      const val = Number(d.jsanamt || 0) + Number(d.jsanvat || 0);
                      return val.toLocaleString();
                  }
                }
			]
		})
	}
	await loadInitData()
})
</script>

<style scoped>
.tabulator-full-height { width: 100% !important; background-color: #fff; border-bottom: 3px solid #005a9f !important; }
</style>
