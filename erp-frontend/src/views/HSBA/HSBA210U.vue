<!--
	=============================================================
	프로그램명	: 행사단가관리 (HSBA210U)
	작성일자	: 2025.03.14
	설명        : 행사 품의 및 단가 관리 (HSOD100U 디자인 표준 적용)
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
        기본정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        행사관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">행사단가관리 (HSBA210U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-save" @click="save">저장</button>
        <button class="btn-erp btn-delete" @click="handleFullDelete" :disabled="!form_02.rowno || form_02.rowno === '000'">전체삭제</button>
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
                <th class="text-center bg-light">품의연월</th>
                <td class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                    <input v-model="form_01.yymm" type="month" class="form-control form-control-sm" style="width: 150px;" @change="search" />
                </td>
                <th class="text-center bg-light border-start">품의부서</th>
                <td>
                    <div class="input-group input-group-sm" style="width: 300px;">
                        <input v-model="form_01.deptnm" class="form-control" placeholder="부서명 검색" @keyup.enter="handleOpenHelp('SEARCH_DEPT')" />
                        <button class="btn btn-outline-secondary" @click="handleOpenHelp('SEARCH_DEPT')"><i class="bi bi-search"></i></button>
                    </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 투-그리드 레이아웃 영역 -->
      <div class="d-flex gap-2 flex-grow-1 overflow-hidden" style="min-height: 0;">

        <!-- ⬅️ 좌측: 품의 목록 -->
        <div class="card border shadow-sm d-flex flex-column overflow-hidden grid-container-left" style="width: 350px; min-width: 350px;">
          <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark">품의 목록</div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="tableRef1" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>

        <!-- ➡️ 우측: 마스터 상세 폼 + 상세 그리드 -->
        <div class="flex-grow-1 d-flex flex-column gap-2 overflow-hidden">

          <!-- 상세 마스터 정보 폼 -->
          <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
            <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between" style="background-color: #f0f7ff !important;">
                <span class="fw-bold small text-primary"><i class="bi bi-pencil-square me-1"></i>행사 품의 상세</span>
                <div class="d-flex gap-2 align-items-center">
                    <span v-if="form_02.rowno && form_02.rowno !== '000'" class="badge bg-warning text-dark" style="font-size: 10px;">수정 모드 ({{ form_02.yymm }}-{{ form_02.rowno }})</span>
                    <span v-else class="badge bg-primary" style="font-size: 10px;">신규 등록</span>
                </div>
            </div>
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
                    <th class="required bg-light">품의부서</th>
                    <td colspan="3">
                      <div class="input-group input-group-sm">
                        <input v-model="form_02.deptnm" class="form-control" @keyup.enter="handleOpenHelp('DEPT')" placeholder="부서명 입력" />
                        <button class="btn btn-outline-secondary" @click="handleOpenHelp('DEPT')" :disabled="form_02.rowno !== '000'"><i class="bi bi-search"></i></button>
                      </div>
                    </td>
                    <th class="bg-light">품의번호</th>
                    <td>
                      <input :value="displayPumNo" class="form-control bg-light text-primary fw-bold text-center" readonly placeholder="자동생성" />
                    </td>
                    <th class="required bg-light border-start">품의일자</th>
                    <td><input v-model="form_02.pumymd" type="date" class="form-control" /></td>
                  </tr>
                  <tr>
                    <th class="required bg-light">행사기간</th>
                    <td colspan="3">
                        <div class="d-flex align-items-center gap-1">
                            <input v-model="form_02.fromdt" type="date" class="form-control" />
                            <span>~</span>
                            <input v-model="form_02.todt" type="date" class="form-control" />
                        </div>
                    </td>
                    <th class="bg-light border-start">완료여부</th>
                    <td class="ps-5">
                        <div class="form-check form-check-inline m-0">
                            <input v-model="form_02.endyn" class="form-check-input" type="checkbox" true-value="Y" false-value="N" id="chkEndyn">
                            <label class="form-check-label small" for="chkEndyn">행사완료</label>
                        </div>
                    </td>
                    <th class="bg-light border-start">승인여부</th>
                    <td class="ps-5">
                        <div class="form-check form-check-inline m-0">
                            <input v-model="form_02.cfmyn" class="form-check-input" type="checkbox" true-value="Y" false-value="N" id="chkCfmyn">
                            <label class="form-check-label small" for="chkCfmyn">승인</label>
                        </div>
                    </td>
                  </tr>
                  <tr>
                    <th class="bg-light">행사내역</th>
                    <td colspan="7">
                        <textarea v-model="form_02.remark" class="form-control form-control-sm" rows="2" style="resize: none;"></textarea>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- 상세 품목 그리드 영역 -->
          <div class="card border shadow-sm flex-grow-1 d-flex flex-column overflow-hidden grid-container-right">
            <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
              <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>행사 품목 리스트</span>
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
const { modalVisible, modalProps } = useCommonHelp()

// [1] 데이터 모델링
const form_01 = reactive({ yymm: today.substring(0, 7), deptcd: '', deptnm: '' })
const form_02 = reactive<any>({
  actkind: 'S0', cmpycd: authStore.cmpycd,
  yymm: today.replace(/-/g, '').substring(0, 6), rowno: '000',
  deptcd: authStore.deptcd, deptnm: authStore.deptnm,
  pumymd: today, fromdt: firstDay, todt: today,
  cfmyn: 'N', endyn: 'N', remark: ''
})

const displayPumNo = computed(() => (!form_02.rowno || form_02.rowno === '000') ? '' : `${form_02.yymm}-${form_02.rowno}`)

const tableRef1 = ref<HTMLDivElement | null>(null); const tableRef2 = ref<HTMLDivElement | null>(null)
let grid1: Tabulator | null = null; let grid2: Tabulator | null = null

const initGrids = () => {
  grid1 = new Tabulator(tableRef1.value!, {
    layout: "fitColumns", height: "100%", placeholder: "데이터 없음",
    columns: [
      { title: "No", formatter: "rownum", width: 40, hozAlign: "center", headerSort: false },
      { title: "부서명", field: "deptnm", hozAlign: "left", headerSort: false },
      { title: "품의번호", hozAlign: "center", width: 120, cssClass: "fw-bold text-primary", headerSort: false, formatter: (c) => {
          const d = c.getData() || {}; return d.yymm && d.rowno ? `${d.yymm}-${d.rowno}` : (d.pumno_full || '');
      }}
    ],
  });
  grid1.on("rowClick", (e, row) => fetchDetail(row.getData()));

  grid2 = new Tabulator(tableRef2.value!, {
    layout: "fitColumns", height: "100%", placeholder: "품목 없음", selectable: true,
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "선택", width: 40, hozAlign: "center", formatter: "rowSelection", titleFormatter: "rowSelection" },
      { title: "상태", field: "_status", width: 60, hozAlign: "center", formatter: (c) => {
          const v = c.getValue() || '';
          if (v === '입력') return '<span class="badge bg-primary">입력</span>';
          if (v === '수정') return '<span class="badge bg-warning text-dark">수정</span>';
          if (v === '삭제') return '<span class="badge bg-danger">삭제</span>';
          return '';
      }},
      { title: "품목명", field: "itemnm", minWidth: 200, widthGrow: 1, cssClass: 'fw-bold text-primary', cellClick: (e, cell) => handleOpenHelp('ITEM', cell.getRow()) },
      { title: "규격", field: "itsize", width: 150, hozAlign: "left" },
      { title: "단위", field: "unit", width: 70, hozAlign: "center" },
      { title: "기준수량", field: "lmtqty", width: 100, hozAlign: "right", editor: "number", formatter: "money", formatterParams: { precision: 0 }, cellEdited: (cell) => {
          const d = cell.getRow().getData();
          if (d._state === 'EXIST' && d._status !== '삭제') cell.getRow().update({ _status: '수정' });
      }},
      { title: "단가", field: "price", width: 120, hozAlign: "right", editor: "number", formatter: "money", formatterParams: { precision: 0 }, cellEdited: (cell) => {
          const d = cell.getRow().getData();
          if (d._state === 'EXIST' && d._status !== '삭제') cell.getRow().update({ _status: '수정' });
      }},
      { title: "재고수량", field: "qty", width: 100, hozAlign: "right" },
      { title: "비고", field: "bigo", width: 200, hozAlign: "left", editor: "text", cellEdited: (cell) => {
          const d = cell.getRow().getData();
          if (d._state === 'EXIST' && d._status !== '삭제') cell.getRow().update({ _status: '수정' });
      }},
      { title: "삭제", width: 40, hozAlign: "center", formatter: (c) => "<i class='bi bi-trash text-danger'></i>", cellClick: (e, cell) => handleRowAction(cell.getRow()) }
    ]
  });
}

async function search() {
  try {
    const res = await api.post('/hsba/HSBA_210U_STR', {
        actkind: 'S1', cmpycd: authStore.cmpycd,
        yymm: form_01.yymm.replace(/-/g, ''),
        deptcd: form_01.deptcd, rowno: '000'
    });
    grid1?.setData(res.data || []);
    vAlert('조회되었습니다.');
  } catch (e: any) { vAlertError('조회 실패'); }
}

async function fetchDetail(row: any) {
  const n = row;
  const fYmd = (d: string) => d && d.length === 8 ? `${d.substring(0, 4)}-${d.substring(4, 6)}-${d.substring(6, 8)}` : today;

  Object.assign(form_02, {
      ...n,
      pumymd: fYmd(n.pumymd),
      fromdt: fYmd(n.fromdt),
      todt: fYmd(n.todt),
      cfmyn: n.cfmyn === 'Y' ? 'Y' : 'N',
      endyn: n.endyn === 'Y' ? 'Y' : 'N'
  });

  try {
    const res = await api.post('/hsba/HSBA_211U_STR', {
        actkind: 'S0',
        cmpycd: authStore.cmpycd,
        yymm: n.yymm,
        rowno: n.rowno
    });
    grid2?.setData((res.data || []).map((i: any) => ({ ...i, _state: 'EXIST', _status: '' })));
  } catch (e: any) { vAlertError('상세 로드 실패'); }
}

async function save() {
  if (!form_02.deptcd) return vAlertError('품의부서를 선택하세요.');
  const details = grid2?.getData().filter((r: any) => r._status) || [];
  if (!details.length && form_02.rowno === '000') return vAlertError('항목을 추가하세요.');

  try {
    const mst = {
        ...form_02,
        actkind: form_02.rowno === '000' ? 'A0' : 'U0',
        pumymd: form_02.pumymd.replace(/-/g, ''),
        fromdt: form_02.fromdt.replace(/-/g, ''),
        todt: form_02.todt.replace(/-/g, ''),
        userid: authStore.userid
    };
    const dtl = details.map((d: any) => ({
        ...d,
        actkind: d._status === '입력' ? 'A0' : (d._status === '삭제' ? 'D0' : 'U0'),
        userid: authStore.userid
    }));

    const res = await api.post('/hsba/HSBA_210U_SAVE', { mst, dtl });
    if (res.data?.[0]?.result === 'N') return vAlertError(res.data[0].msg || '저장 실패');
    vAlert('저장되었습니다.'); search();
  } catch (e: any) { vAlertError('저장 오류'); }
}

const handleOpenHelp = async (type: string, target?: any) => {
  const commonPath = '/ha00/HA00_00P_STR'

  if (type === 'SEARCH_DEPT' || type === 'DEPT') {
    const isSearch = type === 'SEARCH_DEPT'
    const searchValue = isSearch ? form_01.deptnm : form_02.deptnm
    const payload = { gubun: 'D0', cmpycd: authStore.cmpycd, gbncd: '', code: searchValue, remark: '' }

    try {
      const res = await api.post(commonPath, payload)
      const data = res.data || []

      if (data.length === 1) {
        const n = data[0]
        if (isSearch) {
          form_01.deptcd = n.deptcd; form_01.deptnm = n.deptnm; search();
        } else {
          form_02.deptcd = n.deptcd; form_02.deptnm = n.deptnm;
        }
      } else {
        Object.assign(modalProps, {
          title: '부서 선택',
          path: commonPath,
          data: payload,
          columns: [
            { title: '부서코드', field: 'deptcd', width: 120, hozAlign: 'center' },
            { title: '부서명', field: 'deptnm', minWidth: 200, widthGrow: 1, hozAlign: 'left' }
          ],
          onConfirm: (d: any) => {
            const n = d
            if (isSearch) {
              form_01.deptcd = n.deptcd; form_01.deptnm = n.deptnm; search();
            } else {
              form_02.deptcd = n.deptcd; form_02.deptnm = n.deptnm;
            }
          },
          type: 'table'
        })
        modalVisible.value = true
      }
    } catch (e) { vAlertError('조회 중 오류 발생') }

  } else if (type === 'ITEM') {
    const searchValue = target?.getData().itemnm || ''
    const itemPath = '/hs00/HS00_000S_STR'
    const payload = { gubun: 'I1', gbncd: 'COIT', code: '1', cmpycd: authStore.cmpycd, search: searchValue }

    try {
      const res = await api.post(itemPath, payload)
      const data = res.data || []

      if (data.length === 1) {
        const n = data[0]
        target.update({
          itemcd: n.itemcd, itemnm: n.itemnm, itsize: n.itsize, unit: n.unit || 'EA',
          price: n.outcost || 0, qty: n.qty || 0, _status: '입력', _state: 'NEW'
        })
      } else {
        Object.assign(modalProps, {
          title: '품목 선택',
          path: itemPath,
          data: payload,
          columns: [
            { title: '품목코드', field: 'itemcd', width: 100 },
            { title: '품목명', field: 'itemnm', width: 200, hozAlign: 'left' },
            { title: '규격', field: 'itsize', width: 150 },
            { title: '단위', field: 'unit', width: 80 }
          ],
          onConfirm: (d: any) => {
            const n = d
            target.update({
              itemcd: n.itemcd, itemnm: n.itemnm, itsize: n.itsize, unit: n.unit || 'EA',
              price: n.outcost || 0, qty: n.qty || 0, _status: '입력', _state: 'NEW'
            })
          },
          type: 'table'
        })
        modalVisible.value = true
      }
    } catch (e) { vAlertError('조회 중 오류 발생') }
  }
}

const handleRowAction = (row: any) => { const d = row.getData(); if (d._state === 'NEW') row.delete(); else row.update({ _status: d._status === '삭제' ? '' : '삭제' }); }
const deleteSelectedRows = () => { const sel = grid2?.getSelectedRows(); if (sel?.length) sel.forEach(row => handleRowAction(row)); }
const addRow = () => grid2?.addRow({ price: 0, lmtqty: 0, _status: '입력', _state: 'NEW' }, true);

const initialize = () => {
    resetForm(form_02);
    Object.assign(form_02, {
        cmpycd: authStore.cmpycd,
        rowno: '000',
        yymm: today.replace(/-/g, '').substring(0, 6),
        pumymd: today, fromdt: firstDay, todt: today,
        cfmyn: 'N', endyn: 'N',
        deptcd: authStore.deptcd, deptnm: authStore.deptnm
    });
    grid1?.clearData(); grid2?.clearData();
}

async function handleFullDelete() {
  if (!confirm('정말 전체 삭제하시겠습니까?')) return;
  try {
    const res = await api.post('/hsba/HSBA_210U_SAVE', {
        mst: { ...form_02, actkind: 'D0' },
        dtl: []
    });
    if (res.data?.[0]?.result === 'N') return vAlertError(res.data[0].msg || '삭제 실패');
    vAlert('삭제되었습니다.'); initialize(); search();
  } catch (e) { vAlertError('삭제 실패'); }
}

onMounted(() => {
    nextTick(initGrids);
    initialize();
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
