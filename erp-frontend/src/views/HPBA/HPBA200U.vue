<!--
	=============================================================
	프로그램명	: 품목별 표준공정도
	작성일자	: 2025.02.24
	작성자	    : AI Assistant
	설명        : 품목별로 적용되는 생산 공정 순서 및 표준 생산 능력 관리 (상하단 폰트 12px 통일 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container">
    <!-- 🚀 1. 상단 액션 바 (표준 규격 및 함수명 통일) -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-diagram-2-fill me-2 text-primary" style="font-size: 18px;"></i>
        기본정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        <span class="text-primary fw-bolder">품목별 표준공정도 (HPBA200U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-save" @click="save" :disabled="!selectedItem.itemcd">저장</button>
        <button class="btn-erp btn-excel" @click="excel">엑셀</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2">
      <!-- 🅰️ 조회 조건 영역 (표준 erp-table-full 및 80px 레이블 규격) -->
      <div class="card border-0 shadow-sm overflow-hidden flex-shrink-0">
        <div class="card-body p-0">
          <table class="erp-table-full">
            <colgroup>
              <col style="width: 80px;" /><col />
              <col style="width: 80px;" /><col />
              <col />
            </colgroup>
            <tbody>
              <tr>
                <th class="required">생산라인</th>
                <td>
                  <select v-model="searchData.linecd" class="form-select" style="width: 150px;" @change="search">
                    <option v-for="opt in lineOptions" :key="opt.linecd" :value="opt.linecd">
                      [{{ opt.linecd }}] {{ opt.linenm }}
                    </option>
                  </select>
                </td>
                <th class="required">재고자산</th>
                <td>
                  <select v-model="searchData.astkind" class="form-select" style="width: 120px;" @change="search">
                    <option value="200">제품</option>
                    <option value="210">반제품</option>
                  </select>
                </td>
                <td class="text-muted small px-3">
                  <i class="bi bi-info-circle me-1 text-primary"></i> 라인과 자산구분을 선택하여 품목별 표준 공정을 구성하십시오.
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 🅱️ 메인 레이아웃 (좌: 품목목록, 우: 공정설정) -->
      <div class="d-flex flex-grow-1 gap-2 overflow-hidden">
        <!-- 좌측: 품목 목록 -->
        <div class="card border-0 shadow-sm d-flex flex-column" style="width: 350px; border-radius: 8px;">
          <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark d-flex align-items-center">
            <i class="bi bi-box-seam me-2 text-primary"></i> 대상 품목 리스트
          </div>
            <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
              <div ref="itemGridElement" class="tabulator-instance flex-grow-1"></div>
            </div>
        </div>

        <!-- 우측: 공정 설정 -->
        <div class="card border-0 shadow-sm flex-grow-1 overflow-hidden d-flex flex-column" style="border-radius: 8px;">
          <div class="card-header bg-white py-1 px-3 border-bottom d-flex justify-content-between align-items-center flex-shrink-0">
            <span class="fw-bold small text-dark">
              <i class="bi bi-gear-wide-connected me-2 text-primary"></i> 표준 공정 설정
              <span v-if="selectedItem.itemnm" class="badge bg-info ms-2 px-3 text-white" style="font-size: 10px;">{{ selectedItem.itemnm }} ({{ selectedItem.itemcd }})</span>
            </span>
            <div class="small text-muted" style="font-size: 11px;">※ 공정별 순서 및 생산 능력을 등록하십시오.</div>
          </div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="procGridElement" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, nextTick } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import AppAlert from '@/components/AppAlert.vue'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

const searchData = reactive({ linecd: '010', astkind: '200' })
const lineOptions = ref<any[]>([])
const selectedItem = reactive({ itemcd: '', itemnm: '', itsize: '', unit: '' })

const itemGridElement = ref<HTMLElement | null>(null); const procGridElement = ref<HTMLElement | null>(null)
let itemGrid: Tabulator | null = null; let procGrid: Tabulator | null = null
const itemCount = ref(0)

const fetchLineOptions = async () => {
  try {
    const res = await api.get('/hp00/HP00_000S_STR', { params: { gubun: 'L0', cmpycd: authStore.cmpycd, gbncd: 'Y' } })
    lineOptions.value = res.data
  } catch (e) {}
}

const initGrids = () => {
  if (itemGridElement.value) {
    itemGrid = new Tabulator(itemGridElement.value, {
      layout: "fitColumns", height: "100%", placeholder: "데이터가 없습니다.", selectable: 1,
      columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
      columns: [
        { title: "코드", field: "itemcd", width: 90, hozAlign: "center", cssClass: "fw-bold text-primary" },
        { title: "품목명", field: "itemnm", minWidth: 150, cssClass: "fw-bold" },
        { title: "단위", field: "unit", width: 60, hozAlign: "center" }
      ],
    })
    itemGrid.on("rowClick", (e, row) => { const d = row.getData(); Object.assign(selectedItem, d); fetchProcesses(d.itemcd) })
  }

  if (procGridElement.value) {
    procGrid = new Tabulator(procGridElement.value, {
      layout: "fitColumns", height: "100%", placeholder: "공정 데이터가 없습니다.", selectable: true,
      rowFormatter: (row) => {
          const d = row.getData();
          // 🚀 미등록 공정은 연한 회색 배경으로 처리하여 시각적 구분
          if (d._state === 'NEW') {
              row.getElement().style.backgroundColor = "#f8f9fa";
              row.getElement().style.color = "#6c757d";
          } else {
              row.getElement().style.backgroundColor = "#ffffff";
              row.getElement().style.color = "#212529";
          }
      },
      columnDefaults: {
        headerHozAlign: 'center',
        headerSort: false,
        vertAlign: "middle",
        cellEdited: (cell) => {
          const d = cell.getData();
          const field = cell.getField();

          // 1. 상태 업데이트 (EXIST -> 수정, NEW -> 입력)
          if (d._state === 'EXIST' && !d._status) {
              cell.getRow().update({ _status: '수정' });
          } else if (d._state === 'NEW' && !d._status) {
              cell.getRow().update({ _status: '입력' });
          }

          // 2. 생산량 관련 필드 수정 시 자동 계산 실행
          if (['capahh', 'gadtmdd', 'gadrate', 'jungrate'].includes(field)) {
            calculateOutput(cell.getRow());
          }
          // 🚀 [추가] 일생산량을 직접 수정할 경우 소요시간을 역산
          if (field === 'pqtydd') {
            reverseCalculateTime(cell.getRow());
          }
        }
      },
      columns: [
        { title: "선택", width: 40, hozAlign: "center", formatter: "rowSelection", titleFormatter: "rowSelection", headerHozAlign: "center" },
        { title: "상태", field: "_status", width: 70, hozAlign: "center", formatter: (c) => {
            const v = c.getValue();
            const state = c.getRow().getData()._state;

            if (v === '입력') return '<span class="badge bg-primary">입력</span>';
            if (v === '수정') return '<span class="badge bg-warning text-dark">수정</span>';
            if (v === '삭제') return '<span class="badge bg-danger">삭제</span>';

            // 🚀 초기 상태 표시
            return state === 'EXIST'
                ? '<span class="badge bg-success bg-opacity-75">등록</span>'
                : '<span class="badge bg-secondary bg-opacity-50">미등록</span>';
        }},
        { title: "공정", field: "progcd", width: 100, hozAlign: "center" },
        { title: "공 정 명", field: "prognm", minWidth: 200, widthGrow: 1, cssClass: "fw-bold text-primary" },
        { title: "순서", field: "dspord", width: 70, hozAlign: "center", editor: "number" },
        { title: "가동율(%)", field: "gadrate", width: 100, hozAlign: "right", editor: "number" },
        { title: "양품율(%)", field: "jungrate", width: 100, hozAlign: "right", editor: "number" },
        { title: "개당 소요시간(분)", field: "capahh", width: 130, hozAlign: "right", editor: "number", formatter: "money", formatterParams: { precision: 1 } },
        { title: "일가동시간(h)", field: "gadtmdd", width: 120, hozAlign: "right", editor: "number" },
        { title: "일생산량", field: "pqtydd", width: 150, hozAlign: "right", editor: "number", formatter: "money", formatterParams: { precision: 0 }, cssClass: "bg-light-yellow fw-bold" },
        { title: "삭제", width: 50, hozAlign: "center", formatter: () => "<i class='bi bi-trash text-danger cursor-pointer'></i>",
          cellClick: (e, cell) => {
            const row = cell.getRow();
            const d = row.getData();
            row.update({ _status: d._status === '삭제' ? '' : '삭제' });
          }
        }
      ],
    })
  }
}

// 🚀 [역산] 일생산량(목표) 기반 소요시간 계산 (소요시간 = 가동시간(분) / 생산량 * 가동율 * 양품율)
const reverseCalculateTime = (row: any) => {
  const d = row.getData();
  const targetQty = Number(d.pqtydd) || 0;
  const workTimeMin = (Number(d.gadtmdd) || 0) * 60;
  const availRate = (Number(d.gadrate) || 0) / 100;
  const qualityRate = (Number(d.jungrate) || 0) / 100;

  if (targetQty > 0 && workTimeMin > 0) {
    const resultMin = (workTimeMin * availRate * qualityRate) / targetQty;
    row.update({ capahh: Math.round(resultMin * 10) / 10 });
  }
}

// 🚀 [순산] 개당 소요시간 기반 생산량 계산
const calculateOutput = (row: any) => {
  const d = row.getData();
  const stdTimeMin = Number(d.capahh) || 0;
  const workTimeMin = (Number(d.gadtmdd) || 0) * 60;
  const availRate = (Number(d.gadrate) || 0) / 100;
  const qualityRate = (Number(d.jungrate) || 0) / 100;

  if (stdTimeMin > 0 && workTimeMin > 0) {
    const result = Math.floor((workTimeMin / stdTimeMin) * availRate * qualityRate);
    row.update({ pqtydd: result });
  }
}

async function search() {
  try {
    const res = await api.post('/hpba/HPBA_200U_STR', {
        actkind: 'S1',
        cmpycd: authStore.cmpycd,
        itemcd: '',
        linecd: searchData.linecd,
        astkind: searchData.astkind,
        progord: 0, capahh: 0,   gadrate: 0,  pqtytt: 0,
        pqtydd: 0,  gadtmdd: 0, jungrate: 0,  stdworkhh: 0
    })
    itemGrid?.setData(res.data); itemCount.value = res.data.length
    Object.assign(selectedItem, { itemcd: '', itemnm: '', itsize: '', unit: '' }); procGrid?.clearData()
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('품목 조회 실패') }
}

async function fetchProcesses(itemcd: string) {
  try {
    const res = await api.post('/hpba/HPBA_200U_STR', {
        actkind: 'S0', cmpycd: authStore.cmpycd, itemcd: itemcd, linecd: searchData.linecd,
        astkind: searchData.astkind, progcd: '', itsize: '', unit: '', progord: 0,
        capahh: 0, gadrate: 0, pqtytt: 0, pqtydd: 0, gadtmdd: 0, jungrate: 0, stdworkhh: 0, updemp: authStore.userid
     })
    const cleanData = (res.data || []).filter((i: any) => i.progcd || i.PROGCD);
    // 🚀 [보강] 대소문자 및 필드명(capahh/stdworkhh) 혼용 대응
    const processed = cleanData.map((i: any) => {
        // 🚀 [핵심] USEYN 필드를 기준으로 실제 등록 여부 판별
        const useYn = i.useyn || i.USEYN || '';
        const isRegistered = useYn === 'Y';

        const rawTime = i.capahh ?? i.CAPAHH ?? i.stdworkhh ?? i.STDWORKHH ?? 0;

        return {
            ...i,
            progcd: i.progcd || i.PROGCD,
            prognm: i.prognm || i.PROGNM,
            dspord: i.dspord ?? i.DSPORD ?? i.progord ?? i.PROGORD ?? 0,
            // 🚀 미등록인 경우 설정값은 보이지 않아야 함 (0으로 강제 초기화)
            capahh: isRegistered ? (rawTime ? Math.round(Number(rawTime) * 60 * 10) / 10 : 0) : 0,
            gadtmdd: isRegistered ? (i.gadtmdd ?? i.GADTMDD ?? 0) : 0,
            gadrate: isRegistered ? (i.gadrate ?? i.GADRATE ?? 0) : 0,
            jungrate: isRegistered ? (i.jungrate ?? i.JUNGRATE ?? 0) : 0,
            pqtydd: isRegistered ? (i.pqtydd ?? i.PQTYDD ?? 0) : 0,
            _state: isRegistered ? 'EXIST' : 'NEW',
            _status: ''
        }
    })
    procGrid?.setData(processed).then(() => {
      procGrid?.getRows().forEach(row => { if (row.getData().useyn === 'Y') row.select(); });
    });
  } catch (e) { vAlertError('공정 정보 조회 실패') }
}

async function save() {
  if (!selectedItem.itemcd) return vAlertError('저장할 품목을 선택하세요.')
  const changes = (procGrid?.getData() || []).filter((r: any) => r._status)

  if (changes.length === 0) return vAlertError('저장할 변경 항목이 없습니다.')

  // 🚀 [해결] 중요값(소요시간, 가동시간, 가동율, 양품율) 0 등록 방지 벨리데이션
  for (const row of changes) {
    if (row._status === '삭제') continue; // 삭제는 제외

    if (!row.capahh || Number(row.capahh) <= 0) return vAlertError(`[${row.prognm}] 공정의 개당 소요시간은 0보다 커야 합니다.`);
    if (!row.gadtmdd || Number(row.gadtmdd) <= 0) return vAlertError(`[${row.prognm}] 공정의 일가동시간은 0보다 커야 합니다.`);
    if (!row.gadrate || Number(row.gadrate) <= 0) return vAlertError(`[${row.prognm}] 공정의 가동율은 0보다 커야 합니다.`);
    if (!row.jungrate || Number(row.jungrate) <= 0) return vAlertError(`[${row.prognm}] 공정의 양품율은 0보다 커야 합니다.`);
  }

  if (!confirm('현재 공정 설정을 저장하시겠습니까?')) return
  try {
    for (const row of changes) {
        const timeH = (row.capahh || 0) / 60;
        await api.post('/hpba/HPBA_200U_STR', {
            actkind: row._status === '삭제' ? 'D0' : 'A0',
            cmpycd: authStore.cmpycd, userid: authStore.userid, itemcd: selectedItem.itemcd,
            linecd: searchData.linecd, astkind: searchData.astkind,
            progcd: row.progcd, itsize: selectedItem.itsize, unit: selectedItem.unit,
            progord: row.dspord || 0,
            capahh: timeH,       // 두 필드 모두 동일한 값으로 저장
            stdworkhh: timeH,    // 두 필드 모두 동일한 값으로 저장
            gadrate: row.gadrate || 0,
            pqtytt: 0,
            pqtydd: row.pqtydd || 0,
            gadtmdd: row.gadtmdd || 0,
            jungrate: row.jungrate || 0,
            updemp: authStore.userid
        })
    }
    vAlert('정상적으로 저장되었습니다.'); fetchProcesses(selectedItem.itemcd)
  } catch (e) { vAlertError('저장 중 오류 발생') }
}

function initialize() {
  resetForm(searchData); Object.assign(searchData, { linecd: '010', astkind: '200' });
  Object.assign(selectedItem, { itemcd: '', itemnm: '', itsize: '', unit: '' });
  itemGrid?.clearData(); procGrid?.clearData(); itemCount.value = 0; search()
}

function excel() { procGrid?.download("xlsx", `표준공정도_${selectedItem.itemnm || '품목별'}.xlsx`) }

onMounted(() => { fetchLineOptions(); nextTick(() => { initGrids(); search(); }) })
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
.bg-light-yellow { background-color: #fffdf0 !important; }
</style>
