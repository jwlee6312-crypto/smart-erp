<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-stack me-2 text-primary" style="font-size: 18px;"></i>
        생산정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        <span class="text-primary fw-bolder">재공수불부 (HPCL220S)</span>
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
                <th class="required">생산라인</th>
                <td>
                  <select v-model="searchData.linecd" class="form-select form-select-sm" style="width: 180px;" @change="onLineChange">
                    <option value="">라인 선택</option>
                    <option v-for="opt in lineOptions" :key="opt.linecd" :value="opt.linecd">
                      [{{ opt.linecd }}] {{ opt.linenm }}
                    </option>
                  </select>
                </td>
                <th class="required">생산공정</th>
                <td>
                  <select v-model="searchData.progcd" class="form-select form-select-sm" style="width: 180px;" @change="fetchList">
                    <option value="">공정 선택</option>
                    <option v-for="opt in progOptions" :key="opt.progcd" :value="opt.progcd">
                      [{{ opt.progcd }}] {{ opt.prognm }}
                    </option>
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
          <span class="fw-bold small text-dark"><i class="bi bi-table me-1 text-primary"></i> 재공품 월간 수불 상세</span>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="gridElement" class="tabulator-instance flex-grow-1"></div>
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
import * as XLSX from 'xlsx'
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

// 1. 상태 관리
const searchData = reactive({
  yy: String(now.getFullYear()),
  mm: String(now.getMonth() + 1).padStart(2, '0'),
  linecd: '010',
  progcd: ''
})

const yearOptions = ref<string[]>([])
const monthOptions = ['01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12']
const lineOptions = ref<any[]>([])
const progOptions = ref<any[]>([])
const closingInfo = reactive({ clsymd: '', sclsym: '', PCLSym: '' })

const gridElement = ref<HTMLElement | null>(null)
let grid: Tabulator | null = null
const itemCount = ref(0)

const generateYearOptions = () => {
    const currentYear = new Date().getFullYear()
    for (let i = 0; i < 5; i++) {
        yearOptions.value.push(String(currentYear - i))
    }
}

// 🏭 라인/공정 목록 로드
const fetchLineOptions = async () => {
  try {
    const res = await api.get('/hp00/HP00_000S_STR', { params: { gubun: 'L0', cmpycd: authStore.cmpycd, gbncd: 'Y' } })
    lineOptions.value = res.data
  } catch (e) {}
}

const fetchProgOptions = async (lineCd: string) => {
  if (!lineCd) { progOptions.value = []; return; }
  try {
    const res = await api.get('/hp00/HP00_000S_STR', { params: { gubun: 'I8', cmpycd: authStore.cmpycd, linecd: lineCd } })
    progOptions.value = res.data
  } catch (e) {}
}

const onLineChange = () => {
    searchData.progcd = ''
    fetchProgOptions(searchData.linecd)
    fetchList()
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
          title: "재공품정보", frozen: true,
          columns: [
            { title: "코드", field: "itemcd", width: 90, hozAlign: "center", headerSort: false },
            {
              title: "품 목 명", field: "itemnm", minWidth: 200, headerSort: false,
              formatter: "html",
              cellClick: (e, cell) => {
                const d = cell.getData()
                const fymd = `${searchData.yy}-${searchData.mm}-01`
                const lastDay = new Date(Number(searchData.yy), Number(searchData.mm), 0).getDate()
                const tymd = `${searchData.yy}-${searchData.mm}-${String(lastDay).padStart(2, '0')}`

                router.push({
                  path: '/HPIO670S',
                  query: {
                    linecd: searchData.linecd,
                    progcd: searchData.progcd,
                    itemcd: d.itemcd,
                    fymd: fymd,
                    tymd: tymd
                  }
                })
              },
              cssClass: "text-primary text-decoration-underline cursor-pointer fw-bold",
              bottomCalc: () => "합 계"
            },
            { title: "규격", field: "itsize", width: 120, headerSort: false }
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
const fetchClosingStatus = async () => {
  try {
    const res = await api.get('/hp00/HP00_000S_STR', { params: { gubun: 'CL', cmpycd: authStore.cmpycd } })
    if (res.data?.length) {
      closingInfo.PCLSym = String(Object.values(res.data[0])[2]).trim()
      if (closingInfo.PCLSym.length === 6) {
          searchData.yy = closingInfo.PCLSym.substring(0, 4)
          searchData.mm = closingInfo.PCLSym.substring(4, 6)
      }
    }
  } catch (e) {}
}

const fetchList = async () => {
  if (!searchData.linecd || !searchData.progcd) return

  try {
    const res = await api.post('/hpcl/HPCL_220S_STR', {
      cmpycd: authStore.cmpycd,
      ym: searchData.yy + searchData.mm,
      linecd: searchData.linecd,
      progcd: searchData.progcd
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

const initialize = () => {
  resetForm(searchData)
  Object.assign(searchData, { yy: String(now.getFullYear()), mm: String(now.getMonth() + 1).padStart(2, '0'), linecd: '010', progcd: '' })
  grid?.clearData()
  itemCount.value = 0
}

const exportExcel = () => {
  grid?.download("xlsx", `재공수불부_${searchData.yy}${searchData.mm}.xlsx`, { title: "재공 수불부" })
}

const formatNumber = (val: any) => new Intl.NumberFormat().format(Number(val) || 0)
const formatDateString = (v: any, sep: string) => v && String(v).length === 8 ? `${v.substring(0, 4)}${sep}${v.substring(4, 6)}${sep}${v.substring(6, 8)}` : (v || '')

const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

onMounted(() => {
  if (typeof window !== 'undefined') (window as any).XLSX = XLSX
  generateYearOptions()
  fetchLineOptions()
  fetchClosingStatus()
  nextTick(() => initGrid())
})
</script>
