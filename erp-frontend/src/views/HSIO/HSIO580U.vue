<!--
	=============================================================
	프로그램명	: 유통상품이동출고 (HSIO580U)
	작성일자	: 2025.02.24
	설명        : 창고/부서 간 상품 이동 출고 관리 (HSIO_580U_SAVE 통합 저장 및 직관적 API 구성)
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
        영업관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        출고관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">유통상품이동출고 (HSIO580U)</span>
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
                <th class="text-center bg-light">이동일자</th>
                <td class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                  <DateForm v-model:fromdt="form_01.fromdt" v-model:todt="form_01.todt" />
                </td>
                <th class="text-center bg-light">출고창고</th>
                <td>
                  <select v-model="form_01.owhcd" class="form-select form-select-sm w-50">
                    <option value="000">전체</option>
                    <option v-for="item in owhcdData" :key="item.whcd" :value="item.whcd">{{ item.whnm }}</option>
                  </select>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 투-그리드 레이아웃 영역 -->
      <div class="d-flex gap-2 flex-grow-1 overflow-hidden" style="min-height: 0;">

        <!-- ⬅️ 좌측: 이동출고 목록 -->
        <div class="card border shadow-sm d-flex flex-column overflow-hidden grid-container-left" style="width: 350px; min-width: 350px;">
          <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark">이동 목록</div>
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
                    <th class="required bg-light">출고부서</th>
                    <td>
                      <div class="input-group input-group-sm">
                        <input v-model="form_02.odeptnm" class="form-control" readonly />
                        <button class="btn btn-outline-secondary" @click="handleOpenHelp('ODEPT')" :disabled="isClosed"><i class="bi bi-search"></i></button>
                      </div>
                    </td>
                    <th class="bg-light text-center">출고번호</th>
                    <td><input :value="displayIoNo" class="form-control bg-light text-primary fw-bold text-center" readonly placeholder="자동생성" /></td>
                    <th class="required bg-light text-center">출고일자</th>
                    <td><input v-model="form_02.ioymd" type="date" class="form-control" :readonly="isClosed" /></td>
                    <th class="bg-light text-center">입고번호</th>
                    <td><input v-model="form_02.ino" class="form-control bg-light text-center" readonly /></td>
                  </tr>
                  <tr>
                    <th class="required bg-light">출고창고</th>
                    <td>
                      <select v-model="form_02.owhcd" class="form-select" :disabled="isClosed">

                        <option v-for="item in owhcdData" :key="item.whcd" :value="item.whcd">{{ item.whnm }}</option>
                      </select>
                    </td>
                    <th class="required bg-light text-center">입고창고</th>
                    <td>
                      <select v-model="form_02.iwhcd" class="form-select" :disabled="isClosed">
                        <option v-for="item in iwhcdData" :key="item.whcd" :value="item.whcd">{{ item.whnm }}</option>
                      </select>
                    </td>
                    <th class="required bg-light text-center">입고부서</th>
                    <td colspan="3">
                      <div class="input-group input-group-sm">
                        <input v-model="form_02.ideptnm" class="form-control" readonly />
                        <button class="btn btn-outline-secondary" @click="handleOpenHelp('IDEPT')" :disabled="isClosed"><i class="bi bi-search"></i></button>
                      </div>
                    </td>
                  </tr>
                  <tr>
                    <th class="bg-light text-center">특기사항</th>
                    <td colspan="7"><input v-model="form_02.remark" class="form-control" :readonly="isClosed" /></td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- 상세 품목 그리드 영역 -->
          <div class="card border shadow-sm flex-grow-1 d-flex flex-column overflow-hidden grid-container-right">
            <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
              <span class="fw-bold small text-dark d-flex align-items-center"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>이동 품목 리스트</span>
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
import { reactive, ref, onMounted, computed, nextTick } from 'vue'
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
import type { ModalProps } from '@/types/modal'

const authStore = useAuthStore()
const { firstDay, today } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

// [1] 데이터 모델링
const form_01 = reactive({ fromdt: firstDay, todt: today, owhcd: '000' })
const form_02 = reactive<any>({
  actkind: 'L', cmpycd: authStore.cmpycd,
  odeptcd: authStore.deptcd, odeptnm: authStore.deptnm,
  ino: '', cfmyn: '', ioym: '', iono: '', ioymd: today,
  owhcd: '000', iwhcd: '000', ideptcd: '', ideptnm: '', remark: '', sts: 'N'
})

const displayIoNo = computed(() => (!form_02.iono || form_02.iono === '000000') ? '' : `${form_02.ioym}-${form_02.iono}`)

const closingInfo = reactive({ sclsym: '' })
const owhcdData = ref<any[]>([])
const iwhcdData = ref<any[]>([])

const tableRef1 = ref<HTMLDivElement | null>(null)
const tableRef2 = ref<HTMLDivElement | null>(null)
let grid1: Tabulator | null = null
let grid2: Tabulator | null = null

const isClosed = computed(() => {
  if (!closingInfo.sclsym || !form_02.ioymd) return false
  return form_02.ioymd.replace(/-/g, '').substring(0, 6) <= closingInfo.sclsym
})

const initGrids = () => {
  grid1 = new Tabulator(tableRef1.value!, {
    layout: "fitColumns", height: "100%", placeholder: "데이터 없음",
    columns: [
      { title: "No", formatter: "rownum", width: 40, hozAlign: "center", headerSort: false },
      { title: "출고일자", field: "ioymd", hozAlign: "center", width: 100, headerSort: false, formatter: (c) => {
          const v = c.getValue(); return v && v.length === 8 ? `${v.substring(0,4)}-${v.substring(4,6)}-${v.substring(6,8)}` : v;
      }},
      { title: "출고번호", field: "iono_full", hozAlign: "center", width: 110, cssClass: "fw-bold text-primary", headerSort: false },
      { title: "출고부서", field: "odeptnm", hozAlign: "center", width: 110, headerSort: false }
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
          if (v === '입력') return '<span class="badge bg-primary">신규</span>';
          if (v === '수정') return '<span class="badge bg-warning text-dark">수정</span>';
          if (v === '삭제') return '<span class="badge bg-danger">삭제</span>';
          return '';
      }},
      { title: "품목명", field: "itemnm", minWidth: 200, widthGrow: 1, cssClass: 'fw-bold text-primary',
        cellClick: (e, cell) => { if(!isClosed.value) handleOpenHelp('ITEM', cell.getRow()) }
      },
      { title: "규격", field: "itsize", width: 150 },
      { title: "단위", field: "unit", width: 80, hozAlign: "center" },
      { title: "수량", field: "ioqty", width: 100, hozAlign: "right", editor: "number", cellEdited: (cell) => calcRow(cell.getRow()) },
      { title: "삭제", width: 40, hozAlign: "center", formatter: () => "<i class='bi bi-trash text-danger'></i>",
        cellClick: (e, cell) => handleRowAction(cell.getRow())
      }
    ]
  });
}

const calcRow = (row: any) => {
  const d = row.getData();
  if (d._state === 'EXIST' && d._status !== '삭제') row.update({ _status: '수정' });
}

const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

const handleOpenHelp = (type: string, target?: any) => {
  if (isClosed.value) return;
  switch (type) {
    case 'ODEPT': // 출고부서
      Object.assign(modalProps, {
        title: '부서 선택', path: '/ha00/HA00_00P_STR', data: { gubun: 'D0', cmpycd: authStore.cmpycd, codenm: form_02.odeptnm },
        columns: [{ title: '코드', field: 'deptcd', width: 100 }, { title: '부서명', field: 'deptnm', width: 200 }],
        onConfirm: (d: any) => { form_02.odeptcd = d.deptcd; form_02.odeptnm = d.deptnm }
      }); modalVisible.value = true;
      break;

    case 'IDEPT': // 입고부서
      Object.assign(modalProps, {
        title: '부서 선택', path: '/ha00/HA00_00P_STR', data: { gubun: 'D0', cmpycd: authStore.cmpycd, codenm: form_02.ideptnm },
        columns: [{ title: '코드', field: 'deptcd', width: 100 }, { title: '부서명', field: 'deptnm', width: 200 }],
        onConfirm: (d: any) => { form_02.ideptcd = d.deptcd; form_02.ideptnm = d.deptnm }
      }); modalVisible.value = true;
      break;

    case 'ITEM': // 품목
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
            itemcd: d.itemcd,
            itemnm: d.itemnm,
            itsize: d.itsize,
            unit: d.unit || 'EA',
            ioqty: 1,
            _status: '입력',
            _state: 'NEW'
          });
        }
      }); modalVisible.value = true;
      break;
  }
}

const handleRowAction = (row: any) => {
  const d = row.getData();
  if (d._state === 'NEW') row.delete();
  else row.update({ _status: d._status === '삭제' ? '' : '삭제' });
}

async function search() {
  try {
    const res = await api.post('/hsio/HSIO_580U_STR', {
      actkind: 'L',
      cmpycd: authStore.cmpycd,
      iogbn: '200',
      fromdt: form_01.fromdt.replace(/-/g, ''),
      todt: form_01.todt.replace(/-/g, ''),
      owhcd: form_01.owhcd
    });
    const data = (res.data || []).map((item: any) => ({
      ...item,
      iono_full: item.ioym && item.iono ? `${item.ioym}-${item.iono}` : ''
    })).filter((i: any) => i.iono_full);
    grid1?.setData(data);
    vAlert('조회되었습니다.');
  } catch (e) { vAlertError('조회 실패'); }
}

async function fetchDetail(row: any) {
  const fYmd = (d: string) => d && d.length === 8 ? `${d.substring(0, 4)}-${d.substring(4, 6)}-${d.substring(6, 8)}` : today;
  Object.assign(form_02, { ...row, ioymd: fYmd(row.ioymd) });
  try {
    const res = await api.post('/hsio/HSIO_581U_STR', {
      actkind: 'S',
      cmpycd: authStore.cmpycd,
      iogbn: '200',
      ioym: row.ioym,
      iono: row.iono,
      ioqty: 0
    });
    const data = (res.data || []).map((item: any) => ({
      ...item,
      _state: 'EXIST',
      _status: ''
    }));
    grid2?.setData(data);
  } catch (e) { vAlertError('상세 로드 실패'); }
}

async function save() {
  if (isClosed.value) return vAlertError('마감된 월입니다.');
  if (!form_02.odeptcd || !form_02.owhcd || !form_02.ideptcd || !form_02.iwhcd) return vAlertError('출고/입고 부서와 창고를 확인하세요.');

  const details = grid2?.getData().filter(r => r._status) || [];
  if (!details.length && (!form_02.iono || form_02.iono === '000000')) return vAlertError('항목을 추가하세요.');

  // 🚀 저장 시점에만 ioym을 출고일자(ioymd) 기준으로 추출하여 전송 (마스터 키 원칙 준수)
  const targetIoym = form_02.ioymd.replace(/-/g, '').substring(0, 6);

  try {
    const payload = {
      mst: {
        ...form_02,
        actkind: !form_02.iono || form_02.iono === '000000' ? 'A' : 'U',
        ioym: targetIoym,
        ioymd: form_02.ioymd.replace(/-/g, ''),
        iogbn: '200',
        updemp: authStore.userid
      },
      dtl: details.map((item: any) => ({
        ...item,
        actkind: item._status === '입력' ? 'A' : (item._status === '삭제' ? 'D' : 'U'),
        cmpycd: authStore.cmpycd,
        iogbn: '200',
        ioym: targetIoym,
        ono: form_02.iono,
        ino: form_02.ino,
        odeptcd: form_02.odeptcd,
        owhcd: form_02.owhcd,
        ideptcd: form_02.ideptcd,
        iwhcd: form_02.iwhcd,
        ioymd: form_02.ioymd.replace(/-/g, ''),
        updemp: authStore.userid
      }))
    };

    await api.post('/hsio/HSIO_580U_SAVE', payload);
    vAlert('저장되었습니다.');
    search();
  } catch (e) {
    vAlertError('저장 실패');
  }
}

function addRow() {
  if (isClosed.value) return;
  grid2?.addRow({ ioqty: 0, _status: '입력', _state: 'NEW' }, true);
}

function deleteSelectedRows() {
  grid2?.getSelectedRows().forEach(row => handleRowAction(row));
}

function initialize() {
  resetForm(form_02);
  Object.assign(form_02, { cmpycd: authStore.cmpycd, odeptcd: authStore.deptcd, odeptnm: authStore.deptnm, ioymd: today, ioym: '', iono: '', ino: '', sts: 'N', owhcd: '100' });
  if (grid1) grid1.setData([]);
  if (grid2) grid2.setData([]);
}

async function handleFullDelete() {
  if (!confirm('정말 전체 삭제하시겠습니까?')) return;
  try {
    await api.post('/hsio/HSIO_580U_STR', {
      ...form_02,
      actkind: 'D',
      iogbn: '200'
    });
    vAlert('삭제되었습니다.'); initialize(); search();
  } catch (e) { vAlertError('삭제 실패'); }
}

onMounted(async () => {
    nextTick(initGrids);
    api.post('/hs00/HS00_000S_STR', { gubun: 'W0', cmpycd: authStore.cmpycd }).then(r => { owhcdData.value = r.data; iwhcdData.value = r.data; });
    api.post('/hp00/HP00_000S_STR', { gubun: 'CL', cmpycd: authStore.cmpycd }).then(r => { if(r.data?.length) closingInfo.sclsym = r.data[0].sclsym; });
    initialize();
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
