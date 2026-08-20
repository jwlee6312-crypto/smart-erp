<!--
	=============================================================
	프로그램명	: 외부매입전표취소 (HSIO141U)
	작성일자	: 2025.02.24
	설명        : 완료된 외부 매입 전표 내역을 선택하여 취소 처리 (ASP 패턴 기반 순차 저장 로직 및 소문자 통일)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom bg-white py-2 px-3 sticky-top shadow-sm">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-file-earmark-x-fill me-2 text-danger" style="font-size: 18px;"></i>
        구매정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        매입정산 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">외부매입전표취소 (HSIO141U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-2">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchList">조회</button>
        <button class="btn-erp btn-danger" @click="save">전표취소</button>
      </div>
    </div>

    <!-- 🔍 2. 상단 조회 필터 -->
    <div class="p-2 pb-0 flex-shrink-0 bg-light">
      <div class="card border shadow-sm overflow-hidden">
        <div class="card-body p-2 bg-white">
          <div class="d-flex align-items-center gap-4">
            <div class="d-flex align-items-center gap-2">
              <span class="fw-bold small text-dark" style="min-width: 60px;">입고부서</span>
              <div class="input-group input-group-sm" style="width: 250px;">
                <input v-model="searchform.deptcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
                <input v-model="searchform.deptnm" type="text" class="form-control" placeholder="부서 선택" />
                <button class="btn btn-outline-secondary px-2" @click="handleOpenHelp('S_DEPT')"><i class="bi bi-search"></i></button>
              </div>
            </div>
            <div class="d-flex align-items-center gap-2">
              <span class="fw-bold small text-dark" style="min-width: 60px;">정산일자</span>
              <div class="d-flex align-items-center gap-1">
                <DateForm v-model:fromdt="searchform.fromdt" v-model:todt="searchform.todt" />
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 📊 3. 메인 그리드 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 gap-2 bg-light d-flex flex-column">
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white grid-container-right">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
          <span class="fw-bold small text-dark d-flex align-items-center">
            <i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i> 취소 대상 외부 매입 전표 내역
          </span>
          <div class="d-flex align-items-center gap-2">
            <span class="text-danger fw-bold small">선택: {{ activeitemcount }} 건</span>
            <button class="btn btn-xs btn-outline-secondary px-2" style="height: 24px; font-size: 11px;" @click="toggleAllRows">전체선택/해제</button>
          </div>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="gridelement" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>
    </div>
  </div>

  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick, onUnmounted } from 'vue'
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

const searchform = reactive<any>({
  deptcd: authStore.deptcd,
  deptnm: authStore.deptnm,
  fromdt: firstDay,
  todt: today
})

const gridelement = ref<HTMLDivElement | null>(null);
let grid: Tabulator | null = null;
const activeitemcount = ref(0)

const handleOpenHelp = (type: string) => {
  if (type === 'S_DEPT') {
    openCommonHelp('DEPT', (d) => {
      searchform.deptcd = d.deptcd;
      searchform.deptnm = d.deptnm;
    });
  }
}

async function fetchList() {
  try {
    const res = await api.post('/hsio/HSIO_141U_STR', {
      actkind: 'S0',
      cmpycd: authStore.cmpycd,
      iogbn: '100',
      fromdt: searchform.fromdt.replace(/-/g, ''),
      todt: searchform.todt.replace(/-/g, ''),
      deptcd: searchform.deptcd
    });
    grid?.setData(res.data || []);
    activeitemcount.value = 0;
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

/**
 * 🚀 전표 취소 저장 로직 (ASP 패턴 이식 & 100% 소문자화)
 */
async function save() {
  const items = grid?.getData().filter((r: any) => r.procyn === true) || []
  if (!items.length) return vAlertError('취소할 전표를 선택하세요.')

  if (!confirm('선택한 외부 매입 전표들을 정말로 취소하시겠습니까?')) return

  try {
    const resset = await api.post('/ha00/HA00_010S_STR', { cmpycd: authStore.cmpycd, gbn: 'p1' })
    const autoslip = resset.data?.[0]?.slipyn || 'N'

    for (const item of items) {
      const slipymd = (item.slipymd || '').replace(/-/g, '')
      const slipno = item.slipno
      const deptcd = item.deptcd || item.deptcd || searchform.deptcd

      // 1. 자동 전표 승인 취소 (ASP: HASL_020U_STR)
      if (autoslip === 'Y' || autoslip === 'Y') {
        await api.post('/hasl/HASL_020U_STR', {
          actkind: 'A0',
          cmpycd: authStore.cmpycd,
          slipymd: slipymd,
          acctymd: slipymd,
          slipno: slipno,
          deptcd: deptcd,
          slipkind: '030',
          slipyn: 'Y',
          cofmyn: 'N', // 승인 취소
          updemp: authStore.userid
        })
      }

      // 2. 수입전표 취소 처리 (ASP: HSIO_141U_STR 'D0')
      const params = {
        actkind: 'D0',
        cmpycd: authStore.cmpycd,
        iogbn: '100',
        fromdt: searchform.fromdt.replace(/-/g, ''),
        todt: searchform.todt.replace(/-/g, ''),
        deptcd: deptcd,
        slipymd: slipymd,
        slipno: slipno,
        updemp: authStore.userid
      }

      const res = await api.post('/hsio/HSIO_141U_STR', params)
      const resdata = res.data?.[0]

      if (resdata && (resdata.status === 'Y' || resdata.erryn === 'Y')) {
        throw new Error(resdata.msg || '취소 처리 중 오류가 발생했습니다.')
      }
    }

    vAlert('정상적으로 작업이 되었습니다.')
    fetchList(); initialize();
  } catch (e: any) {
    vAlertError(e.message || '취소 실패')
  }
}

const toggleAllRows = () => {
  const rows = grid?.getRows(); if (!rows) return
  const allSelected = rows.every(r => r.getData().procyn === true)
  rows.forEach(r => r.update({ procyn: !allSelected }))
  activeitemcount.value = grid?.getData().filter((r: any) => r.procyn === true).length || 0;
}

function initialize() {
  resetForm(searchform);
  searchform.deptcd = authStore.deptcd; searchform.deptnm = authStore.deptnm;
  searchform.fromdt = firstDay
  searchform.todt = today
  grid?.clearData(); activeitemcount.value = 0;
}

onUnmounted(() => {
  if (grid) grid.destroy();
});

onMounted(async () => {
  if (gridelement.value) {
    grid = new Tabulator(gridelement.value, {
      layout: 'fitColumns', height: '100%',
      columnDefaults: { headerSort: false, headerHozAlign: "center", vertAlign: "middle" },
      columns: [
        { title: '선택', field: 'procyn', hozAlign: 'center', width: 60, formatter: 'tickCross', editor: true,
          cellClick: (e, cell) => {
            nextTick(() => {
                activeitemcount.value = grid?.getData().filter((r: any) => r.procyn === true).length || 0;
            });
          }
        },
        { title: '전표일자', field: 'slipymd', width: 110, hozAlign: 'center', formatter: (c) => {
            const v = c.getValue(); return v && v.length === 8 ? `${v.substring(0,4)}-${v.substring(4,6)}-${v.substring(6,8)}` : v;
        }},
        { title: '전표번호', field: 'slipno', width: 100, cssClass: 'fw-bold text-primary' },
        { title: '발행부서', field: 'deptnm', width: 140, hozAlign: 'left' },
        { title: '거래처', field: 'custnm', minWidth: 150, widthGrow: 1, hozAlign: 'left', cssClass: 'fw-bold' },
        { title: '유형', field: 'vattypenm', width: 100 },
        { title: '공급가', field: 'spyamt', width: 110, hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 } },
        { title: '부가세', field: 'vatamt', width: 100, hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 } }
      ]
    })
  }
  fetchList()
})
</script>

<style scoped>
.main-content-wrapper { padding-bottom: 0px !important; }
.grid-container-right { border-bottom: 3px solid #005a9f !important; }
.erp-table-dense th, .erp-table-dense td {
  height: 32px !important; padding: 0 8px !important; font-size: 12px; vertical-align: middle; border: 1px solid #dee2e6;
}
.erp-table-dense .form-control, .erp-table-dense .form-select, .erp-table-dense .btn {
  height: 26px !important; font-size: 12px !important; border-radius: 2px;
}
.erp-table-dense th { font-weight: 600; color: #495057; }
.tabulator-instance { width: 100% !important; background-color: #fff; }
:deep(.empty-row-style) { opacity: 0.6; }
</style>
