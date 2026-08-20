<!--
	=============================================================
	프로그램명	: 창고수불부 (HSCL210S)
	작성일자	: 2025.02.24
	설명        : 창고별 품목 수불 현황 조회 (HSOD100U 디자인 표준 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-journal-text me-2 text-primary" style="font-size: 18px;"></i>
        마감관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        수불관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">창고수불부 (HSCL210S)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-print" @click="print('Print')">인쇄</button>
        <button class="btn-erp btn-excel" @click="print('Excel')">엑셀</button>
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
                <th class="required text-center bg-light">조회연월</th>
                <td>
                  <div class="d-flex align-items-center gap-1">
                    <select v-model="searchData.yy" class="form-select form-select-sm" style="width: 100px;">
                      <option v-for="y in yearOptions" :key="y" :value="y">{{ y }}년</option>
                    </select>
                    <select v-model="searchData.mm" class="form-select form-select-sm" style="width: 80px;">
                      <option v-for="m in monthOptions" :key="m" :value="m">{{ m }}월</option>
                    </select>
                  </div>
                </td>
                <th class="required text-center bg-light">출고창고</th>
                <td>
                  <select v-model="searchData.whcd" class="form-select form-select-sm">
                    <option v-for="opt in whOptions" :key="opt.whcd" :value="opt.whcd">{{ opt.whnm }}</option>
                  </select>
                </td>
                <th class="required text-center bg-light">재고자산</th>
                <td>
                  <select v-model="searchData.astkind" class="form-select form-select-sm w-50">
                    <option v-for="opt in assetOptions" :key="opt.code" :value="opt.code">{{ opt.cdnm }}</option>
                  </select>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 🅱️ 데이터 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>품목별 창고 수불 현황</span>
          <div class="d-flex gap-3 small fw-bold">
            <span class="text-dark">재고금액합계: <span class="text-danger">{{ formatNumber(totals.stkamt) }}</span></span>
          </div>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="gridElement" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, computed, nextTick } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import * as XLSX from 'xlsx'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { useCommonHelp } from '@/composables/useCommonHelp'
import { getDate } from '@/composables/useDate'

const authStore = useAuthStore()
const { today } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps } = useCommonHelp()

const currentYear = new Date().getFullYear()
const yearOptions = Array.from({ length: 6 }, (_, i) => String(currentYear - i))
const monthOptions = Array.from({ length: 12 }, (_, i) => String(i + 1).padStart(2, '0'))

const searchData = reactive<any>({
  yy: String(currentYear),
  mm: today.substring(5, 7),
  whcd: '',
  astkind: '120'
})

const totals = reactive({ bsamt: 0, inamt: 0, outamt: 0, outtamt: 0, stkamt: 0 })
const whOptions = ref<any[]>([])
const assetOptions = ref<any[]>([])

const gridElement = ref<HTMLElement | null>(null)
const grid = ref<Tabulator | null>(null)

const initGrid = () => {
  if (!gridElement.value) return
  grid.value = new Tabulator(gridElement.value, {
    layout: "fitColumns",
    height: "100%",
    placeholder: "조회된 데이터가 없습니다.",
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "No", formatter: "rownum", width: 40, hozAlign: "center", frozen: true },
      { title: "품목명", field: "itemnm", minWidth: 200, frozen: true, cssClass: "fw-bold border-end" },
      { title: "규격", field: "itsize", width: 150, frozen: true },
      {
        title: "이 월",
        columns: [
          { title: "수량", field: "bsqty", width: 100, hozAlign: "right", formatter: (c) => formatQty(c) },
          { title: "금액", field: "bsamt", width: 120, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
        ]
      },
      {
        title: "입 고",
        columns: [
          { title: "수량", field: "inqty", width: 100, hozAlign: "right", formatter: (c) => formatQty(c) },
          { title: "금액", field: "inamt", width: 120, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
        ]
      },
      {
        title: "출 고",
        columns: [
          { title: "수량", field: "outqty", width: 100, hozAlign: "right", formatter: (c) => formatQty(c) },
          { title: "금액", field: "outamt", width: 120, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
        ]
      },
      {
        title: "타계정",
        columns: [
          { title: "수량", field: "outtqty", width: 100, hozAlign: "right", formatter: (c) => formatQty(c) },
          { title: "금액", field: "outtamt", width: 120, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
        ]
      },
      {
        title: "재 고",
        columns: [
          { title: "수량", field: "stkqty", width: 100, hozAlign: "right", formatter: (c) => formatQty(c) },
          { title: "금액", field: "stkamt", width: 120, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "bg-light-yellow fw-bold" },
        ]
      }
    ]
  })
}

const formatQty = (cell: any) => {
  const data = cell.getData();
  const pnt = Number(data.qtypnt) || 0;
  const val = Number(cell.getValue()) || 0;
  return new Intl.NumberFormat(undefined, { minimumFractionDigits: pnt, maximumFractionDigits: pnt }).format(val);
}

async function fetchOptions() {
  try {
    const resWh = await api.post('/hs00/HS00_000S_STR', { gubun: 'W0', cmpycd: authStore.cmpycd })
    whOptions.value = resWh.data
    if (whOptions.value.length > 0) searchData.whcd = whOptions.value[0].whcd

    const resAsset = await api.post('/hs00/HS00_000S_STR', { gubun: 'E0', cmpycd: authStore.cmpycd, gbncd: '140', code: '' })
    assetOptions.value = resAsset.data
  } catch (e) { console.error('옵션 로드 실패') }
}

async function search() {
  if (!searchData.whcd) return vAlertError('창고를 선택하세요.')
  try {
    const res = await api.post('/hscl/HSCL_210S_STR', {
      cmpycd: authStore.cmpycd,
      whcd: searchData.whcd,
      yymm: `${searchData.yy}${searchData.mm}`,
      astkind: searchData.astkind
    })

    const data = (res.data || []).map((i: any) => Object.fromEntries(Object.entries(i).map(([k, v]) => [k.toLowerCase(), v])));
    grid.value?.setData(data)

    totals.bsamt = data.reduce((acc: number, cur: any) => acc + (Number(cur.bsamt) || 0), 0)
    totals.inamt = data.reduce((acc: number, cur: any) => acc + (Number(cur.inamt) || 0), 0)
    totals.outamt = data.reduce((acc: number, cur: any) => acc + (Number(cur.outamt) || 0), 0)
    totals.outtamt = data.reduce((acc: number, cur: any) => acc + (Number(cur.outtamt) || 0), 0)
    totals.stkamt = data.reduce((acc: number, cur: any) => acc + (Number(cur.stkamt) || 0), 0)

    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

function initialize() {
  resetForm(searchData)
  searchData.yy = String(currentYear)
  searchData.mm = today.substring(5, 7)
  searchData.astkind = '120'
  if (whOptions.value.length > 0) searchData.whcd = whOptions.value[0].whcd
  grid.value?.clearData()
  Object.keys(totals).forEach(key => (totals as any)[key] = 0)
}

function print(type: string) {
  const url = `/report/HSCL_210P?astkind=${searchData.astkind}&whcd=${searchData.whcd}&yy=${searchData.yy}&mm=${searchData.mm}&PRTGU=${type}`
  window.open(url, 'print', 'width=800,height=700,scrollbars=yes')
}

const formatNumber = (val: any) => new Intl.NumberFormat().format(Number(val) || 0)

onMounted(async () => {
  if (typeof window !== 'undefined') (window as any).XLSX = XLSX;
  await fetchOptions()
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
