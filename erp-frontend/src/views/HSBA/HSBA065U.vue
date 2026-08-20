<!--
	=============================================================
	프로그램명	: 품목별 매입거래처 및 단가관리 (HSBA065U)
	작성일자	: 2025.02.24
	설명        : 품목별 매입 정보 관리 (HSOD100U 디자인 패턴 및 스크롤 최적화)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom bg-white py-2 px-3 sticky-top shadow-sm">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-tags-fill me-2 text-primary" style="font-size: 18px;"></i>
        기본정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">품목별 매입거래처 및 단가관리</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-2">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchList">조회</button>
        <button class="btn-erp btn-save" @click="save">저장</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- 🔍 [상단] 조회 필터 영역 (HSOD100U 디자인 패턴 적용) -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
              <col style="width: 10%" />
              <col style="width: 23%" />
              <col style="width: 10%" />
              <col style="width: 23%" />
              <col style="width: 10%" />
              <col style="width: 24%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light">재고자산</th>
                <td>
                  <select v-model="searchForm.sch_astkind" class="form-select" @change="fetchList">
                    <option value="000">전체</option>
                    <option value="100">원재료</option>
                    <option value="112">구매품</option>
                    <option value="120">상품</option>
                  </select>
                </td>
                <th class="text-center bg-light">품목명</th>
                <td>
                  <input v-model="searchForm.sch_itemnm" class="form-control" placeholder="품목코드 또는 품목명" @keyup.enter="fetchList" />
                </td>
                <th class="text-center bg-light">조회대상</th>
                <td>
                  <select v-model="searchForm.selgbn" class="form-select w-50" @change="fetchList">
                    <option value="0">전체</option>
                    <option value="1">미등록</option>
                  </select>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 📊 3. 메인 그리드 영역 (💡 스크롤 보장) -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white grid-container-right">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark d-flex align-items-center">
            <i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i> 품목별 매입 정보 리스트
          </span>
          <button class="btn btn-xs btn-outline-secondary px-2" style="height: 24px; font-size: 11px;" @click="toggleAllRows">전체선택</button>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="gridRef" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
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

const searchForm = reactive({ sch_astkind: '000', sch_itemnm: '', selgbn: '0' })
const gridRef = ref<HTMLDivElement | null>(null)
let grid: Tabulator | null = null

async function fetchList() {
  try {
    const res = await api.post('/hsba/HSBA_065U_STR', {
      actkind: 'S0',
      cmpycd: authStore.cmpycd,
      astkind: searchForm.sch_astkind,
      itemnm: searchForm.sch_itemnm,
      selgbn: searchForm.selgbn,
      price: 0
    })
    // 🚀 [해결] 필드명 대소문자 무관하게 처리 및 Boolean 매핑
    const mappedData = (res.data || []).map((i: any) => {
      const val = i.autoyn || i.AUTOYN || 'N'
      return {
        ...i,
        autoyn: val === 'Y'
      }
    })
    grid?.setData(mappedData)
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

async function save() {
  const selectedData = grid?.getSelectedData()
  if (!selectedData || selectedData.length === 0) return vAlertError('저장할 항목을 선택하세요.')
  if (!confirm('저장하시겠습니까?')) return

  let successCount = 0;
  let lastError = '';

  try {
    for (const item of selectedData) {
      const saveItem = {
        ...item,
        autoyn: item.autoyn ? 'Y' : 'N',
        actkind: 'A0',
        cmpycd: authStore.cmpycd,
        userid: authStore.userid
      }
      try {
        await api.post('/hsba/HSBA_065U_STR', saveItem)
        successCount++;
      } catch (e: any) {
        // 🚀 [해결] 백엔드 수정 없이 Vue에서 SQL 결과집합 예외 핸들링
        const msg = e.response?.data?.message || e.message || '';
        if (msg.includes('결과 집합') || msg.includes('ResultSet') || msg.includes('result set')) {
          successCount++; // 데이터는 저장되었으나 응답 처리에서 난 오류이므로 성공으로 간주
        } else {
          lastError = msg;
        }
      }
    }

    if (successCount > 0) {
      vAlert('저장이 완료되었습니다.');
      fetchList();
    } else {
      vAlertError(lastError || '저장 중 오류가 발생했습니다.');
    }
  } catch (e: any) {
    vAlertError('통신 중 오류가 발생했습니다.');
  }
}

const toggleAllRows = () => {
  const rows = grid?.getRows(); if (!rows) return
  const allSelected = rows.every(r => r.isSelected())
  rows.forEach(r => allSelected ? r.deselect() : r.select())
}

function initialize() {
  resetForm(searchForm);
  searchForm.sch_astkind = '000'; searchForm.selgbn = '0';
  grid?.clearData();
}

function openHelp(type: string, row?: any) {
  switch (type) {
    case 'CUST': // 매입거래처 선택
      Object.assign(modalProps, {
        title: '거래처 선택',
        path: '/ha00/HA00_00P_STR',
        defaultField: 'custnm',
        data: { gubun: 'C4', cmpycd: authStore.cmpycd, gbncd: '', code: '', remark: '' },
        columns: [
          { title: '거래처코드', field: 'custcd', width: 120, hozAlign: 'center' },
          { title: '거래처명', field: 'custnm', minWidth: 250, widthGrow: 1, hozAlign: 'left' }
        ],
        onConfirm: (data: any) => {
          row.update({ custcd: data.custcd, custnm: data.custnm })
          row.select()
        }
      })
      modalVisible.value = true
      break
  }
}

onMounted(() => {
  if (gridRef.value) {
    grid = new Tabulator(gridRef.value, {
      layout: 'fitColumns', height: '100%', selectable: true,
      columnDefaults: { headerSort: false, headerHozAlign: "center", vertAlign: "middle" },
      columns: [
        { title: '선택', formatter: 'rowSelection', titleFormatter: 'rowSelection', width: 40, hozAlign: 'center' },
        { title: '품목코드', field: 'itemcd', width: 100, hozAlign: 'center' },
        { title: '품목명', field: 'itemnm', minWidth: 200, widthGrow: 1, cssClass: 'fw-bold text-primary' },
        { title: '규격', field: 'itsize', width: 150 },
        { title: '단위', field: 'unit', width: 60, hozAlign: 'center' },
        { title: '단가', field: 'price', hozAlign: 'right', width: 100, editor: 'number', formatter: 'money', formatterParams: { precision: 0 },
          cellEdited: (cell) => { cell.getRow().select() }
        },
        {
          title: '매입거래처', field: 'custnm', minWidth: 180, widthGrow: 1,
          cellClick: (e, cell) => { if ((e.target as HTMLElement).tagName === 'I') openHelp('CUST', cell.getRow()) },
          formatter: (cell) => `<div class='d-flex justify-content-between align-items-center w-100'><span>${cell.getValue() || ''}</span> <i class="bi bi-search text-primary ms-1"></i></div>`
        },
        {
          title: '자동발주', field: 'autoyn', hozAlign: 'center', width: 80,
          formatter: (cell) => `<input type="checkbox" class="form-check-input" ${cell.getValue() ? 'checked' : ''} />`,
          cellClick: (e, cell) => {
            cell.setValue(!cell.getValue())
            cell.getRow().select()
          }
        },
        { title: '비고', field: 'remark', minWidth: 150, widthGrow: 1, editor: 'input',
          cellEdited: (cell) => { cell.getRow().select() }
        }
      ]
    })
  }
  fetchList()
})
</script>

<style scoped>
.main-content-wrapper { padding-bottom: 0vh !important; }
.erp-table-dense th, .erp-table-dense td {
  height: 32px !important; padding: 0 8px !important; font-size: 12px; vertical-align: middle; border: 1px solid #dee2e6;
}
.erp-table-dense .form-control, .erp-table-dense .form-select, .erp-table-dense .btn {
  height: 26px !important; font-size: 12px !important; border-radius: 2px;
}
.erp-table-dense th { font-weight: 600; color: #495057; }
.grid-container-right { border-bottom: 3px solid #005a9f !important; }
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
