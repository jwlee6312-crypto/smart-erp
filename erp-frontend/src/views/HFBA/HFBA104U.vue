<!--
	=============================================================
	프로그램명	: 부문/작업장 배부기준 등록 (HFBA104U)
	작성일자	: 2025.02.24
	설명        : 부문별/작업장별 원가 배부 기준 설정 및 관리 (HPIO210U 표준 패턴 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-diagram-3-fill me-2 text-primary" style="font-size: 18px;"></i>
        원가관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        기준정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">부문/작업장 배부기준 등록 (HFBA104U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="handleReset">초기화</button>
        <button class="btn-erp btn-search" @click="handleSearch">조회</button>
        <button class="btn-erp btn-save" @click="save">저장</button>
        <button class="btn-erp btn-delete" @click="deleteData" :disabled="detailForm.mode !== 'U'">삭제</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 조회 필터 영역 (슬림형) -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
                <col style="width: 10%" /><col style="width: 25%" />
                <col style="width: 10%" /><col style="width: 55%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light required">마감년월</th>
                <td>
                  <input v-model="ym_f" type="month" class="form-control form-control-sm" style="max-width: 150px;" @change="handleSearch" />
                </td>
                <th class="text-center bg-light">마감상태</th>
                <td>
                  <div class="d-flex align-items-center gap-3 ps-2">
                    <span v-if="clsInfo.wclsym" class="badge bg-danger-subtle text-danger border border-danger-subtle">마감월: {{ clsInfo.wclsym }}</span>
                    <span v-else class="text-muted small">미마감</span>
                    <span class="text-muted small ms-auto pe-3"><i class="bi bi-info-circle me-1"></i> 년월별 계정과목의 배부 기준을 설정합니다.</span>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [중단] 마스터 상세 입력 폼 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-header py-1 px-3 border-bottom d-flex align-items-center" style="background-color: #f0f7ff !important;">
          <span class="fw-bold small text-primary"><i class="bi bi-pencil-square me-1"></i>배부 기준 설정</span>
        </div>
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense w-100">
            <colgroup>
              <col style="width: 110px;" /><col />
              <col style="width: 110px;" /><col />
              <col style="width: 110px;" /><col />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light required">계정과목</th>
                <td>
                  <select v-model="detailForm.acct" class="form-select" @change="onAcctChange">
                    <option v-for="opt in acctOptions" :key="opt.acct" :value="opt.acct">{{ opt.acctnm }}</option>
                  </select>
                </td>
                <th class="text-center bg-light required">공정배부</th>
                <td>
                  <select v-model="detailForm.divide1" class="form-select">
                    <option v-for="opt in divide1Options" :key="opt.code" :value="opt.code">{{ opt.cdnm }}</option>
                  </select>
                </td>
                <th class="text-center bg-light required">품목배부</th>
                <td>
                  <select v-model="detailForm.divide2" class="form-select">
                    <option v-for="opt in divide2Options" :key="opt.code" :value="opt.code">{{ opt.cdnm }}</option>
                  </select>
                </td>
              </tr>
              <tr>
                <th class="text-center bg-light">비 고</th>
                <td colspan="5">
                  <input v-model="detailForm.bigo" class="form-control" maxlength="50" placeholder="특이사항 입력" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column grid-container-right">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>배부기준 목록</span>
          <span class="text-muted small">※ 항목 클릭 시 상세 정보가 상단 폼에 자동 입력됩니다.</span>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="mainGridRef" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick, computed } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { useCommonHelp } from '@/composables/useCommonHelp'
import { getDate } from '@/composables/useDate'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'

const authStore = useAuthStore()
const { today } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps } = useCommonHelp()

// [1] 데이터 모델링
const searchForm = reactive({ ym: today.substring(0, 7).replace(/-/g, '') })
const ym_f = computed({
  get: () => searchForm.ym ? `${searchForm.ym.substring(0, 4)}-${searchForm.ym.substring(4, 6)}` : '',
  set: (v) => { if (v) searchForm.ym = v.replace(/-/g, '') }
})

const clsInfo = reactive({ wclsym: '' })
const acctOptions = ref<any[]>([])
const divide1Options = ref<any[]>([])
const divide2Options = ref<any[]>([])

const detailForm = reactive<any>({
  acct: '', acctnm: '', divide1: '', divide2: '', bigo: '', mode: 'N'
})

const mainGridRef = ref<HTMLElement | null>(null)
let mainGrid: Tabulator | null = null

// [2] 그리드 초기화
const initGrids = () => {
  if (mainGridRef.value) {
    mainGrid = new Tabulator(mainGridRef.value, {
      layout: 'fitColumns',
      height: '100%',
      placeholder: "데이터 없음",
      columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
      columns: [
        { title: "No", formatter: "rownum", width: 40, hozAlign: "center" },
        { title: '계정코드', field: 'acct', width: 100, hozAlign: 'center', cssClass: 'fw-bold text-primary' },
        { title: '계정과목명', field: 'acctnm', minWidth: 200, widthGrow: 1.5 },
        { title: '공정배부기준', field: 'divide_nm1', widthGrow: 1 },
        { title: '품목배부기준', field: 'divide_nm2', widthGrow: 1 },
        { title: '비고', field: 'bigo', minWidth: 150 }
      ]
    })
    mainGrid.on('rowClick', (e, row) => fetchDetail(row.getData()))
  }
}

// [3] 비즈니스 로직
const loadInitData = async () => {
  try {
    const [cls, accts, d1, d2] = await Promise.all([
      api.post('/hfba/FBA3010U_STR', { cmpycd: authStore.cmpycd, actkind: 'S1', yymm: '', yn: 'N', remark: '' }),
      api.post('/hfba/SELECT_ACCT_LIST', { cmpycd: authStore.cmpycd }),
      api.post('/hfba/SELECT_DIVIDE_LIST', { cmpycd: authStore.cmpycd, cdkd: '1030' }),
      api.post('/hfba/SELECT_DIVIDE_LIST', { cmpycd: authStore.cmpycd, cdkd: '1040' })
    ])
    clsInfo.wclsym = cls.data[0]?.wclsym || ''
    acctOptions.value = accts.data || []
    divide1Options.value = d1.data || []
    divide2Options.value = d2.data || []

    if (acctOptions.value.length > 0) {
      detailForm.acct = acctOptions.value[0].acct
      detailForm.acctnm = acctOptions.value[0].acctnm
    }
  } catch (e) { vAlertError('기초 데이터 로드 실패') }
}

const handleSearch = async () => {
  try {
    const { data } = await api.post('/hfba/FBA1040U_STR', {
      cmpycd: authStore.cmpycd, actkind: 'S0', ym: searchForm.ym, acct: '', divide1: '', divide2: '', userid: authStore.userid
    })
    mainGrid?.setData(data)
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

const fetchDetail = (row: any) => {
  Object.assign(detailForm, { ...row, mode: 'U' });
}

const onAcctChange = () => {
  const selected = acctOptions.value.find(o => o.acct === detailForm.acct)
  if (selected) detailForm.acctnm = selected.acctnm
}

const save = async () => {
  if (clsInfo.wclsym && clsInfo.wclsym >= searchForm.ym) return vAlertError('원가마감 되었습니다.')
  if (!detailForm.acct) return vAlertError('계정과목을 선택하세요.')

  if (!confirm('저장하시겠습니까?')) return

  try {
    const actkind = detailForm.mode === 'U' ? 'U0' : 'A0'
    await api.post('/hfba/FBA1040U_STR', {
      ...detailForm, actkind, cmpycd: authStore.cmpycd, ym: searchForm.ym, userid: authStore.userid
    })
    vAlert('처리되었습니다.')
    handleSearch()
    handleReset()
  } catch (e) { vAlertError('저장 실패') }
}

const deleteData = async () => {
  if (!confirm('정말 삭제하시겠습니까?')) return
  try {
    await api.post('/hfba/FBA1040U_STR', {
      actkind: 'D0', cmpycd: authStore.cmpycd, ym: searchForm.ym, acct: detailForm.acct, userid: authStore.userid
    })
    vAlert('삭제되었습니다.')
    handleSearch()
    handleReset()
  } catch (e) { vAlertError('삭제 실패') }
}

const handleReset = () => {
  resetForm(detailForm)
  detailForm.mode = 'N'
  if (acctOptions.value.length > 0) {
    detailForm.acct = acctOptions.value[0].acct
    detailForm.acctnm = acctOptions.value[0].acctnm
  }
}

const initialize = () => {
  searchForm.ym = today.substring(0, 7).replace(/-/g, '')
  handleReset()
  handleSearch()
}

onMounted(async () => {
  await loadInitData()
  nextTick(initGrids)
  handleSearch()
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
.grid-container-right { border-bottom: 3px solid #005a9f !important; }
</style>
