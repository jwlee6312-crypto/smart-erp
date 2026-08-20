<!--
	=============================================================
	프로그램명	: 월 마감작업 (HACL800U)
	작성일자	: 2025.02.24
	설명        : 회계 월 마감 처리 및 오류 전표 검증
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-calendar-check-fill me-2 text-primary" style="font-size: 18px;"></i>
        회계관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        마감관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">월 마감작업 (HACL800U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-2">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-save" @click="executeClosing">마감실행</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-auto p-2 d-flex flex-column gap-3 bg-light main-content-wrapper">

      <!-- [상단] 마감 조건 설정 -->
      <div class="card border shadow-sm overflow-hidden flex-shrink-0">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center">
          <span class="fw-bold small text-dark"><i class="bi bi-gear-fill me-2 text-primary"></i>마감 처리 조건</span>
        </div>
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
              <col style="width: 150px" /><col />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light required">마감 연월</th>
                <td class="px-3 py-2">
                  <div class="d-flex align-items-center gap-2">
                    <select v-model="formData.yy" class="form-select form-select-sm" style="width: 120px;">
                      <option v-for="y in yearOptions" :key="y" :value="y">{{ y }}년</option>
                    </select>
                    <select v-model="formData.mm" class="form-select form-select-sm" style="width: 100px;">
                      <option v-for="m in monthOptions" :key="m" :value="m">{{ m }}월</option>
                    </select>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [중단] 안내 메시지 -->
      <div class="alert alert-info border-0 shadow-sm mb-0 py-3 px-4 flex-shrink-0 bg-white">
        <div class="d-flex align-items-start gap-3 small">
          <i class="bi bi-info-circle-fill fs-4 text-info"></i>
          <div>
            <p class="mb-2 fw-bold text-dark" style="font-size: 13px;">작업 시 주의사항</p>
            <ul class="mb-0 ps-3 text-muted d-flex flex-column gap-1">
              <li>당월 전표입력이 완료되었으면 마감작업을 하시기 바랍니다.</li>
              <li>마감작업 이후에는 해당월의 전표를 입력하거나 수정할 수 없습니다.</li>
              <li>전표입력이 필요한 경우 <span class="text-primary fw-bold">기본정보 > 마감관리</span>에서 마감정보를 조정하십시오.</li>
            </ul>
          </div>
        </div>
      </div>

      <!-- [하단] 오류 전표 그리드 (검증 실패 시 표시) -->
      <div v-if="errorList.length > 0" class="card border-danger shadow-sm flex-grow-1 overflow-hidden d-flex flex-column">
        <div class="card-header bg-danger text-white py-1 px-3 d-flex align-items-center justify-content-between">
          <span class="fw-bold small"><i class="bi bi-exclamation-triangle-fill me-2"></i>이상이 있는 전표 내역</span>
          <span class="small opacity-75">* 수정 후 재작업 하시기 바랍니다.</span>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="gridRef" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>

      <!-- [하단] 회계 원가대체 규칙 안내 -->
      <div v-else class="card border shadow-sm flex-shrink-0">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center">
            <span class="fw-bold small text-dark"><i class="bi bi-journal-text me-2 text-primary"></i>결산 시 원가대체 분개 규칙</span>
        </div>
        <div class="card-body bg-white p-3">
            <div class="row g-4">
                <div class="col-md-6">
                    <div class="p-2 border-start border-3 border-primary bg-light mb-3">
                        <div class="fw-bold small text-dark mb-1">1. 재료비 대체분개</div>
                        <div class="ps-2 small text-muted">재료비 / 원재료, 부재료</div>
                    </div>
                    <div class="p-2 border-start border-3 border-primary bg-light mb-3">
                        <div class="fw-bold small text-dark mb-1">2. 재공품 대체분개</div>
                        <div class="ps-2 small text-muted">재공품 / 재료비, 노무비, 제조경비</div>
                    </div>
                    <div class="p-2 border-start border-3 border-primary bg-light">
                        <div class="fw-bold small text-dark mb-1">3. 제조계정 대체분개</div>
                        <div class="ps-2 small text-muted">당기제품제조원가 / 제공품</div>
                    </div>
                </div>
                <div class="col-md-6">
                    <div class="p-2 border-start border-3 border-success bg-light mb-3">
                        <div class="fw-bold small text-dark mb-1">4. 제품 대체분개</div>
                        <div class="ps-2 small text-muted">제품 / 당기제품제조원가</div>
                    </div>
                    <div class="p-2 border-start border-3 border-success bg-light mb-3">
                        <div class="fw-bold small text-dark mb-1">5. 제품매출원가 대체분개</div>
                        <div class="ps-2 small text-muted">제품매출원가 / 제품</div>
                    </div>
                    <div class="p-2 border-start border-3 border-success bg-light">
                        <div class="fw-bold small text-dark mb-1">6. 상품매출원가 대체분개</div>
                        <div class="ps-2 small text-muted">상품매출원가 / 상품</div>
                    </div>
                </div>
            </div>
            <div class="mt-3 text-center text-danger small fw-bold">
                * 위 규칙에 준하여 전표를 입력한 후 마감작업을 진행해야 합니다.
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
import AppAlert from '@/components/AppAlert.vue'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useRouter } from 'vue-router'

const authStore = useAuthStore()
const router = useRouter()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()

const now = new Date()
const currentYear = now.getFullYear()
const currentMonth = String(now.getMonth() + 1).padStart(2, '0')

const formData = reactive({
  yy: String(currentYear),
  mm: currentMonth
})

const yearOptions = Array.from({ length: 10 }, (_, i) => String(currentYear - i))
const monthOptions = Array.from({ length: 12 }, (_, i) => String(i + 1).padStart(2, '0'))

const errorList = ref<any[]>([])
const gridRef = ref<HTMLElement | null>(null)
let grid: Tabulator | null = null

const initGrid = () => {
  if (!gridRef.value) return
  grid = new Tabulator(gridRef.value, {
    layout: "fitColumns",
    height: "100%",
    data: errorList.value,
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "전표번호", field: "slip_no_full", width: 150, hozAlign: "center", cssClass: "fw-bold text-primary",
        cellClick: (e, cell) => {
            const d = cell.getData();
            router.push({ name: 'HASL110U', query: { slipymd: d.slipymd, slipno: d.slipno } });
        }
      },
      { title: "적 요", field: "remark", widthGrow: 2, hozAlign: "left" },
      { title: "차 변", field: "amt_dr", width: 140, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "대 변", field: "amt_cr", width: 140, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } }
    ]
  })
}

async function executeClosing() {
  if (!confirm(`${formData.yy}년 ${formData.mm}월 마감작업을 하시겠습니까?`)) return

  try {
    errorList.value = []
    const res = await api.post('/hacl/HACL_800U_STR', {
      cmpycd: authStore.cmpycd,
      yy: formData.yy,
      mm: formData.mm
    })

    if (res.data && res.data.length > 0) {
      const first = res.data[0]
      const resultCode = String(Object.values(first)[0])
      const resultMsg = String(Object.values(first)[1])

      if (resultCode === '999') {
        vAlert(resultMsg)
      } else if (resultCode > '000') {
        vAlertError('이상이 있는 전표가 있습니다. 수정하신 후 재 작업하시기 바랍니다.')
        errorList.value = res.data.map((row: any) => {
            return {
                slipymd: row.slipymd || '',
                slipno: row.slipno || '',
                slip_no_full: `${row.slipymd || ''}-${row.slipno || ''}`,
                remark: row.remark || '',
                amt_dr: Number(row.dbamt || 0),
                amt_cr: Number(row.cramt || 0)
            }
        })

        await nextTick()
        initGrid()
      } else {
        vAlert(resultMsg || '마감작업이 성공적으로 완료되었습니다.')
      }
    }
  } catch (e) {
    vAlertError('작업 중 통신 오류가 발생했습니다.')
  }
}

function initialize() {
  formData.yy = String(currentYear)
  formData.mm = currentMonth
  errorList.value = []
}

onMounted(() => {
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
