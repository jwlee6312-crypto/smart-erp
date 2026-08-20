<!--
	=============================================================
	프로그램명	: 매출정산 (HSIO510U)
	작성일자	: 2025.02.24
	설명        : 출고 내역을 기반으로 매출 정산 처리 (HSIO110U 디자인 및 집계 로직 이식)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-calculator-fill me-2 text-primary" style="font-size: 18px;"></i>
        영업정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        매출관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">매출정산 (HSIO510U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchCustomerList">조회</button>
        <button class="btn-erp btn-save" @click="saveSettlement" :disabled="!selectedCust.custcd">저장</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 조회 필터 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
              <col style="width: 10%" /><col style="width: 23%" />
              <col style="width: 10%" /><col style="width: 23%" />
              <col style="width: 10%" /><col style="width: 24%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="required bg-light text-center">판매부서</th>
                <td>
                  <div class="input-group input-group-sm w-75">
                    <input v-model="searchData.deptcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
                    <input v-model="searchData.deptnm" type="text" class="form-control" placeholder="부서 선택" />
                    <button class="btn btn-outline-secondary px-2" @click="handleOpenHelp('SEARCH_DEPT')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="required bg-light text-center">판매일자</th>
                <td class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                  <DateForm v-model:fromdt="searchData.fromdt" v-model:todt="searchData.todt" />
                </td>
                <th class="bg-light text-center">거&nbsp;&nbsp;래&nbsp;&nbsp;처</th>
                <td>
                  <div class="input-group input-group-sm w-75">
                    <input v-model="searchData.custcd" type="text" class="form-control text-center bg-light" readonly />
                    <input v-model="searchData.custnm" type="text" class="form-control" placeholder="거래처 선택" />
                    <button class="btn btn-outline-secondary px-2" @click="handleOpenHelp('SEARCH_CUST')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 투-그리드 레이아웃 -->
      <div class="d-flex gap-2 flex-grow-1 overflow-hidden" style="min-height: 0;">

        <!-- ⬅️ 좌측: 거래처 목록 -->
        <div class="card border shadow-sm d-flex flex-column flex-shrink-0" style="width: 280px;">
          <div class="card-header bg-white text-dark fw-bold border-bottom py-1 ps-3 h-auto" style="font-size: 13px;">
            <i class="bi bi-list-ul me-1 text-secondary"></i> 정산 대상 거래처
          </div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
              <div ref="custGridElement" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>

        <!-- ➡️ 우측: 상세 정보 및 품목 그리드 -->
        <div class="flex-grow-1 d-flex flex-column gap-2 overflow-hidden">

          <!-- 상세 마스터 정보 폼 (HSIO110U 디자인 이식) -->
          <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
            <div class="card-body p-0 bg-white">
              <table class="erp-table-dense w-100">
                <colgroup>
                  <col style="width: 110px;" /><col />
                  <col style="width: 110px;" /><col />
                  <col style="width: 110px;" /><col />
                </colgroup>
                <tbody>
                  <tr>
                    <th class="bg-light text-center">거&nbsp;&nbsp;래&nbsp;&nbsp;처</th>
                    <td colspan="3">
                      <div class="input-group input-group-sm">
                        <input v-model="selectedCust.custcd" type="text" class="form-control text-center bg-light" style="max-width: 70px;" readonly />
                        <input v-model="selectedCust.custnm" type="text" class="form-control bg-light fw-bold" readonly />
                      </div>
                    </td>
                    <th class="bg-light text-center">출고부서</th>
                    <td>
                      <div class="input-group input-group-sm">
                        <input v-model="masterData.deptcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
                        <input v-model="masterData.deptnm" type="text" class="form-control bg-light" readonly />
                      </div>
                    </td>
                  </tr>
                  <tr>
                    <th class="required bg-light text-center">사업장</th>
                    <td>
                      <select v-model="masterData.taxunit" class="form-select">
                        <option v-for="opt in taxUnitOptions" :key="opt.taxunit" :value="opt.taxunit">{{ opt.unitnm }}</option>
                      </select>
                    </td>
                    <th class="required bg-light text-center">과세유형</th>
                    <td>
                      <select v-model="masterData.vattype" class="form-select">
                        <option v-for="opt in vatTypeOptions" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                      </select>
                    </td>
                    <th class="required bg-light text-center">발&nbsp;행&nbsp;일</th>
                    <td><input v-model="masterData.pubymd" type="date" class="form-control" /></td>
                  </tr>
                  <tr>
                    <th class="bg-light text-center">공급가액</th>
                    <td><input :value="formatNumber(totals.splamt)" class="form-control text-end bg-light fw-bold" readonly /></td>
                    <th class="bg-light text-center">부가세</th>
                    <td><input :value="formatNumber(totals.vatamt)" class="form-control text-end bg-light fw-bold" readonly /></td>
                    <th class="bg-light text-center">총&nbsp;&nbsp;합&nbsp;&nbsp;계</th>
                    <td><input :value="formatNumber(totals.sumamt)" class="form-control text-end bg-warning-subtle fw-bold text-dark" readonly /></td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- 상세 품목 그리드 영역 -->
          <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white grid-container-right">
            <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
              <span class="fw-bold small text-dark d-flex align-items-center">
                <i class="bi bi-grid-3x3-gap-fill me-1 text-primary"></i> 정산 대상 출고 명세
              </span>
              <button class="btn btn-sm btn-outline-secondary py-0 px-2 fw-bold" @click="toggleAllRows" style="font-size: 11px;">전체선택</button>
            </div>
            <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
                <div ref="itemGridElement" class="tabulator-instance flex-grow-1"></div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, computed, nextTick } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import DateForm from '@/components/DateForm.vue'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { useCommonHelp } from '@/composables/useCommonHelp'
import { getDate } from '@/composables/useDate'

const authStore = useAuthStore()
const { firstDay, today } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps, openHelp } = useCommonHelp()

// [1] 데이터 모델링
const searchData = reactive({
  deptcd: authStore.deptcd, deptnm: authStore.deptnm,
  fromdt: firstDay, todt: today, custcd: '', custnm: ''
})

const masterData = reactive<any>({
  deptcd: authStore.deptcd, deptnm: authStore.deptnm,
  taxunit: '', vattype: '010', pubymd: today,
  pricegbn: '1', clsymd: '', sclsym: ''
})

const selectedCust = reactive({ custcd: '', custnm: '', def_vattype: '010' })
const selectedItems = ref<any[]>([])

// [2] 집계 로직 (HSIO110U 이식)
const totals = computed(() => {
  const splamt = selectedItems.value.reduce((acc, cur: any) => acc + (Number(cur.ioamt) || 0), 0)
  const vatamt = selectedItems.value.reduce((acc, cur: any) => acc + (Number(cur.iovat) || 0), 0)
  return { splamt, vatamt, sumamt: splamt + vatamt }
})

// [3] 그리드 관련
const custGridElement = ref<HTMLElement | null>(null)
const itemGridElement = ref<HTMLElement | null>(null)
let custGrid: Tabulator | null = null
let itemGrid: Tabulator | null = null

const taxUnitOptions = ref<any[]>([])
const vatTypeOptions = ref<any[]>([])

const initGrids = () => {
  if (custGridElement.value) {
    custGrid = new Tabulator(custGridElement.value, {
      layout: "fitColumns", height: "100%", placeholder: "데이터 없음",
      columns: [
      { title: "거래처 상호", field: "custnm", headerSort: false, cssClass: "fw-bold text-primary" }
      ],
    })
    custGrid.on("rowClick", (e, row) => {
      const data = row.getData()
      selectedCust.custcd = data.custcd; selectedCust.custnm = data.custnm;
      masterData.deptcd = data.deptcd || searchData.deptcd;
      masterData.deptnm = data.deptnm || searchData.deptnm;
      selectedCust.def_vattype = data.def_vattype || '010'
      masterData.vattype = selectedCust.def_vattype
      fetchSettlementItems()
    })
  }

  if (itemGridElement.value) {
    itemGrid = new Tabulator(itemGridElement.value, {
      layout: "fitColumns", height: "100%", placeholder: "거래처를 선택하세요.", selectable: true,
      columnDefaults: { headerSort: false, headerHozAlign: 'center', vertAlign: "middle" },
      columns: [
        { title: "선택", width: 60, hozAlign: "center", formatter: "rowSelection", titleFormatter: "rowSelection", headerSort: false },
        { title: "출고일", field: "ioymd", width: 100, hozAlign: "center", formatter: (c) => formatDate(c.getValue()) },
        { title: "품목명", field: "itemnm", minWidth: 180, widthGrow: 1, cssClass: "fw-bold", hozAlign: "left" },
        { title: "규격", field: "itsize", width: 120, hozAlign: "left" },
        { title: "단위", field: "unit", width: 60, hozAlign: "center" },
        { title: "수량", field: "ioqty", width: 80, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
        { title: "정산수량", field: "jioqty", width: 80, hozAlign: "right", editor: "number", cssClass: "bg-warning-subtle" },
        { title: "단가", field: "price", width: 100, hozAlign: "right", editor: "number", formatter: "money" },
        { title: "공급가", field: "ioamt", width: 110, hozAlign: "right", editor: "number", formatter: "money" },
        { title: "부가세", field: "iovat", width: 100, hozAlign: "right", editor: "number", formatter: "money" }
      ]
    });

    // 행 선택 변경 시 집계 데이터 동기화
    itemGrid.on("rowSelectionChanged", (data: any[]) => {
      selectedItems.value = data
    })

    itemGrid.on("cellEdited", (cell) => {
        if (['jioqty', 'price', 'ioamt', 'iovat'].includes(cell.getField())) {
            if (['jioqty', 'price'].includes(cell.getField())) calcRow(cell.getRow());
            else if (cell.getField() === 'ioamt') calcRow(cell.getRow(), 'amt');
            // 선택된 상태인 경우 강제 반응성 트리거
            if(cell.getRow().isSelected()) {
                selectedItems.value = [...itemGrid!.getSelectedData()]
            }
        }
    });
  }
}

// [4] 계산 로직
const calcRow = (row: Tabulator.RowComponent, type: string = 'qty') => {
  const d = row.getData(); const prcGbn = masterData.pricegbn;
  let amt = Number(d.ioamt) || 0; let vat = Number(d.iovat) || 0;
  if (type === 'qty') {
    if (prcGbn === '1') {
        amt = Math.round(Number(d.jioqty) * Number(d.price));
        vat = Math.round(amt * 0.1);
    } else {
        amt = Math.round(Number(d.jioqty) * Number(d.price) * (10 / 11));
        vat = Math.round(Number(d.jioqty) * Number(d.price) * (1 / 11));
    }
  } else if (type === 'amt') {
    vat = Math.floor(amt * 0.1);
    row.update({ price: Math.floor((prcGbn === '1' ? amt : amt + vat) / (Number(d.jioqty) || 1)) });
  }
  row.update({ ioamt: amt, iovat: vat });
}

const toggleAllRows = () => {
    if(!itemGrid) return;
    const rows = itemGrid.getRows();
    if (rows.length === itemGrid.getSelectedRows().length) itemGrid.deselectRow();
    else itemGrid.selectRow();
}

// [5] 데이터 페칭
async function fetchCustomerList() {
  try {
    const res = await api.post('/hsio/HSIO_510U_STR', {
        actkind: 'S1',
        cmpycd: authStore.cmpycd,
        iogbn: '200',
        fromdt: searchData.fromdt.replace(/-/g, ''),
        todt: searchData.todt.replace(/-/g, ''),
        deptcd: searchData.deptcd,
        custcd: searchData.custcd
    })

    const data = res.data || [];
    custGrid?.setData(data);
    itemGrid?.clearData();

    // 💡 검색 조건에 거래처가 명시되어 있고 결과에 존재하면 자동 선택
    if (searchData.custcd && data.some((i: any) => i.custcd === searchData.custcd)) {
      const target = data.find((i: any) => i.custcd === searchData.custcd);
      selectedCust.custcd = target.custcd;
      selectedCust.custnm = target.custnm;
      masterData.vattype = target.def_vattype || '010';
      fetchSettlementItems();
    } else {
      selectedCust.custcd = '';
      selectedCust.custnm = '';
    }
  } catch (e) { vAlertError('거래처 목록 조회 실패') }
}

async function fetchSettlementItems() {
  try {
    const res = await api.post('/hsio/HSIO_510U_STR', {
        actkind: 'S0', cmpycd: authStore.cmpycd, iogbn: '200',
        fromdt: searchData.fromdt.replace(/-/g, ''),
        todt: searchData.todt.replace(/-/g, ''),
        deptcd: searchData.deptcd,
        custcd: selectedCust.custcd
    })

    // 💡 normalize 제거하고 res.data를 직접 가공
    const data = (res.data || []).map((i: any) => {
      const ioQty = (Number(i.jsanqty) || 0) - (Number(i.jqty) || 0);
      const ioamt = (Number(i.jsanamt) || 0) - (Number(i.jamt) || 0);
      const iovat = (Number(i.jsanvat) || 0) - (Number(i.jvat) || 0);
      let price = ioQty !== 0 ? (masterData.pricegbn === '1' ? ioamt / ioQty : (ioamt + iovat) / ioQty) : 0
      return { ...i, ioqty: ioQty, jioqty: ioQty, price: Math.round(price), ioamt: ioamt, iovat: iovat, ioymd: i.salsymd }
    })
    itemGrid?.setData(data);
    itemGrid?.selectRow();
  } catch (e) { vAlertError('정산 품목 조회 실패') }
}

async function saveSettlement() {
  if (!masterData.deptcd || !masterData.pubymd) return vAlertError('부서 및 발행일은 필수입니다.')
  const items = itemGrid?.getSelectedData() || []
  if (!items.length) return vAlertError('정산할 품목을 선택하세요.')

  if (!confirm('정산 작업을 진행하시겠습니까?')) return

  try {
    const payload = {
        mst: {
            actkind: 'A0', cmpycd: authStore.cmpycd, iogbn: '200',
            fromdt: searchData.fromdt.replace(/-/g, ''), todt: searchData.todt.replace(/-/g, ''),
            custcd: selectedCust.custcd, deptcd: masterData.deptcd,
            taxunit: masterData.taxunit, vattype: masterData.vattype,
            jsanymd: masterData.pubymd.replace(/-/g, ''),
            jsanamt: totals.value.splamt, jsanvat: totals.value.vatamt,
            updemp: authStore.userid
        },
        dtl: items.map((item: any) => ({
            actkind: 'U0', ioym: item.ioym, iono: item.iono, iorowno: item.iorowno,
            jsanqty: item.jioqty, jsanamt: item.ioamt, jsanvat: item.iovat
        }))
    }

    await api.post('/hsio/HSIO_510U_SAVE', payload);
    vAlert('정상적으로 처리되었습니다.')
    fetchCustomerList(); initialize();
  } catch (e: any) { vAlertError(e.response?.data?.message || '저장 실패') }
}

function handleOpenHelp(type: string) {
  if (type === 'SEARCH_DEPT') openHelp('DEPT', (d) => { searchData.deptcd = d.deptcd; searchData.deptnm = d.deptnm });
  else if (type === 'SEARCH_CUST') openHelp('CUST', (d) => { searchData.custcd = d.custcd; searchData.custnm = d.custnm });
}

function initialize() {
  resetForm(searchData); Object.assign(searchData, { deptcd: authStore.deptcd, deptnm: authStore.deptnm, fromdt: firstDay, todt: today });
  Object.assign(masterData, { deptcd: authStore.deptcd, deptnm: authStore.deptnm, vattype: '010', pubymd: today });
  custGrid?.clearData(); itemGrid?.clearData(); selectedCust.custcd = ''; selectedItems.value = []
}

const formatNumber = (val: any) => Number(val || 0).toLocaleString()
const formatDate = (val: any) => val && val.length === 8 ? `${val.substring(0,4)}-${val.substring(4,6)}-${val.substring(6,8)}` : val;

onMounted(async () => {
  api.post('/hp00/HP00_000S_STR', { gubun: 'CL', cmpycd: authStore.cmpycd }).then(r => { if (r.data?.length) masterData.clsymd = r.data[0].clsymd });
  api.post('/ha00/HA00_010S_STR', { cmpycd: authStore.cmpycd, gbn: 'p1' }).then(r => { if (r.data?.length) masterData.pricegbn = r.data[0].pricegbn || '1' });
  api.post('/ha00/HA00_00P_STR', { gubun: 'SA', cmpycd: authStore.cmpycd }).then(r => {
    taxUnitOptions.value = (r.data || []).map((i:any)=>({taxunit:String(i.taxunit||i.code).trim(), unitnm:String(i.unitnm||i.cdnm).trim()}));
    if(taxUnitOptions.value.length) masterData.taxunit = taxUnitOptions.value[0].taxunit;
  });
  api.post('/ha00/HA00_00P_STR', { gubun: 'E0', gbncd: '130', cmpycd: authStore.cmpycd }).then(r => vatTypeOptions.value = (r.data || []).map((i:any)=>({codecd:String(i.codecd||i.code).trim(), codenm:String(i.codenm||i.cdnm).trim()})));
  nextTick(() => initGrids())
})
</script>

<style scoped>
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
