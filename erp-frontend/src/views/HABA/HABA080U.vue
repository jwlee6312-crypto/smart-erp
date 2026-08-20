<!--
	=============================================================
	프로그램명 : 예산코드등록
	작성일자	: 2025.02.24
	작성자    : AI Assistant
	설명        : 비용구분별 예산코드 관리 (HABA080U)
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container">
		<!-- [헤더] 상단 액션 바 -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-calculator me-2 text-primary" style="font-size: 18px;"></i>
				기본정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
				<span class="text-primary fw-bolder">예산코드등록 (HABA080U)</span>
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
						<col style="width: 100px;" /><col style="width: 200px;" />
						<col style="width: 100px;" /><col style="width: 200px;" />
						<col />
					</colgroup>
					<tbody>
						<tr>
							<th class="text-center border-end">비용구분</th>
							<td class="bg-white border-end px-2">
								<select v-model="searchForm.costtype" class="form-select form-select-sm">
									<option value="0">선택</option>
									<option v-for="opt in costTypeOptions" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
								</select>
							</td>
							<th class="text-center border-end">예산명</th>
							<td class="bg-white border-end px-2">
								<input v-model="searchForm.bugtnm" type="text" class="form-control form-control-sm" placeholder="예산명 입력" @keydown.enter="search" />
							</td>
							<td class="bg-white px-3 text-muted small">
								<i class="bi bi-info-circle me-1"></i> 검색할 비용구분 또는 예산명을 입력해 주십시오.
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
					<span class="small fw-bold text-secondary"><i class="bi bi-pencil-square me-1"></i> 예산코드 상세 정보 [{{ masterForm.actkind === 'A0' ? '신규' : '수정' }}]</span>
				</div>
				<table class="erp-table-full small">
					<colgroup>
						<col style="width: 100px;" /><col style="width: 20%;" />
						<col style="width: 100px;" /><col style="width: 20%;" />
						<col style="width: 100px;" /><col />
					</colgroup>
					<tbody>
						<tr>
							<th class="text-center bg-light-subtle border-end">예산코드</th>
							<td class="bg-white border-end px-2 py-1">
								<input v-model="masterForm.bugtcd" type="text" class="form-control form-control-sm" maxlength="3" :readonly="masterForm.actkind === 'U0'" />
							</td>
							<th class="text-center bg-light-subtle border-end border-top">예산명</th>
							<td class="bg-white border-end border-top px-2 py-1">
								<input v-model="masterForm.bugtnm" type="text" class="form-control form-control-sm" maxlength="50" />
							</td>
							<th class="text-center bg-light-subtle border-end border-top">비용구분</th>
							<td class="bg-white border-top px-2 py-1">
								<select v-model="masterForm.costtype" class="form-select form-select-sm">
									<option value="0">선택</option>
									<option v-for="opt in costTypeOptions" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
								</select>
							</td>
						</tr>
						<tr>
							<th class="text-center bg-light-subtle border-end border-top">비   고</th>
							<td colspan="3" class="bg-white border-end border-top px-2 py-1">
								<input v-model="masterForm.bigo" type="text" class="form-control form-control-sm" maxlength="100" />
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
							<td colspan="6" class="bg-light-subtle px-3 py-1 text-danger small">
								<i class="bi bi-info-circle-fill me-1"></i> 판매관리비 관련 예산코드는 <span class="fw-bold">100 ~ 490</span>, 제조 비용 관련 예산코드는 <span class="fw-bold">500 ~ 990</span> 으로 부여한다.
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
import { ref, reactive, onMounted, nextTick } from 'vue'
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

// 검색 폼 데이터
const searchForm = reactive({
	costtype: '0',
	bugtnm: ''
})

// 마스터 폼 데이터
const masterForm = reactive({
	actkind: 'A0',
	bugtcd: '',
	bugtnm: '',
	costtype: '0',
	bigo: '',
	useyn: 'Y'
})

const costTypeOptions = ref<any[]>([])
const mainGridRef = ref<HTMLDivElement | null>(null)
let mainGrid: Tabulator | null = null

// 초기 데이터 로드
const loadInitData = async () => {
	try {
		// 비용구분 로드
		const res = await api.post('/ha00/HA00_00P_STR', {
			gubun: 'E0',
			gbncd: '110',
			cmpycd: authStore.cmpycd
		})
		costTypeOptions.value = res.data || []

		// 페이지 초기 프로세스 실행
		await api.post('/haba/HABA_080U_STR', {
			actkind: 'A1',
			cmpycd: authStore.cmpycd
		})
	} catch (e) {
		console.error('Initial data load failed:', e)
	}
}

const search = async () => {
	if (searchForm.costtype === '0' && !searchForm.bugtnm) {
		return vAlert('검색할 비용구분 또는 예산명을 기입해주십시오.')
	}
	try {
		const res = await api.post('/haba/HABA_080U_STR', {
			actkind: 'S0',
			cmpycd: authStore.cmpycd,
			bugtcd: '',
			bugtnm: searchForm.bugtnm,
			costtype: searchForm.costtype,
			bigo: '',
			useyn: ''
		})

		const processedData = (res.data || []).map((n: any) => {
			return {
				bugtcd: n.bugtcd,
				bugtnm: n.bugtnm,
				costtype: n.costtype,
				costtypenm: n.costtypenm,
				bigo: n.bigo,
				useyn: n.useyn
			}
		})

		mainGrid?.setData(processedData)
		vAlert('조회되었습니다.')
	} catch (e) {
		vAlertError('조회 중 오류 발생')
	}
}

const save = async () => {
	if (!masterForm.bugtcd) return vAlert('예산코드를 기재해 주십시오.')
	if (!masterForm.bugtnm) return vAlert('예산명을 기재해 주십시오.')
	if (masterForm.costtype === '0') return vAlert('비용구분을 선택해 주십시오.')

	try {
		const payload = {
			...masterForm,
			cmpycd: authStore.cmpycd,
			userid: authStore.userid
		}

		const res = await api.post('/haba/HABA_080U_STR', payload)
        const resData = res.data?.[0] || {};

		if (resData.ret_yn === 'Y') {
			vAlertError(resData.ret_msg || '저장 실패')
		} else {
			vAlert('정상적으로 작업되었습니다.')
			search()
			initialize()
		}
	} catch (e) {
		vAlertError('저장 실패')
	}
}

const initialize = () => {
	resetForm(masterForm)
	masterForm.actkind = 'A0'
	masterForm.costtype = searchForm.costtype
	masterForm.useyn = 'Y'
}

onMounted(async () => {
	await loadInitData()

	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns',
			height: '100%',
			columnDefaults: { headerSort: false, vertAlign: "middle" },
			columns: [
				{ title: "예산코드", field: "bugtcd", width: 120, hozAlign: "center", cssClass: "fw-bold text-primary" },
				{ title: "예산명", field: "bugtnm", minWidth: 200 },
				{ title: "비용구분", field: "costtypenm", width: 150, hozAlign: "center" },
				{ title: "비고", field: "bigo", minWidth: 250 },
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
			masterForm.actkind = 'U0'
			masterForm.bugtcd = d.bugtcd
			masterForm.bugtnm = d.bugtnm
			masterForm.costtype = d.costtype
			masterForm.bigo = d.bigo
			masterForm.useyn = d.useyn
		})

		nextTick(() => search())
	}
})
</script>

<style scoped>
.bg-light-subtle { background-color: #f8f9fa !important; }
:deep(.tabulator-row) { cursor: pointer; }
:deep(.tabulator-row:hover) { background-color: #f0f7ff !important; }
</style>
