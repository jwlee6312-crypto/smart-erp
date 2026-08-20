<!--
	=============================================================
	프로그램명	: 원가마감정보등록 (HFBA301U)
	작성일자	: 2025.02.24
	설명        : 월별 원가 마감 상태 등록 및 관리 (HPIO210U 표준 패턴 적용)
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
        원가관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        기준정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">원가마감정보등록 (HFBA301U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="handleSearch">조회</button>
        <button class="btn-erp btn-save" @click="save">저장</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 조회 필터 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
                <col style="width: 10%" /><col style="width: 20%" />
                <col style="width: 70%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light required">적용년도</th>
                <td>
                  <select v-model="searchForm.year" class="form-select form-select-sm" @change="handleSearch">
                    <option v-for="y in yearOptions" :key="y" :value="y">{{ y }}년</option>
                  </select>
                </td>
                <td class="ps-3 border-start-0">
                  <div class="alert alert-warning py-1 px-3 mb-0 small border-0 d-inline-block" style="background-color: transparent;">
                    <i class="bi bi-info-circle-fill me-2 text-warning"></i>원가마감월 체크 후 저장처리 => 수불부원가생성 => 영업매출원가 확인할 것.
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column grid-container-right">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>연간 원가 마감 상태 목록</span>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="mainGridRef" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { useCommonHelp } from '@/composables/useCommonHelp'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps } = useCommonHelp()

// [1] 데이터 모델링
const yearOptions = ref<string[]>([])
const searchForm = reactive({
  year: new Date().getFullYear().toString()
})

const mainGridRef = ref<HTMLElement | null>(null)
let mainGrid: Tabulator | null = null

// [2] 그리드 초기화
const initGrids = () => {
  if (mainGridRef.value) {
    mainGrid = new Tabulator(mainGridRef.value, {
      layout: "fitColumns", height: '100%', selectable: true,
      columnDefaults: { headerSort: false, headerHozAlign: "center", vertAlign: "middle" },
      columns: [
        {
          title: '마감확정', field: '_sel', width: 100, hozAlign: 'center',
          formatter: 'rowSelection', titleFormatter: 'rowSelection'
        },
        {
          title: '마감년월', field: 'yymm', width: 150, hozAlign: 'center',
          cssClass: 'fw-bold text-primary',
          formatter: (cell) => {
            const val = cell.getValue();
            return val ? `${val.substring(0, 4)}-${val.substring(4, 6)}` : '';
          }
        },
        { title: '비고', field: 'remark', widthGrow: 1, hozAlign: 'left', editor: 'input' }
      ]
    })
  }
}

// [3] 비즈니스 로직
const handleSearch = async () => {
  try {
    const yymm = searchForm.year + "01"
    const { data } = await api.post('/hfba/FBA3010U_STR', {
      cmpycd: authStore.cmpycd,
      yymm: yymm,
      actkind: 'S0'
    })

    if (data) {
      mainGrid?.setData(data)
      nextTick(() => {
        mainGrid?.getRows().forEach(row => {
          if (row.getData().yn === 'Y' || row.getData().YN === 'Y') row.select()
        })
      })
      vAlert('조회되었습니다.')
    }
  } catch (e) { vAlertError('조회 실패') }
}

const save = async () => {
  const allRows = mainGrid?.getRows() || []
  if (allRows.length === 0) return

  if (!confirm('원가 마감 정보를 저장하시겠습니까?')) return

  try {
    for (const row of allRows) {
      const d = row.getData()
      const isSelected = row.isSelected()
      const actkind = isSelected ? 'A0' : 'D0'

      await api.post('/hfba/FBA3010U_STR', {
        cmpycd: authStore.cmpycd,
        yymm: d.yymm,
        yn: isSelected ? 'Y' : 'N',
        remark: d.remark || '',
        actkind: actkind,
        userid: authStore.userid
      })
    }
    vAlert('성공적으로 저장되었습니다.')
    handleSearch()
  } catch (e) { vAlertError('저장 오류') }
}

const initialize = () => {
  searchForm.year = new Date().getFullYear().toString()
  mainGrid?.clearData()
  handleSearch()
}

onMounted(() => {
  const curYear = new Date().getFullYear()
  for (let i = 0; i < 5; i++) yearOptions.value.push((curYear - i).toString())
  nextTick(() => {
    initGrids()
    handleSearch()
  })
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
.grid-container-right { border-bottom: 3px solid #005a9f !important; }
</style>
