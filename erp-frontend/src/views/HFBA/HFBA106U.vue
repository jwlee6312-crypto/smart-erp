<!--
	=============================================================
	프로그램명	  : 품목단위별 배부적수관리 (HFBA106U)
	작성일자	    : 2025.02.24
	설명         : 품목별/배부기준별 가중치(적수) 관리 (HPIO210U 표준 패턴 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-list-ol me-2 text-primary" style="font-size: 18px;"></i>
        원가관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        기준정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">품목단위별 배부적수관리 (HFBA106U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="handleSearch">조회</button>
        <button class="btn-erp btn-save" @click="save">저장</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 조회 필터 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
                <col style="width: 10%" /><col style="width: 20%" />
                <col style="width: 10%" /><col style="width: 20%" />
                <col style="width: 10%" /><col style="width: 30%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light required">년 월</th>
                <td>
                  <input v-model="ym_f" type="month" class="form-control form-control-sm" style="max-width: 150px;" @change="handleSearch" />
                </td>
                <th class="text-center bg-light required">배부기준</th>
                <td>
                  <select v-model="searchForm.divstd" class="form-select form-select-sm" @change="handleSearch">
                    <option v-for="opt in divideOptions" :key="opt.code" :value="opt.code">{{ opt.cdnm }}</option>
                  </select>
                </td>
                <th class="text-center bg-light required">제품구분</th>
                <td>
                  <div class="d-flex align-items-center gap-3 ps-2">
                    <div class="form-check form-check-inline m-0">
                      <input v-model="searchForm.jsangbn" class="form-check-input" type="radio" value="200" id="js200" @change="handleSearch">
                      <label class="form-check-label small" for="js200">제품</label>
                    </div>
                    <div class="form-check form-check-inline m-0">
                      <input v-model="searchForm.jsangbn" class="form-check-input" type="radio" value="210" id="js210" @change="handleSearch">
                      <label class="form-check-label small" for="js210">재공품</label>
                    </div>
                    <div v-if="clsInfo.wclsym" class="ms-auto pe-2">
                      <span class="badge bg-danger-subtle text-danger border border-danger-subtle">마감월: {{ clsInfo.wclsym }}</span>
                    </div>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column grid-container-right">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>품목별 배부적수 목록</span>
          <span class="text-muted small">※ 적수 수량을 수정하면 자동으로 변경 상태로 기록됩니다.</span>
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

import { useManualStore } from '@/stores/manualStore'

const authStore = useAuthStore()
const { today } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps } = useCommonHelp()
const manualStore = useManualStore()

// [1] 데이터 모델링
const searchForm = reactive({
  ym: today.substring(0, 7).replace(/-/g, ''),
  divstd: '',
  jsangbn: '200'
})

const ym_f = computed({
  get: () => searchForm.ym ? `${searchForm.ym.substring(0, 4)}-${searchForm.ym.substring(4, 6)}` : '',
  set: (v) => { if (v) searchForm.ym = v.replace(/-/g, '') }
})

const clsInfo = reactive({ wclsym: '' })
const divideOptions = ref<any[]>([])
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
        { title: "상태", field: "_status", width: 60, hozAlign: "center", formatter: (c) => {
            const v = c.getValue();
            if (v === '수정') return '<span class="badge bg-warning text-dark">수정</span>';
            return '';
        }},
        { title: '품목코드', field: 'itemcd', width: 120, hozAlign: 'center' },
        { title: '품목명', field: 'itemnm', minWidth: 200, widthGrow: 1, cssClass: "fw-bold" },
        { title: '단위', field: 'unit', width: 80, hozAlign: 'center' },
        { title: '단위당적수', field: 'unitamt', width: 130, hozAlign: 'right', editor: "number", formatter: 'money', formatterParams: { precision: 0 },
          cellEdited: (cell) => {
            const row = cell.getRow();
            row.update({ _status: '수정' });
          }
        },
        { title: '비고', field: 'bigo', minWidth: 150, editor: 'input',
          cellEdited: (cell) => {
            const row = cell.getRow();
            row.update({ _status: '수정' });
          }
        }
      ]
    })
  }
}

// [3] 비즈니스 로직
const loadInitData = async () => {
  try {
    const [cls, divOpts] = await Promise.all([
      api.post('/hfba/FBA3010U_STR', { actkind: 'S1', yymm: '', yn: 'N', remark: '' }),
      api.post('/hfba/SELECT_DIVIDE_LIST', { cdkd: '1040' })
    ])

    clsInfo.wclsym = cls.data[0]?.wclsym || ''
    divideOptions.value = (divOpts.data || [])
    if (divideOptions.value.length > 0) searchForm.divstd = divideOptions.value[0].code
  } catch (e) { console.error(e) }
}

const handleSearch = async () => {
  if (!searchForm.divstd) return
  try {
    const { data } = await api.post('/hfba/FBA1060U_STR', {
      cmpycd: authStore.cmpycd, actkind: 'S0', ym: searchForm.ym, costcd: '10000', divstd: searchForm.divstd, jsangbn: searchForm.jsangbn,
      itemcd: '', unit: '', itemnm: '', unitamt: '0', bigo: ''
    })
    mainGrid?.setData(data.map((i: any) => ({ ...i, _status: '' })))
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

const save = async () => {
  if (clsInfo.wclsym && clsInfo.wclsym >= searchForm.ym) return vAlertError('원가마감 되었습니다.')
  const details = mainGrid?.getData().filter((r: any) => r._status === '수정') || []
  if (details.length === 0) return vAlertError('저장할 변경 항목이 없습니다.')

  if (!confirm('변경된 정보를 저장하시겠습니까?')) return

  try {
    for (const row of details) {
      await api.post('/hfba/FBA1060U_STR', {
        ...row, actkind: 'A0', cmpycd: authStore.cmpycd, ym: searchForm.ym, costcd: '10000', divstd: searchForm.divstd, jsangbn: searchForm.jsangbn, userid: authStore.userid
      })
    }
    vAlert('저장되었습니다.'); handleSearch();
  } catch (e) { vAlertError('저장 오류') }
}

const initialize = () => {
  resetForm(searchForm)
  searchForm.ym = today.substring(0, 7).replace(/-/g, '')
  if (divideOptions.value.length > 0) searchForm.divstd = divideOptions.value[0].code
  mainGrid?.clearData()
}

onMounted(async () => {
  await loadInitData()
  nextTick(initGrids)
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
.grid-container-right { border-bottom: 3px solid #005a9f !important; }
</style>
