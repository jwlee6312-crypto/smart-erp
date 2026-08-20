<!--
	=============================================================
	프로그램명	: LOT부여작업 (HPIO200U)
	작성일자	: 2025.02.24
	설명        : 생산 공정별 LOT 번호 부여 및 관리 (HPIO210U 표준 패턴 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-tags-fill me-2 text-primary" style="font-size: 18px;"></i>
        생산정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">LOT부여작업 (HPIO200U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchList">조회</button>
        <button class="btn-erp btn-save" @click="saveData">저장</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 조회 필터 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
                <col style="width: 10%" /><col style="width: 23%" />
                <col style="width: 10%" /><col style="width: 23%" />
                <col style="width: 10%" /><col style="width: 24%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light required">생산라인</th>
                <td>
                  <select v-model="searchForm.linecd" class="form-select form-select-sm" @change="onLineChange">
                    <option value="">라인 선택</option>
                    <option v-for="opt in lineOptions" :key="opt.linecd" :value="opt.linecd">
                      [{{ opt.linecd }}] {{ opt.linenm }}
                    </option>
                  </select>
                </td>
                <th class="text-center bg-light required">LOT일자</th>
                <td>
                  <input v-model="lotymd_f" type="date" class="form-control form-control-sm" @change="fetchList" />
                </td>
                <th class="text-center bg-light">주문번호</th>
                <td>
                  <div class="input-group input-group-sm">
                    <input v-model="ordym_f" type="month" class="form-control" style="max-width: 100px;" />
                    <input v-model="searchForm.ordno" type="text" class="form-control text-center" placeholder="번호" maxlength="4" />
                    <button class="btn btn-outline-secondary px-2" @click="handleOpenHelp('ORDER')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column grid-container-right">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>LOT 부여 내역</span>
          <div class="d-flex gap-1">
            <button class="btn btn-sm btn-outline-primary py-0 px-2 fw-bold" @click="addRow" style="font-size: 11px;">+ 행추가</button>
            <button class="btn btn-sm btn-outline-danger py-0 px-2 fw-bold" @click="deleteSelectedRows" style="font-size: 11px;">- 행삭제</button>
          </div>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="tableRef" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, computed, nextTick } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { useCommonHelp } from '@/composables/useCommonHelp'
import { getDate } from '@/composables/useDate'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'

const authStore = useAuthStore()
const { today } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps } = useCommonHelp()

// [1] 데이터 모델링
const searchForm = reactive({
  linecd: '',
  lotymd: today.replace(/-/g, ''),
  ordym: today.replace(/-/g, '').substring(0, 6),
  ordno: ''
})

const lineOptions = ref<any[]>([])

// 날짜 포맷 변수 (ui 접두어 제거)
const lotymd_f = computed({
  get: () => searchForm.lotymd && searchForm.lotymd.length === 8 ? `${searchForm.lotymd.substring(0, 4)}-${searchForm.lotymd.substring(4, 6)}-${searchForm.lotymd.substring(6, 8)}` : '',
  set: (v) => { if (v) searchForm.lotymd = v.replace(/-/g, '') }
})
const ordym_f = computed({
  get: () => searchForm.ordym && searchForm.ordym.length === 6 ? `${searchForm.ordym.substring(0, 4)}-${searchForm.ordym.substring(4, 6)}` : '',
  set: (v) => { if (v) searchForm.ordym = v.replace(/-/g, '') }
})

const tableRef = ref<HTMLDivElement | null>(null)
let grid: Tabulator | null = null

// [2] 그리드 초기화
const initGrids = () => {
  grid = new Tabulator(tableRef.value!, {
    layout: "fitColumns", height: "100%", placeholder: "데이터 없음", selectable: true,
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
      { title: "제품명", field: "itemnm", minWidth: 200, widthGrow: 1, cssClass: 'fw-bold text-primary',
        cellClick: (e, cell) => { if (!searchForm.linecd) return vAlertError('생산라인을 먼저 선택하세요.'); handleOpenHelp('ITEM', cell.getRow()) }
      },
      { title: "규격", field: "itsize", width: 150 },
      { title: "단위", field: "unit", width: 70, hozAlign: "center" },
      { title: "LOT SIZE", field: "lotsize", width: 100, hozAlign: "right", editor: "number", cellEdited: (cell) => {
          const d = cell.getData(); if (d._state === 'EXIST') cell.getRow().update({ _status: '수정' });
      }},
      { title: "투입일자", field: "stymd", width: 130, hozAlign: "center", editor: "input",
        formatter: (c) => { const v = c.getValue(); return v && v.length === 8 ? `${v.substring(0,4)}-${v.substring(4,6)}-${v.substring(6,8)}` : v; },
        cellEdited: (cell) => { const d = cell.getData(); if (d._state === 'EXIST') cell.getRow().update({ _status: '수정' }); }
      },
      { title: "완료일자", field: "todt", width: 130, hozAlign: "center", editor: "input",
        formatter: (c) => { const v = c.getValue(); return v && v.length === 8 ? `${v.substring(0,4)}-${v.substring(4,6)}-${v.substring(6,8)}` : v; },
        cellEdited: (cell) => { const d = cell.getData(); if (d._state === 'EXIST') cell.getRow().update({ _status: '수정' }); }
      },
      { title: "삭제", width: 40, hozAlign: "center", formatter: () => "<i class='bi bi-trash text-danger'></i>",
        cellClick: (e, cell) => handleRowAction(cell.getRow())
      }
    ],
  });
}

// [3] 비즈니스 로직
const fetchLineOptions = async () => {
  try {
    const res = await api.get('/hp00/HP00_000S_STR', { params: { gubun: 'L0', cmpycd: authStore.cmpycd, gbncd: 'Y', code: '' } })
    lineOptions.value = res.data
  } catch (e) { console.error(e) }
}

const onLineChange = () => fetchList()

async function fetchList() {
  if (!searchForm.linecd) return
  try {
    const res = await api.post('/hpio/HPIO_200U_STR', {
        actkind: 'S0',
        cmpycd: authStore.cmpycd,
        linecd: searchForm.linecd,
        lotymd: searchForm.lotymd,
        lotsize: 0
     });
    grid?.setData(res.data.map((i: any) => ({ ...i, _state: 'EXIST', _status: '' })));
    vAlert('조회되었습니다.');
  } catch (e) { vAlertError('조회 실패'); }
}

const saveData = async () => {
  const details = grid?.getData().filter((r: any) => r._status) || []
  if (details.length === 0) return vAlertError('저장할 변경 항목이 없습니다.')
  if (!confirm('자료를 저장하시겠습니까?')) return

  try {
    for (const item of details) {
      const actkind = item._status === '입력' ? 'A0' : (item._status === '삭제' ? 'D0' : 'U0')
      await api.post('/hpio/HPIO_200U_STR', {
        actkind, cmpycd: authStore.cmpycd, userid: authStore.userid,
        linecd: searchForm.linecd, lotymd: searchForm.lotymd,
        lotno: item.lotno || '', itemcd: item.itemcd, itsize: item.itsize || '', unit: item.unit || '',
        lotsize: String(item.lotsize || '0').replace(/,/g, ''),
        stymd: (item.stymd || '').replace(/-/g, ''),
        todt: (item.todt || '').replace(/-/g, ''),
        bigo: item.bigo || '', useyn: item._status === '삭제' ? 'N' : 'Y',
        ordym: searchForm.ordym, ordno: searchForm.ordno
      })
    }
    vAlert('정상적으로 저장되었습니다.'); fetchList()
  } catch (e) { vAlertError('저장 처리 중 오류 발생') }
}

const handleOpenHelp = (type: string, target?: any) => {
  const commonPath = '/ha00/HA00_00P_STR'

  switch (type) {
    case 'ORDER': // 주문 도움창
      Object.assign(modalProps, {
        title: '주문 선택',
        path: commonPath,
        defaultField: 'ordno',
        data: { gubun: 'OR', cmpycd: authStore.cmpycd },
        columns: [
          { title: '주문월', field: 'ordym', width: 100, hozAlign: 'center' },
          { title: '주문번호', field: 'ordno', width: 100, hozAlign: 'center' },
          { title: '거래처명', field: 'custnm', minWidth: 200, widthGrow: 1 },
          { title: '주문일자', field: 'ordymd', width: 120, hozAlign: 'center' }
        ],
        onConfirm: (data: any) => {
          searchForm.ordym = data.ordym
          searchForm.ordno = data.ordno
        }
      })
      modalVisible.value = true
      break

    case 'ITEM': // 품목 도움창
      Object.assign(modalProps, {
        title: '품목 선택',
        path: '/hp00/HP00_000S_STR',
        defaultField: 'itemnm',
        large: true,
        data: { gubun: 'I1', cmpycd: authStore.cmpycd, gbncd: 'A', code: '', remark: '' },
        columns: [
          { title: '자산구분', field: 'astkindnm', width: 120, hozAlign: 'center' },
          { title: '품목코드', field: 'itemcd', width: 120, hozAlign: 'center' },
          { title: '품목명', field: 'itemnm', minWidth: 250, widthGrow: 1, hozAlign: 'left' },
          { title: '규격', field: 'itsize', width: 150 },
          { title: '단위', field: 'unit', width: 80, hozAlign: 'center' }
        ],
        onConfirm: (data: any) => {
          target.update({
            itemcd: data.itemcd,
            itemnm: data.itemnm,
            itsize: data.itsize,
            unit: data.unit,
            stymd: searchForm.lotymd,
            _status: '입력',
            _state: 'NEW'
          })
        }
      })
      modalVisible.value = true
      break
  }
}

const handleRowAction = (row: any) => {
  const d = row.getData();
  if (d._state === 'NEW') row.delete();
  else row.update({ _status: d._status === '삭제' ? '' : '삭제' });
}

const addRow = () => {
  if (!searchForm.linecd) return vAlertError('생산라인을 먼저 선택하세요.');
  grid?.addRow({ lotsize: 0, _status: '입력', _state: 'NEW', useyn: 'Y' }, true);
}

const deleteSelectedRows = () => {
  const sel = grid?.getSelectedRows();
  if (sel?.length) sel.forEach(row => handleRowAction(row));
}

const initialize = () => {
  resetForm(searchForm);
  Object.assign(searchForm, { linecd: '', lotymd: today.replace(/-/g, ''), ordym: today.replace(/-/g, '').substring(0, 6), ordno: '' });
  grid?.clearData();
}

onMounted(() => {
  fetchLineOptions();
  nextTick(initGrids);
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
.grid-container-right { border-bottom: 3px solid #005a9f !important; }
</style>
