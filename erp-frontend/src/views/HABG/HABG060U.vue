<!--
	=============================================================
	프로그램명	: 예산일괄배정 (HABG060U)
	작성일자	: 2025.02.24
	작성자	    : AI Assistant
	설명        : 부서 범위 및 기간 범위를 지정하여 예산을 일괄 배정 또는 취소
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container d-flex flex-column h-100 bg-white">
		<!-- 🚀 상단 액션 바 (표준 규격) -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-stack me-2 text-primary" style="font-size: 18px;"></i>
				예산관리 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				<span class="text-primary fw-bolder">예산일괄배정 (HABG060U)</span>
			</div>
			<div class="btn-group-erp d-flex gap-1 pe-3">
				<button class="btn-erp btn-save" @click="save">
					<i class="bi bi-play-fill"></i> 작업실행
				</button>
			</div>
		</div>

		<!-- 💡 메인 컨텐츠 영역 -->
		<div class="flex-grow-1 overflow-auto p-4 bg-light main-content-wrapper">
			<div class="card border shadow-sm mx-auto" style="max-width: 800px;">
				<div class="card-header bg-white py-2 px-3 border-bottom">
					<h6 class="mb-0 fw-bold small text-dark"><i class="bi bi-gear-fill me-2 text-primary"></i>배정 작업 조건 설정</h6>
				</div>
				<div class="card-body p-4 bg-white">
					<div class="alert alert-info py-2 px-3 mb-4 small d-flex align-items-center border-0 shadow-sm" style="background-color: #f0f9ff; color: #0369a1;">
						<i class="bi bi-info-circle-fill me-2 fs-5"></i>
						<span>조정금액을 일괄 배정합니다. 추가/조기/이월/전용 금액은 자동으로 배정작업에 포함되지 않습니다.</span>
					</div>

					<div class="row g-4">
						<!-- 예산년월 범위 -->
						<div class="col-12">
							<div class="d-flex align-items-center">
								<span class="erp-label" style="min-width: 100px;">예산년월</span>
								<div class="d-flex align-items-center gap-2 flex-grow-1">
									<div class="d-flex gap-1 flex-grow-1">
										<select v-model="form.bugtyyf" class="form-select form-select-sm">
											<option v-for="year in yearOptions" :key="year" :value="year">{{ year }}년</option>
										</select>
										<select v-model="form.bugtmmf" class="form-select form-select-sm">
											<option v-for="opt in periodOptions" :key="opt.value" :value="opt.value">{{ opt.text }}</option>
										</select>
									</div>
									<span class="text-muted fw-bold">~</span>
									<div class="d-flex gap-1 flex-grow-1">
										<select v-model="form.bugtyyt" class="form-select form-select-sm">
											<option v-for="year in yearOptions" :key="year" :value="year">{{ year }}년</option>
										</select>
										<select v-model="form.bugtmmt" class="form-select form-select-sm">
											<option v-for="opt in periodOptions" :key="opt.value" :value="opt.value">{{ opt.text }}</option>
										</select>
									</div>
								</div>
							</div>
						</div>

						<!-- 예산부서 범위 -->
						<div class="col-12">
							<div class="d-flex align-items-center">
								<span class="erp-label" style="min-width: 100px;">예산부서</span>
								<div class="d-flex align-items-center gap-2 flex-grow-1">
									<div class="input-group input-group-sm flex-grow-1">
										<input v-model="form.deptcdf" type="text" class="form-control text-center bg-light" style="max-width: 65px;" readonly />
										<input v-model="form.deptnmf" type="text" class="form-control" @keydown.enter="openHelp('DEPTF')" placeholder="시작 부서" />
										<button class="btn btn-outline-secondary" @click="openHelp('DEPTF')"><i class="bi bi-search"></i></button>
									</div>
									<span class="text-muted fw-bold">~</span>
									<div class="input-group input-group-sm flex-grow-1">
										<input v-model="form.deptcdt" type="text" class="form-control text-center bg-light" style="max-width: 65px;" readonly />
										<input v-model="form.deptnmt" type="text" class="form-control" @keydown.enter="openHelp('DEPTT')" placeholder="종료 부서" />
										<button class="btn btn-outline-secondary" @click="openHelp('DEPTT')"><i class="bi bi-search"></i></button>
									</div>
								</div>
							</div>
						</div>

						<!-- 작업방법 -->
						<div class="col-12">
							<div class="d-flex align-items-center">
								<span class="erp-label" style="min-width: 100px;">작업방법</span>
								<select v-model="form.wkgbn" class="form-select form-select-sm flex-grow-1 fw-bold">
									<option value="Y">일괄배정 작업을 합니다.</option>
									<option value="N">일괄배정 취소작업을 합니다.</option>
								</select>
							</div>
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>

	<Modal v-model:visible="modalVisible" :modalProps="modalProps" />
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useAlerts } from '@/composables/useAlerts'
import AppAlert from '@/components/AppAlert.vue'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import Modal from '@/components/Modal.vue'
import type { ModalProps } from '@/types/modal'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()

//const currentYear = new Date().getFullYear()
const currentYear = 2011
const yearOptions = Array.from({ length: 7 }, (_, i) => String(currentYear + 1 - i))

const bgType = ref('010')

const form = reactive({
	bugtyyf: String(currentYear + 1),
	bugtmmf: '01',
	bugtyyt: String(currentYear + 1),
	bugtmmt: String(new Date().getMonth() + 1).padStart(2, '0'),
	deptcdf: '',
	deptnmf: '',
	deptcdt: '',
	deptnmt: '',
	wkgbn: 'Y'
})

const periodOptions = computed(() => {
	if (bgType.value === '020') {
		return [
			{ value: '01', text: '01 분기' }, { value: '04', text: '02 분기' },
			{ value: '07', text: '03 분기' }, { value: '10', text: '04 분기' }
		]
	} else if (bgType.value === '030') {
		return [{ value: '01', text: '상반기' }, { value: '07', text: '하반기' }]
	} else if (bgType.value === '040') {
		return [{ value: '01', text: '전체' }]
	}
	return Array.from({ length: 12 }, (_, i) => ({
		value: String(i + 1).padStart(2, '0'),
		text: `${String(i + 1).padStart(2, '0')} 월`
	}))
})

const save = async () => {
	if (!form.deptcdf || !form.deptcdt) return vAlertError('예산부서 범위를 지정해 주십시오.')

	const msg = form.wkgbn === 'Y' ? '배정 작업을 하시겠습니까?' : '배정 취소작업을 하시겠습니까?'
	if (!confirm(msg)) return

	try {
		const res = await api.post('/habg/HABG_060U_STR', {
			cmpycd: authStore.cmpycd,
			bugtymf: `${form.bugtyyf}${form.bugtmmf}`,
			bugtymt: `${form.bugtyyt}${form.bugtmmt}`,
			deptcdf: form.deptcdf,
			deptcdt: form.deptcdt,
			wkgbn: form.wkgbn
		})

		if (res.data?.[0]?.col0 === '000') {
			vAlert('작업이 완료되었습니다.')
		} else {
			vAlertError(res.data?.[0]?.col1 || '작업 중 오류가 발생했습니다.')
		}
	} catch (e) { vAlertError('서버 통신 오류') }
}

const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function openHelp(type: 'DEPTF' | 'DEPTT') {
	const searchVal = type === 'DEPTF' ? form.deptnmf : form.deptnmt
    Object.assign(modalProps, {
        title: '부서 선택', path: '/ha00/HA00_00P_STR', defaultField: 'deptnm',
        data: { gubun: 'D0', cmpycd: authStore.cmpycd, code: searchVal },
        columns: [{ title: '코드', field: 'deptcd', width: 80 }, { title: '부서명', field: 'deptnm', width: 180 }],
        onConfirm: (d: any) => {
			if (type === 'DEPTF') { form.deptcdf = d.deptcd; form.deptnmf = d.deptnm }
			else { form.deptcdt = d.deptcd; form.deptnmt = d.deptnm }
		}
    })
	modalVisible.value = true
}

const fetchBgType = async () => {
	try {
		const res = await api.post('/ha00/HA00_00P_STR', { gubun: 'E0', cmpycd: ' ', gbncd: '200' })
		if (res.data && res.data.length > 0) {
			bgType.value = res.data[0].remark || '010'
		} else {
			bgType.value = '010'
		}
	} catch (e) { bgType.value = '010' }
}

onMounted(() => {
	fetchBgType()
	form.deptcdf = authStore.deptcd || ''
	form.deptnmf = authStore.deptnm || ''
	form.deptcdt = authStore.deptcd || ''
	form.deptnmt = authStore.deptnm || ''
})
</script>

<style scoped>
.main-content-wrapper { background-color: #f8f9fa; }
</style>
