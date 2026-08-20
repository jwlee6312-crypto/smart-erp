<!--기본정보/재무관리/타계정계정과목등록-연동 [ERP 프리미엄 표준] -->
<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container">
    <!-- 🚀 1. 상단 액션 바 (고정) -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-1 px-3 sticky-top shadow-sm flex-shrink-0">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 13px;">
        <i class="bi bi-journal-plus me-2 text-primary" style="font-size: 18px;"></i>
        기본정보 > 타계정등록(연동) > <span class="text-primary fw-bolder">타계정 설정 (HSBA721U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-save" @click="save">저장</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2">
      <!-- 🅰️ 상세 정보 입력 (10열 고밀도 배치) -->
      <div class="card border shadow-sm overflow-hidden flex-shrink-0">
        <div class="card-header py-1 px-3 border-bottom d-flex align-items-center justify-content-between" style="background-color: #f8f9fa;">
          <span class="fw-bold small text-dark"><i class="bi bi-pencil-square me-1"></i> 타계정 유형 및 연동계정 설정</span>
          <div class="d-flex gap-2 align-items-center">
            <span v-if="formData.actkind === 'U0'" class="badge bg-warning text-dark" style="font-size: 10px;">수정 중</span>
            <span v-else class="badge bg-primary" style="font-size: 10px;">신규 등록</span>
          </div>
        </div>
        <div class="card-body p-0">
          <table class="erp-table-full border-0">
            <colgroup>
              <col style="width: 85px;" /><col />
              <col style="width: 85px;" /><col />
              <col style="width: 85px;" /><col />
              <col style="width: 85px;" /><col />
              <col style="width: 80px;" /><col style="width: 80px;" />
            </colgroup>
            <tbody>
              <tr>
                <th class="required">유형코드</th>
                <td><input v-model="formData.tatype" type="text" class="form-control form-control-sm text-center fw-bold text-primary" maxlength="3" :readonly="formData.actkind === 'U0'" placeholder="code" /></td>
                <th class="required">유형명칭</th>
                <td><input v-model="formData.tatypenm" type="text" class="form-control form-control-sm" maxlength="50" /></td>
                <th class="required">차변계정</th>
                <td>
                  <div class="input-group input-group-sm">
                    <input v-model="formData.dacctcd" type="text" class="form-control bg-light text-center" style="max-width: 60px;" readonly />
                    <input v-model="formData.dacctnm" type="text" class="form-control" placeholder="계정 검색" @keyup.enter="openAccountHelp('D')" />
                    <button class="btn btn-outline-secondary btn-sm px-1" @click="openAccountHelp('D')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="required">대변계정</th>
                <td>
                  <div class="input-group input-group-sm">
                    <input v-model="formData.cacctcd" type="text" class="form-control bg-light text-center" style="max-width: 60px;" readonly />
                    <input v-model="formData.cacctnm" type="text" class="form-control" placeholder="계정 검색" @keyup.enter="openAccountHelp('C')" />
                    <button class="btn btn-outline-secondary btn-sm px-1" @click="openAccountHelp('C')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="text-danger">삭제</th>
                <td class="text-center">
                  <div class="form-check d-inline-block">
                    <input v-model="deleteCheck" class="form-check-input" type="checkbox" id="delCheck721">
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Ⓒ 품목 리스트 (15행 페이징 처리) -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
          <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-1"></i> 타계정 연동 리스트</span>
          <span class="text-muted" style="font-size: 11px;">※ 행 클릭 시 상단에 정보가 로드됩니다. (페이지당 15행)</span>
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
const deleteCheck = ref(false)
const formData = reactive({
  actkind: 'A0', cmpycd: authStore.cmpycd, userid: authStore.user_id,
  tatype: '', tatypenm: '', dacctcd: '', dacctnm: '', cacctcd: '', cacctnm: '',
  updemp: authStore.user_id
})

const gridElement = ref<HTMLElement | null>(null); const grid = ref<Tabulator | null>(null); const activeItemCount = ref(0)

// 2. 그리드 초기화
const initGrid = () => {
  if (!gridElement.value) return
  grid.value = new Tabulator(gridElement.value, {
    layout: "fitColumns", height: "100%", pagination: "local", paginationSize: 15,
    placeholder: "조회된 데이터가 없습니다.", columnDefaults: { headerSort: false, headerHozAlign: "center" },
    columns: [
      { title: "유형코드", field: "code", width: 100, hozAlign: "center", cssClass: "fw-bold bg-light" },
      { title: "타계정 유형명", field: "cdnm", width: 200, cssClass: "text-primary fw-bold" },
      { title: "차변코드", field: "dacctcd", width: 100, hozAlign: "center" },
      { title: "차변계정명", field: "dacctnm", widthGrow: 1 },
      { title: "대변코드", field: "cacctcd", width: 100, hozAlign: "center" },
      { title: "대변계정명", field: "cacctnm", widthGrow: 1 }
    ]
  })
  grid.value.on("rowClick", (e, row) => {
    const data = row.getData()
    formData.tatype = data.code
    formData.tatypenm = data.cdnm
    formData.dacctcd = data.dacctcd
    formData.dacctnm = data.dacctnm
    formData.cacctcd = data.cacctcd
    formData.cacctnm = data.cacctnm
    formData.actkind = 'U0'
    deleteCheck.value = false
  })
}

// 3. 기능 구현
async function search() {
  try {
    const res = await api.post('/hsba/HSBA_721U_STR', {
      actkind: 'S0', cmpycd: authStore.cmpycd, tatype: '', dacctcd: '', cacctcd: '', userid: authStore.user_id
    })
    if (grid.value) {
      grid.value.setData(res.data || [])
      activeItemCount.value = (res.data || []).length
    }
  } catch (e) { vAlertError('조회 실패') }
}

async function save() {
  if (!formData.tatype || !formData.tatypenm) return vAlertError('유형 코드와 명칭은 필수입니다.')
  if (!formData.dacctcd || !formData.cacctcd) return vAlertError('차변/대변 계정을 모두 선택하십시오.')

  if (!confirm('설정 정보를 저장하시겠습니까?')) return

  // ASP 로직: 삭제 체크 시 actkind를 D0로 변경
  const finalAct = deleteCheck.value ? 'D0' : formData.actkind

  try {
    const res = await api.post('/hsba/HSBA_721U_STR', { ...formData, actkind: finalAct })
    vAlert('성공적으로 저장되었습니다.')
    search()
    initialize()
  } catch (e) { vAlertError('저장 중 오류 발생') }
}

function initialize() {
  resetForm(formData)
  Object.assign(formData, {
    actkind: 'A0', cmpycd: authStore.cmpycd, userid: authStore.user_id, updemp: authStore.user_id
  })
  deleteCheck.value = false
}

// 4. 도움창(모달) 연동
const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function openAccountHelp(mode: 'D' | 'C') {
  const gbn = mode === 'D' ? '62' : '112' // ASP 코드 규격 적용

  Object.assign(modalProps, {
    title: mode === 'D' ? '차변 계정 선택' : '대변 계정 선택',
    path: '/ha00/HA00_00P_STR',
    data: { gubun: 'AC', ACCT: gbn, cmpycd: authStore.cmpycd },
    defaultField: 'cdnm',
    columns: [
      { title: '코드', field: 'code', width: 100 },
      { title: '계정명', field: 'cdnm', width: 200 }
    ],
    onConfirm: (selected: any) => {
      if (mode === 'D') {
        formData.dacctcd = selected.code
        formData.dacctnm = selected.cdnm
      } else {
        formData.cacctcd = selected.code
        formData.cacctnm = selected.cdnm
      }
    }
  })
  modalVisible.value = true
}

onMounted(() => {
  nextTick(() => {
    initGrid()
    search()
  })
})
</script>
