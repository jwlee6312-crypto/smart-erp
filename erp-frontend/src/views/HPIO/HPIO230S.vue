<!--
	=============================================================
	프로그램명	: 생산지시현황 (HPIO230S)
	작성일자	: 2025.02.24
	설명        : 생산 공정별 지시 내역 및 실적 현황 조회 (HPIO210U 표준 패턴 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-file-earmark-bar-graph me-2 text-primary" style="font-size: 18px;"></i>
        생산정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">생산지시현황 (HPIO230S)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchList">조회</button>
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
                <col style="width: 10%" /><col style="width: 30%" />
                <col style="width: 10%" /><col style="width: 50%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light required">생산라인</th>
                <td>
                  <select v-model="searchData.linecd" class="form-select form-select-sm" style="max-width: 250px;" @change="fetchList">
                    <option value="">라인 선택</option>
                    <option v-for="opt in lineOptions" :key="opt.linecd" :value="opt.linecd">
                      [{{ opt.linecd }}] {{ opt.linenm }}
                    </option>
                  </select>
                </td>
                <th class="text-center bg-light required">지시일자</th>
                <td class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                  <input v-model="fromdt" type="date" class="form-control form-control-sm" style="width: 140px;" />
                  <span class="px-1 opacity-50">~</span>
                  <input v-model="todt" type="date" class="form-control form-control-sm" style="width: 140px;" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column grid-container-right">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark"><i class="bi bi-list-ul me-2 text-primary"></i>생산 지시 목록</span>
          <span v-if="rowCount" class="badge bg-secondary-subtle text-dark border border-secondary-subtle" style="font-size: 10px;">Total: {{ rowCount }}건</span>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="tableRef" class="tabulator-instance flex-grow-1"></div>
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
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { getDate } from '@/composables/useDate'
import AppAlert from '@/components/AppAlert.vue'

const authStore = useAuthStore()
const { today, firstDay } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

// [1] 데이터 모델링
const searchData = reactive({
  linecd: '010',
  fromdt: firstDay.replace(/-/g, ''),
  todt: today.replace(/-/g, '')
})

const lineOptions = ref<any[]>([])
const rowCount = ref(0)

const fromdt = computed({
  get: () => searchData.fromdt && searchData.fromdt.length === 8 ? `${searchData.fromdt.substring(0, 4)}-${searchData.fromdt.substring(4, 6)}-${searchData.fromdt.substring(6, 8)}` : '',
  set: (v) => { if (v) searchData.fromdt = v.replace(/-/g, '') }
})
const todt = computed({
  get: () => searchData.todt && searchData.todt.length === 8 ? `${searchData.todt.substring(0, 4)}-${searchData.todt.substring(4, 6)}-${searchData.todt.substring(6, 8)}` : '',
  set: (v) => { if (v) searchData.todt = v.replace(/-/g, '') }
})

const tableRef = ref<HTMLDivElement | null>(null)
let grid: Tabulator | null = null

// [2] 그리드 초기화
const initGrids = () => {
  grid = new Tabulator(tableRef.value!, {
    layout: "fitColumns", height: "100%", placeholder: "데이터 없음",
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "지시일자", field: "ordymd", width: 100, hozAlign: "center", formatter: (c) => formatDate(c.getValue()) },
      { title: "LOT NO.", field: "lot_disp", width: 150, hozAlign: "center", cssClass: "fw-bold text-primary" },
      { title: "생산공정", field: "prognm", width: 120 },
      { title: "품목명", field: "itemnm", minWidth: 200, widthGrow: 1, cssClass: "fw-bold" },
      { title: "규격", field: "itsize", width: 150 },
      { title: "단위", field: "unit", width: 70, hozAlign: "center" },
      { title: "지시수량", field: "ordqty", width: 100, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "text-primary fw-bold" },
      { title: "생산수량", field: "prodqty", width: 100, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "text-success fw-bold" },
      { title: "완료예정", field: "todt", width: 100, hozAlign: "center", formatter: (c) => formatDate(c.getValue()) },
      { title: "주문번호", field: "ord_disp", width: 130, hozAlign: "center", cssClass: "text-secondary" },
      { title: "납품처", field: "custnm", width: 150, hozAlign: "left" }
    ],
  });
}

// [3] 비즈니스 로직
const fetchLineOptions = async () => {
  try {
    const res = await api.get('/hp00/HP00_000S_STR', { params: { gubun: 'L0', cmpycd: authStore.cmpycd, gbncd: 'Y', code: '' } })
    lineOptions.value = res.data
  } catch (e) { console.error(e) }
}

async function fetchList() {
  if (!searchData.linecd) return vAlertError('생산라인을 선택하세요.')
  try {
    const res = await api.post("/hpio/HPIO_230S_STR", {
        cmpycd: authStore.cmpycd,
        linecd: searchData.linecd,
        fromdt: searchData.fromdt,
        todt: searchData.todt
    })

    const mapped = (res.data || []).map((item: any) => ({
        ...item,
        lot_disp: item.lotymd ? `${formatDate(item.lotymd)}-${item.lotno}` : '',
        ord_disp: item.ordym ? `${formatDate(item.ordym)}-${item.ordno}` : ''
    }))

    grid?.setData(mapped)
    rowCount.value = mapped.length
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

const initialize = () => {
  resetForm(searchData)
  Object.assign(searchData, { linecd: '010', fromdt: firstDay.replace(/-/g, ''), todt: today.replace(/-/g, '') })
  grid?.clearData()
  rowCount.value = 0
}

const exportExcel = () => {
  if (!grid || rowCount.value === 0) return vAlertError('조회된 데이터가 없습니다.')
  grid.download("xlsx", `생산지시현황_${searchData.fromdt}_${searchData.todt}.xlsx`, { sheetName: '생산지시' })
}

const formatDate = (v: any) => {
    if (!v) return ''
    const s = String(v).replace(/-/g, '')
    return s.length >= 6 ? `${s.substring(0, 4)}-${s.substring(4, 6)}${s.length === 8 ? '-' + s.substring(6, 8) : ''}` : s
}

onMounted(() => {
  if (typeof window !== 'undefined') (window as any).XLSX = XLSX
  fetchLineOptions()
  nextTick(initGrids)
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
.grid-container-right { border-bottom: 3px solid #005a9f !important; }
</style>
