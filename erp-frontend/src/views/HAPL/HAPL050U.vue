<!--
	=============================================================
	프로그램명	: 품목별 배부적수 등록 (HAPL050U)
	작성일자	: 2025.02.24
	설명        : 부서별/품목별 원가 배부 적수 관리
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-box-seam me-2 text-primary" style="font-size: 18px;"></i>
        관리손익 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        기준정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">품목별 배부적수 (HAPL050U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="searchDepts">조회</button>
        <button class="btn-erp btn-save" @click="save">저장</button>
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
                <col style="width: 30%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light required">기준연월</th>
                <td>
                  <div class="d-flex align-items-center gap-1 px-1">
                    <select v-model="searchForm.yy" class="form-select form-select-sm" style="width: 100px;">
                      <option v-for="y in yearOptions" :key="y" :value="y">{{ y }}년</option>
                    </select>
                    <select v-model="searchForm.mm" class="form-select form-select-sm" style="width: 80px;">
                      <option v-for="m in monthOptions" :key="m" :value="m">{{ m }}월</option>
                    </select>
                  </div>
                </td>
                <th class="text-center bg-light required">배부기준</th>
                <td>
                  <select v-model="searchForm.divcd" class="form-select form-select-sm" @change="searchDepts">
                    <option v-for="opt in divideOptions" :key="opt.code" :value="opt.code">{{ opt.cdnm }}</option>
                  </select>
                </td>
                <td class="text-end pe-3 border-start-0">
                  <button class="btn btn-sm btn-outline-primary fw-bold" @click="generateWeights" :disabled="!selectedDept.code">
                    <i class="bi bi-gear-wide-connected me-1"></i>배부적수생성
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 투-그리드 레이아웃 -->
      <div class="d-flex gap-2 flex-grow-1 overflow-hidden" style="min-height: 0;">

        <!-- ⬅️ 좌측: 부서 목록 -->
        <div class="card border shadow-sm d-flex flex-column overflow-hidden grid-container-left" style="width: 280px; min-width: 280px;">
          <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark">판매부서 목록</div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="deptGridRef" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>

        <!-- ➡️ 우측: 품목 상세 그리드 -->
        <div class="flex-grow-1 d-flex flex-column gap-2 overflow-hidden">
          <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column grid-container-right">
            <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
              <span class="fw-bold small text-dark">
                <i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>부서별 품목 상세
                <span v-if="selectedDept.nm" class="badge bg-primary-subtle text-primary border border-primary-subtle ms-2">{{ selectedDept.nm }}</span>
              </span>
              <span v-if="updYn === 'N'" class="badge bg-danger">마감됨 (수정불가)</span>
            </div>
            <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
              <div ref="mainGridRef" class="tabulator-instance flex-grow-1"></div>
            </div>
          </div>
        </div>

      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import AppAlert from '@/components/AppAlert.vue'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()

// [1] 데이터 모델링
const currentYear = new Date().getFullYear()
const currentMonth = (new Date().getMonth() + 1).toString().padStart(2, '0')
const yearOptions = Array.from({ length: 20 }, (_, i) => String(currentYear - i))
const monthOptions = Array.from({ length: 12 }, (_, i) => String(i + 1).padStart(2, '0'))

const searchForm = reactive({
  yy: String(currentYear),
  mm: currentMonth,
  divcd: ''
})

const divideOptions = ref<any[]>([])
const selectedDept = reactive({ code: '', nm: '' })
const updYn = ref('Y')

const deptGridRef = ref<HTMLElement | null>(null)
const mainGridRef = ref<HTMLElement | null>(null)
let deptGrid: Tabulator | null = null
let mainGrid: Tabulator | null = null

// [2] 그리드 초기화
const initGrids = () => {
  deptGrid = new Tabulator(deptGridRef.value!, {
    layout: "fitColumns", height: "100%", placeholder: "데이터 없음", selectable: 1,
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "No", formatter: "rownum", width: 40, hozAlign: "center" },
      { title: "부서명", field: "deptnm", hozAlign: "left", cssClass: "fw-bold text-primary" }
    ],
  });
  deptGrid.on("rowClick", (e, row) => {
    const d = row.getData();
    selectedDept.code = d.deptcd;
    selectedDept.nm = d.deptnm;
    fetchItems();
  });

  mainGrid = new Tabulator(mainGridRef.value!, {
    layout: "fitColumns", height: "100%", placeholder: "부서를 선택하세요.",
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "No", formatter: "rownum", width: 40, hozAlign: "center" },
      { title: "상태", field: "_status", width: 60, hozAlign: "center", formatter: (c) => {
          const v = c.getValue();
          if (v === '수정') return '<span class="badge bg-warning text-dark">수정</span>';
          return '';
      }},
      { title: "품목명", field: "itemnm", minWidth: 200, widthGrow: 1, cssClass: "fw-bold text-dark" },
      { title: "규격", field: "itsize", width: 120 },
      { title: "단위", field: "unit", width: 60, hozAlign: "center" },
      { title: "거래처", field: "custnm", width: 180, hozAlign: "left" },
      { title: "배부적수", field: "divrate1", width: 110, hozAlign: "right", editor: "number", cssClass: "bg-light-yellow",
        cellEdited: (cell) => {
            cell.getRow().update({ _status: '수정' });
            calcRate();
        }
      },
      { title: "배부율 (%)", field: "rate1", width: 100, hozAlign: "right", formatter: (c) => Number(c.getValue() || 0).toFixed(1) + '%' }
    ],
  });
}

// [3] 비즈니스 로직
const loadInitData = async () => {
  try {
    const res = await api.post('/ha00/HA00_00P_STR', { gubun: 'SB', cmpycd: authStore.cmpycd, gbncd: '200', code: '200' });
    divideOptions.value = (res.data || []).map((n: any) => ({ code: n.divcd, cdnm: n.divnm }));
    if (divideOptions.value.length > 0) searchForm.divcd = divideOptions.value[0].code;
  } catch (e) { console.error(e) }
}

const searchDepts = async () => {
  try {
    const ym = searchForm.yy + searchForm.mm;
    const res = await api.post('/hapl/HAPL_050U_STR', { actkind: 'S1', cmpycd: authStore.cmpycd, iogbn: '200', yymm: ym });
    deptGrid?.setData(res.data || []);
    mainGrid?.clearData(); selectedDept.code = ''; selectedDept.nm = '';

    const clsRes = await api.post('/hapl/HAPL_100U_STR', { actkind: 'CHECK_CLOSE', cmpycd: authStore.cmpycd, stdym: ym });
    updYn.value = clsRes.data?.[0]?.updyn || 'Y';
    vAlert('조회되었습니다.');
  } catch (e) { vAlertError('조회 실패') }
}

const fetchItems = async () => {
  try {
    const ym = searchForm.yy + searchForm.mm;
    const res = await api.post('/hapl/HAPL_050U_STR', {
      actkind: 'S0', cmpycd: authStore.cmpycd, gubun: '020', yymm: ym, divcd: searchForm.divcd, deptcd: selectedDept.code
    });
    mainGrid?.setData((res.data || []).map((n: any) => ({ ...n, _status: '' })));
  } catch (e) { vAlertError('품목 조회 실패') }
}

const calcRate = () => {
    const data = mainGrid?.getData() || [];
    const totalWeight = data.reduce((acc, cur: any) => acc + Number(cur.divrate1 || 0), 0);
    data.forEach(row => {
        const weight = Number(row.divrate1 || 0);
        const rate = totalWeight > 0 ? (weight / totalWeight) * 100 : 0;
        mainGrid?.getRow(row).update({ rate1: rate });
    });
}

const generateWeights = async () => {
    if (updYn.value === 'N') return vAlertError('마감된 자료는 배부적수를 재생성할 수 없습니다.');
    if (!confirm('배부적수를 생성하시겠습니까?')) return;
    try {
        const ym = searchForm.yy + searchForm.mm;
        await api.post('/hapl/HAPL_050U_STR', {
            actkind: 'DR', cmpycd: authStore.cmpycd, gubun: '020', yymm: ym, divcd: searchForm.divcd, deptcd: selectedDept.code, userid: authStore.userid
        });
        vAlert('배부적수가 생성되었습니다.'); fetchItems();
    } catch (e) { vAlertError('생성 실패') }
}

const save = async () => {
  if (updYn.value === 'N') return vAlertError('마감된 자료는 재작업할 수 없습니다.')
  const details = mainGrid?.getData().filter((r: any) => r._status === '수정') || []
  if (details.length === 0) return vAlertError('수정된 항목이 없습니다.')

  if (!confirm('저장하시겠습니까?')) return

  try {
    const ym = searchForm.yy + searchForm.mm
    const res = await api.post('/hapl/HAPL_050U_STR', {
      actkind: 'U0',
      cmpycd: authStore.cmpycd,
      gubun: '020',
      stdym: ym,
      divcd: searchForm.divcd,
      deptcd: selectedDept.code,
      items: details.map((row: any) => ({
        custcd: row.custcd,
        itemcd: row.itemcd,
        itsize: row.itsize || '',
        unit: row.unit || '',
        itemnm: row.itemnm || '',
        divrate1: Number(row.divrate1 || 0),
        divrate2: 0,
        divrate3: 0
      })),
      userid: authStore.userid
    })

    const resData = res.data?.[0] || {}
    const resCode = String(resData.res || resData.result || resData.outym || '').trim()

    if (resCode === '000000' || resCode === 'N') {
      vAlertError('저장 실패');
    } else {
      vAlert('저장되었습니다.')
      fetchItems()
    }
  } catch (e: any) {
    vAlertError('저장 오류')
  }
}

const initialize = () => {
    searchForm.yy = String(currentYear); searchForm.mm = currentMonth;
    if (divideOptions.value.length > 0) searchForm.divcd = divideOptions.value[0].code;
    deptGrid?.clearData(); mainGrid?.clearData(); selectedDept.code = ''; selectedDept.nm = '';
}

onMounted(async () => {
  await loadInitData()
  nextTick(initGrids)
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
.bg-light-yellow { background-color: #fffde7 !important; }
</style>
