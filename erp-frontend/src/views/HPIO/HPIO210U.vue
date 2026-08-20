<!--
	=============================================================
	프로그램명	: 작업지시 (HPIO210U)
	작성일자	: 2025.02.24
	설명        : 생산 공정별 작업지시 등록 및 관리 (HSOD100U 표준 패턴 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-list-task me-2 text-primary" style="font-size: 18px;"></i>
        생산정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">작업지시 (HPIO210U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchProcesses">조회</button>
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
                <th class="text-center bg-light">생산라인</th>
                <td>
                  <select v-model="searchForm.linecd" class="form-select form-select-sm" @change="onLineChange">
                    <option value="">라인 선택</option>
                    <option v-for="opt in lineOptions" :key="opt.linecd" :value="opt.linecd">
                      [{{ opt.linecd }}] {{ opt.linenm }}
                    </option>
                  </select>
                </td>
                <th class="text-center bg-light">생산일자</th>
                <td>
                  <input v-model="proymd_f" type="date" class="form-control form-control-sm" @change="onDateChange" />
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

      <!-- [하단] 투-그리드 레이아웃 영역 -->
      <div class="d-flex gap-2 flex-grow-1 overflow-hidden" style="min-height: 0;">

        <!-- ⬅️ 좌측: 공정 목록 (grid1) -->
        <div class="card border shadow-sm d-flex flex-column overflow-hidden grid-container-left" style="width: 300px; min-width: 300px;">
          <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark">공정 목록</div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="tableRef1" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>

        <!-- ➡️ 우측: 지시 상세 그리드 (grid2) -->
        <div class="flex-grow-1 d-flex flex-column gap-2 overflow-hidden">
          <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column grid-container-right">
            <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
              <span class="fw-bold small text-dark">
                <i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>지시 상세 내역
                <span v-if="selectedProg.prognm" class="badge bg-primary-subtle text-primary border border-primary-subtle ms-2">{{ selectedProg.prognm }}</span>
              </span>
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
  proymd: today.replace(/-/g, ''),
  ordym: today.replace(/-/g, '').substring(0, 6),
  ordno: ''
})

const lineOptions = ref<any[]>([])
const selectedProg = reactive({ progcd: '', prognm: '' })

// 날짜/년월 포맷팅 헬퍼 (ui 접두어 제거)
const proymd_f = computed({
  get: () => searchForm.proymd && searchForm.proymd.length === 8 ? `${searchForm.proymd.substring(0, 4)}-${searchForm.proymd.substring(4, 6)}-${searchForm.proymd.substring(6, 8)}` : '',
  set: (v) => { if (v) searchForm.proymd = v.replace(/-/g, '') }
})
const ordym_f = computed({
  get: () => searchForm.ordym && searchForm.ordym.length === 6 ? `${searchForm.ordym.substring(0, 4)}-${searchForm.ordym.substring(4, 6)}` : '',
  set: (v) => { if (v) searchForm.ordym = v.replace(/-/g, '') }
})

const tableRef1 = ref<HTMLDivElement | null>(null)
const tableRef2 = ref<HTMLDivElement | null>(null)
let grid1: Tabulator | null = null
let grid2: Tabulator | null = null

// [2] 그리드 초기화 (HSOD100U 패턴 적용)
const initGrids = () => {
  // 좌측: 공정 목록
  grid1 = new Tabulator(tableRef1.value!, {
    layout: "fitColumns", height: "100%", placeholder: "라인을 선택하세요", selectable: 1,
    columns: [
      { title: "No", formatter: "rownum", width: 40, hozAlign: "center", headerSort: false },
      { title: "공정명", field: "prognm", hozAlign: "left", headerSort: false },
      { title: "코드", field: "progcd", hozAlign: "center", width: 80, cssClass: "fw-bold text-primary", headerSort: false }
    ],
  });
  grid1.on("rowClick", (e, row) => onProcessSelect(row.getData()));

  // 우측: 지시 상세
  grid2 = new Tabulator(tableRef2.value!, {
    layout: "fitColumns", height: "100%", placeholder: "공정을 선택해 주세요.", selectable: true,
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
      { title: "제품명", field: "itemnm", minWidth: 250, widthGrow: 1, cssClass: 'fw-bold text-primary',
        cellClick: (e, cell) => handleOpenHelp('ITEM', cell.getRow())
      },
      { title: "규격", field: "itsize", width: 200 },
      { title: "단위", field: "unit", width: 70, hozAlign: "center" },
      { title: "지시량", field: "ordqty", width: 150, hozAlign: "right", editor: "number", cellEdited: (cell) => {
          const d = cell.getData(); if (d._state === 'EXIST') cell.getRow().update({ _status: '수정' });
      }},
      { title: "비고", field: "bigo", minWidth: 150, editor: "input", cellEdited: (cell) => {
          const d = cell.getData(); if (d._state === 'EXIST') cell.getRow().update({ _status: '수정' });
      }},
      { title: "삭제", width: 60, hozAlign: "center", formatter: () => "<i class='bi bi-trash text-danger'></i>",
        cellClick: (e, cell) => handleRowAction(cell.getRow())
      }
    ]
  });
}

// [3] 비즈니스 로직
const fetchLineOptions = async () => {
  try {
    const res = await api.get('/hp00/HP00_000S_STR', { params: { gubun: 'L0', cmpycd: authStore.cmpycd, gbncd: 'Y', code: '' } })
    lineOptions.value = res.data
  } catch (e) { console.error(e) }
}

const fetchProcesses = async () => {
  if (!searchForm.linecd) { vAlertError('생산라인을 먼저 선택하세요.'); return; }
  try {
    const res = await api.get('/hp00/HP00_000S_STR', { params: { gubun: 'G0', cmpycd: authStore.cmpycd, gbncd: searchForm.linecd, code: '' } })
    grid1?.setData(res.data)
    grid2?.clearData()
    selectedProg.progcd = ''; selectedProg.prognm = '';
  } catch (e) { console.error(e) }
}

const onProcessSelect = async (prog: any) => {
  selectedProg.progcd = prog.progcd
  selectedProg.prognm = prog.prognm

  try {
    // 주문 정보 동기화 로직 (필요 시)
    const resS1 = await api.post('/hpio/HPIO_210U_STR', {
        actkind: 'S1',
        cmpycd: authStore.cmpycd,
        proymd: searchForm.proymd,
        linecd: searchForm.linecd,
        progcd: prog.progcd,
        ordqty: 0
     })
     console.log(resS1)
    if (resS1.data && resS1.data.length > 0) {
      const d = resS1.data[0]
      searchForm.ordym = d.ordym || searchForm.ordym
      searchForm.ordno = d.ordno || ''
    }
  } catch (e) {}
  fetchGridData()
}

const fetchGridData = async () => {
  if (!selectedProg.progcd) return
  try {
    const res = await api.post('/hpio/HPIO_210U_STR', {
      actkind: 'S0',
      cmpycd: authStore.cmpycd,
      proymd: searchForm.proymd,
      linecd: searchForm.linecd,
      progcd: selectedProg.progcd,
      ordqty: 0
    })
    grid2?.setData(res.data.map((i: any) => ({ ...i, _state: 'EXIST', _status: '' })))
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

const saveData = async () => {
  const details = grid2?.getData().filter((r: any) => r._status) || []
  if (details.length === 0) return vAlertError('저장할 변경 항목이 없습니다.')
  if (!confirm('작업지시 내역을 저장하시겠습니까?')) return

  try {
    for (const item of details) {
      const actkind = item._status === '입력' ? 'A0' : (item._status === '삭제' ? 'D0' : 'U0')
      await api.post('/hpio/HPIO_210U_STR', {
        actkind, cmpycd: authStore.cmpycd, proymd: searchForm.proymd, linecd: searchForm.linecd, progcd: selectedProg.progcd,
        lotymd: (item.lotymd || searchForm.proymd), lotno: item.lotno || '', itemcd: item.itemcd, itsize: item.itsize || '', unit: item.unit || '',
        ordqty: String(item.ordqty || '0').replace(/,/g, ''), bigo: item.bigo || '', useyn: item._status === '삭제' ? 'N' : 'Y',
        ordym: searchForm.ordym, ordno: searchForm.ordno, userid: authStore.userid
      })
    }
    vAlert('정상적으로 저장되었습니다.'); fetchGridData()
  } catch (e) { vAlertError('저장 처리 중 오류 발생') }
}

const onLineChange = () => fetchProcesses()
const onDateChange = () => { if (selectedProg.progcd) fetchGridData() }

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
            unit: data.unitnm || 'EA',
            ordqty: 0,
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
  if (!selectedProg.progcd) return vAlertError('공정을 먼저 선택하세요.');
  grid2?.addRow({ ordqty: 0, _status: '입력', _state: 'NEW' }, true);
}

const deleteSelectedRows = () => {
  const sel = grid2?.getSelectedRows();
  if (sel?.length) sel.forEach(row => handleRowAction(row));
}

const initialize = () => {
  resetForm(searchForm);
  Object.assign(searchForm, { linecd: '', proymd: today.replace(/-/g, ''), ordym: today.replace(/-/g, '').substring(0, 6), ordno: '' });
  grid1?.clearData(); grid2?.clearData();
  selectedProg.progcd = ''; selectedProg.prognm = '';
}

onMounted(async () => {
  nextTick(initGrids);
  fetchLineOptions();
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
.grid-container-left, .grid-container-right { border-bottom: 3px solid #005a9f !important; }
</style>