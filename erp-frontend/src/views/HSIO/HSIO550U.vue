<!--
	=============================================================
	프로그램명	: 주문출고처리 (HSIO550U)
	작성일자	: 2025.02.24
	설명        : 발주 내역을 기반으로 출고 처리 (창고 로직 표준화 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom bg-white py-2 px-3 sticky-top shadow-sm">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-truck me-2 text-primary" style="font-size: 16px;"></i>
        영업정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        출고관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">주문출고처리 (HSIO550U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchCustList">조회</button>
        <button class="btn-erp btn-save" @click="save">저장</button>
      </div>
    </div>

    <!-- 🔍 2. 상단 조회 필터 (HSOD100U 표준 테이블 레이아웃 적용) -->
    <div class="p-2 pb-0 flex-shrink-0 bg-light">
      <div class="card border shadow-sm overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
              <col style="width: 10%" /><col style="width: 23%" />
              <col style="width: 10%" /><col style="width: 23%" />
              <col style="width: 10%" /><col style="width: 24%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light required">출고창고</th>
                <td>
                  <select v-model="searchParam.whcd" class="form-select form-select-sm">
                    <option value="">전체</option>
                    <option v-for="opt in whOptions" :key="opt.whcd" :value="opt.whcd">{{ opt.whnm }}</option>
                  </select>
                </td>
                <th class="text-center bg-light required">납품일자</th>
                <td>
                  <div class="d-flex align-items-center gap-1">
                    <DateForm v-model:fromdt="searchParam.fromdt" v-model:todt="searchParam.todt" />
                  </div>
                </td>
                <td colspan="2" class="bg-light"></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- 💡 3. 메인 분할 영역 -->
    <div class="flex-grow-1 d-flex flex-row gap-2 p-2 overflow-hidden bg-light">

      <!-- 🅰️ 좌측: 거래처 목록 -->
      <div class="card border shadow-sm d-flex flex-column flex-shrink-0" style="width: 320px;">
        <div class="card-header bg-white text-dark fw-bold border-bottom py-1 ps-3 h-auto" style="font-size: 13px;">
          <i class="bi bi-list-ul me-1 text-secondary"></i> 미출고 거래처
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="custGridRef" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>

      <!-- 🅱️ 우측: 상세 정보 및 주문 내역 -->
      <div class="d-flex flex-column flex-grow-1 gap-2 overflow-hidden">
        <!-- 마스터 정보 카드 -->
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
                  <th class="required bg-light text-center">거 래 처</th>
                  <td>
                    <div class="input-group input-group-sm flex-nowrap">
                      <input v-model="masterData.custcd" type="text" class="form-control text-center bg-light fw-bold" style="max-width: 65px;" readonly />
                      <input v-model="masterData.custnm" type="text" class="form-control border-start-0" readonly />
                    </div>
                  </td>
                  <th class="required bg-light text-center">출고창고</th>
                  <td>
                    <select v-model="masterData.whcd" class="form-select">
                      <option v-for="opt in whOptions" :key="opt.whcd" :value="opt.whcd">{{ opt.whnm }}</option>
                    </select>
                  </td>
                  <th class="required bg-light text-center">출고일자</th>
                  <td><input v-model="masterData.ioymd" type="date" class="form-control" /></td>
                </tr>
                <tr>
                  <th class="bg-light text-center">배 송 처</th>
                  <td colspan="3">
                    <AddressPopupForm
                      v-model:trancd="masterData.trancd"
                      v-model:postno="masterData.postno"
                      v-model:address="masterData.address"
                      v-model:d_address="masterData.d_address"
                      @open-address="handleOpenHelp('ADDR')"
                    />
                  </td>
                  <th class="bg-light text-center">배송담당</th>
                  <td>
                    <select v-model="masterData.trnemp" class="form-select">
                      <option v-for="user in userData" :key="user.userid" :value="user.userid">{{ user.usernm }}</option>
                    </select>
                  </td>
                </tr>
                <tr>
                  <th class="bg-light text-center">특기사항</th>
                  <td colspan="5"><input v-model="masterData.remark" class="form-control" placeholder="비고" /></td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- 주문 상세 내역 그리드 -->
        <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white grid-container-right">
          <div class="card-header bg-white py-1 px-3 border-bottom d-flex justify-content-between align-items-center flex-shrink-0">
            <span class="fw-bold small text-dark"><i class="bi bi-list-check me-1 text-primary"></i> 주문 상세 내역</span>
            <button class="btn btn-xs btn-outline-secondary px-2" style="height: 24px; font-size: 11px;" @click="toggleAllRows">전체선택</button>
          </div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="detailGridRef" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, nextTick } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import DateForm from '@/components/DateForm.vue'
import AddressPopupForm from '@/components/AddressPopupForm.vue'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import type { ModalProps } from '@/types/modal'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

const now = new Date()
const firstDay = new Date(now.getFullYear(), now.getMonth(), 1).toISOString().substring(0, 10)
const initymd = now.toISOString().substring(0, 10)

// [1] 데이터 모델링
const searchParam = reactive({ whcd: '', fromdt: firstDay, todt: initymd })
const masterData = reactive<any>({
  cmpycd: authStore.cmpycd, ioymd: initymd, whcd: '', custcd: '', custnm: '',
  postno: '', address: '', d_address: '', trancd: '', remark: '', trnemp: authStore.userid, area: ''
})

const custGridRef = ref<HTMLElement | null>(null); let custGrid: Tabulator | null = null;
const detailGridRef = ref<HTMLElement | null>(null); let detailGrid: Tabulator | null = null;
const whOptions = ref<any[]>([]); const userData = ref<any[]>([])

// [2] 그리드 초기화
const initGrids = () => {
  if (custGridRef.value) {
    custGrid = new Tabulator(custGridRef.value, {
      layout: "fitColumns", height: "100%", placeholder: "데이터 없음",
      columns: [{ title: "미출고 거래처명", field: "custnm", widthGrow: 1, cssClass: "fw-bold text-primary" }]
    });
    custGrid.on("rowClick", (e, row) => fetchDetails(row.getData()));
  }

  if (detailGridRef.value) {
    detailGrid = new Tabulator(detailGridRef.value, {
      layout: "fitColumns", height: "100%", placeholder: "주문 내역이 없습니다.",
      columnCalcs: "table",
      columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
      columns: [
        { title: "선택", width: 40, hozAlign: "center", formatter: "rowSelection", titleFormatter: "rowSelection", headerSort: false },
        { title: "주문부서", field: "deptnm", width: 140, hozAlign: "left" },
        { title: "주문일", field: "ordymd", width: 100, hozAlign: "center", formatter: (c) => formatDate(c.getValue()) },
        { title: "납품일", field: "outymd", width: 100, hozAlign: "center", formatter: (c) => formatDate(c.getValue()) },
        { title: "품 목", field: "itemnm", minWidth: 150, widthGrow: 1, cssClass: 'fw-bold', hozAlign: "left" },
        { title: "규격", field: "itsize", width: 120, hozAlign: "left" },
        { title: "미출고", field: "janqty", width: 90, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, bottomCalc: "sum" },
        { title: "출고수량", field: "ioqty", width: 90, hozAlign: "right", editor: "number", cssClass: "bg-yellow fw-bold", bottomCalc: "sum" }
      ]
    });
  }
}

// [3] 로직 처리
const fetchWhOptions = async () => {
  try {
    const resWh = await api.get('/hs00/HS00_000S_STR', {
      params: { gubun: 'W0', cmpycd: authStore.cmpycd }
    })
    whOptions.value = (resWh.data || []).map((i: any) => ({
      whcd: String(i.whcd || '').trim(),
      whnm: String(i.whnm || '').trim()
    }))

    if (whOptions.value.length > 0) {
      masterData.whcd = whOptions.value[0].whcd
    }
  } catch (e) {
    whOptions.value = []
  }
}

const fetchCustList = async () => {
  try {
    const res = await api.post('/hsio/HSIO_550U_STR', {
        actkind: 'S1', cmpycd: authStore.cmpycd || '', iogbn: '200',
        fromdt: (searchParam.fromdt || '').replace(/-/g, ''),
        todt: (searchParam.todt || '').replace(/-/g, ''),
        custcd: '', deptcd: '', ioym: '', iono: '', ioymd: '', iotype: '',
        whcd: searchParam.whcd || '', area: '',
        userid: authStore.userid || '', trnemp: '', trancd: '',
        addres: '', remark: '', cfmyn: '', totsum: 0,
        updemp: authStore.userid || ''
    });
    console.log('📦 [Seed-Model] 조회 결과:', res.data);
    custGrid?.setData(res.data || []);
  } catch (e) { vAlertError('조회 실패') }
}

const fetchDetails = async (row: any) => {
  // 🚀 모든 필드에 대해 null 체크 및 빈 문자열 처리 강제
  masterData.custcd = (row.custcd || '').toString().trim();
  masterData.custnm = (row.custnm || '').toString().trim();
  masterData.postno = (row.postno || '').toString().trim();
  masterData.address = (row.address || '').toString().trim();
  masterData.d_address = (row.d_address || '').toString().trim();
  masterData.trancd = (row.trancd || '').toString().trim();
  masterData.trannm = (row.trannm || '').toString().trim();
  masterData.trnemp = (row.trnemp || '').toString().trim();

  try {
    const res = await api.post('/hsio/HSIO_550U_STR', {
        actkind: 'S0', cmpycd: authStore.cmpycd || '', iogbn: '200',
        fromdt: (searchParam.fromdt || '').replace(/-/g, ''),
        todt: (searchParam.todt || '').replace(/-/g, ''),
        custcd: masterData.custcd || '', deptcd: '', ioym: '', iono: '', ioymd: '', iotype: '',
        whcd: searchParam.whcd || '', area: '',
        userid: authStore.userid || '', trnemp: '', trancd: '',
        address: '', remark: '', cfmyn: '', totsum: 0,
        updemp: authStore.userid || ''
    });
    const detailData = (res.data || []).map((i: any) => ({
        ...i,
        ioqty: i.janqty || 0,
        itemcd: i.itemcd || '',
        itsize: i.itsize || '',
        unit: i.unit || '',
        deptnm: i.deptnm || '',
        ordymd: i.ordymd || '',
        outymd: i.outymd || ''
    }));
    detailGrid?.setData(detailData).then(() => {
        detailGrid?.selectRow(); // 🚀 조회 시 전체 선택 기본값 (Standard)
    });
  } catch (e) { vAlertError('상세 조회 실패') }
}

const save = async () => {
  const items = detailGrid?.getSelectedData().filter((r: any) => Number(r.ioqty) > 0) || [];
  if (!items.length) return vAlertError('출고 항목을 선택하고 수량을 확인하세요.');
  if (!masterData.whcd) return vAlertError('출고 창고를 선택하세요.');

  const ioymd = masterData.ioymd.replace(/-/g, '');

  try {
    const resCls = await api.get('/hp00/HP00_000S_STR', { params: { gubun: 'cl', cmpycd: authStore.cmpycd } });
    if (resCls.data?.length > 0) {
        const sclsym = resCls.data[0].sclsym;
        if (sclsym && ioymd.substring(0, 6) <= sclsym) {
            throw new Error(`영업정보가 마감되었습니다. (${sclsym})`);
        }
    }

    if (!confirm('출고 작업을 진행하시겠습니까?')) return

    const detailItems = items.map((item: any) => {
        const ordQty = Number(item.ordqty) || 1;
        const outQty = Number(item.ioqty) || 0;
        const outAmt = Math.round((Number(item.ordamt) / ordQty) * outQty);
        const outVat = Math.round((Number(item.ordvat) / ordQty) * outQty);
        return {
            ...item,
            ioqty: outQty,
            ioamt: outAmt,
            iovat: outVat,
            balym: item.ordym || item.ORDYM || '',
            balno: item.ordno || item.ORDNO || '',
            browno: item.orowno || item.OROWNO || ''
        };
    });

    const totalAmtSum = detailItems.reduce((acc, cur) => acc + cur.ioamt, 0);

    const saveRequest = {
        mst: {
            actkind: 'A0',
            cmpycd: authStore.cmpycd || '',
            iogbn: '200',
            custcd: masterData.custcd || '',
            deptcd: authStore.deptcd || '',
            ioymd: ioymd || '',
            iotype: '100',
            whcd: masterData.whcd || '',
            area: masterData.area || '',
            userid: authStore.userid || '',
            trnemp: masterData.trnemp || '',
            trancd: masterData.trancd || '',
            addres: masterData.address || '',
            d_address: masterData.d_address || '',
            remark: masterData.remark || '',
            cfmyn: 'Y',
            totsum: totalAmtSum || 0,
            updemp: authStore.userid || ''
        },
        dtl: detailItems
    }

    // 🚀 [Seed-Model 표준 루틴] 통합 저장 API 호출 (백엔드 서비스 단에서 무결성 보장)
    const res = await api.post('/hsio/HSIO_550U_SAVE', saveRequest);

    // ApiResponse 표준 처리
    const resData = res.data?.data || res.data;
    const key1 = (resData?.ioym || resData?.IOYM || '').toString().trim();
    const key2 = (resData?.iono || resData?.IONO || '').toString().trim();

    vAlert(`출고 처리가 완료되었습니다.\n[출고번호: ${key1}-${key2}]`);
    fetchCustList(); initialize();
  } catch (e: any) {
    vAlertError(e.response?.data?.message || e.message || '저장 실패');
  }
}

const toggleAllRows = () => {
  const rows = detailGrid?.getRows(); if (!rows) return
  const allSelected = detailGrid?.getSelectedRows().length === rows.length
  if (allSelected) detailGrid?.deselectRow(); else detailGrid?.selectRow();
}

function initialize() {
  resetForm(masterData); masterData.ioymd = initymd;
  if (whOptions.value.length > 0) masterData.whcd = whOptions.value[0].whcd;
  custGrid?.clearData(); detailGrid?.clearData();
}

function handleOpenHelp(type: string) {
    if (type === 'ADDR') {
        if (!masterData.custcd) return vAlertError('거래처를 먼저 선택하세요.')
            Object.assign(modalProps, {
              title: '배송처 선택',
              path: '/hs00/HS00_000S_STR',
              defaultField: 'trannm',
              data: { gubun: 'T0', cmpycd: authStore.cmpycd, gbncd: '', code: masterData.custcd },
              columns: [
                { title: '코드', field: 'trancd', width: 60, hozAlign: 'center' },
                { title: '배송처명', field: 'custnm', width: 100 },
                { title: '우편번호', field: 'postno', width: 50 },
                { title: '주소', field: 'address', width: 200 },
                { title: '상세주소', field: 'd_address', width: 100 }
              ],
              onConfirm: (d: any) => {
                masterData.trancd = d.trancd;
                masterData.postno = d.postno;
                masterData.address = d.address;
                masterData.d_address = d.d_address || '';
              }
        })
        modalVisible.value = true
    }
}

const formatDate = (val: any) => val && val.length === 8 ? `${val.substring(0,4)}-${val.substring(4,6)}-${val.substring(6,8)}` : val;

onMounted(async () => {
  await fetchWhOptions();
  api.post('/ha00/HA00_00P_STR', { gubun: 'SD', cmpycd: authStore.cmpycd, gbncd: '', code: '', remark: '' }).then(r => {
      userData.value = r.data;
  });
  nextTick(() => { initGrids(); fetchCustList(); });
})
</script>

<style scoped>
.main-content-wrapper { padding-bottom: 0px !important; }
.grid-container-left, .grid-container-right { border-bottom: 3px solid #005a9f !important; }
.erp-table-dense th, .erp-table-dense td {
  height: 32px !important; padding: 0 8px !important; font-size: 12px; vertical-align: middle; border: 1px solid #dee2e6;
}
.erp-table-dense .form-control, .erp-table-dense .form-select, .erp-table-dense .btn {
  height: 26px !important; font-size: 12px !important; border-radius: 2px;
}
.erp-table-dense th { font-weight: 600; color: #495057; }
.tabulator-instance { width: 100% !important; background-color: #fff; }
:deep(.empty-row-style) { opacity: 0.6; }
</style>
