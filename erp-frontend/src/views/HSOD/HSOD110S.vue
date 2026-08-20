<!--
	=============================================================
	프로그램명	: 주문현황 (Order Status)
	작성일자	: 25.02.24
	작성자	    : AI Assistant
	설명        : ASP(HSOD_110S) 기반 주문현황 조회 화면 (CSS 표준화 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-card-list me-2 text-primary" style="font-size: 18px;"></i>
        영업정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        주문관리 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        <span class="text-primary fw-bolder">주문현황 (HSOD110S)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-outline-success border" style="font-size: 12px;" @click="exportExcel">엑셀</button>
      </div>
    </div>

    <!-- 🔍 2. 상단 조회 필터 (HSOD100U 표준 테이블 레이아웃 적용) -->
    <div class="p-2 pb-0 flex-shrink-0 bg-light">
      <div class="card border shadow-sm overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
              <col style="width: 10%" /><col style="width: 23%" />
              <col style="width: 10%" /><col style="width: 23%" />
              <col style="width: 10%" /><col style="width: 24%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light required">주문부서</th>
                <td>
                  <div class="input-group input-group-sm">
                    <input v-model="searchForm.deptcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
                    <input v-model="searchForm.deptnm" type="text" class="form-control" placeholder="부서 선택" @keyup.enter="handleOpenHelp('DEPT')" />
                    <button class="btn btn-outline-secondary px-2" @click="handleOpenHelp('DEPT')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="text-center bg-light required">주문일자</th>
                <td>
                  <div class="d-flex align-items-center gap-1">
                    <DateForm v-model:fromdt="searchForm.fromdt" v-model:todt="searchForm.todt" />
                  </div>
                </td>
                <th class="text-center bg-light">거&nbsp;&nbsp;래&nbsp;&nbsp;처</th>
                <td>
                  <div class="d-flex align-items-center gap-1">
                    <div class="input-group input-group-sm">
                      <input v-model="searchForm.custnmfr" type="text" class="form-control" placeholder="From" @keyup.enter="handleOpenHelp('CUST_FR')" />
                      <button class="btn btn-outline-secondary px-1" @click="handleOpenHelp('CUST_FR')"><i class="bi bi-search"></i></button>
                    </div>
                    <span class="text-muted">~</span>
                    <div class="input-group input-group-sm">
                      <input v-model="searchForm.custnmto" type="text" class="form-control" placeholder="To" @keyup.enter="handleOpenHelp('CUST_TO')" />
                      <button class="btn btn-outline-secondary px-1" @click="handleOpenHelp('CUST_TO')"><i class="bi bi-search"></i></button>
                    </div>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- 📊 3. 메인 그리드 영역 (표준화 적용) -->
    <div class="flex-grow-1 overflow-hidden p-2">
      <div class="card border shadow-sm h-100 d-flex flex-column bg-white">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
          <span class="fw-bold small text-dark d-flex align-items-center">
            <i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i> 주문 상세 현황
          </span>
          <span class="text-muted small">※ 주문번호를 클릭하면 상세 등록 화면으로 이동합니다.</span>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column" style="min-height: 0;">
          <div ref="gridElement" class="tabulator-full-height" />
        </div>
      </div>
    </div>

    <Modal v-model:visible="modalVisible" :modalProps="modalProps" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { getDate } from '@/composables/useDate'
import { addDynamicRoute as add_dynamic_route } from '@/router/dynamicRoute'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import DateForm from '@/components/DateForm.vue'
import type { ModalProps } from '@/types/modal'

const authStore = useAuthStore()
const router = useRouter()
const { firstDay, today } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

const searchForm = reactive<any>({
  deptcd: authStore.deptcd,
  deptnm: authStore.deptnm,
  fromdt: firstDay,
  todt: today,
  custcdfr: '', custnmfr: '',
  custcdto: '', custnmto: ''
})

const gridElement = ref<HTMLDivElement | null>(null);
let grid: Tabulator | null = null;

const handleOpenHelp = (type: string) => {
  if (type === 'DEPT') {
    Object.assign(modalProps, {
      title: '주문부서 선택',
      path: '/ha00/HA00_00P_STR',
      data: { gubun: 'D0', cmpycd: authStore.cmpycd, code: '', codenm: searchForm.deptnm, remark: '' },
      columns: [
        { title: '부서코드', field: 'deptcd', width: 100, hozAlign: 'center' },
        { title: '부서명', field: 'deptnm', width: 200 }
      ],
      onConfirm: (d: any) => { searchForm.deptcd = d.deptcd; searchForm.deptnm = d.deptnm }
    })
    modalVisible.value = true
  } else if (type === 'CUST_FR') {
    Object.assign(modalProps, {
      title: '거래처 선택(시작)',
      path: '/ha00/HA00_00P_STR',
      data: { gubun: 'C4', cmpycd: authStore.cmpycd, code: searchForm.custnmfr, codenm: '', remark: '' },
      columns: [
        { title: '코드', field: 'custcd', width: 100, hozAlign: 'center' },
        { title: '거래처명', field: 'custnm', width: 200 }
      ],
      onConfirm: (d: any) => { searchForm.custcdfr = d.custcd; searchForm.custnmfr = d.custnm }
    })
    modalVisible.value = true
  } else if (type === 'CUST_TO') {
    Object.assign(modalProps, {
      title: '거래처 선택(종료)',
      path: '/ha00/HA00_00P_STR',
      data: { gubun: 'C4', cmpycd: authStore.cmpycd, code: searchForm.custnmto, codenm: '', remark: '' },
      columns: [
        { title: '코드', field: 'custcd', width: 100, hozAlign: 'center' },
        { title: '거래처명', field: 'custnm', width: 200 }
      ],
      onConfirm: (d: any) => { searchForm.custcdto = d.custcd; searchForm.custnmto = d.custnm }
    })
    modalVisible.value = true
  }
}

async function search() {
  if (!searchForm.deptcd) return vAlertError('주문부서를 선택해 주십시오.');

  try {
    const res = await api.post('/hsod/HSOD_110S_STR', {
      cmpycd: authStore.cmpycd,
      deptcd: searchForm.deptcd,
      custcdfr: searchForm.custcdfr,
      custcdto: searchForm.custcdto,
      fromdt: searchForm.fromdt.replace(/-/g, ''), // 🚀 fromdt -> fromdt로 수정
      todt: searchForm.todt.replace(/-/g, '')    // 🚀 todt -> todt로 수정
    });

    const mapped = res.data.map((i: any) => ({
      ...i,
      ordno_full: `${i.ordym}-${i.ordno}`
    }));

    grid?.setData(mapped);
    vAlert('조회되었습니다.');
  } catch (e) {
    vAlertError('조회 중 오류가 발생했습니다.');
  }
}

function initialize() {
  resetForm(searchForm);
  searchForm.deptcd = authStore.deptcd;
  searchForm.deptnm = authStore.deptnm;
  searchForm.fromdt = firstDay
  searchForm.todt = today
  grid?.clearData();
}

function exportExcel() {
  grid?.download("xlsx", "주문현황.xlsx", { title: "주문현황" });
}

onMounted(() => {
  if (gridElement.value) {
    grid = new Tabulator(gridElement.value, {
      layout: 'fitColumns',
      height: '100%',
      placeholder: "조회된 데이터가 없습니다.",
      columnDefaults: {
        headerSort: false,
        headerHozAlign: "center",
        hozAlign: 'right', // 🚀 기본값 우측 정렬
        vertAlign: 'middle',
        minWidth: 100
      },
      columns: [
        { title: "No", formatter: "rownum", width: 50, hozAlign: "center" },
        { title: "주문일", field: "ordymd", width: 110, hozAlign: "center", formatter: (c) => formatDate(c.getValue()) },
        { title: "주문번호", field: "ordno_full", width: 120, hozAlign: "center", cssClass: "text-primary cursor-pointer",
          cellClick: (e, cell) => {
            const d = cell.getRow().getData();
            const pgmid = 'HSOD100U';
            add_dynamic_route(pgmid, '주문등록', 'HSOD');
            router.push({ path: `/${pgmid}`, query: { ordym: d.ordym, ordno: d.ordno } });
          }
        },
        { title: "거래처", field: "custnm", minWidth: 180, widthGrow: 1, hozAlign: "left" },
        { title: "품목명", field: "itemnm", minWidth: 200, widthGrow: 2, hozAlign: "left" },
        { title: "규격", field: "itsize", width: 150, hozAlign: "left" },
        { title: "단위", field: "unit", width: 60, hozAlign: "center" },
        { title: "수량", field: "ordqty", width: 100, formatter: "money", formatterParams: { precision: 0 }, bottomCalc: "sum" },
        { title: "공급가", field: "ordamt", width: 120, formatter: "money", formatterParams: { precision: 0 }, bottomCalc: "sum" },
        { title: "부가세", field: "ordvat", width: 110, formatter: "money", formatterParams: { precision: 0 }, bottomCalc: "sum" },
        { title: "합계", field: "amtsum", width: 130, formatter: "money", cssClass: "bg-light",
          mutatorData: (v, d) => Number(d.ordamt || 0) + Number(d.ordvat || 0),
          bottomCalc: "sum"
        },
        { title: "납품량", field: "ioqty", width: 110, formatter: "money", formatterParams: { precision: 0 } }
      ]
    })
  }
  nextTick(() => search());
})

const formatDate = (v: any) => v && v.length === 8 ? `${v.substring(0,4)}-${v.substring(4,6)}-${v.substring(6,8)}` : v;
</script>

<style scoped>
.tabulator-full-height { width: 100% !important; background-color: #fff; border-bottom: 3px solid #005a9f !important; }
.erp-table-dense th, .erp-table-dense td {
  height: 32px !important; padding: 0 8px !important; font-size: 12px; vertical-align: middle; border: 1px solid #dee2e6;
}
.erp-table-dense th { font-weight: 600; color: #495057; }
</style>
