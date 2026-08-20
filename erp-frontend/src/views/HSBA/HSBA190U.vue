<template>
  <appalert :show="showalert" :error="showerror" :message="alertmessage" />

  <!-- 💡 [진짜 표준] 최외각 wrapper: h-100 및 overflow-hidden으로 스크롤 고립 -->
  <div class="erp-container">
    <!-- 🚀 1. 상단 액션 바: flex-shrink-0으로 고정 -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-1 px-3 sticky-top shadow-sm flex-shrink-0">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 13px;">
        <i class="bi bi-person-badge-fill me-2 text-primary"></i>
        기본정보 > <span class="text-primary fw-bolder">거래처 담당자등록 (hsba190u)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1">
        <button class="btn-erp btn-init" @click="initialize(false)">초기화</button>
        <button class="btn-erp btn-search" @click="searchcustomers">조회</button>
        <button class="btn-erp btn-save" @click="save">저장</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역: flex-grow-1 및 overflow-hidden으로 하단 공백 제거 -->
    <div class="flex-grow-1 d-flex flex-column gap-2 p-2 overflow-hidden bg-light">
      <!-- 🅰️ 조회 조건 영역: flex-shrink-0 -->
      <div class="card border shadow-sm flex-shrink-0">
        <div class="card-body p-1 bg-white">
          <div class="d-flex align-items-center gap-3 px-2">
            <div class="input-group input-group-sm flex-nowrap" style="width: 350px;">
              <span class="input-group-text fw-bold border-0 bg-transparent">거래처검색</span>
              <input v-model="searchdata.qcustnm" type="text" class="form-control border-0 bg-light" placeholder="거래처명 또는 코드 입력" @keyup.enter="searchcustomers" />
              <button class="btn btn-dark btn-sm" @click="searchcustomers"><i class="bi bi-search"></i></button>
            </div>
          </div>
        </div>
      </div>

      <!-- 🅱️ 담당자 정보 입력 영역: flex-shrink-0 -->
      <div class="card border shadow-sm overflow-hidden flex-shrink-0">
        <div class="card-header py-1 px-3 border-bottom d-flex align-items-center justify-content-between" style="background-color: #f0f7ff !important;">
          <span class="fw-bold small text-primary"><i class="bi bi-pencil-square me-1"></i> 담당자 정보 입력</span>
          <div class="d-flex gap-2 align-items-center">
            <span v-if="masterdata.actkind === 'U0'" class="badge bg-warning text-dark" style="font-size: 10px;">수정 모드 (no: {{ masterdata.rowno }})</span>
            <span v-else class="badge bg-primary" style="font-size: 10px;">신규 등록</span>
          </div>
        </div>
        <div class="card-body p-0 bg-white">
          <table class="erp-table-full border-0">
            <tbody>
              <tr>
                <th style="width: 80px;">거 래 처</th>
                <td style="width: 350px;">
                  <div class="input-group input-group-sm">
                    <input v-model="masterdata.custcd" type="text" class="form-control text-center bg-light" style="max-width: 90px;" readonly />
                    <input v-model="masterdata.custnm" type="text" class="form-control bg-light" placeholder="거래처 선택" readonly />
                  </div>
                </td>
                <th class="required" style="width: 70px;">부&nbsp;&nbsp;&nbsp;&nbsp;서</th>
                <td style="width: 250px;">
                  <input v-model="masterdata.deptnm" type="text" class="form-control form-control-sm" maxlength="30" />
                </td>
                <th class="required" style="width: 70px;">담 당 자</th>
                <td style="width: 150px;">
                  <input v-model="masterdata.damdang" type="text" class="form-control form-control-sm" maxlength="50" />
                </td>
                <th style="width: 70px;">전화번호</th>
                <td style="width: 150px;">
                  <input v-model="masterdata.telno" type="text" class="form-control form-control-sm" maxlength="30" />
                </td>
                <th style="width: 70px;">메일주소</th>
                <td>
                  <input v-model="masterdata.email" type="text" class="form-control form-control-sm" maxlength="50" style="ime-mode:inactive" />
                </td>
                <th style="width: 50px;">사용</th>
                <td style="width: 60px;">
                  <div class="form-check form-switch m-0 d-flex justify-content-center">
                    <input v-model="masterdata.useyn" class="form-check-input" type="checkbox" id="useyn190" true-value="Y" false-value="N">
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Ⓒ 하단 목록 영역: flex-grow-1 및 min-height: 0으로 남은 공간 꽉 채움 -->
      <div class="d-flex flex-grow-1 gap-2 overflow-hidden" style="min-height: 0;">
        <!-- 좌측: 거래처 목록 -->
        <div class="card border shadow-sm d-flex flex-column overflow-hidden flex-shrink-0" style="width: 320px;">
          <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark">
            <i class="bi bi-building me-1"></i> 거래처 목록
          </div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
              <div ref="custgridelement" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>

        <!-- 우측: 담당자 내역 목록 -->
        <div class="card border shadow-sm flex-grow-1 d-flex flex-column overflow-hidden">
          <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
            <span class="fw-bold small text-dark d-flex align-items-center">
              <i class="bi bi-person-lines-fill me-2 text-primary"></i> 담당자 설정 내역
              <span v-if="selectedcustname" class="badge bg-primary-subtle text-primary border border-primary-subtle ms-2">{{ selectedcustname }}</span>
            </span>
          </div>
            <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
                <div ref="damgridelement" class="tabulator-instance flex-grow-1"></div>
            </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, nextTick } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import appalert from '@/components/AppAlert.vue'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'

const authstore = useAuthStore()
const { showAlert: showalert, showError: showerror, alertMessage: alertmessage, vAlert: valert, vAlertError: valerterror } = useAlerts()
const { resetForm: resetform } = useFormReset()

const searchdata = reactive({ qcustnm: '' })
const masterdata = reactive<any>({
  actkind: 'A0', cmpycd: authstore.cmpycd, custcd: '', custnm: '', rowno: '',
  deptnm: '', damdang: '', telno: '', email: '', useyn: 'Y', updemp: authstore.userid
})

const selectedcustname = ref('')
const custgridelement = ref<HTMLElement | null>(null)
const damgridelement = ref<HTMLElement | null>(null)
let custgrid: Tabulator | null = null
let damgrid: Tabulator | null = null

const initgrids = () => {
  if (custgridelement.value) {
    custgrid = new Tabulator(custgridelement.value, {
      layout: "fitColumns", height: "100%", placeholder: "데이터 없음",
      pagination: "local", paginationSize: 20, paginationButtonCount: 3,
      columnDefaults: { headerSort: false, headerHozAlign: "center", hozAlign: "center", vertAlign: "middle" },
      columns: [
        {
          title: "거래처 상호", field: "custnm", hozAlign: "left",
          formatter: (cell) => {
            const v = cell.getValue() || '';
            return cell.getData().useyn === 'Y' ? `<span class="text-primary fw-bold">${v}</span>` : v;
          }
        }
      ]
    })
    custgrid.on("rowClick", (e, row) => {
      const data = row.getData()
      masterdata.custcd = data.custcd;
      masterdata.custnm = data.custnm;
      selectedcustname.value = data.custnm;
      initialize(true)
      fetchdamlist()
    })
  }

  if (damgridelement.value) {
    damgrid = new Tabulator(damgridelement.value, {
      layout: "fitColumns", height: "100%", placeholder: "내역 없음",
      columnDefaults: { headerSort: false, headerHozAlign: "center", hozAlign: "center", vertAlign: "middle" },
      columns: [
        { title: "no", field: "rowno", width: 80 },
        { title: "부서명", field: "deptnm", width: 200, hozAlign: "left" },
        { title: "담당자명", field: "damdang", width: 150 },
        { title: "전화번호", field: "telno", width: 150 },
        { title: "메일주소", field: "email", minWidth: 200, hozAlign: "left" },
        { title: "사용", field: "useyn", width: 80, hozAlign: "center",  editor: true,
          formatter: (cell) => {
            const val = String(cell.getValue() || '').trim().toUpperCase();
            return val === 'Y' ? '<b class="text-primary">사용</b>' : '';
          }
        }
      ]
    })
    damgrid.on("rowClick", (e, row) => {
      const data = row.getData();
      Object.assign(masterdata, data);
      masterdata.actkind = 'U0';
    })
  }
}

async function searchcustomers() {
  try {
    const res = await api.post('/hsba/hsba_190u_str', {
      actkind: 'S1', cmpycd: authstore.cmpycd, custcd: '', custnm: searchdata.qcustnm || '',
      rowno: '', deptnm: '', damdang: '', telno: '', email: '', useyn: '', userid: authstore.userid
    })
    custgrid?.setData(res.data || []); damgrid?.clearData(); selectedcustname.value = ''
  } catch (e) { valerterror('조회 실패') }
}

async function fetchdamlist() {
  try {
    const res = await api.post('/hsba/hsba_190u_str', {
      actkind: 'S0', cmpycd: authstore.cmpycd, custcd: masterdata.custcd,
      custnm: '', rowno: '', deptnm: '', damdang: '', telno: '', email: '', useyn: '', userid: authstore.userid
    })
    damgrid?.setData(res.data || [])
  } catch (e) { valerterror('내역 조회 실패') }
}

async function save() {
  if (!masterdata.custcd) return valerterror('거래처를 먼저 선택하세요.')
  if (!masterdata.damdang) return valerterror('담당자명은 필수입니다.')
  if (!confirm('저장하시겠습니까?')) return
  try {
    const payload = { ...masterdata, telno: (masterdata.telno || '').replace(/-/g, ''), userid: authstore.userid }
    const res = await api.post('/hsba/hsba_190u_str', payload)
    const resdata = res.data?.[0];
    if (resdata?.result === 'N') return valerterror(resdata.msg || '저장 실패')

    valert('정상 처리되었습니다.'); fetchdamlist(); if (masterdata.actkind === 'A0') initialize(true)
  } catch (e) { valerterror('저장 실패') }
}

function initialize(keepcust = false) {
  const cd = masterdata.custcd; const nm = masterdata.custnm; resetform(masterdata)
  Object.assign(masterdata, { actkind: 'A0', cmpycd: authstore.cmpycd, useyn: 'Y', updemp: authstore.userid })
  if (keepcust) { masterdata.custcd = cd; masterdata.custnm = nm } else { selectedcustname.value = ''; damgrid?.clearData() }
}

onMounted(() => { nextTick(() => { initgrids(); searchcustomers() }) })
</script>
