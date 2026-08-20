<!--
	=============================================================
	프로그램명	: 배부기준관리 (HAPL010U)
	작성일자	: 2025.02.24
	설명        : 부문별/품목별 배부기준 및 구성비 관리
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-diagram-3-fill me-2 text-primary" style="font-size: 18px;"></i>
        관리손익 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">배부기준관리 (HAPL010U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="resetForm">신규</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-save" @click="save">저장</button>
        <button class="btn-erp btn-excel" @click="exportExcel">엑셀</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 조회 필터 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
                <col style="width: 15%" /><col style="width: 85%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light">기준구분</th>
                <td>
                  <select v-model="searchForm.divgbn" class="form-select form-select-sm" style="max-width: 250px;" @change="handleSearchChange">
                    <option value="100">부문별 배부</option>
                    <option value="200">품목별 배부</option>
                  </select>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [중간] 상세 입수정 영역 -->
      <div class="card border shadow-sm overflow-hidden flex-shrink-0">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
          <div class="fw-bold small text-dark"><i class="bi bi-pencil-square me-2 text-primary"></i>배부기준 설정</div>
          <div class="d-flex gap-1">
            <div v-if="formData.actkind === 'U0'" class="badge bg-warning text-dark px-2">수정 중</div>
            <div v-else class="badge bg-primary text-white px-2">신규 등록</div>
          </div>
        </div>
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense w-100">
            <colgroup>
              <col style="width: 100px;" /><col />
              <col style="width: 100px;" /><col />
              <col style="width: 100px;" /><col />
            </colgroup>
            <tbody>
              <tr>
                <th class="required bg-light text-center">기준구분</th>
                <td>
                  <select v-model="formData.divgbn" class="form-select form-select-sm">
                    <option value="100">부문별 배부</option>
                    <option value="200">품목별 배부</option>
                  </select>
                </td>
                <th class="required bg-light text-center">배부기준</th>
                <td>
                  <input v-model="formData.divcd" type="text" class="form-control form-control-sm text-center fw-bold text-primary" maxlength="3" :disabled="formData.actkind === 'U0'" />
                </td>
                <th class="required bg-light text-center">배부기준명</th>
                <td>
                  <input v-model="formData.divnm" type="text" class="form-control form-control-sm" />
                </td>
              </tr>
              <tr>
                <th class="required bg-light text-center">구성비 1 (%)</th>
                <td>
                  <input v-model="formData.rate1" type="number" class="form-control form-control-sm text-end" />
                </td>
                <th class="bg-light text-center">구성비 2 (%)</th>
                <td>
                  <input v-model="formData.rate2" type="number" class="form-control form-control-sm text-end" :disabled="formData.divgbn === '200'" />
                </td>
                <th class="bg-light text-center">구성비 3 (%)</th>
                <td>
                  <input v-model="formData.rate3" type="number" class="form-control form-control-sm text-end" :disabled="formData.divgbn === '200'" />
                </td>
              </tr>
              <tr>
                <th class="bg-light text-center">비&nbsp;&nbsp;&nbsp;&nbsp;고</th>
                <td>
                  <input v-model="formData.remark" type="text" class="form-control form-control-sm" maxlength="30" />
                </td>
                <th class="required bg-light text-center">출현순서</th>
                <td>
                  <input v-model="formData.dspord" type="number" class="form-control form-control-sm text-end" />
                </td>
                <th class="bg-light text-center">사용</th>
                <td>
                  <div class="form-check form-switch m-0 d-flex align-items-center justify-content-center h-100">
                    <input v-model="formData.useyn" class="form-check-input mt-0" type="checkbox" true-value="Y" false-value="N" id="useynHapl010">
                    <label class="form-check-label ms-2 small fw-bold" for="useynHapl010">{{ formData.useyn === 'Y' ? '사용' : '중지' }}</label>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center">
          <i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>
          <span class="fw-bold small text-dark">배부기준 목록</span>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="mainGridRef" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import AppAlert from '@/components/AppAlert.vue'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()

const searchForm = reactive({
  divgbn: '100'
})

const formData = reactive({
  actkind: 'A0', divgbn: '100', divcd: '', divnm: '', rate1: 0, rate2: 0, rate3: 0, remark: '', dspord: 0, useyn: 'Y'
})

const mainGridRef = ref<HTMLDivElement | null>(null)
let mainGrid: Tabulator | null = null

const resetForm = () => {
  Object.assign(formData, { actkind: 'A0', divgbn: searchForm.divgbn, divcd: '', divnm: '', rate1: 0, rate2: 0, rate3: 0, remark: '', dspord: 0, useyn: 'Y' })
}

const handleSearchChange = () => { search(); resetForm() }

const search = async () => {
  try {
    const params = {
      actkind: 'S0',
      cmpycd: authStore.cmpycd,
      gubun: '020',
      divgbn: searchForm.divgbn,
      divcd: '',
      divnm: '',
      rate1: 0,
      rate2: 0,
      rate3: 0,
      remark: '',
      dspord: 0,
      useyn: '',
      userid: authStore.userid
    }

    const res = await api.post('/hapl/HAPL_010U_STR', params)

    const data = (res.data || []).map((row: any) => {
      const n: any = {};
      Object.keys(row).forEach(k => n[k.toLowerCase()] = row[k]);

      return {
        divcd: n.divcd,
        divnm: n.divnm,
        rate1: Number(n.srate || n.rate1 || 0),
        rate2: Number(n.mrate || n.rate2 || 0),
        rate3: Number(n.erate || n.rate3 || 0),
        remark: n.remark,
        dspord: n.dspord,
        useyn: n.useyn,
        divgbn: searchForm.divgbn
      }
    })
    mainGrid?.setData(data)
    vAlert('조회되었습니다.')
  } catch (e) {
    vAlertError('조회 중 오류가 발생했습니다.')
  }
}

const save = async () => {
  if (!formData.divcd || !formData.divnm) return vAlertError('배부기준과 명칭을 입력하세요.')

  const rate1 = Number(formData.rate1 || 0)
  const rate2 = Number(formData.rate2 || 0)
  const rate3 = Number(formData.rate3 || 0)
  const dspord = Number(formData.dspord || 0)

  if (formData.divgbn === '100' && (rate1 + rate2 + rate3) !== 100) {
    return vAlertError('구성비의 합은 100이어야 합니다.')
  }

  if (!confirm('저장하시겠습니까?')) return

  try {
    const res = await api.post('/hapl/HAPL_010U_STR', {
      ...formData,
      rate1, rate2, rate3, dspord,
      gubun: '020',
      cmpycd: authStore.cmpycd,
      userid: authStore.userid
    })

    const resData = res.data?.[0] || {}
    const resCode = String(resData.outym || resData.res || resData.col_0 || resData.result || '').trim()
    const resMsg = String(resData.outno || resData.msg || resData.col_1 || '').trim()

    if (resCode === '000000' || resCode === 'N' || resCode === 'ERROR') {
      vAlertError(resMsg || '저장 중 오류가 발생했습니다.');
    } else {
      vAlert('저장되었습니다.');
      search(); resetForm();
    }
  } catch (e: any) {
    vAlertError('저장 실패')
  }
}

const exportExcel = () => {
  if (!mainGrid || mainGrid.getData().length === 0) return vAlertError('조회된 데이터가 없습니다.')
  mainGrid.download("xlsx", `배부기준관리_${searchForm.divgbn}_${new Date().toISOString().slice(0, 10)}.xlsx`, { sheetName: '배부기준' })
}

onMounted(() => {
  if (mainGridRef.value) {
    mainGrid = new Tabulator(mainGridRef.value, {
      layout: 'fitColumns', height: '100%',
      columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
      columns: [
        { title: "배부기준", field: "divcd", width: 90, hozAlign: "center", cssClass: 'fw-bold border-end' },
        { title: "배부기준 명", field: "divnm", widthGrow: 2, cssClass: 'fw-bold text-primary' },
        { title: "구성비 1", field: "rate1", width: 90, hozAlign: "right", formatter: (c) => c.getValue() + '%' },
        { title: "구성비 2", field: "rate2", width: 90, hozAlign: "right", formatter: (c) => c.getValue() + '%' },
        { title: "구성비 3", field: "rate3", width: 90, hozAlign: "right", formatter: (c) => c.getValue() + '%' },
        { title: "비고", field: "remark", widthGrow: 1.5 },
        { title: "순서", field: "dspord", width: 60, hozAlign: "center" },
        { title: "사용", field: "useyn", width: 80, hozAlign: "center",
          formatter: (cell) => {
            const val = String(cell.getValue() || '').trim().toUpperCase();
            return val === 'Y' ? '<b class="text-primary">사용</b>' : '';
          }
        }
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

watch(() => formData.divgbn, (newVal) => { if (newVal === '200') { formData.rate2 = 0; formData.rate3 = 0 } })
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
