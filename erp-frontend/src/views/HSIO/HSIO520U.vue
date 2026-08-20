<!--
	=============================================================
	프로그램명	: 매출정산취소 (HSIO520U)
	작성일자	: 2025.03.11
	설명        : 매출정산 내역 조회 및 취소 처리 (표준화 디자인 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-x-circle-fill me-2 text-danger" style="font-size: 18px;"></i>
        영업관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        매출관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">매출정산취소 (HSIO520U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchCustomerList">조회</button>
        <button class="btn-erp btn-save" @click="cancelSettlement" :disabled="!selectedCust.custcd">정산취소</button>
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
                <th class="required text-center bg-light">정산일자</th>
                <td>
                  <div class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                    <DateForm v-model:fromdt="searchData.fromdt" v-model:todt="searchData.todt" />
                  </div>
                </td>
                <th class="text-center bg-light">거&nbsp;&nbsp;래&nbsp;&nbsp;처</th>
                <td>
                  <div class="input-group input-group-sm" style="width: 250px;">
                    <input v-model="searchData.custcd" type="text" class="form-control text-center bg-light" style="max-width: 80px;" readonly />
                    <input v-model="searchData.custnm" type="text" class="form-control" placeholder="거래처 선택" @keyup.enter="openCust" />
                    <button class="btn btn-outline-secondary" @click="openCust"><i class="bi bi-search"></i></button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 메인 작업 영역 -->
      <div class="d-flex flex-grow-1 gap-2 overflow-hidden" style="min-height: 0;">

        <!-- ⬅️ 좌측: 정산 대상 거래처 -->
        <div class="card border shadow-sm d-flex flex-column overflow-hidden grid-container-left" style="width: 300px; min-width: 300px;">
          <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark">
            <i class="bi bi-list-ul me-2 text-primary"></i>정산 대상 상호
          </div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="custGridElement" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>

        <!-- ➡️ 우측: 정산 내역 및 취소 -->
        <div class="flex-grow-1 d-flex flex-column gap-2 overflow-hidden">
          <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column grid-container-right">
            <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
              <span class="fw-bold small text-dark">
                <i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>정산 내역 ({{ selectedCust.custnm || '거래처를 선택하세요' }})
              </span>
              <span class="badge bg-primary-subtle text-primary border border-primary-subtle px-2 py-1" v-if="activeItemCount > 0" style="font-size: 11px;">
                총 {{ activeItemCount }} 건
              </span>
            </div>
            <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
              <div ref="settleGridElement" class="tabulator-instance flex-grow-1"></div>
            </div>
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
  fromdt: firstDay, todt: today,
  custcd: '', custnm: ''
})

const closingInfo = reactive({ clsymd: '', sclsym: '' })
const selectedCust = reactive({ custcd: '', custnm: '' })

const custGridElement = ref<HTMLElement | null>(null)
const settleGridElement = ref<HTMLElement | null>(null)
let custGrid: Tabulator | null = null
let settleGrid: Tabulator | null = null
const activeItemCount = ref(0)

const initGrids = () => {
  custGrid = new Tabulator(custGridElement.value!, {
    layout: "fitColumns", height: "100%", placeholder: "데이터 없음",
    columns: [
      { title: "No", formatter: "rownum", width: 40, hozAlign: "center", headerSort: false },
      { title: "거래처 상호", field: "custnm", headerSort: false, cssClass: "fw-bold text-primary" }
    ],
  });
  custGrid.on("rowClick", (e, row) => {
    const data = row.getData()
    selectedCust.custcd = data.custcd
    selectedCust.custnm = data.custnm
    fetchSettlementList()
  })

  settleGrid = new Tabulator(settleGridElement.value!, {
    layout: "fitColumns", height: "100%", placeholder: "거래처를 선택하세요.",
    columnDefaults: { headerSort: false, headerHozAlign: 'center', vertAlign: "middle" },
    selectable: true,
    selectableCheck: (row) => checkCanCancel(row.getData()) === true,
    columns: [
      { title: "", width: 40, hozAlign: "center", headerHozAlign: "center", formatter: "rowSelection", titleFormatter: "rowSelection", resizable: false },
      { title: "발행일", field: "jsanymd", width: 120, hozAlign: "center", formatter: (c) => formatDateString(c.getValue(), '-') },
      { title: "정산부서", field: "deptnm", minWidth: 150, hozAlign: "left", cssClass: "fw-bold" },
      { title: "사업장", field: "unitnm", hozAlign: "center", width: 120 },
      { title: "유형", field: "vattypenm", hozAlign: "center", width: 120 },
      { title: "공급가", field: "spyamt", width: 130, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "부가세", field: "vatamt", width: 120, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "합계", field: "jsansum", width: 140, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "fw-bold text-primary" }
    ]
  });

  settleGrid.on("rowClick", (e, row) => {
    const checkResult = checkCanCancel(row.getData());
    if (checkResult !== true) vAlertError(checkResult);
  })
}

const checkCanCancel = (row: any) => {
  const jsanymd = row.jsanymd || ''
  const jsanyym = String(jsanymd).substring(0, 6)
  if (row.slipymd && row.slipymd > "00000000") return "전표를 발행한 자료입니다."
  if (row.prtymd && row.prtymd > "00000000") return "세금계산서를 발행한 자료입니다."
  if (closingInfo.sclsym && jsanyym <= closingInfo.sclsym) return "영업정보가 마감되었습니다."
  if (closingInfo.clsymd && jsanymd <= closingInfo.clsymd) return "회계정보가 마감되었습니다."
  return true
}

async function fetchCustomerList() {
  if (!searchData.deptcd) return vAlertError('판매부서를 선택하세요.')
  custGrid?.clearData(); settleGrid?.clearData();
  selectedCust.custcd = ''; selectedCust.custnm = ''; activeItemCount.value = 0;

  try {
    const res = await api.post('/hsio/HSIO_520U_STR', {
      actkind: 'S1', cmpycd: authStore.cmpycd, iogbn: '200',
      fromdt: searchData.fromdt.replace(/-/g, ''), todt: searchData.todt.replace(/-/g, ''),
      deptcd: searchData.deptcd, custcd: searchData.custcd
    })
    custGrid?.setData(res.data)
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('거래처 목록 조회 실패') }
}

async function fetchSettlementList() {
  settleGrid?.clearData(); activeItemCount.value = 0;
  try {
    const res = await api.post('/hsio/HSIO_520U_STR', {
      actkind: 'S0', cmpycd: authStore.cmpycd, iogbn: '200',
      fromdt: searchData.fromdt.replace(/-/g, ''), todt: searchData.todt.replace(/-/g, ''),
      deptcd: searchData.deptcd, custcd: selectedCust.custcd
    })
    const mapped = res.data.map((i: any) => ({ ...i, jsansum: Number(i.spyamt || 0) + Number(i.vatamt || 0) }))
    settleGrid?.setData(mapped); activeItemCount.value = mapped.length;
  } catch (e) { vAlertError('정산 내역 조회 실패') }
}

async function cancelSettlement() {
  const selected = settleGrid?.getSelectedData()
  if (!selected?.length) return vAlertError('취소할 정산 자료를 선택하세요.')
  if (!confirm('선택한 정산 자료를 취소하시겠습니까?')) return

  try {
    for (const item of selected) {
      const res = await api.post('/hsio/HSIO_520U_STR', {
        actkind: 'D0', cmpycd: authStore.cmpycd, iogbn: '200',
        fromdt: searchData.fromdt.replace(/-/g, ''), todt: searchData.todt.replace(/-/g, ''),
        deptcd: searchData.deptcd, custcd: selectedCust.custcd,
        jsanym: item.jsanym, jsanno: item.jsanno, userid: authStore.userid
      })
      const resData = res.data?.[0]
      if (resData && (resData.result === 'Y')) {
        vAlertError(resData.msg || '취소 처리 중 업무 오류가 발생했습니다.'); return;
      }
    }
    vAlert('정상적으로 취소되었습니다.'); fetchCustomerList();
  } catch (e) { vAlertError('취소 처리 실패') }
}

const openDept = () => openHelp('DEPT', (d: any) => { searchData.deptcd = d.deptcd; searchData.deptnm = d.deptnm })
const openCust = () => openHelp('CUST', (d: any) => { searchData.custcd = d.custcd; searchData.custnm = d.custnm }, { gubun: 'c4' })

const initialize = () => {
  resetForm(searchData);
  Object.assign(searchData, { deptcd: authStore.deptcd, deptnm: authStore.deptnm, fromdt: firstDay, todt: today, custcd: '', custnm: '' });
  selectedCust.custcd = ''; selectedCust.custnm = ''; activeItemCount.value = 0;
  custGrid?.clearData(); settleGrid?.clearData();
}

const formatDateString = (v: any, sep: string) => v && v.length === 8 ? `${v.substring(0, 4)}${sep}${v.substring(4, 6)}${sep}${v.substring(6, 8)}` : v

onMounted(() => {
  api.get('/hp00/HP00_000S_STR', { params: { gubun: 'cl', cmpycd: authStore.cmpycd } }).then(r => {
    if (r.data?.length) {
      const d = r.data[0]
      closingInfo.clsymd = String(d.clsymd || d.CLSYMD || Object.values(d)[0]).trim()
      closingInfo.sclsym = String(d.sclsym || d.SCLSYM || Object.values(d)[1]).trim()
    }
  })
  nextTick(initGrids)
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
:deep(.tabulator-col[tabulator-field=""] .tabulator-col-content),
:deep(.tabulator-cell[tabulator-field=""]) {
  display: flex !important; align-items: center !important; justify-content: center !important; padding: 0 !important;
}
</style>
