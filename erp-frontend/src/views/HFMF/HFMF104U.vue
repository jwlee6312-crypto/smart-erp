<!--
	=============================================================
	프로그램명	: 제조비용 등록 (HFMF104U)
	작성일자	: 2025.02.24
	설명        : 제조비용 등록 및 관리 (HSOD100U 표준 그리드 및 UI 규격 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-cash-stack me-2 text-primary" style="font-size: 18px;"></i>
        원가관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        기준정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">제조비용 등록 (HFMF104U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-2">
        <button class="btn-erp btn-init" @click="handleReset">초기화</button>
        <button class="btn-erp btn-search" @click="handleSearch">조회</button>
        <button class="btn-erp btn-save" @click="save">저장</button>
        <button class="btn-erp btn-delete" @click="deleteData" :disabled="detailForm.mode !== 'U'">삭제</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 입력 폼 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense w-100">
            <colgroup>
              <col style="width: 100px;" /><col />
              <col style="width: 100px;" /><col />
              <col style="width: 100px;" /><col />
            </colgroup>
            <tbody>
              <tr>
                <th class="required bg-light text-center">년 월</th>
                <td><input v-model="searchForm.ym" type="month" class="form-control form-control-sm" style="max-width: 150px;" @change="handleSearch" /></td>
                <th class="bg-light text-center">계정과목</th>
                <td>
                  <input v-model="detailForm.acctnm" class="form-control form-control-sm bg-light fw-bold text-dark" readonly placeholder="그리드에서 선택하세요" />
                </td>
                <th class="required bg-light text-center">비용금액</th>
                <td><input v-model="detailForm.costamt" type="number" class="form-control form-control-sm text-end fw-bold text-primary" placeholder="0" /></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column grid-container-right">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>제조비용 목록</span>
          <span class="text-muted small" style="font-size: 11px;">* 행 클릭 시 상단 폼에 자동 입력됩니다.</span>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="mainGridRef" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import AppAlert from '@/components/AppAlert.vue'
import { useAuthStore } from '@/stores/authStore'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()

const searchForm = reactive({ ym: new Date().toISOString().substring(0, 7) })
const detailForm = reactive({ ACCT: '', acctnm: '', costamt: 0, mode: 'N' })

const mainGridRef = ref<HTMLElement | null>(null)
let mainGrid: Tabulator | null = null

const handleReset = () => {
  Object.assign(detailForm, { ACCT: '', acctnm: '', costamt: 0, mode: 'N' });
  mainGrid?.deselectRow();
}

const handleSearch = async () => {
  try {
    const ym = searchForm.ym.replace('-', '')
    const payload = {
      cmpycd: authStore.cmpycd,
      actkind: 'S0',
      ym: ym,
      costcd: '10000',
      ACCT: '',
      costamt: '0',
      userid: authStore.userid
    }
    const { data } = await api.post('/hfmf/FMF1040U_STR', payload)
    mainGrid?.setData(data)
    handleReset()
    vAlert('조회되었습니다.')
  } catch (e) {
    vAlertError('조회 실패')
  }
}

const save = async () => {
  if (!detailForm.ACCT) return vAlertError('항목을 그리드에서 선택하세요.')
  if (!confirm('저장하시겠습니까?')) return
  try {
    const payload = {
      cmpycd: authStore.cmpycd,
      actkind: 'A0',
      ym: searchForm.ym.replace('-', ''),
      costcd: '10000',
      ACCT: String(detailForm.ACCT || ''),
      costamt: String(detailForm.costamt || '0'),
      userid: authStore.userid
    }
    await api.post('/hfmf/FMF1040U_STR', payload)
    vAlert('저장되었습니다.')
    handleSearch()
  } catch (e) {
    vAlertError('저장 실패')
  }
}

const deleteData = async () => {
  if (!detailForm.ACCT) return vAlertError('삭제할 항목을 그리드에서 선택하세요.')
  if (!confirm('정말 삭제하시겠습니까?')) return
  try {
    const payload = {
      cmpycd: authStore.cmpycd,
      actkind: 'D0',
      ym: searchForm.ym.replace('-', ''),
      costcd: '10000',
      ACCT: String(detailForm.ACCT || ''),
      costamt: '0',
      userid: authStore.userid
    }
    await api.post('/hfmf/FMF1040U_STR', payload)
    vAlert('삭제되었습니다.')
    handleSearch()
  } catch (e) {
    vAlertError('삭제 실패')
  }
}

onMounted(() => {
  if (mainGridRef.value) {
    mainGrid = new Tabulator(mainGridRef.value, {
      layout: 'fitColumns',
      height: '100%',
      selectable: 1,
      columnCalcs: "table",
      columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
      columns: [
        { title: '계정코드', field: 'ACCT', hozAlign: 'center', width: 120 },
        { title: '계정과목명', field: 'acctnm', widthGrow: 2, hozAlign: 'left', bottomCalc: () => "합계", cssClass: 'fw-bold text-primary' },
        { title: '비용', field: 'DIRcamt', hozAlign: 'right', width: 150, formatter: 'money', formatterParams: { precision: 0 }, bottomCalc: "sum" },
        { title: '비고', field: 'bigo', widthGrow: 3, hozAlign: 'left' }
      ]
    })

    mainGrid.on('rowClick', (e, row) => {
      const data = row.getData()
      detailForm.ACCT = String(data.ACCT || data.acct || '').trim()
      detailForm.acctnm = String(data.acctnm || data.acctnm || '').trim()
      detailForm.costamt = Number(data.DIRcamt || data.dircamt || 0)
      detailForm.mode = 'U'
      mainGrid?.deselectRow()
      row.select()
    })
  }
  handleSearch()
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
