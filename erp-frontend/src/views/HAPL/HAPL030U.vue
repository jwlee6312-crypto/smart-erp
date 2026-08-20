<!--
	=============================================================
	프로그램명	: 배부적수관리 (HAPL030U)
	작성일자	: 2025.02.24
	설명        : 부서별 배부 적수 및 배부율 관리
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom shadow-sm bg-white py-2 px-3 sticky-top flex-shrink-0">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-calculator me-2 text-primary" style="font-size: 18px;"></i>
        관리손익 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">배부적수관리 (HAPL030U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1">
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-save" @click="handleBatchSave">저장</button>
      </div>
    </div>

    <!-- 🔍 2. 상단 검색 조건 -->
    <div class="p-2 bg-light flex-shrink-0">
      <div class="card border shadow-sm">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense w-100">
            <colgroup>
              <col style="width: 100px;" /> <col style="width: 250px;" />
              <col style="width: 100px;" /> <col />
            </colgroup>
            <tbody>
              <tr>
                <th class="bg-light text-center">기준연월</th>
                <td>
                  <div class="d-flex align-items-center gap-1 px-1">
                    <select v-model="searchForm.yy" class="form-select form-select-sm" style="width: 100px;" @change="search">
                      <option v-for="y in yearOptions" :key="y" :value="y">{{ y }}년</option>
                    </select>
                    <select v-model="searchForm.mm" class="form-select form-select-sm" style="width: 80px;" @change="search">
                      <option v-for="m in monthOptions" :key="m" :value="m">{{ m }}월</option>
                    </select>
                  </div>
                </td>
                <th class="bg-light text-center">배부기준</th>
                <td class="px-1">
                  <select v-model="searchForm.divcd" class="form-select form-select-sm" style="max-width: 250px;" @change="search">
                    <option v-for="opt in divOptions" :key="opt.code" :value="opt.code">{{ opt.name }}</option>
                  </select>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- 📊 3. 메인 작업 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex gap-2 bg-light pt-0">

      <!-- ⬅️ 좌측: 관리부서 (Cost Center) 목록 -->
      <div class="card border shadow-sm d-flex flex-column overflow-hidden bg-white" style="width: 320px; min-width: 320px;">
        <div class="card-header py-1 px-3 border-bottom fw-bold small text-dark bg-white">관리부서 (Cost Center)</div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="deptGridRef" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>

      <!-- ➡️ 우측: 배부 수익부서 설정 -->
      <div class="card border shadow-sm flex-grow-1 d-flex flex-column overflow-hidden bg-white">
        <div class="card-header py-1 px-3 border-bottom d-flex align-items-center justify-content-between bg-white">
          <div class="d-flex align-items-center gap-3">
            <span class="fw-bold small text-dark"><i class="bi bi-gear-fill me-1 text-primary"></i>배부 수익부서 설정</span>
            <div v-if="selectedDeptCD" class="d-flex gap-1 align-items-center">
              <span class="badge bg-primary-subtle text-primary border border-primary-subtle">{{ selectedDeptNM }}</span>
              <span class="badge bg-secondary-subtle text-secondary border border-secondary-subtle">구성비: {{ gRates.rate1 }}/{{ gRates.rate2 }}/{{ gRates.rate3 }}%</span>
            </div>
          </div>
          <div class="d-flex gap-1">
            <button v-if="selectedDeptCD" class="btn btn-sm btn-outline-primary py-0 px-2 fw-bold" @click="addRow" style="font-size: 11px;">+ 행추가</button>
            <button v-if="selectedDeptCD" class="btn btn-sm btn-outline-danger py-0 px-2 fw-bold" @click="deleteSelectedRows" style="font-size: 11px;">- 행삭제</button>
            <button v-if="selectedDeptCD" class="btn btn-sm btn-primary py-0 px-2 fw-bold" @click="handleGenerateFactors" style="font-size: 11px; background-color: #005a9f;">
              <i class="bi bi-magic me-1"></i> 적수생성
            </button>
          </div>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="mainGridRef" class="tabulator-instance flex-grow-1"></div>
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
import { useCommonHelp } from '@/composables/useCommonHelp'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { modalVisible, modalProps } = useCommonHelp()

const currentYear = new Date().getFullYear()
const currentMonth = (new Date().getMonth() + 1).toString().padStart(2, '0')
const yearOptions = Array.from({ length: 11 }, (_, i) => String(currentYear - i))
const monthOptions = Array.from({ length: 12 }, (_, i) => String(i + 1).padStart(2, '0'))

const searchForm = reactive({ yy: String(currentYear), mm: currentMonth, divcd: '' })
const divOptions = ref<any[]>([]);
const selectedDeptCD = ref(''); const selectedDeptNM = ref('')
const gRates = reactive({ rate1: 0, rate2: 0, rate3: 0 })

const deptGridRef = ref<HTMLDivElement | null>(null); const mainGridRef = ref<HTMLDivElement | null>(null)
let deptGrid: Tabulator | null = null; let mainGrid: Tabulator | null = null

const fetchOptions = async () => {
  try {
    const res = await api.post('/ha00/HA00_00P_STR', { gubun: 'SB', cmpycd: authStore.cmpycd, gbncd: '100' })
    divOptions.value = (res.data || []).map((i: any) => ({ code: i.code || i.divcd, name: i.name || i.divnm }))
    if (divOptions.value.length > 0) searchForm.divcd = divOptions.value[0].code
  } catch (e) { console.error('옵션 로드 실패', e) }
}

const search = async () => {
  try {
    const res = await api.post('/hapl/HAPL_030U_STR', {
      actkind: 'S1', cmpycd: authStore.cmpycd, gubun: '020', stdym: searchForm.yy + searchForm.mm,
      divcd: searchForm.divcd, userid: authStore.userid
    })

    const data = res.data || []
    deptGrid?.setData(data)
    selectedDeptCD.value = ''
    mainGrid?.clearData()

    if (data.length > 0) vAlert('조회되었습니다.')
    else vAlert('조회 결과가 없습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

const selectDept = async (dept: any) => {
  if (!dept.deptcd) return
  selectedDeptCD.value = dept.deptcd; selectedDeptNM.value = dept.deptnm
  try {
    const resCfg = await api.post('/hapl/HAPL_010U_STR', {
      actkind: 'S2', cmpycd: authStore.cmpycd, gubun: '020', divcd: searchForm.divcd
    })
    if (resCfg.data && resCfg.data[0]) {
      const c = resCfg.data[0]
      gRates.rate1 = Number(c.srate || 0); gRates.rate2 = Number(c.mrate || 0); gRates.rate3 = Number(c.erate || 0)
    }

    const res = await api.post('/hapl/HAPL_030U_STR', {
      actkind: 'S0', cmpycd: authStore.cmpycd, gubun: '020', stdym: searchForm.yy + searchForm.mm,
      divcd: searchForm.divcd, deptcd: dept.deptcd, userid: authStore.userid
    })

    const list = res.data || []
    mainGrid?.setData(list.map((r: any) => ({ ...r, _status: '' })))
    nextTick(() => mainGrid?.redraw(true))
  } catch (e) { vAlertError('상세 로드 실패') }
}

const handleOpenHelp = (row: any) => {
  Object.assign(modalProps, {
    title: '배부 수익부서 선택', path: '/ha00/HA00_00P_STR',
    data: { gubun: 'D0', cmpycd: authStore.cmpycd },
    columns: [{ title: '부서코드', field: 'deptcd', width: 100 }, { title: '부서명', field: 'deptnm', width: 200 }],
    onConfirm: (d: any) => {
      if (mainGrid?.getData().some(it => it.deptcd === d.deptcd)) return vAlertError('이미 등록된 부서입니다.');
      row.update({ deptcd: d.deptcd, deptnm: d.deptnm, _status: 'I' })
    }
  })
  modalVisible.value = true
}

const addRow = () => mainGrid?.addRow({ deptcd: '', deptnm: '(클릭하여 부서선택)', divrate1: 0, divrate2: 0, divrate3: 0, _status: 'I' }, true)

const deleteSelectedRows = () => {
  const selRows = mainGrid?.getSelectedRows()
  if (!selRows?.length) return vAlertError('처리할 행을 선택하세요.')
  selRows.forEach(row => {
    if (row.getData()._status === 'I') row.delete()
    else row.update({ _status: 'D', useyn: 'N' })
  })
}

const handleGenerateFactors = async () => {
  if (!confirm("현재 기준 부서의 배부 적수를 자동 생성(저장)하시겠습니까?")) return
  try {
    const res = await api.post('/hapl/HAPL_030U_STR', {
        actkind: 'DR', cmpycd: authStore.cmpycd, gubun: '020', stdym: searchForm.yy + searchForm.mm,
        divcd: searchForm.divcd, deptcd: selectedDeptCD.value, userid: authStore.userid
    })
    vAlert('생성 및 저장이 완료되었습니다.');
    selectDept({ deptcd: selectedDeptCD.value, deptnm: selectedDeptNM.value })
  } catch (e) { vAlertError('생성 오류') }
}

const handleBatchSave = async () => {
  if (!selectedDeptCD.value) return vAlertError('비용 부서를 선택하세요.')
  const modified = mainGrid?.getData().filter(it => it.deptcd && it._status !== '') || []
  if (modified.length === 0) return vAlertError('변경 사항이 없습니다.')

  if (!confirm(`${modified.length}건을 저장하시겠습니까?`)) return

  try {
    const res = await api.post('/hapl/HAPL_030U_STR', {
      actkind: 'A0',
      cmpycd: authStore.cmpycd, gubun: '020', stdym: (searchForm.yy + searchForm.mm).replace(/-/g, ''),
      divcd: searchForm.divcd, deptcd: selectedDeptCD.value,
      items: modified.map((it: any) => ({
        bdeptcd: it.deptcd,
        divrate1: Number(it.divrate1 || 0), divrate2: Number(it.divrate2 || 0), divrate3: Number(it.divrate3 || 0),
        remark: (it.remark || '').trim(), useyn: it._status === 'D' ? 'N' : 'Y'
      })),
      userid: authStore.userid
    })

    const resData = (res.data && res.data[0]) ? res.data[0] : {}
    const resultStatus = String(resData.res || resData.result || '').toUpperCase();
    const resultMsg = resData.msg || resData.message || '저장 오류';

    if (resultStatus === 'OK') {
      vAlert('성공적으로 저장되었습니다.');
      selectDept({ deptcd: selectedDeptCD.value, deptnm: selectedDeptNM.value })
    } else {
      vAlertError(resultMsg);
    }
  } catch (e: any) { vAlertError('저장 실패') }
}

onMounted(() => {
  if (deptGridRef.value) {
    deptGrid = new Tabulator(deptGridRef.value, {
      layout: 'fitColumns', height: '100%', selectable: 1, placeholder: '데이터 없음',
      columnDefaults: { headerSort: false, headerHozAlign: 'center', vertAlign: "middle" },
      columns: [
        { title: "No", formatter: "rownum", width: 40, hozAlign: "center" },
        { title: "부서명칭", field: "deptnm", hozAlign: "left", cssClass: 'fw-bold text-primary',
          formatter: (cell) => `<span style="padding-left:${Math.max(0, (Number(cell.getData().lev || 0) - 1)) * 15}px">${cell.getValue() || ''}</span>`
        }
      ]
    })
    deptGrid.on("rowClick", (e, row) => selectDept(row.getData()))
  }

  if (mainGridRef.value) {
    mainGrid = new Tabulator(mainGridRef.value, {
      layout: 'fitColumns', height: '100%', placeholder: '수익부서를 선택하여 적수를 입력하세요',
      columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
      selectable: true,
      columns: [
        { title: "No", formatter: "rownum", width: 40, hozAlign: "center" },
        { title: "상태", field: "_status", width: 60, hozAlign: "center",
          formatter: (c) => c.getValue() === 'I' ? '<span class="badge bg-primary">입력</span>' : (c.getValue() === 'U' ? '<span class="badge bg-warning text-dark">수정</span>' : (c.getValue() === 'D' ? '<span class="badge bg-danger">삭제</span>' : ''))
        },
        { title: "수익부서 (Profit Center)", field: "deptnm", widthGrow: 1.5, hozAlign: "left", cssClass: 'fw-bold text-dark editable-cell',
          cellClick: (e, cell) => { if (cell.getRow().getData()._status === 'I') handleOpenHelp(cell.getRow()) }
        },
        { title: "적수 1", field: "divrate1", width: 140, hozAlign: "right", editor: "number", cssClass: 'editable-cell',
          formatter: (c) => Number(c.getValue() || 0).toLocaleString(),
          cellEdited: (c) => { if (!c.getRow().getData()._status) c.getRow().update({ _status: 'U' }) }
        },
        { title: "비율 1", field: "rate1", width: 90, hozAlign: "right",
          formatter: (c) => `<span class="fw-bold text-primary">${Number(c.getValue() || 0).toFixed(2)}%</span>`
        },
        { title: "적수 2", field: "divrate2", width: 140, hozAlign: "right", editor: "number", cssClass: 'editable-cell',
          formatter: (c) => Number(c.getValue() || 0).toLocaleString(),
          cellEdited: (c) => { if (!c.getRow().getData()._status) c.getRow().update({ _status: 'U' }) }
        },
        { title: "비율 2", field: "rate2", width: 90, hozAlign: "right",
          formatter: (c) => `<span class="fw-bold text-success">${Number(c.getValue() || 0).toFixed(2)}%</span>`
        },
        { title: "비고", field: "remark", widthGrow: 1.5, editor: "input", cssClass: 'editable-cell', cellEdited: (c) => { if (!c.getRow().getData()._status) c.getRow().update({ _status: 'U' }) } }
      ]
    })
  }
  fetchOptions().then(() => search())
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; border-bottom: 1px solid #dee2e6; }
:deep(.editable-cell) { background-color: #f8faff !important; color: #005a9f !important; cursor: pointer; }
:deep(.tabulator-row:hover) { background-color: #f1f5f9 !important; }
</style>
