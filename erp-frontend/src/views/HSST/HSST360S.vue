<!--
	=============================================================
	프로그램명	: 거래처/품목별 매출 총이익 명세서 (HSST360S)
	작성일자	: 2025.02.24
	설명        : 거래처별 품목 매출액, 원가, 이익 및 이익률 현황 조회 (HSOD100U 디자인 표준 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-person-badge-fill me-2 text-primary" style="font-size: 18px;"></i>
        매출분석 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        통계현황 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">거래처/품목별 매출 총이익 명세서 (HSST360S)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-print" @click="print">인쇄</button>
        <button class="btn-erp btn-excel" @click="excel">엑셀</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">
      <!-- 🅰️ 조회 조건 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
                <col style="width: 10%" /><col style="width: 25%" />
                <col style="width: 10%" /><col style="width: 20%" />
                <col style="width: 10%" /><col style="width: 25%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="required text-center bg-light">매출부서</th>
                <td>
                  <div class="input-group input-group-sm">
                    <input v-model="searchForm.deptnm" class="form-control fw-bold text-primary" readonly />
                    <button class="btn btn-outline-secondary" @click="openHelp('DEPT')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="required text-center bg-light">조회연월</th>
                <td>
                  <div class="d-flex align-items-center gap-1">
                    <select v-model="searchForm.yy" class="form-select form-select-sm" style="width: 100px;">
                      <option v-for="y in yearOptions" :key="y" :value="y">{{ y }}년</option>
                    </select>
                    <select v-model="searchForm.mm" class="form-select form-select-sm" style="width: 80px;">
                      <option v-for="m in monthOptions" :key="m" :value="m">{{ m }}월</option>
                    </select>
                  </div>
                </td>
                <th class="required text-center bg-light">거 래 처</th>
                <td>
                  <div class="input-group input-group-sm">
                    <input v-model="searchForm.custnm" class="form-control" placeholder="거래처 선택" readonly />
                    <button class="btn btn-outline-secondary" @click="openHelp('CUST')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 🅱️ 데이터 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>거래처/품목별 매출 상세 현황</span>
          <div class="small text-muted">품목명을 클릭하면 품목별 매출 상세 내역(HSST350S)으로 이동합니다.</div>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="mainGridRef" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { useRouter } from 'vue-router'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import { useCommonHelp } from '@/composables/useCommonHelp'
import { getDate } from '@/composables/useDate'

const authStore = useAuthStore()
const router = useRouter()
const { today } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps } = useCommonHelp()

const now = new Date();
const currentyy = String(now.getFullYear());
const currentmm = String(now.getMonth() + 1).padStart(2, '0');

const searchForm = reactive<any>({
	deptcd: authStore.deptcd,
	deptnm: authStore.deptnm,
	custcd: '',
	custnm: '',
	yy: currentyy,
    mm: currentmm
})

const yearOptions = ref<string[]>(Array.from({ length: 5 }, (_, i) => String(now.getFullYear() - i)));
const monthOptions = ref<string[]>(Array.from({ length: 12 }, (_, i) => String(i + 1).padStart(2, '0')));

const mainGridRef = ref<HTMLDivElement | null>(null);
let mainGrid: Tabulator | null = null

const search = async () => {
	if (!searchForm.deptcd) return vAlertError('부서를 선택하세요.');
	if (!searchForm.custcd) return vAlertError('거래처를 선택하세요.');

	try {
		const res = await api.post('/hsst/HSST_360S_STR', {
			...searchForm,
			cmpycd: authStore.cmpycd,
			ym: searchForm.yy + searchForm.mm
		})
		const data = (res.data || []).map((i: any) => Object.fromEntries(Object.entries(i).map(([k, v]) => [k.toLowerCase(), v])));
		mainGrid?.setData(data)
		vAlert('조회되었습니다.')
	} catch (e) { vAlertError('조회 실패') }
}

const initialize = () => {
	resetForm(searchForm);
	searchForm.deptcd = authStore.deptcd; searchForm.deptnm = authStore.deptnm;
	searchForm.yy = currentyy; searchForm.mm = currentmm;
	mainGrid?.clearData();
}

const excel = () => mainGrid?.download("xlsx", "거래처_품목별매출총이익.xlsx")
const print = () => vAlert('인쇄 기능을 준비 중입니다.')

function openHelp(type: string) {
    if (type === 'DEPT') {
        Object.assign(modalProps, {
            title: '부서 선택', path: '/ha00/HA00_00P_STR',
            columns: [{ title: '코드', field: 'deptcd', width: 80 }, { title: '부서명', field: 'deptnm', width: 180 }],
            data: { gubun: 'D0', cmpycd: authStore.cmpycd },
            onConfirm: (d: any) => { searchForm.deptcd = d.deptcd; searchForm.deptnm = d.deptnm; search(); }
        })
    } else if (type === 'CUST') {
        Object.assign(modalProps, {
            title: '거래처 선택', path: '/ha00/HA00_00P_STR',
            columns: [{ title: '코드', field: 'custcd', width: 100 }, { title: '거래처명', field: 'custnm', width: 200 }],
            data: { gubun: 'C4', cmpycd: authStore.cmpycd },
            onConfirm: (d: any) => { searchForm.custcd = d.custcd; searchForm.custnm = d.custnm; search(); }
        })
    }
	modalVisible.value = true
}

onMounted(() => {
	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns', height: '100%',
			columnDefaults: { headerSort: false, headerHozAlign: "center", hozAlign: "center", vertAlign: "middle" },
			columns: [
                { title: "No", formatter: "rownum", width: 40, hozAlign: "center" },
				{
					title: "품목명", field: "itemnm", minWidth: 200, widthGrow: 2, hozAlign: "left", cssClass: "fw-bold text-primary cursor-pointer border-end", frozen: true,
					cellClick: (e, cell) => {
						const d = cell.getData();
						router.push({ path: '/HSST/HSST350S', query: { deptcd: searchForm.deptcd, yy: searchForm.yy, mm: searchForm.mm, itemcd: d.itemcd } });
					}
				},
				{
					title: "당월 실적 (Current Month)",
					columns: [
						{ title: "매출액", field: "salsamt", hozAlign: "right", width: 150, formatter: "money", formatterParams: { precision: 0 }, bottomCalc: "sum", bottomCalcFormatter: "money" },
						{ title: "매출원가", field: "salscost", hozAlign: "right", width: 150, formatter: "money", formatterParams: { precision: 0 }, bottomCalc: "sum", bottomCalcFormatter: "money" },
						{
                            title: "매총이익", field: "salsprof", hozAlign: "right", width: 150, formatter: "money", formatterParams: { precision: 0 },
                            mutatorData: (v,d) => (Number(d.salsamt)||0) - (Number(d.salscost)||0),
                            bottomCalc: "sum", bottomCalcFormatter: "money", cssClass: "bg-light-gray"
                        },
						{ title: "이익률(%)", field: "prof_rate", hozAlign: "right", width: 120, formatter: (c) => Number(c.getValue()||0).toFixed(1) + '%' }
					]
				},
				{
					title: "누계 실적 (Cumulative)",
					columns: [
						{ title: "매출액", field: "salsamt_t", hozAlign: "right", width: 150, formatter: "money", formatterParams: { precision: 0 }, bottomCalc: "sum", bottomCalcFormatter: "money" },
						{ title: "매출원가", field: "salscost_t", hozAlign: "right", width: 150, formatter: "money", formatterParams: { precision: 0 }, bottomCalc: "sum", bottomCalcFormatter: "money" },
						{
                            title: "매총이익", field: "salsprof_t", hozAlign: "right", width: 150, formatter: "money", formatterParams: { precision: 0 },
                            mutatorData: (v,d) => (Number(d.salsamt_t)||0) - (Number(d.salscost_t)||0),
                            bottomCalc: "sum", bottomCalcFormatter: "money", cssClass: "bg-light-gray"
                        },
						{ title: "이익률(%)", field: "prof_rate_t", hozAlign: "right", width: 120, formatter: (c) => Number(c.getValue()||0).toFixed(1) + '%' }
					]
				}
			],
            columnCalcs: "table"
		})
	}
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; border-bottom: 3px solid #005a9f !important; }

/* 🚀 2단 헤더에서 단일 컬럼의 타이틀을 수직 중앙 정렬 */
:deep(.tabulator-header .tabulator-col:not(.tabulator-col-group) .tabulator-col-content) {
	height: 100% !important;
	display: flex !important;
	align-items: center !important;
	justify-content: center !important;
}
</style>
