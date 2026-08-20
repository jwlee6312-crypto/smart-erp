<!--
	=============================================================
	프로그램명	: 월차 손익계산서 (HAPL110S)
	작성일자	: 2025.02.24
	설명        : 부서별 월차 손익 현황 조회
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-file-earmark-bar-graph-fill me-2 text-primary" style="font-size: 18px;"></i>
        관리손익 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">월차 손익계산서 (HAPL110S)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1">
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-print" @click="print">인쇄</button>
        <button class="btn-erp btn-excel" @click="excel">엑셀</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-auto p-3 d-flex flex-column gap-3">

      <!-- 🔍 검색 조건 카드 -->
      <div class="card border-0 shadow-sm overflow-hidden flex-shrink-0">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-full border-0">
            <colgroup>
              <col style="width: 120px;" /><col />
              <col style="width: 120px;" /><col />
            </colgroup>
            <tbody>
              <tr>
                <th class="required">부서</th>
                <td>
                  <div class="input-group input-group-sm flex-nowrap" style="max-width: 350px;">
                    <input v-model="searchForm.deptcd" type="text" class="form-control text-center bg-light fw-bold" style="max-width: 60px;" readonly />
                    <input v-model="searchForm.deptnm" type="text" class="form-control border-start-0" placeholder="부서 선택" @keyup.enter="openHelp('DEPT')" />
                    <button class="btn btn-outline-secondary px-2" @click="openHelp('DEPT')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="required">회계일자</th>
                <td>
                  <div class="d-flex align-items-center gap-1">
                    <select v-model="searchForm.yy" class="form-select" style="width: 110px;">
                      <option v-for="y in yearOptions" :key="y" :value="y">{{ y }}년</option>
                    </select>
                    <select v-model="searchForm.mm" class="form-select" style="width: 90px;">
                      <option v-for="m in monthOptions" :key="m" :value="m">{{ m }}월</option>
                    </select>
                    <span class="ms-1 small text-muted">현재</span>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 📊 그리드 영역 -->
      <div class="card border-0 shadow-sm flex-grow-1 overflow-hidden d-flex flex-column">
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="mainGridRef" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>
    </div>
  </div>

  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import AppAlert from '@/components/AppAlert.vue'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useCommonHelp } from '@/composables/useCommonHelp'
import Modal from '@/components/Modal.vue'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { modalVisible, modalProps, openHelp } = useCommonHelp()

const currentYear = new Date().getFullYear()
const currentMonth = (new Date().getMonth() + 1).toString().padStart(2, '0')
const yearOptions = Array.from({ length: 20 }, (_, i) => String(currentYear - i))
const monthOptions = Array.from({ length: 12 }, (_, i) => String(i + 1).padStart(2, '0'))

const searchForm = reactive({
  yy: String(currentYear),
  mm: currentMonth,
  deptcd: authStore.deptcd,
  deptnm: authStore.deptnm
})

const mainGridRef = ref<HTMLDivElement | null>(null)
let mainGrid: Tabulator | null = null

const sNUM = ["", "Ⅰ", "Ⅱ", "Ⅲ", "Ⅳ", "Ⅴ", "Ⅵ", "Ⅶ", "Ⅷ", "Ⅸ", "Ⅹ", "XI", "XII", "XIII", "XIV"]

const initData = async () => {
  try {
    const res = await api.post('/hapl/HAPL_110S_INIT', {
      cmpycd: authStore.cmpycd,
      stdym: currentYear + currentMonth
    })
    const data = res.data?.[0]?.acctym || '000000'
    if (data !== '000000') {
      searchForm.yy = data.substring(0, 4)
      searchForm.mm = data.substring(4, 6)
    }
  } catch (e) { console.error('초기화 데이터 로드 실패') }
}

const search = async () => {
  if (!searchForm.deptcd) return vAlertError('부서를 선택하세요.')

  try {
    const res = await api.post('/hapl/HAPL_110S_STR', {
      cmpycd: authStore.cmpycd,
      deptcd: searchForm.deptcd,
      stdym: searchForm.yy + searchForm.mm
    })

    const rawData = res.data || []
    let i = 1, j = 1, k = 1

    const processedData = rawData.map((row: any, index: number) => {
      const code = (row.acctcd || '').toString();
      const name = row.acctnm || '';
      const rstyn = row.rstyn || '';
      const salestot_b = Number(row.salestot_b || 0)
      const salestot_c = Number(row.salestot_c || 0)
      const salestot_t = Number(row.salestot_t || 0)

      const bAmt = Number(row.bamt_l || 0) !== 0 ? Number(row.bamt_l) : Number(row.bamt_r || 0)
      const cAmt = Number(row.camt_l || 0) !== 0 ? Number(row.camt_l) : Number(row.camt_r || 0)
      const tAmt = Number(row.tamt_l || 0) !== 0 ? Number(row.tamt_l) : Number(row.tamt_r || 0)

      let dispName = name
      let isBold = false
      let indent = 0

      if (code.length >= 7) {
        if (code.substring(1, 7) === "000000" || ["1990000", "2980000", "3980000", "3990000"].includes(code)) {
          dispName = `[ ${name} ]`; isBold = true; indent = 2
        } else if (code.substring(2, 7) === "00000") {
          dispName = `${sNUM[i] || i}.${name}`; isBold = true; i++
        } else if (code.substring(3, 7) === "0000") {
          dispName = `${j}).${name}`; indent = 1; j++
        } else if (code.substring(5, 7) === "00") {
          if (rstyn === "Y" || code === "5213000" || code === "5223000") {
            dispName = name; indent = 3
          } else {
            dispName = `${k}.${name}`; indent = 2; k++
          }
        } else {
          dispName = name; indent = 3
        }
      }

      const nextRow = rawData[index + 1]
      if (nextRow) {
        const nextCode = (nextRow.acctcd || '').toString()
        if (code.substring(0, 3) !== nextCode.substring(0, 3)) {
          if (code.substring(0, 2) !== "51") k = 1
        }
        if (code.substring(0, 2) !== nextCode.substring(0, 2)) {
          j = 1
        }
      }

      return {
        DISP_NM: dispName,
        IS_BOLD: isBold,
        INDENT: indent,
        bamt: bAmt,
        brate: salestot_b !== 0 ? (bAmt / salestot_b * 100).toFixed(1) : '0.0',
        camt: cAmt,
        crate: salestot_c !== 0 ? (cAmt / salestot_c * 100).toFixed(1) : '0.0',
        tamt: tAmt,
        trate: salestot_t !== 0 ? (tAmt / salestot_t * 100).toFixed(1) : '0.0'
      }
    })

    mainGrid?.setData(processedData)
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

const print = () => {
  const params = `deptcd=${searchForm.deptcd}&deptnm=${searchForm.deptnm}&yy=${searchForm.yy}&mm=${searchForm.mm}&PRTGU=1`
  window.open(`/hapl/HAPL_110P?${params}`, 'ProfitLossPrint', 'width=1000,height=800,scrollbars=yes')
}

const excel = () => {
  mainGrid?.download("xlsx", `월차손익계산서_${searchForm.deptnm}_${searchForm.yy}${searchForm.mm}.xlsx`)
}

onMounted(() => {
  if (mainGridRef.value) {
    mainGrid = new Tabulator(mainGridRef.value, {
      layout: 'fitColumns', height: '100%',
      columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
      columns: [
        {
          title: "과 목", field: "DISP_NM", widthGrow: 2,
          formatter: (cell) => {
            const d = cell.getData()
            const style = d.IS_BOLD ? 'font-weight:bold;' : ''
            const padding = d.INDENT * 15
            return `<div style="${style} padding-left:${padding}px;">${cell.getValue()}</div>`
          }
        },
        {
          title: "전월누계",
          columns: [
            { title: "금 액", field: "bamt", width: 130, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
            { title: "%", field: "brate", width: 60, hozAlign: "center" }
          ]
        },
        {
          title: "당 월",
          columns: [
            { title: "금 액", field: "camt", width: 130, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "text-primary fw-bold" },
            { title: "%", field: "crate", width: 60, hozAlign: "center" }
          ]
        },
        {
          title: "누 계",
          columns: [
            { title: "금 액", field: "tamt", width: 130, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
            { title: "%", field: "trate", width: 60, hozAlign: "center" }
          ]
        }
      ]
    })
  }
  initData()
})
</script>

<style scoped></style>
