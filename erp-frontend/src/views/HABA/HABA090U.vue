<!--
	=============================================================
	프로그램명 : 관리번호 관리
	작성일자	: 2025.02.24
	작성자    : AI Assistant
	설명        : 계정과목의 관리번호(유형별 기타 코드)를 등록하여 관리
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container">
		<!-- [헤더] 상단 액션 바 -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-gear-wide-connected me-2 text-primary" style="font-size: 18px;"></i>
				기본정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				<span class="text-primary fw-bolder">관리번호 관리 (HABA090U)</span>
			</div>
			<div class="btn-group-erp d-flex gap-1">
				<button class="btn-erp btn-init" @click="initialize">
					<i class="bi bi-plus-lg"></i> 신규
				</button>
				<button class="btn-erp btn-search" @click="search">
					<i class="bi bi-search"></i> 조회
				</button>
				<button class="btn-erp btn-save" @click="save">
					<i class="bi bi-check-lg"></i> 저장
				</button>
			</div>
		</div>

		<!-- [조회] 검색 조건 영역 -->
		<div class="p-2 pb-0 flex-shrink-0">
			<div class="card border shadow-sm overflow-hidden bg-light">
				<table class="erp-table-full" style="table-layout: fixed;">
					<colgroup>
						<col style="width: 100px;" /><col style="width: 250px;" />
						<col />
					</colgroup>
					<tbody>
						<tr>
							<th class="text-center border-end">관리번호명</th>
							<td class="bg-white border-end px-2">
								<input v-model="searchForm.mgtnm_h" type="text" class="form-control form-control-sm" placeholder="검색어 입력" @keydown.enter="search" />
							</td>
							<td class="bg-white px-3 text-muted small">
								<i class="bi bi-info-circle me-1"></i> 검색하고자 하는 관리번호명을 입력해 주십시오.
							</td>
						</tr>
					</tbody>
				</table>
			</div>
		</div>

		<!-- [상세] 상세 정보 입력 영역 -->
		<div class="p-2 pb-0 flex-shrink-0">
			<div class="card border shadow-sm bg-white overflow-hidden">
				<div class="card-header py-1 px-2 bg-light border-bottom">
					<span class="small fw-bold text-secondary"><i class="bi bi-pencil-square me-1"></i> 관리번호 상세 정보 [{{ masterForm.actkind === 'A0' ? '신규' : '수정' }}]</span>
				</div>
				<table class="erp-table-full small">
					<colgroup>
						<col style="width: 100px;" /><col style="width: 20%;" />
						<col style="width: 100px;" /><col />
						<col style="width: 100px;" /><col style="width: 15%;" />
					</colgroup>
					<tbody>
						<tr>
							<th class="text-center bg-light-subtle border-end">관리번호</th>
							<td class="bg-white border-end px-2 py-1">
								<input v-model="masterForm.mgtno" type="text" class="form-control form-control-sm" maxlength="5" :readonly="masterForm.actkind === 'U0'" />
							</td>
							<th class="text-center bg-light-subtle border-end border-top">관리번호명</th>
							<td class="bg-white border-end border-top px-2 py-1">
								<input v-model="masterForm.mgtnm" type="text" class="form-control form-control-sm" maxlength="30" />
							</td>
							<th class="text-center bg-light-subtle border-end border-top">출현순서</th>
							<td class="bg-white border-top px-2 py-1">
								<input v-model="masterForm.dspord" type="text" class="form-control form-control-sm text-end" maxlength="3" />
							</td>
						</tr>
						<tr>
							<th class="text-center bg-light-subtle border-end border-top">비   고</th>
							<td colspan="3" class="bg-white border-end border-top px-2 py-1">
								<input v-model="masterForm.remark" type="text" class="form-control form-control-sm" maxlength="50" />
							</td>
							<th class="text-center bg-light-subtle border-end border-top">사용여부</th>
							<td class="bg-white border-top px-3">
								<div class="form-check pt-1">
									<input v-model="masterForm.useyn" class="form-check-input" type="checkbox" id="useCheck" true-value="Y" false-value="N">
									<label class="form-check-label small fw-bold" for="useCheck">사용</label>
								</div>
							</td>
						</tr>
						<tr>
							<td colspan="6" class="bg-light-subtle px-3 py-1 text-primary small">
								<i class="bi bi-info-circle-fill me-1"></i> 계정과목의 관리번호 유형 및 기타 분류 코드를 등록하여 관리한다.
							</td>
						</tr>
					</tbody>
				</table>
			</div>
		</div>

		<!-- [그리드] 데이터 그리드 영역 -->
		<div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column">
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
import { useAlerts } from '@/composables/useAlerts'
import AppAlert from '@/components/AppAlert.vue'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

// 검색 필터 데이터
const searchForm = reactive({
	mgtnm_h: ''
})

// 입력 폼 데이터
const masterForm = reactive({
	actkind: 'A0',
	mgtno: '',
	mgtnm: '',
	remark: '',
	dspord: '',
	useyn: 'Y'
})

const mainGridRef = ref<HTMLDivElement | null>(null)
let mainGrid: Tabulator | null = null

const search = async () => {
	try {
		const res = await api.post('/haba/HABA_090U_STR', {
			actkind: 'S0',
			cmpycd: authStore.cmpycd,
			mgtnm: searchForm.mgtnm_h
		})

		const processedData = (res.data || []).map((n: any) => {
            return {
                mgtno: n.mgtno,
                mgtnm: n.mgtnm,
                remark: n.bigo || n.remark,
                dspord: n.dspord,
                useyn: n.useyn
            }
        })

		mainGrid?.setData(processedData)
		vAlert('조회되었습니다.')
	} catch (e) { vAlertError('조회 중 오류 발생') }
}

const save = async () => {
	if (!masterForm.mgtno) return vAlert('관리번호 코드를 기재해 주십시오.')
	if (!masterForm.mgtnm) return vAlert('관리번호명을 기재해 주십시오.')
	if (!masterForm.dspord) return vAlert('출현순서를 기재해 주십시오.')

	try {
		const payload = {
			...masterForm,
			cmpycd: authStore.cmpycd,
			userid: authStore.userid
		}

		const res = await api.post('/haba/HABA_090U_STR', payload)
        const resData = res.data?.[0] || {};

		if (resData.ret_yn === 'Y') {
			vAlertError(resData.ret_msg || '저장 실패')
		} else {
			vAlert('정상적으로 처리되었습니다.')
			search()
			initialize()
		}
	} catch (e) { vAlertError('저장 중 오류 발생') }
}

const initialize = () => {
	resetForm(masterForm)
	masterForm.actkind = 'A0'
	masterForm.useyn = 'Y'
}

onMounted(() => {
	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns',
			height: '100%',
			columnDefaults: { headerSort: false, vertAlign: "middle" },
			columns: [
				{ title: "관리번호", field: "mgtno", width: 100, hozAlign: "center", cssClass: "fw-bold text-primary" },
				{ title: "관리번호명", field: "mgtnm", width: 250 },
				{ title: "비고", field: "remark", minWidth: 200 },
				{ title: "순서", field: "dspord", width: 100, hozAlign: "center" },
                { title: "사용", field: "useyn", width: 80, hozAlign: "center",
                  formatter: (cell) => {
                    const val = String(cell.getValue() || '').trim().toUpperCase();
                    return val === 'Y' ? '<b class="text-primary">사용</b>' : '';
                  }
                }
			]
		})

		mainGrid.on("rowClick", (e, row) => {
			const d = row.getData()
			Object.assign(masterForm, d)
			masterForm.actkind = 'U0'
		})
	}
    search()
})
</script>

<style scoped>
.erp-label { min-width: 80px; font-weight: 500; font-size: 13px; }
.bg-light-subtle { background-color: #f8f9fa !important; }
:deep(.tabulator-row) { cursor: pointer; }
:deep(.tabulator-row:hover) { background-color: #f0f7ff !important; }
</style>
