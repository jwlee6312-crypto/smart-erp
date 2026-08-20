<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="hppl130s-wrapper d-flex flex-column h-100 bg-white p-0">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-box-seam me-2 text-primary" style="font-size: 18px;"></i>
        생산정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        <span class="text-primary fw-bolder">자재소요량현황 (HPPL130S)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchList">조회</button>
        <button class="btn-erp btn-generate" @click="generateRequirement">소요량 산출</button>
        <button class="btn-erp btn-excel" @click="exportExcel">엑셀</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2">
      <!-- 🅰️ 조회 조건 영역 -->
      <div class="card border shadow-sm overflow-hidden">
        <div class="card-body p-0">
          <table class="erp-table-full">
            <tbody>
              <tr>
                <th class="required">기준일자</th>
                <td>
                  <div class="d-flex align-items-center gap-2">
                    <input v-model="uiymd" type="date" class="form-control form-control-sm" style="width: 150px;" />
                    <span class="small text-muted">현재</span>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 🅱️ 안내 문구 -->
      <div class="alert alert-info py-1 px-3 mb-0 small border-0 shadow-none d-flex align-items-center">
        <i class="bi bi-info-circle-fill me-2"></i>
        <span>자재소요량 작업 후 자동조회됩니다. (주문소요량 - 투입량 + 현재고 + 입고예정량 - 안전재고 > 0)</span>
      </div>

      <!-- 🅲 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column">
        <div class="card-header bg-light py-1 px-3 border-bottom fw-bold small text-dark">
          <i class="bi bi-list-columns-reverse me-1"></i> 자재 소요 상세 내역
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
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

const now = new Date()
const initymd = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}`

// 1. 상태 관리
const searchData = reactive({
  ymd: initymd
})

const uiymd = computed({
  get: () => formatDateString(searchData.ymd, '-'),
  set: (v) => searchData.ymd = v.replace(/-/g, '')
})

const activeItemCount = ref(0)
const gridElement = ref<HTMLElement | null>(null)
let grid: Tabulator | null = null

// 2. 그리드 초기화
const initGrid = () => {
  if (gridElement.value) {
    grid = new Tabulator(gridElement.value, {
      layout: "fitColumns",
      height: "100%",
      placeholder: "조회된 데이터가 없습니다.",
      pagination: "remote",
      paginationSize: 20,
      columns: [
        { title: "품목코드", field: "itemcd", width: 100, hozAlign: "center" },
        { title: "품목명", field: "itemnm", minWidth: 200, cssClass: "fw-bold" },
        { title: "규격", field: "itsize", width: 120 },
        { title: "단위", field: "unit", width: 60, hozAlign: "center" },
        { title: "주문소요량", field: "reqqty", width: 100, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
        { title: "주문투입량", field: "tuqty", width: 100, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
        { title: "현재고", field: "stkqty", width: 100, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
        { title: "안전재고", field: "stock", width: 100, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
        { title: "미입고수량", field: "nonqty", width: 100, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
        {
          title: "예상부족재고", field: "fore_stock", width: 110, hozAlign: "right",
          formatter: "money", formatterParams: { precision: 0 }, cssClass: "text-danger fw-bold"
        },
        { title: "비고", field: "remark", width: 150 }
      ],
      ajaxURL: "/hsio/HSIO_020S_STR", // 백엔드 매핑된 경로 사용
      ajaxConfig: "POST",
      ajaxParams: () => ({
        actkind: 'S',
        cmpycd: authStore.cmpycd,
        ymd: searchData.ymd,
        userid: authStore.userid
      }),
      ajaxResponse: (url, params, response) => {
        // 예상 부족 재고 계산 로직 반영: ((reqqty - tuqty) + (stock - stkqty) - nonqty)
        const mapped = response.map((item: any) => ({
          ...item,
          fore_stock: (Number(item.reqqty || 0) - Number(item.tuqty || 0)) +
                      (Number(item.stock || 0) - Number(item.stkqty || 0)) -
                      Number(item.nonqty || 0)
        }))
        activeItemCount.value = mapped.length
        return mapped
      }
    })
  }
}

// 3. 비즈니스 로직
async function fetchList() {
  grid?.replaceData()
}

async function generateRequirement() {
  if (!confirm("자재소요량 작업처리를 하시겠습니까?")) return

  try {
    const res = await api.post('/hsio/HSIO_020S_STR', {
      actkind: 'B',
      cmpycd: authStore.cmpycd,
      ymd: searchData.ymd,
      userid: authStore.userid
    })

    if (res.data?.[0]?.ERRYN === 'Y') {
      vAlertError(res.data[0].MSG || '작업 중 오류 발생')
    } else {
      vAlert('작업이 정상적으로 완료되었습니다.')
      fetchList()
    }
  } catch (e) { vAlertError('서버 통신 오류') }
}

function initialize() {
  searchData.ymd = initymd
  grid?.clearData()
  activeItemCount.value = 0
}

const exportExcel = () => {
  grid?.download("xlsx", `자재소요량_${searchData.ymd}.xlsx`, { title: "자재소요량 현황" })
}

const formatDateString = (v: any, sep: string) => v && v.length === 8 ? `${v.substring(0, 4)}${sep}${v.substring(4, 6)}${sep}${v.substring(6, 8)}` : v

onMounted(() => {
  nextTick(() => {
    initGrid()
  })
})
</script>

<style scoped>
.hppl130s-wrapper { height: 100%; overflow: hidden; font-family: 'Pretendard', sans-serif; }
.btn-erp { padding: 4px 16px; border-radius: 4px; font-size: 12.5px; font-weight: 700; cursor: pointer; transition: all 0.2s; }
.btn-init { background-color: #fff !important; color: #6c757d !important; border: 1px solid #6c757d !important; }
.btn-search { background-color: #2d3748 !important; color: #fff !important; border: none !important; }
.btn-generate { background-color: #f54508 !important; color: #fff !important; border: none !important; }
.btn-excel { background-color: #198754 !important; color: #fff !important; border: none !important; }

.erp-table-full { width: 100%; border-collapse: collapse; table-layout: fixed; border: 1px solid #dee2e6; }
.erp-table-full th { width: 100px; background-color: #f8f9fa; border: 1px solid #dee2e6; text-align: center; font-weight: 700; font-size: 12px; padding: 8px !important; color: #495057; }
.erp-table-full td { border: 1px solid #dee2e6; padding: 6px 12px !important; background-color: #fff; vertical-align: middle; }
.required::after { content: ' *'; color: #dc3545; }
</style>
