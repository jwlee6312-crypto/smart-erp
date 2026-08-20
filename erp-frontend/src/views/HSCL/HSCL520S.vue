<!--
	=============================================================
	프로그램명	: 거래처매출원가 list (HSCL520S)
	작성일자	: 2025.03.14
	설명        : 거래처별 매출금액 및 매출원가, 이익 현황 조회 (표준 UI 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-list-columns-reverse me-2 text-primary" style="font-size: 18px;"></i>
        마감관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        통계현황 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">거래처매출원가 list (HSCL520S)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-excel" @click="excel">엑셀</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">
      <!-- [상단] 조회 조건 영역 -->
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
                <th class="required text-center bg-light">판매부서</th>
                <td>
                  <div class="input-group input-group-sm px-2">
                    <input v-model="searchForm.deptnm" class="form-control fw-bold text-primary" readonly />
                    <button class="btn btn-outline-secondary" @click="openHelp"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="required text-center bg-light border-start">조회년월</th>
                <td>
                  <div class="d-flex align-items-center gap-1 px-2">
                    <select v-model="searchForm.yy" class="form-select form-select-sm" style="width: 100px;">
                      <option v-for="year in yearOptions" :key="year" :value="year">{{ year }}년</option>
                    </select>
                    <select v-model="searchForm.mm" class="form-select form-select-sm" style="width: 80px;">
                      <option v-for="month in monthOptions" :key="month" :value="month">{{ month }}월</option>
                    </select>
                  </div>
                </td>
                <td class="text-end pe-3 bg-white border-start">
                  <span v-if="closingMonth" class="badge bg-primary px-2">마감: {{ formatYM(closingMonth) }}</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 데이터 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>거래처별 매출금액 및 이익 현황</span>
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
import * as XLSX from 'xlsx'
import { useAlerts } from '@/composables/useAlerts'
import AppAlert from '@/components/AppAlert.vue'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import Modal from '@/components/Modal.vue'
import { useCommonHelp } from '@/composables/useCommonHelp'
import { getDate } from '@/composables/useDate'

const authStore = useAuthStore()
const { today } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps } = useCommonHelp()

const currentYear = new Date().getFullYear()
const yearOptions = Array.from({ length: 6 }, (_, i) => currentYear - i)
const monthOptions = ['01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12']

// 검색 폼 데이터
const searchForm = reactive<any>({
	deptcd: authStore.deptcd,
	deptnm: authStore.deptnm,
	yy: currentYear,
    mm: today.substring(5, 7)
})

const closingMonth = ref('') // 시스템 마감월 (sclsym)
const mainGridRef = ref<HTMLDivElement | null>(null)
let mainGrid: Tabulator | null = null

// 초기 데이터(마감정보) 조회
const getClosingInfo = async () => {
	try {
		const res = await api.post('/hs00/HS00_000S_STR', { gubun: 'CL', cmpycd: authStore.cmpycd })
		if (res.data?.length) {
			closingMonth.value = res.data[0].sclsym
			searchForm.yy = Number(closingMonth.value.substring(0, 4))
			searchForm.mm = closingMonth.value.substring(4, 6)
		}
	} catch (e) { console.error('마감정보 조회 실패') }
}

const search = async () => {
    if (!searchForm.deptcd) return vAlertError('판매부서를 선택해 주십시오.')
	const searchym = `${searchForm.yy}${searchForm.mm}`
    if (closingMonth.value && searchym > closingMonth.value) return vAlertError('작업마감 작업 후 조회하시기 바랍니다.')

	try {
		const res = await api.post('/hscl/HSCL_520S_STR', {
			cmpycd: authStore.cmpycd,
			deptcd: searchForm.deptcd,
			ym: searchym
		})
        const data = (res.data || []).map((i: any) => Object.fromEntries(Object.entries(i).map(([k, v]) => [k.toLowerCase(), v])));
		mainGrid?.setData(data)
		vAlert('조회되었습니다.')
	} catch (e) { vAlertError('조회 중 오류가 발생했습니다.') }
}

const initialize = () => {
	resetForm(searchForm)
    Object.assign(searchForm, { deptcd: authStore.deptcd, deptnm: authStore.deptnm })
	getClosingInfo()
	mainGrid?.clearData()
}

const excel = () => mainGrid?.download("xlsx", `거래처매출원가list_${searchForm.yy}${searchForm.mm}.xlsx`)

// 부서 도움말 팝업
function openHelp() {
    Object.assign(modalProps, {
        title: '부서 선택', path: '/ha00/HA00_00P_STR',
        columns: [{ title: '코드', field: 'deptcd', width: 80 }, { title: '부서명', field: 'deptnm', width: 180 }],
        data: { gubun: 'D0', cmpycd: authStore.cmpycd },
        onConfirm: (d: any) => { searchForm.deptcd = d.deptcd; searchForm.deptnm = d.deptnm; search(); }
    })
	modalVisible.value = true
}

const formatYM = (v: string) => v ? `${v.substring(0, 4)}-${v.substring(4, 6)}` : '';

onMounted(() => {
	if (typeof window !== 'undefined') (window as any).XLSX = XLSX;
	getClosingInfo()

	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns',
			height: '100%',
			groupBy: ["deptnm", "custnm"],
			groupHeader: (value, count, data, group) => {
				const label = group.getField() === "deptnm" ? "부서: " : "거래처: "
				return `<span class='fw-bold text-primary'>${label}${value || ''}</span> <span class='ms-2 text-muted small'>(${count}건)</span>`
			},
			columnDefaults: { headerSort: false, headerHozAlign: "center", hozAlign: "center", vertAlign: "middle" },
			columns: [
                { title: "No", formatter: "rownum", width: 45, hozAlign: "center" },
				{ title: "부서명", field: "deptnm", width: 130, visible: false },
				{ title: "거래처", field: "custnm", minWidth: 180, visible: false },
				{
					title: "매출일자", field: "salsymd", width: 110,
					formatter: (cell) => {
						const val = cell.getValue()
						return val && val.length === 8 ? `${val.substring(0, 4)}.${val.substring(4, 6)}.${val.substring(6, 8)}` : (val || '')
					}
				},
				{
					title: "출고번호", field: "io_no", width: 130,
					formatter: (cell) => {
						const d = cell.getData()
						return (d.ioym && d.iono) ? `${d.ioym}-${d.iono}` : ''
					}
				},
				{ title: "품목명", field: "itemnm", hozAlign: "left", minWidth: 200, cssClass: "fw-bold" },
				{ title: "수량", field: "jsanqty", width: 90, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
				{
					title: "매출금액", field: "jsanamt", width: 120, hozAlign: "right",
					formatter: "money", formatterParams: { precision: 0 },
					bottomCalc: "sum", bottomCalcFormatter: "money"
				},
				{
					title: "매출원가", field: "wonamt", width: 120, hozAlign: "right",
					formatter: "money", formatterParams: { precision: 0 },
					bottomCalc: "sum", bottomCalcFormatter: "money"
				},
				{
					title: "이익율", field: "bnfrate", width: 90, hozAlign: "right",
					formatter: (cell) => {
						const val = cell.getValue()
						return val ? Number(val).toFixed(2) + '%' : '0.00%'
					},
					bottomCalc: (values, data) => {
						const salesSum = data.reduce((acc: number, row: any) => acc + Number(row.jsanamt || 0), 0)
						const costSum = data.reduce((acc: number, row: any) => acc + Number(row.wonamt || 0), 0)
						return salesSum !== 0 ? (((salesSum - costSum) / salesSum) * 100).toFixed(2) + '%' : '0.00%'
					},
                    cssClass: "text-primary fw-bold"
				}
			],
			columnCalcs: "table"
		})
	}
})
</script>

<style scoped>
.erp-container { font-family: 'Pretendard', sans-serif; letter-spacing: -0.02rem; }
.tabulator-instance { width: 100% !important; background-color: #fff; font-size: 12px; }
:deep(.tabulator-group) { background-color: #f8f9fa !important; border-bottom: 1px solid #dee2e6 !important; }
:deep(.tabulator-group-level-1) { padding-left: 20px !important; background-color: #ffffff !important; }
:deep(.tabulator-footer) { background-color: #f8fafc !important; font-weight: 700; }
</style>
