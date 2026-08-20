<!--
	=============================================================
	프로그램명	: 제품검사의뢰 (HPIO900U)
	작성일자	: 2025.02.24
	설명        : 생산 실적 기반 제품 검사 의뢰 등록 (850 검사방법 연동)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-send-check-fill me-2 text-primary" style="font-size: 18px;"></i>
        품질관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        생산품질 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">제품검사의뢰 (HPIO900U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="handleSearch">조회</button>
        <button class="btn-erp btn-save" @click="handleSave">의뢰저장</button>
        <button class="btn-erp btn-delete" @click="handleDelete">의뢰취소</button>
      </div>
    </div>

    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">
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
                  <select v-model="searchForm.insp_gb" class="form-select form-select-sm" style="font-size: 12px;" @change="handleSearch">
                    <option v-for="opt in inspGbOptions" :key="opt.code" :value="opt.code">{{ opt.cdnm }}</option>
                  </select>
                </td>
                <th class="text-center bg-light required">라인</th>
                <td>
                  <select v-model="searchForm.linecd" class="form-select form-select-sm" style="font-size: 12px;">
                    <option v-for="item in lineData" :key="item.linecd" :value="item.linecd">{{ item.linenm }}</option>
                  </select>
                </td>
                <th class="text-center bg-light required">생산일자</th>
                <td>
                  <div class="input-group input-group-sm">
                    <input v-model="searchForm.proymd_fr" type="date" class="form-control" style="font-size: 12px;" />
                    <span class="input-group-text">~</span>
                    <input v-model="searchForm.proymd_to" type="date" class="form-control" style="font-size: 12px;" />
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="card border shadow-sm flex-shrink-0 overflow-hidden border-start border-4 border-info">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense w-100">
            <colgroup>
              <col style="width: 120px;" /><col />
            </colgroup>
            <tbody>
              <tr style="background-color: #f0faff;">
                <th class="text-center bg-info bg-opacity-10 required">검사요청일자</th>
                <td class="px-2 py-1">
                  <input v-model="requestForm.insp_req_dt" type="date" class="form-control form-control-sm d-inline-block" style="width: 150px; font-size: 12px;" />
                  <span class="ms-3 small text-secondary"><i class="bi bi-info-circle me-1"></i>선택한 항목들의 검사방법을 확인 후 저장하세요.</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="card border shadow-sm flex-grow-1 d-flex flex-column overflow-hidden grid-container-right">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark"><i class="bi bi-list-check me-2 text-primary"></i>의뢰 대상 리스트</span>
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

const now = new Date()
const firstDay = new Date(now.getFullYear(), now.getMonth(), 1).toISOString().substring(0, 10)
const today = now.toISOString().substring(0, 10)

const searchForm = reactive({ insp_gb: '', linecd: '', proymd_fr: firstDay, proymd_to: today })
const requestForm = reactive({ insp_req_dt: today })

const inspGbOptions = ref<any[]>([])
const lineData = ref<any[]>([])
const inspHowOptions = ref<Record<string, string>>({}) // 💡 850번 코드 옵션

const tableRef = ref<HTMLElement | null>(null)
let grid: Tabulator | null = null

const mapLower = (list: any[]) => (list || []).map(i => {
    const n: any = {}; for (let k in i) n[k.toLowerCase()] = i[k]; return n;
})

const initialize = () => {
	searchForm.proymd_fr = firstDay; searchForm.proymd_to = today; requestForm.insp_req_dt = today
	if (inspGbOptions.value.length > 0) searchForm.insp_gb = inspGbOptions.value[0].code
	if (lineData.value.length > 0) searchForm.linecd = lineData.value[0].linecd
	grid?.clearData()
}

const handleSearch = async () => {
	if (!searchForm.insp_gb || !searchForm.linecd) return vAlertError('조회 조건을 확인하세요.')
	try {
		const params = { ...searchForm, cmpycd: authStore.cmpycd, proymd_fr: searchForm.proymd_fr.replaceAll('-', ''), proymd_to: searchForm.proymd_to.replaceAll('-', '') }
		const { data } = await api.get('/product/insp-req/target-list', { params })
		grid?.setData(mapLower(data).map((d: any) => ({
            ...d,
            _status: d.insp_req_no ? '의뢰완료' : '입력',
            insp_how: d.insp_how || '100' // 💡 기본값 '전수검사'
        })))
	} catch (e) { vAlertError('조회 실패') }
}

const handleSave = async () => {
	const sel = grid?.getSelectedData().filter((d: any) => d._status === '입력')
	if (!sel?.length) return vAlertError('의뢰 대기 중인 항목을 선택하세요.')
	if (!confirm('의뢰를 생성하시겠습니까?')) return
	try {
		const payload = sel.map(d => ({
			...d,
            state: 'C',
            cmpycd: authStore.cmpycd,
			insp_gb: searchForm.insp_gb,
			insp_req_dt: requestForm.insp_req_dt.replaceAll('-', ''),
			updemp: authStore.userid
		}))
		const { code, message } = await api.post('/product/insp-req/save', payload)
		if (code === 'S') { vAlert(message || '완료되었습니다.'); handleSearch() }
	} catch (e) { vAlertError('저장 실패') }
}

const handleDelete = async () => {
	const sel = grid?.getSelectedData()
	const targets = (sel || []).filter((d: any) => d.insp_req_no)
	if (targets.length === 0) return vAlertError('취소할 의뢰 건을 선택하세요.')
	if (!confirm('정말 취소하시겠습니까?')) return
	try {
		for (const item of targets) {
			await api.post('/product/insp-req/delete', {
				cmpycd: authStore.cmpycd,
				insp_gb: searchForm.insp_gb,
				insp_req_dt: item.insp_req_dt,
				insp_req_no: item.insp_req_no
			})
		}
		vAlert('취소되었습니다.'); handleSearch()
	} catch (e) { vAlertError('취소 실패') }
}

const initGrid = () => {
  if (!tableRef.value) return;
  grid = new Tabulator(tableRef.value, {
    layout: 'fitColumns', height: '100%', selectable: true,
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: '', field: 'chk', width: 40, hozAlign: 'center', formatter: "rowSelection", titleFormatter: "rowSelection" },
      { title: "상태", field: "_status", width: 90, hozAlign: "center", formatter: (cell) => {
          const v = cell.getValue();
          if (v === '입력') return '<span class="badge bg-primary">의뢰대기</span>';
          if (v === '의뢰완료') return '<span class="badge bg-success">의뢰완료</span>';
          return '';
      }},
      { title: '생산일자', field: 'proymd', width: 110, hozAlign: 'center', formatter: (c) => {
          const v = c.getValue(); return v && v.length === 8 ? `${v.substring(0, 4)}-${v.substring(4, 6)}-${v.substring(6, 8)}` : v;
      }},
      { title: '공정명', field: 'prognm', width: 120 },
      { title: '제품명', field: 'itemnm', minWidth: 180, cssClass: 'fw-bold text-primary' },
      { title: '생산량', field: 'prdqty', width: 100, hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 } },
      { title: '판정결과', field: 'judg_cd', width: 90, hozAlign: 'center', formatter: (cell) => {
          const v = cell.getValue();
          if (v === '100') return '<span class="badge bg-success">합격</span>';
          if (v === '200') return '<span class="badge bg-danger">불합격</span>';
          if (v === '300') return '<span class="badge bg-warning text-dark">특채</span>';
          return v ? `<span class="badge bg-light text-dark">${v}</span>` : '';
      }},
      {
        title: '검사방법', field: 'insp_how', width: 200, hozAlign: 'center',
        editor: "list", editorParams: { values: inspHowOptions.value },
        formatter: (cell) => inspHowOptions.value[cell.getValue()] || cell.getValue()
      },
      {
        title: '의뢰번호', field: 'insp_req_no', width: 130, hozAlign: 'center',
        formatter: (cell) => {
          const d = cell.getData();
          return d.insp_req_no ? `${d.insp_req_dt}-${d.insp_req_no}` : '';
        }
      }
    ]
  })
}

onMounted(async () => {
    try {
        const resLine = await api.post('/hp00/HP00_000S_STR', { gubun: 'L0', cmpycd: authStore.cmpycd, gbncd: 'Y' })
        lineData.value = mapLower(resLine.data)
        const resGb = await api.post('/hp00/HP00_000S_STR', { gubun: 'E0', cmpycd: authStore.cmpycd, gbncd: '820' })
        inspGbOptions.value = mapLower(resGb.data)

        // 🚀 [신규] 850번 검사방법 코드 로드
        const resHow = await api.post('/hp00/HP00_000S_STR', { gubun: 'E0', cmpycd: authStore.cmpycd, gbncd: '850' })
        inspHowOptions.value = mapLower(resHow.data).reduce((acc: any, cur: any) => ({ ...acc, [cur.code]: cur.cdnm }), {})

        if (inspGbOptions.value.length > 0) searchForm.insp_gb = inspGbOptions.value[0].code
        if (lineData.value.length > 0) searchForm.linecd = lineData.value[0].linecd
    } catch (e) {}

    nextTick(initGrid);
	if (searchForm.linecd) handleSearch()
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; font-size: 12px; }
:deep(.tabulator-cell[tabulator-field="prdqty"]) { justify-content: flex-end !important; }
.required::after { content: " *"; color: red; }
</style>
