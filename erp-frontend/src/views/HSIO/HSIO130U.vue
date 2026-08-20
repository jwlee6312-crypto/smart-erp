<!--
	=============================================================
	프로그램명	: 매입전표발행 (HSIO130U)
	작성일자	: 2025.02.24
	설명        : 입고 정산 내역을 기반으로 회계 전표 발행 (ASP 패턴 기반 순차 저장 로직 적용 및 소문자 통일)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom bg-white py-2 px-3 sticky-top shadow-sm">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-file-earmark-post-fill me-2 text-primary" style="font-size: 18px;"></i>
        구매정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        매입정산 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        <span class="text-primary fw-bolder">매입전표발행 (HSIO130U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-2">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchList">조회</button>
        <button class="btn-erp btn-save" @click="save">전표발행</button>
      </div>
    </div>

    <!-- 🔍 2. 상단 조회 필터 -->
    <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
            <div class="card-body p-0 bg-white">
              <table class="erp-table-dense" width="100%">
                <colgroup>
                  <col style="width: 10%" /><col style="width: 40%" />
                  <col style="width: 10%" /><col style="width: 40%" />
                </colgroup>
                <tbody>
                  <tr>
                    <th class="text-center bg-light">입고부서</th>
                    <td>
                      <div class="input-group input-group-sm w-75">
                        <input v-model="searchForm.deptcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
                        <input v-model="searchForm.deptnm" type="text" class="form-control" placeholder="부서 선택" />
                        <button class="btn btn-outline-secondary px-2" @click="openHelp('DEPT_search')"><i class="bi bi-search"></i></button>
                      </div>
                    </td>
                    <th class="text-center bg-light">정산일자</th>
                    <td class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                      <DateForm v-model:fromdt="searchForm.fromdt" v-model:todt="searchForm.todt" />
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

    <div class="flex-grow-1 d-flex flex-column overflow-hidden p-2 gap-2 bg-light">
      <!-- 🅰️ 전표 발행 설정 폼 -->
      <div class="card border shadow-sm overflow-hidden flex-shrink-0">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense w-100">
            <colgroup>
              <col style="width: 100px;" /><col  style="width: 200px;" />
              <col style="width: 100px;" /><col  style="width: 350px;" />
              <col style="width: 100px;" /><col  style="width: 150px;" />
              <col style="width: 100px;" /><col />
            </colgroup>
            <tbody>
              <tr>
                <th class="required bg-light text-center">전표일자</th>
                <td><input v-model="formData.pubymd" type="date" class="form-control" /></td>
                <th class="required bg-light text-center">발행부서</th>
                <td>
                  <div class="input-group input-group-sm">
                    <input v-model="formData.deptcd" type="text" class="form-control text-center bg-light fw-bold" style="max-width: 60px;" readonly />
                    <input v-model="formData.deptnm" type="text" class="form-control" readonly />
                    <button class="btn btn-outline-secondary px-2" @click="openHelp('DEPT')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="bg-light text-center">카드결제</th>
                <td>
                  <div class="form-check form-switch m-0 d-flex align-items-center justify-content-center">
                    <input v-model="formData.cardyn" class="form-check-input mt-0" type="checkbox" true-value="Y" false-value="N" id="cardCheck">
                    <label class="form-check-label ms-2 small fw-bold" for="cardCheck">사용</label>
                  </div>
                </td>
                <th class="bg-light text-center">카드번호</th>
                <td>
                  <div class="input-group input-group-sm">
                    <input v-model="formData.cardno" type="text" class="form-control" placeholder="카드번호 선택" :readonly="formData.cardyn !== 'Y'" />
                    <button class="btn btn-outline-secondary px-2" @click="openHelp('CARD')" :disabled="formData.cardyn !== 'Y'"><i class="bi bi-search"></i></button>
                    <input v-model="formData.cardnm" type="text" class="form-control bg-light" readonly style="flex-grow: 2;" />
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 🅱️ 정산 내역 그리드 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white grid-container-right">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
          <span class="fw-bold small text-dark d-flex align-items-center">
            <i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i> 전표 발행 대상 내역
          </span>
          <button class="btn btn-xs btn-outline-secondary px-2" style="height: 24px; font-size: 11px;" @click="toggleAllRows">전체선택/해제</button>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="mainGridRef" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>
    </div>
  </div>

  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick, watch, onUnmounted } from 'vue'
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
import DateForm from '@/components/DateForm.vue'

const authStore = useAuthStore()
const { firstDay, today } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps, openHelp: openCommonHelp } = useCommonHelp()

const searchForm = reactive<any>({
  deptcd: authStore.deptcd,
  deptnm: authStore.deptnm,
  fromdt: firstDay,
  todt: today
})

const formData = reactive<any>({
  actkind: 'A',
  cmpycd: authStore.cmpycd,
  pubymd: today,
  taxunit: '',
  vattype: '010',
  cardyn: 'N',
  cardno: '',
  cardnm: '',
  deptcd: authStore.deptcd,
  deptnm: authStore.deptnm
})

// 카드결제 사용 여부에 따른 카드 정보 초기화
watch(() => formData.cardyn, (newVal) => {
  if (newVal === 'N') {
    formData.cardno = '';
    formData.cardnm = '';
  }
})

const saOptions = ref<any[]>([]);
const vatOptions = ref<any[]>([]);
const mainGridRef = ref<HTMLDivElement | null>(null);
let grid: Tabulator | null = null;

async function fetchOptions() {
  try {
    const [resSa, resVat] = await Promise.all([
      api.post('/ha00/HA00_00P_STR', { gubun: 'SA', cmpycd: authStore.cmpycd }),
      api.post('/ha00/HA00_00P_STR', { gubun: 'E0', cmpycd: authStore.cmpycd, gbncd: '120' })
    ])
    saOptions.value = resSa.data;
    vatOptions.value = resVat.data;
    if (saOptions.value.length > 0) formData.taxunit = saOptions.value[0].taxunit;
  } catch (e) {}
}

const openHelp = (type: string) => {
  if (type === 'S_DEPT' || type === 'DEPT') {
    Object.assign(modalProps, {
      title: '부서 선택',
      path: '/ha00/HA00_00P_STR',
      defaultField: 'deptnm',
      data: { gubun: 'D0', cmpycd: authStore.cmpycd, gbncd: '', code: '', remark: '' },
      columns: [
        { title: '코드', field: 'deptcd', width: 80, hozAlign: 'center' },
        { title: '부서명', field: 'deptnm', width: 200 }
      ],
      onConfirm: (d: any) => {
        if (type === 'S_DEPT') { searchForm.deptcd = d.deptcd; searchForm.deptnm = d.deptnm; }
        else { formData.deptcd = d.deptcd; formData.deptnm = d.deptnm; }
      }
    });
    modalVisible.value = true;
  } else if (type === 'CARD') {

    Object.assign(modalProps, {
      title: '카드 선택',
      path: '/ha00/HA00_00P_STR',
      defaultField: 'usernm',
      data: { gubun: 'M0', cmpycd: authStore.cmpycd, gbncd: '040', code: '', remark: '' },
      columns: [
        { title: '카드번호', field: 'mgtno', width: 100, hozAlign: 'center' },
        { title: '카드명', field: 'mgtnm', width: 150 },
        { title: '비고', field: 'soname', width: 150 }
      ],
      onConfirm: (d: any) => { formData.cardno = d.mgtno; formData.cardnm = d.mgtnm; }
    });
    modalVisible.value = true;
  }
}

async function fetchList() {
  try {
    const res = await api.post('/hsio/HSIO_130U_STR', {
      actkind: 'S0',
      cmpycd: authStore.cmpycd,
      iogbn: '100',
      fromdt: searchForm.fromdt.replace(/-/g, ''),
      todt: searchForm.todt.replace(/-/g, ''),
      deptcd: searchForm.deptcd
    });
    console.log(res.data)
    grid?.setData(res.data.map((i: any) => ({ ...i, procyn: false })));
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

/**
 * 🚀 전표 발행 로직 (통합 백엔드 서비스 방식 - HSIP140U 표준 모델 준수)
 */
async function save() {
  const selectedItems = grid?.getSelectedData() || []

  if (selectedItems.length === 0) return vAlertError('전표 발행할 항목을 선택하세요.')
  if (formData.cardyn === 'Y' && !formData.cardno) return vAlertError('카드번호를 입력하세요.')
  if (!confirm('선택한 항목들에 대해 매입전표를 발행하시겠습니까?')) return

  const payload = {
    mst: {
      pubymd: formData.pubymd,
      deptcd: formData.deptcd,
      cmpycd: authStore.cmpycd,
      usernm: authStore.usernm,
      cardyn: formData.cardyn,
      cardno: formData.cardno,
      fromdt: searchForm.fromdt,
      todt: searchForm.todt,
      taxunit: formData.taxunit,
      vattype: formData.vattype
    },
    items: selectedItems.map(item => ({
      jsanym: item.jsanym,
      jsanno: item.jsanno,
      jsanymd: item.jsanymd,
      spyamt: item.spyamt,
      vatamt: item.vatamt,
      custcd: item.custcd,
      deptcd: item.deptcd
    }))
  }

  try {
    const res = await api.post('/hsio/HSIO_130U_SAVE', payload)
    if (res.data) {
      vAlert('매입전표가 성공적으로 발행되었습니다.')
      fetchList()
      initialize()
    }
  } catch (e: any) {
    vAlertError('발행 실패: ' + (e.response?.data?.message || e.message))
  }
}

function initialize() {
  resetForm(formData);
  formData.pubymd = today
  formData.deptcd = authStore.deptcd; formData.deptnm = authStore.deptnm;
  grid?.clearData();
}

const toggleAllRows = () => {
  const rows = grid?.getRows(); if (!rows || rows.length === 0) return
  const allSelected = grid?.getSelectedRows().length === rows.length
  if (allSelected) grid?.deselectRow(); else grid?.selectRow()
}

onUnmounted(() => {
  if (grid) grid.destroy();
});

onMounted(async () => {
  await fetchOptions();

  if (mainGridRef.value) {
    grid = new Tabulator(mainGridRef.value, {
      layout: "fitColumns", height: "100%", placeholder: "품목 없음", selectable: true,
      columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
      columns: [
        { title: '선택', width: 60, hozAlign: 'center', formatter: 'rowSelection', titleFormatter: 'rowSelection', headerSort: false },
        { title: '정산일', field: 'jsanymd', width: 110, hozAlign: 'center', formatter: (c) => {
            const v = c.getValue(); return v && v.length === 8 ? `${v.substring(0,4)}-${v.substring(4,6)}-${v.substring(6,8)}` : v;
        }},
        { title: '부서', field: 'deptnm', width: 150 },
        { title: '사업장', field: 'unitnm', width: 120 },
        { title: '유형', field: 'vattypenm', width: 100 },
        { title: '거래처', field: 'custnm', minWidth: 200, widthGrow: 1, hozAlign: 'left', cssClass: 'fw-bold' },
        { title: '공급가', field: 'spyamt', width: 110, hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 } },
        { title: '부가세', field: 'vatamt', width: 100, hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 } },
        { title: '합계', field: 'jsansum', width: 120, hozAlign: 'right', formatter: 'money', cssClass: 'bg-light fw-bold',
          mutatorData: (v, d) => Number(d.spyamt || 0) + Number(d.vatamt || 0)
        }
      ]
    })
  }
  nextTick(() => fetchList())
})
</script>

<style scoped>
.main-content-wrapper { padding-bottom: 0px !important; }
.grid-container-right { border-bottom: 3px solid #005a9f !important; }
.erp-table-dense th, .erp-table-dense td {
  height: 32px !important; padding: 0 8px !important; font-size: 12px; vertical-align: middle; border: 1px solid #dee2e6;
}
.erp-table-dense .form-control, .erp-table-dense .form-select, .erp-table-dense .btn {
  height: 26px !important; font-size: 12px !important; border-radius: 2px;
}
.erp-table-dense th { font-weight: 600; color: #495057; }
.tabulator-instance { width: 100% !important; background-color: #fff; }
:deep(.empty-row-style) { opacity: 0.6; }
</style>
