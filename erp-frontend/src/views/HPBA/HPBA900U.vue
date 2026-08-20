<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-code-square me-2 text-primary" style="font-size: 18px;"></i>
        기본정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        <span class="text-primary fw-bolder">코드관리 (HPBA900U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-2">
        <button class="btn-erp btn-init" @click="initialize">
          <i class="bi bi-arrow-clockwise"></i> 초기화
        </button>
        <button class="btn-erp btn-search" @click="fetchGroups">
          <i class="bi bi-search"></i> 조회
        </button>
        <button class="btn-erp btn-save" @click="saveData">
          <i class="bi bi-save"></i> 저장
        </button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2">
      <!-- 🅰️ 입력 폼 영역 -->
      <div class="card border-0 shadow-sm overflow-hidden" style="border-radius: 8px;">
        <div class="card-body p-0">
          <table class="erp-table-full">
            <colgroup>
              <col style="width: 100px;"><col>
              <col style="width: 100px;"><col>
              <col style="width: 100px;"><col>
            </colgroup>
            <tbody>
              <tr>
                <th class="required">코드구분</th>
                <td>
                  <select v-model="formData.cdgbn" class="form-select form-select-sm" style="width: 180px;">
                    <option v-for="opt in groupOptions" :key="opt.code" :value="opt.code">
                      [{{ opt.code }}] {{ opt.cdnm }}
                    </option>
                  </select>
                </td>
                <th class="required">코&nbsp;&nbsp;&nbsp;&nbsp;드</th>
                <td>
                  <input v-model="formData.code" type="text" class="form-control form-control-sm" style="width: 100px;" maxlength="10" :readonly="formData.actkind === 'U0'" />
                </td>
                <th class="required">코&nbsp;드&nbsp;명</th>
                <td>
                  <input v-model="formData.cdnm" type="text" class="form-control form-control-sm" placeholder="코드명 입력" />
                </td>
              </tr>
              <tr>
                <th>비&nbsp;&nbsp;&nbsp;&nbsp;고</th>
                <td>
                  <input v-model="formData.remark" type="text" class="form-control form-control-sm" placeholder="설명 입력" />
                </td>
                <th>출현순서</th>
                <td>
                  <input v-model="formData.dspord" type="number" class="form-control form-control-sm text-end" style="width: 80px;" />
                </td>
                <th>사용여부</th>
                <td>
                  <div class="form-check form-switch pt-1">
                    <input v-model="formData.useyn" class="form-check-input" type="checkbox" true-value="Y" false-value="N">
                    <label class="form-check-label small ms-1">{{ formData.useyn === 'Y' ? '사용함' : '미사용' }}</label>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 🅱️ 메인 레이아웃 (좌: 그룹목록, 우: 코드목록) -->
      <div class="d-flex flex-grow-1 gap-2 overflow-hidden">
        <!-- 좌측: 코드 그룹 목록 -->
        <div class="card border-0 shadow-sm d-flex flex-column" style="width: 300px; border-radius: 8px;">
          <div class="card-header bg-light py-2 px-3 border-bottom fw-bold small text-dark">
            <i class="bi bi-folder2-open me-1 text-primary"></i> 코드 그룹
          </div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="groupGridElement" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>

        <!-- 우측: 상세 코드 목록 -->
        <div class="flex-grow-1 d-flex flex-column gap-2 overflow-hidden">
          <div class="card border-0 shadow-sm flex-grow-1 overflow-hidden d-flex flex-column" style="border-radius: 8px;">
            <div class="card-header bg-white py-2 px-3 border-bottom d-flex justify-content-between align-items-center">
              <span class="fw-bold small text-dark">
                <i class="bi bi-list-ul me-1 text-primary"></i> 상세 코드 리스트
                <span v-if="selectedGroup.cdnm" class="badge bg-info ms-2 px-3">{{ selectedGroup.cdnm }}</span>
              </span>
              <span class="text-muted small">목록을 클릭하면 수정 모드로 전환됩니다.</span>
            </div>
              <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
                <div ref="codeGridElement" class="tabulator-instance flex-grow-1"></div>
              </div>
          </div>
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
  actkind: 'A0',
  cdgbn: '010',
  code: '',
  cdnm: '',
  remark: '',
  dspord: 0,
  useyn: 'Y'
})

const selectedGroup = reactive({ code: '010', cdnm: '' })
const groupOptions = ref<any[]>([])

const groupGridElement = ref<HTMLElement | null>(null)
const codeGridElement = ref<HTMLElement | null>(null)
let groupGrid: Tabulator | null = null
let codeGrid: Tabulator | null = null
const itemCount = ref(0)

// 2. 그리드 초기화
const initGrids = () => {
  if (groupGridElement.value) {
    groupGrid = new Tabulator(groupGridElement.value, {
      layout: "fitColumns",
      height: "100%",
      placeholder: "데이터가 없습니다.",
      selectable: 1,
      columns: [
        { title: "코드 그룹명", field: "cdnm", minWidth: 200, cssClass: "fw-bold" }
      ],
    })

    groupGrid.on("rowClick", (e, row) => {
        const data = row.getData()
        Object.assign(selectedGroup, { code: data.code, cdnm: data.cdnm })
        formData.cdgbn = data.code
        fetchCodes(data.code)
        resetInputForm(data.code)
    })
  }

  if (codeGridElement.value) {
    codeGrid = new Tabulator(codeGridElement.value, {
      layout: "fitColumns",
      height: "100%",
      placeholder: "그룹을 선택하세요.",
      selectable: 1,
      columns: [
        { title: "코드", field: "code", width: 100, hozAlign: "center" },
        { title: "코 드 명", field: "cdnm", minWidth: 200, cssClass: "fw-bold" },
        { title: "비 고", field: "remark", minWidth: 200 },
        { title: "순서", field: "dspord", width: 80, hozAlign: "center" },
        { title: "사용", field: "useyn", width: 80, hozAlign: "center",  editor: true,
          formatter: (cell) => {
            const val = String(cell.getValue() || '').trim().toUpperCase();
            return val === 'Y' ? '<b class="text-primary">사용</b>' : '';
          }
        }
      ],
    })

    codeGrid.on("rowClick", (e, row) => {
        const data = row.getData()
        Object.assign(formData, data)
        formData.actkind = 'U0'
    })
  }
}

// 3. 비즈니스 로직
const fetchGroups = async () => {
  try {
    const res = await api.get('/hp00/HP00_000S_STR', { params: { gubun: 'E0', cmpycd: authStore.cmpycd, gbncd: '010' } })
    groupOptions.value = res.data
    groupGrid?.setData(res.data)
  } catch (e) { vAlertError('그룹 조회 실패') }
}

const fetchCodes = async (groupCd: string) => {
  try {
    const res = await api.post('/hpba/HPBA_900U_STR', {
      actkind: 'S0',
      cmpycd: authStore.cmpycd,
      cdgbn: groupCd
    })
    codeGrid?.setData(res.data)
    itemCount.value = res.data.length
  } catch (e) { vAlertError('코드 조회 실패') }
}

const saveData = async () => {
  if (!formData.code || !formData.cdnm) {
    return vAlertError('코드와 코드명은 필수 입력 사항입니다.')
  }

  const msg = formData.actkind === 'A0' ? '새 코드를 등록하시겠습니까?' : '코드 정보를 수정하시겠습니까?'
  if (!confirm(msg)) return

  try {
    await api.post('/hpba/HPBA_900U_STR', {
      ...formData,
      cmpycd: authStore.cmpycd,
      userid: authStore.userid
    })
    vAlert('정상적으로 처리되었습니다.')
    fetchCodes(formData.cdgbn)
    resetInputForm(formData.cdgbn)
  } catch (e) { vAlertError('저장 중 오류 발생') }
}

const resetInputForm = (groupCd: string) => {
    formData.actkind = 'A0'
    formData.cdgbn = groupCd
    formData.code = ''
    formData.cdnm = ''
    formData.remark = ''
    formData.dspord = 0
    formData.useyn = 'Y'
}

const initialize = () => {
  resetForm(formData)
  Object.assign(formData, { actkind: 'A0', cdgbn: '010', useyn: 'Y', dspord: 0 })
  Object.assign(selectedGroup, { code: '010', cdnm: '' })
  groupGrid?.clearData()
  codeGrid?.clearData()
  itemCount.value = 0
  fetchGroups()
}

onMounted(() => {
  nextTick(() => { initGrids(); fetchGroups(); })
})
</script>

