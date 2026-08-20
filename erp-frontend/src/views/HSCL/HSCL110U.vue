<!--
	=============================================================
	프로그램명	: 타계정전표발행 (HSCL110U)
	작성일자	: 2025.02.24
	설명        : 타계정 출고 내역을 회계 전표로 발행 및 관리 (HSOD100U 디자인 표준 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-journal-check me-2 text-primary" style="font-size: 18px;"></i>
        마감관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        전표관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">타계정전표발행 (HSCL110U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button v-if="!form.slipno || form.slipno === '000'" class="btn-erp btn-save" @click="handleSave">전표발행</button>
        <button v-else class="btn-erp btn-delete" @click="handleDelete">전표삭제</button>
        <button v-if="form.slipno && form.slipno !== '000'" class="btn-erp btn-print" @click="handlePrint">인쇄</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 조회 및 발행 조건 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
                <col style="width: 10%" /><col style="width: 25%" />
                <col style="width: 10%" /><col style="width: 15%" />
                <col style="width: 10%" /><col style="width: 30%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="required text-center bg-light">발행부서</th>
                <td>
                  <div class="input-group input-group-sm">
                    <input v-model="form.deptnm" class="form-control fw-bold text-primary" readonly />
                    <button class="btn btn-outline-secondary" @click="openHelp('DEPT')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="required text-center bg-light">출고연월</th>
                <td>
                  <input v-model="form.yymm" type="month" class="form-control form-control-sm" @change="search" />
                </td>
                <th class="text-center bg-light">전표정보</th>
                <td>
                  <div class="d-flex align-items-center gap-2">
                    <input v-model="form.slipymd" type="date" class="form-control form-control-sm fw-bold text-success" style="width: 140px;" />
                    <input v-model="form.slipno" class="form-control form-control-sm text-center bg-light" style="width: 60px;" readonly placeholder="000" />
                    <span v-if="form.slipno && form.slipno !== '000'" class="badge bg-primary px-2">발행완료</span>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 데이터 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>타계정 출고 내역</span>
          <div class="d-flex gap-3 small fw-bold">
            <span class="text-dark">수량합계: <span class="text-primary">{{ formatNumber(totals.qty) }}</span></span>
            <span class="text-dark pe-2">금액합계: <span class="text-danger">{{ formatNumber(totals.amt) }}</span></span>
          </div>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="gridElement" class="tabulator-instance flex-grow-1"></div>
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
import { useCommonHelp } from '@/composables/useCommonHelp'
import { getDate } from '@/composables/useDate'

const authStore = useAuthStore()
const { today } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps } = useCommonHelp()

// 1. 상태 관리
const form = reactive<any>({
  deptcd: authStore.deptcd,
  deptnm: authStore.deptnm,
  yymm: today.substring(0, 7),
  slipymd: today,
  slipno: '',
  scfmyn: '',
  autoslip: 'N',
  clsymd: '',
  sclsym: ''
})

const totals = reactive({ qty: 0, amt: 0 })
const gridElement = ref<HTMLElement | null>(null)
const grid = ref<Tabulator | null>(null)

// 2. 그리드 초기화
const initGrid = () => {
  if (!gridElement.value) return
  grid.value = new Tabulator(gridElement.value, {
    layout: "fitColumns",
    height: "100%",
    placeholder: "조회된 내역이 없습니다.",
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "No", formatter: "rownum", width: 40, hozAlign: "center" },
      { title: "출고일", field: "ioymd", width: 150, hozAlign: "center", formatter: (c) => formatDate(c.getValue()) },
      { title: "품목명", field: "itemnm", minWidth: 250, cssClass: "fw-bold" },
      { title: "규격", field: "itsize", width: 150 },
      { title: "단위", field: "unit", width: 60, hozAlign: "center" },
      { title: "수량", field: "ioqty", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "금액", field: "jsanamt", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "유형", field: "iotypenm", width: 150 },
      { title: "사용부서", field: "usedeptnm", width: 150 },
      { title: "출고창고", field: "whnm", width: 150 }
    ]
  })
}

// 3. 기능 구현
const search = async () => {
  if (!form.deptcd) return vAlertError('발행부서를 선택해 주십시오.')

  try {
    const res = await api.post('/hscl/HSCL_110U_STR', {
      actkind: 'S',
      cmpycd: authStore.cmpycd,
      deptcd: form.deptcd,
      yymm: form.yymm.replace(/-/g, ''),
      slipymd: '',
      slipno: ''
    })

    const data = res.data || []

    if (data.length > 0) {
      const master = data[0];
      form.slipymd = master.slipymd && master.slipymd !== '00000000' ? formatDate(master.slipymd, '-') : today;
      form.slipno = master.slipno || '';
      form.scfmyn = master.scfmyn || 'N';

      grid.value?.setData(data);
      calculateTotals(data);
      vAlert('조회되었습니다.');
    } else {
      grid.value?.setData([]);
      totals.qty = 0; totals.amt = 0;
      vAlert('조회된 내역이 없습니다.');
    }
  } catch (e) { vAlertError('조회 중 오류 발생'); }
}

const handleSave = async () => {
  if (form.slipno && form.slipno !== '000') return vAlertError('이미 전표가 발행되었습니다.');
  if (form.slipymd.replace(/-/g, '') <= form.clsymd) return vAlertError('회계정보가 마감되었습니다.');
  if (form.yymm.replace(/-/g, '') > form.sclsym) return vAlertError('영업마감작업 후 진행하십시오.');

  if (!confirm('전표를 발행하시겠습니까?')) return;

  try {
    await api.post('/hscl/HSCL_110U_SAVE', {
      actkind: 'A',
      cmpycd: authStore.cmpycd,
      yymm: form.yymm.replace(/-/g, ''),
      slipymd: form.slipymd.replace(/-/g, ''),
      deptcd: form.deptcd,
      deptnm: form.deptnm,
      autoslip: form.autoslip,
      updemp: authStore.userid
    });

    vAlert('정상적으로 전표가 발행되었습니다.');
    search();
  } catch (e: any) { vAlertError('발행 중 오류 발생'); }
}

const handleDelete = async () => {
  if (form.autoslip === 'N' && form.scfmyn === 'Y') return vAlertError('이미 확정된 전표는 삭제할 수 없습니다.');
  if (form.slipymd.replace(/-/g, '') <= form.clsymd) return vAlertError('회계정보가 마감되었습니다.');

  if (!confirm('발행된 전표를 삭제하시겠습니까?')) return;

  try {
    await api.post('/hscl/HSCL_110U_SAVE', {
      actkind: 'D',
      cmpycd: authStore.cmpycd,
      yymm: form.yymm.replace(/-/g, ''),
      slipymd: form.slipymd.replace(/-/g, ''),
      slipno: form.slipno,
      deptcd: form.deptcd,
      autoslip: form.autoslip,
      updemp: authStore.userid
    });

    vAlert('전표가 삭제되었습니다.');
    search();
  } catch (e) { vAlertError('삭제 중 오류 발생'); }
}

const handlePrint = () => {
  const url = `/report/HASL_SLIP_PRINT?slipgu=010&slipymd=${form.slipymd.replace(/-/g, '')}&slipno=${form.slipno}&deptcd=${form.deptcd}`;
  window.open(url, 'Print', 'width=800,height=700,scrollbars=yes');
}

const calculateTotals = (data: any[]) => {
  totals.qty = data.reduce((acc, cur) => acc + (Number(cur.ioqty) || 0), 0);
  totals.amt = data.reduce((acc, cur) => acc + (Number(cur.jsanamt) || 0), 0);
}

const initialize = () => {
  resetForm(form);
  Object.assign(form, { deptcd: authStore.deptcd, deptnm: authStore.deptnm, yymm: today.substring(0, 7), slipymd: today, slipno: '', autoslip: 'N' });
  grid.value?.clearData();
  totals.qty = 0; totals.amt = 0;
  fetchInitialSettings();
}

const openHelp = (type: string) => {
  if (type === 'DEPT') {
    Object.assign(modalProps, {
      title: '부서 선택',
      path: '/ha00/HA00_00P_STR',
      data: { gubun: 'D0', cmpycd: authStore.cmpycd },
      columns: [
        { title: '코드', field: 'deptcd', width: 100 },
        { title: '부서명', field: 'deptnm', width: 200 }
      ],
      onConfirm: (d: any) => { form.deptcd = d.deptcd; form.deptnm = d.deptnm; search(); }
    });
    modalVisible.value = true;
  }
}

const fetchInitialSettings = async () => {
  try {
    const resP1 = await api.post('/ha00/HA00_010S_STR', { gubun: 'P1', cmpycd: authStore.cmpycd });
    if (resP1.data?.length) form.autoslip = resP1.data[0].slipyn || 'N';

    const resCL = await api.post('/hs00/HS00_000S_STR', { gubun: 'CL', cmpycd: authStore.cmpycd });
    if (resCL.data?.length) {
      form.clsymd = resCL.data[0].clsymd;
      form.sclsym = resCL.data[0].sclsym;
    }
  } catch (e) { console.error('설정 로드 실패'); }
}

const formatDate = (v: any, sep = '.') => v && v.length === 8 ? `${v.substring(0, 4)}${sep}${v.substring(4, 6)}${sep}${v.substring(6, 8)}` : v;
const formatNumber = (v: any) => new Intl.NumberFormat().format(v || 0);

onMounted(() => {
  nextTick(() => {
    initGrid();
    fetchInitialSettings();
  });
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
