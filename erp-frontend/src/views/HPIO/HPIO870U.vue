<!--
	=============================================================
	프로그램명	: 외주가공생산실적승인 (HPIO870U)
	작성일자	: 2025.02.24
	설명        : 외주 생산 완료 건에 대한 검토 및 승인 처리 관리 (HPIO210U 표준 패턴 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-shield-check me-2 text-primary" style="font-size: 18px;"></i>
        생산관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        실적관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">외주가공생산실적승인 (HPIO870U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchList">조회</button>
        <button v-if="masterData.iono && searchData.slipyn === 'N'" class="btn-erp btn-save" @click="handleApproval('U0')">승인처리</button>
        <button v-if="masterData.iono && searchData.slipyn === 'Y'" class="btn-erp btn-danger" @click="handleApproval('D0')">승인취소</button>
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
                    <option value="N">미승인건 (대기)</option>
                    <option value="Y">승인완료건</option>
                  </select>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 투-그리드 레이아웃 영역 -->
      <div class="d-flex gap-2 flex-grow-1 overflow-hidden" style="min-height: 0;">

        <!-- ⬅️ 좌측: 실적 대기 목록 (grid1) -->
        <div class="card border shadow-sm d-flex flex-column overflow-hidden grid-container-left" style="width: 350px; min-width: 350px;">
          <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark text-center">실적 대기 목록</div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="tableRef1" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>

        <!-- ➡️ 우측: 상세 및 승인 정보 -->
        <div class="flex-grow-1 d-flex flex-column gap-2 overflow-hidden">
          <!-- 상세 마스터 정보 -->
          <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
            <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
              <span class="fw-bold small text-dark"><i class="bi bi-info-circle me-2 text-primary"></i>실적 상세 정보</span>
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
                    <th class="bg-light text-center">생산량</th>
                    <td class="text-end fw-bold text-success">{{ Number(masterData.proqty || 0).toLocaleString() }}</td>
                  </tr>
                  <tr>
                    <th class="bg-light text-center">출고번호</th>
                    <td>{{ masterData.ioym }}-{{ masterData.iono }}</td>
                    <th class="bg-light text-center">출고일자</th>
                    <td>{{ formatDate(masterData.ioymd) }}</td>
                    <th class="bg-light text-center">생산공정</th>
                    <td>{{ masterData.prognm }}</td>
                  </tr>
                  <tr>
                    <th class="bg-light text-center">주문번호</th>
                    <td>{{ masterData.ordym }}-{{ masterData.ordno }}</td>
                    <th class="bg-light text-center">생산일자</th>
                    <td>{{ formatDate(masterData.proymd) }}</td>
                    <th class="bg-light text-center">진행상태</th>
                    <td class="text-center">
                        <span :class="masterData.cnfm === 'Y' ? 'badge bg-success' : 'badge bg-danger'">{{ masterData.cnfm === 'Y' ? '승인' : '미승인' }}</span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- 상세 제품 그리드 -->
          <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column grid-container-right">
            <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center flex-shrink-0">
              <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>생산 재공품 상세 명세</span>
              <span class="text-danger small fw-bold ms-auto">※ 승인 처리 시 외주 가공비가 정산에 반영됩니다.</span>
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
const { modalVisible, modalProps } = useCommonHelp()

// [1] 데이터 모델링
const initymd = today.replace(/-/g, '')
const searchData = reactive({
  fromdt: initymd,
  todt: initymd,
  linecd: '010',
  slipyn: 'N'
})

const masterData = reactive<any>({})
const lineOptions = ref<any[]>([]); const resultList = ref<any[]>([])
const closingInfo = reactive({ clsymd: '', sclsym: '', pclsym: '' })

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
      { title: "거래처명", field: "custnm", hozAlign: "left", cssClass: "fw-bold" },
      { title: "실적번호", field: "io_disp", width: 120, hozAlign: "center", headerSort: false },
      { title: "상태", field: "slipyn", width: 70, hozAlign: "center", formatter: (c) => c.getValue() === 'Y' ? '완료' : '대기' }
    ],
  });
  grid1.on("rowClick", (e, row) => fetchDetail(row.getData()));

  grid2 = new Tabulator(tableRef2.value!, {
    layout: "fitColumns", height: "100%", placeholder: "실적 내역을 선택하세요.",
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "품목코드", field: "itemcd", width: 100, hozAlign: "center" },
      { title: "생산재공품", field: "itemnm", minWidth: 200, widthGrow: 1, cssClass: "fw-bold" },
      { title: "규격", field: "itsize", width: 150 },
      { title: "단위", field: "unit", width: 70, hozAlign: "center" },
      { title: "지시량", field: "qty", width: 100, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "생산량", field: "prdqty", width: 100, hozAlign: "right", formatter: "money", cssClass: "text-success fw-bold" },
      { title: "가공단가", field: "price", width: 110, hozAlign: "right", formatter: "money" },
      { title: "금액", field: "amt_calc", width: 120, hozAlign: "right", formatter: "money", mutatorData: (v,d) => Number(d.prdqty||0) * Number(d.price||0), cssClass: "text-primary fw-bold" }
    ],
  });
}

// [3] 비즈니스 로직
const fetchLineOptions = async () => {
  try {
    const res = await api.get('/hp00/HP00_000S_STR', { params: { gubun: 'L0', cmpycd: authStore.cmpycd, gbncd: 'Y', code: '' } })
    lineOptions.value = res.data.map((i: any) => ({ linecd: i.code || i.code, linenm: i.cdnm }));
  } catch (e) {}
}

async function fetchList() {
  try {
    const res = await api.post('/hpio/HPIO_870U_STR', {
      actkind: 'S1', cmpycd: authStore.cmpycd, fromdt: searchData.fromdt, todt: searchData.todt, linecd: searchData.linecd, slipyn: searchData.slipyn
    });
    grid1?.setData(res.data.map((i: any) => ({ ...i, io_disp: `${i.ioym}-${i.iono}` })));
    vAlert('조회되었습니다.');
    grid2?.clearData(); Object.keys(masterData).forEach(key => delete masterData[key]);
  } catch (e) { vAlertError('목록 조회 실패'); }
}

async function fetchDetail(row: any) {
  try {
    const [resM, resG] = await Promise.all([
      api.post('/hpio/HPIO_870U_STR', { actkind: 'S0', cmpycd: authStore.cmpycd, ioym: row.ioym, iono: row.iono, custcd: row.custcd }),
      api.post('/hpio/HPIO_870U_STR', { actkind: 'S2', cmpycd: authStore.cmpycd, ioym: row.ioym, iono: row.iono, custcd: row.custcd })
    ]);
    if (resM.data?.length) Object.assign(masterData, resM.data[0]);
    grid2?.setData(resG.data);
  } catch (e) { vAlertError('상세 조회 실패'); }
}

const handleApproval = async (actkind: string) => {
  const ioYmd = masterData.ioymd.replace(/-/g, '')
  if (ioYmd <= closingInfo.clsymd) return vAlertError('회계 마감된 일자입니다.')
  if (ioYmd.substring(0, 6) <= closingInfo.sclsym) return vAlertError('영업 마감된 월입니다.')
  if (ioYmd.substring(0, 6) <= closingInfo.pclsym) return vAlertError('생산 마감된 월입니다.')

  const msg = actkind === 'U0' ? '실적 승인 처리를 하시겠습니까?' : '승인 취소 처리를 하시겠습니까?';
  if (!confirm(msg)) return

  try {
    await api.post('/hpio/HPIO_870U_STR', {
      actkind, cmpycd: authStore.cmpycd, linecd: searchData.linecd, progcd: masterData.progcd,
      sv_slipyn: actkind === 'U0' ? 'Y' : 'N', iogbn: '200', prodcd: '200',
      ioym: masterData.ioym, iono: masterData.iono, custcd: masterData.custcd, userid: authStore.userid
    });
    vAlert('정상적으로 처리되었습니다.'); fetchList();
  } catch (e) { vAlertError('승인 처리 실패'); }
}

const printWindow = () => {
  if (!masterData.iono) return;
  const url = `../HPIO/HPIO_REQIN_PRINT.asp?PRTGU=Print&ioym=${masterData.ioym}&iono=${masterData.iono}&custcd=${masterData.custcd}`;
  window.open(url, '입고의뢰서인쇄', 'width=700,height=600,scrollbars=yes');
}

const initialize = () => {
  resetForm(searchData);
  Object.assign(searchData, { fromdt: initymd, todt: initymd, linecd: '010', slipyn: 'N' });
  grid1?.clearData(); grid2?.clearData();
  Object.keys(masterData).forEach(key => delete masterData[key]);
}

const formatDate = (v: any) => {
    if (!v) return ''
    const s = String(v).replace(/-/g, '')
    return s.length >= 6 ? `${s.substring(0, 4)}-${s.substring(4, 6)}${s.length === 8 ? '-' + s.substring(6, 8) : ''}` : s
}

onMounted(() => {
  fetchLineOptions();
  api.get('/hp00/HP00_000S_STR', { params: { gubun: 'CL', cmpycd: authStore.cmpycd } }).then(r => {
    if (r.data?.length) {
      closingInfo.clsymd = r.data[0].clsymd; closingInfo.sclsym = r.data[0].sclsym; closingInfo.pclsym = r.data[0].pclsym;
    }
  })
  nextTick(initGrids);
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
.grid-container-left, .grid-container-right { border-bottom: 3px solid #005a9f !important; }
</style>
