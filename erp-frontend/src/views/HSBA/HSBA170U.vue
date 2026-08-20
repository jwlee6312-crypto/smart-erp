<template>
  <AppAlert :show="showalert" :error="showerror" :message="alertmessage" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-1 px-3 sticky-top shadow-sm flex-shrink-0">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 13px;">
        <i class="bi bi-shield-check me-2 text-primary"></i>
        기본정보 > <span class="text-primary fw-bolder">거래처 담보관리 (HSBA170U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1">
        <button class="btn-erp btn-init" @click="initialize(false)">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-save" @click="save">저장</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-y-auto p-2 d-flex flex-column gap-2 bg-light">
      <!-- 🅰️ 조회 조건 -->
      <div class="card border shadow-sm flex-shrink-0">
        <div class="card-body p-1 bg-white">
          <div class="d-flex align-items-center gap-3">
            <div class="input-group input-group-sm flex-nowrap" style="width: 350px;">
              <span class="input-group-text fw-bold border-0 bg-transparent">거래처검색</span>
              <input v-model="searchdata.qcustnm" type="text" class="form-control border-0 bg-light" placeholder="거래처명 또는 코드" @keyup.enter="search" />
              <button class="btn btn-dark btn-sm" @click="search"><i class="bi bi-search"></i></button>
            </div>
          </div>
        </div>
      </div>

      <!-- 🅱️ 담보 정보 입력 -->
      <div class="card border shadow-sm overflow-hidden flex-shrink-0">
        <div class="card-header py-1 px-3 border-bottom d-flex align-items-center justify-content-between" style="background-color: #f8f9fa;">
          <span class="fw-bold small text-dark"><i class="bi bi-pencil-square me-1"></i> 담보 설정 정보</span>
          <div class="d-flex gap-2 align-items-center">
            <span v-if="masterdata.actkind === 'U0'" class="badge bg-warning text-dark" style="font-size: 10px;">수정 중 (행번: {{ masterdata.rowno }})</span>
            <span v-else class="badge bg-primary" style="font-size: 10px;">신규 등록</span>
          </div>
        </div>
        <div class="card-body p-0 bg-white">
          <table class="erp-table-full border-0">
            <colgroup>
              <col style="width: 80px;" /><col />
              <col style="width: 80px;" /><col />
              <col style="width: 80px;" /><col />
              <col style="width: 80px;" /><col />
            </colgroup>
            <tbody>
              <tr>
                <th class="required">거 래 처</th>
                <td>
                   <div class="d-flex gap-1">
                    <input v-model="masterdata.custcd" type="text" class="form-control form-control-sm text-center fw-bold bg-light" style="width: 70px;" readonly />
                    <input v-model="masterdata.custnm" type="text" class="form-control form-control-sm bg-light" readonly />
                  </div>
                </td>
                <th class="required">담보구분</th>
                <td>
                  <select v-model="masterdata.damkind" class="form-select form-select-sm">
                    <option v-for="opt in damkindoptions" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                  </select>
                </td>
                <th class="required">담보종류</th>
                <td>
                  <select v-model="masterdata.damyeo" class="form-select form-select-sm">
                    <option v-for="opt in damyeooptions" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                  </select>
                </td>
                <th>증권번호</th>
                <td>
                  <input v-model="masterdata.dmmgt" type="text" class="form-control form-control-sm" maxlength="30" placeholder="증권/관리번호" />
                </td>
              </tr>
              <tr>
                <th>소&nbsp;&nbsp;유&nbsp;&nbsp;자</th>
                <td><input v-model="masterdata.sounm" type="text" class="form-control form-control-sm" maxlength="50" /></td>
                <th class="required">설&nbsp;&nbsp;정&nbsp;&nbsp;액</th>
                <td><input v-model="masterdata.damsel" type="text" class="form-control form-control-sm text-end fw-bold" @input="formatinput('damsel')" /></td>
                <th class="required">여신한도</th>
                <td><input v-model="masterdata.samhan" type="text" class="form-control form-control-sm text-end fw-bold text-primary" @input="formatinput('samhan')" /></td>
                <th>여신기한</th>
                <td>
                  <div class="d-flex align-items-center gap-1">
                    <input v-model="masterdata.rcvdd" type="text" class="form-control form-control-sm text-end" style="max-width: 60px;" />
                    <span class="small fw-bold">일</span>
                  </div>
                </td>
              </tr>
              <tr>
                <th class="required">설&nbsp;&nbsp;정&nbsp;&nbsp;일</th>
                <td><input v-model="fromdt" type="date" class="form-control form-control-sm" /></td>
                <th class="required">유&nbsp;&nbsp;효&nbsp;&nbsp;일</th>
                <td><input v-model="todt" type="date" class="form-control form-control-sm" /></td>
                <th>비&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;고</th>
                <td colspan="3"><input v-model="masterdata.bigo" type="text" class="form-control form-control-sm w-100" maxlength="100" /></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Ⓒ 목록 영역 -->
      <div class="d-flex flex-grow-1 gap-2 overflow-hidden" style="min-height: 0;">
        <div class="card border shadow-sm d-flex flex-column overflow-hidden" style="width: 320px; flex-shrink: 0;">
          <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark">
            <i class="bi bi-list-ul me-1"></i> 거래처 목록
          </div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
              <div ref="custgridelement" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>

        <div class="card border shadow-sm flex-grow-1 d-flex flex-column overflow-hidden">
          <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
            <span class="fw-bold small text-dark d-flex align-items-center">
              <i class="bi bi-shield-lock-fill me-2 text-primary"></i> 담보 설정 내역
              <span v-if="selectedcustname" class="badge bg-primary-subtle text-primary border border-primary-subtle ms-2">{{ selectedcustname }}</span>
            </span>
          </div>
            <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
                <div ref="damgridelement" class="tabulator-instance flex-grow-1"></div>
            </div>
        </div>
      </div>
    </div>
    <Modal v-model:visible="modalvisible" :modalProps="modalprops" />
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

const authstore = useAuthStore()
const { showAlert: showalert, showError: showerror, alertMessage: alertmessage, vAlert: valert, vAlertError: valerterror } = useAlerts()
const { resetForm: resetform } = useFormReset()

const now = new Date()
const initymd = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}`

const searchdata = reactive({ qcustnm: '' })
const masterdata = reactive<any>({
  actkind: 'A0', cmpycd: authstore.cmpycd, custcd: '', custnm: '', rowno: '',
  damkind: '100', damyeo: '100', dmmgt: '', sounm: '',
  damsel: '0', samhan: '0', rcvdd: '0', fromdt: initymd, todt: '', bigo: '', useyn: 'Y'
})

const damkindoptions = ref<any[]>([]); const damyeooptions = ref<any[]>([]); const selectedcustname = ref('')
const fromdt = computed({ get: () => formatdate(masterdata.fromdt, '-'), set: (v) => masterdata.fromdt = v.replace(/-/g, '') })
const todt = computed({ get: () => formatdate(masterdata.todt, '-'), set: (v) => masterdata.todt = v.replace(/-/g, '') })

const custgridelement = ref<HTMLElement | null>(null); const damgridelement = ref<HTMLElement | null>(null)
let custgrid: Tabulator | null = null; let damgrid: Tabulator | null = null

const initgrids = () => {
  if (custgridelement.value) {
    custgrid = new Tabulator(custgridelement.value, {
      layout: "fitColumns", height: "100%", placeholder: "거래처 없음",
      pagination: "local", paginationSize: 20, paginationButtonCount: 3,
      columnDefaults: { headerSort: false, headerHozAlign: "center", hozAlign: "center", vertAlign: "middle" },
      columns: [
        {
          title: "거래처 상호", field: "custnm", hozAlign: "left",
          formatter: (cell) => {
            const v = cell.getValue() || '';
            return cell.getData().endyn === 'Y' ? `<span class="text-primary fw-bold">${v}</span>` : v;
          }
        }
      ]
    })
    custgrid.on("rowClick", (e, row) => {
      const d = row.getData(); masterdata.custcd = d.custcd; masterdata.custnm = d.custnm;
      selectedcustname.value = d.custnm; masterdata.actkind = 'A0'; masterdata.rowno = '';
      fetchdamlist()
    })
  }

  if (damgridelement.value) {
    damgrid = new Tabulator(damgridelement.value, {
      layout: "fitColumns", height: "100%", placeholder: "내역 없음",
      columnDefaults: { headerSort: false, headerHozAlign: "center", hozAlign: "center", vertAlign: "middle" },
      columns: [
        { title: "행번", field: "rowno", width: 70 },
        { title: "담보구분", field: "damkindnm", width: 120 },
        { title: "종류", field: "damyeonm", width: 120 },
        { title: "한도액", field: "samhan", width: 120, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
        { title: "기한(일)", field: "rcvdd", width: 80 },
        { title: "설정일", field: "frymd", width: 110 },
        { title: "유효일", field: "toymd", width: 110 },
        { title: "증권번호", field: "dmmgt", width: 130 },
        { title: "비고", field: "bigo", minWidth: 150, hozAlign: "left" }
      ]
    })

    damgrid.on("rowClick", (e, row) => {
      const data = row.getData();
      Object.assign(masterdata, data);
      masterdata.damsel = formatnumber(data.damsel);
      masterdata.samhan = formatnumber(data.samhan);
      masterdata.actkind = 'U0';
      masterdata.rowno = data.rowno;
      masterdata.fromdt = data.frymd;
      masterdata.todt = data.toymd;

    })
  }
}

async function fetchoptions() {
  try {
    const r1 = await api.post('/hs00/HS00_000S_STR', { gubun: 'e0', gbncd: '500', cmpycd: authstore.cmpycd })
    damkindoptions.value = r1.data.map((n: any) => {
      return { codecd: String(n.code || n.codecd || '').trim(), codenm: String(n.cdnm || n.codenm || '').trim() }
    })
    const r2 = await api.post('/hs00/HS00_000S_STR', { gubun: 'e0', gbncd: '510', cmpycd: authstore.cmpycd })
    damyeooptions.value = r2.data.map((n: any) => {
      return { codecd: String(n.code || n.codecd || '').trim(), codenm: String(n.cdnm || n.codenm || '').trim() }
    })
  } catch (e) {}
}

async function search() {
  try {
    const res = await api.post('/hsba/HSBA_170U_STR', {
      actkind: 'S1', cmpycd: authstore.cmpycd, custcd: '', custnm: searchdata.qcustnm || '',
      rowno: '', damkind: '', damgbn: '', damyeo: '', dmmgt: '', damsel: 0, samhan: 0, rcvdd: 0,
      fromdt: '', todt: '', sounm: '', bigo: '', useyn: '', userid: authstore.userid
    })
    custgrid?.setData(res.data || [])
  } catch (e) { valerterror('조회 실패') }
}

async function fetchdamlist() {
  try {
    const res = await api.post('/hsba/HSBA_170U_STR', {
      actkind: 'S0', cmpycd: authstore.cmpycd, custcd: masterdata.custcd,
      custnm: '', rowno: '', damkind: '', damgbn: '', damyeo: '', dmmgt: '', damsel: 0, samhan: 0, rcvdd: 0,
      fromdt: '', todt: '', sounm: '', bigo: '', useyn: '', userid: authstore.userid
    })
    damgrid?.setData(res.data || [])
  } catch (e) { valerterror('내역 조회 실패') }
}

async function save() {
  if (!masterdata.custcd) return valerterror('거래처를 선택하세요.')
  if (!masterdata.fromdt || !masterdata.todt) return valerterror('설정일과 유효일은 필수입니다.')
  if (!confirm('저장하시겠습니까?')) return
  try {
    const payload = {
        ...masterdata,
        damsel: String(masterdata.damsel).replace(/,/g, ''),
        samhan: String(masterdata.samhan).replace(/,/g, ''),
        rcvdd: String(masterdata.rcvdd).replace(/,/g, ''),
        fromdt: masterdata.fromdt.replace(/-/g, ''),
        todt: masterdata.todt.replace(/-/g, ''),
        userid: authstore.userid
    }
    const res = await api.post('/hsba/HSBA_170U_STR', payload)
    if (res.data?.[0]?.result === 'N') return valerterror(res.data[0].msg || '저장 실패');
    valert('정상 처리되었습니다.');
    fetchdamlist();
    initialize(true)
  } catch (e) { valerterror('저장 실패') }
}

function initialize(keepcust = false) {
  const cd = masterdata.custcd; const nm = masterdata.custnm; resetform(masterdata)
  Object.assign(masterdata, { actkind: 'A0', cmpycd: authstore.cmpycd, damkind: '100', damyeo: '100', damsel: '0', samhan: '0', rcvdd: '0', fromdt: initymd, useyn: 'Y', rowno: '' })
  if (keepcust) { masterdata.custcd = cd; masterdata.custnm = nm } else { selectedcustname.value = ''; damgrid?.clearData() }
}

const formatinput = (field: string) => { let val = String(masterdata[field]).replace(/[^0-9]/g, ''); masterdata[field] = val.replace(/\B(?=(\d{3})+(?!\d))/g, ',') }
const modalvisible = ref(false); const modalprops = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })
const formatdate = (v: any, sep: string) => v && String(v).length === 8 ? `${String(v).substring(0, 4)}${sep}${String(v).substring(4, 6)}${sep}${String(v).substring(6, 8)}` : v
const formatnumber = (val: any) => new Intl.NumberFormat().format(Number(val) || 0)

onMounted(async () => { await fetchoptions(); nextTick(() => { initgrids(); search() }) })
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
