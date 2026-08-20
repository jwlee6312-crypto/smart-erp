<!--
	=============================================================
	프로그램명	: 수입비용코드관리 (HSBA750U)
	작성일자	: 2025.02.24
	설명        : 수입비용코드 관리 (조회 필터 패턴 적용 및 그리드 스크롤 최적화)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom bg-white py-2 px-3 sticky-top shadow-sm">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-gear-wide-connected me-2 text-primary" style="font-size: 18px;"></i>
        기본정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">수입비용코드관리 (HSBA750U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-2">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchList">조회</button>
        <button class="btn-erp btn-save" @click="save">저장</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- 🔍 [상단] 조회 필터 영역 (HSOD100U 디자인 패턴 적용) -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
                <col style="width: 10%" />
                <col style="width: 40%" />
                <col style="width: 10%" />
                <col style="width: 40%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light">비용코드/명</th>
                <td>
                  <input v-model="searchParam.sch_text" class="form-control form-control-sm w-75" placeholder="검색어 입력" @keyup.enter="fetchList" />
                </td>
                <th class="text-center bg-light">사용여부</th>
                <td>
                  <select v-model="searchParam.sch_useyn" class="form-select form-select-sm w-50" @change="fetchList">
                    <option value="">전체</option>
                    <option value="Y">사용</option>
                    <option value="N">미사용</option>
                  </select>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 🅰️ 마스터 상세 입력 폼 (카드 레이아웃) -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-header py-1 px-3 border-bottom d-flex align-items-center" style="background-color: #f0f7ff !important;">
          <span class="fw-bold small text-primary"><i class="bi bi-pencil-square me-1"></i> 코드 상세 정보</span>
        </div>
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense w-100">
            <colgroup>
              <col style="width: 120px;" /><col />
              <col style="width: 120px;" /><col />
              <col style="width: 120px;" /><col />
              <col style="width: 120px;" /><col />
            </colgroup>
            <tbody>
              <tr>
                <th class="required bg-light">비용코드</th>
                <td><input v-model="formData.costcd" class="form-control text-center fw-bold" maxlength="3" :readonly="formData.mode === 'U'" /></td>
                <th class="required bg-light">코 드 명</th>
                <td><input v-model="formData.costnm" class="form-control" maxlength="30" /></td>
                <th class="required bg-light text-center">비용구분</th>
                <td>
                  <select v-model="formData.costgbn" class="form-select fw-bold">
                    <option value="">-- 선택 --</option>
                    <option v-for="opt in costGbnOptions" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                  </select>
                </td>
                <th class="required bg-light text-center">배부기준</th>
                <td>
                  <select v-model="formData.divcd" class="form-select fw-bold">
                    <option value="">-- 선택 --</option>
                    <option v-for="opt in divideOptions" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                  </select>
                </td>
              </tr>
              <tr>
                <th class="bg-light">비&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;고</th>
                <td colspan="5"><input v-model="formData.bigo" class="form-control" maxlength="50" /></td>
                <th class="bg-light text-center">사용여부</th>
                <td>
                  <div class="form-check form-switch m-0 d-flex align-items-center justify-content-center">
                    <input v-model="formData.useyn" class="form-check-input" type="checkbox" true-value="Y" false-value="N" id="useynSwitch">
                    <label class="form-check-label ms-2 small" for="useynSwitch">{{ formData.useyn === 'Y' ? '사용' : '미사용' }}</label>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 🅱️ 데이터 그리드 영역 (스크롤 최적화) -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white grid-container">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark d-flex align-items-center">
            <i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i> 수입비용코드 목록
          </span>
        </div>

        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="gridRef" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>
    </div>
  </div>

  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { useCommonHelp } from '@/composables/useCommonHelp'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps, openHelp } = useCommonHelp()

// [1] 데이터 모델링
const searchParam = reactive({ sch_text: '', sch_useyn: '' })
const formData = reactive<any>({
	mode: 'N', costcd: '', costnm: '', costgbn: '', divcd: '', bigo: '', useyn: 'Y'
})

const costGbnOptions = ref<any[]>([])
const divideOptions = ref<any[]>([])
const gridRef = ref<HTMLDivElement | null>(null)
let grid: Tabulator | null = null

// 콤보박스 데이터 로드
async function fetchComboOptions() {
	try {
		const resGbn = await api.get('/hs00/HS00_000S_STR', {
			params: { gubun: 'E2', cmpycd: authStore.cmpycd, gbncd: '315', code: '', codenm: '', etcval: '' }
		})
		costGbnOptions.value = resGbn.data
		const resDiv = await api.get('/hs00/HS00_000S_STR', {
			params: { gubun: 'E2', cmpycd: authStore.cmpycd, gbncd: '316', code: '', codenm: '', etcval: '' }
		})
		divideOptions.value = resDiv.data
	} catch (e) { console.error('콤보박스 로드 실패', e) }
}

async function fetchList() {
	try {
		const res = await api.post('/hsba/HSBA_750U_STR', {
			actkind: 'S0', cmpycd: authStore.cmpycd, sch_text: searchParam.sch_text, useyn: searchParam.sch_useyn
		})
		grid?.setData(res.data)
	} catch (e) { vAlertError('조회 실패') }
}

async function save() {
	if (!formData.costcd || !formData.costnm || !formData.costgbn || !formData.divcd) {
		return vAlertError('필수 입력 항목을 확인하세요.')
	}
	if (!confirm('저장하시겠습니까?')) return
	try {
		const actkind = formData.mode === 'U' ? 'U0' : 'A0'
		await api.post('/hsba/HSBA_750U_STR', {
			...formData, actkind: actkind, cmpycd: authStore.cmpycd, userid: authStore.userid
		})
		vAlert('저장되었습니다.')
		initialize(); fetchList();
	} catch (e) { vAlertError('저장 실패') }
}

function initialize() {
	resetForm(formData)
	Object.assign(formData, { mode: 'N', useyn: 'Y', costgbn: '', divcd: '', costcd: '', costnm: '', bigo: '' })
	grid?.deselectRow()
}

onMounted(async () => {
	await fetchComboOptions()
	if (gridRef.value) {
		grid = new Tabulator(gridRef.value, {
			layout: 'fitColumns', height: '100%', selectable: 1,
			columnDefaults: { headerSort: false, headerHozAlign: "center", minWidth: 80 },
			columns: [
				{ title: '비용코드', field: 'costcd', width: 120, hozAlign: 'center', cssClass: 'fw-bold text-primary' },
				{ title: '코드명', field: 'costnm', minWidth: 200, widthGrow: 1.5 },
				{ title: '구분코드', field: 'costgbn', width: 100, hozAlign: 'center' },
				{ title: '비용구분명', field: 'costgbnm', width: 150 },
				{ title: '기준코드', field: 'divcd', width: 100, hozAlign: 'center' },
				{ title: '배부기준명', field: 'divnm', width: 150 },
				{ title: '사용', field: 'useyn', width: 100, hozAlign: 'center', formatter: (c) => c.getValue() === 'Y' ? '사용' : '미사용' }
			]
		})
		grid.on('rowClick', (e, row) => {
			const data = row.getData()
			Object.assign(formData, data)
			formData.mode = 'U'
		})
	}
	fetchList()
})
</script>

<style scoped>
/* 🎨 HSOD100U 디자인 표준 이식 */
.main-content-wrapper {
  padding-bottom: 0vh !important;
}

/* 폼 셀 높이 32px 고정 및 정렬 */
.erp-table-dense th, .erp-table-dense td {
  height: 32px !important;
  padding: 0 8px !important;
  font-size: 12px;
  vertical-align: middle;
  border: 1px solid #dee2e6;
}

.erp-table-dense .form-control,
.erp-table-dense .form-select,
.erp-table-dense .btn {
  height: 26px !important;
  font-size: 12px !important;
  border-radius: 2px;
}

.erp-table-dense th {
  font-weight: 600;
  color: #495057;
}

/* 그리드 카드 하단 강조선 */
.grid-container {
  border-bottom: 3px solid #005a9f !important;
}

.tabulator-instance {
  width: 100% !important;
  background-color: #fff;
}
</style>
