<!--
	=============================================================
	프로그램명	: 수입전표취소 [디자인 표준 통합]
	작성일자	: 2025.02.24
	설명        : HSOD100U 디자인 패턴 이식 및 ASP 패턴 기반 순차 취소 로직 적용
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom bg-white py-2 px-3 sticky-top shadow-sm">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-file-earmark-x-fill me-2 text-danger" style="font-size: 18px;"></i>
        수입관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">수입전표취소 (HSIP150U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-2">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchList">조회</button>
        <button class="btn-erp btn-save" @click="save">전표 취소</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- 🔍 [상단] 조회 필터 영역 (HSOD100U 디자인 패턴) -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
              <col style="width: 10%" />
              <col style="width: 40%" />
              <col style="width: 10%" />
              <col style="width: 40%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light">발생부서</th>
                <td>
                  <div class="input-group input-group-sm w-75">
                    <input v-model="searchForm.deptcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
                    <input v-model="searchForm.deptnm" type="text" class="form-control" placeholder="부서 선택" />
                    <button class="btn btn-outline-secondary px-2" @click="openHelp('DEPT')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="text-center bg-light">발생일자</th>
                <td class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                  <DateForm v-model:fromdt="searchForm.ioymdfr" v-model:todt="searchForm.ioymdto" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 📊 3. 발행 전표 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white grid-container-right">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark d-flex align-items-center">
            <i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i> 취소 대상 수입 전표 목록
          </span>
          <div class="d-flex align-items-center gap-2">
            <div class="form-check form-switch m-0 d-flex align-items-center">
              <input v-model="autoSlip" class="form-check-input" type="checkbox" true-value="Y" false-value="N" id="autoSlipSwitch">
              <label class="form-check-label ms-2 small fw-bold text-secondary" for="autoSlipSwitch">자동 전표 연동</label>
            </div>
            <span class="text-muted" style="font-size: 11px;">※ 행 선택 후 '전표 취소' 버튼을 누르세요.</span>
          </div>
        </div>
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
const { modalVisible, modalProps, openHelp: openCommonHelp } = useCommonHelp()

const searchForm = reactive({
  deptcd: authStore.deptcd, deptnm: authStore.deptnm,
  ioymdfr: firstDay,
  ioymdto: today
})

const autoSlip = ref('N')
const mainGridRef = ref<HTMLDivElement | null>(null); let mainGrid: Tabulator | null = null

const fetchList = async () => {
  try {
    const params = {
      actkind: 'S0',
      cmpycd: authStore.cmpycd,
      deptcd: searchForm.deptcd,
      fromdt: searchForm.ioymdfr.replace(/-/g, ''),
      todt: searchForm.ioymdto.replace(/-/g, '')
    }
    const res = await api.post('/hsip/HSIP_150U_STR', params)
    mainGrid?.setData(res.data || [])
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

/**
 * 🚀 전표 취소 로직 (통합 백엔드 서비스 방식)
 */
const save = async () => {
  const selected = mainGrid?.getSelectedData()
  if (!selected || selected.length === 0) return vAlertError('취소할 항목을 선택하세요.')

  if (!confirm(`선택한 ${selected.length}건의 전표를 일괄 취소하시겠습니까?`)) return

  const payload = {
    cmpycd: authStore.cmpycd,
    fromdt: searchForm.ioymdfr,
    todt: searchForm.ioymdto,
    autoSlip: autoSlip.value,
    items: selected.map(item => ({
      slipymd: item.slipymd,
      slipno: item.slipno,
      deptcd: item.udeptcd || item.deptcd
    }))
  }

  try {
    const res = await api.post('/hsip/HSIP_150U_CANCEL', payload)
    if (res.data) {
      vAlert('전표 취소가 성공적으로 완료되었습니다.')
      fetchList()
    }
  } catch (e: any) {
    vAlertError('취소 실패: ' + (e.response?.data?.message || e.message))
  }
}

const initialize = () => {
  resetForm(searchForm);
  mainGrid?.clearData();
}

function openHelp(type: string) {
  if (type === 'DEPT') {
    openCommonHelp('DEPT', (d) => { searchForm.deptcd = d.deptcd; searchForm.deptnm = d.deptnm })
  }
}

onMounted(async () => {
  // 전표 환경 설정 체크 (ASP: HA00_010S_STR 'P1')
  try {
    const resSet = await api.post('/ha00/HA00_010S_STR', { cmpycd: authStore.cmpycd, gbn: 'P1' })
    if (resSet.data?.length > 0) autoSlip.value = resSet.data[0].slipyn || 'N'
  } catch (e) {}

  if (mainGridRef.value) {
    mainGrid = new Tabulator(mainGridRef.value, {
      layout: 'fitColumns', height: '100%', selectable: true,
      columnDefaults: { headerSort: false, headerHozAlign: "center", hozAlign: "center", vertAlign: "middle", minWidth: 100 },
      columns: [
        { title: "선택", formatter: "rowSelection", titleFormatter: "rowSelection", width: 60, hozAlign: "center" },
        { title: "전표일자", field: "slipymd", width: 110, formatter: (c) => {
            const v = c.getValue(); return v && v.length === 8 ? `${v.substring(0,4)}-${v.substring(4,6)}-${v.substring(6,8)}` : v;
        }},
        { title: "전표번호", field: "slipno", width: 100, cssClass: "fw-bold text-primary" },
        { title: "부서명", field: "deptnm", width: 250 },
        { title: "거래처", field: "custnm", width: 250 },
        { title: "비용금액", field: "costamt", hozAlign: "right", width: 130, formatter: "money", formatterParams: { precision: 0 } },
        { title: "비고", field: "bigo", minWidth: 200, widthGrow: 1, hozAlign: "left" }
      ]
    })
  }
  fetchList()
})
</script>

<style scoped>
.main-content-wrapper { padding-bottom: 0vh !important; }
.grid-container-right { border-bottom: 3px solid #005a9f !important; }
.erp-table-dense th, .erp-table-dense td {
  height: 32px !important; padding: 0 8px !important; font-size: 12px; vertical-align: middle; border: 1px solid #dee2e6;
}
.erp-table-dense .form-control, .erp-table-dense .form-select, .erp-table-dense .btn {
  height: 26px !important; font-size: 12px !important; border-radius: 2px;
}
.erp-table-dense th { font-weight: 600; color: #495057; }
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
