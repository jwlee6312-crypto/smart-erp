<!--
	=============================================================
	프로그램명	: 재료비 현황 (HFMF219S)
	작성일자	: 2025.02.24
	설명        : 자재별/제품별 재료비 투입 상세 현황 조회 (HSOD100U 표준 UI 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-box-seam me-2 text-primary" style="font-size: 18px;"></i>
        원가관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        현황조회 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">재료비 현황 (HFMF219S)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-search" @click="handleSearch">조회</button>
        <button class="btn-erp btn-excel" @click="handleExport">엑셀</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 조회 필터 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
                <col style="width: 10%" /><col style="width: 15%" />
                <col style="width: 10%" /><col style="width: 30%" />
                <col style="width: 10%" /><col style="width: 25%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light required">조회년월</th>
                <td>
                  <input v-model="searchForm.ym" type="month" class="form-control form-control-sm" @change="handleSearch" />
                </td>
                <th class="text-center bg-light">자재구분</th>
                <td class="px-2">
                  <div class="d-flex gap-3 mt-1">
                    <div class="form-check">
                      <input v-model="searchForm.jsgbn" class="form-check-input" type="radio" value="100" id="js100" @change="handleSearch">
                      <label class="form-check-label small" for="js100">원재료</label>
                    </div>
                    <div class="form-check">
                      <input v-model="searchForm.jsgbn" class="form-check-input" type="radio" value="112" id="js112" @change="handleSearch">
                      <label class="form-check-label small" for="js112">구매품</label>
                    </div>
                    <div class="form-check">
                      <input v-model="searchForm.jsgbn" class="form-check-input" type="radio" value="TOT" id="jsTot" @change="handleSearch">
                      <label class="form-check-label small" for="jsTot">전체</label>
                    </div>
                  </div>
                </td>
                <th class="text-center bg-light">조회구분</th>
                <td class="px-2">
                  <div class="d-flex gap-3 mt-1">
                    <div class="form-check">
                      <input v-model="searchForm.mmgbn" class="form-check-input" type="radio" value="M" id="mmM" @change="handleSearch">
                      <label class="form-check-label small" for="mmM">월계</label>
                    </div>
                    <div class="form-check">
                      <input v-model="searchForm.mmgbn" class="form-check-input" type="radio" value="N" id="mmN" @change="handleSearch">
                      <label class="form-check-label small" for="mmN">누계</label>
                    </div>
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
          <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>자재별/제품별 재료비 상세 현황</span>
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
import AppAlert from '@/components/AppAlert.vue'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()

const searchForm = reactive({
  ym: new Date().toISOString().substring(0, 7),
  jsgbn: '100',
  mmgbn: 'M'
})

const mainGridRef = ref<HTMLElement | null>(null)
let mainGrid: Tabulator | null = null

const initGrids = () => {
  if (mainGridRef.value) {
    mainGrid = new Tabulator(mainGridRef.value, {
      layout: 'fitColumns',
      height: '100%',
      placeholder: "데이터 없음",
      columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
      columns: [
        { title: "No", formatter: "rownum", width: 40, hozAlign: "center" },
        { title: '자재명', field: 'jajenm', widthGrow: 2, hozAlign: 'left', frozen: true,
          formatter: (cell) => {
            const d = cell.getData();
            return d.jajecd ? `<span class="fw-bold text-primary">${d.jajecd}</span> - ${d.jajenm || ''}` : '';
          }
        },
        { title: '자재단위', field: 'jajeunit', width: 80, hozAlign: 'center' },
        { title: '제품명', field: 'itemnm', widthGrow: 2, hozAlign: 'left',
          formatter: (cell) => {
            const d = cell.getData();
            return d.itemcd ? `<span class="fw-bold text-dark">${d.itemcd}</span> - ${d.itemnm || ''}` : '';
          }
        },
        { title: '제품단위', field: 'unit', width: 80, hozAlign: 'center' },
        { title: '생산량', field: 'proqty', width: 100, hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 } },
        { title: '투입량', field: 'prqqty', width: 100, hozAlign: 'right', formatter: 'money', formatterParams: { precision: 2 } },
        { title: '단가', field: 'price', width: 110, hozAlign: 'right', formatter: 'money', formatterParams: { precision: 2 },
          mutatorData: (value, data) => (Number(data.matlcost) > 0 && Number(data.prqqty) > 0) ? (Number(data.matlcost) / Number(data.prqqty)) : 0
        },
        { title: '재료비', field: 'matlcost', width: 130, hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 }, cssClass: 'fw-bold text-primary bg-light' }
      ]
    })
  }
}

const handleSearch = async () => {
  try {
    const ym = searchForm.ym.replace('-', '')
    const { data } = await api.post('/hfmf/FMF2190R_STR', {
      cmpycd: authStore.cmpycd,
      ym: ym,
      jsgbn: searchForm.jsgbn,
      mmgbn: searchForm.mmgbn,
      actkind: 'S0',
      costcd: '10000'
    })
    mainGrid?.setData(data || [])
    vAlert('조회되었습니다.')
  } catch (e) {
    vAlertError('조회 실패')
  }
}

const handleExport = () => {
  mainGrid?.download('xlsx', `재료비현황_${searchForm.ym}.xlsx`)
}

onMounted(() => {
  nextTick(() => {
    initGrids()
    handleSearch()
  })
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
