<!--
	=============================================================
	프로그램명	: 무주문 출고등록 (HSIO500U)
	작성일자	: 2025.02.24
	설명        : 주문 없이 직접 출고 데이터를 등록/관리 (창고 로직 표준화 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
          <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
            <i class="bi bi-cart-check-fill me-2 text-primary" style="font-size: 18px;"></i> 영업관리
            <i class="bi bi-chevron-right mx-1 small opacity-50"></i> 출고관리
            <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
            <span class="text-primary fw-bolder text-nowrap">무주문 출고등록 (HSIO500U)</span>
        </div>
        <div class="btn-group-erp d-flex gap-1 pe-3">
            <button class="btn-erp btn-init" @click="initialize">초기화</button>
            <button class="btn-erp btn-search" @click="search">조회</button>
            <button class="btn-erp btn-save" @click="save">저장</button>
            <button class="btn-erp btn-delete" @click="handleFullDelete" :disabled="!form_02.iono">전체삭제</button>
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
                <th class="text-center bg-light">출고일자</th>
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

        <!-- ⬅️ 좌측: 출고 목록 -->
        <div class="card border shadow-sm d-flex flex-column overflow-hidden grid-container-left" style="width: 350px; min-width: 350px;">
          <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark">출고 목록</div>
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
                    <th class="required bg-light">판매부서</th>
                    <td>
                      <div class="input-group input-group-sm">
                        <input v-model="form_02.deptnm" class="form-control" readonly />
                        <button class="btn btn-outline-secondary" @click="handleOpenHelp('DEPT')"><i class="bi bi-search"></i></button>
                      </div>
                    </td>
                    <th class="bg-light text-center">출고번호</th>
                    <td><input :value="form_02.ioym && form_02.iono ? `${form_02.ioym}-${form_02.iono}` : ''" class="form-control bg-light text-primary fw-bold text-center" readonly placeholder="자동생성" /></td>
                    <th class="required bg-light text-center">출고일자</th>
                    <td><input v-model="form_02.ioymd" type="date" class="form-control" /></td>
                    <th class="required bg-light text-center">출고창고</th>
                    <td>
                      <select v-model="form_02.whcd" class="form-select">
                        <option v-for="opt in whOptions" :key="opt.whcd" :value="opt.whcd">{{ opt.whnm }}</option>
                      </select>
                    </td>
                  </tr>
                  <tr>
                    <th class="required bg-light">거래처</th>
                    <td>
                      <div class="input-group input-group-sm">
                        <input v-model="form_02.custnm" class="form-control" readonly />
                        <button class="btn btn-outline-secondary" @click="handleOpenHelp('CUST')"><i class="bi bi-search"></i></button>
                      </div>
                    </td>
                    <th class="bg-light text-center">여신잔액</th>
                    <td><input :value="formatNumber(form_02.janamt)" class="form-control bg-light text-end" readonly /></td>
                    <th class="bg-light text-center">여신기한</th>
                    <td><input v-model="form_02.rcvdd" class="form-control bg-light text-center" readonly style="width: 80px;" /></td>
                    <th class="required bg-light text-center">영업담당</th>
                    <td>
                      <select v-model="form_02.sale_userid" class="form-select">
                        <option v-for="item in userData" :key="item.userid" :value="item.userid">{{ item.usernm }}</option>
                      </select>
                    </td>
                  </tr>
                  <tr>
                    <th class="required bg-light">배송처</th>
                    <td colspan="3">
                      <AddressPopupForm
                        v-model:trancd="form_02.addrcd"
                        v-model:postno="form_02.postno"
                        v-model:address="form_02.address"
                        v-model:d_address="form_02.d_address"
                        @open-address="handleOpenHelp('ADDR')"
                      />
                    </td>
                    <th class="bg-light text-center">배송담당</th>
                    <td>
                      <select v-model="form_02.trnemp" class="form-select">
                        <option v-for="item in userData" :key="item.userid" :value="item.userid">{{ item.usernm }}</option>
                      </select>
                    </td>
                    <th class="required bg-light text-center">현금조건</th>
                    <td>
                      <select v-model="form_02.paycndt" class="form-select">
                        <option v-for="item in paycndtData" :key="item.code" :value="item.code">{{ item.cdnm }}</option>
                      </select>
                    </td>
                  </tr>
                  <tr>
                    <th class="bg-light text-center">특기사항</th>
                    <td colspan="3"><input v-model="form_02.remark" class="form-control" /></td>
                    <th class="bg-light text-center">만기일자</th>
                    <td><input v-model="form_02.endymd" type="date" class="form-control" /></td>
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
              <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>출고 품목 리스트</span>
              <div class="d-flex gap-1">
                <button class="btn btn-sm btn-outline-primary py-0 px-2 fw-bold" @click="addRow" style="font-size: 12px;">+ 행추가</button>
                <button class="btn btn-sm btn-outline-danger py-0 px-2 fw-bold" @click="deleteSelectedRows" style="font-size: 12px;">- 행삭제</button>
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
import { reactive, ref, onMounted, computed, onUnmounted, nextTick } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import DateForm from '@/components/DateForm.vue'
import AddressPopupForm from '@/components/AddressPopupForm.vue'
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
const form_01 = reactive({ fromdt: firstDay, todt: today, custnm: '' })
const form_02 = reactive<any>({
  cmpycd: authStore.cmpycd, deptcd: authStore.deptcd, deptnm: authStore.deptnm, iogbn: '200',
  ioym: '', iono: '', ioymd: today, whcd: '', custcd: '', custnm: '', iotype: '100',
  sale_userid: authStore.userid, trnemp: '', trnempnm: '',
  paycndt: '1', postno: '', address: '', d_address: '', addrcd: '', area: '',
  endymd: today, remark: '', totsum: 0, cfmyn: '',
  janamt: 0, rcvdd: ''
})

const closingInfo = reactive({ sclsym: '' })
const whOptions = ref<any[]>([])
const userData = ref<any[]>([])
const paycndtData = ref<any[]>([])

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

  grid1 = new Tabulator(tableRef1.value, {
    layout: "fitColumns", height: "100%", placeholder: "데이터 없음",
    columns: [
      { title: "No", formatter: "rownum", width: 40, hozAlign: "center", headerSort: false },
      { title: "일자", field: "ioymd", hozAlign: "center", width: 90, headerSort: false },
      { title: "출고번호", field: "iono", hozAlign: "center", width: 100, cssClass: "fw-bold text-primary", headerSort: false,
        formatter: (cell) => {
          const d = cell.getRow().getData();
          return d.ioym && d.iono ? `${d.ioym}-${d.iono}` : (d.iono || "");
        }
      },
      { title: "거래처", field: "custnm", hozAlign: "left", headerSort: false }
    ],
  });
  grid1.on("rowClick", (e, row) => fetchDetail(row.getData()));

  grid2 = new Tabulator(tableRef2.value, {
    layout: "fitColumns", height: "100%", placeholder: "품목 없음",
    columnCalcs: "table", selectable: true,
    selectableCheck: (row) => row.getData()._state !== 'EMPTY',
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "선택", width: 40, hozAlign: "center", formatter: "rowSelection", titleFormatter: "rowSelection", headerSort: false, cellClick: (e) => e.stopPropagation() },
      { title: "상태", field: "_status", width: 50, hozAlign: "center",
        formatter: (c) => {
          const v = c.getValue();
          if (v === '신규') return '<span class="badge bg-primary" style="font-size:10px;">신규</span>';
          if (v === '수정') return '<span class="badge bg-warning text-dark" style="font-size:10px;">수정</span>';
          if (v === '삭제') return '<span class="badge bg-danger" style="font-size:10px;">삭제</span>';
          return '';
        }
      },
      { title: "No", formatter: (cell) => cell.getData()._state === 'EMPTY' ? "" : cell.getRow().getPosition(), width: 40, hozAlign: "center" },
      { title: "품목명", field: "itemnm", minWidth: 200, widthGrow: 1, cssClass: 'fw-bold text-primary',
        cellClick: (e, cell) => handleOpenHelp('ITEM', cell.getRow())
      },
      { title: "규격", field: "itsize", width: 150 },
      { title: "단위", field: "unit", width: 80, hozAlign: "center" },
      { title: "수량", field: "ioqty", width: 100, hozAlign: "right", editor: "number", cellEdited: (cell) => calcRow(cell.getRow()), bottomCalc: "sum" },
      { title: "단가", field: "price", width: 120, hozAlign: "right", editor: "number", cellEdited: (cell) => calcRow(cell.getRow()) },
      { title: "금액", field: "ioamt", width: 130, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, bottomCalc: "sum" },
      { title: "부가세", field: "iovat", width: 110, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, bottomCalc: "sum" },
      { title: "합계", field: "sumamt", width: 140, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, bottomCalc: "sum" },
      { title: "삭제", width: 40, hozAlign: "center",
        formatter: (cell) => cell.getData()._state === 'EMPTY' ? "" : "<i class='bi bi-trash text-danger cursor-pointer'></i>",
        cellClick: (e, cell) => handleRowAction(cell.getRow())
      }
    ],
    rowFormatter: (row) => { if (row.getData()._state === 'EMPTY') row.getElement().classList.add("empty-row-style"); }
  });
}

// [3] 로직 처리
const fetchWhOptions = async () => {
  try {
    const resWh = await api.get('/hs00/HS00_000S_STR', {
      params: { gubun: 'W0', cmpycd: authStore.cmpycd }
    })
    whOptions.value = (resWh.data || []).map((i: any) => ({
      whcd: String(i.whcd || '').trim(),
      whnm: String(i.whnm || '').trim()
    }))

    if (whOptions.value.length > 0 && !form_02.whcd) {
      form_02.whcd = whOptions.value[0].whcd
    }
  } catch (e) {
    whOptions.value = []
  }
}

const setGridDataWithPadding = (data: any[]) => {
  const displayData = data.map(i => {
    const ioqty = Number(i.ioqty || 0);
    const ioamt = Number(i.ioamt || 0);
    return {
      ...i,
      ioqty,
      ioamt,
      price: ioqty !== 0 ? Math.round(ioamt / ioqty) : Number(i.price || 0),
      iovat: Number(i.iovat || 0),
      sumamt: ioamt + Number(i.iovat || 0),
      _state: 'EXIST',
      _status: ''
    };
  });
  grid2?.setData(displayData);
}

const calcRow = (row: any) => {
  const data = row.getData();
  if (data._state === 'EMPTY') return;

  const qty = Number(data.ioqty || 0);
  const price = Number(data.price || 0);
  const amt = Math.round(qty * price);
  const vat = Math.round(amt * 0.1);
  row.update({
    ioamt: amt, iovat: vat, sumamt: amt + vat
  });
  if (data._state === 'EXIST' && data._status !== '삭제') row.update({ _status: '수정' });
  updateTotalSum();
}

const updateTotalSum = () => {
  const data = grid2?.getData().filter(r => r._state !== 'EMPTY' && r._status !== '삭제') || [];
  form_02.totsum = data.reduce((sum, r) => sum + Number(r.sumamt || 0), 0);
}
const handleOpenHelp = (type: string, target?: any) => {
  // 1. 부서 선택 (조회용 및 등록용 공통)
  if (type === 'DEPT_SCH' || type === 'DEPT') {
    Object.assign(modalProps, {
      title: '부서 선택',
      path: '/ha00/HA00_00P_STR',
      defaultField: 'deptnm',
      data: { gubun: 'D0', cmpycd: authStore.cmpycd, gbncd: '', code: '', remark: '' },
      columns: [
        { title: '코드', field: 'deptcd', width: 80, hozAlign: 'center' },
        { title: '부서명', field: 'deptnm', width: 200 }
      ],
      onConfirm: (d: any) => {
        if (type === 'DEPT_SCH') { form_01.deptcd = d.deptcd; form_01.deptnm = d.deptnm }
        else { form_02.deptcd = d.deptcd; form_02.deptnm = d.deptnm }
      }
    })
    modalVisible.value = true
  }
  // 2. 거래처 선택
  else if (type === 'CUST') {
    Object.assign(modalProps, {
      title: '거래처 선택',
      path: '/ha00/HA00_00P_STR',
      defaultField: 'custnm',
      data: { gubun: 'C4', cmpycd: authStore.cmpycd, gbncd: '', code: '', remark: '' },
      columns: [
        { title: '코드', field: 'custcd', width: 80, hozAlign: 'center' },
        { title: '거래처명', field: 'custnm', width: 200 }
      ],
      onConfirm: (d: any) => {
        form_02.custcd = d.custcd;
        form_02.custnm = d.custnm;
        // 여신 정보 조회 (HSIO300U 로직 이식)
        api.post('/hs00/HS00_150S_STR', { cmpycd: authStore.cmpycd, custnm: d.custnm }).then(r => {
          if (r.data?.length) {
            const n = r.data[0];
            form_02.rcvdd = n.rcvdd || n.RCVDD || '';
            form_02.janamt = n.janamt || n.JANAMT || 0;
          }
        });
      }
    })
    modalVisible.value = true
  }
  // 3. 배송처 선택 (ADDR) - 상세주소 필드명 d_address 적용
  else if (type === 'ADDR') {
    if (!form_02.custcd) return vAlertError('거래처를 먼저 선택하세요.')
    Object.assign(modalProps, {
      title: '배송처 선택',
      path: '/hs00/HS00_000S_STR',
      defaultField: 'trannm',
      data: { gubun: 'T0', cmpycd: authStore.cmpycd, gbncd: '', code: form_02.custcd },
      columns: [
        { title: '코드', field: 'trancd', width: 60, hozAlign: 'center' },
        { title: '배송처명', field: 'custnm', width: 100 },
        { title: '우편번호', field: 'postno', width: 50 },
        { title: '주소', field: 'address', width: 200 },
        { title: '상세주소', field: 'd_address', width: 100 }
      ],
      onConfirm: (d: any) => {
        form_02.addrcd = d.trancd;
        form_02.postno = d.postno;
        form_02.address = d.address;
        form_02.d_address = d.d_address || '';
      }
    })
    modalVisible.value = true
  }
  // 4. 품목 선택 (그리드 내 행 데이터 업데이트)
  else if (type === 'ITEM') {
    Object.assign(modalProps, {
      title: '품목 선택',
      path: '/hs00/HS00_000S_STR',
      defaultField: 'itemnm',
      data: { gubun: 'I1', cmpycd: authStore.cmpycd, gbncd: '2', code: '', remark: '' },
      columns: [
        { title: '품목코드', field: 'itemcd', width: 100, hozAlign: 'center' },
        { title: '품목명', field: 'itemnm', width: 200 },
        { title: '규격', field: 'itsize', width: 150 },
        { title: '단위', field: 'unit', width: 80, hozAlign: 'center' }
      ],
      onConfirm: (d: any) => {
        target.update({
          itemcd: d.itemcd, itemnm: d.itemnm, itsize: d.itsize, unit: d.unit,
          price: d.outcost || 0, ioqty: 1, _status: '신규', _state: 'NEW'
        });
        calcRow(target);
      }
    })
    modalVisible.value = true
  }
}

const handleRowAction = (row: any) => {
  const data = row.getData();
  if (data._state === 'EMPTY') return;
  if (data._state === 'NEW') row.delete();
  else row.update({ _status: data._status === '삭제' ? '' : '삭제' });
  updateTotalSum();
}

async function search() {
  try {
    const res = await api.post('/hsio/HSIO_500U_STR', {
        ...form_01,
        actkind: 'L',
        cmpycd: authStore.cmpycd,
        fromdt: form_01.fromdt.replace(/-/g, ''),
        todt: form_01.todt.replace(/-/g, ''),
        iogbn: '200'
    });
    grid1?.setData(res.data || []);
    vAlert('조회되었습니다.');
  } catch (e) { vAlertError('조회 실패'); }
}

async function fetchDetail(row: any) {
  Object.assign(form_02, row);
  try {
    const res = await api.post(`/hsio/HSIO_501U_STR`, {
        actkind: 'S',
        cmpycd: authStore.cmpycd,
        iogbn: '200',
        ioym: row.ioym,
        iono: row.iono,
        ioqty: 0, ioamt: 0, iovat: 0
    });
    setGridDataWithPadding(res.data || []);
    updateTotalSum();
  } catch (e) { vAlertError('상세 로드 실패'); }
}

async function save() {
  if (isClosed.value) return vAlertError('마감된 월입니다.');
  if (!form_02.custcd) return vAlertError('거래처를 선택하세요.');

  const details = grid2?.getData().filter(r => r._state !== 'EMPTY' && r._status) || [];
  if (!details.length && !form_02.iono) return vAlertError('저장할 품목이 없습니다.');

  try {
    const ioymd = form_02.ioymd.replace(/-/g, '');
    const mst = {
        ...form_02,
        iogbn: '200',
        iotype: '100',
        actkind: form_02.iono ? 'U' : 'A',
        ioym: ioymd.substring(0, 6),
        ioymd: ioymd,
        endymd: form_02.endymd.replace(/-/g, ''),
        saleuserid: form_02.sale_userid,
        updemp: authStore.userid
    };

    const dtl = details.map(d => ({
        ...d,
        iogbn: form_02.iogbn,
        deptcd: form_02.deptcd,
        custcd: form_02.custcd,
        whcd: form_02.whcd,
        ioymd: ioymd,
        iotype: '100',
        saleuserid: form_02.sale_userid,
        ioqty: Number(d.ioqty || 0),
        ioamt: Number(d.ioamt || 0),
        iovat: Number(d.iovat || 0),
        cfmyn: 'Y',
        actkind: d._status === '신규' ? 'A' : (d._status === '삭제' ? 'D' : 'U'),
        updemp: authStore.userid
    }));

    await api.post('/hsio/HSIO_500U_SAVE', { mst, dtl });
    vAlert('저장되었습니다.'); search();
  } catch (e) { vAlertError('저장 실패'); }
}

function addRow() {
  grid2?.addRow({ _status: '신규', ioqty: 0, price: 0, ioamt: 0, iovat: 0, sumamt: 0, _state: 'NEW' }, true);
}

function deleteSelectedRows() {
  const selected = grid2?.getSelectedRows();
  if (!selected?.length) return vAlertError('삭제할 행을 선택하십시오.');
  selected.forEach(row => handleRowAction(row));
}

function initialize() {
  resetForm(form_02);
  form_02.cmpycd = authStore.cmpycd;
  form_02.ioymd = today;
  form_02.endymd = today;
  if (whOptions.value.length > 0) form_02.whcd = whOptions.value[0].whcd;
  form_02.deptcd = authStore.deptcd;
  form_02.deptnm = authStore.deptnm;

  // 영업담당 및 배송담당 본인 기본 설정
  form_02.sale_userid = authStore.userid;
  form_02.trnemp = authStore.userid;
  form_02.paycndt = '100';
  form_02.totsum = 0;
  form_02.janamt = 0;
  form_02.rcvdd = '';
  form_02.ioym = '';
  grid1?.clearData();
  setGridDataWithPadding([]);
}

async function handleFullDelete() {
  if (!form_02.iono) return;
  if (isClosed.value) return vAlertError('마감된 월입니다.');
  if (!confirm('정말 전체 삭제하시겠습니까?')) return;
  try {
    await api.post('/hsio/HSIO_500U_STR', {
        ...form_02,
        actkind: 'D',
        iogbn: '200',
        ioymd: form_02.ioymd.replace(/-/g, ''),
        cfmyn: 'Y',
        saleuserid: form_02.sale_userid
    });
    vAlert('삭제되었습니다.'); initialize(); search();
  } catch (e) { vAlertError('삭제 실패'); }
}

const formatNumber = (n: any) => Number(n || 0).toLocaleString();

onMounted(async () => {
  await fetchWhOptions();
  api.post('/ha00/HA00_00P_STR', { gubun: 'SD', cmpycd: authStore.cmpycd, gbncd: '', code: '', remark: '' }).then(r => { userData.value = r.data; });

  api.post('/hs00/HS00_000S_STR', {
      gubun: 'E0',
      cmpycd: authStore.cmpycd,
      gbncd: '300',
      code: ''
  }).then(r => {
      paycndtData.value = r.data;
  });

  api.get('/hp00/HP00_000S_STR', { params: { gubun: 'CL', cmpycd: authStore.cmpycd } }).then(r => {
    if(r.data?.length) closingInfo.sclsym = r.data[0].sclsym;
  });
  nextTick(initGrids);
  initialize();
})

onUnmounted(() => { searchStore.removeTab(route.name as string) });
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
