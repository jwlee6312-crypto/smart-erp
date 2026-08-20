<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-journal-text me-2 text-primary" style="font-size: 18px;"></i>
        매출관리 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        <span class="text-primary fw-bolder">판매수금일보 (HSST120S)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-outline-secondary" @click="print">인쇄</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2">
      <!-- 🅰️ 조회 조건 영역 -->
      <div class="card border shadow-sm overflow-hidden">
        <div class="card-body p-0">
          <table class="erp-table-full">
            <tbody>
              <tr>
                <th class="required" style="width: 100px;">판매부서</th>
                <td style="width: 250px;">
                  <div class="input-group input-group-sm" style="width: 220px;">
                    <input v-model="searchData.deptcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
                    <input v-model="searchData.deptnm" type="text" class="form-control" placeholder="부서 선택" @keyup.enter="openHelp('DEPT')" />
                    <button class="btn btn-outline-secondary" @click="openHelp('DEPT')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="required" style="width: 100px;">판매일자</th>
                <td>
                  <input v-model="uiymd" type="date" class="form-control form-control-sm" style="width: 150px;" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 🅱️ 데이터 그리드 영역 (여백 없이 꽉 채움) -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
          <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-1"></i> 거래처별 판매/입금 현황</span>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="gridElement" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>
    </div>
    <Modal v-model:visible="modalVisible" :modalProps="modalProps" />
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, computed, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import type { ModalProps } from '@/types/modal'

const authStore = useAuthStore()
const router = useRouter()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

const now = new Date()
const initymd = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`

// 1. 상태 관리
const searchData = reactive({
  deptcd: authStore.deptcd,
  deptnm: authStore.deptnm,
  ymd: initymd.replace(/-/g, '')
})

const uiymd = computed({
  get: () => formatDateString(searchData.ymd, '-'),
  set: (v) => searchData.ymd = v.replace(/-/g, '')
})

const gridElement = ref<HTMLElement | null>(null)
const grid = ref<Tabulator | null>(null)
const activeItemCount = ref(0)

// 2. 그리드 초기화 (다단 헤더 적용)
const initGrid = () => {
  if (!gridElement.value) return
  grid.value = new Tabulator(gridElement.value, {
    layout: "fitColumns", height: "100%", placeholder: "데이타 없음", selectable: true,
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "거 래 처 명", field: "custnm", minWidth: 200, cssClass: "fw-bold border-end" },
      {
        title: "전월이월액", field: "bfamt", width: 200, hozAlign: "right",
        formatter: "money", formatterParams: { precision: 0 },
        bottomCalc: "sum", bottomCalcFormatter: "money"
      },
      // 판매현황 그룹
      {
        title: "판매현황(vat포함)",
        columns: [
          {
            title: "당일판매금액", field: "sdamt", width: 200, hozAlign: "right",
            formatter: (cell) => {
                const val = cell.getValue();
                return val !== 0 ? `<span class="text-primary text-decoration-underline cursor-pointer">${formatNumber(val)}</span>` : "0";
            },
            bottomCalc: "sum", bottomCalcFormatter: "money",
            cellClick: (e, cell) => { if(cell.getValue() !== 0) navigateToDetail('SD', cell.getData()) }
          },
          {
            title: "당월판매금액", field: "smamt", width: 200, hozAlign: "right",
            formatter: "money", formatterParams: { precision: 0 },
            bottomCalc: "sum", bottomCalcFormatter: "money"
          },
        ]
      },
      // 입금현황 그룹
      {
        title: "입 금 현 황",
        columns: [
          {
            title: "당일입금액", field: "idamt", width: 200, hozAlign: "right",
            formatter: "money", formatterParams: { precision: 0 },
            bottomCalc: "sum", bottomCalcFormatter: "money"
          },
          {
            title: "당월입금액", field: "imamt", width: 200, hozAlign: "right",
            formatter: "money", formatterParams: { precision: 0 },
            bottomCalc: "sum", bottomCalcFormatter: "money"
          },
        ]
      },
      {
        title: "잔액", field: "jnamt", width: 200, hozAlign: "right",
        formatter: "money", formatterParams: { precision: 0 },
        cssClass: "bg-light fw-bold text-danger",
        bottomCalc: "sum", bottomCalcFormatter: "money"
      }
    ]
  })
}

// 3. 기능 구현
async function search() {
  if (!searchData.deptcd) return vAlertError('판매부서를 선택해 주십시요.')
  try {
    const res = await api.post('/hsst/HSST_120S_STR', {
      cmpycd: authStore.cmpycd,
      deptcd: searchData.deptcd,
      ymd: searchData.ymd
    })
    if (grid.value) {
      const mappedData = res.data.map((i: any) => {
          const bf = Number(i.bfamt) || 0;
          const sm = Number(i.smamt) || 0;
          const im = Number(i.imamt) || 0;
          return {
              ...i,
              jnamt: bf + sm - im // ASP 계산 로직: 전월이월 + 당월판매 - 당월입금
          }
      })
      grid.value.setData(mappedData)
      activeItemCount.value = mappedData.length
    }
  } catch (e) { vAlertError('조회 실패') }
}

const navigateToDetail = (type: string, row: any) => {
    // ASP 소스의 링크 로직 구현 (router 이용)
    // 예: HSST_140S.asp?fymd=...
    console.log("Navigate to detail", type, row.custcd);
    // router.push({ name: 'SalesDetail', query: { ... } });
}

function print() {
  window.open(`/report/HSST_120P?deptcd=${searchData.deptcd}&ymd=${searchData.ymd}&PRTGU=Print`, 'print', 'width=700,height=650')
}

function initialize() {
  resetForm(searchData)
  searchData.ymd = initymd.replace(/-/g, '')
  searchData.deptcd = authStore.deptcd
  searchData.deptnm = authStore.deptnm
  grid.value?.clearData()
}

// 4. 팝업 설정
const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function openHelp(type: string) {
  if (type === 'DEPT') {
    Object.assign(modalProps, {
      title: '부서 선택',
      path: '/ha00/HA00_00P_STR',
      defaultField: 'deptnm',
      data: { gubun: 'D0', cmpycd: authStore.cmpycd },
      columns: [
        { title: '코드', field: 'deptcd', width: 80 },
        { title: '부서명', field: 'deptnm', width: 180 }
      ],
      onConfirm: (data: any) => {
        searchData.deptcd = data.deptcd
        searchData.deptnm = data.deptnm
      }
    })
    modalVisible.value = true
  }
}

const formatDateString = (v: any, sep: string) => v && v.length === 8 ? `${v.substring(0, 4)}${sep}${v.substring(4, 6)}${sep}${v.substring(6, 8)}` : v
const formatNumber = (val: any) => new Intl.NumberFormat().format(Number(val) || 0)

onMounted(() => {
  nextTick(() => {
    initGrid()
    search()
  })
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; border-bottom: 3px solid #005a9f !important; }

/* 🚀 2단 헤더에서 단일 컬럼의 타이틀을 수직 중앙 정렬 */
:deep(.tabulator-header .tabulator-col:not(.tabulator-col-group) .tabulator-col-content) {
	height: 100% !important;
	display: flex !important;
	align-items: center !important;
	justify-content: center !important;
}
</style>