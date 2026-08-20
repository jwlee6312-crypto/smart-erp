<!--
	=============================================================
	프로그램명	: 매출정산(일괄) (HSIO590U)
	작성일자	: 2025.03.11
	설명        : 매출정산 일괄 처리 (표준화 디자인 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-calculator me-2 text-primary" style="font-size: 18px;"></i>
        영업관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        매출관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">매출정산(일괄) (HSIO590U)</span>
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
                <col style="width: 100px;" /><col />
                <col style="width: 100px;" /><col />
                <col style="width: 100px;" /><col />
            </colgroup>
            <tbody>
              <tr>
                <th class="required text-center bg-light">판매부서</th>
                <td>
                  <div class="input-group input-group-sm" style="width: 250px;">
                    <input v-model="searchData.deptcd" type="text" class="form-control text-center bg-light" style="max-width: 70px;" readonly />
                    <input v-model="searchData.deptnm" type="text" class="form-control" placeholder="부서 선택" @keyup.enter="openDept" />
                    <button class="btn btn-outline-secondary" @click="openDept"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="required text-center bg-light">판매일자</th>
                <td>
                  <div class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                    <DateForm v-model:fromdt="searchData.fromdt" v-model:todt="searchData.todt" />
                  </div>
                </td>
                <th class="text-center bg-light">영업사원</th>
                <td>
                  <select v-model="searchData.salsemp" class="form-select form-select-sm" style="width: 150px;">
                    <option value="000">전체</option>
                    <option v-for="opt in empOptions" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                  </select>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [중간] 정산 발행 정보 (등록 영역) -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-header py-1 px-3 border-bottom d-flex align-items-center bg-white">
          <span class="fw-bold small text-primary"><i class="bi bi-pencil-square me-2"></i>정산 발행 정보</span>
        </div>
        <div class="card-body p-0 bg-white border-top">
          <table class="erp-table-dense" width="100%">
            <colgroup>
                <col style="width: 100px;" /><col />
                <col style="width: 100px;" /><col />
                <col style="width: 100px;" /><col />
            </colgroup>
            <tbody>
              <tr>
                <th class="required text-center bg-light">사업장</th>
                <td>
                  <select v-model="registerData.taxunit" class="form-select form-select-sm" style="width: 200px;">
                    <option v-for="opt in taxUnitOptions" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                  </select>
                </td>
                <th class="required text-center bg-light">유형</th>
                <td>
                  <select v-model="registerData.vattype" class="form-select form-select-sm" style="width: 150px;">
                    <option v-for="opt in vatTypeOptions" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                  </select>
                </td>
                <th class="required text-center bg-light">발행일</th>
                <td>
                  <input v-model="registerData.pubymd" type="date" class="form-control form-control-sm" style="width: 140px;" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 데이터 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column grid-container-full">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>정산 대상 내역</span>
          <div class="d-flex align-items-center gap-3">
            <div class="d-flex gap-2" v-if="activeItemCount > 0">
                <span class="badge bg-primary-subtle text-primary border border-primary-subtle px-2 py-1" style="font-size: 11px;">
                    선택: {{ activeItemCount }} 건
                </span>
                <span class="badge bg-dark-subtle text-dark border border-dark-subtle px-2 py-1" style="font-size: 11px;">
                    합계: {{ formatNumber(registerData.sumamt) }}
                </span>
            </div>
          </div>
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
import DateForm from '@/components/DateForm.vue'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { useCommonHelp } from '@/composables/useCommonHelp'
import { getDate } from '@/composables/useDate'

const authStore = useAuthStore()
const { firstDay, today } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps, openHelp } = useCommonHelp()

// 1. 상태 관리
const searchData = reactive({
  deptcd: authStore.deptcd, deptnm: authStore.deptnm,
  fromdt: firstDay, todt: today, salsemp: authStore.userid
})

const registerData = reactive({
  taxunit: '100', vattype: '010', pubymd: today,
  splamt: 0, vatamt: 0, sumamt: 0,
  clsymd: '', sclsym: ''
})

const gridElement = ref<HTMLElement | null>(null)
let grid: Tabulator | null = null
const activeItemCount = ref(0)

const empOptions = ref<any[]>([])
const taxUnitOptions = ref<any[]>([])
const vatTypeOptions = ref<any[]>([])

// 2. 그리드 초기화
const initGrid = () => {
  if (!gridElement.value) return
  grid = new Tabulator(gridElement.value, {
    layout: "fitColumns", height: "100%", placeholder: "조회된 내역이 없습니다.",
    columnDefaults: { headerSort: false, headerHozAlign: 'center', vertAlign: "middle" },
    selectable: true,
    columns: [
      { title: "", width: 40, hozAlign: "center", headerHozAlign: "center", formatter: "rowSelection", titleFormatter: "rowSelection", resizable: false },
      { title: "거래처", field: "custnm", minWidth: 200, cssClass: "fw-bold text-primary" },
      { title: "판매부서", field: "deptnm", width: 150 },
      { title: "수량", field: "ioqty", width: 100, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "공급가", field: "ioamt", width: 130, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "부가세", field: "iovat", width: 120, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "합계", field: "iosum", width: 140, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "fw-bold" }
    ]
  })

  grid.on("rowSelectionChanged", updateTotals)
}

const updateTotals = () => {
  if (!grid) return
  const data = grid.getSelectedData()
  activeItemCount.value = data.length
  registerData.splamt = data.reduce((acc, cur) => acc + (Number(cur.ioamt) || 0), 0)
  registerData.vatamt = data.reduce((acc, cur) => acc + (Number(cur.iovat) || 0), 0)
  registerData.sumamt = registerData.splamt + registerData.vatamt
}

// 3. 기능 구현
async function search() {
  if (!searchData.deptcd) return vAlertError('판매부서를 선택하세요.')
  grid?.clearData()
  try {
    const res = await api.post('/hsio/HSIO_590U_STR', {
      actkind: 'S0', cmpycd: authStore.cmpycd, iogbn: '200',
      fromdt: searchData.fromdt.replace(/-/g, ''), todt: searchData.todt.replace(/-/g, ''),
      deptcd: searchData.deptcd, saleuserid: searchData.salsemp === '000' ? '' : searchData.salsemp
    })
    const mapped = res.data.map((i: any) => {
        const ioqty = (Number(i.qty || 0)) - (Number(i.jqty || 0))
        const ioamt = (Number(i.amt || 0)) - (Number(i.jamt || 0))
        const iovat = (Number(i.vat || 0)) - (Number(i.jvat || 0))
        return { ...i, ioqty, ioamt, iovat, iosum: ioamt + iovat }
    })
    grid?.setData(mapped)
    grid?.selectRow() // 전체 선택
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

async function save() {
  const selected = grid?.getSelectedData()
  if (!selected?.length) return vAlertError('정산할 자료를 선택해 주십시오.')

  const pubymd = registerData.pubymd.replace(/-/g, '')
  if (registerData.clsymd && registerData.clsymd >= pubymd) {
    return vAlertError('회계정보가 마감이 되었습니다. 해당 정산일자로 작업할 수 없습니다.')
  }
  if (registerData.sclsym && registerData.sclsym >= pubymd.substring(0, 6)) {
    return vAlertError('영업정보가 마감이 되었습니다. 해당 정산일자로 작업할 수 없습니다.')
  }

  if (!confirm('정산 작업을 진행하시겠습니까?')) return

  try {
    const payload = {
      list: selected.map(item => ({
        actkind: 'U0', cmpycd: authStore.cmpycd, iogbn: '200',
        fromdt: searchData.fromdt.replace(/-/g, ''), todt: searchData.todt.replace(/-/g, ''),
        deptcd: item.deptcd, custcd: item.custcd,
        salsemp: searchData.salsemp === '000' ? '' : searchData.salsemp,
        taxunit: registerData.taxunit, vattype: registerData.vattype,
        jsanymd: pubymd,
        jsanamt: String(item.ioamt || 0), jsanvat: String(item.iovat || 0),
        updemp: authStore.userid
      }))
    }
    await api.post('/hsio/HSIO_590U_SAVE', payload)
    vAlert('정상으로 작업이 되었습니다.'); search()
  } catch (e: any) { vAlertError(e.response?.data?.message || '저장 중 오류 발생') }
}

function initialize() {
  resetForm(searchData)
  Object.assign(searchData, { deptcd: authStore.deptcd, deptnm: authStore.deptnm, fromdt: firstDay, todt: today, salsemp: authStore.userid })
  Object.assign(registerData, { taxunit: '100', vattype: '010', pubymd: today, splamt: 0, vatamt: 0, sumamt: 0 })
  grid?.clearData(); updateTotals()
}

const openDept = () => openHelp('DEPT', (d: any) => { searchData.deptcd = d.deptcd; searchData.deptnm = d.deptnm })

const formatNumber = (val: any) => new Intl.NumberFormat().format(Number(val) || 0)

onMounted(async () => {
  api.get('/hp00/HP00_000S_STR', { params: { gubun: 'cl', cmpycd: authStore.cmpycd } }).then(r => {
    if (r.data?.length) {
      const d = r.data[0]
      registerData.clsymd = String(d.clsymd || d.CLSYMD || Object.values(d)[0]).trim()
      registerData.sclsym = String(d.sclsym || d.SCLSYM || Object.values(d)[1]).trim()
    }
  })
  api.get('/ha00/HA00_00P_STR', { params: { gubun: 'SD', cmpycd: authStore.cmpycd } }).then(r => {
    if (r.data) empOptions.value = r.data.map((i: any) => ({ codecd: i.userid, codenm: i.usernm }))
  })
  api.post('/ha00/HA00_00P_STR', { gubun: 'SA', cmpycd: authStore.cmpycd }).then(r => {
    taxUnitOptions.value = (r.data || []).map((i:any)=>({codecd:String(i.taxunit||Object.values(i)[0]).trim(), codenm:String(i.unitnm||Object.values(i)[1]).trim()}))
    if(taxUnitOptions.value.length) registerData.taxunit = taxUnitOptions.value[0].codecd
  })
  api.post('/ha00/HA00_00P_STR', { gubun: 'E0', gbncd: '130', cmpycd: authStore.cmpycd }).then(r => {
    vatTypeOptions.value = (r.data || []).map((i:any)=>({codecd:String(i.codecd||Object.values(i)[0]).trim(), codenm:String(i.codenm||Object.values(i)[1]).trim()}))
  })

  nextTick(() => initGrid())
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
:deep(.tabulator-col[tabulator-field=""] .tabulator-col-content),
:deep(.tabulator-cell[tabulator-field=""]) {
  display: flex !important; align-items: center !important; justify-content: center !important; padding: 0 !important;
}
</style>
