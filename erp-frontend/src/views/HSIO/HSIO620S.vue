<!--
	=============================================================
	프로그램명	: 거래명세표 (HSIO620S)
	작성일자	: 2025.02.24
	설명        : 영업 출고 내역 조회 및 명세표 인쇄 (소문자 원칙 및 인터셉터 표준화 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-file-earmark-text-fill me-2 text-primary" style="font-size: 18px;"></i>
        영업정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        출고관리 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        <span class="text-primary fw-bolder">거래명세표 (HSIO620S)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="searchMaster">조회</button>
        <button class="btn-erp btn-outline-secondary" @click="printSlip('Print')">거래명세표 인쇄</button>
        <button class="btn-erp btn-outline-secondary" @click="printSlip('Print_Req')">출고의뢰서 인쇄</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2">
      <!-- 🅰️ 조회 조건 영역 -->
      <div class="card border shadow-sm overflow-hidden">
        <div class="card-body p-0">
          <table class="erp-table-full">
            <tbody>
              <tr>
                <th style="width: 100px;">출고창고</th>
                <td style="width: 200px;">
                  <select v-model="searchData.whcd" class="form-select form-select-sm">
                    <option value="000">전체</option>
                    <option v-for="opt in whOptions" :key="opt.whcd" :value="opt.whcd">{{ opt.whnm }}</option>
                  </select>
                </td>
                <th style="width: 100px;">출고일자</th>
                <td style="width: 300px;">
                  <div class="d-flex align-items-center gap-1">
                    <input v-model="searchData.fromdt" type="date" class="form-control form-control-sm" />
                    <span>~</span>
                    <input v-model="searchData.todt" type="date" class="form-control form-control-sm" />
                  </div>
                </td>
                <th style="width: 100px;">확정여부</th>
                <td style="width: 150px;">
                  <select v-model="searchData.slipyn" class="form-select form-select-sm">
                    <option value="Y">확정</option>
                  </select>
                </td>
                <th style="width: 100px;">거&nbsp;&nbsp;래&nbsp;&nbsp;처</th>
                <td>
                  <div class="input-group input-group-sm">
                    <input v-model="searchData.custcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
                    <input v-model="searchData.custnm" type="text" class="form-control" placeholder="거래처 선택" @keyup.enter="openHelp('CUST')" />
                    <button class="btn btn-outline-secondary" @click="openHelp('CUST')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 🅱️ 메인 작업 영역 -->
      <div class="d-flex flex-grow-1 gap-2 overflow-hidden">
        <div class="card border shadow-sm d-flex flex-column" style="width: 350px;">
          <div class="card-header bg-light py-1 px-3 border-bottom fw-bold small text-dark">
            <i class="bi bi-list-ul me-1"></i> 출고 목록
          </div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
              <div ref="masterGridElement" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>

        <div class="flex-grow-1 d-flex flex-column gap-2 overflow-hidden">
          <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column">
            <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
              <span class="fw-bold small text-dark">
                <i class="bi bi-grid-3x3-gap-fill me-1"></i> 상세 품목 내역
                <span v-if="selectedMasterInfo" class="ms-2 text-primary">[{{ selectedMasterInfo.custnm }}] {{ selectedMasterInfo.ioym }}-{{ selectedMasterInfo.iono }}</span>
              </span>
            </div>
            <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
              <div ref="detailGridElement" class="tabulator-instance flex-grow-1"></div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <Modal v-model:visible="modalVisible" :modalProps="modalProps" />
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, computed, nextTick } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { getDate } from '@/composables/useDate'
import type { ModalProps } from '@/types/modal'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { firstDay, today } = getDate()

// 1. 상태 관리
const searchData = reactive({
  whcd: '000',
  fromdt: firstDay,
  todt: today,
  slipyn: 'Y',
  custcd: '',
  custnm: ''
})

const whOptions = ref<any[]>([])
const selectedMasterInfo = ref<any>(null)

const masterGridElement = ref<HTMLElement | null>(null)
const detailGridElement = ref<HTMLElement | null>(null)
let masterGrid: Tabulator | null = null
let detailGrid: Tabulator | null = null

const initGrids = () => {
  if (masterGridElement.value) {
    masterGrid = new Tabulator(masterGridElement.value, {
      layout: "fitColumns", height: "100%", placeholder: "데이터 없음",
      columns: [
        { title: "No", formatter: "rownum", width: 40, hozAlign: "center", headerSort: false },
        { title: "거래처", field: "custnm", minWidth: 150, headerSort: false, cssClass: "fw-bold text-primary cursor-pointer" },
        { title: "출고번호", field: "iono_full", width: 120, hozAlign: "center", headerSort: false,
          formatter: (cell) => {
              const d = cell.getData();
              return `<span class="text-decoration-underline">${d.ioym}-${d.iono}</span>`;
          }
        }
      ]
    })
    masterGrid.on("rowClick", (e, row) => {
      const data = row.getData()
      selectedMasterInfo.value = data
      fetchDetails(data)
    })
  }

  if (detailGridElement.value) {
    detailGrid = new Tabulator(detailGridElement.value, {
        layout: 'fitColumns', height: '100%',
        columnDefaults: {
            headerSort: false,
            headerHozAlign: "center",
            hozAlign: 'right', // 🚀 기본값 우측 정렬
            vertAlign: "middle",
            minWidth: 100
        },
      columns: [
        { title: "품목명", field: "itemnm", minWidth: 200, hozAlign: "left", cssClass: "fw-bold" },
        { title: "규격", field: "itsize", width: 200 },
        { title: "단위", field: "unit", width: 60, hozAlign: "center" },
        { title: "수량", field: "ioqty", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 2 } },
        { title: "금액", field: "jsanamt", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
        { title: "부가세", field: "jsanvat", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } }
      ]
    })
  }
}

// 3. 기능 구현
async function fetchWhOptions() {
  try {
    const res = await api.get('/hs00/HS00_000S_STR', { params: { gubun: 'W0', cmpycd: authStore.cmpycd } })
    // 🚀 DB 오리지널 명칭 whcd, whnm을 사용하여 정확하게 매핑 (인터셉터가 알맹이만 주므로 res.data 사용)
    whOptions.value = res.data;
  } catch (e) { console.error('창고 옵션 로드 실패') }
}

async function searchMaster() {
  try {
    const res = await api.post('/hsio/HSIO_620S_STR', {
        actkind: 'S1',
        cmpycd: authStore.cmpycd,
        iogbn: '200',
        whcd: searchData.whcd,
        fromdt: searchData.fromdt.replace(/-/g, ''),
        todt: searchData.todt.replace(/-/g, ''),
        custcd: searchData.custcd,
        slipyn: searchData.slipyn
    })
    masterGrid?.setData(res.data)
    detailGrid?.clearData()
    selectedMasterInfo.value = null
  } catch (e) { vAlertError('조회 실패') }
}

async function fetchDetails(row: any) {
  try {
    const res = await api.post('/hsio/HSIO_620S_STR', {
        actkind: 'S0',
        cmpycd: authStore.cmpycd,
        iogbn: '200',
        whcd: searchData.whcd,
        fromdt: searchData.fromdt,
        todt: searchData.todt,
        custcd: row.custcd,
        ioym: row.ioym,
        iono: row.iono,
        slipyn: searchData.slipyn
    })
    detailGrid?.setData(res.data)
  } catch (e) { vAlertError('상세 로드 실패') }
}

function printSlip(type: string) {
  if (!selectedMasterInfo.value) return vAlertError('출고 내역을 먼저 선택하세요.')
  const m = selectedMasterInfo.value
  let url = type === 'Print' ? `/report/HSIO_TRANS_PRINT?PRTGU=Print&ioym=${m.ioym}&iono=${m.iono}` : `/report/HSIO_REQOUT_PRINT?PRTGU=Print&ioym=${m.ioym}&iono=${m.iono}`
  window.open(url, 'print', 'width=700,height=600,scrollbars=yes')
}

function initialize() {
  resetForm(searchData)
  searchData.whcd = '000'; searchData.slipyn = 'Y';
  searchData.fromdt = firstDay; searchData.todt = today;
  masterGrid?.clearData(); detailGrid?.clearData(); selectedMasterInfo.value = null
}

const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function openHelp(type: string) {
  if (type === 'CUST') {
    Object.assign(modalProps, {
      title: '거래처 선택', path: '/ha00/HA00_00P_STR', defaultField: 'custnm', large: true,
      data: { gubun: 'C4', cmpycd: authStore.cmpycd },
      columns: [{ title: '코드', field: 'custcd', width: 80 }, { title: '거래처명', field: 'custnm', width: 200 }, { title: '주소', field: 'address', minWidth: 300 }],
      onConfirm: (data: any) => { searchData.custcd = data.custcd; searchData.custnm = data.custnm }
    })
    modalVisible.value = true
  }
}

const formatDateString = (v: any, sep: string) => v && v.length === 8 ? `${v.substring(0, 4)}${sep}${v.substring(4, 6)}${sep}${v.substring(6, 8)}` : v

onMounted(async () => {
  await fetchWhOptions()
  nextTick(() => { initGrids(); searchMaster() })
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
