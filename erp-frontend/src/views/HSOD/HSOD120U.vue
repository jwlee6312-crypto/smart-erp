<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-check2-all me-2 text-primary" style="font-size: 18px;"></i>
        영업정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        주문관리 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        <span class="text-primary fw-bolder">주문승인 (HSOD120U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-save" @click="processApproval">
          {{ searchData.selgbn === 'N' ? '승인처리' : '승인취소' }}
        </button>
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
                <th class="required">주문일자</th>
                <td style="width: 350px;">
                  <div class="d-flex align-items-center gap-1">
                    <input v-model="fromdt" type="date" class="form-control form-control-sm" />
                    <span>~</span>
                    <input v-model="todt" type="date" class="form-control form-control-sm" />
                  </div>
                </td>
                <th class="required">조회대상</th>
                <td style="width: 150px;">
                  <select v-model="searchData.selgbn" class="form-select form-select-sm" @change="search">
                    <option value="N">미승인</option>
                    <option value="Y">승인</option>
                  </select>
                </td>
                <th>주문서종류</th>
                <td>
                  <select v-model="searchData.ordkind" class="form-select form-select-sm" style="width: 150px;" @change="search">
                    <option value="">전체</option>
                    <option v-for="opt in ordKindOptions" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                  </select>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 🅱️ 마스터 그리드 (주문 목록) -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column" style="min-height: 250px;">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
          <span class="fw-bold small text-dark"><i class="bi bi-list-check me-1"></i> 주문 내역</span>
          <div class="d-flex gap-2 align-items-center">
            <span class="small text-muted">전체선택</span>
            <input type="checkbox" v-model="allSelected" @change="toggleAllSelection" class="form-check-input" />
          </div>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="masterGridElement" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>

      <!-- Ⓒ 디테일 정보 영역 (주문 상세) -->
      <div class="card border shadow-sm overflow-hidden d-flex flex-column" style="height: 300px;">
        <div class="card-header bg-light py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
          <span class="fw-bold small text-dark"><i class="bi bi-box-seam me-1"></i> 품목 상세 정보</span>
          <div v-if="selectedOrderInfo" class="small text-muted">
            <span class="me-3">배송처: {{ selectedOrderInfo.address }}</span>
            <span>특기사항: {{ selectedOrderInfo.remark }}</span>
          </div>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="detailGridElement" class="tabulator-instance flex-grow-1"></div>
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
import type { ModalProps } from '@/types/modal'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

const now = new Date()
const initymd = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}`
const initfromdt = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}01`

// 1. 상태 관리
const searchData = reactive({
  fromdt: initfromdt,
  todt: initymd,
  selgbn: 'N',
  ordkind: ''
})

const fromdt = computed({ get: () => formatDateString(searchData.fromdt, '-'), set: (v) => searchData.fromdt = v.replace(/-/g, '') })
const todt = computed({ get: () => formatDateString(searchData.todt, '-'), set: (v) => searchData.todt = v.replace(/-/g, '') })

const masterGridElement = ref<HTMLElement | null>(null)
const detailGridElement = ref<HTMLElement | null>(null)
let masterGrid: Tabulator | null = null
let detailGrid: Tabulator | null = null

const selectedOrderInfo = ref<any>(null)
const activeItemCount = ref(0)
const selectedCount = ref(0)
const detailSum = ref(0)
const allSelected = ref(false)
const ordKindOptions = ref<any[]>([])

// 2. 그리드 초기화
const initGrids = () => {
  if (masterGridElement.value) {
    masterGrid = new Tabulator(masterGridElement.value, {
      layout: "fitColumns",
      height: "100%",
      placeholder: "조회된 데이터가 없습니다.",
      columnDefaults: { headerSort: false },
      columns: [
        {
          title: "선택", field: "procyn", width: 50, hozAlign: "center",
          formatter: "tickCross", editor: true,
          cellClick: (e, cell) => {
              const data = cell.getData();
              if (data.ordemp === 'Y') {
                  vAlertError("출고처리된 주문서입니다. 선택할 수 없습니다.");
                  cell.setValue(false);
              }
          },
          cellEdited: () => updateSelectedCount()
        },
        { title: "주문번호", field: "ord_full", width: 120, hozAlign: "center", cssClass: "fw-bold text-primary cursor-pointer" },
        { title: "주문일자", field: "ordymd_fmt", width: 100, hozAlign: "center" },
        { title: "주문구분", field: "ordkindnm", width: 100, hozAlign: "center" },
        { title: "거래처", field: "custnm", minWidth: 200 },
        { title: "출고일자", field: "outymd_fmt", width: 120, editor: "date", cssClass: "bg-light-yellow" },
        { title: "주문금액", field: "ordamt", width: 120, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
        {
            title: "영업부서", field: "deptnm", width: 150,
            formatter: (cell) => (cell.getValue() || '') + " <i class='bi bi-search text-primary ms-1 cursor-pointer'></i>",
            cellClick: (e, cell) => { if ((e.target as HTMLElement).classList.contains('bi-search')) openHelp('DEPT', cell.getRow()) }
        },
        {
            title: "영업담당", field: "oempnm", width: 120,
            formatter: (cell) => (cell.getValue() || '') + " <i class='bi bi-search text-primary ms-1 cursor-pointer'></i>",
            cellClick: (e, cell) => { if ((e.target as HTMLElement).classList.contains('bi-search')) openHelp('EMP', cell.getRow()) }
        }
      ]
    })

    masterGrid.on("rowClick", (e, row) => {
        const data = row.getData()
        fetchDetails(data)
    })
  }

  if (detailGridElement.value) {
    detailGrid = new Tabulator(detailGridElement.value, {
      layout: "fitColumns",
      height: "100%",
      placeholder: "주문을 선택하세요.",
      columnDefaults: { headerSort: false },
      columns: [
        { title: "No", field: "orowno", width: 50, hozAlign: "center" },
        { title: "품명", field: "itemnm", minWidth: 200 },
        { title: "규격", field: "itsize", width: 150 },
        { title: "단위", field: "unit", width: 60, hozAlign: "center" },
        { title: "수량", field: "ordqty", width: 100, hozAlign: "right", formatter: "money", formatterParams: { precision: 2 } },
        { title: "공급가", field: "ordamt", width: 120, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
        { title: "부가세", field: "ordvat", width: 110, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
        { title: "합계", field: "sumamt", width: 130, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "fw-bold" }
      ]
    })
  }
}

// 3. 기능 구현
async function fetchOptions() {
  try {
    const res = await api.get('/hs00/HS00_000S_STR', { params: { gubun: 'E0', cmpycd: authStore.cmpycd, gbncd: '220' } })
    ordKindOptions.value = res.data.map((i: any) => ({ codecd: Object.values(i)[0], codenm: Object.values(i)[1] }))
  } catch (e) { console.error('옵션 로드 실패') }
}

async function search() {
  if (!searchData.fromdt || !searchData.todt) return vAlertError('조회 기간을 입력하세요.')
  try {
    const res = await api.post('/hsod/HSOD_120U_STR', {
      actkind: 'S0',
      cmpycd: authStore.cmpycd,
      ordkind: searchData.ordkind,
      selgbn: searchData.selgbn,
      fromdt: searchData.fromdt,
      todt: searchData.todt
    })
    if (masterGrid) {
      const mappedData = res.data.map((i: any) => ({
        ...i,
        procyn: searchData.selgbn === 'N' && i.ordemp !== 'Y',
        ord_full: `${i.ordym}-${i.ordno}`,
        ordymd_fmt: formatDateString(i.ordymd, '-'),
        outymd_fmt: formatDateString(i.OUtymd, '-')
      }))
      masterGrid.setData(mappedData)
      allSelected.value = searchData.selgbn === 'N'
      updateSelectedCount()
      detailGrid?.clearData()
      selectedOrderInfo.value = null
    }
  } catch (e) { vAlertError('조회 실패') }
}

async function fetchDetails(row: any) {
  try {
    const res = await api.post('/hsod/HSOD_120U_STR', {
      actkind: 'S1',
      cmpycd: authStore.cmpycd,
      ordym: row.ordym,
      ordno: row.ordno
    })
    selectedOrderInfo.value = {
        address: row.address || '지정된 배송처 없음',
        remark: row.remark || '특기사항 없음'
    }
    if (detailGrid) {
      const mapped = res.data.map((i: any) => ({
          ...i,
          sumamt: Number(i.ordamt || 0) + Number(i.ordvat || 0)
      }))
      detailGrid.setData(mapped)
      detailSum.value = mapped.reduce((acc: number, cur: any) => acc + cur.sumamt, 0)
    }
  } catch (e) { vAlertError('상세 조회 실패') }
}

async function processApproval() {
  const selectedRows = masterGrid?.getData().filter((i: any) => i.procyn)
  if (!selectedRows || selectedRows.length === 0) return vAlertError('처리할 대상을 선택하세요.')

  // 승인 시 필수 항목 체크
  if (searchData.selgbn === 'N') {
      for (const row of selectedRows) {
          if (!row.outymd_fmt) return vAlertError(`[${row.ord_full}] 출고일자를 입력하세요.`)
          if (!row.deptcd) return vAlertError(`[${row.ord_full}] 영업부서를 선택하세요.`)
          if (!row.ordemp) return vAlertError(`[${row.ord_full}] 영업담당자를 선택하세요.`)
      }
  }

  const confirmMsg = searchData.selgbn === 'N' ? '선택한 주문을 승인처리 하시겠습니까?' : '선택한 주문의 승인을 취소하시겠습니까?'
  if (!confirm(confirmMsg)) return

  try {
    for (const row of selectedRows) {
      await api.post('/hsod/HSOD_120U_STR', {
        actkind: 'U0',
        cmpycd: authStore.cmpycd,
        ordkind: searchData.ordkind,
        selgbn: searchData.selgbn,
        ordym: row.ordym,
        ordno: row.ordno,
        OUtymd: row.outymd_fmt.replace(/-/g, ''),
        deptcd: row.deptcd,
        whcd: row.whcd,
        ordemp: row.ordemp,
        userid: authStore.userid
      })
    }
    vAlert('정상적으로 처리되었습니다.')
    search()
  } catch (e) { vAlertError('처리 중 오류 발생') }
}

const updateSelectedCount = () => {
    if (!masterGrid) return
    selectedCount.value = masterGrid.getData().filter((i: any) => i.procyn).length
}

const toggleAllSelection = () => {
  if (!masterGrid) return
  const data = masterGrid.getData()
  masterGrid.updateData(data.map(i => ({ ...i, procyn: allSelected.value && i.ordemp !== 'Y' })))
  updateSelectedCount()
}

function initialize() {
  resetForm(searchData)
  Object.assign(searchData, { fromdt: initfromdt, todt: initymd, selgbn: 'N', ordkind: '' })
  masterGrid?.clearData()
  detailGrid?.clearData()
  selectedOrderInfo.value = null
  selectedCount.value = 0
  detailSum.value = 0
}

// 4. 팝업 설정
const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function openHelp(type: string, row: any) {
  if (type === 'DEPT') {
    Object.assign(modalProps, {
      title: '부서 선택', path: '/ha00/HA00_00P_STR', defaultField: 'deptnm',
      data: { gubun: 'D0', cmpycd: authStore.cmpycd },
      columns: [{ title: '코드', field: 'deptcd', width: 80 }, { title: '부서명', field: 'deptnm', width: 180 }],
      onConfirm: (data: any) => { row.update({ deptcd: data.deptcd, deptnm: data.deptnm }) }
    })
  } else if (type === 'EMP') {
    Object.assign(modalProps, {
      title: '사원 선택', path: '/ha00/HA00_00P_STR', defaultField: 'usernm',
      data: { gubun: 'U1', cmpycd: authStore.cmpycd },
      columns: [{ title: '코드', field: 'userid', width: 80 }, { title: '사원명', field: 'usernm', width: 150 }],
      onConfirm: (data: any) => { row.update({ ordemp: data.userid, oempnm: data.usernm }) }
    })
  }
  modalVisible.value = true
}

const formatDateString = (v: any, sep: string) => v && v.length === 8 ? `${v.substring(0, 4)}${sep}${v.substring(4, 6)}${sep}${v.substring(6, 8)}` : v
const formatNumber = (val: any) => new Intl.NumberFormat().format(Number(val) || 0)

onMounted(async () => {
  await fetchOptions()
  nextTick(() => initGrids())
})
</script>
