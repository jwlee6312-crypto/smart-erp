<!--
	=============================================================
	프로그램명	: 거래처 등록 (HSBA090U)
	작성일자	: 2025.02.24
	설명        : 거래처 정보 관리 (HSOD100U 디자인 패턴 및 누락 항목 원복)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-briefcase-fill me-2 text-primary" style="font-size: 18px;"></i>
        기본정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        거래처관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">거래처 등록 (HSBA090U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-2">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchList">조회</button>
        <button class="btn-erp btn-save" @click="save">저장</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- 🔍 [상단] 조회 필터 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
              <col style="width: 8%" /><col style="width: 25%" />
              <col style="width: 8%" /><col style="width: 25%" />
              <col style="width: 8%" /><col style="width: 26%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light">거래처 구분</th>
                <td>
                  <select v-model="searchParams.custgbn" class="form-select" @change="fetchList">
                    <option value="000">전체</option>
                    <option v-for="opt in options.custGbn" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                  </select>
                </td>
                <th class="text-center bg-light">상태</th>
                <td>
                  <select v-model="searchParams.status" class="form-select" @change="fetchList">
                    <option value="000">전체</option>
                    <option v-for="opt in options.status" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                  </select>
                </td>
                <th class="text-center bg-light">거래처명</th>
                <td>
                  <input v-model="searchParams.custnm" type="text" class="form-control" placeholder="거래처명 입력..." @keyup.enter="fetchList" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 🅰️ 상세 입력 폼 (원복된 항목 포함) -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-header py-1 px-3 border-bottom d-flex align-items-center justify-content-between" style="background-color: #f0f7ff !important;">
          <span class="fw-bold small text-primary"><i class="bi bi-pencil-square me-1"></i> 거래처 상세 정보</span>
          <div class="d-flex gap-2">
            <span v-if="formData.actkind === 'U0'" class="badge bg-warning text-dark" style="font-size: 10px;">수정 모드</span>
            <span v-else class="badge bg-primary" style="font-size: 10px;">신규 등록</span>
          </div>
        </div>
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
                <th class="required bg-light">거래처코드</th>
                <td><input v-model="formData.custcd" type="text" class="form-control text-center fw-bold bg-light" readonly placeholder="자동생성" /></td>
                <th class="required bg-light">거래처상호</th>
                <td><input v-model="formData.custnm" type="text" class="form-control fw-bold border-primary-subtle" maxlength="50" /></td>
                <th class="bg-light">사업자번호</th>
                <td><input v-model="formData.custno" type="text" class="form-control" maxlength="13" placeholder="- 없이 입력" /></td>
                <th class="bg-light">거래구분</th>
                <td>
                  <select v-model="formData.custgbn" class="form-select">
                    <option v-for="opt in options.custGbn" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                  </select>
                </td>
              </tr>
              <tr>
                <th class="bg-light">대표자명</th>
                <td><input v-model="formData.bossnm" type="text" class="form-control" maxlength="30" /></td>
                <th class="bg-light">업 태</th>
                <td><input v-model="formData.custtype" type="text" class="form-control" maxlength="50" /></td>
                <th class="bg-light">종 목</th>
                <td><input v-model="formData.custkind" type="text" class="form-control" maxlength="50" /></td>
                <th class="bg-light">설립일자</th>
                <td><input v-model="formData.fndymd" type="date" class="form-control" /></td>
              </tr>
              <tr>
                <th class="bg-light">전화번호</th>
                <td><input v-model="formData.telno" type="text" class="form-control" maxlength="30" /></td>
                <th class="bg-light">팩스번호</th>
                <td><input v-model="formData.faxno" type="text" class="form-control" maxlength="30" /></td>
                <th class="bg-light">단가(입)</th>
                <td>
                  <select v-model="formData.inprcgbn" class="form-select">
                    <option v-for="opt in options.prcGbn" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                  </select>
                </td>
                <th class="bg-light">단가(출)</th>
                <td>
                  <select v-model="formData.outprcgbn" class="form-select">
                    <option v-for="opt in options.prcGbn" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                  </select>
                </td>
              </tr>
              <tr>
                <th class="bg-light text-center">주소</th>
                <td colspan="5">
                  <AddressPopupForm
                    v-model:postno="formData.postno"
                    v-model:address="formData.address"
                    v-model:d_address="formData.d_address"
                    @open-address="handleOpenHelp('ADDR')"
                  />
                </td>
                <th class="bg-light">전자세금</th>
                <td>
                  <select v-model="formData.elcyn" class="form-select">
                    <option value="Y">발행</option>
                    <option value="N">미발행</option>
                  </select>
                </td>
              </tr>
              <tr>
                <th class="bg-light">한도금액</th>
                <td><input v-model.number="formData.hdamt" type="number" class="form-control text-end" /></td>
                <th class="bg-light">결제일</th>
                <td><input v-model.number="formData.rcvdd" type="number" class="form-control text-end" placeholder="일자" /></td>
                <th class="bg-light">신용등급</th>
                <td>
                  <select v-model="formData.singrd" class="form-select">
                    <option value="A">A등급</option><option value="B">B등급</option><option value="C">C등급</option>
                  </select>
                </td>
                <th class="bg-light">마감구분</th>
                <td>
                  <div class="d-flex align-items-center gap-1">
                    <span class="small text-secondary">일</span>
                    <select v-model="formData.gigbcd" class="form-select">
                      <option v-for="opt in options.gigGbn" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                    </select>
                  </div>
                </td>
              </tr>
              <tr>
                <th class="bg-light">대분류</th>
                <td>
                  <select v-model="formData.agrpcd" class="form-select">
                    <option value="000">선택</option>
                    <option v-for="opt in options.agrp" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                  </select>
                </td>
                <th class="bg-light">중분류</th>
                <td>
                  <select v-model="formData.bgrpcd" class="form-select">
                    <option value="000">선택</option>
                    <option v-for="opt in options.bgrp" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                  </select>
                </td>
                <th class="bg-light">소분류</th>
                <td>
                  <select v-model="formData.cgrpcd" class="form-select">
                    <option value="000">선택</option>
                    <option v-for="opt in options.cgrp" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                  </select>
                </td>
                <th class="bg-light">지 역</th>
                <td>
                  <select v-model="formData.area" class="form-select">
                    <option value="000">선택</option>
                    <option v-for="opt in options.area" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                  </select>
                </td>
              </tr>
              <tr>
                <th class="bg-light">담 당 자</th>
                <td><input v-model="formData.cdamdang" type="text" class="form-control" /></td>
                <th class="bg-light">연락처(SMS)</th>
                <td><input v-model="formData.ctelno" type="text" class="form-control" /></td>
                <th class="bg-light">전자메일</th>
                <td><input v-model="formData.cemail" type="text" class="form-control" /></td>
                <th class="bg-light">매입매출</th>
                <td>
                  <select v-model="formData.iogbn" class="form-select">
                    <option v-for="opt in options.ioGbn" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                  </select>
                </td>
              </tr>
              <tr>
                <th class="bg-light">적용일자</th>
                <td><input v-model="uistdymd" type="date" class="form-control" /></td>
                <th class="bg-light">유효일자</th>
                <td><input v-model="uiclsymd" type="date" class="form-control" /></td>
                <th class="bg-light">상 태</th>
                <td>
                  <select v-model="formData.status" class="form-select">
                    <option v-for="opt in options.status" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                  </select>
                </td>
                <th class="bg-light">사용여부</th>
                <td>
                  <div class="form-check form-switch m-0 d-flex align-items-center justify-content-center">
                    <input v-model="formData.useyn" class="form-check-input" type="checkbox" true-value="Y" false-value="N" id="useYn090">
                    <label class="form-check-label ms-2 small fw-bold" for="useYn090">{{ formData.useyn === 'Y' ? '사용' : '중지' }}</label>
                  </div>
                </td>
              </tr>
              <tr>
                <th class="bg-light text-center">특이사항</th>
                <td colspan="7"><input v-model="formData.remark" class="form-control" placeholder="기타 참고사항 입력" /></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Ⓒ 거래처 목록 그리드 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white grid-container-right">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark"><i class="bi bi-list-ul me-1"></i> 거래처 목록 ({{ activeItemCount }} 건)</span>
        </div>
        <div class="card-body p-0 flex-grow-1 overflow-hidden d-flex flex-column">
          <div ref="gridElement" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>
    </div>
  </div>

  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, nextTick, computed } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import AddressPopupForm from '@/components/AddressPopupForm.vue'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { useCommonHelp } from '@/composables/useCommonHelp'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps, openHelp } = useCommonHelp()

const searchParams = reactive({ custgbn: '000', status: '010', custnm: '' })
const activeItemCount = ref(0)
const gridElement = ref<HTMLElement | null>(null)
let grid: Tabulator | null = null

const formData = reactive<any>({
  actkind: 'A0', custcd: '', custnm: '', custno: '', juminno: '', legalno: '', custgbn: '010',
  bossnm: '', custtype: '', custkind: '', postno: '', address: '', d_address: '', telno: '', faxno: '',
  inprcgbn: '200', outprcgbn: '200', hdamt: 0, rcvdd: 0, gigbcd: '305',
  agrpcd: '000', bgrpcd: '000', cgrpcd: '000', area: '000', cdamdang: '', ctelno: '', cemail: '',
  remark: '', status: '010', banknm: '', gujoa: '', stdymd: '', clsymd: '99991231',
  singrd: 'A', elcyn: 'Y', useyn: 'Y', iogbn: '010', fndymd: ''
})

const options = reactive<any>({
  custGbn: [], status: [], prcGbn: [], gigGbn: [], agrp: [], bgrp: [], cgrp: [], area: [], ioGbn: []
})

const uistdymd = computed({ get: () => formatDate(formData.stdymd), set: (v) => formData.stdymd = v.replace(/-/g, '') })
const uiclsymd = computed({ get: () => formatDate(formData.clsymd), set: (v) => formData.clsymd = v.replace(/-/g, '') })

async function fetchOptions() {
  const getOptHS = async (gbn: string, cd = '') => {
    try {
      const res = await api.post('/hs00/HS00_000S_STR', { gubun: gbn, cmpycd: authStore.cmpycd, gbncd: cd })
      return (res.data || []).map((n: any) => ({ codecd: n.code || n.codecd, codenm: n.cdnm || n.codenm }))
    } catch (e) { return [] }
  }
  const getOptHA = async (gbn: string, cd = '') => {
    try {
      const res = await api.post('/ha00/HA00_00P_STR', { gubun: gbn, cmpycd: authStore.cmpycd, gbncd: cd })
      return (res.data || []).map((n: any) => ({ codecd: n.code || n.codecd, codenm: n.cdnm || n.codenm }))
    } catch (e) { return [] }
  }

  const [o1, o2, o3, o4, o5, o6, o7, o8, o9] = await Promise.all([
    getOptHA('E0', '270'), getOptHA('E0', '280'), getOptHS('E0', '200'),
    getOptHS('E0', '305'), getOptHS('E0', '400'), getOptHS('E0', '410'),
    getOptHS('E0', '420'), getOptHS('AR'), getOptHS('E0', '340')
  ])
  options.custGbn = o1; options.status = o2; options.prcGbn = o3; options.gigGbn = o4;
  options.agrp = o5; options.bgrp = o6; options.cgrp = o7; options.area = o8; options.ioGbn = o9;
}

const initGrid = () => {
  if (!gridElement.value) return
  grid = new Tabulator(gridElement.value, {
    layout: "fitColumns", height: "100%",
    pagination: "local", paginationSize: 20,
    columnDefaults: { vertAlign: "middle", headerHozAlign: "center", headerSort: false },
    columns: [
      { title: "코드", field: "custcd", width: 90, hozAlign: "center", cssClass: "fw-bold text-primary" },
      { title: "거래처명 (상호)", field: "custnm", minWidth: 250, hozAlign: "left" },
      { title: "사업자번호", field: "custno", width: 130, hozAlign: "center" },
      { title: "대표자", field: "bossnm", width: 100, hozAlign: "center" },
      { title: "연락처", field: "telno", width: 130, hozAlign: "left" },
      { title: "상태", field: "statusnm", width: 80, hozAlign: "center" },
      { title: "사용", field: "useyn", width: 80, hozAlign: "center",  editor: true,
        formatter: (cell) => {
          const val = String(cell.getValue() || '').trim().toUpperCase();
          return val === 'Y' ? '<b class="text-primary">사용</b>' : '';
        }
      }
    ]
  })
  grid.on("rowClick", (e, row) => {
    Object.assign(formData, row.getData()); formData.actkind = 'U0'
  })
}

async function fetchList() {
  try {
    const res = await api.post('/hsba/HSBA_090U_STR', {
      actkind: 'S0', cmpycd: authStore.cmpycd,
      custnm: searchParams.custnm,
      custgbn: searchParams.custgbn,
      status: searchParams.status
    })
    grid?.setData(res.data || []);
    activeItemCount.value = (res.data || []).length;
  } catch (e) { vAlertError('조회 실패') }
}

async function save() {
  if (!formData.custnm) return vAlertError('상호를 입력하세요.')
  if (!confirm('저장하시겠습니까?')) return
  try {
    const res = await api.post('/hsba/HSBA_090U_STR', { ...formData, cmpycd: authStore.cmpycd, updemp: authStore.userid })
    if (res.data?.[0]?.result === 'N') return vAlertError(res.data[0].msg || '저장 실패');
    vAlert('저장되었습니다.'); fetchList(); initialize()
  } catch (e) { vAlertError('저장 실패') }
}

function initialize() {
  resetForm(formData);
  Object.assign(formData, {
    actkind: 'A0', custgbn: '010', useyn: 'Y', iogbn: '010',
    stdymd: new Date().toISOString().substring(0, 10).replace(/-/g, ''), clsymd: '99991231'
  });
}

const handleOpenHelp = (type: string) => {
  if (type === 'ADDR') {
    if (!formData.custcd) return vAlertError('거래처를 먼저 선택하세요.');
    openHelp('ADDR', { gubun: 'C5', cmpycd: authStore.cmpycd, gbncd: formData.custcd }, (d: any) => {
      formData.postno = d.postno; formData.address = d.address;
    });
  }
}

const formatDate = (v: string) => v && v.length === 8 ? `${v.slice(0, 4)}-${v.slice(4, 6)}-${v.slice(6, 8)}` : v

onMounted(async () => { await fetchOptions(); nextTick(() => { initGrid(); fetchList() }) })
</script>

<style scoped>
.main-content-wrapper { padding-bottom: 0px !important; }
.erp-table-dense th, .erp-table-dense td {
  height: 32px !important; padding: 0 8px !important; font-size: 12px; vertical-align: middle; border: 1px solid #dee2e6;
}
.erp-table-dense .form-control, .erp-table-dense .form-select, .erp-table-dense .btn {
  height: 26px !important; font-size: 12px !important; border-radius: 2px;
}
.erp-table-dense th { font-weight: 600; color: #495057; }
.grid-container-right { border-bottom: 3px solid #005a9f !important; }
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>