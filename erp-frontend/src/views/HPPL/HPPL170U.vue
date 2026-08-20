<!--
	=============================================================
	프로그램명	: 주간 생산계획
	프로그램 ID	: HPPL170U
	작성일자	: 2025.02.24
	작성자	    : AI Assistant
	설명        : 주문 및 요청 내역을 기반으로 주간 생산계획 수립
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
	<Modal v-model:visible="showModal" :modalProps="props" />

	<div class="erp-container">
		<!-- 🚀 1. 상단 액션 바 (표준 규격) -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-calendar-check-fill me-2 text-primary" style="font-size: 18px;"></i>
				생산계획관리 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				<span class="text-primary fw-bolder">주간 생산계획 (HPPL170U)</span>
			</div>
			<div class="btn-group-erp d-flex gap-1">
				<button class="btn-erp btn-init" @click="initialize">초기화</button>
				<button class="btn-erp btn-search" @click="search">조회</button>
			</div>
		</div>

		<div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2">
			<!-- 🅰️ 조회 조건 영역 (표준 erp-table-full) -->
			<div class="card border-0 shadow-sm overflow-hidden flex-shrink-0">
				<div class="card-body p-0">
					<table class="erp-table-full">
						<colgroup>
							<col style="width: 80px;" /><col style="width: 320px;" />
							<col style="width: 80px;" /><col style="width: 150px;" />
							<col />
						</colgroup>
						<tbody>
							<tr>
								<th class="required">납기기간</th>
								<td>
									<div class="d-flex align-items-center gap-1">
										<input v-model="searchForm.frYmd" type="date" class="form-control form-control-sm" />
										<span class="px-1">~</span>
										<input v-model="searchForm.toYmd" type="date" class="form-control form-control-sm" />
									</div>
								</td>
								<th>대상구분</th>
								<td>
									<select v-model="searchForm.gubun" class="form-select form-select-sm">
										<option value="100">주문건</option>
										<option value="200">양산요청</option>
										<option value="300">외주요청</option>
									</select>
								</td>
								<td class="text-end pe-3">
									<!-- 추가 검색조건 필요 시 배치 -->
								</td>
							</tr>
						</tbody>
					</table>
				</div>
			</div>

			<!-- 🅱️ 주문 및 생산요청 내역 (상단 그리드) -->
			<div class="card border-0 shadow-sm flex-grow-1 overflow-hidden d-flex flex-column" style="min-height: 250px;">
				<div class="card-header bg-white py-1 px-3 border-bottom d-flex justify-content-between align-items-center">
					<div class="d-flex align-items-center gap-3">
						<span class="fw-bold small text-dark"><i class="bi bi-list-check me-1 text-primary"></i>주문 및 생산요청 내역</span>
						<div class="vr mx-1 h-50"></div>
						<div class="d-flex align-items-center gap-2">
							<label class="small fw-bold text-muted" style="font-size: 11px;">생산라인 적용:</label>
							<select v-model="applyLineCd" class="form-select form-select-sm" style="width: 140px;">
								<option value="">라인 선택</option>
								<option v-for="item in lineData" :key="item.linecd" :value="item.linecd">{{ item.linenm }}</option>
							</select>
							<button class="btn btn-xs btn-primary fw-bold px-2" @click="handleMakePlan">계획 생성</button>
						</div>
					</div>
				</div>
				<div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
					<div ref="mainGridRef" class="tabulator-instance flex-grow-1"></div>
				</div>
			</div>

			<!-- 🅲 공정별 생산계획 (하단 그리드) -->
			<div class="card border-0 shadow-sm flex-grow-1 overflow-hidden d-flex flex-column" style="min-height: 250px;">
				<div class="card-header bg-white py-1 px-3 border-bottom d-flex justify-content-between align-items-center">
					<span class="fw-bold small text-dark"><i class="bi bi-calendar-event me-1 text-success"></i>공정별 생산계획</span>
					<button class="btn btn-xs btn-outline-danger px-3 fw-bold" @click="handleDelete">계획 삭제</button>
				</div>
				<div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
					<div ref="subGridRef" class="tabulator-instance flex-grow-1"></div>
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
import { useSearch } from '@/composables/useSearch'
import { useSave } from '@/composables/useSave'
import { fetchLineData, type SelectPdLineData } from '@/composables/useFetchSelectData'
import { api } from '@/utils/axios'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import type { ModalProps } from '@/types/modal'

const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { searchStart } = useSearch()
const { saveBody } = useSave()

const searchForm = reactive({
	frYmd: new Date().toISOString().substring(0, 8) + '01',
	toYmd: new Date().toISOString().substring(0, 10),
    gubun: '100'
})
const applyLineCd = ref('')
const lineData = ref<SelectPdLineData[]>([])

const mainGridRef = ref<HTMLElement | null>(null)
const subGridRef = ref<HTMLElement | null>(null)
let mainGrid: Tabulator | null = null
let subGrid: Tabulator | null = null

const props = ref<ModalProps>({
	type: '', large: false, checked: false, searchDate: null, defaultField: '',
	hasData: false, title: '', columns: [], path: '', data: {}, onConfirm: () => {},
})
const showModal = ref(false)

const initialize = () => {
	searchForm.frYmd = new Date().toISOString().substring(0, 8) + '01'
	searchForm.toYmd = new Date().toISOString().substring(0, 10)
    searchForm.gubun = '100'
	applyLineCd.value = ''
	mainGrid?.clearData()
	subGrid?.clearData()
}

const search = async () => {
	try {
		const params = {
			frymd: searchForm.frYmd.replaceAll('-', ''),
			toymd: searchForm.toYmd.replaceAll('-', ''),
            gubun: searchForm.gubun
		}

		// 상단: 생산계획 대상 조회
		const targetData = await searchStart('/product/pdplan/targetlist', params)
		mainGrid?.setData(targetData.map((d: any) => ({ ...d, showSord: `${d.ordymd}-${d.ordno}` })))

		// 하단: 확정된 생산계획 조회
		const planData = await searchStart('/product/pdplan/list', params)
		subGrid?.setData(planData)

	} catch (e) {
		vAlertError('조회 중 오류가 발생했습니다.')
	}
}

const handleMakePlan = async () => {
	if (!applyLineCd.value) return vAlertError('적용할 생산라인을 선택하세요.')
	const selected = mainGrid?.getSelectedData()
	if (!selected || selected.length === 0) return vAlertError('계획을 생성할 항목을 선택하세요.')

	if (!confirm('선택한 항목에 대해 생산계획을 생성하시겠습니까?')) return

	try {
		const payload = selected.map(d => ({
			...d,
            _status: '입력',
            linecd: applyLineCd.value,
			yymmdd: d.napgiymd || d.napgi_dt || '', // 계획일 기본값은 납기일로 설정
			planqty: d.planqty || d.ordqty
		}))
		const res = await api.post('/product/pdplan/save', payload)
		const out = res.data[0]
		if (out.result === 'OK' || out.RESULT === 'OK') {
            vAlert(out.msg || out.MSG || '계획 생성 완료')
            search()
        } else {
			vAlertError(out.msg || out.MSG || '생성 실패')
		}
	} catch (e: any) {
        vAlertError(e.response?.data?.message || '저장 중 오류가 발생했습니다.')
    }
}

const handleDelete = async () => {
	const selected = subGrid?.getSelectedData()
	if (!selected || selected.length === 0) return vAlertError('삭제할 항목을 선택하세요.')
	if (!confirm('선택한 항목의 계획을 삭제하시겠습니까?')) return

	try {
		const payload = selected.map(d => ({ ...d, _status: '삭제' }))
		const res = await api.post('/product/pdplan/save', payload)
		const out = res.data[0]
		if (out.result === 'OK' || out.RESULT === 'OK') {
            vAlert(out.msg || out.MSG || '삭제 완료')
            search()
        } else {
			vAlertError(out.msg || out.MSG || '삭제 실패')
		}
	} catch (e: any) {
        vAlertError(e.response?.data?.message || '삭제 중 오류가 발생했습니다.')
    }
}

const initGrid = () => {
	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns', height: '100%', selectable: true,
			placeholder: "조회된 데이터가 없습니다.",
			columnDefaults: { headerHozAlign: 'center', vertAlign: 'middle', headerSort: false },
			columns: [
				{ title: '', formatter: 'rowSelection', titleFormatter: 'rowSelection', width: 40, hozAlign: 'center', cellClick: (e, cell) => cell.getRow().toggleSelect() },
				{ title: '구분', field: 'gubun', width: 80, hozAlign: 'center' },
				{ title: '관리번호', field: 'showSord', width: 120, hozAlign: 'center' },
				{ title: '거래처', field: 'custnm', widthGrow: 1.5, hozAlign: 'left' },
				{ title: '납기일', field: 'napgiymd', width: 110, hozAlign: 'center' },
				{ title: '제품명', field: 'itemnm', widthGrow: 2, hozAlign: 'left' },
				{ title: '규격', field: 'itsize', width: 120, hozAlign: 'center' },
				{ title: '요청수량', field: 'ordqty', width: 100, hozAlign: 'right', headerHozAlign: 'right', formatter: 'money' },
				{ title: '담당자', field: 'updemp', width: 100, hozAlign: 'left' }
			]
		})
	}

	if (subGridRef.value) {
		subGrid = new Tabulator(subGridRef.value, {
			layout: 'fitColumns', height: '100%', selectable: true,
			placeholder: "등록된 계획이 없습니다.",
			columnDefaults: { headerHozAlign: 'center', vertAlign: 'middle', headerSort: false },
			columns: [
				{ title: '', formatter: 'rowSelection', titleFormatter: 'rowSelection', width: 40, hozAlign: 'center', cellClick: (e, cell) => cell.getRow().toggleSelect() },
				{ title: '계획일자', field: 'yymmdd', width: 100, hozAlign: 'center' },
				{ title: '요일', field: 'day_nm', width: 80, hozAlign: 'center' },
				{ title: '품목', field: 'itemnm', widthGrow: 2, hozAlign: 'left' },
				{ title: '규격', field: 'itsize', width: 120, hozAlign: 'center' },
				{ title: '라인', field: 'linenm', width: 100, hozAlign: 'left' },
				{ title: '공정', field: 'prognm', width: 100, hozAlign: 'left' },
				{ title: '계획수량', field: 'planqty', width: 90, hozAlign: 'right', headerHozAlign: 'right', formatter: 'money', cssClass: 'text-primary fw-bold' },
				{ title: '일능력', field: 'unit_capa', width: 90, hozAlign: 'right', headerHozAlign: 'right', formatter: 'money', cssClass: 'text-success' },
				{ title: '소요시간(분)', field: 'capahh', width: 110, hozAlign: 'right', headerHozAlign: 'right', formatter: 'money', formatterParams: { precision: 1 } },
				{ title: '가동시간(h)', field: 'gadtmdd', width: 100, hozAlign: 'right', headerHozAlign: 'right', formatter: 'money' },
				{ title: '가동(%)', field: 'gadrate', width: 80, hozAlign: 'right', headerHozAlign: 'right', formatter: (c:any) => c.getValue() ? c.getValue() + '%' : '0%' },
				{ title: '양품(%)', field: 'jungrate', width: 80, hozAlign: 'right', headerHozAlign: 'right', formatter: (c:any) => c.getValue() ? c.getValue() + '%' : '0%' },
				{ title: '납품처', field: 'custnm', width: 120, hozAlign: 'left' },
				{ title: '납기일', field: 'napgiymd', width: 110, hozAlign: 'center' }
			]
		})
	}
}

onMounted(async () => {
	lineData.value = await fetchLineData()
	nextTick(() => {
		initGrid()
		search()
	})
})
</script>

<style scoped>
.erp-container { height: calc(100vh - 110px); display: flex; flex-direction: column; background-color: #f8f9fa; }
.btn-xs { padding: 1px 6px; font-size: 0.75rem; }
.tabulator-instance { border: 1px solid #dee2e6; }
</style>
