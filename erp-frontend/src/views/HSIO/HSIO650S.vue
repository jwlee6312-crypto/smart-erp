<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-arrow-left-right me-2 text-primary" style="font-size: 18px;"></i>
        영업정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        재고관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">창고별 수불현황 (HSIO650S)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-print" @click="print('Print')">인쇄</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">
      <!-- 🅰️ 조회 조건 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
                <col style="width: 10%" /><col style="width: 25%" />
                <col style="width: 10%" /><col style="width: 15%" />
                <col style="width: 10%" /><col style="width: 30%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light">입출일자</th>
                <td class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                  <DateForm v-model:fromdt="searchData.fymd" v-model:todt="searchData.tymd" />
                </td>
                <th class="text-center bg-light">창&nbsp;&nbsp;&nbsp;&nbsp;고</th>
                <td>
                  <select v-model="searchData.whcd" class="form-select form-select-sm">
                    <option value="000">전체</option>
                    <option v-for="opt in whOptions" :key="opt.whcd" :value="opt.whcd">{{ opt.whnm }}</option>
                  </select>
                </td>
                <th class="text-center bg-light">품&nbsp;&nbsp;&nbsp;&nbsp;목</th>
                <td>
                  <div class="input-group input-group-sm">
                    <input v-model="searchData.itemcd" type="text" class="form-control text-center bg-light" style="max-width: 90px;" readonly />
                    <input v-model="searchData.itemnm" type="text" class="form-control fw-bold text-primary" placeholder="품목 선택 (Enter)" @keyup.enter="openHelp('ITEM')" readonly />
                    <input v-model="searchData.itsize" type="text" class="form-control bg-light" readonly />
                    <button class="btn btn-outline-secondary" @click="openHelp('ITEM')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 🅱️ 데이터 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>품목별 상세 수불 이력</span>
          <div class="small text-muted">출고번호 또는 적요를 클릭하면 원본 전표로 이동합니다.</div>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="gridElement" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, computed, nextTick, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import DateForm from '@/components/DateForm.vue'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useTabStore } from '@/stores/tabStore'
import { addDynamicRoute } from '@/router/dynamicRoute'
import { useFormReset } from '@/composables/useFormReset'
import { useCommonHelp } from '@/composables/useCommonHelp'
import { getDate } from '@/composables/useDate'

const authStore = useAuthStore()
const tabStore = useTabStore()
const router = useRouter()
const route = useRoute()
const { firstDay, today } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps } = useCommonHelp()

// 1. 상태 관리
const searchData = reactive({
  fymd: (route.query.fymd as string) || firstDay,
  tymd: (route.query.tymd as string) || today,
  whcd: (route.query.whcd as string) || '000',
  astkind: (route.query.astkind as string) || '120',
  itemcd: (route.query.itemcd as string) || '',
  itemnm: '',
  itsize: '',
  unit: ''
})

const whOptions = ref<any[]>([])
const gridElement = ref<HTMLElement | null>(null)
const grid = ref<Tabulator | null>(null)

// 2. 그리드 초기화
const initGrid = () => {
  if (!gridElement.value) return
  grid.value = new Tabulator(gridElement.value, {
    layout: "fitColumns",
    height: "100%",
    placeholder: "조회된 데이터가 없습니다.",
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },

    columns: [
      { title: "No", formatter: "rownum", width: 40, hozAlign: "center" },
      {
        title: "출고번호", field: "io_full", width: 150, hozAlign: "center",
        formatter: (cell) => {
            const val = cell.getValue();
            return val ? `<span class="fw-bold">${val}</span>` : '';
        }
      },
      {
        title: "적요 (거래처/유형)", field: "remark", minWidth: 250,
        formatter: (cell) => `<span>${cell.getValue()}</span>`
      },
      { title: "입출구분", field: "iotypenm", width: 100, hozAlign: "center" },
      { title: "입고", field: "inqty", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "출고", field: "outqty", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      {
        title: "재고", field: "stkqty", width: 150, hozAlign: "right",
        formatter: (cell) => {
          const val = Number(cell.getValue()) || 0;
          const formatted = new Intl.NumberFormat().format(val);
          return val < 0 ? `<span class="text-danger fw-bold">${formatted}</span>` : `<span class="fw-bold">${formatted}</span>`;
        }
      },
      { title: "입출창고", field: "whnm", width: 150 },
      { title: "입출부서", field: "deptnm", width: 150 }
    ]
  })
}

// 3. 기능 구현
async function fetchWhOptions() {
  try {
    const resWh = await api.post('/hs00/HS00_000S_STR', { gubun: 'W0', cmpycd: authStore.cmpycd })
    whOptions.value = resWh.data
  } catch (e) { console.error('창고 옵션 로드 실패') }
}

async function search() {
  if (!searchData.itemcd) return vAlertError('품목을 선택해 주십시요.')
  try {
    const res = await api.post('/hsio/HSIO_650S_STR', {
      cmpycd: authStore.cmpycd,
      whcd: searchData.whcd,
      fromdt: searchData.fymd.replace(/-/g, ''),
      todt: searchData.tymd.replace(/-/g, ''),
      astkind: searchData.astkind,
      itemcd: searchData.itemcd
    })

    let currentStock = 0;
    const processedData = (res.data || []).map((i: any) => {
        // 🚀 모든 키를 소문자로 변환 (데이터 유실 방지 핵심 로직)
        const item = Object.fromEntries(Object.entries(i).map(([k, v]) => [k.toLowerCase(), v]));

        const inqty = Number(item.inqty) || 0;
        const outqty = Number(item.outqty) || 0;
        currentStock = currentStock + inqty - outqty;

        return {
            ...item,
            io_full: item.ioymd === '00000000' || !item.ioym ? '' : `${item.ioym}-${item.iono}`,
            remark: `${item.custnm || ''} / ${item.iotypenm || ''}`,
            stkqty: currentStock
        }
    })
    grid.value?.setData(processedData)
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

const navigateToOrigin = (row: any) => {
    // 🚀 [지시사항 반영] 프로그램 단위 Link 처리 제거 (조회 전용 유지)
    console.log('상세 데이터:', row);
}

const loadItemInfoAndSearch = async () => {
    if (!searchData.itemcd) return;
    try {
        const r = await api.post('/hs00/HS00_000S_STR', { gubun: 'I1', cmpycd: authStore.cmpycd, gbncd: '2', code: searchData.itemcd });
        if (r.data?.length) {
            const d = r.data[0];
            searchData.itemnm = d.itemnm;
            searchData.itsize = d.itsize;
            searchData.unit = d.unitnm;
            search();
        }
    } catch (e) { console.error('품목 정보 로드 실패'); }
}

watch(() => route.query.itemcd, (newVal) => {
    if (newVal && route.path.includes('/HSIO650S')) {
        // 🚀 URL 쿼리 파라미터가 변경되면 검색 조건 동기화 및 즉시 재조회
        Object.assign(searchData, {
            fymd: (route.query.fymd as string) || searchData.fymd,
            tymd: (route.query.tymd as string) || searchData.tymd,
            whcd: (route.query.whcd as string) || searchData.whcd,
            astkind: (route.query.astkind as string) || searchData.astkind,
            itemcd: newVal as string
        });
        loadItemInfoAndSearch();
    }
}, { immediate: false })

function initialize() {
  resetForm(searchData)
  Object.assign(searchData, { fymd: firstDay, tymd: today, whcd: '000', astkind: '120', itemcd: '', itemnm: '', itsize: '', unit: '' })
  grid.value?.clearData()
}

function print(type: string) {
    vAlert(`${type} 기능은 준비 중입니다.`)
}

function openHelp(type: string) {
  if (type === 'ITEM') {
    Object.assign(modalProps, {
      title: '품목 선택',
      path: '/hs00/HS00_000S_STR',
      defaultField: 'itemnm',
      data: { gubun: 'I1', cmpycd: authStore.cmpycd, gbncd: '2' },
      columns: [
        { title: '코드', field: 'itemcd', width: 100 },
        { title: '품목명', field: 'itemnm', minWidth: 200 },
        { title: '규격', field: 'itsize', width: 150 },
        { title: '단위', field: 'unitnm', width: 80 }
      ],
      onConfirm: (data: any) => {
        searchData.itemcd = data.itemcd
        searchData.itemnm = data.itemnm
        searchData.itsize = data.itsize
        searchData.unit = data.unitnm
        search()
      }
    })
    modalVisible.value = true
  }
}

onMounted(async () => {
  await fetchWhOptions()
  nextTick(() => {
      initGrid()
      loadItemInfoAndSearch()
  })
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
