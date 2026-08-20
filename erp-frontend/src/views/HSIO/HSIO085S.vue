<!--
	=============================================================
	프로그램명	: 구매요청현황 (HSIO085S)
	작성일자	: 2025.08.05
	설명        : ASP(HSIO_085S.asp) 원본 로직 100% 복구 (단일 그리드 구조)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom bg-white py-2 px-3 sticky-top shadow-sm">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-file-earmark-bar-graph me-2 text-primary" style="font-size: 18px;"></i>
        구매정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        구매관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">구매요청현황 (HSIO085S)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-2">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchList">조회</button>
        <button class="btn-erp btn-outline-secondary" @click="print('Print')">인쇄</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- 🔍 [상단] 조회 필터 영역 (ASP 필터 동일 구현) -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
              <col style="width: 10%" /><col style="width: 25%" />
              <col style="width: 10%" /><col style="width: 25%" />
              <col style="width: 10%" /><col style="width: 20%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light">요청부서</th>
                <td>
                  <div class="input-group input-group-sm w-100">
                    <input v-model="searchForm.deptcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
                    <input v-model="searchForm.deptnm" type="text" class="form-control" placeholder="부서 선택" @keyup.enter="fetchList" />
                    <button class="btn btn-outline-secondary px-2" @click="openHelp('DEPT')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="text-center bg-light">요청일자</th>
                <td class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                  <input v-model="searchForm.fromdt" type="date" class="form-control form-control-sm" />
                  <span class="text-muted mx-1">~</span>
                  <input v-model="searchForm.todt" type="date" class="form-control form-control-sm" />
                </td>
                <th class="text-center bg-light">거 래 처</th>
                <td>
                  <div class="input-group input-group-sm w-100">
                    <input v-model="searchForm.custcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
                    <input v-model="searchForm.custnm" type="text" class="form-control" placeholder="거래처 검색" @keyup.enter="fetchList" />
                    <button class="btn btn-outline-secondary px-2" @click="openHelp('CUST')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 📊 [하단] 메인 그리드 (ASP 원본 단일 그리드) -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark"><i class="bi bi-list-ul me-1 text-primary"></i> 구매요청 내역 리스트</span>
          <span class="badge bg-primary-subtle text-primary border-0 rounded-pill px-2" style="font-size: 10px;">{{ poListCount }}건</span>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="mainGridRef" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>
    </div>

    <!-- 💰 3. 하단 합계 라인 (Summary) -->
    <div class="erp-footer bg-dark text-white py-2 px-4 shadow-lg sticky-bottom flex-shrink-0" style="height: 48px;">
      <div class="d-flex justify-content-between align-items-center w-100">
        <div class="small">조회 건수: <span class="fw-bold text-info">{{ poListCount }}</span> 건</div>
        <div class="d-flex gap-4">
          <div class="d-flex align-items-center">
            <span class="small opacity-75 me-2">총 요청량:</span>
            <span class="fw-bold text-white fs-6">{{ formatNumber(totals.qty) }}</span>
          </div>
          <div class="d-flex align-items-center border-start ps-4">
            <span class="small opacity-75 me-2">총 금액:</span>
            <span class="fw-bold text-white fs-6">{{ formatNumber(totals.amt) }}</span>
          </div>
          <div class="d-flex align-items-center border-start ps-4">
            <span class="small opacity-75 me-2">총 발주량:</span>
            <span class="fw-bold text-info fs-6">{{ formatNumber(totals.balqty) }}</span>
          </div>
          <div class="d-flex align-items-center border-start ps-4">
            <span class="small opacity-75 me-2">미발주 합계:</span>
            <span class="fw-bold text-warning fs-5">{{ formatNumber(totals.qty - totals.balqty) }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick, onUnmounted } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { getDate } from '@/composables/useDate'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import type { ModalProps } from '@/types/modal'

const authStore = useAuthStore()
const { firstDay, today } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

// 🔍 [1] 상태 관리
const searchForm = reactive({
  deptcd: authStore.deptcd, deptnm: authStore.deptnm,
  fromdt: firstDay, todt: today, custcd: '', custnm: ''
})

const mainGridRef = ref<HTMLDivElement | null>(null);
let mainGrid: Tabulator | null = null;
const poListCount = ref(0);
const totals = reactive({ qty: 0, amt: 0, balqty: 0 })

// 📊 [2] 그리드 초기화 (ASP 컬럼 명세 100% 반영)
const initGrid = () => {
  if (!mainGridRef.value) return

  mainGrid = new Tabulator(mainGridRef.value, {
    layout: 'fitColumns', height: '100%', placeholder: "데이터 없음",
    columnDefaults: { headerSort: false, headerHozAlign: "center", vertAlign: "middle", hozAlign: 'center' },
    columns: [
      { title: "No", formatter: "rownum", width: 40 },
      { title: "거래처", field: "custnm", minWidth: 180, hozAlign: "left", cssClass: "fw-bold text-dark" },
      { title: "구매요청번호", field: "reqno_full", width: 120, cssClass: "text-primary",
        formatter: (c) => { const d = c.getData(); return d.reqym && d.reqno ? `${d.reqym}-${d.reqno}` : '' }
      },
      { title: "발주번호", field: "balno_full", width: 120,
        formatter: (c) => { const d = c.getData(); return d.balym && d.balno ? `${d.balym}-${d.balno}` : '' }
      },
      { title: "구매요청자", field: "usernm", width: 100 },
      { title: "주요품목", field: "itemnm", minWidth: 250, hozAlign: "left",
        formatter: (c) => {
          const d = c.getData();
          const cnt = Number(d.item_cnt) || 0;
          return cnt > 0 ? `${d.itemnm} 외 ${cnt}건` : d.itemnm;
        }
      },
      { title: "요청수량", field: "reqqty", width: 90, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "금액", field: "reqamt", width: 110, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "발주량", field: "balqty", width: 90, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "text-primary fw-bold" },
      { title: "미발주량", field: "janqty", width: 90, hozAlign: "right", cssClass: "text-danger fw-bold",
        formatter: (c) => {
          const d = c.getData();
          return formatNumber(Number(d.reqqty || 0) - Number(d.balqty || 0));
        }
      }
    ]
  });
}

// 🌐 [3] 데이터 통신
async function fetchList() {
  try {
    const res = await api.post('/hsio/HSIO_085S_STR', {
      cmpycd: authStore.cmpycd,
      deptcd: searchForm.deptcd,
      custcd: searchForm.custcd,
      custnm: searchForm.custnm,
      fromdt: searchForm.fromdt.replace(/-/g, ''),
      todt: searchForm.todt.replace(/-/g, '')
    });

    // 💡 [표준화] res.data는 이미 axios.ts에 의해 소문자 배열임이 보장됨
    const data = res.data || [];
    mainGrid?.setData(data);
    poListCount.value = data.length;

    // 합계 계산
    totals.qty = data.reduce((acc: number, cur: any) => acc + (Number(cur.reqqty) || 0), 0);
    totals.amt = data.reduce((acc: number, cur: any) => acc + (Number(cur.reqamt) || 0), 0);
    totals.balqty = data.reduce((acc: number, cur: any) => acc + (Number(cur.balqty) || 0), 0);

    vAlert('조회되었습니다.');
  } catch (e) { vAlertError('조회 실패') }
}

// 🛠 [4] 기타 기능 및 팝업
function initialize() {
  resetForm(searchForm);
  searchForm.deptcd = authStore.deptcd; searchForm.deptnm = authStore.deptnm;
  searchForm.fromdt = firstDay; searchForm.todt = today;
  mainGrid?.clearData();
  poListCount.value = 0;
  Object.assign(totals, { qty: 0, amt: 0, balqty: 0 });
}

const modalVisible = ref(false);
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function openHelp(type: string) {
  const commonProps = { path: '/ha00/HA00_00P_STR', cmpycd: authStore.cmpycd };
  if (type === 'DEPT') {
    Object.assign(modalProps, {
      title: '부서 선택', ...commonProps, data: { gubun: 'D0' },
      columns: [{ title: '코드', field: 'deptcd', width: 80 }, { title: '부서명', field: 'deptnm', width: 200 }],
      onConfirm: (d: any) => { searchForm.deptcd = d.deptcd; searchForm.deptnm = d.deptnm }
    });
  } else if (type === 'CUST') {
    Object.assign(modalProps, {
      title: '거래처 선택', ...commonProps, data: { gubun: 'C4' },
      columns: [{ title: '코드', field: 'custcd', width: 80 }, { title: '거래처명', field: 'custnm', width: 200 }],
      onConfirm: (d: any) => { searchForm.custcd = d.custcd; searchForm.custnm = d.custnm }
    });
  }
  modalVisible.value = true;
}

const formatNumber = (val: any) => Number(val || 0).toLocaleString();
const print = (prtgu: string) => {
    const p = searchForm;
    window.open(`HSIO_085P.asp?DEPTCD=${p.deptcd}&DEPTNM=${p.deptnm}&CUSTCD=${p.custcd}&CUSTNM=${p.custnm}&FRYMD=${p.fromdt}&TOYMD=${p.todt}&PRTGU=${prtgu}`, 'print', 'width=700,height=600,scrollbars=yes');
}

onMounted(() => { nextTick(() => { initGrid(); fetchList(); }); })
onUnmounted(() => { mainGrid?.destroy(); })
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; border-bottom: 3px solid #005a9f !important; }
.erp-footer { height: 48px; display: flex; align-items: center; }
</style>
