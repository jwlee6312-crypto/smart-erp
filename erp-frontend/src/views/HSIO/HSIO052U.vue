<!--
	=============================================================
	프로그램명	: 일반발주등록 (HSIO052U)
	작성일자	: 2025.02.24
	설명        : 일반발주 마스터/상세 관리 (HSOD100U 표준화 규칙 적용)
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
        <span class="text-primary fw-bolder">일반발주등록 (HSIO052U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-save" @click="save" :disabled="isClosed">저장</button>
        <button class="btn-erp btn-delete" @click="handleFullDelete" :disabled="!form_02.balno || form_02.balno === '0000' || isClosed">전체삭제</button>
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

      <!-- [하단] 투-그리드 레이아웃 영역 -->
      <div class="d-flex gap-2 flex-grow-1 overflow-hidden" style="min-height: 0;">
        <!-- ⬅️ 좌측: 발주 목록 -->
        <div class="card border shadow-sm d-flex flex-column overflow-hidden grid-container-left" style="width: 350px; min-width: 350px;">
          <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark">발주 목록</div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="tableRef1" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>

        <!-- ➡️ 우측: 마스터 상세 폼 + 품목 상세 그리드 -->
        <div class="flex-grow-1 d-flex flex-column gap-2 overflow-hidden">
          <!-- 상세 마스터 정보 폼 -->
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
                      <input :value="displayIoNo" class="form-control bg-light text-primary fw-bold text-center" readonly placeholder="자동생성" />
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
                    <th class="bg-light">이메일</th>
                    <td><input v-model="form_02.email" class="form-control" :readonly="isClosed" /></td>
                    <th class="required bg-light">입고일자</th>
                    <td><input v-model="form_02.reqymd" type="date" class="form-control" :readonly="isClosed" /></td>
                  </tr>
                  <tr>
                    <th class="bg-light">특기사항</th>
                    <td colspan="5"><input v-model="form_02.remark" class="form-control" :readonly="isClosed" /></td>
                    <th class="required bg-light">담당자</th>
                    <td>
                      <select v-model="form_02.bal_userid" class="form-select" :disabled="isClosed">
                        <option v-for="item in userData" :key="item.userid" :value="item.userid">{{ item.usernm }}</option>
                      </select>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- 상세 품목 그리드 영역 -->
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
  custcd: '', custnm: '', bal_userid: authStore.userid,
  remark: '', flag: 'N', email: '', totsum: 0, balgb: '2'
})

const displayIoNo = computed(() => (!form_02.balno || form_02.balno === '0000') ? '' : `${form_02.balym}-${form_02.balno}`)
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
      { title: "품목코드", field: "itemcd", width: 100, hozAlign: "center", cellClick: (e, cell) => handleOpenHelp('ITEM', cell.getRow()) },
      { title: "품목명", field: "itemnm", minWidth: 200, widthGrow: 1, cssClass: 'fw-bold text-primary', cellClick: (e, cell) => handleOpenHelp('ITEM', cell.getRow()) },
      { title: "규격", field: "itsize", width: 120 }, { title: "단위", field: "unit", width: 70, hozAlign: "center" },
      { title: "수량", field: "balqty", width: 100, hozAlign: "right", editor: "number", cellEdited: (cell) => calcRow(cell.getRow()) },
      { title: "단가", field: "price", width: 110, hozAlign: "right", editor: "number", cellEdited: (cell) => calcRow(cell.getRow()) },
      { title: "금액", field: "balamt", width: 120, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "매출처", field: "scustnm", width: 120, cellClick: (e, cell) => handleOpenHelp('GRID_CUST', cell.getRow()) },
      { title: "삭제", width: 40, hozAlign: "center", formatter: (c) => "<i class='bi bi-trash text-danger'></i>", cellClick: (e, cell) => handleRowAction(cell.getRow()) }
    ]
  });
}

const calcRow = (row: any) => {
  const d = row.getData(); const amt = Math.floor(Number(d.balqty || 0) * Number(d.price || 0));
  row.update({ balamt: amt, balvat: Math.floor(amt * 0.1), sumamt: amt + Math.floor(amt * 0.1) });
  if (d._state === 'EXIST' && d._status !== '삭제') row.update({ _status: '수정' });
}

async function search() {
  try {
    const res = await api.post('/hsio/HSIO_052U_STR', {
      actkind: 'L',
      cmpycd: authStore.cmpycd,
      fromdt: form_01.fromdt.replace(/-/g, ''),
      todt: form_01.todt.replace(/-/g, ''),
      gubun: '2',
      custnm: form_01.custnm,
      balym: '', balno: '', balymd: '', reqymd: '', custcd: '', deptcd: '', remark: '', updemp: authStore.userid
    });
    // Normalize keys to lowercase and add balno_full
    const normalizedData = res.data.map((item: any) => {
        const balym = (item.balym || '');
        const balno = (item.balno || '');
        return { ...item, balym, balno, balno_full: `${balym}-${balno}` };
    });
    grid1?.setData(normalizedData);
    vAlert('조회되었습니다.');
  } catch (e) { vAlertError('조회 실패'); }
}

async function fetchDetail(row: any) {
  const fYmd = (d: string) => d && d.length === 8 ? `${d.substring(0, 4)}-${d.substring(4, 6)}-${d.substring(6, 8)}` : today;
  try {
    const mstRes = await api.post('/hsio/HSIO_052U_STR', {
      actkind: 'S', cmpycd: authStore.cmpycd, balym: row.balym, balno: row.balno,
      fromdt: '', todt: '', custnm: '', balymd: '', reqymd: '', custcd: '', deptcd: '', remark: '', updemp: authStore.userid
    });
    if (mstRes.data?.length) {
        const mst = mstRes.data[0];
        Object.assign(form_02, { ...mst, balymd: fYmd(mst.balymd), reqymd: fYmd(mst.reqymd) });
    }
    const res = await api.post('/hsio/HSIO_051U_STR', {
      actkind: 'S', cmpycd: authStore.cmpycd, balym: row.balym, balno: row.balno,
      browno: '', deptcd: '', custcd: '', itemcd: '', itsize: '', unit: '', pkunit: '', balymd: '', balqty: 0, balamt: 0, balvat: 0, scustcd: '', scustnm: '', reqym: '', reqno: '', rrowno: '', updemp: authStore.userid, ordym: '', ordno: ''
    });
    grid2?.setData(res.data.map((i: any) => ({ ...i, _state: 'EXIST', _status: '' })));
  } catch (e) { vAlertError('데이터 로드 실패'); }
}

async function save() {
  if (isClosed.value) return vAlertError('마감된 월입니다.');
  if (!form_02.custcd) return vAlertError('거래처를 선택하세요.');
  const details = grid2?.getData().filter((r: any) => r._status) || [];
  if (!details.length && form_02.balno === '0000') return vAlertError('항목을 추가하세요.');
  try {
    const mst = {
        ...form_02,
        actkind: form_02.balno === '0000' ? 'A' : 'U',
        gubun: '2',
        deptcd: form_02.deptcd,
        custcd: form_02.custcd,
        balymd: form_02.balymd.replace(/-/g, ''),
        reqymd: form_02.reqymd.replace(/-/g, ''),
        updemp: authStore.userid
    };
    const dtl = details.map((d: any) => ({ ...d, cmpycd: authStore.cmpycd, actkind: d._status === '입력' ? 'A' : (d._status === '삭제' ? 'D' : 'U'), updemp: authStore.userid }));
    await api.post('/hsio/HSIO_052U_SAVE', { mst, dtl });
    vAlert('저장되었습니다.'); search();
  } catch (e) { vAlertError('저장 실패'); }
}

const handleOpenHelp = (type: string, target?: any) => {
  if (type === 'DEPT') {
    Object.assign(modalProps, {
    title: '부서 선택', path: '/ha00/HA00_00P_STR',
        defaultField: 'deptnm',
        data: {
        gubun: 'D0',
        cmpycd: authStore.cmpycd,
        gbncd: '',
        code: '',
        remark: ''
    },
    columns: [{ title: '코드', field: 'deptcd', width: 80, hozAlign: 'center' },
        {   title: '부서명', field: 'deptnm', width: 200 }],
            onConfirm: (d: any) => { form_02.deptcd = d.deptcd;
            form_02.deptnm = d.deptnm;
        }
    });
    modalVisible.value = true;
  } else if (type === 'CUST') {
    Object.assign(modalProps, { title: '거래처 선택', path: '/ha00/HA00_00P_STR', defaultField: 'custnm', data: { gubun: 'C4', cmpycd: authStore.cmpycd, gbncd: '', code: '', remark: '' }, columns: [{ title: '코드', field: 'custcd', width: 80, hozAlign: 'center' }, { title: '거래처명', field: 'custnm', width: 200 }], onConfirm: (d: any) => { form_02.custcd = d.custcd; form_02.custnm = d.custnm; form_02.email = d.email || ''; } });
    modalVisible.value = true;
  } else if (type === 'ITEM') {
    Object.assign(modalProps, { title: '품목 선택', path: '/hs00/HS00_000S_STR', defaultField: 'itemnm', data: { gubun: 'I1', cmpycd: authStore.cmpycd, gbncd: '3', code: '', remark: '' }, columns: [{ title: '품목코드', field: 'itemcd', width: 100, hozAlign: 'center' }, { title: '품목명', field: 'itemnm', width: 200 }, { title: '규격', field: 'itsize', width: 150 }, { title: '단위', field: 'unit', width: 80, hozAlign: 'center' }], onConfirm: (d: any) => { target.update({ itemcd: d.itemcd, itemnm: d.itemnm, itsize: d.itsize, unit: d.unit, price: d.incost || 0, balqty: 1, balamt: d.incost || 0, _status: '입력', _state: 'NEW' }); calcRow(target); } });
    modalVisible.value = true;
  } else if (type === 'GRID_CUST') {
    Object.assign(modalProps, { title: '매출처 선택', path: '/ha00/HA00_00P_STR', defaultField: 'custnm', data: { gubun: 'C4', cmpycd: authStore.cmpycd, gbncd: '', code: '', remark: '' }, columns: [{ title: '코드', field: 'custcd', width: 80, hozAlign: 'center' }, { title: '거래처명', field: 'custnm', width: 200 }], onConfirm: (d: any) => { target.update({ scustcd: d.custcd, scustnm: d.custnm, _status: target.getData()._state === 'EXIST' ? '수정' : '입력' }); } });
    modalVisible.value = true;
  }
}

const handleRowAction = (row: any) => {
const d = row.getData();
if (d._state === 'NEW')
    row.delete();
else row.update({
   _status: d._status === '삭제' ? '' : '삭제' });
}
const deleteSelectedRows = () => {
const sel = grid2?.getSelectedRows();
if (sel?.length) sel.forEach(row => handleRowAction(row)); }
const addRow = () => grid2?.addRow({ balqty: 0, price: 0, balamt: 0, _status: '입력', _state: 'NEW' }, true);

function initialize() {
  resetForm(form_02);
  Object.assign(form_02, { cmpycd: authStore.cmpycd, balym: today.replace(/-/g, '').substring(0, 6), balno: '0000', balymd: today, reqymd: today, deptcd: authStore.deptcd, deptnm: authStore.deptnm, bal_userid: authStore.userid, flag: 'N', balgb: '2' });
  grid1?.setData([]); grid2?.setData([]);
}

async function handleFullDelete() {
  if (!confirm('정말 전체 삭제하시겠습니까?')) return;
  try {
    const mst = { actkind: 'D', cmpycd: authStore.cmpycd, balym: form_02.balym, balno: form_02.balno, updemp: authStore.userid };
    await api.post('/hsio/HSIO_052U_SAVE', { mst, dtl: [] });
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
