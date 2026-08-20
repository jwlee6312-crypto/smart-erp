<!--
	=============================================================
	프로그램명	: 수지현황(계정별) (HAFN510S)
	작성일자	: 2025.02.24
	설명        : 기간별/계정별 자금 입금 및 출금 상세 수지 현황 조회
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container">
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-graph-up-arrow me-2 text-primary" style="font-size: 18px;"></i>
        자금관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">수지현황(계정별) (HAFN510S)</span>
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
                <th class="required">조회기간</th>
                <td class="d-flex align-items-center gap-1 border-0" style="height: 32px;">
                    <input v-model="searchForm.fromdt" type="date" class="form-control form-control-sm" style="width: 150px;" />
                    <span class="text-muted">~</span>
                    <input v-model="searchForm.todt" type="date" class="form-control form-control-sm" style="width: 150px;" />
                </td>
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
import { getDate } from '@/composables/useDate'

const authStore = useAuthStore()
const { firstDay, today } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()

const searchForm = reactive({ fromdt: firstDay, todt: today })
const mainGridRef = ref<HTMLDivElement | null>(null)
let mainGrid: Tabulator | null = null

const search = async () => {
  try {
    const res = await api.post('/hafn/HAFN_510S_STR', {
      cmpycd: authStore.cmpycd,
      fromdt: searchForm.fromdt.replace(/-/g, ''),
      todt: searchForm.todt.replace(/-/g, '')
    })
    mainGrid?.setData(res.data || [])
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

const excel = () => mainGrid?.download("xlsx", `계정별수지현황_${searchForm.fromdt}_${searchForm.todt}.xlsx`)

onMounted(() => {
  if (mainGridRef.value) {
    mainGrid = new Tabulator(mainGridRef.value, {
      layout: 'fitColumns', height: '100%',
      columnDefaults: { headerSort: false, headerHozAlign: "center", vertAlign: "middle" },
      columns: [
        { title: "계정과목", field: "acctnm", width: 150, hozAlign: "left", frozen: true },
        { title: "일자", field: "plnymd", width: 110, hozAlign: "center", formatter: (c) => {
            const v = c.getValue(); return v && v.length === 8 ? `${v.substring(0,4)}-${v.substring(4,6)}-${v.substring(6,8)}` : v;
        }},
        { title: "입금액", field: "inamt", width: 130, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
        { title: "출금액", field: "outamt", width: 130, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
        { title: "적요", field: "remark", widthGrow: 1, hozAlign: "left" }
      ]
    })
  }
  nextTick(search)
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
