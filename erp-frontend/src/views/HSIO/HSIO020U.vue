<!--
	=============================================================
	프로그램명	: 구매 자동발주 요청등록 (HSIO020U)
	작성일자	: 2025.02.24
	설명        : 소요량 분석 데이터를 기반으로 구매 요청 마스터/상세 관리
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-cart-plus-fill me-2 text-primary" style="font-size: 18px;"></i>
        구매관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        구매요청 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">구매 자동발주 요청등록 (HSIO020U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchMaster">조회</button>
        <button class="btn-erp btn-save" @click="save">저장</button>
        <button class="btn-erp btn-delete" @click="deleteRequest" :disabled="!masterData.reqno || masterData.reqno === '0000'">삭제</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 조회 필터 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
                <col style="width: 10%" /><col style="width: 40%" />
                <col style="width: 10%" /><col style="width: 40%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light">요청부서</th>
                <td>
                  <div class="input-group input-group-sm w-75">
                    <input v-model="searchParam.deptcd" class="form-control bg-light text-center" style="max-width: 70px;" readonly />
                    <input v-model="searchParam.deptnm" class="form-control" placeholder="부서 선택" />
                    <button class="btn btn-outline-secondary px-2" @click="openHelp('S_DEPT')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="text-center bg-light">요청번호</th>
                <td class="d-flex align-items-center gap-1">
                  <input v-model="searchParam.reqym" type="month" class="form-control form-control-sm text-center" style="width: 130px;" />
                  <input v-model="searchParam.reqno" type="text" class="form-control form-control-sm text-center" placeholder="0000" style="width: 70px;" @keyup.enter="fetchMaster" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 상세 마스터 정보 폼 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense w-100">
            <colgroup>
              <col style="width: 110px;" /><col />
              <col style="width: 110px;" /><col />
              <col style="width: 110px;" /><col />
              <col style="width: 110px;" /><col />
            </colgroup>
            <tbody>
              <tr>
                <th class="required bg-light">요청부서</th>
                <td>
                  <div class="input-group input-group-sm">
                    <input v-model="masterData.deptcd" class="form-control bg-light text-center" style="max-width: 70px;" readonly />
                    <input v-model="masterData.deptnm" class="form-control" readonly />
                    <button class="btn btn-outline-secondary px-2" @click="openHelp('DEPT')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="bg-light text-center">요청번호</th>
                <td class="d-flex gap-1">
                  <input :value="reqym_disp" class="form-control bg-light text-center fw-bold" readonly />
                  <input v-model="masterData.reqno" class="form-control bg-light text-primary fw-bold text-center" readonly />
                </td>
                <th class="required bg-light text-center">요청일자</th>
                <td><input v-model="reqymd_f" type="date" class="form-control" /></td>
                <th class="required bg-light text-center">담당자</th>
                <td>
                  <select v-model="masterData.userid" class="form-select">
                    <option value="">-- 선택 --</option>
                    <option v-for="item in empOptions" :key="item.userid" :value="item.userid">{{ item.usernm }}</option>
                  </select>
                </td>
              </tr>
              <tr>
                <th class="bg-light text-center">가져오기</th>
                <td>
                  <button class="btn btn-sm btn-outline-primary fw-bold w-100 py-1" @click="handleImportAnalysis">
                    <i class="bi bi-download me-1"></i> 소요량 분석 가져오기
                  </button>
                </td>
                <th class="required bg-light text-center">입고요청일</th>
                <td><input v-model="inymd_f" type="date" class="form-control" /></td>
                <th class="bg-light text-center">특기사항</th>
                <td colspan="3"><input v-model="masterData.remark" class="form-control" placeholder="요청 관련 비고 입력" /></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 상세 품목 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white grid-container-right">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark d-flex align-items-center">
            <i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i> 소요량 분석 및 요청 품목 리스트
          </span>
          <div class="d-flex gap-1">
            <button class="btn btn-sm btn-outline-primary py-0 px-2 fw-bold" @click="addRow" style="font-size: 11px;">+ 행추가</button>
          </div>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column" style="min-height: 0;">
          <div ref="gridElement" class="tabulator-full-height"></div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, computed, nextTick, onUnmounted } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import DateForm from '@/components/DateForm.vue'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { getDate } from '@/composables/useDate'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { today, currentMonth } = getDate()

const initym = currentMonth.replace('-', '')
const initymd = today.replace(/-/g, '')

// [1] 데이터 모델링
const searchParam = reactive({ deptcd: authStore.deptcd, deptnm: authStore.deptnm, reqym: currentMonth, reqno: '' })
const masterData = reactive<any>({
  actkind: 'S0', cmpycd: authStore.cmpycd, reqym: initym, reqno: '0000',
  reqymd: initymd, inymd: initymd, deptcd: authStore.deptcd, deptnm: authStore.deptnm,
  remark: '', asgbn: 'Y', userid: String(authStore.userid || '').trim()
})

const reqym_disp = computed(() => masterData.reqym ? `${masterData.reqym.substring(0, 4)}-${masterData.reqym.substring(4, 6)}` : '')
const reqymd_f = computed({
  get: () => masterData.reqymd ? `${masterData.reqymd.substring(0, 4)}-${masterData.reqymd.substring(4, 6)}-${masterData.reqymd.substring(6, 8)}` : '',
  set: (v) => { if(v) masterData.reqymd = v.replace(/-/g, '') }
})
const inymd_f = computed({
  get: () => masterData.inymd ? `${masterData.inymd.substring(0, 4)}-${masterData.inymd.substring(4, 6)}-${masterData.inymd.substring(6, 8)}` : '',
  set: (v) => { if(v) masterData.inymd = v.replace(/-/g, '') }
})

const modalVisible = ref(false)
const modalProps = reactive<any>({ title: '', path: '', data: {}, columns: [], onConfirm: () => {} })

const gridElement = ref<HTMLElement | null>(null)
let grid: Tabulator | null = null
const empOptions = ref<any[]>([])

// [2] 그리드 초기화
const initGrid = () => {
  if (!gridElement.value) return
  grid = new Tabulator(gridElement.value, {
    layout: "fitColumns", height: "100%", placeholder: "분석된 소요량 데이터가 없습니다.",
    columnDefaults: { minWidth: 90, headerHozAlign: 'center', headerSort: false, vertAlign: 'middle' },
    columns: [
      { title: "No", formatter: "rownum", width: 40, hozAlign: "center" },
      { title: "품목명", field: "itemnm", minWidth: 200, widthGrow: 1, cssClass: "fw-bold text-primary" },
      { title: "규격", field: "itsize", width: 120 },
      { title: "단위", field: "unit", width: 60, hozAlign: "center" },
      { title: "소요량", field: "soqty", width: 80, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "현재고", field: "stkqty", width: 80, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "text-primary" },
      { title: "안전재고", field: "stock", width: 80, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "미입고", field: "nonqty", width: 80, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "text-danger" },
      { title: "요청수량", field: "reqqty", width: 100, hozAlign: "right", editor: "number", formatter: "money", formatterParams: { precision: 0 }, cssClass: "bg-light-yellow fw-bold" },
      { title: "구매단가", field: "imprice", width: 100, hozAlign: "right", editor: "number", formatter: "money", cssClass: "bg-light-yellow" },
      { title: "금액", field: "reqamt", width: 120, hozAlign: "right", formatter: "money", bottomCalc: "sum", bottomCalcFormatter: "money", cssClass: "bg-light fw-bold" },
      { title: "거래처명", field: "custnm", width: 150, hozAlign: "left" },
      { title: "발주번호", field: "balno", width: 100, hozAlign: "center", cssClass: "small text-muted" },
      { title: "삭제", width: 40, hozAlign: "center", formatter: () => "<i class='bi bi-trash text-danger cursor-pointer'></i>", cellClick: (e, c) => { c.getRow().delete(); } }
    ]
  });

  grid.on("cellEdited", (cell: any) => {
    const row = cell.getRow(); const d = row.getData();
    if (['reqqty', 'imprice'].includes(cell.getField())) {
      const amt = Math.floor((Number(d.reqqty) || 0) * (Number(d.imprice) || 0));
      row.update({ reqamt: amt, upkind: d.upkind === 'A' ? 'A' : 'U' });
    }
  });
}

// [3] 주요 로직
async function fetchMaster() {
  if (!searchParam.reqym || !searchParam.reqno) return vAlertError('조회할 요청번호를 입력하세요.');
  try {
    const res = await api.post('/hsio/HSIO_020U_STR', {
        ...searchParam,
        reqym: searchParam.reqym.replace('-', ''),
        actkind: 'S0', cmpycd: authStore.cmpycd
    });
    if (res.data?.length) {
      Object.assign(masterData, res.data[0]);
      fetchDetail();
    } else { vAlertError('조회 결과가 없습니다.') }
  } catch (e) { vAlertError('조회 실패') }
}

async function fetchDetail() {
  try {
    const res = await api.post('/hsio/HSIO_021U_STR', { ...masterData, actkind: 'S0' });
    grid?.setData(res.data || []);
  } catch (e) { vAlertError('상세 로드 실패') }
}

async function handleImportAnalysis() {
  if (!masterData.deptcd) return vAlertError('요청부서를 먼저 선택하세요.');
  vAlert('소요량 데이터를 분석하여 가져옵니다...');
  try {
    const res = await api.post('/hsio/HSIO_020U_STR', { ...masterData, actkind: 'S1' });
    if (grid && res.data) {
      grid.setData(res.data.map((i: any) => ({ ...i, upkind: 'A', reqqty: i.soqty })));
      vAlert(`${res.data.length}건의 소요량이 분석되었습니다.`);
    }
  } catch (e) { vAlertError('가져오기 실패') }
}

async function save() {
  const items = grid?.getData() || [];
  if (items.length === 0) return vAlertError('저장할 품목이 없습니다.');

  if (!confirm('저장하시겠습니까?')) return

  try {
    const act = (masterData.reqno === '0000' || !masterData.reqno) ? 'A0' : 'U0';

    // 🚀 [Rule] 모든 마스터 필드를 정제 (NULL 방지, 숫자 준수)
    const mstPayload = {
        ...masterData,
        actkind: act,
        updemp: authStore.userid || '',
        reqym: (masterData.reqym || '').replace(/-/g, ''),
        reqymd: (masterData.reqymd || '').replace(/-/g, ''),
        inymd: (masterData.inymd || '').replace(/-/g, '')
    };

    const mRes = await api.post('/hsio/HSIO_020U_STR', mstPayload);
    const mstResult = mRes.data?.[0];

    // 무결성 키 추출 (0:년월, 1:요청번호)
    const rowValues = Object.values(mstResult || {});
    const resKey1 = String(rowValues[0] || '').trim();
    const resKey2 = String(rowValues[1] || '').trim();

    if (resKey1 === '000000') throw new Error(resKey2 || '업무 규칙 위반으로 저장할 수 없습니다.');
    if (!resKey1 || !resKey2) throw new Error('요청 번호를 서버로부터 수신하지 못했습니다.');

    // 🚀 상세 내역 저장 (A0/U0 루프)
    for (const item of items) {
      const dtlPayload = {
        ...item,
        actkind: (item.upkind || 'U') + '0',
        cmpycd: authStore.cmpycd || '',
        reqym: resKey1,
        reqno: resKey2,
        rrowno: item.rrowno || '', // 💡 항번 공백 유지
        deptcd: masterData.deptcd || '',
        updemp: authStore.userid || '',
        reqqty: Number(item.reqqty) || 0, // 💡 숫자형자료 준수
        imprice: Number(item.imprice) || 0,
        reqamt: Number(item.reqamt) || 0
      };

      const resDtl = await api.post('/hsio/HSIO_021U_STR', dtlPayload);
      const dtlValues = Object.values(resDtl.data?.[0] || {});
      if (String(dtlValues[0]).trim() === '000000') throw new Error(String(dtlValues[1]) || '상세 저장 오류');
    }

    vAlert('저장되었습니다.');
    masterData.reqno = resKey2;
    fetchMaster();
  } catch (e: any) { vAlertError(e.message || '저장 중 오류 발생'); }
}

async function deleteRequest() {
  if (!confirm('요청 자료를 전체 삭제하시겠습니까?')) return;
  try {
    await api.post('/hsio/HSIO_020U_STR', { ...masterData, actkind: 'D0' });
    vAlert('삭제되었습니다.'); initialize();
  } catch (e) { vAlertError('삭제 실패') }
}

function initialize() {
  resetForm(masterData);
  Object.assign(masterData, {
      cmpycd: authStore.cmpycd, reqym: initym, reqno: '0000', reqymd: initymd, inymd: initymd,
      deptcd: authStore.deptcd, deptnm: authStore.deptnm, asgbn: 'Y', userid: String(authStore.userid || '').trim()
  });
  grid?.clearData();
}

function openHelp(type: string) {
  if (type === 'DEPT' || type === 'S_DEPT') {
    Object.assign(modalProps, {
      title: '부서 선택',
      path: '/ha00/HA00_00P_STR',
      data: { gubun: 'D0', cmpycd: authStore.cmpycd },
      columns: [
        { title: '코드', field: 'deptcd', width: 80, hozAlign: 'center' },
        { title: '부서명', field: 'deptnm', width: 200 }
      ],
      onConfirm: (d: any) => {
        if (type === 'S_DEPT') { searchParam.deptcd = d.deptcd; searchParam.deptnm = d.deptnm }
        else { masterData.deptcd = d.deptcd; masterData.deptnm = d.deptnm }
      }
    });
    modalVisible.value = true;
  }
}

function addRow() { grid?.addRow({ upkind: 'A', reqqty: 0, imprice: 0, reqamt: 0, rrowno: '' }); }

onUnmounted(() => {
  if (grid) grid.destroy();
});

onMounted(() => {
  // 담당자(사원) 콤보 로드
  api.post('/ha00/HA00_00P_STR', { gubun: 'SD', cmpycd: authStore.cmpycd }).then(r => {
    empOptions.value = r.data.map((i: any) => ({ userid: i.userid, usernm: i.usernm }));
  })
  nextTick(initGrid);
})
</script>

<style scoped>
.tabulator-full-height { width: 100% !important; background-color: #fff; border-bottom: 3px solid #005a9f !important; }
.bg-light-yellow { background-color: #fffde7 !important; }
</style>
