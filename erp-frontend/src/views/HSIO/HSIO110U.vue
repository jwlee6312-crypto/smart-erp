<!--
	=============================================================
	프로그램명	: 매입정산 (HSIO110U)
	작성일자	: 2025.02.24
	설명        : 입고 내역을 기반으로 매입 정산 처리 (HSOD100U 디자인 표준 및 일자 필드 통합 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-calculator-fill me-2 text-primary" style="font-size: 18px;"></i>
        구매정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        매입관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">매입정산 (HSIO110U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchCustList">조회</button>
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
        <div class="card border shadow-sm d-flex flex-column overflow-hidden grid-container-left" style="width: 300px; min-width: 300px;">
          <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark">매입 거래처</div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="poGridRef" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>

        <!-- ➡️ 우측: 상세 정보 및 품목 그리드 -->
        <div class="flex-grow-1 d-flex flex-column gap-2 overflow-hidden">

          <!-- 상세 마스터 정보 폼 -->
          <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
            <div class="card-body p-0 bg-white">
              <table class="erp-table-dense w-100">
                <colgroup>
                  <col style="width: 110px;" /><col />
                  <col style="width: 110px;" /><col />
                  <col style="width: 110px;" /><col />
                </colgroup>
                <tbody>
                  <tr>
                    <th class="bg-light text-center">거&nbsp;&nbsp;래&nbsp;&nbsp;처</th>
                    <td colspan="3">
                      <div class="input-group input-group-sm">
                        <input v-model="formData.custcd" type="text" class="form-control text-center bg-light" style="max-width: 70px;" readonly />
                        <input v-model="formData.custnm" type="text" class="form-control bg-light" readonly />
                      </div>
                    </td>
                    <th class="required bg-light text-center">입고부서</th>
                    <td>
                      <div class="input-group input-group-sm">
                        <input v-model="formData.deptcd" type="text" class="form-control text-center bg-light fw-bold" style="max-width: 60px;" readonly />
                        <input v-model="formData.deptnm" type="text" class="form-control" readonly />
                        <button class="btn btn-outline-secondary px-2" @click="openHelp('DEPT')"><i class="bi bi-search"></i></button>
                      </div>
                    </td>
                  </tr>
                  <tr>
                    <th class="required bg-light text-center">사업장</th>
                    <td>
                      <div class="d-flex gap-1">
                        <select v-model="formData.taxunit" class="form-select">
                          <option v-for="opt in saOptions" :key="opt.taxunit" :value="opt.taxunit">{{ opt.unitnm }}</option>
                        </select>
                      </div>
                    </td>
                    <th class="required bg-light text-center">과세유형</th>
                    <td>
                      <div class="d-flex gap-1">
                        <select v-model="formData.vattype" class="form-select">
                          <option v-for="opt in vatOptions" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                        </select>
                      </div>
                    </td>
                    <th class="required bg-light text-center">발&nbsp;행&nbsp;일</th>
                    <td><input v-model="formData.pubymd" type="date" class="form-control" /></td>
                  </tr>
                  <tr>
                    <th class="bg-light text-center">공급가액</th>
                    <td><input :value="formatNumber(totals.splamt)" class="form-control text-end bg-light fw-bold" readonly /></td>
                     <th class="bg-light text-center">부가세</th>
                     <td><input :value="formatNumber(totals.vatamt)" class="form-control text-end bg-light fw-bold" readonly /></td>
                   <th class="bg-light text-center">총&nbsp;&nbsp;합&nbsp;&nbsp;계</th>
                    <td><input :value="formatNumber(totals.sumamt)" class="form-control text-end bg-warning-subtle fw-bold text-dark" readonly /></td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- 상세 품목 그리드 영역 -->
          <div class="card border shadow-sm flex-grow-1 d-flex flex-column overflow-hidden grid-container-right">
            <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
              <span class="fw-bold small text-dark d-flex align-items-center">
                <i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i> 정산 대상 입고 명세
              </span>
              <button class="btn btn-sm btn-outline-secondary py-0 px-2 fw-bold" @click="toggleAllRows" style="font-size: 11px;">전체선택</button>
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
import { ref, reactive, onMounted, computed, nextTick, onUnmounted } from 'vue'
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

const formData = reactive<any>({
  actkind: 'S0', cmpycd: authStore.cmpycd, custcd: '', custnm: '', deptcd: authStore.deptcd, deptnm: authStore.deptnm,
  taxunit: '', vattype: '010', pubymd: today
})

const saOptions = ref<any[]>([]); const vatOptions = ref<any[]>([]);
const poGridRef = ref<HTMLDivElement | null>(null); const itemGridRef = ref<HTMLDivElement | null>(null)
let poGrid: Tabulator | null = null; let itemGrid: Tabulator | null = null

const selectedItems = ref<any[]>([])

// [수정] totals 계산 로직을 selectedItems 기준으로 변경
const totals = computed(() => {
  const splamt = selectedItems.value.reduce((acc, cur: any) => acc + (Number(cur.ioamt) || 0), 0)
  const vatamt = selectedItems.value.reduce((acc, cur: any) => acc + (Number(cur.iovat) || 0), 0)
  return { splamt, vatamt, sumamt: splamt + vatamt }
})

async function fetchOptions() {
  try {
    const [resSa, resVat] = await Promise.all([
      api.post('/ha00/HA00_00P_STR', { gubun: 'SA', cmpycd: authStore.cmpycd }),
      api.post('/ha00/HA00_00P_STR', { gubun: 'E0', cmpycd: authStore.cmpycd, gbncd: '120' })
    ])
    saOptions.value = resSa.data; vatOptions.value = resVat.data;
    if (saOptions.value.length > 0) formData.taxunit = saOptions.value[0].taxunit;
  } catch (e) {}
}

async function fetchCustList() {
  try {
    const res = await api.post('/hsio/HSIO_110U_STR', {
      actkind: 'S1', cmpycd: authStore.cmpycd, iogbn: '100',
      fromdt: searchForm.fromdt.replace(/-/g, ''),
      todt: searchForm.todt.replace(/-/g, ''),
      deptcd: searchForm.deptcd
    });
    poGrid?.setData(res.data || []); itemGrid?.clearData(); vAlert('조회되었습니다.')
  } catch (e) { vAlertError('목록 조회 실패') }
}

async function fetchDetail(row: any) {
  const d = row.getData();
  formData.custcd = d.custcd; formData.custnm = d.custnm; formData.deptcd = d.deptcd; formData.deptnm = d.deptnm;
  try {
    const res = await api.post('/hsio/HSIO_110U_STR', {
      actkind: 'S0', cmpycd: authStore.cmpycd, iogbn: '100',
      custcd: d.custcd, deptcd: searchForm.deptcd,
      fromdt: searchForm.fromdt.replace(/-/g, ''),
      todt: searchForm.todt.replace(/-/g, '')
    })

    console.log(res.data)
    itemGrid?.setData(res.data.map((i: any) => ({
    ...i,
    ioymd: i.salsymd,
    ioqty: i.jsanqty,
    ioamt: i.jsanamt,
    iovat: i.jsanvat
    })));
  } catch (e) { vAlertError('상세 조회 실패') }
}

async function save() {
  const items = itemGrid?.getSelectedData() || [] // 체크된 행 데이터만 즉시 가져옴

  if (!items.length) return vAlertError('정산 처리할 품목을 선택하세요.')
  if (!formData.taxunit) return vAlertError('사업장을 선택하세요.')

  if (!confirm('정산 작업을 하시겠습니까?')) return

  try {
    const masterParams = {
      actkind: 'A0', cmpycd: authStore.cmpycd, iogbn: '100',
      fromdt: searchForm.fromdt.replace(/-/g, ''),
      todt: searchForm.todt.replace(/-/g, ''),
      custcd: formData.custcd, deptcd: formData.deptcd,
      taxunit: formData.taxunit, vattype: formData.vattype,
      jsanymd: formData.pubymd.replace(/-/g, ''),
      jsanamt: totals.value.splamt, jsanvat: totals.value.vatamt,
      updemp: authStore.userid
    }

    const resMst = await api.post('/hsio/HSIO_110U_STR', masterParams)
    const mstData = resMst.data?.[0]
    const keyJsanym = mstData?.jsanym;
    const keyJsanno = mstData?.jsanno;

    if (!keyJsanym || keyJsanym === '000000') throw new Error(keyJsanno || '마스터 정산 오류')

    for (const item of items) {
      await api.post('/hsio/HSIO_110U_STR', {
        actkind: 'U0', cmpycd: authStore.cmpycd, iogbn: '100',
        fromdt: searchForm.fromdt.replace(/-/g, ''),
        todt: searchForm.todt.replace(/-/g, ''),
        custcd: formData.custcd, deptcd: formData.deptcd,
        ioym: item.ioym, iono: item.iono, iorowno: item.iorowno,
        taxunit: formData.taxunit, vattype: formData.vattype,
        jsanym: keyJsanym, jsanno: keyJsanno,
        jsanymd: formData.pubymd.replace(/-/g, ''),
        jsanamt: String(item.ioamt || '0').replace(/,/g, ''),
        jsanvat: String(item.iovat || '0').replace(/,/g, ''),
        updemp: authStore.userid
      })
    }

    vAlert('정상으로 정산되었습니다.')
    fetchCustList(); initialize();
  } catch (e: any) { vAlertError(e.message || '정산 실패') }
}

function initialize() {
  resetForm(formData); poGrid?.clearData(); itemGrid?.clearData();
  Object.assign(formData, { cmpycd: authStore.cmpycd, pubymd: today, vattype: '010' });
}

const openHelp = (type: string) => {
  if (type === 'S_DEPT' || type === 'DEPT') {
    Object.assign(modalProps, {
      title: '부서 선택',
      path: '/ha00/HA00_00P_STR',
      defaultField: 'deptnm',
      data: { gubun: 'D0', cmpycd: authStore.cmpycd, gbncd: '', code: '', remark: '' },
      columns: [
        { title: '코드', field: 'deptcd', width: 80, hozAlign: 'center' },
        { title: '부서명', field: 'deptnm', width: 200 }
      ],
      onConfirm: (d: any) => {
        if (type === 'S_DEPT') { searchForm.deptcd = d.deptcd; searchForm.deptnm = d.deptnm; }
        else { formData.deptcd = d.deptcd; formData.deptnm = d.deptnm; }
      }
    });
    modalVisible.value = true;
  }
}

onMounted(async () => {
  await fetchOptions();
  nextTick(() => {
    if (poGridRef.value) {
      poGrid = new Tabulator(poGridRef.value, {
        layout: 'fitColumns', height: '100%', selectable: 1,
        columnDefaults: { headerSort: false, headerHozAlign: "center", hozAlign: "center", vertAlign: "middle" },
        columns: [{ title: '매입 거래처', field: 'custnm', cssClass: 'fw-bold text-dark', hozAlign: 'left' }]
      })
      poGrid.on('rowClick', (e, row) => fetchDetail(row))
    }

    if (itemGridRef.value) {
      itemGrid = new Tabulator(itemGridRef.value, {
      layout: "fitColumns", height: "100%", placeholder: "품목 없음", selectable: true,
      columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
      columns: [
          { title: '선택', width: 60, hozAlign: 'center', formatter: 'rowSelection', titleFormatter: 'rowSelection', headerSort: false },
          { title: '입고일', field: 'ioymd', width: 110, formatter: (c) => formatDate(c.getValue()) },
          { title: '품목명', field: 'itemnm', minWidth: 180, widthGrow: 1, cssClass: 'fw-bold', hozAlign: 'left' },
          { title: '규격', field: 'itsize', width: 120, hozAlign: 'left' },
          { title: '단위', field: 'unit', width: 60 },
          { title: '수량', field: 'ioqty', hozAlign: 'right', width: 90, formatter: 'money', formatterParams: { precision: 0 } },
          { title: '공급가', field: 'ioamt', hozAlign: 'right', width: 110, formatter: 'money', formatterParams: { precision: 0 } },
          { title: '부가세', field: 'iovat', hozAlign: 'right', width: 100, formatter: 'money', formatterParams: { precision: 0 } }
        ]
      })

      // [추가] 행 선택 변경 이벤트 발생 시 Vue 변수에 데이터 동기화
      itemGrid.on("rowSelectionChanged", (data: any[]) => {
         selectedItems.value = data
      })
    }
    fetchCustList()
  })
})

const formatNumber = (val: any) => Number(val || 0).toLocaleString()
const formatDate = (val: any) => val && val.length === 8 ? `${val.substring(0,4)}-${val.substring(4,6)}-${val.substring(6,8)}` : val;

onUnmounted(() => {
  if (poGrid) poGrid.destroy();
  if (itemGrid) itemGrid.destroy();
});
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
