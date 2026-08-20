<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-file-earmark-post me-2 text-primary" style="font-size: 18px;"></i>
        구매정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        매입정산 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        <span class="text-primary fw-bolder">외부매입할인전표 발행 (HSIO171U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchList">조회</button>
        <button class="btn-erp btn-save" @click="saveData">전표발행</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2">
      <!-- 🅰️ 조회 및 발행 조건 영역 -->
      <div class="card border shadow-sm overflow-hidden">
        <div class="card-body p-0">
          <table class="erp-table-full">
            <tbody>
              <tr>
                <th class="required">정산연월</th>
                <td>
                  <input v-model="uijsanym" type="month" class="form-control form-control-sm" style="width: 150px;" @change="fetchList" />
                </td>
                <th class="required">전표일자</th>
                <td>
                  <input v-model="uislipymd" type="date" class="form-control form-control-sm" style="width: 150px;" />
                </td>
                <th class="required">발행부서</th>
                <td>
                  <div class="input-group input-group-sm" style="width: 250px;">
                    <input v-model="formData.deptcd" type="text" class="form-control text-center bg-light" style="max-width: 80px;" readonly />
                    <input v-model="formData.deptnm" type="text" class="form-control" placeholder="부서 선택" @keyup.enter="openHelp('DEPT')" />
                    <button class="btn btn-outline-secondary" @click="openHelp('DEPT')"><i class="bi bi-search"></i></button>
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
          <span class="fw-bold small text-dark"><i class="bi bi-list-check me-1"></i> 매입할인 내역</span>
          <div class="small text-muted">발행할 항목을 선택한 후 [전표발행] 버튼을 클릭하세요.</div>
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
const initym = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}`

// 1. 상태 관리
const searchData = reactive({ jsanym: initym })
const formData = reactive({
  slipymd: initymd,
  deptcd: authStore.deptcd,
  deptnm: authStore.deptnm
})
const closingInfo = reactive({ clsymd: '', sclsym: '' })

const uijsanym = computed({
  get: () => searchData.jsanym ? `${searchData.jsanym.substring(0, 4)}-${searchData.jsanym.substring(4, 6)}` : '',
  set: (v) => searchData.jsanym = v.replace(/-/g, '')
})
const uislipymd = computed({
  get: () => formatDateString(formData.slipymd, '-'),
  set: (v) => formData.slipymd = v.replace(/-/g, '')
})

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
      selectable: true,
      columns: [
        { title: "선택", formatter: "rowSelection", titleFormatter: "rowSelection", width: 40, hozAlign: "center", headerSort: false },
        { title: "거래처", field: "custnm", width: 300 },
        { title: "매입부서", field: "deptnm", width: 250 },
        { title: "매입할인액", field: "halamt", hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "fw-bold text-primary" },
      ],
    })

    grid.on("rowSelectionChanged", () => {
      const selectedData = grid?.getSelectedData() || []
      selectedCount.value = selectedData.length
      totalHalAmt.value = selectedData.reduce((acc, row) => acc + Number(row.halamt || 0), 0)
    })
  }
}

// 3. 비즈니스 로직
const fetchList = async () => {
  if (!searchData.jsanym) return vAlertError('정산연월을 선택하세요.')
  try {
    const res = await api.post('/hsio/HSIO_171U_STR', {
      actkind: 'S0',
      cmpycd: authStore.cmpycd,
      jsanym: searchData.jsanym
    })
    grid?.setData(res.data)
    vAlert('조회되었습니다.')
  } catch (e) {
    vAlertError('조회 실패')
  }
}

const saveData = async () => {
  const selectedData = grid?.getSelectedData() || []
  if (selectedData.length === 0) return vAlertError('전표발행 대상을 선택하시기 바랍니다.')

  // 마감 체크
  if (formData.slipymd <= closingInfo.clsymd) {
    return vAlertError("회계정보가 마감이 되었습니다. 해당 전표일자로 작업할 수 없습니다.")
  }
  if (formData.slipymd.substring(0, 6) <= closingInfo.sclsym) {
    return vAlertError("영업정보가 마감이 되었습니다. 해당 전표일자로 작업할 수 없습니다.")
  }

  if (!confirm("전표를 발행 하시겠습니까?")) return

  try {
    for (const item of selectedData) {
      // 1. 전표 생성 (A0)
      const resA0 = await api.post('/hsio/HSIO_171U_STR', {
        actkind: 'A0',
        cmpycd: authStore.cmpycd,
        userid: authStore.userid,
        jsanym: searchData.jsanym,
        custcd: item.custcd,
        halamt: item.halamt,
        deptcd: formData.deptcd,
        slipymd: formData.slipymd
      })

      const slipNo = resA0.data?.[0]?.slipno || ''

      // 2. 전표 정보 업데이트 (U0)
      const resU0 = await api.post('/hsio/HSIO_171U_STR', {
        actkind: 'U0',
        cmpycd: authStore.cmpycd,
        userid: authStore.userid,
        jsanym: searchData.jsanym,
        custcd: item.custcd,
        halamt: item.halamt,
        deptcd: formData.deptcd,
        slipymd: formData.slipymd,
        slipno: slipNo
      })

      if (resU0.data?.[0]?.RTN_CD && resU0.data[0].RTN_CD !== '00000000') {
        vAlertError(resU0.data[0].RTN_MSG || '전표 발행 중 오류 발생')
        return
      }
    }

    vAlert('정상적으로 발행되었습니다.')
    fetchList()
  } catch (e) {
    vAlertError('전표 발행 처리 실패')
  }
}

const initialize = () => {
  searchData.jsanym = initym
  formData.slipymd = initymd
  grid?.clearData()
  totalHalAmt.value = 0
  selectedCount.value = 0
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
      formData.deptcd = data.deptcd
      formData.deptnm = data.deptnm
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
  nextTick(() => initGrid())
})
</script>
