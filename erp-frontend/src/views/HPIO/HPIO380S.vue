<!--
	=============================================================
	프로그램명	: 투입자재 상세현황 (HPIO380S)
	작성일자	: 2025.02.24
	설명        : 생산 공정별 투입 자재의 상세 내역 및 생산 제품 연계 현황 조회 (HPIO210U 표준 패턴 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-card-list me-2 text-primary" style="font-size: 18px;"></i>
        생산정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">투입자재 상세현황 (HPIO380S)</span>
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
                <col style="width: 10%" /><col style="width: 20%" />
                <col style="width: 10%" /><col style="width: 25%" />
                <col style="width: 10%" /><col style="width: 25%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light required">생산라인</th>
                <td>
                  <select v-model="searchData.linecd" class="form-select form-select-sm" @change="onLineChange">
                    <option v-for="opt in lineOptions" :key="opt.linecd" :value="opt.linecd">{{ opt.linenm }}</option>
                  </select>
                </td>
                <th class="text-center bg-light required">투입일자</th>
                <td class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                  <input v-model="fromdt" type="date" class="form-control form-control-sm" style="width: 140px;" />
                  <span class="px-1 opacity-50">~</span>
                  <input v-model="todt" type="date" class="form-control form-control-sm" style="width: 140px;" />
                </td>
                <th class="text-center bg-light">생산공정</th>
                <td>
                  <select v-model="searchData.progcd" class="form-select form-select-sm">
                    <option value="">전체공정</option>
                    <option v-for="opt in progOptions" :key="opt.progcd" :value="opt.progcd">{{ opt.prognm }}</option>
                  </select>
                </td>
              </tr>
              <tr>
                <th class="text-center bg-light required">조회구분</th>
                <td>
                  <select v-model="searchData.selgbn" class="form-select form-select-sm">
                    <option value="000">전체자산</option>
                    <option v-for="opt in gbnOptions" :key="opt.code" :value="opt.code">{{ opt.cdnm }}</option>
                    <option value="200">재공품</option>
                  </select>
                </td>
                <th class="text-center bg-light">투입자재</th>
                <td colspan="3">
                  <div class="input-group input-group-sm w-75">
                    <input v-model="searchData.mitemcd" type="text" class="form-control text-center bg-light fw-bold" style="max-width: 80px;" readonly />
                    <input v-model="searchData.mitemnm" type="text" class="form-control" placeholder="자재 선택" @keyup.enter="handleOpenHelp('MAT')" />
                    <button v-if="searchData.mitemcd" class="btn btn-outline-secondary px-2" @click="clearMaterial"><i class="bi bi-x"></i></button>
                    <button class="btn btn-outline-secondary px-2" @click="handleOpenHelp('MAT')"><i class="bi bi-search"></i></button>
                    <input v-model="searchData.mitsize" type="text" class="form-control bg-light" style="max-width: 150px;" readonly placeholder="규격" />
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column grid-container-right">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>투입 자재 상세 내역</span>
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
import { useRoute } from 'vue-router'
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
const route = useRoute()
const { today, firstDay } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps, openHelp } = useCommonHelp()

// [1] 데이터 모델링
const initymd = today.replace(/-/g, '')
const initfromdt = firstDay.replace(/-/g, '')

const searchData = reactive<any>({
  linecd: '010', progcd: '', prodcd: '',
  fromdt: initfromdt, todt: initymd,
  mitemcd: '', mitemnm: '', mitsize: '', munit: '',
  selgbn: '000'
})

const gbnOptions = ref<any[]>([]); const lineOptions = ref<any[]>([]); const progOptions = ref<any[]>([])
const rowCount = ref(0)

// 포맷팅 헬퍼 (ui 접두어 제거)
const fromdt = computed({ get: () => formatDate(searchData.fromdt), set: (v) => { if (v) searchData.fromdt = v.replace(/-/g, '') } })
const todt = computed({ get: () => formatDate(searchData.todt), set: (v) => { if (v) searchData.todt = v.replace(/-/g, '') } })

const tableRef = ref<HTMLDivElement | null>(null)
let grid: Tabulator | null = null

// [2] 그리드 초기화
const initGrids = () => {
  grid = new Tabulator(tableRef.value!, {
    layout: "fitColumns", height: "100%", placeholder: "데이터 없음",
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "투입일자", field: "proymd", width: 110, hozAlign: "center", formatter: (c) => formatDate(c.getValue()) },
      { title: "구분", field: "astkindnm", width: 90, hozAlign: "center" },
      { title: "투입자재", field: "mitemnm", minWidth: 200, widthGrow: 1, cssClass: "fw-bold text-success",
        formatter: (cell) => `<div>${cell.getValue() || ''}</div><div class="small text-muted">${cell.getData().mitemcd || ''}</div>`
      },
      { title: "자재규격", field: "mitsize", width: 130 },
      { title: "투입량", field: "inqty", width: 100, hozAlign: "right", formatter: "money", cssClass: "fw-bold" },
      { title: "생산제품", field: "itemnm", minWidth: 200, widthGrow: 1, cssClass: "fw-bold text-primary",
        formatter: (cell) => `<div>${cell.getValue() || ''}</div><div class="small text-muted">${cell.getData().itemcd || ''}</div>`
      },
      { title: "제품규격", field: "itsize", width: 130 },
      { title: "생산량", field: "prdqty", width: 100, hozAlign: "right", formatter: "money" },
      { title: "생산구분", field: "prodnm", width: 100, hozAlign: "center" },
      { title: "투입공정", field: "prognm", width: 120, hozAlign: "center" }
    ],
  });
}

// [3] 비즈니스 로직
const fetchInitOptions = async () => {
  try {
    const [gbn, line] = await Promise.all([
      api.get('/hp00/HP00_000S_STR', { params: { gubun: 'E0', cmpycd: authStore.cmpycd, gbncd: '100' } }),
      api.get('/hp00/HP00_000S_STR', { params: { gubun: 'L0', cmpycd: authStore.cmpycd, gbncd: 'Y', code: '' } })
    ]);
    gbnOptions.value = (gbn.data || []).map((i: any) => ({
        code: i.code || i.CODE || '',
        cdnm: i.cdnm || i.CDNM || ''
    })).filter((i: any) => i.code <= '119');

    lineOptions.value = (line.data || []).map((i: any) => ({
        linecd: i.linecd || i.code || i.CODE || '',
        linenm: i.linenm || i.cdnm || i.CDNM || ''
    }));

    if (lineOptions.value.length > 0) onLineChange();
  } catch (e) {}
}

const onLineChange = async () => {
  try {
    const res = await api.get('/hp00/HP00_000S_STR', { params: { gubun: 'G0', cmpycd: authStore.cmpycd, gbncd: searchData.linecd, code: '' } })
    progOptions.value = (res.data || []).map((i: any) => ({
        progcd: i.progcd || i.code || i.CODE || '',
        prognm: i.prognm || i.cdnm || i.CDNM || ''
    }));
  } catch (e) {}
}

async function fetchList() {
  if (!searchData.linecd) return vAlertError('생산라인을 선택하세요.')
  try {
    const res = await api.post('/hpio/HPIO_380S_STR', {
      cmpycd: authStore.cmpycd,
      selgbn: searchData.selgbn,
      linecd: searchData.linecd,
      progcd: searchData.progcd || '',
      prodcd: searchData.prodcd || '',
      mitemcd: searchData.mitemcd || '',
      fromdt: searchData.fromdt,
      todt: searchData.todt
    })

    grid?.setData(res.data)
    rowCount.value = res.data.length
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

const handleOpenHelp = (type: string) => {
  if (type === 'MAT') {
    openHelp('ITEM', (d) => {
      // 💡 데이터 필드명이 대소문자 섞여서 올 수 있으므로 유연하게 처리
      const itemcd = d.itemcd || d.ITEMCD;
      const itemnm = d.itemnm || d.itemnm;
      const itsize = d.itsize || d.itsize;
      const unit = d.unit || d.unit;
      Object.assign(searchData, { mitemcd: itemcd, mitemnm: itemnm, mitsize: itsize, munit: unit });
    }, { gubun: 'I1', title: '자재 선택' });
  }
}

const clearMaterial = () => {
    Object.assign(searchData, { mitemcd: '', mitemnm: '', mitsize: '', munit: '' });
}

const initialize = () => {
  resetForm(searchData);
  Object.assign(searchData, { linecd: '010', progcd: '', prodcd: '', fromdt: initfromdt, todt: initymd, selgbn: '000', mitemcd: '', mitemnm: '', mitsize: '', munit: '' });
  grid?.clearData(); rowCount.value = 0;
}

const exportExcel = () => grid?.download("xlsx", `투입자재상세_${searchData.todt}.xlsx`)
const formatDate = (v: any) => v && v.length === 8 ? `${v.substring(0, 4)}-${v.substring(4, 6)}-${v.substring(6, 8)}` : v;

onMounted(async () => {
  await fetchInitOptions();
  if (route.query.linecd) {
    Object.assign(searchData, {
      linecd: String(route.query.linecd),
      selgbn: String(route.query.selgbn || '000'),
      fromdt: String(route.query.fromdt),
      todt: String(route.query.todt),
      mitemcd: String(route.query.mitemcd || '')
    });
    await onLineChange();
  }
  nextTick(() => {
    initGrids();
    if (route.query.linecd) fetchList();
  });
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
.grid-container-right { border-bottom: 3px solid #005a9f !important; }
</style>
