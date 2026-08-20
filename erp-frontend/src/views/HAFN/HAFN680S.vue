<!--
	=============================================================
	프로그램명: 카드미지급금현황 (HAFN680S)
	작성일자	: 2025.03.14
	작성자    : AI Assistant
	설명        : 카드사별 카드 미지급금의 발생, 지불, 잔액 현황 조회
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom bg-white py-2 px-3 sticky-top shadow-sm">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-credit-card-2-front me-2 text-primary" style="font-size: 18px;"></i>
        자금관리 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        <span class="text-primary fw-bolder">카드미지급금현황 (HAFN680S)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-print" @click="print">인쇄</button>
        <button class="btn-erp btn-excel" @click="excel">엑셀</button>
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
                <th class="text-center bg-light border-end">기준일자</th>
                <td class="border-end px-2">
                  <div class="d-flex align-items-center gap-1">
                    <input v-model="searchForm.ymd" type="date" class="form-control form-control-sm" style="max-width: 150px;" />
                    <span class="small fw-bold text-secondary">현재</span>
                  </div>
                </td>
                <th class="text-center bg-light border-end">카드사</th>
                <td>
                  <div class="d-flex align-items-center gap-1 px-1">
                    <div class="input-group input-group-sm" style="width: 220px;">
                      <input v-model="searchForm.custcdfr" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
                      <input v-model="searchForm.custnmfr" type="text" class="form-control" placeholder="시작 카드사" @keydown.enter="openHelp('CUSTFR')" />
                      <button class="btn btn-outline-secondary px-2" @click="openHelp('CUSTFR')"><i class="bi bi-search"></i></button>
                    </div>
                    <span class="text-muted">~</span>
                    <div class="input-group input-group-sm" style="width: 220px;">
                      <input v-model="searchForm.custcdto" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
                      <input v-model="searchForm.custnmto" type="text" class="form-control" placeholder="종료 카드사" @keydown.enter="openHelp('CUSTTO')" />
                      <button class="btn btn-outline-secondary px-2" @click="openHelp('CUSTTO')"><i class="bi bi-search"></i></button>
                    </div>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 d-flex flex-column overflow-hidden">
        <div class="card-header bg-white py-1 px-3 border-bottom text-secondary">
          <span class="fw-bold small"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i> 카드사별 미지급 현황</span>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="mainGridRef" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>
    </div>
  </div>

  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { useRouter } from 'vue-router'
import { addDynamicRoute } from '@/router/dynamicRoute'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import type { ModalProps } from '@/types/modal'

const authStore = useAuthStore()
const router = useRouter()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

const today = new Date().toISOString().substring(0, 10)

// 검색 데이터
const searchForm = reactive({
	ymd: today,
	custcdfr: '',
	custnmfr: '',
	custcdto: '',
	custnmto: ''
})

const mainGridRef = ref<HTMLDivElement | null>(null)
let mainGrid: Tabulator | null = null

const search = async () => {
	try {
		const res = await api.post('/hafn/HAFN_680S_STR', {
			cmpycd: authStore.cmpycd,
			ymd: searchForm.ymd.replace(/-/g, ''),
			custcdfr: searchForm.custcdfr,
			custcdto: searchForm.custcdto
		})

		const data = (res.data || []).map((item: any) => {
            return {
                ...item,
                upyamt: Number(item.upyamt || 0),
                payamt: Number(item.payamt || 0),
                janamt: Number(item.janamt || 0)
            }
        })

		mainGrid?.setData(data)
		vAlert('조회되었습니다.')
	} catch (e) { vAlertError('조회 실패') }
}

const initialize = () => {
	resetForm(searchForm)
	searchForm.ymd = today
	mainGrid?.clearData()
}

const excel = () => mainGrid?.download("xlsx", `카드미지급금현황_${today}.xlsx`)

const print = () => {
	const params = `ymd=${searchForm.ymd}&custcdfr=${searchForm.custcdfr}&custcdto=${searchForm.custcdto}`
	window.open(`/hafn/HAFN_680P?${params}`, 'CardUnpaidStatusPrint', 'width=1000,height=800,scrollbars=yes')
}

/**
 * 상세 이동 (HAFN660S로 이동)
 */
const goDetail = (row: any) => {
    const pgmid = 'HAFN660S'
    addDynamicRoute(pgmid, '카드미지급상세현황', 'HAFN')
	router.push({
		path: `/${pgmid}`,
		query: {
            ymd: searchForm.ymd.replace(/-/g, ''),
            custcd: row.custcd,
            mgtno: row.mgtno,
            custnm: row.custnm
        }
	})
}

// 팝업 설정
const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function openHelp(type: string) {
	const isFr = type === 'CUSTFR'
	Object.assign(modalProps, {
		title: '카드사 선택', path: '/ha00/HA00_00P_STR',
		data: { gubun: 'C3', custgbn: '020', cmpycd: authStore.cmpycd, search: isFr ? searchForm.custnmfr : searchForm.custnmto },
		columns: [{ title: '코드', field: 'bankcd', width: 80 }, { title: '카드사명', field: 'banknm', width: 180 }],
		onConfirm: (item: any) => {
			if (isFr) { searchForm.custcdfr = item.bankcd; searchForm.custnmfr = item.banknm }
			else { searchForm.custcdto = item.bankcd; searchForm.custnmto = item.banknm }
		}
	})
	modalVisible.value = true
}

onMounted(() => {
	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns',
			height: '100%',
			columnDefaults: { headerSort: false, vertAlign: "middle" },
			columns: [
				{
					title: "카드사", field: "custcd", width: 100, hozAlign: "center",
					formatter: (cell) => `<span class="text-primary text-decoration-underline cursor-pointer fw-bold">${cell.getValue()}</span>`,
					cellClick: (e, cell) => goDetail(cell.getData())
				},
				{
					title: "카드사명", field: "custnm", width: 200,
					formatter: (cell) => `<span class="text-primary text-decoration-underline cursor-pointer">${cell.getValue()}</span>`,
					cellClick: (e, cell) => goDetail(cell.getData())
				},
				{
                    title: "카드번호", field: "mgtno", width: 200,
                    formatter: (cell) => `<span class="text-primary text-decoration-underline cursor-pointer">${cell.getValue()}</span>`,
					cellClick: (e, cell) => goDetail(cell.getData())
                },
				{ title: "결제구좌", field: "soname", minWidth: 200 },
				{
					title: "발생액", field: "upayamt", width: 130, hozAlign: "right",
					formatter: "money", formatterParams: { precision: 0 },
					bottomCalc: "sum", bottomCalcFormatter: "money", bottomCalcFormatterParams: { precision: 0 }
				},
				{
					title: "지불액", field: "payamt", width: 130, hozAlign: "right",
					formatter: "money", formatterParams: { precision: 0 },
					bottomCalc: "sum", bottomCalcFormatter: "money", bottomCalcFormatterParams: { precision: 0 }
				},
				{
					title: "잔액", field: "janamt", width: 130, hozAlign: "right",
					formatter: "money", formatterParams: { precision: 0 },
					bottomCalc: "sum", bottomCalcFormatter: "money", bottomCalcFormatterParams: { precision: 0 },
					cssClass: "fw-bold text-danger"
				}
			]
		})
	}
    search()
})
</script>

<style scoped>
.erp-label { min-width: 80px; font-weight: 500; font-size: 13px; }
.cursor-pointer { cursor: pointer; }
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
