<!--
	=============================================================
	프로그램명	: 타계정출고 (HSIO570U)
	작성일자	: 2025.02.24
	설명        : 타계정 출고 마스터/상세 관리 (HSIO_570U_SAVE 통합 저장 및 직관적 API 구성)
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
        구매관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        출고관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">타계정출고 (HSIO570U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-save" @click="save">저장</button>
        <button class="btn-erp btn-delete" @click="handleFullDelete" :disabled="!masterData.iono || masterData.iono === '0000'">전체삭제</button>
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
                  <DateForm v-model:fromdt="searchParam.fromdt" v-model:todt="searchParam.todt" />
                </td>
                <th class="text-center bg-light">출고부서</th>
                <td>
                  <div class="input-group input-group-sm w-50">
                    <input v-model="searchParam.deptnm" class="form-control" readonly />
                    <button class="btn btn-outline-secondary" @click="handleOpenHelp('DEPT_SCH')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 투-그리드 레이아웃 -->
      <div class="d-flex gap-2 flex-grow-1 overflow-hidden" style="min-height: 0;">

        <!-- ⬅️ 좌측: 목록 그리드 -->
        <div class="card border shadow-sm d-flex flex-column overflow-hidden grid-container-left" style="width: 400px; min-width: 400px;">
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
                    <th class="required bg-light">출고부서</th>
                    <td>
                      <div class="input-group input-group-sm">
                        <input v-model="masterData.deptnm" class="form-control" readonly />
                        <button class="btn btn-outline-secondary" @click="handleOpenHelp('DEPT')"><i class="bi bi-search"></i></button>
                      </div>
                    </td>
                    <th class="bg-light">출고번호</th>
                    <td><input :value="displayIoNo" class="form-control bg-light text-primary fw-bold text-center" readonly placeholder="자동생성" /></td>
                    <th class="required bg-light">출고일자</th>
                    <td><input v-model="masterData.ioymd" type="date" class="form-control" /></td>
                    <th class="required bg-light">출고창고</th>
                    <td>
                      <select v-model="masterData.whcd" class="form-select">
                        <option value="">-- 선택 --</option>
                        <option v-for="opt in whOptions" :key="opt.whcd" :value="opt.whcd">{{ opt.whnm }}</option>
                      </select>
                    </td>
                  </tr>
                  <tr>
                    <th class="bg-light">특기사항</th>
                    <td colspan="7"><input v-model="masterData.remark" class="form-control" /></td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- 상세 품목 그리드 -->
          <div class="card border shadow-sm flex-grow-1 d-flex flex-column overflow-hidden grid-container-right">
            <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
              <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>출고 품목 리스트</span>
              <div class="d-flex gap-1">
                <button class="btn btn-sm btn-outline-primary py-0 px-2 fw-bold" @click="addRow" style="font-size: 11px;">+ 행추가</button>
                <button class="btn btn-sm btn-outline-danger py-0 px-2 fw-bold" @click="deleteSelectedRows" style="font-size: 11px;">- 행삭제</button>
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
import { reactive, ref, onMounted, computed, watch, nextTick } from 'vue'
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

const searchParam = reactive({ fromdt: firstDay, todt: today, deptcd: authStore.deptcd, deptnm: authStore.deptnm })
const masterData = reactive<any>({
  actkind: 'S0',
  ioym: today.replace(/-/g, '').substring(0, 6), iono: '0000',
  deptcd: authStore.deptcd, deptnm: authStore.deptnm,
  ioymd: today, whcd: '', remark: '', sts: 'N'
})

const displayIoNo = computed(() => (!masterData.iono || masterData.iono === '0000') ? '' : `${masterData.ioym}-${masterData.iono}`)

const closingInfo = reactive({ sclsym: '' })
const whOptions = ref<any[]>([])

const tableRef1 = ref<HTMLDivElement | null>(null); const tableRef2 = ref<HTMLDivElement | null>(null)
let grid1: Tabulator | null = null; let grid2: Tabulator | null = null

const isClosed = computed(() => {
  if (!closingInfo.sclsym || !masterData.ioymd) return false
  return masterData.ioymd.replace(/-/g, '').substring(0, 6) <= closingInfo.sclsym
})

const initGrids = () => {
  grid1 = new Tabulator(tableRef1.value!, {
    layout: "fitColumns", height: "100%", placeholder: "데이터 없음",
    columns: [
      { title: "No", formatter: "rownum", width: 40, hozAlign: "center", headerSort: false },
      { title: "출고일자", field: "ioymd", hozAlign: "center", width: 100, formatter: (c) => {
          const v = c.getValue(); return v && v.length === 8 ? `${v.substring(0,4)}-${v.substring(4,6)}-${v.substring(6,8)}` : v;
      }},
      { title: "출고번호", field: "iono_full", hozAlign: "center", width: 100, cssClass: "fw-bold text-primary", headerSort: false },
      { title: "출고부서", field: "deptnm", hozAlign: "center", width: 150, cssClass: "fw-bold text-primary", headerSort: false }
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
      { title: "유형", field: "iotypenm", width: 120, cellClick: (e, cell) => handleOpenHelp('iotype', cell.getRow()) },
      { title: "품목명", field: "itemnm", minWidth: 200, widthGrow: 1, cssClass: 'fw-bold text-primary', cellClick: (e, cell) => handleOpenHelp('ITEM', cell.getRow()) },
      { title: "규격", field: "itsize", width: 120 },
      { title: "단위", field: "unit", width: 70, hozAlign: "center" },
      { title: "수량", field: "ioqty", width: 100, hozAlign: "right", editor: "number", cellEdited: (cell) => calcRow(cell.getRow()) },
      { title: "단가", field: "price", width: 110, hozAlign: "right", editor: "number", cellEdited: (cell) => calcRow(cell.getRow()) },
      { title: "금액", field: "ioamt", width: 120, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "사용부서", field: "usedeptnm", width: 120, cellClick: (e, cell) => handleOpenHelp('IDEPT', cell.getRow()) },
      { title: "삭제", width: 40, hozAlign: "center", formatter: (c) => "<i class='bi bi-trash text-danger'></i>", cellClick: (e, cell) => handleRowAction(cell.getRow()) }
    ]
  });
}

const calcRow = (row: any) => {
  const d = row.getData(); const amt = Math.floor(Number(d.ioqty || 0) * Number(d.price || 0));
  row.update({ ioamt: amt });
  if (d._state === 'EXIST' && d._status !== '삭제') row.update({ _status: '수정' });
}

async function search() {
  try {
    const res = await api.post('/hsio/HSIO_570U_STR', {
        actkind: 'L',
        iogbn: '200',
        deptcd: searchParam.deptcd,
        deptnm: searchParam.deptnm,
        fromdt: searchParam.fromdt.replace(/-/g, ''),
        todt: searchParam.todt.replace(/-/g, '')
    });

    const data = (res.data || []).map((item: any) => ({
      ...item,
      iono_full: `${item.ioym}-${item.iono}`
    }));

    grid1?.setData(data);
    if (data.length > 0) vAlert('조회되었습니다.');
    else vAlert('조회된 자료가 없습니다.');
  } catch (e) {
    vAlertError('조회 실패');
  }
}

async function fetchDetail(row: any) {
  const fYmd = (d: string) => d && d.length === 8 ? `${d.substring(0, 4)}-${d.substring(4, 6)}-${d.substring(6, 8)}` : today;
  Object.assign(masterData, { ...row, ioymd: fYmd(row.ioymd) });
  try {
    const res = await api.post('/hsio/HSIO_571U_STR', {
        actkind: 'S',
        iogbn: '200',
        ioym: row.ioym,
        iono: row.iono,
        ioqty: 0,
        ioamt: 0,
        iovat: 0
     });
    const data = (res.data || []).map((item: any) => {
      // 🚀 서버 필드(usedept)를 그리드 필드(usedeptcd)로 매핑
      return { ...item, usedeptcd: item.usedept || '', _state: 'EXIST', _status: '' };
    });
    grid2?.setData(data);
  } catch (e) { vAlertError('상세 로드 실패'); }
}

async function save() {
  if (isClosed.value) return vAlertError('마감된 월입니다.');
  if (!masterData.deptcd || !masterData.whcd) return vAlertError('부서와 창고를 선택하세요.');

  const details = grid2?.getData().filter((r: any) => r._status) || [];
  if (!details.length && masterData.iono === '0000') return vAlertError('항목을 추가하세요.');

  // 🚀 저장 시점에만 ioym을 출고일자(ioymd) 기준으로 추출하여 전송 (마스터 키 원칙 준수)
  const targetIoym = masterData.ioymd.replace(/-/g, '').substring(0, 6);

  try {
    const payload = {
      mst: {
        ...masterData,
        actkind: masterData.iono === '0000' ? 'A' : 'U',
        ioym: targetIoym,
        ioymd: masterData.ioymd.replace(/-/g, ''),
        iogbn: '200',
        iotype: '300',
        custcd: '',
        inyn: 'N',
        ino: '',
        cfmyn: 'Y',
        userid: authStore.userid
      },
      dtl: details.map((item: any) => ({
        ...item,
        actkind: item._status === '입력' ? 'A' : (item._status === '삭제' ? 'D' : 'U'),
        iogbn: '200',
        ioym: targetIoym,
        deptcd: masterData.deptcd,
        custcd: '',
        whcd: masterData.whcd,
        ioymd: masterData.ioymd.replace(/-/g, ''),
        iovat: 0,
        cfmyn: 'Y',
        userid: authStore.userid,
        usedept: item.usedeptcd
      }))
    };

    await api.post('/hsio/HSIO_570U_SAVE', payload);
    vAlert('저장되었습니다.');
    search();
  } catch (e) {
    vAlertError('저장 실패');
  }
}

const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

const handleOpenHelp = (type: string, target?: any) => {
  if (isClosed.value) return;
  switch (type) {
    case 'DEPT_SCH': // 조회부서
      Object.assign(modalProps, {
        title: '부서 선택', path: '/ha00/HA00_00P_STR', data: { gubun: 'D0', cmpycd: authStore.cmpycd, codenm: searchParam.deptnm },
        columns: [{ title: '코드', field: 'deptcd', width: 100 }, { title: '부서명', field: 'deptnm', width: 200 }],
        onConfirm: (d: any) => { searchParam.deptcd = d.deptcd; searchParam.deptnm = d.deptnm }
      }); modalVisible.value = true;
      break;

    case 'DEPT': // 마스터부서
      Object.assign(modalProps, {
        title: '부서 선택', path: '/ha00/HA00_00P_STR', data: { gubun: 'D0', cmpycd: authStore.cmpycd, codenm: masterData.deptnm },
        columns: [{ title: '코드', field: 'deptcd', width: 100 }, { title: '부서명', field: 'deptnm', width: 200 }],
        onConfirm: (d: any) => { masterData.deptcd = d.deptcd; masterData.deptnm = d.deptnm }
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
            price: d.incost || 0,
            ioqty: 1,
            ioamt: d.incost || 0,
            _status: '입력',
            _state: 'NEW'
          });
        }
      }); modalVisible.value = true;
      break;

    case 'iotype': // 출고유형
      Object.assign(modalProps, {
        title: '출고유형 선택', path: '/hs00/HS00_000S_STR', data: { gubun: 'E0', gbncd: '130', cmpycd: authStore.cmpycd, code: '1' },
        columns: [{ title: '코드', field: 'code', width: 100 }, { title: '유형명', field: 'cdnm', width: 200 }],
        onConfirm: (d: any) => { target.update({ iotype: d.code, iotypenm: d.cdnm }); }
      }); modalVisible.value = true;
      break;

    case 'IDEPT': // 사용부서
      Object.assign(modalProps, {
        title: '부서 선택', path: '/ha00/HA00_00P_STR', data: { gubun: 'D0', cmpycd: authStore.cmpycd },
        columns: [{ title: '코드', field: 'deptcd', width: 100 }, { title: '부서명', field: 'deptnm', width: 200 }],
        onConfirm: (d: any) => { target.update({ usedeptcd: d.deptcd, usedeptnm: d.deptnm }); }
      }); modalVisible.value = true;
      break;
  }
}

const handleRowAction = (row: any) => { const d = row.getData(); if (d._state === 'NEW') row.delete(); else row.update({ _status: d._status === '삭제' ? '' : '삭제' }); }
const deleteSelectedRows = () => { const sel = grid2?.getSelectedRows(); if (sel?.length) sel.forEach(row => handleRowAction(row)); }
const addRow = () => grid2?.addRow({ usedeptcd: '', usedeptnm: '', ioqty: 0, price: 0, ioamt: 0, _status: '입력', _state: 'NEW' }, true);

function initialize() {
  resetForm(masterData);
  Object.assign(masterData, { cmpycd: authStore.cmpycd, iono: '0000', ioymd: today, ioym: today.replace(/-/g, '').substring(0, 6), deptcd: authStore.deptcd, deptnm: authStore.deptnm, sts: 'N' });
  if (grid1) grid1.setData([]);
  if (grid2) grid2.setData([]);
}

async function handleFullDelete() {
  if (!confirm('정말 전체 삭제하시겠습니까?')) return;
  try {
    await api.post('/hsio/HSIO_570U_STR', {
        ...masterData,
        actkind: 'D',
        iogbn: '200',
        cfmyn: 'Y'
    });
    vAlert('삭제되었습니다.'); initialize(); search();
  } catch (e) { vAlertError('삭제 실패'); }
}

onMounted(async () => {
    nextTick(initGrids);
    api.post('/hs00/HS00_000S_STR', { gubun: 'W0', cmpycd: authStore.cmpycd }).then(r => { whOptions.value = r.data; });
    api.post('/hp00/HP00_000S_STR', { gubun: 'CL', cmpycd: authStore.cmpycd }).then(r => { if(r.data?.length) closingInfo.sclsym = r.data[0].sclsym; });
    initialize();
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
