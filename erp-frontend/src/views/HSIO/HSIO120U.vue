<!--
	=============================================================
	프로그램명	: 입고정산취소 (HSIO120U)
	작성일자	: 2025.02.24
	설명        : 완료된 매입 정산 내역을 선택하여 취소 처리 (HSOD100U 디자인 표준 적용 및 일자 필드 통합)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-arrow-counterclockwise me-2 text-danger" style="font-size: 18px;"></i>
        구매정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        매입정산 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">입고정산취소 (HSIO120U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchCustList">조회</button>
        <button class="btn-erp btn-save" @click="save">정산취소</button>
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
                <th class="text-center bg-light">입고부서</th>
                <td>
                  <div class="input-group input-group-sm w-75">
                    <input v-model="searchForm.deptcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
                    <input v-model="searchForm.deptnm" type="text" class="form-control" placeholder="부서 선택" />
                    <button class="btn btn-outline-secondary px-2" @click="openHelp('DEPT_search')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="text-center bg-light">정산일자</th>
                <td class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                  <DateForm v-model:fromdt="searchForm.fromdt" v-model:todt="searchForm.todt" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 투-그리드 레이아웃 영역 -->
      <div class="d-flex gap-2 flex-grow-1 overflow-hidden" style="min-height: 0;">

        <!-- ⬅️ 좌측: 매입 거래처 목록 -->
        <div class="card border shadow-sm d-flex flex-column overflow-hidden grid-container-left" style="width: 350px; min-width: 350px;">
          <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark">매입 거래처</div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="poGridRef" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>

        <!-- ➡️ 우측: 취소 대상 명세 그리드 -->
        <div class="flex-grow-1 d-flex flex-column gap-2 overflow-hidden">
          <div class="card border shadow-sm flex-grow-1 d-flex flex-column overflow-hidden grid-container-right">
            <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
              <span class="fw-bold small text-dark d-flex align-items-center">
                <i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i> 취소 대상 정산 명세
              </span>
              <div class="btn-group">
                <button class="btn btn-sm btn-outline-secondary py-0 px-2 fw-bold" @click="toggleAllRows" style="font-size: 11px;">전체선택</button>
              </div>
            </div>
            <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
              <div ref="itemGridRef" class="tabulator-instance flex-grow-1"></div>
            </div>
          </div>
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

const searchForm = reactive({
  deptcd: authStore.deptcd, deptnm: authStore.deptnm,
  fromdt: firstDay,
  todt: today
})

const formData = reactive<any>({ custcd: '', custnm: '' })
const clsInfo = reactive({ sclsym: '', pclsym: '', wclsym: '' })
const poGridRef = ref<HTMLDivElement | null>(null); const itemGridRef = ref<HTMLDivElement | null>(null)
let poGrid: Tabulator | null = null; let itemGrid: Tabulator | null = null

const loadClsInfo = async () => {
  try {
    const res = await api.get('/hp00/HP00_000S_STR', { params: { gubun: 'CL', cmpycd: authStore.cmpycd } });
    if (res.data?.length) {
        const d = res.data[0];
        Object.assign(clsInfo, { pclsym: d.pclsym, sclsym: d.sclsym, wclsym: d.wclsym });
    }
  } catch (e) {}
}

async function fetchCustList() {
  try {
    const res = await api.post('/hsio/HSIO_120U_STR', {
      actkind: 'S1', cmpycd: authStore.cmpycd, iogbn: '100',
      fromdt: searchForm.fromdt.replace(/-/g, ''),
      todt: searchForm.todt.replace(/-/g, ''),
      deptcd: searchForm.deptcd
    });
    poGrid?.setData(res.data || []); itemGrid?.clearData();
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('거래처 조회 실패') }
}

async function fetchDetail(cust: any) {
  formData.custcd = cust.custcd; formData.custnm = cust.custnm;
  try {
    const res = await api.post('/hsio/HSIO_120U_STR', {
      actkind: 'S0', cmpycd: authStore.cmpycd, iogbn: '100',
      fromdt: searchForm.fromdt.replace(/-/g, ''),
      todt: searchForm.todt.replace(/-/g, ''),
      custcd: cust.custcd, deptcd: searchForm.deptcd
    })
    itemGrid?.setData(res.data || [])
  } catch (e) { vAlertError('상세 조회 실패') }
}

async function save() {
  const items = itemGrid?.getSelectedData() || []
  if (items.length === 0) return vAlertError('취소할 항목을 선택하세요.')

  if (!confirm('정산자료를 취소하시겠습니까?')) return

  try {
    const fromdt = searchForm.fromdt.replace(/-/g, '')
    const todt = searchForm.todt.replace(/-/g, '')

    for (const item of items) {
        const params = {
            actkind: 'D0', cmpycd: authStore.cmpycd, iogbn: '100',
            fromdt: fromdt, todt: todt,
            custcd: formData.custcd, deptcd: searchForm.deptcd,
            jsanym: item.jsanym, jsanno: item.jsanno, updemp: authStore.userid
        }
        const res = await api.post('/hsio/HSIO_120U_STR', params)
        const resData = res.data?.[0]
        if (resData && (resData.jsanym === '000000')) {
            throw new Error(resData.jsanno || '취소 중 업무 오류가 발생했습니다.')
        }
    }
    vAlert('정산 취소가 완료되었습니다.');
    fetchCustList(); initialize();
  } catch (e: any) { vAlertError(e.message || '취소 처리 실패') }
}

const toggleAllRows = () => {
  const rows = itemGrid?.getRows(); if (!rows) return
  const allSelected = itemGrid?.getSelectedRows().length === rows.length
  if (allSelected) itemGrid?.deselectRow()
  else itemGrid?.selectRow()
}

function initialize() {
  resetForm(searchForm);
  searchForm.deptcd = authStore.deptcd; searchForm.deptnm = authStore.deptnm;
  searchForm.fromdt = firstDay; searchForm.todt = today;
  poGrid?.clearData(); itemGrid?.clearData();
}

function openHelp(type: string) {
  if (type === 'DEPT_search') {
    openCommonHelp('DEPT', (d) => { searchForm.deptcd = d.deptcd; searchForm.deptnm = d.deptnm });
  }
}

onUnmounted(() => {
  if (poGrid) poGrid.destroy();
  if (itemGrid) itemGrid.destroy();
});

onMounted(async () => {
  await loadClsInfo();
  nextTick(() => {
    if (poGridRef.value) {
      poGrid = new Tabulator(poGridRef.value, {
        layout: 'fitColumns', height: '100%', selectable: 1,
        columnDefaults: { headerSort: false, headerHozAlign: "center", hozAlign: "center", vertAlign: "middle" },
        columns: [{ title: '매입 거래처명', field: 'custnm', cssClass: 'fw-bold text-dark', hozAlign: 'left' }]
      })
      poGrid.on('rowClick', (e, row) => fetchDetail(row.getData()))
    }

    if (itemGridRef.value) {
      itemGrid = new Tabulator(itemGridRef.value, {
        layout: 'fitColumns', height: '100%', selectable: true,
        columnDefaults: { headerSort: false, headerHozAlign: "center", hozAlign: "center", vertAlign: "middle", minWidth: 60 },
        columns: [
          { title: '선택', width: 60, hozAlign: 'center', formatter: 'rowSelection', titleFormatter: 'rowSelection', headerSort: false },
          { title: '정산일', field: 'jsanymd', width: 150, formatter: (c) => formatDate(c.getValue()) },
          { title: '정산부서', field: 'deptnm', minWidth: 200, hozAlign: 'left' },
          { title: '사업장', field: 'unitnm', width: 150, hozAlign: 'left' },
          { title: '유형', field: 'vattypenm', width: 150 },
          { title: '공급가', field: 'spyamt', hozAlign: 'right', width: 150, formatter: 'money', formatterParams: { precision: 0 } },
          { title: '부가세', field: 'vatamt', hozAlign: 'right', width: 150, formatter: 'money', formatterParams: { precision: 0 } },
          { title: '합계', field: 'jsansum', hozAlign: 'right', width: 150, formatter: 'money', cssClass: 'fw-bold text-primary', formatterParams: { precision: 0 },
            mutatorData: (v, d) => Number(d.spyamt || 0) + Number(d.vatamt || 0)
          }
        ]
      })
    }
    fetchCustList()
  })
})

const formatDate = (val: any) => val && val.length === 8 ? `${val.substring(0,4)}-${val.substring(4,6)}-${val.substring(6,8)}` : val;
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
