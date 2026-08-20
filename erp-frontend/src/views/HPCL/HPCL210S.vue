<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-box-seam me-2 text-primary" style="font-size: 18px;"></i>
        생산정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        <span class="text-primary fw-bolder">창고수불부 (HPCL210S)</span>
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
                  <div class="d-flex align-items-center gap-2" style="width: 220px;">
                    <select v-model="searchData.yy" class="form-select form-select-sm">
                      <option v-for="year in yearOptions" :key="year" :value="year">{{ year }}년</option>
                    </select>
                    <select v-model="searchData.mm" class="form-select form-select-sm">
                      <option v-for="month in monthOptions" :key="month" :value="month">{{ month }}월</option>
                    </select>
                  </div>
                </td>
                <th class="required">출고창고</th>
                <td>
                  <select v-model="searchData.whcd" class="form-select form-select-sm" style="width: 150px;">
                    <option value="000">전체</option>
                    <option v-for="opt in whOptions" :key="opt.code" :value="opt.code">{{ opt.cdnm }}</option>
                  </select>
                </td>
                <th class="required">재고자산</th>
                <td>
                  <select v-model="searchData.astkind" class="form-select form-select-sm" style="width: 150px;">
                    <option v-for="opt in astOptions" :key="opt.code" :value="opt.code">{{ opt.cdnm }}</option>
                  </select>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 🅲 그리드 영역 -->
      <div class="card border-0 shadow-sm flex-grow-1 overflow-hidden d-flex flex-column" style="border-radius: 8px;">
        <div class="card-header bg-white py-2 px-3 border-bottom d-flex justify-content-between align-items-center">
          <span class="fw-bold small text-dark"><i class="bi bi-table me-1 text-primary"></i> 창고별 상세 수불 내역</span>
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
  whcd: '000',
  astkind: ''
})

const yearOptions = ref<string[]>([])
const monthOptions = ['01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12']
const whOptions = ref<any[]>([])
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
          title: "품목정보", frozen: true,
          columns: [
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
                    whcd: searchData.whcd,
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
            { title: "수량", field: "Bsqty", width: 70, hozAlign: "right", formatter: "money", bottomCalc: "sum" },
            { title: "단가", field: "BSprice", width: 70, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
            { title: "금액", field: "bsamt", width: 85, hozAlign: "right", formatter: "money", bottomCalc: "sum" }
          ]
        },
        {
          title: "당 월 입 고",
          columns: [
            { title: "수량", field: "inqty", width: 70, hozAlign: "right", formatter: "money", bottomCalc: "sum", cssClass: "text-success" },
            { title: "단가", field: "INprice", width: 70, hozAlign: "right", formatter: "money" },
            { title: "금액", field: "Inamt", width: 85, hozAlign: "right", formatter: "money", bottomCalc: "sum" }
          ]
        },
        {
          title: "당 월 출 고",
          columns: [
            { title: "수량", field: "outqty", width: 70, hozAlign: "right", formatter: "money", bottomCalc: "sum", cssClass: "text-danger" },
            { title: "단가", field: "outprice", width: 70, hozAlign: "right", formatter: "money" },
            { title: "금액", field: "outamt", width: 85, hozAlign: "right", formatter: "money", bottomCalc: "sum" }
          ]
        },
        {
          title: "이 관",
          columns: [
            { title: "수량", field: "outmqty", width: 70, hozAlign: "right", formatter: "money", bottomCalc: "sum" },
            { title: "단가", field: "OUTMprice", width: 70, hozAlign: "right", formatter: "money" },
            { title: "금액", field: "outmamt", width: 85, hozAlign: "right", formatter: "money", bottomCalc: "sum" }
          ]
        },
        {
          title: "타 계 정",
          columns: [
            { title: "수량", field: "OUTtqty", width: 70, hozAlign: "right", formatter: "money", bottomCalc: "sum" },
            { title: "단가", field: "OUTTprice", width: 70, hozAlign: "right", formatter: "money" },
            { title: "금액", field: "OUTtamt", width: 85, hozAlign: "right", formatter: "money", bottomCalc: "sum" }
          ]
        },
        {
          title: "재 고 현 황",
          columns: [
            { title: "수량", field: "stkqty", width: 70, hozAlign: "right", formatter: "money", bottomCalc: "sum", cssClass: "fw-bold" },
            { title: "단가", field: "STKprice", width: 70, hozAlign: "right", formatter: "money" },
            { title: "금액", field: "stkamt", width: 100, hozAlign: "right", formatter: "money", bottomCalc: "sum", cssClass: "text-primary fw-bold" }
          ]
        }
      ],
    })
  }
}

// 3. 비즈니스 로직
const fetchOptions = async () => {
  try {
    // 창고 (HS00_000S_STR 'W0')
    const resWh = await api.get('/comm/codes/WH')
    whOptions.value = resWh.data

    // 재고자산 (HP00_000S_STR 'E0', '100')
    const resAst = await api.get('/hp00/HP00_000S_STR', { params: { gubun: 'E0', cmpycd: authStore.cmpycd, gbncd: '100' } })
    astOptions.value = resAst.data.filter((i:any) => i.code <= '119')
    if (astOptions.value.length > 0) searchData.astkind = astOptions.value[0].code

    // 마감 상태에 따른 연월 초기값
    const statusRes = await api.get('/hp00/HP00_000S_STR', { params: { gubun: 'CL', cmpycd: authStore.cmpycd } })
    if (statusRes.data?.length) {
        const pclsym = String(Object.values(statusRes.data[0])[2]).trim()
        if (pclsym.length === 6) {
            searchData.yy = pclsym.substring(0, 4)
            searchData.mm = pclsym.substring(4, 6)
        }
    }
  } catch (e) {}
}

const fetchList = async () => {
  if (!searchData.astkind) return vAlertError('재고자산을 선택하세요.')
  if (!searchData.whcd) return vAlertError('창고를 선택하세요.')

  try {
    const res = await api.post('/hpcl/HPCL_210S_STR', {
      cmpycd: authStore.cmpycd,
      ym: searchData.yy + searchData.mm,
      whcd: searchData.whcd,
      astkind: searchData.astkind
    })

    // 단가 계산 로직
    const mapped = res.data.map((i: any) => ({
        ...i,
        BSprice: Number(i.Bsqty) !== 0 ? Math.round(Number(i.bsamt) / Number(i.Bsqty)) : 0,
        INprice: Number(i.inqty) !== 0 ? Math.round(Number(i.Inamt) / Number(i.inqty)) : 0,
        outprice: Number(i.outqty) !== 0 ? Math.round(Number(i.outamt) / Number(i.outqty)) : 0,
        OUTMprice: Number(i.outmqty) !== 0 ? Math.round(Number(i.outmamt) / Number(i.outmqty)) : 0,
        OUTTprice: Number(i.OUTtqty) !== 0 ? Math.round(Number(i.OUTtamt) / Number(i.OUTtqty)) : 0,
        STKprice: Number(i.stkqty) !== 0 ? Math.round(Number(i.stkamt) / Number(i.stkqty)) : 0
    }))

    grid?.setData(mapped)
    itemCount.value = mapped.length
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

const initialize = () => {
  resetForm(searchData)
  Object.assign(searchData, { yy: String(now.getFullYear()), mm: String(now.getMonth() + 1).padStart(2, '0'), whcd: '000' })
  if (astOptions.value.length > 0) searchData.astkind = astOptions.value[0].code
  grid?.clearData()
  itemCount.value = 0
}

const exportExcel = () => {
  grid?.download("xlsx", `창고수불부_${searchData.yy}${searchData.mm}.xlsx`, { title: "창고별 재고자산 수불부" })
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
