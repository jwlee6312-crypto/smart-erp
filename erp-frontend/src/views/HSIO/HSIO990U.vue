<!--
	=============================================================
	프로그램명	: 외부전표전송 (HSIO990U)
	작성일자	: 2025.02.24
	설명        : 외부 정산 내역 기반 더존(THEJONE) 전표 전송 관리 (HSOD100U 디자인 패턴 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom bg-white py-2 px-3 sticky-top shadow-sm">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-send-check-fill me-2 text-primary" style="font-size: 18px;"></i>
        구매정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        외부전표 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">외부 전표전송 (HSIO990U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-2">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchList">조회</button>
        <button class="btn-erp btn-save" @click="handleTransfer">전표전송</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- 🔍 [상단] 조회 필터 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
              <col style="width: 10%" />
              <col style="width: 23%" />
              <col style="width: 10%" />
              <col style="width: 23%" />
              <col style="width: 10%" />
              <col style="width: 24%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light">전표종류</th>
                <td>
                  <select v-model="searchForm.slipkind" class="form-select form-select-sm" @change="fetchList">
                    <option value="000">전체</option>
                    <option v-for="opt in slipKindOptions" :key="opt.code" :value="opt.code">{{ opt.cdnm }}</option>
                  </select>
                </td>
                <th class="text-center bg-light">발행일자</th>
                <td>
                  <div class="d-flex align-items-center gap-1">
                    <DateForm v-model:fromdt="searchForm.fromdt" v-model:todt="searchForm.todt" />
                  </div>
                </td>
                <th class="text-center bg-light">조회대상</th>
                <td>
                  <select v-model="searchForm.cfmyn" class="form-select form-select-sm w-50" @change="fetchList">
                    <option value="N">미전송</option>
                    <option value="Y">전송완료</option>
                  </select>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 📊 3. 메인 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white grid-container-right">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark d-flex align-items-center">
            <i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i> 전표 전송 대상 리스트
          </span>
          <div class="d-flex align-items-center gap-2">
            <span v-if="rowCount" class="badge bg-secondary-subtle text-dark border border-secondary-subtle" style="font-size: 10px;">Total: {{ rowCount }}건</span>
            <button class="btn btn-xs btn-outline-secondary px-2" style="height: 24px; font-size: 11px;" @click="toggleAllRows">전체선택</button>
          </div>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="gridRef" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick, onUnmounted } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { useCommonHelp } from '@/composables/useCommonHelp'
import { getDate } from '@/composables/useDate'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import DateForm from '@/components/DateForm.vue'

const authStore = useAuthStore()
const { firstDay, today } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps } = useCommonHelp()

const searchForm = reactive({
  slipkind: '000',
  fromdt: firstDay,
  todt: today,
  cfmyn: 'N',
  deptcd: authStore.deptcd
})

const slipKindOptions = ref<any[]>([])
const rowCount = ref(0)
const gridRef = ref<HTMLDivElement | null>(null)
let grid: Tabulator | null = null

// [1] 초기 옵션 로드 (전표종류)
async function fetchOptions() {
  try {
    const res = await api.post('/ha00/HA00_00P_STR', { gubun: 'E0', cmpycd: '', search: '180', code: '' })
    slipKindOptions.value = (res.data || []).map((i: any) => ({
      code: i.col0 || i.codecd || '',
      cdnm: i.col1 || i.codenm || ''
    }))
  } catch (e) { console.error('옵션 로드 실패') }
}

// [2] 목록 조회
async function fetchList() {
  try {
    const res = await api.post('/hsio/HSIO_990U_STR', {
      actkind: 'S0',
      cmpycd: authStore.cmpycd,
      slipkind: searchForm.slipkind,
      deptcd: searchForm.deptcd,
      fromdt: searchForm.fromdt.replace(/-/g, ''),
      todt: searchForm.todt.replace(/-/g, ''),
      cfmyn: searchForm.cfmyn,
      updemp: authStore.userid
    })

    const mappedData = (res.data || []).map((i: any) => {
      const item = Object.fromEntries(Object.entries(i).map(([k, v]) => [k.toLowerCase(), v]))
      return {
        ...item,
        procyn: searchForm.cfmyn === 'N', // 미전송 상태일 때 기본 체크
        slip_full: `${item.slipymd}-${item.slipno}`
      }
    })

    grid?.setData(mappedData)
    rowCount.value = mappedData.length
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

// [3] 전표 전송 실행
async function handleTransfer() {
  const selectedData = grid?.getSelectedData().filter((r: any) => r.procyn)
  if (!selectedData || selectedData.length === 0) return vAlertError('전송할 전표를 선택하세요.')

  const msg = searchForm.cfmyn === 'N' ? '선택한 전표를 더존으로 전송하시겠습니까?' : '선택한 전표를 재전송하시겠습니까?'
  if (!confirm(msg)) return

  try {
    for (const item of selectedData) {
      // 🚀 백엔드 'U0' 액션: 외부 시스템(TheZone) 연동 및 로컬 상태 업데이트 수행
      await api.post('/hsio/HSIO_990U_STR', {
        ...item,
        actkind: 'U0',
        cmpycd: authStore.cmpycd,
        slipkind: searchForm.slipkind,
        deptcd: searchForm.deptcd,
        fromdt: searchForm.fromdt.replace(/-/g, ''),
        todt: searchForm.todt.replace(/-/g, ''),
        cfmyn: searchForm.cfmyn,
        updemp: authStore.userid
      })
    }
    vAlert('전송 처리가 완료되었습니다.')
    fetchList()
  } catch (e: any) {
    vAlertError('전송 중 오류가 발생했습니다.')
  }
}

// [4] 기타 유틸리티
const toggleAllRows = () => {
  const rows = grid?.getRows(); if (!rows) return
  const allSelected = rows.every(r => r.getData().procyn)
  rows.forEach(r => r.update({ procyn: !allSelected }))
}

function initialize() {
  resetForm(searchForm);
  searchForm.fromdt = firstDay; searchForm.todt = today;
  searchForm.slipkind = '000'; searchForm.cfmyn = 'N';
  grid?.clearData(); rowCount.value = 0;
}

function openSlipPrint(row: any) {
  const { slipymd, slipno, hdeptcd } = row
  const url = `/HASL/HASL_SLIP_PRINT_OUT?SLIPGU=010&slipymd=${slipymd}&slipno=${slipno}&deptcd=${hdeptcd || authStore.deptcd}`
  window.open(url, '전표인쇄', 'width=800,height=700,scrollbars=yes')
}

onUnmounted(() => {
  if (grid) grid.destroy();
});

onMounted(async () => {
  await fetchOptions()
  if (gridRef.value) {
    grid = new Tabulator(gridRef.value, {
      layout: 'fitColumns', height: '100%',
      columnDefaults: { headerSort: false, headerHozAlign: "center", vertAlign: "middle" },
      columns: [
        {
          title: '선택', field: 'procyn', width: 40, hozAlign: 'center',
          formatter: "tickCross", editor: true
        },
        {
          title: '발행일', field: 'slipymd', width: 110, hozAlign: 'center',
          formatter: (c) => { const v = c.getValue(); return v && v.length === 8 ? `${v.slice(0, 4)}-${v.slice(4, 6)}-${v.slice(6, 8)}` : v }
        },
        { title: '거래처명', field: 'custnm', minWidth: 200, widthGrow: 1, cssClass: 'text-dark fw-bold' },
        { title: '전송상태', field: 'sendstatus', width: 90, hozAlign: 'center' },
        {
          title: '전표번호', field: 'slip_full', width: 130, hozAlign: 'center',
          cssClass: 'text-primary text-decoration-underline cursor-pointer',
          cellClick: (e, cell) => openSlipPrint(cell.getData())
        },
        { title: '적 요', field: 'business', minWidth: 250, widthGrow: 2 },
        { title: '담당자', field: 'usernm', width: 100, hozAlign: 'center' }
      ]
    })
  }
  fetchList()
})
</script>

<style scoped>
.main-content-wrapper { padding-bottom: 0vh !important; }
.erp-table-dense th, .erp-table-dense td {
  height: 32px !important; padding: 0 8px !important; font-size: 12px; vertical-align: middle; border: 1px solid #dee2e6;
}
.erp-table-dense .form-control, .erp-table-dense .form-select, .erp-table-dense .btn {
  height: 26px !important; font-size: 12px !important; border-radius: 2px;
}
.erp-table-dense th { font-weight: 600; color: #495057; }
.grid-container-right { border-bottom: 3px solid #005a9f !important; }
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
