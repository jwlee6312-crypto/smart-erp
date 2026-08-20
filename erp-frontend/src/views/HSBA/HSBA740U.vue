<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-gear-wide-connected me-2 text-primary" style="font-size: 18px;"></i>
        기본정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        지불계정관리 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        <span class="text-primary fw-bolder">지불계정관리 (HSBA740U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchList">조회</button>
        <button class="btn-erp btn-save" @click="saveData">저장</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2">
      <!-- 🅰️ 상세 정보 입력 영역 -->
      <div class="card border shadow-sm overflow-hidden">
        <div class="card-header bg-light py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
          <span class="fw-bold small text-dark"><i class="bi bi-pencil-square me-1"></i> 지불 유형 정보 입력</span>
          <span v-if="formData.actkind === 'U0'" class="badge bg-warning text-dark">수정 모드</span>
          <span v-else class="badge bg-primary">신규 등록</span>
        </div>
        <div class="card-body p-0">
          <table class="erp-table-full">
            <tbody>
              <tr>
                <th class="required">지불유형</th>
                <td>
                  <input v-model="formData.paygbn" type="text" class="form-control form-control-sm text-center fw-bold" style="width: 100px;" maxlength="3" :readonly="formData.actkind === 'U0'" />
                </td>
                <th class="required">지불유형명</th>
                <td>
                  <input v-model="formData.paygbnNM" type="text" class="form-control form-control-sm" style="width: 250px;" maxlength="20" />
                </td>
                <th>삭제여부</th>
                <td>
                  <div class="form-check form-check-inline mb-0">
                    <input v-model="formData.useyn" class="form-check-input" type="checkbox" id="delYn740" true-value="N" false-value="Y">
                    <label class="form-check-label small" for="delYn740">삭제</label>
                  </div>
                </td>
              </tr>
              <tr>
                <th class="required">차변 발생계정</th>
                <td>
                  <div class="input-group input-group-sm" style="width: 300px;">
                    <input v-model="formData.dacctcd" type="text" class="form-control text-center bg-light" style="max-width: 80px;" readonly />
                    <input v-model="formData.dacctnm" type="text" class="form-control" placeholder="계정 선택" @keyup.enter="openHelp('DACCT')" />
                    <button class="btn btn-outline-secondary" @click="openHelp('DACCT')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="required">대변 발생계정</th>
                <td colspan="3">
                  <div class="input-group input-group-sm" style="width: 300px;">
                    <input v-model="formData.cacctcd" type="text" class="form-control text-center bg-light" style="max-width: 80px;" readonly />
                    <input v-model="formData.cacctnm" type="text" class="form-control" placeholder="계정 선택" @keyup.enter="openHelp('CACCT')" />
                    <button class="btn btn-outline-secondary" @click="openHelp('CACCT')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 🅱️ 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column">
        <div class="card-header bg-light py-1 px-3 border-bottom fw-bold small text-dark">
          <i class="bi bi-list-ul me-1"></i> 지불 유형 목록
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
const formData = reactive<any>({
  actkind: 'A0',
  cmpycd: authStore.cmpycd,
  paygbn: '',
  paygbnNM: '',
  dacctcd: '', dacctnm: '',
  cacctcd: '', cacctnm: '',
  useyn: 'Y'
})

const activeItemCount = ref(0)
const gridElement = ref<HTMLElement | null>(null)
let grid: Tabulator | null = null

// 2. 그리드 초기화
const initGrid = () => {
  if (gridElement.value) {
    grid = new Tabulator(gridElement.value, {
      layout: "fitColumns",
      height: "100%",
      placeholder: "조회된 데이터가 없습니다.",
      columns: [
        { title: "유형코드", field: "paygbn", width: 100, hozAlign: "center", cssClass: "fw-bold" },
        { title: "지불 유형명", field: "paygbnNM", width: 200 },
        { title: "차변 발생계정", field: "dacctnm", minWidth: 200 },
        { title: "대변 발생계정", field: "cacctnm", minWidth: 200 },
      ],
    })

    grid.on("rowClick", (e, row) => {
      const data = row.getData()
      Object.assign(formData, data)
      formData.actkind = 'U0'
      formData.useyn = 'Y'
    })
  }
}

// 3. 비즈니스 로직
async function fetchList() {
  try {
    const res = await api.post('/hsba/HSBA_740U_STR', {
      actkind: 'S0',
      cmpycd: authStore.cmpycd
    })
    grid?.setData(res.data)
    activeItemCount.value = res.data.length
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 중 오류 발생') }
}

async function saveData() {
  if (!formData.paygbn) return vAlertError('지불유형을 입력하세요.')
  if (!formData.paygbnNM) return vAlertError('지불유형명을 입력하세요.')
  if (!formData.dacctcd || !formData.cacctcd) return vAlertError('차변/대변 계정과목을 선택하세요.')

  if (!confirm('자료를 저장하시겠습니까?')) return

  try {
    const res = await api.post('/hsba/HSBA_740U_STR', {
      ...formData,
      userid: authStore.userid
    })

    if (res.data?.[0]?.ERRYN === 'Y') {
      vAlertError(res.data[0].MSG || '저장 중 오류 발생')
    } else {
      vAlert('성공적으로 저장되었습니다.')
      fetchList()
      initialize()
    }
  } catch (e) { vAlertError('서버 통신 오류') }
}

function initialize() {
  resetForm(formData)
  Object.assign(formData, {
    actkind: 'A0',
    cmpycd: authStore.cmpycd,
    useyn: 'Y'
  })
}

// 4. 도움창 (Modal)
const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function openHelp(type: string) {
  let acctType = '21' // 기본 차변
  let title = '차변계정 선택'

  if (type === 'CACCT') {
    acctType = '111'
    title = '대변계정 선택'
  }

  Object.assign(modalProps, {
    title, path: '/comm/HELP_acctcd_LTD', defaultField: 'acctnm',
    data: { ACCT: acctType, cmpycd: authStore.cmpycd },
    columns: [
      { title: '코드', field: 'acctcd', width: 80 },
      { title: '계정과목명', field: 'acctnm', width: 200 }
    ],
    onConfirm: (data: any) => {
      if (type === 'DACCT') { formData.dacctcd = data.acctcd; formData.dacctnm = data.acctnm }
      else if (type === 'CACCT') { formData.cacctcd = data.acctcd; formData.cacctnm = data.acctnm }
    }
  })
  modalVisible.value = true
}

onMounted(() => {
  nextTick(() => {
    initGrid()
    fetchList()
  })
})
</script>
