<!--
	=============================================================
	프로그램명	: 거래명세표 (HSIO620S)
	작성일자	: 2025.02.24
	설명        : 영업 출고 내역 조회 및 화면 데이터 기반 출력/메일 전송 (도움창 복구 및 표준화)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 (버튼 크기 표준화) -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-file-earmark-text-fill me-2 text-primary" style="font-size: 18px;"></i>
        영업정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        출고관리 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        <span class="text-primary fw-bolder">거래명세표 (HSIO620S)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="searchMaster">조회</button>
        <button class="btn-erp btn-primary" @click="printSpecification" :disabled="!selectedMasterInfo">거래명세서 출력</button>
        <button class="btn-erp btn-success" @click="printOutboundSheet" :disabled="!selectedMasterInfo">출고증 출력</button>
        <button class="btn-erp btn-info" @click="sendMail" :disabled="!selectedMasterInfo" style="color: #000 !important; font-weight: bold;">메일 전송</button>
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
                <th class="text-center bg-light">출고창고</th>
                <td>
                  <select v-model="searchData.whcd" class="form-select form-select-sm">
                    <option value="000">전체</option>
                    <option v-for="opt in whOptions" :key="opt.whcd" :value="opt.whcd">{{ opt.whnm }}</option>
                  </select>
                </td>
                <th class="text-center bg-light">출고일자</th>
                <td class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                  <DateForm v-model:fromdt="searchData.fromdt" v-model:todt="searchData.todt" />
                </td>
              </tr>
              <tr>
                <th class="text-center bg-light">거래처명</th>
                <td>
                  <div class="input-group input-group-sm">
                    <input v-model="searchData.custcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
                    <input v-model="searchData.custnm" type="text" class="form-control" placeholder="거래처 선택" @keyup.enter="searchMaster" />
                    <button class="btn btn-outline-secondary" @click="openHelp('CUST')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="text-center bg-light">확정여부</th>
                <td><select v-model="searchData.slipyn" class="form-select form-select-sm" style="max-width: 100px;"><option value="Y">확정</option></select></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="d-flex gap-2 flex-grow-1 overflow-hidden" style="min-height: 0;">
        <div class="card border shadow-sm d-flex flex-column overflow-hidden grid-container-left" style="width: 350px; min-width: 350px;">
          <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark">출고 목록</div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column"><div ref="masterGridElement" class="tabulator-instance flex-grow-1"></div></div>
        </div>
        <div class="flex-grow-1 d-flex flex-column gap-2 overflow-hidden">
          <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column">
            <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
              <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-1"></i> 상세 품목 내역</span>
            </div>
            <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column"><div ref="detailGridElement" class="tabulator-instance flex-grow-1"></div></div>
          </div>
        </div>
      </div>
    </div>
  </div>
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, nextTick, onUnmounted } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { getDate } from '@/composables/useDate'
import { numberToHanja } from '@/utils/hanja'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import DateForm from '@/components/DateForm.vue'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { firstDay, today } = getDate()

const searchData = reactive({
    whcd: '000',
    fromdt: firstDay,
    todt: today,
    slipyn: 'Y',
    custcd: '',
    custnm: ''
})

const whOptions = ref<any[]>([]); const selectedMasterInfo = ref<any>(null);
const companyConfig = ref<any>(null);

const masterGridElement = ref<HTMLElement | null>(null); const detailGridElement = ref<HTMLElement | null>(null)
let masterGrid: Tabulator | null = null; let detailGrid: Tabulator | null = null

    const initGrids = () => {
      masterGrid = new Tabulator(masterGridElement.value!, {
      layout: "fitColumns", height: "100%", columns: [
        { title: "No", formatter: "rownum", width: 40 },
        { title: "거래처", field: "custnm", minWidth: 150, cssClass: "fw-bold text-primary cursor-pointer" }, { title: "출고번호", field: "iono_full", width: 120, hozAlign: "center", mutatorData: (v,d)=>`${d.ioym}-${d.iono}` }]
      });
      masterGrid.on("rowClick", (e, row) => {
          selectedMasterInfo.value = row.getData();
          fetchDetails(row.getData());
      });
      detailGrid = new Tabulator(detailGridElement.value!, {
      layout: 'fitColumns', height: '100%', columns: [
          { title: "품목명", field: "itemnm", minWidth: 200, hozAlign: "left", cssClass: "fw-bold" },
          { title: "규격", field: "itsize", width: 150 },
          { title: "단위", field: "unit", width: 60, hozAlign: "center" },
          { title: "수량", field: "ioqty", width: 100, hozAlign: "right", formatter: "money" },
          { title: "금액", field: "jsanamt", width: 120, hozAlign: "right", formatter: "money" },
          { title: "부가세", field: "jsanvat", width: 110, hozAlign: "right", formatter: "money" }
      ]
      });
    }

async function searchMaster() {
    const res = await api.post('/hsio/HSIO_620S_STR', {
        actkind: 'S1',
        cmpycd: authStore.cmpycd,
        iogbn: '200',
        whcd: searchData.whcd,
        fromdt: searchData.fromdt.replace(/-/g, ''),
        todt: searchData.todt.replace(/-/g, ''),
        custcd: searchData.custcd,
        ioym: '',
        iono: '',
        slipyn: searchData.slipyn
    });

    masterGrid?.setData(res.data);
    detailGrid?.clearData();
    selectedMasterInfo.value = null;
}
async function fetchDetails(row: any) {
    const res = await api.post('/hsio/HSIO_620S_STR', {
        actkind: 'S0',
        cmpycd: authStore.cmpycd,
        iogbn: '200',
        whcd: searchData.whcd,
        fromdt: searchData.fromdt.replace(/-/g, ''),
        todt: searchData.todt.replace(/-/g, ''),
        custcd: row.custcd,
        ioym: row.ioym,
        iono: row.iono,
        slipyn: searchData.slipyn
    });
    detailGrid?.setData(res.data);
}

const generateSpecHtml = async (m: any) => {
    const [hRes, dRes] = await Promise.all([
        api.post('/hsio/HSIO_TRANS_STR', { actkind: 'S1', cmpycd: authStore.cmpycd, ioym: m.ioym, iono: m.iono }),
        api.post('/hsio/HSIO_TRANS_STR', { actkind: 'S0', cmpycd: authStore.cmpycd, ioym: m.ioym, iono: m.iono })
    ]);

    const hRaw = hRes.data?.[0] || {};
    const dtl = dRes.data || [];

    const getVal = (obj: any, key: string) => {
        if (!obj) return '';
        const kLower = key.toLowerCase();
        const kUpper = key.toUpperCase();
        const val = obj[kLower] !== undefined ? obj[kLower] : (obj[kUpper] !== undefined ? obj[kUpper] : '');
        return val === null ? '' : String(val).trim();
    };

    const h = {
        ioymd: getVal(hRaw, 'ioymd'), ioym: getVal(hRaw, 'ioym'), iono: getVal(hRaw, 'iono'),
        custnm: getVal(hRaw, 'custnm'), remark: getVal(hRaw, 'remark'), rcvamt: getVal(hRaw, 'rcvamt'),
        banknm: getVal(hRaw, 'banknm'), gujano: getVal(hRaw, 'gujano'), gaibja: getVal(hRaw, 'gaibja'),
        salsemp: getVal(hRaw, 'salsemp')
    };

    const sInfoRes = await api.post('/haba/HABA_900U_STR', { actkind: 'S0', cmpycd: authStore.cmpycd });
    const cInfoRaw = sInfoRes.data?.[0] || {};

    const stampRes = await api.post('/haba/HABA_100U_STR', { actkind: 'S0', cmpycd: authStore.cmpycd });
    const stampImg = getVal(stampRes.data?.[0], 'stampimg');

    const cInfo = {
        saupno: getVal(cInfoRaw, 'saupno'), cmpynm: getVal(cInfoRaw, 'cmpynm'), bossnm: getVal(cInfoRaw, 'bossnm'),
        address: getVal(cInfoRaw, 'address'), uptae: getVal(cInfoRaw, 'uptae'), upjong: getVal(cInfoRaw, 'upjong')
    };

    const fC = (n: any, p: number = 0) => {
        const val = Number(n || 0);
        return val.toLocaleString(undefined, { minimumFractionDigits: p, maximumFractionDigits: p });
    };

    const formatSaupno = (s: string) => {
        if (!s) return '';
        const clean = s.replace(/-/g, '');
        if (clean.length === 10) return `${clean.substring(0, 3)}-${clean.substring(3, 5)}-${clean.substring(5)}`;
        return s;
    };

    let totalAmt = 0, totalVat = 0;
    dtl.forEach(i => { totalAmt += Number(getVal(i, 'jsanamt') || 0); totalVat += Number(getVal(i, 'jsanvat') || 0); });
    const totalSum = totalAmt + totalVat;

    const stampUrl = stampImg ? `/api/Upload_Images/${authStore.cmpycd}/stampimg/${stampImg}` : '';

    const renderContent = (type: string) => {
        let rowsHtml = '';
        dtl.forEach(i => {
            const qtyPnt = Number(getVal(i, 'qtypnt') || 0);
            const ioqty = Number(getVal(i, 'ioqty')) || 0;
            const jsanamt = Number(getVal(i, 'jsanamt')) || 0;
            const jsanvat = Number(getVal(i, 'jsanvat')) || 0;
            const ioymd = getVal(i, 'ioymd');
            rowsHtml += `
                <tr height="25">
                    <td align="center" style="font-size:8.5pt;">${ioymd ? ioymd.substring(4, 6) + '/' + ioymd.substring(6, 8) : ''}</td>
                    <td align="left" style="padding-left:3px; font-size:8.5pt; white-space:nowrap; overflow:hidden;">${getVal(i, 'itemnm')}</td>
                    <td align="center" style="font-size:8.5pt;">${getVal(i, 'itsize')}</td>
                    <td align="center" style="font-size:8.5pt;">${getVal(i, 'unit')}</td>
                    <td align="right" style="padding-right:3px; font-size:8.5pt;">${fC(ioqty, qtyPnt)}</td>
                    <td align="right" style="padding-right:3px; font-size:8.5pt;">${fC(ioqty !== 0 ? jsanamt/ioqty : 0)}</td>
                    <td align="right" style="padding-right:3px; font-size:8.5pt;">${fC(jsanamt)}</td>
                    <td align="right" style="padding-right:3px; font-size:8.5pt;">${fC(jsanvat)}</td>
                </tr>`;
        });
        for (let k = dtl.length; k < 20; k++) rowsHtml += '<tr height="25"><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td></tr>';

        return `
            <div class="report-wrapper">
                <table class="report-table">
                    <colgroup>
                        <col width="7%">
                        <col width="23%">
                        <col width="20%">
                        <col width="5%">
                        <col width="8%">
                        <col width="11%">
                        <col width="13%">
                        <col width="13%">
                    </colgroup>
                    <thead>
                        <tr><td colspan="8" class="title-cell"><div class="title-main">거&nbsp;&nbsp;래&nbsp;&nbsp;명&nbsp;&nbsp;세&nbsp;&nbsp;표</div><div class="title-sub">(${type} 보관용)</div></td></tr>
                        <tr height="130">
                            <td colspan="2" class="header-left">
                                <div class="header-date" style="font-size:8.5pt;"><b>${h.ioymd ? h.ioymd.substring(0, 4) + '년 ' + h.ioymd.substring(4, 6) + '월 ' + h.ioymd.substring(6, 8) + '일' : ''}</b>&nbsp;[${h.ioym || ''}-${h.iono || ''}]</div>
                                <div class="header-cust" style="font-size:11pt;"><b>${h.custnm}</b>&nbsp;&nbsp;&nbsp;귀중</div>
                            </td>
                            <td colspan="6" style="padding:0;">
                                <table style="width:100%; border-collapse:collapse; border:none; table-layout:fixed; height:100%;">
                                    <colgroup><col width="25"><col width="75"><col width="180"><col width="45"><col></colgroup>
                                    <tr height="26"><td rowspan="5" class="bg-gray" style="border:1px solid #000; font-size:8.5pt; width:25px;">공<br>급<br>자</td><td class="bg-gray" style="border:1px solid #000; font-size:8.5pt; width:75px;">등록번호</td><td colspan="3" style="border:1px solid #000; font-weight:bold; font-size:11pt; text-align:center; letter-spacing:1px;">${formatSaupno(cInfo.saupno)}</td></tr>
                                    <tr height="35"><td class="bg-gray" style="border:1px solid #000; font-size:8.5pt;">상&nbsp;&nbsp;&nbsp;&nbsp;호</td><td style="border:1px solid #000; font-weight:bold; font-size:9.5pt; padding-left:5px; overflow:hidden; white-space:nowrap;">${cInfo.cmpynm}</td><td class="bg-gray" style="width:40px; border:1px solid #000; font-size:8.5pt;">성&nbsp;명</td><td style="border:1px solid #000; font-weight:bold; position:relative; text-align:left; padding-left:10px; font-size:9.5pt;">${cInfo.bossnm}&nbsp;&nbsp;(인)${stampUrl ? `<img src="${stampUrl}" style="position:absolute; top:-8px; right:5px; width:48px; height:48px; opacity:0.8; z-index:10;">` : ''}</td></tr>
                                    <tr height="35"><td class="bg-gray" style="border:1px solid #000; font-size:8.5pt;">사업장주소</td><td colspan="3" align="left" style="border:1px solid #000; font-size:8pt; padding-left:5px; line-height:1.2;">${cInfo.address}</td></tr>
                                    <tr height="24"><td class="bg-gray" style="border:1px solid #000; font-size:8.5pt;">업&nbsp;&nbsp;&nbsp;&nbsp;태</td><td style="border:1px solid #000; font-size:8.5pt; padding-left:3px;">${cInfo.uptae}</td><td class="bg-gray" style="border:1px solid #000; font-size:8.5pt;">종&nbsp;목</td><td style="border:1px solid #000; font-size:8.5pt; padding-left:3px;">${cInfo.upjong}</td></tr>
                                    <tr height="24"><td class="bg-gray" style="border:1px solid #000; font-size:8.5pt;">특기사항</td><td colspan="3" align="left" style="border:1px solid #000; font-size:8pt; padding-left:3px;">${h.remark}</td></tr>
                                </table>
                            </td>
                        </tr>
                        <tr><td colspan="8" class="total-sum-row"><b>&nbsp; 합계금액(VAT 포함) : &nbsp;&nbsp;&nbsp;一金</b>&nbsp;<span class="hanja-amount">${numberToHanja(totalSum)}</span><b> 圓 整 </b>(&nbsp;${fC(totalSum)})</td></tr>
                        <tr class="bg-gray col-header" height="30"><td>월 일</td><td>품 명</td><td>규 격</td><td>단위</td><td>수량</td><td>단가</td><td>공급가액</td><td>세액</td></tr>
                    </thead>
                    <tbody>${rowsHtml}</tbody>
                    <tfoot>
                        <tr class="bg-gray footer-total"><td colspan="6">합&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;계</td><td align="right" style="padding-right:3px; font-size:8.5pt;">&#8361; ${fC(totalAmt)}</td><td align="right" style="padding-right:3px; font-size:8.5pt;">&#8361; ${fC(totalVat)}</td></tr>
                        <tr><td colspan="8" style="padding:0; height:35px;"><div class="meta-flex"><div style="flex:1; font-size:8.5pt;">미수금: ${fC(h.rcvamt)}</div><div style="flex:2; text-align:center; font-size:8.5pt;">${h.banknm} ${h.gujano} ${h.gaibja}</div><div style="flex:1; text-align:right; font-size:8.5pt;">담당자: ${h.salsemp}</div></div></td></tr>
                        <tr class="sign-row"><td colspan="8" style="font-size:8.5pt;">상기 품목을 정히 납품함.&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;인수자:&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;(인)</td></tr>
                    </tfoot>
                </table>
            </div>`;
    };

    return `<html><head><title>거래명세표</title><style>
        @page { size: A4; margin: 5mm; }
        body { font-family: 'GulimChe', '굴림체', monospace; font-size: 8.5pt; color: #000; margin: 0; padding: 20px; }
        .report-wrapper { width: 650px; margin: 0 auto; position: relative; }
        .page-break { page-break-after: always; height: 1px; }
        table.report-table { width: 100%; border-collapse: collapse; border: 2px solid #000; table-layout: fixed; }
        table.report-table td { border: 1px solid #000; padding: 2px; vertical-align: middle; }
        .title-cell { height: 60px; text-align: center; border-bottom: 2px solid #000 !important; }
        .title-main { font-size: 20pt; font-weight: bold; letter-spacing: 15px; }
        .title-sub { font-size: 10pt; margin-top: 5px; }
        .header-left { border-right: none !important; text-align: center; }
        .bg-gray { background-color: #f2f2f2; text-align: center; font-weight: bold; }
        .total-sum-row { text-align: left; padding-left: 15px; height: 35px; font-size: 9.5pt; border-top: 2px solid #000 !important; }
        .hanja-amount { font-family: 'BatangChe', serif; font-size: 10.5pt; font-weight: bold; }
        .col-header td { text-align: center; font-weight: bold; height: 30px; font-size: 8.5pt; }
        .footer-total td { text-align: center; font-weight: bold; height: 32px; font-size: 8.5pt; }
        .meta-flex { display: flex; align-items: center; padding: 0 10px; height: 100%; }
        .sign-row td { text-align: right; padding-right: 60px; height: 45px; border-top: none !important; font-weight: bold; }
    </style></head>
    <body onload="window.print()">
        ${renderContent('공급받는자')}
        <div class="page-break"></div>
        <div style="margin-top: 50px;">
            ${renderContent('공급자')}
        </div>
    </body></html>`;
}

const printSpecification = async () => { const win = window.open('', '_blank', 'width=800,height=900'); if (!win) return alert('팝업 차단 해제 필요'); const html = await generateSpecHtml(selectedMasterInfo.value); win.document.write(html); win.document.close(); }
const printOutboundSheet = async () => { vAlert('출고증 출력 기능을 준비 중입니다.'); }
const sendMail = async () => { const m = selectedMasterInfo.value; const targetEmail = String(m.email || '').trim(); if (!targetEmail.includes('@')) return vAlertError('거래처 메일 주소 없음'); if (!confirm(`${targetEmail}로 전송하시겠습니까?`)) return; try { const html = await generateSpecHtml(m); await api.post('/mail/send-statement', [{ htmlcontent: html, fromnm: authStore.cmpynm, email: targetEmail, custnm: m.custnm, custcd: m.custcd, docgb: 'TRANS', no: `${m.ioym}-${m.iono}` }]); vAlert('메일 전송 완료'); } catch (e) { vAlertError('메일 전송 실패') } }

function initialize() { resetForm(searchData); searchData.whcd = '000'; searchData.slipyn = 'Y'; masterGrid?.clearData(); detailGrid?.clearData(); selectedMasterInfo.value = null; }
const modalVisible = ref(false); const modalProps = reactive<any>({ title: '', path: '', onConfirm: () => {} })

/** 🚀 [긴급복구] 거래처 도움창 컬럼 정의 복구 */
function openHelp(type: string) {
  if (type === 'CUST') {
    Object.assign(modalProps, {
      title: '거래처 선택', path: '/ha00/HA00_00P_STR',
      data: { gubun: 'C4', cmpycd: authStore.cmpycd, code: searchData.custnm },
      columns: [
        { title: '코드', field: 'custcd', width: 100, hozAlign: 'center' },
        { title: '거래처명', field: 'custnm', width: 250 }
      ],
      onConfirm: (d: any) => { searchData.custcd = d.custcd; searchData.custnm = d.custnm }
    })
    modalVisible.value = true;
  }
}

onUnmounted(() => { if (masterGrid) masterGrid.destroy(); if (detailGrid) detailGrid.destroy(); });
onMounted(async () => {
  api.get('/hs00/HS00_000S_STR', { params: { gubun: 'W0', cmpycd: authStore.cmpycd } }).then(r => whOptions.value = r.data);
  api.post('/haba/HABA_100U_STR', { actkind: 'S0', cmpycd: authStore.cmpycd }).then(r => { companyConfig.value = r.data?.[0]; });
  nextTick(() => { initGrids(); searchMaster(); });
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; border-bottom: 3px solid #005a9f !important; }
</style>
