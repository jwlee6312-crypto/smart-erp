<!--
	=============================================================
	프로그램명	: 수입비용 전표대체 (HSIP140U)
	작성일자	: 2025.03.14
	설명        : 미발행 수입 비용 전표 발행 및 지불 정보 관리 (은행코드/계좌번호/매입처 상식적 분리 저장)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-wallet2 me-2 text-primary" style="font-size: 18px;"></i>
        수입관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        수입비용관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">수입비용 전표대체 (HSIP140U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchUnissuedList">조회</button>
        <button class="btn-erp btn-save" @click="handleGenerateSlip">전표발행</button>
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
                <th class="text-center bg-light">수입일자</th>
                <td class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                  <DateForm v-model:fromdt="searchForm.fromdt" v-model:todt="searchForm.todt" />
                </td>
                <th class="text-center bg-light">부서</th>
                <td>
                  <div class="input-group input-group-sm" style="width: 250px;">
                    <input v-model="searchForm.deptnm" class="form-control" readonly @click="openHelp('DEPT', (d) => { searchForm.deptcd = d.deptcd; searchForm.deptnm = d.deptnm })" />
                    <button class="btn btn-outline-secondary px-2" @click="openHelp('DEPT', (d) => { searchForm.deptcd = d.deptcd; searchForm.deptnm = d.deptnm })"><i class="bi bi-search"></i></button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 투-그리드 레이아웃 -->
      <div class="d-flex gap-2 flex-grow-1 overflow-hidden" style="min-height: 0;">

        <!-- ⬅️ 좌측: 미발행 수입 비용 목록 -->
        <div class="card border shadow-sm d-flex flex-column overflow-hidden grid-container-left" style="width: 555px; min-width: 550px;">
          <div class="card-header bg-white py-1 px-3 border-bottom d-flex justify-content-between align-items-center">
            <span class="fw-bold small text-dark">미발행 수입 비용</span>
            <span class="badge bg-primary-subtle text-primary border border-primary-subtle px-2">대상 합계: {{ formatNumber(targetTotalAmt) }}</span>
          </div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="mainGridRef" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>

        <!-- ➡️ 우측: 지불 상세 정보 (부가세 + 지불그리드) -->
        <div class="flex-grow-1 d-flex flex-column gap-2 overflow-hidden">

          <!-- 부가세 정보 (차변 거래처: 매입처) -->
          <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
            <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark">부가세 정보 (차변용)</div>
            <div class="card-body p-0 bg-white">
              <table class="erp-table-dense w-100">
                <colgroup>
                  <col style="width: 120px;" /><col />
                  <col style="width: 120px;" /><col />
                </colgroup>
                <tbody>
                  <tr>
                    <th class="bg-light text-center border-bottom-0">신고사업장</th>
                    <td class="border-bottom-0">
                      <select v-model="vatForm.taxunit" class="form-select form-select-sm">
                        <option v-for="opt in taxUnitOptions" :key="opt.taxunit" :value="opt.taxunit">{{ opt.unitnm }}</option>
                      </select>
                    </td>
                    <th class="bg-light text-center border-bottom-0">매입처(차변)</th>
                    <td class="border-bottom-0">
                      <div class="input-group input-group-sm">
                        <input v-model="vatForm.custnm" class="form-control" readonly @click="openHelp('CUST', (d) => { vatForm.custcd = d.custcd; vatForm.custnm = d.custnm }, { gbncd: '010' })" />
                        <button class="btn btn-outline-secondary px-2" @click="openHelp('CUST', (d) => { vatForm.custcd = d.custcd; vatForm.custnm = d.custnm }, { gbncd: '010' })"><i class="bi bi-search"></i></button>
                      </div>
                    </td>
                  </tr>
                  <tr>
                    <th class="bg-light text-center required text-danger fw-bold">매입유형</th>
                    <td>
                      <select v-model="vatForm.vattype" class="form-select form-select-sm">
                        <option value="000">000. 해당없음</option>
                        <option v-for="opt in vatTypeOptions" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                      </select>
                    </td>
                    <th class="bg-light text-center required text-danger fw-bold">부가세액</th>
                    <td>
                      <input v-model.number="vatForm.vatamt" type="number" class="form-control form-control-sm text-end fw-bold text-danger" @input="calculateTotalPayment" />
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- 지불 정보 그리드 (대변 거래처: 은행/지불처) -->
          <div class="card border shadow-sm flex-grow-1 d-flex flex-column overflow-hidden">
            <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
              <div class="d-flex align-items-center gap-3">
                <span class="fw-bold small text-dark"><i class="bi bi-credit-card-2-back me-2 text-primary"></i>지불 정보 (대변)</span>
                <div class="d-flex align-items-center gap-1">
                  <span class="small fw-bold">발행일자:</span>
                  <input v-model="payInfo.payymd" type="date" class="form-control form-control-sm" style="width: 130px;" />
                </div>
              </div>
              <div class="d-flex gap-1">
                <button class="btn btn-sm btn-outline-primary py-0 px-2 fw-bold" @click="addPayRow" style="font-size: 11px;">+ 행추가</button>
                <button class="btn btn-sm btn-outline-danger py-0 px-2 fw-bold" @click="deleteSelectedPayRows" style="font-size: 11px;">- 행삭제</button>
              </div>
            </div>
            <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
              <div ref="payGridRef" class="tabulator-instance flex-grow-1"></div>
            </div>
            <div class="card-footer bg-white border-top p-2 d-flex justify-content-between align-items-center px-3">
              <span class="small text-danger fw-bold">* 지불대상금액(대상+부가세)과 지불합계가 일치해야 합니다.</span>
              <div class="d-flex align-items-center gap-2">
                <span class="fw-bold small">지불 합계:</span>
                <span class="text-danger fw-bolder fs-5">{{ formatNumber(totalPayAmt) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch, nextTick } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import DateForm from '@/components/DateForm.vue'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { useCommonHelp } from '@/composables/useCommonHelp'
import { getDate } from '@/composables/useDate'

const authStore = useAuthStore()
const { firstDay, today } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps, openHelp } = useCommonHelp()


// 폼 데이터
const searchForm = reactive({
  deptcd: authStore.deptcd, deptnm: authStore.deptnm,
  fromdt: firstDay,
  todt: today
})

const vatForm = reactive({ taxunit: '100', custcd: '', custnm: '', vattype: '010', vatamt: 0 })
const payInfo = reactive({ deptcd: authStore.deptcd, deptnm: authStore.deptnm, payymd: today })

const taxUnitOptions = ref<any[]>([]); const vatTypeOptions = ref<any[]>([])
const mainGridRef = ref<HTMLDivElement | null>(null); let mainGrid: Tabulator | null = null
const payGridRef = ref<HTMLDivElement | null>(null); let payGrid: Tabulator | null = null

const targetTotalAmt = ref(0); const totalPayAmt = ref(0)

// 🚀 데이터 조회
const fetchUnissuedList = async () => {
  try {
    const params = { 
    actkind: 'S0', 
    cmpycd: authStore.cmpycd, 
    deptcd: searchForm.deptcd, 
    fromdt: searchForm.fromdt.replace(/-/g, ''), 
    todt: searchForm.todt.replace(/-/g, '') }
    const res = await api.post('/hsip/HSIP_140U_STR', params)
    mainGrid?.setData(res.data || [])
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

const calculateTotalPayment = () => {
  const data = payGrid?.getData() || [];
  totalPayAmt.value = data.reduce((acc, cur: any) => acc + (Number(cur.payamt) || 0), 0)
}

// 🚀 전표 발행
const handleGenerateSlip = async () => {
  const selectedCosts = mainGrid?.getSelectedData() || []
  if (!selectedCosts.length) return vAlertError('발행 대상을 선택하세요.')
  const selectedPayments = payGrid?.getData().filter((p: any) => Number(p.payamt) > 0) || []
  if (!selectedPayments.length) return vAlertError('지불 정보를 입력하세요.')

  const targetWithVat = targetTotalAmt.value + Number(vatForm.vatamt)
  if (Math.abs(targetWithVat - totalPayAmt.value) > 1) return vAlertError('지불대상금액과 지불합계가 일치하지 않습니다.')

  if (!confirm('전표를 발행하시겠습니까?')) return

  const payload = {
    mst: {
      payymd: payInfo.payymd,
      deptcd: payInfo.deptcd,
      cmpycd: authStore.cmpycd,
      usernm: authStore.usernm,
      fromdt: searchForm.fromdt,
      todt: searchForm.todt,
      search_deptcd: searchForm.deptcd,
      targetTotalAmt: targetTotalAmt.value
    },
    vat: {
      taxunit: vatForm.taxunit,
      custcd: vatForm.custcd,
      vattype: vatForm.vattype,
      vatamt: vatForm.vatamt
    },
    costs: selectedCosts.map(item => ({
      fileno: item.fileno,
      docno: item.docno,
      crowno: item.crowno,
      costamt: item.costamt,
      costnm: item.costnm,
      bigo: item.bigo,
      deptcd: item.deptcd
    })),
    payments: selectedPayments.map(p => ({
      acctcd: p.acctcd,
      payamt: p.payamt,
      bankcd: p.bankcd,
      custcd: p.custcd,
      mgtno: p.mgtno,
      remark: p.remark
    }))
  }

  try {
    const res = await api.post('/hsip/HSIP_140U_SAVE', payload)
    if (res.data?.[0]?.result === 'N') return vAlertError(res.data[0].msg || '발행 실패');
    if (res.data) {
      vAlert('전표가 발행되었습니다.')
      fetchUnissuedList()
      initialize()
    }
  } catch (e: any) {
    vAlertError('발행 실패: ' + (e.response?.data?.message || e.message))
  }
}

const initialize = () => {
  resetForm(searchForm);
  searchForm.fromdt = firstDay;
  searchForm.todt = today;
  searchForm.deptcd = authStore.deptcd;
  searchForm.deptnm = authStore.deptnm;
  Object.assign(vatForm, { taxunit: '100', custcd: '', custnm: '', vattype: '010', vatamt: 0 });
  Object.assign(payInfo, { deptcd: authStore.deptcd, deptnm: authStore.deptnm, payymd: today });
  mainGrid?.clearData(); targetTotalAmt.value = 0; totalPayAmt.value = 0; payGrid?.setData([]);
}

const formatNumber = (val: any) => Number(val || 0).toLocaleString()

/**
 * 🚀 도움말 호출 통합 관리
 */
const handleOpenHelp = (type: string, row: any) => {
  const d = row.getData();
  if (type === 'PAY_ACCT') {
    openHelp('ACCT', (res) => {
      const n = res;
      row.update({
          acctcd: n.acctcd, acctnm: n.acctnm || n.codenm,
          typemgt: n.typemgt, typesub: n.typesub,
          remark: payInfo.deptnm + ' ' + (n.acctnm || n.codenm), mgtno: '', bankcd: '', custcd: '', custnm: ''
      });
    }, { gubun: 'e0', gbncd: '300' });
  } else if (type === 'PAY_MGT') {
    if (!String(d.typemgt || '').trim() || d.typemgt === '000') return vAlertError('관리번호 대상 계정이 아닙니다.');
    openHelp('MGT', (res) => {
        const n = res;
        // ✅ 지시사항: 계좌번호(mgtno)와 은행코드(bankcd)를 명확히 분리하여 저장
        row.update({
            mgtno: n.mgtno || '',    // 계좌번호 보존
            bankcd: n.bankcd || '',   // 은행코드 보존 (상식적인 대변 거래처용)
            custnm: n.mgtnm || ''    // 은행명 (표시용)
        });
    }, { mgtgbn: d.typemgt, acctcd: d.acctcd });
  } else if (type === 'PAY_SMART') {
    if ((!String(d.typesub || '').trim() || d.typesub === '000') && (d.typemgt && d.typemgt !== '000')) {
        return handleOpenHelp('PAY_MGT', row);
    }
    if (!String(d.typesub || '').trim() || d.typesub === '000') return vAlertError('거래처/관리번호 대상 계정이 아닙니다.');
    openHelp('CUST', (res) => {
        const n = res;
        // ✅ 일반 거래처의 경우 custcd와 custnm 저장
        row.update({ custcd: n.custcd, custnm: n.custnm });
    }, { gbncd: d.typesub });
  }
}

const addPayRow = () => {
  payGrid?.addRow({ payamt: 0 }, true).then(row => {
    const remain = (targetTotalAmt.value + Number(vatForm.vatamt)) - totalPayAmt.value;
    if (remain > 0) row.update({ payamt: remain });
    calculateTotalPayment();
    row.select();
  });
}

const deleteSelectedPayRows = () => {
  const sel = payGrid?.getSelectedRows();
  if (sel?.length) { sel.forEach(row => row.delete()); calculateTotalPayment(); }
  else vAlertError('삭제할 행을 선택하세요.');
}

const initGrids = () => {
  if (mainGridRef.value) {
    mainGrid = new Tabulator(mainGridRef.value, {
      layout: 'fitColumns', height: '100%', selectable: true,
      columnDefaults: { headerSort: false, headerHozAlign: "center", hozAlign: "center" },
      placeholder: "데이터가 없습니다.",
      columns: [
        { title: "선택", formatter: "rowSelection", titleFormatter: "rowSelection", width: 40 },
        { title: "PO No.", field: "fileno", width: 120, cssClass: "fw-bold text-primary" },
        { title: "비용종류", field: "costnm", width: 150, hozAlign: "left" },
        { title: "발생일", field: "pubymd", width: 120, formatter: (c) => {
            const v = c.getValue() || ''; return v.length === 8 ? `${v.substring(2,4)}.${v.substring(4,6)}.${v.substring(6,8)}` : v;
        }},
        { title: "비 용", field: "costamt", hozAlign: "right", width: 120, formatter: "money", formatterParams: { precision: 0 } }
      ]
    });
    mainGrid.on('rowSelectionChanged', (data) => {
      targetTotalAmt.value = data.reduce((acc, cur: any) => acc + (Number(cur.costamt) || 0), 0);
    });
  }

  if (payGridRef.value) {
    payGrid = new Tabulator(payGridRef.value, {
      layout: 'fitColumns', height: '100%', selectable: true,
      columnDefaults: { headerSort: false, headerHozAlign: "center", hozAlign: "center" },
      placeholder: "지불 정보를 추가하세요.",
      columns: [
        { title: "선택", formatter: "rowSelection", titleFormatter: "rowSelection", width: 40 },
        { title: "계정과목", field: "acctnm", width: 150, cssClass: "text-primary fw-bold cursor-pointer", cellClick: (e, cell) => handleOpenHelp('PAY_ACCT', cell.getRow()) },
        { title: "계좌/관리번호", field: "mgtno", width: 130, cssClass: "cursor-pointer text-center", cellClick: (e, cell) => handleOpenHelp('PAY_MGT', cell.getRow()) },
        { title: "코드", field: "bankcd", width: 80, hozAlign: "center", formatter: (c) => c.getValue() || c.getData().custcd || '' },
        { title: "거래처/은행", field: "custnm", width: 160, cssClass: "cursor-pointer", cellClick: (e, cell) => handleOpenHelp('PAY_SMART', cell.getRow()) },
        { title: "적 요", field: "remark", editor: "input", hozAlign: "left", widthGrow: 1 },
        { title: "금 액", field: "payamt", width: 120, hozAlign: "right", editor: "number", formatter: "money", formatterParams: { precision: 0 } }
      ]
    });
    payGrid.on('cellEdited', calculateTotalPayment);
    payGrid.on('dataChanged', calculateTotalPayment);
  }
}

onMounted(async () => {
  nextTick(initGrids);
  try {
    const resTax = await api.post('/ha00/HA00_00P_STR', { gubun: 'SA', cmpycd: authStore.cmpycd });
    taxUnitOptions.value = resTax.data;
    const resVat = await api.post('/ha00/HA00_00P_STR', { gubun: 'E0', gbncd: '120', cmpycd: authStore.cmpycd });
    vatTypeOptions.value = resVat.data;
  } catch (e) {}
  initialize(); fetchUnissuedList();
  setTimeout(() => { mainGrid?.redraw(true); payGrid?.redraw(true); }, 300);
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; border: 1px solid #dee2e6; }
.cursor-pointer { cursor: pointer; }
</style>
