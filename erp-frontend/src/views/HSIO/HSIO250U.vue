<!--
	=============================================================
	프로그램명	: 덤입고작업 (HSIO250U)
	작성일자	: 2025.02.24
	설명        : 덤입고(무상입고) 마스터/상세 관리 (HSOD100U 디자인 UI + HSIO580U 좌측그리드 이식)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-gift-fill me-2 text-primary" style="font-size: 18px;"></i>
        구매관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        입고관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">덤입고작업 (HSIO250U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-2">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-save" @click="save" :disabled="isClosed">저장</button>
        <button class="btn-erp btn-delete" @click="handleFullDelete" :disabled="!formData.iono || formData.iono === '0000' || isClosed">전체삭제</button>
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
                <th class="text-center bg-light">입고일자</th>
                <td class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                  <DateForm v-model:fromdt="searchForm.fromdt" v-model:todt="searchForm.todt" />
                </td>
                <th class="text-center bg-light">매입처명</th>
                <td>
                  <input v-model="searchForm.schcustnm" class="form-control form-control-sm" placeholder="매입처 검색" @keyup.enter="search" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 투-그리드 레이아웃 영역 -->
      <div class="d-flex gap-2 flex-grow-1 overflow-hidden" style="min-height: 0;">

        <!-- ⬅️ 좌측: 덤입고 목록 (HSIO580U 스타일 이식) -->
        <div class="card border shadow-sm d-flex flex-column overflow-hidden grid-container-left" style="width: 350px; min-width: 350px;">
          <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark">입고 목록</div>
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
                  <col style="width: 100px;" /><col />
                  <col style="width: 100px;" /><col />
                  <col style="width: 100px;" /><col />
                  <col style="width: 100px;" /><col />
                </colgroup>
                <tbody>
                  <tr>
                    <th class="required bg-light">입고부서</th>
                    <td>
                      <div class="input-group input-group-sm">
                        <input v-model="formData.deptnm" class="form-control" readonly />
                        <button class="btn btn-outline-secondary" @click="handleOpenHelp('DEPT')" :disabled="isClosed"><i class="bi bi-search"></i></button>
                      </div>
                    </td>
                    <th class="bg-light text-center">입고번호</th>
                    <td><input v-model="formData.iono" class="form-control bg-light text-primary fw-bold text-center" readonly placeholder="자동생성" /></td>
                    <th class="required bg-light text-center">입고일자</th>
                    <td><input v-model="formData.ioymd" type="date" class="form-control" :readonly="isClosed" /></td>
                    <th class="required bg-light text-center">입고창고</th>
                    <td>
                      <select v-model="formData.whcd" class="form-select" :disabled="isClosed">
                        <option v-for="opt in whOptions" :key="opt.code" :value="opt.code">{{ opt.cdnm }}</option>
                      </select>
                    </td>
                  </tr>
                  <tr>
                    <th class="required bg-light">매&nbsp;&nbsp;입&nbsp;&nbsp;처</th>
                    <td>
                      <div class="input-group input-group-sm">
                        <input v-model="formData.custnm" class="form-control" readonly />
                        <button class="btn btn-outline-secondary" @click="handleOpenHelp('CUST')" :disabled="isClosed"><i class="bi bi-search"></i></button>
                      </div>
                    </td>
                    <th class="bg-light text-center">작업자</th>
                    <td>
                      <select v-model="formData.userid" class="form-select">
                        <option v-for="user in userData" :key="user.userid" :value="user.userid">{{ user.usernm }}</option>
                      </select>
                    </td>
                    <th class="bg-light text-center">특기사항</th>
                    <td colspan="3"><input v-model="formData.remark" class="form-control" :readonly="isClosed" /></td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- 상세 품목 그리드 영역 -->
          <div class="card border shadow-sm flex-grow-1 d-flex flex-column overflow-hidden grid-container-right">
            <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
              <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>덤입고 품목 리스트</span>
              <div class="d-flex gap-1">
                <button class="btn btn-sm btn-outline-primary py-0 px-2 fw-bold" @click="addRow" :disabled="isClosed" style="font-size: 12px;">+ 행추가</button>
                <button class="btn btn-sm btn-outline-danger py-0 px-2 fw-bold" @click="deleteSelectedRows" :disabled="isClosed" style="font-size: 12px;">- 행삭제</button>
              </div>
            </div>
            <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
              <div ref="tableRef2" class="tabulator-instance flex-grow-1"></div>
            </div>
          </div>
        </div>
      </div>
      <div class="bottom-spacer"></div>
    </div>
  </div>

  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, computed, onUnmounted } from 'vue'
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
import { useSearchStore } from '@/stores/useSearchStore'
import { useRoute } from 'vue-router'
import type { ModalProps } from '@/types/modal'

const authStore = useAuthStore()
const searchStore = useSearchStore()
const route = useRoute()
const { firstDay, today } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

// [1] 데이터 모델링
const searchForm = reactive({ fromdt: firstDay, todt: today, schcustnm: '' })
const formData = reactive<any>({
  actkind: 'S0', cmpycd: authStore.cmpycd, ioym: today.substring(0, 7).replace('-', ''), iono: '',
  ioymd: today, deptcd: authStore.deptcd, deptnm: authStore.deptnm,
  custcd: '', custnm: '', whcd: '100', remark: '', userid: authStore.userid, usernm: authStore.usernm, pkunityn: 'N', astkind: '2'
})

const closingInfo = reactive({ sclsym: '' })
const whOptions = ref<any[]>([])
const userData = ref<any[]>([])

// [2] 그리드 관리
const tableRef1 = ref<HTMLDivElement | null>(null)
const tableRef2 = ref<HTMLDivElement | null>(null)
let grid1: Tabulator | null = null
let grid2: Tabulator | null = null

const isClosed = computed(() => {
  if (!closingInfo.sclsym || !formData.ioymd) return false
  return formData.ioymd.replace(/-/g, '').substring(0, 6) <= closingInfo.sclsym
})

const initGrids = () => {
  if (!tableRef1.value || !tableRef2.value) return

  // ⬅️ 좌측 그리드 (HSIO580U 스타일)
  grid1 = new Tabulator(tableRef1.value, {
    layout: "fitColumns", height: "100%", placeholder: "데이터 없음",
    columns: [
      { title: "No", formatter: "rownum", width: 40, hozAlign: "center", headerSort: false },
      { title: "입고일자", field: "ioymd", hozAlign: "center", width: 100, headerSort: false, formatter: (c) => {
          const v = c.getValue();
          return v && v.length === 8 ? `${v.substring(0,4)}-${v.substring(4,6)}-${v.substring(6,8)}` : v;
        }
      },
      { title: "입고번호", field: "iono", hozAlign: "center", width: 110, cssClass: "fw-bold text-primary", headerSort: false }
    ],
  });
  grid1.on("rowClick", (e, row) => fetchDetail(row.getData()));

  // ➡️ 우측 상세 그리드
  grid2 = new Tabulator(tableRef2.value, {
    layout: "fitColumns", height: "100%", placeholder: "품목 없음",
    columnCalcs: "table", selectable: true,
    selectableCheck: (row) => row.getData()._STATE !== 'EMPTY',
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "선택", width: 40, hozAlign: "center", formatter: "rowSelection", titleFormatter: "rowSelection", headerSort: false, cellClick: (e) => e.stopPropagation() },
      { title: "상태", field: "upkind", width: 50, hozAlign: "center",
        formatter: (c) => {
          const v = c.getValue();
          if (v === 'A') return '<span class="badge bg-primary" style="font-size:10px;">신규</span>';
          if (v === 'U') return '<span class="badge bg-warning text-dark" style="font-size:10px;">수정</span>';
          if (v === 'D') return '<span class="badge bg-danger" style="font-size:10px;">삭제</span>';
          return '';
        }
      },
      { title: "No", formatter: (cell) => cell.getData()._STATE === 'EMPTY' ? "" : cell.getRow().getPosition(), width: 40, hozAlign: "center" },
      { title: "품목명", field: "itemnm", minWidth: 180, widthGrow: 1, cssClass: 'fw-bold text-primary',
        cellClick: (e, cell) => { if(!isClosed.value) handleOpenHelp('ITEM', cell.getRow()) }
      },
      { title: "규격", field: "itsize", width: 120 },
      { title: "단위", field: "unit", width: 60, hozAlign: "center" },
      { title: "수량", field: "ioqty", width: 90, hozAlign: "right", editor: "number", cellEdited: (cell) => calcRow(cell.getRow(), 'ioqty') },
      { title: "단가", field: "price", width: 100, hozAlign: "right", editor: "number", cellEdited: (cell) => calcRow(cell.getRow(), 'price') },
      { title: "금액", field: "ioamt", width: 110, hozAlign: "right", editor: "number", cellEdited: (cell) => calcRow(cell.getRow(), 'ioamt'), formatter: "money", formatterParams: { precision: 0 }, bottomCalc: "sum" },
      { title: "포장용기", field: "pkunitnm", width: 100, visible: formData.pkunityn === 'Y' },
      { title: "삭제", width: 40, hozAlign: "center",
        formatter: (cell) => cell.getData()._STATE === 'EMPTY' ? "" : "<i class='bi bi-trash text-danger cursor-pointer'></i>",
        cellClick: (e, cell) => handleRowAction(cell.getRow())
      }
    ],
    rowFormatter: (row) => { if (row.getData()._STATE === 'EMPTY') row.getElement().classList.add("empty-row-style"); }
  });
}

// [3] 로직 처리
const calcRow = (row: any, field: string) => {
  const data = row.getData();
  if (data._STATE === 'EMPTY') return;

  let qty = Number(data.ioqty || 0);
  let price = Number(data.price || 0);
  let amt = Number(data.ioamt || 0);

  if (field === 'ioqty' || field === 'price') {
    amt = Math.floor(qty * price);
  } else if (field === 'ioamt') {
    price = qty !== 0 ? Math.floor(amt / qty) : 0;
  }

  row.update({ ioamt: amt, price: price, upkind: data.iorowno ? 'U' : 'A' });
}

const handleOpenHelp = (type: string, target?: any) => {
  if (isClosed.value) return;

  if (type === 'DEPT') {
    Object.assign(modalProps, {
      title: '입고부서 선택',
      path: '/ha00/HA00_00P_STR',
      data: { gubun: 'D0', cmpycd: authStore.cmpycd, code: '', codenm: formData.deptnm, remark: '' },
      columns: [
        { title: '부서코드', field: 'deptcd', width: 100, hozAlign: 'center' },
        { title: '부서명', field: 'deptnm', width: 200 }
      ],
      onConfirm: (d: any) => { formData.deptcd = d.deptcd; formData.deptnm = d.deptnm }
    })
    modalVisible.value = true
  } else if (type === 'CUST') {
    Object.assign(modalProps, {
      title: '매입처 선택',
      path: '/ha00/HA00_00P_STR',
      data: { gubun: 'C4', cmpycd: authStore.cmpycd, code: '', codenm: formData.custnm, remark: '' },
      columns: [
        { title: '거래처코드', field: 'custcd', width: 100, hozAlign: 'center' },
        { title: '거래처명', field: 'custnm', width: 200 }
      ],
      onConfirm: (d: any) => { formData.custcd = d.custcd; formData.custnm = d.custnm }
    })
    modalVisible.value = true
  } else if (type === 'ITEM') {
    if (!formData.custcd) return vAlertError('매입처를 먼저 선택하세요.');
    Object.assign(modalProps, {
      title: '품목 선택',
      path: '/hs00/HS00_000S_STR',
      data: { gubun: 'I1', cmpycd: authStore.cmpycd, gbncd: '3', code: '', codenm: '', remark: '' },
      columns: [
        { title: '품목코드', field: 'itemcd', width: 100, hozAlign: 'center' },
        { title: '품목명', field: 'itemnm', width: 200 },
        { title: '규격', field: 'itsize', width: 150 },
        { title: '단위', field: 'unitnm', width: 80, hozAlign: 'center' }
      ],
      onConfirm: (d: any) => {
        target.update({
          itemcd: d.itemcd, itemnm: d.itemnm, itsize: d.itsize, unit: d.unit || d.unitnm,
          pkunit: d.unit || d.unitnm,
          price: d.incost || d.price || 0, ioqty: 1, ioamt: d.incost || 0, upkind: 'A', _STATE: 'NEW'
        });
        calcRow(target, 'price');
      }
    })
    modalVisible.value = true
  }
}

const handleRowAction = (row: any) => {
  const data = row.getData();
  if (data._STATE === 'EMPTY') return;
  if (data._STATE === 'NEW') row.delete();
  else row.update({ upkind: data.upkind === 'D' ? 'U' : 'D' });
}

async function search() {
  try {
    const params = { actkind: 'S0', cmpycd: authStore.cmpycd, fromdt: searchForm.fromdt.replace(/-/g, ''), todt: searchForm.todt.replace(/-/g, ''), custnm: searchForm.schcustnm };
    const res = await api.post('/hsio/HSIO_250U_STR', params);
    grid1?.setData(res.data || res.data);
    vAlert('조회되었습니다.');
  } catch (e) { vAlertError('조회 실패'); }
}

async function fetchDetail(row: any) {
  Object.assign(formData, row);
  try {
    const res = await api.post('/hsio/HSIO_251U_STR', { ...formData, actkind: 'S0' });
    grid2?.setData((res.data || []).map((i: any) => {
      const ioqty = Number(i.ioqty || 0);
      const ioamt = Number(i.ioamt || 0);
      // 단가가 0인 경우 금액/수량으로 계산 (HSIO490U, HSIO190U 동일 로직 적용)
      const price = i.price && Number(i.price) !== 0 ? Number(i.price) : (ioqty !== 0 ? Math.floor(ioamt / ioqty) : 0);
      return { ...i, ioqty, ioamt, price, upkind: 'U', _STATE: 'EXIST' };
    }));
  } catch (e) { vAlertError('상세 로드 실패'); }
}

async function save() {
  const details = grid2?.getData().filter(r => r._STATE !== 'EMPTY' && r.upkind) || [];
  if (!details.length && !formData.iono) return vAlertError('입고 품목을 추가하세요.');

  try {
    const payload = {
    ...formData,
    actkind: formData.iono ? 'U0' : 'A0',
    cfmyn: 'Y',
    items: details };
    await api.post('/hsio/HSIO_250U_STR', payload);
    vAlert('저장되었습니다.'); search();
  } catch (e) { vAlertError('저장 실패'); }
}

function addRow() {
  if (isClosed.value) return;
  grid2?.addRow({ upkind: 'A', ioqty: 0, price: 0, ioamt: 0, _STATE: 'NEW' }, true);
}

function deleteSelectedRows() {
  const selected = grid2?.getSelectedRows();
  if (!selected?.length) return vAlertError('삭제할 행을 선택하십시오.');
  selected.forEach(row => handleRowAction(row));
}

function initialize() {
  resetForm(formData);
  formData.ioym = today.substring(0, 7).replace('-', ''); formData.ioymd = today; formData.whcd = '100';
  grid1?.clearData(); grid2?.setData([]);
}

async function handleFullDelete() {
  if (!formData.iono) return;
  if (!confirm('정말로 전체 삭제하시겠습니까?')) return;
  try {
    await api.post('/hsio/HSIO_250U_STR', {
        ...formData,
        actkind: 'D0',
        cmpycd: authStore.cmpycd,
        ioym: formData.ioym,
        iono: formData.iono,
        iogbn: '100',
        cfmyn: 'Y'
    });
    vAlert('삭제되었습니다.'); initialize(); search();
  } catch (e) { vAlertError('삭제 실패'); }
}

onMounted(async () => {
  api.get('/hs00/HS00_000S_STR', { params: { gubun: 'W0', cmpycd: authStore.cmpycd } })
     .then(r => whOptions.value = r.data.map((i: any) => ({ code: i.whcd || Object.values(i)[0], cdnm: i.whnm || Object.values(i)[1] })));
  api.get('/hp00/HP00_000S_STR', { params: { gubun: 'CL', cmpycd: authStore.cmpycd } }).then(r => {
    if(r.data?.length) closingInfo.sclsym = r.data[0].sclsym;
  });
  api.post('/ha00/HA00_00P_STR', { gubun: 'SD', cmpycd: authStore.cmpycd, gbncd: '', code: '', remark: '' }).then(r => {
    userData.value = r.data;
  });
  initGrids(); initialize();
})

onUnmounted(() => {
  if (grid1) grid1.destroy();
  if (grid2) grid2.destroy();
  searchStore.removeTab(route.name as string)
});
</script>

<style scoped>
.main-content-wrapper { padding-bottom: 0px !important; }
.grid-container-left, .grid-container-right { border-bottom: 3px solid #005a9f !important; }
.erp-table-dense th, .erp-table-dense td {
  height: 32px !important;
  padding: 0 8px !important;
  font-size: 12px;
  vertical-align: middle;
  border: 1px solid #dee2e6;
}
.erp-table-dense .form-control, .erp-table-dense .form-select, .erp-table-dense .btn {
  height: 26px !important;
  font-size: 12px !important;
  border-radius: 2px;
}
.tabulator-instance { width: 100% !important; background-color: #fff; }
:deep(.empty-row-style) { opacity: 0.6; }
</style>
