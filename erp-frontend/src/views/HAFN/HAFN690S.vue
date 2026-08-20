<!--
	=============================================================
	프로그램명: 카드미지급연계현황 (HAFN690S)
	작성일자	: 2025.03.14
	작성자    : AI Assistant
	설명        : 카드사별 카드 미지급금의 발생 및 지불 전표 연계 현황 조회
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom bg-white py-2 px-3 sticky-top shadow-sm">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-arrow-left-right me-2 text-primary" style="font-size: 18px;"></i>
        자금관리 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        <span class="text-primary fw-bolder">카드미지급연계현황 (hafn690s)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-print" @click="print">인쇄</button>
        <button class="btn-erp btn-excel" @click="excel">엑셀</button>
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
              <col style="width: 10%" /><col style="width: 25%" />
              <col style="width: 10%" /><col style="width: 20%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light border-end">회계일자</th>
                <td class="border-end px-2">
                  <div class="d-flex align-items-center gap-1">
                    <input v-model="searchForm.ymd_fr" type="date" class="form-control form-control-sm" />
                    <span class="text-muted">~</span>
                    <input v-model="searchForm.ymd_to" type="date" class="form-control form-control-sm" />
                  </div>
                </td>
                <th class="text-center bg-light border-end">카드사</th>
                <td class="border-end px-2">
                  <div class="input-group input-group-sm">
                    <input v-model="searchForm.custcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
                    <input v-model="searchForm.custnm" type="text" class="form-control" placeholder="카드사 선택" @keydown.enter="openHelp('cust')" />
                    <button class="btn btn-outline-secondary px-2" @click="openHelp('cust')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="text-center bg-light border-end">카드번호</th>
                <td class="px-2">
                  <div class="input-group input-group-sm">
                    <input v-model="searchForm.mgtno" type="text" class="form-control text-center bg-light" style="max-width: 120px;" readonly />
                    <button class="btn btn-outline-secondary px-2" @click="openHelp('mgt')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 d-flex flex-column overflow-hidden">
        <div class="card-header bg-white py-1 px-3 border-bottom text-secondary">
          <span class="fw-bold small"><i class="bi bi-list-columns-reverse me-2 text-primary"></i> 카드사별 회계 내역 리스트</span>
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
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { useRouter, useRoute } from 'vue-router'
import { addDynamicRoute } from '@/router/dynamicRoute'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import type { ModalProps } from '@/types/modal'

const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

const today = new Date().toISOString().substring(0, 10)
const firstDay = today.substring(0, 8) + '01'

const searchForm = reactive({
	ymd_fr: (route.query.ymd_fr as string) || firstDay,
	ymd_to: (route.query.ymd_to as string) || today,
	custcd: (route.query.custcd as string) || '',
	custnm: (route.query.custnm as string) || '',
	mgtno: (route.query.mgtno as string) || ''
})

const mainGridRef = ref<HTMLDivElement | null>(null)
let mainGrid: Tabulator | null = null

const search = async () => {
	try {
		const res = await api.post('/hafn/HAFN_690S_STR', {
			cmpycd: authStore.cmpycd,
			ymd_fr: searchForm.ymd_fr.replace(/-/g, ''),
			ymd_to: searchForm.ymd_to.replace(/-/g, ''),
			custcdfr: searchForm.custcd,
			custcdto: searchForm.custcd,
			mgtno: searchForm.mgtno
		})

		const data = (res.data || []).map((item: any) => {
            return {
                ...item,
                upyamt: Number(item.upyamt || 0),
                payamt: Number(item.payamt || 0),
                janamt: Number(item.janamt || 0),
                slip_no_full: `${item.slipymd || ''}-${item.slipno || ''}-${item.srowno || ''}`
            }
        })
		mainGrid?.setData(data)
		vAlert('조회되었습니다.')
	} catch (e) { vAlertError('조회 실패') }
}

const initialize = () => {
	resetForm(searchForm); searchForm.ymd_fr = firstDay; searchForm.ymd_to = today; mainGrid?.clearData();
}

const excel = () => mainGrid?.download("xlsx", `카드연계현황_${today}.xlsx`)
const print = () => {
	const params = new URLSearchParams({ ymd_fr: searchForm.ymd_fr.replace(/-/g, ''), ymd_to: searchForm.ymd_to.replace(/-/g, ''), custcd: searchForm.custcd, custnm: searchForm.custnm, mgtno: searchForm.mgtno }).toString()
	window.open(`/hafn/HAFN_690P?${params}`, 'Print', 'width=1000,height=800,scrollbars=yes')
}

const goSlipDetail = (row: any) => {
    const pgmid = 'HASL110U'; addDynamicRoute(pgmid, '전표관리', 'HASL')
	router.push({ path: `/${pgmid}`, query: { slipymd: row.slipymd, slipno: row.slipno } })
}

const printSlip = (row: any) => {
    const params = `slipgu=020&slipymd=${row.slipymd}&slipno=${row.slipno}&deptcd=${authStore.deptcd}`
    window.open(`/hasl/HASL_SLIP_G_PRINT?${params}`, 'SlipPrint', 'width=750,height=650')
}

const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function openHelp(type: string) {
	if (type === 'cust') {
		Object.assign(modalProps, {
			title: '카드사 선택', path: '/ha00/HA00_00P_STR',
			data: { gubun: 'C3', custgbn: '020', cmpycd: authStore.cmpycd, search: searchForm.custnm },
			columns: [{ title: '코드', field: 'bankcd', width: 80 }, { title: '카드사명', field: 'banknm', width: 180 }],
			onConfirm: (item: any) => {
                searchForm.custcd = item.bankcd; searchForm.custnm = item.banknm
            }
		})
	} else if (type === 'mgt') {
		Object.assign(modalProps, {
			title: '카드번호 선택', path: '/ha00/HA00_00P_STR',
			data: { gubun: 'M0', gbncd: '040', remark: searchForm.custcd, cmpycd: authStore.cmpycd, search: '' },
			columns: [{ title: '카드번호', field: 'mgtno', width: 120 }, { title: '카드명', field: 'mgtnm', width: 150 }],
			onConfirm: (item: any) => {
                searchForm.mgtno = item.mgtno
            }
		})
	}
	modalVisible.value = true
}

onMounted(() => {
	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns', height: '100%', columnDefaults: { headerSort: false, vertAlign: "middle" },
			columns: [
				{ title: "회계전표", field: "slip_no_full", width: 150, hozAlign: "center", formatter: (cell) => `<span class="text-primary text-decoration-underline cursor-pointer fw-bold">${cell.getValue()}</span>`, cellClick: (e, cell) => goSlipDetail(cell.getData()) },
				{ title: "적요", field: "remark", minWidth: 250, formatter: (cell) => `<span class="text-dark text-decoration-underline cursor-pointer">${cell.getValue()}</span>`, cellClick: (e, cell) => printSlip(cell.getData()) },
				{ title: "카드사", field: "custnm", width: 150 },
				{ title: "카드번호", field: "cardno", width: 150, hozAlign: "center" },
				{ title: "결제계좌", field: "banknm", width: 150 },
				{ title: "발생액", field: "upyamt", width: 130, hozAlign: "right", formatter: "money" },
				{ title: "지불액", field: "payamt", width: 130, hozAlign: "right", formatter: "money" },
				{ title: "잔액", field: "janamt", width: 130, hozAlign: "right", formatter: "money", cssClass: "fw-bold text-danger" }
			]
		})
	}
    if (searchForm.custcd) search()
})
</script>

<style scoped>
.erp-label { min-width: 80px; font-weight: 500; font-size: 13px; }
.cursor-pointer { cursor: pointer; }
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
