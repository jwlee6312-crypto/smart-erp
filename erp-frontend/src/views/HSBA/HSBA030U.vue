<!--기본정보/시스템관리/중분류등록 [ERP 프리미엄 표준 - 계층형 조회 및 등록] -->
<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-diagram-3-fill me-2 text-primary" style="font-size: 18px;"></i>
        기본정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        중분류등록 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        <span class="text-primary fw-bolder">중분류 관리 (HSBA030U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchLargeList">조회</button>
        <button class="btn-erp btn-save" @click="save">저장</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2">

      <!-- 🅰️ 조회 및 입력 영역 (압축 배치) -->
      <div class="card border shadow-sm flex-shrink-0">
        <div class="card-body p-0">
          <table class="erp-table-full border-0">
            <colgroup>
              <col style="width: 100px;" /><col />
              <col style="width: 100px;" /><col />
              <col style="width: 100px;" /><col />
              <col style="width: 100px;" /><col />
              <col style="width: 80px;" /><col style="width: 100px;" />
            </colgroup>
            <tbody>
              <tr>
                <th class="bg-light-subtle">재고자산</th>
                <td>
                  <select v-model="searchForm.astkind" class="form-select form-select-sm" @change="fetchLargeList">
                    <option v-for="opt in assetOptions" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                  </select>
                </td>
                <th class="required">대분류</th>
                <td>
                  <select v-model="formData.agrpcd" class="form-select form-select-sm" @change="fetchMiddleList">
                    <option value="">-- 대분류 선택 --</option>
                    <option v-for="opt in largeOptions" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                  </select>
                </td>
                <th class="required">중분류코드</th>
                <td><input v-model="formData.bgrpcd" type="text" class="form-control form-control-sm text-center fw-bold" maxlength="3" :disabled="formData.actkind === 'U0'" placeholder="3자리" /></td>
                <th class="required">중분류명</th>
                <td><input v-model="formData.bgrpnm" type="text" class="form-control form-control-sm" maxlength="50" placeholder="분류 명칭" /></td>
                <th>사용</th>
                <td>
                  <div class="form-check form-switch m-0 d-flex justify-content-center">
                    <input v-model="formData.useyn" class="form-check-input" type="checkbox" true-value="Y" false-value="N" id="useYn030">
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 🅱️ 데이터 그리드 영역 (2단 분할) -->
      <div class="flex-grow-1 overflow-hidden d-flex gap-2">
        <!-- 3. 좌측: 대분류 리스트 -->
        <div class="card border shadow-sm flex-shrink-0 d-flex flex-column" style="width: 320px;">
          <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center">
            <span class="fw-bold small text-dark"><i class="bi bi-list-ul me-1"></i> 대분류 리스트</span>
          </div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden">
            <div ref="largeGridRef"></div>
          </div>
        </div>

        <!-- 4. 우측: 중분류 리스트 -->
        <div class="card border shadow-sm flex-grow-1 d-flex flex-column">
          <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
            <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-1"></i> 중분류 리스트</span>
            <span v-if="formData.agrpcd" class="text-primary small fw-bold">선택된 대분류: {{ selectedLargeNm }}</span>
          </div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden">
            <div ref="middleGridRef"></div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, nextTick, computed } from 'vue'
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
const searchForm = reactive({ astkind: '120' })
const formData = reactive({
  actkind: 'A0', cmpycd: authStore.cmpycd, userid: authStore.user_id,
  astkind: '120', agrpcd: '', bgrpcd: '', agrpnm: '', bgrpnm: '', useyn: 'Y'
})

const assetOptions = ref<any[]>([])
const largeOptions = ref<any[]>([])
const middleItemCount = ref(0)
const selectedLargeNm = ref('')

const largeGridRef = ref<HTMLElement | null>(null); let largeGrid: Tabulator | null = null
const middleGridRef = ref<HTMLElement | null>(null); let middleGrid: Tabulator | null = null

// 2. 그리드 초기화
const initGrids = () => {
  // 대분류 그리드
  if (largeGridRef.value) {
    largeGrid = new Tabulator(largeGridRef.value, {
      layout: "fitColumns", height: "100%", placeholder: "데이터 없음",
      columnDefaults: { headerSort: false, headerHozAlign: "center" },
      columns: [
        { title: "코드", field: "agrpcd", width: 70, hozAlign: "center", cssClass: "fw-bold text-secondary" },
        { title: "대분류 명칭", field: "agrpnm", minWidth: 150 }
      ]
    })
    largeGrid.on("rowClick", (e, row) => {
      const data = row.getData()
      formData.agrpcd = data.agrpcd
      selectedLargeNm.value = data.agrpnm
      formData.actkind = 'A0' // 대분류 선택 시 중분류는 신규등록 모드로
      fetchMiddleList()
    })
  }

  // 중분류 그리드
  if (middleGridRef.value) {
    middleGrid = new Tabulator(middleGridRef.value, {
      layout: "fitColumns", height: "100%", placeholder: "대분류를 선택하세요.",
      columnDefaults: { headerSort: false, headerHozAlign: "center" },
      columns: [
        { title: "중분류코드", field: "bgrpcd", width: 100, hozAlign: "center", cssClass: "fw-bold text-primary border-end" },
        { title: "중분류 명칭", field: "bgrpnm", minWidth: 250 },
          { title: "사용", field: "useyn", width: 80, hozAlign: "center",  editor: true,
            formatter: (cell) => {
              const val = String(cell.getValue() || '').trim().toUpperCase();
              return val === 'Y' ? '<b class="text-primary">사용</b>' : '';
            }
          }
       ]
    })
    middleGrid.on("rowClick", (e, row) => {
      const data = row.getData()
      Object.assign(formData, data)
      formData.actkind = 'U0'
    })
  }
}

// 3. 데이터 로드
async function fetchAssetCodes() {
  try {
    const res = await api.get('/hs00/HS00_000S_STR', { params: { gubun: 'E0', cmpycd: authStore.cmpycd, gbncd: '100' } })
    assetOptions.value = res.data.map((i: any) => ({ codecd: String(i.code || '').trim(), codenm: String(i.cdnm || '').trim() }))
    if (assetOptions.value.length > 0) {
      searchForm.astkind = assetOptions.value[0].codecd
      fetchLargeList()
    }
  } catch (e) { console.error('자산코드 로드 실패') }
}

async function fetchLargeList() {
  try {
    formData.astkind = searchForm.astkind
    const res = await api.post('/hsba/HSBA_030U_STR', { actkind: 'S1', cmpycd: authStore.cmpycd, astkind: searchForm.astkind })
    largeGrid?.setData(res.data || [])
    largeOptions.value = res.data.map((i: any) => ({ codecd: i.agrpcd, codenm: i.agrpnm }))

    // 초기화
    middleGrid?.setData([])
    formData.agrpcd = ''; formData.bgrpcd = ''; formData.bgrpnm = ''; selectedLargeNm.value = ''
  } catch (e) { vAlertError('조회 실패') }
}

async function fetchMiddleList() {
  if (!formData.agrpcd) return
  try {
    const res = await api.post('/hsba/HSBA_030U_STR', {
      actkind: 'S0', cmpycd: authStore.cmpycd, astkind: searchForm.astkind, agrpcd: formData.agrpcd
    })
    middleGrid?.setData(res.data || [])
    middleItemCount.value = res.data.length
  } catch (e) { vAlertError('중분류 조회 실패') }
}

// 4. 저장
async function save() {
  if (!formData.agrpcd || !formData.bgrpcd || !formData.bgrpnm) return vAlertError('대분류, 중분류 코드 및 명칭은 필수입니다.')
  if (!confirm('설정된 정보를 저장하시겠습니까?')) return

  try {
    const res = await api.post('/hsba/HSBA_030U_STR', { ...formData, userid: authStore.user_id })
    vAlert('저장되었습니다.')
    fetchMiddleList()
    initialize()
  } catch (e) { vAlertError('저장 오류 발생') }
}

function initialize() {
  const currentLarge = formData.agrpcd
  const currentAsset = searchForm.astkind
  resetForm(formData)
  Object.assign(formData, {
    actkind: 'A0', cmpycd: authStore.cmpycd, userid: authStore.user_id,
    astkind: currentAsset, agrpcd: currentLarge, useyn: 'Y'
  })
}

onMounted(async () => {
  await fetchAssetCodes()
  nextTick(() => { initGrids() })
})
</script>
