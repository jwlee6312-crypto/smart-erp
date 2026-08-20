<!--
	=============================================================
	프로그램명	: 입고요청등록 (HSIO010U)
	작성일자	: 2025.02.24
	설명        : 소문자 원칙 및 인터셉터 표준화 적용 (오리지널 명칭 사용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom" style="height: 48px !important;">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-cart-check-fill me-2 text-primary" style="font-size: 18px;"></i>
        구매관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        구매요청 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">구매요청 (HSIO010U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-2">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-save" @click="save" :disabled="form_02.sts === 'Y' || isClosed">저장</button>
        <button class="btn-erp btn-delete" @click="handleFullDelete" :disabled="!form_02.reqno || form_02.reqno === '0000' || form_02.sts === 'Y' || isClosed">전체삭제</button>
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
                <th class="text-center bg-light">요청일자</th>
                <td class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                  <DateForm v-model:fromdt="form_01.fromdt" v-model:todt="form_01.todt" />
                </td>
                <th class="text-center bg-light">부서명</th>
                <td>
                  <input v-model="form_01.deptnm" class="form-control form-control-sm" placeholder="검색어 입력" @keyup.enter="search" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="d-flex gap-2 flex-grow-1 overflow-hidden" style="min-height: 0;">
        <!-- 좌측 그리드 -->
        <div class="card border shadow-sm d-flex flex-column overflow-hidden grid-container-left" style="width: 350px; min-width: 350px;">
          <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark">요청 목록</div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="tableRef1" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>

        <div class="flex-grow-1 d-flex flex-column gap-2 overflow-hidden">
          <!-- 마스터 폼 -->
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
                    <th class="required bg-light">요청부서</th>
                    <td colspan="3">
                      <div class="input-group input-group-sm">
                        <input v-model="form_02.deptnm" class="form-control" readonly />
                        <button class="btn btn-outline-secondary" @click="handleOpenHelp('DEPT')"><i class="bi bi-search"></i></button>
                      </div>
                    </td>
                    <th class="bg-light text-center">요청번호</th>
                    <td><input v-model="form_02.reqno" class="form-control bg-light text-primary fw-bold text-center" readonly placeholder="0000" /></td>
                    <th class="required bg-light text-center">요청일자</th>
                    <td><input v-model="form_02.reqymd" type="date" class="form-control" :readonly="form_02.sts === 'Y' || isClosed" /></td>
                  </tr>
                  <tr>
                    <th class="bg-light text-center">특이사항</th>
                    <td colspan="7"><input v-model="form_02.remark" class="form-control" :readonly="form_02.sts === 'Y' || isClosed" /></td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- 품목 그리드 -->
          <div class="card border shadow-sm flex-grow-1 d-flex flex-column overflow-hidden grid-container-right">
            <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
              <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>요청 품목 리스트</span>
              <div class="d-flex gap-1">
                <button class="btn btn-sm btn-outline-primary py-0 px-2 fw-bold" @click="addRow" :disabled="form_02.sts === 'Y' || isClosed" style="font-size: 11px;">+ 행추가</button>
                <button class="btn btn-sm btn-outline-danger py-0 px-2 fw-bold" @click="deleteSelectedRows" :disabled="form_02.sts === 'Y' || isClosed" style="font-size: 11px;">- 행삭제</button>
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
import { useCommonHelp } from '@/composables/useCommonHelp'
import { getDate } from '@/composables/useDate'

const authStore = useAuthStore()
const { firstDay, today } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps, openHelp } = useCommonHelp()


const form_01 = reactive({ fromdt: firstDay, todt: today, deptnm: '' })
const form_02 = reactive<any>({
  actkind: 'S', cmpycd: authStore.cmpycd, fromdt: firstDay, todt: today, reqym: today.substring(0, 7).replace('-', ''), reqno: '0000',
  deptcd: authStore.deptcd, deptnm: authStore.deptnm, reqymd: today, req_userid: authStore.userid,
  inymd: today, remark: '', sts: 'N', totsum: 0
})

const closingInfo = reactive({ sclsym: '' })
const tableRef1 = ref<HTMLDivElement | null>(null); const tableRef2 = ref<HTMLDivElement | null>(null)
let grid1: Tabulator | null = null; let grid2: Tabulator | null = null

const isClosed = computed(() => {
  if (!closingInfo.sclsym || !form_02.reqymd) return false
  return form_02.reqymd.replace(/-/g, '').substring(0, 6) <= closingInfo.sclsym
})

const initGrids = () => {
  grid1 = new Tabulator(tableRef1.value!, {
    layout: "fitColumns", height: "100%", placeholder: "데이터 없음",
    columns: [
      { title: "No", formatter: "rownum", width: 40, hozAlign: "center", headerSort: false },
      { title: "요청부서", field: "deptnm", hozAlign: "left", headerSort: false },
      { title: "요청번호", field: "reqno_full", hozAlign: "center", width: 120, cssClass: "fw-bold text-primary", headerSort: false,
        formatter: (c) => { const d = c.getData(); return `${d.reqym}-${d.reqno}`; }
      }
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
      { title: "품목명", field: "itemnm", minWidth: 200, widthGrow: 1, cssClass: 'fw-bold text-primary', cellClick: (e, cell) => handleOpenHelp('ITEM', cell.getRow()) },
      { title: "단위", field: "unit", width: 70, hozAlign: "center" },
      { title: "수량", field: "reqqty", width: 100, hozAlign: "right", editor: "number", cellEdited: (cell) => calcRow(cell.getRow()) },
      { title: "단가", field: "imprice", width: 110, hozAlign: "right", editor: "number", cellEdited: (cell) => calcRow(cell.getRow()) },
      { title: "금액", field: "reqamt", width: 120, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "삭제", width: 40, hozAlign: "center", formatter: (c) => "<i class='bi bi-trash text-danger'></i>", cellClick: (e, cell) => handleRowAction(cell.getRow()) }
    ]
  });
}

const calcRow = (row: any) => {
  const d = row.getData(); const amt = Math.floor(Number(d.reqqty || 0) * Number(d.imprice || 0));
  row.update({ reqamt: amt });
  if (d._state === 'EXIST' && d._status !== '삭제') row.update({ _status: '수정' });
}

async function search() {
  try {
    const res = await api.post('/hsio/HSIO_010U_STR', { actkind: 'L', cmpycd: authStore.cmpycd, fromdt: form_01.fromdt.replace(/-/g, ''), todt: form_01.todt.replace(/-/g, ''), deptnm: form_01.deptnm });
    grid1?.setData(res.data); vAlert('조회되었습니다.');
  } catch (e: any) { vAlertError('조회 실패'); }
}

async function fetchDetail(row: any) {
  const fYmd = (d: string) => d && d.length === 8 ? `${d.substring(0, 4)}-${d.substring(4, 6)}-${d.substring(6, 8)}` : today;
  Object.assign(form_02, { ...row, reqymd: fYmd(row.reqymd) });
  try {
    const res = await api.post('/hsio/HSIO_011U_STR', { actkind: 'S', cmpycd: authStore.cmpycd, reqym: row.reqym, reqno: row.reqno });
    grid2?.setData(res.data.map((i: any) => ({ ...i, _state: 'EXIST', _status: '' })));
  } catch (e: any) { vAlertError('상세 로드 실패'); }
}

async function save() {
  if (!form_02.deptcd) return vAlertError('요청부서를 선택하세요.');
  const details = grid2?.getData().filter((r: any) => r._status) || [];
  if (!details.length && (!form_02.reqno || form_02.reqno === '0000')) return vAlertError('저장할 품목이 없습니다.');

  try {
    const payload = {
      mst: {
        ...form_02,
        asgbn: 'N',
        reqymd: form_02.reqymd.replace(/-/g, ''),
        actkind: (!form_02.reqno || form_02.reqno === '0000') ? 'A0' : 'U0',
        updemp: authStore.userid
      },
      dtl: details.map((d: any) => ({
        ...d,
        actkind: d._status === '입력' ? 'A1' : (d._status === '삭제' ? 'D1' : 'U1'),
        updemp: authStore.userid
      }))
    };
    await api.post('/hsio/HSIO_010U_SAVE', payload);
    vAlert('저장되었습니다.'); initialize(); search();
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
  } else if (type === 'ITEM') {
    Object.assign(modalProps, {
      title: '품목 선택', path: '/hs00/HS00_000S_STR', defaultField: 'itemnm',
      data: { gubun: 'I1', cmpycd: authStore.cmpycd, gbncd: '3', code: '', remark: '' },
      columns: [{ title: '품목코드', field: 'itemcd', width: 100, hozAlign: 'center' }, { title: '품목명', field: 'itemnm', width: 200 }, { title: '규격', field: 'itsize', width: 150 }, { title: '단위', field: 'unit', width: 80, hozAlign: 'center' }],
      onConfirm: (d: any) => {
        target.update({ itemcd: d.itemcd, itemnm: d.itemnm, itsize: d.itsize, unit: d.unit, imprice: d.incost || 0, reqqty: 1, reqamt: d.incost || 0, _status: '입력', _state: 'NEW' });
        calcRow(target);
      }
    });
    modalVisible.value = true;
  }
}

const handleRowAction = (row: any) => { const d = row.getData(); if (d._state === 'NEW') row.delete(); else row.update({ _status: d._status === '삭제' ? '' : '삭제' }); }
const deleteSelectedRows = () => { const sel = grid2?.getSelectedRows(); if (sel?.length) sel.forEach(row => handleRowAction(row)); }
const addRow = () => grid2?.addRow({ reqqty: 0, imprice: 0, reqamt: 0, _status: '입력', _state: 'NEW' }, true);
const initialize = () => { resetForm(form_02); form_02.reqno = '0000'; form_02.reqymd = today; form_02.sts = 'N'; form_02.deptcd = authStore.deptcd; form_02.deptnm = authStore.deptnm; grid1?.clearData(); grid2?.clearData(); }

async function handleFullDelete() {
  if (!confirm('정말 전체 삭제하시겠습니까?')) return;
  try {
    await api.post('/hsio/HSIO_010U_STR', { ...form_02, actkind: 'D0' });
    vAlert('삭제되었습니다.'); initialize(); search();
  } catch (e) { vAlertError('삭제 실패'); }
}

onUnmounted(() => {
  if (grid1) grid1.destroy();
  if (grid2) grid2.destroy();
});

onMounted(async () => {
    nextTick(initGrids);
    api.get('/hp00/hp00_000s_str', { params: { gubun: 'CL', cmpycd: authStore.cmpycd } }).then(r => { if(r.data?.length) closingInfo.sclsym = r.data[0].sclsym; });
    initialize();
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
