<!--
	=============================================================
	프로그램명	: 공정관리 (HPBA100U)
	작성일자	: 2025.02.27
	설명        : 생산 라인별 공정 정보 등록 및 순서/사용 여부 관리 (HSOD100U 표준 패턴 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-gear-wide-connected me-2 text-primary" style="font-size: 18px;"></i>
        생산관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        기본정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">공정관리 (HPBA100U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchLines">조회</button>
        <button class="btn-erp btn-save" @click="saveData" :disabled="!selectedLine.linecd">저장</button>
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
                <th class="text-center bg-light">라인조회</th>
                <td>
                  <div class="d-flex gap-2 px-2">
                    <input v-model="searchForm.sch_text" class="form-control form-control-sm" style="max-width: 250px;" placeholder="라인명 검색" @keyup.enter="fetchLines" />
                  </div>
                </td>
                <td colspan="2" class="text-muted small ps-3">
                  <i class="bi bi-info-circle me-1"></i> 라인을 선택하면 우측에서 공정을 등록/수정할 수 있습니다.
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 투-그리드 레이아웃 영역 -->
      <div class="d-flex gap-2 flex-grow-1 overflow-hidden" style="min-height: 0;">

        <!-- ⬅️ 좌측: 라인 목록 (grid1) -->
        <div class="card border shadow-sm d-flex flex-column overflow-hidden grid-container-left" style="width: 350px; min-width: 350px;">
          <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark text-center">생산 라인</div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="tableRef1" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>

        <!-- ➡️ 우측: 공정 상세 그리드 (grid2) -->
        <div class="flex-grow-1 d-flex flex-column gap-2 overflow-hidden">
          <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column grid-container-right">
            <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
              <span class="fw-bold small text-dark">
                <i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>공정 상세 내역
                <span v-if="selectedLine.linenm" class="badge bg-primary-subtle text-primary border border-primary-subtle ms-2">{{ selectedLine.linenm }}</span>
              </span>
              <div class="d-flex gap-1">
                <button class="btn btn-sm btn-outline-primary py-0 px-2 fw-bold" @click="addRow" :disabled="!selectedLine.linecd" style="font-size: 11px;">+ 행추가</button>
                <button class="btn btn-sm btn-outline-danger py-0 px-2 fw-bold" @click="deleteSelectedRows" :disabled="!selectedLine.linecd" style="font-size: 11px;">- 행삭제</button>
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
import { reactive, ref, onMounted, nextTick } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { useCommonHelp } from '@/composables/useCommonHelp'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps, openHelp } = useCommonHelp()

// [1] 데이터 모델링
const searchForm = reactive({ sch_text: '' })
const selectedLine = reactive({ linecd: '', linenm: '' })

const tableRef1 = ref<HTMLDivElement | null>(null)
const tableRef2 = ref<HTMLDivElement | null>(null)
let grid1: Tabulator | null = null
let grid2: Tabulator | null = null

// [2] 상태 관리 헬퍼 (HSOD100U 표준 - 사용자 지시 정답 로직)
const updateRowStatus = (row: any) => {
  const d = row.getData();
  // 🚀 기존 데이터(EXIST)이고 삭제 상태가 아닐 때만 '수정'으로 변경
  if (d._state === 'EXIST' && d._status !== '삭제') {
    row.update({ _status: '수정' });
  }
}

// [3] 그리드 초기화
const initGrids = () => {
  // Left: 라인 목록 (사용자 수정 필드명 linecd, linenm 보존)
  grid1 = new Tabulator(tableRef1.value!, {
    layout: "fitColumns", height: "100%", placeholder: "데이터 없음", selectable: 1,
    columns: [
      { title: "No", formatter: "rownum", width: 40, hozAlign: "center", headerSort: false },
      { title: "라인명", field: "linenm", hozAlign: "left", headerSort: false, cssClass: "fw-bold text-primary" },
      { title: "코드", field: "linecd", hozAlign: "center", width: 80, headerSort: false }
    ],
  });
  grid1.on("rowClick", (e, row) => onLineSelect(row.getData()));

  // Right: 공정 목록
  grid2 = new Tabulator(tableRef2.value!, {
    layout: "fitColumns", height: "100%", placeholder: "라인을 선택하세요.", selectable: true,
    columnDefaults: {
      headerHozAlign: 'center',
      headerSort: false,
      vertAlign: "middle",
      cellEdited: (cell) => updateRowStatus(cell.getRow())
    },
    columns: [
      { title: "선택", width: 40, hozAlign: "center", formatter: "rowSelection", titleFormatter: "rowSelection" },
      { title: "상태", field: "_status", width: 60, hozAlign: "center", formatter: (c) => {
          const v = c.getValue();
          if (v === '입력') return '<span class="badge bg-primary">입력</span>';
          if (v === '수정') return '<span class="badge bg-warning text-dark">수정</span>';
          if (v === '삭제') return '<span class="badge bg-danger">삭제</span>';
          return '';
      }},
      { title: "코드", field: "progcd", width: 80, hozAlign: "center", editor: "input"  },
      { title: "공정명", field: "prognm", minWidth: 200, widthGrow: 1, editor: "input", cssClass: "fw-bold text-primary"   },
      { title: "관리부서", field: "deptnm", width: 150, cellClick: (e, cell) => handleOpenHelp('DEPT', cell.getRow()) },
      { title: "정산순서", field: "dspord", width: 80, hozAlign: "center", editor: "number"  },
      { title: "사용", field: "useyn", width: 80, hozAlign: "center",  editor: "list", editorParams: { values: { "Y": "사용", "N": "미사용" } },
        formatter: (cell) => {
          const val = String(cell.getValue() || '').trim().toUpperCase();
          return val === 'Y' ? '<b class="text-primary">사용</b>' : '<span class="text-muted">미사용</span>';
        }
      },
      { title: "삭제", width: 40, hozAlign: "center", formatter: () => "<i class='bi bi-trash text-danger'></i>", cellClick: (e, cell) => handleRowAction(cell.getRow()) }
    ]
  });
}

// [4] 비즈니스 로직
const fetchLines = async () => {
  try {
    // 🚀 [해결] 사용자 수정 사항 반영: code 파라미터에 검색어 매핑하여 조회 기능 정상화
    const res = await api.get('/hp00/HP00_000S_STR', { params: { gubun: 'L0', cmpycd: authStore.cmpycd, gbncd: 'Y', code: searchForm.sch_text } })
    grid1?.setData(res.data)
    grid2?.clearData(); selectedLine.linecd = ''; selectedLine.linenm = '';
  } catch (e) { vAlertError('라인 목록 조회 실패') }
}

const onLineSelect = (row: any) => {
  selectedLine.linecd = row.linecd; selectedLine.linenm = row.linenm
  fetchProcesses()
}

const fetchProcesses = async () => {
  try {
    const res = await api.post('/hpba/HPBA_100U_STR', { actkind: 'S0', cmpycd: authStore.cmpycd, linecd: selectedLine.linecd })
    // 🚀 [해결] 유령 레코드(빈 칸) 노출 방지: progcd가 없는 데이터는 필터링하여 로드
    const cleanData = (res.data || []).filter((i: any) => i.progcd && String(i.progcd).trim() !== '');
    grid2?.setData(cleanData.map((i: any) => ({ ...i, _state: 'EXIST', _status: '' })))
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

const saveData = async () => {
  const allData = grid2?.getData() || []
  const changes = allData.filter((r: any) => r._status)

  if (changes.length === 0) return vAlertError('저장할 변경 항목이 없습니다.')
  if (!confirm('공정 정보를 저장하시겠습니까?')) return

  try {
    for (const item of changes) {
      const actkind = item._status === '입력' ? 'A0' : (item._status === '삭제' ? 'D0' : 'U0')
      await api.post('/hpba/HPBA_100U_STR', {
        ...item,
        actkind,
        cmpycd: authStore.cmpycd,
        linecd: selectedLine.linecd,
        updemp: authStore.userid
      })
    }
    vAlert('성공적으로 저장되었습니다.'); fetchProcesses()
  } catch (e) { vAlertError('저장 처리 중 오류 발생') }
}

const handleOpenHelp = (type: string, row: any) => {
  if (type === 'DEPT') {
    openHelp('DEPT', (d) => {
      row.update({ deptcd: d.deptcd, deptnm: d.deptnm });
      updateRowStatus(row);
    });
  }
}

const handleRowAction = (row: any) => {
  const d = row.getData();
  if (d._state === 'NEW') row.delete();
  else row.update({ _status: d._status === '삭제' ? '' : '삭제' });
}

const addRow = () => {
  grid2?.addRow({
    useyn: 'Y',
    dspord: '',
    _status: '입력',
    _state: 'NEW'
  }, true);
}

const deleteSelectedRows = () => {
  grid2?.getSelectedRows().forEach(row => handleRowAction(row));
}

const initialize = () => {
  resetForm(searchForm); grid1?.clearData(); grid2?.clearData();
  selectedLine.linecd = ''; selectedLine.linenm = '';
}

onMounted(() => {
  nextTick(initGrids);
  fetchLines();
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
.grid-container-left, .grid-container-right { border-bottom: 3px solid #005a9f !important; }
</style>
