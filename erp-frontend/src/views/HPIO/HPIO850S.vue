<!--
	=============================================================
	프로그램명	: 출고의뢰서 관리 (HPIO850S)
	작성일자	: 2025.02.24
	설명        : 생산 라인별 자재 출고 의뢰 내역 및 상세 자재 명세 조회 (HPIO210U 표준 패턴 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-file-earmark-check me-2 text-primary" style="font-size: 18px;"></i>
        생산정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">출고의뢰서 관리 (HPIO850S)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchList">조회</button>
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
                <th class="text-center bg-light required">출고일자</th>
                <td class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                  <input v-model="fromdt_f" type="date" class="form-control form-control-sm" style="width: 140px;" />
                  <span class="px-1 opacity-50">~</span>
                  <input v-model="todt_f" type="date" class="form-control form-control-sm" style="width: 140px;" />
                </td>
                <th class="text-center bg-light required">생산라인</th>
                <td>
                  <select v-model="searchData.linecd" class="form-select form-select-sm" @change="fetchList">
                    <option v-for="opt in lineOptions" :key="opt.linecd" :value="opt.linecd">[{{ opt.linecd }}] {{ opt.linenm }}</option>
                  </select>
                </td>
                <th class="text-center bg-light required">조회구분</th>
                <td>
                  <select v-model="searchData.slipyn" class="form-select form-select-sm" @change="fetchList">
                    <option value="N">미결건 (대기)</option>
                    <option value="Y">완료건 (출고됨)</option>
                  </select>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 투-그리드 레이아웃 영역 -->
      <div class="d-flex gap-2 flex-grow-1 overflow-hidden" style="min-height: 0;">

        <!-- ⬅️ 좌측: 의뢰 목록 (grid1) -->
        <div class="card border shadow-sm d-flex flex-column overflow-hidden grid-container-left" style="width: 350px; min-width: 350px;">
          <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark text-center">출고 의뢰 목록</div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="tableRef1" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>

        <!-- ➡️ 우측: 상세 정보 및 자재 그리드 (grid2) -->
        <div class="flex-grow-1 d-flex flex-column gap-2 overflow-hidden">
          <!-- 의뢰 마스터 요약 정보 -->
          <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
            <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
              <span class="fw-bold small text-dark"><i class="bi bi-info-circle me-2 text-primary"></i>의뢰 마스터 요약</span>
              <button v-if="masterData.iono" class="btn btn-sm btn-outline-dark py-0 px-2 fw-bold" @click="printWindow" style="font-size: 11px;">인쇄</button>
            </div>
            <div class="card-body p-0 bg-white">
              <table class="erp-table-dense w-100">
                <colgroup>
                  <col style="width: 100px;" /><col />
                  <col style="width: 100px;" /><col />
                  <col style="width: 100px;" /><col />
                </colgroup>
                <tbody>
                  <tr>
                    <th class="bg-light text-center">생산제품</th>
                    <td><span class="fw-bold text-primary">{{ masterData.itemnm }}</span> <span class="small text-muted">({{ masterData.itemcd }})</span></td>
                    <th class="bg-light text-center">규격/단위</th>
                    <td>{{ masterData.itsize }} / {{ masterData.unit }}</td>
                    <th class="bg-light text-center">생산지시량</th>
                    <td class="text-end fw-bold">{{ Number(masterData.proqty || 0).toLocaleString() }}</td>
                  </tr>
                  <tr>
                    <th class="bg-light text-center">출고번호</th>
                    <td>{{ masterData.ioym }}-{{ masterData.iono }}</td>
                    <th class="bg-light text-center">출고창고</th>
                    <td>{{ masterData.whnm }} → {{ masterData.iwhnm }}</td>
                    <th class="bg-light text-center">진행상태</th>
                    <td class="text-center">
                        <span :class="masterData.cnfm === 'Y' ? 'badge bg-success' : 'badge bg-warning text-dark'">{{ masterData.cnfm === 'Y' ? '승인완료' : '미승인' }}</span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- 상세 자재 그리드 -->
          <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column grid-container-right">
            <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center flex-shrink-0">
              <span class="fw-bold small text-dark"><i class="bi bi-box-fill me-2 text-success"></i>소요 자재 상세 명세</span>
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
import { useCommonHelp } from '@/composables/useCommonHelp'
import { getDate } from '@/composables/useDate'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'

const authStore = useAuthStore()
const { today } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps, openHelp } = useCommonHelp()

// [1] 데이터 모델링
const initymd = today.replace(/-/g, '')
const searchData = reactive({
  fromdt: initymd,
  todt: initymd,
  linecd: '010',
  slipyn: 'N'
})

const masterData = reactive<any>({})
const lineOptions = ref<any[]>([]); const rowCount = ref(0)

const fromdt_f = computed({ get: () => formatDate(searchData.fromdt), set: (v) => { if (v) searchData.fromdt = v.replace(/-/g, '') } })
const todt_f = computed({ get: () => formatDate(searchData.todt), set: (v) => { if (v) searchData.todt = v.replace(/-/g, '') } })

const tableRef1 = ref<HTMLDivElement | null>(null); const tableRef2 = ref<HTMLDivElement | null>(null)
let grid1: Tabulator | null = null; let grid2: Tabulator | null = null

// [2] 그리드 초기화
const initGrids = () => {
  grid1 = new Tabulator(tableRef1.value!, {
    layout: "fitColumns", height: "100%", placeholder: "데이터 없음", selectable: 1,
    columns: [
      { title: "No", formatter: "rownum", width: 40, hozAlign: "center", headerSort: false },
      { title: "거래처명", field: "custnm", hozAlign: "left", cssClass: "fw-bold text-primary" },
      { title: "의뢰번호", field: "io_disp", width: 120, hozAlign: "center", headerSort: false }
    ],
  });
  grid1.on("rowClick", (e, row) => fetchDetail(row.getData()));

  grid2 = new Tabulator(tableRef2.value!, {
    layout: "fitColumns", height: "100%", placeholder: "의뢰 내역을 선택하세요.",
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "No", formatter: "rownum", width: 40, hozAlign: "center" },
      { title: "자재코드", field: "itemcd", width: 110, hozAlign: "center" },
      { title: "자재명", field: "itemnm", minWidth: 200, widthGrow: 1, cssClass: "fw-bold" },
      { title: "규격", field: "itsize", width: 150 },
      { title: "단위", field: "unit", width: 70, hozAlign: "center" },
      { title: "출고수량", field: "qty", width: 110, hozAlign: "right", formatter: "money", cssClass: "text-primary fw-bold" },
      { title: "공급거래처", field: "custnm", width: 150 }
    ],
  });
}

// [3] 비즈니스 로직
const fetchLineOptions = async () => {
  try {
    const res = await api.get('/hp00/HP00_000S_STR', { params: { gubun: 'L0', cmpycd: authStore.cmpycd, gbncd: 'Y', code: '' } })
    lineOptions.value = res.data.map((i: any) => ({ linecd: i.linecd, linenm: i.linenm }));
  } catch (e) {}
}

async function fetchList() {
  try {
    const res = await api.post('/hpio/HPIO_850S_STR', {
      actkind: 'S0', cmpycd: authStore.cmpycd, fromdt: searchData.fromdt, todt: searchData.todt, linecd: searchData.linecd, slipyn: searchData.slipyn
    });
    grid1?.setData(res.data.map((i: any) => ({ ...i, io_disp: `${i.ioym}-${i.iono}` })));
    rowCount.value = res.data.length;
    vAlert('조회되었습니다.');
    grid2?.clearData(); Object.keys(masterData).forEach(key => delete masterData[key]);
  } catch (e) { vAlertError('목록 조회 실패'); }
}

async function fetchDetail(row: any) {
  try {
    const [resM, resG] = await Promise.all([
      api.post('/hpio/HPIO_850S_STR', { actkind: 'S1', cmpycd: authStore.cmpycd, ioym: row.ioym, iono: row.iono, custcd: row.custcd }),
      api.post('/hpio/HPIO_850S_STR', { actkind: 'S2', cmpycd: authStore.cmpycd, ioym: row.ioym, iono: row.iono, custcd: row.custcd })
    ]);
    if (resM.data?.length) Object.assign(masterData, resM.data[0]);
    grid2?.setData(resG.data);
  } catch (e) { vAlertError('상세 조회 실패'); }
}

const initialize = () => {
  resetForm(searchData);
  Object.assign(searchData, { fromdt: initymd, todt: initymd, linecd: '010', slipyn: 'N' });
  grid1?.clearData(); grid2?.clearData(); rowCount.value = 0;
  Object.keys(masterData).forEach(key => delete masterData[key]);
}

const printWindow = () => {
  if (!masterData.iono) return;
  const url = `../HPIO/HPIO_REQOUT_PRINT.asp?PRTGU=Print&ioym=${masterData.ioym}&iono=${masterData.iono}&custcd=${masterData.custcd}`;
  window.open(url, '의뢰서인쇄', 'width=700,height=600,scrollbars=yes');
}

const exportExcel = () => grid2?.download("xlsx", `출고의뢰상세_${today}.xlsx`)
const formatDate = (v: any) => v && v.length === 8 ? `${v.substring(0, 4)}-${v.substring(4, 6)}-${v.substring(6, 8)}` : v;

onMounted(() => {
  fetchLineOptions();
  nextTick(initGrids);
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
.grid-container-left, .grid-container-right { border-bottom: 3px solid #005a9f !important; }
</style>
