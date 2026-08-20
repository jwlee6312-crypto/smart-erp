<!--
	=============================================================
	프로그램명	: 품목별 월별 매출 현황 (HSST520S)
	작성일자	: 2025.02.24
	설명        : 품목별/월별(1월~12월) 매출 수량 및 금액 추이 현황 (HSOD100U 디자인 표준 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-calendar3 me-2 text-primary" style="font-size: 18px;"></i>
        매출분석 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        통계현황 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">품목별 월별 매출 현황 (HSST520S)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-excel" @click="excel">엑셀</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">
      <!-- 🅰️ 조회 조건 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
                <col style="width: 10%" /><col style="width: 25%" />
                <col style="width: 10%" /><col style="width: 20%" />
                <col style="width: 10%" /><col style="width: 25%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="required text-center bg-light">판매부서</th>
                <td>
                  <div class="input-group input-group-sm">
                    <input v-model="searchForm.deptcd" class="form-control fw-bold text-primary" readonly />
                    <input v-model="searchForm.deptnm" class="form-control" placeholder="부서 선택" />
                    <button class="btn btn-outline-secondary px-2" @click="openHelp('DEPT')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="required text-center bg-light">조회연월</th>
                <td>
                  <div class="d-flex align-items-center gap-1">
                    <select v-model="searchForm.yy" class="form-select form-select-sm" style="width: 100px;">
                      <option v-for="y in yearOptions" :key="y" :value="y">{{ y }}년</option>
                    </select>
                    <select v-model="searchForm.mm" class="form-select form-select-sm" style="width: 80px;">
                      <option v-for="m in monthOptions" :key="m" :value="m">{{ m }}월</option>
                    </select>
                    <span class="ms-1 small text-muted fw-bold">까지</span>
                  </div>
                </td>
                <td colspan="2" class="text-end pe-3 bg-white">
                    <span v-if="closingInfo.sclsym" class="badge bg-primary px-2">마감: {{ formatYM(closingInfo.sclsym) }}</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 🅱️ 데이터 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>품목별/월별 매출 상세 내역</span>
          <div class="small text-muted">※ 상단 행: 수량(WGT), 하단 행: 금액(AMT)</div>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="mainGridRef" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import { useCommonHelp } from '@/composables/useCommonHelp'
import { getDate } from '@/composables/useDate'

const authStore = useAuthStore()
const { today } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps } = useCommonHelp()

const currentYear = new Date().getFullYear();
const yearOptions = Array.from({ length: 6 }, (_, i) => String(currentYear - i));

const searchForm = reactive<any>({
	deptcd: authStore.deptcd,
	deptnm: authStore.deptnm,
	yy: String(currentYear),
    mm: today.substring(5, 7)
})

const monthOptions = ['01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12']
const closingInfo = reactive({ sclsym: '' })
const mainGridRef = ref<HTMLDivElement | null>(null);
let mainGrid: Tabulator | null = null

const search = async () => {
    if (!searchForm.deptcd) return vAlertError('판매부서를 선택해 주십시오.')
	try {
		const res = await api.post('/hsst/HSST_520S_STR', {
			cmpycd: authStore.cmpycd,
            deptcd: searchForm.deptcd,
            yymm: searchForm.yy + searchForm.mm
		})

        // 💡 SQL 알리아스 기반 데이터 가공 (수량/금액 2행 1세트 변환)
		const rawData = (res.data || []).map((i: any) => Object.fromEntries(Object.entries(i).map(([k, v]) => [k.toLowerCase(), v])));
        const processedData: any[] = [];

        rawData.forEach((item: any) => {
            // 1. 수량 행 (WGT)
            const wgtRow: any = { ...item, gubun: '수량', total: 0 };
            for(let i=1; i<=12; i++) {
                const key = String(i).padStart(2, '0');
                const val = Number(item[`wgt${key}`] || 0);
                wgtRow[`m${key}`] = val;
                wgtRow.total += val;
            }
            processedData.push(wgtRow);

            // 2. 금액 행 (AMT)
            const amtRow: any = { ...item, gubun: '금액', total: 0 };
            for(let i=1; i<=12; i++) {
                const key = String(i).padStart(2, '0');
                const val = Number(item[`amt${key}`] || 0);
                amtRow[`m${key}`] = val;
                amtRow.total += val;
            }
            processedData.push(amtRow);
        });

		mainGrid?.setData(processedData)
		vAlert('조회되었습니다.')
	} catch (e) { vAlertError('조회 실패') }
}

const initialize = () => {
	resetForm(searchForm);
	searchForm.deptcd = authStore.deptcd; searchForm.deptnm = authStore.deptnm;
	searchForm.yy = String(currentYear);
	mainGrid?.clearData();
}

const excel = () => mainGrid?.download("xlsx", `품목별월별매출현황_${searchForm.yy}.xlsx`)

function openHelp(type: string) {
	if (type === 'DEPT') {
		Object.assign(modalProps, {
			title: '부서 선택', path: '/ha00/HA00_00P_STR',
			data: { gubun: 'D0', cmpycd: authStore.cmpycd },
			columns: [{ title: '코드', field: 'deptcd', width: 80 }, { title: '부서명', field: 'deptnm', width: 180 }],
			onConfirm: (d: any) => { searchForm.deptcd = d.deptcd; searchForm.deptnm = d.deptnm }
		})
	}
	modalVisible.value = true
}

const fetchClosingInfo = async () => {
  try {
    const res = await api.post('/hs00/HS00_000S_STR', { gubun: 'CL', cmpycd: authStore.cmpycd });
    if (res.data?.length) closingInfo.sclsym = res.data[0].sclsym;
  } catch (e) {}
}

const formatYM = (v: string) => v ? `${v.substring(0, 4)}-${v.substring(4, 6)}` : '';

onMounted(() => {
    fetchClosingInfo();
	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns', height: '100%',
			columnDefaults: { headerSort: false, headerHozAlign: "center", hozAlign: "center", vertAlign: "middle", minWidth: 80 },
			columns: [
				{
					title: "품목 정보", field: "itemnm", minWidth: 220, widthGrow: 2, hozAlign: "left", cssClass: "fw-bold border-end", frozen: true,
					formatter: (cell) => {
						const d = cell.getData();
						return d.gubun === '수량' ? `[${d.itemcd}] ${d.itemnm} <br><small class='text-muted'>${d.itsize || ''} (${d.unit || ''})</small>` : '';
					}
				},
				{ title: "구분", field: "gubun", width: 70, frozen: true, cssClass: "border-end" },
				{ title: "합계", field: "total", hozAlign: "right", width: 110, formatter: "money",
                  formatterParams: { precision: (c:any) => c.getData().gubun === '수량' ? (Number(c.getData().qtypnt)||0) : 0 },
                  cssClass: "bg-light-yellow fw-bold"
                },
				{ title: "01월", field: "m01", hozAlign: "right", width: 90, formatter: "money", formatterParams: { precision: (c:any) => c.getData().gubun === '수량' ? (Number(c.getData().qtypnt)||0) : 0 } },
				{ title: "02월", field: "m02", hozAlign: "right", width: 90, formatter: "money", formatterParams: { precision: (c:any) => c.getData().gubun === '수량' ? (Number(c.getData().qtypnt)||0) : 0 } },
				{ title: "03월", field: "m03", hozAlign: "right", width: 90, formatter: "money", formatterParams: { precision: (c:any) => c.getData().gubun === '수량' ? (Number(c.getData().qtypnt)||0) : 0 } },
				{ title: "04월", field: "m04", hozAlign: "right", width: 90, formatter: "money", formatterParams: { precision: (c:any) => c.getData().gubun === '수량' ? (Number(c.getData().qtypnt)||0) : 0 } },
				{ title: "05월", field: "m05", hozAlign: "right", width: 90, formatter: "money", formatterParams: { precision: (c:any) => c.getData().gubun === '수량' ? (Number(c.getData().qtypnt)||0) : 0 } },
				{ title: "06월", field: "m06", hozAlign: "right", width: 90, formatter: "money", formatterParams: { precision: (c:any) => c.getData().gubun === '수량' ? (Number(c.getData().qtypnt)||0) : 0 } },
				{ title: "07월", field: "m07", hozAlign: "right", width: 90, formatter: "money", formatterParams: { precision: (c:any) => c.getData().gubun === '수량' ? (Number(c.getData().qtypnt)||0) : 0 } },
				{ title: "08월", field: "m08", hozAlign: "right", width: 90, formatter: "money", formatterParams: { precision: (c:any) => c.getData().gubun === '수량' ? (Number(c.getData().qtypnt)||0) : 0 } },
				{ title: "09월", field: "m09", hozAlign: "right", width: 90, formatter: "money", formatterParams: { precision: (c:any) => c.getData().gubun === '수량' ? (Number(c.getData().qtypnt)||0) : 0 } },
				{ title: "10월", field: "m10", hozAlign: "right", width: 90, formatter: "money", formatterParams: { precision: (c:any) => c.getData().gubun === '수량' ? (Number(c.getData().qtypnt)||0) : 0 } },
				{ title: "11월", field: "m11", hozAlign: "right", width: 90, formatter: "money", formatterParams: { precision: (c:any) => c.getData().gubun === '수량' ? (Number(c.getData().qtypnt)||0) : 0 } },
				{ title: "12월", field: "m12", hozAlign: "right", width: 90, formatter: "money", formatterParams: { precision: (c:any) => c.getData().gubun === '수량' ? (Number(c.getData().qtypnt)||0) : 0 } }
			]
		})
	}
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; border-bottom: 3px solid #005a9f !important; }

/* 🚀 2단 헤더에서 단일 컬럼의 타이틀을 수직 중앙 정렬 */
:deep(.tabulator-header .tabulator-col:not(.tabulator-col-group) .tabulator-col-content) {
	height: 100% !important;
	display: flex !important;
	align-items: center !important;
	justify-content: center !important;
}
</style>
