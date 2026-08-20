<!--
	=============================================================
	프로그램명	: 매출전표발행 [디자인 원칙 13가지 + 검색영역/등록영역 분리]
	작성일자	: 2025.02.24
	작성자	    : AI Assistant
	설명        : 매출 확정 내역에 대한 전표 생성 및 조회/등록 영역 분리 UI 적용
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container">
		<!-- 🚀 1. 상단 액션 바 -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-file-earmark-check-fill me-2 text-primary" style="font-size: 18px;"></i>
				영업정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				매출관리 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				<span class="text-primary fw-bolder">매출전표발행 (HSIO530U)</span>
			</div>
			<div class="btn-group-erp d-flex gap-1">
				<button class="btn-erp btn-init" @click="initialize">
					<i class="bi bi-arrow-clockwise"></i> 초기화
				</button>
				<button class="btn-erp btn-search" @click="fetchUnissuedList">
					<i class="bi bi-search"></i> 조회
				</button>
				<button class="btn-erp btn-save" @click="handleGenerateSlip">
					<i class="bi bi-file-earmark-check"></i> 전표 발행
				</button>
			</div>
		</div>

		<!-- 💡 2. 메인 컨텐츠 영역 -->
		<div class="flex-grow-1 overflow-auto p-2 d-flex flex-column gap-2">
			<!-- 🅰️ 조회 조건 영역 -->
			<div class="card border shadow-sm overflow-hidden">
				<div class="card-header bg-light py-1 px-3 border-bottom d-flex align-items-center">
					<span class="fw-bold small text-dark"><i class="bi bi-search me-1"></i> 조회 조건</span>
				</div>
				<div class="card-body p-0">
					<table class="erp-table-full">
						<colgroup>
							<col style="width: 10%" />
							<col style="width: 23%" />
							<col style="width: 10%" />
							<col style="width: 23%" />
							<col style="width: 34%" />
						</colgroup>
						<tbody>
							<tr>
								<th class="required">판매부서</th>
								<td>
									<div class="input-group input-group-sm flex-nowrap" style="max-width: 220px;">
										<input v-model="searchForm.deptcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
										<input v-model="searchForm.deptnm" type="text" class="form-control" placeholder="부서 선택" @keyup.enter="openHelp('DEPT')" />
										<button class="btn btn-outline-secondary px-2" @click="openHelp('DEPT')"><i class="bi bi-search"></i></button>
									</div>
								</td>
								<th class="required">정산일자</th>
								<td>
									<div class="d-flex align-items-center gap-1 flex-grow-1" style="max-width: 260px;">
										<input v-model="searchForm.fromdt" type="date" class="form-control form-control-sm" />
										<span class="text-muted">~</span>
										<input v-model="searchForm.todt" type="date" class="form-control form-control-sm" />
									</div>
								</td>
								<td></td>
							</tr>
						</tbody>
					</table>
				</div>
			</div>

			<!-- 🅱️ 전표 발행 설정 (등록 영역) -->
			<div class="card border shadow-sm overflow-hidden">
				<div class="card-header py-1 px-3 border-bottom d-flex align-items-center" style="background-color: #f0f7ff !important;">
					<span class="fw-bold small text-primary"><i class="bi bi-pencil-square me-1"></i> 전표 발행 정보 (등록용)</span>
				</div>
				<div class="card-body p-0">
					<table class="erp-table-full">
						<colgroup>
							<col style="width: 10%" />
							<col style="width: 23%" />
							<col style="width: 10%" />
							<col style="width: 23%" />
							<col style="width: 34%" />
						</colgroup>
						<tbody>
							<tr>
								<th class="required bg-light-primary">발행부서</th>
								<td>
									<div class="input-group input-group-sm flex-nowrap" style="max-width: 220px;">
										<input v-model="formData.deptcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
										<input v-model="formData.deptnm" type="text" class="form-control" placeholder="발행부서 선택" @keyup.enter="openHelp('ISSUE_DEPT')" />
										<button class="btn btn-outline-secondary px-2" @click="openHelp('ISSUE_DEPT')"><i class="bi bi-search"></i></button>
									</div>
								</td>
								<th class="required bg-light-primary">발행일자</th>
								<td>
									<input v-model="slipForm.slipymd" type="date" class="form-control form-control-sm" style="max-width: 140px;" />
								</td>
								<td></td>
							</tr>
						</tbody>
					</table>
				</div>
			</div>

			<!-- Ⓒ 데이터 그리드 영역 -->
			<div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column">
				<div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
					<span class="fw-bold small text-dark d-flex align-items-center">
						<i class="bi bi-list-check me-2 text-primary"></i> 전표 발행 대기 목록
					</span>
				</div>
				<div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
					<div ref="mainGridRef" class="tabulator-instance flex-grow-1"></div>
				</div>
				<!-- 🚀 하단 요약 정보 (Footer) -->
				<div class="card-footer bg-light border-top py-1 px-3 d-flex justify-content-between align-items-center flex-shrink-0">
					<div class="small text-muted">
						[ 선택: <span class="fw-bold text-primary">{{ activeItemCount }}</span> 건 ]
					</div>
					<div class="d-flex gap-4 small">
						<div class="d-flex align-items-center">
							<span class="me-2 text-muted">공급가:</span>
							<span class="fw-bold text-dark">{{ formatNumber(totals.spyamt) }}</span>
						</div>
						<div class="d-flex align-items-center border-start ps-4">
							<span class="me-2 text-muted text-info">부가세:</span>
							<span class="fw-bold text-info">{{ formatNumber(totals.vatamt) }}</span>
						</div>
						<div class="d-flex align-items-center border-start ps-4">
							<span class="me-2 text-muted text-danger">합계금액:</span>
							<span class="fw-bold text-danger">{{ formatNumber(totals.jsansum) }}</span>
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>

	<Modal v-model:visible="modalVisible" :modalProps="modalProps" />
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
import type { ModalProps } from '@/types/modal'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

// 상태 관리
const searchForm = reactive({
	deptcd: authStore.deptcd, deptnm: authStore.deptnm,
	fromdt: new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString().substring(0, 10),
	todt: new Date().toISOString().substring(0, 10)
})

const slipForm = reactive({ slipymd: new Date().toISOString().substring(0, 10) })
const formData = reactive({ deptcd: authStore.deptcd, deptnm: authStore.deptnm })
const closingInfo = reactive({ clsymd: '', sclsym: '' })
const activeItemCount = ref(0)
const totals = reactive({ spyamt: 0, vatamt: 0, jsansum: 0 })

const mainGridRef = ref<HTMLDivElement | null>(null); let mainGrid: Tabulator | null = null

const fetchUnissuedList = async () => {
	try {
		const res = await api.post('/hsio/HSIO_530U_STR', {
			actkind: 'S0',
			iogbn: '200',
			cmpycd: authStore.cmpycd,
			fromdt: searchForm.fromdt.replace(/-/g, ''),
			todt: searchForm.todt.replace(/-/g, ''),
			deptcd: searchForm.deptcd
		})
		// 🚀 합계금액(공급가+부가세) 계산
		const data = (res.data || []).map((item: any) => {
			item.jsansum = (Number(item.spyamt) || 0) + (Number(item.vatamt) || 0)
			return item
		})
		mainGrid?.setData(data)
		vAlert('조회되었습니다.')
	} catch (e) { vAlertError('조회 실패') }
}

/**
 * 🚀 전표 발행 처리 (ASP 패턴 반영)
 */
const handleGenerateSlip = async () => {
	const selected = mainGrid?.getSelectedData()
	if (!selected || selected.length === 0) return vAlertError('발행할 항목을 선택하세요.')

	const slipymd = slipForm.slipymd.replace(/-/g, '')
	if (slipymd <= closingInfo.clsymd) return vAlertError(`회계 마감된 일자입니다. (마감: ${closingInfo.clsymd})`)
	if (slipymd.substring(0,6) <= closingInfo.sclsym) return vAlertError(`영업 마감된 월입니다. (마감: ${closingInfo.sclsym})`)

	if (!confirm('전표를 발행하시겠습니까?')) return
	try {
		const payload = {
			mst: {
				slipymd: slipForm.slipymd,
				cmpycd: authStore.cmpycd,
				deptcd: formData.deptcd,
				usernm: authStore.usernm,
				fromdt: searchForm.fromdt,
				todt: searchForm.todt
			},
			items: selected.map(item => ({
				udeptcd: item.udeptcd, // ASP의 UDEPTCD 대응
				deptcd: item.deptcd,
				jsanym: item.jsanym,
				jsanno: item.jsanno,
				jsanymd: item.jsanymd,
				spyamt: item.spyamt,
				vatamt: item.vatamt,
				custcd: item.custcd,
				taxunit: item.taxunit,
				vattype: item.vattype
			}))
		}

		const res = await api.post('/hsio/HSIO_530U_SAVE', payload)
		const { slipno, res: status } = res.data

		if (status === 'OK') {
			vAlert('전표가 발행되었습니다.')
			// 전표 인쇄 팝업
			const printUrl = `../HASL/HASL_SLIP_PRINT.asp?slipgu=010&slipymd=${slipymd}&slipno=${slipno}&deptcd=${formData.deptcd}`
			window.open(printUrl, '전표인쇄', 'left=10,top=10,width=700,height=650,scrollbars=yes')
			fetchUnissuedList()
		} else {
			vAlertError('발행 처리 중 오류가 발생했습니다.')
		}
	} catch (e: any) {
		vAlertError(e.response?.data?.message || e.message || '발행 실패')
	}
}

const toggleAllRows = () => {
	if (!mainGrid) return
	const rows = mainGrid.getRows()
	const allSelected = mainGrid.getSelectedRows().length === rows.length
	if (allSelected) mainGrid.deselectRow()
	else mainGrid.selectRow()
}

const initialize = () => {
	resetForm(searchForm);
	searchForm.deptcd = authStore.deptcd; searchForm.deptnm = authStore.deptnm;
	searchForm.fromdt = new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString().substring(0, 10);
	searchForm.todt = new Date().toISOString().substring(0, 10);

	formData.deptcd = authStore.deptcd; formData.deptnm = authStore.deptnm;
	slipForm.slipymd = new Date().toISOString().substring(0, 10);

	mainGrid?.clearData(); activeItemCount.value = 0;
	totals.spyamt = 0; totals.vatamt = 0; totals.jsansum = 0;
}

const modalVisible = ref(false);
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function openHelp(type: string) {
	if (type === 'DEPT' || type === 'ISSUE_DEPT') {
		Object.assign(modalProps, {
			title: '부서 선택', path: '/ha00/HA00_00P_STR', defaultField: 'deptnm',
			data: { gubun: 'D0', cmpycd: authStore.cmpycd },
			columns: [{ title: '코드', field: 'deptcd', width: 80 }, { title: '부서명', field: 'deptnm', width: 180 }],
			onConfirm: (d: any) => {
				if (type === 'DEPT') { searchForm.deptcd = d.deptcd; searchForm.deptnm = d.deptnm }
				else { formData.deptcd = d.deptcd; formData.deptnm = d.deptnm }
			}
		})
	}
	modalVisible.value = true
}

onMounted(async () => {
	api.get('/hs00/HS00_000S_STR', { params: { gubun: 'cl', cmpycd: authStore.cmpycd } }).then(r => {
		if (r.data?.length) {
            const d = r.data[0]
			closingInfo.clsymd = String(d.clsymd || '');
			closingInfo.sclsym = String(d.sclsym || '');
		}
	});

	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns', height: '100%', selectable: true,
			columnDefaults: { headerSort: false, headerHozAlign: "center", hozAlign: "center", vertAlign: "middle", minWidth: 100 },
			columns: [
				{
					title: "선택", formatter: "rowSelection", titleFormatter: "rowSelection", width: 40, hozAlign: "center",
					headerClick: () => toggleAllRows()
				},
				{ title: "정산일", field: "jsanymd", width: 150, formatter: (c) => {
                    const v = c.getValue() || c.getData().jsanymd;
                    return v && v.length === 8 ? `${v.substring(0,4)}-${v.substring(4,6)}-${v.substring(6,8)}` : v;
                }},
				{ title: "정산부서", field: "deptnm", width: 250 },
				{ title: "거래처", field: "custnm", minWidth: 200, hozAlign: "left", cssClass: "fw-bold" },
				{ title: "사업장", field: "unitnm", width: 200 },
				{ title: "유형", field: "vattypenm", width: 150 },
				{ title: "공급가액", field: "spyamt", hozAlign: "right", width: 150, formatter: "money", formatterParams: { precision: 0 } },
				{ title: "부가세", field: "vatamt", hozAlign: "right", width: 150, formatter: "money", formatterParams: { precision: 0 } },
				{ title: "합계금액", field: "jsansum", hozAlign: "right", width: 150, formatter: "money", cssClass: "text-primary fw-bold" }
			]
		})
		mainGrid.on('rowSelectionChanged', (data) => {
			activeItemCount.value = data.length
			totals.spyamt = data.reduce((acc, cur: any) => acc + (Number(cur.spyamt) || 0), 0)
			totals.vatamt = data.reduce((acc, cur: any) => acc + (Number(cur.vatamt) || 0), 0)
			totals.jsansum = data.reduce((acc, cur: any) => acc + (Number(cur.jsansum) || 0), 0)
		})
	}
})

const formatNumber = (val: any) => Number(val || 0).toLocaleString()
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; border-bottom: 3px solid #005a9f !important; }

/* 🚀 등록 영역 강조 스타일 */
.bg-light-primary { background-color: #f8fbff !important; }
</style>
