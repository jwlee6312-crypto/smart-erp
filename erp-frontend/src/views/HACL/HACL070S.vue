<!--
	=============================================================
	프로그램명	: 손익계산서(프로젝트) (HACL070S)
	작성일자	: 2025.02.24
	설명        : HSOD100U 표준 그리드 패턴을 준수하여 프로젝트별 손익계산서 조회
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-graph-up-arrow me-2 text-primary" style="font-size: 18px;"></i>
        재무제표 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">손익계산서(프로젝트) (HACL070S)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-print" @click="print">인쇄</button>
        <button class="btn-erp btn-excel" @click="excel">엑셀</button>
      </div>
    </div>

    <!-- 🔍 2. 조회 조건 영역 -->
    <div class="p-2 pb-0 flex-shrink-0 bg-light">
      <div class="card border shadow-sm overflow-hidden">
        <div class="card-body p-2 bg-white">
          <div class="d-flex align-items-center gap-3 small">
            <div class="d-flex align-items-center">
              <span class="erp-label" style="min-width: 70px;"><i class="bi bi-dot text-primary"></i>프로젝트</span>
              <div class="input-group input-group-sm shadow-sm" style="width: 280px;">
                <input v-model="searchForm.prjcd" type="text" class="form-control text-center bg-light" style="max-width: 70px;" readonly />
                <input v-model="searchForm.prjnm" type="text" class="form-control" @keydown.enter="openHelp('PRJ')" placeholder="프로젝트 선택" />
                <button class="btn btn-outline-secondary px-2" @click="openHelp('PRJ')"><i class="bi bi-search"></i></button>
              </div>
            </div>
            <div class="d-flex align-items-center">
              <span class="erp-label" style="min-width: 70px;"><i class="bi bi-dot text-primary"></i>회계일자</span>
              <div class="d-flex align-items-center gap-1">
                <select v-model="searchForm.yy" class="form-select form-select-sm" style="width: 100px;" @change="search">
                  <option v-for="year in yearOptions" :key="year" :value="year">{{ year }}년</option>
                </select>
                <select v-model="searchForm.mm" class="form-select form-select-sm" style="width: 80px;" @change="search">
                  <option v-for="month in monthOptions" :key="month" :value="month">{{ month }}</option>
                </select>
                <span class="ms-1 fw-bold">월 까지</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 📊 3. 그리드 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column bg-light">
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white rounded-0">
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="tableRef" class="tabulator-instance flex-grow-1 border-0"></div>
        </div>
      </div>
    </div>
  </div>

  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import AppAlert from '@/components/AppAlert.vue'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import Modal from '@/components/Modal.vue'
import type { ModalProps } from '@/types/modal'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()

const yearOptions = Array.from({ length: 16 }, (_, i) => String(2025 - i))
const monthOptions = Array.from({ length: 12 }, (_, i) => String(i + 1).padStart(2, '0'))

const searchForm = reactive({ prjcd: '', prjnm: '', yy: "2011", mm: "06" })

const tableRef = ref<HTMLDivElement | null>(null)
let grid: Tabulator | null = null

const sNUM = ['', 'Ⅰ', 'Ⅱ', 'Ⅲ', 'Ⅳ', 'Ⅴ', 'Ⅵ', 'Ⅶ', 'Ⅷ', 'Ⅸ', 'Ⅹ', 'Ⅺ', 'Ⅻ']

const initGrid = () => {
  if (!tableRef.value) return;
  grid = new Tabulator(tableRef.value, {
    layout: "fitColumns",
    height: "100%",
    placeholder: "데이터 없음",
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      {
        title: "과 목", field: "formatted_nm", widthGrow: 2,
        formatter: "html",
        cssClass: "border-end bg-light fw-bold",
        cellClick: (e, cell) => goDrillDown(cell.getData())
      },
      {
        title: "전월누계",
        columns: [
          { title: "금 액", field: "bamt", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
          { title: "%", field: "brate", width: 60, hozAlign: "center" }
        ]
      },
      {
        title: "당 월",
        columns: [
          { title: "금 액", field: "camt", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "bg-light-subtle fw-bold" },
          { title: "%", field: "crate", width: 60, hozAlign: "center", cssClass: "bg-light-subtle" }
        ]
      },
      {
        title: "당기누계",
        columns: [
          { title: "금 액", field: "tamt", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
          { title: "%", field: "trate", width: 60, hozAlign: "center" }
        ]
      }
    ],
  });
}

const search = async () => {
  if (!searchForm.prjcd) return vAlertError('프로젝트를 선택하세요.');

  try {
    const res = await api.post('/hacl/HACL_070S_STR', {
      cmpycd: authStore.cmpycd,
      prjcd: searchForm.prjcd,
      yymm: `${searchForm.yy}${searchForm.mm}`
    });

    const rawData = res.data || []

    let i = 1, j = 1, k = 1
    const processedData = rawData.map((row: any, idx: number) => {
      const acctcd = String(row.acctcd || row.col0 || '').trim()
      const acctnm = String(row.acctnm || row.col1 || '')
      const bamtl = Number(row.bamtl || 0), bamtr = Number(row.bamtr || 0)
      const camtl = Number(row.camtl || 0), camtr = Number(row.camtr || 0)
      const tamtl = Number(row.tamtl || row.tamt_l || 0), tamtr = Number(row.tamtr || row.tamt_r || 0)
      const salestotb = Number(row.salestot_b || row.salestotb || 0)
      const salestotc = Number(row.salestot_c || row.salestotc || 0)
      const salestott = Number(row.salestot_t || row.salestott || 0)

      let formatted_nm = acctnm, brate: any = "", crate: any = "", trate: any = "", spaces = "", can_drill = false

      if (acctcd.substring(1, 7) === "000000" || ["1990000", "2980000", "3980000", "3990000"].includes(acctcd)) {
        formatted_nm = `[ ${acctnm} ]`
      } else if (acctcd.substring(2, 7) === "00000") {
        formatted_nm = `${sNUM[i] || i}. ${acctnm}`; i++
        const bval = bamtl !== 0 ? bamtl : bamtr
        const cval = camtl !== 0 ? camtl : camtr
        const tval = tamtl !== 0 ? tamtl : tamtr
        if (salestotb !== 0) brate = ((bval / salestotb) * 100).toFixed(1);
        if (salestotc !== 0) crate = ((cval / salestotc) * 100).toFixed(1);
        if (salestott !== 0) trate = ((tval / salestott) * 100).toFixed(1);
      } else if (acctcd.substring(3, 7) === "0000") {
        formatted_nm = `${j}). ${acctnm}`; j++; spaces = "&nbsp;&nbsp;"; can_drill = true
        const bval = bamtl !== 0 ? bamtl : bamtr
        const cval = camtl !== 0 ? camtl : camtr
        const tval = tamtl !== 0 ? tamtl : tamtr
        if (salestotb !== 0) brate = ((bval / salestotb) * 100).toFixed(1);
        if (salestotc !== 0) crate = ((cval / salestotc) * 100).toFixed(1);
        if (salestott !== 0) trate = ((tval / salestott) * 100).toFixed(1);
      } else if (acctcd.substring(5, 7) === "00") {
        if (row.rstyn === 'Y' || acctcd === "5213000" || acctcd === "5223000") {
          formatted_nm = acctnm
          const bval = bamtl !== 0 ? bamtl : bamtr
          const cval = camtl !== 0 ? camtl : camtr
          const tval = tamtl !== 0 ? tamtl : tamtr
          if (salestotb !== 0) brate = ((bval / salestotb) * 100).toFixed(1);
          if (salestotc !== 0) crate = ((cval / salestotc) * 100).toFixed(1);
          if (salestott !== 0) trate = ((tval / salestott) * 100).toFixed(1);
        } else {
          formatted_nm = `${k}. ${acctnm}`; k++
        }
        spaces = "&nbsp;&nbsp;&nbsp;&nbsp;"; can_drill = true
      } else {
        formatted_nm = acctnm; spaces = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"; can_drill = true
      }

      const next = rawData[idx + 1]
      if (next) {
        const nCd = String(next.acctcd || next.col0 || '').trim()
        if (acctcd.substring(0, 3) !== nCd.substring(0, 3)) { if (acctcd.substring(0, 2) !== "51") k = 1 }
        if (acctcd.substring(0, 2) !== nCd.substring(0, 2)) j = 1
        if (acctcd.substring(0, 1) !== nCd.substring(0, 1)) i = 1
      }

      const bamt = bamtl !== 0 ? bamtl : bamtr
      const camt = camtl !== 0 ? camtl : camtr
      const tamt = tamtl !== 0 ? tamtl : tamtr

      let final_nm = spaces + formatted_nm
      if (can_drill) final_nm = `<span class="text-primary text-decoration-underline cursor-pointer">${final_nm}</span>`

      return { ...row, formatted_nm: final_nm, bamt, brate, camt, crate, tamt, trate, _can_drill: can_drill, _drill_nm: acctnm, _drill_amt: camt }
    })

    grid?.setData(processedData)
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

const goDrillDown = (data: any) => {
  if (!data._can_drill) return
  const params = `prjcd=${searchForm.prjcd}&upacct=${data.acctcd || data.col0}&ym=${searchForm.yy}${searchForm.mm}&acctnm=${data._drill_nm}&amt=${data._drill_amt}`
  window.open(`/hacl/HACL_071U?${params}`, 'ProjectCostDetail', 'width=500,height=600,scrollbars=yes')
}

const excel = () => grid?.download("xlsx", `손익계산서_프로젝트_${searchForm.yy}${searchForm.mm}.xlsx`)
const print = () => window.open(`/hacl/HACL_070P?prjcd=${searchForm.prjcd}&prjnm=${searchForm.prjnm}&yy=${searchForm.yy}&mm=${searchForm.mm}&PRTGU=1`)

const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function openHelp(type: string) {
  Object.assign(modalProps, {
    title: '프로젝트 선택', path: '/ha00/HA00_03P_STR', defaultField: 'col1',
    data: { custgbn: '060', cmpycd: authStore.cmpycd, search: searchForm.prjnm },
    columns: [{ title: '코드', field: 'col0', width: 80 }, { title: '프로젝트명', field: 'col1', width: 250 }],
    onConfirm: (d: any) => {
      searchForm.prjcd = d.col0
      searchForm.prjnm = d.col1
      search()
    }
  })
  modalVisible.value = true
}

onMounted(() => {
  nextTick(initGrid)
})
</script>

<style scoped>
.erp-label { min-width: 80px; font-weight: 500; font-size: 13px; }
.tabulator-instance { border: 1px solid #dee2e6 !important; width: 100% !important; background-color: #fff; }
</style>
