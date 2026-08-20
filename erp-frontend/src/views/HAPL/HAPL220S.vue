<!--
	=============================================================
	프로그램명	: 품목별 손익현황 (HAPL220S)
	작성일자	: 2025.02.24
	설명        : 부서/거래처별 품목별 매출, 원가, 판관비 및 이익 현황 조회
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-box-seam-fill me-2 text-primary" style="font-size: 18px;"></i>
        관리손익 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        현황조회 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">품목별 손익현황 (HAPL220S)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-2">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-print" @click="handlePrint('1')">인쇄</button>
        <button class="btn-erp btn-excel" @click="handlePrint('2')">엑셀</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 조회 필터 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
                <col style="width: 8%" /><col style="width: 25%" />
                <col style="width: 8%" /><col style="width: 25%" />
                <col style="width: 8%" /><col style="width: 26%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light required">부서</th>
                <td>
                  <div class="input-group input-group-sm">
                    <input v-model="searchForm.deptnm" class="form-control" placeholder="부서 선택" readonly @click="handleOpenHelp('DEPT')" />
                    <button class="btn btn-outline-secondary" @click="handleOpenHelp('DEPT')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="text-center bg-light required">조회기간</th>
                <td>
                  <div class="d-flex align-items-center gap-1 px-1">
                    <select v-model="searchForm.yyyy" class="form-select form-select-sm" style="width: 90px;">
                      <option v-for="y in yearOptions" :key="y" :value="y">{{ y }}년</option>
                    </select>
                    <select v-model="searchForm.fmm" class="form-select form-select-sm" style="width: 70px;">
                      <option v-for="m in monthOptions" :key="m" :value="m">{{ m }}월</option>
                    </select>
                    <span class="mx-1 small">~</span>
                    <select v-model="searchForm.tmm" class="form-select form-select-sm" style="width: 70px;">
                      <option v-for="m in monthOptions" :key="m" :value="m">{{ m }}월</option>
                    </select>
                  </div>
                </td>
                <th class="text-center bg-light">정렬구분</th>
                <td>
                  <select v-model="searchForm.ordgbn" class="form-select form-select-sm w-75">
                    <option value="00">거래처 순</option>
                    <option value="10">매출액 순</option>
                    <option value="20">매총이익 순</option>
                    <option value="30">매총이익율 순</option>
                    <option value="40">영업이익 순</option>
                    <option value="50">영업이익율 순</option>
                    <option value="60">경상이익 순</option>
                    <option value="70">경상이익율 순</option>
                  </select>
                </td>
              </tr>
              <tr>
                <th class="text-center bg-light">거래처</th>
                <td>
                  <div class="input-group input-group-sm">
                    <input v-model="searchForm.custnm" class="form-control" placeholder="전체 거래처" readonly @click="handleOpenHelp('CUST')" />
                    <button class="btn btn-outline-secondary" @click="handleOpenHelp('CUST')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <td colspan="4" class="bg-white border-start-0"></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column grid-container-right">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>품목 손익 리스트</span>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column" style="min-height: 0;">
          <div ref="mainGridRef" class="tabulator-full-height" />
        </div>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import type { ModalProps } from '@/types/modal'

const authStore = useAuthStore()
const route = useRoute()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()

const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

// [1] 데이터 모델링
const currentYear = new Date().getFullYear()
const currentMonth = (new Date().getMonth() + 1).toString().padStart(2, '0')
const yearOptions = Array.from({ length: 6 }, (_, i) => String(currentYear - i))
const monthOptions = Array.from({ length: 12 }, (_, i) => String(i + 1).padStart(2, '0'))

const searchForm = reactive({
  deptcd: (route.query.deptcd as string) || authStore.deptcd,
  deptnm: '',
  custcd: (route.query.custcd as string) || '',
  custnm: '',
  yyyy: (route.query.yyyy as string) || String(currentYear),
  fmm: (route.query.fmm as string) || currentMonth,
  tmm: (route.query.tmm as string) || currentMonth,
  ordgbn: '00'
})

const mainGridRef = ref<HTMLElement | null>(null)
let mainGrid: Tabulator | null = null

// [2] 그리드 초기화
const initGrids = () => {
  if (!mainGridRef.value) return

  mainGrid = new Tabulator(mainGridRef.value, {
    layout: "fitColumns",
    height: "100%",
    placeholder: "조회된 데이터가 없습니다.",
    columnDefaults: {
      headerHozAlign: 'center',
      headerSort: false,
      hozAlign: 'right',
      vertAlign: 'middle'
    },
    columnCalcs: "table",
    columns: [
      { title: "품목명", field: "itemnm", widthGrow: 1.5, frozen: true, cssClass: "fw-bold text-dark", bottomCalc: () => "합계" },
      { title: "수량", field: "salsqty", hozAlign: "right", width: 80, formatter: "money", formatterParams: { precision: 0 }, bottomCalc: "sum" },
      { title: "매출액", field: "salsamt", hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, bottomCalc: "sum" },
      { title: "매출원가", field: "salscost", hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, bottomCalc: "sum" },
      { title: "매총이익", field: "mprofit", hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, bottomCalc: "sum", cssClass: "bg-light-blue" },
      { title: "이익율(%)", field: "m_rate", hozAlign: "right", width: 80,
        formatter: (c) => Number(c.getValue() || 0).toFixed(2) + '%',
        bottomCalc: (values, data) => {
            const sumAmt = data.reduce((a: any, b: any) => a + (Number(b.salsamt) || 0), 0);
            const sumProfit = data.reduce((a: any, b: any) => a + (Number(b.mprofit) || 0), 0);
            return sumAmt === 0 ? '0.00%' : ((sumProfit / sumAmt) * 100).toFixed(2) + '%';
        }
      },
      { title: "판관비", field: "admin_cost", hozAlign: "right", formatter: "money", formatterParams: { precision: 0 },
        bottomCalc: "sum"
      },
      { title: "영업이익", field: "yyprofit", hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, bottomCalc: "sum", cssClass: "bg-light-green" },
      { title: "이익율(%)", field: "y_rate", hozAlign: "right", width: 80,
        formatter: (c) => Number(c.getValue() || 0).toFixed(2) + '%',
        bottomCalc: (values, data) => {
            const sumAmt = data.reduce((a: any, b: any) => a + (Number(b.salsamt) || 0), 0);
            const sumProfit = data.reduce((a: any, b: any) => a + (Number(b.yyprofit) || 0), 0);
            return sumAmt === 0 ? '0.00%' : ((sumProfit / sumAmt) * 100).toFixed(2) + '%';
        }
      },
      { title: "경상이익", field: "gprofit", hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, bottomCalc: "sum", cssClass: "text-primary fw-bold bg-light" },
      { title: "이익율(%)", field: "g_rate", hozAlign: "right", width: 80,
        formatter: (c) => Number(c.getValue() || 0).toFixed(2) + '%',
        bottomCalc: (values, data) => {
            const sumAmt = data.reduce((a: any, b: any) => a + (Number(b.salsamt) || 0), 0);
            const sumProfit = data.reduce((a: any, b: any) => a + (Number(b.gprofit) || 0), 0);
            return sumAmt === 0 ? '0.00%' : ((sumProfit / sumAmt) * 100).toFixed(2) + '%';
        }
      }
    ],
  });
}

// [3] 비즈니스 로직
const fetchInitialNames = async () => {
    try {
        if (searchForm.deptcd) {
            const dRes = await api.post('/ha00/HA00_010S_STR', { cmpycd: authStore.cmpycd, col0: 'D0', col2: searchForm.deptcd });
            if (dRes.data?.length) searchForm.deptnm = dRes.data[0].col0;
        }
        if (searchForm.custcd) {
            const cRes = await api.post('/ha00/HA00_010S_STR', { cmpycd: authStore.cmpycd, col0: 'C0', col2: searchForm.custcd });
            if (cRes.data?.length) searchForm.custnm = cRes.data[0].col0;
        }
    } catch (e) {}
}

async function search() {
  if (!searchForm.deptcd) return vAlertError('부서를 선택하세요.');
  try {
    const res = await api.post('/hapl/HAPL_220S_STR', {
      cmpycd: authStore.cmpycd,
      deptcd: searchForm.deptcd,
      custcd: searchForm.custcd,
      ymfr: searchForm.yyyy + searchForm.fmm,
      ymto: searchForm.yyyy + searchForm.tmm,
      ordgbn: searchForm.ordgbn
    });

    const list = (res.data || []).map((i: any) => {
        return {
            ...i,
            admin_cost: Number(i.dpcost || 0) + Number(i.ipcost || 0),
            m_rate: i.salsamt ? (i.mprofit / i.salsamt * 100) : 0,
            y_rate: i.salsamt ? (i.yyprofit / i.salsamt * 100) : 0,
            g_rate: i.salsamt ? (i.gprofit / i.salsamt * 100) : 0
        };
    });

    mainGrid?.setData(list);
    vAlert('조회되었습니다.');
  } catch (e) { vAlertError('조회 실패'); }
}

const handleOpenHelp = (type: string) => {
  if (type === 'DEPT') {
    Object.assign(modalProps, {
      title: '부서 선택',
      path: '/ha00/HA00_00P_STR',
      data: { gubun: 'D0', cmpycd: authStore.cmpycd, code: '', codenm: searchForm.deptnm, remark: '' },
      columns: [
        { title: '부서코드', field: 'deptcd', width: 100, hozAlign: 'center' },
        { title: '부서명', field: 'deptnm', width: 200 }
      ],
      onConfirm: (d: any) => { searchForm.deptcd = d.deptcd; searchForm.deptnm = d.deptnm }
    })
    modalVisible.value = true
  } else if (type === 'CUST') {
    Object.assign(modalProps, {
      title: '거래처 선택',
      path: '/ha00/HA00_00P_STR',
      data: { gubun: 'C0', cmpycd: authStore.cmpycd, code: '', codenm: searchForm.custnm, remark: '' },
      columns: [
        { title: '거래처코드', field: 'custcd', width: 100, hozAlign: 'center' },
        { title: '거래처명', field: 'custnm', width: 200 }
      ],
      onConfirm: (d: any) => { searchForm.custcd = d.custcd; searchForm.custnm = d.custnm }
    })
    modalVisible.value = true
  }
}

const handlePrint = (prtgu: string) => {
    if (!searchForm.deptcd) return vAlertError('부서를 선택하세요.');
    const params = new URLSearchParams({
        deptcd: searchForm.deptcd,
        custcd: searchForm.custcd,
        ymfr: searchForm.yyyy + searchForm.fmm,
        ymto: searchForm.yyyy + searchForm.tmm,
        ordgbn: searchForm.ordgbn,
        prtgu: prtgu
    }).toString();
    window.open(`/hapl/HAPL_220P?${params}`, 'ItemProfitPrint', 'width=1000,height=800,scrollbars=yes');
}

const initialize = () => {
    searchForm.deptcd = authStore.deptcd;
    searchForm.deptnm = authStore.deptnm;
    searchForm.custcd = '';
    searchForm.custnm = '';
    searchForm.yyyy = String(currentYear);
    searchForm.fmm = currentMonth;
    searchForm.tmm = currentMonth;
    searchForm.ordgbn = '00';
    mainGrid?.clearData();
    fetchInitialNames();
}

onMounted(() => {
  nextTick(async () => {
    initGrids();
    await fetchInitialNames();
    if (searchForm.deptcd) search();
  });
})
</script>

<style scoped>
.tabulator-full-height { width: 100% !important; background-color: #fff; border-bottom: 3px solid #005a9f !important; }
.bg-light-blue { background-color: #e3f2fd !important; }
.bg-light-green { background-color: #f1f8e9 !important; }
</style>
