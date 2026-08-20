<!--
	=============================================================
	프로그램명	: 공정별배부작업 (HFMF202U)
	작성일자	: 2025.02.24
	설명        : 작업장별 제조비용 배부 실행 및 취소 (HSOD100U 표준 UI 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-diagram-2 me-2 text-primary" style="font-size: 18px;"></i>
        원가관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        원가결산 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">공정별배부작업 (HFMF202U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="loadInitData">초기화</button>
        <button class="btn-erp btn-save" @click="handleExecute">배부실행</button>
        <button class="btn-erp btn-delete" @click="handleCancel">배부취소</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 처리 대상 및 조건 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center">
          <span class="fw-bold small text-dark"><i class="bi bi-gear-fill me-2 text-primary"></i>배부 처리 조건</span>
        </div>
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
                <col style="width: 180px" /><col />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light">1. 최종배부작업년월</th>
                <td>
                  <div class="px-2">
                    <input :value="dvdYm" class="form-control form-control-sm bg-light text-center fw-bold" readonly style="max-width: 120px;" />
                  </div>
                </td>
              </tr>
              <tr>
                <th class="text-center bg-light required">2. 배부 작업 년월</th>
                <td>
                  <div class="d-flex align-items-center gap-2 px-2">
                    <input v-model="searchForm.ym" type="month" class="form-control form-control-sm" style="max-width: 150px;" />
                    <span class="text-primary small fw-bold">※ 원가마감 전 작업을 완료하세요.</span>
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
                  <input :value="resultInfo.startdt" class="form-control bg-light text-center fw-bold" readonly />
                </div>
                <div class="d-flex align-items-center">
                  <span class="erp-label" style="min-width: 100px;">종료시간</span>
                  <input :value="resultInfo.enddt" class="form-control bg-light text-center fw-bold" readonly />
                </div>
              </div>
            </div>
            <div class="col-md-6">
              <h6 class="fw-bold text-success mb-3"><i class="bi bi-calculator me-2"></i>공정배부 금액</h6>
              <div class="d-flex flex-column gap-3">
                <div class="d-flex align-items-center">
                  <span class="erp-label" style="min-width: 100px;">배부전합계</span>
                  <input :value="Number(resultInfo.bfamt).toLocaleString()" class="form-control bg-light text-end fw-bold" readonly />
                </div>
                <div class="d-flex align-items-center">
                  <span class="erp-label" style="min-width: 100px;">배부후합계</span>
                  <input :value="Number(resultInfo.afamt).toLocaleString()" class="form-control bg-light text-end fw-bold text-success" readonly />
                </div>
              </div>
            </div>
          </div>

          <div class="mt-5 p-3 bg-light rounded border">
            <p class="mb-0 small text-muted">
              <i class="bi bi-info-circle-fill me-2"></i>
              작업장 배부 작업은 공통 제조비용을 각 생산 공정(작업장)별로 배분하는 과정입니다.
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

const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()

const dvdYm = ref('')
const clsWclsym = ref('')
const searchForm = reactive({ ym: new Date().toISOString().substring(0, 7) })
const resultInfo = reactive({ startdt: '-', enddt: '-', bfamt: '0', afamt: '0' })

const loadInitData = async () => {
	try {
		const { data } = await api.post('/the/basicInfo/fmf/callFmf2020uStr', { actkind: 'S0' })
		if (data.data && data.data[0]) {
			dvdYm.value = data.data[0].DVDym || data.data[0].dvdym || ''
			clsWclsym.value = data.data[0].wclsym || ''
		}
	} catch (e) {}
}

const handleExecute = async () => {
	const ym = searchForm.ym.replace('-', '')
	if (clsWclsym.value && clsWclsym.value >= ym) return vAlertError('원가마감 되었습니다. 마감 취소 후 작업하세요.')

	if (!confirm('작업장 배부작업을 하시겠습니까?')) return

	try {
		resultInfo.startdt = new Date().toLocaleTimeString()
		const res = await api.post('/the/basicInfo/fmf/callFmf2020uStr', { ym, actkind: 'A1', val: '' })

		const d = res.data[0]
		if (d.RESULT === 'N' || d.result === 'N') {
			vAlertError(d.MESSAGE || d.message)
		} else {
			Object.assign(resultInfo, {
				bfamt: d.amt_BF || d.bfamt || '0',
				afamt: d.amt_AF || d.afamt || '0',
				enddt: new Date().toLocaleTimeString()
			})
			vAlert(d.MESSAGE || d.message || '배부작업이 완료되었습니다.')
		}
	} catch (e) { vAlertError('작업 중 오류 발생') }
}

const handleCancel = async () => {
	const ym = searchForm.ym.replace('-', '')
	if (clsWclsym.value && clsWclsym.value >= ym) return vAlertError('원가마감 되었습니다.')
	if (!confirm('작업장 배부을 취소 하시겠습니까?')) return

	try {
		const res = await api.post('/the/basicInfo/fmf/callFmf2020uStr', { ym, actkind: 'D0' })
		vAlert(res.data[0].MESSAGE || res.data[0].message || '취소되었습니다.')
		Object.assign(resultInfo, { startdt: '-', enddt: '-', bfamt: '0', afamt: '0' })
	} catch (e) { vAlertError('취소 중 오류 발생') }
}

onMounted(loadInitData)
</script>

<style scoped>
.grid-container-right { border-bottom: 3px solid #198754 !important; }
.erp-label { min-width: 80px; font-weight: 600; color: #495057; font-size: 12px; }
</style>
