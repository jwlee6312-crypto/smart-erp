<!--
	=============================================================
	프로그램명	: 외상매출건별 입금입력 (HSIO305U)
	작성일자	: 2025.02.24
	설명        : 매출건별 상계 처리 (HSIO300U의 팝업 및 여신 조회 기능 완벽 이식)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom" style="height: 48px !important;">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-cash-coin me-2 text-primary" style="font-size: 18px;"></i>
        영업관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        입금관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">외상매출건별 입금입력 (HSIO305U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-2">
        <button class="btn-erp btn-init" @click="initialize">신규</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-save" @click="save" :disabled="isClosed">저장</button>
        <button class="btn-erp btn-delete" @click="handleDelete" :disabled="!formMst.imno || formMst.imno === '0000' || isClosed">삭제</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [필터] 조회 조건 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense w-100">
            <colgroup><col style="width: 100px;" /><col style="width: 160px;" /><col style="width: 100px;" /><col /><col style="width: 100px;" /><col style="width: 220px;" /></colgroup>
            <tbody>
              <tr>
                <th class="bg-light text-center">기준일자</th>
                <td><input type="date" v-model="filter.imymd" class="form-control form-control-sm" /></td>
                <th class="required bg-light text-center">거래처</th>
                <td>
                  <div class="input-group input-group-sm">
                    <input v-model="filter.schCustcd" class="form-control bg-light" style="width: 70px;" readonly />
                    <input v-model="filter.schCustnm" class="form-control" placeholder="거래처 검색" @keyup.enter="handleOpenHelp('CUST_SCH')" />
                    <button class="btn btn-outline-secondary" @click="handleOpenHelp('CUST_SCH')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="bg-light text-center">입금번호</th>
                <td>
                  <div class="d-flex gap-1 align-items-center">
                    <input v-model="filter.schImym" class="form-control form-control-sm text-center" style="width: 90px;" placeholder="YYYYMM" />
                    <input v-model="filter.schImno" class="form-control form-control-sm text-center" style="width: 60px;" placeholder="0000" />
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [상단 그리드] 미수 내역 -->
      <div class="card border shadow-sm flex-grow-1 d-flex flex-column overflow-hidden grid-container-standard">
        <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark d-flex justify-content-between align-items-center">
          <span><i class="bi bi-list-check me-2 text-primary"></i>매출 건별 미수 내역 (상계대상)</span>
          <div class="d-flex gap-4">
             <span class="text-primary small fw-bold">미수 합계: {{ formatNumber(totals.janSum) }}</span>
             <span class="text-danger small fw-bold">상계 선택 합계: {{ formatNumber(totals.setSum) }}</span>
          </div>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="grid1Ref" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>

      <!-- [중간] 마스터 폼 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense w-100">
            <colgroup><col style="width: 120px;" /><col /><col style="width: 120px;" /><col /><col style="width: 120px;" /><col /></colgroup>
            <tbody>
              <tr>
                <th class="required bg-light text-center">입금부서</th>
                <td>
                  <div class="input-group input-group-sm">
                    <input v-model="formMst.deptcd" class="form-control bg-light" style="width: 60px;" readonly />
                    <input v-model="formMst.deptnm" class="form-control" readonly />
                    <button class="btn btn-outline-secondary" @click="handleOpenHelp('DEPT')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="bg-light text-center">입금번호</th>
                <td>
                  <div class="d-flex gap-1">
                    <input v-model="formMst.imym" class="form-control bg-light text-center" readonly style="width: 90px;" />
                    <input v-model="formMst.imno" class="form-control bg-light text-center text-primary fw-bold" readonly style="width: 60px;" />
                  </div>
                </td>
                <th class="required bg-light text-center">입금일자</th>
                <td><input type="date" v-model="formMst.imymd" class="form-control form-control-sm" :readonly="isClosed" /></td>
              </tr>
              <tr>
                <th class="bg-light text-center">거래처명</th>
                <td><input v-model="formMst.custnm" class="form-control bg-light" readonly /></td>
                <th class="bg-light text-center">여신잔액</th>
                <td><input :value="formatNumber(formMst.janamt)" class="form-control bg-light text-end" readonly /></td>
                <th class="bg-light text-center">여신기한</th>
                <td><input v-model="formMst.rcvdd" class="form-control bg-light text-center" readonly style="width: 60px;" /></td>
              </tr>
              <tr>
                <th class="bg-light text-center">적요</th>
                <td colspan="5"><input v-model="formMst.remark" class="form-control form-control-sm" :readonly="isClosed" /></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단 그리드] 입금 수단 -->
      <div class="card border shadow-sm flex-shrink-0 d-flex flex-column overflow-hidden grid-container-standard" style="height: 200px;">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
          <span class="fw-bold small text-dark"><i class="bi bi-wallet2 me-2 text-primary"></i>입금 수단 리스트</span>
          <div class="d-flex gap-3 align-items-center">
            <span class="text-primary small fw-bold">수단 합계: {{ formatNumber(totals.paySum) }}</span>
            <div class="btn-group">
              <button class="btn btn-sm btn-outline-primary py-0 px-2 fw-bold" @click="addPayRow" :disabled="isClosed" style="font-size: 11px;">+ 행추가</button>
              <button class="btn btn-sm btn-outline-danger py-0 px-2 fw-bold" @click="deletePayRow" :disabled="isClosed" style="font-size: 11px;">- 행삭제</button>
            </div>
          </div>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="grid2Ref" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, nextTick, computed, onUnmounted } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { getDate } from '@/composables/useDate'

const authStore = useAuthStore()
const { today } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()

// [로컬 상태 정의] 팝업 관리
const modalVisible = ref(false)
const modalProps = reactive<any>({
  title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table'
})

const filter = reactive({ imymd: today, schCustcd: '', schCustnm: '', schImym: today.substring(0, 7).replace(/-/g, ''), schImno: '' })
const formMst = reactive<any>({
  imym: '', imno: '0000', imymd: today, deptcd: authStore.deptcd, deptnm: authStore.deptnm,
  custcd: '', custnm: '', janamt: 0, rcvdd: '', remark: '', actkind: 'A0',
  hanamt: 0, handd: ''
})

const totals = reactive({ janSum: 0, setSum: 0, paySum: 0 })
const closingInfo = reactive({ sclsym: '' })
const billgbnOptions = ref<any[]>([])

const isClosed = computed(() => {
  if (!closingInfo.sclsym || !formMst.imymd) return false
  return formMst.imymd.replace(/-/g, '').substring(0, 6) <= closingInfo.sclsym
})

const grid1Ref = ref<HTMLDivElement | null>(null)
const grid2Ref = ref<HTMLDivElement | null>(null)
let grid1: Tabulator | null = null
let grid2: Tabulator | null = null

/** 💡 데이터 정규화 (고정 길이 보장: imgbn 3, dacctcd 7, cacctcd 7) */
const normalize = (data: any[]) => {
  if (!data || !Array.isArray(data)) return [];
  return data.map(item => {
    const obj: any = {};
    for (const key in item) {
      const val = item[key];
      const k = key.toLowerCase();
      if (['dacctcd', 'cacctcd'].includes(k)) {
        obj[k] = String(val || '').trim().padEnd(7, ' ');
      } else if (k === 'imgbn') {
        obj[k] = String(val || '').trim().padEnd(3, ' ');
      } else if (typeof val === 'string') {
        obj[k] = val.trim();
      } else {
        obj[k] = val;
      }
    }
    return obj;
  });
}

const initGrids = () => {
  // 1. 상단: 미수 내역
  grid1 = new Tabulator(grid1Ref.value!, {
    layout: "fitColumns", height: "100%", placeholder: "데이터가 없습니다.",
    selectable: true,
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "선택", width: 40, hozAlign: "center", formatter: "rowSelection", titleFormatter: "rowSelection", cellClick: (e, cell) => {
          cell.getRow().toggleSelect();
          calculateTotals();
      }},
      { title: "정산번호", field: "jsanno_full", hozAlign: "center", width: 120,
        formatter: (c) => { const d = c.getData(); return d.jsanym ? `${d.jsanym}-${d.jsanno}` : ''; }
      },
      { title: "출고번호", field: "iono_full", hozAlign: "center", width: 120,
        formatter: (c) => { const d = c.getData(); return d.ioym ? `${d.ioym}-${d.iono}` : ''; }
      },
      { title: "매출부서", field: "deptnm", hozAlign: "left" },
      { title: "정산일", field: "jsanymd", width: 100, hozAlign: "center",
        formatter: (c) => { const val = c.getValue(); return (val && val.length === 8) ? `${val.substring(0,4)}-${val.substring(4,6)}-${val.substring(6,8)}` : val; }
      },
      { title: "매출총액", field: "maeamt", width: 110, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "미수잔액", field: "janamt", width: 110, hozAlign: "right", formatter: "money", cssClass: "text-danger fw-bold" },
      { title: "상계금액", field: "imamt", width: 120, hozAlign: "right", editor: "number", cellEdited: () => calculateTotals(), cssClass: "bg-yellow" }
    ],
  });

  // 2. 하단: 입금 수단
  grid2 = new Tabulator(grid2Ref.value!, {
    layout: "fitColumns", height: "100%", selectable: true,
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "선택", width: 40, hozAlign: "center", formatter: "rowSelection", titleFormatter: "rowSelection" },
      {
        title: "입금유형", field: "imtypenm", width: 180,
        cssClass: "text-primary fw-bold cursor-pointer",
        cellClick: (e, cell) => handleOpenHelp('IMTYPE', cell.getRow()),
        formatter: (c) => c.getValue() || '<span class="text-muted small">선택(클릭)</span>'
      },
      { title: "입금액", field: "imamt", width: 110, hozAlign: "right", editor: "number", formatter: "money", formatterParams: { precision: 0 }, cellEdited: () => calculateTotals() },
      { title: "관리번호", field: "mgtno", width: 150,
        cssClass: "text-primary cursor-pointer",
        cellClick: (e, cell) => handleOpenHelp('MGT', cell.getRow()),
        formatter: (cell) => {
            const v = cell.getValue() || "";
            const d = cell.getRow().getData();
            if (['200', '210', '300', '500', '510', '600'].includes(String(d.imgbn || '').trim())) {
                return `<div class="d-flex justify-content-between"><span>${v}</span><i class="bi bi-search text-primary small"></i></div>`;
            }
            return v;
        }
      },
      { title: "어음액면가", field: "billamt", width: 110, hozAlign: "right", editor: "number", formatter: "money", formatterParams: { precision: 0 }, cellEdited: () => calculateTotals() },
      { title: "어음종류", field: "billgbn", width: 100, editor: "list",
        editorParams: { values: () => { const lookup: any = {}; billgbnOptions.value.forEach(o => { lookup[o.codecd] = o.codenm; }); return lookup; } },
        formatter: (c) => { const o = billgbnOptions.value.find(x => x.codecd === c.getValue()); return o ? o.codenm : c.getValue(); }
      },
      { title: "발행일", field: "pubymd", width: 110, editor: "input" },
      { title: "발행인", field: "pubman", width: 110, editor: "input" },
      { title: "만기일자", field: "endymd", width: 110, hozAlign: "center", editor: "input" },
      { title: "발행은행", field: "pubbank", width: 150, editor: "input" },
      { title: "삭제", width: 40, hozAlign: "center", formatter: () => "<i class='bi bi-trash text-danger cursor-pointer'></i>",
        cellClick: (e, cell) => { cell.getRow().delete(); calculateTotals(); }
      }
    ]
  });
}

const calculateTotals = () => {
  totals.janSum = (grid1?.getData() || []).reduce((acc, r:any) => acc + Number(r.janamt || 0), 0);
  totals.setSum = (grid1?.getSelectedData() || []).reduce((acc, r:any) => acc + Number(r.imamt || 0), 0);
  totals.paySum = (grid2?.getData() || []).reduce((acc, r:any) => acc + Number(r.imamt || 0), 0);
}

const search = async () => {
  if (!filter.schCustcd) return vAlertError('거래처를 선택하세요.');
  try {
    const res = await api.post('/hsio/HSIO_300U_STR', { actkind: 'S2', cmpycd: authStore.cmpycd, custcd: filter.schCustcd, imymd: filter.imymd.replace(/-/g, '') });
    const normalized = normalize(res.data);

    grid1?.setData(normalized.map(i => ({
      ...i,
      imamt: Number(i.janamt || 0),
      maeamt: Number(i.ioamt || 0) + Number(i.iovat || 0)
    })));

    formMst.custcd = filter.schCustcd;
    formMst.custnm = filter.schCustnm;

    // 💡 HS00_150S_STR 사용하여 여신 정보 조회 (HSIO300U 이식)
    api.post('/hs00/HS00_150S_STR', { cmpycd: authStore.cmpycd, custnm: filter.schCustnm }).then(r => {
      if(r.data?.length) {
        const n = normalize(r.data)[0];
        formMst.janamt = n.janamt;
        formMst.rcvdd = n.recdd;
        formMst.hanamt = n.hanamt;
        formMst.handd = n.handd;
      }
    });

    vAlert('조회되었습니다.');
    nextTick(calculateTotals);
  } catch (e) { vAlertError('조회 실패'); }
}

const save = async () => {
  if (Math.abs(totals.setSum - totals.paySum) > 1) return vAlertError('상계 합계와 수단 합계가 일치해야 합니다.');
  if (totals.setSum <= 0) return vAlertError('금액을 입력하세요.');

  try {
    const payload = {
      mst: { ...formMst, imymd: formMst.imymd.replace(/-/g, ''), actkind: formMst.imno === '0000' ? 'A0' : 'U0' },
      dtl: grid2?.getData().map((d:any) => ({ ...d, actkind: 'A2', cmpycd: authStore.cmpycd })),
      setdtl: grid1?.getSelectedData().map((d:any) => ({ ...d, actkind: 'A0', cmpycd: authStore.cmpycd }))
    };
    await api.post('/hsio/HSIO_300U_SAVE', payload);
    vAlert('저장되었습니다.');
    initialize();
  } catch (e: any) { vAlertError(e.response?.data?.message || '저장 실패'); }
}

const handleOpenHelp = (type: string, targetRow?: any) => {
  if (type === 'CUST_SCH') {
    Object.assign(modalProps, {
      title: '거래처 선택', path: '/ha00/HA00_00P_STR', defaultField: 'custnm',
      data: { gubun: 'C1', cmpycd: authStore.cmpycd, gbncd: '', code: '', remark: '' },
      columns: [{ title: '코드', field: 'custcd', width: 80, hozAlign: 'center' }, { title: '거래처명', field: 'custnm', width: 200 }],
      onConfirm: (d: any) => { filter.schCustcd = d.custcd; filter.schCustnm = d.custnm; search(); }
    });
    modalVisible.value = true;
  } else if (type === 'DEPT') {
    Object.assign(modalProps, {
      title: '부서 선택', path: '/ha00/HA00_00P_STR', defaultField: 'deptnm',
      data: { gubun: 'D1', cmpycd: authStore.cmpycd, gbncd: '', code: '', remark: '' },
      columns: [{ title: '코드', field: 'deptcd', width: 80, hozAlign: 'center' }, { title: '부서명', field: 'deptnm', width: 200 }],
      onConfirm: (d: any) => { formMst.deptcd = d.deptcd; formMst.deptnm = d.deptnm; }
    });
    modalVisible.value = true;
  } else if (type === 'IMTYPE') {
    // 💡 입금유형 팝업 (HSIO300U 이식)
    Object.assign(modalProps, {
      title: '입금유형 선택', path: '/hs00/HS00_000S_STR', defaultField: 'imgbnnm',
      data: { gubun: 'I2', cmpycd: authStore.cmpycd, gbncd: '', code: '', codennm: '', etcval: '' },
      columns: [
        { title: '유형코드', field: 'imgbn', width: 80, hozAlign: 'center' },
        { title: '입금유형명', field: 'imgbnnm', width: 180 },
        { title: '차변계정', field: 'dacctcd', visible: false },
        { title: '대변계정', field: 'cacctcd', visible: false }
      ],
      onConfirm: (d: any) => {
        const res = normalize([d])[0];
        targetRow.update({
          imgbn: res.imgbn, dacctcd: res.dacctcd, cacctcd: res.cacctcd,
          imtypenm: res.imgbnnm, mgtno: ''
        });
      }
    });
    modalVisible.value = true;
  } else if (type === 'MGT') {
    // 💡 관리번호 팝업 (HSIO300U 이식)
    const rowData = normalize([targetRow.getData()])[0];
    const imgbn = String(rowData.imgbn || '').trim();
    if (imgbn === '400') return vAlertError('어음번호는 직접 입력하십시오.');

    let gubun = 'M0'; let gbncd = '010'; let remark = rowData.dacctcd;
    let columns: any[] = [{ title: '번호/코드', field: 'mgtno', width: 150 }, { title: '명칭', field: 'mgtnm', width: 200 }];

    if (imgbn === '200' || imgbn === '600' || imgbn === '210') {
      remark = (imgbn === '210') ? '1145' : '1120';
      columns = [
        { title: '계좌번호', field: 'mgtno', width: 140 },
        { title: '은행명', field: 'mgtnm', width: 130 },
        { title: '비고', field: 'remark', width: 100 },
        { title: '은행코드', field: 'bankcd', width: 80 }
      ];
    } else if (imgbn === '300') {
      gbncd = '040'; remark = '1110';
      columns = [
        { title: '카드번호', field: 'mgtno', width: 140 },
        { title: '카드사', field: 'mgtnm', width: 130 },
        { title: '가맹점', field: 'soname', width: 100 },
        { title: '코드', field: 'bankcd', width: 80 }
      ];
    } else if (imgbn === '500' || imgbn === '510') {
      if (!formMst.custcd) return vAlertError('거래처를 먼저 선택하십시오.');
      gubun = 'P1';
      gbncd = (imgbn === '500') ? '2110' : '2125';
      remark = formMst.custcd;
      columns = [
        { title: '발행일', field: 'slipymd', width: 100 },
        { title: '번호', field: 'slipno', width: 80 },
        { title: '순번', field: 'srowno', width: 50 },
        { title: '적요', field: 'remark', width: 150 },
        { title: '거래처', field: 'custnm', width: 120 },
        { title: '잔액', field: 'janamt', width: 100, hozAlign: 'right', formatter: 'money' }
      ];
    } else {
      return vAlertError('관리번호 대상이 아닙니다.');
    }

    Object.assign(modalProps, {
      title: '관리번호 선택', path: '/ha00/HA00_00P_STR',
      defaultField: (gubun === 'P1') ? 'slipno' : 'mgtno',
      data: { gubun, cmpycd: authStore.cmpycd, gbncd, code: '', remark },
      columns: columns,
      onConfirm: (d: any) => {
        targetRow.update({ mgtno: (gubun === 'P1') ? d.slipno : d.mgtno });
      }
    });
    modalVisible.value = true;
  }
}

const addPayRow = () => {
  const diff = totals.setSum - totals.paySum;
  grid2?.addRow({ imamt: diff > 0 ? diff : 0 }, true);
}

const deletePayRow = () => {
  grid2?.getSelectedRows().forEach(r => r.delete());
  calculateTotals();
}

const initialize = () => {
  Object.assign(formMst, { imym: '', imno: '0000', imymd: today, custcd: '', custnm: '', janamt: 0, rcvdd: '', remark: '', actkind: 'A0' });
  grid1?.clearData(); grid2?.clearData();
  totals.janSum = 0; totals.setSum = 0; totals.paySum = 0;
}

const handleDelete = async () => {
  if (!confirm('삭제하시겠습니까?')) return;
  try {
    await api.post('/hsio/HSIO_300U_STR', { ...formMst, actkind: 'D0' });
    vAlert('삭제되었습니다.'); initialize();
  } catch (e) { vAlertError('삭제 실패'); }
}

const formatNumber = (n: any) => Number(n || 0).toLocaleString();

onUnmounted(() => {
  if (grid1) grid1.destroy();
  if (grid2) grid2.destroy();
});

onMounted(() => {
  // 어음종류 코드 로드
  api.post('/ha00/HA00_00P_STR', { gubun: 'E0', cmpycd: authStore.cmpycd, gbncd: '150' }).then(r => {
    billgbnOptions.value = normalize(r.data);
  });
  // 마감 정보 로드
  api.get('/hp00/hp00_000s_str', { params: { gubun: 'CL', cmpycd: authStore.cmpycd } }).then(r => {
    if (r.data?.length) { const n = normalize(r.data)[0]; closingInfo.sclsym = n.sclsym; }
  });
  nextTick(initGrids);
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; font-size: 12px; border: 1px solid #dee2e6; }
.bg-yellow { background-color: #fffde7 !important; }
.erp-table-dense th, .erp-table-dense td { height: 32px !important; padding: 0 8px !important; font-size: 12px; vertical-align: middle; border: 1px solid #dee2e6; }
.erp-table-dense .form-control, .erp-table-dense .btn { height: 26px !important; font-size: 12px !important; }
.cursor-pointer { cursor: pointer; }
.grid-container-standard { border-bottom: 2px solid #005a9f !important; }
</style>
