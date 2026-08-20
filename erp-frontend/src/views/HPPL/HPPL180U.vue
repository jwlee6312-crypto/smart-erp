<!--
	=============================================================
	프로그램명	: 월간생산계획조정 (HPPL180U)
	작성일자	: 2025.03.31 (AI 가독성/기능 보완)
	설명        : Capa 현황 기반의 직관적 생산 계획 조정 (가독성 강화 버전)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom bg-white shadow-sm">
      <div class="fw-bold ps-2 text-dark d-flex align-items-center" style="font-size: 15px;">
        <i class="bi bi-calendar-check-fill me-2 text-primary" style="font-size: 20px;"></i>
        생산계획 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">월간생산계획조정 (HPPL180U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchData">조회</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light">

      <!-- [상단] 조회 필터 -->
      <div class="card border-0 shadow-sm flex-shrink-0">
        <div class="card-body p-2 px-3 bg-white border-top border-3 border-secondary rounded-top">
          <div class="row g-3 align-items-center">
            <div class="col-auto">
              <div class="btn-group shadow-sm">
                <button class="btn btn-outline-secondary px-3" @click="changeMonth(-1)"><i class="bi bi-chevron-left"></i></button>
                <span class="btn btn-white fw-bolder px-4" style="min-width: 140px; font-size: 16px;">{{ currentYearMonth }}</span>
                <button class="btn btn-outline-secondary px-3" @click="changeMonth(1)"><i class="bi bi-chevron-right"></i></button>
              </div>
            </div>
            <div class="col-auto">
              <div class="d-flex align-items-center bg-light border rounded-pill px-3 py-1">
                <label class="small fw-bold me-2 text-primary text-nowrap"><i class="bi bi-funnel-fill me-1"></i>생산라인</label>
                <select v-model="searchForm.linecd" class="form-select form-select-sm border-0 bg-transparent fw-bold" style="width: 200px;" @change="fetchData">
                  <option value="">전체 생산 라인</option>
                  <option v-for="opt in lineData" :key="opt.linecd" :value="opt.linecd">{{ opt.linenm }}</option>
                </select>
              </div>
            </div>
            <div class="col-auto ms-auto d-flex gap-3">
                <div class="status-legend"><span class="dot bg-success"></span> 여유 (80%미만)</div>
                <div class="status-legend"><span class="dot bg-warning"></span> 주의 (80~100%)</div>
                <div class="status-legend"><span class="dot bg-danger"></span> 초과 (100%초과)</div>
            </div>
          </div>
        </div>
      </div>

      <!-- [하단] 6:4 분할 레이아웃 -->
      <div class="d-flex gap-2 flex-grow-1 overflow-hidden">

        <!-- 🅰️ 좌측: Capa 현황 캘린더 (50%) -->
        <div class="card border-0 shadow-sm overflow-hidden d-flex flex-column border-top border-4 border-primary" style="flex: 1;">
          <div class="card-header bg-white py-2 px-3 border-bottom d-flex align-items-center justify-content-between">
            <span class="fw-bold text-dark"><i class="bi bi-calendar3 me-2 text-primary"></i>일자별 Capa 현황판</span>
            <span class="text-primary small fw-bold anim-blink"><i class="bi bi-hand-index-thumb me-1"></i>이동할 항목을 날짜 칸에 떨어뜨리세요.</span>
          </div>
          <div class="card-body p-0 bg-white d-flex flex-column">
            <!-- 요일 표시 -->
            <div class="calendar-header-row d-grid bg-light border-bottom">
                <div v-for="d in ['일','월','화','수','목','금','토']" :key="d" class="cal-header-cell" :class="{'sunday': d==='일', 'saturday': d==='토'}">{{ d }}</div>
            </div>
            <!-- 날짜 박스 -->
            <div class="calendar-body-grid d-grid flex-grow-1 overflow-auto">
              <div v-for="day in summaryData" :key="day.yymmdd"
                   class="capa-day-box"
                   :class="getDayBoxClass(day)"
                   @click="selectDateAction(day.yymmdd)"
                   @drop="onDrop($event, day.yymmdd)"
                   @dragover.prevent="onDragOver($event)"
                   @dragleave="onDragLeave($event)">

                <div class="d-flex justify-content-between align-items-center mb-1">
                    <span class="day-num">{{ parseInt(day.yymmdd.substring(6)) }}</span>
                    <span v-if="day.rate > 0" class="rate-badge-lg" :class="getRateBadgeClass(day.rate)">{{ day.rate }}%</span>
                </div>

                <div class="day-content">
                  <div class="qty-row">
                    <span class="q-label">계획량</span>
                    <span class="q-val text-dark">{{ day.plan_qty.toLocaleString() }}</span>
                  </div>
                  <div class="qty-row">
                    <span class="q-label">Capa</span>
                    <span class="q-val text-muted">{{ day.capa_qty.toLocaleString() }}</span>
                  </div>
                  <div class="progress mt-2" style="height: 6px; background-color: #eee;">
                    <div class="progress-bar progress-bar-striped progress-bar-animated" :class="getProgressClass(day.rate)" :style="{width: Math.min(day.rate, 100) + '%' }"></div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 🅱️ 우측: 상세 내역 및 계산 근거 (50%) -->
        <div class="card border-0 shadow-sm overflow-hidden d-flex flex-column border-top border-4 border-success" style="flex: 1;">
          <!-- [상세 계산 근거 패널] -->
          <div class="card-header bg-white py-2 px-3 border-bottom d-flex align-items-center gap-2">
              <i class="bi bi-calculator-fill text-success fs-5"></i>
              <span class="fw-bold text-dark">계산 근거 및 상세 내역</span>
              <span v-if="selectedDate" class="badge bg-dark ms-auto px-3 py-2">{{ formatDateDisplay(selectedDate) }}</span>
          </div>

          <div v-if="selectedDaySummary" class="bg-success-subtle p-2 border-bottom">
              <div class="row text-center g-2">
                  <div class="col-4 border-end border-success-subtle">
                      <div class="small text-muted" style="font-size: 11px;">능력(A)</div>
                      <div class="fw-bold">{{ selectedDaySummary.capa_qty.toLocaleString() }}</div>
                  </div>
                  <div class="col-4 border-end border-success-subtle">
                      <div class="small text-muted" style="font-size: 11px;">계획(B)</div>
                      <div class="fw-bold text-primary">{{ selectedDaySummary.plan_qty.toLocaleString() }}</div>
                  </div>
                  <div class="col-4">
                      <div class="small text-muted" style="font-size: 11px;">잔여(A-B)</div>
                      <div class="fw-bold" :class="selectedDaySummary.capa_qty - selectedDaySummary.plan_qty < 0 ? 'text-danger' : 'text-success'">
                          {{ (selectedDaySummary.capa_qty - selectedDaySummary.plan_qty).toLocaleString() }}
                      </div>
                  </div>
              </div>
              <div class="mt-1 py-1 px-2 bg-white bg-opacity-50 border rounded text-center" style="font-size: 11px;">
                  <span class="fw-bold text-success">생산능력:</span> (가동시간×60 / 개당 소요시간(분)) × 가동율 × 양품율
              </div>
          </div>

          <!-- 상세 리스트 그리드 -->
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="gridElement" class="tabulator-instance flex-grow-1"></div>
          </div>

          <div class="card-footer bg-light border-top p-2 text-center">
              <span class="text-muted small"><i class="bi bi-info-circle me-1"></i>행 왼쪽 핸들을 잡고 드래그하여 날짜를 이동하세요.</span>
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
import { fetchLineData, type SelectPdLineData } from '@/composables/useFetchSelectData'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { getDate } from '@/composables/useDate'
import AppAlert from '@/components/AppAlert.vue'

const authStore = useAuthStore()
const { today } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

// 1. 상태 관리
const viewDate = ref(new Date())
const searchForm = reactive({ linecd: '' })
const lineData = ref<SelectPdLineData[]>([])
const summaryData = ref<any[]>([])
const selectedDate = ref('')
const gridElement = ref<HTMLElement | null>(null)
let grid: Tabulator | null = null

const currentYearMonth = computed(() => `${viewDate.value.getFullYear()}년 ${viewDate.value.getMonth() + 1}월`)
const selectedDaySummary = computed(() => summaryData.value.find(d => d.yymmdd === selectedDate.value))

// 2. 초기화 및 조회
const initialize = () => { viewDate.value = new Date(); searchForm.linecd = ''; fetchData(); }
const changeMonth = (val: number) => { viewDate.value = new Date(viewDate.value.getFullYear(), viewDate.value.getMonth() + val, 1); fetchData(); }

const fetchData = async () => {
  try {
    const year = viewDate.value.getFullYear(); const month = viewDate.value.getMonth()
    const firstDay = new Date(year, month, 1); const startOffset = firstDay.getDay()
    const startDate = new Date(year, month, 1 - startOffset)
    const frymd = formatDateStr(startDate); const endDate = new Date(startDate); endDate.setDate(startDate.getDate() + 41); const toymd = formatDateStr(endDate)

    const resSum = await api.get('/product/pdplan/monthly-summary', { params: { cmpycd: authStore.cmpycd, linecd: searchForm.linecd, frymd, toymd } })
    summaryData.value = resSum.data
    if (!selectedDate.value || !selectedDate.value.startsWith(frymd.substring(0, 4))) {
        selectedDate.value = summaryData.value.find(d => d.yymmdd.startsWith(`${year}${String(month+1).padStart(2,'0')}`))?.yymmdd || ''
    }
    fetchDetail()
  } catch (e) { vAlertError('현황 조회 실패') }
}

const fetchDetail = async () => {
  if (!selectedDate.value) { grid?.clearData(); return; }
  try {
    const res = await api.get('/product/pdplan/monthly-list', { params: { cmpycd: authStore.cmpycd, frymd: selectedDate.value, toymd: selectedDate.value, linecd: searchForm.linecd } })
    grid?.setData(res.data)
  } catch (e) { vAlertError('상세 조회 실패') }
}

const selectDateAction = (date: string) => { selectedDate.value = date; fetchDetail(); }

// 4. 드래그 앤 드롭 구현부 (중요)
const onDrop = async (e: DragEvent, targetDate: string) => {
    (e.currentTarget as HTMLElement).classList.remove('drag-over')
    const rowDataStr = e.dataTransfer?.getData('application/json') || e.dataTransfer?.getData('text/plain')
    if (!rowDataStr) return

    const rowData = JSON.parse(rowDataStr)
    if (rowData.yymmdd === targetDate) return

    // 🚀 [해결] 작업지시가 이미 내려간 항목은 이동 불가
    if (Number(rowData.ordqty) > 0) {
        return vAlertError('이미 작업지시가 등록된 항목은 이동할 수 없습니다.\n작업지시 취소 후 이동해 주세요.');
    }

    // 🚀 Capa 초과 사전 체크 로직 추가
    const targetDay = summaryData.value.find(d => d.yymmdd === targetDate)
    let confirmMsg = `[${rowData.itemnm}]\n계획일을 ${formatDateDisplay(targetDate)}로 이동하시겠습니까?`

    if (targetDay && targetDay.capa_qty > 0) {
        const afterPlanQty = targetDay.plan_qty + (rowData.planqty || 0)
        const afterRate = Math.round((afterPlanQty / targetDay.capa_qty) * 100)

        if (afterRate > 100) {
            confirmMsg = `⚠️ [Capa 초과 경고] ⚠️\n\n${formatDateDisplay(targetDate)}로 이동 시\n부하율이 ${afterRate}%로 생산 능력을 초과하게 됩니다.\n\n그래도 이동을 강행하시겠습니까?`
        }
    }

    if (!confirm(confirmMsg)) return

    try {
        const res = await api.post('/product/pdplan/move', { oldyymmdd: rowData.yymmdd, oldser: rowData.ser, newyymmdd: targetDate, updemp: authStore.userid })
        if (res.data[0].result === 'OK' || res.data[0].RESULT === 'OK') {
            vAlert('이동 성공!'); fetchData();
        } else vAlertError(res.data[0].msg || '이동 실패')
    } catch (err) { vAlertError('처리 오류') }
}

const onDragOver = (e: DragEvent) => {
    e.preventDefault();
    if (e.dataTransfer) e.dataTransfer.dropEffect = 'move';
    (e.currentTarget as HTMLElement).classList.add('drag-over');
}
const onDragLeave = (e: DragEvent) => { (e.currentTarget as HTMLElement).classList.remove('drag-over'); }

// 5. 그리드 설정
const initGrid = () => {
  if (!gridElement.value) return
  grid = new Tabulator(gridElement.value, {
    layout: 'fitColumns', height: '100%',
    movableRows: false,
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: 'middle' },
    // 🚀 행 생성 시 네이티브 드래그 데이터 강제 주입
    rowFormatter: (row) => {
        const el = row.getElement();
        el.setAttribute('draggable', 'true');
        el.style.cursor = 'grab';

        el.addEventListener('dragstart', (e: DragEvent) => {
            if (e.dataTransfer) {
                const data = JSON.stringify(row.getData());
                e.dataTransfer.setData('application/json', data);
                e.dataTransfer.setData('text/plain', data);
                e.dataTransfer.effectAllowed = 'move';
                el.style.opacity = '0.5';
            }
        });

        el.addEventListener('dragend', () => {
            el.style.opacity = '1';
        });
    },
    columns: [
      { formatter: 'handle', width: 30, frozen: true, headerSort: false },
      { title: '공정', field: 'prognm', width: 70, hozAlign: 'center', cssClass: 'fw-bold' },
      { title: '제품명', field: 'itemnm', widthGrow: 2, cssClass: 'fw-bold text-dark' },
      { title: '가동율', field: 'gadrate', width: 60, hozAlign: 'right', formatter: (c:any) => c.getValue() + '%' },
      { title: '양품율', field: 'jungrate', width: 60, hozAlign: 'right', formatter: (c:any) => c.getValue() + '%' },
      { title: '소요시간(분)', field: 'capahh', width: 90, hozAlign: 'right', formatter: "money", formatterParams: { precision: 1 } },
      { title: '가동시간(h)', field: 'gadtmdd', width: 80, hozAlign: 'right', formatter: 'money' },
      { title: '능력', field: 'unit_capa', width: 70, hozAlign: 'right', formatter: 'money', cssClass: 'text-success fw-bold' },
      { title: '계획', field: 'planqty', width: 90, hozAlign: 'right',
        formatter: (cell: any) => {
            const val = cell.getValue() || 0;
            const capa = cell.getData().unit_capa || 0;
            const fmtVal = val.toLocaleString();
            if (capa > 0 && val > capa) {
                return `<span class="text-danger fw-black"><i class="bi bi-exclamation-triangle-fill me-1"></i>${fmtVal}</span>`;
            }
            return `<span class="text-primary fw-bolder">${fmtVal}</span>`;
        }
      },
      { title: '거래처', field: 'custnm', width: 100, textTruncate: true, cssClass: 'small' }
    ]
  })
}

// 시각화 유틸리티
const getDayBoxClass = (day: any) => {
    const classes = []
    if (selectedDate.value === day.yymmdd) classes.push('selected')
    if (isHoliday(day.yymmdd)) classes.push('holiday')
    if (day.rate >= 100) classes.push('status-over')
    else if (day.rate >= 80) classes.push('status-warn')
    return classes.join(' ')
}
const getRateBadgeClass = (r: number) => r >= 100 ? 'bg-danger' : r >= 80 ? 'bg-warning text-dark' : 'bg-success'
const getProgressClass = (r: number) => r >= 100 ? 'bg-danger' : r >= 80 ? 'bg-warning' : 'bg-success'
const formatDateStr = (d: Date) => `${d.getFullYear()}${String(d.getMonth() + 1).padStart(2, '0')}${String(d.getDate()).padStart(2, '0')}`
const formatDateDisplay = (v: string) => v ? `${v.substring(0, 4)}년 ${v.substring(4, 6)}월 ${v.substring(6, 8)}일` : ''
const isHoliday = (v: string) => {
    const d = new Date(parseInt(v.substring(0,4)), parseInt(v.substring(4,6))-1, parseInt(v.substring(6,8)))
    return d.getDay() === 0 || d.getDay() === 6
}

onMounted(async () => { lineData.value = await fetchLineData(); nextTick(initGrid); fetchData() })
</script>

<style scoped>
/* 캘린더 레이아웃 */
.calendar-header-row { grid-template-columns: repeat(7, 1fr); border-top: 1px solid #dee2e6; }
.cal-header-cell { padding: 10px; text-align: center; font-weight: 800; font-size: 14px; color: #495057; border-right: 1px solid #dee2e6; }
.cal-header-cell.sunday { color: #e03131; background-color: #fff5f5; }
.cal-header-cell.saturday { color: #1971c2; background-color: #e7f5ff; }

.calendar-body-grid { grid-template-columns: repeat(7, 1fr); grid-template-rows: repeat(6, 1fr); min-height: 0; }

/* 날짜 박스 스타일링 */
.capa-day-box {
    border-right: 1px solid #dee2e6; border-bottom: 1px solid #dee2e6;
    padding: 10px; background-color: #fff; cursor: pointer;
    display: flex; flex-direction: column; min-height: 110px; transition: all 0.15s ease;
}
.capa-day-box:hover { background-color: #f1f3f5; transform: scale(1.02); z-index: 5; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
.capa-day-box.selected { background-color: #f0f7ff; border: 2px solid #228be6 !important; z-index: 4; }
.capa-day-box.drag-over { background-color: #d0ebff !important; border: 2px dashed #228be6 !important; transform: scale(1.05); }

/* 부하 상태별 색상 */
.status-over { background-color: #fff5f5; }
.status-warn { background-color: #fff9db; }

.day-num { font-weight: 900; font-size: 18px; color: #adb5bd; }
.holiday .day-num { color: #fa5252; }
.selected .day-num { color: #228be6; }

.rate-badge-lg { font-size: 12px; font-weight: 900; padding: 2px 8px; border-radius: 20px; color: #fff; }

/* 수치 정보 가독성 */
.day-content { margin-top: auto; }
.qty-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 2px; }
.q-label { font-size: 11px; font-weight: bold; color: #868e96; }
.q-val { font-size: 13px; font-weight: 800; }

/* 범례 및 기타 */
.status-legend { font-size: 12px; font-weight: bold; color: #495057; display: flex; align-items: center; gap: 5px; }
.dot { width: 10px; height: 10px; border-radius: 50%; display: inline-block; }
.anim-blink { animation: blink 1.5s infinite; }
@keyframes blink { 0% { opacity: 1; } 50% { opacity: 0.5; } 100% { opacity: 1; } }

.tabulator-instance { width: 100% !important; background-color: #fff; border: none !important; }
.btn-white { background-color: white; border: 1px solid #dee2e6; }
</style>
