<!--
	=============================================================
	프로그램명	: 품목별 배부작업 (HAPL200U)
	작성일자	: 2025.02.24
	설명        : 품목별 제조원가 배부 실행 및 검증
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-calculator-fill me-2 text-primary" style="font-size: 18px;"></i>
        관리손익 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        원가결산 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">품목별 배부작업 (HAPL200U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-save" @click="handleExecute">배부실행</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 처리 대상 및 조건 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center">
          <span class="fw-bold small text-dark"><i class="bi bi-gear-fill me-2 text-primary"></i>배부 처리 조건</span>
        </div>
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
                <col style="width: 150px" /><col />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light required">작업 연월</th>
                <td>
                  <div class="d-flex align-items-center gap-2 px-2 py-1">
                    <select v-model="searchForm.yy" class="form-select form-select-sm" style="width: 100px;">
                      <option v-for="y in yearOptions" :key="y" :value="y">{{ y }}년</option>
                    </select>
                    <select v-model="searchForm.mm" class="form-select form-select-sm" style="width: 80px;">
                      <option v-for="m in monthOptions" :key="m" :value="m">{{ m }}월</option>
                    </select>
                    <span v-if="clsInfo.wclsym" class="badge bg-danger-subtle text-danger border border-danger-subtle ms-3">마감월: {{ clsInfo.wclsym }}</span>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [중단] 안내 사항 -->
      <div class="card border shadow-sm flex-shrink-0">
        <div class="card-body p-3 bg-white">
            <div class="d-flex flex-column gap-2 small">
                <div class="d-flex align-items-center">
                    <i class="bi bi-check2-circle me-2 text-success fw-bold"></i>
                    <span>1. 당월 마감작업이 완료되었으면 배부작업을 하시기 바랍니다.</span>
                </div>
                <div class="d-flex align-items-center">
                    <i class="bi bi-check2-circle me-2 text-success fw-bold"></i>
                    <span>2. 기본정보 > 마감관리의 마감정보와 작업월의 말일자가 일치해야 만 배부작업이 가능합니다.</span>
                </div>
                <div class="d-flex align-items-center">
                    <i class="bi bi-check2-circle me-2 text-success fw-bold"></i>
                    <span>3. 부문별 배부작업 후 품목별 배부작업을 하시기 바랍니다.</span>
                </div>
            </div>
        </div>
      </div>

      <!-- [하단] 결과 그리드 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column grid-container-right">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark">
            <i class="bi bi-exclamation-triangle-fill me-2 text-warning" v-if="hasError"></i>
            <i class="bi bi-grid-3x3-gap-fill me-2 text-primary" v-else></i>
            배부 검증 결과 <span v-if="hasError" class="text-danger ms-2">(배부 전/후 금액 차이 발생)</span>
          </span>
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
import AppAlert from '@/components/AppAlert.vue'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()

const currentYear = new Date().getFullYear()
const currentMonth = (new Date().getMonth() + 1).toString().padStart(2, '0')
const yearOptions = Array.from({ length: 6 }, (_, i) => String(currentYear - i))
const monthOptions = Array.from({ length: 12 }, (_, i) => String(i + 1).padStart(2, '0'))

const searchForm = reactive({
  yy: String(currentYear),
  mm: currentMonth
})

const clsInfo = reactive({ wclsym: '' })
const hasError = ref(false)

const mainGridRef = ref<HTMLElement | null>(null)
let mainGrid: Tabulator | null = null

const initGrids = () => {
  if (!mainGridRef.value) return

  mainGrid = new Tabulator(mainGridRef.value, {
    layout: "fitColumns",
    height: "100%",
    placeholder: "정상적으로 배부되었습니다.",
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "No", formatter: "rownum", width: 40, hozAlign: "center" },
      { title: "부서", field: "deptcd", width: 100, hozAlign: "center" },
      { title: "부서명", field: "deptnm", widthGrow: 1, hozAlign: "left" },
      { title: "계정", field: "acct", width: 100, hozAlign: "center" },
      { title: "계정명", field: "acctnm", widthGrow: 1, hozAlign: "left" },
      { title: "배부전금액", field: "bfamt", width: 130, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "배부후금액", field: "afamt", width: 130, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "차액", field: "diffamt", width: 120, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "text-danger fw-bold" }
    ],
  });
}

const loadClsInfo = async () => {
  try {
    const res = await api.post('/ha00/HA00_00P_STR', { gubun: 'CL', cmpycd: authStore.cmpycd })
    if (res.data?.length > 0) {
      clsInfo.wclsym = res.data[0].wclsym || ''
    }
  } catch (e) { console.error(e) }
}

const handleExecute = async () => {
    const ym = searchForm.yy + searchForm.mm
    if (!confirm(`${searchForm.yy}년 ${searchForm.mm}월의 배부작업을 하시겠습니까?`)) return

    try {
        hasError.value = false;
        mainGrid?.clearData();

        const res = await api.post('/hapl/HAPL_200U_STR', {
            actkind: 'A',
            cmpycd: authStore.cmpycd,
            stdym: ym,
            userid: authStore.userid
        });

        if (res.data && res.data.length > 0) {
            hasError.value = true;
            mainGrid?.setData(res.data);
            vAlertError('배부 전/후 금액이 다릅니다. 기준 데이터를 확인하세요.');
        } else {
            await api.post('/hapl/HAPL_200U_STR', {
                actkind: 'C',
                cmpycd: authStore.cmpycd,
                stdym: ym,
                userid: authStore.userid
            });
            vAlert('배부 및 집계 작업이 정상적으로 완료되었습니다.');
        }
    } catch (e: any) {
        vAlertError('작업 중 오류 발생');
    }
}

const initialize = () => {
    searchForm.yy = String(currentYear);
    searchForm.mm = currentMonth;
    hasError.value = false;
    mainGrid?.clearData();
}

onMounted(() => {
  nextTick(() => {
    initGrids();
    loadClsInfo();
  });
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
