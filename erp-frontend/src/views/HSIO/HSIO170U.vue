<!--
	=============================================================
	프로그램명	: 기타입고정산 (Other Purchase Settlement)
	작성일자	: 2025.02.24
	설명        : 기타 입고 내역을 기반으로 정산 및 전표 발행 (소문자 표준 및 ASP 패턴 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom bg-white py-2 px-3 sticky-top shadow-sm">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-calculator me-2 text-primary" style="font-size: 18px;"></i>
        구매정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        기타입고관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">기타입고정산 (HSIO170U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-2">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchCustList">조회</button>
        <button class="btn-erp btn-save" @click="save">전표발행</button>
      </div>
    </div>

    <!-- 🔍 2. 상단 조회 필터 -->
    <div class="p-2 pb-0 flex-shrink-0 bg-light">
      <div class="card border shadow-sm overflow-hidden">
        <div class="card-body p-2 bg-white">
          <div class="d-flex align-items-center gap-4">
            <div class="d-flex align-items-center gap-2">
              <span class="fw-bold small text-dark" style="min-width: 60px;">입고부서</span>
              <div class="input-group input-group-sm" style="width: 250px;">
                <input v-model="searchForm.deptcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
                <input v-model="searchForm.deptnm" type="text" class="form-control" placeholder="부서 선택" />
                <button class="btn btn-outline-secondary px-2" @click="openHelp('S_DEPT')"><i class="bi bi-search"></i></button>
              </div>
            </div>
            <div class="d-flex align-items-center gap-2">
              <span class="fw-bold small text-dark" style="min-width: 60px;">입고일자</span>
              <DateForm v-model:fromdt="searchForm.fromdt" v-model:todt="searchForm.todt" />
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="d-flex flex-row flex-grow-1 overflow-hidden p-2 gap-2 bg-light">
      <!-- 3-1. 좌측: 거래처 목록 -->
      <div class="card border shadow-sm d-flex flex-column" style="width: 280px; min-width: 280px;">
        <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark">정산 거래처</div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="poGridRef" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>

      <!-- 3-2. 우측: 상세 정보 및 전표 설정 -->
      <div class="flex-grow-1 d-flex flex-column gap-2 overflow-hidden">
        <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
          <div class="card-body p-0 bg-white">
            <table class="erp-table-dense w-100">
              <colgroup>
                <col style="width: 100px;" /><col />
                <col style="width: 100px;" /><col />
                <col style="width: 100px;" /><col />
              </colgroup>
              <tbody>
                <tr>
                  <th class="required bg-light text-center">전표일자</th>
                  <td><input v-model="formData.slipymd" type="date" class="form-control" /></td>
                  <th class="required bg-light text-center">발행부서</th>
                  <td>
                    <div class="input-group input-group-sm">
                      <input v-model="formData.deptcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
                      <input v-model="formData.deptnm" type="text" class="form-control" readonly />
                      <button class="btn btn-outline-secondary px-2" @click="openHelp('DEPT')"><i class="bi bi-search"></i></button>
                    </div>
                  </td>
                  <th class="bg-light text-center">합계금액</th>
                  <td><input :value="formatNumber(targetTotalSum)" class="form-control text-end bg-light fw-bold text-danger" readonly /></td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white grid-container-right">
          <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
            <span class="fw-bold small text-dark d-flex align-items-center">
              <i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i> 정산 대상 목록
            </span>
            <button class="btn btn-xs btn-outline-secondary px-2" style="height: 24px; font-size: 11px;" @click="toggleAllRows">전체선택</button>
          </div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="itemGridRef" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch, onUnmounted } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { useCommonHelp } from '@/composables/useCommonHelp'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import DateForm from '@/components/DateForm.vue'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps, openHelp: openCommonHelp } = useCommonHelp()

const searchForm = reactive({
  deptcd: authStore.deptcd, deptnm: authStore.deptnm,
  fromdt: new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString().substring(0, 10),
  todt: new Date().toISOString().substring(0, 10)
})

const formData = reactive<any>({
  slipymd: new Date().toISOString().substring(0, 10),
  deptcd: authStore.deptcd, deptnm: authStore.deptnm,
  custcd: '', custnm: ''
})

const targetTotalSum = ref(0)
const poGridRef = ref<HTMLDivElement | null>(null); const itemGridRef = ref<HTMLDivElement | null>(null)
let poGrid: Tabulator | null = null; let itemGrid: Tabulator | null = null

const fetchCustList = async () => {
  try {
    const res = await api.post('/hsio/HSIO_170U_STR', {
      actkind: 'S1', cmpycd: authStore.cmpycd, iogbn: '100',
      fromdt: searchForm.fromdt.replace(/-/g, ''),
      todt: searchForm.todt.replace(/-/g, ''),
      deptcd: searchForm.deptcd
    })
    poGrid?.setData(res.data || [])
    itemGrid?.clearData();
  } catch (e) { vAlertError('조회 실패') }
}

const fetchDetail = async (row: any) => {
  formData.custcd = row.custcd; formData.custnm = row.custnm;
  try {
    const res = await api.post('/hsio/HSIO_170U_STR', {
      actkind: 'S0', cmpycd: authStore.cmpycd, iogbn: '100',
      fromdt: searchForm.fromdt.replace(/-/g, ''),
      todt: searchForm.todt.replace(/-/g, ''),
      custcd: row.custcd, deptcd: searchForm.deptcd
    })
    itemGrid?.setData(res.data || [])
  } catch (e) { vAlertError('상세 조회 실패') }
}

/**
 * 🚀 저장 로직 (ASP 패턴: 마스터 전표 생성 -> 상세 루프 업데이트)
 */
const save = async () => {
  const items = itemGrid?.getData().filter((r: any) => r.procyn === 'Y') || []
  if (items.length === 0) return vAlertError('발행할 항목을 선택하세요.')

  if (!confirm('선택한 항목들에 대해 전표를 발행하시겠습니까?')) return

  try {
    const slipYmd = formData.slipymd.replace(/-/g, '')

    // 🚀 Step 1. 전표 마스터 생성
    const resMst = await api.post('/hasl/HASL_010U_STR', {
      actkind: 'A', cmpycd: authStore.cmpycd, slipymd: slipYmd,
      deptcd: formData.deptcd, empnm: authStore.usernm,
      slipkind: '030', business: '기타입고 매입 정산 건', updemp: authStore.userid
    })
    const slipno = resMst.data?.[0]?.slipno
    if (!slipno || slipno === '000000') throw new Error('전표 마스터 생성 실패')

    // 🚀 Step 2. 상세 루프: 수입 상태 업데이트 및 전표 상세 생성
    for (const item of items) {
      // 수입 데이터에 전표번호 연결
      await api.post('/hsio/HSIO_170U_STR', {
        actkind: 'U0', cmpycd: authStore.cmpycd, iogbn: '100',
        ioym: item.ioym, iono: item.iono, iorowno: item.iorowno,
        slipymd: slipYmd, slipno: slipno, updemp: authStore.userid
      })
    }

    vAlert('성공적으로 전표가 발행되었습니다.')
    fetchCustList(); initialize();
  } catch (e: any) { vAlertError(e.message || '발행 실패') }
}

const toggleAllRows = () => {
  const rows = itemGrid?.getRows(); if (!rows) return
  const allSelected = rows.every(r => r.getData().procyn === 'Y')
  rows.forEach(r => r.update({ procyn: allSelected ? 'N' : 'Y' }))
}

function initialize() {
  resetForm(formData); poGrid?.clearData(); itemGrid?.clearData();
  formData.slipymd = new Date().toISOString().substring(0, 10);
  formData.deptcd = authStore.deptcd; formData.deptnm = authStore.deptnm;
}

function openHelp(type: string) {
  if (type === 'DEPT' || type === 'S_DEPT') {
    openCommonHelp('DEPT', (d) => {
      if (type === 'DEPT') { formData.deptcd = d.deptcd; formData.deptnm = d.deptnm }
      else { searchForm.deptcd = d.deptcd; searchForm.deptnm = d.deptnm }
    })
  }
}

onUnmounted(() => {
  if (poGrid) poGrid.destroy();
  if (itemGrid) itemGrid.destroy();
});

onMounted(() => {
  if (poGridRef.value) {
    poGrid = new Tabulator(poGridRef.value, {
      layout: 'fitColumns', height: '100%', selectable: 1,
      columns: [{ title: '입고 거래처', field: 'custnm', cssClass: 'fw-bold text-dark', hozAlign: 'left' }]
    })
    poGrid.on('rowClick', (e, row) => fetchDetail(row.getData()))
  }
  if (itemGridRef.value) {
    itemGrid = new Tabulator(itemGridRef.value, {
      layout: 'fitColumns', height: '100%', selectable: true,
      columnDefaults: { headerSort: false, vertAlign: "middle" },
      columns: [
        { title: '선택', field: 'procyn', hozAlign: 'center', width: 60, formatter: 'tickCross', editor: true },
        { title: '입고일', field: 'ioymd', width: 110, formatter: (c) => c.getValue()?.replace(/(\d{4})(\d{2})(\d{2})/, '$1-$2-$3') },
        { title: '입고번호', field: 'iono', width: 120 },
        { title: '품목명', field: 'itemnm', minWidth: 150, widthGrow: 1, cssClass: 'fw-bold' },
        { title: '금액', field: 'ioamt', hozAlign: 'right', width: 120, formatter: 'money', formatterParams: { precision: 0 } }
      ]
    })
    itemGrid.on('rowSelectionChanged', (data) => {
        targetTotalSum.value = data.reduce((acc, cur: any) => acc + (Number(cur.ioamt) || 0), 0)
    })
  }
  fetchCustList()
})

const formatNumber = (val: any) => Number(val || 0).toLocaleString()
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
