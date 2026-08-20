<!--
	=============================================================
	프로그램명	: 매출전표취소 (HSIO540U)
	작성일자	: 2025.02.24
	설명        : 발행된 매출 전표 조회 및 취소 처리 (ASP 패턴 기반 순차 저장 로직 및 소문자 통일)
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container">
		<!-- 🚀 1. 상단 액션 바 -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-file-earmark-x-fill me-2 text-danger" style="font-size: 18px;"></i>
				영업정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				매출관리 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				<span class="text-primary fw-bolder">매출전표취소 (HSIO540U)</span>
			</div>
			<div class="btn-group-erp d-flex gap-1">
				<button class="btn-erp btn-init" @click="initialize">
					<i class="bi bi-arrow-clockwise"></i> 초기화
				</button>
				<button class="btn-erp btn-search" @click="fetchIssuedList">
					<i class="bi bi-search"></i> 조회
				</button>
				<button class="btn-erp btn-delete" @click="handleCancelSlip">
					<i class="bi bi-x-circle"></i> 전표 취소
				</button>
			</div>
		</div>

		<!-- 🔍 2. 최상단 검색 항목 영역 -->
		<div class="p-2 pb-0 flex-shrink-0">
			<div class="card border shadow-sm overflow-hidden">
				<table class="erp-table-full" style="table-layout: fixed;">
					<colgroup>
						<col style="width: 50%;" />
						<col style="width: 50%;" />
					</colgroup>
					<tbody>
						<tr>
							<td>
								<div class="d-flex align-items-center px-2">
									<span class="erp-label me-2">판매부서</span>
									<div class="input-group input-group-sm flex-nowrap" style="max-width: 300px;">
										<input v-model="searchForm.deptcd" type="text" class="form-control text-center bg-white" style="max-width: 60px;" readonly />
										<input v-model="searchForm.deptnm" type="text" class="form-control" placeholder="부서 선택" @keyup.enter="openHelp('DEPT')" />
										<button class="btn btn-outline-secondary px-2" @click="openHelp('DEPT')"><i class="bi bi-search"></i></button>
									</div>
								</div>
							</td>
							<td>
								<div class="d-flex align-items-center px-2">
									<span class="erp-label me-2">발행일자</span>
									<div class="d-flex align-items-center gap-1 flex-grow-1" style="max-width: 320px;">
										<input v-model="searchForm.fromdt" type="date" class="form-control form-control-sm" />
										<span class="text-muted">~</span>
										<input v-model="searchForm.todt" type="date" class="form-control form-control-sm" />
									</div>
                                    <div class="ms-auto d-flex align-items-center gap-2 bg-white border rounded px-2 py-1 shadow-sm">
                                      <div class="form-check form-switch mb-0">
                                        <input v-model="autoSlip" class="form-check-input" type="checkbox" id="autoSlipCheck" true-value="Y" false-value="N">
                                        <label class="form-check-label small fw-bold text-primary" for="autoSlipCheck">자동전표취소</label>
                                      </div>
                                    </div>
								</div>
							</td>
						</tr>
					</tbody>
				</table>
			</div>
		</div>

		<!-- 📊 3. 중앙: 취소 대상 그리드 -->
		<div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2">
			<div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white">
				<div class="card-header bg-white py-2 px-3 border-bottom d-flex align-items-center justify-content-between">
					<span class="fw-bold small text-dark d-flex align-items-center">
						<i class="bi bi-list-check me-2 text-primary"></i> 전표 취소 대상 목록
					</span>
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

// 상태 관리 (소문자 통일)
const searchForm = reactive({
	deptcd: authStore.deptcd, deptnm: authStore.deptnm,
	fromdt: new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString().substring(0, 10),
	todt: new Date().toISOString().substring(0, 10)
})

const autoSlip = ref('N')
const closingInfo = reactive({ clsymd: '', sclsym: '' })
const activeItemCount = ref(0)
const totals = reactive({ sum: 0 })

const mainGridRef = ref<HTMLDivElement | null>(null); let mainGrid: Tabulator | null = null

const fetchIssuedList = async () => {
	try {
		const res = await api.post('/hsio/HSIO_540U_STR', {
			actkind: 'S0',
			cmpycd: authStore.cmpycd,
            iogbn: '200',
			fromdt: searchForm.fromdt.replace(/-/g, ''),
			todt: searchForm.todt.replace(/-/g, ''),
            deptcd: searchForm.deptcd
		})

		// 🚀 백엔드 공통 처리에서 데이터가 없을 때 반환하는 [{res: 'OK'}] 필터링
		const rawData = (res.data || []).filter((row: any) => !(row.res === 'OK' || row.RES === 'OK'))

		mainGrid?.setData(rawData.map((row: any) => {
			const item = Object.fromEntries(Object.entries(row).map(([k, v]) => [k.toLowerCase(), v]))
			item.jsansum = (Number(item.spyamt) || 0) + (Number(item.vatamt) || 0)
			item.slip_full = item.slipymd > '00000000' ? `${item.slipymd}-${item.slipno}` : ''
			return item
		}))
		vAlert(rawData.length > 0 ? '조회되었습니다.' : '조회된 내역이 없습니다.')
	} catch (e) { vAlertError('조회 실패') }
}

/**
 * 🚀 전표 취소 처리 (ASP 로직 반영)
 */
const handleCancelSlip = async () => {
	const selected = mainGrid?.getSelectedData()
	if (!selected || selected.length === 0) return vAlertError('취소할 전표를 선택하세요.')

	if (!confirm('선택한 전표를 삭제(취소) 하시겠습니까?')) return
	try {
        const payload = {
            cmpycd: authStore.cmpycd,
            fromdt: searchForm.fromdt,
            todt: searchForm.todt,
            autoSlip: autoSlip.value,
            items: selected.map(item => ({
                slipymd: item.slipymd,
                slipno: item.slipno,
                deptcd: item.deptcd
            }))
        }

        const res = await api.post('/hsio/HSIO_540U_CANCEL', payload)

        if (res.data?.status === 'SUCCESS' || res.data?.res === 'OK') {
            vAlert('정상적으로 처리되었습니다.')
            fetchIssuedList()
        } else {
            vAlertError(res.data?.message || '취소 실패')
        }
	} catch (e: any) {
        vAlertError(e.response?.data?.message || e.message || '오류 발생')
    }
}

const toggleAllRows = () => {
	if (!mainGrid) return
	const rows = mainGrid.getRows()
	const selectableRows = rows.filter(row => {
		const d = row.getData();
		return (d.cfmyn || d.CFMYN || 'N') !== 'Y';
	})
	const allSelected = mainGrid.getSelectedRows().length === selectableRows.length
	if (allSelected) mainGrid.deselectRow()
	else mainGrid.selectRow()
}

const initialize = () => {
	resetForm(searchForm);
	searchForm.deptcd = authStore.deptcd; searchForm.deptnm = authStore.deptnm;
	searchForm.fromdt = new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString().substring(0, 10);
	searchForm.todt = new Date().toISOString().substring(0, 10);
	mainGrid?.clearData(); activeItemCount.value = 0; totals.sum = 0;
}

const modalVisible = ref(false);
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function openHelp(type: string) {
	if (type === 'DEPT') {
		Object.assign(modalProps, {
			title: '부서 선택', path: '/ha00/HA00_00P_STR', defaultField: 'deptnm',
			data: { gubun: 'D0', cmpycd: authStore.cmpycd },
			columns: [{ title: '코드', field: 'deptcd', width: 80 }, { title: '부서명', field: 'deptnm', width: 180 }],
			onConfirm: (d: any) => { searchForm.deptcd = d.deptcd; searchForm.deptnm = d.deptnm }
		})
	}
	modalVisible.value = true
}

onMounted(async () => {
	api.get('/hs00/HS00_000S_STR', { params: { gubun: 'CL', cmpycd: authStore.cmpycd } }).then(r => {
		if (r.data?.length) {
            const d = r.data[0]
			closingInfo.clsymd = String(d.clsymd || d.CLSYMD || '');
			closingInfo.sclsym = String(d.sclsym || d.SCLSYM || '');
		}
	});

	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns', height: '100%', selectable: true,
			selectableCheck: (row) => {
				const d = row.getData();
				return (d.cfmyn || d.CFMYN || 'N') !== 'Y';
			},
			columnDefaults: { headerSort: false, headerHozAlign: "center", hozAlign: "center", vertAlign: "middle", minWidth: 100 },
			columns: [
				{
					title: "선택", formatter: "rowSelection", titleFormatter: "rowSelection", width: 80, hozAlign: "center",
					headerClick: () => toggleAllRows()
				},
				{ title: "전표번호", field: "slip_full", width: 160, cssClass: "fw-bold text-primary",
				  formatter: (cell) => {
					  const d = cell.getData();
                      const slipymd = d.slipymd || d.slipymd;
                      const slipno = d.slipno || d.slipno;
					  return slipymd && slipno ? `${slipymd}-${slipno}` : '';
				  }
				},
				{
					title: "회계확정", field: "cfmyn", width: 100,
					formatter: (cell) => {
						const d = cell.getData();
						const val = d.cfmyn || d.CFMYN || 'N';
						return val === 'Y' ? '<span class="badge bg-danger">확정</span>' : '<span class="badge bg-secondary">미확정</span>';
					}
				},
				{ title: "발행부서", field: "deptnm", width: 250 },
				{ title: "거래처", field: "custnm", minWidth: 200, hozAlign: "left", cssClass: "fw-bold" },
				{ title: "유형", field: "vattypenm", width: 150 },
				{ title: "공급가액", field: "spyamt", hozAlign: "right", width: 150, formatter: "money", formatterParams: { precision: 0 } },
				{ title: "부가세", field: "vatamt", hozAlign: "right", width: 150, formatter: "money", formatterParams: { precision: 0 } },
				{ title: "합계금액", field: "jsansum", hozAlign: "right", width: 150, formatter: "money", cssClass: "text-danger fw-bold",
				  mutatorData: (v,d) => Number(d.spyamt||0) + Number(d.vatamt||0)
				}
			]
		})
		mainGrid.on('rowSelectionChanged', (data) => {
			activeItemCount.value = data.length
			totals.sum = data.reduce((acc, cur: any) => acc + (Number(cur.spyamt||0) + Number(cur.vatamt||0)), 0)
		})
	}
})
</script>
