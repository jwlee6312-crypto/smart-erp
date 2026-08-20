<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-gear-fill me-2 text-primary" style="font-size: 18px;"></i>
        기본정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        매입할인 계정과목 관리 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        <span class="text-primary fw-bolder">매입할인 계정과목 관리 (HSBA730U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1">
        <button class="btn-erp btn-save" @click="saveData">저장</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-auto p-4 d-flex justify-content-center bg-light">
      <div class="card border shadow-sm" style="width: 100%; max-width: 800px; height: fit-content;">
        <div class="card-header bg-white py-2 px-3 border-bottom fw-bold text-dark">
          <i class="bi bi-pencil-square me-1"></i> 계정과목 설정
        </div>
        <div class="card-body p-0">
          <table class="erp-table-full">
            <tbody>
              <!-- 🅰️ 금액 관련 설정 -->
              <tr class="bg-light">
                <th colspan="2" class="text-start ps-3 py-2 fs-6 fw-bolder text-primary">
                  <i class="bi bi-coin me-2"></i>금액 (Amount) 관련 설정
                </th>
              </tr>
              <tr>
                <th class="required" style="width: 150px;">차변 발생계정</th>
                <td>
                  <div class="input-group input-group-sm" style="width: 350px;">
                    <input v-model="formData.dacctcd" type="text" class="form-control text-center bg-light" style="max-width: 80px;" readonly />
                    <input v-model="formData.dacctnm" type="text" class="form-control" placeholder="계정 선택" @keyup.enter="openHelp('DACCT')" />
                    <button class="btn btn-outline-secondary" @click="openHelp('DACCT')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
              </tr>
              <tr>
                <th class="required">대변 발생계정</th>
                <td>
                  <div class="input-group input-group-sm" style="width: 350px;">
                    <input v-model="formData.cacctcd" type="text" class="form-control text-center bg-light" style="max-width: 80px;" readonly />
                    <input v-model="formData.cacctnm" type="text" class="form-control" placeholder="계정 선택" @keyup.enter="openHelp('CACCT')" />
                    <button class="btn btn-outline-secondary" @click="openHelp('CACCT')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
              </tr>

              <!-- 🅱️ 수량 관련 설정 -->
              <tr class="bg-light border-top">
                <th colspan="2" class="text-start ps-3 py-2 fs-6 fw-bolder text-success">
                  <i class="bi bi-box-seam me-2"></i>수량 (Quantity) 관련 설정
                </th>
              </tr>
              <tr>
                <th style="width: 150px;">차변 발생계정</th>
                <td>
                  <div class="input-group input-group-sm" style="width: 350px;">
                    <input v-model="formData.Ddacctcd" type="text" class="form-control text-center bg-light" style="max-width: 80px;" readonly />
                    <input v-model="formData.Ddacctnm" type="text" class="form-control" placeholder="계정 선택" @keyup.enter="openHelp('DDACCT')" />
                    <button class="btn btn-outline-secondary" @click="openHelp('DDACCT')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
              </tr>
              <tr>
                <th>대변 발생계정</th>
                <td>
                  <div class="input-group input-group-sm" style="width: 350px;">
                    <input v-model="formData.Dcacctcd" type="text" class="form-control text-center bg-light" style="max-width: 80px;" readonly />
                    <input v-model="formData.Dcacctnm" type="text" class="form-control" placeholder="계정 선택" @keyup.enter="openHelp('DCACCT')" />
                    <button class="btn btn-outline-secondary" @click="openHelp('DCACCT')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
              </tr>

              <!-- 🅲 기타 설정 -->
              <tr class="border-top">
                <th>삭제여부</th>
                <td>
                  <div class="form-check form-check-inline mb-0">
                    <input v-model="formData.useyn" class="form-check-input" type="checkbox" id="delYn730" true-value="N" false-value="Y">
                    <label class="form-check-label small" for="delYn730">삭제 시 체크</label>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="card-footer bg-white text-muted small py-2 px-3">
          <i class="bi bi-info-circle me-1"></i> 설정된 계정과목은 매입할인 전표 발행 시 자동으로 적용됩니다.
        </div>
      </div>
    </div>

    <Modal v-model:visible="modalVisible" :modalProps="modalProps" />
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
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
  dacctcd: '', dacctnm: '',
  cacctcd: '', cacctnm: '',
  Ddacctcd: '', Ddacctnm: '',
  Dcacctcd: '', Dcacctnm: '',
  useyn: 'Y'
})

// 2. 기능 구현
async function fetchSetting() {
  try {
    const res = await api.post('/hsba/HSBA_730U_STR', {
      actkind: 'S0',
      cmpycd: authStore.cmpycd
    })
    if (res.data && res.data.length > 0) {
      const data = res.data[0]
      Object.assign(formData, data)
      formData.actkind = 'U0'
      formData.useyn = 'Y'
    } else {
      formData.actkind = 'A0'
    }
  } catch (e) { vAlertError('설정 정보 조회 실패') }
}

async function saveData() {
  if (!formData.dacctcd || !formData.cacctcd) {
    return vAlertError('금액 관련 차변/대변 계정은 필수입니다.')
  }

  if (!confirm('설정 내용을 저장하시겠습니까?')) return

  try {
    const res = await api.post('/hsba/HSBA_730U_STR', {
      ...formData,
      userid: authStore.userid
    })

    if (res.data?.[0]?.ERRYN === 'Y') {
      vAlertError(res.data[0].MSG || '저장 중 오류 발생')
    } else {
      vAlert('성공적으로 저장되었습니다.')
      fetchSetting()
    }
  } catch (e) { vAlertError('서버 통신 오류') }
}

// 3. 도움창 (Modal)
const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function openHelp(type: string) {
  let acctType = '21' // 기본 차변
  let title = '차변계정 선택'

  if (type === 'CACCT' || type === 'DCACCT') {
    acctType = '112'
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
      else if (type === 'DDACCT') { formData.Ddacctcd = data.acctcd; formData.Ddacctnm = data.acctnm }
      else if (type === 'DCACCT') { formData.Dcacctcd = data.acctcd; formData.Dcacctnm = data.acctnm }
    }
  })
  modalVisible.value = true
}

onMounted(() => {
  fetchSetting()
})
</script>

