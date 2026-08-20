<!--
	=============================================================
	프로그램명	: 공정별제조비용 배부현황 (HFMF206S)
	작성일자	: 2025.02.24
	설명        : 계정과목별 공정 배부 현황 출력 (HSOD100U 유사 UI 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 섹션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-file-earmark-bar-graph me-2 text-primary" style="font-size: 18px;"></i>
        원가관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        현황조회 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">공정별제조비용 배부현황 (HFMF206S)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-search" @click="handleSearch">조회</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 조회 필터 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
                <col style="width: 10%" /><col style="width: 40%" />
                <col style="width: 10%" /><col style="width: 40%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light required">조회년월</th>
                <td class="d-flex align-items-center gap-1 border-0" style="height: 32px;">
                  <select v-model="searchForm.yyyy" class="form-select form-select-sm" style="width: 100px;">
                    <option v-for="year in yearOptions" :key="year" :value="year">{{ year }}년</option>
                  </select>
                  <select v-model="searchForm.mm" class="form-select form-select-sm" style="width: 80px;">
                    <option v-for="month in monthOptions" :key="month" :value="month">{{ month }}월</option>
                  </select>
                </td>
                <th class="text-center bg-light">마감상태</th>
                <td>
                  <span v-if="clsInfo.wclsym" class="badge bg-danger-subtle text-danger border border-danger-subtle">마감일 {{ clsInfo.wclsym }}</span>
                  <span v-else class="text-muted small">미마감</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 보고서 영역 (HTML Table) -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white">
        <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark">
          <i class="bi bi-table me-2 text-primary"></i>공정별 제조비용 배부 상세
        </div>
        <div class="card-body p-0 overflow-auto flex-grow-1">
          <table class="report-table">
            <thead class="sticky-top bg-light">
              <tr>
                <th style="width: 15%;">계정과목</th>
                <th v-for="(h, idx) in processHeaders" :key="idx" style="width: 7.7%;">{{ h }}</th>
                <th v-for="n in (10 - processHeaders.length)" :key="'h-empty-'+n" style="width: 7.7%;"></th>
                <th style="width: 8%;">합계</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="reportData.length === 0">
                <td colspan="12" class="text-center py-5 text-muted">조회된 데이터가 없습니다.</td>
              </tr>
              <tr v-for="(row, idx) in reportData" :key="idx" class="data-row">
                <td class="text-start ps-3 fw-bold" :class="{'bg-light-yellow': row.lev === '2'}" v-html="formatAcctNm(row)"></td>
                <td v-for="(amt, aIdx) in row.processamts" :key="aIdx" class="text-end pe-2">
                  {{ formatNumber(amt) }}
                </td>
                <td v-for="n in (10 - row.processamts.length)" :key="'empty-'+n" class="text-end pe-2"></td>
                <td class="text-end pe-2 fw-bold text-primary bg-primary-subtle">
                  {{ formatNumber(row.totalamt) }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useAlerts } from '@/composables/useAlerts'
import AppAlert from '@/components/AppAlert.vue'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlertError } = useAlerts()

const currentYear = new Date().getFullYear()
const yearOptions = Array.from({ length: 6 }, (_, i) => String(currentYear - i))
const monthOptions = Array.from({ length: 12 }, (_, i) => String(i + 1).padStart(2, '0'))

const searchForm = reactive({
	yyyy: String(currentYear),
	mm: String(new Date().getMonth() + 1).padStart(2, '0')
})

const clsInfo = reactive({ wclsym: '' })
const reportData = ref<any[]>([])
const processHeaders = ref<string[]>([])

const formatNumber = (val: any) => {
	if (!val || isNaN(Number(val)) || Number(val) === 0) return ''
	return Number(val).toLocaleString()
}

const formatAcctNm = (row: any) => {
	const acct = row.acct || ''
	const lev = String(row.lev || '')
	let nm = row.acctnm || ''
	if (lev === '2' && acct === '412-0000') return `[소계] ${nm}`
	if (lev === '2' && acct === '413-0000') return `[합계] ${nm}`
	if (lev === '5' && acct === '414-0000') return nm
	return `&nbsp;&nbsp;&nbsp;&nbsp;${nm}`
}

const handleSearch = async () => {
	try {
		const ym = `${searchForm.yyyy}${searchForm.mm}`
		const { data } = await api.post('/hfmf/FMF2060R_STR', {
			cmpycd: authStore.cmpycd,
			actkind: 'S0',
			ym: ym,
			costcd: '10000'
		})

		const grouped: any[] = []
		let currentAcct = ''
		let currentRow: any = null
		const headerSet: Set<string> = new Set()

		data.forEach((item: any) => {
			if (item.prognm) headerSet.add(item.prognm)
			if (item.acct !== currentAcct) {
				if (currentRow) grouped.push(currentRow)
				currentAcct = item.acct
				currentRow = {
					acct: item.acct, acctnm: item.acctnm, lev: String(item.lev || ''),
					processamts: [], totalamt: 0
				}
			}
			const amt = Number(item.totamt || 0)
			currentRow.processamts.push(amt)
			currentRow.totalamt += amt
		})
		if (currentRow) grouped.push(currentRow)

		processHeaders.value = Array.from(headerSet).slice(0, 10)
		reportData.value = grouped
	} catch (e) {
		vAlertError('조회 중 오류가 발생했습니다.')
	}
}

onMounted(async () => {
	try {
		const { data } = await api.get('/hp00/HP00_000S_STR', {
			params: { gubun: 'CL', cmpycd: authStore.cmpycd }
		})
		if (data?.length > 0) clsInfo.wclsym = data[0].wclsym || ''
	} catch (e) {}
	handleSearch()
})
</script>

<style scoped>
.report-table { width: 100%; border-collapse: collapse; font-size: 12px; }
.report-table th, .report-table td { border: 1px solid #dee2e6; padding: 8px 4px; height: 35px; vertical-align: middle; }
.report-table th { background-color: #f8f9fa; font-weight: bold; text-align: center; }
.data-row:hover { background-color: #f1f8ff; }
.bg-light-yellow { background-color: #fffde7 !important; }
</style>
