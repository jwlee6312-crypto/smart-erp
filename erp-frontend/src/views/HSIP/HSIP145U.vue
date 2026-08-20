<!--
	=============================================================
	프로그램명	: 외부수입전표 발행 (HSIP145U)
	작성일자	: 2025.03.14 (ASP 로직 완전 이식)
	설명        : 외부회계시스템 연동을 위한 비용 정산 및 전표 생성
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom bg-white py-2 px-3 sticky-top shadow-sm">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-file-earmark-arrow-up-fill me-2 text-primary" style="font-size: 18px;"></i>
        수입관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">외부수입전표발행 (HSIP145U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-2">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchList">조회</button>
        <button class="btn-erp btn-save" @click="save">저장(전표발행)</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- 🔍 [상단] 조회 필터 영역 (부서, 발생일자) -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
              <col style="width: 10%" /><col style="width: 40%" />
              <col style="width: 10%" /><col style="width: 40%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light">부서</th>
                <td>
                  <div class="input-group input-group-sm w-75">
                    <input v-model="searchForm.deptcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
                    <input v-model="searchForm.deptnm" type="text" class="form-control" placeholder="부서 선택" readonly />
                    <button class="btn btn-outline-secondary px-2" @click="openHelp('S_DEPT')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="text-center bg-light">발생일자</th>
                <td class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                  <DateForm v-model:fromdt="searchForm.ioymdfr" v-model:todt="searchForm.ioymdto" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 📊 3. 수입비용 목록 그리드 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white grid-container-right">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark d-flex align-items-center">
            <i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i> 수입비용 내역
          </span>
          <div class="d-flex align-items-center gap-3">
            <span class="badge bg-primary-subtle text-primary border border-primary-subtle px-2">선택 합계: {{ formatNumber(totalSelectedAmt) }}</span>
            <span class="text-muted" style="font-size: 11px;">※ 행 선택 후 거래처와 부가세를 확인하세요.</span>
          </div>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="mainGridRef" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>
    </div>
  </div>

  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, nextTick } from 'vue'
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
import DateForm from '@/components/DateForm.vue'

const authStore = useAuthStore()
const { firstDay, today } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps, openHelp: openCommonHelp } = useCommonHelp()

const searchForm = reactive({
  deptcd: authStore.deptcd, deptnm: authStore.deptnm,
  ioymdfr: firstDay, ioymdto: today
})

const formData = reactive<any>({
  pubymd: today
})

const totalSelectedAmt = ref(0)
const mainGridRef = ref<HTMLDivElement | null>(null); let mainGrid: Tabulator | null = null

const fetchList = async () => {
  try {
    const params = {
      actkind: 'S0',
      cmpycd: authStore.cmpycd,
      deptcd: searchForm.deptcd,
      fromdt: searchForm.ioymdfr.replace(/-/g, ''),
      todt: searchForm.ioymdto.replace(/-/g, '')
    }
    const res = await api.post('/hsip/HSIP_145U_STR', params)
    const data = (res.data || []).map((item: any) => {
      // 💡 지시사항: 전표일자가 있으면 발행완료(Y), 없으면 미발행(N)
      item.SLIPYN = (item.slipymd && item.slipymd > '00000000') ? 'Y' : 'N'

      // 물대(200)가 아니면 거래처를 비워서 직접 입력하게 함
      if (item.costcd !== '200') {
        item.custcd = ''
        item.custnm = ''
        // 부가세 10% 자동 계산
        if (!item.vatamt || Number(item.vatamt) === 0) {
          item.vatamt = Math.floor(Number(item.costamt || 0) * 0.1)
        }
      }
      return item
    })
    mainGrid?.setData(data)
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

const save = async () => {
  const selected = mainGrid?.getSelectedData() || []
  if (selected.length === 0) return vAlertError('처리할 항목을 선택하세요.')

  // ASP 상식적 검증
  for(const item of selected) {
    if(!item.custcd) return vAlertError(`[${item.costnm}] 항목의 거래처가 없습니다.`)
    if(Number(item.costamt || 0) === 0) return vAlertError(`[${item.costnm}] 항목의 금액을 확인하세요.`)
    if(item.costcd !== '200' && Number(item.vatamt || 0) === 0) return vAlertError(`[${item.costnm}] 항목의 부가세를 입력하세요.`)
  }

  if (!confirm(`${selected.length}건의 전표를 일괄 발행하시겠습니까?`)) return

  const payload = {
    pubymd: formData.pubymd,
    fromdt: searchForm.ioymdfr,
    todt: searchForm.ioymdto,
    items: selected.map(item => ({
      costcd: item.costcd || item.COSTCD,
      docno: item.docno || item.DOCNO,
      crowno: item.crowno || item.CROWNO,
      fileno: item.fileno || item.FILENO,
      costamt: item.costamt || item.COSTAMT,
      vatamt: item.vatamt || item.VATAMT,
      custcd: item.custcd || item.CUSTCD,
      bigo: item.bigo || item.BIGO,
      deptcd: item.deptcd || item.DEPTCD,
      taxunit: '100',
      vattype: '010'
    }))
  }

  try {
    const res = await api.post('/hsip/HSIP_145U_SAVE', payload)
    if (res.data) {
      vAlert('정상적으로 전표 발행이 완료되었습니다.')
      fetchList()
    }
  } catch (e: any) {
    vAlertError('저장 실패: ' + (e.response?.data?.message || e.message))
  }
}

const formatNumber = (val: any) => Number(val || 0).toLocaleString()

const initialize = () => {
  resetForm(searchForm); searchForm.ioymdfr = firstDay; searchForm.ioymdto = today;
  searchForm.deptcd = authStore.deptcd; searchForm.deptnm = authStore.deptnm;
  formData.pubymd = today; mainGrid?.clearData(); totalSelectedAmt.value = 0;
}

function openHelp(type: string, row?: any) {
  if (type === 'S_DEPT') {
    openCommonHelp('DEPT', (d: any) => { searchForm.deptcd = d.deptcd; searchForm.deptnm = d.deptnm })
  } else if (type === 'CUST') {
    openCommonHelp('CUST', (d: any) => {
      row.update({ custcd: d.custcd, custnm: d.custnm })
    }, { gbncd: '010' })
  }
}

onMounted(() => {
  if (mainGridRef.value) {
    mainGrid = new Tabulator(mainGridRef.value, {
      layout: 'fitColumns', height: '100%', selectable: true,
      columnDefaults: { headerSort: false, headerHozAlign: "center", hozAlign: "center", vertAlign: "middle" },
      columns: [
        { title: "선택", formatter: "rowSelection", titleFormatter: "rowSelection", width: 50 },
        { title: "PO No.", field: "fileno", width: 130, cssClass: "fw-bold text-primary" },
        { title: "비용종류", field: "costnm", width: 120, hozAlign: "left" },
        { title: "발생일", field: "pubymd", width: 100, formatter: (c) => {
            const v = c.getValue(); return v && v.length === 8 ? `${v.substring(2,4)}.${v.substring(4,6)}.${v.substring(6,8)}` : v;
        }},
        { title: "적요", field: "bigo", minWidth: 200, widthGrow: 1, hozAlign: "left" },
        { title: "비용금액", field: "costamt", width: 110, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
        {
          title: "발행처", field: "custnm", width: 180, hozAlign: "left",
          formatter: (cell) => {
            return `<div class='d-flex justify-content-between w-100'><span>${cell.getValue() || ''}</span><i class='bi bi-search text-primary'></i></div>`
          },
          cellClick: (e, cell) => openHelp('CUST', cell.getRow())
        },
        { title: "부가세", field: "vatamt", width: 100, hozAlign: "right", editor: "number", formatter: "money", formatterParams: { precision: 0 }, cssClass: "bg-yellow" },
        { title: "전표번호", field: "slipno", width: 120, formatter: (c) => {
            const d = c.getData(); return d.slipymd ? `${d.slipymd}-${d.slipno}` : '';
        }}
      ]
    })
    mainGrid.on('rowSelectionChanged', (data) => {
      totalSelectedAmt.value = data.reduce((acc, cur: any) => acc + (Number(cur.costamt) || 0), 0)
    })
  }
  fetchList()
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
.bg-yellow { background-color: #fffde7 !important; }
</style>
