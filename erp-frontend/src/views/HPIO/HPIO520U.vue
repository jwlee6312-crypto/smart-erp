<!--
	=============================================================
	프로그램명	: 재공품 타계정출고 (HPIO520U)
	작성일자	: 2025.02.25
	설명        : 생산 공정 내 재공품의 타계정 출고 관리 (HPIO510U 디자인 참조)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-box-seam me-2 text-primary" style="font-size: 18px;"></i>
        생산관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        재고관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">재공품 타계정출고 (HPIO520U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-save" @click="saveData">저장</button>
        <button class="btn-erp btn-delete" @click="deleteData" :disabled="!masterData.iono || masterData.iono === '0000'">전체삭제</button>
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
                <th class="text-center bg-light">생산라인</th>
                <td>
                  <select v-model="searchParam.linecd" class="form-select form-select-sm w-50">
                    <option value="">전체</option>
                    <option v-for="opt in lineOptions" :key="opt.linecd" :value="opt.linecd">{{ opt.linenm }}</option>
                  </select>
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
          <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark">이관 목록</div>
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
                    <th class="required bg-light text-center">생산라인</th>
                    <td>
                      <select v-model="masterData.linecd" class="form-select" @change="onLineChange">
                        <option v-for="opt in lineOptions" :key="opt.linecd" :value="opt.linecd">{{ opt.linenm }}</option>
                      </select>
                    </td>
                    <th class="required bg-light text-center">공정명</th>
                    <td>
                      <select v-model="masterData.progcd" class="form-select">
                        <option v-for="opt in progOptions" :key="opt.progcd" :value="opt.progcd">{{ opt.prognm }}</option>
                      </select>
                    </td>
                    <th class="bg-light text-center">출고번호</th>
                    <td><input :value="displayIoNo" class="form-control bg-light text-primary fw-bold text-center" readonly placeholder="자동생성" /></td>
                    <th class="required bg-light text-center">출고일자</th>
                    <td><input v-model="ioymd_f" type="date" class="form-control" /></td>
                  </tr>
                  <tr>
                    <th class="bg-light text-center">특기사항</th>
                    <td colspan="7"><input v-model="masterData.remark" class="form-control" /></td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- 상세 품목 그리드 -->
          <div class="card border shadow-sm flex-grow-1 d-flex flex-column overflow-hidden grid-container-right">
            <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
              <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>재공품 출고 품목 리스트</span>
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
import { useCommonHelp } from '@/composables/useCommonHelp'
import { getDate } from '@/composables/useDate'
import type { ModalProps } from '@/types/modal'

const authStore = useAuthStore()
const { firstDay, today } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps } = useCommonHelp()

const searchParam = reactive({ fromdt: firstDay, todt: today, linecd: '010' })
const masterData = reactive<any>({
  actkind: 'S0', cmpycd: authStore.cmpycd,
  ioym: today.replace(/-/g, '').substring(0, 6), iono: '0000',
  linecd: '010', progcd: '', ioymd: today.replace(/-/g, ''),
  remark: '', slipno: ''
})

const displayIoNo = computed(() => (!masterData.iono || masterData.iono === '0000') ? '' : `${masterData.ioym}-${masterData.iono}`)
const ioymd_f = computed({ get: () => formatDate(masterData.ioymd), set: (v) => { if (v) masterData.ioymd = v.replace(/-/g, '') } })

const closingInfo = reactive({ clsymd: '', sclsym: '' })
const lineOptions = ref<any[]>([])
const progOptions = ref<any[]>([])

const tableRef1 = ref<HTMLDivElement | null>(null); const tableRef2 = ref<HTMLDivElement | null>(null)
let grid1: Tabulator | null = null; let grid2: Tabulator | null = null

const isClosed = computed(() => {
  if (!closingInfo.sclsym || !masterData.ioymd) return false
  return masterData.ioymd.substring(0, 6) <= closingInfo.sclsym
})

const initGrids = () => {
  grid1 = new Tabulator(tableRef1.value!, {
    layout: "fitColumns", height: "100%", placeholder: "데이터 없음", selectable: 1,
    columns: [
      { title: "No", formatter: "rownum", width: 40, hozAlign: "center", headerSort: false },
      { title: "출고일자", field: "ioymd", hozAlign: "center", width: 100, formatter: (c) => formatDate(c.getValue()) },
      { title: "출고번호", field: "iono_full", hozAlign: "center", width: 110, cssClass: "fw-bold text-primary", headerSort: false },
      { title: "생산라인", field: "linenm", hozAlign: "left", headerSort: false }
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
      { title: "유형", field: "iotypenm", width: 150, cellClick: (e, cell) => handleOpenHelp('iotype', cell.getRow()) },
      { title: "재공품명", field: "itemnm", minWidth: 200, widthGrow: 1, cssClass: 'fw-bold text-primary', cellClick: (e, cell) => handleOpenHelp('ITEM', cell.getRow()) },
      { title: "규격", field: "itsize", width: 180 },
      { title: "단위", field: "unit", width: 70, hozAlign: "center" },
      { title: "수량", field: "ioqty", width: 100, hozAlign: "right", editor: "number", cssClass: "bg-light-yellow fw-bold",
        cellEdited: (cell) => { const d = cell.getData(); if (d._state === 'EXIST' && d._status !== '삭제') cell.getRow().update({ _status: '수정' }); }
      },
      { title: "사용부서", field: "ideptnm", width: 150, cellClick: (e, cell) => handleOpenHelp('IDEPT', cell.getRow()) },
      { title: "거래처", field: "scustnm", width: 200, cellClick: (e, cell) => handleOpenHelp('SCUST', cell.getRow()) },
      { title: "삭제", width: 40, hozAlign: "center", formatter: () => "<i class='bi bi-trash text-danger'></i>", cellClick: (e, cell) => handleRowAction(cell.getRow()) }
    ]
  });
}

async function search() {
  try {
    const res = await api.post('/hpio/HPIO_520U_STR', {
        actkind: 'L',
        cmpycd: authStore.cmpycd,
        linecd: searchParam.linecd,
        fromdt: searchParam.fromdt.replace(/-/g, ''),
        todt: searchParam.todt.replace(/-/g, '')
    });
    const data = (res.data || []).map((i: any) => ({
      ...i,
      iono_full: `${i.ioym}-${i.iono}`
    }));
    grid1?.setData(data);
    vAlert('조회되었습니다.');
  } catch (e) { vAlertError('조회 실패'); }
}

async function fetchDetail(row: any) {
  const fYmd = (d: string) => d && d.length === 8 ? `${d.substring(0, 4)}-${d.substring(4, 6)}-${d.substring(6, 8)}` : today;
  Object.assign(masterData, { ...row, ioymd: fYmd(row.ioymd) });
  if (row.linecd) onLineChange();

  try {
    const res = await api.post('/hpio/HPIO_521U_STR', { actkind: 'S', cmpycd: authStore.cmpycd, ioym: row.ioym, iono: row.iono, linecd: row.linecd });
    grid2?.setData(res.data.map((i: any) => ({ ...i, _state: 'EXIST', _status: '' })));
  } catch (e) { vAlertError('상세 로드 실패'); }
}

async function saveData() {
  if (masterData.ioymd <= closingInfo.clsymd) return vAlertError('회계 마감된 일자입니다.');
  if (masterData.slipno > '000') return vAlertError('입출가 발행되어 수정할 수 없습니다.');

  const details = grid2?.getData().filter((r: any) => r._status) || [];
  if (!details.length && masterData.iono === '0000') return vAlertError('출고할 항목을 추가하세요.');

  if (!confirm('저장하시겠습니까?')) return

  try {
    const actkind = masterData.iono === '0000' ? 'A' : 'U'
    const resM = await api.post('/hpio/HPIO_520U_STR', { ...masterData, actkind, cmpycd: authStore.cmpycd, userid: authStore.userid });
    const mstData = resM.data?.[0];
    const rowValues = mstData?.returnkeyvalue || Object.values(mstData || {});
    const key1 = (mstData?.ioym || rowValues[0] || '').toString().trim();
    const key2 = (mstData?.iono || rowValues[1] || '').toString().trim();

    if (key1 === '000000') throw new Error(key2 || '업무 규칙 위반으로 저장할 수 없습니다.');
    if (!key1 || !key2) throw new Error('입출 번호를 서버로부터 수신하지 못했습니다.');

    const newIono = key2;

    for (const item of details) {
      const act = item._status === '입력' ? 'A' : (item._status === '삭제' ? 'D' : 'U');
      await api.post('/hpio/HPIO_521U_STR', {
        ...item, actkind: act, cmpycd: authStore.cmpycd,
        ioym: key1, iono: newIono,
        linecd: masterData.linecd, progcd: masterData.progcd, ioymd: masterData.ioymd, userid: authStore.userid
      });
    }

    vAlert('저장되었습니다.'); search();
  } catch (e: any) { vAlertError(e.message || '저장 실패'); }
}

async function deleteData() {
  if (!confirm('현재 출고 내역 전체를 삭제하시겠습니까?')) return
  try {
    await api.post('/hpio/HPIO_520U_STR', { ...masterData, actkind: 'D', cmpycd: authStore.cmpycd });
    vAlert('삭제되었습니다.'); initialize(); search();
  } catch (e) { vAlertError('삭제 실패'); }
}

const handleOpenHelp = (type: string, target?: any) => {
  const commonPath = '/ha00/HA00_00P_STR'
  const hpPath = '/hp00/HP00_000S_STR'

  switch (type) {
    case 'ITEM':
      Object.assign(modalProps, {
        title: '재공품 선택', path: hpPath, defaultField: 'itemnm', large: true,
        data: { gubun: 'I1', cmpycd: authStore.cmpycd, gbncd: '210' },
        columns: [
          { title: '코드', field: 'itemcd', width: 120, hozAlign: 'center' },
          { title: '품목명', field: 'itemnm', minWidth: 250, widthGrow: 1 },
          { title: '규격', field: 'itsize', width: 150 },
          { title: '단위', field: 'unit', width: 80, hozAlign: 'center' }
        ],
        onConfirm: (d: any) => target.update({ itemcd: d.itemcd, itemnm: d.itemnm, itsize: d.itsize, unit: d.unit, _status: '입력', _state: 'NEW' })
      }); modalVisible.value = true;
      break;
    case 'iotype':
      Object.assign(modalProps, {
        title: '출고유형 선택', path: hpPath, data: { gubun: 'E0', cmpycd: authStore.cmpycd, gbncd: '130', code: '1' },
        columns: [{ title: '코드', field: 'code', width: 100 }, { title: '유형명', field: 'cdnm', minWidth: 200 }],
        onConfirm: (d: any) => {
          target.update({ iotype: d.code, iotypenm: d.cdnm });
          if (target.getData()._state === 'EXIST') target.update({ _status: '수정' });
        }
      }); modalVisible.value = true;
      break;
    case 'IDEPT':
      Object.assign(modalProps, {
        title: '사용부서 선택', path: commonPath, data: { gubun: 'D0', cmpycd: authStore.cmpycd },
        columns: [{ title: '코드', field: 'deptcd', width: 100 }, { title: '부서명', field: 'deptnm', minWidth: 200 }],
        onConfirm: (d: any) => target.update({ ideptcd: d.deptcd, ideptnm: d.deptnm })
      }); modalVisible.value = true;
      break;
    case 'SCUST':
      Object.assign(modalProps, {
        title: '거래처 선택', path: commonPath, data: { gubun: 'C4', cmpycd: authStore.cmpycd },
        columns: [{ title: '코드', field: 'custcd', width: 100 }, { title: '거래처명', field: 'custnm', minWidth: 250 }],
        onConfirm: (d: any) => target.update({ scustcd: d.custcd, scustnm: d.custnm })
      }); modalVisible.value = true;
      break;
  }
}

const handleRowAction = (row: any) => { const d = row.getData(); if (d._state === 'NEW') row.delete(); else row.update({ _status: d._status === '삭제' ? '' : '삭제' }); }
const deleteSelectedRows = () => grid2?.getSelectedRows().forEach(r => handleRowAction(r));
const addRow = () => grid2?.addRow({ ioqty: 0, _status: '입력', _state: 'NEW' }, true);

async function fetchLineOptions() {
  try {
    const res = await api.get('/hp00/HP00_000S_STR', { params: { gubun: 'L0', cmpycd: authStore.cmpycd, gbncd: 'Y' } });
    lineOptions.value = res.data;
    if (lineOptions.value.length && !masterData.linecd) masterData.linecd = lineOptions.value[0].linecd;
    onLineChange();
  } catch (e) {}
}

const onLineChange = async () => {
  if (!masterData.linecd) { progOptions.value = []; return; }
  try {
    const res = await api.get('/hp00/HP00_000S_STR', { params: { gubun: 'G0', cmpycd: authStore.cmpycd, gbncd: masterData.linecd } });
    progOptions.value = res.data;
    if (progOptions.value.length && !masterData.progcd) masterData.progcd = progOptions.value[0].progcd;
  } catch (e) {}
}

function initialize() {
  resetForm(masterData);
  Object.assign(masterData, { cmpycd: authStore.cmpycd, linecd: '010', ioym: today.replace(/-/g, '').substring(0, 6), iono: '0000', ioymd: today.replace(/-/g, ''), progcd: '', remark: '' });
  grid1?.clearData(); grid2?.clearData();
}

const formatDate = (v: any) => v && v.length === 8 ? `${v.substring(0, 4)}-${v.substring(4, 6)}-${v.substring(6, 8)}` : v;

onMounted(async () => {
  fetchLineOptions();
  api.post('/hp00/HP00_000S_STR', { gubun: 'CL', cmpycd: authStore.cmpycd }).then(r => { if (r.data?.length) { closingInfo.clsymd = r.data[0].clsymd; closingInfo.sclsym = r.data[0].sclsym; } });
  nextTick(initGrids);
  initialize();
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; font-size: 12px; }
.grid-container-left, .grid-container-right { border-bottom: 3px solid #005a9f !important; }
</style>
