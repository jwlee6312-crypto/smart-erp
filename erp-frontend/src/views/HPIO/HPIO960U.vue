<!--
	=============================================================
	프로그램명	: 검사항목 불량유형 매핑 (HPIO960U)
	작성일자	: 2025.02.24
	설명        : 검사항목별로 발생 가능한 불량유형을 매핑하여 관리 (HSOD100U 디자인 및 상태 플래그 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 1. 헤더 영역 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-shield-check-fill me-2 text-primary" style="font-size: 18px;"></i>
        품질관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        기준관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">검사항목 불량유형 매핑 (HPIO960U)</span>
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

      <!-- 메인 콘텐츠 (좌/우 분할) -->
      <div class="flex-grow-1 d-flex gap-2 overflow-hidden">
        <!-- [좌측] 검사항목 목록 -->
        <div class="card border shadow-sm d-flex flex-column overflow-hidden grid-container-left" style="width: 350px; min-width: 350px;">
          <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark d-flex justify-content-between align-items-center">
            <span><i class="bi bi-list-task me-2 text-primary"></i>검사항목 목록</span>
          </div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="leftTableRef" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>

        <!-- [우측] 매핑 설정 영역 -->
        <div class="flex-grow-1 d-flex flex-column gap-2 overflow-hidden">
          <!-- 선택 항목 정보 카드 -->
          <div class="card border shadow-sm flex-shrink-0 overflow-hidden bg-light bg-opacity-50 border-start border-4 border-info">
            <div class="card-body p-0 bg-white">
              <table class="erp-table-dense w-100">
                <colgroup><col style="width: 120px;" /><col /></colgroup>
                <tbody>
                  <tr style="background-color: #f0faff;">
                    <th class="text-center bg-info bg-opacity-10">선택 검사항목</th>
                    <td class="px-2 py-1">
                      <input v-model="detailForm.insp_nm" class="form-control form-control-sm bg-light fw-bold text-primary" readonly placeholder="좌측 목록에서 선택하세요." />
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- 불량유형 매핑 그리드 카드 -->
          <div class="card border shadow-sm flex-grow-1 d-flex flex-column overflow-hidden grid-container-right">
            <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
              <span class="fw-bold small text-dark"><i class="bi bi-list-stars me-2 text-primary"></i>불량유형 매핑 설정</span>
              <div class="d-flex gap-1">
                <button class="btn btn-sm btn-outline-primary py-0 px-2 fw-bold" @click="addRow" style="font-size: 11px;">+ 행추가</button>
                <button class="btn btn-sm btn-outline-danger py-0 px-2 fw-bold" @click="delRow" style="font-size: 11px;">- 행삭제</button>
              </div>
            </div>
            <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
              <div ref="mainTableRef" class="tabulator-instance flex-grow-1"></div>
            </div>
          </div>
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
const detailForm = reactive({ inspcd: '', insp_nm: '', insp_gb: '' })

const inspGbOptions = ref<any[]>([]); const errTypeOptions = ref<Record<string, string>>({})
const leftTableRef = ref<HTMLElement | null>(null); const mainTableRef = ref<HTMLElement | null>(null)
let leftTable: Tabulator | null = null; let mainTable: Tabulator | null = null

const mapLower = (list: any[]) => {
    if (!Array.isArray(list)) return [];
    return list.map(i => {
        const n: any = {}; for (let k in i) n[k.toLowerCase()] = i[k]; return n;
    });
}

const initialize = () => {
	if (inspGbOptions.value.length > 0) searchForm.insp_gb = inspGbOptions.value[0].code
	detailForm.inspcd = ''; detailForm.insp_nm = ''; detailForm.insp_gb = ''
	leftTable?.clearData(); mainTable?.clearData()
}

const search = async () => {
	if (!searchForm.insp_gb) return vAlertError('검사분류를 선택하세요.')
	try {
		const { data } = await api.get('/product/insp-err-type/insp-list', { params: { cmpycd: authStore.cmpycd, insp_gb: searchForm.insp_gb } })
		leftTable?.setData(mapLower(data))
		mainTable?.clearData(); detailForm.inspcd = ''; detailForm.insp_nm = ''
	} catch (e) { vAlertError('조회 실패') }
}

const selectRecords = async (rowData: any) => {
    if (!rowData.inspcd) return;
	try {
		const { data } = await api.get('/product/insp-err-type/err-list', {
            params: {
                cmpycd: authStore.cmpycd,
                insp_gb: searchForm.insp_gb,
                inspcd: rowData.inspcd
            }
        })
		mainTable?.setData(mapLower(data).map(n => ({ ...n, _status: '' })))
	} catch (e) {}
}

const save = async () => {
	const all = mainTable?.getData() || []; const targets = all.filter((d: any) => d._status)
	if (targets.length === 0) return vAlertError('변경사항이 없습니다.')
	if (!confirm('저장하시겠습니까?')) return
	try {
		const payload = targets.map(d => ({
            ...d,
            cmpycd: authStore.cmpycd,
            updemp: authStore.userid,
            state: d._status === '입력' ? 'C' : (d._status === '삭제' ? 'D' : 'U')
        }))
		const { code } = await api.post('/product/insp-err-type/save', payload)
		if (code === 'S') { vAlert('저장되었습니다.'); selectRecords(detailForm) }
	} catch (e) {}
}

const addRow = () => {
	if (!detailForm.inspcd) return vAlertError('검사항목을 선택하세요.')
	mainTable?.addRow({ _status: '입력', inspcd: detailForm.inspcd, insp_gb: searchForm.insp_gb, err_type: '' }, true)
}

const delRow = () => {
	const sel = mainTable?.getSelectedRows()
	if (!sel?.length) return vAlertError('삭제할 행을 선택하세요.')
	sel.forEach(row => {
        const d = row.getData();
        if (d._status === '입력') row.delete();
        else row.update({ _status: d._status === '삭제' ? '' : '삭제' })
    })
}

const initGrids = () => {
    if (!leftTableRef.value || !mainTableRef.value) return;

    leftTable = new Tabulator(leftTableRef.value, {
        layout: 'fitColumns', height: '100%', selectable: 1,
        placeholder: "데이터가 없습니다.",
        columns: [
            { title: '코드', field: 'inspcd', width: 100, hozAlign: 'center' },
            { title: '검사항목명', field: 'insp_nm', hozAlign: 'left' }
        ]
    })

    // 💡 rowClick으로 즉시 전달 보장
	leftTable.on('rowClick', (e, row) => {
        const d = row.getData();
        detailForm.inspcd = d.inspcd;
        detailForm.insp_nm = d.insp_nm;
        detailForm.insp_gb = searchForm.insp_gb;
        selectRecords(d);
    })

	mainTable = new Tabulator(mainTableRef.value, {
        layout: 'fitColumns', height: '100%', selectable: true,
        placeholder: "검사항목을 선택하세요.",
        columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
        columns: [
            { title: '선택', width: 40, hozAlign: 'center', formatter: "rowSelection", titleFormatter: "rowSelection" },
            {
                title: "상태", field: "_status", width: 70, hozAlign: "center",
                formatter: "html",
                formatterParams: (cell: any) => {
                    const val = cell.getValue();
                    if (val === '입력') return '<span class="badge bg-primary">입력</span>';
                    if (val === '수정') return '<span class="badge bg-warning text-dark">수정</span>';
                    if (val === '삭제') return '<span class="badge bg-danger">삭제</span>';
                    return '';
                }
            },
            {
                title: '불량유형명', field: 'err_type', widthGrow: 1, hozAlign: 'center',
                editor: 'list', editorParams: { values: errTypeOptions.value },
                formatter: (cell) => errTypeOptions.value[cell.getValue()] || cell.getValue()
            }
        ]
    })

	mainTable.on('cellEdited', (cell) => {
        const d = cell.getRow().getData();
        if (d._status !== '입력' && d._status !== '삭제') cell.getRow().update({ _status: '수정' })
    })
}

onMounted(async () => {
    try {
        const resGb = await api.post('/hp00/HP00_000S_STR', { gubun: 'E0', cmpycd: authStore.cmpycd, gbncd: '820' })
        inspGbOptions.value = mapLower(resGb.data)
        const resErr = await api.post('/hp00/HP00_000S_STR', { gubun: 'E0', cmpycd: authStore.cmpycd, gbncd: '840' })
        errTypeOptions.value = mapLower(resErr.data).reduce((acc: any, cur: any) => ({ ...acc, [cur.code]: cur.cdnm }), {})
        if (inspGbOptions.value.length > 0) searchForm.insp_gb = inspGbOptions.value[0].code
    } catch (e) {}

    nextTick(() => {
        initGrids();
        if (searchForm.insp_gb) search();
    });
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; font-size: 12px; }
.required::after { content: " *"; color: red; }
</style>
