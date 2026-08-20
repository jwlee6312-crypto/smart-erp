<!--
	=============================================================
	프로그램명	: 제품입고명세서 (HPIO430S)
	작성일자	: 2025.02.24
	설명        : 창고별/제품별 상세 입고 내역 및 생산 연계 정보 조회 (HPIO210U 표준 패턴 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-file-earmark-ruled me-2 text-primary" style="font-size: 18px;"></i>
        생산정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">제품입고명세서 (HPIO430S)</span>
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
                <col style="width: 10%" /><col style="width: 25%" />
                <col style="width: 10%" /><col style="width: 25%" />
                <col style="width: 10%" /><col style="width: 30%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light required">입고창고</th>
                <td>
                  <div class="input-group input-group-sm">
                    <input v-model="searchData.whcd" type="text" class="form-control text-center bg-light fw-bold" style="max-width: 60px;" readonly />
                    <input v-model="searchData.whnm" type="text" class="form-control" placeholder="창고 선택" />
                    <button class="btn btn-outline-secondary px-2" @click="handleOpenHelp('WH')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="text-center bg-light required">입고일자</th>
                <td class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                  <input v-model="fromdt" type="date" class="form-control form-control-sm" style="width: 140px;" />
                  <span class="px-1 opacity-50">~</span>
                  <input v-model="todt" type="date" class="form-control form-control-sm" style="width: 140px;" />
                </td>
                <th class="text-center bg-light">제 품</th>
                <td>
                  <div class="input-group input-group-sm">
                    <input v-model="searchData.itemcd" type="text" class="form-control text-center bg-light fw-bold" style="max-width: 80px;" readonly />
                    <input v-model="searchData.itemnm" type="text" class="form-control" placeholder="제품 선택" />
                    <button class="btn btn-outline-secondary px-2" @click="handleOpenHelp('ITEM')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column grid-container-right">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark"><i class="bi bi-list-columns me-2 text-primary"></i>제품 입고 상세 내역</span>
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
import { useCommonHelp } from '@/composables/useCommonHelp'
import { getDate } from '@/composables/useDate'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'

const authStore = useAuthStore()
const { today, firstDay } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps, openHelp } = useCommonHelp()

// [1] 데이터 모델링
const searchData = reactive({
  whcd: '200',
  whnm: '제품창고',
  fromdt: firstDay.replace(/-/g, ''),
  todt: today.replace(/-/g, ''),
  itemcd: '', itemnm: '', itsize: '', unit: ''
})

const rowCount = ref(0)
const fromdt = computed({ get: () => formatDate(searchData.fromdt), set: (v) => { if (v) searchData.fromdt = v.replace(/-/g, '') } })
const todt = computed({ get: () => formatDate(searchData.todt), set: (v) => { if (v) searchData.todt = v.replace(/-/g, '') } })

const tableRef = ref<HTMLDivElement | null>(null)
let grid: Tabulator | null = null

// [2] 그리드 초기화
const initGrids = () => {
  grid = new Tabulator(tableRef.value!, {
    layout: "fitColumns", height: "100%", placeholder: "데이터 없음",
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "No", formatter: "rownum", width: 40, hozAlign: "center" },
      { title: "입고일자", field: "inymd", width: 110, hozAlign: "center", formatter: (c) => formatDate(c.getValue()) },
      { title: "제품명", field: "itemnm", minWidth: 200, widthGrow: 1, cssClass: "fw-bold", formatter: (c) => `[${c.getData().itemcd || ''}] ${c.getValue() || ''}` },
      { title: "규격", field: "itsize", width: 150 },
      { title: "단위", field: "unit", width: 70, hozAlign: "center" },
      { title: "생산라인", field: "linenm", width: 150 },
      { title: "생산공정", field: "prognm", width: 150 },
      { title: "생산일자", field: "proymd", width: 110, hozAlign: "center", formatter: (c) => formatDate(c.getValue()) },
      { title: "입고량", field: "inqty", width: 120, hozAlign: "right", formatter: "money", cssClass: "text-primary fw-bold" }
    ],
  });
}

// [3] 비즈니스 로직
async function fetchList() {
  if (!searchData.whcd) return vAlertError('입고창고를 선택하세요.')
  try {
    const res = await api.post('/hpio/HPIO_430S_STR', {
      cmpycd: authStore.cmpycd, whcd: searchData.whcd, itemcd: searchData.itemcd || '',
      proymdF: searchData.fromdt, proymdT: searchData.todt
    })
    grid?.setData(res.data)
    rowCount.value = res.data.length
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

const handleOpenHelp = (type: string) => {
  if (type === 'WH') {
    openHelp('WH', (d) => { searchData.whcd = d.whcd; searchData.whnm = d.whnm }, { gubun: 'W1' });
  } else if (type === 'ITEM') {
    openHelp('ITEM', (d) => {
      Object.assign(searchData, { itemcd: d.itemcd, itemnm: d.itemnm, itsize: d.itsize, unit: d.unit });
    }, { codegbn: 'B' });
  }
}

const initialize = () => {
  resetForm(searchData)
  Object.assign(searchData, { whcd: '200', whnm: '제품창고', fromdt: firstDay.replace(/-/g, ''), todt: today.replace(/-/g, '') })
  grid?.clearData(); rowCount.value = 0;
}

const exportExcel = () => grid?.download("xlsx", `제품입고명세_${searchData.todt}.xlsx`)
const formatDate = (v: any) => v && v.length === 8 ? `${v.substring(0, 4)}-${v.substring(4, 6)}-${v.substring(6, 8)}` : v;

onMounted(() => { nextTick(initGrids); fetchList(); })
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
.grid-container-right { border-bottom: 3px solid #005a9f !important; }
</style>
