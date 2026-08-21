<!--
	=============================================================
	프로그램명	: 거래명세표 (HSIO620S)
	작성일자	: 2025.02.24
	설명        : 영업 출고 내역 조회 및 화면 데이터 기반 메일 전송
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-file-earmark-text-fill me-2 text-primary" style="font-size: 18px;"></i>
        영업정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        출고관리 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        <span class="text-primary fw-bolder">거래명세표 (HSIO620S)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="searchMaster">조회</button>
        <button class="btn-erp btn-primary" @click="printSpecification" :disabled="!selectedMasterInfo">거래명세서 출력</button>
        <button class="btn-erp btn-success" @click="printOutboundSheet" :disabled="!selectedMasterInfo">출고증 출력</button>
        <!-- 🚀 [개선] 버튼 가시성 확보 -->
        <button class="btn btn-sm btn-info text-white fw-bold px-3" @click="sendMail" :disabled="!selectedMasterInfo" style="min-width: 80px; height: 32px;">
            <i class="bi bi-envelope-at me-1"></i> 메일 전송
        </button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2">
      <div class="card border shadow-sm overflow-hidden">
        <div class="card-body p-0">
          <table class="erp-table-full">
            <tbody>
              <tr>
                <th style="width: 100px;">출고창고</th>
                <td style="width: 200px;">
                  <select v-model="searchData.whcd" class="form-select form-select-sm">
                    <option value="000">전체</option>
                    <option v-for="opt in whOptions" :key="opt.whcd" :value="opt.whcd">{{ opt.whnm }}</option>
                  </select>
                </td>
                <th style="width: 100px;">출고일자</th>
                <td style="width: 300px;">
                  <div class="d-flex align-items-center gap-1">
                    <input v-model="searchData.fromdt" type="date" class="form-control form-control-sm" />
                    <span>~</span>
                    <input v-model="searchData.todt" type="date" class="form-control form-control-sm" />
                  </div>
                </td>
                <th style="width: 100px;">확정여부</th>
                <td style="width: 150px;">
                  <select v-model="searchData.slipyn" class="form-select form-select-sm"><option value="Y">확정</option></select>
                </td>
                <th style="width: 100px;">거&nbsp;&nbsp;래&nbsp;&nbsp;처</th>
                <td>
                  <div class="input-group input-group-sm">
                    <input v-model="searchData.custcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
                    <input v-model="searchData.custnm" type="text" class="form-control" placeholder="거래처 선택" @keyup.enter="openHelp('CUST')" />
                    <button class="btn btn-outline-secondary" @click="openHelp('CUST')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="d-flex flex-grow-1 gap-2 overflow-hidden">
        <div class="card border shadow-sm d-flex flex-column" style="width: 350px;">
          <div class="card-header bg-light py-1 px-3 border-bottom fw-bold small text-dark">출고 목록</div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
              <div ref="masterGridElement" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>
        <div class="flex-grow-1 d-flex flex-column gap-2 overflow-hidden">
          <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column">
            <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
              <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-1"></i> 상세 품목 내역</span>
            </div>
            <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
              <div ref="detailGridElement" class="tabulator-instance flex-grow-1"></div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <Modal v-model:visible="modalVisible" :modalProps="modalProps" />
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, nextTick } from 'vue'
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

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { firstDay, today } = getDate()

const searchData = reactive({ whcd: '000', fromdt: firstDay, todt: today, slipyn: 'Y', custcd: '', custnm: '' })
const whOptions = ref<any[]>([]); const selectedMasterInfo = ref<any>(null)
const masterGridElement = ref<HTMLElement | null>(null); const detailGridElement = ref<HTMLElement | null>(null)
let masterGrid: Tabulator | null = null; let detailGrid: Tabulator | null = null

const initGrids = () => {
  masterGrid = new Tabulator(masterGridElement.value!, {
    layout: "fitColumns", height: "100%", placeholder: "데이터 없음",
    columns: [
      { title: "No", formatter: "rownum", width: 40, hozAlign: "center" },
      { title: "거래처", field: "custnm", minWidth: 150, cssClass: "fw-bold text-primary cursor-pointer" },
      { title: "출고번호", field: "iono_full", width: 120, hozAlign: "center", mutatorData: (v,d)=>`${d.ioym}-${d.iono}` }
    ]
  });
  masterGrid.on("rowClick", (e, row) => { selectedMasterInfo.value = row.getData(); fetchDetails(row.getData()); });

  detailGrid = new Tabulator(detailGridElement.value!, {
    layout: 'fitColumns', height: '100%',
    columns: [
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
  try {
    const res = await api.post('/hsio/HSIO_620S_STR', { actkind: 'S1', cmpycd: authStore.cmpycd, iogbn: '200', whcd: searchData.whcd, fromdt: searchData.fromdt.replace(/-/g,''), todt: searchData.todt.replace(/-/g,''), custcd: searchData.custcd, slipyn: searchData.slipyn });
    masterGrid?.setData(res.data); detailGrid?.clearData(); selectedMasterInfo.value = null;
  } catch (e) { vAlertError('조회 실패') }
}

async function fetchDetails(row: any) {
  try {
    const res = await api.post('/hsio/HSIO_620S_STR', { actkind: 'S0', cmpycd: authStore.cmpycd, iogbn: '200', custcd: row.custcd, ioym: row.ioym, iono: row.iono });
    detailGrid?.setData(res.data);
  } catch (e) { vAlertError('상세 로드 실패') }
}

/** 🚀 [공통] 거래명세서 HTML 생성 로직 (v2.0) */
const generateSpecHtml = async (m: any) => {
    const [hRes, dRes, ourRes, stampRes] = await Promise.all([
        api.post('/hsio/HSIO_TRANS_STR', { actkind: 'S1', cmpycd: authStore.cmpycd, ioym: m.ioym, iono: m.iono }),
        api.post('/hsio/HSIO_TRANS_STR', { actkind: 'S0', cmpycd: authStore.cmpycd, ioym: m.ioym, iono: m.iono }),
        api.post('/comm/HABA_900U_STR', { actkind: 'S0', cmpycd: authStore.cmpycd }),
        api.post('/comm/HABA_100U_STR', { actkind: 'S0', cmpycd: authStore.cmpycd })
    ]);
    const h = hRes.data[0]; const dtl = dRes.data || []; const our = ourRes.data[0]; const sInfo = stampRes.data[0];
    const fC = (n: any) => Number(n || 0).toLocaleString();
    let totalAmt = 0, totalVat = 0, rowsHtml = '';
    dtl.forEach(i => {
        const amt = Number(i.jsanamt || 0); const vat = Number(i.jsanvat || 0); totalAmt += amt; totalVat += vat;
        rowsHtml += `<tr height="25"><td>${i.ioymd.substring(4,8)}</td><td align="left">${i.itemnm}</td><td>${i.itsize || ''}</td><td>${i.unit || ''}</td><td align="right">${fC(i.ioqty)}</td><td align="right">${fC(i.jsanamt)}</td><td align="right">${fC(i.jsanvat)}</td></tr>`;
    });
    for(let k=dtl.length; k<15; k++) rowsHtml += '<tr height="25"><td></td><td></td><td></td><td></td><td></td><td></td><td></td></tr>';
    const stampUrl = sInfo.stampimg ? `/api/Upload_Images/${authStore.cmpycd}/LOGOIMG/${sInfo.stampimg}` : '';

    return `<html><head><style>body { font-family: 'GulimChe', sans-serif; font-size: 9pt; } table { width: 100%; border-collapse: collapse; } th, td { border: 1px solid #333; padding: 4px; text-align: center; } .stamp { position: absolute; top: 80px; left: 580px; width: 50px; }</style></head>
    <body onload="window.print()">
        ${stampUrl ? `<img src="${stampUrl}" class="stamp">` : ''}
        <h2 align="center">거 래 명 세 표</h2>
        <table border="1"><tr><td width="50%"><b>${h.ioymd.substring(0,4)}년 ${h.ioymd.substring(4,6)}월 ${h.ioymd.substring(6,8)}일</b><br><br><b>${h.custnm}</b> 귀중</td><td>공급자<br>번호: ${our.saupno}<br>상호: ${our.cmpynm}<br>성명: ${our.bossnm}</td></tr></table>
        <table border="1" style="margin-top:10px"><tr bgcolor="#eee"><th>월일</th><th>품목</th><th>규격</th><th>단위</th><th>수량</th><th>공급가</th><th>부가세</th></tr><tbody>${rowsHtml}</tbody>
        <tfoot><tr bgcolor="#eee"><td>합계</td><td colspan="4"> 一金 ${numberToHanja(totalAmt+totalVat)}圓 整</td><td align="right">${fC(totalAmt)}</td><td align="right">${fC(totalVat)}</td></tr></tfoot></table>
    </body></html>`;
}

const printSpecification = async () => {
    const html = await generateSpecHtml(selectedMasterInfo.value);
    const win = window.open('', '_blank', 'width=800,height=900');
    win?.document.write(html); win?.document.close();
}

const printOutboundSheet = async () => { vAlert('출고증 출력 기능을 준비 중입니다.'); }

const sendMail = async () => {
    const m = selectedMasterInfo.value;
    const targetEmail = String(m.email || '').trim();
    if (!targetEmail.includes('@')) return vAlertError('거래처 메일 주소가 없습니다.');
    if (!confirm(`${targetEmail}로 거래명세서를 전송하시겠습니까?`)) return;
    try {
        const html = await generateSpecHtml(m);
        await api.post('/mail/send-statement', [{ htmlcontent: html, email: targetEmail, custnm: m.custnm, custcd: m.custcd, docgb: 'TRANS', no: `${m.ioym}-${m.iono}` }]);
        vAlert('메일 전송 완료');
    } catch (e) { vAlertError('메일 전송 실패') }
}

function initialize() {
  resetForm(searchData); searchData.whcd = '000'; searchData.slipyn = 'Y';
  masterGrid?.clearData(); detailGrid?.clearData(); selectedMasterInfo.value = null;
}

const modalVisible = ref(false); const modalProps = reactive<any>({ title: '', path: '', onConfirm: () => {} })
function openHelp(type: string) {
  if (type === 'CUST') {
    Object.assign(modalProps, { title: '거래처 선택', path: '/ha00/HA00_00P_STR', data: { gubun: 'C4', cmpycd: authStore.cmpycd }, onConfirm: (d: any) => { searchData.custcd = d.custcd; searchData.custnm = d.custnm } })
    modalVisible.value = true;
  }
}

onMounted(async () => {
  api.get('/hs00/HS00_000S_STR', { params: { gubun: 'W0', cmpycd: authStore.cmpycd } }).then(r => whOptions.value = r.data);
  nextTick(() => { initGrids(); searchMaster(); });
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; border-bottom: 3px solid #005a9f !important; }
</style>
