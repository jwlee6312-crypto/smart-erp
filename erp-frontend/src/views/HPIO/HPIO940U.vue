<!--
	=============================================================
	프로그램명	: 불량유형 상세등록 (HPIO940U)
	작성일자	: 2025.02.24
	설명        : 제품 검사 상세 불량 유형 등록 (HSOD100U 표준 디자인 및 상태 플래그 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 1. 헤더 영역 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-tools me-2 text-primary" style="font-size: 18px;"></i>
        품질관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        검사결과 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">불량유형 상세등록 (HPIO940U)</span>
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
                <col style="width: 10%" /><col style="width: 23%" />
                <col style="width: 10%" /><col style="width: 24%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light required">검사분류</th>
                <td>
                  <select v-model="searchForm.insp_gb" class="form-select form-select-sm" style="font-size: 12px;" @change="search">
                    <option v-for="opt in inspGbOptions" :key="opt.code" :value="opt.code">{{ opt.cdnm }}</option>
                  </select>
                </td>
                <th class="text-center bg-light required">의뢰일자</th>
                <td>
                  <div class="input-group input-group-sm">
                    <input v-model="searchForm.insp_req_dt_f" type="date" class="form-control" style="font-size: 12px;" />
                    <span class="input-group-text">~</span>
                    <input v-model="searchForm.insp_req_dt_t" type="date" class="form-control" style="font-size: 12px;" />
                  </div>
                </td>
                <th class="text-center bg-light required">라인</th>
                <td>
                  <select v-model="searchForm.linecd" class="form-select form-select-sm" style="font-size: 12px;">
                    <option v-for="item in lineData" :key="item.linecd" :value="item.linecd">{{ item.linenm }}</option>
                  </select>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 3단 분할 레이아웃 -->
      <div class="flex-grow-1 d-flex gap-2 overflow-hidden">
        <!-- [1] 의뢰 목록 -->
        <div class="card border shadow-sm d-flex flex-column overflow-hidden grid-container-left" style="width: 500px; min-width: 500px;">
          <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark d-flex justify-content-between align-items-center">
            <span><i class="bi bi-list-task me-2 text-primary"></i>의뢰 목록</span>
          </div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="leftGridRef" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>

        <!-- [2&3] 항목 및 상세 입력 -->
        <div class="flex-grow-1 d-flex flex-column gap-2 overflow-hidden">
          <!-- 상단 정보 요약 -->
          <div class="card border shadow-sm flex-shrink-0 overflow-hidden bg-light bg-opacity-50 border-start border-4 border-info">
            <div class="card-body p-0 bg-white">
              <table class="erp-table-dense w-100">
                <colgroup>
                  <col style="width: 100px;" /><col style="width: 25%;" />
                  <col style="width: 100px;" /><col style="width: 25%;" />
                  <col style="width: 100px;" /><col />
                </colgroup>
                <tbody>
                  <tr style="background-color: #f0faff;">
                    <th class="text-center bg-info bg-opacity-10">제품명</th>
                    <td class="px-2 py-1"><input v-model="detailForm.itemnm" class="form-control form-control-sm bg-light fw-bold" readonly /></td>
                    <th class="text-center bg-info bg-opacity-10">의뢰번호</th>
                    <td class="px-2 py-1"><input :value="displayReqNo" class="form-control form-control-sm bg-light text-center" readonly /></td>
                    <th class="text-center bg-info bg-opacity-10">생산수량</th>
                    <td class="px-2 py-1"><input v-model="detailForm.prdqty" class="form-control form-control-sm bg-light text-end" readonly /></td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- 하단 병렬 그리드 -->
          <div class="flex-grow-1 d-flex gap-2 overflow-hidden">
            <div class="card border shadow-sm flex-grow-1 d-flex flex-column overflow-hidden">
              <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark">
                <i class="bi bi-card-checklist me-2 text-primary"></i>검사항목별 불합격수
              </div>
              <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
                <div ref="midGridRef" class="tabulator-instance flex-grow-1"></div>
              </div>
            </div>

            <div class="card border shadow-sm flex-grow-1 d-flex flex-column overflow-hidden">
              <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
                <span class="fw-bold small text-dark"><i class="bi bi-tools me-2 text-primary"></i>불량유형 상세</span>
                <div class="d-flex gap-1">
                  <button class="btn btn-sm btn-outline-primary py-0 px-2 fw-bold" @click="addRow" style="font-size: 11px;">+ 추가</button>
                  <button class="btn btn-sm btn-outline-danger py-0 px-2 fw-bold" @click="delRow" style="font-size: 11px;">- 삭제</button>
                </div>
              </div>
              <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
                <div ref="mainGridRef" class="tabulator-instance flex-grow-1"></div>
              </div>
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
import AppAlert from '@/components/AppAlert.vue'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()

const now = new Date(); const firstDay = new Date(now.getFullYear(), now.getMonth(), 1).toISOString().substring(0, 10); const today = now.toISOString().substring(0, 10)
const searchForm = reactive({ insp_gb: '', insp_req_dt_f: firstDay, insp_req_dt_t: today, linecd: '' })
const detailForm = reactive({ insp_req_dt: '', insp_req_no: '', prodid: null, itemnm: '', prdqty: 0, linecd: '', insp_gb: '' })
const displayReqNo = computed(() => detailForm.insp_req_no ? `${detailForm.insp_req_dt}-${detailForm.insp_req_no}` : '')

const inspGbOptions = ref<any[]>([]); const lineData = ref<any[]>([]); const errTypeOptions = ref<Record<string, string>>({}); const selectedInspData = ref<any>(null)
const leftGridRef = ref<HTMLElement | null>(null); const midGridRef = ref<HTMLElement | null>(null); const mainGridRef = ref<HTMLElement | null>(null)
let leftGrid: Tabulator | null = null; let midGrid: Tabulator | null = null; let mainGrid: Tabulator | null = null

const mapLower = (list: any[]) => (list || []).map(i => {
    const n: any = {}; for (let k in i) n[k.toLowerCase()] = i[k]; return n;
})

const initialize = () => {
	searchForm.insp_req_dt_f = firstDay; searchForm.insp_req_dt_t = today
	if (inspGbOptions.value.length > 0) searchForm.insp_gb = inspGbOptions.value[0].code
	if (lineData.value.length > 0) searchForm.linecd = lineData.value[0].linecd
	Object.assign(detailForm, { insp_req_dt: '', insp_req_no: '', prodid: null, itemnm: '', prdqty: 0 })
	leftGrid?.clearData(); midGrid?.clearData(); mainGrid?.clearData()
}

const search = async () => {
	if (!searchForm.insp_gb || !searchForm.linecd) return vAlertError('조회 조건을 확인하세요.')
	try {
		const params = { ...searchForm, cmpycd: authStore.cmpycd, insp_req_dt_f: searchForm.insp_req_dt_f.replaceAll('-', ''), insp_req_dt_t: searchForm.insp_req_dt_t.replaceAll('-', '') }
		const { data } = await api.get('/product/insp-rslt-dtl/req-list', { params })
		leftGrid?.setData(mapLower(data).map(n => ({ ...n, showinspreqno: `${n.insp_req_dt}-${n.insp_req_no}` })))
		if (data?.length > 0) nextTick(() => leftGrid?.selectRow(leftGrid.getRows()[0]))
	} catch (e) {}
}

const fetchMstList = async (rowData: any) => {
	try {
		const { data } = await api.get('/product/insp-rslt-dtl/mst-list', { params: { ...rowData, cmpycd: authStore.cmpycd, insp_gb: searchForm.insp_gb } })
		midGrid?.setData(mapLower(data))
		if (data?.length > 0) nextTick(() => midGrid?.selectRow(midGrid.getRows()[0]))
	} catch (e) {}
}

const fetchDtlList = async (rowData: any) => {
	try {
		const params = { ...detailForm, cmpycd: authStore.cmpycd, insp_gb: searchForm.insp_gb, insp_ord: rowData.insp_ord, inspcd: rowData.inspcd }
		const { data } = await api.get('/product/insp-rslt-dtl/list', { params })
		mainGrid?.setData(mapLower(data).map(n => ({ ...n, _status: '' })))
	} catch (e) {}
}

const save = async () => {
	const all = mainGrid?.getData() || []; const targets = all.filter((d: any) => d._status)
	if (targets.length === 0) return vAlertError('변경사항이 없습니다.')
	const totalErr = all.filter(d => d._status !== '삭제').reduce((acc, cur) => acc + Number(cur.errqty || 0), 0)
	if (Number(selectedInspData.value.errqty) !== totalErr) return vAlertError(`불량수 합계 불일치 (항목:${selectedInspData.value.errqty}, 상세:${totalErr})`)
	if (!confirm('저장하시겠습니까?')) return
	try {
		const payload = targets.map(d => ({ ...d, cmpycd: authStore.cmpycd, updemp: authStore.userid, state: d._status === '입력' ? 'C' : (d._status === '삭제' ? 'D' : 'U') }))
		const { code, message } = await api.post('/product/insp-rslt-dtl/save', payload)
		if (code === 'S') { vAlert(message || '저장되었습니다.'); fetchDtlList(selectedInspData.value) }
	} catch (e) {}
}

const addRow = () => {
	if (!selectedInspData.value) return vAlertError('검사항목을 선택하세요.')
	mainGrid?.addRow({ _status: '입력', ...detailForm, insp_gb: searchForm.insp_gb, insp_ord: selectedInspData.value.insp_ord, inspcd: selectedInspData.value.inspcd, err_type: '', errqty: 0 }, true)
}

const delRow = () => {
	const sel = mainGrid?.getSelectedRows()
	if (!sel?.length) return vAlertError('삭제할 행을 선택하세요.')
	sel.forEach(row => { const d = row.getData(); if (d._status === '입력') row.delete(); else row.update({ _status: d._status === '삭제' ? '' : '삭제' }) })
}

onMounted(async () => {
    try {
        const resLine = await api.post('/hp00/HP00_000S_STR', { gubun: 'L0', cmpycd: authStore.cmpycd, gbncd: 'Y' })
        lineData.value = mapLower(resLine.data)
        const resGb = await api.post('/hp00/HP00_000S_STR', { gubun: 'E0', cmpycd: authStore.cmpycd, gbncd: '820' })
        inspGbOptions.value = mapLower(resGb.data)
        const resErr = await api.post('/hp00/HP00_000S_STR', { gubun: 'E0', cmpycd: authStore.cmpycd, gbncd: '840' })
        errTypeOptions.value = mapLower(resErr.data).reduce((acc: any, cur: any) => ({ ...acc, [cur.code]: cur.cdnm }), {})
        if (inspGbOptions.value.length > 0) searchForm.insp_gb = inspGbOptions.value[0].code
        if (lineData.value.length > 0) searchForm.linecd = lineData.value[0].linecd
    } catch (e) {}

	leftGrid = new Tabulator(leftGridRef.value!, {
		layout: 'fitColumns', height: '100%', selectable: 1,
		columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: 'middle' },
		columns: [
			{ title: '의뢰번호', field: 'insp_req_no', width: 140, hozAlign: 'center', cssClass: 'fw-bold',
              formatter: (cell) => {
                const d = cell.getData();
                return d.insp_req_no ? `${d.insp_req_dt}-${d.insp_req_no}` : '';
              }
            },
			{ title: '제품명', field: 'itemnm', hozAlign: 'left' },
			{ title: '수량', field: 'prdqty', width: 80, hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 } },
			{ title: '불량수', field: 'errqty', width: 80, hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 } }
		]
	})
	leftGrid.on('rowSelected', (row) => { const d = row.getData(); Object.assign(detailForm, d); fetchMstList(d) })
	midGrid = new Tabulator(midGridRef.value!, {
		layout: 'fitColumns', height: '100%', selectable: 1,
		columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: 'middle' },
		columns: [
			{ title: '코드', field: 'inspcd', width: 70, hozAlign: 'center' },
			{ title: '검사항목', field: 'insp_nm', widthGrow: 1, hozAlign: 'left' },
			{ title: '시료수', field: 'sample_cnt', width: 100, hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 } },
			{ title: '불량수', field: 'errqty', width: 100, hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 } }
		]
	})
	midGrid.on('rowSelected', (row) => { selectedInspData.value = row.getData(); fetchDtlList(row.getData()) })
	mainGrid = new Tabulator(mainGridRef.value!, {
        layout: 'fitColumns', height: '100%', selectable: true,
		columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: 'middle' },
        columns: [
            { title: '선택', width: 40, hozAlign: 'center', formatter: "rowSelection", titleFormatter: "rowSelection" },
            { title: "상태", field: "_status", width: 70, hozAlign: "center", formatter: (cell) => {
                const v = cell.getValue();
                if (v === '입력') return '<span class="badge bg-primary">입력</span>';
                if (v === '수정') return '<span class="badge bg-warning text-dark">수정</span>';
                if (v === '삭제') return '<span class="badge bg-danger">삭제</span>';
                return '';
            }},
            { title: '불량유형', field: 'err_type', widthGrow: 1, hozAlign: 'center', editor: 'list', editorParams: { values: errTypeOptions.value }, formatter: (cell) => errTypeOptions.value[cell.getValue()] || cell.getValue() },
            { title: '불량수', field: 'errqty', width: 110, hozAlign: 'right', editor: 'number', formatter: 'money', formatterParams: { precision: 0 } }
        ]
    })
	mainGrid.on('cellEdited', (cell) => { if (cell.getRow().getData()._status !== '입력') cell.getRow().update({ _status: '수정' }) })
	if (searchForm.linecd) search()
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; font-size: 12px; }
.required::after { content: " *"; color: red; }
</style>
