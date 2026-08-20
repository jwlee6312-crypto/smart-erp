<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-book-half me-2 text-primary" style="font-size: 18px;"></i>
        생산정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        <span class="text-primary fw-bolder">수불부 (HPCL200S)</span>
      </div>
      <div class="btn-group-erp d-flex gap-2">
        <button class="btn-erp btn-init" @click="initialize">
          <i class="bi bi-arrow-clockwise"></i> 초기화
        </button>
        <button class="btn-erp btn-search" @click="fetchList">
          <i class="bi bi-search"></i> 조회
        </button>
        <button class="btn-erp btn-excel" @click="exportExcel">
          <i class="bi bi-file-earmark-excel"></i> 엑셀
        </button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2">
      <!-- 🅰️ 조회 조건 영역 -->
      <div class="card border-0 shadow-sm overflow-hidden" style="border-radius: 8px;">
        <div class="card-body p-0">
          <table class="erp-table-full">
            <colgroup>
              <col style="width: 100px;"><col>
              <col style="width: 100px;"><col>
              <col style="width: 100px;"><col>
            </colgroup>
            <tbody>
              <tr>
                <th class="required">연&nbsp;&nbsp;&nbsp;&nbsp;월</th>
                <td>
                  <div class="d-flex align-items-center gap-2" style="width: 250px;">
                    <select v-model="searchData.yy" class="form-select form-select-sm">
                      <option v-for="year in yearOptions" :key="year" :value="year">{{ year }}년</option>
                    </select>
                    <select v-model="searchData.mm" class="form-select form-select-sm">
                      <option v-for="month in monthOptions" :key="month" :value="month">{{ month }}월</option>
                    </select>
                  </div>
                </td>
                <th class="required">재고자산</th>
                <td>
                  <select v-model="searchData.astkind" class="form-select form-select-sm" style="width: 180px;">
                    <option v-for="opt in astOptions" :key="opt.code" :value="opt.code">{{ opt.cdnm }}</option>
                  </select>
                </td>
                <td class="text-muted small">
                  <i class="bi bi-info-circle me-1"></i> 월별 마감 데이터 기준 수불 현황입니다.
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 🅲 그리드 영역 -->
      <div class="card border-0 shadow-sm flex-grow-1 overflow-hidden d-flex flex-column" style="border-radius: 8px;">
        <div class="card-header bg-white py-2 px-3 border-bottom d-flex justify-content-between align-items-center">
          <span class="fw-bold small text-dark"><i class="bi bi-table me-1 text-primary"></i> 월간 수불 집계 내역</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, computed, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import * as XLSX from 'xlsx'
import AppAlert from '@/components/AppAlert.vue'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'

const authStore = useAuthStore()
const router = useRouter()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

const now = new Date()

// 1. 상태 관리
const searchData = reactive({
  yy: String(now.getFullYear()),
  mm: String(now.getMonth() + 1).padStart(2, '0'),
  astkind: ''
})

const yearOptions = ref<string[]>([])
const monthOptions = ['01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12']
const astOptions = ref<any[]>([])

const gridElement = ref<HTMLElement | null>(null)
let grid: Tabulator | null = null
const itemCount = ref(0)

const generateYearOptions = () => {
    const currentYear = new Date().getFullYear()
    for (let i = 0; i < 5; i++) {
        yearOptions.value.push(String(currentYear - i))
    }
}

// 2. 그리드 초기화
const initGrid = () => {
  if (gridElement.value) {
    grid = new Tabulator(gridElement.value, {
      layout: "fitColumns",
      height: "100%",
      placeholder: "조회된 데이터가 없습니다.",
      columnHeaderVertAlign: "middle",
      columns: [
        {
          title: "품 목 정 보",
          columns: [
            { title: "품목코드", field: "itemcd", width: 90, hozAlign: "center", headerSort: false },
            {
              title: "품 목 명", field: "itemnm", minWidth: 200, headerSort: false,
              formatter: "html",
              cellClick: (e, cell) => {
                const d = cell.getData()
                const fymd = `${searchData.yy}-${searchData.mm}-01`
                const lastDay = new Date(Number(searchData.yy), Number(searchData.mm), 0).getDate()
                const tymd = `${searchData.yy}-${searchData.mm}-${String(lastDay).padStart(2, '0')}`

                router.push({
                  path: '/HPIO650S',
                  query: {
                    astkind: searchData.astkind,
                    whcd: '000',
                    itemcd: d.itemcd,
                    fymd: fymd,
                    tymd: tymd
                  }
                })
              },
              cssClass: "text-primary text-decoration-underline cursor-pointer fw-bold",
              bottomCalc: () => "합 계"
            }
          ]
        },
        {
          title: "전 월 이 월",
          columns: [
            { title: "수량", field: "Bsqty", width: 80, hozAlign: "right", formatter: "money", bottomCalc: "sum" },
            { title: "단가", field: "BSprice", width: 80, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
            { title: "금액", field: "bsamt", width: 90, hozAlign: "right", formatter: "money", bottomCalc: "sum" }
          ]
        },
        {
          title: "당 월 입 고",
          columns: [
            { title: "수량", field: "inqty", width: 80, hozAlign: "right", formatter: "money", bottomCalc: "sum", cssClass: "text-success" },
            { title: "단가", field: "INprice", width: 80, hozAlign: "right", formatter: "money" },
            { title: "금액", field: "Inamt", width: 90, hozAlign: "right", formatter: "money", bottomCalc: "sum" }
          ]
        },
        {
          title: "당 월 출 고",
          columns: [
            { title: "수량", field: "outqty", width: 80, hozAlign: "right", formatter: "money", bottomCalc: "sum", cssClass: "text-danger" },
            { title: "단가", field: "outprice", width: 80, hozAlign: "right", formatter: "money" },
            { title: "금액", field: "outamt", width: 90, hozAlign: "right", formatter: "money", bottomCalc: "sum" }
          ]
        },
        {
          title: "타 계 정",
          columns: [
            { title: "수량", field: "OUTtqty", width: 80, hozAlign: "right", formatter: "money", bottomCalc: "sum" },
            { title: "단가", field: "OUTTprice", width: 80, hozAlign: "right", formatter: "money" },
            { title: "금액", field: "OUTtamt", width: 90, hozAlign: "right", formatter: "money", bottomCalc: "sum" }
          ]
        },
        {
          title: "재 고 현 황",
          columns: [
            { title: "수량", field: "stkqty", width: 80, hozAlign: "right", formatter: "money", bottomCalc: "sum", cssClass: "fw-bold" },
            { title: "단가", field: "STKprice", width: 80, hozAlign: "right", formatter: "money" },
            { title: "금액", field: "stkamt", width: 100, hozAlign: "right", formatter: "money", bottomCalc: "sum", cssClass: "text-primary fw-bold" }
          ]
        }
      ],
    })
  }
}

// 3. 기능 구현
async function fetchOptions() {
  try {
    // 💎 요청하신 대로 HS00_000S_STR 'E0', '140' 으로 호출
    const res = await api.get('/hs00/HS00_000S_STR', {
      params: { gubun: 'E0', cmpycd: authStore.cmpycd, gbncd: '140', code: '' }
    })
    // 💎 컬럼명 code, cdnm 으로 매핑
    astOptions.value = res.data.map((i: any) => ({
        code: i.code,
        cdnm: i.cdnm
    }))

    const statusRes = await api.get('/hp00/HP00_000S_STR', { params: { gubun: 'CL', cmpycd: authStore.cmpycd } })
    if (statusRes.data?.length) {
        const pclsym = String(Object.values(statusRes.data[0])[2]).trim()
        searchData.yy = pclsym.substring(0, 4)
        searchData.mm = pclsym.substring(4, 6)
    }

    if (astOptions.value.length > 0) searchData.astkind = astOptions.value[0].code
  } catch (e) { console.error('기초 정보 로드 실패') }
}

async function fetchList() {
  if (!searchData.astkind) return vAlertError('재고자산을 선택하세요.')

  try {
    const res = await api.post('/hpcl/HPCL_200S_STR', {
      cmpycd: authStore.cmpycd,
      ym: searchData.yy + searchData.mm,
      astkind: searchData.astkind
    })

    const mapped = res.data.map((i: any) => ({
        ...i,
        BSprice: Number(i.Bsqty) !== 0 ? Math.round(Number(i.bsamt) / Number(i.Bsqty)) : 0,
        INprice: Number(i.inqty) !== 0 ? Math.round(Number(i.Inamt) / Number(i.inqty)) : 0,
        outprice: Number(i.outqty) !== 0 ? Math.round(Number(i.outamt) / Number(i.outqty)) : 0,
        OUTTprice: Number(i.OUTtqty) !== 0 ? Math.round(Number(i.OUTtamt) / Number(i.OUTtqty)) : 0,
        STKprice: Number(i.stkqty) !== 0 ? Math.round(Number(i.stkamt) / Number(i.stkqty)) : 0
    }))

    grid?.setData(mapped)
    itemCount.value = mapped.length
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

function initialize() {
  generateYearOptions()
  fetchOptions()
  grid?.clearData()
  itemCount.value = 0
}

const exportExcel = () => {
  grid?.download("xlsx", `수불부_${searchData.yy}${searchData.mm}.xlsx`, { title: "재고자산 수불부" })
}

const formatNumber = (val: any) => new Intl.NumberFormat().format(Number(val) || 0)
const formatDateString = (v: any, sep: string) => v && String(v).length === 8 ? `${v.substring(0, 4)}${sep}${v.substring(4, 6)}${sep}${v.substring(6, 8)}` : (v || '')

onMounted(() => {
  if (typeof window !== 'undefined') (window as any).XLSX = XLSX
  generateYearOptions()
  fetchOptions()
  nextTick(() => initGrid())
})
</script>
