<!--
	=============================================================
	프로그램명	: 전표확정작업 (HASL020U)
	작성일자	: 2025.02.24
	설명        : 미확정 전표를 조회하여 회계일자 지정 및 확정 처리
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container">
		<!-- 🚀 상단 액션 바 -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-check-all me-2 text-primary" style="font-size: 18px;"></i>
				전표관리 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				<span class="text-primary fw-bolder">전표확정작업 (HASL020U)</span>
			</div>
			<div class="btn-group-erp d-flex gap-1">
				<button class="btn-erp btn-init" @click="initialize">초기화</button>
				<button class="btn-erp btn-search" @click="fetchSlips">조회</button>
				<button class="btn-erp btn-save" @click="save">저장</button>
			</div>
		</div>

		<!-- 🔍 검색 조건 영역 -->
		<div class="p-2 pb-0 flex-shrink-0">
			<div class="card border shadow-sm overflow-hidden bg-light">
				<table class="erp-table-full">
					<colgroup>
						<col style="width: 25%;" />
						<col style="width: 25%;" />
						<col style="width: 25%;" />
						<col style="width: 25%;" />
					</colgroup>
					<tbody>
						<tr>
							<td>
								<div class="d-flex align-items-center px-2">
									<span class="erp-label">발행일자</span>
									<div class="d-flex align-items-center gap-1 flex-grow-1">
										<input v-model="searchForm.fromdt" type="date" class="form-control" />
										<span class="text-muted">~</span>
										<input v-model="searchForm.todt" type="date" class="form-control" />
									</div>
								</div>
							</td>
							<td>
								<div class="d-flex align-items-center px-2">
									<span class="erp-label">발행부서</span>
									<div class="input-group flex-nowrap">
										<input v-model="searchForm.deptcd" type="text" class="form-control text-center bg-white" style="max-width: 60px;" readonly />
										<input v-model="searchForm.deptnm" type="text" class="form-control" placeholder="부서 선택" @keydown.enter="openHelp('SEARCH_DEPT')" />
										<button class="btn btn-outline-secondary px-2" @click="openHelp('SEARCH_DEPT')"><i class="bi bi-search"></i></button>
									</div>
								</div>
							</td>
							<td>
								<div class="d-flex align-items-center px-2">
									<span class="erp-label">전표종류</span>
									<select v-model="searchForm.slipkind" class="form-select">
										<option value="000">전체</option>
										<option v-for="item in slipKindOptions" :key="item.codecd" :value="item.codecd">{{ item.codenm }}</option>
									</select>
								</div>
							</td>
							<td>
								<div class="d-flex align-items-center px-2">
									<span class="erp-label">확정여부</span>
									<select v-model="searchForm.slipyn" class="form-select">
										<option value="N">미확정</option>
										<option value="Y">확정</option>
									</select>
								</div>
							</td>
						</tr>
					</tbody>
				</table>
			</div>
		</div>

		<!-- 📝 마스터 정보 입력 영역 -->
		<div class="p-2 pb-0 flex-shrink-0">
			<div class="card border shadow-sm bg-white overflow-hidden">
				<div class="card-header py-1 px-2 bg-light border-bottom d-flex justify-content-between align-items-center">
					<span class="fw-bold small text-secondary">선택 전표 상세</span>
					<button v-if="masterForm.slipno" class="btn btn-erp btn-init" style="height: 22px; padding: 0 8px;" @click="goSlipDetail">
						<i class="bi bi-link-45deg"></i> 전표보기
					</button>
				</div>
				<table class="erp-table-full">
					<colgroup>
						<col style="width: 100px;" /><col />
						<col style="width: 100px;" /><col />
						<col style="width: 100px;" /><col />
					</colgroup>
					<tbody>
						<tr>
							<th>전표번호</th>
							<td>
								<div class="d-flex align-items-center gap-1">
									<input v-model="masterForm.slipymd" type="text" class="form-control bg-light" readonly />
									<input v-model="masterForm.slipno" type="text" class="form-control text-center bg-light" style="width: 60px;" readonly />
								</div>
							</td>
							<th>발 행 인</th>
							<td><input v-model="masterForm.empnm" type="text" class="form-control bg-light" readonly /></td>
							<th class="text-primary">회계일자</th>
							<td>
								<input v-model="masterForm.acctymd" type="date" class="form-control border-primary shadow-sm" />
							</td>
						</tr>
						<tr>
							<th>차변금액</th>
							<td><input :value="formatMoney(masterForm.dbamt)" type="text" class="form-control text-end bg-light" readonly /></td>
							<th>대변금액</th>
							<td><input :value="formatMoney(masterForm.cramt)" type="text" class="form-control text-end bg-light" readonly /></td>
							<th>확정여부</th>
							<td>
								<div class="form-check form-switch d-flex align-items-center h-100 ps-5">
									<input v-model="masterForm.cofmyn" class="form-check-input" type="checkbox" id="cofmSwitch" />
									<label class="form-check-label ms-2" for="cofmSwitch">{{ masterForm.cofmyn ? '확정' : '미확정' }}</label>
								</div>
							</td>
						</tr>
						<tr>
							<th>적 요</th>
							<td colspan="5">
								<input v-model="masterForm.remark" type="text" class="form-control bg-light" readonly />
							</td>
						</tr>
					</tbody>
				</table>
			</div>
		</div>

		<!-- 📊 그리드 영역 -->
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

const authStore = useAuthStore()
const router = useRouter()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

const now = new Date()
const firstDay = new Date(now.getFullYear(), now.getMonth(), 1).toISOString().substring(0, 10)
const today = now.toISOString().substring(0, 10)

const searchForm = reactive({
	fromdt: firstDay,
	todt: today,
	deptcd: '',
	deptnm: '',
	slipkind: '000',
	slipyn: 'N'
})

const masterForm = reactive({
	slipymd: '',
	slipno: '',
	empnm: '',
	acctymd: today,
	dbamt: 0,
	cramt: 0,
	remark: '',
	cofmyn: false,
	deptcd: '',
	deptnm: '',
	clsymd: '00000000'
})

const slipKindOptions = ref<any[]>([])
const mainGridRef = ref<HTMLDivElement | null>(null)
let mainGrid: Tabulator | null = null

const formatMoney = (val: any) => Number(val || 0).toLocaleString()

const loadInitData = async () => {
	try {
		const resCls = await api.post('/ha00/HA00_010S_STR', { gubun: 'P1', cmpycd: authStore.cmpycd })
		if (resCls.data && resCls.data.length > 0) {
			masterForm.clsymd = resCls.data[0].clsymd || '00000000'
		}

		const resKind = await api.post('/ha00/HA00_00P_STR', { gubun: 'E0', gbncd: '180' })
		slipKindOptions.value = resKind.data || []
	} catch (e) { console.error('초기 데이터 로드 실패') }
}

const fetchSlips = async () => {
	try {
		const res = await api.post('/hasl/HASL_020U_STR', {
			actkind: 'S0',
			cmpycd: authStore.cmpycd,
			slipymd: searchForm.fromdt.replace(/-/g, ''),
			acctymd: searchForm.todt.replace(/-/g, ''),
			deptcd: searchForm.deptcd,
			slipkind: searchForm.slipkind,
			slipyn: searchForm.slipyn
		})
		const data = (res.data || []).map((row: any) => {
			return Object.fromEntries(Object.entries(row).map(([k, v]) => [k.toLowerCase(), v]))
		})
		mainGrid?.setData(data)
		vAlert('조회되었습니다.')
	} catch (e) { vAlertError('조회 실패') }
}

const save = async () => {
	if (!masterForm.slipno) return vAlert('처리할 전표를 선택하십시오.')
	const acctYmdRaw = masterForm.acctymd.replace(/-/g, '')
	if (acctYmdRaw <= masterForm.clsymd) {
		return vAlert('회계 마감된 일자입니다.')
	}
	if (searchForm.slipyn === 'N' && masterForm.cofmyn) {
		if (masterForm.dbamt !== masterForm.cramt) {
			return vAlert('차대 금액이 일치하지 않습니다.')
		}
	}
	try {
		await api.post('/hasl/HASL_020U_STR', {
			actkind: 'U0',
			cmpycd: authStore.cmpycd,
			slipymd: masterForm.slipymd.replace(/-/g, ''),
			slipno: masterForm.slipno,
			acctymd: acctYmdRaw,
			deptcd: masterForm.deptcd,
			cofmyn: masterForm.cofmyn ? 'Y' : 'N',
			slipkind: searchForm.slipkind,
			slipyn: searchForm.slipyn,
			userid: authStore.userid
		})
		vAlert('저장되었습니다.')
		fetchSlips()
	} catch (e) { vAlertError('저장 실패') }
}

const initialize = () => {
	resetForm(searchForm)
	searchForm.fromdt = firstDay
	searchForm.todt = today
	searchForm.slipkind = '000'
	searchForm.slipyn = 'N'

	masterForm.slipymd = ''
	masterForm.slipno = ''
	masterForm.empnm = ''
	masterForm.acctymd = today
	masterForm.dbamt = 0
	masterForm.cramt = 0
	masterForm.remark = ''
	masterForm.cofmyn = false
	masterForm.deptcd = ''
	masterForm.deptnm = ''

	mainGrid?.clearData()
}

const goSlipDetail = () => {
	if (!masterForm.slipymd || !masterForm.slipno) return
	router.push({
		path: '/HASL/HASL010U',
		query: { deptcd: masterForm.deptcd, slipymd: (masterForm.slipymd || '').replace(/-/g, ''), slipno: masterForm.slipno }
	})
}

const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function openHelp(type: string) {
	if (type === 'SEARCH_DEPT') {
		Object.assign(modalProps, {
			title: '부서 선택', path: '/ha00/HA00_00P_STR', defaultField: 'deptnm',
			data: { gubun: 'D0', cmpycd: authStore.cmpycd, code: searchForm.deptnm },
			columns: [{ title: '코드', field: 'deptcd', width: 80 }, { title: '부서명', field: 'deptnm', width: 180 }],
			onConfirm: (d: any) => { searchForm.deptcd = d.deptcd; searchForm.deptnm = d.deptnm }
		})
	}
	modalVisible.value = true
}

onMounted(() => {
	loadInitData()
	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns', height: '100%', selectable: 1,
			columnDefaults: { headerSort: false, headerHozAlign: "center", hozAlign: "center", vertAlign: "middle" },
			columns: [
				{
					title: "전표번호", field: "slip_no_full", width: 150,
					formatter: (cell) => {
						const d = cell.getData()
						if (!d.slipymd || !d.slipno) return ''
						return `${d.slipymd}-${d.slipno}`
					}
				},
				{ title: "적요", field: "remark", hozAlign: "left", minWidth: 250 },
				{ title: "발행부서", field: "deptnm", width: 200 },
				{ title: "발행인", field: "empnm", width: 150 },
				{ title: "차변금액", field: "dbamt", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
				{ title: "대변금액", field: "cramt", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
				{
					title: "확정", field: "acctymd", width: 100,
					formatter: (cell) => {
						const val = cell.getValue()
						return val && val !== '00000000' ? '✔' : ''
					}
				}
			]
		})

		mainGrid.on("rowClick", (e, row) => {
			const d = row.getData()
			masterForm.slipno = d.slipno || ''
			masterForm.empnm = d.empnm || ''
			masterForm.dbamt = Number(d.dbamt || 0)
			masterForm.cramt = Number(d.cramt || 0)
			masterForm.remark = d.remark || ''
			masterForm.deptcd = d.deptcd || ''
			masterForm.deptnm = d.deptnm || ''
			if (d.slipymd && d.slipymd.length === 8) {
				masterForm.slipymd = `${d.slipymd.substring(0, 4)}-${d.slipymd.substring(4, 6)}-${d.slipymd.substring(6, 8)}`
			} else {
				masterForm.slipymd = d.slipymd || ''
			}
			if (d.acctymd && d.acctymd !== '00000000' && d.acctymd.length === 8) {
				masterForm.acctymd = `${d.acctymd.substring(0, 4)}-${d.acctymd.substring(4, 6)}-${d.acctymd.substring(6, 8)}`
				masterForm.cofmyn = true
			} else {
				masterForm.acctymd = today
				masterForm.cofmyn = false
			}
		})
	}
})
</script>

<style scoped></style>
