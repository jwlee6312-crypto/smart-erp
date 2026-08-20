<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="hppl120u-wrapper d-flex flex-column h-100 bg-white p-0">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-calendar-event-fill me-2 text-primary" style="font-size: 18px;"></i>
        생산정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        <span class="text-primary fw-bolder">일일생산계획등록 (HPPL120U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchList">조회</button>
        <button class="btn-erp btn-save" @click="saveData">저장</button>
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
                <th class="required">계획일자</th>
                <td>
                  <input v-model="uiymd" type="date" class="form-control form-control-sm" style="width: 150px;" @change="fetchList" />
                </td>
                <th class="required">생산라인</th>
                <td>
                  <select v-model="searchData.linecd" class="form-select form-select-sm" style="width: 200px;" @change="onLineChange">
                    <option value="">라인 선택</option>
                    <option v-for="opt in lineOptions" :key="opt.linecd" :value="opt.linecd">
                      [{{ opt.linecd }}] {{ opt.linenm }}
                    </option>
                  </select>
                </td>
                <th class="required">생산공정</th>
                <td>
                  <select v-model="searchData.progcd" class="form-select form-select-sm" style="width: 200px;" @change="fetchList">
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
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column">
        <div class="card-header bg-light py-1 px-3 border-bottom d-flex justify-content-between align-items-center">
          <span class="fw-bold small text-dark"><i class="bi bi-list-task me-1"></i> 생산 계획 품목</span>
          <div class="small text-muted">항목 선택 후 계획수량을 수정하고 저장 버튼을 누르세요.</div>
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
  ymd: initymd,
  linecd: '',
  progcd: ''
})

const lineOptions = ref<any[]>([])
const progOptions = ref<any[]>([])

const uiymd = computed({
  get: () => formatDateString(searchData.ymd, '-'),
  set: (v) => { if (v) searchData.ymd = v.replace(/-/g, '') }
})

const gridElement = ref<HTMLElement | null>(null)
let grid: Tabulator | null = null
const itemCount = ref(0)

// 🏭 라인 목록 로드
const fetchLineOptions = async () => {
  try {
    const res = await api.get('/hp00/HP00_000S_STR', {
      params: { gubun: 'L0', cmpycd: authStore.cmpycd, gbncd: 'Y', code: '' }
    })
    lineOptions.value = res.data
  } catch (e) {
    console.error('라인 목록 조회 실패', e)
  }
}

// ⚙️ 공정 목록 로드
const fetchProgOptions = async (lineCd: string) => {
  if (!lineCd) {
    progOptions.value = []
    return
  }
  try {
    const res = await api.get('/hp00/HP00_000S_STR', {
      params: { gubun: 'G0', cmpycd: authStore.cmpycd, gbncd: lineCd, code: '' }
    })
    progOptions.value = res.data
  } catch (e) {
    console.error('공정 목록 조회 실패', e)
  }
}

const onLineChange = () => {
  searchData.progcd = ''
  fetchProgOptions(searchData.linecd)
}

// 2. 그리드 초기화
const initGrid = () => {
  if (gridElement.value) {
    grid = new Tabulator(gridElement.value, {
      layout: "fitColumns",
      height: "100%",
      placeholder: "조회된 데이터가 없습니다.",
      selectable: true,
      columns: [
        { title: "선택", formatter: "rowSelection", titleFormatter: "rowSelection", width: 40, hozAlign: "center", vertAlign: "middle", headerSort: false },
        { title: "품목명", field: "itemnm", minWidth: 250 },
        { title: "규격", field: "itsize", width: 200, hozAlign: "center" },
        { title: "단위", field: "unit", width: 80, hozAlign: "center" },
        {
          title: "계획수량", field: "plnqty", width: 200, hozAlign: "right",
          editor: "number",
          formatter: (cell) => {
            const val = Number(cell.getValue() || 0)
            return new Intl.NumberFormat().format(val)
          }
        },
      ],
    })
  }
}

// 3. 비즈니스 로직
const fetchList = async () => {
  if (!searchData.linecd || !searchData.progcd) return

  try {
    const res = await api.post('/hppl/HPPL_120U_STR', {
      actkind: 'S0',
      cmpycd: authStore.cmpycd,
      linecd: searchData.linecd,
      progcd: searchData.progcd,
      ymd: searchData.ymd
    })
    grid?.setData(res.data)
    itemCount.value = res.data.length
    vAlert('조회되었습니다.')
  } catch (e) {
    vAlertError('조회 중 오류가 발생했습니다.')
  }
}

const saveData = async () => {
  const selectedData = grid?.getSelectedData() || []
  if (selectedData.length === 0) return vAlertError('저장할 항목을 선택하세요.')

  if (!confirm('선택한 품목의 일일생산계획을 저장하시겠습니까?')) return

  try {
    for (const item of selectedData) {
      await api.post('/hppl/HPPL_120U_STR', {
        actkind: 'A0',
        cmpycd: authStore.cmpycd,
        userid: authStore.userid,
        linecd: searchData.linecd,
        progcd: searchData.progcd,
        yymmDD: searchData.ymd,
        itemcd: item.itemcd,
        itsize: item.itsize,
        unit: item.unit,
        plnqty: item.plnqty
      })
    }
    vAlert('정상적으로 저장되었습니다.')
    fetchList()
  } catch (e) {
    vAlertError('저장 중 오류 발생')
  }
}

const initialize = () => {
  searchData.ymd = initymd
  searchData.linecd = ''
  searchData.progcd = ''
  progOptions.value = []
  grid?.clearData()
  itemCount.value = 0
}

const exportExcel = () => {
  grid?.download("xlsx", `일일생산계획_${searchData.ymd}.xlsx`, { title: "일일 생산 계획" })
}

const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

const formatDateString = (v: any, sep: string) => v && v.length === 8 ? `${v.substring(0, 4)}${sep}${v.substring(4, 6)}${sep}${v.substring(6, 8)}` : v

onMounted(() => {
  fetchLineOptions()
  nextTick(() => {
    initGrid()
  })
})
</script>

<style scoped>
.hppl120u-wrapper { height: 100%; overflow: hidden; font-family: 'Pretendard', sans-serif; }
.btn-erp { padding: 4px 16px; border-radius: 4px; font-size: 12.5px; font-weight: 700; cursor: pointer; transition: all 0.2s; }
.btn-init { background-color: #fff !important; color: #6c757d !important; border: 1px solid #6c757d !important; }
.btn-search { background-color: #2d3748 !important; color: #fff !important; border: none !important; }
.btn-save { background-color: #0056b3 !important; color: #fff !important; border: none !important; }
.btn-excel { background-color: #198754 !important; color: #fff !important; border: none !important; }

.erp-table-full { width: 100%; border-collapse: collapse; table-layout: fixed; border: 1px solid #dee2e6; }
.erp-table-full th { width: 100px; background-color: #f8f9fa; border: 1px solid #dee2e6; text-align: center; font-weight: 700; font-size: 12px; padding: 8px !important; color: #495057; }
.erp-table-full td { border: 1px solid #dee2e6; padding: 6px 12px !important; background-color: #fff; vertical-align: middle; }
.required::after { content: ' *'; color: #dc3545; }

/* Tabulator 체크박스 중앙 정렬 */
:deep(.tabulator-cell.tabulator-selectable) {
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
}
:deep(.tabulator-col.tabulator-selectable .tabulator-col-content) {
  padding: 0 !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
}
</style>
