<!--
	=============================================================
	프로그램명	: 품목별생산계획 (HPPL100U)
	작성일자	: 2025.02.24
	설명        : 생산 라인/공정별 월간 생산 계획 수립 및 관리 (HPIO210U 표준 패턴 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-calendar-check me-2 text-primary" style="font-size: 18px;"></i>
        생산관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        생산계획 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">품목별생산계획 (HPPL100U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchList">조회</button>
        <button class="btn-erp btn-save" @click="saveData">저장</button>
        <button class="btn-erp btn-excel" @click="exportExcel">엑셀</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 조회 필터 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
                <col style="width: 10%" /><col style="width: 23%" />
                <col style="width: 10%" /><col style="width: 23%" />
                <col style="width: 10%" /><col style="width: 24%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light required">연 월</th>
                <td>
                  <input v-model="yymm_f" type="month" class="form-control form-control-sm" style="max-width: 150px;" @change="onMonthChange" />
                </td>
                <th class="text-center bg-light required">생산라인</th>
                <td>
                  <select v-model="searchForm.linecd" class="form-select form-select-sm" @change="onLineChange">
                    <option value="">라인 선택</option>
                    <option v-for="opt in lineOptions" :key="opt.linecd" :value="opt.linecd">
                      [{{ opt.linecd }}] {{ opt.linenm }}
                    </option>
                  </select>
                </td>
                <th class="text-center bg-light required">생산공정</th>
                <td>
                  <select v-model="searchForm.progcd" class="form-select form-select-sm" @change="fetchList">
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

      <!-- [하단] 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column grid-container-right">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>생산 계획 명세</span>
          <div class="d-flex align-items-center gap-3">
             <div class="form-check form-check-inline m-0 small">
               <input class="form-check-input" type="checkbox" id="checkAll" v-model="allSelected" @change="toggleAllSelection">
               <label class="form-check-label text-muted" for="checkAll">전체선택</label>
             </div>
             <span class="text-muted small">※ 일자별 계획 수량을 입력 후 저장하세요.</span>
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
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { useCommonHelp } from '@/composables/useCommonHelp'
import { getDate } from '@/composables/useDate'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'

const authStore = useAuthStore()
const { today } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps } = useCommonHelp()

// [1] 데이터 모델링
const searchForm = reactive({
  yymm: today.replace(/-/g, '').substring(0, 6),
  linecd: '',
  progcd: ''
})

const yymm_f = computed({
  get: () => searchForm.yymm ? `${searchForm.yymm.substring(0, 4)}-${searchForm.yymm.substring(4, 6)}` : '',
  set: (v) => { if (v) searchForm.yymm = v.replace(/-/g, '') }
})

const lineOptions = ref<any[]>([])
const progOptions = ref<any[]>([])
const gridElement = ref<HTMLElement | null>(null)
let grid: Tabulator | null = null
const lastDay = ref(0)
const allSelected = ref(false)

// [2] 기초 데이터 로드
const fetchLineOptions = async () => {
  try {
    const res = await api.get('/hp00/HP00_000S_STR', {
      params: { gubun: 'L0', cmpycd: authStore.cmpycd, gbncd: 'Y', code: '' }
    })
    lineOptions.value = res.data
  } catch (e) { console.error(e) }
}

const fetchProgOptions = async (lineCd: string) => {
  if (!lineCd) { progOptions.value = []; return; }
  try {
    const res = await api.get('/hp00/HP00_000S_STR', {
      params: { gubun: 'G0', cmpycd: authStore.cmpycd, gbncd: lineCd, code: '' }
    })
    progOptions.value = res.data
  } catch (e) { console.error(e) }
}

const onLineChange = () => {
  searchForm.progcd = ''
  fetchProgOptions(searchForm.linecd)
}

const onMonthChange = () => {
  if (grid) grid.setColumns(generateColumns())
}

// [3] 그리드 설정
const generateColumns = () => {
  const year = parseInt(searchForm.yymm.substring(0, 4))
  const month = parseInt(searchForm.yymm.substring(4, 6))
  const lastDate = new Date(year, month, 0).getDate()
  const weekDays = ['일', '월', '화', '수', '목', '금', '토']

  lastDay.value = lastDate

  const columns: any[] = [
    { title: "선택", field: "procyn", formatter: "tickCross", formatterParams: { crossElement: false }, editor: true, width: 50, hozAlign: "center", frozen: true, headerSort: false },
    {
      title: "품목 정보",
      frozen: true,
      columns: [
        { title: "품목명", field: "itemnm", width: 200, headerSort: false },
        { title: "합계", field: "qty_tt", width: 90, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "bg-light fw-bold", headerSort: false }
      ]
    }
  ]

  const dateColumns: any[] = []
  for (let d = 1; d <= lastDate; d++) {
    const curDate = new Date(year, month - 1, d)
    const ww = weekDays[curDate.getDay()]
    const fieldName = `mm${String(d - 1).padStart(2, '0')}`

    dateColumns.push({
      title: `${d}일\n(${ww})`,
      field: fieldName,
      width: 70,
      hozAlign: "right",
      editor: "number",
      headerSort: false,
      formatter: (cell: any) => Number(cell.getValue()) === 0 ? "" : Number(cell.getValue()).toLocaleString(),
      cellEdited: (cell: any) => {
        const row = cell.getRow()
        const data = row.getData()
        let sum = 0
        for (let i = 0; i < lastDay.value; i++) {
          sum += Number(data[`mm${String(i).padStart(2, '0')}`] || 0)
        }
        row.update({ qty_tt: sum, procyn: true })
      }
    })
  }

  columns.push({
    title: `${year}년 ${month}월 생산계획`,
    columns: dateColumns
  })

  return columns
}

const initGrid = () => {
  if (!gridElement.value) return
  grid = new Tabulator(gridElement.value, {
    layout: "fitData",
    height: "100%",
    placeholder: "조회 조건을 선택 후 조회하세요.",
    headerSort: false,
    columns: generateColumns()
  })
}

// [4] 비즈니스 로직
const fetchList = async () => {
  if (!searchForm.linecd || !searchForm.progcd) return

  try {
    const res = await api.post('/hppl/HPPL_100U_STR', {
      actkind: 'S0',
      cmpycd: authStore.cmpycd,
      yymm: searchForm.yymm,
      linecd: searchForm.linecd,
      progcd: searchForm.progcd
    })

    const mappedData = res.data.map((item: any) => {
      const row = { ...item, procyn: null }
      for (let i = 0; i < lastDay.value; i++) {
        const d = i + 1
        const val = item[String(d)] ?? item[String(d).padStart(2, '0')] ?? 0
        row[`mm${String(i).padStart(2, '0')}`] = Number(val)
      }
      return row
    })

    grid?.setData(mappedData)
    allSelected.value = false
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

const saveData = async () => {
  const selectedData = grid?.getData().filter((r: any) => r.procyn === true) || []
  if (selectedData.length === 0) return vAlertError('저장할 항목을 선택하세요.')

  if (!confirm('생산계획을 저장하시겠습니까?')) return

  try {
    for (const row of selectedData) {
      for (let d = 0; d < lastDay.value; d++) {
        const dayStr = String(d + 1).padStart(2, '0')
        const qty = Number(row[`mm${String(d).padStart(2, '0')}`] || 0)

        await api.post('/hppl/HPPL_100U_STR', {
          actkind: 'A0',
          cmpycd: authStore.cmpycd,
          linecd: searchForm.linecd,
          progcd: searchForm.progcd,
          yymmDD: searchForm.yymm + dayStr,
          itemcd: row.itemcd,
          plnqty: qty,
          userid: authStore.userid
        })
      }
    }
    vAlert('정상적으로 저장되었습니다.')
    fetchList()
  } catch (e) { vAlertError('저장 오류') }
}

const toggleAllSelection = () => {
  if (!grid) return
  const data = grid.getData()
  grid.updateData(data.map(i => ({ ...i, procyn: allSelected.value ? true : null })))
}

const initialize = () => {
  resetForm(searchForm)
  searchForm.yymm = today.replace(/-/g, '').substring(0, 6)
  progOptions.value = []
  grid?.clearData()
  allSelected.value = false
  if (grid) grid.setColumns(generateColumns())
}

const exportExcel = () => grid?.download("xlsx", `생산계획_${searchForm.yymm}.xlsx`)

onMounted(() => {
  fetchLineOptions()
  nextTick(initGrid)
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
.grid-container-right { border-bottom: 3px solid #005a9f !important; }
</style>
