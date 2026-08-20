<!--
	=============================================================
	프로그램명	: 월별 제품 제조원가명세서 (HFMF214S)
	작성일자	: 2025.02.24
	설명        : 품목별 제조원가계산서 (월별 추이) 조회 (HSOD100U 표준 UI 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <ItemHelpModal :visible="itemHelpVisible" :cmpycd="authStore.cmpycd" astKind="2" @close="itemHelpVisible = false" @confirm="onSelectItem" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-calendar3 me-2 text-primary" style="font-size: 18px;"></i>
        원가관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        현황조회 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">월별 제품 제조원가명세서 (HFMF214S)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-search" @click="handleSearch">조회</button>
        <button class="btn-erp btn-excel" @click="handleExport">엑셀저장</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 조회 필터 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
                <col style="width: 10%" /><col style="width: 15%" />
                <col style="width: 10%" /><col style="width: 15%" />
                <col style="width: 10%" /><col style="width: 40%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light required">조회년도</th>
                <td>
                  <select v-model="searchForm.year" class="form-select form-select-sm">
                    <option v-for="y in yearOptions" :key="y" :value="y">{{ y }}년</option>
                  </select>
                </td>
                <th class="text-center bg-light">재고자산</th>
                <td>
                  <select v-model="searchForm.astkind" class="form-select form-select-sm">
                    <option value="200">제품</option>
                    <option value="210">반제품</option>
                  </select>
                </td>
                <th class="text-center bg-light required">품목선택</th>
                <td>
                  <div class="input-group input-group-sm" style="max-width: 400px;">
                    <input v-model="searchForm.itemcd" class="form-control text-center bg-light" style="max-width: 100px;" placeholder="코드" readonly />
                    <input v-model="searchForm.itemnm" class="form-control bg-light" placeholder="품목 선택" readonly @click="openHelp" />
                    <button class="btn btn-outline-secondary" type="button" @click="openHelp"><i class="bi bi-search"></i></button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column grid-container-right">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark">
            <i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>월별 제조원가 추이 (1월 ~ 12월)
            <span v-if="searchForm.itemnm" class="badge bg-primary-subtle text-primary border border-primary-subtle ms-2">{{ searchForm.itemnm }}</span>
          </span>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="mainGridRef" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import AppAlert from '@/components/AppAlert.vue'
import ItemHelpModal from '@/components/ItemHelpModal.vue'
import { useAuthStore } from '@/stores/authStore'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()

const yearOptions = ref<string[]>([])
const searchForm = reactive({
	year: new Date().getFullYear().toString(),
	astkind: '200',
	itemcd: '',
	itemnm: ''
})

const itemHelpVisible = ref(false)
const mainGridRef = ref<HTMLElement | null>(null)
let mainGrid: Tabulator | null = null

const openHelp = () => { itemHelpVisible.value = true }
const onSelectItem = (data: any) => {
    searchForm.itemcd = String(data.itemcd || '').trim()
    searchForm.itemnm = String(data.itemnm || '').trim()
    itemHelpVisible.value = false
    handleSearch()
}

const handleSearch = async () => {
    if(!searchForm.itemcd) return vAlertError('품목을 먼저 선택하세요.');
	try {
		const ym = searchForm.year + '01';
		const { data } = await api.post('/hfmf/FMF2140R_STR', {
            cmpycd: String(authStore.cmpycd), yymm: String(ym), itemcd: String(searchForm.itemcd)
        })
		mainGrid?.setData(data)
    vAlert('조회되었습니다.')
	} catch (e) { vAlertError('조회 실패') }
}

const handleExport = () => {
	mainGrid?.download('xlsx', `월별제조원가명세서_${searchForm.year}_${searchForm.itemnm}.xlsx`)
}

onMounted(() => {
    const curYear = new Date().getFullYear();
    for(let i=0; i<6; i++) yearOptions.value.push((curYear - i).toString());

	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns', height: '100%',
      columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
			columns: [
				{ title: '과목', field: 'acctnm', width: 200, frozen: true, hozAlign: 'left', cssClass: 'fw-bold' },
				{ title: '1월', field: 'M01', hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 } },
				{ title: '2월', field: 'M02', hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 } },
				{ title: '3월', field: 'M03', hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 } },
				{ title: '4월', field: 'M04', hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 } },
				{ title: '5월', field: 'M05', hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 } },
				{ title: '6월', field: 'M06', hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 } },
				{ title: '7월', field: 'M07', hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 } },
				{ title: '8월', field: 'M08', hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 } },
				{ title: '9월', field: 'M09', hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 } },
				{ title: '10월', field: 'M10', hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 } },
				{ title: '11월', field: 'M11', hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 } },
				{ title: '12월', field: 'M12', hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 } }
			]
		})
	}
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
