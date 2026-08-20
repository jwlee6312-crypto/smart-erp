<!--
	=============================================================
	프로그램명	: 기초재고 등록 (HPBA800U)
	작성일자	: 2025.02.24
	설명        : 품목별 초기 이월 재고 수량 및 금액 등록 관리 (표준 패턴 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-box-seam-fill me-2 text-primary" style="font-size: 18px;"></i>
        기초자료 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">기초재고 등록 (HPBA800U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
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
                <col style="width: 10%" /><col style="width: 40%" />
                <col style="width: 10%" /><col style="width: 40%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light">기준연월</th>
                <td class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                  <select v-model="formData.yy" class="form-select form-select-sm" style="width: 100px;">
                    <option v-for="year in yearOptions" :key="year" :value="year">{{ year }}년</option>
                  </select>
                  <select v-model="monthStr" class="form-select form-select-sm" style="width: 80px;">
                    <option v-for="month in monthOptions" :key="month" :value="month">{{ month }}월</option>
                  </select>
                </td>
                <th class="text-center bg-light">품 목</th>
                <td>
                  <div class="input-group input-group-sm w-75">
                    <input v-model="formData.itemcd" type="text" class="form-control text-center bg-light fw-bold" style="max-width: 80px;" readonly />
                    <input v-model="formData.itemnm" type="text" class="form-control" placeholder="품목 선택" @keyup.enter="handleOpenHelp('ITEM')" />
                    <button class="btn btn-outline-secondary" @click="handleOpenHelp('ITEM')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 투-그리드 레이아웃 영역 (마스터 입력 + 리스트) -->
      <div class="d-flex flex-column gap-2 flex-grow-1 overflow-hidden" style="min-height: 0;">

        <!-- 상세 입력 폼 -->
        <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
          <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center">
            <span class="fw-bold small text-dark"><i class="bi bi-pencil-square me-2 text-primary"></i>재고 정보 입력</span>
          </div>
          <div class="card-body p-0 bg-white">
            <table class="erp-table-dense w-100">
              <colgroup>
                <col style="width: 120px;" /><col />
                <col style="width: 120px;" /><col />
                <col style="width: 120px;" /><col />
              </colgroup>
              <tbody>
                <tr>
                  <th class="bg-light text-center">규격/단위</th>
                  <td>
                    <div class="d-flex gap-1">
                      <input v-model="formData.itsize" class="form-control bg-light" readonly placeholder="규격" />
                      <input v-model="formData.unit" class="form-control bg-light text-center" style="max-width: 60px;" readonly placeholder="단위" />
                    </div>
                  </td>
                  <th class="required bg-light text-center">기초재고수량</th>
                  <td>
                    <input v-model="formData.qty" type="number" class="form-control text-end fw-bold" />
                  </td>
                  <th class="required bg-light text-center">기초재고금액</th>
                  <td>
                    <input v-model="formData.amt" type="number" class="form-control text-end fw-bold text-primary" />
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- 리스트 그리드 영역 -->
        <div class="card border shadow-sm flex-grow-1 d-flex flex-column overflow-hidden grid-container-right">
          <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
            <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>기초 재고 등록 내역</span>
          </div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="tableRef" class="tabulator-instance flex-grow-1"></div>
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
const formData = reactive<any>({
  actkind: 'S0', cmpycd: authStore.cmpycd,
  yy: today.substring(0, 4), mm: Number(today.substring(5, 7)),
  itemcd: '', itemnm: '', itsize: '', unit: '', qty: 0, amt: 0
})

const monthStr = computed({
  get: () => String(formData.mm).padStart(2, '0'),
  set: (v) => { formData.mm = Number(v) }
})

const yearOptions = ref<string[]>([])
const monthOptions = ['01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12']

const tableRef = ref<HTMLDivElement | null>(null)
let grid: Tabulator | null = null

const initGrids = () => {
  grid = new Tabulator(tableRef.value!, {
    layout: "fitColumns", height: "100%", placeholder: "데이터 없음",
    columns: [
      { title: "No", formatter: "rownum", width: 40, hozAlign: "center", headerSort: false },
      { title: "연월", field: "ym", width: 100, hozAlign: "center", formatter: (c) => {
          const v = c.getValue(); return v && v.length === 6 ? `${v.substring(0,4)}-${v.substring(4,6)}` : v;
      }},
      { title: "품목코드", field: "itemcd", width: 110, hozAlign: "center", cssClass: "text-primary fw-bold" },
      { title: "품목명", field: "itemnm", minWidth: 200, widthGrow: 1, cssClass: "fw-bold" },
      { title: "규격", field: "itsize", width: 150 },
      { title: "단위", field: "unit", width: 70, hozAlign: "center" },
      { title: "수량", field: "qty", width: 100, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "금액", field: "amt", width: 120, hozAlign: "right", formatter: "money", cssClass: "text-primary fw-bold" }
    ],
  });
  grid.on("rowClick", (e, row) => fetchDetail(row.getData()));
}

async function search() {
  try {
    const res = await api.post('/hpba/HPBA_800U_STR', {
      actkind: 'S1', cmpycd: authStore.cmpycd,
      yy: formData.yy, mm: monthStr.value, itemcd: formData.itemcd
    });
    grid?.setData(res.data);
    vAlert('조회되었습니다.');
  } catch (e) { vAlertError('조회 실패'); }
}

function fetchDetail(row: any) {
  Object.assign(formData, { ...row, actkind: 'U0' });
  if (row.ym) {
    formData.yy = row.ym.substring(0, 4);
    formData.mm = Number(row.ym.substring(4, 6));
  }
}

async function save() {
  if (!formData.itemcd) return vAlertError('품목을 선택하세요.');
  if (formData.qty === 0 && formData.amt === 0) return vAlertError('수량 또는 금액을 입력하세요.');

  if (!confirm('기초 재고 정보를 저장하시겠습니까?')) return

  try {
    const payload = {
      ...formData,
      actkind: formData.actkind === 'U0' ? 'U0' : 'A0',
      mm: monthStr.value,
      userid: authStore.userid
    };
    await api.post('/hpba/HPBA_800U_STR', payload);
    vAlert('저장되었습니다.');
    search();
    initializeFormOnly();
  } catch (e) { vAlertError('저장 실패'); }
}

const handleOpenHelp = (type: string) => {
  if (type === 'ITEM') {
    openHelp('ITEM', (d) => {
      Object.assign(formData, { itemcd: d.itemcd, itemnm: d.itemnm, itsize: d.itsize, unit: d.unit });
    }, { codegbn: 'B' }); // 생산용 품목 필터
  }
}

const initializeFormOnly = () => {
  formData.actkind = 'S0';
  formData.itemcd = ''; formData.itemnm = ''; formData.itsize = ''; formData.unit = '';
  formData.qty = 0; formData.amt = 0;
}

const initialize = () => {
  resetForm(formData);
  Object.assign(formData, {
    cmpycd: authStore.cmpycd, actkind: 'S0',
    yy: today.substring(0, 4), mm: Number(today.substring(5, 7))
  });
  grid?.clearData();
}

onMounted(() => {
  const cur = new Date().getFullYear();
  for (let i = 0; i < 5; i++) yearOptions.value.push(String(cur - i));
  nextTick(initGrids);
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
.grid-container-right { border-bottom: 3px solid #005a9f !important; }
</style>
