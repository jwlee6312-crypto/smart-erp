<!--영업관리/출고관리/출고확정-->
<!--
	=============================================================
	프로그램명	  : 출고확정
  프로그램 ID	: HESB030U
	작성일자	    : 25.05.14
	작성자	      : 박현목
	수정일자     : 25.10.28
	수정자	      : 이현준
	수정 내용    : 자잘한 수정
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container d-flex flex-column h-100 bg-white">
		<!-- 🚀 1. 상단 액션 바 -->
		<div
			class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom"
		>
			<div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px">
				<i class="bi bi-check2-square me-2 text-primary" style="font-size: 18px"></i>
				영업관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
				출고관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
				<span class="text-primary fw-bolder">출고확정 (HSOD200U)</span>
			</div>
			<div class="btn-group-erp d-flex gap-1 pe-3">
				<button class="btn-erp btn-init" @click="initialize">초기화</button>
				<button class="btn-erp btn-search" @click="search">조회</button>
				<button class="btn-erp btn-save" @click="save">저장</button>
			</div>
		</div>

		<!-- 💡 2. 메인 컨텐츠 영역 -->
		<div
			class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper"
		>
			<!-- [1] 조회 필터 영역 -->
			<div class="card border shadow-sm flex-shrink-0 overflow-hidden">
				<div class="card-body p-0 bg-white">
					<table class="erp-table-dense" width="100%">
						<colgroup>
							<col style="width: 10%" />
							<col style="width: 40%" />
							<col style="width: 10%" />
							<col style="width: 40%" />
						</colgroup>
						<tbody>
							<tr>
								<th class="text-center bg-light">출고일자</th>
								<td class="d-flex align-items-center border-0 gap-1" style="height: 32px">
									<DateForm v-model:fromdt="form_01.fromdt" v-model:todt="form_01.todt" />
								</td>
								<th class="text-center bg-light">확정구분</th>
								<td>
									<select v-model="form_01.slipyn" class="form-select form-select-sm w-25" @change="search">
										<option value="N">미확정</option>
										<option value="Y">확정</option>
									</select>
								</td>
							</tr>
						</tbody>
					</table>
				</div>
			</div>

			<!-- [2] 상세 마스터 정보 폼 -->
			<div class="card border shadow-sm flex-shrink-0 overflow-hidden">
				<div class="card-body p-0 bg-white">
					<table class="erp-table-dense w-100">
						<colgroup>
							<col style="width: 100px" />
							<col />
							<col style="width: 100px" />
							<col />
							<col style="width: 100px" />
							<col />
						</colgroup>
						<tbody>
							<tr>
								<th class="bg-light">출고부서</th>
								<td>
									<input
										v-model="form_02.deptnm"
										class="form-control form-control-sm bg-light"
										readonly
									/>
								</td>
								<th class="bg-light text-center">출고번호</th>
								<td>
                                    <input
                                        :value="ioymno"
                                        class="form-control form-control-sm bg-light fw-bold text-primary"
                                        placeholder="조회 시 자동 표시"
                                        readonly
                                    />
								</td>
								<th class="bg-light text-center">출고일자</th>
								<td>
									<input
										v-model="form_02.ioymd"
										class="form-control form-control-sm bg-light"
										readonly
									/>
								</td>
							</tr>
							<tr>
								<th class="bg-light">거래처</th>
								<td>
									<input
										v-model="form_02.custnm"
										class="form-control form-control-sm bg-light"
										readonly
									/>
								</td>
								<th class="bg-light text-center">출고창고</th>
								<td>
									<input
										v-model="form_02.whnm"
										class="form-control form-control-sm bg-light"
										readonly
									/>
								</td>
								<th class="bg-light text-center">출고금액</th>
								<td>
									<input
										:value="formattedSum"
										class="form-control form-control-sm text-end bg-light fw-bold"
										readonly
									/>
								</td>
							</tr>
							<tr>
								<th class="bg-light">특이사항</th>
								<td colspan="3">
									<input v-model="form_02.remark" class="form-control form-control-sm bg-light" readonly />
								</td>
								<th class="bg-light text-center">확정여부</th>
								<td class="text-center">
									<div class="form-check form-switch d-inline-block">
										<input
											v-model="form_02.cfmyn"
											true-value="Y"
											false-value="N"
											class="form-check-input"
											type="checkbox"
											id="cfmyn_switch"
											role="switch"
                                            style="cursor: pointer;"
										>
										<label class="form-check-label fw-bold small ms-1" :class="form_02.cfmyn === 'Y' ? 'text-primary' : 'text-danger'" for="cfmyn_switch" style="cursor: pointer;">
											{{ form_02.cfmyn === 'Y' ? '확정' : '미확정' }}
										</label>
									</div>
								</td>
							</tr>
						</tbody>
					</table>
				</div>
			</div>

			<!-- [3] 하단 투-그리드 레이아웃 영역 -->
			<div class="d-flex gap-2 flex-grow-1 overflow-hidden" style="min-height: 0">
				<!-- ⬅️ 좌측: 출고 목록 (60%) -->
				<div
					class="card border shadow-sm d-flex flex-column overflow-hidden grid-container-left"
					style="width: 60%; min-width: 400px"
				>
					<div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark">
						출고 목록
					</div>
					<div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
						<div ref="tableRef1" class="tabulator-instance flex-grow-1" />
					</div>
				</div>

				<!-- ➡️ 우측: 품목 상세 그리드 (40%) -->
				<div
					class="card border shadow-sm d-flex flex-column overflow-hidden grid-container-right"
					style="width: 40%; min-width: 300px"
				>
					<div
						class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0"
					>
						<span class="fw-bold small text-dark">
							<i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>출고 상세 내역
						</span>
					</div>
					<div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
						<div ref="tableRef2" class="tabulator-instance flex-grow-1" />
					</div>
				</div>
			</div>
		</div>
	</div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, nextTick } from 'vue'
import { type RowComponent, TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import AppAlert from '@/components/AppAlert.vue'
import DateForm from '@/components/DateForm.vue'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { formatNumber } from '@/composables/useFormatUtils'
import { getDate } from '@/composables/useDate'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { today, firstDay } = getDate()

// Grid Refs
const tableRef1 = ref<HTMLDivElement | null>(null)
const tableRef2 = ref<HTMLDivElement | null>(null)
let grid1: Tabulator | null = null
let grid2: Tabulator | null = null

const form_01 = reactive({
	fromdt: firstDay,
	todt: today,
	slipyn: 'N',
})

const form_02 = reactive<any>({
	actkind: '', cmpycd: authStore.cmpycd, iogbn: '200', ioym: '', iono: '',
    iotype: '', ioymd: '', deptcd: '', deptnm: '',
	whcd: '000', whnm: '', custcd: '', custnm: '', ioamt: 0,
	remark: '', cfmyn: 'N', slipyn: 'N',
    ideptcd: '', ideptnm: '', iwhcd: '', iwhnm: ''
})

const ioymno = computed(() => (form_02.ioym && form_02.iono ? `${form_02.ioym}-${form_02.iono}` : ''))
const formattedSum = computed(() => formatNumber(form_02.ioamt))

const initGrids = () => {
	grid1 = new Tabulator(tableRef1.value!, {
		layout: 'fitColumns',
		height: '100%', placeholder: '데이터 없음',
		columns: [
            { title: "출고번호", field: "ioymno", width: 180, hozAlign: "center", headerHozAlign: "center", mutator: (v,d) => d.ioym && d.iono ? `${d.ioym}-${d.iono}` : "" },
			{ title: '출고일', field: 'ioymd', hozAlign: 'center', headerHozAlign: 'center', width: 100, formatter: (c) => {
                const v = c.getValue(); return v && v.length === 8 ? `${v.substring(0,4)}-${v.substring(4,6)}-${v.substring(6,8)}` : v;
            }},
            { title: '구분', field: 'gubunnm', hozAlign: 'center', headerHozAlign: 'center', width: 90, mutator: (v, d) => {
                if (d.iotype === '100') {
                    if (d.gubun === '1') return Number(d.ioamt) > 0 ? '판매출고' : '매출반품';
                    if (d.gubun === '2') return '주문출고';
                }
                if (d.iotype === '200') return '이관출고';
                if (d.iotype === '300') return '타계정출고';
                return v;
            }},
			{ title: '거래처', field: 'custnm', hozAlign: 'left', headerHozAlign: 'center', widthGrow: 1, formatter: (c) => {
                const d = c.getData();
                if (d.iotype === '200') return `입고 : ${d.ideptnm || ''}-${d.iwhnm || ''}`;
                if (d.iotype === '300') return d.remark ? d.remark.substring(0, 20) : '';
                return c.getValue();
            }},
			{ title: '출고금액', field: 'ioamt', hozAlign: 'right', headerHozAlign: 'center', width: 120, formatter: 'money', formatterParams: { precision: 0 } },
			{ title: '출고창고', field: 'whnm', hozAlign: 'center', headerHozAlign: 'center', width: 120 },
			{ title: '확정', field: 'cfmyn', hozAlign: 'center', headerHozAlign: 'center', width: 80, formatter: (c) => (c.getValue() === 'Y' ? '확정' : '미확정') },
		],
	})
	grid1.on('rowClick', (e, row) => fetchDetail(row.getData()))

	grid2 = new Tabulator(tableRef2.value!, {
		layout: 'fitColumns', height: '100%', placeholder: '데이터 없음',
		columns: [
			{ title: '품목명', field: 'itemnm', hozAlign: 'left', headerHozAlign: 'center', widthGrow: 1 },
			{ title: '규격', field: 'itsize', width: 100, hozAlign: 'left', headerHozAlign: 'center' },
			{ title: '단 단위', field: 'unit', width: 60, hozAlign: 'center', headerHozAlign: 'center' },
			{ title: '수량', field: 'ioqty', width: 80, hozAlign: 'right', headerHozAlign: 'center', formatter: 'money', formatterParams: { precision: 0 } },
			{ title: '금액', field: 'ioamt', width: 100, hozAlign: 'right', headerHozAlign: 'center', formatter: 'money', formatterParams: { precision: 0 } },
			{ title: '합계', field: 'totamt', width: 110, hozAlign: 'right', headerHozAlign: 'center', formatter: 'money', formatterParams: { precision: 0 } },
		],
	})
}

async function search() {
	try {
		const res = await api.post('/hsod/HSOD_200U_STR', {
            ...form_01,
            actkind: 'S0',
            cmpycd: authStore.cmpycd,
            fromdt: form_01.fromdt.replace(/-/g, ''),
            todt: form_01.todt.replace(/-/g, ''),
            iogbn: '200',
            whcd: '000',
            slipyn: form_01.slipyn === 'Y' ? 'Y' : 'N',
            updemp: authStore.userid
        });
		grid1?.setData(res.data || []);
		vAlert('조회되었습니다.');
	} catch (e) { vAlertError('조회 실패'); }
}

async function fetchDetail(rowData: any) {
    Object.assign(form_02, rowData);
    if (rowData.ioymd) form_02.ioymd = rowData.ioymd.replace(/(\d{4})(\d{2})(\d{2})/, '$1-$2-$3');

	try {
		const res = await api.post('/hsod/HSOD_200U_STR', {
            actkind: 'S1',
            cmpycd: authStore.cmpycd,
            iogbn: '200',
            ioym: rowData.ioym,
            iono: rowData.iono,
            iotype: rowData.iotype,
            whcd: rowData.whcd,
            whnm: rowData.whnm,
            custcd: rowData.custcd,
            ioamt: rowData.ioamt,
            remark: rowData.remark,
            userid: authStore.userid
        });
		grid2?.setData(res.data || []);
	} catch (e) { vAlertError('상세 조회 실패'); }
}

async function save() {
    if (!form_02.iono) return vAlertError('대상 항목을 선택하세요.');

    // 조회조건(slipyn)이 'Y'였다면 이미 처리된 건의 수정(U0), 'N'이었다면 신규 확정 처리(A0)
    const act = form_01.slipyn === 'Y' ? 'U0' : 'A0';

	try {
		await api.post('/hsod/HSOD_200U_STR', {
            ...form_02,
            actkind: act,
            cmpycd: authStore.cmpycd,
            fromdt: form_02.ioymd.replace(/-/g, ''),
            cfmyn: form_02.cfmyn === 'Y' ? 'Y' : 'N',
            slipyn: form_01.slipyn === 'Y' ? 'Y' : 'N',
            whcd: '000',
            updemp: authStore.userid
        });

        if (form_02.cfmyn === 'N') {
            vAlert('확정이 취소되었습니다.');
        } else {
            vAlert(act === 'U0' ? '수정되었습니다.' : '출고확정 처리되었습니다.');
        }

		await search();
		initialize();
	} catch (e) { vAlertError('저장 처리 실패'); }
}

function initialize() {
	form_01.slipyn = 'N';
	resetForm(form_02);
	Object.assign(form_02, { cmpycd: authStore.cmpycd, ioymd: today, cfmyn: 'Y', iogbn: '200' });
	grid2?.clearData();
}

onMounted(() => {
	nextTick(initGrids);
});
</script>
