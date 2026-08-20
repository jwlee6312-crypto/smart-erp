<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-percent me-2 text-success" style="font-size: 18px;"></i>
        구매정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        매입정산 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        <span class="text-primary fw-bolder">매입할인 등록 (HSIO160U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchList">조회</button>
        <button class="btn-erp btn-save" @click="saveData">저장</button>
        <button class="btn-erp btn-delete" @click="deleteData">삭제</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2">
      <!-- 🅰️ 조회 및 배부 설정 영역 -->
      <div class="card border shadow-sm overflow-hidden">
        <div class="card-body p-0">
          <table class="erp-table-full">
            <tbody>
              <tr>
                <th class="required">정산연월</th>
                <td>
                  <input v-model="uijsanym" type="month" class="form-control form-control-sm" style="width: 150px;" />
                </td>
                <th class="required">매&nbsp;&nbsp;입&nbsp;&nbsp;처</th>
                <td>
                  <div class="input-group input-group-sm" style="width: 250px;">
                    <input v-model="searchData.custcd" type="text" class="form-control text-center bg-light" style="max-width: 80px;" readonly />
                    <input v-model="searchData.custnm" type="text" class="form-control" placeholder="거래처 선택" @keyup.enter="openHelp('CUST')" />
                    <button class="btn btn-outline-secondary" @click="openHelp('CUST')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="required">정산일자</th>
                <td>
                  <input v-model="uijsanymd" type="date" class="form-control form-control-sm" style="width: 150px;" />
                </td>
              </tr>
              <tr>
                <th class="bg-light-blue">할인금액</th>
                <td>
                  <input v-model.number="inputData.divsum" type="number" class="form-control form-control-sm text-end fw-bold text-primary" style="width: 150px;" @input="applyAllocation" />
                </td>
                <th class="bg-light-blue">배부기준</th>
                <td colspan="3">
                  <div class="d-flex gap-3 align-items-center mt-1">
                    <div class="form-check form-check-inline">
                      <input class="form-check-input" type="radio" v-model="inputData.divgbn" value="amt" id="gbnAmt" @change="applyAllocation">
                      <label class="form-check-label small fw-bold" for="gbnAmt">금액기준</label>
                    </div>
                    <div class="form-check form-check-inline">
                      <input class="form-check-input" type="radio" v-model="inputData.divgbn" value="qty" id="gbnqty" @change="applyAllocation">
                      <label class="form-check-label small fw-bold" for="gbnqty">수량기준</label>
                    </div>
                    <span class="text-muted small ms-3"><i class="bi bi-info-circle me-1"></i> 할인금액 입력 시 선택된 항목들에 자동 배부됩니다.</span>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 🅲 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column">
        <div class="card-header bg-light py-1 px-3 border-bottom d-flex justify-content-between align-items-center">
          <span class="fw-bold small text-dark"><i class="bi bi-list-check me-1"></i> 매입 내역</span>
          <div class="small text-muted">전표 발행된 자료는 수정/삭제가 불가능합니다.</div>
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
import { reactive, ref, onMounted, computed, nextTick, watch, onUnmounted } from 'vue'
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
const initymd = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}`
const initym = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}`

// 1. 상태 관리
const searchData = reactive({
  jsanym: initym,
  custcd: '',
  custnm: '',
  jsanymd: initymd
})

const inputData = reactive({
  divsum: 0,
  divgbn: 'amt'
})

const uijsanym = computed({
  get: () => searchData.jsanym ? `${searchData.jsanym.substring(0, 4)}-${searchData.jsanym.substring(4, 6)}` : '',
  set: (v) => searchData.jsanym = v.replace(/-/g, '')
})
const uijsanymd = computed({
  get: () => formatDateString(searchData.jsanymd, '-'),
  set: (v) => searchData.jsanymd = v.replace(/-/g, '')
})

const gridElement = ref<HTMLElement | null>(null)
let grid: Tabulator | null = null
const selectedCount = ref(0)
const totals = reactive({ qty: 0, amt: 0, hal: 0 })

// 2. 그리드 초기화
const initGrid = () => {
  if (gridElement.value) {
    grid = new Tabulator(gridElement.value, {
      layout: "fitColumns",
      height: "100%",
      placeholder: "조회된 데이터가 없습니다.",
      selectable: true,
      columns: [
        { title: "선택", formatter: "rowSelection", titleFormatter: "rowSelection", width: 80, hozAlign: "center", headerSort: false },
        { title: "품목명", field: "itemnm", minWidth: 100, widthGrow: 1 },
        { title: "규격", field: "itemsIZE", width: 200 },
        { title: "수량", field: "ioqty", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
        { title: "공급가", field: "ioamt", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
        { title: "할인금액", field: "halamt", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "text-primary fw-bold" },
        { title: "입고부서", field: "deptnm", width: 250 },
        { title: "전표발행", field: "slipymd", width: 150, hozAlign: "center", formatter: (c) => c.getValue() > "00000000" ? "발행" : "" },
      ],
    })

    grid.on("rowSelectionChanged", () => {
      calculateTotals()
      applyAllocation()
    })
  }
}

// 3. 로직 함수
const calculateTotals = () => {
  const selectedRows = grid?.getSelectedData() || []
  selectedCount.value = selectedRows.length
  totals.qty = selectedRows.reduce((acc, row) => acc + Number(row.ioqty || 0), 0)
  totals.amt = selectedRows.reduce((acc, row) => acc + Number(row.ioamt || 0), 0)
}

// ASP의 Div_Total_Calc 구현
const applyAllocation = () => {
  const selectedRows = grid?.getSelectedRows() || []
  if (selectedRows.length === 0 || inputData.divsum <= 0) {
    grid?.getData().forEach(row => grid?.updateData([{ id: row.id, halamt: 0 }]))
    totals.hal = 0
    return
  }

  const divSum = inputData.divsum
  const isAmtBasis = inputData.divgbn === 'amt'
  const totalBasis = isAmtBasis ? totals.amt : totals.qty

  if (totalBasis === 0) return

  let allocatedSum = 0
  const updates: any[] = []

  selectedRows.forEach((row, idx) => {
    const data = row.getData()
    const basisValue = isAmtBasis ? Number(data.ioamt) : Number(data.ioqty)

    // 비례 배분 계산
    let halAmt = Math.round(divSum * (basisValue / totalBasis))

    // 마지막 행에서 단수 차이 보정
    if (idx === selectedRows.length - 1) {
      halAmt = divSum - allocatedSum
    }

    allocatedSum += halAmt
    updates.push({ ...data, halamt: halAmt })
  })

  // 선택되지 않은 행들은 0으로 초기화
  grid?.getData().forEach(row => {
    if (!selectedRows.find(r => r.getData().iorowno === row.iorowno)) {
      updates.push({ ...row, halamt: 0 })
    }
  })

  grid?.updateData(updates)
  totals.hal = allocatedSum
}

const fetchList = async () => {
  if (!searchData.custcd) return vAlertError('매입처를 선택하세요.')
  try {
    const res = await api.post('/hsio/HSIO_160U_STR', {
      actkind: 'S0',
      cmpycd: authStore.cmpycd,
      jsanym: searchData.jsanym,
      custcd: searchData.custcd
    })

    // Tabulator는 고유 ID가 필요하므로 iorowno 등을 활용하거나 인덱스 부여
    const dataWithId = res.data.map((item: any, idx: number) => ({ ...item, id: idx }))
    grid?.setData(dataWithId)

    // 기존에 할인이 등록된 행(halamt > 0) 자동 선택
    nextTick(() => {
      grid?.getRows().forEach(row => {
        if (Number(row.getData().halamt) > 0) {
          row.select()
        }
      })
    })

    vAlert('조회되었습니다.')
  } catch (e) {
    vAlertError('조회 중 오류가 발생했습니다.')
  }
}

const saveData = async () => {
  const selectedData = grid?.getSelectedData() || []
  if (selectedData.length === 0) return vAlertError('저장할 항목을 선택하세요.')
  if (totals.hal !== inputData.divsum) return vAlertError('배부된 할인금액 합계가 입력한 할인금액과 일치하지 않습니다.')

  // 전표 발행 체크
  if (selectedData.some(item => item.slipymd > "00000000")) {
    return vAlertError('전표가 발행된 자료는 수정할 수 없습니다.')
  }

  if (!confirm('매입할인 정보를 저장하시겠습니까?')) return

  try {
    // ASP 로직처럼 루프를 돌며 개별 저장하거나, 백엔드에서 리스트 처리가 가능하면 일괄 전송
    for (const item of selectedData) {
      const halrate = inputData.divgbn === 'amt'
                      ? (Number(item.ioamt) / totals.amt)
                      : (Number(item.ioqty) / totals.qty)

      await api.post('/hsio/HSIO_160U_STR', {
        actkind: 'A0',
        cmpycd: authStore.cmpycd,
        userid: authStore.userid,
        jsanym: searchData.jsanym,
        jsanymd: searchData.jsanymd,
        custcd: searchData.custcd,
        divgbn: inputData.divgbn,
        // 개별 아이템 정보
        jsanno: item.jsanno,
        ioym: item.ioym,
        iono: item.iono,
        iorowno: item.iorowno,
        deptcd: item.deptcd,
        itemcd: item.itemcd,
        ioqty: item.ioqty,
        ioamt: item.ioamt,
        halamt: item.halamt,
        halrate: halrate.toFixed(8)
      })
    }
    vAlert('정상적으로 저장되었습니다.')
    fetchList()
  } catch (e) {
    vAlertError('저장 중 오류가 발생했습니다.')
  }
}

const deleteData = async () => {
  const selectedData = grid?.getSelectedData() || []
  if (selectedData.length === 0) return vAlertError('삭제할 항목을 선택하세요.')
  if (selectedData.some(item => item.slipymd > "00000000")) return vAlertError('전표가 발행된 자료는 삭제할 수 없습니다.')

  if (!confirm('선택한 항목의 할인 정보를 삭제하시겠습니까?')) return

  try {
    for (const item of selectedData) {
      await api.post('/hsio/HSIO_160U_STR', {
        actkind: 'D0',
        cmpycd: authStore.cmpycd,
        jsanym: item.jsanym,
        jsanno: item.jsanno,
        ioym: item.ioym,
        iono: item.iono,
        iorowno: item.iorowno
      })
    }
    vAlert('삭제되었습니다.')
    fetchList()
  } catch (e) {
    vAlertError('삭제 실패')
  }
}

const initialize = () => {
  searchData.jsanym = initym
  searchData.custcd = ''
  searchData.custnm = ''
  searchData.jsanymd = initymd
  inputData.divsum = 0
  grid?.clearData()
  calculateTotals()
}

// 4. 도움창
const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function openHelp(type: string) {
  Object.assign(modalProps, {
    title: '거래처 선택', path: '/ha00/HA00_00P_STR', defaultField: 'custnm',
    data: { gubun: 'C4', cmpycd: authStore.cmpycd },
    columns: [{ title: '코드', field: 'custcd', width: 80 }, { title: '거래처명', field: 'custnm', width: 200 }],
    onConfirm: (data: any) => {
      searchData.custcd = data.custcd
      searchData.custnm = data.custnm
      fetchList()
    }
  })
  modalVisible.value = true
}

const formatDateString = (v: any, sep: string) => v && v.length === 8 ? `${v.substring(0, 4)}${sep}${v.substring(4, 6)}${sep}${v.substring(6, 8)}` : v
const formatNumber = (val: any) => new Intl.NumberFormat().format(Number(val) || 0)

onUnmounted(() => {
  if (grid) grid.destroy();
});

onMounted(() => {
  nextTick(() => initGrid())
})
</script>
