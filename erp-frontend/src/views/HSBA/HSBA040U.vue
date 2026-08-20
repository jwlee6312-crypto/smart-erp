<!--기본정보/시스템관리/단위등록 [ERP 프리미엄 표준] -->
<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-1 px-3 sticky-top shadow-sm flex-shrink-0">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 13px;">
        <i class="bi bi-box-fill me-2 text-primary" style="font-size: 18px;"></i>
        기본정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        단위등록 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        <span class="text-primary fw-bolder">단위 관리 (HSBA040U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-save" @click="save">저장</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2">
      <!-- 🅰️ 조회 조건 -->
      <div class="card border shadow-sm flex-shrink-0">
        <div class="card-body p-1 bg-light-subtle">
          <div class="d-flex align-items-center gap-3">
            <div class="input-group input-group-sm flex-nowrap" style="width: 350px;">
              <span class="input-group-text fw-bold border-0 bg-transparent">단위명</span>
              <input v-model="searchForm.unitnm" type="text" class="form-control border-0 bg-white" placeholder="단위명 검색" @keyup.enter="search" />
              <button class="btn btn-dark btn-sm" @click="search"><i class="bi bi-search"></i></button>
            </div>
          </div>
        </div>
      </div>

      <!-- 🅱️ 상세 정보 입력 (고밀도 1줄 배치) -->
      <div class="card border shadow-sm overflow-hidden flex-shrink-0">
        <div class="card-header py-1 px-3 border-bottom d-flex align-items-center justify-content-between" style="background-color: #f8f9fa;">
          <span class="fw-bold small text-dark"><i class="bi bi-pencil-square me-1"></i> 단위 상세 정보</span>
          <span v-if="formData.actkind === 'U'" class="badge bg-warning text-dark" style="font-size: 10px;">수정 중</span>
          <span v-else class="badge bg-primary" style="font-size: 10px;">신규 등록</span>
        </div>
        <div class="card-body p-0">
          <table class="erp-table-full border-0">
            <colgroup>
              <col style="width: 80px;" /><col />
              <col style="width: 80px;" /><col />
              <col style="width: 80px;" /><col />
              <col style="width: 80px;" /><col />
              <col style="width: 80px;" /><col style="width: 60px;" />
            </colgroup>
            <tbody>
              <tr>
                <th class="required">단위코드</th>
                <td><input v-model="formData.unit" type="text" class="form-control form-control-sm text-center fw-bold text-primary" maxlength="4" :disabled="formData.actkind === 'U'" placeholder="code" /></td>
                <th class="required">단위명</th>
                <td><input v-model="formData.unitnm" type="text" class="form-control form-control-sm" maxlength="20" /></td>
                <th>비고</th>
                <td><input v-model="formData.remark" type="text" class="form-control form-control-sm" maxlength="50" /></td>
                <th>출현순서</th>
                <td><input v-model="formData.dspord" type="number" class="form-control form-control-sm text-end" maxlength="3" /></td>
                <th>사용</th>
                <td class="text-center">
                  <div class="form-check form-switch d-inline-block">
                    <input v-model="formData.useyn" class="form-check-input" type="checkbox" true-value="Y" false-value="N" id="useYn040">
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Ⓒ 데이터 그리드 (15행 페이징 처리) -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
          <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-1"></i> 단위 목록</span>
          <span class="text-muted" style="font-size: 11px;">※ 행 클릭 시 상단에 정보가 로드됩니다. (페이지당 15행)</span>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="gridElement" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, nextTick } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import AppAlert from '@/components/AppAlert.vue'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

// 1. 상태 관리
const searchForm = reactive({ unitnm: '' })
const formData = reactive({
  actkind: 'A', cmpycd: authStore.cmpycd, userid: authStore.user_id,
  unit: '', unitnm: '', remark: '', dspord: '1', useyn: 'Y'
})

const gridElement = ref<HTMLElement | null>(null); const grid = ref<Tabulator | null>(null); const activeItemCount = ref(0)

// 2. 그리드 초기화
const initGrid = () => {
  if (!gridElement.value) return
  grid.value = new Tabulator(gridElement.value, {
    layout: "fitColumns", height: "100%", pagination: "local", paginationSize: 15,
    paginationButtonCount: 5, paginationSizeSelector: [15, 30, 50],
    placeholder: "데이터가 없습니다.", columnDefaults: { headerSort: false, headerHozAlign: "center" },
    columns: [
      { title: "단위코드", field: "unit", width: 100, hozAlign: "center", cssClass: "fw-bold text-primary border-end" },
      { title: "단위명", field: "unitnm", width: 250 },
      { title: "비고", field: "remark", minWidth: 300 },
      { title: "순서", field: "dspord", width: 80, hozAlign: "center" },
      { title: "사용", field: "useyn", width: 80, hozAlign: "center",  editor: true,
        formatter: (cell) => {
          const val = String(cell.getValue() || '').trim().toUpperCase();
          return val === 'Y' ? '<b class="text-primary">사용</b>' : '';
        }
      }
    ]
  })
  grid.value.on("rowClick", (e, row) => {
    Object.assign(formData, row.getData())
    formData.actkind = 'U'
  })
}

// 3. 기능 구현
async function search() {
  try {
    const res = await api.post('/hsba/HSBA_040U_STR', {
      actkind: 'S', cmpycd: authStore.cmpycd, unitnm: searchForm.unitnm
    })
    if (grid.value) {
      grid.value.setData(res.data || [])
      activeItemCount.value = (res.data || []).length
    }
  } catch (e) { vAlertError('조회 실패') }
}

async function save() {
  if (!formData.unit || !formData.unitnm) return vAlertError('단위코드와 명칭은 필수입니다.')
  if (!confirm('저장하시겠습니까?')) return

  try {
    await api.post('/hsba/HSBA_040U_STR', { ...formData })
    vAlert('성공적으로 저장되었습니다.')
    search()
    initialize()
  } catch (e) { vAlertError('저장 중 오류 발생') }
}

function initialize() {
  resetForm(formData)
  Object.assign(formData, {
    actkind: 'A', cmpycd: authStore.cmpycd, userid: authStore.user_id,
    useyn: 'Y', dspord: '1'
  })
}

onMounted(() => {
  nextTick(() => {
    initGrid()
    search()
  })
})
</script>
