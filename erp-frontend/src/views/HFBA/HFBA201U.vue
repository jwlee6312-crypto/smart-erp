<!--
	=============================================================
	프로그램명	  : 제조원가 계정관리 (HFBA201U)
	작성일자	    : 2025.02.24
	설명         : 제조원가 계산에 사용되는 계정과목 등록 및 관리 (HPIO210U 표준 패턴 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-journal-check me-2 text-primary" style="font-size: 18px;"></i>
        원가관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        기준정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">제조원가 계정관리 (HFBA201U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="handleSearch">조회</button>
        <button class="btn-erp btn-save" @click="save">저장</button>
        <button class="btn-erp btn-delete" @click="deleteData" :disabled="formData.mode === 'N'">삭제</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 입력 폼 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
                <col style="width: 10%" /><col style="width: 23%" />
                <col style="width: 10%" /><col style="width: 23%" />
                <col style="width: 10%" /><col style="width: 24%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light required">계정코드</th>
                <td>
                  <input v-model="formData.acct" class="form-control fw-bold text-primary" maxlength="8" :readonly="formData.mode === 'U'" placeholder="코드 입력" />
                </td>
                <th class="text-center bg-light required">계정과목명</th>
                <td>
                  <input v-model="formData.acctnm" class="form-control" maxlength="50" placeholder="과목명 입력" />
                </td>
                <th class="text-center bg-light">외주가공</th>
                <td>
                  <div class="form-check form-switch d-flex justify-content-center mt-1">
                    <input v-model="formData.acntgbn" class="form-check-input" type="checkbox" true-value="1010" false-value="1000">
                    <label class="form-check-label small ms-2">{{ formData.acntgbn === '1010' ? '대상' : '일반' }}</label>
                  </div>
                </td>
              </tr>
              <tr>
                <th class="text-center bg-light">비 고</th>
                <td>
                  <input v-model="formData.bigo" class="form-control" maxlength="50" />
                </td>
                <th class="text-center bg-light">사용여부</th>
                <td>
                  <div class="form-check form-switch d-flex justify-content-center mt-1">
                    <input v-model="formData.useyn" class="form-check-input" type="checkbox" true-value="Y" false-value="N">
                    <label class="form-check-label small ms-2">{{ formData.useyn === 'Y' ? '사용' : '미사용' }}</label>
                  </div>
                </td>
                <td colspan="2" class="bg-white"></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column grid-container-right">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>제조원가 계정 목록</span>
          <span class="text-muted small">※ 목록 클릭 시 수정 모드로 전환</span>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="tableRef" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { useCommonHelp } from '@/composables/useCommonHelp'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps } = useCommonHelp()

// [1] 데이터 모델링 (소문자 원칙)
const formData = reactive<any>({
  acct: '', acct_o: '', acctnm: '', bigo: '', acntgbn: '1000', useyn: 'Y', mode: 'N'
})

const tableRef = ref<HTMLDivElement | null>(null)
let grid: Tabulator | null = null

// [2] 그리드 초기화
const initGrids = () => {
  grid = new Tabulator(tableRef.value!, {
    layout: "fitColumns", height: "100%", placeholder: "데이터 없음", selectable: 1,
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "No", formatter: "rownum", width: 40, hozAlign: "center" },
      { title: "계정코드", field: "acct", width: 120, hozAlign: "center", cssClass: "fw-bold text-primary" },
      { title: "계정과목명", field: "acctnm", minWidth: 250, widthGrow: 1, cssClass: "fw-bold" },
      { title: "외주가공", field: "acntgbn", width: 100, hozAlign: "center", formatter: (c) => c.getValue() === '1010' ? '대상' : '일반' },
        { title: "사용", field: "useyn", width: 80, hozAlign: "center",
          formatter: (cell) => {
            const val = String(cell.getValue() || '').trim().toUpperCase();
            return val === 'Y' ? '<b class="text-primary">사용</b>' : '';
          }
        },
      { title: "비고", field: "bigo", minWidth: 200 }
    ],
  });
  grid.on("rowClick", (e, row) => fetchDetail(row.getData()));
}

// [3] 비즈니스 로직
const handleSearch = async () => {
  try {
    const { data } = await api.post('/hfba/FBA2010U_STR', {
      actkind: 'S0', cmpycd: authStore.cmpycd, acct: '', acct_o: '', acctnm: '', bigo: '', useyn: 'Y', acntgbn: '', userid: authStore.userid
    })
    grid?.setData(data)
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

function fetchDetail(row: any) {
  Object.assign(formData, { ...row, acct_o: row.acct, mode: 'U' });
}

const save = async () => {
  if (!formData.acct || !formData.acctnm) return vAlertError('계정코드와 과목명은 필수입니다.');
  if (!confirm('저장하시겠습니까?')) return

  try {
    const actkind = formData.mode === 'U' ? 'U0' : 'A0'
    await api.post('/hfba/FBA2010U_STR', {
      ...formData, actkind: actkind, cmpycd: authStore.cmpycd, userid: authStore.userid
    })
    vAlert('처리되었습니다.');
    handleSearch();
    initialize();
  } catch (e) { vAlertError('저장 실패') }
}

const deleteData = async () => {
  if (formData.mode === 'N') return
  if (!confirm('정말 삭제하시겠습니까?')) return
  try {
    await api.post('/hfba/FBA2010U_STR', {
      actkind: 'D0', cmpycd: authStore.cmpycd, acct: formData.acct, acct_o: formData.acct_o, userid: authStore.userid
    })
    vAlert('삭제되었습니다.');
    handleSearch();
    initialize();
  } catch (e) { vAlertError('삭제 실패') }
}

const initialize = () => {
  resetForm(formData);
  Object.assign(formData, { acct: '', acct_o: '', acctnm: '', bigo: '', acntgbn: '1000', useyn: 'Y', mode: 'N' });
  grid?.deselectRow();
}

onMounted(() => {
  nextTick(() => {
    initGrids();
    handleSearch();
  });
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
.grid-container-right { border-bottom: 3px solid #005a9f !important; }
</style>
