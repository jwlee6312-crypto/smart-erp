<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-truck me-2 text-primary" style="font-size: 18px;"></i>
        수입관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        수입선적 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">선적등록 (HSIP110U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchPoList">조회</button>
        <button class="btn-erp btn-save" @click="save">저장</button>
      </div>
    </div>

    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
                <col style="width: 10%" /><col style="width: 25%" />
                <col style="width: 10%" /><col style="width: 25%" />
                <col style="width: 10%" /><col style="width: 20%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="bg-light text-center">조회기간</th>
                <td class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                    <DateForm v-model:fromdt="searchForm.fromdt" v-model:todt="searchForm.todt" />
                </td>
                <th class="bg-light text-center">관리부서</th>
                <td>
                  <div class="d-flex gap-1">
                    <input v-model="searchForm.deptcd" class="form-control bg-light" style="max-width: 80px;" readonly />
                    <div class="input-group input-group-sm">
                      <input v-model="searchForm.deptnm" class="form-control bg-light" readonly />
                      <button class="btn btn-outline-secondary" @click="openHelp('DEPT_search')"><i class="bi bi-search"></i></button>
                    </div>
                  </div>
                </td>
                <th class="bg-light"></th><td></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="d-flex gap-2 flex-grow-1 overflow-hidden" style="min-height: 0;">
        <div class="card border shadow-sm d-flex flex-column overflow-hidden grid-container-left" style="width: 300px; min-width: 300px;">
          <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark">PO 목록</div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="poGridRef" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>

        <div class="flex-grow-1 d-flex flex-column gap-2 overflow-hidden">
          <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
            <div class="card-body p-0 bg-white">
              <table class="erp-table-dense w-100">
                <colgroup>
                  <col style="width: 100px;" /><col />
                  <col style="width: 100px;" /><col />
                  <col style="width: 100px;" /><col />
                  <col style="width: 100px;" /><col />
                </colgroup>
                <tbody>
                  <tr>
                    <th class="required bg-light text-center">파일번호</th>
                    <td><input v-model="formData.fileno" class="form-control bg-light fw-bold text-primary" readonly /></td>
                    <th class="required bg-light text-center">선적일자</th>
                    <td><input v-model="formData.shipymd" type="date" class="form-control" /></td>
                    <th class="required bg-light text-center">보관창고</th>
                    <td>
                      <select v-model="formData.storseat" class="form-select">
                        <option value="">선택</option>
                        <option v-for="opt in seatOptions" :key="opt.code" :value="opt.code">{{ opt.cdnm }}</option>
                      </select>
                    </td>
                    <th class="required bg-light text-center">도착일자</th>
                    <td><input v-model="formData.arvymd" type="date" class="form-control" /></td>
                  </tr>
                  <tr>
                    <th class="required bg-light text-center">B/L No.</th>
                    <td><input v-model="formData.blno" class="form-control" /></td>
                    <th class="required bg-light text-center">적용환율</th>
                    <td><input v-model="formData.frgnrate" type="number" step="0.01" class="form-control text-end fw-bold text-primary" @input="calcWonAmt" /></td>
                    <th class="bg-light"></th><td></td>
                  </tr>
                  <tr>
                    <th class="bg-light text-center">거 래 처</th>
                    <td colspan="3">
                      <div class="d-flex gap-1">
                        <input v-model="formData.custcd" class="form-control bg-light" style="max-width: 80px;" readonly />
                        <input v-model="formData.custnm" class="form-control bg-light" readonly />
                      </div>
                    </td>
                    <th class="required bg-light text-center">원&nbsp;&nbsp;산&nbsp;&nbsp;지</th>
                    <td>
                      <select v-model="formData.nacd" class="form-select">
                        <option value="">선택</option>
                        <option v-for="opt in nacdOptions" :key="opt.code" :value="opt.code">{{ opt.cdnm }}</option>
                      </select>
                    </td>
                    <th class="required bg-light text-center">통화구분</th>
                    <td>
                    <select v-model="formData.currcd" class="form-select" style="width: 100px;">
                      <option value="">선택</option>
                      <option v-for="opt in currcdOptions" :key="opt.code" :value="opt.code">{{ opt.cdnm }}</option>
                    </select>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white grid-container-right">
            <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center">
              <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>선적 품목 명세</span>
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
import { ref, reactive, onMounted, computed } from 'vue'
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
  fileno: '', shipseq: '10', blno: '',
  shipymd: today,
  arvymd: today,
  storseat: '100', frgnrate: 1350.00, custnm: '', custcd: '',
  nacd: '', currcd: '', shipport: '', arvport: '',
  paycond: '', pricond: '', payterm: ''
})

const seatOptions = ref<any[]>([])
const nacdOptions = ref<any[]>([])
const currcdOptions = ref<any[]>([])

const poGridRef = ref<HTMLDivElement | null>(null); const itemGridRef = ref<HTMLDivElement | null>(null)
let poGrid: Tabulator | null = null; let itemGrid: Tabulator | null = null

async function fetchPoList() {
  try {
    const res = await api.post('/hsip/HSIP_112U_STR', {
        cmpycd: authStore.cmpycd,
        deptcd: searchForm.deptcd,
        fromdt: searchForm.fromdt.replace(/-/g, ''),
        todt: searchForm.todt.replace(/-/g, '')
    })
    poGrid?.setData(res.data || [])
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('PO 조회 실패') }
}

async function fetchDetail() {
  if (!formData.fileno) return
  try {
    const res = await api.post('/hsip/HSIP_110U_STR', {
        fileno: formData.fileno,
        shipseq: formData.shipseq,
        frgnrate: 0,
        actkind: 'S0'
    })

    if (res.data?.length) {
          const master = res.data[0];
          Object.assign(formData, {...master,
            issymd: master.issymd && master.issymd.length === 8 ? `${master.issymd.substring(0, 4)}-${master.issymd.substring(4, 6)}-${master.issymd.substring(6, 8)}` : master.issymd,
            shipymd: master.shipymd && master.shipymd.length === 8 ? `${master.shipymd.substring(0, 4)}-${master.shipymd.substring(4, 6)}-${master.shipymd.substring(6, 8)}` : master.shipymd,
            arvymd: master.arvymd && master.arvymd.length === 8 ? `${master.arvymd.substring(0, 4)}-${master.arvymd.substring(4, 6)}-${master.arvymd.substring(6, 8)}` : master.arvymd
          });

          const resItems = await api.post('/hsip/HSIP_111U_STR', {
              actkind: 'S0',
              cmpycd: authStore.cmpycd,
              fileno: formData.fileno,
              shipseq: formData.shipseq,
              qty: 0,
              amt: 0,
              wonamt: 0
          });
          itemGrid?.setData(resItems.data || [])
        }
  } catch (e) { vAlertError('상세 조회 실패') }
}

async function save() {
  const items = itemGrid?.getSelectedData() || []
  if (items.length === 0) return vAlertError('선적 처리할 품목을 선택하세요.')

  if (!confirm('선적정보를 등록 하시겠습니까?')) return

  try {
    const masterActkind = formData.imymd ? 'U0' : 'A0'

    const masterParams = {
      ...formData,
      actkind: masterActkind,
      cmpycd: authStore.cmpycd,
      updemp: authStore.userid,
      shipymd: formData.shipymd.replace(/-/g, ''),
      arvymd: formData.arvymd.replace(/-/g, '')
    }

    // 🚀 [Seed-Model] Step 1. 마스터 저장 실행
    const resMst = await api.post('/hsip/HSIP_110U_STR', masterParams)
    const mstData = resMst.data?.[0]

    // 🚀 [Seed-Model] Step 2. 무결성 키 추출 (0번: 상태/번호, 1번: 순번)
    const rowValues = mstData?.returnkeyvalue || Object.values(mstData || {});
    const key1 = (mstData?.fileno || rowValues[0] || '').toString().trim();
    const key2 = (mstData?.shipseq || rowValues[1] || '').toString().trim();

    // 🚀 [Seed-Model] Step 3. 에러 판별
    if (key1 === '000000') {
      throw new Error(key2 || '선적 정보 저장 중 업무 에러가 발생했습니다.')
    }

    const keyFileno = key1 || formData.fileno
    const keyShipseq = key2 || formData.shipseq

    if (!keyFileno) throw new Error('파일번호 수신 실패 (Data Integrity Error)')

    // 🚀 [Seed-Model] Step 4. 상세 내역 연결 (A1 루프)
    for (const item of items) {
      const detailParams = {
          ...item,
          qty: Number(item.sqty || 0),
          amt: Number(item.amt || 0),
          wonamt: Number(item.wonamt || 0),
          actkind: 'A1',
          cmpycd: authStore.cmpycd,
          updemp: authStore.userid,
          fileno: keyFileno,
          shipseq: keyShipseq
      }

      const resDtl = await api.post('/hsip/HSIP_111U_STR', detailParams)
      const dtlData = resDtl.data?.[0]
      const dtlValues = dtlData?.returnkeyvalue || Object.values(dtlData || {});

      if (dtlValues[0] === '000000') {
          throw new Error(String(dtlValues[1] || '상세 내역 저장 중 오류 발생'))
      }
    }

    vAlert('성공적으로 저장되었습니다.')
    fetchPoList()
    initialize()

  } catch (e: any) {
    console.error('저장 중 오류 발생:', e)
    vAlertError(e.response?.data?.message || e.message || '저장 처리 중 오류가 발생했습니다.')
  }
}

const calcWonAmt = () => {
  const rows = itemGrid?.getRows()
  rows?.forEach(row => {
    const d = row.getData()
    row.update({ wonamt: Math.floor((Number(d.amt) || 0) * formData.frgnrate) })
  })
}

function initialize() {
  resetForm(formData); formData.shipseq = '10'; itemGrid?.clearData(); poGrid?.deselectRow()
}

function openHelp(type: string) {
  if (type === 'DEPT_search') {
    openCommonHelp('DEPT', (d) => { searchForm.deptcd = d.deptcd; searchForm.deptnm = d.deptnm });
  }
}

onMounted(async () => {
  try {
      const resSeat = await api.get('/hs00/HS00_000S_STR', { params: { gubun: 'E0', cmpycd: authStore.cmpycd, gbncd: '330', code: '', codenm: '', etcval: '' } })
      seatOptions.value = resSeat.data.map((i: any) => ({ code: i.codecd || i.code, cdnm: i.codenm || i.cdnm }))

      const resNacd = await api.get('/hs00/HS00_000S_STR', { params: { gubun: 'E2', cmpycd: authStore.cmpycd, gbncd: '305', code: '', codenm: '', etcval: '' } })
      nacdOptions.value = resNacd.data.map((j: any) => ({ code: j.codecd || j.code, cdnm: j.codenm || j.cdnm }))

      const resCurrcd = await api.get('/hs00/HS00_000S_STR', { params: { gubun: 'E2', cmpycd: authStore.cmpycd, gbncd: '310', code: '', codenm: '', etcval: '' } })
      currcdOptions.value = resCurrcd.data.map((k: any) => ({ code: k.codecd || k.code, cdnm: k.codenm || k.cdnm }))
  } catch (e) {}

  if (poGridRef.value) {
    poGrid = new Tabulator(poGridRef.value, {
      layout: 'fitColumns', height: "100%", selectable: 1,
      columnDefaults: { headerSort: false, headerHozAlign: "center", hozAlign: "center", vertAlign: "middle" },
      columns: [
        { title: 'PO No.', field: 'fileno', cssClass: 'fw-bold text-primary', Width: 80 },
        { title: '상태', field: 'shipyn', width: 60, formatter: (c) => c.getValue() === 'Y' ? '완료' : '미완료' },
        { title: "수입처", field: "custnm", hozAlign: "left", minWidth: 100, cssClass: "fw-bold text-primary", headerSort: false }
      ]
    })
    poGrid.on('rowClick', (e, row) => {
      formData.fileno = row.getData().fileno; fetchDetail()
    })
  }

  if (itemGridRef.value) {
    itemGrid = new Tabulator(itemGridRef.value, {
      layout: 'fitColumns', height: "100%", selectable: true,
      columnCalcs: "table",
      columnDefaults: { headerSort: false, headerHozAlign: "center", hozAlign: "center", vertAlign: "middle", minWidth: 100 },
      columns: [
        { title: '선택', width: 50, hozAlign: 'center', formatter: 'rowSelection', titleFormatter: 'rowSelection', headerSort: false, bottomCalc: () => "합계" },
        { title: '품목명', field: 'itemnm', minWidth: 180, widthGrow: 1, cssClass: 'fw-bold', hozAlign: 'left' },
        { title: '규격', field: 'itsize', width: 120, hozAlign: 'left' },
        { title: '단위', field: 'unit', width: 60 },
        { title: '발주량', field: 'pqty', hozAlign: 'right', width: 90, formatter: 'money', formatterParams: { precision: 0 }, bottomCalc: "sum" },
        { title: '단가', field: 'price', hozAlign: 'right', width: 100, formatter: 'money', formatterParams: { precision: 2 } },
        { title: '잔량', field: 'jqty', hozAlign: 'right', width: 90, cssClass: 'text-danger fw-bold', bottomCalc: "sum" },
        { title: '선적량', field: 'sqty', hozAlign: 'right', width: 90, editor: 'number', cssClass: 'bg-yellow fw-bold', bottomCalc: "sum" },
        { title: '외화금액', field: 'amt', hozAlign: 'right', width: 110, formatter: 'money', formatterParams: { precision: 2 }, bottomCalc: "sum" },
        { title: '원화금액', field: 'wonamt', hozAlign: 'right', width: 120, formatter: 'money', formatterParams: { precision: 0 }, bottomCalc: "sum" }
      ]
    })
  }
  fetchPoList()
})
</script>

<style scoped>
.main-content-wrapper { padding-bottom: 0px !important; }
.grid-container-left, .grid-container-right { border-bottom: 3px solid #005a9f !important; }
.erp-table-dense th, .erp-table-dense td {
  height: 32px !important; padding: 0 8px !important; font-size: 12px; vertical-align: middle; border: 1px solid #dee2e6;
}
.erp-table-dense .form-control, .erp-table-dense .form-select, .erp-table-dense .btn {
  height: 26px !important; font-size: 12px !important; border-radius: 2px;
}
.erp-table-dense th { font-weight: 500; }
</style>