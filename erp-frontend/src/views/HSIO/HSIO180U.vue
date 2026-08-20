<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-file-earmark-x-fill me-2 text-danger" style="font-size: 18px;"></i>
        구매정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        매입정산 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        <span class="text-primary fw-bolder">매입할인전표 취소 (HSIO180U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchList">조회</button>
        <button class="btn-erp btn-delete" @click="deleteData">전표취소</button>
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
                <th class="required">발행부서</th>
                <td>
                  <div class="input-group input-group-sm" style="width: 250px;">
                    <input v-model="searchData.deptcd" type="text" class="form-control text-center bg-light" style="max-width: 80px;" readonly />
                    <input v-model="searchData.deptnm" type="text" class="form-control" placeholder="부서 선택" @keyup.enter="openHelp('DEPT')" />
                    <button class="btn btn-outline-secondary" @click="openHelp('DEPT')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="required">발행일자</th>
                <td>
                  <div class="d-flex align-items-center gap-1" style="width: 260px;">
                    <input v-model="fromdt" type="date" class="form-control form-control-sm" />
                    <span class="px-1">~</span>
                    <input v-model="todt" type="date" class="form-control form-control-sm" />
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
          <span class="fw-bold small text-dark"><i class="bi bi-list-ul me-1"></i> 전표 발행 내역</span>
          <div class="small text-muted">마감된 자료는 취소할 수 없습니다.</div>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="gridElement" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>
    </div>

    <!-- 📊 하단 합계 바 -->
    <div class="erp-footer bg-dark text-white py-2 px-4 shadow-lg sticky-bottom">
      <div class="d-flex justify-content-between align-items-center w-100">
        <div class="small">선택건수: <span class="fw-bold text-info">{{ selectedCount }}</span> 건</div>
        <div class="small">할인금액 합계: <span class="fw-bold text-warning" style="font-size: 16px;">{{ formatNumber(totalHalAmt) }}</span> 원</div>
      </div>
    </div>

    <Modal v-model:visible="modalVisible" :modalProps="modalProps" />
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, computed, nextTick, onUnmounted } from 'vue'
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
  deptcd: authStore.deptcd,
  deptnm: authStore.deptnm,
  fromdt: initfromdt,
  todt: initymd
})

const closingInfo = reactive({ clsymd: '', sclsym: '' })
const autoslipinfo = ref('N')

const fromdt = computed({ get: () => formatDateString(searchData.fromdt, '-'), set: (v) => searchData.fromdt = v.replace(/-/g, '') })
const todt = computed({ get: () => formatDateString(searchData.todt, '-'), set: (v) => searchData.todt = v.replace(/-/g, '') })

const gridElement = ref<HTMLElement | null>(null)
let grid: Tabulator | null = null
const selectedCount = ref(0)
const totalHalAmt = ref(0)

// 2. 그리드 초기화
const initGrid = () => {
  if (gridElement.value) {
    grid = new Tabulator(gridElement.value, {
      layout: "fitColumns",
      height: "100%",
      placeholder: "조회된 데이터가 없습니다.",
      selectable: (row) => {
        const data = row.getData()
        return checkCanCancel(data) === true
      },
      columns: [
        { title: "선택", formatter: "rowSelection", titleFormatter: "rowSelection", width: 40, hozAlign: "center", headerSort: false },
        {
          title: "전표번호", field: "slipno_disp", width: 180, hozAlign: "center",
          formatter: (cell) => {
            const d = cell.getData()
            return `<a class="text-primary fw-bold text-decoration-none">${d.slipymd}-${d.slipno}</a>`
          },
          cellClick: (e, cell) => printSlip(cell.getData())
        },
        { title: "발행부서", field: "deptnm", width: 180 },
        { title: "거래처", field: "custnm", width: 250 },
        { title: "할인금액", field: "halamt", hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "fw-bold" },
      ],
    })

    grid.on("rowSelectionChanged", () => {
      const selectedData = grid?.getSelectedData() || []
      selectedCount.value = selectedData.length
      totalHalAmt.value = selectedData.reduce((acc, row) => acc + Number(row.halamt || 0), 0)
    })
  }
}

// 마감 및 취소 가능 여부 체크
const checkCanCancel = (row: any) => {
  const halYm = String(row.halymd || '').substring(0, 6)
  if (halYm <= closingInfo.sclsym) return "영업정보 마감"
  if (row.halymd <= closingInfo.clsymd) return "회계정보 마감"
  if (row.cfmyn === 'Y' && autoslipinfo.value === 'N') return "확정전표"
  return true
}

// 3. 비즈니스 로직
const fetchList = async () => {
  if (!searchData.deptcd) return vAlertError('발행부서를 선택하세요.')
  try {
    const res = await api.post('/hsio/HSIO_180U_STR', {
      actkind: 'S0',
      cmpycd: authStore.cmpycd,
      fromdt: searchData.fromdt,
      todt: searchData.todt,
      deptcd: searchData.deptcd
    })
    grid?.setData(res.data)
    vAlert('조회되었습니다.')
  } catch (e) {
    vAlertError('조회 실패')
  }
}

const deleteData = async () => {
  const selectedData = grid?.getSelectedData() || []
  if (selectedData.length === 0) return vAlertError('취소할 전표를 선택하세요.')

  if (!confirm('선택한 전표를 삭제(취소)하시겠습니까?')) return

  try {
    for (const item of selectedData) {
      await api.post('/hsio/HSIO_180U_STR', {
        actkind: 'D0',
        cmpycd: authStore.cmpycd,
        updemp: authStore.userid,
        fromdt: searchData.fromdt,
        todt: searchData.todt,
        deptcd: item.deptcd,
        slipymd: item.slipymd,
        slipno: item.slipno
      })
    }
    vAlert('전표 취소가 완료되었습니다.')
    fetchList()
  } catch (e) {
    vAlertError('취소 처리 실패')
  }
}

const initialize = () => {
  resetForm(searchData)
  Object.assign(searchData, { deptcd: authStore.deptcd, deptnm: authStore.deptnm, fromdt: initfromdt, todt: initymd })
  grid?.clearData()
  selectedCount.value = 0
  totalHalAmt.value = 0
}

const printSlip = (row: any) => {
  const url = `../HASL/HASL_SLIP_PRINT.asp?slipgu=010&slipymd=${row.slipymd}&slipno=${row.slipno}&deptcd=${row.deptcd}`
  window.open(url, '전표인쇄', 'left=10,top=10,width=700,height=650,scrollbars=yes')
}

// 4. 도움창
const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function openHelp(type: string) {
  Object.assign(modalProps, {
    title: '부서 선택', path: '/ha00/HA00_00P_STR', defaultField: 'deptnm',
    data: { gubun: 'D0', cmpycd: authStore.cmpycd },
    columns: [{ title: '코드', field: 'deptcd', width: 80 }, { title: '부서명', field: 'deptnm', width: 200 }],
    onConfirm: (data: any) => {
      searchData.deptcd = data.deptcd
      searchData.deptnm = data.deptnm
    }
  })
  modalVisible.value = true
}

const formatDateString = (v: any, sep: string) => v && v.length === 8 ? `${v.substring(0, 4)}${sep}${v.substring(4, 6)}${sep}${v.substring(6, 8)}` : v
const formatNumber = (val: any) => new Intl.NumberFormat().format(Number(val) || 0)

onUnmounted(() => {
  if (grid) grid.destroy();
});

onMounted(async () => {
  api.get('/hp00/HP00_000S_STR', { params: { gubun: 'CL', cmpycd: authStore.cmpycd } }).then(r => {
    if (r.data?.length) {
      closingInfo.clsymd = String(Object.values(r.data[0])[0]).trim()
      closingInfo.sclsym = String(Object.values(r.data[0])[1]).trim()
    }
  })
  api.post('/ha00/HA00_010S_STR', { cmpycd: authStore.cmpycd, gubun: 'P1' }).then(r => {
    if (r.data?.length) autoslipinfo.value = r.data[0].slipyn || 'N'
  })

  nextTick(() => initGrid())
})
</script>
