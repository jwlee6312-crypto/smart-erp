<!--
	=============================================================
	프로그램명	: 일계표 (HASL510S)
	작성일자	: 2025.02.24
	설명        : 지정된 회계일자의 차변/대변 합계 및 현금 현재 현황 조회
	=============================================================
-->

<template>
  <AppAlert :show="show_alert" :error="show_error" :message="alert_message" />
  <Modal v-model:visible="modal_visible" :modalProps="modal_props" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom bg-white py-2 px-3 sticky-top shadow-sm">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-calendar-check me-2 text-primary" style="font-size: 18px;"></i>
        장부관리 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        <span class="text-primary fw-bolder">일계표 (HASL510S)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-print" @click="print">인쇄</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 조회 필터 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
              <col style="width: 10%" /><col style="width: 40%" />
              <col style="width: 10%" /><col style="width: 40%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light border-end">회계일자</th>
                <td class="border-end px-2">
                  <div class="d-flex align-items-center gap-1">
                    <input v-model="search_form.ymd" type="date" class="form-control form-control-sm" style="max-width: 150px;" @change="search" />
                    <span class="small fw-bold text-secondary">기준</span>
                  </div>
                </td>
                <th class="text-center bg-light border-end">조회구분</th>
                <td class="px-2">
                  <select v-model="search_form.actkind" class="form-select form-select-sm" style="max-width: 120px;" @change="search">
                    <option value="S0">상세내역</option>
                    <option value="S1">계정집약</option>
                  </select>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [중앙] 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 d-flex flex-column overflow-hidden grid-container-right">
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="main_grid_ref" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>

      <!-- [하단] 현금 현재 현황 -->
      <div class="card border shadow-sm flex-shrink-0 bg-white overflow-hidden">
        <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark">
          <i class="bi bi-cash-coin me-2 text-success"></i>현금 현재 및 전표 현황
        </div>
        <div class="card-body p-0">
          <table class="erp-table-dense text-center w-100">
            <colgroup>
              <col style="width: 80px;" />
              <col style="width: 120px;" /><col style="width: 120px;" />
              <col style="width: 120px;" /><col style="width: 120px;" />
              <col style="width: 100px;" /><col />
            </colgroup>
            <thead>
              <tr class="bg-light border-bottom">
                <th class="border-end py-1" rowspan="2">구분</th>
                <th class="border-end py-1" colspan="4">현금 흐름</th>
                <th class="border-end py-1" rowspan="2">전표건수</th>
                <th class="py-1" rowspan="2">조회결과</th>
              </tr>
              <tr class="bg-light border-bottom text-muted" style="font-size: 11px;">
                <th class="border-end py-0">전일잔액</th>
                <th class="border-end py-0">금일증가</th>
                <th class="border-end py-0">금일감소</th>
                <th class="border-end py-0 text-primary">현재잔액</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <th class="bg-light border-end py-2">현금현재</th>
                <td class="border-end text-end px-2">{{ format_number(summary_data.befo_cash) }}</td>
                <td class="border-end text-end px-2 text-primary">+ {{ format_number(summary_data.curr_db_cash) }}</td>
                <td class="border-end text-end px-2 text-danger">- {{ format_number(summary_data.curr_cr_cash) }}</td>
                <td class="border-end text-end px-2 fw-bold text-primary bg-light-subtle">{{ format_number(summary_data.curr_jan_cash) }}</td>
                <td class="border-end px-2 fw-bold text-dark">{{ summary_data.slip_cnt }} 건</td>
                <td class="px-2 text-start text-muted small italic">정상적으로 집계되었습니다.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref as _ref, reactive as _reactive, onMounted as _on_mounted, computed as _computed, nextTick as _next_tick } from 'vue'
import { TabulatorFull as tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts as use_alerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore as use_auth_store } from '@/stores/authStore'
import { useFormReset as use_form_reset } from '@/composables/useFormReset'
import { useRouter as use_router } from 'vue-router'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'

const auth_store = use_auth_store()
const router = use_router()
const { showAlert: show_alert, showError: show_error, alertMessage: alert_message, vAlert: v_alert, vAlertError: v_alert_error } = use_alerts()
const { resetForm: reset_form } = use_form_reset()

const today = new Date().toISOString().substring(0, 10)

const search_form = _reactive({
	ymd: today,
	actkind: 'S0'
})

const summary_data = _reactive({
	befo_cash: 0,
	curr_db_cash: 0,
	curr_cr_cash: 0,
	curr_jan_cash: 0,
	slip_cnt: 0
})

const main_grid_ref = _ref<HTMLDivElement | null>(null)
let main_grid: tabulator | null = null

const modal_visible = _ref(false)
const modal_props = _reactive<any>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

const search = async () => {
	try {
		const res = await api.post('/hasl/HASL_510S_STR', {
			actkind: search_form.actkind,
			cmpycd: auth_store.cmpycd,
			ymd: search_form.ymd.replace(/-/g, '')
		})

		const data = res.data || []

		if (data.length >= 2) {
			const row0 = data[0]
			summary_data.befo_cash = Number(row0.bjanamt || 0)
			summary_data.curr_db_cash = Number(row0.dbamt || 0)
			summary_data.curr_cr_cash = Number(row0.cramt || 0)
			summary_data.curr_jan_cash = summary_data.befo_cash + summary_data.curr_db_cash - summary_data.curr_cr_cash

			const row1 = data[1]
			summary_data.slip_cnt = Number(row1.dbamt || 0)

			const details = data.slice(2).map((row: any) => ({
				...row,
				dbamt: Number(row.dbamt || 0),
				cramt: Number(row.cramt || 0),
				is_total: row.acctcd === '9999999'
			}))

			main_grid?.setData(details)
			v_alert('조회되었습니다.')
		} else {
			main_grid?.clearData()
			reset_summary()
			v_alert('데이터가 존재하지 않습니다.')
		}
	} catch (e) {
		v_alert_error('조회 중 오류 발생')
		reset_summary()
	}
}

const reset_summary = () => {
	summary_data.befo_cash = 0
	summary_data.curr_db_cash = 0
	summary_data.curr_cr_cash = 0
	summary_data.curr_jan_cash = 0
	summary_data.slip_cnt = 0
}

const format_number = (val: number) => {
	return new Intl.NumberFormat('ko-KR').format(val)
}

const initialize = () => {
	reset_form(search_form)
	search_form.ymd = today
	search_form.actkind = 'S0'
	reset_summary()
	main_grid?.clearData()
}

const print = () => {
	const params = `ymd=${search_form.ymd.replace(/-/g, '')}&actkind=${search_form.actkind}&PRTGU=1`
	window.open(`/hasl/HASL_510P?${params}`, 'Print', 'width=800,height=800,scrollbars=yes')
}

const go_account_detail = (acct_cd: string) => {
	if (search_form.actkind !== 'S0' || !acct_cd || acct_cd === '9999999') return
	router.push({
		path: '/HASL540S',
		query: { fromdt: search_form.ymd.replace(/-/g, ''), todt: search_form.ymd.replace(/-/g, ''), acctcd: acct_cd }
	})
}

_on_mounted(() => {
	if (main_grid_ref.value) {
		main_grid = new tabulator(main_grid_ref.value, {
			layout: 'fitColumns',
			height: '100%',
			columnDefaults: { headerSort: false, vertAlign: "middle" },
			columns: [
				{
					title: "차변 (Debit)", field: "dbamt", widthGrow: 1, hozAlign: "right",
					formatter: "money", formatterParams: { precision: 0 },
					cssClass: "fw-bold"
				},
				{
					title: "계정과목 (Account)", field: "acctnm", widthGrow: 1, hozAlign: "center",
					formatter: (cell) => {
						const d = cell.getData()
						if (d.is_total) return `<strong>${cell.getValue()}</strong>`
						if (search_form.actkind === 'S0' && d.acctcd) {
							return `<span class="text-primary text-decoration-underline cursor-pointer">${cell.getValue()}</span>`
						}
						return cell.getValue()
					},
					cellClick: (e, cell) => {
						const d = cell.getData()
						go_account_detail(d.acctcd)
					}
				},
				{
					title: "대변 (Credit)", field: "cramt", widthGrow: 1, hozAlign: "right",
					formatter: "money", formatterParams: { precision: 0 },
					cssClass: "fw-bold"
				}
			],
			rowFormatter: (row) => {
				const d = row.getData()
				if (d.is_total) {
					row.getElement().style.backgroundColor = "#f0f7ff"
					row.getElement().style.fontWeight = "bold"
				}
			}
		})
	}
	search()
})
</script>

<style scoped>
.erp-label { min-width: 80px; font-weight: 500; font-size: 13px; }
.bg-light-subtle { background-color: #f8f9fa !important; }
.italic { font-style: italic; }
.tabulator-instance { width: 100% !important; background-color: #fff; }
:deep(.tabulator-cell) { border-right: 1px solid #dee2e6 !important; }
:deep(.tabulator-header .tabulator-col) { border-right: 1px solid #dee2e6 !important; background-color: #f8f9fa !important; }
</style>
