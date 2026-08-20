<!--
	=============================================================
	프로그램명	: 외주가공 자재출고 (HPIO340U)
	작성일자	: 2025.03.11
	설명        : 외주가공 자재 출고 관리 (HSOD100U 표준 패턴 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-box-arrow-right me-2 text-primary" style="font-size: 18px;"></i>
        생산정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">외주가공 자재출고 (HPIO340U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchList">조회</button>
        <button class="btn-erp btn-save" @click="saveAll">저장</button>
        <button class="btn-erp btn-delete" @click="handleFullDelete" :disabled="!form_02.outno || form_02.outno === '0000'">전체삭제</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 조회 필터 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
                <col style="width: 10%" /><col style="width: 25%" />
                <col style="width: 10%" /><col style="width: 25%" />
                <col style="width: 30%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light required">생산라인</th>
                <td>
                  <div class="px-1">
                    <select v-model="form_01.linecd" class="form-select form-select-sm" @change="fetchList">
                      <option value="">라인 선택</option>
                      <option v-for="l in lineData" :key="l.linecd" :value="l.linecd">{{ l.linenm }}</option>
                    </select>
                  </div>
                </td>
                <th class="text-center bg-light required">출고일자</th>
                <td>
                  <div class="d-flex align-items-center gap-1 px-1">
                    <input v-model="form_01.fromdt" type="date" class="form-control form-control-sm" @change="fetchList" />
                    <span class="small">~</span>
                    <input v-model="form_01.todt" type="date" class="form-control form-control-sm" @change="fetchList" />
                  </div>
                </td>
                <td class="text-muted small ps-2">※ 생산라인과 연월을 선택하여 조회하십시오.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 투-그리드 레이아웃 영역 -->
      <div class="d-flex gap-2 flex-grow-1 overflow-hidden" style="min-height: 0;">

        <!-- ⬅️ 좌측: 출고 목록 -->
        <div class="card border shadow-sm d-flex flex-column overflow-hidden grid-container-left" style="width: 350px; min-width: 350px;">
          <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
            <span class="fw-bold small text-dark"><i class="bi bi-list-ul me-2 text-primary"></i>출고 목록</span>
          </div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="listTableRef" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>

        <!-- ➡️ 우측: 상세 내용 및 그리드 -->
        <div class="flex-grow-1 d-flex flex-column gap-2 overflow-hidden">

          <!-- 마스터 정보 폼 (이미지 기반 4단 레이아웃) -->
          <div class="card border shadow-sm flex-shrink-0">
            <div class="card-body p-0 bg-white">
              <table class="erp-table-dense w-100">
                <colgroup>
                  <col style="width: 80px;" /><col  />
                  <col style="width: 80px;" /><col style="width: 15%;" />
                  <col style="width: 80px;" /><col style="width: 15%;" />
                  <col style="width: 80px;" /><col style="width: 15%;" />
                </colgroup>
                <tbody>
                  <tr>
                    <th class="required bg-light text-center">품의번호</th>
                    <td>
                      <div class="input-group input-group-sm px-1">
                        <input v-model="form_02.pumym" type="text" class="form-control" style="max-width: 90px;" />
                        <input v-model="form_02.pumno" type="text" class="form-control text-center fw-bold" />
                        <button class="btn btn-outline-secondary px-2" @click="handleOpenHelp('PUM')"><i class="bi bi-search"></i></button>
                      </div>
                    </td>
                    <th class="required bg-light text-center">생산라인</th>
                    <td>
                      <div class="px-1">
                        <select v-model="form_02.linecd" class="form-select form-select-sm">
                          <option value="">라인 선택</option>
                          <option v-for="l in lineData" :key="l.linecd" :value="l.linecd">{{ l.linenm }}</option>
                        </select>
                      </div>
                    </td>
                    <th class="required bg-light text-center">출고번호</th>
                    <td>
                      <div class="d-flex align-items-center gap-1 px-1">
                        <input v-model="form_02.outym" type="text" class="form-control form-control-sm" style="max-width: 90px;" />
                        <input v-model="form_02.outno" type="text" class="form-control form-control-sm text-center bg-light fw-bold text-primary" placeholder="0000" readonly />
                      </div>
                    </td>
                    <th class="required bg-light text-center">출고일자</th>
                    <td>
                      <div class="px-1">
                        <input v-model="form_02.outymd" type="date" class="form-control form-control-sm" />
                      </div>
                    </td>
                  </tr>
                  <tr>
                    <th class="required bg-light text-center">거 래 처</th>
                    <td>
                      <div class="input-group input-group-sm px-1">
                        <input v-model="form_02.custcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
                        <input v-model="form_02.custnm" type="text" class="form-control" readonly />
                        <button class="btn btn-outline-secondary px-2" @click="handleOpenHelp('CUST')"><i class="bi bi-search"></i></button>
                      </div>
                    </td>
                    <th class="required bg-light text-center">생산공정</th>
                    <td>
                      <div class="px-1">
                        <select v-model="form_02.progcd" class="form-select form-select-sm">
                          <option value="">공정 선택</option>
                          <option v-for="p in processData" :key="p.progcd" :value="p.progcd">{{ p.prognm }}</option>
                        </select>
                      </div>
                    </td>
                    <th class="required bg-light text-center">출고창고</th>
                    <td>
                      <div class="px-1">
                        <select v-model="form_02.whcd" class="form-select form-select-sm">
                          <option value="">출고창고 선택</option>
                          <option v-for="w in warehouseData" :key="w.whcd" :value="w.whcd">{{ w.whnm }}</option>
                        </select>
                      </div>
                    </td>
                    <th class="required bg-light text-center">입고창고</th>
                    <td>
                      <div class="px-1">
                        <select v-model="form_02.iwhcd" class="form-select form-select-sm">
                          <option value="">입고창고 선택</option>
                          <option v-for="w in warehouseData" :key="w.whcd" :value="w.whcd">{{ w.whnm }}</option>
                        </select>
                      </div>
                    </td>
                  </tr>
                  <tr>
                    <th class="required bg-light text-center">제품코드</th>
                    <td>
                      <div class="input-group input-group-sm px-1">
                        <input v-model="form_02.itemcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
                        <input v-model="form_02.itemnm" type="text" class="form-control" readonly />
                        <button class="btn btn-outline-secondary px-2" @click="handleOpenHelp('HITEM')"><i class="bi bi-search"></i></button>
                      </div>
                    </td>
                    <th class="required bg-light text-center">가공단가</th>
                    <td>
                      <div class="px-1">
                        <input v-model="form_02.price" type="number" class="form-control form-control-sm text-end" />
                      </div>
                    </td>
                    <th class="required bg-light text-center">생 산 량</th>
                    <td>
                      <div class="px-1">
                        <input v-model="form_02.proqty" type="number" class="form-control form-control-sm text-end" @change="fetchDetailItems" />
                      </div>
                    </td>
                    <th class="required bg-light text-center">입고일자</th>
                    <td>
                      <div class="px-1">
                        <input v-model="form_02.proymd" type="date" class="form-control form-control-sm" />
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- 상세 품목 그리드 영역 -->
          <div class="card border shadow-sm flex-grow-1 d-flex flex-column overflow-hidden grid-container-right">
            <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
              <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>투입 자재 상세 리스트</span>
              <div class="d-flex gap-1">
                <button class="btn btn-sm btn-outline-primary py-0 px-2 fw-bold" @click="addRow" style="font-size: 11px;">+ 행추가</button>
                <button class="btn btn-sm btn-outline-danger py-0 px-2 fw-bold" @click="deleteSelectedRows" style="font-size: 11px;">- 행삭제</button>
              </div>
            </div>
            <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
              <div ref="itemTableRef" class="tabulator-instance flex-grow-1"></div>
            </div>
          </div>

        </div>

      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, computed, nextTick, watch } from 'vue'
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
const { today } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps, openHelp } = useCommonHelp()

// [1] 데이터 모델링
const lineData = ref<any[]>([])
const processData = ref<any[]>([])
const warehouseData = ref<any[]>([])

const form_01 = reactive({
  linecd: '888',
  linenm: '외주라인',
  fromdt: today.substring(0, 8) + '01',
  todt: today
})

const form_02 = reactive<any>({
  actkind: 'S0', cmpycd: authStore.cmpycd,
  pumym: '',
  pumno: '',
  linecd: '888', linenm: '외주라인',
  outym: '',
  outno: '',
  iorowno: '',
  outymd: today,
  custcd: '', custnm: '',
  progcd: '888', prognm: '외주공정',
  whcd: '100', whnm: '자재창고',
  iwhcd: '900', iwhnm: '외부창고',
  itemcd: '', itemnm: '', itsize: '', unit: '', astkind: '',
  price: 0, proqty: 0, proymd: today,
  remark: '',
  inym: '', inno: ''
})

function formatDateDash(v: string) {
  if (v && v.length === 8) return `${v.substring(0, 4)}-${v.substring(4, 6)}-${v.substring(6, 8)}`;
  return v;
}

function formatDate(v: string) { return v && v.length === 8 ? `${v.substring(0, 4)}-${v.substring(4, 6)}-${v.substring(6, 8)}` : '' }

const listTableRef = ref<HTMLDivElement | null>(null)
const itemTableRef = ref<HTMLDivElement | null>(null)
let grid1: Tabulator | null = null
let grid2: Tabulator | null = null

// [2] 그리드 초기화
const initGrids = () => {
  // Left: 출고 목록
  grid1 = new Tabulator(listTableRef.value!, {
    layout: "fitColumns", height: "100%", placeholder: "데이터 없음", selectable: 1,
    columns: [
      { title: "출고번호", field: "outno_full", hozAlign: "center", width: 120, cssClass: "fw-bold text-primary", headerSort: false },
      { title: "거래처명", field: "custnm", hozAlign: "left", headerSort: false },
      { title: "출고일", field: "outymd", hozAlign: "center", width: 100, formatter: (c) => formatDate(c.getValue() || ''), headerSort: false }
    ],
  });
  grid1.on("rowClick", (e, row) => fetchDetail(row.getData()));

  // Right: 투입 자재 상세
  grid2 = new Tabulator(itemTableRef.value!, {
    layout: "fitColumns", height: "100%", placeholder: "항목 없음", selectable: true,
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "선택", width: 40, hozAlign: "center", formatter: "rowSelection", titleFormatter: "rowSelection" },
      { title: "상태", field: "_status", width: 50, hozAlign: "center", formatter: (c) => {
          const v = c.getValue();
          if (v === '입력') return '<span class="badge bg-primary">입력</span>';
          if (v === '수정') return '<span class="badge bg-warning text-dark">수정</span>';
          if (v === '삭제') return '<span class="badge bg-danger">삭제</span>';
          return '';
      }},
      { title: "자재코드", field: "itemcd", width: 100, hozAlign: "center", cellClick: (e, cell) => handleOpenHelp('MITEM', cell.getRow()) },
      { title: "자재명", field: "itemnm", minWidth: 200, widthGrow: 1, cssClass: "fw-bold text-primary", cellClick: (e, cell) => handleOpenHelp('MITEM', cell.getRow()) },
      { title: "규격", field: "itsize", width: 150 },
      { title: "단위", field: "unit", width: 70, hozAlign: "center" },
      { title: "출고수량", field: "ioqty", width: 100, hozAlign: "right", editor: "number", cellEdited: (cell) => updateStatus(cell.getRow()) },
      { title: "비고", field: "remark", minWidth: 150, editor: "input", cellEdited: (cell) => updateStatus(cell.getRow()) }
    ]
  });
}

const updateStatus = (row: any) => {
  const d = row.getData();
  if (d._state === 'EXIST' && d._status !== '삭제') row.update({ _status: '수정' });
}

// [3] 비즈니스 로직
async function fetchList() {
  if (!form_01.linecd) return vAlertError('생산라인을 먼저 선택하세요.');

  try {
    const res = await api.post('/hpio/HPIO_340U_STR', {
      actkind: 'L',
      cmpycd: authStore.cmpycd,
      fromdt: form_01.fromdt.replace(/-/g, ''),
      todt: form_01.todt.replace(/-/g, ''),
      iogbn: '200',
      outym: '',
      outno: '0000',
      proqty: 0,
      linecd: form_01.linecd
    });
    const data = res.data || [];
    grid1?.setData(data.map((i: any) => ({
        ...i,
        outno_full: `${i.outym}-${i.outno}`,
        linecd: i.linecd,
        progcd: i.progcd,
        proymd: i.proymd,
        custcd: i.ocustcd,
        custnm: i.ocustnm
     })));
    vAlert('조회되었습니다.');
  } catch (e) { vAlertError('조회 실패'); }
}

async function fetchDetail(row: any) {
  const formattedRow = { ...row };
  if (row.outymd) formattedRow.outymd = formatDateDash(row.outymd);
  if (row.proymd) formattedRow.proymd = formatDateDash(row.proymd);

  Object.assign(form_02, formattedRow);

  if (row.linecd) {
    api.post('/hp00/HP00_000S_STR', { gubun: 'G0', cmpycd: authStore.cmpycd, gbncd: row.linecd }).then(res => {
      processData.value = (res.data || []).map((i: any) => ({ progcd: i.progcd, prognm: i.prognm }));
    });
  }
  try {
    const res = await api.post('/hpio/HPIO_341U_STR', {
      actkind: 'S',
      cmpycd: authStore.cmpycd,
      iogbn: '200',
      outym: row.outym,
      outno: row.outno,
      ioqty: 0
    });
    grid2?.setData((res.data || []).map((i: any) => ({ ...i, _state: 'EXIST', _status: '' })));
  } catch (e) { vAlertError('상세 조회 실패'); }
}

// 품목/수량 변경 시 자재 상세 자동 조회 (준비조회)
async function fetchDetailItems() {
  if (!form_02.itemcd) return;

  // 수량이 없으면 1로 조회하여 목록이라도 나오게 함
  const searchQty = (form_02.proqty && Number(form_02.proqty) > 0) ? form_02.proqty : 1;

  try {
    const res = await api.post('/hpio/HPIO_341U_STR', {
      actkind: 'B',
      cmpycd: authStore.cmpycd,
      iogbn: '200',
      pumym: String(form_02.pumym || '').replace(/-/g, ''),
      pumno: form_02.pumno,
      itemcd: form_02.itemcd,
      ioqty: searchQty,
      outym: String(form_02.outym || '').replace(/-/g, ''),
      outno: form_02.outno,
      linecd: form_02.linecd,
      progcd: form_02.progcd,
      ioymd: form_02.outymd.replace(/-/g, ''),
      proymd: form_02.proymd.replace(/-/g, '')
    });
    grid2?.setData((res.data || []).map((i: any) => ({ ...i, _status: '입력', _state: 'NEW' })));
  } catch (e) {}
}

async function saveAll() {
  if (!form_02.linecd) return vAlertError('생산라인을 선택하세요.');
  if (!form_02.progcd) return vAlertError('생산공정을 선택하세요.');
  if (!form_02.whcd) return vAlertError('출고창고를 선택하세요.');
  if (!form_02.iwhcd) return vAlertError('입고창고를 선택하세요.');
  if (!form_02.outymd) return vAlertError('출고일자를 선택하세요.');
  if (!form_02.proymd) return vAlertError('입고일자를 선택하세요.');
  if (!form_02.custcd) return vAlertError('거래처를 선택하세요.');

  const details = grid2?.getData().filter((r: any) => r._status) || [];

  if ((!form_02.outno || form_02.outno === '0000') && details.length === 0) return vAlertError('저장할 항목이 없습니다.');

  if (!confirm('저장하시겠습니까?')) return

  try {
    const act = (!form_02.outno || form_02.outno === '0000' || form_02.outno.trim() === '') ? 'A' : 'U';
    // 1. 마스터 저장
    const resM = await api.post('/hpio/HPIO_340U_STR', {
      ...form_02,
      actkind: act,
      pumym: form_02.pumym.replace(/-/g, ''),
      outym: form_02.outymd.replace(/-/g, '').substring(0, 6),
      outymd: form_02.outymd.replace(/-/g, ''),
      proymd: form_02.proymd.replace(/-/g, ''),
      inyn: 'Y', // 입고처리 강제 (SI 연동)
      owhcd: form_02.whcd,
      ocustcd: form_02.custcd,
      iwhcd: form_02.iwhcd,
      prodcd: '200',
      userid: authStore.userid
    });

    const mResult = resM.data?.[0];
    if (mResult && (mResult.outym === '000000' || mResult.col_0 === '000000')) {
      alert(mResult.outno || mResult.col_1 || '저장 중 오류가 발생했습니다.');
      return;
    }

    const newoutym = mResult?.outym || '';
    const newoutno = mResult?.outno || '';
    const newinno = mResult?.inno || '';

    // 2. 상세 저장
    for (const item of details) {
      const dAct = item._status === '입력' ? 'A' : (item._status === '삭제' ? 'D' : 'U');
      const resD = await api.post('/hpio/HPIO_341U_STR', {
        ...item,
        actkind: dAct,
        cmpycd: authStore.cmpycd,
        iogbn: '200',
        ioymd: form_02.outymd.replace(/-/g, ''),
        outym: newoutym,
        outno: newoutno,
        inno: newinno,
        owhcd: form_02.whcd,
        custcd: form_02.custcd,
        linecd: form_02.linecd,
        progcd: form_02.progcd,
        proymd: form_02.proymd.replace(/-/g, ''),
        pumym: form_02.pumym.replace(/-/g, ''),
        pumno: form_02.pumno,
        userid: authStore.userid
      });

      const dResult = resD.data?.[0];
      if (dResult && (dResult.outym === '000000' || dResult.col_0 === '000000')) {
        alert(dResult.outno || dResult.col_1 || '상세 저장 중 오류가 발생했습니다.');
        return;
      }
    }

    vAlert('저장되었습니다.');
    fetchList();
    fetchDetail({ outym: form_02.outym, outno: newoutno, inno: newinno });
  } catch (e: any) {
    const errorMsg = e.response?.data?.message || e.message || '저장 오류 발생';
    vAlertError(errorMsg);
  }
}

async function handleFullDelete() {
  if (!confirm('전체 삭제하시겠습니까?')) return;
  try {
    await api.post('/hpio/HPIO_340U_STR', { ...form_02, actkind: 'D', userid: authStore.userid });
    vAlert('삭제되었습니다.'); initialize(); fetchList();
  } catch (e: any) {
    const errorMsg = e.response?.data?.message || e.message || '삭제 실패';
    vAlertError(errorMsg);
  }
}

const handleOpenHelp = (type: string, target?: any) => {
  const props: any = { title: '', path: '', data: { cmpycd: authStore.cmpycd }, columns: [], onConfirm: () => {} };

  if (type === 'CUST') {
    props.title = '거래처 선택'; props.path = '/ha00/HA00_00P_STR'; props.data.gubun = 'C4';
    props.columns = [{ title: '코드', field: 'custcd', width: 80 }, { title: '거래처명', field: 'custnm' }];
    props.onConfirm = (d: any) => { const item = d; form_02.custcd = item.custcd; form_02.custnm = item.custnm }
  } else if (type === 'PUM') {
    props.title = '외주가공 품의조회';
    props.path = '/hpio/HPIO_340U_POPUP';
    props.data = {
      cmpycd: authStore.cmpycd,
      fromdt: form_01.fromdt.replace(/-/g, ''),
      todt: form_01.todt.replace(/-/g, ''),
    };
    props.columns = [
        { title: '품의일자', field: 'pumymd', width: 100, hozAlign: 'center', formatter: (c:any) => formatDate(c.getValue()) },
        { title: '번호', field: 'pumno', width: 60, hozAlign: 'center' },
        { title: '거래처', field: 'custnm', width: 150 },
        { title: '품목명', field: 'itemnm', width: 180 },
        { title: '규격', field: 'itsize', width: 120 }
    ];
    props.onConfirm = (d: any) => {
        const item = d;
        form_02.pumym = item.pumym;
        form_02.pumno = item.pumno;
        form_02.custcd = item.custcd;
        form_02.custnm = item.custnm;
        form_02.itemcd = item.itemcd;
        form_02.itemnm = item.itemnm;
        form_02.itsize = item.itsize;
        form_02.unit = item.unit;
        form_02.price = item.price;
        if (item.qty) form_02.proqty = item.qty;

        nextTick(() => { fetchDetailItems(); });
    }
  } else if (type === 'HITEM') {
    props.title = '제품 선택'; props.path = '/hp00/HP00_000S_STR'; props.data.gubun = 'I0';
    props.columns = [{ title: '코드', field: 'itemcd', width: 100 }, { title: '제품명', field: 'itemnm' }, { title: '규격', field: 'itsize' }];
    props.onConfirm = (d: any) => {
        form_02.hitemcd = d.itemcd; form_02.hitemnm = d.itemnm; form_02.hitsize = d.itsize; form_02.hunit = d.unit;
    }
  } else if (type === 'MITEM') {
    props.title = '자재 선택'; props.path = '/hp00/HP00_000S_STR'; props.data.gubun = 'I0';
    props.columns = [{ title: '코드', field: 'itemcd', width: 100 }, { title: '자재명', field: 'itemnm' }, { title: '규격', field: 'itsize' }];
    props.onConfirm = (d: any) => {
        target.update({ itemcd: d.itemcd, itemnm: d.itemnm, itsize: d.itsize, unit: d.unit, _status: '입력', _state: 'NEW' });
    }
  }

  Object.assign(modalProps, props); modalVisible.value = true;
}

const handleRowAction = (row: any) => { const d = row.getData(); if (d._state === 'NEW') row.delete(); else row.update({ _status: d._status === '삭제' ? '' : '삭제' }); }
const deleteSelectedRows = () => { grid2?.getSelectedRows().forEach(r => handleRowAction(r)); }
const addRow = () => grid2?.addRow({ ioqty: 0, _status: '입력', _state: 'NEW' }, true);

const initialize = () => {
  resetForm(form_02);
  Object.assign(form_02, {
      cmpycd: authStore.cmpycd, outym: today.replace(/-/g, '').substring(0, 6), outno: '0000', outymd: today,
      proymd: today, price: 0, proqty: 0, actkind: 'S0',
      linecd: '888', linenm: '외주라인',
      progcd: '888', prognm: '외주공정',
      whcd: '100', whnm: '자재창고',
      iwhcd: '900', iwhnm: '외부창고'
  });
  grid1?.clearData(); grid2?.clearData();
}

watch(() => form_02.linecd, (newVal) => {
  if (newVal) {
    api.post('/hp00/HP00_000S_STR', { gubun: 'G0', cmpycd: authStore.cmpycd, gbncd: newVal }).then(res => {
      processData.value = (res.data || []).map((i: any) => ({ progcd: i.progcd, prognm: i.prognm }));
    });
  } else {
    processData.value = [];
  }
});

onMounted(() => {
  nextTick(initGrids);
  // 라인 데이터 로드
  api.post('/hp00/HP00_000S_STR', { gubun: 'L0', cmpycd: authStore.cmpycd, gbncd: 'Y' }).then(res => {
    lineData.value = (res.data || []).map((i: any) => ({ linecd: i.linecd, linenm: i.linenm }));
  });
  // 창고 데이터 로드
  api.post('/hs00/HS00_000S_STR', { gubun: 'W0', cmpycd: authStore.cmpycd }).then(res => {
    warehouseData.value = (res.data || []).map((i: any) => ({ whcd: i.whcd, whnm: i.whnm }));
  });

})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; font-size: 12px; }
.erp-table-dense th { font-size: 11px; padding: 4px; }
</style>
