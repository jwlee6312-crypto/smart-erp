<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-file-earmark-bar-graph me-2 text-primary" style="font-size: 18px;"></i>
        구매정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        발주관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">발주현황 (HSIO080S)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-2">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchList">조회</button>
        <button class="btn-erp btn-print" @click="print">인쇄</button>
        <button class="btn-erp btn-excel" @click="excel"></button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- 🔍 [상단] 조회 필터 영역 (HSOD100U 디자인 패턴 1줄 적용) -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
              <col style="width: 8%" />
              <col style="width: 25%" />
              <col style="width: 8%" />
              <col style="width: 26%" />
              <col style="width: 8%" />
              <col style="width: 25%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light">발주부서</th>
                <td>
                  <div class="input-group input-group-sm w-100">
                    <input v-model="searchParam.deptcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
                    <input v-model="searchParam.deptnm" type="text" class="form-control" placeholder="부서 선택" @keyup.enter="fetchList" />
                    <button class="btn btn-outline-secondary px-2" @click="openHelp('DEPT')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="text-center bg-light">발주일자</th>
                <td class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                  <input v-model="searchParam.fromdt" type="date" class="form-control form-control-sm" style="width: 130px;" @change="fetchList" />
                  <span class="text-muted mx-1">~</span>
                  <input v-model="searchParam.todt" type="date" class="form-control form-control-sm" style="width: 130px;" @change="fetchList" />
                </td>
                <th class="text-center bg-light">거 래 처</th>
                <td>
                  <div class="input-group input-group-sm w-100">
                    <input v-model="searchParam.custcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
                    <input v-model="searchParam.custnm" type="text" class="form-control" placeholder="거래처 검색" @keyup.enter="fetchList" />
                    <button class="btn btn-outline-secondary px-2" @click="openHelp('CUST')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 📊 3. 메인 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white">
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="gridElement" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>
    </div>

    <!-- 💰 4. 하단 합계 라인 (Summary) -->
    <div class="erp-footer bg-dark text-white py-2 px-4 shadow-lg sticky-bottom flex-shrink-0">
      <div class="row align-items-center">
        <div class="col-md-3 small">조회 결과: <span class="fw-bold text-info">{{ activeItemCount }}</span> 건</div>
        <div class="col-md-9 text-end">
          <span class="me-4 small opacity-75">총 발주량: <span class="fw-bold text-white ms-1">{{ formatNumber(totals.qty) }}</span></span>
          <span class="me-4 small opacity-75">총 공급가: <span class="fw-bold text-white ms-1">{{ formatNumber(totals.amt) }}</span></span>
          <span class="me-4 small opacity-75">총 부가세: <span class="fw-bold text-white ms-1">{{ formatNumber(totals.vat) }}</span></span>
          <span class="fs-5 ms-2 fw-light">총 합계: <span class="fw-bold text-warning ms-2">{{ formatNumber(totals.sum) }}</span> 원</span>
        </div>
      </div>
    </div>

    <Modal v-model:visible="modalVisible" :modalProps="modalProps" />
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, computed, nextTick, onUnmounted } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import * as XLSX from 'xlsx'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { getDate } from '@/composables/useDate'
import type { ModalProps } from '@/types/modal'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { firstDay, today } = getDate()

const searchParam = reactive({
  deptcd: authStore.deptcd,
  deptnm: authStore.deptnm,
  fromdt: firstDay,
  todt: today,
  custcd: '',
  custnm: ''
})

const gridElement = ref<HTMLElement | null>(null);
const grid = ref<Tabulator | null>(null);
const activeItemCount = ref(0)
const totals = reactive({ qty: 0, amt: 0, vat: 0, sum: 0 })


const initGrid = () => {
  if (!gridElement.value) return
  grid.value = new Tabulator(gridElement.value, {
    layout: "fitColumns", height: "100%", placeholder: "조회된 데이터가 없습니다", selectable: true,
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "거래처", field: "custnm", minWidth: 200, widthGrow: 1, cssClass: "fw-bold text-dark cursor-pointer", cellClick: (e, cell) => {
        console.log('Navigate to HSIO090S with', cell.getData().custcd);
      }},
      { title: "구매요청자", field: "usernm", width: 100, hozAlign: "center" },
      { title: "주요품목", field: "itemnm", minWidth: 250, widthGrow: 2, formatter: (cell) => {
        const d = cell.getData();
        const cnt = Number(d.item_cnt) || 0;
        return cnt > 0 ? `${d.itemnm} 외 ${cnt}건` : d.itemnm;
      }},
      { title: "발주량", field: "balqty", width: 90, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "공급가", field: "balamt", width: 110, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "부가세", field: "balvat", width: 100, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "합계", field: "total", width: 120, hozAlign: "right", cssClass: "fw-bold", formatter: (cell) => {
        const d = cell.getData();
        return formatNumber((Number(d.balamt) || 0) + (Number(d.balvat) || 0));
      }},
      { title: "입고량", field: "inqty", width: 90, hozAlign: "right", formatter: "money", formatterParams: { precision: (cell:any) => cell.getData().qtypnt || 0 } },
      { title: "미입고량", field: "janqty", width: 90, hozAlign: "right", cssClass: "text-danger fw-bold", formatter: (cell) => {
        const d = cell.getData();
        return formatNumber((Number(d.balqty) || 0) - (Number(d.inqty) || 0));
      }}
    ]
  });
}

const updateTotals = (data: any[]) => {
  activeItemCount.value = data.length;
  totals.qty = data.reduce((acc, i) => acc + (Number(i.balqty) || 0), 0);
  totals.amt = data.reduce((acc, i) => acc + (Number(i.balamt) || 0), 0);
  totals.vat = data.reduce((acc, i) => acc + (Number(i.balvat) || 0), 0);
  totals.sum = totals.amt + totals.vat;
}

const modalVisible = ref(false);
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

async function fetchList() {
  if (!searchParam.deptcd) return vAlertError('발주부서를 선택하세요.');
  try {
    const res = await api.post('/hsio/HSIO_080S_STR', {
      cmpycd: authStore.cmpycd,
      deptcd: searchParam.deptcd,
      custcd: searchParam.custcd,
      custnm: searchParam.custnm,
      fromdt: searchParam.fromdt.replace(/-/g, ''),
      todt: searchParam.todt.replace(/-/g, '')
    });

    if (grid.value) {
      console.log(res.data);
      grid.value.setData(res.data);
      updateTotals(res.data);
      vAlert('조회되었습니다.');
    }
  } catch (e) { vAlertError('조회 실패') }
}

function initialize() {
  resetForm(searchParam);
  Object.assign(searchParam, {
    deptcd: authStore.deptcd,
    deptnm: authStore.deptnm,
    fromdt: firstDay,
    todt: today
  });
  grid.value?.clearData();
  updateTotals([]);
}

function openHelp(type: string) {
  const commonProps = { path: '/ha00/HA00_00P_STR', cmpycd: authStore.cmpycd };
  if (type === 'DEPT') {
    Object.assign(modalProps, {
      title: '부서 선택', ...commonProps,
      data: { gubun: 'D0', cmpycd: authStore.cmpycd },
      columns: [
        { title: '코드', field: 'deptcd', width: 80 },
        { title: '부서명', field: 'deptnm', width: 180 },
        { title: '상위부서', field: 'Udeptnm', width: 150 }
      ],
      onConfirm: (d: any) => { searchParam.deptcd = d.deptcd; searchParam.deptnm = d.deptnm }
    });
  } else if (type === 'CUST') {
    Object.assign(modalProps, {
      title: '거래처 선택', ...commonProps,
      data: { gubun: 'C4', cmpycd: authStore.cmpycd },
      columns: [
        { title: '코드', field: 'custcd', width: 70 },
        { title: '거래처명', field: 'custnm', width: 180 },
        { title: '사업자번호', field: 'custno', width: 110 },
        { title: '대표자', field: 'bossnm', width: 80 },
        { title: '주소', field: 'address', minWidth: 250 }
      ],
      onConfirm: (d: any) => { searchParam.custcd = d.custcd; searchParam.custnm = d.custnm }
    });
  }
  modalVisible.value = true;
}

const formatNumber = (val: any) => new Intl.NumberFormat().format(Number(val) || 0);
const print = () => vAlert('출력 기능을 준비 중입니다.');
const excel = () => grid.value?.download("xlsx", "발주현황.xlsx", { sheetName: "발주현황" });

onUnmounted(() => {
  if (grid.value) grid.value.destroy();
});

onMounted(() => {
  if (typeof window !== 'undefined') (window as any).XLSX = XLSX;
  nextTick(() => {
    initGrid();
    fetchList();
  });
})
</script>

<style scoped>
/* 🎨 HSOD100U 디자인 표준 이식 */
.main-content-wrapper {
  padding-bottom: 0vh !important;
}

/* 폼 셀 높이 32px 고정 및 정렬 */
.erp-table-dense th, .erp-table-dense td {
  height: 32px !important;
  padding: 0 8px !important;
  font-size: 12px;
  vertical-align: middle;
  border: 1px solid #dee2e6;
}

.erp-table-dense .form-control,
.erp-table-dense .form-select,
.erp-table-dense .btn {
  height: 26px !important;
  font-size: 12px !important;
  border-radius: 2px;
}

.erp-table-dense th {
  font-weight: 600;
  color: #495057;
}

.erp-footer {
  height: 48px;
  display: flex;
  align-items: center;
}
</style>