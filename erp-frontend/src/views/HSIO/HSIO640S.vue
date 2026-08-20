<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-box-seam me-2 text-primary" style="font-size: 18px;"></i>
        영업정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        재고관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">입출고현황 (HSIO640S)</span>
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
                <th class="text-center bg-light">입출일자</th>
                <td class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                  <DateForm v-model:fromdt="searchData.fymd" v-model:todt="searchData.tymd" />
                </td>
                <th class="text-center bg-light">창&nbsp;&nbsp;&nbsp;&nbsp;고</th>
                <td>
                  <select v-model="searchData.whcd" class="form-select form-select-sm">
                    <option value="000">전체</option>
                    <option v-for="opt in whOptions" :key="opt.whcd" :value="opt.whcd">{{ opt.whnm }}</option>
                  </select>
                </td>
                <th class="text-center bg-light">재고자산</th>
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
          <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>입출고 현황 리스트</span>
          <div class="small text-muted">품목명을 클릭하면 품목별 이력을 확인할 수 있습니다.</div>
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
import Modal from '@/components/Modal.vue'
import DateForm from '@/components/DateForm.vue'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { useCommonHelp } from '@/composables/useCommonHelp'
import { getDate } from '@/composables/useDate'

const authStore = useAuthStore()
const router = useRouter()
const { firstDay, today } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps } = useCommonHelp()

// 1. 상태 관리
const searchData = reactive({
  fymd: firstDay,
  tymd: today,
  whcd: '000',
  astkind: '120'
})

const whOptions = ref<any[]>([])
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
      { title: "No", formatter: "rownum", width: 40, hozAlign: "center" },
      { title: "품목코드", field: "itemcd", width: 100, hozAlign: "center", cssClass: "fw-bold" },
      {
        title: "품목명", field: "itemnm", minWidth: 200,
        formatter: (cell) => `<span class="text-primary cursor-pointer text-decoration-underline fw-bold">${cell.getValue()}</span>`,
        cellClick: (e, cell) => navigateToHistory(cell.getData())
      },
      { title: "전월이월", field: "bqty", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "입고", field: "iqty", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "이관입고", field: "ieqty", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "출고", field: "oqty", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "이관출고", field: "oeqty", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "타계정", field: "tqty", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      {
        title: "현재고", field: "sqty", width: 150, hozAlign: "right",
        formatter: (cell) => {
          const val = Number(cell.getValue()) || 0;
          const formatted = new Intl.NumberFormat().format(val);
          return val < 0 ? `<span class="text-danger fw-bold">${formatted}</span>` : `<span class="fw-bold">${formatted}</span>`;
        }
      }
    ]
  })
}

// 3. 기능 구현
async function fetchOptions() {
  try {
    const resWh = await api.post('/hs00/HS00_000S_STR', { gubun: 'W0', cmpycd: authStore.cmpycd })
    whOptions.value = resWh.data

    const resAsset = await api.post('/hs00/HS00_000S_STR', { gubun: 'E0', cmpycd: authStore.cmpycd, gbncd: '140', code: '' })
    assetOptions.value = resAsset.data
  } catch (e) { console.error('옵션 로드 실패') }
}

async function search() {
  try {
    const res = await api.post('/hsio/HSIO_640S_STR', {
      cmpycd: authStore.cmpycd,
      whcd: searchData.whcd,
      fromdt: searchData.fymd.replace(/-/g, ''),
      todt: searchData.tymd.replace(/-/g, ''),
      astkind: searchData.astkind
    })

    // 🚀 모든 키를 소문자로 변환 (데이터 누락 방지)
    const data = (res.data || []).map((i: any) => {
        return Object.fromEntries(Object.entries(i).map(([k, v]) => [k.toLowerCase(), v]));
    });

    grid.value?.setData(data)
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

const navigateToHistory = (row: any) => {
    router.push({
        path: '/HSIO650S',
        query: {
            astkind: searchData.astkind,
            whcd: searchData.whcd,
            itemcd: row.itemcd,
            fymd: searchData.fymd,
            tymd: searchData.tymd
        }
    })
}

function initialize() {
  resetForm(searchData)
  Object.assign(searchData, { fymd: firstDay, tymd: today, whcd: '000', astkind: '120' })
  grid.value?.clearData()
}

function print(type: string) {
    vAlert(`${type} 기능은 준비 중입니다.`)
}

onMounted(async () => {
  if (typeof window !== 'undefined') (window as any).XLSX = XLSX;
  await fetchOptions()
  nextTick(() => initGrid())
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
