<!--
	=============================================================
	프로그램명	: 수불부 원가 생성작업 (HFMF301U)
	작성일자	: 2025.02.24
	설명        : 제품 및 재공품 수불부에 원가 데이터를 생성 및 반영 (HSOD100U 표준 UI 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-arrow-repeat me-2 text-primary" style="font-size: 18px;"></i>
        원가관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        원가결산 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">수불부 원가 생성작업 (HFMF301U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="loadClsInfo">초기화</button>
        <button class="btn-erp btn-save" @click="handleExecute">생성실행</button>
        <button class="btn-erp btn-print" @click="openManual">도움말</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 처리 대상 및 조건 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center">
          <span class="fw-bold small text-dark"><i class="bi bi-gear-fill me-2 text-primary"></i>작업 처리 조건</span>
        </div>
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
                <col style="width: 180px" /><col />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light">1. 최종마감작업년월</th>
                <td>
                  <div class="px-2">
                    <input :value="clsInfo.wclsym" class="form-control form-control-sm bg-light text-center fw-bold" readonly style="max-width: 120px;" />
                  </div>
                </td>
              </tr>
              <tr>
                <th class="text-center bg-light required">2. 작업 년 월</th>
                <td>
                  <div class="px-2">
                    <input v-model="searchForm.ym" type="month" class="form-control form-control-sm" style="max-width: 150px;" />
                  </div>
                </td>
              </tr>
              <tr>
                <th class="text-center bg-light">3. 생성 대상 선택</th>
                <td>
                  <div class="d-flex gap-4 px-2 mt-1">
                    <div class="form-check">
                      <input v-model="targetForm.useyn1" class="form-check-input" type="checkbox" id="chk1" true-value="Y" false-value="N">
                      <label class="form-check-label small fw-bold" for="chk1">재공품수불부</label>
                    </div>
                    <div class="form-check">
                      <input v-model="targetForm.useyn2" class="form-check-input" type="checkbox" id="chk2" true-value="Y" false-value="N">
                      <label class="form-check-label small fw-bold" for="chk2">제품수불부</label>
                    </div>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 작업 결과 및 안내 -->
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
                  <input :value="resultInfo.startdt" class="form-control bg-light text-center fw-bold" readonly />
                </div>
                <div class="d-flex align-items-center">
                  <span class="erp-label" style="min-width: 100px;">종료시간</span>
                  <input :value="resultInfo.enddt" class="form-control bg-light text-center fw-bold" readonly />
                </div>
              </div>
            </div>
            <div class="col-md-6">
                <div class="alert alert-warning border-0 shadow-sm small py-3 px-4 h-100 d-flex flex-column justify-content-center">
                    <div class="d-flex align-items-center mb-2 fw-bold text-dark">
                        <i class="bi bi-exclamation-triangle-fill me-2 fs-5 text-warning"></i>
                        주의사항
                    </div>
                    <p class="mb-0 text-muted">
                        품목별 제조원가를 제품 및 재공품의 수불부 입고금액으로 적용하여 출고 및 재고 금액을 일괄 생성합니다.
                        원가 작업이 완료된 후 실행하십시오.
                    </p>
                </div>
            </div>
          </div>

          <div class="mt-5 p-3 bg-light rounded border">
            <p class="mb-0 small text-muted">
              <i class="bi bi-info-circle-fill me-2"></i>
              마감된 연월의 경우 수불부 원가 재생성이 제한될 수 있습니다.
            </p>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import AppAlert from '@/components/AppAlert.vue'
import { useAuthStore } from '@/stores/authStore'

import { useManualStore } from '@/stores/manualStore'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const manualStore = useManualStore()

const clsInfo = reactive({ wclsym: '' })
const searchForm = reactive({ ym: new Date().toISOString().substring(0, 7) })
const targetForm = reactive({ useyn1: 'Y', useyn2: 'Y' })
const resultInfo = reactive({ startdt: '-', enddt: '-' })

const loadClsInfo = async () => {
	try {
		const res = await api.get('/hp00/HP00_000S_STR', {
			params: { gubun: 'CL', cmpycd: authStore.cmpycd }
		})
        if (res.data?.length > 0) {
            clsInfo.wclsym = res.data[0].wclsym || res.data[0].codecd || ''
        }
	} catch (e) { console.error(e) }
}

const handleExecute = async () => {
	const ym = searchForm.ym.replace('-', '')
	if (clsInfo.wclsym && clsInfo.wclsym >= ym) return vAlertError('원가마감된 자료는 수불부 원가 재성성이 불가합니다.')
	if (targetForm.useyn1 === 'N' && targetForm.useyn2 === 'N') return vAlertError('생성 대상을 선택하세요.')

	if (!confirm('수불부 원가 생성작업을 하시겠습니까?')) return

	try {
		resultInfo.startdt = new Date().toLocaleTimeString()
        resultInfo.enddt = '-'

        let jagbn = '00'
        if (targetForm.useyn1 === 'Y' && targetForm.useyn2 === 'Y') jagbn = '11'
        else if (targetForm.useyn1 === 'Y') jagbn = '10'
        else if (targetForm.useyn2 === 'Y') jagbn = '01'

		await api.post('/hfmf/FMF3010U_STR', {
            cmpycd: String(authStore.cmpycd), actkind: 'A0', ym: String(ym), jagbn: String(jagbn), userid: String(authStore.userid)
		})

		vAlert('원가 생성 작업이 완료되었습니다.')
		resultInfo.enddt = new Date().toLocaleTimeString()
	} catch (e) { vAlertError('작업 중 오류 발생') }
}

const openManual = () => {
  manualStore.open('HFMF301U')
}

onMounted(loadClsInfo)
</script>

<style scoped>
.grid-container-right { border-bottom: 3px solid #198754 !important; }
.erp-label { min-width: 80px; font-weight: 600; color: #495057; font-size: 12px; }
</style>
