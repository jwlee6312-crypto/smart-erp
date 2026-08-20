<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-file-earmark-ruled me-2 text-primary" style="font-size: 18px;"></i>
        구매정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        발주관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">발주상세현황 (HSIO082S)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-2">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchList">조회</button>
        <button class="btn-erp btn-print" @click="print">인쇄</button>
        <button class="btn-erp btn-excel" @click="excel">엑셀</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- 🔍 [상단] 조회 필터 영역 (HSOD100U 디자인 패턴 2줄 적용) -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
              <col style="width: 8%" />
              <col style="width: 25%" />
              <col style="width: 8%" />
              <col style="width: 25%" />
              <col style="width: 8%" />
              <col style="width: 26%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light">발주일자</th>
                <td class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                  <input v-model="searchParam.fromdt" type="date" class="form-control form-control-sm" style="width: 130px;" />
                  <span class="text-muted">~</span>
                  <input v-model="searchParam.todt" type="date" class="form-control form-control-sm" style="width: 130px;" />
                </td>
                <th class="text-center bg-light">거 래 처</th>
                <td>
                  <div class="input-group input-group-sm w-100">
                    <input v-model="searchParam.custcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
                    <input v-model="searchParam.custnm" type="text" class="form-control" placeholder="거래처 선택" @keyup.enter="fetchList" />
                    <button class="btn btn-outline-secondary px-2" @click="openHelp('CUST')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="text-center bg-light">주문번호</th>
                <td>
                  <div class="input-group input-group-sm w-100">
                    <input v-model="searchParam.ordym" type="month" class="form-control text-center bg-light" style="max-width: 100px;" />
                    <input v-model="searchParam.ordno" type="text" class="form-control text-center" placeholder="0000" />
                    <button class="btn btn-outline-secondary px-2" @click="openHelp('ORDER')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
              </tr>
              <tr>
                <th class="text-center bg-light">담 당 자</th>
                <td>
                  <select v-model="searchParam.userid" class="form-select form-select-sm w-100">
                    <option value="">-- 전체 --</option>
                    <option v-for="item in empOptions" :key="item.userid" :value="item.userid">{{ item.usernm }}</option>
                  </select>
                </td>
                <th class="text-center bg-light">입고여부</th>
                <td>
                  <select v-model="searchParam.ipgoyn" class="form-select form-select-sm w-50">
                    <option value="Y">전체</option>
                    <option value="N">미입고</option>
                  </select>
                </td>
                <th class="text-center bg-light">특이사항</th>
                <td>
                  <input v-model="searchParam.remark" type="text" class="form-control form-control-sm w-100" placeholder="특기사항 검색어 입력" @keyup.enter="fetchList" />
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
import type { ModalProps } from '@/types/modal'
import { useRouter } from 'vue-router'

const authStore = useAuthStore()
const router = useRouter()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

const now = new Date();
const firstDay = new Date(now.getFullYear(), now.getMonth(), 1);
const formatDateStr = (date: Date) => date.toISOString().substring(0, 10);

const searchParam = reactive({
  fromdt: formatDateStr(firstDay),
  todt: formatDateStr(now),
  custcd: '',
  custnm: '',
  ordym: '',
  ordno: '',
  userid: '',
  ipgoyn: 'Y',
  remark: ''
})

const gridElement = ref<HTMLElement | null>(null);
const grid = ref<Tabulator | null>(null);
const activeItemCount = ref(0)
const totals = reactive({ qty: 0, amt: 0, vat: 0, inqty: 0 })
const empOptions = ref<any[]>([])

const initGrid = () => {
  if (!gridElement.value) return
  grid.value = new Tabulator(gridElement.value, {
    layout: "fitColumns", height: "100%", placeholder: "조회된 데이터가 없습니다", selectable: true,
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "발주일자", field: "balymd", width: 100, hozAlign: "center", formatter: (c) => formatDate(c.getValue()) },
      { title: "거래처", field: "custnm", minWidth: 150, cssClass: "fw-bold text-dark cursor-pointer", cellClick: (e, cell) => {
        const d = cell.getData();
        const path = d.gubun === '1' ? '/HSIO/HSIO050U' : '/HSIO/HSIO052U';
        router.push({ path, query: { BALym: d.BALym, balno: d.balno } });
      }},
      { title: "담당자", field: "usernm", width: 90, hozAlign: "center" },
      { title: "주문번호", field: "order_no", width: 120, formatter: (c) => {
        const d = c.getData(); return d.ordym ? `${d.ordym}-${d.ordno}` : '';
      }},
      { title: "품목코드", field: "itemcd", width: 100 },
      { title: "품목명", field: "itemnm", minWidth: 180 },
      { title: "규격", field: "itsize", width: 180 },
      { title: "발주량", field: "balqty", width: 80, hozAlign: "right", formatter: "money", formatterParams: { precision: (c:any) => c.getData().qtypnt || 0 } },
      { title: "공급가", field: "balamt", width: 100, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "부가세", field: "balvat", width: 100, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "입고량", field: "inqty", width: 100, hozAlign: "right", formatter: "money", formatterParams: { precision: (c:any) => c.getData().qtypnt || 0 } },
      { title: "미입고량", field: "janqty", width: 100, hozAlign: "right", cssClass: "text-danger fw-bold", formatter: (c) => {
        const d = c.getData(); return formatNumber((Number(d.balqty) || 0) - (Number(d.inqty) || 0));
      }}
    ]
  });
}

const updateTotals = (data: any[]) => {
  activeItemCount.value = data.length;
  totals.qty = data.reduce((acc, i) => acc + (Number(i.balqty) || 0), 0);
  totals.amt = data.reduce((acc, i) => acc + (Number(i.balamt) || 0), 0);
  totals.vat = data.reduce((acc, i) => acc + (Number(i.balvat) || 0), 0);
  totals.inqty = data.reduce((acc, i) => acc + (Number(i.inqty) || 0), 0);
}

const modalVisible = ref(false);
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

async function fetchList() {
  try {
    const res = await api.post('/hsio/HSIO_082S_STR', {
      cmpycd: authStore.cmpycd,
      fromdt: searchParam.fromdt.replace(/-/g, ''),
      todt: searchParam.todt.replace(/-/g, ''),
      ordym: searchParam.ordym.replace(/-/g, ''),
      ordno: searchParam.ordno,
      custcd: searchParam.custcd,
      userid: searchParam.userid,
      ipgoyn: searchParam.ipgoyn,
      remark: searchParam.remark
    });
    if (grid.value) {
      grid.value.setData(res.data);
      updateTotals(res.data);
      vAlert('조회되었습니다.');
    }
  } catch (e) { vAlertError('조회 실패') }
}

function initialize() {
  resetForm(searchParam);
  Object.assign(searchParam, {
    fromdt: formatDateStr(firstDay), todt: formatDateStr(now),
    custcd: '', custnm: '', ordym: '', ordno: '', userid: '', ipgoyn: 'Y', remark: ''
  });
  grid.value?.clearData();
  updateTotals([]);
}

function openHelp(type: string) {
  const commonProps = { path: '/ha00/HA00_00P_STR', cmpycd: authStore.cmpycd };
  if (type === 'CUST') {
    Object.assign(modalProps, {
      title: '거래처 선택', ...commonProps,
      data: { gubun: 'C4', cmpycd: authStore.cmpycd },
      columns: [
        { title: '코드', field: 'custcd', width: 70 },
        { title: '거래처명', field: 'custnm', width: 180 },
        { title: '사업자번호', field: 'custno', width: 110 },
        { title: '대표자', field: 'bossnm', width: 80 }
      ],
      onConfirm: (d: any) => { searchParam.custcd = d.custcd; searchParam.custnm = d.custnm }
    });
  } else if (type === 'ORDER') {
    Object.assign(modalProps, {
      title: '주문 선택', ...commonProps,
      data: { gubun: 'OR', cmpycd: authStore.cmpycd },
      columns: [
        { title: '주문월', field: 'ordym', width: 90 },
        { title: '번호', field: 'ordno', width: 70 },
        { title: '주문처', field: 'custnm', width: 180 },
        { title: '주문일자', field: 'ordymd', width: 100 }
      ],
      onConfirm: (d: any) => { searchParam.ordym = d.ordym; searchParam.ordno = d.ordno }
    });
  }
  modalVisible.value = true;
}

const formatNumber = (val: any) => new Intl.NumberFormat().format(Number(val) || 0);
const formatDate = (val: any) => val && val.length === 8 ? `${val.substring(0,4)}-${val.substring(4,6)}-${val.substring(6,8)}` : val;
const print = () => vAlert('출력 기능을 준비 중입니다.');
const excel = () => grid.value?.download("xlsx", "발주상세현황.xlsx", { sheetName: "발주상세" });

onUnmounted(() => {
  if (grid.value) grid.value.destroy();
});

onMounted(() => {
  if (typeof window !== 'undefined') (window as any).XLSX = XLSX;
  api.get('/ha00/HA00_00P_STR', { params: { gubun: 'SD', cmpycd: authStore.cmpycd, gbncd: '', code: '', remark: '' } }).then(r => {
    console.log(r.data);
    empOptions.value = r.data.map((i: any) => ({
      userid: String(i.userid).trim(),
      usernm: String(i.usernm).trim()
    }))
  })
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
</style>
