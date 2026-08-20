<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-database-fill-gear me-2 text-primary" style="font-size: 18px;"></i>
        기초자료 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        <span class="text-primary fw-bolder">창고재고등록 (HSBA810U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-save" @click="save">저장</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-auto p-2 d-flex flex-column gap-2">
      <!-- 🅰️ 조회 조건 영역 -->
      <div class="card border shadow-sm overflow-hidden">
        <div class="card-header bg-light py-1 px-3 border-bottom d-flex align-items-center">
          <span class="fw-bold small text-dark"><i class="bi bi-search me-1"></i> 조회 조건</span>
        </div>
        <div class="card-body p-0">
          <table class="erp-table-full">
            <tbody>
              <tr>
                <th style="width: 100px;">연 월</th>
                <td style="width: 250px;">
                  <div class="d-flex align-items-center gap-2">
                    <select v-model="searchData.yy" class="form-select form-select-sm" style="width: 100px;">
                      <option v-for="y in yearOptions" :key="y" :value="y">{{ y }}년</option>
                    </select>
                    <select v-model="searchData.mm" class="form-select form-select-sm" style="width: 80px;">
                      <option v-for="m in monthOptions" :key="m" :value="m">{{ m }}월</option>
                    </select>
                  </div>
                </td>
                <th style="width: 100px;">창 고</th>
                <td>
                  <select v-model="searchData.whcd" class="form-select form-select-sm" style="width: 150px;">
                    <option v-for="opt in whOptions" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                  </select>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 🅱️ 마스터 정보 입력 영역 (등록 전용) -->
      <div class="card border shadow-sm overflow-hidden">
        <div class="card-header py-1 px-3 border-bottom d-flex align-items-center justify-content-between" style="background-color: #f0f7ff !important;">
          <span class="fw-bold small text-primary"><i class="bi bi-pencil-square me-1"></i> 창고 기초재고 정보 입력</span>
          <span v-if="masterData.actkind === 'U0'" class="badge bg-warning text-dark ms-2">수정 모드</span>
          <span v-else class="badge bg-primary ms-2">신규 등록</span>
        </div>
        <div class="card-body p-0">
          <table class="erp-table-full">
            <tbody>
              <tr>
                <th class="required" style="width: 100px;">연 월</th>
                <td style="width: 200px;">
                  <div class="d-flex align-items-center gap-1">
                    <select v-model="masterData.yy" class="form-select form-select-sm" style="width: 90px;" :disabled="masterData.actkind === 'U0'">
                      <option v-for="y in yearOptions" :key="y" :value="y">{{ y }}년</option>
                    </select>
                    <select v-model="masterData.mm" class="form-select form-select-sm" style="width: 70px;" :disabled="masterData.actkind === 'U0'">
                      <option v-for="m in monthOptions" :key="m" :value="m">{{ m }}월</option>
                    </select>
                  </div>
                </td>
                <th class="required" style="width: 100px;">창 고</th>
                <td style="width: 180px;">
                  <select v-model="masterData.whcd" class="form-select form-select-sm" :disabled="masterData.actkind === 'U0'">
                    <option v-for="opt in whOptions" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                  </select>
                </td>
                <th class="required" style="width: 100px;">품 목</th>
                <td>
                  <div class="input-group input-group-sm" style="width: 400px;">
                    <input v-model="masterData.itemcd" type="text" class="form-control text-center bg-light" style="max-width: 80px;" readonly />
                    <input v-model="masterData.itemnm" type="text" class="form-control" placeholder="품목 선택" @keyup.enter="openHelp('ITEM')" />
                    <input v-model="masterData.itsize" type="text" class="form-control bg-light text-center" style="max-width: 120px;" readonly />
                    <input v-model="masterData.unit" type="text" class="form-control bg-light text-center" style="max-width: 60px;" readonly />
                    <button class="btn btn-outline-secondary" @click="openHelp('ITEM')" :disabled="masterData.actkind === 'U0'"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="required">기초재고수량</th>
                <td>
                  <input v-model="masterData.qty" type="text" class="form-control form-control-sm text-end" @input="formatInput('qty')" />
                </td>
                <th class="required">기초재고금액</th>
                <td>
                  <input v-model="masterData.amt" type="text" class="form-control form-control-sm text-end" style="width: 150px;" @input="formatInput('amt')" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Ⓒ 데이터 그리드 영역 (여백 없이 꽉 채움) -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
          <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-1"></i> 창고별 기초재고 목록</span>
          <div class="small text-muted">행 클릭 시 상세 정보를 수정할 수 있습니다.</div>
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
import { reactive, ref, onMounted, nextTick, watch } from 'vue'
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

const now = new Date()
const currentYear = now.getFullYear()
const currentMonth = String(now.getMonth() + 1).padStart(2, '0')

// 1. 상태 관리
const searchData = reactive({
  yy: String(currentYear),
  mm: currentMonth,
  whcd: ''
})

const masterData = reactive<any>({
  actkind: 'A0',
  cmpycd: authStore.cmpycd,
  yy: String(currentYear),
  mm: currentMonth,
  whcd: '',
  itemcd: '',
  itemnm: '',
  itsize: '',
  unit: '',
  qty: '0',
  amt: '0'
})

const yearOptions = Array.from({ length: 6 }, (_, i) => String(currentYear - i))
const monthOptions = Array.from({ length: 12 }, (_, i) => String(i + 1).padStart(2, '0'))
const whOptions = ref<any[]>([])

const gridElement = ref<HTMLElement | null>(null)
const grid = ref<Tabulator | null>(null)
const activeItemCount = ref(0)

// 2. 그리드 초기화
const initGrid = () => {
  if (!gridElement.value) return
  grid.value = new Tabulator(gridElement.value, {
    layout: "fitColumns",
    height: "100%",
    placeholder: "조회된 데이터가 없습니다.",
    columnDefaults: { headerSort: false },
    columns: [
      { title: "품목명", field: "itemnm", minWidth: 250, cssClass: "fw-bold" },
      { title: "규격", field: "itsize", width: 150 },
      { title: "단위", field: "unit", width: 80, hozAlign: "center" },
      { title: "기초재고수량", field: "qty", width: 120, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      {
        title: "단가", field: "price", width: 120, hozAlign: "right",
        formatter: (cell) => {
            const data = cell.getData();
            const qty = Number(data.qty) || 0;
            const amt = Number(data.amt) || 0;
            const price = qty !== 0 ? Math.floor(amt / qty) : 0;
            return new Intl.NumberFormat().format(price);
        }
      },
      { title: "재고금액", field: "amt", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } }
    ]
  })

  grid.value.on("rowClick", (e, row) => {
    const data = row.getData()
    Object.assign(masterData, data)
    masterData.yy = searchData.yy
    masterData.mm = searchData.mm
    masterData.whcd = searchData.whcd
    masterData.qty = formatNumber(data.qty)
    masterData.amt = formatNumber(data.amt)
    masterData.actkind = 'U0'
  })
}

// 3. 기능 구현
async function fetchWhOptions() {
  try {
    const res = await api.get('/hs00/HS00_000S_STR', { params: { gubun: 'W0', cmpycd: authStore.cmpycd } })
    whOptions.value = res.data.map((i: any) => ({ codecd: i.whcd, codenm: i.whnm }))
    if (whOptions.value.length > 0) {
        searchData.whcd = whOptions.value[0].codecd
        masterData.whcd = whOptions.value[0].codecd
    }
  } catch (e) { console.error('창고 목록 로드 실패') }
}

async function search() {
  try {
    const res = await api.post('/hsba/HSBA_810U_STR', {
      actkind: 'S0',
      cmpycd: authStore.cmpycd,
      yy: searchData.yy,
      mm: searchData.mm,
      whcd: searchData.whcd,
      itemcd: '',
      qty: 0
    })
    if (grid.value) {
      grid.value.setData(res.data)
      activeItemCount.value = res.data.length
    }
  } catch (e) { vAlertError('조회 실패') }
}

async function save() {
  if (!masterData.itemcd) return vAlertError('품목을 선택해 주십시요.')
  if (!masterData.whcd) return vAlertError('창고를 선택해 주십시요.')
  if (Number(masterData.qty.replace(/,/g, '')) === 0) return vAlertError('수량을 입력해 주십시요.')

  const confirmMsg = masterData.actkind === 'A0' ? '창고 기초재고를 등록하시겠습니까?' : '창고 기초재고 정보를 수정하시겠습니까?'
  if (!confirm(confirmMsg)) return

  try {
    const payload = {
      ...masterData,
      qty: masterData.qty.replace(/,/g, ''),
      amt: masterData.amt.replace(/,/g, ''), // Procedure in IF Turn 2 was missing amt, but Turn 1 submission had it. I'll include it.
      userid: authStore.userid
    }
    await api.post('/hsba/HSBA_810U_STR', payload)
    vAlert('성공적으로 저장되었습니다.')
    search()
    initialize()
  } catch (e) { vAlertError('저장 중 오류 발생') }
}

function initialize() {
  const currentY = searchData.yy
  const currentM = searchData.mm
  const currentWh = searchData.whcd
  resetForm(masterData)
  Object.assign(masterData, {
    actkind: 'A0',
    cmpycd: authStore.cmpycd,
    yy: currentY,
    mm: currentM,
    whcd: currentWh,
    qty: '0',
    amt: '0'
  })
}

const formatInput = (field: string) => {
  let val = masterData[field].replace(/[^0-9]/g, '')
  masterData[field] = val.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

// 4. 팝업 설정
const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function openHelp(type: string) {
  if (type === 'ITEM') {
    Object.assign(modalProps, {
      title: '품목 선택',
      path: '/hs00/HS00_000S_STR',
      defaultField: 'itemnm',
      large: true,
      data: { gubun: 'I1', cmpycd: authStore.cmpycd, gbncd: '2', LIMITOFFSET: 0, LIMITROWS: 9999 },
      columns: [
        { title: '코드', field: 'itemcd', width: 100 },
        { title: '품목명', field: 'itemnm', minWidth: 200 },
        { title: '규격', field: 'itsize', width: 150 },
        { title: '단위', field: 'unitnm', width: 80 }
      ],
      onConfirm: (data: any) => {
        masterData.itemcd = data.itemcd
        masterData.itemnm = data.itemnm
        masterData.itsize = data.itsize
        masterData.unit = data.unitnm
      }
    })
    modalVisible.value = true
  }
}

const formatNumber = (val: any) => new Intl.NumberFormat().format(Number(val) || 0)

onMounted(async () => {
  await fetchWhOptions()
  nextTick(() => {
    initGrid()
    search()
  })
})

// Sync masterData with search conditions when in new entry mode
watch(() => searchData.yy, (val) => { if (masterData.actkind === 'A0') masterData.yy = val })
watch(() => searchData.mm, (val) => { if (masterData.actkind === 'A0') masterData.mm = val })
watch(() => searchData.whcd, (val) => { if (masterData.actkind === 'A0') masterData.whcd = val })

</script>

