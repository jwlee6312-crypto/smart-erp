<!--
	=============================================================
	프로그램명	: 재공입출고현황 (HPIO660S)
	작성일자	: 2025.02.24
	설명        : 생산 공정별/라인별 재공품의 입고, 출고, 현재고 현황 조회 (HPIO210U 표준 패턴 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-box-seam me-2 text-primary" style="font-size: 18px;"></i>
        생산정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">재공입출고현황 (HPIO660S)</span>
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
                  <select v-model="searchForm.linecd" class="form-select form-select-sm" @change="onLineChange">
                    <option value="">라인 선택</option>
                    <option v-for="opt in lineOptions" :key="opt.linecd" :value="opt.linecd">
                      [{{ opt.linecd }}] {{ opt.linenm }}
                    </option>
                  </select>
                </td>
                <th class="text-center bg-light">생산공정</th>
                <td>
                  <select v-model="searchForm.progcd" class="form-select form-select-sm" @change="fetchList">
                    <option value="">전체공정</option>
                    <option v-for="opt in progOptions" :key="opt.progcd" :value="opt.progcd">
                      [{{ opt.progcd }}] {{ opt.prognm }}
                    </option>
                  </select>
                </td>
                <th class="text-center bg-light required">입출일자</th>
                <td class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                  <input v-model="fymd_f" type="date" class="form-control form-control-sm" style="width: 140px;" />
                  <span class="px-1 opacity-50">~</span>
                  <input v-model="tymd_f" type="date" class="form-control form-control-sm" style="width: 140px;" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column grid-container-right">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark"><i class="bi bi-list-columns-reverse me-2 text-primary"></i>재공품 수불 현황</span>
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
const inittymd = today.replace(/-/g, '')
const initfymd = firstDay.replace(/-/g, '')

const searchForm = reactive({
  linecd: '010',
  progcd: '',
  fymd: initfymd,
  tymd: inittymd
})

const lineOptions = ref<any[]>([]); const progOptions = ref<any[]>([])
const rowCount = ref(0)

const fymd_f = computed({ get: () => formatDate(searchForm.fymd), set: (v) => { if (v) searchForm.fymd = v.replace(/-/g, '') } })
const tymd_f = computed({ get: () => formatDate(searchForm.tymd), set: (v) => { if (v) searchForm.tymd = v.replace(/-/g, '') } })

const tableRef = ref<HTMLDivElement | null>(null)
let grid: Tabulator | null = null

// [2] 그리드 초기화
const initGrids = () => {
  grid = new Tabulator(tableRef.value!, {
    layout: "fitColumns", height: "100%", placeholder: "데이터 없음",
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "품목코드", field: "itemcd", width: 100, hozAlign: "center" },
      { title: "품 목", field: "itemnm", minWidth: 200, widthGrow: 1, cssClass: "fw-bold text-primary text-decoration-underline cursor-pointer",
        cellClick: (e, cell) => {
          const d = cell.getData();
          router.push({
            path: '/HPIO/HPIO670S',
            query: { linecd: searchForm.linecd, progcd: searchForm.progcd, itemcd: d.itemcd, fymd: searchForm.fymd, tymd: searchForm.tymd }
          });
        }
      },
      { title: "규격", field: "itsize", width: 150 },
      { title: "단위", field: "unit", width: 60, hozAlign: "center" },
      { title: "전월이월", field: "bqty", width: 120, hozAlign: "right", formatter: "money" },
      { title: "입고", field: "iqty", width: 120, hozAlign: "right", formatter: "money", cssClass: "text-success" },
      { title: "기타입고", field: "ieqty", width: 120, hozAlign: "right", formatter: "money" },
      { title: "출고", field: "oqty", width: 120, hozAlign: "right", formatter: "money", cssClass: "text-danger" },
      { title: "이관출고", field: "tqty", width: 120, hozAlign: "right", formatter: "money" },
      { title: "타계정", field: "oeqty", width: 120, hozAlign: "right", formatter: "money" },
      { title: "현재고", field: "sqty", width: 120, hozAlign: "right", formatter: "money", cssClass: "fw-bold bg-light" }
    ],
    columnCalcs: "table"
  });
}

// [3] 비즈니스 로직
const fetchOptions = async () => {
  try {
    const resLine = await api.post('/hp00/HP00_000S_STR', { gubun: 'L0', cmpycd: authStore.cmpycd })
    lineOptions.value = resLine.data || []
    if (lineOptions.value.length > 0 && !searchForm.linecd) {
      searchForm.linecd = lineOptions.value[0].linecd
    }
    onLineChange()
  } catch (e) {}
}

const onLineChange = async () => {
  try {
    const resProg = await api.post('/hp00/HP00_000S_STR', {
      gubun: 'G0',
      cmpycd: authStore.cmpycd,
      gbncd: searchForm.linecd
    })
    progOptions.value = resProg.data || []
  } catch (e) {}
}

async function fetchList() {
  if (!searchForm.linecd) return vAlertError('생산라인을 선택하세요.')
  try {
    const res = await api.post('/hpio/HPIO_660S_STR', {
      cmpycd: authStore.cmpycd, linecd: searchForm.linecd, progcd: searchForm.progcd || '', fymd: searchForm.fymd, tymd: searchForm.tymd
    })
    grid?.setData(res.data)
    rowCount.value = res.data.length
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

const initialize = () => {
  resetForm(searchForm)
  Object.assign(searchForm, { linecd: '010', progcd: '', fymd: initfymd, tymd: inittymd })
  onLineChange();
  grid?.clearData(); rowCount.value = 0;
}

const exportExcel = () => grid?.download("xlsx", `재공입출고현황_${searchForm.tymd}.xlsx`)
const formatDate = (v: any) => v && v.length === 8 ? `${v.substring(0, 4)}-${v.substring(4, 6)}-${v.substring(6, 8)}` : v;

onMounted(() => {
  fetchOptions();
  nextTick(initGrids);
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
.grid-container-right { border-bottom: 3px solid #005a9f !important; }
</style>
