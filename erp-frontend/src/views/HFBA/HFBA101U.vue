<!--
	=============================================================
	프로그램명	: 코드관리 (HFBA101U)
	작성일자	: 2025.02.24
	설명        : 공통 코드 및 원가 기준 코드 관리 (HSOD100U 표준 패턴 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-gear-fill me-2 text-primary" style="font-size: 18px;"></i>
        기본정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">코드관리 (HFBA101U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchGroups">조회</button>
        <button class="btn-erp btn-save" @click="save">저장</button>
        <button class="btn-erp btn-delete" @click="deleteData" :disabled="formData.mode === 'N'">삭제</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 입력 및 필터 영역 -->
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
                <th class="text-center bg-light">코드구분</th>
                <td>
                  <select v-model="formData.cdkd" class="form-select" @change="onCdkdChange">
                    <option v-for="opt in cdkdOptions" :key="opt.code" :value="opt.code">{{ opt.cdnm }}</option>
                  </select>
                </td>
                <th class="text-center bg-light required">코 드</th>
                <td>
                  <input v-model="formData.code" class="form-control fw-bold text-primary" maxlength="10" :readonly="formData.mode === 'U'" placeholder="코드 입력" />
                </td>
                <th class="text-center bg-light required">코드명</th>
                <td>
                  <input v-model="formData.cdnm" class="form-control" maxlength="30" placeholder="코드명 입력" />
                </td>
              </tr>
              <tr>
                <th class="text-center bg-light">비 고</th>
                <td>
                  <input v-model="formData.remark" class="form-control" maxlength="50" />
                </td>
                <th class="text-center bg-light">출현순서</th>
                <td>
                  <input v-model="formData.dispord" type="number" class="form-control text-end" />
                </td>
                <th class="text-center bg-light">사용여부</th>
                <td>
                  <div class="form-check form-switch d-flex justify-content-center mt-1">
                    <input v-model="formData.useyn" class="form-check-input" type="checkbox" true-value="Y" false-value="N">
                    <label class="form-check-label small ms-2">{{ formData.useyn === 'Y' ? '사용' : '미사용' }}</label>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 투-그리드 레이아웃 영역 -->
      <div class="d-flex gap-2 flex-grow-1 overflow-hidden" style="min-height: 0;">

        <!-- ⬅️ 좌측: 코드 그룹 (grid1) -->
        <div class="card border shadow-sm d-flex flex-column overflow-hidden grid-container-left" style="width: 300px; min-width: 300px;">
          <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark text-center">코드 그룹</div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="tableRef1" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>

        <!-- ➡️ 우측: 상세 코드 목록 (grid2) -->
        <div class="flex-grow-1 d-flex flex-column gap-2 overflow-hidden">
          <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column grid-container-right">
            <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
              <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>상세 코드 목록</span>
            </div>
            <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
              <div ref="tableRef2" class="tabulator-instance flex-grow-1"></div>
            </div>
          </div>
        </div>

      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, nextTick } from 'vue'
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

// [1] 데이터 모델링 (소문자 원칙)
const formData = reactive<any>({
  cdkd: '0000', code: '', cdnm: '', remark: '', dispord: 0, useyn: 'Y', mode: 'N'
})

const cdkdOptions = ref<any[]>([])
const tableRef1 = ref<HTMLDivElement | null>(null)
const tableRef2 = ref<HTMLDivElement | null>(null)
let grid1: Tabulator | null = null
let grid2: Tabulator | null = null

// [2] 그리드 초기화
const initGrids = () => {
  // Left: 그룹 그리드
  grid1 = new Tabulator(tableRef1.value!, {
    layout: "fitColumns", height: "100%", placeholder: "데이터 없음", selectable: 1,
    columns: [
      { title: "No", formatter: "rownum", width: 40, hozAlign: "center", headerSort: false },
      { title: "코드", field: "code", width: 80, hozAlign: "center", headerSort: false },
      { title: "코드그룹명", field: "cdnm", hozAlign: "left", headerSort: false, cssClass: "fw-bold text-primary" }
    ],
  });
  grid1.on("rowClick", (e, row) => {
    const data = row.getData();
    formData.cdkd = data.code;
    fetchDetails(data.code);
    resetInputOnly();
  });

  // Right: 상세 그리드
  grid2 = new Tabulator(tableRef2.value!, {
    layout: "fitColumns", height: "100%", placeholder: "데이터 없음", selectable: 1,
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "코드", field: "code", width: 100, hozAlign: "center", cssClass: "fw-bold text-primary" },
      { title: "코드명", field: "cdnm", minWidth: 200, widthGrow: 1 },
      { title: "순서", field: "dispord", width: 80, hozAlign: "center" },
      { title: "사용", field: "useyn", width: 80, hozAlign: "center",
        formatter: (cell) => {
          const val = String(cell.getValue() || '').trim().toUpperCase();
          return val === 'Y' ? '<b class="text-primary">사용</b>' : '';
        }
      },
      { title: "비고", field: "remark", minWidth: 150 }
    ],
  });
  grid2.on("rowClick", (e, row) => fetchDetailRow(row.getData()));
}

// [3] 비즈니스 로직
const loadOptions = async () => {
  try {
    const res = await api.get('/hp00/HP00_000S_STR', {
      params: { gubun: 'E2', cmpycd: authStore.cmpycd, gbncd: '0000', code: '' }
    })
    cdkdOptions.value = res.data;
  } catch (e) { console.error(e) }
}

const fetchGroups = async () => {
  try {
    const res = await api.post('/hfba/FBA1010U_STR', {
      actkind: 'S0', cmpycd: authStore.cmpycd, cdkd: '0000', code: '', cdnm: '', remark: '', dispord: '0', useyn: 'Y', userid: authStore.userid
    })
    grid1?.setData(res.data)
    if (res.data?.length > 0) {
      nextTick(() => {
        const firstRow = grid1?.getRows()[0];
        if (firstRow) {
            grid1?.selectRow(firstRow);
            onProcessSelect(firstRow.getData());
        }
      })
    }
  } catch (e) { vAlertError('그룹 조회 실패') }
}

const fetchDetails = async (cdkd: string) => {
  if (!cdkd) return
  try {
    const res = await api.post('/hfba/FBA1010U_STR', {
      actkind: 'S0', cmpycd: authStore.cmpycd, cdkd: cdkd, code: '', cdnm: '', remark: '', dispord: '0', useyn: 'Y', userid: authStore.userid
    })
    grid2?.setData(res.data)
  } catch (e) { vAlertError('상세 조회 실패') }
}

const onProcessSelect = (data: any) => {
    formData.cdkd = data.code;
    fetchDetails(data.code);
}

const fetchDetailRow = (row: any) => {
  Object.assign(formData, { ...row, mode: 'U' });
}

const onCdkdChange = () => {
    resetInputOnly();
    fetchDetails(formData.cdkd);
}

const save = async () => {
  if (!formData.code || !formData.cdnm) return vAlertError('코드와 코드명은 필수 입력 사항입니다.');
  if (!confirm('저장하시겠습니까?')) return

  try {
    const actkind = formData.mode === 'U' ? 'U0' : 'A0'
    await api.post('/hfba/FBA1010U_STR', {
      ...formData, actkind: actkind, cmpycd: authStore.cmpycd, userid: authStore.userid
    })
    vAlert('저장되었습니다.');
    fetchDetails(formData.cdkd);
    if (formData.cdkd === '0000') fetchGroups();
  } catch (e) { vAlertError('저장 실패') }
}

const deleteData = async () => {
  if (formData.mode === 'N') return
  if (!confirm('정말 삭제하시겠습니까?')) return
  try {
    await api.post('/hfba/FBA1010U_STR', {
      actkind: 'D0', cmpycd: authStore.cmpycd, cdkd: formData.cdkd, code: formData.code, userid: authStore.userid
    })
    vAlert('삭제되었습니다.');
    fetchDetails(formData.cdkd);
    if (formData.cdkd === '0000') fetchGroups();
    resetInputOnly();
  } catch (e) { vAlertError('삭제 실패') }
}

const resetInputOnly = () => {
    const currentCdkd = formData.cdkd;
    Object.assign(formData, { cdkd: currentCdkd, code: '', cdnm: '', remark: '', dispord: 0, useyn: 'Y', mode: 'N' });
    grid2?.deselectRow();
}

const initialize = () => {
  resetForm(formData);
  Object.assign(formData, { cdkd: '0000', code: '', cdnm: '', remark: '', dispord: 0, useyn: 'Y', mode: 'N' });
  grid1?.clearData(); grid2?.clearData();
  fetchGroups();
}

onMounted(() => {
  loadOptions();
  nextTick(initGrids);
  fetchGroups();
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
.grid-container-left, .grid-container-right { border-bottom: 3px solid #005a9f !important; }
</style>
