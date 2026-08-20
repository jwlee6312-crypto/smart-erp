<!--
	=============================================================
	프로그램명	: 생산일보 (HPIO360S)
	작성일자	: 2025.02.24
	설명        : 생산 라인/공정별 일일 생산 실적 및 자재 투입 현황 조회 (HPIO210U 표준 패턴 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-calendar-check me-2 text-primary" style="font-size: 18px;"></i>
        생산정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">생산일보 (HPIO360S)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchList">조회</button>
        <button class="btn-erp btn-excel" @click="exportExcel">엑셀</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 조회 필터 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
                <col style="width: 10%" /><col style="width: 23%" />
                <col style="width: 10%" /><col style="width: 23%" />
                <col style="width: 10%" /><col style="width: 24%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light required">생산라인</th>
                <td>
                  <select v-model="searchData.linecd" class="form-select form-select-sm" @change="onLineChange">
                    <option value="">라인 선택</option>
                    <option v-for="opt in lineOptions" :key="opt.linecd" :value="opt.linecd">
                      [{{ opt.linecd }}] {{ opt.linenm }}
                    </option>
                  </select>
                </td>
                <th class="text-center bg-light required">생산공정</th>
                <td>
                  <select v-model="searchData.progcd" class="form-select form-select-sm">
                    <option value="">공정 선택</option>
                    <option v-for="opt in progOptions" :key="opt.progcd" :value="opt.progcd">
                      [{{ opt.progcd }}] {{ opt.prognm }}
                    </option>
                  </select>
                </td>
                <th class="text-center bg-light required">생산일자</th>
                <td>
                  <input v-model="proymd_f" type="date" class="form-control form-control-sm" style="max-width: 150px;" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 2단 그리드 레이아웃 영역 -->
      <div class="d-flex flex-column gap-2 flex-grow-1 overflow-hidden" style="min-height: 0;">

        <!-- 상단: 생산 내역 (grid1) -->
        <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column grid-container-right">
          <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
            <span class="fw-bold small text-dark"><i class="bi bi-list-stars me-2 text-primary"></i>일일 생산 실적 현황</span>
          </div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="prodTableRef" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>

        <!-- 하단: 자재 투입 현황 (grid2) -->
        <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column grid-container-right">
          <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
            <span class="fw-bold small text-dark"><i class="bi bi-box-arrow-in-right me-2 text-success"></i>자재 투입 및 월간 누계</span>
          </div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="matTableRef" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>

      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, computed, nextTick } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { getDate } from '@/composables/useDate'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'

const authStore = useAuthStore()
const { today } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

// [1] 데이터 모델링
const searchData = reactive({
  linecd: '010',
  progcd: '',
  proymd: today.replace(/-/g, '')
})

const lineOptions = ref<any[]>([]); const progOptions = ref<any[]>([])
const proymd_f = computed({ get: () => formatDate(searchData.proymd), set: (v) => { if (v) searchData.proymd = v.replace(/-/g, '') } })

const prodTableRef = ref<HTMLDivElement | null>(null); const matTableRef = ref<HTMLDivElement | null>(null)
let prodGrid: Tabulator | null = null; let matGrid: Tabulator | null = null

// [2] 그리드 초기화
const initGrids = () => {
  // Production Grid
  prodGrid = new Tabulator(prodTableRef.value!, {
    layout: "fitColumns", height: "100%", placeholder: "데이터 없음",
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "제품코드", field: "itemcd", width: 100, hozAlign: "center" },
      { title: "제품명", field: "itemnm", minWidth: 200, widthGrow: 1, cssClass: "fw-bold" },
      { title: "규격", field: "itsize", width: 200 },
      { title: "단위", field: "unit", width: 60, hozAlign: "center" },
      { title: "생산구분", field: "prodnm", width: 150, hozAlign: "center" },
      { title: "금회생산", field: "prdqty", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "text-primary fw-bold" },
      { title: "양품수량", field: "godqty", width: 150, hozAlign: "right", formatter: "money" },
      { title: "불량수량", field: "errqty", width: 150, hozAlign: "right", formatter: "money", cssClass: "text-danger" },
      { title: "월 누계", field: "prdqty_m", width: 150, hozAlign: "right", formatter: "money", cssClass: "bg-light" }
    ],
  });
   // Material Grid
  matGrid = new Tabulator(matTableRef.value!, {
    layout: "fitColumns", height: "100%", placeholder: "데이터 없음",
    groupBy: "itemcd",
    groupHeader: (value, count, data) => `<span class='text-primary fw-bold'>[${value}] ${data[0].itemnm}</span> <span class='ms-2 opacity-50'>(${data[0].itsize})</span>`,
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "제품명", field: "itemnm", minWidth: 200, widthGrow: 1, cssClass: "fw-bold text-success" },
      { title: "생산구분", field: "prodnm", minWidth: 150, widthGrow: 1, cssClass: "fw-bold text-success" },
      { title: "자재코드", field: "mitemcd", width: 110, hozAlign: "center" },
      { title: "투입자재명", field: "mitemnm", minWidth: 200, widthGrow: 1, cssClass: "fw-bold text-success" },
      { title: "규격", field: "mitsize", width: 200 },
      { title: "단위", field: "munit", width: 70, hozAlign: "center" },
      { title: "금회투입", field: "inqty", width: 150, hozAlign: "right", formatter: "money", cssClass: "text-success fw-bold" },
      { title: "월 누계", field: "inqty_m", width: 150, hozAlign: "right", formatter: "money", cssClass: "bg-light" }
    ]
  });
}

// [3] 비즈니스 로직
const fetchLineOptions = async () => {
  try {
    const res = await api.get('/hp00/HP00_000S_STR', { params: { gubun: 'L0', cmpycd: authStore.cmpycd, gbncd: 'Y', code: '' } })
    lineOptions.value = (res.data || []).map((i: any) => ({
        linecd: i.linecd || i.code || i.CODE || '',
        linenm: i.linenm || i.cdnm || i.CDNM || ''
    }));
    if (lineOptions.value.length > 0) onLineChange();
  } catch (e) {}
}

const onLineChange = async () => {
  try {
    const res = await api.get('/hp00/HP00_000S_STR', { params: { gubun: 'G0', cmpycd: authStore.cmpycd, gbncd: searchData.linecd, code: '' } })
    progOptions.value = (res.data || []).map((i: any) => ({
        progcd: i.progcd || i.code || i.CODE || '',
        prognm: i.prognm || i.cdnm || i.CDNM || ''
    }));
    if (progOptions.value.length > 0) searchData.progcd = progOptions.value[0].progcd;
  } catch (e) {}
}

async function fetchList() {
  if (!searchData.linecd || !searchData.progcd) return vAlertError('라인과 공정을 선택하세요.')
  try {
    const [resProd, resMat] = await Promise.all([
      api.post('/hpio/HPIO_360S_STR', {
      actkind: 'S0',
      cmpycd: authStore.cmpycd,
      linecd: searchData.linecd,
      progcd: searchData.progcd,
      proymd: searchData.proymd
      }),
      api.post('/hpio/HPIO_360S_STR', { actkind: 'S1', cmpycd: authStore.cmpycd, linecd: searchData.linecd, progcd: searchData.progcd, proymd: searchData.proymd })
    ]);

    prodGrid?.setData(resProd.data);
    matGrid?.setData(resMat.data);
    vAlert('조회되었습니다.');
  } catch (e) { vAlertError('조회 실패'); }
}

const initialize = () => {
  resetForm(searchData)
  Object.assign(searchData, { linecd: '010', proymd: today.replace(/-/g, '') });
  onLineChange();
  prodGrid?.clearData(); matGrid?.clearData();
}

const exportExcel = () => prodGrid?.download("xlsx", `생산일보_${searchData.proymd}.xlsx`)
const formatDate = (v: any) => v && v.length === 8 ? `${v.substring(0, 4)}-${v.substring(4, 6)}-${v.substring(6, 8)}` : v;

onMounted(() => {
  fetchLineOptions()
  nextTick(initGrids);
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
.grid-container-right { border-bottom: 3px solid #005a9f !important; }
</style>
