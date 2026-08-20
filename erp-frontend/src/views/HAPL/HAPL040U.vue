<!--
	=============================================================
	프로그램명	: 부서배부/집계수식관리 (HAPL040U)
	작성일자	: 2025.02.24
	설명        : 부서간 손익 집계 수식 정의
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom px-3 py-2 shadow-sm">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-calculator-fill me-2 text-primary" style="font-size: 18px;"></i>
        관리손익 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">부서집계수식 (HAPL040U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1">
        <button class="btn-erp btn-search" @click="fetchLeftList">조회</button>
        <button class="btn-erp btn-save" @click="save">저장</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 조회 필터 -->
      <div class="card border shadow-sm flex-shrink-0">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense w-100">
            <colgroup><col style="width: 120px;" /><col /></colgroup>
            <tbody>
              <tr>
                <th class="bg-light text-center">기준연월</th>
                <td class="px-2">
                  <div class="d-flex align-items-center gap-1">
                    <select v-model="searchForm.yy" class="form-select form-select-sm" style="width: 100px;" @change="fetchLeftList">
                      <option v-for="y in yearOptions" :key="y" :value="y">{{ y }}년</option>
                    </select>
                    <select v-model="searchForm.mm" class="form-select form-select-sm" style="width: 80px;" @change="fetchLeftList">
                      <option v-for="m in monthOptions" :key="m" :value="m">{{ m }}월</option>
                    </select>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 📂 3. 데이터 영역 -->
      <div class="d-flex gap-2 flex-grow-1 overflow-hidden" style="min-height: 0;">

        <!-- ⬅️ 좌측: 기준 부서 목록 -->
        <div class="card border shadow-sm d-flex flex-column flex-shrink-0 grid-container-left" style="width: 320px; min-width: 320px;">
          <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark text-center">대상 부서 목록</div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="leftGridRef" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>

        <!-- ➡️ 우측: 구성 부서 설정 -->
        <div class="card border shadow-sm flex-grow-1 d-flex flex-column overflow-hidden grid-container-right">
          <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between bg-white">
            <div class="fw-bold small text-dark d-flex align-items-center gap-2">
              <i class="bi bi-gear-fill text-primary"></i>수식 구성 설정
              <span v-if="selectedDeptCD" class="badge bg-primary-subtle text-primary border border-primary-subtle">{{ selectedDeptNM }}</span>
            </div>
            <div class="d-flex gap-1">
              <button v-if="selectedDeptCD" class="btn btn-sm btn-outline-primary py-0 px-2 fw-bold" @click="addRow" style="font-size: 11px;">+ 행추가</button>
              <button v-if="selectedDeptCD" class="btn btn-sm btn-outline-danger py-0 px-2 fw-bold" @click="deleteSelectedRows" style="font-size: 11px;">- 행삭제</button>
            </div>
          </div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="mainGridRef" class="tabulator-instance flex-grow-1"></div>
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

const searchForm = reactive({ yy: String(currentYear), mm: currentMonth })
const selectedDeptCD = ref(''); const selectedDeptNM = ref('')

const leftGridRef = ref<HTMLDivElement | null>(null)
const mainGridRef = ref<HTMLDivElement | null>(null)
let leftGrid: Tabulator | null = null; let mainGrid: Tabulator | null = null

const fetchLeftList = async () => {
  try {
    const res = await api.post('/hapl/HAPL_040U_STR', {
      actkind: 'S1', cmpycd: authStore.cmpycd, stdym: searchForm.yy + searchForm.mm,
      deptcd: '', cdeptcd: '', rate: 0, remark: '', userid: authStore.userid
    })
    leftGrid?.setData(res.data || [])
    selectedDeptCD.value = ''; mainGrid?.clearData();
    if (res.data?.length > 0) vAlert('조회되었습니다.')
  } catch (e) { vAlertError('목록 로드 실패') }
}

const selectDept = async (dept: any) => {
  selectedDeptCD.value = dept.deptcd; selectedDeptNM.value = dept.deptnm
  try {
    const res = await api.post('/hapl/HAPL_040U_STR', {
      actkind: 'S0', cmpycd: authStore.cmpycd, stdym: searchForm.yy + searchForm.mm, deptcd: dept.deptcd,
      cdeptcd: '', rate: 0, remark: '', userid: authStore.userid
    })
    const list = res.data || []
    mainGrid?.setData(list.map((r: any) => ({
      ...r,
      deptcd: r.cdeptcd || r.deptcd,
      _selected: r.useyn === 'Y',
      _status: ''
    })))
  } catch (e) { vAlertError('상세 수식 로드 실패') }
}

const handleOpenHelp = (row: any) => {
  Object.assign(modalProps, {
    title: '구성 부서 선택', path: '/ha00/HA00_00P_STR',
    data: { gubun: 'D0', cmpycd: authStore.cmpycd },
    columns: [{ title: '부서코드', field: 'deptcd', width: 100 }, { title: '부서명', field: 'deptnm', width: 200 }],
    onConfirm: (d: any) => {
      if (mainGrid?.getData().some(it => it.deptcd === d.deptcd)) return vAlertError('이미 등록된 부서입니다.');
      row.update({ deptcd: d.deptcd, deptnm: d.deptnm, _selected: true, _status: '입력' })
    }
  })
  modalVisible.value = true
}

const addRow = () => mainGrid?.addRow({ deptcd: '', deptnm: '(부서 선택)', _selected: true, _status: '입력' }, true)

const deleteSelectedRows = () => {
  const selRows = mainGrid?.getSelectedRows()
  if (!selRows?.length) return vAlertError('행을 선택하세요.')
  selRows.forEach(row => {
    if (row.getData()._status === '입력') row.delete()
    else row.update({ _selected: false, _status: '수정' })
  })
}

const save = async () => {
  if (!selectedDeptCD.value) return vAlertError('대상 부서를 선택하세요.')
  const modified = (mainGrid?.getData() || []).filter(it => it._status !== '')
  if (modified.length === 0) return vAlertError('변경 사항이 없습니다.')

  if (!confirm(`${modified.length}건을 저장하시겠습니까?`)) return

  try {
    const res = await api.post('/hapl/HAPL_040U_STR', {
      actkind: 'A0',
      cmpycd: authStore.cmpycd,
      stdym: (searchForm.yy + searchForm.mm),
      deptcd: selectedDeptCD.value,
      items: modified.map((it: any) => ({
        cdeptcd: it.deptcd,
        rate: (it._selected === true) ? 100 : 0,
        remark: (it.remark || '').trim()
      })),
      userid: authStore.userid
    })

    const resData = res.data?.[0] || {}
    const resultStatus = String(resData.res || resData.result || '').toUpperCase();

    if (resultStatus === 'OK') {
      vAlert('성공적으로 저장되었습니다.');
      selectDept({ deptcd: selectedDeptCD.value, deptnm: selectedDeptNM.value });
    } else {
      vAlertError(resData.msg || '저장 실패');
    }
  } catch (e: any) {
    vAlertError('저장 오류');
  }
}

onMounted(() => {
  if (leftGridRef.value) {
    leftGrid = new Tabulator(leftGridRef.value, {
      layout: 'fitColumns', height: '100%', selectable: 1, placeholder: '데이터 없음',
      columnDefaults: { headerSort: false, headerHozAlign: 'center', vertAlign: "middle" },
      columns: [
        { title: "부서명칭", field: "deptnm", hozAlign: "left", cssClass: 'fw-bold text-primary',
          formatter: (cell) => `<span style="padding-left:${Math.max(0, (Number(cell.getData().lev || 0) - 1)) * 15}px">${cell.getValue() || ''}</span>`
        }
      ]
    })
    leftGrid.on("rowClick", (e, row) => selectDept(row.getData()))
  }

  if (mainGridRef.value) {
    mainGrid = new Tabulator(mainGridRef.value, {
      layout: 'fitColumns', height: '100%', placeholder: '부서를 구성하세요',
      columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
      selectable: true,
      columns: [
        { title: "No", formatter: "rownum", width: 40, hozAlign: "center" },
        { title: "선택", field: "_selected", width: 50, hozAlign: "center",
          formatter: (cell: any) => cell.getValue() ? '<i class="bi bi-check-square-fill text-primary"></i>' : '<i class="bi bi-square"></i>',
          cellClick: (e, cell) => {
            cell.setValue(!cell.getValue());
            if (cell.getRow().getData()._status === '') cell.getRow().update({ _status: '수정' });
          }
        },
        { title: "부서명", field: "deptnm", widthGrow: 1.5, hozAlign: "left", cssClass: 'fw-bold editable-cell',
          cellClick: (e, cell) => { if (cell.getRow().getData()._status === '입력') handleOpenHelp(cell.getRow()) }
        },
        { title: "비고", field: "remark", widthGrow: 2, editor: "input", cssClass: 'editable-cell',
          cellEdited: (cell) => { if (cell.getRow().getData()._status === '') cell.getRow().update({ _status: '수정' }) }
        }
      ]
    })
  }
  fetchLeftList()
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
:deep(.editable-cell) { background-color: #f8faff !important; color: #005a9f !important; cursor: pointer; }
</style>
