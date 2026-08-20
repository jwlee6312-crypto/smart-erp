<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-gift-fill me-2 text-primary" style="font-size: 18px;"></i>
        구매정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        매입정산 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        <span class="text-primary fw-bolder">장려 및 재고보상금 관리 (HSOD210U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-save" @click="save">저장</button>
        <button class="btn-erp btn-danger" @click="deleteData" :disabled="formData.actkind !== 'U'">삭제</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2">
      <!-- 🅰️ 조회 조건 영역 -->
      <div class="card border shadow-sm overflow-hidden flex-shrink-0">
        <div class="card-body p-0">
          <table class="erp-table-full border-0">
            <tbody>
              <tr>
                <th>구&nbsp;&nbsp;&nbsp;&nbsp;분</th>
                <td>
                  <select v-model="searchData.gbn" class="form-select form-select-sm" style="width: 150px;">
                    <option value="001">판매장려금</option>
                    <option value="002">재고보상금</option>
                  </select>
                </td>
                <th class="required">등록일자</th>
                <td>
                  <div class="d-flex align-items-center gap-1" style="width: 260px;">
                    <input v-model="uifromdt" type="date" class="form-control form-control-sm" />
                    <span class="px-1">~</span>
                    <input v-model="uitodt" type="date" class="form-control form-control-sm" />
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 🅱️ 입력 폼 영역 -->
      <div class="card border shadow-sm overflow-hidden flex-shrink-0">
        <div class="card-body p-0">
          <table class="erp-table-full border-0">
            <tbody>
              <tr>
                <th class="required">구&nbsp;&nbsp;&nbsp;&nbsp;분</th>
                <td>
                  <select v-model="formData.gbn" class="form-select form-select-sm" style="width: 150px;">
                    <option value="001">판매장려금</option>
                    <option value="002">재고보상금</option>
                  </select>
                </td>
                <th class="required">거&nbsp;래&nbsp;처</th>
                <td>
                  <div class="input-group input-group-sm" style="width: 250px;">
                    <input v-model="formData.custcd" type="text" class="form-control text-center bg-light fw-bold" style="max-width: 80px;" readonly />
                    <input v-model="formData.custnm" type="text" class="form-control border-start-0" placeholder="거래처 선택" @keyup.enter="handleOpenHelp('CUST')" />
                    <button class="btn btn-outline-secondary px-2" @click="handleOpenHelp('CUST')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th>부&nbsp;&nbsp;&nbsp;&nbsp;서</th>
                <td>
                  <div class="input-group input-group-sm" style="width: 250px;">
                    <input v-model="formData.deptcd" type="text" class="form-control text-center bg-light fw-bold" style="max-width: 80px;" readonly />
                    <input v-model="formData.deptnm" type="text" class="form-control border-start-0" placeholder="부서 선택" @keyup.enter="handleOpenHelp('DEPT')" />
                    <button class="btn btn-outline-secondary px-2" @click="handleOpenHelp('DEPT')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
              </tr>
              <tr>
                <th class="required">등록일자</th>
                <td>
                  <input v-model="uiioymd" type="date" class="form-control form-control-sm" style="width: 150px;" />
                </td>
                <th class="required">금&nbsp;&nbsp;&nbsp;&nbsp;액</th>
                <td>
                  <input v-model.number="formData.ioamt" type="number" class="form-control form-control-sm text-end" style="width: 150px;" @input="calculateVat" />
                </td>
                <th>부&nbsp;가&nbsp;세</th>
                <td>
                  <input v-model.number="formData.iovat" type="number" class="form-control form-control-sm text-end" style="width: 150px;" />
                </td>
              </tr>
              <tr>
                <th>비&nbsp;&nbsp;&nbsp;&nbsp;고</th>
                <td colspan="5">
                  <input v-model="formData.remark" type="text" class="form-control form-control-sm" maxlength="100" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 🅲 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column">
        <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark d-flex justify-content-between align-items-center">
          <span><i class="bi bi-list-check me-1 text-primary"></i> 처리 내역 ({{ itemCount }} 건)</span>
          <span class="text-muted" style="font-size: 11px;">※ 목록에서 행을 클릭하면 수정 모드로 전환됩니다.</span>
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
import { useCommonHelp } from '@/composables/useCommonHelp'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps, openHelp } = useCommonHelp()

const now = new Date()
const initymd = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}`
const initfromdt = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}01`

// 1. 상태 관리
const searchData = reactive({
  gbn: '001',
  fromdt: initfromdt,
  todt: initymd
})

const formData = reactive<any>({
  actkind: 'A',
  gbn: '001',
  custcd: '', custnm: '',
  deptcd: authStore.deptcd, deptnm: authStore.deptnm,
  ioymd: initymd,
  ioamt: 0,
  iovat: 0,
  remark: '',
  ioym: '', iono: '' // 수정용 키값
})

const uifromdt = computed({ get: () => formatDateString(searchData.fromdt, '-'), set: (v) => searchData.fromdt = v.replace(/-/g, '') })
const uitodt = computed({ get: () => formatDateString(searchData.todt, '-'), set: (v) => searchData.todt = v.replace(/-/g, '') })
const uiioymd = computed({ get: () => formatDateString(formData.ioymd, '-'), set: (v) => formData.ioymd = v.replace(/-/g, '') })

const gridElement = ref<HTMLElement | null>(null)
let grid: Tabulator | null = null
const itemCount = ref(0)

// 2. 그리드 초기화
const initGrid = () => {
  if (gridElement.value) {
    grid = new Tabulator(gridElement.value, {
      layout: "fitColumns",
      height: "100%",
      placeholder: "조회된 데이터가 없습니다.",
      columnDefaults: { headerSort: false, headerHozAlign: 'center' },
      columns: [
        { title: "구분", field: "gbn", width: 100, hozAlign: "center", formatter: (c) => c.getValue() === '001' ? '판매장려금' : '재고보상금' },
        { title: "거래처 상호", field: "custnm", minWidth: 200, widthGrow: 1, cssClass: 'fw-bold' },
        { title: "등록일", field: "ioymd", width: 120, hozAlign: "center", formatter: (c) => formatDateString(c.getValue(), '-') },
        { title: "금액", field: "ioamt", width: 130, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
        { title: "부가세", field: "iovat", width: 110, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
        { title: "비고", field: "remark", width: 250, hozAlign: "left" },
      ],
    })

    grid.on("rowClick", (e, row) => {
      const data = row.getData()
      Object.assign(formData, data)
      formData.actkind = 'U'
    })
  }
}

// 3. 비즈니스 로직
const calculateVat = () => {
  formData.iovat = Math.floor(Number(formData.ioamt || 0) * 0.1)
}

const search = async () => {
  try {
    const res = await api.post('/hsod/HSOD_210U_STR', {
      actkind: 'S',
      cmpycd: authStore.cmpycd,
      gbn: searchData.gbn,
      fromdt: searchData.fromdt,
      todt: searchData.todt
    })
    grid?.setData(res.data)
    itemCount.value = res.data.length
    vAlert('조회되었습니다.')
  } catch (e) {
    vAlertError('조회 중 오류가 발생했습니다.')
  }
}

const save = async () => {
  if (!formData.custcd) return vAlertError('거래처를 선택하세요.')
  if (!formData.ioamt) return vAlertError('금액을 입력하세요.')

  if (!confirm('자료를 저장하시겠습니까?')) return

  try {
    const res = await api.post('/hsod/HSOD_210U_STR', {
      ...formData,
      cmpycd: authStore.cmpycd,
      userid: authStore.userid
    })

    if (res.data?.[0]?.erryn === 'Y' || (res.data?.[0]?.rtn_cd && res.data[0].rtn_cd !== '000000')) {
      vAlertError(res.data[0].rtn_msg || '저장 중 오류 발생')
    } else {
      vAlert('저장되었습니다.')
      search()
      initialize()
    }
  } catch (e) {
    vAlertError('저장 실패')
  }
}

const deleteData = async () => {
  if (formData.actkind !== 'U') return
  if (!confirm('해당 자료를 삭제하시겠습니까?')) return

  try {
    await api.post('/hsod/HSOD_210U_STR', {
      actkind: 'D',
      cmpycd: authStore.cmpycd,
      ioym: formData.ioym,
      iono: formData.iono
    })
    vAlert('삭제되었습니다.')
    search()
    initialize()
  } catch (e) {
    vAlertError('삭제 실패')
  }
}

const initialize = () => {
  resetForm(formData)
  Object.assign(formData, {
    actkind: 'A',
    gbn: searchData.gbn,
    deptcd: authStore.deptcd,
    deptnm: authStore.deptnm,
    ioymd: initymd,
    ioamt: 0,
    iovat: 0
  })
}

// 4. 도움창 핸들러 (표준화)
const handleOpenHelp = (type: string) => {
  if (type === 'CUST') {
    openHelp('CUST', (data: any) => {
      formData.custcd = data.custcd;
      formData.custnm = data.custnm;
    });
  } else if (type === 'DEPT') {
    openHelp('DEPT', (data: any) => {
      formData.deptcd = data.deptcd;
      formData.deptnm = data.deptnm;
    });
  }
}

const formatDateString = (v: any, sep: string) => v && v.length === 8 ? `${v.substring(0, 4)}${sep}${v.substring(4, 6)}${sep}${v.substring(6, 8)}` : v

onMounted(() => {
  nextTick(() => initGrid())
  search()
})
</script>
