<!--
	=============================================================
	프로그램명	: 표준 BOM 관리
	작성일자	: 2025.02.24
	작성자	    : AI Assistant
	설명        : 제품별/공정별 투입 원자재 및 표준 소요량(BOM) 관리 (상하단 폰트 12px 통일 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container">
    <!-- 🚀 1. 상단 액션 바 (표준 규격 및 함수명 통일) -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-diagram-3-fill me-2 text-primary" style="font-size: 18px;"></i>
        기본정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        <span class="text-primary fw-bolder">표준 BOM (HPBA210U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-save" @click="save" :disabled="!selectedProg.progcd">저장</button>
        <button class="btn-erp btn-excel" @click="excel">엑셀</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2">
      <!-- 🅰️ 조회 조건 영역 (표준 erp-table-full 및 80px 레이블 규격) -->
      <div class="card border-0 shadow-sm overflow-hidden flex-shrink-0">
        <div class="card-body p-0">
          <table class="erp-table-full">
            <colgroup>
              <col style="width: 80px;" /><col />
              <col style="width: 80px;" /><col />
              <col style="width: 80px;" /><col />
            </colgroup>
            <tbody>
              <tr>
                <th class="required">생산라인</th>
                <td>
                  <select v-model="searchData.linecd" class="form-select" style="width: 150px;" @change="onLineChange">
                    <option v-for="opt in lineOptions" :key="opt.linecd" :value="opt.linecd">
                      [{{ opt.linecd }}] {{ opt.linenm }}
                    </option>
                  </select>
                </td>
                <th class="required">재고자산</th>
                <td>
                  <select v-model="searchData.astkind" class="form-select" style="width: 120px;" @change="onLineChange">
                    <option value="200">제품</option>
                    <option value="210">반제품</option>
                  </select>
                </td>
                <th class="required">제품명</th>
                <td>
                  <div class="input-group">
                    <input v-model="searchData.itemcd" type="text" class="form-control text-center bg-light fw-bold" style="max-width: 80px;" readonly />
                    <input v-model="searchData.itemnm" type="text" class="form-control" placeholder="제품 선택" @keyup.enter="openHelp('ITEM')" />
                    <input v-model="searchData.itsize" type="text" class="form-control bg-light" style="max-width: 100px;" readonly placeholder="규격" />
                    <button class="btn" @click="openHelp('ITEM')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 🅱️ 데이터 영역 (좌: 공정목록, 우: 자재그리드) -->
      <div class="d-flex flex-grow-1 gap-2 overflow-hidden">
        <!-- 좌측: 공정 목록 리스트 -->
        <div class="card border-0 shadow-sm d-flex flex-column" style="width: 250px; border-radius: 8px;">
          <div class="card-header bg-white py-2 px-3 border-bottom fw-bold small text-dark d-flex align-items-center">
            <i class="bi bi-gear-wide-connected me-2 text-primary"></i> 생산 공정 리스트
          </div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-auto">
            <ul class="list-group list-group-flush">
              <li v-if="processList.length === 0" class="list-group-item text-center py-5 text-muted small">제품을 먼저 조회하세요.</li>
              <li
                v-for="prog in processList"
                :key="prog.progcd"
                class="list-group-item list-group-item-action d-flex justify-content-between align-items-center py-2 px-3 cursor-pointer"
                :class="{ 'bg-primary-subtle text-primary fw-bold': selectedProg.progcd === prog.progcd }"
                @click="onProcessSelect(prog)"
              >
                <div>
                    <div class="small">{{ prog.prognm }}</div>
                    <div class="text-muted" style="font-size: 10px;">{{ prog.progcd }}</div>
                </div>
                <i class="bi bi-chevron-right small"></i>
              </li>
            </ul>
          </div>
        </div>

        <!-- 우측: 자재 상세 그리드 -->
        <div class="flex-grow-1 d-flex flex-column gap-2 overflow-hidden">
          <div class="card border-0 shadow-sm flex-grow-1 overflow-hidden d-flex flex-column" style="border-radius: 8px;">
            <div class="card-header bg-white py-1 px-3 border-bottom d-flex justify-content-between align-items-center flex-shrink-0">
              <span class="fw-bold small text-dark">
                <i class="bi bi-box-seam me-1 text-primary"></i> 투입 자재 명세 (BOM)
                <span v-if="selectedProg.prognm" class="badge bg-info ms-2 px-3 text-white" style="font-size: 10px;">{{ selectedProg.prognm }}</span>
              </span>
              <button class="btn-erp btn-init" style="height: 22px; padding: 0 8px;" @click="addRow" :disabled="!selectedProg.progcd">
                <i class="bi bi-plus-circle"></i> 행추가
              </button>
            </div>
            <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
              <div ref="gridElement" class="tabulator-instance flex-grow-1"></div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <Modal v-model:visible="modalVisible" :modalProps="modalProps" />
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, nextTick } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import type { ModalProps } from '@/types/modal'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

const searchData = reactive({ linecd: '010', astkind: '200', itemcd: '', itemnm: '', itsize: '', unit: '' })
const lineOptions = ref<any[]>([]); const processList = ref<any[]>([])
const selectedProg = reactive({ progcd: '', prognm: '' })

const gridElement = ref<HTMLElement | null>(null)
let grid: Tabulator | null = null
const itemCount = ref(0)

const fetchLineOptions = async () => {
  try {
    const res = await api.get('/hp00/HP00_000S_STR', {
        params: { gubun: 'L0', cmpycd: authStore.cmpycd, gbncd: 'Y', code: '', codenm: '', etcval: '' }
    })
    // 🚀 직관적인 매핑: 서버에서 넘어온 필드명(linecd, linenm)을 명시적으로 처리
    lineOptions.value = res.data.map((i: any) => ({
        linecd: i.linecd || '',
        linenm: i.linenm || ''
    }))
    if (lineOptions.value.length > 0 && !searchData.linecd) {
        searchData.linecd = lineOptions.value[0].linecd
    }
  } catch (e) { console.error('라인 목록 로드 실패') }
}

const onLineChange = () => { processList.value = []; selectedProg.progcd = ''; grid?.clearData() }

// [2] 상태 관리 헬퍼 (HSOD100U 표준)
const updateRowStatus = (row: any) => {
  const d = row.getData();
  if (d._state === 'EXIST' && d._status !== '삭제') {
    row.update({ _status: '수정' });
  }
}

const initGrid = () => {
  if (gridElement.value) {
    grid = new Tabulator(gridElement.value, {
      layout: "fitColumns", height: "100%", placeholder: "공정을 선택하세요.", selectable: true,
      columnDefaults: {
        headerHozAlign: 'center', headerSort: false, vertAlign: "middle",
        cellEdited: (cell) => updateRowStatus(cell.getRow())
      },
      columns: [
        { title: "선택", width: 40, hozAlign: "center", formatter: "rowSelection", titleFormatter: "rowSelection", headerHozAlign: "center" },
        { title: "상태", field: "_status", width: 60, hozAlign: "center", formatter: (c) => {
            const v = c.getValue();
            if (v === '입력') return '<span class="badge bg-primary">입력</span>';
            if (v === '수정') return '<span class="badge bg-warning text-dark">수정</span>';
            if (v === '삭제') return '<span class="badge bg-danger">삭제</span>';
            return '';
        }},
        { title: "코드", field: "mitemcd", width: 100, hozAlign: "center", cssClass: "text-primary fw-bold" },
        { title: "투입자재명", field: "mitemnm", minWidth: 200, cellClick: (e, cell) => openHelp('GRID_MAT', cell) },
        { title: "규격", field: "mitsize", width: 150 },
        { title: "단위", field: "munit", width: 70, hozAlign: "center" },
        { title: "소요량", field: "inqty", width: 100, hozAlign: "right", editor: "number", formatter: "money", formatterParams: { precision: 4 }, cssClass: "fw-bold" },
        { title: "LOSS율(%)", field: "losrate", width: 90, hozAlign: "right", editor: "number" },
        { title: "이전공정", field: "bprognm", width: 160,
          formatter: (cell) => {
            const d = cell.getData();
            return d.befprog ? `[${d.befprog}] ${d.bprognm || ''}` : (d.bprognm || '');
          },
          cellClick: (e, cell) => openHelp('GRID_PROG', cell)
        },
        { title: "사용", field: "useyn", width: 80, hozAlign: "center",  editor: "list", editorParams: { values: { "Y": "사용", "N": "미사용" } },
          formatter: (cell) => {
            const val = String(cell.getValue() || '').trim().toUpperCase();
            return val === 'Y' ? '<b class="text-primary">사용</b>' : '';
          }
        },
        { title: "삭제", width: 40, hozAlign: "center", formatter: () => "<i class='bi bi-trash text-danger cursor-pointer'></i>", cellClick: (e, cell) => {
            const row = cell.getRow();
            const d = row.getData();
            if (d._state === 'NEW') row.delete();
            else row.update({ _status: d._status === '삭제' ? '' : '삭제' });
        }}
      ],
    })
  }
}

async function search() {
  if (!searchData.itemcd) return vAlertError('제품을 먼저 선택하세요.')
  try {
    const res = await api.get('/hp00/HP00_000S_STR', { params: { gubun: 'G1', cmpycd: authStore.cmpycd, gbncd: searchData.linecd, code: searchData.itemcd } })
    processList.value = res.data; selectedProg.progcd = ''; grid?.clearData(); vAlert('조회되었습니다.')
  } catch (e) { vAlertError('공정 조회 실패') }
}

const onProcessSelect = (prog: any) => { selectedProg.progcd = prog.progcd; selectedProg.prognm = prog.prognm; fetchBomDetails() }


async function fetchBomDetails() {
  try {
    const res = await api.post('/hpba/HPBA_210U_STR', {
        actkind: 'S0', cmpycd: authStore.cmpycd, itemcd: searchData.itemcd, linecd: searchData.linecd, progcd: selectedProg.progcd,
        inqty: 0, losrate: 0
    })

    // 🚀 [해결] 필드명 통합: befprognm 등 다양한 명칭을 bprognm으로 단일화
    const cleanData = (res.data || []).map((i: any) => ({
        ...i,
        // 이전공정명이 befprognm으로 내려올 경우를 대비한 매핑
        bprognm: i.bprognm || i.befprognm || i.prognm || ''
    })).filter((i: any) => i.mitemcd);

    grid?.setData(cleanData.map((i: any) => ({ ...i, _state: 'EXIST', _status: '' })));
    itemCount.value = cleanData.length
  } catch (e) { vAlertError('BOM 상세 조회 실패') }
}

async function save() {
  const allData = grid?.getData() || []
  const changes = allData.filter((r: any) => r._status)

  if (changes.length === 0) return vAlertError('저장할 자재 항목이 없습니다.')
  if (!confirm('현재 공정의 BOM 설정을 저장하시겠습니까?')) return
  try {
    for (const row of changes) {
      const actkind = row._status === '입력' ? 'A0' : (row._status === '삭제' ? 'D0' : 'U0')
      await api.post('/hpba/HPBA_210U_STR', {
        ...row,
        actkind: actkind,
        cmpycd: authStore.cmpycd,
        userid: authStore.userid,
        itemcd: searchData.itemcd,
        linecd: searchData.linecd,
        progcd: selectedProg.progcd,
        inqty: Number(row.inqty || 0),
        losrate: Number(row.losrate || 0),
        useyn: (row.useyn === 'Y' || row.useyn === true) ? 'Y' : 'N'
      })
    }
    vAlert('정상적으로 저장되었습니다.'); fetchBomDetails()
  } catch (e) { vAlertError('저장 중 오류 발생') }
}

function initialize() {
  resetForm(searchData); Object.assign(searchData, { linecd: '010', astkind: '200' });
  processList.value = []; selectedProg.progcd = ''; grid?.clearData(); itemCount.value = 0
}

function excel() { grid?.download("xlsx", `BOM명세_${searchData.itemnm}_${selectedProg.prognm}.xlsx`) }

function addRow() {
    if (!selectedProg.progcd) return vAlertError('공정을 먼저 선택하세요.')
    // 🚀 투입수량 기본 1, LOSS율 0 세팅
    grid?.addRow({ _status: '입력', _state: 'NEW', mitemcd: '', mitemnm: '', inqty: 1, losrate: 0, useyn: 'Y' }, true)
}

const modalVisible = ref(false); const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })
function openHelp(type: string, cell?: any) {
  let config: any = {}
  if (type === 'ITEM') {
    config = {
        title: '제품 선택',
        path: '/hp00/HP00_000S_STR',
        data: { gubun: 'I1', cmpycd: authStore.cmpycd, gbncd: '2' },
        columns: [
            { title: '코드', field: 'itemcd', width: 100 },
            { title: '제품명', field: 'itemnm', width: 250 },
            { title: '규격', field: 'itsize', width: 150 }
        ],
        onConfirm: (d: any) => {
            // 🚀 대소문자 무관하게 매핑 (소문자 원칙 준수)
            const itemcd = d.itemcd || '';
            const itemnm = d.itemnm || '';
            const itsize = d.itsize || '';
            const unit = d.unit || '';

            Object.assign(searchData, { itemcd, itemnm, itsize, unit });
            search();
        }
    }
  } else if (type === 'GRID_MAT') {
    config = { title: '자재 선택', path: '/hp00/HP00_000S_STR', data: {
        gubun: 'I0',
        cmpycd: authStore.cmpycd,
        gbncd: 'A'
    }, columns: [{ title: '코드', field: 'itemcd', width: 100 }, { title: '자재명', field: 'itemnm', width: 250 }, { title: '자산구분', field: 'astkind', width: 80 }],
        onConfirm: (d: any) => {
            // 🚀 대소문자 무관하게 자산구분(astkind) 매핑
            const astkind = d.astkind || '';
            cell.getRow().update({
                mitemcd: d.itemcd,
                mitemnm: d.itemnm,
                mitsize: d.itsize,
                munit: d.unit,
                mastkind: astkind
            });
        }
    }
  } else if (type === 'GRID_PROG') {
    const rowData = cell.getRow().getData();
    // 🚀 mastkind 뿐만 아니라 서버에서 넘어올 수 있는 astkind, mastkind 필드 모두 체크
    const checkKind = String(rowData.mastkind || rowData.astkind || '').trim();

    if (checkKind !== '200' && checkKind !== '210') {
        return vAlertError(`현재 선택된 자재(구분:${checkKind})는 원재료이므로 이전공정을 선택할 수 없습니다. 반제품/제품만 가능합니다.`);
    }

    config = { title: '이전공정 선택', path: '/hp00/HP00_000S_STR', data: {
    gubun: 'G0',
    gbncd: searchData.linecd,
    cmpycd: authStore.cmpycd }, columns: [{ title: '코드', field: 'progcd', width: 80 }, { title: '공정명', field: 'prognm', width: 150 }],
        onConfirm: (d: any) => {
            const row = cell.getRow();
            row.update({ befprog: d.progcd, bprognm: d.prognm });
            updateRowStatus(row);
        }
    }
  }
  Object.assign(modalProps, config); modalVisible.value = true
}

onMounted(() => { fetchLineOptions(); nextTick(() => initGrid()) })
</script>
