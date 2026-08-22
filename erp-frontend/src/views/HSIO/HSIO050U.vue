<!--
	=============================================================
	프로그램명	: 발주등록(구매요청분) (HSIO050U)
	작성일자	: 2025.02.24
	설명        : 발주 마스터/상세 관리 (구매요청 팝업 및 레이아웃 복구 완결판)
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
        <button class="btn-erp btn-info" @click="sendMail" :disabled="!form_02.balno || form_02.balno === '0000'" style="color: #000 !important; font-weight: bold;">메일 전송</button>

        <button class="btn-erp btn-save" @click="save" :disabled="isClosed">저장</button>
        <button class="btn-erp btn-delete" @click="handleFullDelete" :disabled="!form_02.balno || form_02.balno === '0000' || isClosed">전체삭제</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup><col style="width: 10%" /><col style="width: 40%" /><col style="width: 10%" /><col style="width: 40%" /></colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light">발주일자</th>
                <td class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                  <DateForm v-model:fromdt="form_01.fromdt" v-model:todt="form_01.todt" />
                </td>
                <th class="text-center bg-light">거래처명</th>
                <td><input v-model="form_01.custnm" class="form-control form-control-sm" placeholder="거래처명 검색" @keyup.enter="search" /></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="d-flex gap-2 flex-grow-1 overflow-hidden" style="min-height: 0;">
        <div class="card border shadow-sm d-flex flex-column overflow-hidden grid-container-left" style="width: 350px; min-width: 350px;">
          <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark">발주 목록</div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column"><div ref="tableRef1" class="tabulator-instance flex-grow-1"></div></div>
        </div>

        <div class="flex-grow-1 d-flex flex-column gap-2 overflow-hidden">
          <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
            <div class="card-body p-0 bg-white">
              <table class="erp-table-dense w-100">
                <colgroup><col style="width: 110px;" /><col /><col style="width: 110px;" /><col /><col style="width: 110px;" /><col /><col style="width: 110px;" /><col /></colgroup>
                <tbody>
                  <tr>
                    <th class="required bg-light">발주부서</th>
                    <td colspan="3"><div class="input-group input-group-sm"><input v-model="form_02.deptnm" class="form-control" readonly /><button class="btn btn-outline-secondary" @click="handleOpenHelp('DEPT')" :disabled="isClosed"><i class="bi bi-search"></i></button></div></td>
                    <th class="bg-light">발주번호</th><td><input :value="displayIoNo" class="form-control bg-light text-primary fw-bold text-center" readonly placeholder="자동생성" /></td>
                    <th class="required bg-light">발주일자</th><td><input v-model="form_02.balymd" type="date" class="form-control" :readonly="isClosed" /></td>
                  </tr>
                  <tr>
                    <th class="required bg-light">거래처</th>
                    <td colspan="3"><div class="input-group input-group-sm"><input v-model="form_02.custnm" class="form-control" readonly /><button class="btn btn-outline-secondary" @click="handleOpenHelp('CUST')" :disabled="isClosed"><i class="bi bi-search"></i></button></div></td>
                    <th class="bg-light">이메일</th><td><input v-model="form_02.email" class="form-control" :readonly="isClosed" /></td>
                    <th class="required bg-light">입고일자</th><td><input v-model="form_02.reqymd" type="date" class="form-control" :readonly="isClosed" /></td>
                  </tr>
                  <tr>
                    <th class="bg-light">특기사항</th><td colspan="5"><input v-model="form_02.remark" class="form-control" :readonly="isClosed" /></td>
                    <th class="required bg-light">담당자</th><td><select v-model="form_02.userid" class="form-select" :disabled="isClosed"><option v-for="item in userData" :key="item.userid" :value="item.userid">{{ item.usernm }}</option></select></td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
          <div class="card border shadow-sm flex-grow-1 d-flex flex-column overflow-hidden grid-container-right">
            <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
              <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>발주 품목 리스트</span>
              <div class="d-flex gap-1"><button class="btn btn-sm btn-outline-primary py-0 px-2 fw-bold" @click="addRow" :disabled="isClosed" style="font-size: 11px;">+ 행추가</button><button class="btn btn-sm btn-outline-danger py-0 px-2 fw-bold" @click="deleteSelectedRows" :disabled="isClosed" style="font-size: 11px;">- 행삭제</button></div>
            </div>
            <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column"><div ref="tableRef2" class="tabulator-instance flex-grow-1"></div></div>
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
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { useCommonHelp } from '@/composables/useCommonHelp'
import { getDate } from '@/composables/useDate'
import { numberToHanja } from '@/utils/hanja'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import DateForm from '@/components/DateForm.vue'

const authStore = useAuthStore()
const { firstDay, today } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps, openHelp } = useCommonHelp()

const form_02 = reactive<any>({ cmpycd: authStore.cmpycd, balym: today.replace(/-/g, '').substring(0, 6), balno: '0000', balymd: today, reqymd: today, deptcd: authStore.deptcd, deptnm: authStore.deptnm, userid: authStore.userid, remark: '', email: '', balgb: '1', reqno: '', reqym: '' })
const form_01 = reactive({ fromdt: firstDay, todt: today, custnm: '' })

const companyConfig = ref<any>(null);
const ourInfo = ref<any>(null);

const displayIoNo = computed(() => (!form_02.balno || form_02.balno === '0000') ? '' : `${form_02.balym}-${form_02.balno}`)
watch(() => form_02.balymd, (nv) => { if (nv) form_02.balym = nv.replace(/-/g, '').substring(0, 6) })

const closingInfo = reactive({ sclsym: '' }); const userData = ref<any[]>([])
const tableRef1 = ref<HTMLDivElement | null>(null); const tableRef2 = ref<HTMLDivElement | null>(null)
let grid1: Tabulator | null = null; let grid2: Tabulator | null = null

const isClosed = computed(() => closingInfo.sclsym && form_02.balymd.replace(/-/g, '').substring(0, 6) <= closingInfo.sclsym)

const initGrids = () => {
  grid1 = new Tabulator(tableRef1.value!, { layout: "fitColumns", height: "100%", columns: [{ title: "No", formatter: "rownum", width: 40 }, { title: "거래처명", field: "custnm", hozAlign: "left" }, { title: "발주번호", field: "balno_full", width: 140, cssClass: "fw-bold text-primary", mutatorData: (v,d)=>`${d.balym}-${d.balno}` }] });
  grid1.on("rowClick", (e, row) => fetchDetail(row.getData()));
  grid2 = new Tabulator(tableRef2.value!, { layout: "fitColumns", height: "100%", selectable: true, columns: [{ title: "선택", width: 40, formatter: "rowSelection", titleFormatter: "rowSelection" }, { title: "품목명", field: "itemnm", minWidth: 200, cssClass: 'fw-bold text-primary', cellClick: (e, cell) => handleOpenHelp('ITEM', cell.getRow()) }, { title: "규격", field: "itsize", width: 120 }, { title: "수량", field: "balqty", width: 100, hozAlign: "right", editor: "number", cellEdited: (cell) => calcRow(cell.getRow(), 'balqty') }, { title: "단가", field: "price", width: 110, hozAlign: "right", editor: "number", cellEdited: (cell) => calcRow(cell.getRow(), 'price') }, { title: "금액", field: "balamt", width: 120, hozAlign: "right", formatter: "money" }, { title: "삭제", width: 40, formatter: () => "<i class='bi bi-trash text-danger'></i>", cellClick: (e, cell) => handleRowAction(cell.getRow()) }] });
}

const calcRow = (row: any, field: string) => {
  const d = row.getData(); let qty = Number(d.balqty || 0), price = Number(d.price || 0), amt = Number(d.balamt || 0), vat = Number(d.balvat || 0);
  if (field === 'balqty' || field === 'price') { amt = Math.floor(qty * price); vat = Math.floor(amt * 0.1); }
  else if (field === 'balamt') { vat = Math.floor(amt * 0.1); if (qty > 0) price = Math.round(amt / qty); }
  row.update({ price, balamt: amt, balvat: vat, amtsum: amt + vat });
  if (d._state === 'EXIST' && d._status !== '삭제') row.update({ _status: '수정' });
}

async function search() { const res = await api.post('/hsio/HSIO_050U_STR', { actkind: 'L', cmpycd: authStore.cmpycd, fromdt: form_01.fromdt.replace(/-/g, ''), todt: form_01.todt.replace(/-/g, ''), custnm: form_01.custnm, gubun: '1' }); grid1?.setData(res.data); }

async function fetchDetail(row: any) {
  const fYmd = (d: string) => d && d.length === 8 ? `${d.substring(0, 4)}-${d.substring(4, 6)}-${d.substring(6, 8)}` : today;
  const mstRes = await api.post('/hsio/HSIO_050U_STR', { actkind: 'S', cmpycd: authStore.cmpycd, balym: row.balym, balno: row.balno });
  if (mstRes.data?.length) { const mst = mstRes.data[0]; Object.assign(form_02, { ...mst, balymd: fYmd(mst.balymd), reqymd: fYmd(mst.reqymd) }); }
  const res = await api.post('/hsio/HSIO_051U_STR', { actkind: 'S', cmpycd: authStore.cmpycd, balym: row.balym, balno: row.balno });
  grid2?.setData(res.data.map((i: any) => ({ ...i, _state: 'EXIST', _status: '' })));
}

async function save() {
  if (!form_02.custcd) return vAlertError('거래처를 선택하세요.');
  const details = (grid2?.getData() || []).filter((r: any) => r._status); if (!details.length && form_02.balno === '0000') return vAlertError('항목을 추가하세요.');
  const mst = { ...form_02, actkind: form_02.balno === '0000' ? 'A' : 'U',
      balymd: form_02.balymd.replace(/-/g, ''),
      reqymd: form_02.reqymd.replace(/-/g, ''),
      gubun: '1',
      updemp: authStore.userid
  };
  const dtl = details.map((d: any) => ({ ...d,
      cmpycd: authStore.cmpycd,
      actkind: d._status === '입력' ? 'A' : (d._status === '삭제' ? 'D' : 'U'),
      deptcd: form_02.deptcd,
      custcd: form_02.custcd,
      balymd: form_02.balymd.replace(/-/g, ''),
      updemp: authStore.userid
  }));
  await api.post('/hsio/HSIO_050U_SAVE', { mst, dtl }); vAlert('저장되었습니다.'); search();
}

/** 🚀 [정밀 보정] 발주서 HTML 생성 */
const generateOrderHtml = () => {
    const mst = form_02; const dtl = grid2?.getData() || [];
    const cInfo = companyConfig.value || {};
    const cnm = authStore.cmpynm || "";
    const gLines = []; ['gline1', 'gline2', 'gline3', 'gline4', 'gline5'].forEach(k => { const v = String(cInfo[k] || '').trim(); if (v) gLines.push(v); });
    if (gLines.length === 0) gLines.push('담 당', '팀 장', '부 장', '사 장');
    const gWidth = gLines.length === 5 ? 45 : (gLines.length === 4 ? 40 : (gLines.length === 3 ? 33 : 24));
    const fC = (n: any) => Number(n || 0).toLocaleString();
    let totalAmt = 0, totalVat = 0, rowsHtml = '';
    dtl.forEach(i => {
        if (i.itemnm) {
            const qty = Number(i.balqty || 0); const amt = Number(i.balamt || 0); const vat = Number(i.balvat || 0);
            totalAmt += amt; totalVat += vat;
            rowsHtml += `<tr height="30"><td align="left" style="padding-left:3px; font-size:8.5pt;">${i.itemnm}</td><td align="right" style="padding-right:3px; font-size:8.5pt;">${fC(qty)}</td><td align="right" style="padding-right:3px; font-size:8.5pt;">${fC(qty > 0 ? amt/qty : 0)}</td><td align="right" style="padding-right:3px; font-size:8.5pt;">${fC(amt)}</td><td align="right" style="padding-right:3px; font-size:8.5pt;">${fC(vat)}</td><td align="right" style="padding-right:3px; font-size:8.5pt;">${fC(amt+vat)}</td><td>&nbsp;</td></tr>`;
        }
    });
    for(let k=dtl.length; k<14; k++) rowsHtml += '<tr height="30"><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>';
    return `<html><head><style>body { font-family: 'GulimChe', sans-serif; font-size: 8.5pt; color:black; margin:0; padding:10px; } table { width: 650px; border-collapse: collapse; margin: 0 auto; table-layout: fixed; } th, td { border: 1px solid #BDBDBD; padding: 4px; text-align: center; font-size: 8.5pt; } .bg-eee { background-color: #eee; } .align-start { text-align: left; padding-left: 140px; line-height: 1.6; }</style></head>
    <body onload="window.print()"><table border="0" style="border:0; height:72px; margin-bottom:10px;"><tr><td width="350px" align="center" style="font-size:20pt; font-weight:bold; vertical-align:middle; border:0;">발&nbsp;&nbsp;주&nbsp;&nbsp;서</td><td width="300px" align="right" valign="top" style="border:0;"><table border="1" style="width:100%; border-collapse:collapse; height:72px;"><tr><td rowspan="2" width="20px" class="bg-eee" style="font-size:10pt; line-height:1.2;">결<br>재</td>${gLines.map(g => `<td class="bg-eee" height="20" style="font-size:8.5pt;">${g}</td>`).join('')}</tr><tr>${gLines.map(()=>`<td height="52" width="55"></td>`).join('')}</tr></table></td></tr></table>
    <table border="1" style="width:650px; margin:0 auto; border-collapse:collapse;"><colgroup><col style="width:12%"/><col style="width:38%"/><col style="width:12%"/><col style="width:38%"/></colgroup><tr><th class="bg-eee">발&nbsp;&nbsp;&nbsp;&nbsp;신</th><td align="left">&nbsp;${cnm}</td><th class="bg-eee">발주일자</th><td align="left">&nbsp;${mst.balymd}</td></tr><tr><th class="bg-eee">수&nbsp;&nbsp;&nbsp;&nbsp;신</th><td align="left">&nbsp;${mst.custnm}</td><th class="bg-eee">납품기한</th><td align="left">&nbsp;${mst.reqymd}</td></tr><tr><td colspan="4" class="align-start">1. 귀사의 일약 번창하심을 기원합니다.<br>2. 아래와 같이 발주하오니 검토하시고 납기를 준수하여 주시기 바랍니다.</td></tr></table>
    <table border="1" style="width:650px; margin:5px auto 0 auto; border-collapse:collapse;"><thead><tr class="bg-eee" height="28px"><th>품목</th><th>수량</th><th>단가</th><th>공급가</th><th>부가세</th><th>합계</th><th>비고</th></tr></thead><tbody>${rowsHtml}</tbody><tfoot><tr class="bg-eee" style="font-weight:bold; height:32px;"><td colspan="3">합&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;계</td><td align="right" style="padding-right:3px; font-size:8pt;">\\ ${fC(totalAmt)}</td><td align="right" style="padding-right:3px; font-size:8pt;">\\ ${fC(totalVat)}</td><td align="right" style="padding-right:3px; font-size:8pt;">\\ ${fC(totalAmt+totalVat)}</td><td>&nbsp;</td></tr></tfoot></table><table border="1" style="width:650px; margin:5px auto 0 auto; border-collapse:collapse;"><tr height="65px"><th class="bg-eee" width="6%" style="font-size:8.5pt;">특<br>기<br>사<br>항</th><td align="left" valign="top" style="padding:5px; font-size:8.5pt;">${(mst.remark || '').replace(/\n/g, '<br>')}</td></tr></table></body></html>`;
};

const printOrder = () => { const win = window.open('', '_blank', 'width=800,height=900'); if (!win) return alert('팝업 차단 해제 필요'); win.document.write(generateOrderHtml()); win.document.close(); };
const sendMail = async () => { const mst = form_02; if (!mst.email || !mst.email.includes('@')) return vAlertError('거래처 메일 주소 없음'); if (!confirm(`${mst.email}로 전송하시겠습니까?`)) return; try { await api.post('/mail/send-bal', { htmlcontent: generateOrderHtml(), fromnm: authStore.cmpynm, email: mst.email, custnm: mst.custnm, custcd: mst.custcd, docgb: 'BAL', no: `${mst.balym}-${mst.balno}` }); vAlert('메일 전송 완료'); } catch (e) { vAlertError('메일 전송 실패'); } };

const handleOpenHelp = (type: string, target?: any) => {
  if (type === 'DEPT') { Object.assign(modalProps, { title: '부서 선택', path: '/ha00/HA00_00P_STR', data: { gubun: 'D0', cmpycd: authStore.cmpycd }, columns: [{ title: '코드', field: 'deptcd', width: 80 }, { title: '부서명', field: 'deptnm', width: 200 }], onConfirm: (d: any) => { form_02.deptcd = d.deptcd; form_02.deptnm = d.deptnm; } }); modalVisible.value = true; }
  else if (type === 'CUST') { Object.assign(modalProps, { title: '거래처 선택', path: '/ha00/HA00_00P_STR', data: { gubun: 'C4', cmpycd: authStore.cmpycd }, columns: [{ title: '코드', field: 'custcd', width: 100, hozAlign: 'center' }, { title: '거래처명', field: 'custnm', width: 250 }], onConfirm: (d: any) => { form_02.custcd = d.custcd; form_02.custnm = d.custnm; form_02.email = d.email || ''; } }); modalVisible.value = true; }
  else if (type === 'ITEM') { Object.assign(modalProps, { title: '품목 선택', path: '/hs00/HS00_000S_STR', data: { gubun: 'I1', cmpycd: authStore.cmpycd, gbncd: '3' }, columns: [{ title: '코드', field: 'itemcd', width: 120 }, { title: '품목명', field: 'itemnm', width: 250 }, { title: '규격', field: 'itsize', width: 150 }], onConfirm: (d: any) => { target.update({ itemcd: d.itemcd, itemnm: d.itemnm, itsize: d.itsize, unit: d.unit, price: d.incost || 0, balqty: 1, balamt: d.incost || 0, _status: '입력', _state: 'NEW' }); calcRow(target, 'balqty'); } }); modalVisible.value = true; }
  else if (type === 'REQ') {
    Object.assign(modalProps, {
      title: '구매요청 조회', path: '/hsio/HSIO_010U_STR',
      data: { actkind: 'L', cmpycd: authStore.cmpycd, fromdt: form_01.fromdt.replace(/-/g, ''), todt: form_01.todt.replace(/-/g, '') },
      columns: [
          { title: '요청일자', field: 'reqymd', width: 100, hozAlign: 'center', formatter: (c: any) => { const v = c.getValue(); return v && v.length === 8 ? `${v.substring(4,6)}-${v.substring(6,8)}` : v; } },
          { title: "요청번호", field: "reqno_full", hozAlign: "center", width: 120, cssClass: "fw-bold text-primary", mutatorData: (v,d)=>`${d.reqym}-${d.reqno}` },
          { title: '부서명', field: 'deptnm', width: 150, hozAlign: 'left' },
          { title: '거래처', field: 'custnm', width: 150, hozAlign: 'left' }
      ],
      onConfirm: async (d: any) => {
        Object.assign(form_02, { reqno: d.reqno, reqym: d.reqym, reqymd: d.reqymd, custcd: d.custcd, custnm: d.custnm });
        try {
          const res = await api.post('/hsio/HSIO_011U_STR', { actkind: 'S1', cmpycd: authStore.cmpycd, reqym: d.reqym, reqno: d.reqno });
          grid2?.setData(res.data.map((i: any) => ({ ...i, balqty: i.reqqty, price: i.imprice, balamt: i.reqqty * i.imprice, balvat: Math.floor(i.reqqty * i.imprice * 0.1), _status: '입력', _state: 'NEW' })));
        } catch (e) { vAlertError('상세 로드 실패'); }
      }
    });
    modalVisible.value = true;
  }
}

const handleRowAction = (row: any) => { const d = row.getData(); if (d._state === 'NEW') row.delete(); else row.update({ _status: d._status === '삭제' ? '' : '삭제' }); }
const deleteSelectedRows = () => grid2?.getSelectedRows().forEach(row => handleRowAction(row));
const addRow = () => grid2?.addRow({ balqty: 0, price: 0, balamt: 0, balvat: 0, amtsum: 0, jqty: 0, _status: '입력', _state: 'NEW' }, true);

function initialize() { resetForm(form_02); Object.assign(form_02, { cmpycd: authStore.cmpycd, balno: '0000', balym: today.replace(/-/g, '').substring(0, 6), balymd: today, reqymd: today, deptcd: authStore.deptcd, deptnm: authStore.deptnm, userid: authStore.userid, balgb: '1', reqno: '', reqym: '', email: '' }); grid1?.setData([]); grid2?.setData([]); }
async function handleFullDelete() { if (!confirm('정말 전체 삭제하시겠습니까?')) return; await api.post('/hsio/HSIO_050U_SAVE', { mst: { actkind: 'D0', cmpycd: authStore.cmpycd, balym: form_02.balym, balno: form_02.balno, updemp: authStore.userid }, dtl: [] }); vAlert('삭제되었습니다.'); initialize(); search(); }

onUnmounted(() => { if (grid1) grid1.destroy(); if (grid2) grid2.destroy(); });
onMounted(async () => {
    nextTick(initGrids);
    // 🚀 [긴급복구] 담당자 목록 조회 파라미터 원복
    api.post('/ha00/HA00_00P_STR', { gubun: 'SD', cmpycd: authStore.cmpycd, gbncd: '', code: '', remark: '' }).then(r => { userData.value = r.data; });
    api.post('/haba/HABA_100U_STR', { actkind: 'S0', cmpycd: authStore.cmpycd }).then(r => { companyConfig.value = r.data?.[0]; });
    api.post('/haba/HABA_900U_STR', { actkind: 'S0', cmpycd: authStore.cmpycd }).then(r => { ourInfo.value = r.data?.[0]; });
    api.get('/hp00/HP00_000S_STR', { params: { gubun: 'CL', cmpycd: authStore.cmpycd } }).then(r => { if(r.data?.length) closingInfo.sclsym = r.data[0].sclsym; });
    initialize();
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
