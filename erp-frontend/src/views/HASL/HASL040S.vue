<!--
	=============================================================
	프로그램명	: 자동분개전표 (HASL040S)
	작성일자	: 2025.02.24
	설명        : 연동별 자동 분개 유형 및 내역 조회 (HASL050U 연동)
	=============================================================
-->

<template>
  <app_alert :show="show_alert" :error="show_error" :message="alert_message" />

  <div class="erp-container">
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-lightning-charge me-2 text-primary" style="font-size: 18px;"></i>
        전표관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">자동분개전표 (HASL040S)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
      </div>
    </div>

    <div class="p-2 pb-0 flex-shrink-0">
      <div class="card border shadow-sm overflow-hidden bg-light">
        <table class="erp-table-full" style="table-layout: fixed;">
          <colgroup>
            <col style="width: 30%;" /><col style="width: 50%;" /><col style="width: 20%;" />
          </colgroup>
          <tbody>
            <tr>
              <td>
                <div class="d-flex align-items-center px-2">
                  <span class="erp-label me-2">검색대상</span>
                  <select v-model="searchform.searchgbn" class="form-select form-select-sm">
                    <option value="100">분개유형</option>
                    <option value="200">분개전표</option>
                  </select>
                </div>
              </td>
              <td>
                <div class="d-flex align-items-center px-2">
                  <span class="erp-label me-2">검색어</span>
                  <input v-model="searchform.keyword" type="text" class="form-control form-control-sm"
                    placeholder="키워드 입력" @keydown.enter="search" ref="keywordinput" />
                </div>
              </td>
              <td><div class="px-2 text-muted small">(검색어를 입력하고 조회를 누르세요)</div></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column">
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white">
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="maingridref" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref as _ref, reactive as _reactive, onMounted as _on_mounted, nextTick as _next_tick } from 'vue'
import { TabulatorFull as tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts as use_alerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore as use_auth_store } from '@/stores/authStore'
import { useRouter as use_router } from 'vue-router'
import { addDynamicRoute as add_dynamic_route } from '@/router/dynamicRoute'
import app_alert from '@/components/AppAlert.vue'

const authstore = use_auth_store()
const router = use_router()
const { showAlert: show_alert, showError: show_error, alertMessage: alert_message, vAlert: v_alert, vAlertError: v_alert_error } = use_alerts()

const keywordinput = _ref<any>(null)
const searchform = _reactive({ searchgbn: '100', keyword: '' })
const maingridref = _ref<any>(null)
let maingrid: any = null

const search = async () => {
  try {
    const res = await api.post('/hasl/hasl_040s_str', {
      cmpycd: authstore.cmpycd,
      search_gbn: searchform.searchgbn,
      keyword: searchform.keyword
    })
    maingrid?.setData(res.data || [])
    if ((res.data || []).length === 0) v_alert('검색 결과가 없습니다.')
    else v_alert('조회되었습니다.')
  } catch (e) {
    v_alert_error('조회 중 오류가 발생했습니다.')
  }
}

const initialize = () => {
  searchform.searchgbn = '100'; searchform.keyword = ''; maingrid?.clearData(); keywordinput.value?.focus()
}

_on_mounted(() => {
  if (maingridref.value) {
    maingrid = new tabulator(maingridref.value, {
      layout: 'fitColumns',
      height: '100%',
      groupBy: "jurncd",
      groupHeader: (value: any, count: any, data: any) => {
        const firstrow = data[0]
        const costgbnm = firstrow.costgbnm ? `[${firstrow.costgbnm}] ` : ''
        return `
          <div class='d-flex flex-column w-100 py-1'>
            <div class='d-flex justify-content-between align-items-center'>
              <span class='text-primary fw-bold cursor-pointer' onclick='window.dispatchEvent(new CustomEvent("go-detail", {detail: {jurncd: "${value}", gbn: "${searchform.searchgbn}"}}))'>
                <i class="bi bi-check2-circle me-1"></i> ${costgbnm}${firstrow.jurnnm}
              </span>
              <span class='badge bg-secondary opacity-75'>${count} lines</span>
            </div>
            <div class='text-muted fw-normal small mt-1'>설명: ${firstrow.remark || ''}</div>
          </div>
        `
      },
      columnDefaults: { headerSort: false, headerHozAlign: "center", hozAlign: "center", vertAlign: "middle" },
      columns: [
        { title: "no", field: "jurnno", width: 60 },
        { title: "구분", field: "dbcr", width: 100, formatter: (cell: any) => cell.getValue().toLowerCase() === 'd' ? '<b class="text-primary">차변</b>' : '<b class="text-danger">대변</b>' },
        { title: "계정코드", field: "acctcd", width: 120 },
        { title: "계정과목명", field: "acctnm", hozAlign: "left", minWidth: 200 },
        { title: "jurncd", field: "jurncd", visible: false }
      ],
      placeholder: "조회된 데이터가 없습니다."
    })

    window.addEventListener("go-detail", ((e: any) => {
      const pgmid = 'HASL050U'
      add_dynamic_route(pgmid, '자동분개전표등록', 'HASL')
      router.push({
        path: `/${pgmid}`,
        query: { jurncd: e.detail.jurncd, gbn: e.detail.gbn }
      })
    }) as any)
  }
  keywordinput.value?.focus()
})
</script>

<style scoped>
.erp-label { min-width: 70px; font-weight: 500; color: #495057; }
:deep(.tabulator-group) { background-color: #f8f9fa !important; border-top: 1px solid #dee2e6 !important; }
:deep(.tabulator-group-header) { padding: 8px 12px !important; }
:deep(.tabulator-group span) { display: inline-block; }
</style>
