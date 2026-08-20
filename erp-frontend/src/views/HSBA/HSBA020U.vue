<!--기본정보/시스템관리/대분류등록 [ERP 프리미엄 표준] -->
<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-1 px-3 sticky-top shadow-sm flex-shrink-0">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 13px;">
        <i class="bi bi-diagram-2-fill me-2 text-primary" style="font-size: 18px;"></i>
        기본정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        대분류등록 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        <span class="text-primary fw-bolder">대분류 관리 (HSBA020U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-save" @click="save">저장</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2">
      <!-- 🅰️ 조회 및 입력 영역 -->
      <div class="card border shadow-sm flex-shrink-0">
        <div class="card-body p-0">
          <table class="erp-table-full border-0">
            <colgroup>
              <col style="width: 100px;" /><col />
              <col style="width: 100px;" /><col />
              <col style="width: 100px;" /><col />
              <col style="width: 80px;" /><col style="width: 100px;" />
            </colgroup>
            <tbody>
              <tr>
                <th class="bg-light-subtle">재고자산</th>
                <td>
                  <select v-model="formData.astkind" class="form-select form-select-sm" @change="search">
                    <option v-for="opt in assetOptions" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                  </select>
                </td>
                <th class="required">대분류코드</th>
                <td><input v-model="formData.agrpcd" type="text" class="form-control form-control-sm text-center fw-bold" maxlength="3" :disabled="formData.actkind === 'U0'" placeholder="3자리" /></td>
                <th class="required">대분류명</th>
                <td><input v-model="formData.agrpnm" type="text" class="form-control form-control-sm" maxlength="50" @keyup.enter="search" /></td>
                <th>사용여부</th>
                <td>
                  <div class="form-check form-switch m-0 d-flex justify-content-center">
                    <input v-model="formData.useyn" class="form-check-input" type="checkbox" true-value="Y" false-value="N" id="useyn020">
                    <label class="ms-2 small fw-bold" for="useyn020">{{ formData.useyn === 'Y' ? '사용' : '중지' }}</label>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Ⓒ 데이터 그리드 영역 (15행 페이징 처리) -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
          <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-1"></i> 대분류 리스트</span>
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
const formData = reactive({
  actkind: 'A0', cmpycd: authStore.cmpycd, userid: authStore.user_id,
  astkind: '120', agrpcd: '', agrpnm: '', useyn: 'Y'
})

const assetOptions = ref<any[]>([])
const gridElement = ref<HTMLElement | null>(null); const grid = ref<Tabulator | null>(null); const activeItemCount = ref(0)

// 2. 그리드 초기화
const initGrid = () => {
  if (!gridElement.value) return
  grid.value = new Tabulator(gridElement.value, {
    layout: "fitColumns", height: "100%", pagination: "local", paginationSize: 15,
    paginationButtonCount: 5, paginationSizeSelector: [15, 30, 50],
    placeholder: "데이터가 없습니다.", columnDefaults: { headerSort: false, headerHozAlign: "center" },
    columns: [
      { title: "대분류코드", field: "agrpcd", width: 120, hozAlign: "center", cssClass: "fw-bold text-primary border-end" },
      { title: "대분류 명칭", field: "agrpnm", minWidth: 300 },
        { title: "사용", field: "useyn", width: 80, hozAlign: "center",  editor: true,
          formatter: (cell) => {
            const val = String(cell.getValue() || '').trim().toUpperCase();
            return val === 'Y' ? '<b class="text-primary">사용</b>' : '';
          }
        }
    ]
  })
  grid.value.on("rowClick", (e, row) => { Object.assign(formData, row.getData()); formData.actkind = 'U0' })
}

// 3. 기능 구현
async function fetchAssetCodes() {
  try {
    const res = await api.get('/hs00/HS00_000S_STR', { params: { gubun: 'E0', cmpycd: authStore.cmpycd, gbncd: '100' } })
    assetOptions.value = res.data.map((i: any) => ({ codecd: String(i.code || i.codecd || '').trim(), codenm: String(i.cdnm || i.codenm || '').trim() }))
    if (assetOptions.value.length > 0) {
      formData.astkind = assetOptions.value[0].codecd
      search()
    }
  } catch (e) { console.error('코드 로드 실패', e) }
}

async function search() {
  try {
    const res = await api.post('/hsba/HSBA_020U_STR', {
      actkind: 'S0', cmpycd: authStore.cmpycd, astkind: formData.astkind, agrpnm: formData.agrpnm
    })
    if (grid.value) {
      grid.value.setData(res.data || []);
      activeItemCount.value = (res.data || []).length
    }
  } catch (e) { vAlertError('조회 실패') }
}

async function save() {
  if (!formData.agrpcd || !formData.agrpnm) return vAlertError('대분류 코드와 명칭은 필수입니다.')
  if (!confirm('저장하시겠습니까?')) return

  try {
    const res = await api.post('/hsba/HSBA_020U_STR', { ...formData })
    vAlert('성공적으로 저장되었습니다.')
    search()
    initialize()
  } catch (e) { vAlertError('저장 중 오류 발생') }
}

function initialize() {
  const currentAsset = formData.astkind
  resetForm(formData)
  Object.assign(formData, {
    actkind: 'A0', cmpycd: authStore.cmpycd, userid: authStore.user_id,
    astkind: currentAsset, useyn: 'Y'
  })
}

onMounted(async () => {
  await fetchAssetCodes()
  nextTick(() => { initGrid() })
})
</script>
