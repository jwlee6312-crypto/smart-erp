<!--
	=============================================================
	프로그램명	: 품목코드관리 (haba010u)
	작성일자	: 2025.03.14
	설명        : 기초 정보성 품목 코드 관리 (소문자 원칙 및 표준 팝업 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showalert" :error="showerror" :message="alertmessage" />
  <Modal v-model:visible="modalvisible" :modalProps="modalprops" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-tag-fill me-2 text-primary" style="font-size: 18px;"></i>
        기본정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        기초관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">품목코드관리 (haba010u)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-save" @click="save">저장</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 입력 폼 -->
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
                <th class="required bg-light text-center">자산구분</th>
                <td>
                  <select v-model="masterdata.astkind" class="form-select form-select-sm" @change="search">
                    <option v-for="opt in assetoptions" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                  </select>
                </td>
                <th class="required bg-light text-center border-start">품목코드</th>
                <td>
                  <input v-model="masterdata.itemcd" class="form-control form-control-sm text-center fw-bold text-primary" maxlength="7" :readonly="masterdata.actkind === 'U0'" />
                </td>
                <th class="required bg-light text-center border-start">품목명</th>
                <td><input v-model="masterdata.itemnm" class="form-control form-control-sm fw-bold" /></td>
              </tr>
              <tr>
                <th class="bg-light text-center border-top">규격</th>
                <td class="border-top"><input v-model="masterdata.itsize" class="form-control form-control-sm" /></td>
                <th class="bg-light text-center border-start border-top">단위</th>
                <td class="border-top">
                  <select v-model="masterdata.unit" class="form-select form-select-sm">
                    <option v-for="opt in unitoptions" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                  </select>
                </td>
                <th class="bg-light text-center border-start border-top">사용여부</th>
                <td class="border-top">
                  <select v-model="masterdata.useyn" class="form-select form-select-sm">
                    <option value="Y">사용</option>
                    <option value="N">중지</option>
                  </select>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 그리드 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white">
        <div class="card-body p-0 flex-grow-1 overflow-hidden d-flex flex-column">
          <div ref="gridelement" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, nextTick } from 'vue'
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

const masterdata = reactive<any>({ actkind: 'A0', cmpycd: authstore.cmpycd, astkind: '120', itemcd: '', itsize: '', unit: 'EA', itemnm: '', useyn: 'Y' })
const assetoptions = ref<any[]>([]); const unitoptions = ref<any[]>([])
const gridelement = ref<HTMLElement | null>(null); let grid: Tabulator | null = null

const initgrid = () => {
  if (!gridelement.value) return
  grid = new Tabulator(gridelement.value, {
    layout: "fitColumns", height: "100%", pagination: "local", paginationSize: 20,
    placeholder: "데이터가 없습니다.", columnDefaults: { headerSort: false, headerHozAlign: "center", hozAlign: "center", vertAlign: "middle" },
    columns: [
      { title: "코드", field: "itemcd", width: 100, cssClass: "fw-bold text-primary" },
      { title: "품목명", field: "itemnm", minWidth: 200, hozAlign: "left" },
      { title: "규격", field: "itsize", width: 180, hozAlign: "left" },
      { title: "단위", field: "unit", width: 80 },
      { title: "사용", field: "useyn", width: 80, formatter: (c) => c.getValue() === 'Y' ? '사용' : '-' }
    ]
  })
  grid.on("rowClick", (e, row) => {
    const data = row.getData(); Object.assign(masterdata, data); masterdata.actkind = 'U0';
  })
}

async function fetchoptions() {
  try {
    const r1 = await api.get('/hs00/hs00_000s_str', { params: { gubun: 'e0', cmpycd: authstore.cmpycd, gbncd: '100' } })
    const r2 = await api.get('/hs00/hs00_000s_str', { params: { gubun: 'U0', cmpycd: authstore.cmpycd, gbncd: '', code: '' } })
    assetoptions.value = r1.data.map((n: any) => ({ codecd: n.code || n.codecd, codenm: n.cdnm || n.codenm }))
    unitoptions.value = r2.data.map((n: any) => ({ codecd: n.unit, codenm: n.unitnm }))
  } catch (e) {}
}

async function search() {
  try {
    const res = await api.post('/hsba/hsba_010u_str', { actkind: 'S0', cmpycd: authstore.cmpycd, astkind: masterdata.astkind, itemnm: '', icqty: 0, ocqty: 0, imprice: 0, omprice: 0, stock: 0, qtypnt: 0 })
    grid?.setData(res.data || []);
  } catch (e) { valerterror('조회 실패') }
}

async function save() {
  if (!masterdata.itemnm || !masterdata.itemcd) return valerterror('품목코드와 명칭은 필수입니다.')
  if (!confirm('저장하시겠습니까?')) return
  try {
    await api.post('/hsba/hsba_010u_str', { ...masterdata, userid: authstore.userid })
    valert('저장되었습니다.'); search(); initialize()
  } catch (e) { valerterror('저장 오류') }
}

function initialize() {
  const currentasset = masterdata.astkind; resetform(masterdata)
  Object.assign(masterdata, { actkind: 'A0', cmpycd: authstore.cmpycd, unit: 'EA', astkind: currentasset, useyn: 'Y' })
}

const modalvisible = ref(false); const modalprops = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

onMounted(async () => { await fetchoptions(); nextTick(() => { initgrid(); search() }) })
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
