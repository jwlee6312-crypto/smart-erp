<!--
	=============================================================
	프로그램명	: 주문등록 (HSOD100U)
	작성일자	: 2025.02.24
	설명        : 영업 주문 마스터/상세 관리 (오리지널 소문자 필드 원칙 및 인터셉터 표준화 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-cart-check-fill me-2 text-primary" style="font-size: 18px;"></i>
        영업관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        주문관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">주문등록 (HSOD100U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-save" @click="save">저장</button>
        <button class="btn-erp btn-delete" @click="handleFullDelete" :disabled="!form_02.ordno || form_02.ordno === '0000'">전체삭제</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 조회 필터 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
                <col style="width: 10%" /><col style="width: 40%" />
                <col style="width: 10%" /><col style="width: 40%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light">주문일자</th>
                <td class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                  <DateForm v-model:fromdt="form_01.fromdt" v-model:todt="form_01.todt" />
                </td>
                <th class="text-center bg-light">주문거래명</th>
                <td>
                  <input v-model="form_01.schcustnm" class="form-control form-control-sm" placeholder="거래처명 검색" @keyup.enter="search" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 투-그리드 레이아웃 영역 -->
      <div class="d-flex gap-2 flex-grow-1 overflow-hidden" style="min-height: 0;">

        <!-- ⬅️ 좌측: 주문 목록 -->
        <div class="card border shadow-sm d-flex flex-column overflow-hidden grid-container-left" style="width: 350px; min-width: 350px;">
          <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark">주문 목록</div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="tableRef1" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>

        <!-- ➡️ 우측: 마스터 상세 폼 + 품목 상세 그리드 -->
        <div class="flex-grow-1 d-flex flex-column gap-2 overflow-hidden">

          <!-- 상세 마스터 정보 폼 -->
          <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
            <div class="card-body p-0 bg-white">
              <table class="erp-table-dense w-100">
                <colgroup>
                  <col style="width: 110px;" /><col />
                  <col style="width: 110px;" /><col />
                  <col style="width: 110px;" /><col />
                  <col style="width: 110px;" /><col />
                </colgroup>
                <tbody>
                  <tr>
                    <th class="required bg-light">주문부서</th>
                    <td colspan="3">
                      <div class="input-group input-group-sm">
                        <input v-model="form_02.deptnm" class="form-control" readonly />
                        <button class="btn btn-outline-secondary" @click="handleOpenHelp('DEPT')"><i class="bi bi-search"></i></button>
                      </div>
                    </td>
                    <th class="bg-light">주문번호</th>
                    <td>
                      <input :value="displayOrdNo" class="form-control bg-light text-primary fw-bold text-center" readonly placeholder="자동생성" />
                    </td>
                    <th class="required bg-light">주문일자</th>
                    <td><input v-model="form_02.ordymd" type="date" class="form-control" /></td>
                  </tr>
                  <tr>
                    <th class="required bg-light">거래처</th>
                    <td colspan="3">
                      <div class="input-group input-group-sm">
                        <input v-model="form_02.custnm" class="form-control" readonly />
                        <button class="btn btn-outline-secondary" @click="handleOpenHelp('CUST')"><i class="bi bi-search"></i></button>
                      </div>
                    </td>
                    <th class="required bg-light">납품일자</th>
                    <td><input v-model="form_02.outymd" type="date" class="form-control" /></td>
                    <th class="required bg-light">주문종류</th>
                    <td>
                      <select v-model="form_02.ordkind" class="form-select">
                        <option v-for="item in ordkindData" :key="item.code" :value="item.code">{{ item.cdnm }}</option>
                      </select>
                    </td>
                  </tr>
                  <tr>
                    <th class="required bg-light">배송처</th>
                    <td colspan="3">
                      <AddressPopupForm
                        v-model:trancd="form_02.trancd"
                        v-model:postno="form_02.postno"
                        v-model:address="form_02.address"
                        v-model:d_address="form_02.d_address"
                        @open-address="handleOpenHelp('ADDR')"
                      />
                    </td>
                    <th class="required bg-light">결제조건</th>
                    <td>
                      <select v-model="form_02.paycndt" class="form-select">
                        <option v-for="item in paycndtData" :key="item.code" :value="item.code">{{ item.cdnm }}</option>
                      </select>
                    </td>
                    <th class="required bg-light">영업담당</th>
                    <td>
                      <select v-model="form_02.ordemp" class="form-select">
                        <option v-for="item in userData" :key="item.userid" :value="item.userid">{{ item.usernm }}</option>
                      </select>
                    </td>
                  </tr>
                  <tr>
                    <th class="bg-light">특기사항</th>
                    <td colspan="7"><input v-model="form_02.remark" class="form-control" /></td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- 상세 품목 그리드 영역 -->
          <div class="card border shadow-sm flex-grow-1 d-flex flex-column overflow-hidden grid-container-right">
            <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
              <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>주문 품목 리스트</span>
              <div class="d-flex gap-1">
                <button class="btn btn-sm btn-outline-primary py-0 px-2 fw-bold" @click="addRow" style="font-size: 11px;">+ 행추가</button>
                <button class="btn btn-sm btn-outline-danger py-0 px-2 fw-bold" @click="deleteSelectedRows" style="font-size: 11px;">- 행삭제</button>
              </div>
            </div>
            <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
              <div ref="tableRef2" class="tabulator-instance flex-grow-1"></div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, computed, watch, nextTick } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import DateForm from '@/components/DateForm.vue'
import AddressPopupForm from '@/components/AddressPopupForm.vue'
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

// [1] 데이터 모델링 (모두 소문자 원칙)
const form_01 = reactive({ fromdt: firstDay, todt: today, schcustnm: '' })
const form_02 = reactive<any>({
  actkind: 'S0', cmpycd: authStore.cmpycd,
  ordym: today.replace(/-/g, '').substring(0, 6), ordno: '0000',
  deptcd: authStore.deptcd, deptnm: authStore.deptnm,
  ordymd: today, custcd: '', custnm: '', outymd: today,
  ordkind: '100', paycndt: '110', ordemp: authStore.userid,
  remark: '', sts: 'Y', totsum: 0, trancd: '', address: '', postno: '', d_address: ''
})

const displayOrdNo = computed(() => (!form_02.ordno || form_02.ordno === '0000') ? '' : `${form_02.ordym}-${form_02.ordno}`)
watch(() => form_02.ordymd, (nv) => { if (nv) form_02.ordym = nv.replace(/-/g, '').substring(0, 6) })

const closingInfo = reactive({ sclsym: '' })
const ordkindData = ref<any[]>([]); const paycndtData = ref<any[]>([]);
const userData = ref<any[]>([])

const tableRef1 = ref<HTMLDivElement | null>(null); const tableRef2 = ref<HTMLDivElement | null>(null)
let grid1: Tabulator | null = null; let grid2: Tabulator | null = null

const initGrids = () => {
  grid1 = new Tabulator(tableRef1.value!, {
    layout: "fitColumns", height: "100%", placeholder: "데이터 없음",
    columns: [
      { title: "No", formatter: "rownum", width: 40, hozAlign: "center", headerSort: false },
      { title: "거래처명", field: "custnm", hozAlign: "left", headerSort: false },
      { title: "주문번호", field: "ordno_full", hozAlign: "center", width: 120, cssClass: "fw-bold text-primary", headerSort: false }
    ],
  });
  grid1.on("rowClick", (e, row) => fetchDetail(row.getData()));

  grid2 = new Tabulator(tableRef2.value!, {
    layout: "fitColumns", height: "100%", placeholder: "품목 없음", selectable: true,
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "선택", width: 40, hozAlign: "center", formatter: "rowSelection", titleFormatter: "rowSelection" },
      { title: "상태", field: "_status", width: 60, hozAlign: "center", formatter: (c) => {
          const v = c.getValue();
          if (v === '입력') return '<span class="badge bg-primary">입력</span>';
          if (v === '수정') return '<span class="badge bg-warning text-dark">수정</span>';
          if (v === '삭제') return '<span class="badge bg-danger">삭제</span>';
          return '';
      }},
      { title: "품목코드", field: "itemcd", width: 100, hozAlign: "center", cellClick: (e, cell) => handleOpenHelp('ITEM', cell.getRow()) },
      { title: "품목명", field: "itemnm", minWidth: 200, widthGrow: 1, cssClass: 'fw-bold text-primary', cellClick: (e, cell) => handleOpenHelp('ITEM', cell.getRow()) },
      { title: "단위", field: "unit", width: 70, hozAlign: "center" },
      { title: "수량", field: "ordqty", width: 80, hozAlign: "right", editor: "number", formatter: "money", formatterParams: { precision: 0 }, cellEdited: (cell) => calcRow(cell.getRow()) },
      { title: "단가", field: "price", width: 100, hozAlign: "right", editor: "number", formatter: "money", formatterParams: { precision: 0 }, cellEdited: (cell) => calcRow(cell.getRow()) },
      { title: "금액", field: "ordamt", width: 110, hozAlign: "right", editor: "number", formatter: "money", formatterParams: { precision: 0 }, cellEdited: (cell) => calcRowAmt(cell.getRow()) },
      { title: "부가세", field: "ordvat", width: 100, hozAlign: "right", editor: "number", formatter: "money", formatterParams: { precision: 0 }, cellEdited: (cell) => calcRowVat(cell.getRow()) },
      { title: "합계", field: "amtsum", width: 110, hozAlign: "right", editor: "number", formatter: "money", formatterParams: { precision: 0 }, cellEdited: (cell) => calcRowTotal(cell.getRow()) },
      { title: "삭제", width: 40, hozAlign: "center", formatter: (c) => "<i class='bi bi-trash text-danger'></i>", cellClick: (e, cell) => handleRowAction(cell.getRow()) }
     ]
  });
}

const updateRowStatus = (row: any) => {
  const d = row.getData();
  if (d._state === 'EXIST' && d._status !== '삭제') row.update({ _status: '수정' });
}

const calcRow = (row: any) => {
  const d = row.getData();
  const amt = Math.floor(Number(d.ordqty || 0) * Number(d.price || 0));
  const vat = Math.floor(amt * 0.1);
  row.update({ ordamt: amt, ordvat: vat, amtsum: amt + vat });
  updateRowStatus(row);
}

const calcRowAmt = (row: any) => {
  const d = row.getData();
  const amt = Number(d.ordamt || 0);
  const vat = Math.floor(amt * 0.1);
  row.update({ ordvat: vat, amtsum: amt + vat });
  updateRowStatus(row);
}

const calcRowVat = (row: any) => {
  const d = row.getData();
  const amt = Number(d.ordamt || 0);
  const vat = Number(d.ordvat || 0);
  row.update({ amtsum: amt + vat });
  updateRowStatus(row);
}

const calcRowTotal = (row: any) => {
  const d = row.getData();
  const total = Number(d.amtsum || 0);
  const amt = Math.round(total / 1.1);
  const vat = total - amt;
  row.update({ ordamt: amt, ordvat: vat });
  updateRowStatus(row);
}

async function search() {
  try {
    const res = await api.post('/hsod/HSOD_100U_STR', {
        actkind: 'S1', cmpycd: authStore.cmpycd,
        fromdt: form_01.fromdt.replace(/-/g, ''), todt: form_01.todt.replace(/-/g, ''),
        custnm: form_01.schcustnm, ordno: '0000', ordkind: '100', ordemp: authStore.userid
    });
    grid1?.setData(res.data.map((i: any) => ({ ...i, ordno_full: `${i.ordym}-${i.ordno}` })));
    vAlert('조회되었습니다.');
  } catch (e: any) { vAlertError('조회 실패'); }
}

async function fetchDetail(row: any) {
  const fYmd = (d: string) => d && d.length === 8 ? `${d.substring(0, 4)}-${d.substring(4, 6)}-${d.substring(6, 8)}` : today;
  Object.assign(form_02, { ...row, ordymd: fYmd(row.ordymd), outymd: fYmd(row.outymd) });
  try {
    const res = await api.post('/hsod/HSOD_101U_STR', { actkind: 'S0', cmpycd: authStore.cmpycd, ordym: row.ordym, ordno: row.ordno });
    grid2?.setData(res.data.map((i: any) => {
      const ordqty = Number(i.ordqty || 0);
      const ordamt = Number(i.ordamt || 0);
      const ordvat = Number(i.ordvat || 0);
      const price = ordqty > 0 ? Math.round(ordamt / ordqty) : 0;
      return { ...i, _state: 'EXIST', _status: '', price: price, amtsum: ordamt + ordvat };
    }));
  } catch (e: any) { vAlertError('상세 로드 실패'); }
}

async function save() {
  if (!form_02.custcd) return vAlertError('거래처를 선택하세요.');
  const details = grid2?.getData().filter((r: any) => r._status) || [];
  if (!details.length && form_02.ordno === '0000') return vAlertError('항목을 추가하세요.');

  try {
    const mst = {
    ...form_02,
    actkind: form_02.ordno === '0000' ? 'A0' : 'U0',
    ordymd: form_02.ordymd.replace(/-/g, ''),
    outymd: form_02.outymd.replace(/-/g, ''),
    sts: 'Y',
    updemp: authStore.userid };
    const dtl = details.map((d: any) => ({ ...d, actkind: d._status === '입력' ? 'A0' : (d._status === '삭제' ? 'D0' : 'U0'), updemp: authStore.userid }));
    await api.post('/hsod/HSOD_100U_SAVE', { mst, dtl });
    vAlert('저장되었습니다.'); search();
  } catch (e: any) { vAlertError('저장 오류'); }
}

const handleOpenHelp = (type: string, target?: any) => {
  if (type === 'DEPT') {
    Object.assign(modalProps, {
      title: '부서 선택',
      path: '/ha00/HA00_00P_STR',
      defaultField: 'deptnm',
      data: { gubun: 'D0', cmpycd: authStore.cmpycd, gbncd: '', code: '', remark: '' },
      columns: [
        { title: '코드', field: 'deptcd', width: 80, hozAlign: 'center' },
        { title: '부서명', field: 'deptnm', width: 200 }
      ],
      onConfirm: (d: any) => { form_02.deptcd = d.deptcd; form_02.deptnm = d.deptnm }
    })
    modalVisible.value = true
  } else if (type === 'CUST') {
    Object.assign(modalProps, {
      title: '거래처 선택',
      path: '/ha00/HA00_00P_STR',
      defaultField: 'custnm',
      data: { gubun: 'C4', cmpycd: authStore.cmpycd, gbncd: '', code: '', remark: '' },
      columns: [
        { title: '코드', field: 'custcd', width: 80, hozAlign: 'center' },
        { title: '거래처명', field: 'custnm', width: 200 }
      ],
      onConfirm: (d: any) => { form_02.custcd = d.custcd; form_02.custnm = d.custnm }
    })
    modalVisible.value = true

    } else if (type === 'ADDR') {
    if (!form_02.custcd) return vAlertError('거래처를 먼저 선택하세요.')
    Object.assign(modalProps, {
      title: '배송처 선택',
      path: '/hs00/HS00_000S_STR',
      defaultField: 'trannm',
      data: { gubun: 'T0', cmpycd: authStore.cmpycd, gbncd: '', code: form_02.custcd },
      columns: [
        { title: '코드', field: 'trancd', width: 60, hozAlign: 'center' },
        { title: '배송처명', field: 'custnm', width: 100 },
        { title: '우편번호', field: 'postno', width: 50 },
        { title: '주소', field: 'address', width: 200 },
        { title: '상세주소', field: 'd_address', width: 100 }
      ],
      onConfirm: (d: any) => {
        form_02.trancd = d.trancd;
        form_02.postno = d.postno;
        form_02.address = d.address;
        form_02.d_address = d.d_address || '';
      }
    })
    modalVisible.value = true
  } else if (type === 'ITEM') {
    Object.assign(modalProps, {
      title: '품목 선택',
      path: '/hs00/HS00_000S_STR',
      defaultField: 'itemnm',
      data: { gubun: 'I1', cmpycd: authStore.cmpycd, gbncd: '2', code: '', remark: '' },
      columns: [
        { title: '품목코드', field: 'itemcd', width: 100, hozAlign: 'center' },
        { title: '품목명', field: 'itemnm', width: 200 },
        { title: '규격', field: 'itsize', width: 150 },
        { title: '단위', field: 'unit', width: 80, hozAlign: 'center' }
      ],
      onConfirm: (d: any) => {
        const price = d.outcost || 0;
        const vat = Math.floor(price * 0.1);
        target.update({
          itemcd: d.itemcd,
          itemnm: d.itemnm,
          unit: d.unit || 'EA',
          price: price,
          ordqty: 1,
          ordamt: price,
          ordvat: vat,
          amtsum: price + vat,
          _status: '입력',
          _state: 'NEW'
        })
      }
    })
    modalVisible.value = true
  }
}

const handleRowAction = (row: any) => { const d = row.getData(); if (d._state === 'NEW') row.delete(); else row.update({ _status: d._status === '삭제' ? '' : '삭제' }); }
const deleteSelectedRows = () => { const sel = grid2?.getSelectedRows(); if (sel?.length) sel.forEach(row => handleRowAction(row)); }
const addRow = () => grid2?.addRow({ ordqty: 0, price: 0, ordamt: 0, ordvat: 0, amtsum: 0, _status: '입력', _state: 'NEW' }, true);
const initialize = () => {
    resetForm(form_02);
    Object.assign(form_02, {
        cmpycd: authStore.cmpycd,
        ordno: '0000',
        ordymd: today,
        ordym: today.replace(/-/g, '').substring(0, 6),
        outymd: today,
        sts: 'Y',
        deptcd: authStore.deptcd,
        deptnm: authStore.deptnm,
        ordemp: authStore.userid,
        ordkind: '100',
        paycndt: '110' });

    grid1?.clearData(); grid2?.clearData();
}

async function handleFullDelete() {
  if (!confirm('정말 전체 삭제하시겠습니까?')) return;
  try {
    await api.post('/hsod/HSOD_100U_SAVE', { mst: { ...form_02, actkind: 'D0', ordymd: form_02.ordymd.replace(/-/g, ''), outymd: form_02.outymd.replace(/-/g, '') }, dtl: [] });
    vAlert('삭제되었습니다.'); initialize(); search();
  } catch (e) { vAlertError('삭제 실패'); }
}

onMounted(async () => {
    nextTick(initGrids);
    api.post('/hs00/HS00_000S_STR', { gubun: 'E0', cmpycd: authStore.cmpycd, gbncd: '220' }).then(r => {
        ordkindData.value = r.data;
    });
    api.post('/hs00/HS00_000S_STR', { gubun: 'E0', cmpycd: authStore.cmpycd, gbncd: '300' }).then(r => {
        paycndtData.value = r.data;
    });
    api.get('/hp00/HP00_000S_STR', { params: { gubun: 'CL' } }).then(r => {
        if(r.data?.length) closingInfo.sclsym = r.data[0].sclsym;
    });
    api.post('/ha00/HA00_00P_STR', { gubun: 'SD', cmpycd: authStore.cmpycd, gbncd: '', code: '', remark: '' }).then(r => {
        userData.value = r.data;
    });

    initialize();
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
