<!--
	=============================================================
	프로그램명	: 외부 입금전표발행 (HSIO325U)
	작성일자	: 2025.02.24
	설명        : 입금 내역을 기반으로 회계 전표 발행 (ASP 패턴 기반 순차 저장 로직 및 소문자 통일)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-file-earmark-post me-2 text-primary" style="font-size: 18px;"></i>
        영업정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        입금관리 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        <span class="text-primary fw-bolder">외부 입금전표발행 (HSIO325U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-save" @click="issueSlip">전표발행</button>
        <button class="btn-erp btn-danger" @click="deleteSlip">전표삭제</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">
      <!-- 🅰️ 조회 조건 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
              <col style="width: 10%" /><col style="width: 23%" />
              <col style="width: 10%" /><col style="width: 23%" />
              <col style="width: 10%" /><col style="width: 24%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="required bg-light text-center">입금부서</th>
                <td>
                  <div class="input-group input-group-sm w-75">
                    <input v-model="searchdata.deptcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
                    <input v-model="searchdata.deptnm" type="text" class="form-control" placeholder="부서 선택" />
                    <button class="btn btn-outline-secondary px-2" @click="openHelp('DEPT')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="required bg-light text-center">입금일자</th>
                <td class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                  <DateForm v-model:fromdt="searchdata.fromdt" v-model:todt="searchdata.todt" />
                </td>
                <th class="bg-light text-center">영업사원</th>
                <td>
                  <select v-model="searchdata.salsemp" class="form-select form-select-sm">
                    <option value="">전체</option>
                    <option v-for="opt in empOptions" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                  </select>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 🅱️ 전표 발행 설정 -->
      <div class="card border shadow-sm overflow-hidden flex-shrink-0">
        <div class="card-header py-1 px-3 border-bottom d-flex align-items-center" style="background-color: #f0f7ff !important;">
          <span class="fw-bold small text-primary"><i class="bi bi-pencil-square me-1"></i> 전표 발행 설정</span>
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
                <th class="required bg-light text-center">전표일자</th>
                <td>
                  <input v-model="slipmaster.slipymd" type="date" class="form-control" />
                </td>
                <th class="required bg-light text-center">발행부서</th>
                <td colspan="3">
                  <div class="input-group input-group-sm" style="width: 250px;">
                    <input v-model="slipmaster.deptcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
                    <input v-model="slipmaster.deptnm" type="text" class="form-control" readonly />
                    <button class="btn btn-outline-secondary px-2" @click="openHelp('SLIP_DEPT')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Ⓒ 데이터 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white grid-container-right">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark d-flex align-items-center">
            <i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i> 입금 내역 리스트
          </span>
          <div class="d-flex align-items-center gap-2">
            <span class="text-danger fw-bold small">선택: {{ activeitemcount }} 건 / {{ formatNumber(sumtotal) }} 원</span>
            <button class="btn btn-xs btn-outline-secondary px-2" style="height: 22px; font-size: 11px;" @click="toggleAllSelection">전체선택</button>
          </div>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="gridElement" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>
    </div>
  </div>

  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, computed, nextTick, onUnmounted } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import DateForm from '@/components/DateForm.vue'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { useCommonHelp } from '@/composables/useCommonHelp'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps, openHelp: openCommonHelp } = useCommonHelp()

const now = new Date()
const initymd = now.toISOString().substring(0, 10)
const initfromdt = new Date(now.getFullYear(), now.getMonth(), 1).toISOString().substring(0, 10)

// [1] 데이터 모델링 (소문자 통일)
const searchdata = reactive({
  deptcd: authStore.deptcd, deptnm: authStore.deptnm,
  fromdt: initfromdt, todt: initymd, salsemp: ''
})

const slipmaster = reactive({
  slipymd: initymd, deptcd: authStore.deptcd, deptnm: authStore.deptnm,
  clsymd: '', sclsym: ''
})

const gridElement = ref<HTMLElement | null>(null)
let grid: Tabulator | null = null
const activeitemcount = ref(0)
const sumtotal = ref(0)
const empOptions = ref<any[]>([])

const initGrid = () => {
  if (!gridElement.value) return
  grid = new Tabulator(gridElement.value, {
    layout: "fitColumns", height: "100%", placeholder: "조회된 내역이 없습니다.",
    selectable: true,
    columnDefaults: { headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "선택", field: "selected", width: 50, hozAlign: "center", formatter: "tickCross", editor: true },
      { title: "입금번호", field: "im_full", width: 140, hozAlign: "center" },
      { title: "입금일자", field: "imymd", width: 110, hozAlign: "center", formatter: (c) => formatDate(c.getValue()) },
      { title: "입금부서", field: "deptnm", width: 140 },
      { title: "입금금액", field: "imamt", width: 120, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "거래처명", field: "custnm", minWidth: 200, widthGrow: 1, cssClass: "fw-bold" },
      {
        title: "전송여부", field: "cofmyn", width: 80, hozAlign: "center",
        formatter: (c) => c.getValue() === 'Y' || c.getValue() === 'Y' ? '<span class="badge bg-success">완료</span>' : '<span class="badge bg-secondary">미전송</span>'
      },
      { title: "전표번호", field: "slip_full", width: 160, hozAlign: "center", cssClass: "fw-bold text-primary" }
    ]
  });
  grid.on("cellEdited", (cell) => { if (cell.getField() === 'selected') updateTotals(); });
}

const updateTotals = () => {
  const data = grid?.getData().filter((i: any) => i.selected) || []
  activeitemcount.value = data.length
  sumtotal.value = data.reduce((acc, cur) => acc + (Number(cur.imamt) || 0), 0)
}

const toggleAllSelection = () => {
  const rows = grid?.getRows(); if (!rows) return
  const allSelected = grid?.getSelectedRows().length === rows.length
  if (allSelected) grid?.deselectRow(); else grid?.selectRow();
  const data = grid?.getData() || []
  grid?.updateData(data.map(i => ({ ...i, selected: !allSelected })))
  updateTotals()
}

async function search() {
  if (!searchdata.deptcd) return vAlertError('입금부서를 선택하세요.')
  try {
    const res = await api.post('/hsio/HSIO_325U_STR', {
      actkind: 'S', cmpycd: authStore.cmpycd,
      deptcd: searchdata.deptcd, fromdt: searchdata.fromdt.replace(/-/g, ''),
      todt: searchdata.todt.replace(/-/g, ''), salsemp: searchdata.salsemp
    })
    const data = (res.data || []).map((i: any) => ({
        ...i, selected: false,
        im_full: `${i.imym}-${i.imno}`,
        slip_full: i.slipno && i.slipno > '000' ? `${formatDate(i.slipymd)}-${i.slipno}` : ''
    }))
    grid?.setData(data)
    updateTotals()
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

/**
 * 🚀 전표 발행 로직 (ASP 패턴 이식: a(채번) -> u(저장) 순차 호출)
 */
async function issueSlip() {
  const selected = grid?.getData().filter((i: any) => i.selected && i.cofmyn !== 'Y' && i.cofmyn !== 'Y') || []
  if (selected.length === 0) return vAlertError('발행할 대상을 선택하세요. (이미 완료된 항목 제외)')

  const slipymd = slipmaster.slipymd.replace(/-/g, '')
  if (slipymd <= (slipmaster.clsymd || '')) return vAlertError('회계 마감된 일자입니다.')

  if (!confirm('선택한 내역에 대해 전표를 발행하시겠습니까?')) return

  try {
    const payload = {
        mst: {
            slipymd: slipmaster.slipymd,
            cmpycd: authStore.cmpycd,
            deptcd: slipmaster.deptcd,
            usernm: authStore.usernm,
            fromdt: searchdata.fromdt,
            todt: searchdata.todt
        },
        items: selected.map(item => ({
            imym: item.imym,
            imno: item.imno,
            deptcd: item.deptcd,
            imamt: item.imamt,
            custcd: item.custcd,
            custnm: item.custnm
        }))
    }

    const res = await api.post('/hsio/HSIO_325U_SAVE', payload)

    if (res.data?.data?.res === 'OK') {
        vAlert('정상적으로 전표 발행이 완료되었습니다.')
        search()
    } else {
        vAlertError('전표 발행 중 오류 발생')
    }
  } catch (e: any) { vAlertError(e.response?.data?.message || e.message || '전표 발행 실패') }
}

async function deleteSlip() {
  const selected = grid?.getData().filter((i: any) => i.selected && i.slipno && i.slipno > '000') || []
  if (selected.length === 0) return vAlertError('삭제할 전표를 선택하세요.')

  if (!confirm('선택한 전표를 삭제하시겠습니까?')) return

  try {
    const payload = {
        cmpycd: authStore.cmpycd,
        items: selected.map(item => ({
            slipymd: item.slipymd,
            slipno: item.slipno,
            deptcd: item.deptcd,
            imym: item.imym,
            imno: item.imno
        }))
    }
    const res = await api.post('/hsio/HSIO_325U_CANCEL', payload)
    if (res.data?.data?.res === 'OK') {
        vAlert('전표 삭제 완료'); search()
    } else {
        vAlertError('전표 삭제 실패')
    }
  } catch (e: any) { vAlertError(e.response?.data?.message || '전표 삭제 실패') }
}

function initialize() {
  resetForm(searchdata); searchdata.fromdt = initfromdt; searchdata.todt = initymd;
  resetForm(slipmaster); slipmaster.slipymd = initymd;
  grid?.clearData(); activeitemcount.value = 0; sumtotal.value = 0;
}

function openHelp(type: string) {
  if (type === 'DEPT' || type === 'SLIP_DEPT') {
    openCommonHelp('DEPT', (d) => {
      if (type === 'DEPT') { searchdata.deptcd = d.deptcd; searchdata.deptnm = d.deptnm }
      else { slipmaster.deptcd = d.deptcd; slipmaster.deptnm = d.deptnm }
    })
  }
}

function formatNumber(val: any) { return new Intl.NumberFormat().format(Number(val) || 0) }
function formatDate(val: any) { return val && val.length === 8 ? `${val.substring(0,4)}-${val.substring(4,6)}-${val.substring(6,8)}` : val; }

onUnmounted(() => {
  if (grid) grid.destroy();
});

onMounted(async () => {
  api.get('/hp00/HP00_000S_STR', { params: { gubun: 'cl', cmpycd: authStore.cmpycd } }).then(r => {
    if (r.data?.length) { slipmaster.clsymd = r.data[0].clsymd; slipmaster.sclsym = r.data[0].sclsym; }
  })
  api.get('/ha00/HA00_00P_STR', { params: { gubun: 'sd', cmpycd: authStore.cmpycd } }).then(r => {
    if (r.data) empOptions.value = r.data.map((i: any) => ({ codecd: i.userid, codenm: i.usernm }))
  })
  nextTick(initGrid)
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
