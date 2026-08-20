<!--
	=============================================================
	프로그램명	: 검사분류 항목관리 (HPIO910U)
	작성일자	: 2025.02.24
	설명        : 검사분류(820)별 검사항목(830) 매핑 관리 (HPIO920U 표준 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 1. 헤더 영역 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-patch-check-fill me-2 text-primary" style="font-size: 18px;"></i>
        품질관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        기준관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">검사분류 항목관리 (HPIO910U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-save" @click="handleSave">저장</button>
      </div>
    </div>

    <!-- 2. 메인 콘텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- 조회 조건 카드 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
                <col style="width: 10%" /><col style="width: 40%" />
                <col style="width: 10%" /><col style="width: 40%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light required">검사분류</th>
                <td>
                  <select v-model="searchForm.insp_gb" class="form-select form-select-sm" style="font-size: 12px; max-width: 250px;" @change="search">
                    <option v-for="opt in inspGbOptions" :key="opt.code" :value="opt.code">[{{ opt.code }}] {{ opt.cdnm }}</option>
                  </select>
                </td>
                <th class="bg-light"></th>
                <td></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 그리드 카드 -->
      <div class="card border shadow-sm flex-grow-1 d-flex flex-column overflow-hidden grid-container-right">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark"><i class="bi bi-list-task me-2 text-primary"></i>검사항목 매핑 목록</span>
          <div class="d-flex gap-1">
            <button class="btn btn-sm btn-outline-primary py-0 px-2 fw-bold" @click="addRow" style="font-size: 11px;">+ 행추가</button>
            <button class="btn btn-sm btn-outline-danger py-0 px-2 fw-bold" @click="delRow" style="font-size: 11px;">- 행삭제</button>
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
import { ref, reactive, onMounted, nextTick } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import AppAlert from '@/components/AppAlert.vue'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()

const searchForm = reactive({ insp_gb: '' })
const inspGbOptions = ref<any[]>([])
const inspCdOptions = ref<Record<string, string>>({})
const tableRef = ref<HTMLElement | null>(null)
let grid: Tabulator | null = null

// 🛡️ 모든 키를 소문자로 변환하는 유틸리티
const mapLower = (list: any[]) => {
    if (!Array.isArray(list)) return [];
    return list.map(i => {
        const n: any = {}; for (let k in i) n[k.toLowerCase()] = i[k]; return n;
    });
}

const initialize = () => {
  if (inspGbOptions.value.length > 0) searchForm.insp_gb = inspGbOptions.value[0].code
  grid?.clearData()
}

const search = async () => {
  if (!searchForm.insp_gb) return vAlertError('검사분류를 선택하세요.')
  try {
    const { data } = await api.get('/product/insp-clsfy/list', { params: { cmpycd: authStore.cmpycd, insp_gb: searchForm.insp_gb } })
    grid?.setData(mapLower(data).map(i => ({ ...i, _status: '' })))
  } catch (e) { vAlertError('조회 실패') }
}

const addRow = () => {
  if (!searchForm.insp_gb) return vAlertError('검사분류를 먼저 선택하세요.')
  grid?.addRow({ _status: '입력', insp_gb: searchForm.insp_gb, inspcd: '', insp_ord: (grid.getData().length + 1) * 10, bigo: '' }, true)
}

const delRow = () => {
  const selectedRows = grid?.getSelectedRows()
  if (!selectedRows || selectedRows.length === 0) return vAlertError('삭제할 행을 선택하세요.')
  selectedRows.forEach(row => {
    const data = row.getData()
    if (data._status === '입력') row.delete()
    else row.update({ _status: data._status === '삭제' ? '' : '삭제' })
  })
}

const handleSave = async () => {
  const all = grid?.getData() || []
  const targets = all.filter((d: any) => d._status)
  if (targets.length === 0) return vAlertError('저장할 데이터가 없습니다.')
  if (!confirm('저장하시겠습니까?')) return
  try {
    const payload = targets.map(d => ({
        ...d,
        cmpycd: authStore.cmpycd,
        updemp: authStore.userid,
        state: d._status === '입력' ? 'C' : (d._status === '삭제' ? 'D' : 'U')
    }))
    await api.post('/product/insp-clsfy/save', payload)
    vAlert('저장되었습니다.'); search()
  } catch (e) { vAlertError('저장 실패') }
}

const initGrid = () => {
  if (!tableRef.value) return;
  grid = new Tabulator(tableRef.value, {
    layout: 'fitColumns', height: '100%', selectable: true, placeholder: "데이터가 없습니다.",
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: '선택', width: 40, hozAlign: 'center', formatter: "rowSelection", titleFormatter: "rowSelection" },
      {
        title: "상태", field: "_status", width: 70, hozAlign: "center",
        formatter: (cell) => {
            const val = cell.getValue();
            if (val === '입력') return '<span class="badge bg-primary">입력</span>';
            if (val === '수정') return '<span class="badge bg-warning text-dark">수정</span>';
            if (val === '삭제') return '<span class="badge bg-danger">삭제</span>';
            return '';
        }
      },
      { title: '검사항목', field: 'inspcd', widthGrow: 2, hozAlign: 'center', editor: 'list', editorParams: { values: inspCdOptions.value },
        formatter: (cell) => inspCdOptions.value[cell.getValue()] || cell.getValue() },
      { title: '표시순서', field: 'insp_ord', width: 100, hozAlign: 'center', editor: 'input' },
      { title: '비고', field: 'bigo', widthGrow: 3, hozAlign: 'left', editor: 'input' }
    ]
  })
  grid.on('cellEdited', (cell) => {
    const d = cell.getRow().getData()
    if (d._status !== '입력' && d._status !== '삭제') cell.getRow().update({ _status: '수정' })
  })
}

onMounted(async () => {
  try {
    const resGb = await api.post('/hp00/HP00_000S_STR', { gubun: 'E0', cmpycd: authStore.cmpycd, gbncd: '820' })
    inspGbOptions.value = mapLower(resGb.data)

    const resCd = await api.post('/hp00/HP00_000S_STR', { gubun: 'E0', cmpycd: authStore.cmpycd, gbncd: '830' })
    inspCdOptions.value = mapLower(resCd.data).reduce((acc: any, cur: any) => ({ ...acc, [cur.code]: cur.cdnm }), {})

    if (inspGbOptions.value.length > 0) searchForm.insp_gb = inspGbOptions.value[0].code
  } catch (e) {}

  nextTick(initGrid);
  if (searchForm.insp_gb) search()
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; font-size: 12px; }
.required::after { content: " *"; color: red; }
</style>
