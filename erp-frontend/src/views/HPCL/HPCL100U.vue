<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-calendar-check-fill me-2 text-primary" style="font-size: 18px;"></i>
        생산정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        <span class="text-primary fw-bolder">월마감 작업 (HPCL100U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-2">
        <button class="btn-erp btn-save" @click="handleProcess">
          <i class="bi bi-check2-circle"></i> 실행
        </button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-auto p-4 d-flex justify-content-center align-items-start">
      <div class="card border-0 shadow-lg" style="width: 600px; border-radius: 12px;">
        <div class="card-header bg-primary text-white py-3 px-4" style="border-radius: 12px 12px 0 0;">
          <h5 class="card-title mb-0 fw-bold"><i class="bi bi-lock-fill me-2"></i> 생산 월마감 프로세스</h5>
        </div>
        <div class="card-body p-4 bg-light">
          <!-- 마감 연월 및 작업 구분 -->
          <div class="row g-4 mb-4">
            <div class="col-md-6">
              <label class="form-label fw-bold text-secondary small mb-2">마감 연월</label>
              <div class="input-group">
                <select v-model="closeData.yy" class="form-select border-primary shadow-sm">
                  <option v-for="year in yearOptions" :key="year" :value="year">{{ year }}년</option>
                </select>
                <select v-model="closeData.mm" class="form-select border-primary shadow-sm">
                  <option v-for="month in monthOptions" :key="month" :value="month">{{ month }}월</option>
                </select>
              </div>
            </div>
            <div class="col-md-6">
              <label class="form-label fw-bold text-secondary small mb-2">작업 구분</label>
              <select v-model="closeData.wkgbn" class="form-select border-primary shadow-sm fw-bold" :class="closeData.wkgbn === 'Y' ? 'text-primary' : 'text-danger'">
                <option value="Y">마감 실행 (Lock)</option>
                <option value="N">마감 취소 (Unlock)</option>
              </select>
            </div>
          </div>

          <!-- 안내 사항 -->
          <div class="alert alert-warning border-0 shadow-sm mb-4" style="border-left: 5px solid #ffc107 !important;">
            <div class="d-flex">
              <i class="bi bi-exclamation-triangle-fill fs-4 me-3 text-warning"></i>
              <div>
                <p class="mb-1 fw-bold">주의사항</p>
                <ul class="mb-0 small ps-3">
                  <li v-if="closeData.wkgbn === 'Y'">월마감을 진행하면 해당 월의 자재 입고 및 생산 실적 자료를 <strong>수정하거나 추가할 수 없습니다.</strong></li>
                  <li v-else>마감을 취소하면 해당 월의 생산 데이터를 <strong>다시 편집할 수 있는 상태</strong>가 됩니다.</li>
                  <li>원가 마감이 진행된 경우, 먼저 원가 마감을 취소해야 생산 마감 작업이 가능합니다.</li>
                </ul>
              </div>
            </div>
          </div>

          <!-- 회계 결산 안내 -->
          <div class="p-3 bg-white border rounded shadow-sm">
            <h6 class="fw-bold text-dark border-bottom pb-2 mb-3 small"><i class="bi bi-journal-text me-1 text-info"></i> 회계 결산 처리 안내</h6>
            <div class="row row-cols-1 row-cols-md-2 g-3 small">
              <div class="col">
                <div class="fw-bold text-primary mb-1">1. 재료비 대체</div>
                <div class="ps-2 text-muted">재료비 / 원재료, 부재료</div>
              </div>
              <div class="col">
                <div class="fw-bold text-primary mb-1">2. 재공품 대체</div>
                <div class="ps-2 text-muted">재공품 / 재료비, 노무비, 경비</div>
              </div>
              <div class="col">
                <div class="fw-bold text-primary mb-1">3. 제조계정 대체</div>
                <div class="ps-2 text-muted">제조원가 / 재공품</div>
              </div>
              <div class="col">
                <div class="fw-bold text-primary mb-1">4. 제품/매출원가</div>
                <div class="ps-2 text-muted">제품 / 제조원가, 매출원가 / 제품</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import AppAlert from '@/components/AppAlert.vue'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()

const closeData = reactive({ yy: '', mm: '', wkgbn: 'Y' })
const closingInfo = reactive({ clsymd: '', sclsym: '', PCLSym: '', wclsym: '' })
const yearOptions = ref<string[]>([])
const monthOptions = ['01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12']

const generateYearOptions = () => {
    const currentYear = new Date().getFullYear()
    for (let i = 0; i < 5; i++) yearOptions.value.push(String(currentYear - i))
}

const fetchClosingStatus = async () => {
  try {
    const res = await api.get('/hp00/HP00_000S_STR', { params: { gubun: 'CL', cmpycd: authStore.cmpycd } })
    if (res.data?.length) {
      closingInfo.clsymd = String(Object.values(res.data[0])[0]).trim()
      closingInfo.sclsym = String(Object.values(res.data[0])[1]).trim()
      closingInfo.PCLSym = String(Object.values(res.data[0])[2]).trim()
      closingInfo.wclsym = String(Object.values(res.data[0])[3]).trim()
      if (closingInfo.PCLSym.length === 6) {
          closeData.yy = closingInfo.PCLSym.substring(0, 4)
          closeData.mm = closingInfo.PCLSym.substring(4, 6)
      }
    }
  } catch (e) {}
}

const handleProcess = async () => {
    const ym = closeData.yy + closeData.mm
    if (closingInfo.wclsym >= ym) return vAlertError(`원가마감(${closingInfo.wclsym}) 되었습니다. 원가마감 취소 후 작업 바랍니다.`)
    const confirmMsg = closeData.wkgbn === 'Y' ? `${closeData.yy}년 ${closeData.mm}월 마감 작업을 하시겠습니까?` : `${closeData.yy}년 ${closeData.mm}월 마감 취소 작업을 하시겠습니까?`
    if (!confirm(confirmMsg)) return

    try {
        const actkind = closeData.wkgbn === 'Y' ? 'A0' : 'D0'
        await api.post('/hpcl/HPCL_100U_STR', { actkind: actkind, cmpycd: authStore.cmpycd, ym: ym, userid: authStore.userid })
        vAlert('정상적으로 작업이 완료되었습니다.')
        fetchClosingStatus()
    } catch (e) { vAlertError('처리 중 오류 발생') }
}

onMounted(() => { generateYearOptions(); fetchClosingStatus() })
</script>
