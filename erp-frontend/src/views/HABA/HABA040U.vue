<!--
	=============================================================
	프로그램명	: 보조과목관리 (HABA040U)
	작성일자	: 2025.03.14
	설명        : 계정과목의 보조부유형이 기타인 코드를 관리 (HSOD100U 디자인 표준 준수)
	=============================================================
-->

<template>
  <AppAlert :show="showalert" :error="showerror" :message="alertmessage" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-3 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-collection-fill me-2 text-primary" style="font-size: 18px;"></i>
        기본정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        기초관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">보조과목관리 (HABA040U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-save" @click="save">저장</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 조회 필터 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-2 bg-white">
          <div class="d-flex align-items-center gap-3">
            <div class="input-group input-group-sm flex-nowrap" style="width: 350px;">
              <span class="input-group-text fw-bold border-0 bg-transparent">보조과목 명</span>
              <input v-model="searchform.subnm_h" type="text" class="form-control" placeholder="검색할 보조과목명을 입력하세요" @keydown.enter="search" />
              <button class="btn btn-dark" @click="search"><i class="bi bi-search"></i></button>
            </div>
            <div class="small text-muted"><i class="bi bi-info-circle me-1"></i>계정과목의 보조부유형이 기타인 코드를 등록하여 관리합니다.</div>
          </div>
        </div>
      </div>

      <!-- [중간] 상세 정보 입력 영역 -->
      <div class="card border shadow-sm overflow-hidden flex-shrink-0">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
          <div class="fw-bold small text-dark"><i class="bi bi-pencil-square me-2 text-secondary"></i>보조과목 상세 정보</div>
          <div v-if="masterform.actkind === 'U0'" class="badge bg-primary-subtle text-primary border border-primary-subtle px-2">수정 중</div>
          <div v-else class="badge bg-success-subtle text-success border border-success-subtle px-2">신규 등록</div>
        </div>
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense w-100">
            <colgroup>
              <col style="width: 100px;" /><col style="width: 200px;" />
              <col style="width: 100px;" /><col style="width: 300px;" />
              <col style="width: 100px;" /><col />
            </colgroup>
            <tbody>
              <tr>
                <th class="required bg-light text-center">보조과목</th>
                <td><input v-model="masterform.subcd" type="text" class="form-control form-control-sm text-center fw-bold" maxlength="5" :readonly="masterform.actkind === 'U0'" placeholder="코드" /></td>
                <th class="required bg-light text-center border-start">보조과목명</th>
                <td colspan="3"><input v-model="masterform.subnm" type="text" class="form-control form-control-sm" maxlength="30" placeholder="보조과목 명칭 입력" /></td>
              </tr>
              <tr>
                <th class="bg-light text-center border-top">비    고</th>
                <td class="border-top"><input v-model="masterform.bigo" type="text" class="form-control form-control-sm" maxlength="50" /></td>
                <th class="required bg-light text-center border-start border-top">출현순서</th>
                <td class="border-top"><input v-model="masterform.dspord" type="number" class="form-control form-control-sm text-center" style="width: 80px;" /></td>
                <th class="bg-light text-center border-start border-top">사용여부</th>
                <td class="border-top">
                  <div class="form-check form-switch m-0 d-flex align-items-center h-100">
                    <input v-model="masterform.useyn" class="form-check-input mt-0" type="checkbox" true-value="Y" false-value="N" id="useYnSwitch">
                    <label class="form-check-label ms-2 small fw-bold" for="useYnSwitch">{{ masterform.useyn === 'Y' ? '사용' : '미사용' }}</label>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center">
          <i class="bi bi-table me-2 text-primary"></i>
          <span class="fw-bold small text-dark">보조과목 목록</span>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="maingridelement" class="tabulator-instance flex-grow-1"></div>
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
import { useFormReset } from '@/composables/useFormReset'
import AppAlert from '@/components/AppAlert.vue'

const authstore = useAuthStore()
const { showAlert: showalert, showError: showerror, alertMessage: alertmessage, vAlert: valert, vAlertError: valerterror } = useAlerts()
const { resetForm: resetform } = useFormReset()

const searchform = reactive({ subnm_h: '' })
const masterform = reactive<any>({
  actkind: 'A0',
  cmpycd: authstore.cmpycd,
  subcd: '',
  subnm: '',
  bigo: '',
  dspord: '',
  useyn: 'Y',
  userid: authstore.userid
})

const maingridelement = ref<HTMLDivElement | null>(null)
let maingrid: Tabulator | null = null

const search = async () => {
  try {
    const res = await api.post('/haba/HABA_040U_STR', {
      actkind: 'S0',
      cmpycd: authstore.cmpycd,
      subcd: '',
      subnm: searchform.subnm_h,
      bigo: '',
      dspord: '',
      useyn: '',
      userid: authstore.userid
    })
    maingrid?.setData(res.data || [])
    valert('조회되었습니다.')
  } catch (e) {
    valerterror('조회 실패')
  }
}

const save = async () => {
  if (!masterform.subcd) return valerterror('보조과목 코드를 입력하세요.')
  if (!masterform.subnm) return valerterror('보조과목 명을 입력하세요.')
  if (!masterform.dspord) return valerterror('출현순서를 입력하세요.')

  if (!confirm('저장하시겠습니까?')) return

  try {
    const res = await api.post('/haba/HABA_040U_STR', {
      ...masterform,
      actkind: masterform.actkind === 'A0' ? 'A0' : 'U0'
    })

    valert('저장되었습니다.')
    search()
    initialize()
  } catch (e) {
    valerterror('저장 실패')
  }
}

const initialize = () => {
  resetform(masterform)
  Object.assign(masterform, {
    actkind: 'A0',
    cmpycd: authstore.cmpycd,
    useyn: 'Y',
    userid: authstore.userid
  })
}

onMounted(() => {
  if (maingridelement.value) {
    maingrid = new Tabulator(maingridelement.value, {
      layout: "fitColumns",
      height: "100%",
      selectable: 1,
      columnDefaults: { headerSort: false, headerHozAlign: 'center', hozAlign: 'center', vertAlign: "middle" },
      columns: [
        { title: "No", formatter: "rownum", width: 40 },
        { title: "보조과목", field: "subcd", width: 100, cssClass: "fw-bold text-primary border-end" },
        { title: "보조과목 명", field: "subnm", minWidth: 200, hozAlign: "left" },
        { title: "비    고", field: "bigo", minWidth: 250, hozAlign: "left" },
        { title: "순서", field: "dspord", width: 70 },
        { title: "사용", field: "useyn", width: 80, hozAlign: "center",
          formatter: (cell) => {
            const val = String(cell.getValue() || '').trim().toUpperCase();
            return val === 'Y' ? '<b class="text-primary">사용</b>' : '';
          }
        }
      ]
    })

    maingrid.on("rowClick", (e, row) => {
      const data = row.getData()
      Object.assign(masterform, data)
      masterform.actkind = 'U0'
    })
  }

  nextTick(() => search())
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
:deep(.tabulator-row-selected) { background-color: #e7f1ff !important; }
.bg-light-yellow { background-color: #f9f6e7 !important; }
</style>
