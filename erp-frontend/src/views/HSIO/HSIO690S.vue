<!--
	=============================================================
	프로그램명	: 매입미정산현황 (Purchase Non-Settlement Status)
	작성일자	: 2025.02.24
	작성자	    : AI Assistant
	설명        : HSOD100U 디자인 패턴 이식 및 그리드 스크롤 최적화
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom bg-white py-2 px-3 sticky-top shadow-sm">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-calendar-x me-2 text-primary" style="font-size: 18px;"></i>
        구매정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        매입정산 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        <span class="text-primary fw-bolder">매입미정산현황 (HSIO690S)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-2">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchList">조회</button>
        <button class="btn-erp btn-print" @click="print">인쇄</button>
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
                <th class="text-center bg-light">입고부서</th>
                <td>
                  <div class="input-group input-group-sm w-75">
                    <input v-model="searchForm.deptcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
                    <input v-model="searchForm.deptnm" type="text" class="form-control" placeholder="부서 선택" />
                    <button class="btn btn-outline-secondary px-2" @click="openHelp('DEPT')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="text-center bg-light">입고일자</th>
                <td class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                  <DateForm v-model:fromdt="searchForm.fymd" v-model:todt="searchForm.tymd" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 📊 3. 그리드 영역 (스크롤 최적화) -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white grid-container-right">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center">
          <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i> 거래처별 입고/정산/미정산 요약 현황</span>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column" style="min-height: 0;">
          <div ref="mainGridRef" class="tabulator-full-height" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
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
import type { ModalProps } from '@/types/modal'

const authStore = useAuthStore()
const { firstDay, today } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

const searchForm = reactive({
  deptcd: authStore.deptcd,
  deptnm: authStore.deptnm,
  fymd: firstDay,
  tymd: today
})

const mainGridRef = ref<HTMLDivElement | null>(null); let mainGrid: Tabulator | null = null

async function fetchList() {
  try {
    const res = await api.post('/hsio/HSIO_690S_STR', {
      ...searchForm,
      cmpycd: authStore.cmpycd,
      fymd: searchForm.fymd.replace(/-/g, ''),
      tymd: searchForm.tymd.replace(/-/g, '')
    })
    const data = res.data || []
    mainGrid?.setData(data)
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

function initialize() {
  resetForm(searchForm);
  searchForm.deptcd = authStore.deptcd; searchForm.deptnm = authStore.deptnm;
  searchForm.fymd = firstDay
  searchForm.tymd = today
  mainGrid?.clearData();
}

function openHelp(type: string) {
  if (type === 'DEPT') {
    Object.assign(modalProps, {
      title: '입고부서 선택',
      path: '/ha00/HA00_00P_STR',
      data: { gubun: 'D0', cmpycd: authStore.cmpycd, code: '', codenm: searchForm.deptnm, remark: '' },
      columns: [
        { title: '부서코드', field: 'deptcd', width: 100, hozAlign: 'center' },
        { title: '부서명', field: 'deptnm', width: 200 }
      ],
      onConfirm: (d: any) => { searchForm.deptcd = d.deptcd; searchForm.deptnm = d.deptnm }
    })
    modalVisible.value = true
  }
}

const print = () => { vAlert('인쇄 기능을 준비 중입니다.') }

onMounted(() => {
	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns', height: '100%',
			columnDefaults: {
				headerSort: false,
				headerHozAlign: "center",
				hozAlign: 'right', // 🚀 기본값을 우측 정렬로 설정
				vertAlign: 'middle',
				minWidth: 80
			},
			columns: [
				{ title: '거래처 명', field: 'custnm', minWidth: 180, widthGrow: 1.5, cssClass: 'fw-bold text-dark', frozen: true },
				{
					title: '입고 정보',
					columns: [
						{ title: '수량', field: 'oqty', hozAlign: 'right', width: 90, formatter: 'money', formatterParams: { precision: 0 } },
						{ title: '공급가', field: 'oamt', hozAlign: 'right', width: 110, formatter: 'money', formatterParams: { precision: 0 } },
						{ title: '부가세', field: 'ovat', hozAlign: 'right', width: 100, formatter: 'money', formatterParams: { precision: 0 } },
						{ title: '합계', field: 'osum', hozAlign: 'right', width: 110, formatter: 'money', formatterParams: { precision: 0 }, mutatorData: (v, d) => Number(d.oamt || 0) + Number(d.ovat || 0) }
					]
				},
				{
					title: '정산 내역',
					columns: [
						{ title: '수량', field: 'jqty', hozAlign: 'right', width: 90, formatter: 'money', formatterParams: { precision: 0 } },
						{ title: '공급가', field: 'jamt', hozAlign: 'right', width: 110, formatter: 'money', formatterParams: { precision: 0 } },
						{ title: '부가세', field: 'jvat', hozAlign: 'right', width: 100, formatter: 'money', formatterParams: { precision: 0 } },
						{ title: '합계', field: 'jsum', hozAlign: 'right', width: 110, formatter: 'money', formatterParams: { precision: 0 }, mutatorData: (v, d) => Number(d.jamt || 0) + Number(d.jvat || 0) }
					]
				},
				{
					title: '미정산 잔액',
					columns: [
						{ title: '수량', field: 'nqty', hozAlign: 'right', width: 90, formatter: 'money', formatterParams: { precision: 0 }, cssClass: 'text-danger' },
						{ title: '공급가', field: 'namt', hozAlign: 'right', width: 110, formatter: 'money', formatterParams: { precision: 0 }, cssClass: 'text-danger' },
						{ title: '부가세', field: 'nvat', hozAlign: 'right', width: 100, formatter: 'money', formatterParams: { precision: 0 }, cssClass: 'text-danger' },
						{ title: '합계', field: 'nsum', hozAlign: 'right', width: 110, formatter: 'money', formatterParams: { precision: 0 }, cssClass: 'fw-bold text-danger bg-light',
						  mutatorData: (v, d) => Number(d.namt || 0) + Number(d.nvat || 0) }
					]
				}
			]
		})
	}
    fetchList()
})
</script>

<style scoped>
.tabulator-full-height { width: 100% !important; background-color: #fff; border-bottom: 3px solid #005a9f !important; }
</style>

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
