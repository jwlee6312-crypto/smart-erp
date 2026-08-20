<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-link-45deg me-2 text-primary" style="font-size: 18px;"></i>
        기본정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        <span class="text-primary fw-bolder">외부회계연동 계정정보 등록 (HSBA701U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-save" @click="save">저장</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-auto p-2 d-flex flex-column gap-2">
      <!-- 🅰️ 데이터 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
          <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-1"></i> 계정 연동 설정 목록</span>
          <button class="btn btn-xs btn-primary fw-bold" @click="addRow">
            <i class="bi bi-plus-lg me-1"></i> 행추가
          </button>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="gridElement" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>
    </div>
    <Modal v-model:visible="modalVisible" :modalProps="modalProps" />
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, nextTick } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import type { ModalProps } from '@/types/modal'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

// 1. 상태 관리
const gridElement = ref<HTMLElement | null>(null)
const grid = ref<Tabulator | null>(null)
const activeItemCount = ref(0)

const slipKindOptions = ref<any[]>([])
const assetKindOptions = ref<any[]>([])

// 2. 그리드 초기화
const initGrid = () => {
  if (!gridElement.value) return
  grid.value = new Tabulator(gridElement.value, {
    layout: "fitColumns",
    height: "100%",
    placeholder: "조회된 내역이 없습니다.",
    columnDefaults: { headerSort: false },
    columns: [
      {
        title: "선택", field: "procyn", width: 50, hozAlign: "center",
        formatter: "tickCross", editor: true
      },
      {
        title: "분개유형", field: "slipkind", width: 200, editor: "list",
        editorParams: { values: slipKindOptions.value.reduce((acc, opt) => ({ ...acc, [opt.codecd]: opt.codenm }), {}) },
        formatter: (cell) => {
          const val = cell.getValue();
          const opt = slipKindOptions.value.find(o => o.codecd === val);
          return opt ? opt.codenm : val;
        }
      },
      {
        title: "재고자산", field: "astkind", width: 200, editor: "list",
        editorParams: { values: assetKindOptions.value.reduce((acc, opt) => ({ ...acc, [opt.codecd]: opt.codenm }), {}) },
        formatter: (cell) => {
          const val = cell.getValue();
          const opt = assetKindOptions.value.find(o => o.codecd === val);
          return opt ? opt.codenm : val;
        }
      },
      {
        title: "차변계정코드", field: "acctcd1", width: 100, editor: "input",
        cellClick: (e, cell) => { if ((e.target as HTMLElement).classList.contains('bi-search')) openHelp('ACCT1', cell.getRow()) },
        formatter: (cell) => (cell.getValue() || '') + " <i class='bi bi-search text-primary ms-1 cursor-pointer'></i>"
      },
      { title: "차변계정명", field: "acctnm1", width: 150, editor: "input" },
      {
        title: "대변계정코드", field: "acctcd2", width: 100, editor: "input",
        cellClick: (e, cell) => { if ((e.target as HTMLElement).classList.contains('bi-search')) openHelp('ACCT2', cell.getRow()) },
        formatter: (cell) => (cell.getValue() || '') + " <i class='bi bi-search text-primary ms-1 cursor-pointer'></i>"
      },
      { title: "대변계정명", field: "acctnm2", width: 150, editor: "input" },
      {
        title: "부가세코드", field: "vacctcd", width: 100, editor: "input",
        cellClick: (e, cell) => { if ((e.target as HTMLElement).classList.contains('bi-search')) openHelp('VACCT', cell.getRow()) },
        formatter: (cell) => (cell.getValue() || '') + " <i class='bi bi-search text-primary ms-1 cursor-pointer'></i>"
      },
      { title: "부가세명", field: "vacctnm", width: 150, editor: "input" },
      {
        title: "", width: 40, hozAlign: "center",
        formatter: () => "<i class='bi bi-trash text-danger cursor-pointer'></i>",
        cellClick: (e, c) => {
          c.getRow().delete();
          updateTotals();
        }
      }
    ]
  })
}

const updateTotals = () => {
  if (!grid.value) return
  activeItemCount.value = grid.value.getData().length
}

// 3. 기능 구현
async function fetchOptions() {
  try {
    // 분개유형 (610)
    const resSlip = await api.get('/hs00/HS00_000S_STR', { params: { gubun: 'GB', cmpycd: authStore.cmpycd, gbncd: '610' } })
    slipKindOptions.value = resSlip.data.map((i: any) => ({ codecd: Object.values(i)[0], codenm: Object.values(i)[1] }))

    // 재고자산 (100)

    const resAsset = await api.get('/hs00/HS00_000S_STR', { params: { gubun: 'E0', cmpycd: authStore.cmpycd, gbncd: '100' } })
    assetKindOptions.value = resAsset.data.map((i: any) => ({ codecd: i.code, codenm: i.cdnm }))
  } catch (e) { console.error('옵션 로드 실패') }
}

async function search() {
  try {
    const res = await api.post('/hsba/HSBA_701U_STR', {
      actkind: 'S0',
      cmpycd: authStore.cmpycd
    })
    if (grid.value) {
      grid.value.setData(res.data.map((i: any) => ({ ...i, procyn: false })))
      activeItemCount.value = res.data.length
    }
  } catch (e) { vAlertError('조회 실패') }
}

async function save() {
  const selectedRows = grid.value?.getData().filter((i: any) => i.procyn)
  if (!selectedRows || selectedRows.length === 0) {
    return vAlertError('저장할 대상을 선택해 주십시오.')
  }

  if (!confirm('선택한 항목들을 저장하시겠습니까?')) return

  try {
    for (const row of selectedRows) {
      const actKind = row.SEQ ? 'U0' : 'A0'
      await api.post('/hsba/HSBA_701U_STR', {
        ...row,
        actkind: actKind,
        cmpycd: authStore.cmpycd,
        userid: authStore.userid
      })
    }
    vAlert('정상적으로 저장되었습니다.')
    search()
  } catch (e) { vAlertError('저장 중 오류 발생') }
}

function initialize() {
  if (grid.value) grid.value.clearData()
  activeItemCount.value = 0
  search()
}

function addRow() {
  if (grid.value) {
    grid.value.addRow({ procyn: true, slipkind: '', astkind: '', acctcd1: '', acctnm1: '', acctcd2: '', acctnm2: '', vacctcd: '', vacctnm: '' })
    updateTotals()
  }
}

// 4. 팝업 설정
const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function openHelp(type: string, row: any) {
  Object.assign(modalProps, {
    title: '계정코드 선택',
    path: '/comm/HELP_acctcd_LTD',
    defaultField: 'acctnm',
    data: { cmpycd: authStore.cmpycd },
    columns: [
      { title: '코드', field: 'acctcd', width: 100 },
      { title: '계정명', field: 'acctnm', minWidth: 200 }
    ],
    onConfirm: (data: any) => {
      if (type === 'ACCT1') row.update({ acctcd1: data.acctcd, acctnm1: data.acctnm })
      else if (type === 'ACCT2') row.update({ acctcd2: data.acctcd, acctnm2: data.acctnm })
      else if (type === 'VACCT') row.update({ vacctcd: data.acctcd, vacctnm: data.acctnm })
    }
  })
  modalVisible.value = true
}

const formatNumber = (val: any) => new Intl.NumberFormat().format(Number(val) || 0)

onMounted(async () => {
  await fetchOptions()
  nextTick(() => {
    initGrid()
    search()
  })
})
</script>

