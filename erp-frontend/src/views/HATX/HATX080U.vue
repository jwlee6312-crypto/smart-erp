<!--	=============================================================
	프로그램명 : 전자세금계산서발행
	작성일자	: 2025.02.24
	작성자    : AI Assistant
	설명        : 매출 세금계산서의 국세청 전송 상태 조회 및 전자발행/전송 처리
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container">
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-send-check me-2 text-primary" style="font-size: 18px;"></i>
				재무관리<i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				<span class="text-primary fw-bolder">전자세금계산서발행(HATX080U)</span>
			</div>
			<div class="btn-group-erp d-flex gap-1">
				<button class="btn-erp btn-save" @click="handleBatchAction('U0')">
					<i class="bi bi-send"></i> {{ searchForm.cfmyn === 'N' ? '전송처리' : '전송취소' }}
				</button>
				<button v-if="searchForm.cfmyn === 'Y'" class="btn-erp btn-danger" @click="handleBatchAction('D0')">
					<i class="bi bi-trash"></i> 폐기처리
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
					<div class="d-flex align-items-center flex-wrap gap-3 small">
						<div class="d-flex align-items-center">
							<span class="erp-label"><i class="bi bi-dot"></i>사업장</span>
							<select v-model="searchForm.taxunit" class="form-select form-select-sm" style="width: 150px;" @change="search">
								<option v-for="opt in taxUnitOptions" :key="opt.code" :value="opt.code">{{ opt.name }}</option>
							</select>
						</div>
						<div class="d-flex align-items-center">
							<span class="erp-label"><i class="bi bi-dot"></i>발행부서</span>
							<div class="input-group input-group-sm" style="width: 250px;">
								<input v-model="searchForm.deptcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
								<input v-model="searchForm.deptnm" type="text" class="form-control" @keydown.enter="openHelp('DEPT')" placeholder="부서 선택" />
								<button class="btn btn-outline-secondary px-2" @click="openHelp('DEPT')"><i class="bi bi-search"></i></button>
							</div>
						</div>
						<div class="d-flex align-items-center">
							<span class="erp-label"><i class="bi bi-dot"></i>발행일</span>
							<div class="d-flex align-items-center gap-1">
								<input v-model="searchForm.fromdt" type="date" class="form-control form-control-sm" style="width: 135px;" />
								<span>~</span>
								<input v-model="searchForm.todt" type="date" class="form-control form-control-sm" style="width: 135px;" />
							</div>
						</div>
						<div class="d-flex align-items-center">
							<span class="erp-label"><i class="bi bi-dot"></i>조회상태</span>
							<select v-model="searchForm.cfmyn" class="form-select form-select-sm" style="width: 100px;" @change="search">
								<option value="N">미전송</option>
								<option value="Y">전송</option>
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
import Modal from '@/components/Modal.vue'
import type { ModalProps } from '@/types/modal'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()

const today = new Date().toISOString().slice(0, 10)
const firstDay = today.slice(0, 8) + '01'

const taxUnitOptions = ref<any[]>([])

const searchForm = reactive({
	taxunit: '',
	deptcd: authStore.deptcd || '',
	deptnm: authStore.deptnm || '',
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

const search = async () => {
	if (!searchForm.taxunit) return vAlertError('사업장을 선택하세요.')
	if (!searchForm.deptcd) return vAlertError('발행부서를 선택하세요.')

	// 2011-07-01 이후 로직 반영
	const frYmd = searchForm.fromdt.replace(/-/g, '')
	const toYmd = searchForm.todt.replace(/-/g, '')
	if (frYmd < '20110701' || toYmd < '20110701') {
		return vAlertError('2011년 07월 01일 이후 발행계산서만 조회 가능합니다.')
	}

	try {
		const res = await api.post('/hatx/HATX_080U_STR', {
			actkind: 'S0',
			cmpycd: authStore.cmpycd,
			taxunit: searchForm.taxunit,
			deptcd: searchForm.deptcd,
			fromdt: frYmd,
			todt: toYmd,
			cfmyn: searchForm.cfmyn,
			taxkind: '200' // 매출
		})

		const data = (res.data || []).map((row: any) => ({
			pubymd: row.pubymd,
			custnm: row.custnm,
			sendstatus: row.sendstatus,
			documentstatus: row.documentstatus,
			nts_status: row.filler2 === '1' ? '전송' : '미전송',
			supyamt: Number(row.supyamt || 0),
			vatamt: Number(row.vatamt || 0),
			totamt: Number(row.totamt || 0),
			descnm: row.descnm,
			damdang: row.damdang,
			telno: row.telno,
			email: row.email,
			taxym: row.taxym,
			taxno: row.taxno,
			_selected: searchForm.cfmyn === 'N'
		}))

		mainGrid?.setData(data)
		if (searchForm.cfmyn === 'N') {
			mainGrid?.selectRow()
		}
		vAlert('조회되었습니다.')
	} catch (e) { vAlertError('조회 중 오류 발생') }
}

const handleBatchAction = async (gbn: string) => {
	const selectedRows = mainGrid?.getSelectedData() || []
	if (selectedRows.length === 0) return vAlertError('처리할 대상을 선택해 주십시오.')

	let msg = ''
	if (gbn === 'U0') {
		msg = searchForm.cfmyn === 'N' ? '선택한 세금계산서를 전송 하시겠습니까?' : '선택한 세금계산서를 전송취소 하시겠습니까?'
	} else {
		msg = '선택한 세금계산서를 폐기 하시겠습니까?'
	}

	if (!confirm(msg)) return

	try {
		for (const row of selectedRows) {
			const res = await api.post('/hatx/HATX_080U_STR', {
				actkind: gbn,
				cmpycd: authStore.cmpycd,
				taxunit: searchForm.taxunit,
				deptcd: searchForm.deptcd,
				fromdt: searchForm.fromdt.replace(/-/g, ''),
				todt: searchForm.todt.replace(/-/g, ''),
				cfmyn: searchForm.cfmyn,
				taxkind: '200',
				taxym: row.taxym,
				taxno: row.taxno,
				elcyn: 'N', // ASP: 'N'으로 하드코딩됨
				userid: authStore.userid
			})

			// res.data[0].col0 (원래 res.data[0].COL0이었을 수 있음)
			if (res.data && res.data[0] && (res.data[0].col0 === 'N' || res.data[0].result === 'N')) {
				vAlertError(res.data[0].col2 || res.data[0].msg || '처리 중 오류가 발생했습니다.')
				break
			}
		}

		vAlert('정상적으로 처리되었습니다.')
		search()
	} catch (e) { vAlertError('처리 중 오류 발생') }
}

// --- 팝업 설정 ---
const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function openHelp(type: string) {
	if (type === 'DEPT') {
		Object.assign(modalProps, {
			title: '부서 선택', path: '/ha00/HA00_00P_STR',
			data: { gubun: 'D0', cmpycd: authStore.cmpycd, search: searchForm.deptnm },
			columns: [{ title: '코드', field: 'deptcd', width: 80 }, { title: '부서명', field: 'deptnm', width: 180 }],
			onConfirm: (d: any) => { searchForm.deptcd = d.deptcd; searchForm.deptnm = d.deptnm; search() }
		})
	}
	modalVisible.value = true
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
					title: "발행일", field: "pubymd", width: 100, hozAlign: "center",
					formatter: (cell) => { const v = cell.getValue(); return v ? `${v.slice(0,4)}-${v.slice(4,6)}-${v.slice(6,8)}` : '' }
				},
				{ title: "상호", field: "custnm", width: 250 },
				{ title: "전송상태", field: "sendstatus", width: 100, hozAlign: "center" },
				{ title: "문서상태", field: "documentstatus", width: 100, hozAlign: "center" },
				{ title: "국세청", field: "nts_status", width: 100, hozAlign: "center" },
				{ title: "공급가", field: "supyamt", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
				{ title: "부가세", field: "vatamt", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
				{ title: "합계", field: "totamt", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "fw-bold" },
				{ title: "적요", field: "descnm", width: 150 },
				{ title: "담당자", field: "damdang", width: 100 },
				{ title: "연락처", field: "telno", width: 120 },
				{ title: "이메일", field: "email", width: 150 }
			]
		})
	}
})
</script>

<style scoped>
.erp-label { min-width: 80px; font-weight: 500; font-size: 13px; }
:deep(.tabulator-cell) { border-right: 1px solid #dee2e6 !important; font-size: 11px; }
:deep(.tabulator-header .tabulator-col) { border-right: 1px solid #dee2e6 !important; background-color: #f8f9fa !important; font-size: 11px; }
:deep(.tabulator-selected) { background-color: #e7f1ff !important; }
</style>
