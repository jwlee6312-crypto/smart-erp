<!--
	=============================================================
	프로그램명	: 품목원가등록 (HSIP180U)
	작성일자	: 2025.02.24
	설명        : 수입 품목별 최종 원가 등록 (로컬 팝업 로직 적용 및 직관적 구성)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom bg-white py-2 px-3 sticky-top shadow-sm">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-box-seam me-2 text-primary" style="font-size: 18px;"></i>
        수입관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">품목원가등록 (HSIP180U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-2">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchList">조회</button>
        <button class="btn-erp btn-save" @click="save">원가 저장</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">
      <!-- 🔍 [상단] 조회 필터 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
              <col style="width: 130px;" /> <col />
              <col style="width: 130px;" /> <col />
              <col style="width: 130px;" /> <col />
              <col style="width: 130px;" /> <col />
            </colgroup>
            <tbody>
              <tr>
                <th class="required bg-light text-center">PO No</th>
                <td>
                  <div class="input-group input-group-sm flex-nowrap ms-1">
                    <input v-model="searchForm.fileno" type="text" class="form-control border-primary-subtle fw-bold text-primary" placeholder="PO 번호 입력" @keyup.enter="fetchList" />
                    <button class="btn btn-outline-secondary px-2" @click="openPoPopup"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="required bg-light text-center">선적차수</th>
                <td>
                  <div class="input-group input-group-sm flex-nowrap ms-1">
                    <input v-model="searchForm.shipseq" type="text" class="form-control text-center" style="max-width: 60px;" readonly />
                    <button class="btn btn-outline-secondary px-2" @click="openShipPopup"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="required bg-light text-center">통관차수</th>
                <td>
                  <div class="input-group input-group-sm flex-nowrap ms-1">
                    <input v-model="searchForm.passseq" type="text" class="form-control text-center" style="max-width: 60px;" readonly />
                    <button class="btn btn-outline-secondary px-2" @click="openPassPopup"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <td colspan="2" class="bg-light text-center small text-muted">※ 전표 마감 여부를 확인 후 작업하십시오.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 📊 3. 하단 원가 리스트 그리드 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white grid-container-right">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark d-flex align-items-center">
            <i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i> 품목별 최종 수입 원가 등록 현황
          </span>
          <div class="d-flex align-items-center gap-2">
            <span class="text-danger fw-bold small">총 원가금액: {{ totalSummaryAmt.toLocaleString() }}</span>
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
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

// [도움창 상태 정의]
const modalVisible = ref(false)
const modalProps = reactive<any>({
  title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table'
})

// [1] 데이터 모델링
const searchForm = reactive({
  fileno: '', shipseq: '', passseq: ''
})

const totalSummaryAmt = ref(0)
const mainGridRef = ref<HTMLDivElement | null>(null); let mainGrid: Tabulator | null = null

const fetchList = async () => {
  if (!searchForm.fileno || !searchForm.shipseq || !searchForm.passseq) {
    return vAlertError('조회 조건을 모두 입력하세요.')
  }
  try {
    const res = await api.post('/hsip/HSIP_180U_STR', {
      ...searchForm, actkind: 'S0', cmpycd: authStore.cmpycd, ioamt: 0
    })
    mainGrid?.setData(res.data || [])
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

/**
 * 🚀 저장 로직 (ASP 패턴: 상세 루프 호출)
 */
const save = async () => {
  const selected = mainGrid?.getSelectedData() || []
  if (selected.length === 0) return vAlertError('저장할 항목을 선택하세요.')

  if (!confirm('정상적으로 작업하시겠습니까?')) return

  try {
    for (const item of selected) {
      const params = {
        actkind: 'U0',
        cmpycd: authStore.cmpycd,
        fileno: searchForm.fileno,
        shipseq: searchForm.shipseq,
        passseq: searchForm.passseq,
        ioym: item.ioym,
        iono: item.iono,
        iorowno: item.iorowno,
        ioamt: String(item.ioamt || '0').replace(/,/g, ''),
        updemp: authStore.userid
      }

      const res = await api.post('/hsip/HSIP_180U_STR', params)
      const resData = res.data?.[0]

      if (resData && (resData.erryn === 'Y' || resData.status === 'Y')) {
        throw new Error(resData.msg || '저장 중 업무 오류가 발생했습니다.')
      }
    }

    vAlert('정상적으로 작업이 되었습니다.')
    fetchList()
  } catch (e: any) {
    vAlertError(e.message || '저장 실패')
  }
}

function initialize() {
  resetForm(searchForm);
  mainGrid?.clearData();
  totalSummaryAmt.value = 0;
}

/** 🚀 로컬 도움창 핸들러 (HSIP170S 방식) */
function openPoPopup() {
  Object.assign(modalProps, {
    title: 'PO 선택', path: '/hs00/HS00_000S_STR',
    data: { gubun: 'F0', cmpycd: authStore.cmpycd, gbncd: '1', code: '', codenm: searchForm.fileno, etcval: '' },
    columns: [
      { title: 'PO No', field: 'fileno', width: 150, hozAlign: 'center' },
      { title: '거래처', field: 'custnm', minWidth: 200, widthGrow: 1 },
      { title: '발주일자', field: 'issymd', width: 120, hozAlign: 'center' }
    ],
    onConfirm: (d: any) => {
      searchForm.fileno = d.fileno;
      searchForm.shipseq = '';
      searchForm.passseq = '';
    }
  });
  modalVisible.value = true;
}

function openShipPopup() {
  if (!searchForm.fileno) return vAlertError('PO No를 먼저 선택하세요.')
  Object.assign(modalProps, {
    title: '선적차수 선택', path: '/hs00/HS00_000S_STR',
    data: { gubun: 'F0', cmpycd: authStore.cmpycd, gbncd: '2', code: searchForm.fileno, codenm: '', etcval: '' },
    columns: [
      { title: '선적차수', field: 'shipseq', width: 100, hozAlign: 'center' },
      { title: 'PO No', field: 'fileno', width: 150 },
      { title: '거래처', field: 'custnm', minWidth: 200, widthGrow: 1 }
    ],
    onConfirm: (d: any) => {
      searchForm.shipseq = d.shipseq;
      searchForm.passseq = '';
    }
  });
  modalVisible.value = true;
}

async function openPassPopup() {
  if (!searchForm.fileno || !searchForm.shipseq) return vAlertError('PO No와 선적차수를 먼저 선택하세요.')

  console.log('🔍 [openPassPopup] Preparing Params:', { fileno: searchForm.fileno, shipseq: searchForm.shipseq })

  // 1. 프로시저 파라미터 구조와 100% 일치시킴
  Object.assign(modalProps, {
    title: '통관차수 선택',
    path: '/hs00/HS00_000S_STR',
    data: {
      gubun: 'F0',
      cmpycd: authStore.cmpycd,
      gbncd: '3',
      code: searchForm.fileno,   // @code: PO번호
      codenm: searchForm.shipseq, // @codenm: 선적차수
      etcval: ''
    },
    columns: [
      { title: '통관차수', field: 'passseq', width: 100, hozAlign: 'center' },
      { title: 'PO No', field: 'fileno', width: 150 },
      { title: '거래처', field: 'custnm', minWidth: 200, widthGrow: 1 }
    ],
    onConfirm: (d: any) => {
      searchForm.passseq = d.passseq;
    }
  });

  // 2. 비동기 반영 대기 후 팝업 표시
  await nextTick();
  modalVisible.value = true;
}

onMounted(() => {
  if (mainGridRef.value) {
    mainGrid = new Tabulator(mainGridRef.value, {
      layout: 'fitColumns', height: '100%', selectable: true,
      columnDefaults: { headerSort: false, headerHozAlign: "center", hozAlign: "center", vertAlign: "middle", minWidth: 40 },
      columns: [
        { title: "선택", formatter: "rowSelection", titleFormatter: "rowSelection", width: 60, hozAlign: "center" },
        { title: '품목코드', field: 'itemcd', width: 120, hozAlign: "center" },
        { title: '품목명', field: 'itemnm', minWidth: 200, widthGrow: 1, cssClass: 'fw-bold', hozAlign: 'left' },
        { title: '규격', field: 'itsize', minWidth: 150, widthGrow: 1, cssClass: 'fw-bold', hozAlign: 'left' },
        { title: '단위', field: 'unit', Width: 70,  cssClass: 'fw-bold', hozAlign: 'center' },
        { title: '입고일자', field: 'ioymd', width: 110, formatter: (c) => {
            const v = c.getValue(); return v && v.length === 8 ? `${v.substring(0,4)}-${v.substring(4,6)}-${v.substring(6,8)}` : v;
        }},
        { title: '입고수량', field: 'ioqty', hozAlign: 'right', width: 100, formatter: 'money', formatterParams: { precision: 0 } },
        { title: '최종 수입원가 (KRW)', field: 'ioamt', hozAlign: 'right', width: 150, editor: 'number', formatter: 'money', cssClass: 'bg-yellow fw-bold text-danger' }
      ]
    })
    mainGrid.on('rowSelectionChanged', () => {
      const selected = mainGrid?.getSelectedData() || []
      totalSummaryAmt.value = selected.reduce((acc, cur: any) => acc + (Number(cur.ioamt) || 0), 0)
    })
  }
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
</style>
