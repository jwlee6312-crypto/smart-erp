<!--
	=============================================================
	프로그램명	: 창고별현재고 (HPIO720S)
	작성일자	: 2025.02.24
	설명        : 창고별/자산별 품목의 실시간 재고 현황 및 적정재고 대비 과부족 조회 (HPIO210U 표준 패턴 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-house-door-fill me-2 text-primary" style="font-size: 18px;"></i>
        생산정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">창고별현재고 (HPIO720S)</span>
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
                <col style="width: 10%" /><col style="width: 20%" />
                <col style="width: 10%" /><col style="width: 25%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light required">기준일자</th>
                <td><input v-model="ymd_f" type="date" class="form-control form-control-sm" style="max-width: 150px;" /></td>
                <th class="text-center bg-light required">창 고</th>
                <td>
                  <select v-model="searchData.whcd" class="form-select form-select-sm">
                    <option value="000">전체창고</option>
                    <option v-for="opt in whOptions" :key="opt.whcd" :value="opt.whcd">{{ opt.whnm }}</option>
                  </select>
                </td>
                <th class="text-center bg-light required">재고자산</th>
                <td>
                  <select v-model="searchData.astkind" class="form-select form-select-sm" style="max-width: 150px;">
                    <option v-for="opt in astOptions" :key="opt.code" :value="opt.code">{{ opt.cdnm }}</option>
                  </select>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column grid-container-right">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark"><i class="bi bi-list-columns-reverse me-2 text-primary"></i>창고별 품목 재고 현황</span>
          <div class="d-flex align-items-center gap-3">
            <span class="text-muted small"><i class="bi bi-info-circle me-1"></i> 품목명을 클릭하면 상세 수불 내역으로 이동합니다.</span>
            <span v-if="rowCount" class="badge bg-secondary-subtle text-dark border border-secondary-subtle" style="font-size: 10px;">Total: {{ rowCount }}건</span>
          </div>
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
const { today } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps, openHelp } = useCommonHelp()

// [1] 데이터 모델링
const searchData = reactive({
  ymd: today.replace(/-/g, ''),
  whcd: '000',
  astkind: '100'
})

const whOptions = ref<any[]>([]); const astOptions = ref<any[]>([])
const rowCount = ref(0)
const ymd_f = computed({ get: () => formatDate(searchData.ymd), set: (v) => { if (v) searchData.ymd = v.replace(/-/g, '') } })

const tableRef = ref<HTMLDivElement | null>(null)
let grid: Tabulator | null = null

// [2] 그리드 초기화
const initGrids = () => {
  grid = new Tabulator(tableRef.value!, {
    layout: "fitColumns", height: "100%", placeholder: "데이터 없음",
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "품목코드", field: "itemcd", width: 100, hozAlign: "center" },
      { title: "품 목 명", field: "itemnm", minWidth: 200, widthGrow: 1, cssClass: "fw-bold text-primary text-decoration-underline cursor-pointer",
        cellClick: (e, cell) => navigateToDetail(cell.getData())
      },
      { title: "규격", field: "itsize", width: 200 },
      { title: "단위", field: "unit", width: 60, hozAlign: "center" },
      { title: "적정재고", field: "stock", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "현재고", field: "stkqty", width: 150, hozAlign: "right", formatter: "money",
        cssClass: "fw-bold",
        formatterParams: (cell: any) => {
          const d = cell.getData();
          return { color: Number(d.stkqty) < Number(d.stock) ? "red" : "" }
        }
      },
      { title: "과부족", field: "diff", width: 150, hozAlign: "right", formatter: "money", mutatorData: (v,d) => Number(d.stkqty||0) - Number(d.stock||0) },
      { title: "재고금액", field: "stkamt", width: 150, hozAlign: "right", formatter: "money" },
      { title: "미입고량", field: "nonqty", width: 150, hozAlign: "right", formatter: "money" },
      { title: "총 수 량", field: "total_qty", width: 150, hozAlign: "right", formatter: "money", mutatorData: (v,d) => Number(d.stkqty||0) + Number(d.nonqty||0), cssClass: "text-primary fw-bold" }
    ],
    columnCalcs: "table"
  });
}

// [3] 비즈니스 로직
const fetchOptions = async () => {
  try {
    const [wh, ast] = await Promise.all([
      api.get('/hs00/HS00_000S_STR', { params: { gubun: 'W0', cmpycd: authStore.cmpycd } }),
      api.get('/hp00/HP00_000S_STR', { params: { gubun: 'J0', cmpycd: authStore.cmpycd, gbncd: '100' } })
    ]);
    whOptions.value = wh.data.map((i: any) => ({ whcd: i.whcd, whnm: i.whnm }));
    astOptions.value = ast.data.map((i: any) => ({ code: i.code || i.code, cdnm: i.cdnm }));
    if (astOptions.value.length > 0) searchData.astkind = astOptions.value[0].code;
  } catch (e) {}
}

async function fetchList() {
  if (!searchData.astkind) return vAlertError('재고자산을 선택하세요.')
  try {
    const res = await api.post('/hpio/HPIO_720S_STR', {
      cmpycd: authStore.cmpycd, ymd: searchData.ymd, whcd: searchData.whcd, astkind: searchData.astkind
    })
    grid?.setData(res.data)
    rowCount.value = res.data.length
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

const navigateToDetail = (data: any) => {
    router.push({
        path: '/HPIO/HPIO650S',
        query: { astkind: searchData.astkind, whcd: searchData.whcd, itemcd: data.itemcd, fymd: searchData.ymd.substring(0, 6) + '01', tymd: searchData.ymd }
    })
}

const initialize = () => {
  resetForm(searchData)
  Object.assign(searchData, { ymd: today.replace(/-/g, ''), whcd: '000', astkind: '100' })
  grid?.clearData(); rowCount.value = 0;
}

const exportExcel = () => grid?.download("xlsx", `창고별현재고_${searchData.ymd}.xlsx`)
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
