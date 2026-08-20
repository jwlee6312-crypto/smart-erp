<!--영업정보/입금관리/입금현황 [ERP 프리미엄 고밀도 표준 적용] -->
<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container">
		<!-- 🚀 1. 상단 액션 바 -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 shadow-sm sticky-top">
			<div class="fw-bold ps-3 text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-cash-coin me-2 text-primary" style="font-size: 18px;"></i>
				영업정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
				입금관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
				<span class="text-primary fw-bolder">입금현황 (HSIO400S)</span>
			</div>
			<div class="btn-group-erp pe-3">
				<button class="btn-erp btn-init" @click="initialize">초기화</button>
				<button class="btn-erp btn-search" @click="fetchList">조회</button>
				<button class="btn-erp btn-print" @click="printReport"><i class="bi bi-printer me-1"></i>인쇄</button>
			</div>
		</div>

		<!-- 💡 메인 컨텐츠 영역 (섹션 간 확실한 간격 확보: gap-3) -->
		<div class="flex-grow-1 overflow-hidden d-flex flex-column gap-3 p-3">

			<!-- 🔍 2. 검색 바 섹션 (3열 배치로 균형 확보) -->
			<div class="card border-0 shadow-sm flex-shrink-0">
				<div class="card-body p-0 bg-white">
					<table class="erp-table-full border-0">
						<colgroup>
							<col style="width: 100px;" /><col />
							<col style="width: 100px;" /><col />
							<col style="width: 100px;" /><col />
						</colgroup>
						<tbody>
							<tr>
								<th class="required">입금부서</th>
								<td>
									<div class="input-group input-group-sm flex-nowrap">
										<input v-model="searchForm.deptcd" type="text" class="form-control bg-light text-center fw-bold" style="max-width: 60px;" readonly />
										<input v-model="searchForm.deptnm" type="text" class="form-control" placeholder="부서 선택" @keyup.enter="openHelp('DEPT')" />
										<button class="btn btn-outline-secondary px-2" @click="openHelp('DEPT')"><i class="bi bi-search"></i></button>
									</div>
								</td>
								<th class="required">입금일자</th>
								<td>
									<div class="d-flex align-items-center gap-1">
										<input v-model="searchForm.fromdt" type="date" class="form-control" />
										<span class="text-muted">~</span>
										<input v-model="searchForm.todt" type="date" class="form-control" />
									</div>
								</td>
								<th>영업사원</th>
								<td>
									<select v-model="searchForm.salsemp" class="form-select">
										<option value="000">전체 사원</option>
										<option v-for="item in empOptions" :key="item.userid" :value="item.userid">{{ item.usernm }}</option>
									</select>
								</td>
							</tr>
							<tr>
								<th class="border-bottom-0">거 래 처</th>
								<td colspan="3" class="border-bottom-0">
									<div class="d-flex align-items-center gap-1">
										<div class="input-group input-group-sm flex-nowrap" style="width: 250px;">
											<input v-model="searchForm.custcdfr" type="text" class="form-control bg-light text-center" style="max-width: 60px;" readonly />
											<input v-model="searchForm.custnmfr" type="text" class="form-control" placeholder="거래처(시작)" @keyup.enter="openHelp('CUSTFR')" />
											<button class="btn btn-outline-secondary px-2" @click="openHelp('CUSTFR')"><i class="bi bi-search"></i></button>
										</div>
										<span class="text-muted mx-1">~</span>
										<div class="input-group input-group-sm flex-nowrap" style="width: 250px;">
											<input v-model="searchForm.custcdto" type="text" class="form-control bg-light text-center" style="max-width: 60px;" readonly />
											<input v-model="searchForm.custnmto" type="text" class="form-control" placeholder="거래처(종료)" @keyup.enter="openHelp('CUSTTO')" />
											<button class="btn btn-outline-secondary px-2" @click="openHelp('CUSTTO')"><i class="bi bi-search"></i></button>
										</div>
									</div>
								</td>
								<td colspan="2" class="border-bottom-0 bg-light-subtle text-end px-3">
									<span class="small text-muted"><i class="bi bi-info-circle me-1"></i> 거래처를 지정하지 않으면 전체를 조회합니다.</span>
								</td>
							</tr>
						</tbody>
					</table>
				</div>
			</div>

			<!-- 📊 3. 하단 그리드 영역 (전체 폭 복구) -->
			<div class="card border-0 shadow-sm flex-grow-1 overflow-hidden">
				<div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
					<span class="fw-bold small text-dark"><i class="bi bi-table me-2 text-secondary"></i>입금 내역 상세 현황</span>
					<div class="d-flex gap-3 small">
						<span class="text-muted">조회 건수: <strong class="text-primary">{{ listCount }}</strong> 건</span>
						<span class="text-muted text-primary fw-bold">합계: {{ formatMoney(totalSum) }}</span>
					</div>
				</div>
				<div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column" style="min-height: 0;">
                    <div ref="mainGridRef" class="tabulator-full-height" />
				</div>
			</div>
		</div>
		<Modal v-model:visible="modalVisible" :modalProps="modalProps" />
	</div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, nextTick, onUnmounted } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { getDate } from '@/composables/useDate'
import type { ModalProps } from '@/types/modal'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { firstDay, today } = getDate()

const searchForm = reactive({
	deptcd: authStore.deptcd, deptnm: authStore.deptnm,
	fromdt: firstDay, todt: today, salsemp: '000',
	custcdfr: '', custnmfr: '', custcdto: '', custnmto: ''
})

const empOptions = ref<any[]>([])
const listCount = ref(0); const totalSum = ref(0)
const mainGridRef = ref<HTMLDivElement | null>(null); let mainGrid: Tabulator | null = null

const modalVisible = ref(false);
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

async function fetchList() {
	try {
		const res = await api.post('/comm/executeHS00_000S_STR', {
			cmpycd: authStore.cmpycd,
			deptcd: searchForm.deptcd,
			fromdt: searchForm.fromdt.replace(/-/g, ''),
			todt: searchForm.todt.replace(/-/g, ''),
			custcdfr: searchForm.custcdfr,
			custcdto: searchForm.custcdto,
			salsemp: searchForm.salsemp
		})
		mainGrid?.setData(res.data || [])
		listCount.value = res.data?.length || 0
		totalSum.value = (res.data || []).reduce((acc: number, cur: any) => acc + (Number(cur.amttot) || 0), 0)
		vAlert('입금현황 조회가 완료되었습니다.')
	} catch (e) { vAlertError('조회 실패') }
}

function initialize() {
	resetForm(searchForm)
	searchForm.fromdt = firstDay; searchForm.todt = today; searchForm.salsemp = '000'
	searchForm.deptcd = authStore.deptcd; searchForm.deptnm = authStore.deptnm
	mainGrid?.clearData(); listCount.value = 0; totalSum.value = 0
}

function openHelp(type: string) {
	if (type === 'DEPT') {
		Object.assign(modalProps, {
			title: '부서 선택', path: '/ha00/HA00_00P_STR', defaultField: 'deptnm',
			data: { gubun: 'D0', cmpycd: authStore.cmpycd },
			columns: [{ title: '코드', field: 'deptcd', width: 80 }, { title: '부서명', field: 'deptnm', width: 180 }],
			onConfirm: (data: any) => { searchForm.deptcd = data.deptcd; searchForm.deptnm = data.deptnm }
		})
	} else if (type.startsWith('CUST')) {
		Object.assign(modalProps, {
			title: '거래처 선택', path: '/ha00/HA00_00P_STR', defaultField: 'custnm',
			data: { gubun: 'C4', cmpycd: authStore.cmpycd },
			columns: [{ title: '코드', field: 'custcd', width: 80 }, { title: '거래처명', field: 'custnm', width: 200 }],
			onConfirm: (data: any) => {
				if (type === 'CUSTFR') { searchForm.custcdfr = data.custcd; searchForm.custnmfr = data.custnm }
				else { searchForm.custcdto = data.custcd; searchForm.custnmto = data.custnm }
			}
		})
	}
	modalVisible.value = true
}

function formatMoney(val: any) { return new Intl.NumberFormat().format(Number(val) || 0) }
function printReport() { vAlert('인쇄 기능을 준비 중입니다.') }

onUnmounted(() => {
  if (mainGrid) mainGrid.destroy();
});

onMounted(async () => {
	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns', height: '100%',
			placeholder: '조회된 데이터가 없습니다.',
			columnDefaults: {
				headerHozAlign: 'center',
				headerSort: false,
				hozAlign: 'right', // 🚀 기본값 우측 정렬
				vertAlign: 'middle'
			},
			columns: [
				{ title: '거래처 명칭', field: 'custnm', minWidth: 200, widthGrow: 2, cssClass: 'fw-bold' },
				{ title: '현금', field: 'cashamt', hozAlign: 'right', formatter: "money", formatterParams: { precision: 0 }, width: 110 },
				{ title: '예금', field: 'bankamt', hozAlign: 'right', formatter: "money", formatterParams: { precision: 0 }, width: 110 },
				{ title: '카드', field: 'cardamt', hozAlign: 'right', formatter: "money", formatterParams: { precision: 0 }, width: 110 },
				{ title: '어음', field: 'billamt', hozAlign: 'right', formatter: "money", formatterParams: { precision: 0 }, width: 110 },
				{ title: '대체', field: 'sangamt', hozAlign: 'right', formatter: "money", formatterParams: { precision: 0 }, width: 110 },
				{ title: '기타', field: 'etcamt', hozAlign: 'right', formatter: "money", formatterParams: { precision: 0 }, width: 110 },
				{ title: '합계', field: 'amttot', hozAlign: 'right', formatter: "money", formatterParams: { precision: 0 }, width: 130, cssClass: 'fw-bold text-primary bg-light-subtle' }
			]
		})
	}
	api.get('/ha00/HA00_00P_STR', { params: { gubun: 'SD', cmpycd: authStore.cmpycd, gbncd: '', code: '', remark: '' } }).then(r => {
		if (r.data) empOptions.value = r.data.map((i: any) => ({ userid: String(i.userid || i.userid || Object.values(i)[0]).trim(), usernm: String(i.usernm || i.usernm || Object.values(i)[1]).trim() }))
	})
})
</script>

<style scoped>
.tabulator-full-height { width: 100% !important; background-color: #fff; border-bottom: 3px solid #005a9f !important; }
</style>
