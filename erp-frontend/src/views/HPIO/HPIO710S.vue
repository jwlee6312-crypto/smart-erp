<!--
	=============================================================
	프로그램명	: 입출고 현황 (HPIO710S)
	작성일자	: 2025.02.24
	설명        : 창고별/품목별 상세 입출고 이력 조회 (HPIO210U 표준 패턴 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-box-arrow-in-down me-2 text-primary" style="font-size: 18px;"></i>
        재고관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">입출고 현황 (HPIO710S)</span>
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
                <th class="text-center bg-light required">입고창고</th>
                <td>
                  <select v-model="searchData.whcd" class="form-select form-select-sm">
                    <option value="000">전체창고</option>
                    <option v-for="opt in whOptions" :key="opt.whcd" :value="opt.whcd">{{ opt.whnm }}</option>
                  </select>
                </td>
                <th class="text-center bg-light required">입고유형</th>
                <td>
                  <select v-model="searchData.iotype" class="form-select form-select-sm">
                    <option value="000">전체유형</option>
                    <option v-for="opt in iotypeOptions" :key="opt.code" :value="opt.code">{{ opt.cdnm }}</option>
                  </select>
                </td>
                <th class="text-center bg-light required">입고일자</th>
                <td class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                  <input v-model="fromdt_f" type="date" class="form-control form-control-sm" style="width: 140px;" />
                  <span class="px-1 opacity-50">~</span>
                  <input v-model="todt_f" type="date" class="form-control form-control-sm" style="width: 140px;" />
                </td>
              </tr>
              <tr>
                <th class="text-center bg-light">거 래 처</th>
                <td>
                  <div class="input-group input-group-sm">
                    <input v-model="searchData.custcd" type="text" class="form-control text-center bg-light fw-bold" style="max-width: 60px;" readonly />
                    <input v-model="searchData.custnm" type="text" class="form-control" placeholder="거래처 선택" />
                    <button class="btn btn-outline-secondary px-2" @click="handleOpenHelp('CUST')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="text-center bg-light">입고품목</th>
                <td colspan="3">
                  <div class="input-group input-group-sm w-75">
                    <input v-model="searchData.itemcd" type="text" class="form-control text-center bg-light fw-bold" style="max-width: 80px;" readonly />
                    <input v-model="searchData.itemnm" type="text" class="form-control" placeholder="품목 선택" />
                    <button class="btn btn-outline-secondary px-2" @click="handleOpenHelp('ITEM')"><i class="bi bi-search"></i></button>
                    <input v-model="searchData.itsize" type="text" class="form-control bg-light" style="max-width: 150px;" readonly placeholder="규격" />
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
          <span class="fw-bold small text-dark"><i class="bi bi-list-check me-2 text-primary"></i>입고 상세 내역</span>
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
import { useRouter } from 'vue-router'
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
const router = useRouter()
const { today, firstDay } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps, openHelp } = useCommonHelp()

// [1] 데이터 모델링
const searchData = reactive({
  whcd: '000',
  iotype: '000',
  fromdt: firstDay.replace(/-/g, ''),
  todt: today.replace(/-/g, ''),
  custcd: '', custnm: '',
  itemcd: '', itemnm: '', itsize: '', unit: ''
})

const whOptions = ref<any[]>([]); const iotypeOptions = ref<any[]>([])
const rowCount = ref(0)

// 포맷팅 헬퍼
const fromdt_f = computed({ get: () => formatDate(searchData.fromdt), set: (v) => { if (v) searchData.fromdt = v.replace(/-/g, '') } })
const todt_f = computed({ get: () => formatDate(searchData.todt), set: (v) => { if (v) searchData.todt = v.replace(/-/g, '') } })

const tableRef = ref<HTMLDivElement | null>(null)
let grid: Tabulator | null = null

// [2] 그리드 초기화
const initGrids = () => {
  grid = new Tabulator(tableRef.value!, {
    layout: "fitColumns", height: "100%", placeholder: "데이터 없음",
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "No", formatter: "rownum", width: 40, hozAlign: "center" },
      { title: "일 자", field: "ioymd", width: 110, hozAlign: "center", formatter: (c) => formatDate(c.getValue()) },
      {
        title: "품 명", field: "itemnm", minWidth: 200, widthGrow: 1,
        formatter: (cell) => `<span class="${cell.getData().gubun === '1' && cell.getData().iotype === '100' ? 'text-primary text-decoration-underline cursor-pointer fw-bold' : 'fw-bold'}">${cell.getValue() || ''}</span>`,
        cellClick: (e, cell) => {
          const d = cell.getData()
          if (d.gubun === '1' && d.iotype === '100') {
            router.push({ path: '/HSIO/HSIO100U', query: { deptcd: d.deptcd, ioym: d.ioym, iono: d.iono } })
          }
        },
        bottomCalc: () => "합 계"
      },
      { title: "규격", field: "itsize", width: 150 },
      { title: "단위", field: "unit", width: 60, hozAlign: "center" },
      { title: "거래처", field: "custnm", minWidth: 150 },
      { title: "수량", field: "ioqty", width: 90, hozAlign: "right", formatter: "money", bottomCalc: "sum" },
      { title: "단가", field: "price", width: 100, hozAlign: "right", formatter: "money" },
      { title: "공급가", field: "jsanamt", width: 110, hozAlign: "right", formatter: "money", bottomCalc: "sum" },
      { title: "부가세", field: "jsanvat", width: 100, hozAlign: "right", formatter: "money", bottomCalc: "sum" },
      { title: "합 계", field: "tot_amt", width: 120, hozAlign: "right", formatter: "money", mutatorData: (v,d) => Number(d.jsanamt||0)+Number(d.jsanvat||0), bottomCalc: "sum", cssClass: "text-primary fw-bold" }
    ],
    columnCalcs: "table"
  });
}

// [3] 비즈니스 로직
const fetchOptions = async () => {
  try {
    const [wh, io] = await Promise.all([
      api.get('/hs00/HS00_000S_STR', { params: { gubun: 'W0', cmpycd: authStore.cmpycd } }),
      api.get('/hp00/HP00_000S_STR', { params: { gubun: 'E0', cmpycd: authStore.cmpycd, gbncd: '120' } })
    ]);
    whOptions.value = wh.data.map((i: any) => ({ whcd: i.whcd, whnm: i.whnm }));
    iotypeOptions.value = io.data.map((i: any) => ({ code: i.code || i.code, cdnm: i.cdnm }));
  } catch (e) {}
}

async function fetchList() {
  try {
    const res = await api.post('/hpio/HPIO_710S_STR', {
      cmpycd: authStore.cmpycd, whcd: searchData.whcd, iotype: searchData.iotype, fromdt: searchData.fromdt, todt: searchData.todt, custcd: searchData.custcd, itemcd: searchData.itemcd
    })
    grid?.setData(res.data)
    rowCount.value = res.data.length
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

const handleOpenHelp = (type: string) => {
  if (type === 'CUST') {
    openHelp('CUST', (d) => { searchData.custcd = d.custcd; searchData.custnm = d.custnm });
  } else if (type === 'ITEM') {
    openHelp('ITEM', (d) => {
      Object.assign(searchData, { itemcd: d.itemcd, itemnm: d.itemnm, itsize: d.itsize, unit: d.unit });
    }, { asetkind: '1' });
  }
}

const initialize = () => {
  resetForm(searchData)
  Object.assign(searchData, { whcd: '000', iotype: '000', fromdt: initfromdt, todt: inittymd })
  grid?.clearData(); rowCount.value = 0;
}

const exportExcel = () => grid?.download("xlsx", `입출고현황_${searchData.todt}.xlsx`)
const formatDate = (v: any) => v && v.length === 8 ? `${v.substring(0, 4)}-${v.substring(4, 6)}-${v.substring(6, 8)}` : v;

onMounted(async () => {
  await fetchOptions();
  nextTick(initGrids);
  fetchList();
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
.grid-container-right { border-bottom: 3px solid #005a9f !important; }
</style>
