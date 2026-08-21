<!--
	=============================================================
	프로그램명	: 발주등록(구매요청분) (HSIO050U)
	작성일자	: 2025.02.24
	설명        : 발주 마스터/상세 관리 (화면 데이터 기반 출력/메일 기능 탑재)
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
        구매관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        발주관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">발주등록(구매요청분) (HSIO050U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-success" @click="handleOpenHelp('REQ')">구매요청</button>
        <button class="btn-erp btn-primary" @click="printOrder" :disabled="!form_02.balno || form_02.balno === '0000'">발주서 출력</button>

        <!-- 🚀 [개선] 메일 전송 버튼 스타일 강화 -->
        <button class="btn btn-sm btn-info text-white fw-bold px-3" @click="sendMail" :disabled="!form_02.balno || form_02.balno === '0000'" style="height: 32px; min-width: 80px;">
          <i class="bi bi-envelope-at me-1"></i> 메일 전송
        </button>

        <button class="btn-erp btn-save" @click="save" :disabled="isClosed">저장</button>
        <button class="btn-erp btn-delete" @click="handleFullDelete" :disabled="!form_02.balno || form_02.balno === '0000' || isClosed">전체삭제</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
                <col style="width: 10%" /><col style="width: 40%" />
                <col style="width: 10%" /><col style="width: 40%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light">발주일자</th>
                <td class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                  <DateForm v-model:fromdt="form_01.fromdt" v-model:todt="form_01.todt" />
                </td>
                <th class="text-center bg-light">거래처명</th>
                <td>
                  <input v-model="form_01.custnm" class="form-control form-control-sm" placeholder="거래처명 검색" @keyup.enter="search" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="d-flex gap-2 flex-grow-1 overflow-hidden" style="min-height: 0;">
        <div class="card border shadow-sm d-flex flex-column overflow-hidden grid-container-left" style="width: 320px; min-width: 320px;">
          <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark">발주 목록</div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="tableRef1" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>

        <div class="flex-grow-1 d-flex flex-column gap-2 overflow-hidden">
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
                    <th class="required bg-light">발주부서</th>
                    <td colspan="3">
                      <div class="input-group input-group-sm">
                        <input v-model="form_02.deptnm" class="form-control" readonly />
                        <button class="btn btn-outline-secondary" @click="handleOpenHelp('DEPT')" :disabled="isClosed"><i class="bi bi-search"></i></button>
                      </div>
                    </td>
                    <th class="bg-light">발주번호</th>
                    <td>
                      <input :value="displayBalNo" class="form-control bg-light text-primary fw-bold text-center" readonly placeholder="자동생성" />
                    </td>
                    <th class="required bg-light">발주일자</th>
                    <td><input v-model="form_02.balymd" type="date" class="form-control" :readonly="isClosed" /></td>
                  </tr>
                  <tr>
                    <th class="required bg-light">거래처</th>
                    <td colspan="3">
                      <div class="input-group input-group-sm">
                        <input v-model="form_02.custnm" class="form-control" readonly />
                        <button class="btn btn-outline-secondary" @click="handleOpenHelp('CUST')" :disabled="isClosed"><i class="bi bi-search"></i></button>
                      </div>
                    </td>
                    <th class="bg-light">요청번호</th>
                    <td>
                      <input :value="displayReqNo" class="form-control bg-light text-primary fw-bold text-center" readonly placeholder="연동번호" />
                    </td>
                    <th class="required bg-light">입고일자</th>
                    <td><input v-model="form_02.reqymd" type="date" class="form-control" :readonly="isClosed" /></td>
                  </tr>
                  <tr>
                    <th class="bg-light">특기사항</th>
                    <td colspan="3"><input v-model="form_02.remark" class="form-control" :readonly="isClosed" /></td>
                    <th class="required bg-light">담당자</th>
                    <td>
                      <select v-model="form_02.userid" class="form-select" :disabled="isClosed">
                        <option v-for="item in userData" :key="item.userid" :value="item.userid">{{ item.usernm }}</option>
                      </select>
                    </td>
                    <th class="bg-light">이메일</th>
                    <td><input v-model="form_02.email" class="form-control" :readonly="isClosed" /></td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <div class="card border shadow-sm flex-grow-1 d-flex flex-column overflow-hidden grid-container-right">
            <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
              <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>발주 품목 리스트</span>
              <div class="d-flex gap-1">
                <button class="btn btn-sm btn-outline-primary py-0 px-2 fw-bold" @click="addRow" :disabled="isClosed" style="font-size: 11px;">+ 행추가</button>
                <button class="btn btn-sm btn-outline-danger py-0 px-2 fw-bold" @click="deleteSelectedRows" :disabled="isClosed" style="font-size: 11px;">- 행삭제</button>
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
import { reactive, ref, onMounted, computed, watch, nextTick, onUnmounted } from 'vue'
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
const form_01 = reactive({ fromdt: firstDay, todt: today, custnm: '' })
const form_02 = reactive<any>({
  actkind: 'S', cmpycd: authStore.cmpycd,
  balym: today.replace(/-/g, '').substring(0, 6), balno: '0000',
  balymd: today, reqymd: today,
  deptcd: authStore.deptcd, deptnm: authStore.deptnm,
  custcd: '', custnm: '', userid: authStore.userid,
  remark: '', totsum: 0, balgb: '1', reqno: '', reqym: '', email: ''
})

const displayBalNo = computed(() => (!form_02.balno || form_02.balno === '0000') ? '' : `${form_02.balym}-${form_02.balno}`)
const displayReqNo = computed(() => (!form_02.reqno || form_02.reqno === '0000') ? '' : `${form_02.reqym}-${form_02.reqno}`)

watch(() => form_02.balymd, (nv) => { if (nv) form_02.balym = nv.replace(/-/g, '').substring(0, 6) })

const closingInfo = reactive({ sclsym: '' }); const userData = ref<any[]>([])
const tableRef1 = ref<HTMLDivElement | null>(null); const tableRef2 = ref<HTMLDivElement | null>(null)
let grid1: Tabulator | null = null; let grid2: Tabulator | null = null

const isClosed = computed(() => closingInfo.sclsym && form_02.balymd.replace(/-/g, '').substring(0, 6) <= closingInfo.sclsym)

const initGrids = () => {
  grid1 = new Tabulator(tableRef1.value!, {
    layout: "fitColumns", height: "100%", placeholder: "데이터 없음",
    columns: [
      { title: "No", formatter: "rownum", width: 40, hozAlign: "center", headerSort: false },
      { title: "거래처명", field: "custnm", hozAlign: "left", headerSort: false },
      { title: "발주번호", field: "balno_full", hozAlign: "center", width: 140, cssClass: "fw-bold text-primary", headerSort: false }
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
      { title: "품목명", field: "itemnm", minWidth: 150, widthGrow: 1, cssClass: 'fw-bold text-primary', cellClick: (e, cell) => handleOpenHelp('ITEM', cell.getRow()) },
      { title: "규격", field: "itsize", width: 100 },
      { title: "단위", field: "unit", width: 60, hozAlign: "center" },
      { title: "재고", field: "jqty", width: 80, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "수량", field: "balqty", width: 80, hozAlign: "right", editor: "number", bottomCalc: "sum", cellEdited: (cell) => calcRow(cell.getRow(), 'balqty') },
      { title: "단가", field: "price", width: 90, hozAlign: "right", editor: "number", cellEdited: (cell) => calcRow(cell.getRow(), 'price') },
      { title: "금액", field: "balamt", width: 100, hozAlign: "right", editor: "number", bottomCalc: "sum", cellEdited: (cell) => calcRow(cell.getRow(), 'balamt') },
      { title: "부가세", field: "balvat", width: 90, hozAlign: "right", editor: "number", bottomCalc: "sum", cellEdited: (cell) => calcRow(cell.getRow(), 'balvat') },
      { title: "합계", field: "amtsum", width: 110, hozAlign: "right", formatter: "money", bottomCalc: "sum", cssClass: "fw-bold text-dark bg-light" },
      { title: "매출처", field: "scustnm", width: 120, cellClick: (e, cell) => handleOpenHelp('SCUST', cell.getRow()) },
      { title: "삭제", width: 40, hozAlign: "center", formatter: (c) => "<i class='bi bi-trash text-danger'></i>", cellClick: (e, cell) => handleRowAction(cell.getRow()) }
    ]
  });
}

const calcRow = (row: any, field: string) => {
  const d = row.getData();
  let qty = Number(d.balqty || 0); let price = Number(d.price || 0);
  let amt = Number(d.balamt || 0); let vat = Number(d.balvat || 0);

  if (field === 'balqty' || field === 'price') {
    amt = Math.floor(qty * price); vat = Math.floor(amt * 0.1);
  } else if (field === 'balamt') {
    vat = Math.floor(amt * 0.1); if (qty > 0) price = Math.round(amt / qty);
  }
  const sum = amt + vat;
  row.update({ price, balamt: amt, balvat: vat, amtsum: sum });
  if (d._state === 'EXIST' && d._status !== '삭제') row.update({ _status: '수정' });
}

async function search() {
  try {
    const res = await api.post('/hsio/HSIO_050U_STR', {
        actkind: 'L', cmpycd: authStore.cmpycd,
        fromdt: form_01.fromdt.replace(/-/g, ''), todt: form_01.todt.replace(/-/g, ''),
        custnm: form_01.custnm, gubun: '1'
    });
    grid1?.setData(res.data.map((i: any) => ({
    ...i,
    reqno_full: `${i.ordym}-${i.ordno}`,
    balno_full: `${i.balym}-${i.balno}` })));
    vAlert('조회되었습니다.');
  } catch (e: any) { vAlertError('조회 실패'); }
}

async function fetchDetail(row: any) {
  const fYmd = (d: string) => d && d.length === 8 ? `${d.substring(0, 4)}-${d.substring(4, 6)}-${d.substring(6, 8)}` : today;
  try {
    const mstRes = await api.post('/hsio/HSIO_050U_STR', {
      actkind: 'S', cmpycd: authStore.cmpycd, balym: row.balym, balno: row.balno
    });

    if (mstRes.data?.length) {
      const mst = mstRes.data[0];
      Object.assign(form_02, { ...mst, balymd: fYmd(mst.balymd), reqymd: fYmd(mst.reqymd) });
    } else {
      Object.assign(form_02, { ...row, balymd: fYmd(row.balymd), reqymd: fYmd(row.reqymd) });
    }

    const res = await api.post('/hsio/HSIO_051U_STR', { actkind: 'S', cmpycd: authStore.cmpycd, balym: row.balym, balno: row.balno });
    grid2?.setData(res.data.map((i: any) => ({ ...i, amtsum: (Number(i.balamt || 0) + Number(i.balvat || 0)), _state: 'EXIST', _status: '' })));
  } catch (e) { vAlertError('데이터 로드 실패'); }
}

async function save() {
  if (!form_02.custcd) return vAlertError('거래처를 선택하세요.');
  const details = grid2?.getData().filter((r: any) => r._status) || [];
  if (!details.length && form_02.balno === '0000') return vAlertError('항목을 추가하세요.');

  try {
    const mst = {
        ...form_02,
        actkind: form_02.balno === '0000' ? 'A' : 'U',
        balymd: form_02.balymd.replace(/-/g, ''), reqymd: form_02.reqymd.replace(/-/g, ''),
        ordym: form_02.reqym.replace(/-/g, ''), ordno: form_02.reqno,
        gubun: '1',
        updemp: authStore.userid
    };

    const dtl = details.map((d: any) => ({
        ...d,
        cmpycd: authStore.cmpycd,
        actkind: d._status === '입력' ? 'A' : (d._status === '삭제' ? 'D' : 'U'),
        updemp: authStore.userid
    }));

    await api.post('/hsio/HSIO_050U_SAVE', { mst, dtl });
    vAlert('저장되었습니다.'); search();
  } catch (e) { vAlertError('저장 실패'); }
}

/** 🚀 [추가] 화면 데이터 기반 발주서 HTML 생성 */
const generateOrderHtml = async () => {
    const mst = form_02;
    const dtl = grid2?.getData() || [];
    const companyRes = await api.post('/comm/HABA_100U_STR', { actkind: 'S0', cmpycd: authStore.cmpycd });
    const cInfo = companyRes.data?.[0] || {};
    const gLines = [];
    ['gline1', 'gline2', 'gline3', 'gline4', 'gline5'].forEach(k => {
        const v = String(cInfo[k] || '').trim(); if (v) gLines.push(v);
    });
    const gWidth = gLines.length === 5 ? 45 : (gLines.length === 4 ? 40 : (gLines.length === 3 ? 33 : 24));
    const fC = (n: any) => Number(n || 0).toLocaleString();
    let totalAmt = 0, totalVat = 0;
    let rowsHtml = '';
    for (let i = 0; i < Math.max(dtl.length, 14); i++) {
        const item = dtl[i] || {};
        if (item.itemnm) {
            const qty = Number(item.balqty || 0); const amt = Number(item.balamt || 0); const vat = Number(item.balvat || 0);
            totalAmt += amt; totalVat += vat;
            rowsHtml += `<tr height="35"><td align="left">${item.itemnm}</td><td>${item.itsize || ''}</td><td>${item.unit || ''}</td><td align="right">${fC(qty)}</td><td align="right">${fC(qty > 0 ? amt/qty : 0)}</td><td align="right">${fC(amt)}</td><td align="right">${fC(vat)}</td><td align="right">${fC(amt+vat)}</td><td>&nbsp;</td></tr>`;
        } else { rowsHtml += `<tr height="35"><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>`; }
    }
    return `<html><head><style>body { font-family: 'GulimChe', sans-serif; font-size: 9pt; } table { width: 100%; border-collapse: collapse; } th, td { border: 1px solid #333; padding: 4px; text-align: center; }</style></head>
    <body onload="window.print()">
        <table border="0" style="margin-bottom:20px"><tr><td width="${100-gWidth}%" style="font-size:20pt; font-weight:bold">발&nbsp;&nbsp;주&nbsp;&nbsp;서</td><td width="${gWidth}%">
        <table border="1"><tr><td rowspan="2" width="20px">결재</td>${gLines.map(g => `<td>${g}</td>`).join('')}</tr><tr>${gLines.map(()=>`<td height="50px"></td>`).join('')}</tr></table></td></tr></table>
        <table border="1"><tr><th>발신</th><td>${authStore.cmpynm}</td><th>발주일자</th><td>${mst.balymd}</td></tr><tr><th>수신</th><td>${mst.custnm}</td><th>납품기한</th><td>${mst.reqymd}</td></tr></table>
        <table border="1" style="margin-top:10px"><thead><tr bgcolor="#eee"><th>품목</th><th>규격</th><th>단위</th><th>수량</th><th>단가</th><th>금액</th><th>부가세</th><th>합계</th><th>비고</th></tr></thead>
        <tbody>${rowsHtml}</tbody><tfoot><tr bgcolor="#eee"><td>합계</td><td colspan="4"></td><td align="right">${fC(totalAmt)}</td><td align="right">${fC(totalVat)}</td><td align="right">${fC(totalAmt + totalVat)}</td><td></td></tr></tfoot></table>
    </body></html>`;
};

/** 🚀 [추가] 발주서 출력 */
const printOrder = async () => {
    const html = await generateOrderHtml();
    const win = window.open('', '_blank', 'width=800,height=900');
    win?.document.write(html); win?.document.close();
};

/** 🚀 [추가] 발주서 메일 전송 */
const sendMail = async () => {
    const mst = form_02;
    if (!mst.balno || mst.balno === '0000') return vAlertError('발주서를 먼저 조회하세요.');
    if (!mst.email || !mst.email.includes('@')) return vAlertError('유효한 거래처 메일 주소가 없습니다.');
    if (!confirm(`${mst.email}로 발주서를 전송하시겠습니까?`)) return;
    try {
        const html = await generateOrderHtml();
        await api.post('/mail/send-bal', { htmlcontent: html, email: mst.email, custnm: mst.custnm, custcd: mst.custcd, docgb: 'BAL', no: `${mst.balym}-${mst.balno}` });
        vAlert('메일이 성공적으로 전송되었습니다.');
    } catch (e) { vAlertError('메일 전송 실패'); }
};

const handleOpenHelp = (type: string, target?: any) => {
  if (type === 'DEPT') {
    Object.assign(modalProps, {
      title: '부서 선택', path: '/ha00/HA00_00P_STR', defaultField: 'deptnm',
      data: { gubun: 'D0', cmpycd: authStore.cmpycd, gbncd: '', code: '', remark: '' },
      columns: [{ title: '코드', field: 'deptcd', width: 80, hozAlign: 'center' }, { title: '부서명', field: 'deptnm', width: 200 }],
      onConfirm: (d: any) => { form_02.deptcd = d.deptcd; form_02.deptnm = d.deptnm; }
    });
    modalVisible.value = true;
  } else if (type === 'CUST') {
    Object.assign(modalProps, {
      title: '거래처 선택', path: '/ha00/HA00_00P_STR', defaultField: 'custnm',
      data: { gubun: 'C4', cmpycd: authStore.cmpycd, gbncd: '', code: '', remark: '' },
      columns: [{ title: '코드', field: 'custcd', width: 80, hozAlign: 'center' }, { title: '거래처명', field: 'custnm', width: 200 }],
      onConfirm: (d: any) => { form_02.custcd = d.custcd; form_02.custnm = d.custnm; form_02.email = d.email || ''; }
    });
    modalVisible.value = true;
  } else if (type === 'ITEM') {
    Object.assign(modalProps, {
      title: '품목 선택', path: '/hs00/HS00_000S_STR', defaultField: 'itemnm',
      data: { gubun: 'I1', cmpycd: authStore.cmpycd, gbncd: '3', code: '', remark: '' },
      columns: [{ title: '품목코드', field: 'itemcd', width: 100 }, { title: '품목명', field: 'itemnm', width: 200 }, { title: '규격', field: 'itsize', width: 150 }, { title: '단위', field: 'unit', width: 80 }],
      onConfirm: (d: any) => {
        target.update({ itemcd: d.itemcd, itemnm: d.itemnm, itsize: d.itsize, unit: d.unit, price: d.incost || 0, balqty: 1, _status: '입력', _state: 'NEW' });
        calcRow(target, 'balqty');
      }
    });
    modalVisible.value = true;
  } else if (type === 'SCUST' || type === 'IDEPT' || type === 'pkunit') {
    const isDept = type === 'IDEPT'; const isPk = type === 'pkunit';
    Object.assign(modalProps, {
      title: isDept ? '입고부서 선택' : (isPk ? '포장용기 선택' : '매출처 선택'),
      path: '/ha00/HA00_00P_STR',
      data: { gubun: isDept ? 'D1' : (isPk ? 'PK' : 'C4'), cmpycd: authStore.cmpycd, gbncd: '', code: '', remark: '' },
      columns: [{ title: '코드', field: isDept ? 'deptcd' : (isPk ? 'pkunit' : 'custcd'), width: 80 }, { title: '명칭', field: isDept ? 'deptnm' : (isPk ? 'pkunitnm' : 'custnm'), width: 200 }],
      onConfirm: (d: any) => {
        if(isDept) target.update({ ideptcd: d.deptcd, ideptnm: d.deptnm });
        else if(isPk) target.update({ pkunit: d.pkunit, pkunitnm: d.pkunitnm });
        else target.update({ scustcd: d.custcd, scustnm: d.custnm });
        if (target.getData()._state === 'EXIST') target.update({ _status: '수정' }); else if (target.getData()._status !== '입력') target.update({ _status: '입력' });
      }
    });
    modalVisible.value = true;
  } else if (type === 'REQ') {
    Object.assign(modalProps, {
      title: '구매요청 조회', path: '/hsio/HSIO_010U_STR', defaultField: 'reqno',
      data: {
        actkind: 'L',
        cmpycd: authStore.cmpycd,
        deptcd: authStore.deptcd,
        fromdt: form_01.fromdt.replace(/-/g, ''),
        todt: form_01.todt.replace(/-/g, '')
      },
      columns: [
        { title: '요청일자', field: 'reqymd', width: 100, hozAlign: 'center', formatter: (c: any) => {
          const v = c.getValue(); return v && v.length === 8 ? `${v.substring(4,6)}-${v.substring(6,8)}` : v;
        }},
        { title: "요청번호", field: "reqno_full", hozAlign: "center", width: 120, cssClass: "fw-bold text-primary", formatter: (c: any) => { const d = c.getData(); return `${d.reqym}-${d.reqno}`; } },
        { title: '부서명', field: 'deptnm', width: 150, hozAlign: 'left' }
      ],
      onConfirm: async (d: any) => {
        Object.assign(form_02, { reqno: d.reqno, reqym: d.reqym, reqymd: d.reqymd, custcd: d.custcd, custnm: d.custnm });
        try {
          const res = await api.post('/hsio/HSIO_011U_STR', { actkind: 'S1', cmpycd: authStore.cmpycd, reqym: d.reqym, reqno: d.reqno });
          grid2?.setData(res.data.map((i: any) => {
            const amt = Number(i.reqamt || (i.reqqty * i.imprice)); const vat = Math.floor(amt * 0.1);
            return { ...i, balqty: i.reqqty, price: i.imprice, balamt: amt, balvat: vat, amtsum: amt + vat, ideptnm: i.deptnm, _status: '입력', _state: 'NEW' };
          }));
        } catch (e) { vAlertError('요청 상세 로드 실패'); }
      }
    });
    modalVisible.value = true;
  }
}

const handleRowAction = (row: any) => { const d = row.getData(); if (d._state === 'NEW') row.delete(); else row.update({ _status: d._status === '삭제' ? '' : '삭제' }); }
const deleteSelectedRows = () => grid2?.getSelectedRows().forEach(row => handleRowAction(row));
const addRow = () => grid2?.addRow({ balqty: 0, price: 0, balamt: 0, balvat: 0, amtsum: 0, jqty: 0, _status: '입력', _state: 'NEW' }, true);

function initialize() {
  resetForm(form_02);
  Object.assign(form_02, {
    cmpycd: authStore.cmpycd, balno: '0000', balym: today.replace(/-/g, '').substring(0, 6), balymd: today, reqymd: today,
    deptcd: authStore.deptcd, deptnm: authStore.deptnm, userid: authstore.userid, balgb: '1', reqno: '', reqym: '', email: ''
  });
  grid1?.clearData(); grid2?.clearData();
}

async function handleFullDelete() {
  if (!confirm('정말 전체 삭제하시겠습니까?')) return;
  try {
    const mst = { actkind: 'D0', cmpycd: authStore.cmpycd, balym: form_02.balym, balno: form_02.balno, updemp: authStore.userid };
    await api.post('/hsio/HSIO_050U_SAVE', { mst, dtl: [] });
    vAlert('삭제되었습니다.'); initialize(); search();
  } catch (e) { vAlertError('삭제 실패'); }
}

onUnmounted(() => {
  if (grid1) grid1.destroy();
  if (grid2) grid2.destroy();
});

onMounted(async () => {
    nextTick(initGrids);
    api.post('/ha00/HA00_00P_STR', { gubun: 'SD', cmpycd: authStore.cmpycd, gbncd: '', code: '', remark: '' }).then(r => { userData.value = r.data; });
    api.get('/hp00/HP00_000S_STR', { params: { gubun: 'CL', cmpycd: authStore.cmpycd } }).then(r => { if(r.data?.length) closingInfo.sclsym = r.data[0].sclsym; });
    initialize();
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
