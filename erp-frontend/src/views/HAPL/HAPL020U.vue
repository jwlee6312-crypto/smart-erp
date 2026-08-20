<!--
	=============================================================
	프로그램명	: 계정별 배부기준관리 (HAPL020U)
	작성일자	: 2025.02.24
	작성자	    : AI Assistant
	설명        : 계정과목별 부문 및 품목 배부기준 설정 관리
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-list-check me-2 text-primary" style="font-size: 18px;"></i>
        관리손익 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        <span class="text-primary fw-bolder">계정별 배부기준관리 (HAPL020U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1">
        <button class="btn-erp btn-init" @click="resetForm">신규</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-save" @click="save">저장</button>
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
            </colgroup>
            <tbody>
              <tr>
                <th>기준연월</th>
                <td>
                  <div class="d-flex align-items-center gap-1">
                    <select v-model="searchForm.yy" class="form-select" style="width: 100px;" @change="handleymChange">
                      <option v-for="y in yearOptions" :key="y" :value="y">{{ y }}년</option>
                    </select>
                    <select v-model="searchForm.mm" class="form-select" style="width: 80px;" @change="handleymChange">
                      <option v-for="m in monthOptions" :key="m" :value="m">{{ m }}월</option>
                    </select>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 📝 입력 정보 카드 -->
      <div class="card border-0 shadow-sm overflow-hidden flex-shrink-0">
        <div class="card-header bg-white py-2 px-3 border-bottom">
          <div class="fw-bold small text-dark"><i class="bi bi-pencil-square me-1 text-secondary"></i>배부기준 설정</div>
        </div>
        <div class="card-body p-0 bg-white">
          <table class="erp-table-full border-0">
            <colgroup>
              <col style="width: 110px;" /><col />
              <col style="width: 110px;" /><col />
              <col style="width: 110px;" /><col />
            </colgroup>
            <tbody>
              <tr>
                <th>계정과목</th>
                <td>
                  <div class="input-group input-group-sm">
                    <input v-model="formData.acctcd" type="text" class="form-control text-center bg-light fw-bold" style="max-width: 80px;" readonly />
                    <input v-model="formData.acctnm" type="text" class="form-control bg-light" readonly />
                  </div>
                </td>
                <th class="required">부문배부</th>
                <td>
                  <select v-model="formData.deptdivcd" class="form-select">
                    <option v-for="opt in deptDivOptions" :key="opt.divcd" :value="opt.divcd">{{ opt.divnm }}</option>
                  </select>
                </td>
                <th class="required">품목배부</th>
                <td>
                  <select v-model="formData.itemdivcd" class="form-select">
                    <option v-for="opt in itemDivOptions" :key="opt.divcd" :value="opt.divcd">{{ opt.divnm }}</option>
                  </select>
                </td>
              </tr>
              <tr>
                <th>비&nbsp;&nbsp;&nbsp;&nbsp;고</th>
                <td colspan="3">
                  <input v-model="formData.remark" type="text" class="form-control" maxlength="30" />
                </td>
                <th>삭제구분</th>
                <td>
                  <div class="form-check mb-0">
                    <input v-model="formData.useyn" class="form-check-input" type="checkbox" id="useynCheckHapl020" true-value="N" false-value="Y">
                    <label class="form-check-label small fw-bold text-danger" for="useynCheckHapl020">삭제</label>
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
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import * as XLSX from 'xlsx'
import AppAlert from '@/components/AppAlert.vue'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()

const currentYear = new Date().getFullYear()
const currentMonth = (new Date().getMonth() + 1).toString().padStart(2, '0')
const yearOptions = Array.from({ length: 11 }, (_, i) => String(currentYear - i))
const monthOptions = Array.from({ length: 12 }, (_, i) => String(i + 1).padStart(2, '0'))

const searchForm = reactive({ yy: String(currentYear), mm: currentMonth })
const formData = reactive({ actkind: 'A0', acctcd: '', acctnm: '', deptdivcd: '', itemdivcd: '', remark: '', useyn: 'Y' })

const deptDivOptions = ref<any[]>([])
const itemDivOptions = ref<any[]>([])

const mainGridRef = ref<HTMLDivElement | null>(null)
let mainGrid: Tabulator | null = null

const fetchOptions = async () => {
  try {
    const [resDept, resItem] = await Promise.all([
      api.post('/ha00/HA00_00P_STR', { gubun: 'SB', cmpycd: authStore.cmpycd, gbncd: '100' }),
      api.post('/ha00/HA00_00P_STR', { gubun: 'SB', cmpycd: authStore.cmpycd, gbncd: '200' })
    ])

    const mapper = (data: any[]) => (data || []).map(i => ({
      divcd: i.divcd || i.code || '',
      divnm: i.divnm || i.name || i.cdnm || ''
    }));

    deptDivOptions.value = mapper(resDept.data)
    itemDivOptions.value = mapper(resItem.data)
  } catch (e) { console.error('HAPL020U Options Error:', e) }
}

const resetForm = () => {
  Object.assign(formData, {
    actkind: 'A0', acctcd: '', acctnm: '',
    deptdivcd: deptDivOptions.value[0]?.divcd || '',
    itemdivcd: itemDivOptions.value[0]?.divcd || '',
    remark: '', useyn: 'Y'
  })
}

const handleymChange = () => search()

const search = async () => {
  try {
    const res = await api.post('/hapl/HAPL_020U_STR', {
        actkind: 'S0',
        cmpycd: authStore.cmpycd,
        stdym: searchForm.yy + searchForm.mm,
        userid: authStore.userid
    })

    const rawData = res.data || []
    const normalizedData = rawData.map((row: any) => {
      const n: any = {};
      Object.keys(row).forEach(k => n[k.toLowerCase()] = row[k]);
      return n;
    })

    mainGrid?.setData(normalizedData)
    if (normalizedData.length > 0) vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

const save = async () => {
  if (!formData.acctcd) return vAlertError('계정과목을 선택하세요.')
  if (!confirm('저장하시겠습니까?')) return

  try {
    const res = await api.post('/hapl/HAPL_020U_STR', {
      ...formData,
      cmpycd: authStore.cmpycd,
      stdym: (searchForm.yy + searchForm.mm).replace(/-/g, ''),
      userid: authStore.userid
    })

    const resData = res.data?.[0] || {}
    const resCode = String(resData.res || resData.result || resData.col_0 || '').trim()
    const resMsg = String(resData.msg || resData.message || resData.col_1 || '').trim()

    if (resCode === '000000' || resCode === 'N' || resCode === 'ERROR') {
      vAlertError(resMsg || '저장 오류가 발생했습니다.')
    } else {
      vAlert('저장되었습니다.')
      search()
    }
  } catch (e: any) {
    vAlertError('저장 실패')
  }
}

const excel = () => mainGrid?.download("xlsx", `계정별배부기준_${searchForm.yy}${searchForm.mm}.xlsx`)


onMounted(() => {
  if (typeof window !== 'undefined') (window as any).XLSX = XLSX
  fetchOptions()
  if (mainGridRef.value) {
    mainGrid = new Tabulator(mainGridRef.value, {
      layout: 'fitColumns', height: '100%',
      columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
      columns: [
        { title: "계정과목", field: "acctcd", width: 100, hozAlign: "center" },
        { title: "계정과목 명", field: "acctnm", widthGrow: 1.5, cssClass: 'fw-bold text-primary' },
        { title: "부문 배부기준", field: "deptdivnm", width: 150 },
        { title: "품목 배부기준", field: "itemdivnm", width: 150 },
        { title: "비고", field: "remark", widthGrow: 1.5 }
      ]
    })


    mainGrid.on("rowClick", (e, row) => {
        const d = row.getData();
        Object.assign(formData, d);
        formData.actkind = 'U0'
    })
  }
  search()
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
