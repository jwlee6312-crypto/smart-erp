<!--
	=============================================================
	프로그램명	: 자재불출요청 상세내역 (HPIO251S)
	작성일자	: 2025.02.24
	설명        : 불출 요청된 자재의 상세 내역 조회 (표준 디자인 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" v-bind="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-file-earmark-ruled me-2 text-primary" style="font-size: 18px;"></i>
        자재관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">자재불출요청 상세내역 (HPIO251S)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-excel" @click="exportExcel">엑셀</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 조회 필터 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
                <col style="width: 100px" /><col style="width: 300px" />
                <col style="width: 100px" /><col />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light">요청부서</th>
                <td>
                  <div class="input-group input-group-sm" style="width: 250px;">
                    <input v-model="searchForm.deptcd" class="form-control text-center" style="width: 70px;" placeholder="코드" @keyup.enter="handleDeptSearch" />
                    <button class="btn btn-dark" @click="handleDeptSearch"><i class="bi bi-search" /></button>
                    <input v-model="searchForm.deptnm" class="form-control bg-light" readonly />
                  </div>
                </td>
                <th class="text-center bg-light">요청기간</th>
                <td class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                  <DateForm v-model:fromdt="searchForm.frymd" v-model:todt="searchForm.toymd" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden border-top border-3 border-primary">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex justify-content-between align-items-center">
          <span class="fw-bold small text-dark"><i class="bi bi-list-columns me-1 text-primary"></i>불출 요청 상세 목록</span>
          <span class="badge bg-secondary" style="font-size: 10px;">{{ gridCount }}건</span>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white">
          <div ref="tableRef" class="tabulator-instance h-100"></div>
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
import { useCommonHelp } from '@/composables/useCommonHelp'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { getDate } from '@/composables/useDate'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import DateForm from '@/components/DateForm.vue'

const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const authStore = useAuthStore()
const { modalVisible, modalProps, openHelp } = useCommonHelp()
const { firstDay, today } = getDate()

const searchForm = reactive({ deptcd: '', deptnm: '', frymd: firstDay, toymd: today })
const gridCount = ref(0)
const tableRef = ref<HTMLElement | null>(null)
let tableInstance: Tabulator | null = null

const initGrid = () => {
  if (tableInstance) return;
  tableInstance = new Tabulator(tableRef.value!, {
    layout: 'fitColumns', height: '100%',
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "No", formatter: "rownum", width: 40, hozAlign: "center" },
      { title: '요청번호', field: 'outymno', width: 130, hozAlign: 'center', mutator: (v,d) => d.outym && d.outno ? `${d.outym}-${d.outno}` : '' },
      { title: '라인명', field: 'linenm', width: 120 },
      { title: '작업지시번호', field: 'lotymno', width: 130, hozAlign: 'center', mutator: (v,d) => d.lotymd && d.lotno ? `${d.lotymd}-${d.lotno}` : '' },
      { title: '주문번호', field: 'ordymno', width: 130, hozAlign: 'center', mutator: (v,d) => d.gubun && d.ordym && d.ordno ? `${d.gubun}-${d.ordym}-${d.ordno}` : '' },
      { title: '제품명', field: 'itemnm', minWidth: 200, widthGrow: 1, cssClass: 'text-primary fw-bold' },
      { title: '자재명', field: 'mitemnm', minWidth: 200, widthGrow: 1, cssClass: 'text-success fw-bold' },
      { title: '규격', field: 'mitsize', width: 150 },
      { title: '단위', field: 'munit', width: 60, hozAlign: 'center' },
      { title: '지시수량', field: 'ordqty', width: 100, hozAlign: 'right', formatter: 'money' },
      { title: '요청수량', field: 'outqty', width: 100, hozAlign: 'right', formatter: 'money' }
    ]
  });
}

const handleDeptSearch = () => {
  openHelp('DEPT', (d) => { searchForm.deptcd = d.deptcd; searchForm.deptnm = d.deptnm });
}

const search = async () => {
  try {
    const params = {
      ...searchForm,
      cmpycd: authStore.cmpycd,
      fromdt: searchForm.frymd.replace(/-/g, ''),
      todt: searchForm.toymd.replace(/-/g, '')
    }
    const { data } = await api.post('/hpio/HPIO_251S_STR', params)
    tableInstance?.setData(data || []);
    gridCount.value = (data || []).length;
    vAlert('조회되었습니다.');
  } catch (e) { vAlertError('조회 실패') }
}

const initialize = () => {
  Object.assign(searchForm, { deptcd: '', deptnm: '', frymd: firstDay, toymd: today });
  tableInstance?.clearData(); gridCount.value = 0;
}

const exportExcel = () => {
  if (!tableInstance || !gridCount.value) return vAlertError('데이터가 없습니다.');
  tableInstance.download('xlsx', `자재불출상세_${searchForm.frymd}.xlsx`);
}

onMounted(() => { nextTick(initGrid); });
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; font-size: 12px; }
</style>
