<!--
	=============================================================
	프로그램명	: 품목별 배부적수 등록 (HFMF102U)
	작성일자	: 2025.02.24
	설명        : 작업장별/품목별 원가 배부 적수(가중치) 등록 및 관리 (HSOD100U 표준 그리드 적용)
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
        원가관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        기준정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">품목별 배부적수 등록 (HFMF102U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-2">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="handleSearch">조회</button>
        <button class="btn-erp btn-save" @click="save">저장</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 조회 필터 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
                <col style="width: 10%" /><col style="width: 25%" />
                <col style="width: 10%" /><col style="width: 25%" />
                <col style="width: 30%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light required">년 월</th>
                <td>
                  <input v-model="ym_f" type="month" class="form-control form-control-sm" style="max-width: 150px;" @change="handleSearch" />
                </td>
                <th class="text-center bg-light required">배부기준</th>
                <td>
                  <select v-model="searchForm.divstd" class="form-select form-select-sm" @change="handleSearch">
                    <option v-for="opt in divideOptions" :key="opt.code" :value="opt.code">{{ opt.cdnm }}</option>
                  </select>
                </td>
                <td class="text-end pe-3 border-start-0">
                  <span v-if="clsInfo.wclsym" class="badge bg-danger-subtle text-danger border border-danger-subtle">마감월: {{ clsInfo.wclsym }}</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 투-그리드 레이아웃 영역 -->
      <div class="d-flex gap-2 flex-grow-1 overflow-hidden" style="min-height: 0;">

        <!-- ⬅️ 좌측: 작업장 목록 (grid1) -->
        <div class="card border shadow-sm d-flex flex-column overflow-hidden grid-container-left" style="width: 280px; min-width: 280px;">
          <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark">작업장 목록</div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="tableRef1" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>

        <!-- ➡️ 우측: 품목 상세 그리드 (grid2) -->
        <div class="flex-grow-1 d-flex flex-column gap-2 overflow-hidden">
          <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column grid-container-right">
            <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
              <span class="fw-bold small text-dark">
                <i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>품목별 상세 내역
                <span v-if="selectedLine.cdnm" class="badge bg-primary-subtle text-primary border border-primary-subtle ms-2">{{ selectedLine.cdnm }}</span>
              </span>
              <div class="d-flex gap-1">
                <button class="btn btn-sm btn-outline-primary py-0 px-2 fw-bold" @click="addRow" :disabled="!selectedLine.code" style="font-size: 11px;">+ 행추가</button>
                <button class="btn btn-sm btn-outline-danger py-0 px-2 fw-bold" @click="deleteSelectedRows" :disabled="!selectedLine.code" style="font-size: 11px;">- 행삭제</button>
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
import { ref, reactive, onMounted, nextTick, computed } from 'vue'
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
const { modalVisible, modalProps, openHelp } = useCommonHelp()

// [1] 데이터 모델링
const searchForm = reactive({
  ym: today.substring(0, 7).replace(/-/g, ''),
  divstd: ''
})

const ym_f = computed({
  get: () => searchForm.ym ? `${searchForm.ym.substring(0, 4)}-${searchForm.ym.substring(4, 6)}` : '',
  set: (v) => { if (v) searchForm.ym = v.replace(/-/g, '') }
})

const clsInfo = reactive({ wclsym: '' })
const divideOptions = ref<any[]>([])
const selectedLine = reactive({ code: '', cdnm: '' })

const tableRef1 = ref<HTMLDivElement | null>(null)
const tableRef2 = ref<HTMLDivElement | null>(null)
let grid1: Tabulator | null = null
let grid2: Tabulator | null = null

// [2] 그리드 초기화 (HSOD100U 표준 적용)
const initGrids = () => {
  if (!tableRef1.value || !tableRef2.value) return

  // Left: 작업장
  grid1 = new Tabulator(tableRef1.value, {
    layout: "fitColumns", height: "100%", placeholder: "데이터 없음", selectable: 1,
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "No", formatter: "rownum", width: 40, hozAlign: "center" },
      { title: "작업장", field: "cdnm", hozAlign: "left", cssClass: "fw-bold text-primary",
        formatter: (c) => {
          const d = c.getData();
          return d.linecd ? `[${d.linecd}] ${d.cdnm || ''}` : '';
        }
      }
    ],
  });
  grid1.on("rowClick", (e, row) => {
    const d = row.getData();
    selectedLine.code = d.linecd;
    selectedLine.cdnm = d.cdnm;
    fetchDetails();
  });

  // Right: 품목 상세
  grid2 = new Tabulator(tableRef2.value, {
    layout: "fitColumns", height: "100%", placeholder: "작업장을 선택하세요.", selectable: true,
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
      { title: "품목코드", field: "itemcd", width: 110, hozAlign: "center" },
      { title: "품목명", field: "itemnm", minWidth: 200, widthGrow: 1, cssClass: 'fw-bold text-primary',
        cellClick: (e, cell) => handleOpenHelp('ITEM', cell.getRow())
      },
      { title: "단위", field: "unit", width: 70, hozAlign: "center" },
      { title: "생산량", field: "proqty", width: 90, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "배부적수", field: "juksu", width: 100, hozAlign: "right", editor: "number", cellEdited: (cell) => {
          if (cell.getData()._state === 'EXIST') cell.getRow().update({ _status: '수정' });
      }},
      { title: "배부율(%)", field: "rate", width: 100, hozAlign: "right", editor: "number", cellEdited: (cell) => {
          if (cell.getData()._state === 'EXIST') cell.getRow().update({ _status: '수정' });
      }},
      { title: "비고", field: "remark", minWidth: 150, editor: "input" },
      { title: "삭제", width: 40, hozAlign: "center", formatter: () => "<i class='bi bi-trash text-danger'></i>",
        cellClick: (e, cell) => handleRowAction(cell.getRow())
      }
    ],
  });
}

// [3] 비즈니스 로직
const loadInitData = async () => {
  try {
    const [cls, divOpts] = await Promise.all([
      api.get('/hp00/HP00_000S_STR', { params: { gubun: 'CL', cmpycd: authStore.cmpycd } }),
      api.post('/hfba/SELECT_DIVIDE_LIST', { cdkd: '1040' })
    ])
    clsInfo.wclsym = cls.data[0]?.wclsym || ''
    divideOptions.value = divOpts.data || []
    if (divideOptions.value.length > 0) searchForm.divstd = divideOptions.value[0].code
  } catch (e) { console.error(e) }
}

const handleSearch = async () => {
  if (!searchForm.divstd) return
  try {
    const res = await api.post('/hfmf/FMF1020U_STR', {
      cmpycd: authStore.cmpycd, actkind: 'S1', yymm: searchForm.ym, divrate: searchForm.divstd, itemcd: ''
    })
    // 🚀 실제 비즈니스 데이터(linecd가 있는 경우)만 필터링하여 그리드에 세팅
    const validData = (res.data || []).filter((r: any) => r.linecd);
    grid1?.setData(validData)
    grid2?.clearData(); selectedLine.code = ''; selectedLine.cdnm = '';
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

const fetchDetails = async () => {
  try {
    const { data } = await api.post('/hfmf/FMF1020U_STR', {
      cmpycd: authStore.cmpycd, actkind: 'S0', yymm: searchForm.ym, divrate: searchForm.divstd, itemcd: selectedLine.code
    })
    grid2?.setData(data.map((i: any) => ({ ...i, _state: 'EXIST', _status: '' })))
  } catch (e) {}
}

const save = async () => {
  if (clsInfo.wclsym && clsInfo.wclsym >= searchForm.ym) return vAlertError('원가마감 되었습니다.')
  const details = grid2?.getData().filter((r: any) => r._status) || []
  if (details.length === 0) return vAlertError('저장할 변경 항목이 없습니다.')

  if (!confirm('저장하시겠습니까?')) return

  try {
    for (const row of details) {
      const act = row._status === '입력' ? 'A0' : (row._status === '삭제' ? 'D0' : 'U0');
      await api.post('/hfmf/FMF1020U_STR', {
        cmpycd: authStore.cmpycd, actkind: act, yymm: searchForm.ym, itemcd: row.itemcd, divrate: row.rate || '0'
      })
    }
    vAlert('저장되었습니다.'); fetchDetails();
  } catch (e) { vAlertError('저장 실패') }
}

const handleOpenHelp = (type: string, row: any) => {
  if (type === 'ITEM') {
    openHelp('ITEM', (d) => row.update({ itemcd: d.itemcd, itemnm: d.itemnm, unit: d.unit, _status: '입력', _state: 'NEW' }), { codegbn: 'B' });
  }
}

const handleRowAction = (row: any) => {
  const d = row.getData();
  if (d._state === 'NEW') row.delete();
  else row.update({ _status: d._status === '삭제' ? '' : '삭제' });
}

const deleteSelectedRows = () => { grid2?.getSelectedRows().forEach(row => handleRowAction(row)); }

const addRow = () => {
  grid2?.addRow({ rate: '0', juksu: '0', _status: '입력', _state: 'NEW' }, true);
}

const initialize = () => {
  resetForm(searchForm);
  searchForm.ym = today.substring(0, 7).replace(/-/g, '');
  if (divideOptions.value.length > 0) searchForm.divstd = divideOptions.value[0].code;
  grid1?.clearData(); grid2?.clearData();
  selectedLine.code = ''; selectedLine.cdnm = '';
  handleSearch();
}

onMounted(async () => {
  await loadInitData()
  nextTick(initGrids)
  handleSearch()
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
