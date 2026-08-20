<!--
	=============================================================
	프로그램명	: 품목별 제조원가 계산서 (HFMF212S)
	작성일자	: 2025.02.24
	설명        : 품목별 제조원가 상세 내역 조회 (HSOD100U 표준 UI 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-file-earmark-medical me-2 text-primary" style="font-size: 18px;"></i>
        원가관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        현황조회 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">품목별 제조원가 계산서 (HFMF212S)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-search" @click="handleSearch">조회</button>
        <button class="btn-erp btn-print" @click="openManual">도움말</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 조회 필터 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
                <col style="width: 10%" /><col style="width: 25%" />
                <col style="width: 10%" /><col style="width: 55%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light required">마감년월</th>
                <td>
                  <input v-model="searchForm.ym" type="month" class="form-control form-control-sm" style="max-width: 150px;" @change="handleSearch" />
                </td>
                <th class="text-center bg-light">품목선택</th>
                <td>
                  <div class="input-group input-group-sm" style="max-width: 400px;">
                    <input v-model="searchForm.itemcd" class="form-control text-center" style="max-width: 100px;" placeholder="코드" @keyup.enter="handleItemHelp" />
                    <input v-model="searchForm.itemnm" class="form-control" placeholder="품목명 입력" @keyup.enter="handleItemHelp" />
                    <button class="btn btn-outline-secondary" type="button" @click="handleItemHelp"><i class="bi bi-search"></i></button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 투-그리드 레이아웃 -->
      <div class="d-flex gap-2 flex-grow-1 overflow-hidden" style="min-height: 0;">
        <!-- 좌측: 품목 목록 -->
        <div class="card border shadow-sm d-flex flex-column overflow-hidden grid-container-left" style="width: 350px; min-width: 350px;">
          <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark">대상 품목 리스트</div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="leftGridRef" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>

        <!-- 우측: 상세 제조원가 계산서 -->
        <div class="flex-grow-1 d-flex flex-column gap-2 overflow-hidden">
          <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column grid-container-right">
            <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
              <span class="fw-bold small text-dark">
                <i class="bi bi-list-check me-2 text-primary"></i>제조원가 상세 명세
                <span v-if="selectedItemNm" class="badge bg-primary-subtle text-primary border border-primary-subtle ms-2">{{ selectedItemNm }}</span>
              </span>
              <div class="d-flex gap-1">
                <button class="btn btn-sm btn-outline-success py-0 px-2 fw-bold" @click="print('Excel')" style="font-size: 11px;">Excel</button>
                <button class="btn btn-sm btn-outline-dark py-0 px-2 fw-bold" @click="print('Print')" style="font-size: 11px;">인쇄</button>
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
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import { useCommonHelp } from '@/composables/useCommonHelp'
import { api } from '@/utils/axios'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import { useAuthStore } from '@/stores/authStore'

import { useManualStore } from '@/stores/manualStore'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { modalVisible, modalProps, openHelp } = useCommonHelp()
const manualStore = useManualStore()

const searchForm = reactive({
	ym: new Date().toISOString().substring(0, 7),
	itemcd: '',
	itemnm: ''
})

const selectedItemNm = ref('')
const leftGridRef = ref<HTMLElement | null>(null)
const mainGridRef = ref<HTMLElement | null>(null)
let leftGrid: Tabulator | null = null
let mainGrid: Tabulator | null = null

const handleItemHelp = async () => {
    const params = {
        gubun: 'I1',
        cmpycd: String(authStore.cmpycd),
        gbncd: '2',
        code: String(searchForm.itemcd || '').trim(),
        codenm: String(searchForm.itemnm || '').trim(),
        ETCVAL: ''
    }

    if (params.codenm || params.code) {
        try {
            const { data } = await api.post('/hs00/HS00_000S_STR', params)
            if (data && data.length === 1) {
                onSelectItem(data[0])
                return
            }
        } catch (e) { console.error('품목 검색 오류', e) }
    }

    Object.assign(modalProps, {
        title: '품목 선택',
        path: '/hs00/HS00_000S_STR',
        defaultField: 'itemnm',
        large: true,
        data: params,
        columns: [
            { title: '품목코드', field: 'itemcd', width: 120, hozAlign: 'center' },
            { title: '품목명', field: 'itemnm', minWidth: 250, widthGrow: 1, hozAlign: 'left', cssClass: 'fw-bold text-primary' },
            { title: '규격', field: 'itsize', width: 150 },
            { title: '단위', field: 'unitnm', width: 80, hozAlign: 'center' }
        ],
        onConfirm: onSelectItem
    })
    modalVisible.value = true
}

const onSelectItem = (data: any) => {
    searchForm.itemcd = String(data.itemcd || '').trim()
    searchForm.itemnm = String(data.itemnm || '').trim()
    handleSearch()
}

const handleSearch = async () => {
	try {
		const ym = searchForm.ym.replace('-', '')
		const { data } = await api.post('/hfmf/FMF2120R_STR', {
			cmpycd: String(authStore.cmpycd),
			yymm: String(ym),
			itemcd: String(searchForm.itemcd)
		})
		leftGrid?.setData(data)
		mainGrid?.clearData()
		selectedItemNm.value = ''

		if (data && data.length > 0) {
			nextTick(() => leftGrid?.selectRow(leftGrid.getRows()[0]))
		}
    vAlert('조회되었습니다.')
	} catch (e) { vAlertError('조회 실패') }
}

const selectDetails = async (itemcd: string) => {
	try {
		const ym = searchForm.ym.replace('-', '')
		const { data } = await api.post('/hfmf/FMF2120R_STR', {
			cmpycd: String(authStore.cmpycd),
			yymm: String(ym),
			itemcd: String(itemcd)
		})
		mainGrid?.setData(data)
	} catch (e) { vAlertError('상세 명세 조회 실패') }
}

const print = (type: 'Excel' | 'Print') => {
    if(!selectedItemNm.value) return vAlertError('품목을 선택하세요.');
    const fileName = `품목별제조원가계산서_${selectedItemNm.value}_${searchForm.ym}`;
    if (type === 'Excel') {
        mainGrid?.download('xlsx', `${fileName}.xlsx`, { sheetName: '원가계산서' });
    } else {
        mainGrid?.print(false, true);
    }
}

const openManual = () => {
  manualStore.open('HFMF212S')
}

onMounted(() => {
	if (leftGridRef.value) {
		leftGrid = new Tabulator(leftGridRef.value, {
			layout: 'fitColumns', height: '100%', selectable: 1,
      columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
			columns: [
				{ title: "No", formatter: "rownum", width: 40, hozAlign: "center" },
				{ title: '구분', field: 'gubun', width: 80, hozAlign: 'center' },
				{ title: '품목코드', field: 'itemcd', width: 120, hozAlign: 'center' },
				{ title: '품목명', field: 'itemnm', widthGrow: 1, hozAlign: 'left', cssClass: 'fw-bold text-primary' }
			]
		})
		leftGrid.on('rowSelected', (row) => {
			const data = row.getData()
			selectedItemNm.value = data.itemnm
			selectDetails(data.itemcd)
		})
	}

	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns', height: '100%',
			columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
			columns: [
				{
            title: '계정과목 명', field: 'acctnm', widthGrow: 2, hozAlign: 'left', frozen: true,
            formatter: (cell) => {
                const row = cell.getData();
                const acct = String(row.ACCT || '');
                const acctnm = cell.getValue();
                if (acct === '411-0000') return `&nbsp;&nbsp;${acctnm}`;
                if (acct === '412-0000') return `<b>Ⅱ. ${acctnm}</b>`;
                if (acct === '413-0000') return `<b>Ⅲ. ${acctnm}</b>`;
                if (acct === '419-0000') return `&nbsp;${acctnm}`;
                return `&nbsp;&nbsp;&nbsp;&nbsp;${acctnm}`;
            }
        },
				{ title: '수량', field: 'prqqty', hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 } },
				{ title: '금액', field: 'totamt', hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 } },
				{ title: '단위당원가', field: 'price', hozAlign: 'right', formatter: 'money', formatterParams: { precision: 2 }, cssClass: 'fw-bold text-primary bg-light' }
			]
		})
	}
	handleSearch()
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
