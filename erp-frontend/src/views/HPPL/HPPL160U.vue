<!--
	=============================================================
	프로그램명	: 외주생산계획 (HPPL160U)
	작성일자	: 2025.03.14
	설명        : 외주 생산 요청 자료 등록 및 관리 (원천 자료 관리)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="showModal" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-truck me-2 text-primary" style="font-size: 18px;"></i>
        생산관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        생산계획 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">외주생산계획 (HPPL160U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchData">조회</button>
        <button class="btn-erp btn-save" @click="saveData">저장</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 조회 조건 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense w-100">
            <colgroup>
              <col style="width: 100px;" /><col />
              <col style="width: 100px;" /><col />
              <col style="width: 100px;" /><col />
            </colgroup>
            <tbody>
              <tr>
                <th class="required bg-light text-center">요청일자</th>
                <td>
                  <div class="d-flex align-items-center gap-1 px-2">
                    <input v-model="searchForm.frymd" type="date" class="form-control form-control-sm" style="width: 140px;" />
                    <span class="text-muted">~</span>
                    <input v-model="searchForm.toymd" type="date" class="form-control form-control-sm" style="width: 140px;" />
                  </div>
                </td>
                <th class="bg-light text-center border-start">계획구분</th>
                <td>
                  <select v-model="searchForm.gubun" class="form-select form-select-sm" style="width: 120px;" disabled>
                    <option value="300">외주계획</option>
                  </select>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>외주 생산 요청 내역</span>
          <div class="d-flex gap-1">
            <button class="btn btn-sm btn-outline-primary py-0 px-2 fw-bold" @click="addRow" style="font-size: 11px;">+ 행추가</button>
            <button class="btn btn-sm btn-outline-danger py-0 px-2 fw-bold" @click="delRow" style="font-size: 11px;">- 행삭제</button>
          </div>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="mainGridRef" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { TabulatorFull as Tabulator, type RowComponent } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import type { ModalProps } from '@/types/modal'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

// 1. 상태 관리
const searchForm = reactive({
  frymd: new Date().toISOString().substring(0, 8) + '01',
  toymd: new Date().toISOString().substring(0, 10),
  gubun: "300"
})

const mainGridRef = ref<HTMLElement | null>(null)
let mainGrid: Tabulator | null = null

const showModal = ref(false)
const modalProps = reactive<ModalProps>({
  type: 'table', title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}
})

// 2. 그리드 초기화
const initGrid = () => {
  if (!mainGridRef.value) return
  mainGrid = new Tabulator(mainGridRef.value, {
    layout: 'fitColumns', height: '100%', selectable: true,
    columnDefaults: {
        headerHozAlign: 'center', headerSort: false, vertAlign: "middle",
        cellEdited: (cell) => {
            const row = cell.getRow(); const d = row.getData();
            if (d._status !== '입력' && d._status !== '삭제') row.update({ _status: '수정' });
        }
    },
    columns: [
      { title: "선택", width: 40, hozAlign: "center", formatter: "rowSelection", titleFormatter: "rowSelection" },
      { title: "상태", field: "_status", width: 60, hozAlign: "center", formatter: (c) => {
          const v = c.getValue();
          if (v === '입력') return '<span class="badge bg-primary">입력</span>';
          if (v === '수정') return '<span class="badge bg-warning text-dark">수정</span>';
          if (v === '삭제') return '<span class="badge bg-danger">삭제</span>';
          return '';
      }},
      { title: '요청일자', field: 'ordymd', width: 130, hozAlign: 'center', editor: 'date' },
      { title: '외주가공처 <i class="bi bi-search"></i>', field: 'custnm', width: 150, cellClick: (e, cell) => openModal('CUST', cell.getRow()) },
      { title: '제품코드', field: 'itemcd', width: 100, hozAlign: 'center' },
      { title: '제품명 <i class="bi bi-search"></i>', field: 'itemnm', widthGrow: 2, cellClick: (e, cell) => openModal('ITEM', cell.getRow()) },
      { title: '규격', field: 'itsize', width: 150 },
      { title: '단위', field: 'unit', width: 70, hozAlign: 'center' },
      { title: '요청수량', field: 'ordqty', width: 100, hozAlign: 'right', editor: 'number', formatter: 'money', cssClass: 'bg-light-yellow fw-bold' },
      { title: '완료요청일', field: 'napgiymd', width: 130, hozAlign: 'center', editor: 'date' },
      { title: '비고', field: 'bigo', widthGrow: 1, editor: 'input' }
    ]
  })
}

// 3. 비즈니스 로직
const fetchData = async () => {
  try {
    const res = await api.get('/product/pdplan/request-list', {
        params: {
            frymd: searchForm.frymd.replace(/-/g, ''),
            toymd: searchForm.toymd.replace(/-/g, ''),
            gubun: searchForm.gubun
        }
    })
    mainGrid?.setData((res.data || []).map((i: any) => ({ ...i, _status: '' })))
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 중 오류가 발생했습니다.') }
}

const saveData = async () => {
  const allData = mainGrid?.getData() || []
  const changes = allData.filter((r: any) => r._status)
  if (changes.length === 0) return vAlertError('변경사항이 없습니다.')

  if (!confirm('외주 요청 자료를 저장하시겠습니까?')) return

  try {
    const res = await api.post('/product/pdplan/request-save', changes.map(i => ({
        ...i,
        cmpycd: authStore.cmpycd,
        updemp: authStore.userid,
        gubun: searchForm.gubun
    })))

    const out = res.data[0];
    if (out.result === 'OK' || out.RESULT === 'OK') {
        vAlert(out.msg || out.MSG || '저장되었습니다.');
        fetchData();
    } else {
        vAlertError(out.msg || out.MSG || '저장 실패');
    }
  } catch (e) { vAlertError('저장 중 오류 발생') }
}

const initialize = () => { resetForm(searchForm); mainGrid?.clearData(); }

const addRow = () => {
  mainGrid?.addRow({
    _status: '입력',
    ordymd: new Date().toISOString().substring(0, 10),
    napgiymd: new Date().toISOString().substring(0, 10),
    ordqty: 0,
    gubun: searchForm.gubun
  }, true)
}

const delRow = () => {
  mainGrid?.getSelectedRows().forEach(row => {
    const d = row.getData();
    if (d._status === '입력') row.delete();
    else row.update({ _status: d._status === '삭제' ? '' : '삭제' });
  })
}

// 4. 도움창 로직
const openModal = (type: string, row: RowComponent) => {
  if (type === 'ITEM') {
    Object.assign(modalProps, {
      title: '제품 선택',
      path: '/hp00/HP00_000S_STR',
      data: { gubun: 'I0', cmpycd: authStore.cmpycd, gbncd: 'A' },
      columns: [
        { title: '자산구분', field: 'astkindnm', width: 100 },
        { title: '코드', field: 'itemcd', width: 100 },
        { title: '제품명', field: 'itemnm', width: 250 },
        { title: '규격', field: 'itsize', width: 150 }
      ],
      onConfirm: (d: any) => {
        row.update({ itemcd: d.itemcd, itemnm: d.itemnm, itsize: d.itsize, unit: d.unit });
        showModal.value = false;
      }
    })
  } else {
    Object.assign(modalProps, {
      title: '외주처 선택',
      path: '/ha00/HA00_00P_STR',
      data: { gubun: 'C4', cmpycd: authStore.cmpycd, gbncd: '', code: '', remark: '' },
      columns: [
        { title: '코드', field: 'custcd', width: 80, hozAlign: 'center' },
        { title: '거래처명', field: 'custnm', width: 200 }
      ],
      onConfirm: (d: any) => {
        row.update({ custcd: d.custcd || d.custcd, custnm: d.custnm || d.custnm });
        showModal.value = false;
      }
    })
  }
  showModal.value = true
}

onMounted(() => { nextTick(initGrid); fetchData(); })
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
.bg-light-yellow { background-color: #fffdf0 !important; }
</style>
