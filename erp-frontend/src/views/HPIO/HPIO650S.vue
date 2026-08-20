<!--
	=============================================================
	프로그램명	: 품목별 수불현황 (HPIO650S)
	작성일자	: 2025.02.24
	설명        : 개별 품목에 대한 기간별 상세 입출고 이력 및 재고 흐름 조회 (HPIO210U 표준 패턴 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-file-earmark-medical me-2 text-primary" style="font-size: 18px;"></i>
        생산정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">품목별 수불현황 (HPIO650S)</span>
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
                <col style="width: 10%" /><col style="width: 25%" />
                <col style="width: 10%" /><col style="width: 15%" />
                <col style="width: 10%" /><col style="width: 30%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light required">입출일자</th>
                <td class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                  <input v-model="fymd_f" type="date" class="form-control form-control-sm" style="width: 140px;" />
                  <span class="px-1 opacity-50">~</span>
                  <input v-model="tymd_f" type="date" class="form-control form-control-sm" style="width: 140px;" />
                </td>
                <th class="text-center bg-light required">창 고</th>
                <td>
                  <select v-model="searchData.whcd" class="form-select form-select-sm">
                    <option value="000">전체창고</option>
                    <option v-for="opt in whOptions" :key="opt.whcd" :value="opt.whcd">{{ opt.whnm }}</option>
                  </select>
                </td>
                <th class="text-center bg-light required">품 목</th>
                <td>
                  <div class="input-group input-group-sm w-75">
                    <input v-model="searchData.itemcd" type="text" class="form-control text-center bg-light fw-bold" style="max-width: 80px;" readonly />
                    <input v-model="searchData.itemnm" type="text" class="form-control" placeholder="품목 선택" @keyup.enter="handleOpenHelp('ITEM')" />
                    <button class="btn btn-outline-secondary px-2" @click="handleOpenHelp('ITEM')"><i class="bi bi-search"></i></button>
                    <input v-model="searchData.itsize" type="text" class="form-control bg-light" style="max-width: 120px;" readonly placeholder="규격" />
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
          <span class="fw-bold small text-dark"><i class="bi bi-list-check me-2 text-primary"></i>상세 입출고 이력 및 재고 추이</span>
          <div class="d-flex align-items-center gap-3">
            <span v-if="searchData.itemnm" class="badge bg-primary-subtle text-primary border border-primary-subtle px-3">{{ searchData.itemnm }} [{{ searchData.itemcd }}]</span>
            <span v-if="rowCount" class="badge bg-secondary-subtle text-dark border border-secondary-subtle" style="font-size: 10px;">Total: {{ rowCount }}건</span>
          </div>
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
import { useRoute, useRouter } from 'vue-router'
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
const router = useRouter()
const { today, firstDay } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps } = useCommonHelp()

// [1] 데이터 모델링
const searchData = reactive({
  fymd: firstDay.replace(/-/g, ''),
  tymd: today.replace(/-/g, ''),
  whcd: '000',
  itemcd: '', itemnm: '', itsize: '', unit: '',
  astkind: ''
})

const whOptions = ref<any[]>([])
const rowCount = ref(0)

// 포맷팅 헬퍼 (ui 접두어 제거)
const fymd_f = computed({ get: () => formatDateString(searchData.fymd), set: (v) => { if (v) searchData.fymd = v.replace(/-/g, '') } })
const tymd_f = computed({ get: () => formatDateString(searchData.tymd), set: (v) => { if (v) searchData.tymd = v.replace(/-/g, '') } })

const tableRef = ref<HTMLDivElement | null>(null)
let grid: Tabulator | null = null

// [2] 그리드 초기화
const initGrids = () => {
  grid = new Tabulator(tableRef.value!, {
    layout: "fitColumns", height: "100%", placeholder: "데이터 없음",
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "일 자", field: "ioymd", width: 150, hozAlign: "center", formatter: (c) => formatDateString(c.getValue()) },
      {
        title: "적 요 (거래처/내역)", field: "custnm", minWidth: 250, widthGrow: 1,
        formatter: (cell) => {
          const d = cell.getData();
          let isLinkable = false;
          if (d.iogbn === "200" && d.iotype >= "300" && d.iotype <= "390" && d.iotype !== "380") isLinkable = true;
          else if (d.iogbn === "200" && d.iotype === "380") isLinkable = true;
          else if (d.iogbn === "100" && d.gubun === "1" && d.iotype <= "190") isLinkable = true;

          return isLinkable ? `<span class="text-primary text-decoration-underline cursor-pointer fw-bold">${cell.getValue() || ''}</span>` : `<span>${cell.getValue() || ''}</span>`;
        },
        cellClick: (e, cell) => {
          const d = cell.getData();
          if (d.iogbn === "200" && d.iotype >= "300" && d.iotype <= "390" && d.iotype !== "380") {
            router.push({ path: '/HPIO/HPIO510U', query: { ioym: d.ioym, iono: d.iono, deptcd: d.deptcd } });
          } else if (d.iogbn === "200" && d.iotype === "380") {
            router.push({ path: '/HSIO/HSIO570U', query: { ioym: d.ioym, iono: d.iono, deptcd: d.deptcd } });
          } else if (d.iogbn === "100" && d.gubun === "1" && d.iotype <= "190") {
            router.push({ path: '/HSIO/HSIO100U', query: { ioym: d.ioym, iono: d.iono, deptcd: d.deptcd } });
          }
        },
        bottomCalc: () => "합 계"
      },
      { title: "입출구분", field: "iotypenm", width: 150, hozAlign: "center" },
      { title: "입 고", field: "inqty", width: 150, hozAlign: "right", formatter: "money", cssClass: "text-success", bottomCalc: "sum" },
      { title: "출 고", field: "outqty", width: 150, hozAlign: "right", formatter: "money", cssClass: "text-danger", bottomCalc: "sum" },
      {
        title: "재 고", field: "stkqty", width: 150, hozAlign: "right",
        formatter: (c) => `<span class="${Number(c.getValue()) < 0 ? 'text-danger' : ''} fw-bold">${Number(c.getValue()).toLocaleString()}</span>`
      },
      { title: "창 고", field: "whnm", width: 150 },
      { title: "비 고", field: "pkunitnm", width: 150 }
    ],
    columnCalcs: "table"
  });
}


// [3] 비즈니스 로직
const fetchWhOptions = async () => {
  try {
    const res = await api.get('/hs00/HS00_000S_STR', { params: { gubun: 'W0', cmpycd: authStore.cmpycd } });
    whOptions.value = res.data.map((i: any) => ({ whcd: i.whcd, whnm: i.whnm }));
  } catch (e) {}
}

async function fetchList() {
  if (!searchData.itemcd) return vAlertError('품목을 선택하세요.');
  try {
    const res = await api.post('/hpio/HPIO_650S_STR', {
      cmpycd: authStore.cmpycd, whcd: searchData.whcd,
      fymd: searchData.fymd, tymd: searchData.tymd,
      astkind: searchData.astkind || '', itemcd: searchData.itemcd
    });

    let runningStock = 0;
    const mapped = res.data.map((item: any) => {
      runningStock = runningStock + Number(item.inqty || 0) - Number(item.outqty || 0);
      return { ...item, stkqty: runningStock };
    });

    grid?.setData(mapped);
    rowCount.value = mapped.length;
    vAlert('조회되었습니다.');
  } catch (e) { vAlertError('조회 실패'); }
}

const handleOpenHelp = (type: string) => {
  const hpPath = '/hp00/HP00_000S_STR'
  const hsPath = '/hs00/HS00_000S_STR'

  switch (type) {
    case 'ITEM': // 품목 선택
      Object.assign(modalProps, {
        title: '품목 선택',
        path: hpPath,
        large: true,
        data: { gubun: 'I0', cmpycd: authStore.cmpycd, gbncd: 'A', code: '', remark: '' },
        columns: [
          { title: '품목코드', field: 'itemcd', width: 120, hozAlign: 'center' },
          { title: '품목명', field: 'itemnm', minWidth: 250, widthGrow: 1, hozAlign: 'left' },
          { title: '규격', field: 'itsize', width: 150 },
          { title: '단위', field: 'unit', width: 80, hozAlign: 'center' }
        ],
        onConfirm: (d: any) => {
          Object.assign(searchData, { itemcd: d.itemcd, itemnm: d.itemnm, itsize: d.itsize, unit: d.unit, astkind: d.astkind });
          fetchList();
        }
      })
      break

    case 'WH': // 창고 선택
      Object.assign(modalProps, {
        title: '창고 선택',
        path: hsPath,
        data: { gubun: 'W1', cmpycd: authStore.cmpycd },
        columns: [
          { title: '창고코드', field: 'whcd', width: 100, hozAlign: 'center' },
          { title: '창고명', field: 'whnm', minWidth: 150, widthGrow: 1, hozAlign: 'left' }
        ],
        onConfirm: (d: any) => {
          searchData.whcd = d.whcd;
          searchData.whnm = d.whnm;
        }
      })
      break
  }
  modalVisible.value = true
}

const initialize = () => {
  resetForm(searchData);
  Object.assign(searchData, { fymd: firstDay.replace(/-/g, ''), tymd: today.replace(/-/g, ''), whcd: '000' });
  grid?.clearData(); rowCount.value = 0;
}

const exportExcel = () => grid?.download("xlsx", `품목수불_${searchData.itemnm || searchData.itemcd}.xlsx`)
const formatDateString = (v: any) => v && v.length === 8 ? `${v.substring(0, 4)}-${v.substring(4, 6)}-${v.substring(6, 8)}` : v;

onMounted(async () => {
  await fetchWhOptions();

  if (route.query.itemcd) {
    Object.assign(searchData, {
      itemcd: String(route.query.itemcd),
      astkind: String(route.query.astkind || ''),
      whcd: String(route.query.whcd || '000'),
      fymd: String(route.query.fymd),
      tymd: String(route.query.tymd)
    });
  }

  nextTick(() => {
    initGrids();
    if (searchData.itemcd) fetchList();
  });
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
.grid-container-right { border-bottom: 3px solid #005a9f !important; }
</style>
