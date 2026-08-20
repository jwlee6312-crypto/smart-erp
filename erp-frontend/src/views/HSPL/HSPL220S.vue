<!--
	=============================================================
	프로그램명	: 개인별판매계획대실적 (Personal Sale Plan vs Performance)
	작성일자	: 25.02.24
	작성자	    : AI Assistant
	설명        : [표준화] HSOD100U 디자인 패턴 + 그리드 체크박스 추가 + 구문 오류 수정
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom bg-white py-2 px-3 sticky-top shadow-sm">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-person-workspace me-2 text-primary" style="font-size: 18px;"></i>
        매출계획 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        <span class="text-primary fw-bolder">개인별판매계획대실적 (HSPL220S)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-outline-secondary" @click="print('Print')">인쇄</button>
        <button class="btn-erp btn-outline-success" @click="print('Excel')">엑셀</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">
      <!-- [상단] 조회 필터 영역 (HSOD100U 패턴 적용) -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
              <col style="width: 100px;" /><col style="width: 150px;" />
              <col style="width: 100px;" /><col style="width: 250px;" />
              <col />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light required">연도</th>
                <td>
                  <input v-model="searchData.yyyy" type="text" maxlength="4" class="form-control form-control-sm text-center fw-bold" placeholder="yyyy" />
                </td>
                <th class="text-center bg-light required">영업부서</th>
                <td>
                  <div class="input-group input-group-sm">
                    <input v-model="searchData.deptcd" type="text" class="form-control text-center bg-light fw-bold" style="max-width: 60px;" readonly />
                    <input v-model="searchData.deptnm" type="text" class="form-control border-start-0" placeholder="부서 선택" @keyup.enter="handleOpenHelp('DEPT')" />
                    <button class="btn btn-outline-secondary px-2" @click="handleOpenHelp('DEPT')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <td></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 🅱️ 데이터 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
          <span class="fw-bold small text-dark"><i class="bi bi-table me-1"></i> 개인별 월간 실적 대비표</span>
          <div class="d-flex gap-2 align-items-center">
            <span class="small text-muted">전체선택</span>
            <input type="checkbox" v-model="allSelected" @change="toggleAllSelection" class="form-check-input" />
          </div>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="gridElement" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>
    </div>
  </div>

  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { useCommonHelp } from '@/composables/useCommonHelp'

const authStore = useAuthStore()
const router = useRouter()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps, openHelp } = useCommonHelp()

// 1. 상태 관리
const searchData = reactive({
  yyyy: new Date().getFullYear().toString(),
  deptcd: authStore.deptcd,
  deptnm: authStore.deptnm
})

const gridElement = ref<HTMLElement | null>(null)
const grid = ref<Tabulator | null>(null)
const allSelected = ref(false)

// 2. 그리드 초기화
const initGrid = () => {
  if (!gridElement.value) return
  grid.value = new Tabulator(gridElement.value, {
    layout: "fitColumns",
    height: "100%",
    placeholder: "조회된 데이터가 없습니다.",
    selectable: true,
    columnDefaults: { headerSort: false, headerHozAlign: 'center', vertAlign: "middle" },
    columns: [
      {
        title: "선택", width: 40, hozAlign: "center",
        formatter: "rowSelection", titleFormatter: "rowSelection",
        headerSort: false, cellClick: (e, cell) => { cell.getRow().toggleSelect(); }
      },
      {
        title: "영업사원", field: "usernm", widthGrow: 1.5, minWidth: 120, cssClass: "fw-bold",
        formatter: (cell) => {
            const data = cell.getData();
            if (data.TYPE === '계획' && !data.is_total) {
                return `<span class="text-primary cursor-pointer text-decoration-underline">${cell.getValue()}</span>`;
            }
            return data.TYPE === '계획' ? cell.getValue() : '';
        },
        cellClick: (e, cell) => {
            const data = cell.getData();
            if (data.TYPE === '계획' && !data.is_total && data.userid) {
                router.push({
                    name: '품목별판매계획대실적',
                    query: { yyyy: searchData.yyyy, deptcd: searchData.deptcd, userid: data.userid }
                });
            }
        }
      },
      { title: "구분", field: "TYPE", width: 70, hozAlign: "center" },
      {
        title: "합계", field: "total", width: 100, hozAlign: "right",
        formatter: (cell) => {
            const data = cell.getData();
            return data.TYPE === '달성율' ? Number(cell.getValue() || 0).toFixed(2) + '%' : formatNumber(cell.getValue());
        }
      },
      ...Array.from({ length: 12 }, (_, i) => {
        const month = String(i + 1).padStart(2, '0')
        return {
          title: `${month}월`,
          field: `mm${month}`,
          width: 85,
          hozAlign: "right",
          formatter: (cell: any) => {
            const data = cell.getData();
            const val = cell.getValue();
            return data.TYPE === '달성율' ? (Number(val) || 0).toFixed(2) + '%' : formatNumber(val);
          }
        }
      })
    ],
    rowFormatter: (row) => {
        const data = row.getData();
        if (data.TYPE === '달성율') row.getElement().style.backgroundColor = "#f9f6e7";
        if (data.is_total) {
            row.getElement().style.backgroundColor = "#dfd9bd";
            row.getElement().style.fontWeight = "bold";
        }
    }
  })
}

const toggleAllSelection = () => {
  if (!grid.value) return
  if (allSelected.value) {
    grid.value.selectRow();
  } else {
    grid.value.deselectRow();
  }
}

// 3. 기능 구현
async function search() {
  if (!searchData.deptcd) return vAlertError('영업부서를 선택하세요.')
  try {
    const res = await api.post('/hspl/HSPL_220S_STR', {
      actkind: 'S0', cmpycd: authStore.cmpycd, yyyy: searchData.yyyy, deptcd: searchData.deptcd
    })

    if (grid.value) {
      const displayData: any[] = []
      let pTotals = Array(13).fill(0); let sTotals = Array(13).fill(0)

      res.data.forEach((item: any) => {
          const plans = Array.from({ length: 12 }, (_, i) => Number(item[`PL${String(i + 1).padStart(2, '0')}`]) || 0)
          const sales = Array.from({ length: 12 }, (_, i) => Number(item[`SL${String(i + 1).padStart(2, '0')}`]) || 0)
          const planSum = plans.reduce((a, b) => a + b, 0); const saleSum = sales.reduce((a, b) => a + b, 0)

          const pRow: any = { usernm: item.usernm, userid: item.userid, TYPE: '계획', total: planSum }
          plans.forEach((v, idx) => {
            pRow[`mm${String(idx+1).padStart(2, '0')}`] = v;
            pTotals[idx+1] += v
          });
          pTotals[0] += planSum;
          displayData.push(pRow)

          const sRow: any = { usernm: item.usernm, userid: item.userid, TYPE: '실적', total: saleSum }
          sales.forEach((v, idx) => {
            sRow[`mm${String(idx+1).padStart(2, '0')}`] = v;
            sTotals[idx+1] += v
          });
          sTotals[0] += saleSum;
          displayData.push(sRow)

          const rRow: any = { usernm: item.usernm, userid: item.userid, TYPE: '달성율', total: planSum !== 0 ? (saleSum / planSum * 100) : 0 }
          plans.forEach((p, idx) => {
            const s = sales[idx];
            rRow[`mm${String(idx+1).padStart(2, '0')}`] = p !== 0 ? (s / p * 100) : 0
          });
          displayData.push(rRow)
      })

      if (displayData.length > 0) {
          const tpRow: any = { usernm: '합 계', TYPE: '계획', total: pTotals[0], is_total: true }
          pTotals.slice(1).forEach((v, i) => {
            tpRow[`mm${String(i+1).padStart(2, '0')}`] = v
          });
          displayData.push(tpRow)

          const tsRow: any = { usernm: '', TYPE: '실적', total: sTotals[0], is_total: true }
          sTotals.slice(1).forEach((v, i) => {
            tsRow[`mm${String(i+1).padStart(2, '0')}`] = v
          });
          displayData.push(tsRow)

          const trRow: any = { usernm: '', TYPE: '달성율', total: pTotals[0] !== 0 ? (sTotals[0] / pTotals[0] * 100) : 0, is_total: true }
          pTotals.slice(1).forEach((p, i) => {
            const s = sTotals[i+1];
            trRow[`mm${String(i+1).padStart(2, '0')}`] = p !== 0 ? (s / p * 100) : 0
          });
          displayData.push(trRow)
      }
      grid.value.setData(displayData)
      vAlert('조회되었습니다.')
    }
  } catch (e) { vAlertError('조회 실패') }
}

function handleOpenHelp(type: string) {
  if (type === 'DEPT') {
    openHelp('DEPT', (data: any) => {
      searchData.deptcd = data.deptcd;
      searchData.deptnm = data.deptnm;
    });
  }
}

function initialize() {
  resetForm(searchData)
  Object.assign(searchData, {
    yyyy: new Date().getFullYear().toString(),
    deptcd: authStore.deptcd, deptnm: authStore.deptnm
  })
  grid?.clearData()
  allSelected.value = false
}

function print(type: string) { vAlert(`${type} 기능은 준비 중입니다.`) }
const formatNumber = (val: any) => new Intl.NumberFormat().format(Number(val) || 0)

onMounted(() => { nextTick(() => initGrid()) })
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
:deep(.tabulator-row:hover) { background-color: #f0f7ff !important; cursor: pointer; }
:deep(.erp-table-dense th.required::after) { content: " *"; color: red; }
</style>
