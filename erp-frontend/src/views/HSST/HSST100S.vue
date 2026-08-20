<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-journal-text me-2 text-primary" style="font-size: 18px;"></i>
        매출관리 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        <span class="text-primary fw-bolder">매출처원장 (HSST100S)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-outline-secondary" @click="print('Print')">인쇄</button>
        <button class="btn-erp btn-outline-success" @click="print('Excel')">엑셀</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2">
      <!-- 🅰️ 조회 조건 영역 -->
      <div class="card border shadow-sm overflow-hidden flex-shrink-0">
        <div class="card-body p-0">
          <table class="erp-table-full">
            <tbody>
              <tr>
                <th class="required" style="width: 100px;">판매부서</th>
                <td style="width: 250px;">
                  <div class="input-group input-group-sm" style="width: 220px;">
                    <input v-model="searchData.deptcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
                    <input v-model="searchData.deptnm" type="text" class="form-control" placeholder="부서 선택" @keyup.enter="openHelp('DEPT')" />
                    <button class="btn btn-outline-secondary" @click="openHelp('DEPT')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="required" style="width: 100px;">판매일자</th>
                <td style="width: 300px;">
                  <div class="d-flex align-items-center gap-1">
                    <input v-model="fromdt" type="date" class="form-control form-control-sm" />
                    <span>~</span>
                    <input v-model="todt" type="date" class="form-control form-control-sm" />
                  </div>
                </td>
                <th style="width: 100px;">거&nbsp;&nbsp;래&nbsp;&nbsp;처</th>
                <td>
                  <div class="input-group input-group-sm" style="width: 300px;">
                    <input v-model="searchData.custcdfr" type="text" class="form-control text-center bg-light" style="max-width: 80px;" readonly />
                    <input v-model="searchData.custnmfr" type="text" class="form-control" placeholder="거래처 선택" @keyup.enter="openHelp('CUST')" />
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
        <div class="card-header bg-white py-1 px-3 border-bottom">
          <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-1"></i> 거래처별 원장 내역</span>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="gridElement" class="tabulator-instance flex-grow-1"></div>
        </div>
        <!-- 🚀 하단 요약 정보 (Footer 표준 추가) -->
        <div class="card-footer bg-light border-top py-1 px-3 d-flex justify-content-between align-items-center flex-shrink-0">
          <div class="small text-muted">
            [ 총 <span class="fw-bold text-primary">{{ rowCount }}</span> 건 ]
          </div>
          <div class="d-flex gap-4 small">
            <div class="d-flex align-items-center">
              <span class="me-2 text-muted">매출계 합계:</span>
              <span class="fw-bold text-dark">{{ formatNumber(totals.sales_total) }}</span>
            </div>
            <div class="d-flex align-items-center border-start ps-4">
              <span class="me-2 text-muted text-info">입금계 합계:</span>
              <span class="fw-bold text-info">{{ formatNumber(totals.deposit_total) }}</span>
            </div>
            <div class="d-flex align-items-center border-start ps-4">
              <span class="me-2 text-muted text-danger">잔액 합계:</span>
              <span class="fw-bold text-danger">{{ formatNumber(totals.balance) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
    <Modal v-model:visible="modalVisible" :modalProps="modalProps" />
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, computed, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import type { ModalProps } from '@/types/modal'

const authStore = useAuthStore()
const router = useRouter()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

const now = new Date()
const initymd = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
const initfromdt = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`

// 1. 상태 관리
const searchData = reactive({
  deptcd: authStore.deptcd,
  deptnm: authStore.deptnm,
  fromdt: initfromdt.replace(/-/g, ''),
  todt: initymd.replace(/-/g, ''),
  custcdfr: '',
  custnmfr: '',
  selgbn: '1'
})

const fromdt = computed({ get: () => formatDateString(searchData.fromdt, '-'), set: (v) => searchData.fromdt = v.replace(/-/g, '') })
const todt = computed({ get: () => formatDateString(searchData.todt, '-'), set: (v) => searchData.todt = v.replace(/-/g, '') })

const gridElement = ref<HTMLElement | null>(null)
const grid = ref<Tabulator | null>(null)
const rowCount = ref(0)
const totals = reactive({ sales_total: 0, deposit_total: 0, balance: 0 })

// 2. 그리드 초기화 (다단 헤더 적용)
const initGrid = () => {
  if (!gridElement.value) return
  grid.value = new Tabulator(gridElement.value, {
    layout: "fitColumns", height: "100%", placeholder: "자료 없음", selectable: true,
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      {
        title: "거래처", field: "custnm", minWidth: 180, cssClass: "fw-bold border-end",
        formatter: (cell) => `<span class="text-primary text-decoration-underline cursor-pointer">${cell.getValue()}</span>`,
        cellClick: (e, cell) => navigateToDetail(cell.getData())
      },
      {
        title: "이월액", field: "baseamt", width: 120, hozAlign: "right",
        formatter: "money", formatterParams: { precision: 0 },
        bottomCalc: "sum", bottomCalcFormatter: "money", bottomCalcFormatterParams: { precision: 0 }
      },
      // 판매현황 그룹
      {
        title: "판매현황(vat포함)",
        columns: [
          { title: "공급가", field: "spyamt", width: 120, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, bottomCalc: "sum", bottomCalcFormatter: "money", bottomCalcFormatterParams: { precision: 0 } },
          { title: "부가세", field: "vatamt", width: 120, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, bottomCalc: "sum", bottomCalcFormatter: "money", bottomCalcFormatterParams: { precision: 0 } },
          {
            title: "매출계", field: "SALES_total", width: 150, hozAlign: "right",
            mutator: (v, d) => Number(d.spyamt || 0) + Number(d.vatamt || 0),
            formatter: "money", formatterParams: { precision: 0 },
            cssClass: "bg-light border-end", bottomCalc: "sum", bottomCalcFormatter: "money", bottomCalcFormatterParams: { precision: 0 }
          },
        ]
      },
      // 입금현황 그룹
      {
        title: "입 금 현 황",
        columns: [
          { title: "현금", field: "cashamt", width: 120, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, bottomCalc: "sum", bottomCalcFormatter: "money", bottomCalcFormatterParams: { precision: 0 } },
          { title: "예금", field: "bankamt", width: 120, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, bottomCalc: "sum", bottomCalcFormatter: "money", bottomCalcFormatterParams: { precision: 0 } },
          { title: "어음/카드", field: "billamt", width: 120, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, bottomCalc: "sum", bottomCalcFormatter: "money", bottomCalcFormatterParams: { precision: 0 } },
          { title: "기타", field: "etcamt", width: 120, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, bottomCalc: "sum", bottomCalcFormatter: "money", bottomCalcFormatterParams: { precision: 0 } },
          {
            title: "입금계", field: "DEPOSIT_total", width: 150, hozAlign: "right",
            mutator: (v, d) => Number(d.cashamt || 0) + Number(d.bankamt || 0) + Number(d.billamt || 0) + Number(d.etcamt || 0),
            formatter: "money", formatterParams: { precision: 0 },
            cssClass: "bg-light border-end", bottomCalc: "sum", bottomCalcFormatter: "money", bottomCalcFormatterParams: { precision: 0 }
          },
        ]
      },
      {
        title: "잔액", field: "BALANCE", width: 150, hozAlign: "right",
        mutator: (v, d) => {
          const totalSales = Number(d.spyamt || 0) + Number(d.vatamt || 0);
          const totalDeposit = Number(d.cashamt || 0) + Number(d.bankamt || 0) + Number(d.billamt || 0) + Number(d.etcamt || 0);
          return Number(d.baseamt || 0) + totalSales - totalDeposit;
        },
        formatter: "money", formatterParams: { precision: 0 },
        cssClass: "bg-light fw-bold text-danger",
        bottomCalc: "sum", bottomCalcFormatter: "money", bottomCalcFormatterParams: { precision: 0 }
      }
    ]
  })
}

// 3. 기능 구현
async function search() {
  if (!searchData.deptcd) return vAlertError('판매부서를 선택해 주십시요.')
  try {
    const res = await api.post('/hsst/HSST_100S_STR', {
      cmpycd: authStore.cmpycd,
      selgbn: searchData.selgbn,
      deptcd: searchData.deptcd,
      custfr: searchData.custcdfr,
      custto: searchData.custcdfr, // ASP 로직상 시작/종료를 동일하게 사용하거나 범위 지정
      fromdt: searchData.fromdt,
      todt: searchData.todt
    })

    const data = res.data || []

    if (grid.value) {
      grid.value.setData(data)
      rowCount.value = data.length

      // 하단 요약 바용 합계 계산
      totals.sales_total = data.reduce((acc: number, cur: any) => acc + (Number(cur.spyamt || 0) + Number(cur.vatamt || 0)), 0)
      totals.deposit_total = data.reduce((acc: number, cur: any) => acc + (Number(cur.cashamt || 0) + Number(cur.bankamt || 0) + Number(cur.billamt || 0) + Number(cur.etcamt || 0)), 0)
      totals.balance = data.reduce((acc: number, cur: any) => {
          const sales = Number(cur.spyamt || 0) + Number(cur.vatamt || 0);
          const deposit = Number(cur.cashamt || 0) + Number(cur.bankamt || 0) + Number(cur.billamt || 0) + Number(cur.etcamt || 0);
          return acc + (Number(cur.baseamt || 0) + sales - deposit);
      }, 0)

      vAlert('조회되었습니다.')
    }
  } catch (e) { vAlertError('조회 실패') }
}

const navigateToDetail = (row: any) => {
    router.push({
        name: '매출처상세원장',
        query: {
            custcd: row.custcd,
            custnm: row.custnm,
            fromdt: searchData.fromdt,
            todt: searchData.todt,
            deptcd: searchData.deptcd
        }
    })
}

function initialize() {
  resetForm(searchData)
  searchData.fromdt = initfromdt.replace(/-/g, '')
  searchData.todt = initymd.replace(/-/g, '')
  searchData.deptcd = authStore.deptcd
  searchData.deptnm = authStore.deptnm
  grid.value?.clearData()
  rowCount.value = 0
  Object.assign(totals, { sales_total: 0, deposit_total: 0, balance: 0 })
}

function print(type: string) {
  const url = `HSST_100P.asp?selgbn=1&deptcd=${searchData.deptcd}&custcdfr=${searchData.custcdfr}&fromdt=${searchData.fromdt}&todt=${searchData.todt}&PRTGU=${type}`
  window.open(url, 'print', 'width=700,height=650,scrollbars=yes')
}

// 4. 팝업 설정
const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function openHelp(type: string) {
  if (type === 'DEPT') {
    Object.assign(modalProps, {
      title: '부서 선택', path: '/ha00/HA00_00P_STR', defaultField: 'deptnm',
      data: { gubun: 'D0', cmpycd: authStore.cmpycd },
      columns: [{ title: '코드', field: 'deptcd', width: 80 }, { title: '부서명', field: 'deptnm', width: 180 }],
      onConfirm: (data: any) => { searchData.deptcd = data.deptcd; searchData.deptnm = data.deptnm }
    })
  } else if (type === 'CUST') {
    Object.assign(modalProps, {
      title: '거래처 선택', path: '/ha00/HA00_00P_STR', defaultField: 'custnm',
      data: { gubun: 'C4', cmpycd: authStore.cmpycd },
      columns: [{ title: '코드', field: 'custcd', width: 80 }, { title: '거래처명', field: 'custnm', width: 200 }],
      onConfirm: (data: any) => { searchData.custcdfr = data.custcd; searchData.custnmfr = data.custnm }
    })
  }
  modalVisible.value = true
}

const formatDateString = (v: any, sep: string) => v && v.length === 8 ? `${v.substring(0, 4)}${sep}${v.substring(4, 6)}${sep}${v.substring(6, 8)}` : v
const formatNumber = (val: any) => new Intl.NumberFormat().format(Number(val) || 0)

onMounted(() => {
  nextTick(() => initGrid())
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
