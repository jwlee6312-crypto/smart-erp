<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-globe2 me-2 text-primary" style="font-size: 18px;"></i>
        기본정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        <span class="text-primary fw-bolder">수출 거래처관리 (HSBA280U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-save" @click="save">저장</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-y-auto p-2 d-flex flex-column gap-2">
      <!-- 🅰️ 조회 조건 영역 -->
      <div class="card border shadow-sm flex-shrink-0">
        <div class="card-body p-1 bg-light-subtle">
          <div class="d-flex align-items-center gap-3 px-2">
            <div class="d-flex align-items-center gap-2">
              <span class="fw-bold small">수출입구분</span>
              <select v-model="searchData.iogbn" class="form-select form-select-sm" style="width: 150px;" @change="search">
                <option value="200">수출 거래처</option>
                <option value="100">수입 거래처</option>
              </select>
            </div>
          </div>
        </div>
      </div>

      <!-- 🅱️ 마스터 정보 입력 영역: 콤보박스 적용 -->
      <div class="card border shadow-sm overflow-hidden flex-shrink-0">
        <div class="card-header py-1 px-3 border-bottom d-flex align-items-center" style="background-color: #f0f7ff !important;">
          <span class="fw-bold small text-primary"><i class="bi bi-pencil-square me-1"></i> 거래처 정보 입력</span>
          <span v-if="masterData.actkind === 'U0'" class="badge bg-warning text-dark ms-2" style="font-size: 10px;">수정 모드</span>
          <span v-else class="badge bg-primary ms-2" style="font-size: 10px;">신규 등록</span>
        </div>
        <div class="card-body p-0">
          <table class="erp-table-full border-0">
            <colgroup>
              <col style="width: 100px;" /><col />
              <col style="width: 100px;" /><col />
              <col style="width: 100px;" /><col />
            </colgroup>
            <tbody>
              <tr>
                <th class="required">수 출 입</th>
                <td>
                  <select v-model="masterData.iogbn" class="form-select form-select-sm">
                    <option value="200">수출 거래처</option>
                    <option value="100">수입 거래처</option>
                  </select>
                </td>
                <th class="required">거 래 처</th>
                <td>
                  <div class="input-group input-group-sm">
                    <input v-model="masterData.custcd" type="text" class="form-control text-center fw-bold" style="max-width: 80px;" readonly />
                    <input v-model="masterData.custnm" type="text" class="form-control bg-light" placeholder="거래처 선택..." readonly />
                    <button class="btn btn-outline-secondary" @click="openHelp('CUST')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="required">국가코드</th>
                <td>
                  <select v-model="masterData.nacd" class="form-select form-select-sm">
                    <option value="">-- 국가 선택 --</option>
                    <option v-for="opt in comboOptions.na" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                  </select>
                </td>
              </tr>
              <tr>
                <th class="required">선 적 항</th>
                <td>
                  <select v-model="masterData.shipport" class="form-select form-select-sm">
                    <option value="">-- 선적항 선택 --</option>
                    <option v-for="opt in comboOptions.ship" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                  </select>
                </td>
                <th class="required">도 착 항</th>
                <td>
                  <select v-model="masterData.arvport" class="form-select form-select-sm">
                    <option value="">-- 도착항 선택 --</option>
                    <option v-for="opt in comboOptions.arv" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                  </select>
                </td>
                <th class="required">통화단위</th>
                <td>
                  <select v-model="masterData.currcd" class="form-select form-select-sm">
                    <option value="">-- 통화 선택 --</option>
                    <option v-for="opt in comboOptions.curr" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                  </select>
                </td>
              </tr>
              <tr>
                <th class="required">결제조건</th>
                <td>
                  <select v-model="masterData.paycond" class="form-select form-select-sm">
                    <option value="">-- 결제조건 선택 --</option>
                    <option v-for="opt in comboOptions.pay" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                  </select>
                </td>
                <th class="required">가격조건</th>
                <td>
                  <select v-model="masterData.pricond" class="form-select form-select-sm">
                    <option value="">-- 가격조건 선택 --</option>
                    <option v-for="opt in comboOptions.pri" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                  </select>
                </td>
                <th>사용여부</th>
                <td>
                  <div class="form-check form-switch m-0">
                    <input v-model="masterData.useyn" class="form-check-input" type="checkbox" id="useYn280" true-value="Y" false-value="N">
                    <label class="form-check-label small fw-bold" for="useYn280">사용</label>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Ⓒ 데이터 그리드 영역: 높이 표준화 400px -->
      <div class="card border shadow-sm erp-main-grid overflow-hidden d-flex flex-column">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-1"></i> 수출 거래처 목록 ({{ activeItemCount }} 건)</span>
          <span class="text-muted" style="font-size: 11px;">※ 행 클릭 시 상세 정보를 수정할 수 있습니다.</span>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="gridElement" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>
    </div>
  </div>

  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />
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
import type { ModalProps } from '@/types/modal'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

// 1. 상태 관리
const searchData = reactive({ iogbn: '200' })
const masterData = reactive<any>({
  actkind: 'A0', cmpycd: authStore.cmpycd, iogbn: '200', custcd: '', custnm: '', nacd: '', shipport: '', arvport: '', currcd: '', paycond: '', pricond: '', useyn: 'Y'
})

const comboOptions = reactive<any>({ na: [], ship: [], arv: [], curr: [], pay: [], pri: [] })
const gridElement = ref<HTMLElement | null>(null); let grid: Tabulator | null = null; const activeItemCount = ref(0)

// 2. 그리드 초기화
const initGrid = () => {
  if (!gridElement.value) return
  grid = new Tabulator(gridElement.value, {
    layout: "fitColumns", height: "100%", placeholder: "조회된 데이터가 없습니다.",
    columnDefaults: { headerSort: false, headerHozAlign: "center" },
    columns: [
      { title: "수출거래처", field: "custnm", minWidth: 200, hozAlign: "left", cssClass: "fw-bold" },
      { title: "국가", field: "nanm", width: 100 },
      { title: "선적항", field: "shipportnm", width: 120 },
      { title: "도착항", field: "arvportnm", width: 120 },
      { title: "통화단위", field: "currnm", width: 100 },
      { title: "결제조건", field: "paycondnm", width: 120 },
      { title: "가격조건", field: "pricondnm", width: 120 },
      {
        title: "사용", field: "useyn", width: 60, hozAlign: "center",
        formatter: (c) => c.getValue() === 'Y' ? 'O' : 'X'
      }
    ]
  })

  grid.on("rowClick", (e, row) => {
    const data = row.getData()
    const cleaned: any = {}
    Object.keys(data).forEach(k => cleaned[k] = typeof data[k] === 'string' ? data[k].trim() : data[k])
    Object.assign(masterData, cleaned); masterData.actkind = 'U0'
  })
}

// 3. 기능 구현
async function fetchOptions() {
  const getOpt = (cd: string) => api.post('/hs00/HS00_000S_STR', { gubun: 'E2', cmpycd: authStore.cmpycd, gbncd: cd })
  try {
    const [r1, r2, r3, r4, r5, r6] = await Promise.all([
      getOpt('305'), getOpt('318'), getOpt('319'), getOpt('310'), getOpt('312'), getOpt('314')
    ])
    const map = (r: any) => r.data.map((i: any) => ({ codecd: String(i.code || i.codecd || '').trim(), codenm: String(i.cdnm || i.codenm || '').trim() }))
    comboOptions.na = map(r1); comboOptions.ship = map(r2); comboOptions.arv = map(r3);
    comboOptions.curr = map(r4); comboOptions.pay = map(r5); comboOptions.pri = map(r6);
  } catch (e) {}
}

async function search() {
  try {
    const res = await api.post('/hsba/HSBA_280U_STR', { actkind: 'S0', cmpycd: authStore.cmpycd, iogbn: searchData.iogbn })
    grid?.setData(res.data); activeItemCount.value = res.data.length
  } catch (e) { vAlertError('조회 실패') }
}

async function save() {
  if (!masterData.custcd) return vAlertError('거래처를 선택하세요.')
  if (!confirm('저장하시겠습니까?')) return
  try {
    const res = await api.post('/hsba/HSBA_280U_STR', { ...masterData, userid: authStore.userid })
    vAlert('정상적으로 저장되었습니다.'); search(); initialize()
  } catch (e) { vAlertError('저장 중 오류 발생') }
}

function initialize() {
  const currentIo = searchData.iogbn; resetForm(masterData)
  Object.assign(masterData, { actkind: 'A0', cmpycd: authStore.cmpycd, iogbn: currentIo, useyn: 'Y' })
}

const modalVisible = ref(false); const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })
function openHelp(type: string) {
  if (type === 'CUST') {
    Object.assign(modalProps, { title: '거래처 선택', path: '/ha00/HA00_00P_STR', defaultField: 'custnm', data: { gubun: 'C4', cmpycd: authStore.cmpycd }, columns: [{ title: '코드', field: 'custcd', width: 80 }, { title: '거래처명', field: 'custnm', width: 200 }], onConfirm: (d: any) => { masterData.custcd = d.custcd; masterData.custnm = d.custnm } })
    modalVisible.value = true
  }
}

onMounted(async () => { await fetchOptions(); nextTick(() => { initGrid(); search() }) })
</script>

