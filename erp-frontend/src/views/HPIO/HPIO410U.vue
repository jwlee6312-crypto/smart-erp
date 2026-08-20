<!--
	=============================================================
	프로그램명	: 제품입고취소(자가생산) (HPIO410U)
	작성일자	: 2025.02.24
	설명        : 생산 공정별 이미 완료된 입고 건의 취소 처리 관리 (HPIO210U 표준 패턴 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-box-arrow-left me-2 text-danger" style="font-size: 18px;"></i>
        생산정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">제품입고취소(자가생산) (HPIO410U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchProcesses">조회</button>
        <button class="btn-erp btn-save" style="background-color: #ef4444 !important;" @click="saveCancellation" :disabled="!selectedProg.progcd">입고취소</button>
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
                <col style="width: 10%" /><col style="width: 20%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light required">생산라인</th>
                <td>
                  <select v-model="searchData.linecd" class="form-select form-select-sm" @change="onLineChange">
                    <option value="">라인 선택</option>
                    <option v-for="opt in lineOptions" :key="opt.linecd" :value="opt.linecd">
                      [{{ opt.linecd }}] {{ opt.linenm }}
                    </option>
                  </select>
                </td>
                <th class="text-center bg-light required">입고창고</th>
                <td>
                  <select v-model="searchData.whcd" class="form-select form-select-sm">
                    <option v-for="opt in whOptions" :key="opt.whcd" :value="opt.whcd">{{ opt.whnm }}</option>
                  </select>
                </td>
                <th class="text-center bg-light required">입고일자</th>
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

      <!-- [하단] 투-그리드 레이아웃 영역 -->
      <div class="d-flex gap-2 flex-grow-1 overflow-hidden" style="min-height: 0;">

        <!-- ⬅️ 좌측: 공정 목록 (grid1) -->
        <div class="card border shadow-sm d-flex flex-column overflow-hidden grid-container-left" style="width: 250px; min-width: 250px;">
          <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark text-center">공정 목록</div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="tableRef1" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>

        <!-- ➡️ 우측: 취소 대상 그리드 (grid2) -->
        <div class="flex-grow-1 d-flex flex-column gap-2 overflow-hidden">
          <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column grid-container-right">
            <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
              <span class="fw-bold small text-dark">
                <i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>입고 취소 대상 내역
                <span v-if="selectedProg.prognm" class="badge bg-danger-subtle text-danger border border-danger-subtle ms-2">{{ selectedProg.prognm }}</span>
              </span>
              <div class="small text-muted">※ 취소할 항목을 선택 후 '입고취소'를 클릭하세요.</div>
            </div>
            <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
              <div ref="tableRef2" class="tabulator-instance flex-grow-1"></div>
            </div>
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
const initymd = today.replace(/-/g, '')
const initfromdt = firstDay.replace(/-/g, '')

const searchData = reactive({
  linecd: '010',
  whcd: '200',
  fromdt: initfromdt,
  todt: initymd
})

const lineOptions = ref<any[]>([]); const whOptions = ref<any[]>([])
const selectedProg = reactive({ progcd: '', prognm: '' })
const closingInfo = reactive({ clsymd: '', sclsym: '', pclsym: '' })

// 포맷팅 헬퍼 (ui 접두어 제거)
const fromdt = computed({ get: () => formatDate(searchData.fromdt), set: (v) => { if(v) searchData.fromdt = v.replace(/-/g, '') } })
const todt = computed({ get: () => formatDate(searchData.todt), set: (v) => { if(v) searchData.todt = v.replace(/-/g, '') } })

const tableRef1 = ref<HTMLDivElement | null>(null)
const tableRef2 = ref<HTMLDivElement | null>(null)
let grid1: Tabulator | null = null
let grid2: Tabulator | null = null

// [2] 그리드 초기화
const initGrids = () => {
  // Left: 공정 목록
  grid1 = new Tabulator(tableRef1.value!, {
    layout: "fitColumns", height: "100%", placeholder: "라인 선택", selectable: 1,
    columns: [
      { title: "No", formatter: "rownum", width: 40, hozAlign: "center", headerSort: false },
      { title: "공정명", field: "cdnm", hozAlign: "left", headerSort: false },
      { title: "코드", field: "code", hozAlign: "center", width: 80, cssClass: "fw-bold text-primary", headerSort: false }
    ],
  });
  grid1.on("rowClick", (e, row) => onProcessSelect(row.getData()));

  // Right: 취소 대상
  grid2 = new Tabulator(tableRef2.value!, {
    layout: "fitColumns", height: "100%", placeholder: "공정을 선택하세요.", selectable: true,
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "선택", formatter: "rowSelection", titleFormatter: "rowSelection", width: 40, hozAlign: "center", headerSort: false },
      { title: "입고일자", field: "inymd", width: 120, hozAlign: "center", formatter: (c) => formatDate(c.getValue()) },
      { title: "품목명", field: "itemnm", minWidth: 200, widthGrow: 1, cssClass: "fw-bold" },
      { title: "규격", field: "itsize", width: 180 },
      { title: "단위", field: "unit", width: 70, hozAlign: "center" },
      { title: "생산량", field: "prdqty", width: 120, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "입고량", field: "inqty", width: 120, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "text-primary fw-bold" },
      { title: "작업처", field: "work_info", width: 200, hozAlign: "left", formatter: (cell) => {
          const d = cell.getData(); return d.prodcd === '100' ? d.worknm : d.custnm;
      }}
    ]
  });
}

// [3] 비즈니스 로직
const fetchLineOptions = async () => {
  try {
    const res = await api.get('/hp00/HP00_000S_STR', { params: { gubun: 'L0', cmpycd: authStore.cmpycd, gbncd: 'Y', code: '' } })
    lineOptions.value = res.data.map((i: any) => ({ linecd: i.code || i.code, linenm: i.cdnm }));
  } catch (e) {}
}

const fetchWhOptions = async () => {
  try {
    const res = await api.post('/hs00/HS00_000S_STR', { gubun: 'W0', cmpycd: authStore.cmpycd, gbncd: '', code: '', codenm: '', etcval: '' })
    whOptions.value = res.data.map((i: any) => ({ whcd: i.whcd, whnm: i.whnm }));
  } catch (e) {}
}

const fetchProcesses = async () => {
  if (!searchData.linecd) { vAlertError('라인을 선택하세요.'); return; }
  try {
    const res = await api.get('/hp00/HP00_000S_STR', { params: { gubun: 'G0', cmpycd: authStore.cmpycd, gbncd: searchData.linecd, code: '' } })
    grid1?.setData(res.data)
    grid2?.clearData(); selectedProg.progcd = ''; selectedProg.prognm = '';
  } catch (e) {}
}

const onProcessSelect = (prog: any) => {
  selectedProg.progcd = prog.code; selectedProg.prognm = prog.cdnm
  fetchGridData()
}

const fetchGridData = async () => {
  try {
    const res = await api.post('/hpio/HPIO_410U_STR', {
      actkind: 'S0', cmpycd: authStore.cmpycd, linecd: searchData.linecd, progcd: selectedProg.progcd, whcd: searchData.whcd, fromdt: searchData.fromdt, todt: searchData.todt
    })
    grid2?.setData(res.data)
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

const saveCancellation = async () => {
  const selected = grid2?.getSelectedData() || []
  if (selected.length === 0) return vAlertError('취소할 항목을 선택하세요.')

  if (initymd <= closingInfo.clsymd) return vAlertError('회계 마감된 데이터는 취소할 수 없습니다.')
  if (initymd.substring(0, 6) <= closingInfo.sclsym) return vAlertError('영업 마감된 월의 데이터는 취소할 수 없습니다.')

  if (!confirm('선택한 입고 내역을 취소 처리하시겠습니까?')) return

  try {
    for (const item of selected) {
      await api.post('/hpio/HPIO_410U_STR', {
        actkind: 'D0', cmpycd: authStore.cmpycd, linecd: searchData.linecd, progcd: selectedProg.progcd, whcd: searchData.whcd,
        proymd: item.proymd, Jinymd: item.inymd, equpcd: '000', PRODCD: item.prodcd, workjo: '000', wkgbn: item.wkgbn,
        itemcd: item.itemcd, custcd: item.custcd || '', iogbn: item.iogbn, ioym: item.ioym, iono: item.iono, iorowno: item.iorowno, userid: authStore.userid
      })
    }
    vAlert('정상적으로 취소되었습니다.'); fetchGridData()
  } catch (e) { vAlertError('취소 처리 중 오류 발생') }
}

const onLineChange = () => fetchProcesses()
const initialize = () => {
  resetForm(searchData);
  Object.assign(searchData, { linecd: '010', whcd: '200', fromdt: initfromdt, todt: initymd });
  grid1?.clearData(); grid2?.clearData();
  selectedProg.progcd = ''; selectedProg.prognm = '';
  fetchProcesses();
}

const formatDate = (v: any) => v && v.length === 8 ? `${v.substring(0, 4)}-${v.substring(4, 6)}-${v.substring(6, 8)}` : v;

onMounted(async () => {
  api.get('/hp00/HP00_000S_STR', { params: { gubun: 'CL', cmpycd: authStore.cmpycd } }).then(r => {
    if (r.data?.length) {
      closingInfo.clsymd = r.data[0].clsymd
      closingInfo.sclsym = r.data[0].sclsym
      closingInfo.pclsym = r.data[0].pclsym
    }
  })
  fetchLineOptions(); fetchWhOptions(); fetchProcesses();
  nextTick(initGrids);
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
.grid-container-left, .grid-container-right { border-bottom: 3px solid #005a9f !important; }
</style>
