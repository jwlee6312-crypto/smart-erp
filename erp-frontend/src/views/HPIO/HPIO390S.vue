<!--
	=============================================================
	프로그램명	: 품목투입자재현황 (HPIO390S)
	작성일자	: 2025.02.24
	설명        : 생산 제품별 투입된 자재의 상세 내역 및 소요량 현황 조회 (HPIO210U 표준 패턴 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-diagram-3 me-2 text-primary" style="font-size: 18px;"></i>
        생산정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">품목투입자재현황 (HPIO390S)</span>
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
                <col style="width: 10%" /><col style="width: 20%" />
                <col style="width: 10%" /><col style="width: 20%" />
                <col style="width: 10%" /><col style="width: 30%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light required">생산라인</th>
                <td>
                  <select v-model="searchData.linecd" class="form-select form-select-sm" @change="onLineChange">
                    <option v-for="opt in lineOptions" :key="opt.linecd" :value="opt.linecd">{{ opt.linenm }}</option>
                  </select>
                </td>
                <th class="text-center bg-light">생산공정</th>
                <td>
                  <select v-model="searchData.progcd" class="form-select form-select-sm">
                    <option value="">전체공정</option>
                    <option v-for="opt in progOptions" :key="opt.progcd" :value="opt.progcd">{{ opt.prognm }}</option>
                  </select>
                </td>
                <th class="text-center bg-light required">투입일자</th>
                <td class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                  <input v-model="fromdt" type="date" class="form-control form-control-sm" style="width: 140px;" />
                  <span class="px-1 opacity-50">~</span>
                  <input v-model="todt" type="date" class="form-control form-control-sm" style="width: 140px;" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column grid-container-right">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark"><i class="bi bi-list-check me-2 text-primary"></i>품목별 자재 투입 상세 내역</span>
          <span v-if="rowCount" class="badge bg-secondary-subtle text-dark border border-secondary-subtle" style="font-size: 10px;">Total: {{ rowCount }}건</span>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="tableRef" class="tabulator-instance flex-grow-1"></div>
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
const { today, firstDay } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

// [1] 데이터 모델링
const searchData = reactive({
  linecd: '010',
  progcd: '',
  fromdt: firstDay.replace(/-/g, ''),
  todt: today.replace(/-/g, '')
})

const lineOptions = ref<any[]>([]); const progOptions = ref<any[]>([])
const rowCount = ref(0)

// 포맷팅 헬퍼 (ui 접두어 제거)
const fromdt = computed({ get: () => formatDate(searchData.fromdt), set: (v) => { if (v) searchData.fromdt = v.replace(/-/g, '') } })
const todt = computed({ get: () => formatDate(searchData.todt), set: (v) => { if (v) searchData.todt = v.replace(/-/g, '') } })

const tableRef = ref<HTMLDivElement | null>(null)
let grid: Tabulator | null = null

// [2] 그리드 초기화
const initGrids = () => {
  grid = new Tabulator(tableRef.value!, {
    layout: "fitColumns", height: "100%", placeholder: "데이터 없음",
    groupBy: "itemcd",
    groupHeader: (value, count, data) => {
      return `
        <div class="d-flex align-items-center gap-3">
            <span class="badge bg-primary-subtle text-primary border border-primary-subtle px-2">생산제품</span>
            <span class="fw-bold">[${value}] ${data[0].itemnm}</span>
            <span class="text-secondary small">| 규격: ${data[0].itsize || '-'}</span>
            <span class="text-secondary small">| 단위: ${data[0].unit}</span>
            <span class="ms-auto text-dark fw-bold pe-3">생산량: ${Number(data[0].prdqty || 0).toLocaleString()}</span>
        </div>
      `;
    },
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "생산구분", field: "prodnm", width: 150, hozAlign: "center" },
      { title: "자재코드", field: "mitemcd", width: 150, hozAlign: "center" },
      { title: "투입자재명", field: "mitemnm", minWidth: 250, widthGrow: 1, cssClass: "fw-bold text-success" },
      { title: "규격", field: "mitsize", width: 200 },
      { title: "단위", field: "munit", width: 80, hozAlign: "center" },
      { title: "투입량", field: "inqty", width: 200, hozAlign: "right", formatter: "money", cssClass: "text-primary fw-bold" }
    ],
  });
}

// [3] 비즈니스 로직
const fetchLineOptions = async () => {
  try {
    const res = await api.get('/hp00/HP00_000S_STR', { params: { gubun: 'L0', cmpycd: authStore.cmpycd, gbncd: 'Y', code: '' } })
    lineOptions.value = (res.data || []).map((i: any) => ({
        linecd: i.linecd,
        linenm: i.linenm
    }));
    if (lineOptions.value.length > 0) onLineChange();
  } catch (e) {}
}

const onLineChange = async () => {
  try {
    const res = await api.get('/hp00/HP00_000S_STR', { params: { gubun: 'G0', cmpycd: authStore.cmpycd, gbncd: searchData.linecd, code: '' } })
    progOptions.value = (res.data || []).map((i: any) => ({
        progcd: i.progcd,
        prognm: i.prognm
    }));
  } catch (e) {}
}

async function fetchList() {
  if (!searchData.linecd) return vAlertError('생산라인을 선택하세요.')
  try {
    const res = await api.post('/hpio/HPIO_390S_STR', {
      cmpycd: authStore.cmpycd,
      linecd: searchData.linecd,
      progcd: searchData.progcd,
      fromdt: searchData.fromdt,
      todt: searchData.todt
    })

    grid?.setData(res.data)
    rowCount.value = res.data.length
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

const initialize = () => {
  resetForm(searchData)
  Object.assign(searchData, { linecd: '010', progcd: '', fromdt: firstDay.replace(/-/g, ''), todt: today.replace(/-/g, '') })
  grid?.clearData(); rowCount.value = 0;
}

const exportExcel = () => grid?.download("xlsx", `품목투입자재현황_${searchData.todt}.xlsx`)
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
