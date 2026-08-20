<!--
	=============================================================
	프로그램명	: 품목별 검사항목 표준관리 (HPIO920U)
	작성일자	: 2025.02.24
	설명        : 품목별로 수행해야 할 표준 검사항목 설정 (상태 플래그 배지 보강)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="showModal" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 1. 헤더 영역 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-gear-wide-connected me-2 text-primary" style="font-size: 18px;"></i>
        품질관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        기준관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">품목별 검사항목 표준관리 (HPIO920U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-save" @click="save">저장</button>
      </div>
    </div>

    <!-- 2. 메인 콘텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- 조회 조건 카드 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
                <col style="width: 10%" /><col style="width: 23%" />
                <col style="width: 10%" /><col style="width: 57%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light required">검사분류</th>
                <td>
                  <select v-model="searchForm.insp_gb" class="form-select form-select-sm" style="font-size: 12px;" @change="search">
                    <option v-for="opt in inspGbOptions" :key="opt.code" :value="opt.code">[{{ opt.code }}] {{ opt.cdnm }}</option>
                  </select>
                </td>
                <th class="text-center bg-light required">대상품목</th>
                <td class="px-2">
                  <div class="input-group input-group-sm" style="max-width: 450px;">
                    <input v-model="searchForm.itemcd" type="text" class="form-control bg-light" readonly placeholder="코드" style="width: 100px;" />
                    <input v-model="searchForm.itemnm" type="text" class="form-control bg-light" readonly placeholder="품목을 선택하세요" />
                    <button class="btn btn-outline-secondary" @click="openModal('ITEM')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 그리드 카드 -->
      <div class="card border shadow-sm flex-grow-1 d-flex flex-column overflow-hidden grid-container-right">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark"><i class="bi bi-list-check me-2 text-primary"></i>표준 검사항목 리스트</span>
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
import Modal from '@/components/Modal.vue'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()

const searchForm = reactive({ insp_gb: '', itemcd: '', itemnm: '' })
const inspGbOptions = ref<any[]>([])
const inspCdOptions = ref<Record<string, string>>({})

const showModal = ref(false)
const modalProps = reactive<any>({ type: 'table', title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {} })

const tableRef = ref<HTMLElement | null>(null)
let grid: Tabulator | null = null

const mapLower = (list: any[]) => {
    if (!Array.isArray(list)) return [];
    return list.map(i => {
        const n: any = {}; for (let k in i) { n[k.toLowerCase()] = i[k]; } return n;
    });
}

const initialize = () => {
	searchForm.itemcd = ''; searchForm.itemnm = ''
	if (inspGbOptions.value.length > 0) searchForm.insp_gb = inspGbOptions.value[0].code
	grid?.clearData()
}

const openModal = (type: string) => {
    if (type === 'ITEM') {
        Object.assign(modalProps, {
            title: '제품 품목조회', path: '/hp00/HP00_000S_STR', data: { gubun: 'I1', cmpycd: authStore.cmpycd, gbncd: '2' },
            columns: [ { title: '코드', field: 'itemcd', width: 120 }, { title: '품목명', field: 'itemnm', width: 250 } ],
            onConfirm: (d: any) => {
                const item = mapLower([d])[0];
                searchForm.itemcd = item.itemcd; searchForm.itemnm = item.itemnm;
                showModal.value = false; search()
            }
        })
    }
    showModal.value = true
}

const search = async () => {
	if (!searchForm.itemcd) return vAlertError('품목을 선택하세요.')
	try {
		const { data } = await api.get('/product/insp-item-std/list', { params: { ...searchForm, cmpycd: authStore.cmpycd } })
		grid?.setData(mapLower(data).map(n => ({
            ...n,
            _status: n.insp_ord ? '' : '입력'
        })))
	} catch (e) { vAlertError('조회 실패') }
}

const addRow = () => {
	if (!searchForm.itemcd) return vAlertError('품목을 먼저 선택하세요.')
	grid?.addRow({ _status: '입력', itemcd: searchForm.itemcd, insp_gb: searchForm.insp_gb, inspcd: '', insp_ord: (grid.getData().length + 1) * 10 }, true)
}

const delRow = () => {
	const sel = grid?.getSelectedRows()
	if (!sel?.length) return vAlertError('삭제할 행을 선택하세요.')
	sel.forEach(row => {
		const d = row.getData()
		if (d._status === '입력') row.delete()
		else row.update({ _status: d._status === '삭제' ? '' : '삭제' })
	})
}

const save = async () => {
	const all = grid?.getData() || []
	const targets = all.filter((d: any) => d._status)
	if (targets.length === 0) return vAlertError('저장할 데이터가 없습니다.')
	if (!confirm('저장하시겠습니까?')) return
	try {
		await api.post('/product/insp-item-std/save', targets.map(d => ({
            ...d, cmpycd: authStore.cmpycd, updemp: authStore.userid,
            state: d._status === '입력' ? 'C' : (d._status === '삭제' ? 'D' : 'U')
        })))
		vAlert('저장되었습니다.'); search()
	} catch (e) { vAlertError('저장 실패') }
}

const initGrid = () => {
  if (!tableRef.value) return;
  grid = new Tabulator(tableRef.value, {
    layout: 'fitColumns', height: '100%', selectable: true,
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
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; font-size: 12px; }
.required::after { content: " *"; color: red; }
</style>
