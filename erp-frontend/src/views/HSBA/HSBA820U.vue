<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-cash-stack me-2 text-primary" style="font-size: 18px;"></i>
        기초자료 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        <span class="text-primary fw-bolder">미수금 등록 (HSBA820U)</span>
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
                <th style="width: 100px;">판매부서</th>
                <td>
                  <div class="input-group input-group-sm" style="width: 250px;">
                    <input v-model="searchData.deptcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
                    <input v-model="searchData.deptnm" type="text" class="form-control" placeholder="부서 선택" @keyup.enter="openHelp('SEARCH_DEPT')" />
                    <button class="btn btn-outline-secondary" @click="openHelp('SEARCH_DEPT')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 🅱️ 미수금 정보 입력 영역 -->
      <div class="card border shadow-sm overflow-hidden">
        <div class="card-header py-1 px-3 border-bottom d-flex align-items-center justify-content-between" style="background-color: #f0f7ff !important;">
          <span class="fw-bold small text-primary"><i class="bi bi-pencil-square me-1"></i> 미수금 정보 입력</span>
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
                <th class="required" style="width: 100px;">판매부서</th>
                <td style="width: 250px;">
                  <div class="input-group input-group-sm">
                    <input v-model="masterData.deptcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
                    <input v-model="masterData.deptnm" type="text" class="form-control bg-light" readonly />
                    <button class="btn btn-outline-secondary" @click="openHelp('MASTER_DEPT')" :disabled="masterData.actkind === 'U0'"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="required" style="width: 100px;">거 래 처</th>
                <td style="width: 280px;">
                  <div class="input-group input-group-sm">
                    <input v-model="masterData.custcd" type="text" class="form-control text-center bg-light" style="max-width: 80px;" readonly />
                    <input v-model="masterData.custnm" type="text" class="form-control" placeholder="거래처 선택" @keyup.enter="openHelp('CUST')" />
                    <button class="btn btn-outline-secondary" @click="openHelp('CUST')" :disabled="masterData.actkind === 'U0'"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="required" style="width: 100px;">미 수 금</th>
                <td>
                  <input v-model="masterData.amt" type="text" class="form-control form-control-sm text-end fw-bold" @input="formatInput('amt')" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Ⓒ 데이터 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
          <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-1"></i> 거래처별 미수금 내역</span>
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
  deptcd: authStore.deptcd,
  deptnm: authStore.deptnm
})

const masterData = reactive<any>({
  actkind: 'A0',
  cmpycd: authStore.cmpycd,
  yy: String(currentYear),
  mm: currentMonth,
  deptcd: authStore.deptcd,
  deptnm: authStore.deptnm,
  custcd: '',
  custnm: '',
  amt: '0'
})

const yearOptions = Array.from({ length: 6 }, (_, i) => String(currentYear - i))
const monthOptions = Array.from({ length: 12 }, (_, i) => String(i + 1).padStart(2, '0'))

const gridElement = ref<HTMLElement | null>(null)
const grid = ref<Tabulator | null>(null)
const activeItemCount = ref(0)
const totalMisuSum = ref(0)

// 2. 그리드 초기화
const initGrid = () => {
  if (!gridElement.value) return
  grid.value = new Tabulator(gridElement.value, {
    layout: "fitColumns",
    height: "100%",
    placeholder: "조회된 데이터가 없습니다.",
    columnDefaults: { headerSort: false },
    columns: [
      { title: "거래처", field: "custnm", minWidth: 200, cssClass: "fw-bold" },
      { title: "전월잔액", field: "bnonamt", width: 120, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "매출액", field: "spyamt", width: 120, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "부가세", field: "vatamt", width: 100, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      {
        title: "합계", field: "total", width: 120, hozAlign: "right",
        formatter: (cell) => {
            const data = cell.getData();
            return new Intl.NumberFormat().format(Number(data.spyamt || 0) + Number(data.vatamt || 0));
        }
      },
      { title: "지급액", field: "imamt", width: 120, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "미수금", field: "amt", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "text-primary fw-bold" }
    ]
  })

  grid.value.on("rowClick", (e, row) => {
    const data = row.getData()
    Object.assign(masterData, data)
    masterData.yy = searchData.yy
    masterData.mm = searchData.mm
    masterData.deptcd = searchData.deptcd
    masterData.deptnm = searchData.deptnm
    masterData.amt = formatNumber(data.amt)
    masterData.actkind = 'U0'
  })
}

// 3. 기능 구현
async function search() {
  if (!searchData.deptcd) return vAlertError('판매부서를 선택해 주십시요.')
  try {
    const res = await api.post('/hsba/HSBA_820U_STR', {
      actkind: 'S0',
      cmpycd: authStore.cmpycd,
      yy: searchData.yy,
      mm: searchData.mm,
      deptcd: searchData.deptcd,
      custcd: '',
      amt: 0
    })
    if (grid.value) {
      const mappedData = res.data.map((i: any) => {
          const bnon = Number(i.bnonamt) || 0;
          const spy = (Number(i.spyamt) || 0) + (Number(i.dscamt) || 0) + (Number(i.rtnamt) || 0);
          const vat = Number(i.vatamt) || 0;
          const im = (Number(i.cashamt) || 0) + (Number(i.bankamt) || 0) + (Number(i.billamt) || 0) + (Number(i.etcamt) || 0);
          const amt = bnon + spy + vat - im;
          return { ...i, bnonamt: bnon, spyamt: spy, vatamt: vat, imamt: im, amt: amt }
      })
      grid.value.setData(mappedData)
      activeItemCount.value = mappedData.length
      totalMisuSum.value = mappedData.reduce((acc: number, cur: any) => acc + cur.amt, 0)
    }
  } catch (e) { vAlertError('조회 실패') }
}

async function save() {
  if (!masterData.deptcd) return vAlertError('판매부서를 선택해 주십시요.')
  if (!masterData.custcd) return vAlertError('거래처를 선택해 주십시요.')
  if (masterData.amt === '' || masterData.amt === '0') return vAlertError('미수금을 입력해 주십시요.')

  const confirmMsg = masterData.actkind === 'A0' ? '미수금을 등록하시겠습니까?' : '미수금 정보를 수정하시겠습니까?'
  if (!confirm(confirmMsg)) return

  try {
    const payload = {
      ...masterData,
      amt: masterData.amt.replace(/,/g, ''),
      userid: authStore.userid
    }
    await api.post('/hsba/HSBA_820U_STR', payload)
    vAlert('정상적으로 처리되었습니다.')
    search()
    initialize(true)
  } catch (e) { vAlertError('저장 중 오류 발생') }
}

function initialize(keepYmDept = false) {
  const currentY = masterData.yy
  const currentM = masterData.mm
  const currentDept = masterData.deptcd
  const currentDeptNm = masterData.deptnm

  resetForm(masterData)

  if (keepYmDept) {
    Object.assign(masterData, {
      actkind: 'A0',
      cmpycd: authStore.cmpycd,
      yy: currentY,
      mm: currentM,
      deptcd: currentDept,
      deptnm: currentDeptNm,
      amt: '0'
    })
  } else {
    Object.assign(masterData, {
      actkind: 'A0',
      cmpycd: authStore.cmpycd,
      yy: searchData.yy,
      mm: searchData.mm,
      deptcd: searchData.deptcd,
      deptnm: searchData.deptnm,
      amt: '0'
    })
  }
}

const formatInput = (field: string) => {
  let val = masterData[field].replace(/[^0-9]/g, '')
  masterData[field] = val.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

// 4. 팝업 설정
const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function openHelp(type: string) {
  let gubun = 'D0', title = '부서 선택', field = 'deptnm'
  if (type === 'CUST') { gubun = 'C4'; title = '거래처 선택'; field = 'custnm' }

  Object.assign(modalProps, {
    title, path: '/ha00/HA00_00P_STR', defaultField: field,
    data: { gubun: gubun, cmpycd: authStore.cmpycd },
    columns: gubun === 'D0'
      ? [{ title: '코드', field: 'deptcd', width: 80 }, { title: '부서명', field: 'deptnm', width: 180 }]
      : [{ title: '코드', field: 'custcd', width: 100 }, { title: '거래처명', field: 'custnm', minWidth: 200 }],
    onConfirm: (data: any) => {
      if (type === 'SEARCH_DEPT') { searchData.deptcd = data.deptcd; searchData.deptnm = data.deptnm }
      else if (type === 'MASTER_DEPT') { masterData.deptcd = data.deptcd; masterData.deptnm = data.deptnm }
      else if (type === 'CUST') { masterData.custcd = data.custcd; masterData.custnm = data.custnm }
    }
  })
  modalVisible.value = true
}

const formatNumber = (val: any) => new Intl.NumberFormat().format(Number(val) || 0)

onMounted(() => {
  nextTick(() => {
    initGrid()
    search()
  })
})

// Sync masterData with search conditions when in new entry mode
watch(() => searchData.yy, (val) => { if (masterData.actkind === 'A0') masterData.yy = val })
watch(() => searchData.mm, (val) => { if (masterData.actkind === 'A0') masterData.mm = val })
watch(() => searchData.deptcd, (val) => { if (masterData.actkind === 'A0') masterData.deptcd = val })
watch(() => searchData.deptnm, (val) => { if (masterData.actkind === 'A0') masterData.deptnm = val })

</script>

