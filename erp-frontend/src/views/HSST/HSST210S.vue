<!--
	=============================================================
	프로그램명	: 월별 채권 잔액 현황 (HSST210S)
	작성일자	: 2025.02.24
	설명        : 거래처별 최근 6개월 판매/수금 추이 현황 (HSOD100U 디자인 표준 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-calendar-range-fill me-2 text-primary" style="font-size: 18px;"></i>
        매출분석 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        통계현황 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">월별 채권 잔액 현황 (HSST210S)</span>
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
                <th class="text-center bg-light">판매부서</th>
                <td>
                  <div class="input-group input-group-sm">
                    <input v-model="searchForm.deptnm" class="form-control fw-bold text-primary" readonly />
                    <button class="btn btn-outline-secondary" @click="openHelp('DEPT')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="required text-center bg-light">기준일자</th>
                <td>
                  <input v-model="searchForm.ymd" type="date" class="form-control form-control-sm" @change="search" />
                </td>
                <th class="text-center bg-light">거 래 처</th>
                <td>
                  <div class="d-flex align-items-center gap-1">
                    <input v-model="searchForm.custnmfr" class="form-control form-control-sm" placeholder="시작" @keyup.enter="openHelp('CUST_FR')" />
                    <span class="text-muted">~</span>
                    <input v-model="searchForm.custnmto" class="form-control form-control-sm" placeholder="종료" @keyup.enter="openHelp('CUST_TO')" />
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 🅱️ 데이터 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>거래처별 최근 6개월 매출/수금 추이</span>
          <div class="small text-muted">금액 단위: 원 (VAT포함)</div>
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

const searchForm = reactive<any>({
	deptcd: authStore.deptcd, deptnm: authStore.deptnm,
	ymd: today, custcdfr: '', custnmfr: '', custcdto: '', custnmto: ''
})

const mainGridRef = ref<HTMLDivElement | null>(null);
let mainGrid: Tabulator | null = null

// 헤더 날짜 계산 (기준일로부터 역순 6개월)
const getMonthLabels = (baseDate: string) => {
	const date = new Date(baseDate);
	const labels = [];
	for (let i = 5; i >= 0; i--) {
		const d = new Date(date.getFullYear(), date.getMonth() - i, 1);
		labels.push(`${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, '0')}`);
	}
	return labels; // [5달전, 4달전, ..., 현재달]
}

const getColumnsConfig = (baseDate: string) => {
	const months = getMonthLabels(baseDate);
	const columns: any[] = [
        { title: "No", formatter: "rownum", width: 40, hozAlign: "center", frozen: true },
		{ title: "거래처명", field: "custnm", minWidth: 180, hozAlign: "left", cssClass: "fw-bold border-end", frozen: true,
		  formatter: (c: any) => {
			  const d = c.getData();
			  return `<span class="text-secondary small me-1">[${d.custcd}]</span> ${d.custnm}`;
		  }
		},
		{ title: "영업담당", field: "empnm", width: 100, frozen: true },
        { title: "전잔액", field: "bjanamt", width: 120, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "bg-light border-end", frozen: true }
	];

    // 🚀 SALSAMT5/INAMT5 ~ SALSAMT0/INAMT0 매핑
	months.forEach((label, idx) => {
        const dataIdx = 5 - idx; // 5, 4, 3, 2, 1, 0 순서
		columns.push({
			title: label,
			columns: [
				{ title: "매출", field: `salsamt${dataIdx}`, hozAlign: "right", width: 100, formatter: "money", formatterParams: { precision: 0 } },
				{ title: "수금", field: `inamt${dataIdx}`, hozAlign: "right", width: 100, formatter: "money", formatterParams: { precision: 0 } }
			]
		});
	});

	columns.push({ title: "입금요청", field: "reqamt", width: 110, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "bg-light-yellow fw-bold" });
    columns.push({ title: "담당/전화", field: "contact", width: 150, hozAlign: "left",
        formatter: (c:any) => {
            const d = c.getData();
            return d.damdang ? `${d.damdang} / ${d.telno || ''}` : '';
        }
    });

	return columns;
}

const search = async () => {
	if (!searchForm.deptcd) return vAlertError('부서를 선택하세요.');
	try {
		const res = await api.post('/hsst/HSST_210S_STR', {
			...searchForm, cmpycd: authStore.cmpycd, ymd: searchForm.ymd.replace(/-/g, '')
		})

		const data = res.data || []

        mainGrid?.setColumns(getColumnsConfig(searchForm.ymd));
		mainGrid?.setData(data);
		vAlert('조회되었습니다.');
	} catch (e) { vAlertError('조회 실패') }
}

const initialize = () => {
	resetForm(searchForm);
	Object.assign(searchForm, { deptcd: authStore.deptcd, deptnm: authStore.deptnm, ymd: today });
	mainGrid?.clearData();
	mainGrid?.setColumns(getColumnsConfig(today));
}

const excel = () => mainGrid?.download("xlsx", `월별채권잔액현황_${searchForm.ymd}.xlsx`)

function openHelp(type: string) {
	if (type === 'DEPT') {
		Object.assign(modalProps, {
			title: '부서 선택', path: '/ha00/HA00_00P_STR',
			data: { gubun: 'D0', cmpycd: authStore.cmpycd },
			columns: [{ title: '코드', field: 'deptcd', width: 80 }, { title: '부서명', field: 'deptnm', width: 180 }],
			onConfirm: (d: any) => { searchForm.deptcd = d.deptcd; searchForm.deptnm = d.deptnm }
		})
	} else if (type.startsWith('CUST')) {
		Object.assign(modalProps, {
			title: '거래처 선택', path: '/ha00/HA00_00P_STR',
			data: { gubun: 'C0', cmpycd: authStore.cmpycd },
			columns: [{ title: '코드', field: 'custcd', width: 100 }, { title: '거래처명', field: 'custnm', width: 200 }],
			onConfirm: (d: any) => {
				if (type === 'CUST_FR') { searchForm.custcdfr = d.custcd; searchForm.custnmfr = d.custnm }
				else { searchForm.custcdto = d.custcd; searchForm.custnmto = d.custnm }
			}
		})
	}
	modalVisible.value = true
}

onMounted(() => {
	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns', height: '100%',
			columnDefaults: { headerSort: false, headerHozAlign: "center", hozAlign: "center", vertAlign: "middle" },
			columns: getColumnsConfig(searchForm.ymd),
			placeholder: "조회된 데이터가 없습니다."
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
