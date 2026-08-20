<!--
	=============================================================
	프로그램명	: 부서별 기초재고 등록
	작성일자	: 2025.02.24
	작성자	    : AI Assistant
	설명        : 부서별/품목별 초기 이월 재고 수량 및 금액 등록 관리 (디자인 표준화 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container">
    <!-- 🚀 1. 상단 액션 바 (표준 규격 및 함수명 통일) -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-box-seam-fill me-2 text-primary" style="font-size: 18px;"></i>
        기초자료 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        <span class="text-primary fw-bolder">부서별 기초재고 등록 (HPBA810U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-save" @click="save">저장</button>
        <button class="btn-erp btn-excel" @click="excel">엑셀</button>
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
                <th class="required">기준연월</th>
                <td>
                  <div class="d-flex align-items-center gap-1 px-1">
                    <select v-model="formData.yy" class="form-select" style="width: 100px;">
                      <option v-for="year in yearOptions" :key="year" :value="year">{{ year }}년</option>
                    </select>
                    <select v-model="monthStr" class="form-select" style="width: 80px;">
                      <option v-for="month in monthOptions" :key="month" :value="month">{{ month }}월</option>
                    </select>
                  </div>
                </td>
                <th class="required">재고부서</th>
                <td>
                  <div class="input-group px-1" style="max-width: 250px;">
                    <input v-model="formData.deptcd" type="text" class="form-control text-center bg-light fw-bold" style="max-width: 60px;" readonly />
                    <input v-model="formData.deptnm" type="text" class="form-control" placeholder="부서 선택" @keyup.enter="openHelp('DEPT')" />
                    <button class="btn" @click="openHelp('DEPT')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="required">품 목 명</th>
                <td>
                  <div class="input-group px-1" style="max-width: 450px;">
                    <input v-model="formData.itemcd" type="text" class="form-control text-center bg-light fw-bold" style="max-width: 80px;" readonly />
                    <input v-model="formData.itemnm" type="text" class="form-control" placeholder="품목 선택" @keyup.enter="openHelp('ITEM')" />
                    <input v-model="formData.itsize" type="text" class="form-control bg-light" style="max-width: 120px;" readonly placeholder="규격" />
                    <button class="btn" @click="openHelp('ITEM')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
              </tr>
              <tr>
                <th class="required">재고수량</th>
                <td>
                  <div class="px-1">
                    <input v-model="formData.qty" type="number" class="form-control text-end fw-bold" style="width: 150px;" />
                  </div>
                </td>
                <th class="required">재고금액</th>
                <td>
                  <div class="px-1">
                    <input v-model="formData.amt" type="number" class="form-control text-end fw-bold text-primary" style="width: 180px;" />
                  </div>
                </td>
                <td colspan="2" class="text-muted small px-3">
                   <i class="bi bi-info-circle me-1"></i> 각 부서별 창고에 보관 중인 기초 재고 정보를 등록합니다.
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 🅲 그리드 영역 -->
      <div class="card border-0 shadow-sm flex-grow-1 overflow-hidden d-flex flex-column">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex justify-content-between align-items-center">
          <span class="fw-bold small text-dark"><i class="bi bi-table me-1 text-primary"></i> 부서별 기초 재고 내역</span>
          <span class="text-muted small" style="font-size: 11px;">항목 클릭 시 수정 모드로 전환됩니다.</span>
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
import { reactive, ref, onMounted, computed, nextTick } from 'vue'
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

const now = new Date()

const formData = reactive({ actkind: 'A0', yy: String(now.getFullYear()), mm: now.getMonth() + 1, deptcd: authStore.deptcd, deptnm: authStore.deptnm, itemcd: '', itemnm: '', itsize: '', unit: '', qty: 0, amt: 0 })
const monthStr = computed({ get: () => String(formData.mm).padStart(2, '0'), set: (v) => { formData.mm = Number(v) } })
const yearOptions = ref<string[]>([]); const monthOptions = ['01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12']

const gridElement = ref<HTMLElement | null>(null); let grid: Tabulator | null = null; const itemCount = ref(0)

const generateYearOptions = () => { const cur = new Date().getFullYear(); for (let i = 0; i < 5; i++) yearOptions.value.push(String(cur - i)) }

const initGrid = () => {
  if (gridElement.value) {
    grid = new Tabulator(gridElement.value, {
      layout: "fitColumns", height: "100%", placeholder: "데이터가 없습니다.",
      columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
      columns: [
        { title: "연월", field: "ym", width: 90, hozAlign: "center", formatter: (c) => formatDateString(c.getValue(), '-') },
        { title: "부서명", field: "deptnm", width: 150 },
        { title: "코드", field: "itemcd", width: 100, hozAlign: "center", cssClass: "text-primary fw-bold" },
        { title: "품 목 명", field: "itemnm", minWidth: 200, widthGrow: 1, cssClass: "fw-bold" },
        { title: "규격", field: "itsize", width: 150 },
        { title: "수량", field: "qty", width: 100, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
        { title: "금액", field: "amt", width: 120, hozAlign: "right", formatter: "money", cssClass: "text-primary fw-bold" }
      ],
    })
    grid.on("rowClick", (e, row) => { const d = row.getData(); Object.assign(formData, { ...d, actkind: 'U0', yy: d.ym.substring(0, 4), mm: Number(d.ym.substring(4, 6)) }) })
  }
}

async function search() {
  try {
    const res = await api.post('/hpba/HPBA_810U_STR', { actkind: 'S0', cmpycd: authStore.cmpycd, yy: formData.yy, mm: monthStr.value, deptcd: formData.deptcd, itemcd: formData.itemcd })
    grid?.setData(res.data); itemCount.value = res.data.length; vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

async function save() {
  if (!formData.deptcd || !formData.itemcd) return vAlertError('부서와 품목을 모두 선택하세요.')
  if (formData.qty === 0 && formData.amt === 0) return vAlertError('수량 또는 금액을 입력하세요.')
  if (!confirm('기초 재고 정보를 저장하시겠습니까?')) return
  try {
    await api.post('/hpba/HPBA_810U_STR', { ...formData, mm: monthStr.value, cmpycd: authStore.cmpycd, userid: authStore.userid })
    vAlert('정상적으로 저장되었습니다.'); search(); initializeFormOnly()
  } catch (e) { vAlertError('저장 실패') }
}

function excel() { grid?.download("xlsx", `부서별기초재고_${formData.yy}${monthStr.value}.xlsx`) }

const initializeFormOnly = () => { Object.assign(formData, { actkind: 'A0', itemcd: '', itemnm: '', itsize: '', unit: '', qty: 0, amt: 0 }) }

function initialize() {
  resetForm(formData); Object.assign(formData, { actkind: 'A0', yy: String(now.getFullYear()), mm: now.getMonth() + 1, deptcd: authStore.deptcd, deptnm: authStore.deptnm, qty: 0, amt: 0 })
  grid?.clearData(); itemCount.value = 0; search()
}

const modalVisible = ref(false); const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })
function openHelp(type: string) {
  if (type === 'ITEM') {
    Object.assign(modalProps, { title: '품목 선택', path: '/ha00/HA00_00P_STR', data: { gubun: 'I0', cmpycd: authStore.cmpycd, codegbn: 'B' }, columns: [{ title: '코드', field: 'itemcd', width: 100 }, { title: '품목명', field: 'itemnm', width: 250 }],
      onConfirm: (d: any) => { Object.assign(formData, { itemcd: d.itemcd, itemnm: d.itemnm, itsize: d.itsize, unit: d.unit }) }
    })
  } else if (type === 'DEPT') {
    Object.assign(modalProps, { title: '부서 선택', path: '/ha00/HA00_00P_STR', data: { gubun: 'D0', cmpycd: authStore.cmpycd }, columns: [{ title: '코드', field: 'code', width: 80 }, { title: '부서명', field: 'cdnm', width: 180 }],
      onConfirm: (d: any) => { formData.deptcd = d.code; formData.deptnm = d.cdnm }
    })
  }
  modalVisible.value = true
}

const formatDateString = (v: any, sep: string) => v && String(v).length >= 6 ? `${v.substring(0, 4)}${sep}${v.substring(4, 6)}` : (v || '')

onMounted(() => { generateYearOptions(); nextTick(() => { initGrid(); search(); }) })
</script>
