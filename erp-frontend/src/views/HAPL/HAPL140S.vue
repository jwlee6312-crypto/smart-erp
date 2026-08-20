<!--
	=============================================================
	프로그램명	: 부서별 손익계산서 (HAPL140S)
	작성일자	: 2025.02.24
	작성자	    : AI Assistant
	설명        : 부서별 월차 손익 현황 가로 비교 조회
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-columns-gap me-2 text-primary" style="font-size: 18px;"></i>
        관리손익 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">부서별 손익계산서 (HAPL140S)</span>
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
  } catch (e) { console.error('초기화 실패') }
}

const search = async () => {
  if (!searchForm.deptcd) return vAlertError('부서를 선택하세요.')

  try {
    const resDept = await api.post('/hapl/HAPL_140S_STR', {
      actkind: 'D',
      cmpycd: authStore.cmpycd,
      deptcd: searchForm.deptcd,
      stdym: searchForm.yy + searchForm.mm
    })
    const departments = resDept.data || []

    const resData = await api.post('/hapl/HAPL_140S_STR', {
      actkind: 'S',
      cmpycd: authStore.cmpycd,
      deptcd: searchForm.deptcd,
      stdym: searchForm.yy + searchForm.mm
    })
    const rawData = resData.data || []

    const processedMap = new Map()
    const salesTotalMap = new Map()

    rawData.forEach((row: any) => {
      const acctCd = (row.acctcd || '').toString();
      if (!acctCd) return;

      if (!processedMap.has(acctCd)) {
        processedMap.set(acctCd, {
          acctcd: acctCd,
          acctnm: row.acctnm || '',
          rstyn: row.rstyn || '',
          total_amt: 0,
          deptAmts: {}
        })
      }
      const acctObj = processedMap.get(acctCd)
      const deptCd = (row.deptcd || '').toString()
      const amt = Number(row.amt || 0)

      if (acctCd === "5100000") {
        salesTotalMap.set(deptCd, (salesTotalMap.get(deptCd) || 0) + amt)
        salesTotalMap.set('total', (salesTotalMap.get('total') || 0) + amt)
      }

      if (deptCd) {
        acctObj.deptAmts[deptCd] = (acctObj.deptAmts[deptCd] || 0) + amt
      }
      acctObj.total_amt += amt
    })

    let i = 1, j = 1, k = 1
    const gridData = Array.from(processedMap.values()).map((row: any, index: number, arr: any[]) => {
      const code = row.acctcd
      const name = row.acctnm

      let dispName = name
      let isBold = false
      let indent = 0

      if (code && code.length >= 7) {
        if (code.substring(1, 7) === "000000" || ["1990000", "2980000", "3980000", "3990000"].includes(code)) {
          dispName = `[ ${name} ]`; isBold = true; indent = 1
        } else if (code.substring(2, 7) === "00000") {
          dispName = `${sNUM[i] || i}.${name}`; isBold = true; i++
        } else if (code.substring(3, 7) === "0000") {
          dispName = `${j}).${name}`; indent = 1; j++
        } else if (code.substring(5, 7) === "00") {
          if (row.rstyn === "Y" || code === "5213000" || code === "5223000") {
            dispName = name; indent = 3
          } else {
            dispName = `${k}.${name}`; indent = 2; k++
          }
        } else {
          dispName = name; indent = 3
        }
      }

      const nextRow = arr[index + 1]
      if (nextRow && nextRow.acctcd) {
        if (code.substring(0, 3) !== nextRow.acctcd.substring(0, 3)) {
          if (code.substring(0, 2) !== "51") k = 1
        }
        if (code.substring(0, 2) !== nextRow.acctcd.substring(0, 2)) j = 1
      }

      const totalSales = salesTotalMap.get('total') || 0
      const rowResult: any = {
        DISP_NM: dispName,
        IS_BOLD: isBold,
        INDENT: indent,
        total_amt: row.total_amt,
        total_rate: totalSales !== 0 ? (row.total_amt / totalSales * 100).toFixed(1) : '0.0'
      }

      departments.forEach((d: any) => {
        const dAmt = row.deptAmts[d.deptcd] || 0
        const dSales = salesTotalMap.get(d.deptcd) || 0
        rowResult[`amt_${d.deptcd}`] = dAmt
        rowResult[`rate_${d.deptcd}`] = dSales !== 0 ? (dAmt / dSales * 100).toFixed(1) : '0.0'
      })

      return rowResult
    })

    const columns: any[] = [
      {
        title: "과 목", field: "DISP_NM", width: 220, frozen: true,
        formatter: (cell: any) => {
          const d = cell.getData()
          return `<div style="${d.IS_BOLD ? 'font-weight:bold;' : ''} padding-left:${d.INDENT * 15}px;">${cell.getValue()}</div>`
        }
      },
      {
        title: "합 계",
        columns: [
          { title: "금 액", field: "total_amt", width: 120, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
          { title: "%", field: "total_rate", width: 60, hozAlign: "center" }
        ]
      }
    ]

    departments.forEach((d: any) => {
      columns.push({
        title: d.deptnm,
        columns: [
          { title: "금 액", field: `amt_${d.deptcd}`, width: 110, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
          { title: "%", field: `rate_${d.deptcd}`, width: 55, hozAlign: "center" }
        ]
      })
    })

    mainGrid?.setColumns(columns)
    mainGrid?.setData(gridData)
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

const print = () => {
  const params = `deptcd=${searchForm.deptcd}&deptnm=${searchForm.deptnm}&yy=${searchForm.yy}&mm=${searchForm.mm}&PRTGU=1`
  window.open(`/hapl/HAPL_140P?${params}`, 'DeptProfitLossPrint', 'width=1200,height=800,scrollbars=yes')
}

const excel = () => {
  mainGrid?.download("xlsx", `부서별손익계산서_${searchForm.yy}${searchForm.mm}.xlsx`)
}

onMounted(() => {
  if (mainGridRef.value) {
    mainGrid = new Tabulator(mainGridRef.value, {
      layout: 'fitColumns', height: '100%',
      columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" }
    })
  }
  initData()
})
</script>

<style scoped></style>
