<!--
	=============================================================
	프로그램명	: 자재불출요청 (HPIO250U)
	작성일자	: 2025.02.24
	설명        : 작업지시 기반 자재 불출 요청 및 상세 관리
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-box-arrow-right me-2 text-primary" style="font-size: 18px;"></i>
        생산관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        자재관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">자재불출요청 (HPIO250U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="handleSearch">조회</button>
        <button class="btn-erp btn-save" @click="handleSave">저장</button>
        <button class="btn-erp btn-delete" @click="handleFullDelete" :disabled="!form_mst.outno || form_mst.outno === '0000'">전체삭제</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light">

      <!-- [상단] 조회 필터 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
                <col style="width: 100px" /><col style="width: 320px" />
                <col style="width: 100px" /><col />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light">불출일자</th>
                <td class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                    <input v-model="searchForm.fromdt" type="date" class="form-control form-control-sm" style="width: 140px;" />
                    <span class="px-1 opacity-50">~</span>
                    <input v-model="searchForm.todt" type="date" class="form-control form-control-sm" style="width: 140px;" />
                </td>
                <th class="text-center bg-light">라인선택</th>
                <td>
                  <select v-model="searchForm.linecd" class="form-select form-select-sm" style="max-width: 200px;">
                    <option value="">전체라인</option>
                    <option v-for="item in lineData" :key="item.linecd" :value="item.linecd">{{ item.linenm }}</option>
                  </select>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 3단 레이아웃 영역 -->
      <div class="d-flex gap-2 flex-grow-1 overflow-hidden" style="min-height: 0;">

        <!-- ⬅️ 좌측: 불출 이력 목록 -->
        <div class="card border shadow-sm d-flex flex-column overflow-hidden grid-container-left" style="width: 420px; min-width: 420px;">
          <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark d-flex justify-content-between align-items-center">
            <span>불출 요청 내역</span>
          </div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="tableRef1" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>

        <!-- ➡️ 우측: 상세 정보 -->
        <div class="flex-grow-1 d-flex flex-column gap-2 overflow-hidden">

          <!-- [1] 마스터 상세 정보 -->
          <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
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
                    <th class="required bg-light small">요청라인</th>
                    <td>
                      <select v-model="form_mst.linecd" class="form-select form-select-sm">
                        <option v-for="item in lineData" :key="item.linecd" :value="item.linecd">{{ item.linenm }}</option>
                      </select>
                    </td>
                    <th class="bg-light small">요청번호</th>
                    <td>
                      <input :value="displayOutNo" class="form-control form-control-sm bg-light text-primary fw-bold text-center" readonly />
                    </td>
                    <th class="required bg-light small">요청일자</th>
                    <td><input v-model="form_mst.outymd" type="date" class="form-control form-control-sm" /></td>
                    <th class="required bg-light small">희망일자</th>
                    <td><input v-model="form_mst.hope_inymd" type="date" class="form-control form-control-sm border-primary" /></td>
                  </tr>
                  <tr>
                    <th class="required bg-light small">출고창고</th>
                    <td>
                      <select v-model="form_mst.whcd" class="form-select form-select-sm">
                        <option v-for="wh in whData" :key="wh.whcd" :value="wh.whcd">{{ wh.whnm }}</option>
                      </select>
                    </td>
                    <th class="required bg-light small">현장창고</th>
                    <td>
                      <select v-model="form_mst.iwhcd" class="form-select form-select-sm">
                        <option v-for="wh in whData" :key="wh.whcd" :value="wh.whcd">{{ wh.whnm }}</option>
                      </select>
                    </td>
                    <th class="bg-light small">요청자</th>
                    <td>
                      <select v-model="form_mst.userid" class="form-select form-select-sm">
                          <option v-for="item in userData" :key="item.userid" :value="item.userid">{{ item.usernm }}</option>
                      </select>
                    </td>
                    <th class="bg-light small">요청부서</th>
                    <td>
                      <div class="input-group input-group-sm">
                        <input v-model="form_mst.deptnm" class="form-control bg-light" readonly />
                        <button class="btn btn-outline-secondary" @click="handleOpenHelp('DEPT')"><i class="bi bi-search"></i></button>
                      </div>
                    </td>
                  </tr>
                  <tr>
                    <th class="bg-light small">비고</th>
                    <td colspan="7"><input v-model="form_mst.bigo" class="form-control form-control-sm" /></td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- [2] 제품 리스트 (Middle) -->
          <div class="card border shadow-sm overflow-hidden d-flex flex-column" style="height: 35%;">
            <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
              <span class="fw-bold small text-dark"><i class="bi bi-journal-check me-2 text-primary"></i>불출 대상 제품</span>
              <div class="d-flex gap-1">
                <button class="btn btn-xs btn-outline-primary py-0 px-2 fw-bold" @click="openJobOrderPop" style="font-size: 11px;">+ 작업지시 추가</button>
                <button class="btn btn-xs btn-outline-danger py-0 px-2 fw-bold" @click="deleteRows('MID')" style="font-size: 11px;">- 삭제</button>
              </div>
            </div>
            <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
              <div ref="tableRef2" class="tabulator-instance flex-grow-1"></div>
            </div>
          </div>

          <!-- [3] 자재 상세 (Detail) -->
          <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column grid-container-right">
            <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
              <span class="fw-bold small text-dark"><i class="bi bi-box-seam me-2 text-success"></i>불출 요청 자재 상세</span>
              <div class="d-flex gap-1">
                <button class="btn btn-xs btn-outline-success py-0 px-2 fw-bold" @click="fetchBOMExplosion" style="font-size: 11px;">BOM 전개</button>
                <button class="btn btn-xs btn-outline-primary py-0 px-2 fw-bold" @click="addRow('DTL')" style="font-size: 11px;">+ 행추가</button>
              </div>
            </div>
            <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
              <div ref="tableRef3" class="tabulator-instance flex-grow-1"></div>
            </div>
          </div>

        </div>
      </div>
    </div>
  </div>

  <!-- 🚀 작업지시 검색 팝업 -->
  <div v-if="jobOrderVisible" class="modal fade show d-block" tabindex="-1" style="background: rgba(0,0,0,0.5); z-index: 1060;">
    <div class="modal-dialog modal-lg modal-dialog-centered">
      <div class="modal-content shadow-lg border-0">
        <div class="modal-header bg-white py-2 border-bottom">
          <h5 class="modal-title fw-bold small"><i class="bi bi-search me-2 text-primary"></i>작업지시 검색</h5>
          <button type="button" class="btn-close small" @click="jobOrderVisible = false"></button>
        </div>
        <div class="modal-body p-3 bg-light">
          <div class="d-flex align-items-center gap-2 mb-3 bg-white p-2 border rounded shadow-sm">
            <span class="small fw-bold text-muted ps-1">지시기간</span>
            <input v-model="jobSearch.frymd" type="date" class="form-control form-control-sm" style="width: 140px;" />
            <span class="text-muted">~</span>
            <input v-model="jobSearch.toymd" type="date" class="form-control form-control-sm" style="width: 140px;" />
            <button class="btn btn-primary btn-sm px-3 fw-bold ms-auto" @click="searchJobOrders">조회</button>
          </div>
          <div class="border rounded bg-white overflow-hidden" style="height: 400px; position: relative;">
            <div v-if="jobLoading" class="position-absolute top-50 start-50 translate-middle text-center" style="z-index: 10;">
              <div class="spinner-border text-primary spinner-border-sm mb-1"></div>
              <div class="small fw-bold text-primary">조회 중...</div>
            </div>
            <div ref="jobGridRef" class="tabulator-instance h-100"></div>
          </div>
        </div>
        <div class="modal-footer py-2 bg-white">
          <button class="btn btn-outline-secondary btn-sm px-4 fw-bold" @click="jobOrderVisible = false">닫기</button>
          <button class="btn btn-primary btn-sm px-4 fw-bold" @click="confirmJobOrders">선택추가</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, computed, nextTick } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useCommonHelp } from '@/composables/useCommonHelp'
import { getDate } from '@/composables/useDate'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'

const authStore = useAuthStore()
const { firstDay, today } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { modalVisible, modalProps } = useCommonHelp()

// [1] 데이터 모델링
const searchForm = reactive({ fromdt: firstDay, todt: today, linecd: '' })
const form_mst = reactive<any>({
  cmpycd: authStore.cmpycd, outym: today.replace(/-/g, '').substring(0, 6), outno: '0000',
  outymd: today, hope_inymd: today, deptcd: authStore.deptcd, deptnm: authStore.deptnm,
  whcd: '100', iwhcd: '300', linecd: '010', userid: authStore.userid, bigo: ''
})

const displayOutNo = computed(() => (!form_mst.outno || form_mst.outno === '0000') ? '' : `${form_mst.outym}-${form_mst.outno}`)
const lineData = ref<any[]>([]); const userData = ref<any[]>([]); const whData = ref<any[]>([])
const tableRef1 = ref<HTMLDivElement | null>(null); const tableRef2 = ref<HTMLDivElement | null>(null); const tableRef3 = ref<HTMLDivElement | null>(null)
let grid1: Tabulator | null = null; let grid2: Tabulator | null = null; let grid3: Tabulator | null = null

const jobOrderVisible = ref(false); const jobLoading = ref(false); const jobSearch = reactive({ frymd: today, toymd: today }); const jobGridRef = ref<HTMLDivElement | null>(null); let jobGrid: Tabulator | null = null

// [2] 그리드 초기화
const initGrids = () => {
  grid1 = new Tabulator(tableRef1.value!, {
    layout: "fitColumns", height: "100%", placeholder: "데이터 없음",
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "No", formatter: "rownum", width: 40, hozAlign: "center" },
      { title: "요청일자", field: "outymd", width: 120, hozAlign: "center", formatter: (c) => formatDate(c.getValue()) },
      { title: "요청번호", field: "outno_full", width: 150, hozAlign: "center", cssClass: "fw-bold text-primary" },
      { title: "생산라인", field: "linenm", widthGrow: 1, hozAlign: "left" }
    ],
  });
  grid1.on("rowClick", (e, row) => fetchDetail(row.getData()));

  grid2 = new Tabulator(tableRef2.value!, {
    layout: "fitColumns", height: "100%", placeholder: "작업지시를 추가하세요.", selectable: true,
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "선택", width: 40, hozAlign: "center", formatter: "rowSelection", titleFormatter: "rowSelection" },
      { title: "상태", field: "_status", width: 60, hozAlign: "center", formatter: (c) => statusBadge(c.getValue()) },
      { title: "지시번호", field: "lotno_full", width: 150, hozAlign: "center", mutator: (v,d) => d.lotymd && d.lotno ? `${d.lotymd}-${d.lotno}` : "" },
      { title: "주문정보", field: "ordno_full", width: 180, hozAlign: "center", mutator: (v,d) => d.gubun && d.ordym && d.ordno ? `${d.gubun}-${d.ordym}-${d.ordno}` : "" },
      { title: "제품명", field: "itemnm", minWidth: 200, widthGrow: 1, hozAlign: "left", cssClass: "fw-bold text-primary" },
      { title: "규격", field: "itsize", width: 180 },
      { title: "단위", field: "unit", width: 60, hozAlign: "center" },
      { title: "지시량", field: "ordqty", width: 110, hozAlign: "right", formatter: "money" },
      { title: "불출량", field: "outqty", width: 110, hozAlign: "right", editor: "number", cssClass: "bg-light-yellow fw-bold" }
    ]
  });

  grid3 = new Tabulator(tableRef3.value!, {
    layout: "fitColumns", height: "100%", placeholder: "BOM 전개를 실행하거나 자재를 추가하세요.", selectable: true,
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "선택", width: 40, hozAlign: "center", formatter: "rowSelection", titleFormatter: "rowSelection" },
      { title: "상태", field: "_status", width: 60, hozAlign: "center", formatter: (c) => statusBadge(c.getValue()) },
      { title: "자재코드", field: "mitemcd", width: 100, hozAlign: "center" },
      { title: "자재명", field: "mitemnm", minWidth: 200, widthGrow: 1, hozAlign: "left", cssClass: "fw-bold text-success", cellClick: (e, cell) => handleOpenHelp('ITEM', cell.getRow()) },
      { title: "규격", field: "mitsize", width: 150 },
      { title: "단위", field: "munit", width: 60, hozAlign: "center" },
      { title: "요청수량", field: "reqqty", width: 120, hozAlign: "right", editor: "number", formatter: "money", cssClass: "bg-light-green fw-bold" },
      { title: "불출량", field: "outqty", width: 100, hozAlign: "right", formatter: "money", cssClass: "bg-light" },
      { title: "대체자재", field: "ritemcd", width: 120, hozAlign: "center", editor: "input" }
    ]
  });
}

// [3] 비즈니스 로직
const handleSearch = async () => {
  try {
    // 🚀 사용자 XML 정의에 맞춰 파라미터 전달 (S1: 목록조회)
    const res = await api.post('/hpio/HPIO_250U_STR', {
      actkind: 'L0',
      cmpycd: authStore.cmpycd,
      fromdt: searchForm.fromdt.replace(/-/g, ''),
      todt: searchForm.todt.replace(/-/g, ''),
      linecd: searchForm.linecd || '',
      ordqty: 0, outqty: 0
    });

    if (res.data) {
      const normalizedData = (res.data || []).map((i: any) => {
          const item = Object.fromEntries(Object.entries(i).map(([k, v]) => [k.toLowerCase(), v]));
          return { ...item, outno_full: `${item.outym}-${item.outno}` };
      });
      grid1?.setData(normalizedData);
      vAlert('조회되었습니다.');
    }
  } catch (e) {
    console.error('❌ [SEARCH ERROR]:', e);
    vAlertError('조회 실패');
  }
}

const fetchDetail = async (row: any) => {
  Object.assign(form_mst, { ...row, outymd: formatDate(row.outymd), hope_inymd: formatDate(row.hope_inymd) });
  try {
    const resMid = await api.post('/hpio/HPIO_250U_STR', {
        actkind: 'S1',
        cmpycd: authStore.cmpycd,
        outym: row.outym,
        outno: row.outno,
        outymd: row.outymd,
        ordqty: 0, outqty: 0
     });
    grid2?.setData(resMid.data.map((i: any) => ({ ...i, _status: '', _state: 'EXIST' })));
    const resDtl = await api.post('/hpio/HPIO_253U_STR', {
        actkind: 'S0',
        cmpycd: authStore.cmpycd,
        outym: row.outym,
        outno: row.outno,
        reqqty: 0
     });
    grid3?.setData(resDtl.data.map((i: any) => ({ ...i, _status: '', _state: 'EXIST' })));
  } catch (e) { vAlertError('조회 실패') }
}

const handleSave = async () => {
  if (!form_mst.linecd || !form_mst.whcd || !form_mst.iwhcd) return vAlertError('라인 및 창고 정보를 확인하세요.');
  if (!confirm('불출 요청 정보를 저장하시겠습니까?')) return

  try {
    const mstData = {
        ...form_mst,
        actkind: (!form_mst.outno || form_mst.outno === '0000') ? 'A0' : 'U0',
        outymd: form_mst.outymd.replace(/-/g, ''),
        hope_inymd: form_mst.hope_inymd.replace(/-/g, ''),
        ordqty: 0, outqty: 0
    };

    const midListData = (grid2?.getRows() || []).map(row => {
        const item = row.getData();
        return {
            ...item, cmpycd: authStore.cmpycd, outymd: mstData.outymd,
            lotymd: item.lotymd || '', lotno: item.lotno || '', gubun: item.gubun || '', ordym: item.ordym || '', ordno: item.ordno || '',
            ordqty: Number(item.ordqty || 0), outqty: Number(item.outqty || 0),
            actkind: item._status === '입력' || item._state === 'NEW' ? 'A1' : (item._status === '삭제' ? 'D0' : 'U1')
        };
    });

    const dtlListData = (grid3?.getRows() || []).map(row => {
        const item = row.getData();
        return {
            ...item, cmpycd: authStore.cmpycd,
            reqqty: Number(item.reqqty || 0),
            actkind: item._status === '입력' || item._state === 'NEW' ? 'A0' : (item._status === '삭제' ? 'D0' : 'U0')
        };
    });

    const payload = { cmpycd: authStore.cmpycd, updemp: authStore.userid, mst: mstData, midlist: midListData, dtllist: dtlListData };
    const res = await api.post('/hpio/HPIO_250U_SAVE', payload);

    if (res.data && res.data.res === 'OK') {
        vAlert('저장되었습니다.');
        if (res.data.midlist) grid2?.setData(res.data.midlist.map((i: any) => ({ ...i, _status: '', _state: 'EXIST' })));
        if (res.data.dtllist) grid3?.setData(res.data.dtllist.map((i: any) => ({ ...i, _status: '', _state: 'EXIST' })));
        if (res.data.outno) form_mst.outno = res.data.outno;
        handleSearch();
    }
  } catch (e: any) { vAlertError('저장 실패: ' + (e.response?.data?.error || e.message)); }
}

const fetchBOMExplosion = () => {
    if (grid3?.getData().length) {
        if (!confirm('이미 자재 내역이 존재합니다. 지우고 다시 BOM 전개하시겠습니까?')) return;
    }
    grid3?.clearData();
    vAlert('저장 시 자동으로 BOM이 전개됩니다. 제품 리스트 확인 후 저장을 눌러주세요.');
}

const openJobOrderPop = () => {
  if (!form_mst.linecd) return vAlertError('라인을 선택하세요.');
  jobOrderVisible.value = true;
  nextTick(() => {
    if (jobGrid) { jobGrid.destroy(); jobGrid = null; }
    jobGrid = new Tabulator(jobGridRef.value!, {
      layout: "fitColumns", height: "100%", selectable: true,
      columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
      columns: [
        { title: "선택", width: 40, formatter: "rowSelection", titleFormatter: "rowSelection" },
        { title: "지시일자", field: "lotymd", width: 110, hozAlign: "center", formatter: (c) => formatDate(c.getValue()) },
        { title: "지시번호", field: "lotno", width: 100, hozAlign: "center" },
        { title: "주문구분", field: "gubun", width: 110, hozAlign: "center" },
        { title: "주문년월", field: "ordym", width: 110, hozAlign: "center" },
        { title: "주문번호", field: "ordno", width: 100, hozAlign: "center" },
        { title: "제품명", field: "itemnm", minWidth: 200, widthGrow: 1, hozAlign: "left" },
        { title: "지시량", field: "ordqty", width: 100, hozAlign: "right", formatter: "money" },
        { title: "기불출량", field: "outqty", hozAlign: "right", formatter: "money" },
        { title: "잔량", field: "remqty", width: 100, hozAlign: "right", formatter: "money", mutator: (v,d) => (Number(d.ordqty || 0)) - (Number(d.outqty || 0)) }
      ]
    });
    searchJobOrders();
  });
}

const searchJobOrders = async () => {
  jobLoading.value = true;
  try {
    const res = await api.post('/hpio/HPIO_250U_POP', { cmpycd: authStore.cmpycd, linecd: form_mst.linecd, frymd: jobSearch.frymd.replace(/-/g, ''), toymd: jobSearch.toymd.replace(/-/g, '') });
    jobGrid?.setData(res.data);
  } catch (e) { vAlertError('조회 실패') }
  finally { jobLoading.value = false; }
}

const confirmJobOrders = () => {
  const selected = jobGrid?.getSelectedData() || [];
  selected.forEach((item: any) => {
    if (!grid2?.getData().find(r => r.lotno === item.lotno && r.lotymd === item.lotymd)) {
      grid2?.addRow({ ...item, outqty: (item.ordqty || 0) - (item.outqty || 0), _status: '입력', _state: 'NEW' }, true);
    }
  });
  jobOrderVisible.value = false;
}

const handleOpenHelp = (type: string, target?: any) => {
  const commonPath = '/ha00/HA00_00P_STR'
  const hpPath = '/hp00/HP00_000S_STR'

  switch (type) {
    case 'DEPT': // 부서 선택
      Object.assign(modalProps, {
        title: '부서 선택',
        path: commonPath,
        defaultField: 'deptnm',
        data: { gubun: 'D0', cmpycd: authStore.cmpycd },
        columns: [
          { title: '부서코드', field: 'deptcd', width: 100, hozAlign: 'center' },
          { title: '부서명', field: 'deptnm', minWidth: 200 }
        ],
        onConfirm: (d: any) => { form_mst.deptcd = d.deptcd; form_mst.deptnm = d.deptnm }
      })
      break

    case 'ITEM': // 자재 선택
      Object.assign(modalProps, {
        title: '자재 선택',
        path: hpPath,
        defaultField: 'itemnm',
        large: true,
        data: { gubun: 'I0', cmpycd: authStore.cmpycd, gbncd: 'A', code: '', remark: '' },
        columns: [
          { title: '자재코드', field: 'itemcd', width: 120, hozAlign: 'center' },
          { title: '자재명', field: 'itemnm', minWidth: 250, widthGrow: 1 },
          { title: '규격', field: 'itsize', width: 150 },
          { title: '단위', field: 'unit', width: 80, hozAlign: 'center' }
        ],
        onConfirm: (d: any) => {
          target.update({
            mitemcd: d.itemcd,
            mitemnm: d.itemnm,
            mitsize: d.itsize,
            munit: d.unit,
            _status: '입력',
            _state: 'NEW'
          })
        }
      })
      break
  }
  modalVisible.value = true
}

const initialize = () => {
  Object.assign(form_mst, { outno: '0000', outym: today.replace(/-/g, '').substring(0, 6), outymd: today, hope_inymd: today, bigo: '' });
  grid1?.clearData(); grid2?.clearData(); grid3?.clearData();
}

const handleFullDelete = async () => {
  if (!form_mst.outno || form_mst.outno === '0000') return;
  if (!confirm('불출 요청 전체를 삭제하시겠습니까?')) return;
  try {
    await api.post('/hpio/HPIO_250U_STR', {
      actkind: 'D0',
      cmpycd: authStore.cmpycd,
      outym: form_mst.outym,
      outno: form_mst.outno,
      ordqty: 0, outqty: 0
    });
    vAlert('삭제되었습니다.');
    initialize();
    handleSearch();
  } catch (e) { vAlertError('삭제 실패'); }
}

const deleteRows = (type: string) => {
  const g = type === 'MID' ? grid2 : grid3;
  g?.getSelectedRows().forEach(row => row.delete());
}

const addRow = (type: string) => { if (type === 'DTL') grid3?.addRow({ reqqty: 0, outqty: 0, _status: '입력', _state: 'NEW' }, true); }
const formatDate = (v: any) => v && v.length === 8 ? `${v.substring(0, 4)}-${v.substring(4, 6)}-${v.substring(6, 8)}` : v;
const statusBadge = (v: string) => v === '입력' ? '<span class="badge bg-primary">신규</span>' : (v === '수정' ? '<span class="badge bg-warning">수정</span>' : '');

onMounted(() => {
  nextTick(initGrids);
  api.get('/hp00/HP00_000S_STR', { params: { gubun: 'L0', cmpycd: authStore.cmpycd, gbncd: 'Y' } }).then(res => lineData.value = res.data);
  api.post('/ha00/HA00_00P_STR', { gubun: 'SD', cmpycd: authStore.cmpycd, gbncd: '', code: '', remark: '' }).then(res => userData.value = res.data);
  api.get('/hs00/HS00_000S_STR', { params: { gubun: 'W0', cmpycd: authStore.cmpycd } }).then(res => whData.value = res.data);
});
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; font-size: 12px; }
.grid-container-left { border-bottom: 3px solid #005a9f !important; }
.grid-container-right { border-bottom: 3px solid #198754 !important; }
</style>
