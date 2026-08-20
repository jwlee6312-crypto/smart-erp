<!--
	=============================================================
	프로그램명	: 보조원장 (HASL540S)
	작성일자	: 2025.02.24
	설명        : 특정 계정과목에 대한 상세 전표 내역 및 잔액 조회
	=============================================================
-->

<template>
  <AppAlert :show="show_alert" :error="show_error" :message="alert_message" />
  <Modal v-model:visible="modal_visible" :modalProps="modal_props" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom bg-white py-2 px-3 sticky-top shadow-sm">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-journal-text me-2 text-primary" style="font-size: 18px;"></i>
        장부관리 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        <span class="text-primary fw-bolder">보조원장 (HASL540S)</span>
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
                <th class="text-center bg-light border-end">계정과목</th>
                <td class="border-end px-2">
                  <div class="input-group input-group-sm" style="max-width: 320px;">
                    <input v-model="search_form.acctcd" type="text" class="form-control text-center bg-light" style="max-width: 70px;" readonly />
                    <input v-model="search_form.acctnm" type="text" class="form-control" placeholder="계정명 입력" @keydown.enter="open_help('ACCT')" />
                    <button class="btn btn-outline-secondary px-2" @click="open_help('ACCT')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="text-center bg-light border-end">회계일자</th>
                <td class="px-2">
                  <div class="d-flex align-items-center gap-1">
                    <input v-model="search_form.fromdt" type="date" class="form-control form-control-sm" style="max-width: 150px;" />
                    <span class="text-muted">~</span>
                    <input v-model="search_form.todt" type="date" class="form-control form-control-sm" style="max-width: 150px;" />
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [중앙] 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 d-flex flex-column overflow-hidden grid-container-right">
        <div class="card-header bg-white py-1 px-3 border-bottom text-secondary">
          <span class="fw-bold small"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i> 계정별 상세 거래 내역</span>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="main_grid_ref" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref as _ref, reactive as _reactive, onMounted as _on_mounted, watch as _watch } from 'vue'
import { TabulatorFull as tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts as use_alerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore as use_auth_store } from '@/stores/authStore'
import { useRouter as use_router, useRoute as use_route } from 'vue-router'
import { useFormReset as use_form_reset } from '@/composables/useFormReset'
import { addDynamicRoute as add_dynamic_route } from '@/router/dynamicRoute'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import type { ModalProps as modal_props_type } from '@/types/modal'

const auth_store = use_auth_store()
const router = use_router()
const route = use_route()
const { showAlert: show_alert, showError: show_error, alertMessage: alert_message, vAlert: v_alert, vAlertError: v_alert_error } = use_alerts()
const { resetForm: reset_form } = use_form_reset()

const format_date = (val: any) => {
	if (!val || typeof val !== 'string' || val.length !== 8) return val
	return `${val.substring(0, 4)}-${val.substring(4, 6)}-${val.substring(6, 8)}`
}

const now = new Date()
const first_day = new Date(now.getFullYear(), now.getMonth(), 1).toISOString().substring(0, 10)
const today = now.toISOString().substring(0, 10)

const search_form = _reactive({
	acctcd: (route.query.acctcd as string) || '',
	acctnm: (route.query.acctnm as string) || '',
	fromdt: format_date(route.query.fromdt) || first_day,
	todt: format_date(route.query.todt) || today
})

const main_grid_ref = _ref<HTMLDivElement | null>(null)
let main_grid: tabulator | null = null



const search = async () => {
	if (!search_form.acctcd) {
		v_alert_error('계정과목을 선택해 주십시오.')
		return
	}

	try {
		const res = await api.post('/hasl/HASL_540S_STR', {
			cmpycd: auth_store.cmpycd,
			acctcd: search_form.acctcd,
			fromdt: search_form.fromdt.replace(/-/g, ''),
			todt: search_form.todt.replace(/-/g, '')
		})

		const raw_data = res.data || []

		const processed_data: any[] = []
		if (raw_data.length > 0) {
			let carry_row = raw_data[0]
			let db_mm_tot = Number(carry_row.dbamt || 0)
			let cr_mm_tot = Number(carry_row.cramt || 0)
			let dbcr = carry_row.dbcr
			let jan_amt = dbcr === 'D' ? (db_mm_tot - cr_mm_tot) : (cr_mm_tot - db_mm_tot)

			processed_data.push({ acctymd: '', descnm: carry_row.descnm, slipno: '', dbamt: db_mm_tot, cramt: cr_mm_tot, janamt: jan_amt, is_summary: true })

			let i = 1
			while (i < raw_data.length) {
				let current_ym = String(raw_data[i].acctymd).substring(0, 6)
				let db_mm_amt = 0; let cr_mm_amt = 0;
				while (i < raw_data.length && String(raw_data[i].acctymd).substring(0, 6) === current_ym) {
					const row = raw_data[i]
					const db = Number(row.dbamt || 0); const cr = Number(row.cramt || 0)
					if (dbcr === 'D') jan_amt += (db - cr); else jan_amt += (cr - db)

					let details: string[] = []
					if (row.deptnm) details.push(String(row.deptnm).trim())
					if (row.subnm) details.push(String(row.subnm).trim())
					if (row.mgtno) details.push(String(row.mgtno).trim())

					processed_data.push({
						acctymd: row.acctymd,
						descnm: row.descnm + (row.sslipno && row.sslipno !== '0' ? ` (${row.sslipno})` : ''),
						slipno: row.slipno, dbamt: db, cramt: cr, janamt: jan_amt, detail_str: details.join(' | '), is_data: true
					})
					db_mm_amt += db; cr_mm_amt += cr; i++
				}
				processed_data.push({ acctymd: '', descnm: '월  계', dbamt: db_mm_amt, cramt: cr_mm_amt, janamt: null, is_monthly: true })
				db_mm_tot += db_mm_amt; cr_mm_tot += cr_mm_amt;
				processed_data.push({ acctymd: '', descnm: '누  계', dbamt: db_mm_tot, cramt: cr_mm_tot, janamt: jan_amt, is_total: true })
			}
		}
		main_grid?.setData(processed_data)
		v_alert(processed_data.length > 0 ? '조회되었습니다.' : '데이터가 존재하지 않습니다.')
	} catch (e) { v_alert_error('조회 실패') }
}

const initialize = () => {
	reset_form(search_form)
	search_form.fromdt = first_day
	search_form.todt = today
	main_grid?.clearData()
}

const go_slip_detail = (slip_no: string) => {
	if (!slip_no) return
	let ymd = ""; let no = ""
	if (slip_no.includes('-')) {
		const parts = slip_no.split('-'); ymd = parts[0]; no = parts[1]
	} else if (slip_no.length >= 11) {
		ymd = slip_no.substring(0, 8); no = slip_no.substring(8)
	} else return

	const pgmid = 'HASL110U'
	add_dynamic_route(pgmid, '경리전표등록', 'HASL')
	router.push({ path: `/${pgmid}`, query: { slipymd: ymd, slipno: no } })
}

const modal_visible = _ref(false)
const modal_props = _reactive<modal_props_type>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function open_help(type: string) {
	Object.assign(modal_props, {
		title: '계정과목 선택', path: '/ha00/HA00_00P_STR', defaultField: 'acctnm',
		data: { gubun: 'A0', cmpycd: auth_store.cmpycd, search: search_form.acctnm },
		columns: [{ title: '코드', field: 'acctcd', width: 80 }, { title: '계정명', field: 'acctnm', width: 180 }],
		onConfirm: (d: any) => {
            const item = d;
            search_form.acctcd = item.acctcd; search_form.acctnm = item.acctnm; search()
        }
	})
	modal_visible.value = true
}

const print = () => {
	if (!search_form.acctcd) return v_alert_error('계정과목을 먼저 선택하세요.')
	const params = `acctcd=${search_form.acctcd}&acctnm=${search_form.acctnm}&fromdt=${search_form.fromdt.replace(/-/g, '')}&todt=${search_form.todt.replace(/-/g, '')}&PRTGU=1`
	window.open(`/hasl/HASL_540P?${params}`, 'Print', 'width=800,height=800')
}

_on_mounted(async () => {
	if (main_grid_ref.value) {
		main_grid = new tabulator(main_grid_ref.value, {
			layout: 'fitColumns', height: '100%', columnDefaults: { headerSort: false, vertAlign: "middle" },
			columns: [
				{ title: "일자", field: "acctymd", width: 90, hozAlign: "center", formatter: (cell) => { const v = cell.getValue(); return v && v.length === 8 ? `${v.substring(2, 4)}.${v.substring(4, 6)}.${v.substring(6, 8)}` : '' } },
				{ title: "적요 / 상세내역", field: "descnm", widthGrow: 2.5, formatter: (cell) => { const d = cell.getData(); return d.is_data ? `<div>${cell.getValue()}</div><div class="small text-secondary fw-normal mt-1" style="font-size: 11px;">${d.detail_str}</div>` : cell.getValue() } },
				{ title: "전표번호", field: "slipno", width: 120, hozAlign: "center", formatter: (cell) => cell.getValue() ? `<span class="text-primary text-decoration-underline cursor-pointer">${cell.getValue()}</span>` : '', cellClick: (e, cell) => { go_slip_detail(cell.getValue()) } },
				{ title: "차변", field: "dbamt", width: 120, hozAlign: "right", formatter: "money" },
				{ title: "대변", field: "cramt", width: 120, hozAlign: "right", formatter: "money" },
				{ title: "잔액", field: "janamt", width: 120, hozAlign: "right", formatter: "money" }
			],
			rowFormatter: (row) => { const d = row.getData(); if (d.is_monthly || d.is_total || d.is_summary) { row.getElement().style.backgroundColor = "#f8f9fa"; row.getElement().style.fontWeight = "bold"; } }
		})
	}
	if (search_form.acctcd) {
		if (search_form.acctnm) search();
		else {
			try {
				const res = await api.post('/ha00/HA00_010S_STR', { cmpycd: auth_store.cmpycd, gubun: 'A0', search: '', acctcd: search_form.acctcd })
				if (res.data?.length > 0) {
                    const item = res.data[0];
                    search_form.acctnm = item.col0 || item.acctnm; search()
                }
			} catch (e) {}
		}
	}
})
</script>

<style scoped>
.erp-label { min-width: 80px; font-weight: 500; font-size: 13px; }
:deep(.tabulator-cell) { border-right: 1px solid #dee2e6 !important; }
:deep(.tabulator-header .tabulator-col) { border-right: 1px solid #dee2e6 !important; background-color: #f8f9fa !important; }
</style>
