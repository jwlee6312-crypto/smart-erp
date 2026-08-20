<!--
	=============================================================
	프로그램명	: 제품검사결과 등록 (HPIO930U)
	작성일자	: 2025.02.24
	설명        : 생산 검사의뢰에 대한 결과 등록 (클릭 시 우측 조회 완벽 해결)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-ui-checks me-2 text-primary" style="font-size: 18px;"></i>
        품질관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        검사결과 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">제품검사결과 등록 (HPIO930U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-save" @click="save">결과저장</button>
        <button class="btn-erp btn-delete" @click="deleteResult">결과삭제</button>
      </div>
    </div>

    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">
      <!-- 조회 조건 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup><col style="width: 10%" /><col style="width: 23%" /><col style="width: 10%" /><col style="width: 23%" /><col style="width: 10%" /><col style="width: 24%" /></colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light required">검사분류</th>
                <td><select v-model="searchForm.insp_gb" class="form-select form-select-sm" @change="search"><option v-for="opt in inspGbOptions" :key="opt.code" :value="opt.code">{{ opt.cdnm }}</option></select></td>
                <th class="text-center bg-light required">의뢰일자</th>
                <td><div class="input-group input-group-sm"><input v-model="searchForm.insp_req_dt_f" type="date" class="form-control" /><span class="input-group-text">~</span><input v-model="searchForm.insp_req_dt_t" type="date" class="form-control" /></div></td>
                <th class="text-center bg-light required">라인</th>
                <td><select v-model="searchForm.linecd" class="form-select form-select-sm"><option v-for="item in lineData" :key="item.linecd" :value="item.linecd">{{ item.linenm }}</option></select></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="flex-grow-1 d-flex gap-2 overflow-hidden">
        <!-- [좌측] 의뢰 목록 -->
        <div class="card border shadow-sm d-flex flex-column overflow-hidden grid-container-left" style="width: 380px; min-width: 380px;">
          <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark">
            <i class="bi bi-list-task me-2 text-primary"></i>의뢰 목록
          </div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column"><div ref="leftGridRef" class="tabulator-instance flex-grow-1"></div></div>
        </div>

        <!-- [우측] 결과 입력 -->
        <div class="flex-grow-1 d-flex flex-column gap-2 overflow-hidden">
          <div class="card border shadow-sm flex-shrink-0 overflow-hidden border-start border-4 border-info">
            <div class="card-body p-0 bg-white">
              <table class="erp-table-dense w-100">
                <colgroup><col style="width: 100px;" /><col style="width: 25%;" /><col style="width: 100px;" /><col style="width: 25%;" /><col style="width: 100px;" /><col /></colgroup>
                <tbody>
                  <tr style="background-color: #f0faff;">
                    <th class="text-center bg-info bg-opacity-10">의뢰번호</th>
                    <td class="px-2 py-1"><input :value="displayReqNo" class="form-control form-control-sm bg-light text-center fw-bold" readonly /></td>
                    <th class="text-center bg-info bg-opacity-10">제품명</th>
                    <td class="px-2 py-1"><input v-model="detailForm.itemnm" class="form-control form-control-sm bg-light fw-bold" readonly /></td>
                    <th class="text-center bg-info bg-opacity-10 required">검사일자</th>
                    <td class="px-2 py-1"><input v-model="detailForm.insp_dt" type="date" class="form-control form-control-sm" /></td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <div class="card border shadow-sm flex-grow-1 d-flex flex-column overflow-hidden grid-container-right">
            <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark"><i class="bi bi-card-checklist me-2 text-primary"></i>검사항목 결과 입력</div>
            <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column"><div ref="mainGridRef" class="tabulator-instance flex-grow-1"></div></div>
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
const detailForm = reactive({ insp_req_dt: '', insp_req_no: '', prodid: null, itemnm: '', insp_dt: today, prdqty: 0 })
const displayReqNo = computed(() => detailForm.insp_req_no ? `${detailForm.insp_req_dt}-${detailForm.insp_req_no}` : '')

const inspGbOptions = ref<any[]>([]); const lineData = ref<any[]>([])
const leftGridRef = ref<HTMLElement | null>(null); const mainGridRef = ref<HTMLElement | null>(null)
let leftGrid: Tabulator | null = null; let mainGrid: Tabulator | null = null

const mapLower = (list: any[]) => (list || []).map(i => {
    const n: any = {}; for (let k in i) n[k.toLowerCase()] = i[k]; return n;
})

const initialize = () => {
	searchForm.insp_req_dt_f = firstDay; searchForm.insp_req_dt_t = today
	if (inspGbOptions.value.length > 0) searchForm.insp_gb = inspGbOptions.value[0].code
	if (lineData.value.length > 0) searchForm.linecd = lineData.value[0].linecd
	Object.assign(detailForm, { insp_req_dt: '', insp_req_no: '', prodid: null, itemnm: '', insp_dt: today })
	leftGrid?.clearData(); mainGrid?.clearData()
}

const search = async () => {
	if (!searchForm.insp_gb || !searchForm.linecd) return vAlertError('조회 조건을 확인하세요.')
	try {
		const params = { ...searchForm, cmpycd: authStore.cmpycd, insp_req_dt_f: searchForm.insp_req_dt_f.replaceAll('-', ''), insp_req_dt_t: searchForm.insp_req_dt_t.replaceAll('-', '') }
		const { data } = await api.get('/product/insp-rslt/req-list', { params })
		leftGrid?.setData(mapLower(data))
	} catch (e) { vAlertError('조회 실패') }
}

const fetchResultList = async (rowData: any) => {
    if (!rowData.insp_req_no) return;
	try {
        // 🚀 혁신: 파라미터를 가장 확실한 값으로만 구성하여 조회
		const { data } = await api.get('/product/insp-rslt/list', {
            params: {
                cmpycd: authStore.cmpycd,
                insp_req_dt: rowData.insp_req_dt,
                insp_req_no: rowData.insp_req_no
            }
        })
		mainGrid?.setData(mapLower(data).map(n => ({ ...n, _status: n.insp_dt ? '' : '입력' })))
	} catch (e) { vAlertError('상세 조회 실패') }
}

const save = async () => {
	const all = mainGrid?.getData() || []; const targets = all.filter((d: any) => d._status)
	if (targets.length === 0) return vAlertError('저장할 데이터가 없습니다.')
	if (!confirm('저장하시겠습니까?')) return
	try {
		const payload = targets.map(d => ({ ...d, cmpycd: authStore.cmpycd, insp_dt: detailForm.insp_dt.replaceAll('-', ''), updemp: authStore.userid, state: d._status === '입력' ? 'C' : 'U' }))
		const { code } = await api.post('/product/insp-rslt/save', payload)
		if (code === 'S') {
            vAlert('저장되었습니다.');
            await search();
            await fetchResultList(detailForm);
        }
	} catch (e) { vAlertError('저장 실패') }
}

const deleteResult = async () => {
	if (!detailForm.insp_req_no) return vAlertError('데이터가 없습니다.')
	if (!confirm('삭제하시겠습니까?')) return
	try {
		const { code } = await api.post('/product/insp-rslt/delete', { ...detailForm, cmpycd: authStore.cmpycd, insp_gb: searchForm.insp_gb })
		if (code === 'S') { vAlert('삭제되었습니다.'); search(); mainGrid?.clearData() }
	} catch (e) {}
}

onMounted(async () => {
    try {
        const resLine = await api.post('/hp00/HP00_000S_STR', { gubun: 'L0', cmpycd: authStore.cmpycd, gbncd: 'Y' })
        lineData.value = mapLower(resLine.data)
        const resGb = await api.post('/hp00/HP00_000S_STR', { gubun: 'E0', cmpycd: authStore.cmpycd, gbncd: '820' })
        inspGbOptions.value = mapLower(resGb.data)
        if (inspGbOptions.value.length > 0) searchForm.insp_gb = inspGbOptions.value[0].code
        if (lineData.value.length > 0) searchForm.linecd = lineData.value[0].linecd
    } catch (e) {}

	leftGrid = new Tabulator(leftGridRef.value!, {
		layout: 'fitColumns', height: '100%', selectable: 1,
		columns: [
			{ title: '상태', field: 'state', width: 85, hozAlign: 'center', formatter: (cell) => cell.getValue() === 'Y' ? '<span class="badge bg-success">등록완료</span>' : '<span class="badge bg-secondary">의뢰대기</span>' },
			{ title: '의뢰번호', field: 'insp_req_no', width: 140, hozAlign: 'center', cssClass: 'fw-bold text-primary', formatter: (cell) => { const d = cell.getData(); return d.insp_req_no ? `${d.insp_req_dt}-${d.insp_req_no}` : ''; } },
			{ title: '제품명', field: 'itemnm', hozAlign: 'left' }
		]
	})

	leftGrid.on('rowClick', (e, row) => {
        const d = row.getData();
        Object.assign(detailForm, d);
        if (d.insp_dt) detailForm.insp_dt = d.insp_dt.replace(/(\d{4})(\d{2})(\d{2})/, '$1-$2-$3');
        fetchResultList(d);
    })

	mainGrid = new Tabulator(mainGridRef.value!, {
		layout: 'fitColumns', height: '100%',
		columns: [
            { title: "상태", field: "_status", width: 70, hozAlign: "center", headerHozAlign: "center", formatter: (cell) => { const v = cell.getValue(); if (v === '입력') return '<span class="badge bg-primary">입력</span>'; if (v === '수정') return '<span class="badge bg-warning text-dark">수정</span>'; return ''; } },
			{ title: '순번', field: 'insp_ord', width: 80, hozAlign: 'center', headerHozAlign: 'center' },
			{ title: '검사항목', field: 'insp_nm', widthGrow: 2, hozAlign: 'left', headerHozAlign: 'center' },
			{ title: '시료수', field: 'sample_cnt', width: 150, hozAlign: 'right', headerHozAlign: 'center', editor: 'number', formatter: 'money', formatterParams: { precision: 0 } },
			{ title: '불합격수', field: 'errqty', width: 150, hozAlign: 'right', headerHozAlign: 'center', editor: 'number', formatter: 'money', formatterParams: { precision: 0 } }
		]
	})
	mainGrid.on('cellEdited', (cell) => { if (cell.getRow().getData()._status !== '입력') cell.getRow().update({ _status: '수정' }) })
	if (searchForm.linecd) search()
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; font-size: 12px; }
:deep(.tabulator-cell[tabulator-field="sample_cnt"]), :deep(.tabulator-cell[tabulator-field="errqty"]) { justify-content: flex-end !important; }
.required::after { content: " *"; color: red; }
</style>
