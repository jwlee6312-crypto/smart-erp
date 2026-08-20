<!--
	=============================================================
	프로그램명	: 자산대장조회 (HAFA020S)
	작성일자	: 2025.02.24
	설명        : 현재 보유 중인 모든 고정자산의 상세 내역 및 장부가액 조회
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container">
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-journal-text me-2 text-primary" style="font-size: 18px;"></i>
        고정자산 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">자산대장조회 (HAFA020S)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1">
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-excel" @click="excel">엑셀</button>
      </div>
    </div>

    <div class="flex-grow-1 overflow-auto p-3 d-flex flex-column gap-3 bg-light">
      <div class="card border-0 shadow-sm overflow-hidden flex-shrink-0">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-full border-0">
            <colgroup>
              <col style="width: 150px;" /><col />
            </colgroup>
            <tbody>
              <tr>
                <th class="bg-light text-center">자산명칭</th>
                <td><input v-model="searchForm.astnm" class="form-control form-control-sm" style="max-width: 300px;" placeholder="자산명 검색" @keyup.enter="search" /></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="card border-0 shadow-sm flex-grow-1 overflow-hidden d-flex flex-column">
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

const searchForm = reactive({ astnm: '' })
const mainGridRef = ref<HTMLDivElement | null>(null)
let mainGrid: Tabulator | null = null

const search = async () => {
  try {
    const res = await api.post('/hafa/HAFA_020S_STR', {
      cmpycd: authStore.cmpycd,
      astnm: searchForm.astnm
    })
    mainGrid?.setData(res.data || [])
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

const excel = () => mainGrid?.download("xlsx", `자산대장_${new Date().toISOString().substring(0, 10)}.xlsx`)

onMounted(() => {
  if (mainGridRef.value) {
    mainGrid = new Tabulator(mainGridRef.value, {
      layout: 'fitColumns', height: '100%',
      columnDefaults: { headerSort: false, headerHozAlign: "center", vertAlign: "middle" },
      columns: [
        { title: "자산코드", field: "astcd", width: 100, hozAlign: "center" },
        { title: "자산명칭", field: "astnm", minWidth: 150, hozAlign: "left", cssClass: "fw-bold" },
        { title: "규격", field: "astsize", width: 120 },
        { title: "취득일자", field: "acqymd", width: 110, hozAlign: "center", formatter: (c) => {
            const v = c.getValue(); return v && v.length === 8 ? `${v.substring(0,4)}-${v.substring(4,6)}-${v.substring(6,8)}` : v;
        }},
        { title: "취득가액", field: "acqamt", width: 130, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
        { title: "상각누계", field: "depramt", width: 130, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
        { title: "장부가액", field: "janamt", width: 130, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "text-primary fw-bold" }
      ]
    })
  }
  nextTick(search)
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
