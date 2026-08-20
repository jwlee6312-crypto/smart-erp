<!--
	=============================================================
	프로그램명	: 외주비 정산 (HPIO110U)
	작성일자	: 2025.02.25
	설명        : 외주 가공 입고 내역을 기반으로 매입 정산 처리 (HSIO110U 참조 및 기능 고도화)
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
        매입정산 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">외주비 정산 (HPIO110U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchCustList">조회</button>
        <button class="btn-erp btn-save" @click="save" :disabled="!formData.custcd">정산저장</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 조회 필터 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
                <col style="width: 10%" /><col style="width: 30%" />
                <col style="width: 10%" /><col style="width: 50%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light">입고부서</th>
                <td>
                  <div class="input-group input-group-sm w-75">
                    <input v-model="searchForm.deptcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
                    <input v-model="searchForm.deptnm" type="text" class="form-control bg-light" readonly />
                    <button class="btn btn-outline-secondary px-2" @click="openHelp('DEPT_SCH')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="text-center bg-light">입고일자</th>
                <td class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                  <DateForm v-model:fromdt="searchForm.fromdt" v-model:todt="searchForm.todt" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 투-그리드 레이아웃 영역 -->
      <div class="d-flex gap-2 flex-grow-1 overflow-hidden" style="min-height: 0;">

        <!-- ⬅️ 좌측: 외주가공업체 목록 -->
        <div class="card border shadow-sm d-flex flex-column overflow-hidden grid-container-left" style="width: 300px; min-width: 300px;">
          <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark">외주가공업체</div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="poGridRef" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>

        <!-- ➡️ 우측: 정산 정보 + 상세 내역 그리드 -->
        <div class="flex-grow-1 d-flex flex-column gap-2 overflow-hidden">

          <!-- 상세 정산 정보 폼 -->
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
                        <input v-model="formData.custcd" type="text" class="form-control text-center bg-light" style="max-width: 70px;" readonly />
                        <input v-model="formData.custnm" type="text" class="form-control bg-light" readonly />
                      </div>
                    </td>
                    <th class="bg-light text-center">입고부서</th>
                    <td>
                      <div class="input-group input-group-sm">
                        <input v-model="formData.deptcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
                        <input v-model="formData.deptnm" type="text" class="form-control bg-light" readonly />
                        <button class="btn btn-outline-secondary px-2" @click="openHelp('DEPT')"><i class="bi bi-search"></i></button>
                      </div>
                    </td>
                  </tr>
                  <tr>
                    <th class="required bg-light text-center">사업장</th>
                    <td>
                      <select v-model="formData.taxunit" class="form-select">
                        <option v-for="opt in saOptions" :key="opt.code" :value="opt.code">{{ opt.cdnm }}</option>
                      </select>
                    </td>
                    <th class="required bg-light text-center">과세유형</th>
                    <td>
                      <select v-model="formData.vattype" class="form-select">
                        <option v-for="opt in vatOptions" :key="opt.code" :value="opt.code">{{ opt.cdnm }}</option>
                      </select>
                    </td>
                    <th class="required bg-light text-center">발&nbsp;행&nbsp;일</th>
                    <td><input v-model="formData.pubymd" type="date" class="form-control" /></td>
                  </tr>
                  <tr class="bg-primary bg-opacity-10">
                    <th class="bg-primary bg-opacity-10 text-primary text-center">공급가액</th>
                    <td><input :value="formatNumber(totals.splamt)" class="form-control text-end bg-light fw-bold text-primary" readonly /></td>
                    <th class="bg-primary bg-opacity-10 text-primary text-center">부 가 세</th>
                    <td><input :value="formatNumber(totals.vatamt)" class="form-control text-end bg-light fw-bold text-primary" readonly /></td>
                    <th class="bg-primary bg-opacity-10 text-primary text-center">총합계액</th>
                    <td><input :value="formatNumber(totals.sumamt)" class="form-control text-end bg-light fw-bold text-primary" readonly /></td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- 정산 상세 그리드 영역 -->
          <div class="card border shadow-sm flex-grow-1 d-flex flex-column overflow-hidden grid-container-right">
            <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
              <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>정산 대상 내역</span>
              <div class="small text-muted" style="font-size: 11px;">※ 정산할 항목을 체크하고 단가를 확인하세요.</div>
            </div>
            <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
              <div ref="itemGridRef" class="tabulator-instance flex-grow-1"></div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, computed, nextTick, onUnmounted } from 'vue'
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
const { modalVisible, modalProps, openHelp: openCommonHelp } = useCommonHelp()

// [1] 데이터 모델링
const searchForm = reactive({
  deptcd: authStore.deptcd, deptnm: authStore.deptnm,
  fromdt: firstDay, todt: today
})

const formData = reactive<any>({
  actkind: 'S0', cmpycd: authStore.cmpycd, custcd: '', custnm: '', deptcd: authStore.deptcd, deptnm: authStore.deptnm,
  taxunit: '100', vattype: '120', pubymd: today, gubun: ''
})

const saOptions = ref<any[]>([]); const vatOptions = ref<any[]>([])
const priceGbn = ref('1'); const suYn = ref('N')

const poGridRef = ref<HTMLDivElement | null>(null); const itemGridRef = ref<HTMLDivElement | null>(null)
let poGrid: Tabulator | null = null; let itemGrid: Tabulator | null = null

const selectedItems = ref<any[]>([])

const totals = computed(() => {
  const splamt = selectedItems.value.reduce((acc, cur) => acc + (Number(cur.ioamt) || 0), 0)
  const vatamt = selectedItems.value.reduce((acc, cur) => acc + (Number(cur.iovat) || 0), 0)
  return { splamt, vatamt, sumamt: splamt + vatamt }
})

const initGrids = () => {
  poGrid = new Tabulator(poGridRef.value!, {
    layout: "fitColumns", height: "100%", placeholder: "거래처 없음", selectable: 1,
    columnDefaults: { headerSort: false, headerHozAlign: "center", hozAlign: "center", vertAlign: "middle" },
    columns: [{ title: "외주가공업체", field: "custnm", hozAlign: "left", cssClass: 'fw-bold text-dark' }],
  });
  poGrid.on("rowClick", (e, row) => fetchDetail(row.getData()));

  itemGrid = new Tabulator(itemGridRef.value!, {
    layout: "fitColumns", height: "100%", placeholder: "대상을 선택하세요.", selectable: true,
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "선택", width: 60, hozAlign: "center", formatter: "rowSelection", titleFormatter: "rowSelection" },
      { title: "입고일", field: "ioymd", width: 110, hozAlign: "center", formatter: (c) => formatDate(c.getValue()) },
      { title: "품목명", field: "itemnm", minWidth: 200, widthGrow: 1, cssClass: 'fw-bold text-primary', hozAlign: 'left' },
      { title: "규격", field: "itsize", width: 120, hozAlign: 'left' },
      { title: "단위", field: "unit", width: 60, hozAlign: "center" },
      { title: "수량", field: "ioqty", width: 90, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "단가", field: "price", width: 100, hozAlign: "right", editor: "number", formatter: "money", formatterParams: { precision: 0 } },
      { title: "공급가", field: "ioamt", width: 120, hozAlign: "right", editor: "number", formatter: "money", formatterParams: { precision: 0 } },
      { title: "부가세", field: "iovat", width: 110, hozAlign: "right", editor: "number", formatter: "money", formatterParams: { precision: 0 } }
    ]
  });

  itemGrid.on("rowSelectionChanged", (data) => { selectedItems.value = data });
  itemGrid.on("cellEdited", (cell) => {
    const field = cell.getField();
    if (field === 'price') calcByPrice(cell.getRow());
    else if (field === 'ioamt') calcByAmt(cell.getRow());
    // rowSelectionChanged will handle total update if the row is selected
  });
}

const calcByPrice = (row: any) => {
  const d = row.getData();
  const qty = Number(d.ioqty || 0);
  const price = Number(d.price || 0);
  let amt = 0, vat = 0;
  if (priceGbn.value === '1') {
    amt = Math.round(qty * price);
    vat = suYn.value === 'Y' ? 0 : Math.round(amt * 0.1);
  } else {
    const total = Math.round(qty * price);
    amt = Math.round(total * 10 / 11);
    vat = suYn.value === 'Y' ? 0 : total - amt;
  }
  row.update({ ioamt: amt, iovat: vat });
}

const calcByAmt = (row: any) => {
  const d = row.getData();
  const amt = Number(d.ioamt || 0);
  const qty = Number(d.ioqty || 0);
  const vat = suYn.value === 'Y' ? 0 : Math.floor(amt * 0.1);
  let price = 0;
  if (qty !== 0) {
    if (priceGbn.value === '1') price = Math.floor(amt / qty);
    else price = Math.floor((amt + vat) / qty);
  }
  row.update({ iovat: vat, price: price });
}

async function fetchOptions() {
  try {
    const [resSa, resVat] = await Promise.all([
      api.post('/ha00/HA00_00P_STR', { gubun: 'SA', cmpycd: authStore.cmpycd }),
      api.post('/ha00/HA00_00P_STR', { gubun: 'E0', cmpycd: authStore.cmpycd, gbncd: '120' })
    ]);
    saOptions.value = resSa.data; vatOptions.value = resVat.data;
    if (saOptions.value.length > 0) formData.taxunit = saOptions.value[0].code;

    const resP1 = await api.post('/ha00/HA00_010S_STR', { cmpycd: authStore.cmpycd, gubun: 'P1' });
    priceGbn.value = resP1.data?.[0]?.pricegbn || '1';
  } catch (e) {}
}

async function fetchCustList() {
  try {
    const res = await api.post('/hpio/HPIO_110U_STR', {
      actkind: 'S1', cmpycd: authStore.cmpycd, iogbn: '100',
      fromdt: searchForm.fromdt.replace(/-/g, ''),
      todt: searchForm.todt.replace(/-/g, ''),
      deptcd: searchForm.deptcd
    });
    poGrid?.setData(res.data || []); itemGrid?.clearData(); vAlert('조회되었습니다.')
  } catch (e) { vAlertError('거래처 조회 실패') }
}

async function fetchDetail(cust: any) {
  formData.custcd = cust.custcd; formData.custnm = cust.custnm; formData.gubun = cust.gubun;
  try {
    const checkRes = await api.post('/hpio/HPIO_110U_CHECK_SUYN', { cmpycd: authStore.cmpycd, custcd: cust.custcd });
    suYn.value = checkRes.data.length > 0 ? 'Y' : 'N';

    const res = await api.post('/hpio/HPIO_110U_STR', {
      actkind: 'S0', cmpycd: authStore.cmpycd, iogbn: '100',
      fromdt: searchForm.fromdt.replace(/-/g, ''),
      todt: searchForm.todt.replace(/-/g, ''),
      custcd: cust.custcd, deptcd: formData.deptcd
    });
    itemGrid?.setData(res.data.map((i: any) => {
      const ioqty = Number(i.ioqty || 0); const ioamt = Number(i.ioamt || 0); const iovat = Number(i.iovat || 0);
      let price = 0;
      if (ioqty !== 0) price = priceGbn.value === '1' ? (ioamt / ioqty) : ((ioamt + iovat) / ioqty);
      return { ...i, price: Math.round(price) };
    }));
  } catch (e) { vAlertError('상세 조회 실패') }
}

async function save() {
  const items = itemGrid?.getSelectedData() || []
  if (!items.length) return vAlertError('정산 처리할 항목을 선택하세요.')
  if (!confirm('정상으로 정산 처리하시겠습니까?')) return

  try {
    const mstRes = await api.post('/hpio/HPIO_110U_STR', {
      actkind: 'A0', cmpycd: authStore.cmpycd, iogbn: '100',
      fromdt: searchForm.fromdt.replace(/-/g, ''), todt: searchForm.todt.replace(/-/g, ''),
      custcd: formData.custcd, deptcd: formData.deptcd,
      taxunit: formData.taxunit, vattype: formData.vattype,
      jsanymd: formData.pubymd.replace(/-/g, ''),
      jsanamt: totals.value.splamt, jsanvat: totals.value.vatamt,
      updemp: authStore.userid
    });

    const mstData = mstRes.data?.[0]
    const jsanym = mstData?.jsanym || mstData?.JSANYM;
    const jsanno = mstData?.jsanno || mstData?.JSANNO;

    if (!jsanym || jsanym === '000000') throw new Error(jsanno || '마스터 정산 생성 실패');

    for (const item of items) {
      await api.post('/hpio/HPIO_110U_STR', {
        actkind: 'U0', cmpycd: authStore.cmpycd, iogbn: '100',
        fromdt: searchForm.fromdt.replace(/-/g, ''), todt: searchForm.todt.replace(/-/g, ''),
        custcd: formData.custcd, deptcd: formData.deptcd,
        ioym: item.ioym, iono: item.iono, iorowno: item.iorowno,
        taxunit: formData.taxunit, vattype: formData.vattype,
        jsanym: jsanym, jsanno: jsanno,
        jsanymd: formData.pubymd.replace(/-/g, ''),
        jsanamt: item.ioamt, jsanvat: item.iovat,
        updemp: authStore.userid
      });
    }

    vAlert('정상적으로 정산되었습니다.');
    fetchCustList(); initialize();
  } catch (e: any) { vAlertError(e.message || '정산 저장 중 오류 발생'); }
}

const openHelp = (type: string) => {
  if (type === 'DEPT_SCH') openCommonHelp('DEPT', (d) => { searchForm.deptcd = d.deptcd; searchForm.deptnm = d.deptnm });
  else if (type === 'DEPT') openCommonHelp('DEPT', (d) => { formData.deptcd = d.deptcd; formData.deptnm = d.deptnm });
}

function initialize() {
  resetForm(formData); poGrid?.clearData(); itemGrid?.clearData();
  Object.assign(formData, { cmpycd: authStore.cmpycd, deptcd: authStore.deptcd, deptnm: authStore.deptnm, taxunit: '100', vattype: '120', pubymd: today });
  if (saOptions.value.length > 0) formData.taxunit = saOptions.value[0].code;
}

const formatDate = (v: any) => v && v.length === 8 ? `${v.substring(0, 4)}-${v.substring(4, 6)}-${v.substring(6, 8)}` : v;
const formatNumber = (v: any) => new Intl.NumberFormat().format(v || 0);

onMounted(async () => {
  await fetchOptions();
  nextTick(initGrids);
  initialize();
  fetchCustList();
})

onUnmounted(() => {
  if (poGrid) poGrid.destroy();
  if (itemGrid) itemGrid.destroy();
});
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; font-size: 12px; }
.grid-container-left, .grid-container-right { border-bottom: 3px solid #005a9f !important; }
</style>
