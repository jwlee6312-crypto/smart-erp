<!--
	=============================================================
	프로그램명	: 입고반품등록 (HSIO190U)
	작성일자	: 2025.02.24
	설명        : 구매 입고반품 마스터/상세 관리 (HSOD100U 표준 구조 및 소문자 원칙 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-arrow-left-right me-2 text-primary" style="font-size: 18px;"></i>
        구매관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        입고관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">입고반품등록 (HSIO190U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-2">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-save" @click="save" :disabled="isClosed">저장</button>
        <button class="btn-erp btn-delete" @click="handleFullDelete" :disabled="!form_02.iono || isClosed">전체삭제</button>
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
                <th class="text-center bg-light">반품일자</th>
                <td class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                  <DateForm v-model:fromdt="form_01.fromdt" v-model:todt="form_01.todt" />
                </td>
                <th class="text-center bg-light">거래처명</th>
                <td>
                  <input v-model="form_01.schcustnm" class="form-control form-control-sm" placeholder="매입거래처 검색" @keyup.enter="search" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 투-그리드 레이아웃 영역 -->
      <div class="d-flex gap-2 flex-grow-1 overflow-hidden" style="min-height: 0;">

        <!-- ⬅️ 좌측: 반품 목록 -->
        <div class="card border shadow-sm d-flex flex-column overflow-hidden grid-container-left" style="width: 350px; min-width: 350px;">
          <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark">반품 목록</div>
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
                    <th class="required bg-light">입고부서</th>
                    <td>
                      <div class="input-group input-group-sm">
                        <input v-model="form_02.deptnm" class="form-control" readonly />
                        <button class="btn btn-outline-secondary" @click="handleOpenHelp('DEPT')" :disabled="isClosed"><i class="bi bi-search"></i></button>
                      </div>
                    </td>
                    <th class="bg-light text-center">반품번호</th>
                    <td><input :value="form_02.iono ? (form_02.ioym || '') + form_02.iono : ''" class="form-control bg-light text-primary fw-bold text-center" readonly placeholder="자동생성" /></td>
                    <th class="required bg-light text-center">반품일자</th>
                    <td><input v-model="form_02.ioymd" type="date" class="form-control" :readonly="isClosed" /></td>
                    <th class="required bg-light text-center">담당자</th>
                    <td>
                      <select v-model="form_02.userid" class="form-select">
                        <option v-for="item in userData" :key="item.userid" :value="item.userid">{{ item.usernm }}</option>
                      </select>
                    </td>
                  </tr>
                  <tr>
                    <th class="required bg-light">매입거래처</th>
                    <td>
                      <div class="input-group input-group-sm">
                        <input v-model="form_02.custnm" class="form-control" readonly />
                        <button class="btn btn-outline-secondary" @click="handleOpenHelp('CUST')" :disabled="isClosed"><i class="bi bi-search"></i></button>
                      </div>
                    </td>
                    <th class="required bg-light text-center">출고창고</th>
                    <td>
                      <select v-model="form_02.whcd" class="form-select" :disabled="isClosed">
                        <option v-for="item in whcdData" :key="item.whcd" :value="item.whcd">{{ item.whnm }}</option>
                      </select>
                    </td>
                    <th class="bg-light text-center">특기사항</th>
                    <td><input v-model="form_02.remark" class="form-control" :readonly="isClosed" /></td>
                    <th class="bg-light text-center">합계금액</th>
                    <td><input v-model="form_02.totsum" class="form-control bg-light text-end fw-bold" readonly /></td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- 상세 품목 그리드 영역 -->
          <div class="card border shadow-sm flex-grow-1 d-flex flex-column overflow-hidden grid-container-right">
            <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
              <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>반품 품목 리스트</span>
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

  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, computed, onUnmounted, nextTick } from 'vue'
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
import { useSearchStore } from '@/stores/useSearchStore'
import { useRoute } from 'vue-router'

const authStore = useAuthStore()
const searchStore = useSearchStore()
const route = useRoute()
const { firstDay, today } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps, openHelp } = useCommonHelp()

// [1] 데이터 모델링
const form_01 = reactive({ fromdt: firstDay, todt: today, schcustnm: '' })
const form_02 = reactive<any>({
  actkind: 'S0', cmpycd: authStore.cmpycd,
  deptcd: authStore.deptcd, deptnm: authStore.deptnm,
  ioym: today.replace(/-/g, '').substring(0, 6), iono: '',
  ioymd: today, custcd: '', custnm: '', whcd: '100', userid: authStore.userid,
  remark: '', totsum: 0, sts: 'N'
})

const closingInfo = reactive({ sclsym: '' })
const whcdData = ref<any[]>([])
const userData = ref<any[]>([])

// [2] 그리드 관리
const tableRef1 = ref<HTMLDivElement | null>(null)
const tableRef2 = ref<HTMLDivElement | null>(null)
let grid1: Tabulator | null = null
let grid2: Tabulator | null = null

const isClosed = computed(() => {
  if (!closingInfo.sclsym || !form_02.ioymd) return false
  return form_02.ioymd.replace(/-/g, '').substring(0, 6) <= closingInfo.sclsym
})

const initGrids = () => {
  if (!tableRef1.value || !tableRef2.value) return

  grid1 = new Tabulator(tableRef1.value!, {
    data: [], // 🚀 초기 데이터 설정
    layout: "fitColumns", height: "100%", placeholder: "데이터 없음",
    columns: [
      { title: "No", formatter: "rownum", width: 40, hozAlign: "center", headerSort: false },
      { title: "거래처명", field: "custnm", hozAlign: "left", headerSort: false },
      { title: "반품번호", field: "iono", hozAlign: "center", width: 120, cssClass: "fw-bold text-primary", headerSort: false,
        formatter: (cell: any) => {
          const data = cell.getData();
          return (data.ioym || '') + (data.iono || '');
        }
      }
    ],
  });
  grid1.on("rowClick", (e, row) => fetchDetail(row.getData()));

  grid2 = new Tabulator(tableRef2.value!, {
    data: [], // 🚀 초기 데이터 설정
    layout: "fitColumns", height: "100%", placeholder: "품목 없음", selectable: true,
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "선택", width: 40, hozAlign: "center", formatter: "rowSelection", titleFormatter: "rowSelection" },
      { title: "상태", field: "_status", width: 60, hozAlign: "center", formatter: (c) => {
          const v = c.getValue();
          if (v === '입력') return '<span class="badge bg-primary">신규</span>';
          if (v === '수정') return '<span class="badge bg-warning text-dark">수정</span>';
          if (v === '삭제') return '<span class="badge bg-danger">삭제</span>';
          return '';
      }},
      { title: "품목명", field: "itemnm", minWidth: 180, widthGrow: 1, cssClass: 'fw-bold text-primary',
        cellClick: (e, cell) => { if(!isClosed.value) handleOpenHelp('ITEM', cell.getRow()) }
      },
      { title: "규격", field: "itsize", width: 100 },
      { title: "단위", field: "unit", width: 60, hozAlign: "center" },
      { title: "수량", field: "ioqty", width: 100, hozAlign: "right", editor: "number", cellEdited: (cell) => calcRow(cell.getRow(), 'ioqty') },
      { title: "단가", field: "price", width: 110, hozAlign: "right", editor: "number", cellEdited: (cell) => calcRow(cell.getRow(), 'price') },
      { title: "공급가", field: "ioamt", width: 120, hozAlign: "right", editor: "number", cellEdited: (cell) => calcRow(cell.getRow(), 'ioamt'), formatter: "money", formatterParams: { precision: 0 } },
      { title: "부가세", field: "iovat", width: 100, hozAlign: "right", editor: "number", cellEdited: (cell) => calcRow(cell.getRow(), 'iovat'), formatter: "money", formatterParams: { precision: 0 } },
      { title: "합계금액", field: "totamt", width: 120, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "fw-bold text-dark bg-light" },
      { title: "삭제", width: 40, hozAlign: "center", formatter: () => "<i class='bi bi-trash text-danger'></i>", cellClick: (e, cell) => handleRowAction(cell.getRow()) }
    ]
  });
}

const calcRow = (row: any, field: string) => {
  const d = row.getData();
  let qty = Number(d.ioqty || 0);
  let price = Number(d.price || 0);
  let amt = Number(d.ioamt || 0);
  let vat = Number(d.iovat || 0);

  if (field === 'ioqty' || field === 'price') {
    amt = Math.floor(qty * price);
    vat = Math.floor(amt * 0.1);
  } else if (field === 'ioamt') {
    price = qty !== 0 ? Math.floor(amt / qty) : 0;
    vat = Math.floor(amt * 0.1);
  } else if (field === 'iovat') {
    // 부가세 직접 수정 시 별도 로직 없음 (그대로 반영)
  }

  row.update({ ioamt: amt, price: price, iovat: vat, totamt: amt + vat });
  if (d._state === 'EXIST' && d._status !== '삭제') row.update({ _status: '수정' });
  updateTotalSum();
}

const updateTotalSum = () => {
  const data = grid2?.getData().filter((r: any) => r._status !== '삭제') || [];
  form_02.totsum = data.reduce((sum: number, r: any) => sum + Number(r.totamt || 0), 0);
}

const handleOpenHelp = (type: string, target?: any) => {
  if (type === 'DEPT') openHelp('DEPT', (d) => { form_02.deptcd = d.deptcd; form_02.deptnm = d.deptnm });
  else if (type === 'CUST') openHelp('CUST', (d) => { form_02.custcd = d.custcd; form_02.custnm = d.custnm });
  else if (type === 'ITEM') {
    Object.assign(modalProps, {
      title: '품목 선택',
      path: '/hs00/HS00_000S_STR',
      defaultField: 'itemnm',
      data: { gubun: 'I1', cmpycd: authStore.cmpycd, gbncd: '1', code: '', remark: '' },
      columns: [
        { title: '품목코드', field: 'itemcd', width: 100, hozAlign: 'center' },
        { title: '품목명', field: 'itemnm', width: 200 },
        { title: '규격', field: 'itsize', width: 150 },
        { title: '단위', field: 'unit', width: 80, hozAlign: 'center' }
      ],
      onConfirm: (d: any) => {
        target.update({
          itemcd: d.itemcd, itemnm: d.itemnm, itsize: d.itsize, unit: d.unit,
          pkunit: d.unit,
          price: d.inprice || 0, ioqty: 1, ioamt: d.inprice || 0, _status: '입력', _state: 'NEW'
        });
        calcRow(target, 'price');
      }
    });
    modalVisible.value = true;
  }
}

const handleRowAction = (row: any) => {
  const d = row.getData();
  if (d._state === 'NEW') row.delete();
  else row.update({ _status: d._status === '삭제' ? '' : '삭제' });
}

async function search() {
  try {
    const res = await api.post('/hsio/HSIO_190U_STR', {
      actkind: 'L',
      cmpycd: authStore.cmpycd,
      iogbn: '100',
      fromdt: form_01.fromdt.replace(/-/g, ''),
      todt: form_01.todt.replace(/-/g, ''),
      custnm: form_01.schcustnm
    });
    grid1?.setData(res.data); vAlert('조회되었습니다.');
  } catch (e) { vAlertError('조회 실패'); }
}

async function fetchDetail(row: any) {
  const fYmd = (d: string) => d && d.length === 8 ? `${d.substring(0, 4)}-${d.substring(4, 6)}-${d.substring(6, 8)}` : today;
  Object.assign(form_02, { ...row, ioymd: fYmd(row.ioymd) });
  try {
    const res = await api.post('/hsio/HSIO_191U_STR', {
      actkind: 'S',
      cmpycd: authStore.cmpycd,
      iogbn: '100',
      ioym: row.ioym,
      iono: row.iono,
      ioqty: 0, ioamt: 0, iovat: 0
    });
    grid2?.setData(res.data.map((i: any) => {
      const ioqty = Math.abs(Number(i.ioqty || 0));
      const ioamt = Math.abs(Number(i.ioamt || 0));
      const iovat = Math.abs(Number(i.iovat || 0));
      // 단가가 0인 경우 금액/수량으로 계산
      const price = i.price && Number(i.price) !== 0 ? Math.abs(Number(i.price)) : (ioqty !== 0 ? Math.floor(ioamt / ioqty) : 0);
      return {
        ...i,
        ioqty,
        ioamt,
        price,
        iovat,
        totamt: ioamt + iovat,
        _state: 'EXIST',
        _status: ''
      };
    }));
    updateTotalSum();
  } catch (e) { vAlertError('상세 로드 실패'); }
}

async function save() {
  if (!form_02.custcd) return vAlertError('거래처를 선택하세요.');
  const details = grid2?.getData().filter((r: any) => r._status) || [];
  if (!details.length && !form_02.iono) return vAlertError('항목을 추가하세요.');

  if (!confirm('저장하시겠습니까?')) return

  try {
    // 🚀 [Service-Base] 백엔드 통합 저장 서비스 호출 (트랜잭션 보장)
    const payload = {
      mst: {
        ...form_02,
        iogbn: '100',
        iotype: '100',
        cfmyn: 'Y',
        gubun: '1',
        totsum: String(form_02.totsum || '0'), // char(15) 대응: 문자열 변환
        actkind: form_02.iono ? 'U' : 'A',
        ioymd: form_02.ioymd.replace(/-/g, '')
      },
      dtl: details.map(item => ({
        ...item,
        iogbn: '100',
        iotype: '100',
        cfmyn: 'Y',
        ioqty: Number(item.ioqty || 0),
        ioamt: Number(item.ioamt || 0),
        iovat: Number(item.iovat || 0),
        actkind: item._status === '입력' ? 'A' : (item._status === '삭제' ? 'D' : 'U')
      }))
    };

    const res = await api.post('/hsio/HSIO_190U_SAVE', payload);

    if (res.data?.status === 'success') {
      vAlert('저장되었습니다.');
      search();
    } else {
      // 서버 응답 메시지 알맹이만 전달
      vAlertError(res.data?.message);
    }
  } catch (e: any) {
    // 500 에러 등의 경우 서버가 보낸 핵심 메시지만 추출
    vAlertError(e.response?.data?.message || e.message);
  }
}

function addRow() {
  grid2?.addRow({ ioqty: 0, price: 0, ioamt: 0, iovat: 0, totamt: 0, _status: '입력', _state: 'NEW' }, true);
}

function deleteSelectedRows() {
  const sel = grid2?.getSelectedRows();
  if (sel?.length) sel.forEach(row => handleRowAction(row));
}

function initialize() {
  resetForm(form_02);
  Object.assign(form_02, { cmpycd: authStore.cmpycd, iono: '', ioymd: today, ioym: today.replace(/-/g, '').substring(0, 6), deptcd: authStore.deptcd, deptnm: authStore.deptnm, sts: 'N', whcd: '100' });
  if (grid1) grid1.setData([]);
  if (grid2) grid2.setData([]);
}

async function handleFullDelete() {
  if (!confirm('정말 전체 삭제하시겠습니까?')) return;
  try {
    const res = await api.post('/hsio/HSIO_190U_STR', {
          actkind: 'D',
          cmpycd: authStore.cmpycd,
          iogbn: '100',
          ioym: form_02.ioym || form_02.ioymd.replace(/-/g, '').substring(0, 6),
          iono: form_02.iono,
          cfmyn: 'Y'
    });

    if (res.data?.length > 0) {
      const firstRow = res.data[0];
      const resCode = Object.values(firstRow)[0]; // 첫 번째 컬럼값 (ioym 등)
      const resMsg = Object.values(firstRow)[1];  // 두 번째 컬럼값 (iono 등)

      if (resCode === '000000') {
        vAlertError(String(resMsg));
        return;
      }
    }

    vAlert('삭제되었습니다.'); initialize(); search();
  } catch (e: any) {
    vAlertError(e.response?.data?.message || e.message);
  }
}

onMounted(async () => {
    nextTick(initGrids);
    api.get('/hs00/HS00_000S_STR', { params: { gubun: 'W0' } }).then(r => { whcdData.value = r.data; });
    api.post('/ha00/HA00_00P_STR', { gubun: 'SD', cmpycd: authStore.cmpycd, gbncd: '', code: '', remark: '' }).then(r => {
        userData.value = r.data;
    });
    api.get('/hp00/HP00_000S_STR', { params: { gubun: 'CL' } }).then(r => { if(r.data?.length) closingInfo.sclsym = r.data[0].sclsym; });
    initialize();
})

onUnmounted(() => {
  if (grid1) grid1.destroy();
  if (grid2) grid2.destroy();
  searchStore.removeTab(route.name as string)
});
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
