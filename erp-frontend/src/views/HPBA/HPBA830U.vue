<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-box-seam me-2 text-primary" style="font-size: 18px;"></i>
        기초자료 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        <span class="text-primary fw-bolder">재공기초재고수량 등록 (HPBA830U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-2">
        <button class="btn-erp btn-init" @click="initialize">
          <i class="bi bi-arrow-clockwise"></i> 초기화
        </button>
        <button class="btn-erp btn-search" @click="fetchList">
          <i class="bi bi-search"></i> 조회
        </button>
        <button class="btn-erp btn-save" @click="saveData">
          <i class="bi bi-save"></i> 저장
        </button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-3">
      <!-- 🅰️ 입력 폼 영역 -->
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
                <th class="required">연&nbsp;&nbsp;&nbsp;&nbsp;월</th>
                <td>
                  <div class="d-flex align-items-center gap-2" style="width: 200px;">
                    <select v-model="formData.yy" class="form-select form-select-sm">
                      <option v-for="year in yearOptions" :key="year" :value="year">{{ year }}년</option>
                    </select>
                    <select v-model="monthStr" class="form-select form-select-sm">
                      <option v-for="month in monthOptions" :key="month" :value="month">{{ month }}월</option>
                    </select>
                  </div>
                </td>
                <th class="required">생산라인</th>
                <td>
                  <select v-model="formData.linecd" class="form-select form-select-sm" style="width: 150px;" @change="onLineChange">
                    <option value="">라인 선택</option>
                    <option v-for="opt in lineOptions" :key="opt.linecd" :value="opt.linecd">
                      [{{ opt.linecd }}] {{ opt.linenm }}
                    </option>
                  </select>
                </td>
                <th class="required">생산공정</th>
                <td>
                   <select v-model="formData.progcd" class="form-select form-select-sm" style="width: 180px;">
                    <option value="">공정 선택</option>
                    <option v-for="opt in progOptions" :key="opt.progcd" :value="opt.progcd">
                      [{{ opt.progcd }}] {{ opt.prognm }}
                    </option>
                  </select>
                </td>
              </tr>
              <tr>
                <th class="required">품&nbsp;&nbsp;&nbsp;&nbsp;목</th>
                <td colspan="3">
                  <div class="input-group input-group-sm" style="width: 480px;">
                    <input v-model="formData.itemcd" type="text" class="form-control text-center bg-light" style="max-width: 80px;" readonly />
                    <input v-model="formData.itemnm" type="text" class="form-control" placeholder="품목 선택" @keyup.enter="openHelp('ITEM')" />
                    <input v-model="formData.itsize" type="text" class="form-control bg-light" style="max-width: 120px;" readonly />
                    <input v-model="formData.unit" type="text" class="form-control bg-light text-center" style="max-width: 50px;" readonly />
                    <button class="btn btn-outline-secondary" @click="openHelp('ITEM')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="required">재고수량</th>
                <td colspan="3">
                  <div class="d-flex align-items-center gap-3">
                    <input v-model="formData.qty" type="number" class="form-control form-control-sm text-end border-primary" style="width: 150px;" />
                    <span class="text-muted small"><i class="bi bi-info-circle me-1"></i> 해당 공정에 남아 있는 순수 재고수량만 입력하십시오.</span>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 🅲 그리드 영역 -->
      <div class="card border-0 shadow-sm flex-grow-1 overflow-hidden d-flex flex-column" style="border-radius: 8px;">
        <div class="card-header bg-white py-2 px-3 border-bottom d-flex justify-content-between align-items-center">
          <span class="fw-bold small text-dark"><i class="bi bi-table me-1 text-primary"></i> 재공 기초 재고 등록 내역</span>
          <span class="text-muted small">목록을 클릭하면 수정 모드로 전환됩니다.</span>
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
import { reactive, ref, onMounted, computed, nextTick } from 'vue'
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

// 1. 상태 관리
const formData = reactive({
  actkind: 'A0',
  yy: String(now.getFullYear()),
  mm: now.getMonth() + 1,
  linecd: '010',
  linenm: '통합라인',
  progcd: '',
  prognm: '',
  itemcd: '',
  itemnm: '',
  itsize: '',
  unit: '',
  qty: 0
})

const lineOptions = ref<any[]>([])
const progOptions = ref<any[]>([])

const monthStr = computed({
    get: () => String(formData.mm).padStart(2, '0'),
    set: (v) => { formData.mm = Number(v) }
})

const yearOptions = ref<string[]>([])
const monthOptions = ['01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12']

const gridElement = ref<HTMLElement | null>(null)
let grid: Tabulator | null = null
const itemCount = ref(0)

const generateYearOptions = () => {
    const currentYear = new Date().getFullYear()
    for (let i = 0; i < 5; i++) yearOptions.value.push(String(currentYear - i))
}

const fetchLineOptions = async () => {
  try {
    const res = await api.get('/hp00/HP00_000S_STR', { params: { gubun: 'L0', cmpycd: authStore.cmpycd, gbncd: 'Y' } })
    lineOptions.value = res.data
  } catch (e) {}
}

const fetchProgOptions = async (lineCd: string) => {
  if (!lineCd) { progOptions.value = []; return; }
  try {
    const res = await api.get('/hp00/HP00_000S_STR', { params: { gubun: 'G0', cmpycd: authStore.cmpycd, linecd: lineCd } })
    progOptions.value = res.data
  } catch (e) {}
}

const onLineChange = () => {
    formData.progcd = ''
    fetchProgOptions(formData.linecd)
}

const initGrid = () => {
  if (gridElement.value) {
    grid = new Tabulator(gridElement.value, {
      layout: "fitColumns",
      height: "100%",
      placeholder: "조회된 데이터가 없습니다.",
      columns: [
        { title: "품목코드", field: "itemcd", width: 100, hozAlign: "center" },
        { title: "품 목", field: "itemnm", minWidth: 200, cssClass: "fw-bold" },
        { title: "규격", field: "itsize", width: 150 },
        { title: "단위", field: "unit", width: 70, hozAlign: "center" },
        { title: "기초재고수량", field: "stkqty", width: 130, hozAlign: "right", formatter: "money", formatterParams: { precision: (c:any)=>c.getData().qtypnt||0 }, cssClass: "text-primary fw-bold" },
        { title: "단가", field: "price", width: 100, hozAlign: "right", formatter: "money", formatterParams: { precision: 2 } },
        { title: "기초재고금액", field: "stkamt", width: 120, hozAlign: "right", formatter: "money" }
      ],
    })

    grid.on("rowClick", (e, row) => {
        const data = row.getData()
        Object.assign(formData, {
            ...data,
            actkind: 'U0',
            yy: data.ym.substring(0, 4),
            mm: Number(data.ym.substring(4, 6)),
            qty: data.stkqty
        })
        fetchProgOptions(formData.linecd)
    })
  }
}

const fetchList = async () => {
  if (!formData.linecd || !formData.progcd) return vAlertError('라인과 공정을 선택하세요.')
  try {
    const res = await api.post('/hpba/HPBA_830U_STR', {
      actkind: 'S0', cmpycd: authStore.cmpycd, yy: formData.yy, mm: monthStr.value,
      linecd: formData.linecd, progcd: formData.progcd, itemcd: formData.itemcd
    })
    const mapped = res.data.map((i: any) => ({
        ...i,
        ym: i.ym || (formData.yy + monthStr.value),
        price: Number(i.stkqty) !== 0 ? Number((Number(i.stkamt) / Number(i.stkqty)).toFixed(2)) : 0
    }))
    grid?.setData(mapped); itemCount.value = mapped.length; vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

const saveData = async () => {
  if (!formData.linecd || !formData.progcd) return vAlertError('라인과 공정을 선택하세요.')
  if (!formData.itemcd) return vAlertError('품목을 선택하세요.')
  if (formData.qty === 0) return vAlertError('수량을 입력하세요.')
  if (!confirm('재공 기초 재고 정보를 저장하시겠습니까?')) return
  try {
    await api.post('/hpba/HPBA_830U_STR', { ...formData, mm: monthStr.value, cmpycd: authStore.cmpycd, userid: authStore.userid })
    vAlert('정상적으로 저장되었습니다.'); fetchList(); initializeFormOnly()
  } catch (e) { vAlertError('저장 처리 중 오류 발생') }
}

const initializeFormOnly = () => {
    formData.actkind = 'A0'; formData.itemcd = ''; formData.itemnm = ''; formData.itsize = ''; formData.unit = ''; formData.qty = 0
}

const initialize = () => {
  resetForm(formData)
  Object.assign(formData, { actkind: 'A0', yy: String(now.getFullYear()), mm: now.getMonth() + 1, linecd: '010', linenm: '통합라인', qty: 0 })
  grid?.clearData(); itemCount.value = 0
  fetchProgOptions(formData.linecd)
}

const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function openHelp(type: string) {
  let config: any = {}
  if (type === 'ITEM') {
    config = {
      title: '품목 선택', path: '/ha00/HA00_00P_STR', defaultField: 'itemnm',
      data: { gubun: 'I0', cmpycd: authStore.cmpycd, codegbn: '210' },
      columns: [{ title: '코드', field: 'itemcd', width: 100 }, { title: '품목명', field: 'itemnm', width: 250 }, { title: '규격', field: 'itsize', width: 120 }],
      onConfirm: (data: any) => {
          formData.itemcd = data.itemcd;
          formData.itemnm = data.itemnm;
          formData.itsize = data.itsize;
          formData.unit = data.unit;
      }
    }
  }
  Object.assign(modalProps, config); modalVisible.value = true
}

const formatDateString = (v: any, sep: string) => v && String(v).length >= 6 ? `${v.substring(0, 4)}${sep}${v.substring(4, 6)}${v.length === 8 ? sep + v.substring(6, 8) : ''}` : (v || '')

onMounted(() => {
  generateYearOptions()
  fetchLineOptions()
  fetchProgOptions(formData.linecd)
  nextTick(() => { initGrid(); })
})
</script>
