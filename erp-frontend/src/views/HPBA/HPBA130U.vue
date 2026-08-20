<!--
	=============================================================
	프로그램명	: 라인관리
	작성일자	: 2025.02.24
	작성자	    : AI Assistant
	설명        : 생산 라인 정보 등록 및 부서 연동 관리
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container">
    <!-- 🚀 1. 상단 액션 바 (표준 규격 및 함수명 통일) -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-gear-fill me-2 text-primary" style="font-size: 18px;"></i>
        기본정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        <span class="text-primary fw-bolder">라인관리 (HPBA130U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-save" @click="save">저장</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2">
      <!-- 🅰️ 입력 폼 영역 (표준 erp-table-full 및 80px 레이블 규격) -->
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
                <th class="required">라인코드</th>
                <td>
                  <input v-model="formData.linecd" type="text" class="form-control text-center fw-bold" style="width: 100px;" maxlength="3" :readonly="formData.actkind === 'U'" />
                </td>
                <th class="required">라 인 명</th>
                <td>
                  <input v-model="formData.linenm" type="text" class="form-control" placeholder="라인명 입력" />
                </td>
                <th class="required">부 서</th>
                <td>
                  <div class="input-group" style="width: 220px;">
                    <input v-model="formData.deptcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
                    <input v-model="formData.deptnm" type="text" class="form-control" placeholder="부서 선택" @keyup.enter="openHelp('DEPT')" />
                    <button class="btn" @click="openHelp('DEPT')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
              </tr>
              <tr>
                <th>비 고</th>
                <td>
                  <input v-model="formData.bigo" type="text" class="form-control" placeholder="비고 사항 입력" />
                </td>
                <th>출현순서</th>
                <td>
                  <input v-model="formData.dspord" type="number" class="form-control text-end" style="width: 80px;" />
                </td>
                <th>사용여부</th>
                <td>
                  <div class="form-check form-switch ms-2">
                    <input v-model="formData.useyn" class="form-check-input" type="checkbox" true-value="Y" false-value="N" id="useYn130">
                    <label class="form-check-label small fw-bold" for="useYn130">{{ formData.useyn === 'Y' ? '사용함' : '미사용' }}</label>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 🅲 그리드 영역 -->
      <div class="card border-0 shadow-sm flex-grow-1 overflow-hidden d-flex flex-column">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex justify-content-between align-items-center">
          <span class="fw-bold small text-dark"><i class="bi bi-list-task me-1 text-primary"></i> 생산 라인 목록</span>
          <span class="text-muted small" style="font-size: 11px;">목록 클릭 시 수정 모드로 전환됩니다.</span>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="gridElement" class="tabulator-instance flex-grow-1"></div>
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

const formData = reactive({ actkind: 'A', linecd: '', linenm: '', deptcd: '', deptnm: '', bigo: '', dspord: 0, useyn: 'Y' })
const gridElement = ref<HTMLElement | null>(null)
let grid: Tabulator | null = null
const itemCount = ref(0)

const initGrid = () => {
  if (gridElement.value) {
    grid = new Tabulator(gridElement.value, {
      layout: "fitColumns", height: "100%", placeholder: "데이터가 없습니다.", selectable: 1,
      columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
      columns: [
        { title: "라인코드", field: "linecd", width: 100, hozAlign: "center", cssClass: "fw-bold text-primary" },
        { title: "라 인 명", field: "linenm", minWidth: 200, cssClass: "fw-bold" },
        { title: "부 서 명", field: "deptnm", width: 200 },
        { title: "비 고", field: "bigo", minWidth: 200 },
        { title: "순서", field: "dspord", width: 80, hozAlign: "center" },
        { title: "사용", field: "useyn", width: 80, hozAlign: "center",
          formatter: (cell) => {
            const val = String(cell.getValue() || '').trim().toUpperCase();
            return val === 'Y' ? '<b class="text-primary">사용</b>' : '';
          }
        }
       ],
    })
    grid.on("rowClick", (e, row) => { Object.assign(formData, row.getData()); formData.actkind = 'U' })
  }
}

async function search() {
  try {
    const res = await api.post('/hpba/HPBA_130U_STR', { actkind: 'S0', cmpycd: authStore.cmpycd })
    grid?.setData(res.data); itemCount.value = res.data.length; vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

async function save() {
  if (!formData.linecd || !formData.linenm || !formData.deptcd) return vAlertError('라인코드, 라인명, 부서는 필수 입력 사항입니다.')
  if (!confirm(formData.actkind === 'A' ? '새 라인을 등록하시겠습니까?' : '수정된 정보를 저장하시겠습니까?')) return
  try {
    await api.post('/hpba/HPBA_130U_STR', { ...formData, cmpycd: authStore.cmpycd, userid: authStore.userid })
    vAlert('정상적으로 처리되었습니다.'); search(); initialize()
  } catch (e) { vAlertError('저장 중 오류 발생') }
}

function initialize() {
  resetForm(formData); Object.assign(formData, { actkind: 'A', useyn: 'Y', dspord: 0 })
}

const modalVisible = ref(false); const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })
function openHelp(type: string) {
  if (type === 'DEPT') {
    Object.assign(modalProps, {
      title: '부서 선택', path: '/ha00/HA00_00P_STR', defaultField: 'deptnm',
      data: { gubun: 'D0', cmpycd: authStore.cmpycd },
      columns: [{ title: '코드', field: 'deptcd', width: 80 }, { title: '부서명', field: 'deptnm', width: 200 }],
      onConfirm: (data: any) => { formData.deptcd = data.deptcd; formData.deptnm = data.deptnm }
    }); modalVisible.value = true
  }
}

onMounted(() => { nextTick(() => { initGrid(); search(); }) })
</script>
