<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="hppl110s-wrapper d-flex flex-column h-100 bg-white p-0">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-file-earmark-bar-graph me-2 text-primary" style="font-size: 18px;"></i>
        생산정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        <span class="text-primary fw-bolder">자재소요량현황 (HPPL110S)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchList">조회</button>
        <button class="btn-erp btn-excel" @click="exportExcel">엑셀</button>
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
                <th class="required">기준일자</th>
                <td>
                  <div class="d-flex align-items-center gap-2">
                    <input v-model="uiymd" type="date" class="form-control form-control-sm" style="width: 150px;" @change="fetchList" />
                    <span class="small text-muted">현재</span>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 🅲 리포트 영역 (커스텀 테이블) -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column">
        <div class="card-header bg-light py-1 px-3 border-bottom d-flex justify-content-between align-items-center">
          <span class="fw-bold small text-dark"><i class="bi bi-table me-1"></i> 자재 수불 상세 (4줄 보기)</span>
          <div class="small text-muted text-danger fw-bold">※ 우측으로 스크롤하여 1일부터 말일까지 확인하세요.</div>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-auto scrollbar-custom">
          <table class="report-table">
            <thead>
              <tr class="header-main">
                <th rowspan="2" class="frozen-col first-col" style="width: 180px;">품목명</th>
                <th rowspan="2" class="frozen-col" style="width: 120px;">규격</th>
                <th rowspan="2" class="frozen-col" style="width: 60px;">단위</th>
                <th rowspan="2" class="frozen-col last-frozen" style="width: 60px;">구분</th>
                <th v-for="h in daysHeaders" :key="h.DD" class="date-header">
                  {{ h.DD }}일
                </th>
              </tr>
              <tr class="header-sub">
                <th v-for="h in daysHeaders" :key="h.DD" :class="['day-header', {'text-danger': h.WW==='일', 'text-primary': h.WW==='토'}]">
                  ({{ h.WW }})
                </th>
              </tr>
            </thead>
            <tbody>
              <template v-if="reportData.length === 0">
                <tr>
                  <td :colspan="daysHeaders.length + 4" class="text-center py-5 text-muted">
                    조회된 데이터가 없습니다.
                  </td>
                </tr>
              </template>
              <template v-for="item in reportData" :key="item.itemcd">
                <!-- 1. 기초 -->
                <tr class="row-begin">
                  <td rowspan="4" class="frozen-col first-col text-start fw-bold">{{ item.itemnm }}</td>
                  <td rowspan="4" class="frozen-col text-center small">{{ item.itsize }}</td>
                  <td rowspan="4" class="frozen-col text-center small">{{ item.unit }}</td>
                  <td class="frozen-col last-frozen text-center bg-light-blue small">기초</td>
                  <td v-for="(val, idx) in item.dailyData.Bsqty" :key="idx" class="text-end px-2">
                    {{ formatNumber(val, item.qtypnt) }}
                  </td>
                </tr>
                <!-- 2. 입고 -->
                <tr class="row-in">
                  <td class="frozen-col last-frozen text-center bg-light-green small">입고</td>
                  <td v-for="(val, idx) in item.dailyData.inqty" :key="idx" class="text-end px-2 text-primary">
                    {{ val !== 0 ? formatNumber(val, item.qtypnt) : '' }}
                  </td>
                </tr>
                <!-- 3. 출고 -->
                <tr class="row-out">
                  <td class="frozen-col last-frozen text-center bg-light-red small">출고</td>
                  <td v-for="(val, idx) in item.dailyData.outqty" :key="idx" class="text-end px-2 text-danger">
                    {{ val !== 0 ? formatNumber(val, item.qtypnt) : '' }}
                  </td>
                </tr>
                <!-- 4. 재고 -->
                <tr class="row-stock">
                  <td class="frozen-col last-frozen text-center bg-light-yellow small fw-bold">재고</td>
                  <td v-for="(val, idx) in item.dailyData.stkqty" :key="idx" class="text-end px-2 fw-bold bg-stock-cell">
                    {{ formatNumber(val, item.qtypnt) }}
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, computed } from 'vue'
import AppAlert from '@/components/AppAlert.vue'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import * as XLSX from 'xlsx'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

const now = new Date()
const initymd = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}`

// 1. 상태 관리
const searchData = reactive({
  ymd: initymd
})

const uiymd = computed({
  get: () => formatDateString(searchData.ymd, '-'),
  set: (v) => { if (v) searchData.ymd = v.replace(/-/g, '') }
})

const daysHeaders = ref<any[]>([])
const reportData = ref<any[]>([])

// 2. 데이터 조회 및 가공 로직
const fetchList = async () => {
  try {
    // 1) 헤더 정보(요일) 가져오기
    const headerRes = await api.post('/hppl/HPPL_100U_STR', {
      actkind: 'S1', cmpycd: authStore.cmpycd, yymmDD: searchData.ymd
    })
    daysHeaders.value = headerRes.data
    const lastDayCount = daysHeaders.value.length

    // 2) 실제 수불 데이터 가져오기
    const dataRes = await api.post('/hppl/HPPL_110S_STR', {
      cmpycd: authStore.cmpycd, iyymmDD: searchData.ymd
    })

    // 3) 데이터 그룹화 및 재고 계산 (ASP 로직 이식)
    const rawList = dataRes.data
    const groupedItems: any[] = []
    const itemMap = new Map()

    rawList.forEach((row: any) => {
      const id = row.itemcd
      if (!itemMap.has(id)) {
        const itemObj = {
          itemcd: id,
          itemnm: String(row.itemnm || '').trim(),
          itsize: String(row.itsize || '').trim(),
          unit: String(row.unit || '').trim(),
          qtypnt: Number(row.qtypnt || 0),
          dailyData: {
            Bsqty: Array(lastDayCount).fill(0),
            inqty: Array(lastDayCount).fill(0),
            outqty: Array(lastDayCount).fill(0),
            stkqty: Array(lastDayCount).fill(0)
          }
        }
        itemMap.set(id, itemObj)
        groupedItems.push(itemObj)
      }

      const item = itemMap.get(id)
      const gbn = String(row.gbn) // 1:기초, 2:입고, 3:출고

      for (let i = 0; i < lastDayCount; i++) {
        const val = Number(row[i + 5] || row[String(i + 1)] || 0)
        if (gbn === '1') item.dailyData.Bsqty[i] = val
        else if (gbn === '2') item.dailyData.inqty[i] = val
        else if (gbn === '3') item.dailyData.outqty[i] = val
      }
    })

    // 4) 수불 로직 계산: 재고 = 기초 + 입고 - 출고 (익일 기초 = 전일 재고)
    groupedItems.forEach(item => {
      let runningStock = item.dailyData.Bsqty[0]
      for (let i = 0; i < lastDayCount; i++) {
        item.dailyData.Bsqty[i] = runningStock
        item.dailyData.stkqty[i] = runningStock + item.dailyData.inqty[i] - item.dailyData.outqty[i]
        runningStock = item.dailyData.stkqty[i]
      }
    })

    reportData.value = groupedItems
    vAlert('조회되었습니다.')
  } catch (e) {
    vAlertError('조회 처리 중 오류 발생')
  }
}

const initialize = () => {
  searchData.ymd = initymd
  reportData.value = []
  fetchList()
}

const exportExcel = () => {
  if (reportData.value.length === 0) return vAlertError('내보낼 데이터가 없습니다.')

  const wsData: any[] = []
  // 헤더 생성
  const headerRow = ["품목명", "규격", "단위", "구분"]
  daysHeaders.value.forEach(h => headerRow.push(`${h.DD}일(${h.WW})`))
  wsData.push(headerRow)

  // 데이터 행 생성
  reportData.value.forEach(item => {
    const rowTypes = [
      { label: '기초', data: item.dailyData.Bsqty },
      { label: '입고', data: item.dailyData.inqty },
      { label: '출고', data: item.dailyData.outqty },
      { label: '재고', data: item.dailyData.stkqty }
    ]
    rowTypes.forEach((t, idx) => {
      const row = [
        idx === 0 ? item.itemnm : '',
        idx === 0 ? item.itsize : '',
        idx === 0 ? item.unit : '',
        t.label
      ]
      t.data.forEach(val => row.push(val))
      wsData.push(row)
    })
  })

  const ws = XLSX.utils.aoa_to_sheet(wsData)
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, "자재소요량")
  XLSX.writeFile(wb, `자재소요량현황_${searchData.ymd}.xlsx`)
}

const formatDateString = (v: any, sep: string) => v && v.length === 8 ? `${v.substring(0, 4)}${sep}${v.substring(4, 6)}${sep}${v.substring(6, 8)}` : v
const formatNumber = (val: any, precision: number = 0) => new Intl.NumberFormat('ko-KR', { minimumFractionDigits: precision, maximumFractionDigits: precision }).format(Number(val) || 0)

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.hppl110s-wrapper { height: 100%; overflow: hidden; font-family: 'Pretendard', sans-serif; }
.btn-erp { padding: 4px 16px; border-radius: 4px; font-size: 12.5px; font-weight: 700; cursor: pointer; transition: all 0.2s; }
.btn-init { background-color: #fff !important; color: #6c757d !important; border: 1px solid #6c757d !important; }
.btn-search { background-color: #2d3748 !important; color: #fff !important; border: none !important; }
.btn-excel { background-color: #198754 !important; color: #fff !important; border: none !important; }

.erp-table-full { width: 100%; border-collapse: collapse; table-layout: fixed; border: 1px solid #dee2e6; }
.erp-table-full th { width: 100px; background-color: #f8f9fa; border: 1px solid #dee2e6; text-align: center; font-weight: 700; font-size: 12px; padding: 8px !important; color: #495057; }
.erp-table-full td { border: 1px solid #dee2e6; padding: 4px 8px !important; background-color: #fff; vertical-align: middle; }
.required::after { content: ' *'; color: #dc3545; }

/* 리포트 테이블 스타일 */
.report-table { border-collapse: separate; border-spacing: 0; width: max-content; min-width: 100%; border: 1px solid #ccc; font-size: 12.5px; }
.report-table th, .report-table td { border: 1px solid #dee2e6; padding: 4px; white-space: nowrap; height: 32px; }

/* 고정 헤더 */
.report-table thead { position: sticky; top: 0; z-index: 10; }
.header-main th { background-color: #dfd9bd; color: #333; font-weight: 800; text-align: center; }
.header-sub th { background-color: #f0ede0; font-size: 11px; text-align: center; }

/* 좌측 컬럼 고정 (Frozen) */
.frozen-col { position: sticky; left: 0; z-index: 5; background-color: #fff; }
.first-col { left: 0; }
/* 품목명(0) 규격(180) 단위(300) 구분(360) 누적 좌표 */
.report-table th:nth-child(2), .report-table td:nth-child(2) { left: 180px; }
.report-table th:nth-child(3), .report-table td:nth-child(3) { left: 300px; }
.report-table th:nth-child(4), .report-table td:nth-child(4) { left: 360px; }
.last-frozen { border-right: 2px solid #bbb !important; }

/* 구분별 배경색 */
.bg-light-blue { background-color: #e3f2fd !important; }
.bg-light-green { background-color: #e8f5e9 !important; }
.bg-light-red { background-color: #ffebee !important; }
.bg-light-yellow { background-color: #fff9c4 !important; }
.bg-stock-cell { background-color: #fdfae5 !important; }

.scrollbar-custom::-webkit-scrollbar { height: 12px; width: 8px; }
.scrollbar-custom::-webkit-scrollbar-thumb { background: #ccc; border-radius: 10px; }
.scrollbar-custom::-webkit-scrollbar-track { background: #f1f1f1; }
</style>
