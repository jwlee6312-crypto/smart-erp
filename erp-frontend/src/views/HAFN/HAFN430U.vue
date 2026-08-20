<!--
	=============================================================
	프로그램명	: 자금계획등록 (HAFN430U)
	작성일자	: 2025.02.24
	설명        : 특정 일자의 입금/출금 자금 계획 수동 등록 및 관리
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-pencil-square me-2 text-primary" style="font-size: 18px;"></i>
        자금관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">자금계획등록 (HAFN430U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-save" @click="save">저장</button>
      </div>
    </div>

    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
                <col style="width: 100px" /><col />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light required">계획일자</th>
                <td class="px-3 py-1">
                    <input v-model="searchForm.plnymd" type="date" class="form-control form-control-sm" style="width: 150px;" @change="search" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column grid-container-right">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>자금 계획 목록</span>
          <div class="d-flex gap-1">
            <button class="btn btn-sm btn-outline-primary py-0 px-2 fw-bold" @click="addRow" style="font-size: 11px;">+ 행추가</button>
          </div>
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
import AppAlert from '@/components/AppAlert.vue'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()

const searchForm = reactive({ plnymd: new Date().toISOString().substring(0, 10) })
const mainGridRef = ref<HTMLDivElement | null>(null)
let mainGrid: Tabulator | null = null

const initGrid = () => {
  if (!mainGridRef.value) return
  mainGrid = new Tabulator(mainGridRef.value, {
    layout: "fitColumns", height: "100%", placeholder: "계획 없음",
    columnDefaults: { headerSort: false, headerHozAlign: "center", vertAlign: "middle" },
    columns: [
      { title: "No", formatter: "rownum", width: 40, hozAlign: "center" },
      { title: "구분", field: "inoutgbn", width: 90, editor: "list", editorParams: { values: { "1": "입금", "2": "출금" } }, formatter: (c) => c.getValue() === "1" ? "입금" : "출금" },
      { title: "계정과목", field: "acctnm", width: 150, editor: "input" },
      { title: "금액", field: "plnamt", width: 130, hozAlign: "right", editor: "number", formatter: "money", formatterParams: { precision: 0 } },
      { title: "적요", field: "remark", widthGrow: 1, editor: "input" },
      { title: "삭제", width: 40, hozAlign: "center", formatter: (c) => "<i class='bi bi-trash text-danger'></i>", cellClick: (e, cell) => cell.getRow().delete() }
    ]
  })
}

const search = async () => {
  try {
    const res = await api.post('/hafn/HAFN_430S_STR', {
      cmpycd: authStore.cmpycd,
      plnymd: searchForm.plnymd.replace(/-/g, '')
    })
    mainGrid?.setData(res.data || [])
  } catch (e) { vAlertError('조회 실패') }
}

const save = async () => {
  const data = mainGrid?.getData() || []
  try {
    await api.post('/hafn/HAFN_430U_STR', {
      actkind: 'A0',
      cmpycd: authStore.cmpycd,
      plnymd: searchForm.plnymd.replace(/-/g, ''),
      items: data,
      userid: authStore.userid
    })
    vAlert('저장되었습니다.')
    search()
  } catch (e) { vAlertError('저장 실패') }
}

const addRow = () => mainGrid?.addRow({ inoutgbn: "1", plnamt: 0, remark: "" }, true)
const initialize = () => { searchForm.plnymd = new Date().toISOString().substring(0, 10); mainGrid?.clearData(); search() }

onMounted(() => { nextTick(() => { initGrid(); search() }) })
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
