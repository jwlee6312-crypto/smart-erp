<!--
	=============================================================
	프로그램명	: 제조원가등록(수작업) (HPCL110U)
	작성일자	: 2025.02.24
	설명        : 제품별 제조원가(단가/금액) 수동 등록 및 관리 (HPIO210U 표준 패턴 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-currency-dollar me-2 text-primary" style="font-size: 18px;"></i>
        생산정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">제조원가등록(수작업) (HPCL110U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchList">조회</button>
        <button class="btn-erp btn-save" @click="saveData">저장</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 조회 필터 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
                <col style="width: 10%" /><col style="width: 25%" />
                <col style="width: 65%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light required">입고연월</th>
                <td>
                  <div class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                    <select v-model="searchData.yy" class="form-select form-select-sm" style="width: 100px;">
                      <option v-for="year in yearOptions" :key="year" :value="year">{{ year }}년</option>
                    </select>
                    <select v-model="searchData.mm" class="form-select form-select-sm" style="width: 80px;">
                      <option v-for="month in monthOptions" :key="month" :value="month">{{ month }}월</option>
                    </select>
                  </div>
                </td>
                <td class="text-muted small ps-3">
                  <i class="bi bi-info-circle me-1"></i> 생산 월마감(HPCL100U) 작업이 완료된 월에 대해서만 원가 등록이 가능합니다.
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column grid-container-right">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark"><i class="bi bi-list-check me-2 text-primary"></i>제품별 제조원가 리스트</span>
          <div class="small text-muted">※ 단가 또는 금액을 수정하면 자동으로 계산되어 반영됩니다.</div>
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
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { getDate } from '@/composables/useDate'
import AppAlert from '@/components/AppAlert.vue'

const authStore = useAuthStore()
const { today } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

const now = new Date()

// [1] 데이터 모델링
const searchData = reactive({
  yy: now.getFullYear().toString(),
  mm: String(now.getMonth() + 1).padStart(2, '0')
})

const yearOptions = ref<string[]>([])
const monthOptions = ['01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12']
const closingInfo = reactive({ clsymd: '', sclsym: '', PCLSym: '', wclsym: '' })

const tableRef = ref<HTMLDivElement | null>(null)
let grid: Tabulator | null = null

// [2] 그리드 초기화
const initGrids = () => {
  grid = new Tabulator(tableRef.value!, {
    layout: "fitColumns", height: "100%", placeholder: "데이터 없음", selectable: true,
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "선택", width: 40, hozAlign: "center", formatter: "rowSelection", titleFormatter: "rowSelection" },
      { title: "제품코드", field: "itemcd", width: 120, hozAlign: "center" },
      { title: "제품명", field: "itemnm", minWidth: 200, widthGrow: 1, cssClass: "fw-bold" },
      { title: "규격", field: "itsize", width: 150 },
      { title: "단위", field: "unit", width: 80, hozAlign: "center" },
      { title: "수량", field: "inqty", width: 100, hozAlign: "right", formatter: "money", formatterParams: { precision: (c:any)=>c.getData().qtypnt||0 } },
      { title: "단가", field: "price", width: 120, hozAlign: "right", editor: "number", formatter: "money", formatterParams: { precision: 2 }, cssClass: "bg-light-yellow fw-bold" },
      { title: "금액", field: "Inamt", width: 130, hozAlign: "right", editor: "number", formatter: "money", formatterParams: { precision: 0 }, cssClass: "bg-light-blue fw-bold" }
    ],
  });

  grid.on("cellEdited", (cell) => {
    const field = cell.getField()
    const data = cell.getData()
    const qty = Number(data.inqty || 0)

    if (field === 'price') {
      cell.getRow().update({ Inamt: Math.round(qty * Number(data.price || 0)) })
    } else if (field === 'Inamt') {
      if (qty !== 0) {
        cell.getRow().update({ price: Number((Number(data.Inamt || 0) / qty).toFixed(2)) })
      }
    }
    cell.getRow().select()
  })
}

// [3] 비즈니스 로직
const fetchClosingStatus = async () => {
  try {
    const res = await api.get('/hp00/HP00_000S_STR', { params: { gubun: 'CL', cmpycd: authStore.cmpycd } })
    if (res.data?.length) {
      const d = res.data[0]
      closingInfo.PCLSym = String(d.PCLSym || d.pclsym || '').trim()
      if (closingInfo.PCLSym.length === 6) {
        searchData.yy = closingInfo.PCLSym.substring(0, 4)
        searchData.mm = closingInfo.PCLSym.substring(4, 6)
      }
    }
  } catch (e) {}
}

async function fetchList() {
  try {
    const res = await api.post('/hpcl/HPCL_110U_STR', {
      actkind: 'S', cmpycd: authStore.cmpycd, ym: searchData.yy + searchData.mm
    })
    const mapped = res.data.map((item: any) => ({
      ...item,
      price: Number(item.inqty) !== 0 ? Number((Number(item.Inamt) / Number(item.inqty)).toFixed(2)) : 0
    }))
    grid?.setData(mapped)
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

async function saveData() {
  const ym = searchData.yy + searchData.mm
  if (ym > closingInfo.PCLSym) {
    return vAlertError(`생산정보 마감작업(${closingInfo.PCLSym}) 후 작업하시기 바랍니다.`)
  }

  const selected = grid?.getSelectedData() || []
  if (selected.length === 0) return vAlertError('저장할 항목을 선택하세요.')

  if (!confirm('선택한 품목의 제조원가를 저장하시겠습니까?')) return

  try {
    for (const item of selected) {
      await api.post('/hpcl/HPCL_110U_STR', {
        actkind: 'U0', cmpycd: authStore.cmpycd, ym: ym,
        itemcd: item.itemcd, inqty: item.inqty, Inamt: item.Inamt, userid: authStore.userid
      })
    }
    vAlert('저장되었습니다.'); fetchList()
  } catch (e) { vAlertError('저장 처리 중 오류 발생') }
}

const initialize = () => {
  const curYear = new Date().getFullYear();
  yearOptions.value = Array.from({ length: 5 }, (_, i) => String(curYear - i));
  fetchClosingStatus();
  grid?.clearData();
}

onMounted(() => {
  initialize();
  nextTick(initGrids);
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
.grid-container-right { border-bottom: 3px solid #005a9f !important; }
:deep(.bg-light-yellow) { background-color: #fffde7 !important; }
:deep(.bg-light-blue) { background-color: #e3f2fd !important; }
</style>
