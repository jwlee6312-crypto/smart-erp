<!--
	=============================================================
	프로그램명	: 입고취소 (HSIO070U)
	작성일자	: 2025.02.24
	설명        : 입고 완료된 내역을 선택하여 취소 처리 (표준화 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-x-circle-fill me-2 text-danger" style="font-size: 18px;"></i>
        구매정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        입고관리 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        <span class="text-primary fw-bolder">입고취소 (HSIO070U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-2">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchCustList">조회</button>
        <button class="btn-erp btn-delete" @click="save">취소저장</button>
      </div>
    </div>

    <!-- 🔍 2. 최상단 검색 필터 영역 -->
    <div class="p-2 pb-0 flex-shrink-0 bg-light">
      <div class="card border shadow-sm overflow-hidden">
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
                <th class="text-center bg-light">입고일자</th>
                <td class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                  <!-- ✅ fromdt, todt로 표준화 -->
                  <DateForm v-model:fromdt="searchForm.fromdt" v-model:todt="searchForm.todt" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- 📊 3. 메인 작업 영역 -->
    <div class="d-flex flex-row flex-grow-1 overflow-hidden p-2 gap-2 bg-light">
      <!-- 3-1. 좌측: 발주 거래처 목록 -->
      <div class="card border shadow-sm d-flex flex-column" style="width: 300px; min-width: 300px;">
        <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark">발주 거래처</div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="poGridRef" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>

      <!-- 3-2. 우측: 취소 대상 입고 명세 -->
      <div class="flex-grow-1 d-flex flex-column gap-2 overflow-hidden">
        <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white grid-container-right">
          <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
            <span class="fw-bold small text-dark d-flex align-items-center">
              <i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i> 취소 대상 입고 명세
            </span>
            <div class="d-flex align-items-center gap-2">
               <span class="text-danger fw-bold small">선택: {{ activeItemCount }} 건</span>
            </div>
          </div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="itemGridRef" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
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

// [1] 데이터 모델링 (fromdt/todt 표준화)
const searchForm = reactive({
  deptcd: authStore.deptcd, deptnm: authStore.deptnm,
  fromdt: firstDay,
  todt: today
})

const clsInfo = reactive({ sclsym: '' })
const poGridRef = ref<HTMLDivElement | null>(null); const itemGridRef = ref<HTMLDivElement | null>(null)
let poGrid: Tabulator | null = null; let itemGrid: Tabulator | null = null
const activeItemCount = ref(0)

const loadClsInfo = async () => {
  try {
    const res = await api.get('/hp00/HP00_000S_STR', { params: { gubun: 'CL', cmpycd: authStore.cmpycd } });
    if (res.data?.length) clsInfo.sclsym = res.data[0].sclsym;
  } catch (e) {}
}

async function fetchCustList() {
  try {
    const res = await api.post('/hsio/HSIO_070U_STR', {
      actkind: 'S1', cmpycd: authStore.cmpycd, iogbn: '100',
      fromdt: searchForm.fromdt.replace(/-/g, ''),
      todt: searchForm.todt.replace(/-/g, ''),
      deptcd: searchForm.deptcd
    });
    poGrid?.setData(res.data || []); itemGrid?.clearData(); activeItemCount.value = 0;
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

async function fetchDetail(cust: any) {
  try {
    const res = await api.post('/hsio/HSIO_070U_STR', {
      actkind: 'S0', cmpycd: authStore.cmpycd, iogbn: '100',
      fromdt: searchForm.fromdt.replace(/-/g, ''),
      todt: searchForm.todt.replace(/-/g, ''),
      custcd: cust.custcd, deptcd: searchForm.deptcd
    })
    itemGrid?.setData(res.data || [])
  } catch (e) { vAlertError('상세 조회 실패') }
}

/**
 * 🚀 저장(취소) 로직 (getSelectedData 표준 방식 적용)
 */
async function save() {
  const items = itemGrid?.getSelectedData() || []
  if (items.length === 0) return vAlertError('취소할 항목을 선택하세요.')
  if (!confirm('입고처리를 취소하시겠습니까?')) return

  try {
    for (const item of items) {
        const params = {
            actkind: 'D0',
            cmpycd: authStore.cmpycd,
            iogbn: '100',
            fromdt: searchForm.fromdt.replace(/-/g, ''),
            todt: searchForm.todt.replace(/-/g, ''),
            deptcd: searchForm.deptcd,
            custcd: item.custcd,
            ioym: item.ioym,
            iono: item.iono,
            updemp: authStore.userid
        }
        const res = await api.post('/hsio/HSIO_070U_STR', params)
        const resData = res.data?.[0]
        if (resData && (resData.ioym === '000000')) {
            throw new Error(resData.iono || '취소 중 업무 오류가 발생했습니다.')
        }
    }
    vAlert('정상적으로 취소되었습니다.'); fetchCustList();
  } catch (e: any) { vAlertError(e.message || '취소 처리 실패') }
}

function initialize() {
  resetForm(searchForm); searchForm.deptcd = authStore.deptcd; searchForm.deptnm = authStore.deptnm;
  poGrid?.clearData(); itemGrid?.clearData(); activeItemCount.value = 0;
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

  if (poGridRef.value) {
    poGrid = new Tabulator(poGridRef.value, {
      layout: 'fitColumns', height: '100%', selectable: 1,
      columnDefaults: { headerSort: false, headerHozAlign: "center", hozAlign: "center", vertAlign: "middle" },
      columns: [{ title: '발주 거래처', field: 'custnm', cssClass: 'fw-bold text-dark', hozAlign: 'left' }]
    })
    poGrid.on('rowClick', (e, row) => fetchDetail(row.getData()))
  }

  if (itemGridRef.value) {
    itemGrid = new Tabulator(itemGridRef.value, {
      layout: 'fitColumns', height: '100%',
      selectable: true, // ✅ 행 선택 활성화
      selectableCheck: (row) => { // ✅ 정산/마감 데이터 선택 방지
          const d = row.getData();
          if (d.jyn === 'Y') return false;
          const ioym = d.ioymd?.substring(0, 6) || '';
          if (clsInfo.sclsym && ioym <= clsInfo.sclsym) return false;
          return true;
      },
      columnDefaults: { headerSort: false, headerHozAlign: "center", hozAlign: "center", vertAlign: "middle", minWidth: 100 },
      columns: [
        // ✅ 표준 체크박스 포맷터 적용
        { title: '선택', width: 60, hozAlign: 'center', formatter: 'rowSelection', titleFormatter: 'rowSelection', headerSort: false },
        { title: '입고일', field: 'ioymd', width: 110, formatter: (c) => formatDate(c.getValue()) },
        { title: '입고창고', field: 'whnm', minWidth: 120, widthGrow: 1, cssClass: 'fw-bold', hozAlign: 'left' },
        { title: '입고번호', field: 'iono', width: 120, hozAlign: 'center' },
        { title: '수량', field: 'ioqty', hozAlign: 'right', width: 100, formatter: 'money', formatterParams: { precision: 0 } },
        { title: '금액', field: 'ioamt', hozAlign: 'right', width: 110, formatter: 'money', formatterParams: { precision: 0 } },
        { title: '부가세', field: 'iovat', hozAlign: 'right', width: 100, formatter: 'money', formatterParams: { precision: 0 } }
      ]
    })

    // ✅ 선택 변경 시 건수 업데이트
    itemGrid.on("rowSelectionChanged", () => {
        activeItemCount.value = itemGrid?.getSelectedData().length || 0
    })
  }
  fetchCustList()
})

const formatDate = (val: any) => val && val.length === 8 ? `${val.substring(0,4)}-${val.substring(4,6)}-${val.substring(6,8)}` : val;
</script>

<style scoped>
.grid-container-right { border-bottom: 3px solid #005a9f !important; }
.erp-table-dense th, .erp-table-dense td {
  height: 32px !important; padding: 0 8px !important; font-size: 12px; vertical-align: middle; border: 1px solid #dee2e6;
}
.erp-table-dense .form-control, .erp-table-dense .form-select, .erp-table-dense .btn {
  height: 26px !important; font-size: 12px !important; border-radius: 2px;
}
.erp-table-dense th { font-weight: 600; color: #495057; }
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>