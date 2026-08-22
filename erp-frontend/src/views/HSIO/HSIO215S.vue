<!--
	=============================================================
	프로그램명	: 입고의뢰서 출력 (Goods Receipt Request Print)
	작성일자	: 25.02.21
	설명        : [최종완성] 입고의뢰서 ASP 원본 로직 완벽 이식
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container">
		<!-- 🚀 1. 상단 액션 바 -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-printer-fill me-2 text-primary" style="font-size: 18px;"></i>
				구매정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				입고관리 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				<span class="text-primary fw-bolder">입고의뢰서 출력 (HSIO215S)</span>
			</div>
			<div class="btn-group-erp d-flex gap-1">
				<button class="btn-erp btn-init" @click="initialize">초기화</button>
				<button class="btn-erp btn-search" @click="fetchCustList">조회</button>
				<button class="btn-erp btn-primary" @click="printInboundSheet">입고의뢰서 출력</button>
			</div>
		</div>

		<!-- 🔍 2. 최상단 검색 조건 -->
		<div class="p-2 pb-0">
			<div class="card border shadow-sm">
				<div class="card-body p-0">
					<table class="erp-table-full">
						<colgroup>
							<col style="width: 100px;"><col />
							<col style="width: 100px;"><col />
							<col style="width: 100px;"><col />
							<col style="width: 100px;"><col />
						</colgroup>
						<tbody>
							<tr>
								<th>입고창고</th>
								<td>
									<select v-model="searchForm.whcd" class="form-select form-select-sm">
										<option value="000">전체</option>
										<option v-for="opt in whOptions" :key="opt.code" :value="opt.code">{{ opt.cdnm }}</option>
									</select>
								</td>
								<th class="required">입고일자</th>
								<td>
									<div class="d-flex align-items-center gap-1">
										<input v-model="searchForm.fromdt" type="date" class="form-control form-control-sm" />
										<span class="text-muted">~</span>
										<input v-model="searchForm.todt" type="date" class="form-control form-control-sm" />
									</div>
								</td>
								<th>거&nbsp;&nbsp;래&nbsp;&nbsp;처</th>
								<td>
									<div class="input-group input-group-sm">
										<input v-model="searchForm.custcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
										<input v-model="searchForm.custnm" type="text" class="form-control" placeholder="거래처 검색" />
										<button class="btn btn-outline-secondary px-2" @click="openHelp('CUST')"><i class="bi bi-search"></i></button>
									</div>
								</td>
								<th>확정여부</th>
								<td>
									<select v-model="searchForm.slipyn" class="form-select form-select-sm">
										<option value="Y">확정</option>
										<option value="N">미확정</option>
									</select>
								</td>
							</tr>
						</tbody>
					</table>
				</div>
			</div>
		</div>

		<!-- 📊 3. 메인 작업 영역 -->
		<div class="d-flex flex-row flex-grow-1 overflow-hidden p-2 gap-2">
			<!-- ⬅️ 좌측: 입고 목록 -->
			<div class="card border shadow-sm d-flex flex-column" style="width: 320px; min-width: 320px;">
				<div class="card-header bg-light py-1 px-3 border-bottom d-flex align-items-center">
					<span class="fw-bold small text-dark"><i class="bi bi-list-check me-1"></i> 입고증 대상 목록</span>
				</div>
                  <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column" style="min-height: 0;">
                    <div ref="poGridRef" class="tabulator-full-height" />
                  </div>
			</div>

			<!-- 🅱️ 우측: 상세 품목 그리드 -->
			<div class="flex-grow-1 d-flex flex-column gap-2 overflow-hidden">
				<div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white">
					<div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between" style="height: 40px;">
						<span class="fw-bold small text-dark d-flex align-items-center">
							<i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i> 입고의뢰 상세 내역
						</span>
						<span v-if="selectedInfo" class="badge bg-primary-subtle text-primary">번호: {{ selectedInfo }}</span>
					</div>
                      <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column" style="min-height: 0;">
                        <div ref="itemGridRef" class="tabulator-full-height" />
                      </div>
				</div>
			</div>
		</div>
	</div>

	<Modal v-model:visible="modalVisible" :modalProps="modalProps" />
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, onUnmounted, nextTick } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { getDate } from '@/composables/useDate'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { firstDay, today } = getDate()

const searchForm = reactive<any>({
  whcd: '000',
  fromdt: firstDay,
  todt: today,
  custcd: '', custnm: '',
  slipyn: 'N'
})

const selectedMasterRow = ref<any>(null);
const selectedInfo = ref('');
const whOptions = ref<any[]>([]);
const poGridRef = ref<HTMLDivElement | null>(null);
const itemGridRef = ref<HTMLDivElement | null>(null);
let poGrid: Tabulator | null = null;
let itemGrid: Tabulator | null = null

async function fetchCustList() {
  try {
    const res = await api.post('/hsio/HSIO_215S_STR', {
      actkind: 'S1', cmpycd: authStore.cmpycd, iogbn: '100',
      fromdt: searchForm.fromdt.replace(/-/g, ''),
      todt: searchForm.todt.replace(/-/g, ''),
      whcd: searchForm.whcd, custcd: searchForm.custcd, slipyn: searchForm.slipyn
    });
    poGrid?.setData(res.data || []);
    itemGrid?.clearData();
    selectedInfo.value = '';
    selectedMasterRow.value = null;
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

async function fetchDetail(row: any) {
  const d = row.getData();
  selectedMasterRow.value = d;
  selectedInfo.value = `${d.ioym}-${d.iono}`;
  try {
    const res = await api.post('/hsio/HSIO_215S_STR', {
      actkind: 'S0', cmpycd: authStore.cmpycd, iogbn: '100',
      whcd: searchForm.whcd, custcd: d.custcd, ioym: d.ioym, iono: d.iono,
      fromdt: searchForm.fromdt.replace(/-/g, ''),
      todt: searchForm.todt.replace(/-/g, '')
    })
    itemGrid?.setData(res.data || [])
  } catch (e) { vAlertError('상세 내역 조회 실패') }
}

function initialize() {
  resetForm(searchForm);
  searchForm.whcd = '000'; searchForm.slipyn = 'N';
  searchForm.fromdt = firstDay;
  searchForm.todt = today;
  poGrid?.clearData(); itemGrid?.clearData(); selectedInfo.value = ''; selectedMasterRow.value = null;
}

/** 🚀 [최종] 입고의뢰서 출력 (ASP 로직 이식) */
const printInboundSheet = async () => {
    if (!selectedMasterRow.value) return vAlertError('입고 내역을 먼저 선택하세요.')
    const m = selectedMasterRow.value

    try {
        const [hRes, dRes, companyRes, stampRes] = await Promise.all([
            api.post('/hsio/HSIO_REQIN_STR', { actkind: 'S1', cmpycd: authStore.cmpycd, ioym: m.ioym, iono: m.iono }),
            api.post('/hsio/HSIO_REQIN_STR', { actkind: 'S0', cmpycd: authStore.cmpycd, ioym: m.ioym, iono: m.iono }),
            api.post('/haba/HABA_900U_STR', { actkind: 'S0', cmpycd: authStore.cmpycd }),
            api.post('/haba/HABA_100U_STR', { actkind: 'S0', cmpycd: authStore.cmpycd })
        ])

        if (!hRes.data?.length) return vAlertError('입고의뢰 정보를 찾을 수 없습니다.')
        const h = hRes.data[0]
        const dtl = dRes.data || []
        const sInfo = stampRes.data?.[0] || {}

        // 결재라인 추출
        const gLines = [];
        ['gline1', 'gline2', 'gline3', 'gline4', 'gline5'].forEach(key => {
            const val = String(sInfo[key] || '').trim();
            if (val) gLines.push(val);
        });
        if (gLines.length === 0) gLines.push('담 당', '팀 장', '부 장', '사 장');

        const fC = (n: any) => Number(n || 0).toLocaleString()
        const fSaup = (v: any) => {
            const s = String(v || '').replace(/[^0-9]/g, '');
            return s.length === 10 ? `${s.substring(0,3)}-${s.substring(3,5)}-${s.substring(5)}` : s;
        }

        const fDate = (v: any) => {
            const s = String(v || '').replace(/[^0-9]/g, '');
            return s.length === 8 ? `${s.substring(0,4)}-${s.substring(4,6)}-${s.substring(6,8)}` : v;
        }

        let rowsHtml = ''
        let qtysum = 0, amtsum = 0

        for (let i = 0; i < Math.max(dtl.length, 15); i++) {
            const item = dtl[i] || {}
            if (item.itemnm) {
                const qty = Number(item.ioqty || 0); const amt = Number(item.ioamt || 0);
                qtysum += qty; amtsum += amt;
                rowsHtml += `
                <tr height="28">
                    <td class="text-center" style="font-size:8.5pt;">${i + 1}</td>
                    <td class="text-center" style="font-size:8.5pt;">${item.itemcd || ''}</td>
                    <td class="text-left" style="padding-left:3px; font-size:8.5pt;">${String(item.itemnm || '').trim()}</td>
                    <td class="text-left" style="padding-left:3px; font-size:8.5pt;">${String(item.itsize || '').trim()}</td>
                    <td class="text-center" style="font-size:8.5pt;">${item.unit || ''}</td>
                    <td class="text-right" style="padding-right:3px; font-size:8.5pt;">${fC(qty)}</td>
                    <td class="text-right" style="padding-right:3px; font-size:8.5pt;">${fC(qty > 0 ? amt/qty : 0)}</td>
                    <td class="text-right" style="padding-right:3px; font-size:8.5pt;">${fC(amt)}</td>
                </tr>`
            } else {
                rowsHtml += `<tr height="28"><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>`
            }
        }

        const html = `
        <html>
        <head>
            <title>입고의뢰서</title>
            <style>
                body { font-family: 'GulimChe', '굴림체', sans-serif; color: black; margin: 0; padding: 10px; }
                table { border-collapse: collapse; font-size: 8.5pt; width: 650px; margin: 0 auto; table-layout: fixed; }
                th, td { border: 1px solid #BDBDBD; padding: 2px; text-align: center; }
                .title-font { font-size: 20pt; font-weight: bold; }
                .bg-eee { background-color: #eee; }
                .text-left { text-align: left !important; }
                .text-right { text-align: right !important; }
                .text-center { text-align: center !important; }
            </style>
        </head>
        <body onload="window.print()">
            <table border="0" style="border:0; height:72px; margin-bottom:10px;">
                <tr>
                    <td width="350px" align="center" style="font-size:20pt; font-weight:bold; vertical-align:middle; border:0;">입&nbsp;&nbsp;고&nbsp;&nbsp;의&nbsp;&nbsp;뢰&nbsp;&nbsp;서</td>
                    <td width="300px" align="right" valign="top" style="border:0;">
                        <table border="1" style="width:100%; border-collapse:collapse; height:72px;">
                            <tr>
                                <td rowspan="2" width="20px" class="bg-eee" style="font-size:10pt; line-height:1.2;">결<br>재</td>
                                ${gLines.map(g => `<td class="bg-eee" height="20" style="font-size:8.5pt;">${g}</td>`).join('')}
                            </tr>
                            <tr>${gLines.map(() => '<td height="52" width="55"></td>').join('')}</tr>
                        </table>
                    </td>
                </tr>
            </table>

            <table border="1" style="width:650px; margin:0 auto; border-collapse:collapse;">
                <colgroup><col style="width:12%"/><col style="width:38%"/><col style="width:12%"/><col style="width:38%"/></colgroup>
                <tr height="25">
                    <td class="bg-eee">입고번호</td><td class="text-left">&nbsp;${h.ioym || m.ioym}-${h.iono || m.iono}</td>
                    <td class="bg-eee">회 사 명</td><td class="text-center"><b>${h.ccustnm || ''}</b></td>
                </tr>
                <tr height="25">
                    <td class="bg-eee">입고일자</td><td class="text-left">&nbsp;${fDate(h.ioymd)}</td>
                    <td class="bg-eee">등록번호</td><td class="text-center">&nbsp;${fSaup(h.ccustno)}</td>
                </tr>
                <tr height="25">
                    <td class="bg-eee">입고창고</td><td class="text-left">&nbsp;${h.whnm || ''}</td>
                    <td class="bg-eee">소 재 지</td><td class="text-center" style="font-size:8pt;">&nbsp;${h.caddress || ''}</td>
                </tr>
                <tr height="25">
                    <td class="bg-eee">입고부서</td><td class="text-left">&nbsp;${h.deptnm || ''}</td>
                    <td class="bg-eee" style="padding:0;">
                        <div style="display:flex; height:100%;">
                            <div style="flex:1; border-right:1px solid #BDBDBD; display:flex; align-items:center; justify-content:center;">전&nbsp;&nbsp;&nbsp;&nbsp;화</div>
                            <div style="flex:1; display:flex; align-items:center; justify-content:center;">팩&nbsp;&nbsp;&nbsp;&nbsp;스</div>
                        </div>
                    </td>
                    <td style="padding:0;">
                        <div style="display:flex; height:100%;">
                            <div style="flex:1; border-right:1px solid #BDBDBD; display:flex; align-items:center; justify-content:center;">${h.ctelno || ''}</div>
                            <div style="flex:1; display:flex; align-items:center; justify-content:center;">${h.cfaxno || ''}</div>
                        </div>
                    </td>
                </tr>
                <tr height="25">
                    <td class="bg-eee">담당자명</td><td align="left">&nbsp;${h.usernm || ''} (인)</td>
                    <td class="bg-eee">담당자명</td><td align="center">&nbsp;${h.cdamdang || ''}</td>
                </tr>
                <tr height="25">
                    <td class="bg-eee">입고구분</td><td align="left">&nbsp;${h.iotypenm || ''}</td>
                    <td class="bg-eee">담당연락처</td><td align="center">${h.ctelno || ''}</td>
                </tr>
                <tr height="25"><td class="bg-eee">특기사항</td><td colspan="3" align="left">&nbsp;${h.remark || ''}</td></tr>
            </table>

            <table border="1" style="width:650px; margin:5px auto 0 auto; border-collapse:collapse;">
                <colgroup>
                    <col style="width:5%"/><col style="width:12%"/><col style="width:33%"/><col style="width:10%"/><col style="width:6%"/><col style="width:11%"/><col style="width:11%"/><col style="width:12%"/>
                </colgroup>
                <thead>
                    <tr class="bg-eee" height="28">
                        <td>No.</td><td>품 목</td><td>품 목 명</td><td>규 격</td><td>단위</td><td>수 량</td><td>단 가</td><td>금 액</td>
                    </tr>
                </thead>
                <tbody>
                    ${rowsHtml}
                </tbody>
                <tfoot>
                    <tr height="28" style="font-weight:bold" class="bg-eee">
                        <td class="text-center" colspan="5">합 계</td>
                        <td class="text-right" style="padding-right:3px;">${fC(qtysum)}</td>
                        <td>&nbsp;</td>
                        <td class="text-right" style="padding-right:3px;">${fC(amtsum)}</td>
                    </tr>
                </tfoot>
            </table>
        </body>
        </html>`;

        const win = window.open('', '_blank', 'width=800,height=900');
        win?.document.write(html);
        win?.document.close();
    } catch (e) { vAlertError('입고의뢰서 출력 실패') }
}

const openHelp = (type: string) => {
    if (type === 'CUST') {
        Object.assign(modalProps, {
            title: '거래처 선택',
            path: '/ha00/HA00_00P_STR',
            data: { gubun: 'C4', cmpycd: authStore.cmpycd, code: searchForm.custnm, codenm: '' , remark: '' },
            columns: [
                { title: '코드', field: 'custcd', width: 100, hozAlign: 'center' },
                { title: '거래처명', field: 'custnm', width: 200 }
            ],
            onConfirm: (d: any) => {
                searchForm.custcd = d.custcd;
                searchForm.custnm = d.custnm;
            }
        })
        modalVisible.value = true
    }
}

onUnmounted(() => {
  if (poGrid) poGrid.destroy();
  if (itemGrid) itemGrid.destroy();
});

onMounted(async () => {
  api.get('/hs00/HS00_000S_STR', { params: { gubun: 'W0', cmpycd: authStore.cmpycd } })
     .then(r => whOptions.value = r.data.map((i:any)=>({code: i.code || i.whcd, cdnm: i.cdnm || i.whnm})));

  if (poGridRef.value) {
    poGrid = new Tabulator(poGridRef.value, {
      layout: 'fitColumns', height: '100%', selectable: 1,
      columnDefaults: {
        headerSort: false,
        headerHozAlign: "center",
        hozAlign: 'right',
        vertAlign: 'middle',
        minWidth: 80
      },
      columns: [
        { title: '거래처', field: 'custnm', minWidth: 150, hozAlign: 'left', widthGrow: 1, cssClass: 'fw-bold' },
        { title: '입고번호', field: 'iono_disp', width: 120, hozAlign: 'center', mutatorData: (v, d) => `${d.ioym}-${d.iono}` }
      ]
    })
    poGrid.on('rowClick', (e, row) => fetchDetail(row))
  }

  if (itemGridRef.value) {
    itemGrid = new Tabulator(itemGridRef.value, {
      layout: 'fitColumns', height: '100%',
      columnDefaults: {
        headerSort: false,
        headerHozAlign: 'center',
        hozAlign: 'right',
        vertAlign: 'middle',
        minWidth: 100
      },
      columns: [
        { title: '품목명', field: 'itemnm', minWidth: 200, hozAlign: 'left', widthGrow: 1, cssClass: 'fw-bold' },
        { title: '규격', field: 'itsize', width: 150 },
        { title: '단위', field: 'unit', width: 60, hozAlign: 'center' },
        { title: '수량', field: 'ioqty', hozAlign: 'right', width: 90, formatter: 'money', formatterParams: { precision: 0 } },
        { title: '공급가', field: 'jsanamt', hozAlign: 'right', width: 120, formatter: 'money', formatterParams: { precision: 0 } },
        { title: '부가세', field: 'jsanvat', hozAlign: 'right', width: 110, formatter: 'money', formatterParams: { precision: 0 } },
        { title: '합계', field: 'sum_amt', hozAlign: 'right', width: 130, formatter: 'money', cssClass: 'fw-bold bg-light',
          mutatorData: (v, d) => Number(d.jsanamt || 0) + Number(d.jsanvat || 0) }
      ]
    })
  }
})

const modalVisible = ref(false); const modalProps = reactive<any>({ title: '', path: '', onConfirm: () => {} })
</script>

<style scoped>
.tabulator-full-height { width: 100% !important; background-color: #fff; border-bottom: 3px solid #005a9f !important; }
</style>
