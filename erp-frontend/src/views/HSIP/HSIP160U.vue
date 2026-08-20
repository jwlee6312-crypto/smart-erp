<!--
	=============================================================
	프로그램명	: 수입원가생성 (비용배부작업)
	작성일자	: 2025.02.24
	설명        : ASP 원본 로직 반영 및 좌우 분할 레이아웃 적용
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom bg-white py-2 px-3 sticky-top shadow-sm">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-calculator me-2 text-primary" style="font-size: 18px;"></i>
        수입관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">수입원가생성 (HSIP160U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-2">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchList">조회</button>
        <button class="btn-erp btn-save" @click="handleProcess">
          {{ searchForm.divyn === 'N' ? '비용배부' : '배부취소' }}
        </button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- 🔍 [상단] 조회 필터 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
              <col style="width: 10%" /><col style="width: 25%" />
              <col style="width: 10%" /><col style="width: 25%" />
              <col style="width: 30%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light">발주부서</th>
                <td>
                  <div class="input-group input-group-sm">
                    <input v-model="searchForm.deptcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
                    <input v-model="searchForm.deptnm" type="text" class="form-control" placeholder="부서 선택" readonly />
                    <button class="btn btn-outline-secondary px-2" @click="openHelp('DEPT')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="text-center bg-light">통관일자</th>
                <td class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                  <DateForm v-model:fromdt="searchForm.fromdt" v-model:todt="searchForm.todt" />
                </td>
                <td class="text-center border-0">
                  <div class="form-check form-check-inline mb-0">
                    <input class="form-check-input" type="radio" v-model="searchForm.divyn" value="N" id="divynN" />
                    <label class="form-check-label small" for="divynN">미배부</label>
                  </div>
                  <div class="form-check form-check-inline mb-0">
                    <input class="form-check-input" type="radio" v-model="searchForm.divyn" value="Y" id="divynY" />
                    <label class="form-check-label small" for="divynY">배부</label>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 좌우 분할 레이아웃 -->
      <div class="flex-grow-1 d-flex gap-2 overflow-hidden" style="min-height: 0;">

        <!-- ⬅️ 좌측 그리드: 배부 대상 목록 -->
        <div class="card border shadow-sm d-flex flex-column overflow-hidden bg-white" style="width: 1050px; min-width: 1050px;">
          <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
            <span class="fw-bold small text-dark"><i class="bi bi-list-check me-2 text-primary"></i> 배부 대상 목록</span>
          </div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="masterGridRef" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>

        <!-- ➡️ 우측 그리드: 품목 정보 -->
        <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white">
          <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark d-flex justify-content-between align-items-center">
            <span><i class="bi bi-box-seam me-2 text-primary"></i> 품목 및 비용 상세 정보</span>
          </div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="detailGridRef" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>

      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { getDate } from '@/composables/useDate'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import DateForm from '@/components/DateForm.vue'

const authStore = useAuthStore()
const { firstDay, today } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

// 1. 상태 정의
const searchForm = reactive({
  deptcd: authStore.deptcd,
  deptnm: authStore.deptnm,
  fromdt: firstDay,
  todt: today,
  divyn: 'N'
})

const modalVisible = ref(false)
const modalProps = reactive<any>({
  title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table'
})

const masterGridRef = ref<HTMLDivElement | null>(null); let masterGrid: Tabulator | null = null
const detailGridRef = ref<HTMLDivElement | null>(null); let detailGrid: Tabulator | null = null

// 2. 조회 로직
const fetchList = async () => {
  try {
    const res = await api.post('/hsip/HSIP_160U_STR', {
      actkind: 'S0',
      cmpycd: authStore.cmpycd,
      deptcd: searchForm.deptcd,
      fromdt: searchForm.fromdt.replace(/-/g, ''),
      todt: searchForm.todt.replace(/-/g, ''),
      divyn: searchForm.divyn
    })
    masterGrid?.setData(res.data || [])
    detailGrid?.clearData()
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('목록 조회 실패') }
}

const fetchDetail = async (row: any) => {
  const d = row.getData()
  try {
    const res = await api.post('/hsip/HSIP_161S_STR', {
      actkind: 'S0',
      cmpycd: authStore.cmpycd,
      fileno: d.fileno,
      shipseq: d.shipseq,
      passseq: d.passseq,
      divyn: searchForm.divyn,
      itemcd: ''
    })
    detailGrid?.setData(res.data || [])
  } catch (e) { vAlertError('상세 내역 조회 실패') }
}

// 3. 처리 로직
const handleProcess = async () => {
  const selectedRows = masterGrid?.getSelectedData() || []
  if (selectedRows.length === 0) return vAlertError('대상을 선택하시기 바랍니다.')

  const msg = searchForm.divyn === 'N' ? '수입비용 배부작업을 하시겠습니까?' : '수입비용 배부작업을 취소 하시겠습니까?'
  if (!confirm(msg)) return

  try {
    for (const row of selectedRows) {
      const res = await api.post('/hsip/HSIP_160U_STR', {
        actkind: 'U0',
        cmpycd: authStore.cmpycd,
        deptcd: searchForm.deptcd,
        fromdt: searchForm.fromdt.replace(/-/g, ''),
        todt: searchForm.todt.replace(/-/g, ''),
        divyn: searchForm.divyn,
        fileno: row.fileno,
        shipseq: row.shipseq,
        passseq: row.passseq,
        updemp: authStore.userid
      })
      if (res.data && res.data[0] && res.data[0].res === 'Y') {
        throw new Error(res.data[0].msg || '처리 중 오류가 발생했습니다.')
      }
    }
    vAlert('정상적으로 작업이 완료되었습니다.')
    fetchList()
  } catch (e: any) { vAlertError(e.message || '처리 중 오류 발생') }
}

// 4. 도움창
const openHelp = (type: string) => {
  if (type === 'DEPT') {
    Object.assign(modalProps, {
      title: '부서 선택', path: '/ha00/HA00_00P_STR', defaultField: 'deptnm',
      data: { gubun: 'D0', cmpycd: authStore.cmpycd, gbncd: '', code: '', remark: '' },
      columns: [
        { title: '코드', field: 'deptcd', width: 80, hozAlign: 'center' },
        { title: '부서명', field: 'deptnm', width: 150 }
      ],
      onConfirm: (d: any) => { searchForm.deptcd = d.deptcd; searchForm.deptnm = d.deptnm }
    })
    modalVisible.value = true
  }
}

const initialize = () => {
  resetForm(searchForm)
  Object.assign(searchForm, { deptcd: authStore.deptcd, deptnm: authStore.deptnm, fromdt: firstDay, todt: today, divyn: 'N' })
  masterGrid?.clearData(); detailGrid?.clearData()
}

// 5. 그리드 초기화
onMounted(() => {
  if (masterGridRef.value) {
    masterGrid = new Tabulator(masterGridRef.value, {
      layout: 'fitColumns', height: '100%', selectable: true,
      columnDefaults: { headerSort: false, headerHozAlign: 'center', vertAlign: 'middle' },
      columns: [
        { title: '선택', width: 40, hozAlign: 'center', formatter: 'rowSelection', titleFormatter: 'rowSelection' },
        { title: 'PO No.', field: 'fileno', width: 120, hozAlign: 'center', cssClass: 'fw-bold text-primary' },
        { title: '선적', field: 'shipseq', width: 100, hozAlign: 'center', formatter: (c) => `${c.getValue()?.substring(0,1)}차` },
        { title: '통관', field: 'passseq', width: 100, hozAlign: 'center', formatter: (c) => `${c.getValue()?.substring(0,1)}차` },
        { title: '거래처', field: 'custnm', minWidth: 120, hozAlign: 'left' },
        { title: '금액', field: 'costamt', width: 120, hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 } },
        { title: '배부일', field: 'divymd', width: 120, hozAlign: 'center', formatter: (c) => {
          const v = c.getValue(); return (v && v.length === 8) ? `${v.substring(4,6)}/${v.substring(6,8)}` : v
        }}
      ]
    })
    masterGrid.on('rowClick', (e, row) => {
      masterGrid?.deselectRow(); row.select(); fetchDetail(row)
    })
  }

  if (detailGridRef.value) {
    detailGrid = new Tabulator(detailGridRef.value, {
      layout: 'fitColumns', height: '100%',
      columnDefaults: { headerSort: false, headerHozAlign: 'center', vertAlign: 'middle' },
      groupBy: 'itemcd',
      groupHeader: (value, count, data) => {
        const f = new Intl.NumberFormat();
        const sum = data.reduce((acc, cur) => acc + (Number(cur.costamt) || 0), 0);
        return `<span class='fw-bold text-primary'>${data[0].itemnm}</span>
                <span class='ms-2 small opacity-75'>[${data[0].itsize} | ${data[0].unit} | 입고:${f.format(data[0].inqty || 0)}]</span>
                <span class='ms-3 fw-bold text-danger'> (계: ${f.format(sum)})</span>`
      },
      columns: [
        { title: '비용종류', field: 'costnm', minWidth: 150, hozAlign: 'left', cssClass: 'ps-4' },
        { title: '금액', field: 'costamt', width: 130, hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 },
          bottomCalc: 'sum', bottomCalcFormatter: 'money', bottomCalcFormatterParams: { precision: 0 }
        },
        { title: '배부기준', field: 'divnm', width: 120, hozAlign: 'center' }
      ]
    })
  }
})
</script>

<style scoped>
.main-content-wrapper { padding-bottom: 0px !important; }
.erp-table-dense th, .erp-table-dense td { height: 32px !important; padding: 0 8px !important; font-size: 12px; border: 1px solid #dee2e6; }
.tabulator-instance { width: 100% !important; background-color: #fff; border-bottom: 3px solid #005a9f !important; }
:deep(.tabulator-row.tabulator-selected) { background-color: #e9ecef !important; }
:deep(.tabulator-group) { background: #f8f9fa !important; border-top: 1px solid #dee2e6 !important; padding: 4px 10px !important; }
</style>
