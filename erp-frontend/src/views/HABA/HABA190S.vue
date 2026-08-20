<!--
	=============================================================
	프로그램명: 거래처현황 (haba190s)
	작성일자	: 2025.03.14
	설명        : 등록된 모든 거래처의 상세 현황 조회 및 관리화면 이동
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container d-flex flex-column h-100 bg-white">
		<!-- 🚀 1. 상단 액션 바 (검색 및 네비게이션용) -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-1 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 13px;">
				<i class="bi bi-card-list me-2 text-primary"></i>
				기본정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
				기초관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
				<span class="text-primary fw-bolder">거래처현황 (haba190s)</span>
			</div>
			<div class="btn-group-erp d-flex gap-1">
				<button class="btn-erp btn-init" @click="initialize">초기화</button>
				<button class="btn-erp btn-search" @click="search">조회</button>
				<button class="btn-erp btn-excel" @click="excel">엑셀</button>
				<button class="btn-erp btn-print" @click="print">인쇄</button>
			</div>
		</div>

		<!-- 💡 2. 검색 조건 영역 -->
		<div class="p-2 pb-0 flex-shrink-0 bg-light">
			<div class="card border shadow-sm overflow-hidden bg-white">
				<table class="erp-table-dense w-100">
					<colgroup>
						<col style="width: 80px;" /><col style="width: 150px;" />
						<col style="width: 80px;" /><col style="width: 200px;" />
						<col style="width: 80px;" /><col style="width: 200px;" />
						<col />
					</colgroup>
					<tbody>
						<tr>
							<th class="bg-light text-center">입출구분</th>
							<td>
								<select v-model="searchForm.iogbn" class="form-select form-select-sm" @change="search">
									<option value="000">전체</option>
									<option v-for="opt in ioGbnOptions" :key="opt.CD" :value="opt.CD">{{ opt.NM }}</option>
								</select>
							</td>
							<th class="bg-light text-center border-start">거래처명</th>
							<td class="px-2">
								<input v-model="searchForm.custnm" type="text" class="form-control form-control-sm" placeholder="거래처명 검색" @keydown.enter="search" />
							</td>
							<th class="bg-light text-center border-start">대표자</th>
							<td class="px-2">
								<input v-model="searchForm.bossnm" type="text" class="form-control form-control-sm" placeholder="대표자명 검색" @keydown.enter="search" />
							</td>
							<td class="px-3 text-muted small border-0">
								<i class="bi bi-info-circle me-1"></i> 번호를 클릭하면 거래처관리화면으로 이동합니다.
							</td>
						</tr>
					</tbody>
				</table>
			</div>
		</div>

		<!-- 📊 3. 그리드 영역 -->
		<div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column bg-light">
			<div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white">
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
import * as XLSX from 'xlsx'
import { useAlerts } from '@/composables/useAlerts'
import AppAlert from '@/components/AppAlert.vue'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { useRouter } from 'vue-router'

const authStore = useAuthStore()
const router = useRouter()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

// 검색 데이터
const searchForm = reactive({
	iogbn: '010',
	custnm: '',
    bossnm: ''
})

const ioGbnOptions = ref<any[]>([])
const mainGridRef = ref<HTMLDivElement | null>(null)
let mainGrid: Tabulator | null = null

// 상세 정보 페이지(HABA180U)로 이동 (네비게이션 처리)
const goDetail = (data: any) => {
	router.push({
		path: '/HABA/HABA180U',
		query: {
            custcd: data.custcd,   // 상세 조회를 위한 코드 전달
			custnm: data.custnm,
            custgbn: data.custgbn,
            status: data.status
		}
	})
}

// 초기 정보 로드
const loadInitData = async () => {
	try {
		const resIo = await api.post('/ha00/HA00_00P_STR', { gubun: 'E0', gbncd: '340' })
		ioGbnOptions.value = (resIo.data || []).map((n: any) => ({ CD: n.codecd, NM: n.codenm }))
	} catch (e) {
		console.error('기초 데이터 로드 실패')
	}
}

const search = async () => {
	try {
		const res = await api.post('/haba/HABA_190S_STR', {
			iogbn: searchForm.iogbn,
			custnm: searchForm.custnm,
		    bossnm: searchForm.bossnm
		})

		const processedData = (res.data || []).map((normalized: any) => {
			let formattedNo = normalized.custno || ''
			if (formattedNo.length === 10) {
				formattedNo = `${formattedNo.substring(0, 3)}-${formattedNo.substring(3, 5)}-${formattedNo.substring(5)}`
			} else if (formattedNo.length === 13) {
				formattedNo = `${formattedNo.substring(0, 6)}-${formattedNo.substring(6)}`
			}

			return {
				...normalized,
				custno_f: formattedNo,
				bank_acc: `${normalized.banknm || ''} ${normalized.gujoa || ''}`.trim()
			}
		})

		mainGrid?.setData(processedData)
		vAlert('조회되었습니다.')
	} catch (e) {
		vAlertError('조회 중 오류 발생')
	}
}

const initialize = () => {
	resetForm(searchForm)
	searchForm.iogbn = '010'
	mainGrid?.clearData()
}

const excel = () => mainGrid?.download("xlsx", `거래처현황_${new Date().toISOString().substring(0, 10)}.xlsx`)
const print = () => {
    const params = new URLSearchParams(searchForm).toString();
    window.open(`/haba/HABA_190P?${params}`)
}

onMounted(() => {
	if (typeof window !== 'undefined') (window as any).XLSX = XLSX;
	loadInitData()
	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns',
			height: '100%',
			pagination: "local",
			paginationSize: 50,
			paginationButtonCount: 10,
			paginationSizeSelector: [50, 100, 200, 500],
			columnDefaults: { headerSort: false, vertAlign: "middle" },
			columns: [
				{
					title: "거래처", field: "custcd", width: 85, hozAlign: "center",
					formatter: (cell) => `<span class="text-primary text-decoration-underline cursor-pointer fw-bold">${cell.getValue()}</span>`,
					cellClick: (e, cell) => goDetail(cell.getData())
				},
				{
					title: "거래처상호", field: "custnm", minWidth: 200,
					formatter: (cell) => `<span class="text-primary text-decoration-underline cursor-pointer fw-bold">${cell.getValue()}</span>`,
					cellClick: (e, cell) => goDetail(cell.getData())
				},
				{ title: "구분", field: "iogbnnm", width: 80, hozAlign: "center" },
				{ title: "종류", field: "custgbnm", width: 80, hozAlign: "center" },
				{ title: "사업자번호", field: "custno_f", width: 115, hozAlign: "center" },
				{ title: "대표자", field: "bossnm", width: 90, hozAlign: "center" },
				{ title: "업태", field: "custtype", width: 120 },
				{ title: "종목", field: "custkind", width: 120 },
				{ title: "연락처", field: "telno", width: 110, hozAlign: "center" },
				{ title: "상태", field: "statusnm", width: 80, hozAlign: "center" },
                { title: "사용", field: "useyn", width: 80, hozAlign: "center",
                  formatter: (cell) => {
                    const val = String(cell.getValue() || '').trim().toUpperCase();
                    return val === 'Y' ? '<b class="text-primary">사용</b>' : '';
                  }
                }
			]
		})
	}
    search()
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
:deep(.tabulator-row:hover) { background-color: #f0f7ff !important; cursor: pointer; }
.cursor-pointer { cursor: pointer; }
:deep(.tabulator-footer) {
	background-color: #f8f9fa !important;
	border-top: 1px solid #dee2e6 !important;
	font-size: 12px !important;
	padding: 5px !important;
}
:deep(.tabulator-page) {
	padding: 2px 8px !important;
	margin: 0 2px !important;
	border: 1px solid #ddd !important;
	border-radius: 3px !important;
	background: #fff !important;
}
:deep(.tabulator-page.active) {
	background: #0d6efd !important;
	color: #fff !important;
	border-color: #0d6efd !important;
}
</style>
