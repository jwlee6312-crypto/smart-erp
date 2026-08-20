<!--
	=============================================================
	프로그램명	: 매입할인현황 (Purchase Discount Status)
	작성일자	: 25.02.24
	작성자	    : AI Assistant
	설명        : [최종완성] ERP 고밀도 표준 및 상하정중앙 정렬 완벽 보정
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container">
		<!-- 🚀 1. 상단 액션 바 (표준 버튼 배치 및 색상) -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-percent me-2 text-primary" style="font-size: 18px;"></i>
				구매정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				매입정산 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				<span class="text-primary fw-bolder">매입할인현황</span>
			</div>
			<div class="btn-group-erp d-flex gap-1">
				<button class="btn-erp btn-init" @click="initialize">초기화</button>
				<button class="btn-erp btn-search" @click="fetchList">조회</button>
				<button class="btn-erp btn-print" @click="print">인쇄</button>
			</div>
		</div>

		<!-- 🔍 2. 검색 조건 영역 (고밀도 표준 적용) -->
		<div class="p-2 pb-0">
			<div class="card border shadow-sm bg-light bg-opacity-50">
				<div class="card-body py-2 px-3">
					<table class="erp-table-form">
						<tbody>
							<tr>
								<th class="border-0 bg-transparent text-end pr-2" style="width: 1%; white-space: nowrap;">할인일자</th>
								<td class="border-0 bg-transparent" style="width: auto;">
									<div class="d-flex align-items-center gap-1">
										<input v-model="searchForm.fromdt" type="date" class="form-control form-control-sm" style="width: 140px;" />
										<span class="text-muted mx-1">~</span>
										<input v-model="searchForm.todt" type="date" class="form-control form-control-sm" style="width: 140px;" />
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
				<!-- ✅ 그리드 타이틀 상하 정중앙 배치 -->
				<div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between" style="height: 40px; min-height: 40px;">
					<span class="fw-bold small text-dark d-flex align-items-center">
						<i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i> 거래처별 할인 요약 내역
					</span>
				</div>
                <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column" style="min-height: 0;">
                  <div ref="mainGridRef" class="tabulator-full-height" />
                </div>
			</div>
		</div>

	</div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, onUnmounted } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import AppAlert from '@/components/AppAlert.vue'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

const now = new Date()
const searchForm = reactive({
  fromdt: new Date(now.getFullYear(), now.getMonth(), 1).toISOString().substring(0, 10),
  todt: now.toISOString().substring(0, 10)
})

const mainGridRef = ref<HTMLDivElement | null>(null); let mainGrid: Tabulator | null = null
const activeItemCount = ref(0)
const totals = reactive({ amt: 0, hal: 0 })

async function fetchList() {
  try {
    const res = await api.post('/hsio/HSIO_220S_STR', {
      cmpycd: authStore.cmpycd,
      fromdt: searchForm.fromdt.replace(/-/g, ''),
      todt: searchForm.todt.replace(/-/g, '')
    })
    const data = res.data || []
    mainGrid?.setData(data)
    activeItemCount.value = data.length
    totals.amt = data.reduce((acc: number, cur: any) => acc + (Number(cur.spyamt) || 0), 0)
    totals.hal = data.reduce((acc: number, cur: any) => acc + (Number(cur.halamt) || 0), 0)
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

function initialize() {
  resetForm(searchForm);
  searchForm.fromdt = new Date(now.getFullYear(), now.getMonth(), 1).toISOString().substring(0, 10);
  searchForm.todt = now.toISOString().substring(0, 10);
  mainGrid?.clearData(); totals.amt = 0; totals.hal = 0; activeItemCount.value = 0;
}

const print = () => { vAlert('인쇄 기능을 준비 중입니다.') }
const formatNumber = (val: any) => Number(val || 0).toLocaleString()

onUnmounted(() => {
  if (mainGrid) mainGrid.destroy();
});

onMounted(() => {
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
				{ title: '거래처', field: 'custnm', minWidth: 200, widthGrow: 2, cssClass: 'fw-bold text-dark' },
				{ title: '품목수', field: 'itmcnt', hozAlign: 'right', width: 200, formatter: 'money', formatterParams: { precision: 0 } },
				{ title: '공급가', field: 'spyamt', hozAlign: 'right', width: 200, formatter: 'money', formatterParams: { precision: 0 } },
				{ title: '할인금액', field: 'halamt', hozAlign: 'right', width: 200, formatter: 'money', formatterParams: { precision: 0 }, cssClass: 'text-danger fw-bold' }
			]
		})
	}
    fetchList()
})
</script>

<style scoped>
.tabulator-full-height { width: 100% !important; background-color: #fff; border-bottom: 3px solid #005a9f !important; }
</style>
