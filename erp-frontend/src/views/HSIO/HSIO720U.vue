<!--
	=============================================================
	프로그램명	: 세트상품입고작업 (HSIO720U)
	작성일자	: 2025.02.24
	설명        : 세트상품 입고 관리 (HSOD100U 디자인 패턴 및 스크롤 최적화)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom bg-white py-2 px-3 sticky-top shadow-sm">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-box-seam-fill me-2 text-primary" style="font-size: 18px;"></i>
        구매정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        입고관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">세트상품입고작업 (HSIO720U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-2">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchList">조회</button>
        <button class="btn-erp btn-save" @click="save">저장</button>
        <button class="btn-erp btn-delete" @click="deleteData" :disabled="!formData.iono || formData.iono === ''">삭제</button>
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
                    <input v-model="searchForm.deptnm" type="text" class="form-control" placeholder="부서 선택" @keyup.enter="fetchList" />
                    <button class="btn btn-outline-secondary px-2" @click="handleOpenHelp('DEPT_search')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="text-center bg-light">입고연월</th>
                <td class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                  <input v-model="uiSearchym" type="month" class="form-control form-control-sm" style="width: 150px;" @change="fetchList" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 📊 3. 메인 작업 영역 -->
      <div class="d-flex flex-row flex-grow-1 overflow-hidden gap-2" style="min-height: 0;">
        <!-- 3-1. 좌측: 세트 입고 목록 -->
        <div class="card border shadow-sm d-flex flex-column overflow-hidden grid-container-left" style="width: 280px; min-width: 280px;">
          <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark">세트 입고 목록</div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="poGridRef" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>

        <!-- 3-2. 우측: 상세 정보 및 품목 그리드 -->
        <div class="flex-grow-1 d-flex flex-column gap-2 overflow-hidden">
          <!-- 마스터 정보 폼 -->
          <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
            <div class="card-body p-0 bg-white">
              <table class="erp-table-dense w-100">
                <colgroup>
                  <col style="width: 100px;" /><col />
                  <col style="width: 100px;" /><col />
                  <col style="width: 100px;" /><col />
                </colgroup>
                <tbody>
                  <tr>
                    <th class="bg-light text-center">입고부서</th>
                    <td>
                      <div class="input-group input-group-sm">
                        <input v-model="formData.deptcd" class="form-control bg-light" style="max-width: 60px;" readonly />
                        <input v-model="formData.deptnm" class="form-control bg-light" readonly />
                      </div>
                    </td>
                    <th class="required bg-light text-center">입고번호</th>
                    <td>
                      <div class="d-flex gap-1">
                        <input v-model="uiioym" type="month" class="form-control form-control-sm text-center fw-bold bg-light" style="width: 120px;" readonly />
                        <input v-model="formData.iono" class="form-control form-control-sm text-center bg-light fw-bold text-primary" style="width: 60px;" readonly placeholder="0000" />
                      </div>
                    </td>
                    <th class="required bg-light text-center">입고일자</th>
                    <td><input v-model="formData.ioymd" type="date" class="form-control form-control-sm" /></td>
                  </tr>
                  <tr>
                    <th class="required bg-light text-center">입고창고</th>
                    <td>
                      <select v-model="formData.whcd" class="form-select form-select-sm">
                        <option v-for="opt in whOptions" :key="opt.code" :value="opt.code">{{ opt.cdnm }}</option>
                      </select>
                    </td>
                    <th class="bg-light text-center">Lot No.</th>
                    <td><input v-model="formData.lotno" type="text" class="form-control form-control-sm" /></td>
                    <th class="bg-light text-center">특기사항</th>
                    <td><input v-model="formData.remark" type="text" class="form-control form-control-sm" /></td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- 디테일 그리드 (💡 사용자 원칙 적용: tabulator-instance 클래스 명시) -->
          <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white grid-container-right">
            <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
              <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i> 세트 구성 품목 명세</span>
              <button class="btn btn-sm btn-outline-primary py-0 px-2 fw-bold" @click="addRow" style="font-size: 12px;">+ 행추가</button>
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
  <ItemHelpModal :visible="itemHelpVisible" :cmpycd="authStore.cmpycd" :astKind="String(formData.astkind || '2')" @close="itemHelpVisible = false" @confirm="onSelectItem" />
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, nextTick } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import ItemHelpModal from '@/components/ItemHelpModal.vue'
import type { ModalProps } from '@/types/modal'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

const itemHelpVisible = ref(false)
const currentTargetRow = ref<any>(null)

const searchForm = reactive({
  deptcd: authStore.deptcd, deptnm: authStore.deptnm,
  ioym: new Date().toISOString().substring(0, 7).replace('-', '')
})

const formData = reactive<any>({
  actkind: 'S0', cmpycd: authStore.cmpycd, ioym: new Date().toISOString().substring(0, 7).replace('-', ''), iono: '',
  ioymd: new Date().toISOString().substring(0, 10), deptcd: authStore.deptcd, deptnm: authStore.deptnm,
  whcd: '100', lotno: '', remark: '', usernm: authStore.usernm, astkind: '2'
})

const uiSearchym = computed({ get: () => `${searchForm.ioym.substring(0, 4)}-${searchForm.ioym.substring(4, 6)}`, set: (v) => searchForm.ioym = v.replace('-', '') })
const uiioym = computed({ get: () => `${formData.ioym.substring(0, 4)}-${formData.ioym.substring(4, 6)}`, set: (v) => formData.ioym = v.replace('-', '') })

const whOptions = ref<any[]>([]);
const poGridRef = ref<HTMLDivElement | null>(null); const itemGridRef = ref<HTMLDivElement | null>(null)
let poGrid: Tabulator | null = null; let itemGrid: Tabulator | null = null

async function fetchList() {
  try {
    const res = await api.post('/hsio/HSIO_720U_STR', { ...searchForm, actkind: 'S1', cmpycd: authStore.cmpycd })
    poGrid?.setData(res.data || []); itemGrid?.clearData(); vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

async function fetchDetail(row: any) {
  const d = row.getData();
  try {
    const res = await api.post('/hsio/HSIO_720U_STR', { ioym: d.ioym, iono: d.iono, actkind: 'S0', cmpycd: authStore.cmpycd })
    if (res.data?.length) {
      Object.assign(formData, res.data[0])
      const resItems = await api.post('/hsio/HSIO_721U_STR', { ioym: d.ioym, iono: d.iono, actkind: 'S0', cmpycd: authStore.cmpycd })
      itemGrid?.setData(resItems.data?.map((i: any) => ({ ...i, upkind: 'U' })) || [])
    }
  } catch (e) { vAlertError('상세 조회 실패') }
}

async function save() {
  const items = itemGrid?.getData();
  if (!items || items.length === 0) return vAlertError('입고 품목을 추가하세요.')
  try {
    await api.post('/hsio/HSIO_720U_STR', { ...formData, actkind: formData.iono ? 'U0' : 'A0', items: items })
    vAlert('정상으로 작업이 되었습니다.'); fetchList()
  } catch (e) { vAlertError('저장 실패') }
}

async function deleteData() {
    if(!confirm('삭제하시겠습니까?')) return
    try {
        await api.post('/hsio/HSIO_720U_STR', { ...formData, actkind: 'D0' })
        vAlert('삭제되었습니다.'); initialize(); fetchList();
    } catch (e) { vAlertError('삭제 실패') }
}

function handleOpenHelp(type: string, target?: any) {
  if (type === 'DEPT_search') {
    Object.assign(modalProps, {
      title: '부서 선택',
      path: '/ha00/HA00_00P_STR',
      data: { gubun: 'D0', cmpycd: authStore.cmpycd, code: '', codenm: searchForm.deptnm, remark: '' },
      columns: [
        { title: '부서코드', field: 'deptcd', width: 100, hozAlign: 'center' },
        { title: '부서명', field: 'deptnm', width: 200 }
      ],
      onConfirm: (d: any) => { searchForm.deptcd = d.deptcd; searchForm.deptnm = d.deptnm }
    })
    modalVisible.value = true
  } else if (type === 'ITEM') {
    currentTargetRow.value = target;
    itemHelpVisible.value = true;
  }
}

const onSelectItem = (d: any) => {
  if (!currentTargetRow.value) return
  currentTargetRow.value.update({
    itemcd: d.itemcd, itemnm: d.itemnm, itsize: d.itsize, unit: d.unit || d.unitnm, price: d.incost || d.price || 0, upkind: 'A'
  });
  itemHelpVisible.value = false;
}

const addRow = () => { itemGrid?.addRow({ ioqty: 0, price: 0, ioamt: 0, upkind: 'A' }); }

function initialize() {
  resetForm(formData);
  formData.ioym = new Date().toISOString().substring(0, 7).replace('-', '');
  formData.ioymd = new Date().toISOString().substring(0, 10);
  formData.astkind = '2';
  itemGrid?.clearData(); poGrid?.deselectRow();
}

onMounted(async () => {
  api.get('/hs00/HS00_000S_STR', { params: { gubun: 'W0', cmpycd: authStore.cmpycd } })
     .then(r => whOptions.value = r.data.map((i:any)=>({code: i.code || i.whcd, cdnm: i.cdnm || i.whnm})));

  if (poGridRef.value) {
    poGrid = new Tabulator(poGridRef.value, {
      layout: 'fitColumns', height: '100%', selectable: 1,
      columns: [{ title: '입고번호', field: 'iono_disp', hozAlign: 'center', cssClass: 'fw-bold text-primary', mutatorData: (v, d) => `${d.ioym}-${d.iono}` }]
    })
    poGrid.on('rowClick', (e, row) => fetchDetail(row))
  }

  if (itemGridRef.value) {
    itemGrid = new Tabulator(itemGridRef.value, {
      layout: 'fitColumns', height: '100%',
      columnDefaults: { headerSort: false, headerHozAlign: "center", minWidth: 100 },
      columns: [
        { title: "상태", field: "upkind", width: 50, hozAlign: "center", formatter: (c) => {
            const v = c.getValue();
            if (v === 'A') return '<i class="bi bi-plus-circle-fill text-primary"></i>';
            if (v === 'U') return '<i class="bi bi-pencil-square text-warning"></i>';
            if (v === 'D') return '<i class="bi bi-dash-circle-fill text-danger"></i>';
            return '';
        }},
        { title: "품목코드", field: "itemcd", width: 120, formatter: (cell) => (cell.getValue() || '') + " <i class='bi bi-search text-primary ms-1 cursor-pointer'></i>", cellClick: (e, cell) => { if ((e.target as HTMLElement).classList.contains('bi-search')) handleOpenHelp('ITEM', cell.getRow()) } },
        { title: '품목명', field: 'itemnm', minWidth: 200, widthGrow: 1, cssClass: 'fw-bold' },
        { title: '규격', field: 'itsize', width: 150 },
        { title: '단위', field: 'unit', width: 60, hozAlign: 'center' },
        { title: '수량', field: 'ioqty', hozAlign: 'right', width: 100, editor: 'number' },
        { title: '단가', field: 'price', hozAlign: 'right', width: 110, editor: 'number' },
        { title: '금액', field: 'ioamt', hozAlign: 'right', width: 130, formatter: 'money' },
        { title: "", width: 40, hozAlign: "center", formatter: () => "<i class='bi bi-trash text-danger cursor-pointer'></i>", cellClick: (e, c) => { if (c.getRow().getData().upkind === 'A') c.getRow().delete(); else { c.getRow().update({ upkind: 'D' }); c.getRow().getElement().style.opacity = '0.5' }; } }
      ]
    })
    itemGrid.on("cellEdited", (cell) => {
        const field = cell.getField();
        if (['ioqty', 'price'].includes(field)) {
            const d = cell.getData();
            const amt = Number(d.ioqty || 0) * Number(d.price || 0);
            cell.getRow().update({ ioamt: amt });
            if (d.upkind !== 'A') cell.getRow().update({ upkind: 'U' });
        }
    })
  }
  fetchList();
})
</script>

<style scoped>
.tabulator-instance { background-color: #fff; }
</style>
