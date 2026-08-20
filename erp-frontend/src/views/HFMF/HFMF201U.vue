<!--
	=============================================================
	프로그램명	: 생산실적집계 (HFMF201U)
	작성일자	: 2025.02.24
	설명        : 월별 생산 실적 및 배부 적수 데이터를 원가 모듈로 집계 (HSOD100U 표준 UI 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-stack me-2 text-primary" style="font-size: 18px;"></i>
        원가관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        원가결산 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">생산실적집계 (HFMF201U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-save" @click="handleExecute">집계실행</button>
        <button class="btn-erp btn-delete" @click="handleCancel">집계취소</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 처리 대상 및 조건 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center">
          <span class="fw-bold small text-dark"><i class="bi bi-gear-fill me-2 text-primary"></i>집계 처리 조건</span>
        </div>
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
                <col style="width: 150px" /><col />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light">1. 회계마감 현황</th>
                <td>
                  <div class="d-flex align-items-center gap-3 px-2">
                    <input :value="clsInfo.wclsym" class="form-control form-control-sm bg-light text-center fw-bold" readonly style="max-width: 120px;" />
                    <span class="text-danger small fw-bold"><i class="bi bi-exclamation-triangle me-1"></i>생산시스템 마감 후 처리가 가능합니다.</span>
                  </div>
                </td>
              </tr>
              <tr>
                <th class="text-center bg-light required">2. 집계연월</th>
                <td>
                  <div class="px-2">
                    <input v-model="ym_f" type="month" class="form-control form-control-sm" style="max-width: 150px;" />
                  </div>
                </td>
              </tr>
              <tr>
                <th class="text-center bg-light">3. 집계대상 선택</th>
                <td>
                  <div class="d-flex gap-4 px-2 mt-1">
                    <div class="form-check">
                      <input v-model="targetForm.useyn1" class="form-check-input" type="checkbox" id="chk1" true-value="Y" false-value="N">
                      <label class="form-check-label small fw-bold" for="chk1">생산실적</label>
                    </div>
                    <div class="form-check">
                      <input v-model="targetForm.useyn2" class="form-check-input" type="checkbox" id="chk2" true-value="Y" false-value="N">
                      <label class="form-check-label small fw-bold" for="chk2">품목별 배부적수</label>
                    </div>
                    <div class="form-check">
                      <input v-model="targetForm.useyn3" class="form-check-input" type="checkbox" id="chk3" true-value="Y" false-value="N">
                      <label class="form-check-label small fw-bold" for="chk3">작업장 배부적수</label>
                    </div>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 작업 결과 현황 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column grid-container-right">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center">
          <span class="fw-bold small text-dark"><i class="bi bi-clipboard-data me-2 text-success"></i>작업 처리 결과</span>
        </div>
        <div class="card-body p-4 bg-white overflow-auto">
          <div class="row g-5">
            <div class="col-md-6 border-end">
              <h6 class="fw-bold text-primary mb-3"><i class="bi bi-clock-history me-2"></i>처리 시간 상세</h6>
              <div class="d-flex flex-column gap-3">
                <div class="d-flex align-items-center">
                  <span class="erp-label" style="min-width: 100px;">시작시간</span>
                  <input :value="resultInfo.bfamt" class="form-control bg-light text-center fw-bold" readonly />
                </div>
                <div class="d-flex align-items-center">
                  <span class="erp-label" style="min-width: 100px;">종료시간</span>
                  <input :value="resultInfo.afamt" class="form-control bg-light text-center fw-bold" readonly />
                </div>
              </div>
            </div>
            <div class="col-md-6">
              <h6 class="fw-bold text-success mb-3"><i class="bi bi-graph-up me-2"></i>집계 생산량 변동</h6>
              <div class="d-flex flex-column gap-3">
                <div class="d-flex align-items-center">
                  <span class="erp-label" style="min-width: 100px;">집계전 수량</span>
                  <input :value="Number(resultInfo.qty_bf).toLocaleString()" class="form-control bg-light text-end fw-bold" readonly />
                </div>
                <div class="d-flex align-items-center">
                  <span class="erp-label" style="min-width: 100px;">집계후 수량</span>
                  <input :value="Number(resultInfo.qty_af).toLocaleString()" class="form-control bg-light text-end fw-bold text-success" readonly />
                </div>
              </div>
            </div>
          </div>

          <div class="mt-5 p-3 bg-light rounded border">
            <p class="mb-0 small text-muted">
              <i class="bi bi-info-circle-fill me-2"></i>
              실적 집계는 해당 월의 모든 생산 공정 데이터를 원가 계산용 데이터로 변환합니다. 집계 전 반드시 생산 마감 상태를 확인하십시오.
            </p>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { getDate } from '@/composables/useDate'
import AppAlert from '@/components/AppAlert.vue'

const authStore = useAuthStore()
const { today } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

// [1] 데이터 모델링
const searchForm = reactive({ ym: today.substring(0, 7).replace(/-/g, '') })
const ym_f = computed({
  get: () => searchForm.ym ? `${searchForm.ym.substring(0, 4)}-${searchForm.ym.substring(4, 6)}` : '',
  set: (v) => { if (v) searchForm.ym = v.replace(/-/g, '') }
})

const clsInfo = reactive({ wclsym: '' })
const targetForm = reactive({ useyn1: 'Y', useyn2: 'Y', useyn3: 'Y' })
const resultInfo = reactive({ bfamt: '-', afamt: '-', qty_bf: '0', qty_af: '0' })

// [2] 비즈니스 로직
const loadClsInfo = async () => {
  try {
    const res = await api.get('/hp00/HP00_000S_STR', { params: { gubun: 'CL', cmpycd: authStore.cmpycd } })
    if (res.data?.length > 0) {
      clsInfo.wclsym = res.data[0].wclsym || ''
    }
  } catch (e) { console.error(e) }
}

const handleExecute = async () => {
  const ym = searchForm.ym
  if (clsInfo.wclsym && clsInfo.wclsym >= ym) return vAlertError('원가마감 되었습니다. 마감 취소 후 작업하세요.')
  if (targetForm.useyn1 === 'N' && targetForm.useyn2 === 'N' && targetForm.useyn3 === 'N') {
    return vAlertError('적어도 하나 이상의 집계대상을 선택하세요.')
  }

  if (!confirm('생산실적집계 작업을 실행하시겠습니까?')) return

  try {
    const checkRes = await api.post('/hfmf/FMF2010U_STR', {
      cmpycd: authStore.cmpycd, actkind: 'C0', ym: ym, colgbn: '2000', userid: authStore.userid
    })

    const cData = checkRes.data[0] || {};
    if ((cData.status || cData.STATUS) !== 'Y') {
      return vAlertError(cData.message || cData.MESSAGE || '사전 체크 실패');
    }

    if (targetForm.useyn1 === 'Y') {
      const res = await api.post('/hfmf/FMF2010U_STR', {
        cmpycd: authStore.cmpycd, actkind: 'A0', ym: ym, colgbn: '2000', userid: authStore.userid
      })
      const v = Object.values(res.data[0]);
      Object.assign(resultInfo, { bfamt: v[2], afamt: v[3], qty_bf: v[4], qty_af: v[5] });
    }
    if (targetForm.useyn2 === 'Y') await api.post('/hfmf/FMF2010U_STR', { cmpycd: authStore.cmpycd, actkind: 'A0', ym: ym, colgbn: '9000', userid: authStore.userid });
    if (targetForm.useyn3 === 'Y') await api.post('/hfmf/FMF2010U_STR', { cmpycd: authStore.cmpycd, actkind: 'A0', ym: ym, colgbn: '8000', userid: authStore.userid });

    vAlert('집계 작업이 완료되었습니다.')
  } catch (e) { vAlertError('작업 중 오류 발생') }
}

const handleCancel = async () => {
  if (clsInfo.wclsym && clsInfo.wclsym >= searchForm.ym) return vAlertError('원가마감 되었습니다.')
  if (!confirm('집계 작업을 취소하시겠습니까?')) return

  try {
    const params = { cmpycd: authStore.cmpycd, actkind: 'D0', ym: searchForm.ym, userid: authStore.userid }
    if (targetForm.useyn1 === 'Y') await api.post('/hfmf/FMF2010U_STR', { ...params, colgbn: '2000' })
    if (targetForm.useyn2 === 'Y') await api.post('/hfmf/FMF2010U_STR', { ...params, colgbn: '9000' })
    if (targetForm.useyn3 === 'Y') await api.post('/hfmf/FMF2010U_STR', { ...params, colgbn: '8000' })

    vAlert('취소되었습니다.')
    Object.assign(resultInfo, { bfamt: '-', afamt: '-', qty_bf: '0', qty_af: '0' })
  } catch (e) { vAlertError('취소 중 오류 발생') }
}

const initialize = () => {
  searchForm.ym = today.substring(0, 7).replace(/-/g, '')
  Object.assign(resultInfo, { bfamt: '-', afamt: '-', qty_bf: '0', qty_af: '0' })
  loadClsInfo()
}

onMounted(() => { loadClsInfo() })
</script>

<style scoped>
.grid-container-right { border-bottom: 3px solid #198754 !important; }
.erp-label { min-width: 80px; font-weight: 600; color: #495057; font-size: 12px; }
</style>
