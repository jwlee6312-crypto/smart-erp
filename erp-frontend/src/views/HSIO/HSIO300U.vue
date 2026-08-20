<!--
	=============================================================
	프로그램명	: 입금입력 (HSIO300U)
	작성일자	: 2025.02.24
	설명        : 영업 입금 관리 (HSOD100U 표준 구조, 입금유형/관리번호 팝업 처리 완벽 구현)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-cash-stack me-2 text-primary" style="font-size: 18px;"></i>
        영업관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        입금관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">입금입력 (HSIO300U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-save" @click="save" :disabled="isClosed">저장</button>
        <button class="btn-erp btn-delete" @click="handleFullDelete" :disabled="!form_02.imno || form_02.imno === '0000' || isClosed">전체삭제</button>
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
                <th class="text-center bg-light">입금일자</th>
                <td class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                  <DateForm v-model:fromdt="form_01.fromdt" v-model:todt="form_01.todt" />
                </td>
                <th class="text-center bg-light">거래처명</th>
                <td>
                  <input v-model="form_01.schcustnm" class="form-control form-control-sm" placeholder="거래처명 검색" @keyup.enter="search" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 투-그리드 레이아웃 -->
      <div class="d-flex gap-2 flex-grow-1 overflow-hidden" style="min-height: 0;">

        <!-- ⬅️ 좌측: 입금 목록 -->
        <div class="card border shadow-sm d-flex flex-column overflow-hidden grid-container-left" style="width: 380px; min-width: 380px;">
          <div class="card-header bg-white py-1 px-3 border-bottom d-flex justify-content-between align-items-center">
            <span class="fw-bold small text-dark">입금 목록</span>
            <span class="badge bg-info text-dark" style="font-size: 10px;">{{ grid1?.getData().length || 0 }} 건</span>
          </div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="tableRef1" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>

        <!-- ➡️ 우측: 마스터 상세 폼 + 상세 내역 그리드 -->
        <div class="flex-grow-1 d-flex flex-column gap-2 overflow-hidden">

          <!-- 마스터 폼 -->
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
                    <th class="required bg-light text-center">입금부서</th>
                    <td>
                      <div class="input-group input-group-sm">
                        <input v-model="form_02.deptnm" class="form-control" readonly />
                        <button class="btn btn-outline-secondary" @click="handleOpenHelp('DEPT')" :disabled="isClosed"><i class="bi bi-search"></i></button>
                      </div>
                    </td>
                    <th class="bg-light text-center">입금번호</th>
                    <td>
                      <div class="d-flex gap-1">
                        <input v-model="form_02.imym" class="form-control bg-light text-center" readonly style="width: 85px;" placeholder="연월" />
                        <input v-model="form_02.imno" class="form-control bg-light text-primary fw-bold text-center" readonly style="width: 65px;" placeholder="번호" />
                      </div>
                    </td>
                    <th class="required bg-light text-center">입금일자</th>
                    <td><input v-model="form_02.imymd" type="date" class="form-control" :readonly="isClosed" /></td>
                  </tr>
                  <tr>
                    <th class="required bg-light text-center">거래처</th>
                    <td>
                      <div class="input-group input-group-sm">
                        <input v-model="form_02.custnm" class="form-control" readonly />
                        <button class="btn btn-outline-secondary" @click="handleOpenHelp('CUST')" :disabled="isClosed"><i class="bi bi-search"></i></button>
                      </div>
                    </td>
                    <th class="bg-light text-center">여신잔액</th>
                    <td><input :value="formatNumber(form_02.janamt)" class="form-control bg-light text-end" readonly /></td>
                    <th class="bg-light text-center">여신기한</th>
                    <td><input v-model="form_02.rcvdd" class="form-control bg-light text-center" readonly style="width: 60px;" /></td>
                  </tr>
                  <tr>
                    <th class="bg-light text-center">적요</th>
                    <td colspan="5"><input v-model="form_02.remark" class="form-control" :readonly="isClosed" /></td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- 상세 그리드 (어음/관리번호 팝업 포함) -->
          <div class="card border shadow-sm flex-grow-1 d-flex flex-column overflow-hidden grid-container-right">
            <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
              <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>입금 상세 내역 (어음포함)</span>
              <div class="d-flex gap-1">
                <button class="btn btn-sm btn-outline-primary py-0 px-2 fw-bold" @click="addRow" :disabled="isClosed" style="font-size: 11px;">+ 행추가</button>
                <button class="btn btn-sm btn-outline-danger py-0 px-2 fw-bold" @click="deleteSelectedRows" :disabled="isClosed" style="font-size: 11px;">- 행삭제</button>
              </div>
            </div>
            <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
              <div ref="tableRef2" class="tabulator-instance flex-grow-1"></div>
            </div>
            <!-- 합계 영역 -->
            <div class="card-footer bg-light p-1 border-top d-flex justify-content-end gap-5 pe-4">
               <div class="small fw-bold">입금액 합계: <span class="text-primary fw-bold">{{ formatNumber(amtTot) }}</span></div>
               <div class="small fw-bold">어음 합계: <span class="text-danger fw-bold">{{ formatNumber(billTot) }}</span></div>
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
import { getDate } from '@/composables/useDate'

const authStore = useAuthStore()
const { firstDay, today } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

// [로컬 상태 정의] useCommonHelp 대신 컴포넌트 내부에 직접 정의
const modalVisible = ref(false)
const modalProps = reactive<any>({
  title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table'
})

const form_01 = reactive({ fromdt: firstDay, todt: today, schcustnm: '' })
const form_02 = reactive<any>({
  deptcd: authStore.deptcd, deptnm: authStore.deptnm, imno: '0000', imym: '', imymd: today,
  custcd: '', custnm: '', janamt: 0, rcvdd: '', remark: '', actkind: 'A0',
  hanamt: 0, handd: ''
})

const closingInfo = reactive({ sclsym: '' })
const billgbnOptions = ref<any[]>([
  { codecd: '010', codenm: '자수어음' },
  { codecd: '020', codenm: '타수어음' }
])
const amtTot = ref(0); const billTot = ref(0)

/** 💡 데이터 정규화 (고정 길이 7자리 및 3자리 엄격 보장) */
const normalize = (data: any[]) => {
  if (!data || !Array.isArray(data)) return [];
  return data.map(item => {
    const obj: any = {};
    for (const key in item) {
      const val = item[key];
      const k = key.toLowerCase();
      if (['dacctcd', 'cacctcd'].includes(k)) {
        obj[k] = String(val || '').trim().padEnd(7, ' ');
      } else if (['imgbn', 'imtype', 'billgbn'].includes(k)) {
        obj[k] = String(val || '').trim();
      } else if (typeof val === 'string') {
        obj[k] = val.trim();
      } else {
        obj[k] = val;
      }
    }
    return obj;
  });
}

const tableRef1 = ref<HTMLDivElement | null>(null)
const tableRef2 = ref<HTMLDivElement | null>(null)
let grid1: Tabulator | null = null
let grid2: Tabulator | null = null

const isClosed = computed(() => {
  if (!closingInfo.sclsym || !form_02.imymd) return false
  return form_02.imymd.replace(/-/g, '').substring(0, 6) <= closingInfo.sclsym
})

const initGrids = () => {
  // [좌측 그리드] 입금 목록
  grid1 = new Tabulator(tableRef1.value!, {
    layout: "fitColumns", height: "100%", placeholder: "데이터 없음",
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "No", formatter: "rownum", width: 40, hozAlign: "center" },
      { title: "거래처명", field: "custnm", hozAlign: "left", minWidth: 150 },
      { title: "입금일자", field: "imymd", hozAlign: "center", width: 100, formatter: (c) => formatDate(c.getValue()) },
      { title: "입금번호", field: "imno", hozAlign: "center", width: 90, cssClass: "fw-bold text-primary" }
    ],
  });
  grid1.on("rowClick", (e, row) => fetchDetail(row.getData()));

  // [우측 그리드] 상세 내역
  grid2 = new Tabulator(tableRef2.value!, {
    layout: "fitColumns", height: "100%", placeholder: "상세 내역 없음", selectable: true,
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "선택", width: 40, hozAlign: "center", formatter: "rowSelection", titleFormatter: "rowSelection" },
      { title: "상태", field: "_status", width: 60, hozAlign: "center",
        formatter: (c) => {
          const v = c.getValue();
          if (v === '입력') return '<span class="badge bg-primary">신규</span>';
          if (v === '수정') return '<span class="badge bg-warning text-dark">수정</span>';
          if (v === '삭제') return '<span class="badge bg-danger">삭제</span>';
          return '';
        }
      },
      { title: "입금유형", field: "imtypenm", minWidth: 200, widthGrow: 1,
        cssClass: "text-primary fw-bold cursor-pointer",
        cellClick: (e, cell) => handleOpenHelp('IMTYPE', cell.getRow()),
        formatter: (c) => c.getValue() || '<span class="text-muted small">선택(클릭)</span>'
      },
      { title: "입금액", field: "imamt", width: 110, hozAlign: "right", editor: "number", formatter: "money", formatterParams: { precision: 0 }, cellEdited: () => calcTotals() },
      { title: "관리번호", field: "mgtno", width: 150,
        cssClass: "text-primary cursor-pointer",
        cellClick: (e, cell) => handleOpenHelp('MGT', cell.getRow()),
        formatter: (cell) => {
            const v = cell.getValue() || "";
            const d = cell.getRow().getData();
            if (['200', '210', '300', '500', '510', '600'].includes(String(d.imtype || '').trim())) {
                return `<div class="d-flex justify-content-between"><span>${v}</span><i class="bi bi-search text-primary small"></i></div>`;
            }
            return v;
        }
      },
      { title: "어음액면가", field: "billamt", width: 110, hozAlign: "right", editor: "number", formatter: "money", formatterParams: { precision: 0 }, cellEdited: () => calcTotals() },
      { title: "어음종류", field: "billgbn", width: 100,
        cssClass: "text-primary cursor-pointer",
        cellClick: (e, cell) => handleOpenHelp('BILLGBN', cell.getRow()),
        formatter: (c) => {
          const val = String(c.getValue() || '').trim();
          if (!val || val === '000' || val === '0' || val === 'null') return '<span class="text-muted small">선택(클릭)</span>';
          const o = billgbnOptions.value.find(x => String(x.codecd).trim() === val);
          return o ? o.codenm : val;
        }
      },
      { title: "발행일", field: "pubymd", width: 110, editor: "input", cellEdited: (cell) => updateRowStatus(cell.getRow()) },
      { title: "발행인", field: "pubman", width: 110, editor: "input", cellEdited: (cell) => updateRowStatus(cell.getRow()) },
      { title: "만기일", field: "endymd", width: 110, hozAlign: "center", editor: "input", cellEdited: (cell) => updateRowStatus(cell.getRow()) },
      { title: "발행은행", field: "pubbank", width: 120, editor: "input", cellEdited: (cell) => updateRowStatus(cell.getRow()) },
      { title: "삭제", width: 40, hozAlign: "center", formatter: () => "<i class='bi bi-trash text-danger cursor-pointer'></i>",
        cellClick: (e, cell) => handleRowAction(cell.getRow())
      }
    ]
  });
}

const updateRowStatus = (row: any) => {
  const d = row.getData();
  if (d._state === 'EXIST' && d._status !== '삭제') row.update({ _status: '수정' });
  calcTotals();
}

const calcTotals = () => {
  const data = grid2?.getData() || [];
  amtTot.value = data.filter(d => d._status !== '삭제').reduce((acc, cur:any) => acc + (Number(cur.imamt) || 0), 0);
  billTot.value = data.filter(d => d._status !== '삭제').reduce((acc, cur:any) => acc + (Number(cur.billamt) || 0), 0);
}

/** 🚀 로컬 도움창 핸들러 */
const handleOpenHelp = (type: string, targetRow?: any) => {
  if (isClosed.value) return;

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
      onConfirm: (d: any) => {
        form_02.custcd = d.custcd; form_02.custnm = d.custnm;
        // 💡 HS00_150S_STR 사용하여 여신 정보 조회 (RCVDD, HANAMT, JANAMT)
        api.post('/hs00/HS00_150S_STR', { cmpycd: authStore.cmpycd, custnm: d.custnm }).then(r => {
          if(r.data?.length) {
            const n = normalize(r.data)[0];
            form_02.rcvdd = n.rcvdd;   // 여신기한 (Rs(3))
            form_02.hanamt = n.hanamt; // 여신한도 (Rs(4))
            form_02.janamt = n.janamt; // 여신잔액 (Rs(5))
          }
        });
      }
    });
    modalVisible.value = true;
  } else if (type === 'IMTYPE') {
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
          imtype: res.imgbn, // 💡 imgbn 대신 imtype으로 저장 (백엔드 매핑)
          dacctcd: res.dacctcd, cacctcd: res.cacctcd,
          imtypenm: res.imgbnnm, mgtno: ''
        });
        updateRowStatus(targetRow);
      }
    });
    modalVisible.value = true;
  } else if (type === 'MGT') {
    const rowData = normalize([targetRow.getData()])[0];
    const imtype = String(rowData.imtype || '').trim(); // 💡 imgbn -> imtype
    if (imtype === '400') return vAlertError('어음번호는 직접 입력하십시오.');

    let gubun = 'M0'; let gbncd = '010'; let remark = rowData.dacctcd;
    let columns: any[] = [{ title: '번호/코드', field: 'mgtno', width: 150 }, { title: '명칭', field: 'mgtnm', width: 200 }];

    if (imtype === '200' || imtype === '600' || imtype === '210') {
      // 💡 예금 등: 구좌번호 팝업 (mgtno, mgtnm, remark, bankcd)
      remark = (imtype === '210') ? '1145' : '1120';
      columns = [
        { title: '계좌번호', field: 'mgtno', width: 140 },
        { title: '은행명', field: 'mgtnm', width: 130 },
        { title: '비고', field: 'remark', width: 100 },
        { title: '은행코드', field: 'bankcd', width: 80 }
      ];
    } else if (imtype === '300') {
      // 💡 카드결제: 카드번호 팝업 (mgtno, mgtnm, soname, bankcd)
      gbncd = '040'; remark = '1110';
      columns = [
        { title: '카드번호', field: 'mgtno', width: 140 },
        { title: '카드사', field: 'mgtnm', width: 130 },
        { title: '가맹점', field: 'soname', width: 100 },
        { title: '코드', field: 'bankcd', width: 80 }
      ];
    } else if (imtype === '500' || imtype === '510') {
      // 💡 상계대체: 미처리전표 조회 (P1)
      if (!form_02.custcd) return vAlertError('거래처를 먼저 선택하십시오.');
      gubun = 'P1';
      gbncd = (imtype === '500') ? '2110' : '2125';
      remark = form_02.custcd;
      columns = [
        { title: '발행일', field: 'slipymd', width: 100 },
        { title: '번호', field: 'slipno', width: 80 },
        { title: '순번', field: 'srowno', width: 50 },
        { title: '적요', field: 'remark', width: 150 },
        { title: '거래처', field: 'custnm', width: 120 },
        { title: '잔액', field: 'janamt', width: 100, hozAlign: 'right', formatter: 'money' }
      ];
    }

    Object.assign(modalProps, {
      title: '관리번호 선택', path: '/ha00/HA00_00P_STR',
      defaultField: (gubun === 'P1') ? 'slipno' : 'mgtno',
      data: { gubun, cmpycd: authStore.cmpycd, gbncd, code: '', remark },
      columns: columns,
      onConfirm: (d: any) => {
        targetRow.update({ mgtno: (gubun === 'P1') ? d.slipno : d.mgtno });
        updateRowStatus(targetRow);
      }
    });
    modalVisible.value = true;
  } else if (type === 'BILLGBN') {
    Object.assign(modalProps, {
      title: '어음종류 선택', path: '/ha00/HA00_00P_STR', defaultField: 'codenm',
      data: { gubun: 'E0', cmpycd: authStore.cmpycd, gbncd: '150', code: '', remark: '' },
      columns: [{ title: '코드', field: 'codecd', width: 80, hozAlign: 'center' }, { title: '명칭', field: 'codenm', width: 200 }],
      onConfirm: (d: any) => {
        targetRow.update({ billgbn: String(d.codecd).trim() });
        updateRowStatus(targetRow);
      }
    });
    modalVisible.value = true;
  }
}

const handleRowAction = (row: any) => {
  const d = row.getData();
  if (d._state === 'NEW') row.delete();
  else row.update({ _status: d._status === '삭제' ? '' : '삭제' });
  calcTotals();
}

async function search() {
  try {
    const res = await api.post('/hsio/HSIO_300U_STR',
    {   actkind: 'L0',
        fromdt: form_01.fromdt.replace(/-/g, ''),
        todt: form_01.todt.replace(/-/g, ''),
        custnm: form_01.schcustnm
    });
    grid1?.setData(normalize(res.data)); vAlert('조회되었습니다.');
  } catch (e) { vAlertError('조회 실패'); }
}

async function fetchDetail(row: any) {
  Object.assign(form_02, row);
  form_02.imymd = formatDate(row.imymd);
  try {
    const res = await api.post('/hsio/HSIO_300U_STR', { actkind: 'S1', imym: row.imym, imno: row.imno }));
    const details = normalize(res.data).map(i => ({
      ...i,
      imtype: (i.imtype || i.imgbn || '').trim(),
      imtypenm: (i.imtypenm || i.imgbnnm || '').trim(),
      _state: 'EXIST',
      _status: ''
    }));
    grid2?.setData(details);
    nextTick(() => {
        calcTotals();
        grid2?.redraw();
    });
  } catch (e) { vAlertError('상세 로드 실패'); }
}

async function save() {
  if (!form_02.custcd) return vAlertError('거래처를 선택하세요.');
  const details = grid2?.getData().filter((r: any) => r._status) || [];
  if (!details.length && (!form_02.imno || form_02.imno === '0000')) return vAlertError('저장할 내역이 없습니다.');

  // 💡 [데이터 정규화] XML 매퍼 요구 파라미터 25개 필드 정의 및 NULL 방지
  const fields = [
    'actkind','cmpycd','fromdt','todt','custnm','imym','imno','imrowno','deptcd','custcd',
    'imymd','remark','imtype','imamt','mgtno','pubymd','endymd','billgbn','billamt',
    'pubbank','pubman','slipyn','slipymd','slipno','updemp'
  ];

  const prepare = (source: any) => {
    const obj: any = {};
    fields.forEach(f => {
      let val = source[f];
      if (val === null || val === undefined) val = "";

      // 1. 날짜 하이픈 제거 처리
      if (['imymd', 'pubymd', 'endymd', 'slipymd', 'fromdt', 'todt'].includes(f)) {
        val = String(val).replace(/-/g, '');
      }
      // 2. 금액 및 숫자형 필드의 콤마 제거 처리
      if (['imamt', 'billamt', 'hanamt', 'janamt'].includes(f)) {
        val = String(val).replace(/,/g, '');
      }

      obj[f] = String(val); // 💡 모든 값을 최종적으로 문자열로 변환
    });
    return obj;
  };

  try {
    const payload = {
      mst: prepare({
        ...form_02,
        actkind: (!form_02.imno || form_02.imno === '0000') ? 'A0' : 'U0',
        cmpycd: authStore.cmpycd,
        updemp: authStore.userid
      }),
      dtl: details.map((d: any) => prepare({
        ...d,
        actkind: d._status === '입력' ? 'A1' : (d._status === '삭제' ? 'D1' : 'U1'),
        cmpycd: authStore.cmpycd,
        imym: form_02.imym,
        imno: form_02.imno,
        deptcd: form_02.deptcd,
        custcd: form_02.custcd,
        imymd: form_02.imymd,
        updemp: authStore.userid
      }))
    };
    await api.post('/hsio/HSIO_300U_SAVE', payload);
    vAlert('저장되었습니다.'); search();
  } catch (e) { vAlertError('저장 실패'); }
}

const addRow = () => grid2?.addRow({ _status: '입력', _state: 'NEW', imamt: 0, billamt: 0, billgbn: '000' }, true);
const deleteSelectedRows = () => grid2?.getSelectedRows().forEach(r => handleRowAction(r));

function initialize() {
  resetForm(form_02);
  Object.assign(form_02, {
    deptcd: authStore.deptcd, deptnm: authStore.deptnm, imno: '0000', imym: '', imymd: today,
    custcd: '', custnm: '', janamt: 0, rcvdd: '', remark: '', actkind: 'A0'
  });
  grid1?.clearData(); grid2?.clearData(); amtTot.value = 0; billTot.value = 0;
}

async function handleFullDelete() {
  if (!confirm('정말 삭제하시겠습니까?')) return;
  try {
    await api.post('/hsio/HSIO_300U_STR', { ...form_02, actkind: 'D0' });
    vAlert('삭제되었습니다.'); initialize(); search();
  } catch (e) { vAlertError('삭제 실패'); }
}

const formatNumber = (n: any) => Number(n || 0).toLocaleString();
const formatDate = (d: any) => (d && d.length === 8) ? `${d.substring(0,4)}-${d.substring(4,6)}-${d.substring(6,8)}` : d;

onUnmounted(() => {
  if (grid1) grid1.destroy();
  if (grid2) grid2.destroy();
});

onMounted(() => {
  // 어음종류 코드 로드 (강제 테스트 데이터 이식)
  api.post('/ha00/HA00_00P_STR', { gubun: 'E0', cmpycd: authStore.cmpycd, gbncd: '150' }).then(r => {
    const data = normalize(r.data);
    const testData = [
      { codecd: '010', codenm: '자수어음' },
      { codecd: '020', codenm: '타수어음' }
    ];
    const combined = [...testData];
    data.forEach(d => {
        if(!combined.find(c => String(c.codecd).trim() === String(d.codecd).trim())) combined.push(d);
    });
    billgbnOptions.value = combined;
    nextTick(() => { if(grid2) grid2.redraw(); });
  });
  // 마감 정보 로드
  api.get('/hp00/hp00_000s_str', { params: { gubun: 'CL', cmpycd: authStore.cmpycd } }).then(r => {
    if (r.data?.length) { const n = normalize(r.data)[0]; closingInfo.sclsym = n.sclsym; }
  });
  nextTick(initGrids);
  initialize();
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; font-size: 12px; border: 1px solid #dee2e6; }
.cursor-pointer { cursor: pointer; }
.grid-container-left, .grid-container-right { border-bottom: 3px solid #005a9f !important; }
.erp-table-dense th, .erp-table-dense td { height: 32px !important; padding: 0 8px !important; font-size: 12px; vertical-align: middle; border: 1px solid #dee2e6; }
.erp-table-dense .form-control, .erp-table-dense .form-select, .erp-table-dense .btn { height: 26px !important; font-size: 12px !important; border-radius: 2px; }
</style>
