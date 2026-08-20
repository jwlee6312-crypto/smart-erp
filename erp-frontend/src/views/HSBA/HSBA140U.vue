<!--
	=============================================================
	프로그램명	: 수출입거래처관리 (HSBA140U)
	작성일자	: 2025.02.24
	설명        : 수출입 거래처 관리 (HSOD100U 디자인 패턴 및 스크롤 최적화)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom bg-white py-2 px-3 sticky-top shadow-sm">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-globe me-2 text-primary" style="font-size: 18px;"></i>
        기본정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        수출입거래처관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">수출입거래처관리 (HSBA140U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-2">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchList">조회</button>
        <button class="btn-erp btn-save" @click="save">저장</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- 🔍 [상단] 조회 필터 영역 (HSOD100U 디자인 패턴 적용) -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
              <col style="width: 10%" />
              <col style="width: 40%" />
              <col style="width: 10%" />
              <col style="width: 40%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light">수출입구분</th>
                <td>
                  <select v-model="searchParams.iogbn" class="form-select w-50" @change="fetchList">
                    <option value="100">수입 거래처</option>
                    <option value="200">수출 거래처</option>
                  </select>
                </td>
                <th class="bg-light"></th>
                <td></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 🅰️ 상세 입력 폼 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-header py-1 px-3 border-bottom d-flex align-items-center justify-content-between" style="background-color: #f0f7ff !important;">
          <span class="fw-bold small text-primary"><i class="bi bi-pencil-square me-1"></i> 수출입 관리 정보</span>
        </div>
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense w-100">
            <colgroup>
              <col style="width: 100px;" /><col />
              <col style="width: 100px;" /><col />
              <col style="width: 100px;" /><col />
              <col style="width: 100px;" /><col />
            </colgroup>
            <tbody>
              <tr>
                <th class="required bg-light">수 출 입</th>
                <td>
                  <select v-model="formData.iogbn" class="form-select">
                    <option value="100">수입 거래처</option>
                    <option value="200">수출 거래처</option>
                  </select>
                </td>
                <th class="required bg-light">거 래 처</th>
                <td>
                  <div class="input-group input-group-sm">
                    <input v-model="formData.custcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
                    <input v-model="formData.custnm" type="text" class="form-control" placeholder="거래처 선택" readonly />
                    <button class="btn btn-outline-secondary px-2" @click="openHelp('CUST')" :disabled="formData.actkind === 'U0'"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="required bg-light">국&nbsp;&nbsp;&nbsp;&nbsp;가</th>
                <td>
                  <select v-model="formData.nacd" class="form-select">
                    <option value="">-- 선택 --</option>
                    <option v-for="opt in comboOptions.na" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                  </select>
                </td>
                <th class="required bg-light text-center">선 적 항</th>
                <td>
                  <select v-model="formData.shipport" class="form-select">
                    <option value="">-- 선택 --</option>
                    <option v-for="opt in comboOptions.ship" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                  </select>
                </td>
              </tr>
              <tr>
                <th class="required bg-light text-center">도 착 항</th>
                <td>
                  <select v-model="formData.arvport" class="form-select">
                    <option value="">-- 선택 --</option>
                    <option v-for="opt in comboOptions.arv" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                  </select>
                </td>
                <th class="required bg-light">통화단위</th>
                <td>
                  <select v-model="formData.currcd" class="form-select">
                    <option value="">-- 선택 --</option>
                    <option v-for="opt in comboOptions.curr" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                  </select>
                </td>
                <th class="required bg-light text-center">결제조건</th>
                <td>
                  <select v-model="formData.paycond" class="form-select">
                    <option value="">-- 선택 --</option>
                    <option v-for="opt in comboOptions.pay" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                  </select>
                </td>
                <th class="required bg-light text-center">가격조건</th>
                <td>
                  <select v-model="formData.pricond" class="form-select">
                    <option value="">-- 선택 --</option>
                    <option v-for="opt in comboOptions.pri" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                  </select>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 🅲 목록 영역 (스크롤 최적화) -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white grid-container-right">
        <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark flex-shrink-0">
          <i class="bi bi-list-ul me-1"></i> 수출입 거래처 내역 ({{ activeItemCount }} 건)
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="gridElement" class="tabulator-instance flex-grow-1"></div>
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
import Modal from '@/components/Modal.vue'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { useCommonHelp } from '@/composables/useCommonHelp'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps, openHelp: openCommonHelp } = useCommonHelp()

// [1] 데이터 모델링
const searchParams = reactive({ iogbn: '100' })
const formData = reactive<any>({
  actkind: 'A0', iogbn: '100', custcd: '', custnm: '', nacd: '', shipport: '', arvport: '', currcd: '', paycond: '', pricond: '', useyn: 'Y'
})

const comboOptions = reactive<any>({ na: [], ship: [], arv: [], curr: [], pay: [], pri: [] })
const gridElement = ref<HTMLElement | null>(null); let grid: Tabulator | null = null; const activeItemCount = ref(0)

// [2] 그리드 초기화
const initGrid = () => {
  if (!gridElement.value) return
  grid = new Tabulator(gridElement.value, {
    layout: "fitColumns", height: "100%", placeholder: "조회된 데이터가 없습니다.",
    pagination: "local", paginationSize: 20,
    columnDefaults: { headerSort: false, headerHozAlign: "center", vertAlign: "middle" },
    columns: [
      { title: "거래처", field: "custnm", minWidth: 200, hozAlign: "left", cssClass: "fw-bold text-primary" },
      { title: "국가", field: "nanm", width: 100 },
      { title: "선적항", field: "shipportnm", width: 120 },
      { title: "도착항", field: "arvportnm", width: 120 },
      { title: "통화단위", field: "currnm", width: 100 },
      { title: "결제조건", field: "paycondnm", width: 120 },
      { title: "가격조건", field: "pricondnm", width: 120 }
    ]
  })

  grid.on("rowClick", (e, row) => {
    Object.assign(formData, row.getData()); formData.actkind = 'U0'; formData.useyn = 'Y';
  })
}

// [3] 기능 구현
async function fetchOptions() {
  const getOpt = (cd: string) => api.post('/hs00/HS00_000S_STR', { gubun: 'E2', cmpycd: authStore.cmpycd, gbncd: cd })
  try {
    const [r1, r2, r3, r4, r5, r6] = await Promise.all([
      getOpt('305'), getOpt('308'), getOpt('309'), getOpt('310'), getOpt('312'), getOpt('314')
    ])
    const map = (r: any) => r.data.map((i: any) => ({ codecd: String(i.code || i.codecd || '').trim(), codenm: String(i.cdnm || i.codenm || '').trim() }))
    comboOptions.na = map(r1); comboOptions.ship = map(r2); comboOptions.arv = map(r3);
    comboOptions.curr = map(r4); comboOptions.pay = map(r5); comboOptions.pri = map(r6);
  } catch (e) {}
}

async function fetchList() {
  try {
    const res = await api.post('/hsba/HSBA_140U_STR', { actkind: 'S0', cmpycd: authStore.cmpycd, iogbn: searchParams.iogbn })
    grid?.setData(res.data); activeItemCount.value = res.data.length
  } catch (e) { vAlertError('조회 실패') }
}

async function save() {
  if (!formData.custcd) return vAlertError('거래처를 선택하세요.')
  if (!confirm('저장하시겠습니까?')) return
  try {
    const res = await api.post('/hsba/HSBA_140U_STR', { ...formData, cmpycd: authStore.cmpycd, userid: authStore.userid })
    if (res.data?.[0]?.ERRYN === 'Y') vAlertError(res.data[0].MSG)
    else { vAlert('정상적으로 저장되었습니다.'); fetchList(); initialize() }
  } catch (e) { vAlertError('저장 실패') }
}

function initialize() {
  const currentGbn = searchParams.iogbn; resetForm(formData)
  Object.assign(formData, { actkind: 'A0', iogbn: currentGbn, useyn: 'Y' })
}

function openHelp(type: string) {
  if (type === 'CUST') {
    openCommonHelp('CUST', (d) => { formData.custcd = d.custcd; formData.custnm = d.custnm });
  }
}

onMounted(async () => { await fetchOptions(); nextTick(() => { initGrid(); fetchList() }) })
</script>

<style scoped>
.main-content-wrapper { padding-bottom: 0vh !important; }
.erp-table-dense th, .erp-table-dense td {
  height: 32px !important; padding: 0 8px !important; font-size: 12px; vertical-align: middle; border: 1px solid #dee2e6;
}
.erp-table-dense .form-control, .erp-table-dense .form-select, .erp-table-dense .btn {
  height: 26px !important; font-size: 12px !important; border-radius: 2px;
}
.erp-table-dense th { font-weight: 600; color: #495057; }
.grid-container-right { border-bottom: 3px solid #005a9f !important; }
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
