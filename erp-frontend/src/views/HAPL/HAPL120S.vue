<!--
	=============================================================
	프로그램명	: 배부현황 (HAPL120S)
	작성일자	: 2025.02.24
	작성자	    : AI Assistant
	설명        : 부서/계정별 비용 배부 상세 내역 조회
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-share-fill me-2 text-primary" style="font-size: 18px;"></i>
        관리손익 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">배부현황 (HAPL120S)</span>
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
                <th class="required">배부연월</th>
                <td>
                  <div class="d-flex align-items-center gap-1">
                    <select v-model="searchForm.yy" class="form-select" style="width: 110px;">
                      <option v-for="y in yearOptions" :key="y" :value="y">{{ y }}년</option>
                    </select>
                    <select v-model="searchForm.mm" class="form-select" style="width: 90px;">
                      <option v-for="m in monthOptions" :key="m" :value="m">{{ m }}월</option>
                    </select>
                  </div>
                </td>
                <th>계정과목</th>
                <td>
                  <div class="d-flex align-items-center gap-1">
                    <div class="input-group input-group-sm flex-nowrap" style="width: 200px;">
                      <input v-model="searchForm.acctcdfr" type="text" class="form-control text-center bg-light fw-bold" style="max-width: 60px;" readonly />
                      <input v-model="searchForm.acctcdfrnm" type="text" class="form-control border-start-0" placeholder="시작 계정" @keyup.enter="openHelp('ACCT_FR')" />
                      <button class="btn btn-outline-secondary px-2" @click="openHelp('ACCT_FR')"><i class="bi bi-search"></i></button>
                    </div>
                    <span>~</span>
                    <div class="input-group input-group-sm flex-nowrap" style="width: 200px;">
                      <input v-model="searchForm.acctcdto" type="text" class="form-control text-center bg-light fw-bold" style="max-width: 60px;" readonly />
                      <input v-model="searchForm.acctcdtonm" type="text" class="form-control border-start-0" placeholder="종료 계정" @keyup.enter="openHelp('ACCT_TO')" />
                      <button class="btn btn-outline-secondary px-2" @click="openHelp('ACCT_TO')"><i class="bi bi-search"></i></button>
                    </div>
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
const { modalVisible, modalProps, openHelp: commonOpenHelp } = useCommonHelp()

const currentYear = new Date().getFullYear()
const currentMonth = (new Date().getMonth() + 1).toString().padStart(2, '0')
const yearOptions = Array.from({ length: 20 }, (_, i) => String(currentYear - i))
const monthOptions = Array.from({ length: 12 }, (_, i) => String(i + 1).padStart(2, '0'))

const searchForm = reactive({
  yy: String(currentYear),
  mm: currentMonth,
  acctcdfr: '',
  acctcdfrnm: '',
  acctcdto: '',
  acctcdtonm: ''
})

const mainGridRef = ref<HTMLDivElement | null>(null)
let mainGrid: Tabulator | null = null

const search = async () => {
  try {
    const res = await api.post('/hapl/HAPL_120S_STR', {
      cmpycd: authStore.cmpycd,
      stdym: searchForm.yy + searchForm.mm,
      acctcdfr: searchForm.acctcdfr,
      acctcdto: searchForm.acctcdto
    })

    const data = (res.data || []).map((row: any) => {
      const diramt = Number(row.dircost || row.DIRCOST || 0);
      const idiramt = Number(row.idircost || row.IDIRCOST || 0);

      return {
        bfdeptcd: row.bfdeptcd || '',
        bfdeptnm: row.bfdeptnm || '',
        acctcd: row.acctcd || '',
        acctnm: row.acctnm || '',
        afdeptnm: row.afdeptnm || '',
        diramt: diramt,
        idiramt: idiramt,
        total: diramt + idiramt
      }
    })

    mainGrid?.setData(data)
    vAlert('조회되었습니다.')
  } catch (e) {
    vAlertError('조회 실패')
  }
}

const print = () => {
  const params = `acctcdfr=${searchForm.acctcdfr}&acctcdto=${searchForm.acctcdto}&ym=${searchForm.yy}${searchForm.mm}&PRTGU=1`
  window.open(`/hapl/HAPL_120P?${params}`, 'AllocationPrint', 'width=1000,height=800,scrollbars=yes')
}

const excel = () => {
  mainGrid?.download("xlsx", `배부현황_${searchForm.yy}${searchForm.mm}.xlsx`)
}

const openHelp = (type: string) => {
  if (type.startsWith('ACCT')) {
    commonOpenHelp('ACCT', (d) => {
      if (type === 'ACCT_FR') { searchForm.acctcdfr = d.acctcd; searchForm.acctcdfrnm = d.acctnm }
      else { searchForm.acctcdto = d.acctcd; searchForm.acctcdtonm = d.acctnm }
    })
  }
}


onMounted(() => {
  if (mainGridRef.value) {
    mainGrid = new Tabulator(mainGridRef.value, {
      layout: 'fitColumns', height: '100%',
      columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
      groupBy: "bfdeptcd",
      groupHeader: (value, count, data) => {
        return `<span class="fw-bold text-dark">${data[0].bfdeptnm}</span> <span class="badge bg-secondary ms-2">${count}건</span>`;
      },
      columns: [
        { title: "계정과목", field: "acctcd", width: 100, hozAlign: "center" },
        { title: "계정과목 명", field: "acctnm", width: 200, cssClass: 'fw-bold' },
        { title: "배부후 부서", field: "afdeptnm", widthGrow: 1 },
        { title: "직접비", field: "diramt", width: 130, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, bottomCalc: "sum" },
        { title: "간접비", field: "idiramt", width: 130, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, bottomCalc: "sum" },
        { title: "합 계", field: "total", width: 140, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, bottomCalc: "sum", cssClass: "bg-light fw-bold text-primary" }
      ]
    })
  }
  search()
})
</script>

<style scoped></style>
