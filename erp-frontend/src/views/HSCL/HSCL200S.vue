<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-journals me-2 text-primary" style="font-size: 18px;"></i>
        마감관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        수불관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">수불부 (HSCL200S)</span>
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
              <col style="width: 100px;" /><col style="width: 300px;" />
              <col style="width: 100px;" /><col style="width: 200px;" />
              <col />
            </colgroup>
            <tbody>
              <tr>
                <th class="required bg-light text-center">조회연월</th>
                <td>
                  <div class="d-flex align-items-center gap-1">
                    <select v-model="searchData.yy" class="form-select form-select-sm" style="width: 100px;">
                      <option v-for="y in yearOptions" :key="y" :value="y">{{ y }}년</option>
                    </select>
                    <select v-model="searchData.mm" class="form-select form-select-sm" style="width: 80px;">
                      <option v-for="m in monthOptions" :key="m" :value="m">{{ m }}월</option>
                    </select>
                    <span class="px-1 text-muted">~</span>
                    <select v-model="searchData.tomm" class="form-select form-select-sm" style="width: 80px;">
                      <option v-for="m in monthOptions" :key="m" :value="m">{{ m }}월</option>
                    </select>
                  </div>
                </td>
                <th class="required bg-light text-center">재고자산</th>
                <td>
                  <select v-model="searchData.astkind" class="form-select form-select-sm">
                    <option v-for="opt in assetOptions" :key="opt.code" :value="opt.code">{{ opt.cdnm }}</option>
                  </select>
                </td>
                <td class="text-end pe-3">
                  <span v-if="closingInfo.sclsym" class="badge bg-primary px-2">마감: {{ closingInfo.sclsym_fmt }}</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 🅱️ 데이터 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>품목별 월간 수불 현황</span>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="gridElement" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, nextTick } from 'vue'
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

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps } = useCommonHelp()

const now = new Date()
const currentYear = now.getFullYear()

// 1. 상태 관리
const searchData = reactive({
  yy: String(currentYear),
  mm: '01',
  tomm: String(now.getMonth() + 1).padStart(2, '0'),
  astkind: '120'
})

const closingInfo = reactive({
  clsymd: '', sclsym: '', wclsym: '',
  sclsym_fmt: '', wclsym_fmt: ''
})

const yearOptions = Array.from({ length: 6 }, (_, i) => String(currentYear - i))
const monthOptions = Array.from({ length: 12 }, (_, i) => String(i + 1).padStart(2, '0'))
const assetOptions = ref<any[]>([])

const gridElement = ref<HTMLElement | null>(null)
const grid = ref<Tabulator | null>(null)

// 2. 그리드 초기화

const initGrid = () => {
  if (!gridElement.value) return
  grid.value = new Tabulator(gridElement.value, {
    layout: "fitColumns",
    height: "100%",
    placeholder: "조회된 데이터가 없습니다.",
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "No", formatter: "rownum", width: 40, hozAlign: "center", frozen: true },
      { title: "품목명", field: "itemnm", minWidth: 250, frozen: true, cssClass: "fw-bold border-end" },
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

// 3. 기능 구현
async function fetchOptions() {
  try {
    const resAsset = await api.post('/hs00/HS00_000S_STR', { gubun: 'E0', cmpycd: authStore.cmpycd, gbncd: '140', code: '' })
    assetOptions.value = resAsset.data

    const resCl = await api.post('/hs00/HS00_000S_STR', { gubun: 'CL', cmpycd: authStore.cmpycd })
    if (resCl.data?.length) {
      const data = resCl.data[0]
      closingInfo.clsymd = data.clsymd;
      closingInfo.sclsym = data.sclsym;
      closingInfo.wclsym = data.wclsym;
      closingInfo.sclsym_fmt = data.sclsym ? `${data.sclsym.substring(0, 4)}-${data.sclsym.substring(4, 6)}` : '';

      searchData.yy = data.sclsym.substring(0, 4);
      searchData.mm = data.sclsym.substring(4, 6);
      searchData.tomm = data.sclsym.substring(4, 6);
    }
  } catch (e) { console.error('기초 정보 로드 실패') }
}

async function search() {
  if (searchData.mm > searchData.tomm) return vAlertError('조회기간을 확인하십시오. (시작월 > 종료월)')

  try {
    const res = await api.post('/hscl/HSCL_200S_STR', {
      cmpycd: authStore.cmpycd,
      yymm_fr: `${searchData.yy}${searchData.mm}`,
      yymm_to: `${searchData.yy}${searchData.tomm}`,
      astkind: searchData.astkind
    })

    const data = res.data || []
    grid.value?.setData(data)
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

function initialize() {
  resetForm(searchData)
  if (closingInfo.sclsym) {
    searchData.yy = closingInfo.sclsym.substring(0, 4)
    searchData.mm = closingInfo.sclsym.substring(4, 6)
    searchData.tomm = closingInfo.sclsym.substring(4, 6)
  }
  searchData.astkind = '120'
  grid.value?.clearData()
}

function print(type: string) {
  vAlert(`${type} 기능은 현재 준비 중입니다.`)
}

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

