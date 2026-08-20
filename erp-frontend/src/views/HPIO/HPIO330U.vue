<!--
	=============================================================
	프로그램명	: 외주가공생산실적 (HPIO350U)
	작성일자	: 2025.03.12 (UI 개편)
	설명        : 외주 생산 제품 실적 및 투입 자재 상세 관리 (좌/우 분리형)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-journal-check me-2 text-primary" style="font-size: 18px;"></i>
        생산정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">외주가공생산실적 (HPIO350U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchOrderList">조회</button>
        <button class="btn-erp btn-save" @click="saveAll">저장</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 조회 조건 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
                <col style="width: 10%" /><col style="width: 20%" />
                <col style="width: 10%" /><col style="width: 25%" />
                <col style="width: 10%" /><col style="width: 25%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light required">생산라인</th>
                <td>
                  <div class="px-1">
                    <select v-model="searchForm.linecd" class="form-select form-select-sm" @change="onLineChange">
                      <option v-for="opt in lineOptions" :key="opt.linecd" :value="opt.linecd">[{{ opt.linecd }}] {{ opt.linenm }}</option>
                    </select>
                  </div>
                </td>
                <th class="text-center bg-light required">지시일자</th>
                <td>
                  <div class="d-flex align-items-center gap-1 px-1">
                    <input v-model="fromdt_f" type="date" class="form-control form-control-sm" />
                    <span class="small">~</span>
                    <input v-model="todt_f" type="date" class="form-control form-control-sm" />
                  </div>
                </td>
                <th class="text-center bg-light required">거 래 처</th>
                <td>
                  <div class="input-group input-group-sm px-1">
                    <input v-model="searchForm.custcd" type="text" class="form-control text-center bg-light" style="max-width: 65px;" readonly />
                    <input v-model="searchForm.custnm" type="text" class="form-control fw-bold text-primary" readonly />
                    <button class="btn btn-outline-secondary px-2" @click="handleOpenHelp('CUST')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 2단 레이아웃 (좌측 목록 / 우측 상세) -->
      <div class="d-flex flex-row flex-grow-1 overflow-hidden gap-2" style="min-height: 0;">

        <!-- ⬅️ 좌측: 등록 대상 목록 -->
        <div class="card border shadow-sm d-flex flex-column overflow-hidden grid-container-left" style="width: 380px; min-width: 380px;">
          <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
            <span class="fw-bold small text-dark"><i class="bi bi-list-check me-2 text-primary"></i>실적 등록 대상</span>
            <span class="badge bg-primary-subtle text-primary border border-primary-subtle" style="font-size: 10px;">Orders</span>
          </div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="orderTableRef" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>

        <!-- ➡️ 우측: 상세 입력 및 그리드들 -->
        <div class="flex-grow-1 d-flex flex-column gap-2 overflow-hidden">

          <!-- 실적 마스터 정보 (입력 영역) -->
          <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
            <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
              <span class="fw-bold small text-dark"><i class="bi bi-pencil-square me-2 text-success"></i>실적 공통 입력 정보</span>
              <span class="text-muted" style="font-size: 11px;">※ 창고 정보는 생산라인 및 공정 설정에 따라 자동 반영됩니다.</span>
            </div>
            <div class="card-body p-0 bg-white">
              <table class="erp-table-dense" width="100%">
                <colgroup>
                    <col style="width: 15%" /><col style="width: 35%" />
                    <col style="width: 15%" /><col style="width: 35%" />
                </colgroup>
                <tbody>
                  <tr>
                    <th class="text-center bg-light required">입고일자</th>
                    <td>
                      <div class="px-1">
                        <input v-model="proymd_f" type="date" class="form-control form-control-sm" style="max-width: 200px;" />
                      </div>
                    </td>
                    <th class="text-center bg-light required">입고공정</th>
                    <td>
                      <div class="px-1">
                        <select v-model="masterInfo.progcd" class="form-select form-select-sm" style="max-width: 250px;">
                          <option v-for="opt in progOptions" :key="opt.progcd" :value="opt.progcd">{{ opt.prognm }}</option>
                        </select>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- 상단: 외주 생산 제품 (grid1) -->
          <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column">
            <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
              <span class="fw-bold small text-dark"><i class="bi bi-box-seam me-2 text-primary"></i>외주 생산 제품 실적</span>
            </div>
            <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
              <div ref="tableRef1" class="tabulator-instance flex-grow-1"></div>
            </div>
          </div>

          <!-- 하단: 투입 자재 상세 (grid2) -->
          <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column">
            <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
              <span class="fw-bold small text-dark">
                <i class="bi bi-tools me-2 text-success"></i>투입 자재 상세
                <span v-if="selectedProduct.itemnm" class="badge bg-success-subtle text-success border border-success-subtle ms-2">{{ selectedProduct.itemnm }}</span>
              </span>
              <div class="d-flex gap-1">
                <button class="btn btn-sm btn-outline-primary py-0 px-2 fw-bold" @click="addRow('MAT')" :disabled="!selectedProduct.itemcd" style="font-size: 11px;">+ 자재추가</button>
                <button class="btn btn-sm btn-outline-danger py-0 px-2 fw-bold" @click="deleteRows('MAT')" :disabled="!selectedProduct.itemcd" style="font-size: 11px;">- 삭제</button>
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
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, computed, nextTick } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { useCommonHelp } from '@/composables/useCommonHelp'
import { getDate } from '@/composables/useDate'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'

const authStore = useAuthStore()
const { today, firstDay } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps, openHelp } = useCommonHelp()

// [1] 데이터 모델링
const initymd = today.replace(/-/g, '')
const searchForm = reactive({ linecd: '888', fromdt: firstDay.replace(/-/g, ''), todt: initymd, custcd: '', custnm: '' })
const masterInfo = reactive({ proymd: initymd, whcd: '300', iwhcd: '900', progcd: '' })

const fromdt_f = computed({ get: () => formatDateDash(searchForm.fromdt), set: (v) => { if (v) searchForm.fromdt = v.replace(/-/g, '') } })
const todt_f = computed({ get: () => formatDateDash(searchForm.todt), set: (v) => { if (v) searchForm.todt = v.replace(/-/g, '') } })
const proymd_f = computed({ get: () => formatDateDash(masterInfo.proymd), set: (v) => { if (v) masterInfo.proymd = v.replace(/-/g, '') } })

const lineOptions = ref<any[]>([]);
const progOptions = ref<any[]>([]);
const whOptions = ref<any[]>([])
const selectedProduct = reactive<any>({ itemcd: '', itemnm: '' })
const closingInfo = reactive({ clsymd: '' })

const orderTableRef = ref<HTMLDivElement | null>(null)
const tableRef1 = ref<HTMLDivElement | null>(null)
const tableRef2 = ref<HTMLDivElement | null>(null)

let grid0: Tabulator | null = null
let grid1: Tabulator | null = null
let grid2: Tabulator | null = null

// [2] 그리드 초기화
const initGrids = () => {
  // [Grid 0] 좌측 목록
  grid0 = new Tabulator(orderTableRef.value!, {
    layout: "fitColumns", height: "100%", placeholder: "데이터가 없습니다.", selectable: 1,
    columns: [
      { title: "거래처명", field: "custnm", minWidth: 150, cssClass: "fw-bold" },
      { title: "지시일자", field: "ordymd", width: 100, hozAlign: "center", formatter: (c) => formatDateDash(c.getValue()) }
    ]
  });
  grid0.on("rowClick", (e, row) => fetchPerformanceMaster(row.getData()));

  // [Grid 1] 우측 상단 제품 실적
  grid1 = new Tabulator(tableRef1.value!, {
    layout: "fitColumns", height: "100%", placeholder: "대상 목록을 선택하세요.", selectable: 1,
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "No", formatter: "rownum", width: 40, hozAlign: "center", frozen: true },
      { title: "상태", field: "_status", width: 60, hozAlign: "center", formatter: (c) => {
          const v = c.getValue();
          if (v === '수정' || v === '입력') return '<span class="badge bg-warning text-dark">수정</span>';
          if (v === '삭제') return '<span class="badge bg-danger">삭제</span>';
          return '';
      }, frozen: true },
      { title: "제품명", field: "itemnm", minWidth: 200, widthGrow: 1, cssClass: 'fw-bold text-primary' },
      { title: "규격", field: "itsize", width: 150 },
      { title: "단위", field: "unit", width: 60, hozAlign: "center" },
      { title: "지시량", field: "ordqty", width: 90, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "양품량", field: "godqty", width: 100, hozAlign: "right", formatter: "money", cssClass: "bg-light fw-bold" },
      { title: "불량량", field: "errqty", width: 100, hozAlign: "right", formatter: "money", editor: "number", cssClass: "bg-light-yellow fw-bold", cellEdited: (cell) => calcTotal(cell.getRow()) },
      { title: "생산량", field: "prdqty", width: 100, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, editor: "number", cssClass: "bg-light-yellow fw-bold", cellEdited: (cell) => calcTotal(cell.getRow()) },
      { title: "가공단가", field: "price", width: 110, hozAlign: "right", formatter: "money", cssClass: "bg-light fw-bold", editor: "number", cellEdited: (cell) => calcTotal(cell.getRow()) },
      { title: "합계금액", field: "outtot", width: 120, hozAlign: "right", formatter: "money", cssClass: "fw-bold text-primary" }
    ],
  });
  grid1.on("rowClick", (e, row) => fetchPerformanceDetails(row.getData()));

  // [Grid 2] 우측 하단 투입 자재 실적
  grid2 = new Tabulator(tableRef2.value!, {
    layout: "fitColumns", height: "100%", placeholder: "제품을 선택하세요.", selectable: true,
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "No", formatter: "rownum", width: 40, hozAlign: "center" },
      { title: "상태", field: "_status", width: 60, hozAlign: "center", formatter: (c) => {
          const v = c.getValue();
          if (v === '수정' || v === '입력') return '<span class="badge bg-warning text-dark">수정</span>';
          if (v === '삭제') return '<span class="badge bg-danger">삭제</span>';
          return '';
      }},
      { title: "투입자재", field: "mitemnm", minWidth: 200, widthGrow: 1, cssClass: 'fw-bold text-success', cellClick: (e, cell) => handleOpenHelp('MAT', cell.getRow()) },
      { title: "규격", field: "mitsize", width: 150 },
      { title: "단위", field: "munit", width: 70, hozAlign: "center" },
      { title: "소요량", field: "soqty", width: 90, hozAlign: "right", formatter: "money" },
      { title: "투입량", field: "inqty", width: 100, hozAlign: "right", editor: "number", cssClass: "bg-light-green fw-bold",
        cellEdited: (cell) => { const d = cell.getData(); if (d._state === 'EXIST') cell.getRow().update({ _status: '수정' }); }
      },
      { title: "출고공정", field: "bprognm", width: 150, cellClick: (e, cell) => handleOpenHelp('befprog', cell.getRow()) },
      { title: "삭제", width: 40, hozAlign: "center", formatter: () => "<i class='bi bi-trash text-danger'></i>", cellClick: (e, cell) => handleRowAction(cell.getRow()) }
    ]
  });
}

const calcTotal = (row: any) => {
  const d = row.getData();
  let prd = Number(d.prdqty || 0);
  const ord = Number(d.ordqty || 0);
  const err = Number(d.errqty || 0);

  // [체크] 지시량 초과 입력 제한
  if (prd > ord) {
    alert(`생산량이 지시량(${ord})을 초과할 수 없습니다.`);
    prd = ord;
  }

  const god = Math.max(0, prd - err);
  const price = Number(d.price || 0);

  const amt = Math.floor(prd * price);
  row.update({
    prdqty: prd,
    godqty: god,
    outamt: amt,
    outvat: Math.floor(amt * 0.1),
    outtot: Math.floor(amt * 1.1)
  });
}

// [3] 비즈니스 로직
const fetchInitCodes = async () => {
  try {
    const [line, wh] = await Promise.all([
      api.post('/hp00/HP00_000S_STR', { gubun: 'L0', cmpycd: authStore.cmpycd, gbncd: 'Y' }),
      api.post('/hs00/HS00_000S_STR', { gubun: 'W0', cmpycd: authStore.cmpycd })
    ]);
    lineOptions.value = (line.data || []).map((i: any) => ({ linecd: i.linecd, linenm: i.linenm }));
    whOptions.value = (wh.data || []).map((i: any) => ({ whcd: i.whcd, whnm: i.whnm }));

    if (lineOptions.value.length > 0) {
      if (!searchForm.linecd) searchForm.linecd = lineOptions.value[0].linecd;
      onLineChange();
    }
  } catch (e) {}
}

const onLineChange = async () => {
  if (!searchForm.linecd) return;
  try {
    const res = await api.post('/hp00/HP00_000S_STR', { gubun: 'G0', cmpycd: authStore.cmpycd, gbncd: searchForm.linecd });
    progOptions.value = (res.data || []).map((i: any) => ({ progcd: i.progcd, prognm: i.prognm }));
    if (progOptions.value.length > 0) masterInfo.progcd = progOptions.value[0].progcd;
  } catch (e) {}
}

// 🅰️ 좌측 목록 조회 (지시 목록)
const fetchOrderList = async () => {
  try {
    // 💡 목록 조회를 위해 actkind: 'L0' 사용
    // 💡 XML 명세에 따라 ordymd(시작), proymd(종료) 위치에 기간을 실어 보냄
    const res = await api.post('/hpio/HPIO_350U_STR', {
        actkind: 'L0',
        cmpycd: authStore.cmpycd,
        prodid: 0,
        linecd: searchForm.linecd,
        progcd: '888',
        proymd: searchForm.todt,   // 종료일
        ordymd: searchForm.fromdt, // 시작일
        equpcd: '', prodcd: '200', wkgbn: '', whcd: '', itemcd: '', custcd: searchForm.custcd,
        itsize: '', unit: '', prdqty: 0, godqty: 0, errqty: 0, lotymd: '', lotno: '',
        workmm: 0, bigo: '', outym: '', outno: '', useyn: 'Y', updemp: authStore.userid
    });
    grid0?.setData(res.data || []);
    grid1?.clearData(); grid2?.clearData();
    vAlert('목록이 조회되었습니다.');
  } catch (e) { vAlertError('목록 조회 실패'); }
}

// 🅱️ 우측 상세 조회
const fetchPerformanceMaster = async (row: any) => {
  searchForm.custcd = row.custcd;
  searchForm.custnm = row.custnm;

  try {
    const res = await api.post('/hpio/HPIO_350U_STR', {
        actkind: 'S0',
        cmpycd: authStore.cmpycd,
        prodid: row.prodid || 0, // 💡 좌측 목록의 PRODID로 상세 조회
        linecd: searchForm.linecd,
        custcd: row.custcd,
        ordymd: row.ordymd,
        proymd: masterInfo.proymd,
        progcd: '888', equpcd: '', prodcd: '200', wkgbn: '', whcd: '',
        itemcd: '', itsize: '', unit: '', prdqty: 0, godqty: 0, errqty: 0,
        lotymd: '', lotno: '', workmm: 0, bigo: '', outym: '', outno: '', useyn: 'Y', updemp: authStore.userid
    });
    const data = (res.data || []).map((i: any) => {
        const amt = Math.floor(Number(i.prdqty || 0) * Number(i.price || 0));
        return {
            ...i,
            outamt: amt,
            outvat: Math.floor(amt * 0.1),
            outtot: Math.floor(amt * 1.1),
            _state: (i.prodid && Number(i.prodid) > 0) ? 'EXIST' : 'NEW',
            _status: '수정'
        }
    });
    grid1?.setData(data);
    if (data.length > 0) {
      grid1?.selectRow(grid1.getRows()[0]);
      fetchPerformanceDetails(data[0]);
    } else {
      grid2?.clearData(); selectedProduct.itemcd = ''; selectedProduct.itemnm = '';
    }
  } catch (e) { vAlertError('상세 조회 실패'); }
}

const fetchPerformanceDetails = async (row: any) => {
  selectedProduct.itemcd = row.itemcd;
  selectedProduct.itemnm = row.itemnm;
  selectedProduct.prodid = row.prodid; // 💡 상위 PRODID 보관

  if (!row.itemcd) { grid2?.clearData(); return; }

  // 💡 신규 제품 행(prodid 없음)인 경우 서버 조회를 하지 않고 그리드만 초기화
  if (!row.prodid) {
    grid2?.clearData();
    return;
  }

  try {
    // 🚀 수정된 XML 명세(14개 파라미터)에 맞춰 조회 호출
    const res = await api.post('/hpio/HPIO_351U_STR', {
        actkind: 'S0',
        cmpycd: authStore.cmpycd,
        prodid: row.prodid,
        matlid: 0,
        mitemcd: '', mitsize: '', munit: '', whcd: '',
        befprog: '', astkind: '', soqty: 0, inqty: 0, useyn: 'Y',
        updemp: authStore.userid
    });
    grid2?.setData((res.data || []).map((i: any) => ({ ...i, _state: 'EXIST', _status: '수정' })));
  } catch (e) {}
}

const saveAll = async () => {
  if (masterInfo.proymd <= closingInfo.clsymd) return vAlertError('회계 마감된 일자입니다.');
  const prods = grid1?.getData().filter((r: any) => r._status) || [];
  const mats = grid2?.getData().filter((r: any) => r._status) || [];
  if (!prods.length && !mats.length) return vAlertError('저장할 변경 내용이 없습니다.');

  const hasDelete = prods.some(p => p._status === '삭제' || Number(p.prdqty || 0) === 0 || p.useyn === 'N') ||
                    mats.some(m => m._status === '삭제' || Number(m.inqty || 0) === 0 || m.useyn === 'N');
  const confirmMsg = hasDelete
    ? '삭제 항목이 포함되어 있습니다. (이미 투입된 제품은 삭제가 제한될 수 있습니다)\n계속하시겠습니까?'
    : '변경된 정보를 저장하시겠습니까?';
  if (!confirm(confirmMsg)) return

  try {
    let lastMsg = '';
    for (const p of prods) {
      const actkind = 'U0' // 마스터 삭제 없이 수정(원복) 개념으로 처리
      const resP = await api.post('/hpio/HPIO_350U_STR', {
        ...p,
        actkind,
        cmpycd: authStore.cmpycd,
        linecd: searchForm.linecd,
        custcd: searchForm.custcd,
        ordymd: p.ordymd,
        proymd: masterInfo.proymd,
        progcd: masterInfo.progcd,
        whcd: '300',
        prodcd: '200',
        useyn: p.useyn || 'Y',
        updemp: authStore.userid
      });
      const pData = resP.data?.[0] || {};
      const pValues = pData.returnkeyvalue || Object.values(pData);
      // 서버에서 보낸 에러 메시지(이미 투입됨 등)를 사용자에게 전달
      if (pValues[0] === '000000') throw new Error(String(pValues[1] || '제품 실적 처리 중 오류 발생'));
      lastMsg = pData.msg || pValues[1];
    }
    for (const m of mats) {
      const actkind = (m._status === '삭제' || m.useyn === 'N') ? 'D0' : 'U0'
      const resM = await api.post('/hpio/HPIO_351U_STR', {
        actkind,
        cmpycd: authStore.cmpycd,
        prodid: selectedProduct.prodid, // 💡 부모 PRODID
        matlid: m.matlid || 0,         // 💡 자재 고유 ID
        mitemcd: m.mitemcd,
        mitsize: m.mitsize || '',
        munit: m.munit || '',
        whcd: '300', // 💡 외주공정입고창고 고정
        befprog: m.befprog || '',
        astkind: m.astkind || '',
        soqty: m.soqty || 0,
        inqty: m.inqty || 0,
        useyn: m.useyn || 'Y',
        updemp: authStore.userid
      });
      const mData = resM.data?.[0] || {};
      const mValues = mData.returnkeyvalue || Object.values(mData);
      if (mValues[0] === '000000') throw new Error(String(mValues[1] || '자재 소모 저장 중 오류 발생'));
      lastMsg = mData.msg || mValues[1] || '정상 처리되었습니다.';
    }

    // 💡 성공 메시지 출력 (DB에서 받은 메시지 우선)
    alert(lastMsg || '성공적으로 저장되었습니다.');

    // 현재 선택된 목록 다시 조회
    const selectedRow = grid0?.getSelectedData()[0];
    if(selectedRow) fetchPerformanceMaster(selectedRow);
  } catch (e: any) { alert(e.message || '저장 실패'); }
}

const handleOpenHelp = (type: string, target?: any) => {
  const props: any = { title: '', path: '', data: { cmpycd: authStore.cmpycd }, columns: [], onConfirm: () => {} };

  if (type === 'CUST') {
    props.title = '거래처 선택'; props.path = '/ha00/HA00_00P_STR'; props.data.gubun = 'C4';
    props.columns = [{ title: '코드', field: 'custcd', width: 80 }, { title: '거래처명', field: 'custnm' }];
    props.onConfirm = (d: any) => { searchForm.custcd = d.custcd; searchForm.custnm = d.custnm }
  }
  else if (type === 'ITEM') {
    props.title = '제품 선택'; props.path = '/hp00/HP00_000S_STR'; props.data.gubun = 'I0'; props.data.gbncd = 'A';
    props.columns = [
      { title: '코드', field: 'itemcd', width: 100, hozAlign: 'center' },
      { title: '제품명', field: 'itemnm', width: 200 },
      { title: '규격', field: 'itsize', width: 150 },
      { title: '단위', field: 'unit', width: 80, hozAlign: 'center' }
    ]
    props.onConfirm = (d: any) => {
      target.update({ itemcd: d.itemcd, itemnm: d.itemnm, itsize: d.itsize, unit: d.unit, _status: '수정', _state: 'NEW' });
      selectedProduct.itemcd = d.itemcd; selectedProduct.itemnm = d.itemnm;
    }
  }
  else if (type === 'MAT') {
    props.title = '자재 선택'; props.path = '/hp00/HP00_000S_STR'; props.data.gubun = 'I0'; props.data.gbncd = 'A';
    props.columns = [
      { title: '코드', field: 'itemcd', width: 100, hozAlign: 'center' },
      { title: '자재명', field: 'itemnm', width: 200 },
      { title: '규격', field: 'itsize', width: 150 },
      { title: '단위', field: 'unit', width: 80, hozAlign: 'center' }
    ]
    props.onConfirm = (d: any) => target.update({ mitemcd: d.itemcd, mitemnm: d.itemnm, mitsize: d.itsize, munit: d.unit, mastkind: d.astkind, _status: '수정', _state: 'NEW' })
  }
  else if (type === 'befprog') {
    props.title = '출고공정 선택'; props.path = '/hp00/HP00_000S_STR'; props.data.gubun = 'G0'; props.data.gbncd = searchForm.linecd;
    props.columns = [{ title: '코드', field: 'progcd', width: 100, hozAlign: 'center' }, { title: '공정명', field: 'prognm', width: 200 }];
    props.onConfirm = (d: any) => target.update({ befprog: d.progcd, bprognm: d.prognm })
  }

  Object.assign(modalProps, props)
  modalVisible.value = true
}

const handleRowAction = (row: any) => {
  const d = row.getData();
  if (d._state === 'NEW') row.delete();
  else row.update({ _status: d._status === '삭제' ? '' : '삭제' });
}

const addRow = (type: string) => {
  if (type === 'PROD') grid1?.addRow({ prdqty: 0, price: 0, outtot: 0, _status: '수정', _state: 'NEW', useyn: 'Y' }, true);
  else grid2?.addRow({ inqty: 0, _status: '수정', _state: 'NEW', useyn: 'Y' }, true);
}

const deleteRows = (type: string) => {
  const g = type === 'PROD' ? grid1 : grid2;
  g?.getSelectedRows().forEach(r => handleRowAction(r));
}

const initialize = () => {
  resetForm(searchForm); Object.assign(searchForm, { linecd: '888', fromdt: firstDay.replace(/-/g, ''), todt: initymd });
  resetForm(masterInfo); Object.assign(masterInfo, { proymd: initymd, whcd: '300', iwhcd: '900', progcd: '888' });
  grid0?.clearData(); grid1?.clearData(); grid2?.clearData(); selectedProduct.itemcd = ''; selectedProduct.itemnm = '';
  fetchInitCodes();
}

const formatDateDash = (v: any) => v && v.length === 8 ? `${v.substring(0, 4)}-${v.substring(4, 6)}-${v.substring(6, 8)}` : v;
onMounted(() => {
  fetchInitCodes();
  api.get('/hp00/HP00_000S_STR', { params: { gubun: 'CL', cmpycd: authStore.cmpycd } }).then(r => { if(r.data?.length) closingInfo.clsymd = r.data[0].clsymd })
  nextTick(initGrids);
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; font-size: 12px; }
.grid-container-left { border-right: 1px solid #dee2e6; }
.erp-table-dense th { font-size: 11px; padding: 4px; }
</style>
