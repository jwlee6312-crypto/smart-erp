<!--
	=============================================================
	프로그램명	: 출고취소 (HSIO560U)
	작성일자	: 2025.02.24
	설명        : 출고 완료된 내역을 선택하여 취소 처리 (창고 로직 표준화 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-x-circle-fill me-2 text-danger" style="font-size: 18px;"></i>
        영업정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        출고관리 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        <span class="text-danger fw-bolder">출고취소 (HSIO560U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchCustList">조회</button>
        <button class="btn-erp btn-save" @click="saveCancel" :disabled="activeItemCount === 0">취소처리</button>
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
                <th class="text-center bg-light required">출고창고</th>
                <td>
                  <select v-model="searchForm.whcd" class="form-select form-select-sm">
                    <option v-for="opt in whOptions" :key="opt.whcd" :value="opt.whcd">{{ opt.whnm }}</option>
                  </select>
                </td>
                <th class="text-center bg-light required">출고일자</th>
                <td colspan="3">
                  <div class="d-flex align-items-center gap-1">
                    <DateForm v-model:fromdt="searchForm.fromdt" v-model:todt="searchForm.todt" />
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- 💡 3. 메인 컨텐츠 영역 (HSOD100U 투-그리드 레이아웃 이식) -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex gap-2 bg-light">

      <!-- ⬅️ 좌측: 출고 완료 목록 -->
      <div class="card border shadow-sm d-flex flex-column overflow-hidden grid-container-left" style="width: 350px; min-width: 350px;">
        <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark">출고 완료 상호</div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column" style="min-height: 0;">
          <div ref="leftGridRef" class="tabulator-full-height"></div>
        </div>
      </div>

      <!-- ➡️ 우측: 마스터 정보 및 품목 그리드 -->
      <div class="d-flex flex-column flex-grow-1 gap-2 overflow-hidden">
        <!-- 마스터 정보 (HSIO550U 레이아웃 완벽 이식) -->
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
                  <th class="bg-light text-center">거 래 처</th>
                  <td>
                    <div class="input-group input-group-sm flex-nowrap">
                      <input :value="masterData.custcd" type="text" class="form-control text-center bg-light fw-bold" style="max-width: 65px;" readonly />
                      <input :value="masterData.custnm" type="text" class="form-control border-start-0 bg-light" readonly />
                    </div>
                  </td>
                  <th class="bg-light text-center">출고창고</th>
                  <td>
                    <select v-model="masterData.whcd" class="form-select bg-light" disabled>
                      <option v-for="opt in whOptions" :key="opt.whcd" :value="opt.whcd">{{ opt.whnm }}</option>
                    </select>
                  </td>
                  <th class="bg-light text-center">출고일자</th>
                  <td><input :value="formatDate(masterData.ioymd)" class="form-control bg-light text-center" readonly /></td>
                </tr>
                <tr>
                  <th class="bg-light text-center">배 송 처</th>
                  <td colspan="3">
                    <AddressPopupForm
                      v-model:postno="masterData.postno"
                      v-model:address="masterData.address"
                      v-model:d_address="masterData.d_address"
                      readonly
                    />
                  </td>
                  <th class="bg-light text-center">배송담당</th>
                  <td>
                    <select v-model="masterData.trnemp" class="form-select bg-light" disabled>
                      <option v-for="user in userData" :key="user.userid" :value="user.userid">{{ user.usernm }}</option>
                    </select>
                  </td>
                </tr>
                <tr>
                  <th class="bg-light text-center">특기사항</th>
                  <td colspan="5"><input v-model="masterData.remark" class="form-control bg-light" readonly /></td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- 품목 그리드 -->
        <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white grid-container-right">
          <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
            <span class="fw-bold small text-dark d-flex align-items-center">
              <i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i> 출고 품목 상세 명세
            </span>
            <div class="d-flex align-items-center gap-2">
               <span class="text-danger fw-bold small">선택: {{ activeItemCount }} 건</span>
               <button class="btn btn-xs btn-outline-secondary px-2" style="height: 22px; font-size: 11px;" @click="toggleAllRows">전체선택</button>
            </div>
          </div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column" style="min-height: 0;">
            <div ref="mainGridRef" class="tabulator-full-height"></div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, nextTick } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import DateForm from '@/components/DateForm.vue'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { getDate } from '@/composables/useDate'
import AddressPopupForm from '@/components/AddressPopupForm.vue'
import type { ModalProps } from '@/types/modal'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { firstDay, today } = getDate()

const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

const searchForm = reactive({
  whcd: '',
  fromdt: firstDay,
  todt: today
})

const masterData = reactive<any>({
  custcd: '', custnm: '', whcd: '', whnm: '', ioymd: '', postno: '', address: '', d_address: '', trnemp: '', remark: '', ioym: '', iono: '',
  clsymd: '', sclsym: ''
})

const mainGridRef = ref<HTMLElement | null>(null);
const leftGridRef = ref<HTMLElement | null>(null);
let grid: Tabulator | null = null;
let leftGrid: Tabulator | null = null;
const activeItemCount = ref(0);

const custList = ref<any[]>([]);
const selectedCust = ref<any>(null);
const whOptions = ref<any[]>([]);
const userData = ref<any[]>([])

const initGrids = () => {
  if (leftGridRef.value) {
    leftGrid = new Tabulator(leftGridRef.value, {
      layout: "fitColumns", height: "100%", placeholder: "데이터 없음",
      columns: [
        { title: "No", formatter: "rownum", width: 40, hozAlign: "center", headerSort: false },
        { title: "거래처명", field: "custnm", hozAlign: "left", headerSort: false },
        { title: "출고번호", field: "iono_full", hozAlign: "center", width: 120, cssClass: "text-primary", headerSort: false,
          // 🚀 사용자님의 원칙에 따라 명시적 알리아스 사용
          mutator: (v, d) => {
            const ym = d.ioym || d.IOYM || '';
            const no = d.iono || d.IONO || '';
            return ym && no ? `${ym}-${no}` : '';
          }
        }
      ]
    });
    leftGrid.on("rowClick", (e, row) => selectCust(row.getData()));
  }
   if (mainGridRef.value) {
    grid = new Tabulator(mainGridRef.value, {
      layout: "fitColumns", height: "100%", placeholder: "목록에서 출고 건을 선택하세요.",
      columnCalcs: "table",
      selectable: true,
      columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle", minWidth: 100 },
      columns: [
        { title: "선택", formatter: "rowSelection", titleFormatter: "rowSelection", width: 40, hozAlign: "center", headerSort: false },
        { title: "품목명", field: "itemnm", minWidth: 200, widthGrow: 1, hozAlign: "left", cssClass: "fw-bold" },
        { title: "규격", field: "itsize", width: 150, hozAlign: "left" },
        { title: "단위", field: "unit", width: 60, hozAlign: "center" },
        { title: "출고수량", field: "ioqty", width: 110, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, bottomCalc: "sum" },
        { title: "금액", field: "jsanamt", width: 120, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, bottomCalc: "sum" },
        { title: "부가세", field: "jsanvat", width: 110, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, bottomCalc: "sum" },
        { title: "합계", field: "iosum", width: 130, hozAlign: "right", cssClass: "bg-light fw-bold", formatter: "money", formatterParams: { precision: 0 },
          mutator: (v, d) => (Number(d.jsanamt) || 0) + (Number(d.jsanvat) || 0),
          bottomCalc: "sum"
        },
        {
          title: "정산여부", field: "jyn", width: 80, hozAlign: "center",
          formatter: (c) => c.getValue() === 'Y' ? '<span class="badge bg-success">완료</span>' : '<span class="badge bg-secondary">미정산</span>'
        },
        // 🚀 취소 처리를 위한 기술적 필드 (숨김)
        { field: "ioym", visible: false },
        { field: "iono", visible: false },
        { field: "iorowno", visible: false }
      ]
    });
    grid.on("rowSelectionChanged", (data) => { activeItemCount.value = data.length; });
  }
}

async function fetchWhOptions() {
  try {
    const resWh = await api.get('/hs00/HS00_000S_STR', {
      params: { gubun: 'W0', cmpycd: authStore.cmpycd }
    })
    whOptions.value = (resWh.data || []).map((i: any) => ({
      whcd: String(i.whcd || '').trim(),
      whnm: String(i.whnm || '').trim()
    }))

    if (whOptions.value.length > 0) {
      searchForm.whcd = whOptions.value[0].whcd
    }
  } catch (e) {
    whOptions.value = []
  }
}

async function fetchCustList() {
  try {
    const res = await api.post('/hsio/HSIO_560U_STR', {
      actkind: 'S1',
      cmpycd: authStore.cmpycd,
      iogbn: '200',
      fromdt: searchForm.fromdt.replace(/-/g, ''),
      todt: searchForm.todt.replace(/-/g, ''),
      whcd: searchForm.whcd,
      custcd: '', ioym: '', iono: '', iorowno: '', updemp: authStore.userid
    });
    leftGrid?.setData(res.data || []);
    grid?.clearData(); selectedCust.value = null; resetForm(masterData);
    vAlert('조회되었습니다.');
  } catch (e) { vAlertError('목록 조회 실패'); }
}

async function selectCust(cust: any) {
  selectedCust.value = cust;

  // 🚀 명시적 알리아스를 우선 사용 (대소문자 방어)
  masterData.ioym = (cust.ioym || cust.IOYM || '').toString().trim();
  masterData.iono = (cust.iono || cust.IONO || '').toString().trim();

  // 나머지 정보 전달
  Object.assign(masterData, cust);
  masterData.address = cust.address || cust.addres || cust.ADDRESS || '';
  masterData.d_address = cust.d_address || cust.d_addres || cust.D_ADDRESS || '';
  masterData.postno = cust.postno || cust.POSTNO || '';
  masterData.trnemp = cust.trnemp || cust.TRNEMP || '';

  fetchDetail(masterData);
}

async function fetchDetail(cust: any) {
  try {
    const res = await api.post('/hsio/HSIO_560U_STR', {
      actkind: 'S0',
      cmpycd: authStore.cmpycd,
      iogbn: '200',
      fromdt: searchForm.fromdt.replace(/-/g, ''),
      todt: searchForm.todt.replace(/-/g, ''),
      whcd: searchForm.whcd,
      custcd: cust.custcd || '',
      ioym: cust.ioym || '', // 🚀 보정된 키값 사용
      iono: cust.iono || '', // 🚀 보정된 키값 사용
      iorowno: '',
      updemp: authStore.userid
    });

    const data = (res.data || []).map((i: any) => ({
        ...i,
        iosum: (Number(i.jsanamt) || 0) + (Number(i.jsanvat) || 0)
    }));
    grid?.setData(data);
  } catch (e) { vAlertError('상세 내역 로드 실패'); }
}

async function saveCancel() {
  const items = grid?.getSelectedData() || [];
  if (items.length === 0) return vAlertError('취소할 품목을 선택하세요.');

  const hasSettled = items.some((i: any) => i.jyn === 'Y' || i.jyn === 'Y');
  if (hasSettled) return vAlertError('정산 완료된 자료는 취소할 수 없습니다.');

  const ioymd = String(masterData.ioymd).replace(/-/g, '');
  if (ioymd <= (masterData.clsymd || '')) return vAlertError('회계 마감된 일자입니다.');
  if (ioymd.substring(0, 6) <= (masterData.sclsym || '')) return vAlertError('영업 마감된 월입니다.');

  if (!confirm("선택한 품목의 출고 처리를 취소하시겠습니까?")) return;

  try {
    const fromdt = searchForm.fromdt.replace(/-/g, '');
    const todt = searchForm.todt.replace(/-/g, '');

    for (const item of items) {
      const params = {
        actkind: 'D0',
        cmpycd: authStore.cmpycd,
        iogbn: '200',
        fromdt: fromdt,
        todt: todt,
        whcd: masterData.whcd,
        custcd: masterData.custcd,
        ioym: masterData.ioym,
        iono: masterData.iono,
        iorowno: item.iorowno,
        updemp: authStore.userid
      }
      const res = await api.post('/hsio/HSIO_560U_STR', params);
      const resData = res.data?.[0];
      if (resData && (resData.ioym === '000000' || resData.status === 'Y' || resData.erryn === 'Y')) {
          throw new Error(resData.iono || resData.msg || '취소 중 업무 오류가 발생했습니다.');
      }
    }
    vAlert('출고 취소가 완료되었습니다.');
    fetchCustList(); initialize();
  } catch (e: any) { vAlertError(e.message || '취소 처리 실패'); }
}

const toggleAllRows = () => {
  const rows = grid?.getRows(); if (!rows) return
  const allSelected = grid?.getSelectedRows().length === rows.length
  if (allSelected) grid?.deselectRow(); else grid?.selectRow();
}

function initialize() {
  resetForm(searchForm);
  searchForm.fromdt = firstDay;
  searchForm.todt = today;
  if (whOptions.value.length > 0) {
    searchForm.whcd = whOptions.value[0].whcd;
  }
  leftGrid?.clearData(); grid?.clearData();
  selectedCust.value = null; resetForm(masterData);
  activeItemCount.value = 0;
}

function openHelp(type: string) {
    // 직관화된 개별 팝업 구현 필요 시 여기에 작성
}

onMounted(async () => {
  api.get('/hp00/HP00_000S_STR', { params: { gubun: 'CL', cmpycd: authStore.cmpycd } })
     .then(r => { if (r.data?.length) { masterData.clsymd = r.data[0].clsymd; masterData.sclsym = r.data[0].sclsym; } })

  api.post('/ha00/HA00_00P_STR', { gubun: 'SD', cmpycd: authStore.cmpycd, gbncd: '', code: '', remark: '' }).then(r => {
      userData.value = r.data;
  });

  await fetchWhOptions();
  nextTick(initGrids);
})

function formatNumber(val: any) { return new Intl.NumberFormat().format(Number(val) || 0) }
function formatDate(val: any) { return val && val.length === 8 ? `${val.substring(0,4)}-${val.substring(4,6)}-${val.substring(6,8)}` : val; }
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
