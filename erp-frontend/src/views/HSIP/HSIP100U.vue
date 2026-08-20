<!--
	=============================================================
	프로그램명	: 수입발주작업 (HSIP_100U)
	작성일자	: 2025.02.24
	설명        : 수입 발주 마스터/상세 관리 (HSOD100U 표준 구조 및 소문자 원칙 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-globe-americas me-2 text-primary" style="font-size: 18px;"></i>
        수입관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">수입발주작업 (HSIP_100U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-2">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-save" @click="save">저장</button>
        <button class="btn-erp btn-delete" @click="handleFullDelete" :disabled="!formData.fileno || formData.fileno === '0000'">전체삭제</button>
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
                <th class="text-center bg-light">발주일자</th>
                <td class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                  <DateForm v-model:fromdt="searchForm.fromdt" v-model:todt="searchForm.todt" />
                </td>
                <th class="text-center bg-light">거래처명</th>
                <td>
                  <input v-model="searchForm.custnm" class="form-control form-control-sm" placeholder="거래처명 검색" @keyup.enter="search" />
                </td>
            </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 투-그리드 레이아웃 영역 -->
      <div class="d-flex gap-2 flex-grow-1 overflow-hidden" style="min-height: 0;">

        <!-- ⬅️ 좌측: 발주 목록 -->
        <div class="card border shadow-sm d-flex flex-column overflow-hidden grid-container-left" style="width: 350px; min-width: 350px;">
          <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark">발주 목록</div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="tableRef1" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>

        <!-- ➡️ 우측: 마스터 상세 폼 + 품목 상세 그리드 -->
        <div class="flex-grow-1 d-flex flex-column gap-2 overflow-hidden">

          <!-- 상세 마스터 정보 폼 -->
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
                    <th class="required bg-light">발주부서</th>
                    <td>
                      <div class="input-group input-group-sm">
                        <input v-model="formData.deptnm" class="form-control" readonly />
                        <button class="btn btn-outline-secondary" @click="handleOpenHelp('DEPT')"><i class="bi bi-search"></i></button>
                      </div>
                    </td>
                    <th class="bg-light text-center">PO No</th>
                    <td><input v-model="formData.fileno" class="form-control bg-light text-primary fw-bold text-center" placeholder="PO 번호 입력" /></td>
                    <th class="required bg-light text-center">발주일자</th>
                    <td><input v-model="formData.issymd" type="date" class="form-control" /></td>
                    <th class="required bg-light text-center">수입구분</th>
                    <td>
                      <select v-model="formData.imptgbn" class="form-select">
                        <option value="">선택</option>
                        <option v-for="opt in comboData.imptgbn" :key="opt.code" :value="opt.code">{{ opt.cdnm }}</option>
                      </select>
                    </td>
                  </tr>
                  <tr>
                    <th class="required bg-light text-center">거&nbsp;&nbsp;래&nbsp;&nbsp;처</th>
                    <td>
                      <div class="input-group input-group-sm">
                        <input v-model="formData.custnm" class="form-control" readonly />
                        <button class="btn btn-outline-secondary" @click="handleOpenHelp('CUST')"><i class="bi bi-search"></i></button>
                      </div>
                    </td>
                    <th class="required bg-light text-center">원&nbsp;&nbsp;산&nbsp;&nbsp;지</th>
                    <td>
                      <select v-model="formData.nacd" class="form-select">
                        <option value="">선택</option>
                        <option v-for="opt in comboData.nacd" :key="opt.code" :value="opt.code">{{ opt.cdnm }}</option>
                      </select>
                    </td>
                    <th class="required bg-light text-center">선&nbsp;&nbsp;적&nbsp;&nbsp;항</th>
                    <td>
                      <select v-model="formData.shipport" class="form-select">
                        <option value="">선택</option>
                        <option v-for="opt in comboData.shipport" :key="opt.code" :value="opt.code">{{ opt.cdnm }}</option>
                      </select>
                    </td>
                    <th class="required bg-light text-center">도&nbsp;&nbsp;착&nbsp;&nbsp;항</th>
                    <td>
                      <select v-model="formData.arvport" class="form-select">
                        <option value="">선택</option>
                        <option v-for="opt in comboData.arvport" :key="opt.code" :value="opt.code">{{ opt.cdnm }}</option>
                      </select>
                    </td>
                  </tr>
                  <tr>
                    <th class="required bg-light text-center">결제조건</th>
                    <td>
                      <select v-model="formData.paycond" class="form-select">
                        <option value="">선택</option>
                        <option v-for="opt in comboData.paycond" :key="opt.code" :value="opt.code">{{ opt.cdnm }}</option>
                      </select>
                    </td>
                    <th class="required bg-light text-center">가격조건</th>
                    <td>
                      <select v-model="formData.pricond" class="form-select">
                        <option value="">선택</option>
                        <option v-for="opt in comboData.pricond" :key="opt.code" :value="opt.code">{{ opt.cdnm }}</option>
                      </select>
                    </td>
                    <th class="required bg-light text-center">통화/환율</th>
                    <td colspan="3">
                      <div class="d-flex gap-1">
                        <select v-model="formData.currcd" class="form-select" style="width: 100px;">
                          <option value="">선택</option>
                          <option v-for="opt in comboData.currcd" :key="opt.code" :value="opt.code">{{ opt.cdnm }}</option>
                        </select>
                        <input v-model="formData.frgnrate" type="number" class="form-control text-end" step="0.01" @input="updateTotals" />
                      </div>
                    </td>
                  </tr>
                  <tr>
                    <th class="bg-light text-center">소요비용</th>
                    <td>
                      <div class="d-flex gap-1 align-items-center">
                        <span class="small  text-nowrap">L/C:</span>
                        <input v-model="formData.lcamt" type="number" class="form-control text-end" @input="updateTotals" />
                        <span class="small  text-nowrap">통관:</span>
                        <input v-model="formData.xtamt" type="number" class="form-control text-end" @input="updateTotals" />
                      </div>
                    </td>
                    <th class="bg-light text-center">B/L 합계</th>
                    <td colspan="3">
                      <div class="d-flex gap-1 align-items-center">
                        <span class="small  text-nowrap">외화:</span>
                        <input :value="formData.frgnamt?.toLocaleString()" class="form-control text-end bg-light" readonly />
                        <span class="small  text-nowrap">원화:</span>
                        <input :value="formData.wonamt?.toLocaleString()" class="form-control text-end bg-light" readonly />
                      </div>
                    </td>
                    <th class="bg-light text-center">총합계</th>
                    <td><input :value="formData.costsum?.toLocaleString()" class="form-control text-end bg-light fw-bold text-primary" readonly /></td>
                  </tr>
                  <tr>
                    <th class="bg-light text-center">특기사항</th>
                    <td colspan="7"><input v-model="formData.bigo" class="form-control" placeholder="비고 입력" /></td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- 상세 품목 그리드 영역 -->
          <div class="card border shadow-sm flex-grow-1 d-flex flex-column overflow-hidden grid-container-right">
            <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
              <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>수입 발주 품목 리스트</span>
              <div class="d-flex gap-1">
                <button class="btn btn-sm btn-outline-primary py-0 px-2 fw-bold" @click="addGridRow" style="font-size: 11px;">+ 행추가</button>
                <button class="btn btn-sm btn-outline-danger py-0 px-2 fw-bold" @click="deleteSelectedRows" style="font-size: 11px;">- 행삭제</button>
              </div>
            </div>
            <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
              <div ref="mainGridRef" class="tabulator-instance flex-grow-1"></div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, nextTick, computed, onUnmounted } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { useCommonHelp } from '@/composables/useCommonHelp'
import { getDate } from '@/composables/useDate'
import { useManualStore } from '@/stores/manualStore'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import DateForm from '@/components/DateForm.vue'
import { useSearchStore } from '@/stores/useSearchStore'
import { useRoute } from 'vue-router'

const authStore = useAuthStore()
const searchStore = useSearchStore()
const route = useRoute()

const { firstDay, today } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps, openHelp } = useCommonHelp()
const manualStore = useManualStore()

// [1] 데이터 모델링 (HSOD100U 기준 소문자 원칙)
const searchForm = reactive({ fromdt: firstDay, todt: today, custnm: '' })
const formData = reactive<any>({
  _isNew: true, // 신규 등록 여부 플래그
  actkind: 'S0', cmpycd: authStore.cmpycd, fileno: '', deptcd: authStore.deptcd, deptnm: authStore.deptnm,
  issymd: today, custcd: '', custnm: '', nacd: '',
  currcd: '', frgnrate: 1350, pricond: '', paycond: '', imptgbn: '',
  shipport: '', arvport: '', bigo: '', wonamt: 0, frgnamt: 0, lcamt: 0, xtamt: 0, costsum: 0
})

const comboData = reactive<any>({ imptgbn: [], nacd: [], shipport: [], arvport: [], currcd: [], paycond: [], pricond: [] })

const tableRef1 = ref<HTMLElement | null>(null)
const mainGridRef = ref<HTMLElement | null>(null)
let grid1: Tabulator | null = null
let mainGrid: Tabulator | null = null

// [2] 그리드 초기화
const initGrids = () => {
  if (!tableRef1.value || !mainGridRef.value) return

  grid1 = new Tabulator(tableRef1.value, {
    data: [], // 🚀 초기 데이터 설정
    layout: "fitColumns", height: "100%", placeholder: "데이터 없음",
    columns: [
      { title: "No", formatter: "rownum", width: 30, hozAlign: "center", headerSort: false },
      { title: "발주일자", field: "issymd", hozAlign: "center", width: 90, formatter: (c) => {
          const v = c.getValue(); return v && v.length === 8 ? `${v.substring(0,4)}-${v.substring(4,6)}-${v.substring(6,8)}` : v;
      }},
      { title: "PO No", field: "fileno", hozAlign: "center", width: 100, cssClass: "fw-bold text-primary", headerSort: false },
      { title: "수입처", field: "custnm", hozAlign: "left", minWidth: 100, cssClass: "fw-bold text-primary", headerSort: false }
    ],
  });
  grid1.on("rowClick", (e, row) => fetchDetail(row.getData().fileno));

  mainGrid = new Tabulator(mainGridRef.value, {
    data: [], // 🚀 초기 데이터 설정
    layout: "fitColumns", height: "100%", placeholder: "발주 품목이 없습니다.", selectable: true,
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "선택", width: 40, hozAlign: "center", formatter: "rowSelection", titleFormatter: "rowSelection" },
      { title: "상태", field: "_status", width: 60, hozAlign: "center", formatter: (c) => {
          const v = c.getValue();
          if (v === '입력') return '<span class="badge bg-primary">신규</span>';
          if (v === '수정') return '<span class="badge bg-warning text-dark">수정</span>';
          if (v === '삭제') return '<span class="badge bg-danger">삭제</span>';
          return '';
      }},
      { title: "품목명", field: "itemnm", minWidth: 200, widthGrow: 1, cssClass: 'fw-bold text-primary',
        cellClick: (e, cell) => handleOpenHelp('ITEM', cell.getRow())
      },
      { title: "규격", field: "itsize", width: 200 },
      { title: "단위", field: "unit", width: 80, hozAlign: "center" },
      { title: "수량", field: "qty", width: 150, hozAlign: "right", editor: "number", cellEdited: (cell) => calcRow(cell.getRow()) },
      { title: "단가", field: "price", width: 150, hozAlign: "right", editor: "number", cellEdited: (cell) => calcRow(cell.getRow()) },
      { title: "금액", field: "amt", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "삭제", width: 50, hozAlign: "center", formatter: () => "<i class='bi bi-trash text-danger'></i>",
        cellClick: (e, cell) => handleRowAction(cell.getRow())
      }
    ]
  });
}

const calcRow = (row: any) => {
  const d = row.getData();
  const amt = Math.floor(Number(d.qty || 0) * Number(d.price || 0));
  row.update({ amt: amt });
  if (d._state === 'EXIST' && d._status !== '삭제') row.update({ _status: '수정' });
  updateTotals();
}

const updateTotals = () => {
    const data = mainGrid?.getData().filter(i => i._status !== '삭제') || []
    const frgnAmt = data.reduce((acc, cur: any) => acc + (Number(cur.amt) || 0), 0)
    formData.frgnamt = frgnAmt
    formData.wonamt = Math.floor(frgnAmt * (Number(formData.frgnrate) || 0))
    formData.costsum = (Number(formData.lcamt) || 0) + (Number(formData.xtamt) || 0) + (Number(formData.wonamt) || 0)
}

const handleOpenHelp = (type: string, target?: any) => {
  if (type === 'DEPT') {
    Object.assign(modalProps, {
      title: '부서 선택',
      path: '/ha00/HA00_00P_STR',
      defaultField: 'deptnm',
      data: { gubun: 'D0', cmpycd: authStore.cmpycd, gbncd: '', code: '', remark: '' },
      columns: [
        { title: '코드', field: 'deptcd', width: 80, hozAlign: 'center' },
        { title: '부서명', field: 'deptnm', width: 200 }
      ],
      onConfirm: (d: any) => { formData.deptcd = d.deptcd; formData.deptnm = d.deptnm }
    })
    modalVisible.value = true
  } else if (type === 'CUST') {
    Object.assign(modalProps, {
      title: '거래처 선택',
      path: '/ha00/HA00_00P_STR',
      defaultField: 'custnm',
      data: { gubun: 'C7', cmpycd: authStore.cmpycd, gbncd: '100', code: '', remark: '' },
      columns: [
        { title: '코드', field: 'custcd', width: 80, hozAlign: 'center' },
        { title: '거래처명', field: 'custnm', width: 200 }
      ],
      onConfirm: (d: any) => {
          formData.custcd = d.custcd;
          formData.custnm = d.custnm,
          formData.nacd = d.nacd,
          formData.shipport = d.shipport,
          formData.arvport = d.arvport,
          formData.currcd = d.currcd,
          formData.paycond = d.paycond,
          formData.pricond = d.pricond
      }
    })
    modalVisible.value = true
  } else if (type === 'ITEM') {
    Object.assign(modalProps, {
      title: '품목 선택',
      path: '/hs00/HS00_000S_STR',
      defaultField: 'itemnm',
      data: { gubun: 'I1', cmpycd: authStore.cmpycd, gbncd: '3', code: '', codenm: '', etcval: '' },
      columns: [
        { title: '품목코드', field: 'itemcd', width: 100, hozAlign: 'center' },
        { title: '품목명', field: 'itemnm', width: 200 },
        { title: '규격', field: 'itsize', width: 150 },
        { title: '단위', field: 'unit', width: 80, hozAlign: 'center' }
      ],
      onConfirm: (d: any) => {
        target.update({
          itemcd: d.itemcd,
          itemnm: d.itemnm,
          itsize: d.itsize,
          unit: d.unit || 'EA',
          _status: '입력',
          _state: 'NEW'
        })
      }
    })
    modalVisible.value = true
  }
}


const handleRowAction = (row: any) => {
  const d = row.getData();
  if (d._state === 'NEW') row.delete();
  else row.update({ _status: d._status === '삭제' ? '' : '삭제' });
  updateTotals();
}

async function search() {
  try {
    const res = await api.post('/hsip/HSIP_100U_STR', {
        actkind: 'L0',
        fromdt: searchForm.fromdt.replace(/-/g, ''),
        todt: searchForm.todt.replace(/-/g, ''),
        custnm: searchForm.custnm,
        // 🚀 숫자 필드는 0, 나머지는 빈 문자열로 명시적 초기화 (NULL 방지)
        frgnrate: 0, lcamt: 0, wonamt: 0, frgnamt: 0, xtamt: 0,
        fileno: '', deptcd: '', offerno: '', supergbn: '', imptgbn: '',
        issymd: '', custcd: '', currcd: '', nacd: '', shipport: '',
        pricond: '', pritext: '', arvport: '', paycond: '', payterm: '',
        bigo: '', inymd: '', apvyn: '', apvymd: '', apvemp: '', updemp: ''
    });
    const list = res.data?.data || res.data || [];
    grid1?.setData(list); vAlert('조회되었습니다.');
  } catch (e) { vAlertError('조회 실패'); }
}

async function fetchDetail(fileNo: string) {
  try {
    const res = await api.post('/hsip/HSIP_100U_STR', {
        fileno: fileNo,
        actkind: 'S0',
        cmpycd: authStore.cmpycd,
        fromdt: '', todt: '', custnm: '',
        // 🚀 숫자 필드는 0, 나머지는 빈 문자열 (NULL 방지)
        frgnrate: 0, lcamt: 0, wonamt: 0, frgnamt: 0, xtamt: 0,
        deptcd: '', offerno: '', supergbn: '', imptgbn: '',
        issymd: '', custcd: '', currcd: '', nacd: '', shipport: '',
        pricond: '', pritext: '', arvport: '', paycond: '', payterm: '',
        bigo: '', inymd: '', apvyn: '', apvymd: '', apvemp: '', updemp: ''
    });
    if (res.data?.length) {
      // 🚀 전역 axios 인터셉터가 이미 data를 파싱하므로 res.data로 바로 접근
      const master = res.data[0];
      Object.assign(formData, {...master,
        issymd: master.issymd && master.issymd.length === 8
          ? `${master.issymd.substring(0, 4)}-${master.issymd.substring(4, 6)}-${master.issymd.substring(6, 8)}`
          : master.issymd
      });

      formData._isNew = false; // 🚀 기존 자료이므로 '수정' 모드로 변경

      const itemRes = await api.post('/hsip/HSIP_101U_STR', {
          fileno: fileNo, actkind: 'S0', qty: 0, amt: 0
      });
      mainGrid?.setData((itemRes.data || []).map((i: any) => ({ ...i, _state: 'EXIST', _status: '' })));
      updateTotals();
    }
  } catch (e) { vAlertError('상세 로드 실패'); }
}

async function save() {
  if (!formData.custcd) return vAlertError('거래처를 선택하세요.');
  const details = mainGrid?.getData().filter(r => r._status) || [];
  if (!formData.fileno) return vAlertError('PO번호를 입력하세요.');
  if (formData._isNew === true && !details.length) return vAlertError('품목을 추가하세요.');

  if (!confirm('저장하시겠습니까?')) return

  try {
    // 🚀 [SP Spec Match] HSIP_100U_STR 31개 파라미터 순서 및 타입 완벽 준수
    const mstData = {
        actkind: formData._isNew ? 'A0' : 'U0',
        cmpycd: authStore.cmpycd || '',
        fromdt: (searchForm.fromdt || '').replace(/-/g, ''),
        todt: (searchForm.todt || '').replace(/-/g, ''),
        custnm: formData.custnm || '',
        fileno: formData.fileno || '',
        deptcd: formData.deptcd || '',
        offerno: formData.offerno || '',
        supergbn: formData.supergbn || '',
        imptgbn: formData.imptgbn || '',
        issymd: (formData.issymd || '').replace(/-/g, ''),
        custcd: formData.custcd || '',
        currcd: formData.currcd || '',
        frgnrate: Number(formData.frgnrate) || 0, // numeric(8,3)
        nacd: formData.nacd || '',
        shipport: formData.shipport || '',
        pricond: formData.pricond || '',
        pritext: formData.pritext || '',
        arvport: formData.arvport || '',
        paycond: formData.paycond || '',
        payterm: formData.payterm || '',
        lcamt: Number(formData.lcamt) || 0,       // numeric(15,2)
        wonamt: Number(formData.wonamt) || 0,     // numeric(15,0)
        frgnamt: Number(formData.frgnamt) || 0,   // numeric(14,4)
        xtamt: Number(formData.xtamt) || 0,       // numeric(15,2)
        bigo: formData.bigo || '',
        inymd: (formData.inymd || '').replace(/-/g, ''),
        apvyn: formData.apvyn || '',
        apvymd: formData.apvymd || '',
        apvemp: formData.apvemp || '',
        updemp: authStore.userid || ''
    };

    const payload = {
        mst: mstData,
        dtl: details.map(item => ({
            actkind: item._status === '입력' ? 'A0' : (item._status === '삭제' ? 'D0' : 'U0'),
            cmpycd: authStore.cmpycd || '',
            fileno: formData.fileno || '',
            prowno: item.prowno || '', // 🚀 [Rule] 공백 전송 시 SP에서 행 생성 (숫자 금지)
            itemcd: item.itemcd || '',
            itsize: item.itsize || '',
            unit: item.unit || '',
            qty: Number(item.qty) || 0, // numeric(15,2)
            amt: Number(item.amt) || 0, // numeric(15,4)
            updemp: authStore.userid || ''
        }))
    };

    const res = await api.post('/hsip/HSIP_100U_SAVE', payload);

    // 🚀 [Rule] _SAVE 호출 시 인터셉터는 'data' 객체(Map)를 즉시 반환함
    if (res.data) {
        vAlert('저장되었습니다.');
        const resultData = res.data;
        // 키값은 인터셉터에서 소문자화되어 전달됨
        const newFileNo = resultData.fileno || mstData.fileno;
        search();
        fetchDetail(newFileNo);
    } else {
        throw new Error('저장 후 응답 데이터가 없습니다.');
    }
  } catch (e: any) {
    vAlertError(e.message || '저장 실패');
  }
}

function initialize() {
  resetForm(formData);
  Object.assign(formData, {
      cmpycd: authStore.cmpycd,
      deptcd: authStore.deptcd,
      deptnm: authStore.deptnm,
       _isNew: true,
      fileno: '',
      issymd: today,
      frgnrate: 1350
  });
  if (grid1) grid1.setData([]);
  if (mainGrid) mainGrid.setData([]);
  updateTotals();
}

async function handleFullDelete() {
  if (!confirm('정말 삭제하시겠습니까?')) return;
  try {
    await api.post('/hsip/HSIP_100U_STR', {
        fileno: formData.fileno,
        actkind: 'D0',
        cmpycd: authStore.cmpycd,
        // 🚀 모든 필드 명시적 전송 (NULL 방지)
        frgnrate: 0, lcamt: 0, wonamt: 0, frgnamt: 0, xtamt: 0,
        deptcd: '', offerno: '', supergbn: '', imptgbn: '',
        issymd: '', custcd: '', currcd: '', nacd: '', shipport: '',
        pricond: '', pritext: '', arvport: '', paycond: '', payterm: '',
        bigo: '', inymd: '', apvyn: '', apvymd: '', apvemp: '', updemp: authStore.userid
    });
    vAlert('삭제되었습니다.'); initialize(); search();
  } catch (e) { vAlertError('삭제 실패'); }
}

function addGridRow() { mainGrid?.addRow({ qty: 0, price: 0, amt: 0, _status: '입력', _state: 'NEW' }, true); }
function deleteSelectedRows() { mainGrid?.getSelectedRows().forEach(row => handleRowAction(row)); }

const openManual = () => {
  manualStore.open('HSIP_100U')
}

onMounted(async () => {
    nextTick(initGrids);

    // 공통 콤보 로드 로직 (수입관리 표준 HS00_000S_STR 사용)
    const loadCombo = async (gbncd: string, target: string) => {
        try {
            const res = await api.post('/hs00/HS00_000S_STR', {
                gubun: 'E2', cmpycd: authStore.cmpycd, gbncd: gbncd, code: '', codenm: '', etcval: ''
            });
            const list = res.data?.data || res.data || [];
            comboData[target] = list.map((i: any) => ({
                code: i.code || i.code || i.codecd || i.codecd || '',
                cdnm: i.codenm || i.codenm || i.cdnm || i.CDNM || ''
            }));
        } catch (e) { console.error(`Combo load error (${target}):`, e); }
    };

    await Promise.all([
        loadCombo('311', 'imptgbn'),
        loadCombo('305', 'nacd'),
        loadCombo('308', 'shipport'),
        loadCombo('309', 'arvport'),
        loadCombo('310', 'currcd'),
        loadCombo('312', 'paycond'),
        loadCombo('314', 'pricond')
    ]);

    initialize();
})

onUnmounted(() => { searchStore.removeTab(route.name as string) });
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
