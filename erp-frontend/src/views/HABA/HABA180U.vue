<!--
	=============================================================
	프로그램명	: 거래처관리 (haba180u)
	작성일자	: 2025.03.14
	설명        : 거래처 정보 상세 관리 (완전 소문자 원칙 적용 및 네비게이션 대응)
	=============================================================
-->

<template>
  <AppAlert :show="showalert" :error="showerror" :message="alertmessage" />

  <div class="erp-container h-100 d-flex flex-column bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-1 px-3 sticky-top shadow-sm flex-shrink-0">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 13px;">
        <i class="bi bi-briefcase-fill me-2 text-primary"></i>
        기본정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        기초관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">거래처관리 (haba180u)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-save" @click="save">저장</button>
        <button class="btn-erp btn-excel" @click="excel">엑셀</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 d-flex flex-column gap-2 p-2 overflow-hidden bg-light">
      <!-- 🅰️ 조회 조건 영역 -->
      <div class="card border shadow-sm flex-shrink-0">
        <div class="card-body p-1 bg-white">
          <div class="d-flex align-items-center gap-3">
            <div class="input-group input-group-sm flex-nowrap" style="width: 180px;">
              <span class="input-group-text fw-bold border-0 bg-transparent">종류</span>
              <select v-model="searchform.qcustgbn" class="form-select border-0 bg-light" @change="search">
                <option value="000">전체</option>
                <option v-for="opt in options.custgbn" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
              </select>
            </div>
            <div class="input-group input-group-sm flex-nowrap" style="width: 180px;">
              <span class="input-group-text fw-bold border-0 bg-transparent">상태</span>
              <select v-model="searchform.qstatus" class="form-select border-0 bg-light" @change="search">
                <option value="000">전체</option>
                <option v-for="opt in options.status" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
              </select>
            </div>
            <div class="input-group input-group-sm flex-nowrap" style="width: 300px;">
              <span class="input-group-text fw-bold border-0 bg-transparent">거래처명</span>
              <input v-model="searchform.qcustnm" type="text" class="form-control border-0 bg-light" placeholder="거래처명 입력..." @keyup.enter="search" />
              <button class="btn btn-dark btn-sm" @click="search"><i class="bi bi-search"></i></button>
            </div>
          </div>
        </div>
      </div>

      <!-- 🅱️ 상세 입력 폼 영역 -->
      <div class="card border shadow-sm overflow-hidden flex-shrink-0">
        <div class="card-header py-1 px-3 border-bottom d-flex align-items-center justify-content-between" style="background-color: #f8f9fa;">
          <span class="fw-bold small text-dark"><i class="bi bi-pencil-square me-1"></i> 거래처 상세 정보</span>
          <div class="d-flex gap-2">
            <span v-if="masterdata.actkind === 'U0'" class="badge bg-warning text-dark" style="font-size: 10px;">수정 모드</span>
            <span v-else class="badge bg-primary text-white px-2" style="font-size: 10px;">신규 등록</span>
          </div>
        </div>
        <div class="card-body p-0 bg-white">
          <table class="erp-table-full border-0">
            <colgroup>
              <col style="width: 80px;" /><col />
              <col style="width: 80px;" /><col />
              <col style="width: 80px;" /><col />
              <col style="width: 80px;" /><col />
              <col style="width: 80px;" /><col />
            </colgroup>
            <tbody>
              <tr>
                <th class="required">거래처코드</th>
                <td><input v-model="masterdata.custcd" type="text" class="form-control form-control-sm text-center fw-bold text-primary" maxlength="7" :readonly="masterdata.actkind === 'U0'" /></td>
                <th class="required">상&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;호</th>
                <td><input v-model="masterdata.custnm" type="text" class="form-control form-control-sm fw-bold border-primary-subtle" maxlength="50" /></td>
                <th>사업자번호</th>
                <td><input v-model="masterdata.custno" type="text" class="form-control form-control-sm" maxlength="13" placeholder="- 없이" /></td>
                <th>법인번호</th>
                <td><input v-model="masterdata.legalno" type="text" class="form-control form-control-sm" maxlength="14" placeholder="- 없이" /></td>
                <th>대&nbsp;&nbsp;표&nbsp;&nbsp;자</th>
                <td><input v-model="masterdata.bossnm" type="text" class="form-control form-control-sm" maxlength="30" /></td>
              </tr>
              <tr>
                <th>거&nbsp;래&nbsp;종&nbsp;류</th>
                <td>
                  <select v-model="masterdata.custgbn" class="form-select form-select-sm">
                    <option v-for="opt in options.custgbn" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                  </select>
                </td>
                <th>업&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;태</th>
                <td><input v-model="masterdata.custtype" type="text" class="form-control form-control-sm" maxlength="50" /></td>
                <th>종&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;목</th>
                <td><input v-model="masterdata.custkind" type="text" class="form-control form-control-sm" maxlength="50" /></td>
                <th>연&nbsp;락&nbsp;처</th>
                <td><input v-model="masterdata.telno" type="text" class="form-control form-control-sm" maxlength="30" /></td>
                <th>FAX번호</th>
                <td><input v-model="masterdata.faxno" type="text" class="form-control form-control-sm" maxlength="30" /></td>
              </tr>
              <tr>
                <th>주&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;소</th>
                <td colspan="9">
                  <AddressPopupForm
                    v-model:postno="masterdata.postno"
                    v-model:address="masterdata.address"
                    v-model:d_address="masterdata.d_address"
                    @open-address="handleopenhelp('ADDR')"
                  />
                </td>
              </tr>
              <tr>
                <th>금융기관</th>
                <td><input v-model="masterdata.banknm" type="text" class="form-control form-control-sm" maxlength="30" /></td>
                <th>계좌번호</th>
                <td colspan="3"><input v-model="masterdata.gujoa" type="text" class="form-control form-control-sm" maxlength="30" /></td>
                <th>적용일자</th>
                <td><input v-model="uistdymd" type="date" class="form-control form-control-sm" /></td>
                <th>종료일자</th>
                <td><input v-model="uiclsymd" type="date" class="form-control form-control-sm" /></td>
              </tr>
              <tr>
                <th>입출구분</th>
                <td>
                  <select v-model="masterdata.iogbn" class="form-select form-select-sm">
                    <option v-for="opt in options.iogbn" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                  </select>
                </td>
                <th>상&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;태</th>
                <td>
                  <select v-model="masterdata.status" class="form-select form-select-sm">
                    <option v-for="opt in options.status" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                  </select>
                </td>
                <th>신용등급</th>
                <td>
                  <select v-model="masterdata.singrd" class="form-select form-select-sm">
                    <option value="a">a등급</option><option value="b">b등급</option><option value="c">c등급</option>
                  </select>
                </td>
                <th>전자여부</th>
                <td>
                  <div class="form-check form-switch m-0 d-flex align-items-center h-100">
                    <input v-model="masterdata.elcyn" class="form-check-input mt-0" type="checkbox" true-value="Y" false-value="N" id="elcYnSwitch">
                    <label class="form-check-label ms-2 small fw-bold" for="elcYnSwitch">전자계산서</label>
                  </div>
                </td>
                <th>사용여부</th>
                <td>
                  <div class="form-check form-switch m-0 d-flex align-items-center h-100">
                    <input v-model="masterdata.useyn" class="form-check-input mt-0" type="checkbox" true-value="Y" false-value="N" id="useYnSwitch">
                    <label class="form-check-label ms-2 small fw-bold" for="useYnSwitch">사용</label>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Ⓒ 거래처 목록 그리드 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark"><i class="bi bi-list-ul me-1"></i> 거래처 목록 ({{ activeitemcount }} 건)</span>
          <span class="text-muted" style="font-size: 11px;">※ 행 클릭 시 상세 정보가 로드됩니다.</span>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="maingridelement" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>
    </div>

    <Modal v-model:visible="modalvisible" :modalProps="modalprops" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick, computed } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import * as XLSX from 'xlsx'
import AppAlert from '@/components/AppAlert.vue'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { useRoute } from 'vue-router'
import Modal from '@/components/Modal.vue'
import AddressPopupForm from '@/components/AddressPopupForm.vue'
import { useCommonHelp } from '@/composables/useCommonHelp'

const authstore = useAuthStore()
const route = useRoute()
const { showAlert: showalert, showError: showerror, alertMessage: alertmessage, vAlert: valert, vAlertError: valerterror } = useAlerts()
const { resetForm: resetform } = useFormReset()
const { modalVisible: modalvisible, modalProps: modalprops, openHelp } = useCommonHelp()
const today = new Date().toISOString().substring(0, 10)

// 1. 상태 관리 (완전 소문자)
const searchform = reactive({ qcustgbn: '000', qstatus: '010', qcustnm: '', qcustcd: '' })
const masterdata = reactive<any>({
  actkind: 'i0', cmpycd: authstore.cmpycd, custcd: '', custnm: '', custno: '', custgbn: '010',
  bossnm: '', legalno: '', postno: '', address: '', d_address: '',
  custtype: '', custkind: '', telno: '', faxno: '', banknm: '', gujoa: '',
  stdymd: today.replace(/-/g, ''), clsymd: '99991231', singrd: 'A', elcyn: 'N', iogbn: '010', status: '010', useyn: 'Y',
  userid: authstore.userid
})

const uistdymd = computed({
  get: () => masterdata.stdymd ? `${masterdata.stdymd.substring(0, 4)}-${masterdata.stdymd.substring(4, 6)}-${masterdata.stdymd.substring(6, 8)}` : '',
  set: (v) => masterdata.stdymd = (v || '').replace(/-/g, '')
})

const uiclsymd = computed({
  get: () => masterdata.clsymd ? `${masterdata.clsymd.substring(0, 4)}-${masterdata.clsymd.substring(4, 6)}-${masterdata.clsymd.substring(6, 8)}` : '',
  set: (v) => masterdata.clsymd = (v || '').replace(/-/g, '')
})

const options = reactive<any>({ custgbn: [], status: [], iogbn: [] })
const maingridelement = ref<HTMLDivElement | null>(null); let maingrid: Tabulator | null = null; const activeitemcount = ref(0)

// 2. 기초 데이터 로드
const loadinitdata = async () => {
  try {
    const [resgbn, resstatus, resio] = await Promise.all([
      api.post('/ha00/ha00_00p_str', { gubun: 'E0', gbncd: '270', cmpycd: authstore.cmpycd }),
      api.post('/ha00/ha00_00p_str', { gubun: 'E0', gbncd: '280', cmpycd: authstore.cmpycd }),
      api.post('/ha00/ha00_00p_str', { gubun: 'E0', gbncd: '340', cmpycd: authstore.cmpycd })
    ])
    const map = (r: any) => r.data.map((n: any) => ({ codecd: n.code || n.codecd, codenm: n.cdnm || n.codenm }))
    options.custgbn = map(resgbn)
    options.status = map(resstatus)
    options.iogbn = map(resio)
  } catch (e) { console.error('기초 데이터 로드 실패') }
}

// 3. 기능 구현
const search = async () => {
  try {
    const res = await api.post('/haba/HABA_180U_STR', {
      actkind: 'S0', cmpycd: authstore.cmpycd,
      custcd: searchform.qcustcd || '',
      custnm: searchform.qcustnm,
      custgbn: searchform.qcustgbn,
      status: searchform.qstatus
    })
    const list = res.data || [];
    maingrid?.setData(list)
    activeitemcount.value = list.length

    // 🚀 네비게이션 처리: 검색 결과가 있고 특정 코드로 넘어온 경우 자동 바인딩
    if (route.query.custcd && list.length > 0) {
        const target = list.find(r => String(r.custcd).trim() === String(route.query.custcd).trim()) || list[0];
        Object.assign(masterdata, target);
        masterdata.actkind = 'U0';
    } else if (route.query.custnm && list.length === 1) {
        Object.assign(masterdata, list[0]);
        masterdata.actkind = 'U0';
    }

    valert('조회되었습니다.')
  } catch (e) { valerterror('조회 실패') }
}

const save = async () => {
  if (!masterdata.custnm) return valerterror('상호는 필수입니다.')
  if (!masterdata.custcd) return valerterror('거래처코드는 필수입니다.')
  if (!confirm('저장하시겠습니까?')) return
  try {
    const payload = {
      ...masterdata,
      actkind: masterdata.actkind === 'i0' ? 'i0' : 'U0',
      cmpycd: authstore.cmpycd, userid: authstore.userid,
      stdymd: masterdata.stdymd.replace(/-/g, ''),
      clsymd: masterdata.clsymd.replace(/-/g, ''),
      custno: (masterdata.custno || '').replace(/-/g, ''),
      legalno: (masterdata.legalno || '').replace(/-/g, ''),
      address_det: masterdata.d_address
    }
    const res = await api.post('/haba/HABA_180U_STR', payload)
    const resdata = res.data?.[0] || {};
    if (resdata.result === 'N' || resdata.erryn === 'Y') valerterror(resdata.msg || '저장 실패')
    else { valert('정상 처리되었습니다.'); search(); initialize(); }
  } catch (e) { valerterror('저장 실패') }
}

const initialize = () => {
  resetform(masterdata)
  Object.assign(masterdata, {
    actkind: 'i0', cmpycd: authstore.cmpycd, custgbn: '010', iogbn: '010', status: '010',
    useyn: 'Y', stdymd: today.replace(/-/g, ''), clsymd: '99991231', singrd: 'A', elcyn: 'N', userid: authstore.userid
  })
}

const handleopenhelp = (type: string) => {
  if (type === 'ADDR') {
    if (!masterdata.custcd) return valerterror('거래처를 먼저 선택하세요.');
    openHelp('ADDR', { gubun: 'C5', cmpycd: authstore.cmpycd, gbncd: masterdata.custcd }, (d: any) => {
      masterdata.postno = d.postno; masterdata.address = d.address;
    });
  }
}

const excel = () => maingrid?.download("xlsx", `거래처관리_${new Date().toISOString().substring(0, 10)}.xlsx`)

onMounted(async () => {
  if (typeof window !== 'undefined') (window as any).XLSX = XLSX
  await loadinitdata()

  // 🚀 프로그램 네비게이션 처리 (HABA190S 등에서 넘어온 파라미터 처리)
  if (route.query.custcd || route.query.custnm) {
    if (route.query.custcd) searchform.qcustcd = String(route.query.custcd)
    if (route.query.custnm) searchform.qcustnm = String(route.query.custnm)
    if (route.query.custgbn) searchform.qcustgbn = String(route.query.custgbn)
    if (route.query.status) searchform.qstatus = String(route.query.status)
  }

  if (maingridelement.value) {
    maingrid = new Tabulator(maingridelement.value, {
      layout: 'fitColumns', height: '100%', selectable: 1,
      pagination: "local", paginationSize: 20, paginationSizeSelector: [20, 50, 100],
      columnDefaults: { headerSort: false, headerHozAlign: 'center', hozAlign: "center", vertAlign: "middle" },
      columns: [
        { title: "코드", field: "custcd", width: 90, cssClass: "fw-bold text-center text-primary" },
        { title: "거래처상호", field: "custnm", minWidth: 200, hozAlign: "left" },
        { title: "사업자번호", field: "custno", width: 120 },
        { title: "대표자", field: "bossnm", width: 100 },
        { title: "연락처", field: "telno", width: 120 },
        { title: "구분", field: "iogbnnm", width: 90 },
        { title: "상태", field: "statusnm", width: 90 },
        { title: "사용", field: "useyn", width: 80, hozAlign: "center",
          formatter: (cell) => {
            const val = String(cell.getValue() || '').trim().toUpperCase();
            return val === 'Y' ? '<b class="text-primary">사용</b>' : '';
          }
        }
      ]
    })
    maingrid.on('rowClick', (e, row) => {
      const data = row.getData();
      Object.assign(masterdata, data);
      masterdata.actkind = 'U0'
    })
  }
  search()
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; font-size: 12px; }
:deep(.tabulator-row:hover) { background-color: #f0f7ff !important; cursor: pointer; }
</style>
