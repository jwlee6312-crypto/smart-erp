<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-box-arrow-up-right me-2 text-primary" style="font-size: 18px;"></i>
        생산정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        <span class="text-primary fw-bolder">재공타계정출고 (HPIO530U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-2">
        <button class="btn-erp btn-init" @click="initialize">
          <i class="bi bi-arrow-clockwise"></i> 초기화
        </button>
        <button class="btn-erp btn-search" @click="fetchMaster">
          <i class="bi bi-search"></i> 조회
        </button>
        <button class="btn-erp btn-save" @click="saveData">
          <i class="bi bi-save"></i> 저장
        </button>
        <button v-if="masterData.iono" class="btn-erp btn-delete" @click="deleteData">
          <i class="bi bi-trash"></i> 삭제
        </button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-auto p-2 d-flex flex-column gap-3">
      <!-- 🅰️ 출고 및 주문 정보 -->
      <div class="card border-0 shadow-sm overflow-hidden" style="border-radius: 8px;">
        <div class="card-body p-0">
          <table class="erp-table-full">
            <colgroup>
              <col style="width: 100px;"><col>
              <col style="width: 100px;"><col>
              <col style="width: 100px;"><col>
              <col style="width: 100px;"><col>
            </colgroup>
            <tbody>
              <tr>
                <th class="required">출고부서</th>
                <td>
                  <div class="input-group input-group-sm" style="width: 200px;">
                    <input v-model="masterData.deptcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
                    <input v-model="masterData.deptnm" type="text" class="form-control" placeholder="부서 선택" @keyup.enter="openHelp('DEPT')" />
                    <button class="btn btn-outline-secondary" @click="openHelp('DEPT')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="required">출고번호</th>
                <td>
                  <div class="d-flex align-items-center gap-1" style="width: 180px;">
                    <input v-model="uiioym" type="month" class="form-control form-control-sm" style="width: 120px;" />
                    <input v-model="masterData.iono" type="text" class="form-control form-control-sm text-center bg-light" style="width: 50px;" readonly />
                  </div>
                </td>
                <th class="required">출고일자</th>
                <td>
                  <input v-model="uiioymd" type="date" class="form-control form-control-sm" style="width: 140px;" />
                </td>
                <th class="required">출고창고</th>
                <td>
                  <select v-model="masterData.whcd" class="form-select form-select-sm" style="width: 140px;">
                    <option v-for="opt in whOptions" :key="opt.code" :value="opt.code">{{ opt.cdnm }}</option>
                  </select>
                </td>
              </tr>
              <tr>
                <th class="required">주문번호</th>
                <td>
                  <div class="input-group input-group-sm" style="width: 220px;">
                    <input v-model="uiordym" type="month" class="form-control" />
                    <input v-model="masterData.ordno" type="text" class="form-control text-center" style="max-width: 60px;" @keyup.enter="openHelp('ORDER')" />
                    <button class="btn btn-outline-secondary" @click="openHelp('ORDER')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th>품목(모델)</th>
                <td>
                  <select v-model="masterData.modelcd" class="form-select form-select-sm" style="width: 180px;">
                    <option value="0000000">전체</option>
                    <option v-for="m in modelOptions" :key="m.itemcd" :value="m.itemcd">{{ m.itemnm }}</option>
                  </select>
                </td>
                <th class="required">입고창고</th>
                <td>
                  <select v-model="masterData.iwhcd" class="form-select form-select-sm" style="width: 140px;">
                    <option v-for="opt in whOptions" :key="opt.code" :value="opt.code">{{ opt.cdnm }}</option>
                  </select>
                </td>
                <td colspan="2" class="bg-light">
                  <div class="d-flex gap-2 justify-content-center">
                    <button class="btn btn-xs btn-primary px-2" style="height: 26px;" @click="generatebom">
                      <i class="bi bi-diagram-3-fill me-1"></i> BOM전개
                    </button>
                    <button class="btn btn-xs btn-info text-white px-2" style="height: 26px;" @click="importData">
                      <i class="bi bi-download me-1"></i> 불러오기
                    </button>
                  </div>
                </td>
              </tr>
              <tr>
                <th>특기사항</th>
                <td colspan="7">
                  <input v-model="masterData.remark" type="text" class="form-control form-control-sm" placeholder="비고 입력" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 🅱️ 자재 그리드 영역 -->
      <div class="card border-0 shadow-sm flex-grow-1 overflow-hidden d-flex flex-column" style="border-radius: 8px;">
        <div class="card-header bg-white py-2 px-3 border-bottom d-flex justify-content-between align-items-center">
          <span class="fw-bold small text-dark"><i class="bi bi-list-ul me-1 text-primary"></i> 타계정 출고 자재 내역</span>
          <div class="btn-group gap-1">
            <button class="btn btn-sm btn-outline-primary px-3" style="height: 28px; font-size: 12px; font-weight: 600;" @click="addRow">
              <i class="bi bi-plus-circle me-1"></i> 행추가
            </button>
            <button class="btn btn-sm btn-outline-danger px-3" style="height: 28px; font-size: 12px; font-weight: 600;" @click="deleteSelectedRows">
              <i class="bi bi-dash-circle me-1"></i> 행삭제
            </button>
          </div>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="gridElement" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>
    </div>
    <Modal v-model:visible="modalVisible" :modalProps="modalProps" />
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, computed, nextTick, watch } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import type { ModalProps } from '@/types/modal'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

const now = new Date()
const initym = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}`
const initymd = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}`

// 1. 상태 관리
const masterData = reactive({
  deptcd: authStore.deptcd, deptnm: authStore.deptnm,
  ioym: initym, iono: '',
  ioymd: initymd,
  whcd: '200',
  iwhcd: '100',
  ordym: initym, ordno: '',
  modelcd: '0000000',
  remark: ''
})

const whOptions = ref<any[]>([])
const modelOptions = ref<any[]>([])
const closingInfo = reactive({ clsymd: '', sclsym: '', PCLSym: '' })

const uiioym = computed({ get: () => `${masterData.ioym.substring(0, 4)}-${masterData.ioym.substring(4, 6)}`, set: (v) => { if (v) masterData.ioym = v.replace(/-/g, '') } })
const uiioymd = computed({ get: () => formatDateString(masterData.ioymd, '-'), set: (v) => { if (v) masterData.ioymd = v.replace(/-/g, '') } })
const uiordym = computed({ get: () => `${masterData.ordym.substring(0, 4)}-${masterData.ordym.substring(4, 6)}`, set: (v) => { if (v) masterData.ordym = v.replace(/-/g, '') } })

const gridElement = ref<HTMLElement | null>(null)
let grid: Tabulator | null = null
const itemCount = ref(0)

// 주문번호 변경 시 모델 옵션 로드
watch(() => [masterData.ordym, masterData.ordno], async ([ym, no]) => {
  if (ym && no && String(no).length >= 3) {
    try {
        const res = await api.get('/prod/codes/ORDER_MODELS', { params: { cmpycd: authStore.cmpycd, ordym: ym, ordno: no } })
        modelOptions.value = res.data
    } catch (e) { modelOptions.value = [] }
  } else {
    modelOptions.value = []
  }
})

// 2. 그리드 초기화
const initGrid = () => {
  if (gridElement.value) {
    grid = new Tabulator(gridElement.value, {
      layout: "fitColumns", height: "100%", selectable: true,
      columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
      columns: [
        { title: "선택", width: 40, hozAlign: "center", formatter: "rowSelection", titleFormatter: "rowSelection", headerHozAlign: "center" },
        { title: "상태", field: "_status", width: 60, hozAlign: "center", formatter: (c) => {
            const v = c.getValue();
            if (v === '입력') return '<span class="badge bg-primary">입력</span>';
            if (v === '수정') return '<span class="badge bg-warning text-dark">수정</span>';
            if (v === '삭제') return '<span class="badge bg-danger">삭제</span>';
            return '';
        }},
        { title: "자재코드", field: "itemcd", width: 120, hozAlign: "center" },
        {
          title: "원(부)자재명", field: "itemnm", minWidth: 250,
          cellClick: (e, cell) => openHelp('GRID_ITEM', cell)
        },
        { title: "규격", field: "itsize", width: 180 },
        { title: "단위", field: "unit", width: 80, hozAlign: "center" },
        { title: "재고수량", field: "jqty", width: 120, hozAlign: "right", formatter: "money" },
        { title: "출고수량", field: "ioqty", width: 120, hozAlign: "right", editor: "number", formatter: "money", cssClass: "bg-light-yellow fw-bold" },
        { title: "비고", field: "remark", width: 200, editor: "input" },
        { title: "삭제", width: 60, hozAlign: "center", formatter: "buttonCross", cellClick: (e, cell) => cell.getRow().delete() }
      ]
    })
    grid.on("cellEdited", (cell) => {
        const row = cell.getRow()
        const d = row.getData()
        if (d._state === 'EXIST' && !d._status) row.update({ _status: '수정' })
    })
  }
}

// 3. 비즈니스 로직
const fetchWhOptions = async () => {
  try {
    const res = await api.get('/comm/codes/WH')
    whOptions.value = res.data
  } catch (e) {}
}

const fetchMaster = async () => {
  if (!masterData.ioym || !masterData.iono) return vAlertError('출고번호를 입력하세요.')
  try {
    const res = await api.post('/hpio/HPIO_530U_STR', {
      actkind: 'S', cmpycd: authStore.cmpycd, iogbn: '200',
      ioym: masterData.ioym, iono: masterData.iono
    })
    if (res.data && res.data.length > 0) {
      Object.assign(masterData, res.data[0])
      fetchDetails()
      vAlert('조회되었습니다.')
    } else {
      vAlert('조회된 데이터가 없습니다.')
    }
  } catch (e) { vAlertError('마스터 조회 실패') }
}

const fetchDetails = async () => {
  try {
    const res = await api.post('/hpio/HPIO_501U_STR', { // Detail은 501U 공동사용
      actkind: 'S', cmpycd: authStore.cmpycd, iogbn: '200',
      ioym: masterData.ioym, iono: masterData.iono
    })
    const mapped = (res.data || []).map((i: any) => ({ ...i, _state: 'EXIST', _status: '' }))
    grid?.setData(mapped)
    itemCount.value = mapped.length
  } catch (e) {}
}

const generatebom = async () => {
    if (!masterData.ordym || !masterData.ordno) return vAlertError('주문번호를 입력하세요.')
    if (!confirm('BOM 전개를 실행하시겠습니까?')) return
    try {
        await api.post('/hpio/HPIO_530U_STR', {
            actkind: 'G', cmpycd: authStore.cmpycd,
            ordym: masterData.ordym, ordno: masterData.ordno
        })
        vAlert('BOM 전개가 완료되었습니다.')
        importData()
    } catch (e) { vAlertError('BOM 전개 실패') }
}

const importData = async () => {
    if (!masterData.ordym || !masterData.ordno) return vAlertError('주문번호를 입력하세요.')
    try {
        const res = await api.post('/hpio/HPIO_530S_STR', {
            cmpycd: authStore.cmpycd,
            ordym: masterData.ordym, ordno: masterData.ordno,
            modelcd: masterData.modelcd, whcd: masterData.whcd
        })
        const mapped = res.data.map((i: any) => ({ ...i, _state: 'NEW', _status: '입력', ioqty: i.reqqty }))
        grid?.setData(mapped)
        itemCount.value = mapped.length
        vAlert('데이터를 불러왔습니다.')
    } catch (e) { vAlertError('데이터 로드 실패') }
}

const saveData = async () => {
  const ioYmd = masterData.ioymd.replace(/-/g, '')
  if (ioYmd.substring(0, 6) <= closingInfo.PCLSym) return vAlertError('생산 마감된 월입니다.')
  if (ioYmd.substring(0, 6) <= closingInfo.sclsym) return vAlertError('영업 마감된 월입니다.')

  const details = grid?.getData() || []
  if (details.length === 0) return vAlertError('항목이 없습니다.')

  if (!confirm('저장하시겠습니까?')) return

  try {
    const actkind = !masterData.iono ? 'A' : 'U'
    const resM = await api.post('/hpio/HPIO_500U_STR', { // Save Logic은 500U 공동사용
      ...masterData, actkind: actkind, cmpycd: authStore.cmpycd, userid: authStore.userid,
      iogbn: '200', PRODCD: '100', linecd: '010', progcd: '888'
    })

    const newIono = resM.data[0].iono
    const newInno = resM.data[0].INNO

    for (const item of details) {
        if (!item.itemcd) continue

        let act = item.upkind;
        if (item._status === '입력') act = 'A';
        else if (item._status === '삭제') act = 'D';
        else if (item._status === '수정') act = 'U';
        else act = !masterData.iono ? 'A' : 'U';

        await api.post('/hpio/HPIO_501U_STR', {
            ...item, actkind: act, cmpycd: authStore.cmpycd, userid: authStore.userid,
            iogbn: '200', ioym: masterData.ioym, iono: newIono, INNO: newInno,
            deptcd: masterData.deptcd, whcd: masterData.whcd, iwhcd: masterData.iwhcd, ioymd: ioYmd,
            linecd: '010', progcd: '888'
        })
    }
    vAlert('정상적으로 저장되었습니다.')
    masterData.iono = newIono
    fetchMaster()
  } catch (e) { vAlertError('저장 중 오류 발생') }
}

const deleteData = async () => {
    if (!confirm('삭제하시겠습니까?')) return
    try {
        await api.post('/hpio/HPIO_500U_STR', { ...masterData, actkind: 'D', cmpycd: authStore.cmpycd })
        vAlert('삭제되었습니다.')
        initialize()
    } catch (e) { vAlertError('삭제 실패') }
}

const addRow = () => { grid?.addRow({ _status: '입력', _state: 'NEW', itemcd: '', itemnm: '', ioqty: 0 }, true) }

const deleteSelectedRows = () => {
  const selectedRows = grid?.getSelectedRows() || [];
  if (selectedRows.length === 0) return vAlertError('삭제할 행을 선택하세요.');
  selectedRows.forEach(row => {
    const d = row.getData();
    if (d._state === 'NEW') row.delete();
    else row.update({ _status: d._status === '삭제' ? '' : '삭제' });
  });
}

const initialize = () => {
  resetForm(masterData)
  Object.assign(masterData, {
      deptcd: authStore.deptcd, deptnm: authStore.deptnm,
      ioym: initym, iono: '', ioymd: initymd,
      whcd: '200', iwhcd: '100',
      ordym: initym, ordno: '', modelcd: '0000000'
  })
  grid?.clearData(); itemCount.value = 0
}

// 4. 도움창 (Modal)
const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function openHelp(type: string, cell?: any) {
  let config: any = {}
  if (type === 'DEPT') {
    config = { title: '부서 선택', path: '/ha00/HA00_00P_STR', defaultField: 'cdnm', data: { gubun: 'D0', cmpycd: authStore.cmpycd }, columns: [{ title: '코드', field: 'code', width: 80 }, { title: '부서명', field: 'cdnm', width: 150 }], onConfirm: (data: any) => { masterData.deptcd = data.code; masterData.deptnm = data.cdnm } }
  } else if (type === 'ORDER') {
    config = { title: '주문 선택', path: '/ha00/HA00_00P_STR', defaultField: 'ordno', data: { gubun: 'ORDER', cmpycd: authStore.cmpycd }, columns: [{ title: '연월', field: 'ordym', width: 80 }, { title: '번호', field: 'ordno', width: 60 }, { title: '거래처', field: 'custnm', width: 150 }], onConfirm: (data: any) => { masterData.ordym = data.ordym; masterData.ordno = data.ordno } }
  } else if (type === 'GRID_ITEM') {
    config = { title: '자재 선택', path: '/ha00/HA00_00P_STR', defaultField: 'itemnm', data: { gubun: 'I1', cmpycd: authStore.cmpycd, iogbn: 'I' }, columns: [{ title: '코드', field: 'itemcd', width: 100 }, { title: '자재명', field: 'itemnm', width: 250 }], onConfirm: (data: any) => { cell.getRow().update({ itemcd: data.itemcd, itemnm: data.itemnm, itsize: data.itsize, unit: data.unit, qtypnt: data.qtypnt }) } }
  }
  Object.assign(modalProps, config); modalVisible.value = true
}

const formatDateString = (v: any, sep: string) => v && String(v).length === 8 ? `${v.substring(0, 4)}${sep}${v.substring(4, 6)}${sep}${v.substring(6, 8)}` : (v || '')
const formatNumber = (val: any) => new Intl.NumberFormat().format(Number(val) || 0)

onMounted(async () => {
  api.get('/hp00/HP00_000S_STR', { params: { gubun: 'CL', cmpycd: authStore.cmpycd } }).then(r => {
    if (r.data?.length) {
      closingInfo.clsymd = String(Object.values(r.data[0])[0]).trim()
      closingInfo.sclsym = String(Object.values(r.data[0])[1]).trim()
      closingInfo.PCLSym = String(Object.values(r.data[0])[2]).trim()
    }
  })
  fetchWhOptions()
  nextTick(() => initGrid())
})
</script>
