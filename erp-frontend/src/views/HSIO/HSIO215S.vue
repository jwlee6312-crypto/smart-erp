<!--
	=============================================================
	프로그램명	: 입고의뢰서 출력 (Goods Receipt Request Print)
	작성일자	: 25.02.24
	작성자	    : AI Assistant
	설명        : [최종완성] HSIO550U(레이아웃) + HSOD100U(스타일/정렬) 표준 적용
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container">
		<!-- 🚀 1. 상단 액션 바 (표준 버튼 배치 및 색상) -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-printer-fill me-2 text-primary" style="font-size: 18px;"></i>
				구매정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				입고관리 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				<span class="text-primary fw-bolder">입고의뢰서 출력</span>
			</div>
			<div class="btn-group-erp d-flex gap-1">
				<button class="btn-erp btn-init" @click="initialize">초기화</button>
				<button class="btn-erp btn-search" @click="fetchCustList">조회</button>
				<button class="btn-erp btn-print" @click="print">인쇄</button>
			</div>
		</div>

		<!-- 🔍 2. 최상단 검색 조건 (ASP 기준 항목 구성) -->
		<div class="p-2 pb-0">
			<div class="card border shadow-sm">
				<div class="card-body p-0">
					<table class="erp-table-full">
						<colgroup>
							<col style="width: 100px;"><col />
							<col style="width: 100px;"><col />
							<col style="width: 100px;"><col />
							<col style="width: 100px;"><col />
						</colgroup>
						<tbody>
							<tr>
								<th>입고창고</th>
								<td>
									<select v-model="searchForm.whcd" class="form-select form-select-sm">
										<option value="000">전체</option>
										<option v-for="opt in whOptions" :key="opt.code" :value="opt.code">{{ opt.cdnm }}</option>
									</select>
								</td>
								<th class="required">입고일자</th>
								<td>
									<div class="d-flex align-items-center gap-1">
										<input v-model="searchForm.fromdt" type="date" class="form-control form-control-sm" />
										<span class="text-muted">~</span>
										<input v-model="searchForm.todt" type="date" class="form-control form-control-sm" />
									</div>
								</td>
								<th>거&nbsp;&nbsp;래&nbsp;&nbsp;처</th>
								<td>
									<div class="input-group input-group-sm">
										<input v-model="searchForm.custcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
										<input v-model="searchForm.custnm" type="text" class="form-control" placeholder="거래처 검색" />
										<button class="btn btn-outline-secondary px-2" @click="openHelp('CUST')"><i class="bi bi-search"></i></button>
									</div>
								</td>
								<th>확정여부</th>
								<td>
									<select v-model="searchForm.slipyn" class="form-select form-select-sm">
										<option value="Y">확정</option>
										<option value="N">미확정</option>
									</select>
								</td>
							</tr>
						</tbody>
					</table>
				</div>
			</div>
		</div>

		<!-- 📊 3. 메인 작업 영역 (좌우 분할 레이아웃) -->
		<div class="d-flex flex-row flex-grow-1 overflow-hidden p-2 gap-2">
			<!-- 🅰️ 좌측: 입고 거래처 및 번호 목록 -->
			<div class="card border shadow-sm d-flex flex-column" style="width: 320px; min-width: 320px;">
				<div class="card-header bg-light py-1 px-3 border-bottom d-flex align-items-center">
					<span class="fw-bold small text-dark"><i class="bi bi-list-check me-1"></i> 입고증 대상 목록</span>
				</div>
                  <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column" style="min-height: 0;">
                    <div ref="poGridRef" class="tabulator-full-height" />
                  </div>
			</div>

			<!-- 🅱️ 우측: 입고 상세 품목 그리드 -->
			<div class="flex-grow-1 d-flex flex-column gap-2 overflow-hidden">
				<div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white">
					<!-- ✅ 그리드 타이틀 bg-white 및 상하 중앙 정렬 -->
					<div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between" style="height: 40px;">
						<span class="fw-bold small text-dark d-flex align-items-center">
							<i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i> 입고의뢰 상세 내역
						</span>
						<span v-if="selectedInfo" class="badge bg-primary-subtle text-primary">번호: {{ selectedInfo }}</span>
					</div>
                      <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column" style="min-height: 0;">
                        <div ref="itemGridRef" class="tabulator-full-height" />
                      </div>
				</div>
			</div>
		</div>

	</div>

	<Modal v-model:visible="modalVisible" :modalProps="modalProps" />
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, onUnmounted } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { getDate } from '@/composables/useDate'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { firstDay, today } = getDate()

const searchForm = reactive<any>({
  whcd: '000',
  fromdt: firstDay,
  todt: today,
  custcd: '', custnm: '',
  slipyn: 'N'
})

const selectedInfo = ref('');
const whOptions = ref<any[]>([]);
const poGridRef = ref<HTMLDivElement | null>(null);
const itemGridRef = ref<HTMLDivElement | null>(null);
let poGrid: Tabulator | null = null;
let itemGrid: Tabulator | null = null;
const activeItemCount = ref(0);

const totalSummary = computed(() => {
  const items = itemGrid?.getData() || []
  return items.reduce((acc, cur: any) => acc + (Number(cur.jsanamt) + Number(cur.jsanvat) || 0), 0)
})

async function fetchCustList() {
  try {
    const res = await api.post('/hsio/HSIO_215S_STR', {
      actkind: 'S1', cmpycd: authStore.cmpycd, iogbn: '100',
      fromdt: searchForm.fromdt.replace(/-/g, ''),
      todt: searchForm.todt.replace(/-/g, ''),
      whcd: searchForm.whcd, custcd: searchForm.custcd, slipyn: searchForm.slipyn
    });
    poGrid?.setData(res.data || []);
    itemGrid?.clearData();
    selectedInfo.value = '';
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

async function fetchDetail(row: any) {
  const d = row.getData();
  selectedInfo.value = `${d.ioym}-${d.iono}`;
  try {
    const res = await api.post('/hsio/HSIO_215S_STR', {
      actkind: 'S0', cmpycd: authStore.cmpycd, iogbn: '100',
      whcd: searchForm.whcd, custcd: d.custcd, ioym: d.ioym, iono: d.iono,
      fromdt: searchForm.fromdt.replace(/-/g, ''),
      todt: searchForm.todt.replace(/-/g, '')
    })
    itemGrid?.setData(res.data || [])
  } catch (e) { vAlertError('상세 내역 조회 실패') }
}

function initialize() {
  resetForm(searchForm);
  searchForm.whcd = '000'; searchForm.slipyn = 'N';
  searchForm.fromdt = firstDay;
  searchForm.todt = today;
  poGrid?.clearData(); itemGrid?.clearData(); selectedInfo.value = '';
}

const print = () => { vAlert('인쇄 기능을 준비 중입니다.') }
const openHelp = (type: string) => {
    if (type === 'CUST') {
        Object.assign(modalProps, {
            title: '거래처 선택',
            path: '/ha00/HA00_00P_STR',
            data: { gubun: 'C4', cmpycd: authStore.cmpycd, code: searchForm.custnm, codenm: '' , remark: '' },
            columns: [
                { title: '코드', field: 'custcd', width: 100, hozAlign: 'center' },
                { title: '거래처명', field: 'custnm', width: 200 }
            ],
            onConfirm: (d: any) => {
                searchForm.custcd = d.custcd;
                searchForm.custnm = d.custnm;
            }
        })
        modalVisible.value = true
    }
}
const formatNumber = (val: any) => Number(val || 0).toLocaleString()

onUnmounted(() => {
  if (poGrid) poGrid.destroy();
  if (itemGrid) itemGrid.destroy();
});

onMounted(async () => {
  api.get('/hs00/HS00_000S_STR', { params: { gubun: 'W0', cmpycd: authStore.cmpycd } })
     .then(r => whOptions.value = r.data.map((i:any)=>({code: i.code || i.whcd, cdnm: i.cdnm || i.whnm})));

  if (poGridRef.value) {
    poGrid = new Tabulator(poGridRef.value, {
      layout: 'fitColumns', height: '100%', selectable: 1,
      columnDefaults: {
        headerSort: false,
        headerHozAlign: "center",
        hozAlign: 'right',
        vertAlign: 'middle',
        minWidth: 80
      },
      columns: [
        { title: '거래처', field: 'custnm', minWidth: 150, hozAlign: 'left', widthGrow: 1, cssClass: 'fw-bold' },
        { title: '입고번호', field: 'iono_disp', width: 120, hozAlign: 'center', mutatorData: (v, d) => `${d.ioym}-${d.iono}` }
      ]
    })
    poGrid.on('rowClick', (e, row) => fetchDetail(row))
  }

  if (itemGridRef.value) {
    itemGrid = new Tabulator(itemGridRef.value, {
      layout: 'fitColumns', height: '100%',
      columnDefaults: {
        headerSort: false,
        headerHozAlign: 'center',
        hozAlign: 'right', // 🚀 기본값 우측 정렬
        vertAlign: 'middle',
        minWidth: 100
      },
      columns: [
        { title: '품목명', field: 'itemnm', minWidth: 200, hozAlign: 'left', widthGrow: 1, cssClass: 'fw-bold' },
        { title: '규격', field: 'itsize', width: 150 },
        { title: '단위', field: 'unit', width: 60, hozAlign: 'center' },
        { title: '수량', field: 'ioqty', hozAlign: 'right', width: 90, formatter: 'money', formatterParams: { precision: 0 } },
        { title: '공급가', field: 'jsanamt', hozAlign: 'right', width: 120, formatter: 'money', formatterParams: { precision: 0 } },
        { title: '부가세', field: 'jsanvat', hozAlign: 'right', width: 110, formatter: 'money', formatterParams: { precision: 0 } },
        { title: '합계', field: 'sum_amt', hozAlign: 'right', width: 130, formatter: 'money', cssClass: 'fw-bold bg-light',
          mutatorData: (v, d) => Number(d.jsanamt || 0) + Number(d.jsanvat || 0) }
      ]
    })
    itemGrid.on('dataLoaded', (data) => activeItemCount.value = data.length)
  }
})

const modalVisible = ref(false); const modalProps = reactive<any>({ title: '', path: '', onConfirm: () => {} })
</script>

<style scoped>
.tabulator-full-height { width: 100% !important; background-color: #fff; border-bottom: 3px solid #005a9f !important; }
</style>
