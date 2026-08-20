<!--	=============================================================
	프로그램명 : 부가세전산매체생성
	작성일자	: 2025.02.24
	작성자    : AI Assistant
	설명        : 부가가치세 신고를 위한 전산매체 파일 생성 및 다운로드 (HSOD100U 표준 UI 적용)
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container">
		<!-- [Header] 액션 바 -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-file-earmark-binary me-2 text-primary" style="font-size: 18px;"></i>
				세무관리<i class="bi bi-chevron-right mx-1 small opacity-50"></i>
				<span class="text-primary fw-bolder">부가세전산매체생성 (HATX050U)</span>
			</div>
			<div class="btn-group-erp d-flex gap-1">
				<button class="btn-erp btn-init" @click="initialize">
					초기화
				</button>
				<button class="btn-erp btn-save" @click="generateMedia">
					<i class="bi bi-gear-fill"></i> 전산매체 생성
				</button>
			</div>
		</div>

		<!-- [Content] 메인 콘텐츠 -->
		<div class="flex-grow-1 overflow-auto p-3 d-flex flex-column gap-3 bg-light main-content-wrapper">

			<!-- [Card] 생성 조건 설정 -->
			<div class="card border shadow-sm bg-white overflow-hidden" style="max-width: 800px; margin: 0 auto;">
				<div class="card-header bg-white py-2 px-3 border-bottom d-flex align-items-center">
					<span class="fw-bold small text-dark"><i class="bi bi-sliders me-1 text-primary"></i> 전산매체 생성 조건</span>
				</div>
				<div class="card-body p-4">
					<table class="erp-table-dense w-100 border">
						<colgroup>
							<col style="width: 120px;" /><col />
						</colgroup>
						<tbody>
							<tr>
								<th class="bg-light text-center py-3">사 업 장</th>
								<td class="p-2">
									<select v-model="searchForm.taxunit" class="form-select form-select-sm">
										<option v-for="opt in taxUnitOptions" :key="opt.code" :value="opt.code">{{ opt.name }}</option>
									</select>
								</td>
							</tr>
							<tr>
								<th class="bg-light text-center py-3">신고연도</th>
								<td class="p-2">
									<select v-model="searchForm.yy" class="form-select form-select-sm d-inline-block" style="width: 120px;">
										<option v-for="y in yearOptions" :key="y" :value="y">{{ y }}년</option>
									</select>
								</td>
							</tr>
							<tr>
								<th class="bg-light text-center py-3">신고기수</th>
								<td class="p-2">
									<div class="d-flex gap-4">
										<div class="form-check">
											<input class="form-check-input" type="radio" v-model="searchForm.gisu" value="1" id="gisu1">
											<label class="form-check-label small" for="gisu1">1 기 (1월~6월)</label>
										</div>
										<div class="form-check">
											<input class="form-check-input" type="radio" v-model="searchForm.gisu" value="2" id="gisu2">
											<label class="form-check-label small" for="gisu2">2 기 (7월~12월)</label>
										</div>
									</div>
								</td>
							</tr>
							<tr>
								<th class="bg-light text-center py-3">신고구분</th>
								<td class="p-2">
									<div class="d-flex gap-4">
										<div class="form-check">
											<input class="form-check-input" type="radio" v-model="searchForm.gbn" value="1" id="gbn1">
											<label class="form-check-label small" for="gbn1">예정 신고</label>
										</div>
										<div class="form-check">
											<input class="form-check-input" type="radio" v-model="searchForm.gbn" value="2" id="gbn2">
											<label class="form-check-label small" for="gbn2">확정 신고</label>
										</div>
									</div>
								</td>
							</tr>
						</tbody>
					</table>

					<div class="alert alert-info mt-4 mb-0 border-0 shadow-sm py-3">
						<div class="d-flex align-items-start gap-2">
							<i class="bi bi-info-circle-fill text-info" style="font-size: 1.2rem;"></i>
							<div class="small">
								<p class="mb-1 fw-bold">※ 전산매체 생성 안내</p>
								<ul class="mb-0 ps-3">
									<li>입력하신 조건에 따라 국세청 신고용 전산매체 파일이 생성됩니다.</li>
									<li>생성 완료 시 브라우저를 통해 텍스트 파일(.101)이 자동으로 다운로드됩니다.</li>
									<li>데이터 양에 따라 수 초의 시간이 소요될 수 있습니다.</li>
								</ul>
							</div>
						</div>
					</div>
				</div>
			</div>

		</div>
	</div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useAlerts } from '@/composables/useAlerts'
import AppAlert from '@/components/AppAlert.vue'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()

const now = new Date()
const currentYear = now.getFullYear()

const taxUnitOptions = ref<any[]>([])
const yearOptions = Array.from({ length: 6 }, (_, i) => String(currentYear - i))

const searchForm = reactive({
	taxunit: '000',
	yy: String(currentYear),
	gisu: '1',
	gbn: '1'
})

const fetchOptions = async () => {
	try {
        const res = await api.post('/ha00/HA00_00P_STR', { gubun: 'SA', cmpycd: authStore.cmpycd })
        taxUnitOptions.value = (res.data || []).map((i: any) => ({ code: i.taxunit, name: i.unitnm }))
        if (taxUnitOptions.value.length > 0) searchForm.taxunit = taxUnitOptions.value[0].code
	} catch (e) { console.error('사업장 정보 로드 실패', e) }
}

async function generateMedia() {
	if (!confirm('선택하신 조건으로 전산매체를 생성하시겠습니까?')) return

	try {
		vAlert('전산매체 생성 중입니다. 잠시만 기다려 주십시오...')

		const res = await api.post('/hatx/HATX_050U_STR', {
			actkind: 'A0',
			cmpycd: authStore.cmpycd,
			taxunit: searchForm.taxunit,
			yy: searchForm.yy,
			gisu: searchForm.gisu,
			gbn: searchForm.gbn
		})

		if (res.data && res.data.length > 0) {
			// 🚀 데이터 가공 (모든 행의 첫 번째 컬럼 값을 개행 문자로 합침)
			// 레거시 ASP 로직 상 Rs(0)에 데이터 라인이 들어있음
			const fileContent = res.data.map((row: any) => Object.values(row)[0]).join('\n')

			// 📂 파일 다운로드 처리
			const fileName = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}.101`
			const blob = new Blob([fileContent], { type: 'text/plain' })
			const link = document.createElement('a')
			link.href = URL.createObjectURL(blob)
			link.download = fileName
			link.click()
			URL.revokeObjectURL(link.href)

			vAlert('전산매체 파일이 생성 및 다운로드되었습니다.')
		} else {
			vAlertError('생성할 데이터가 존재하지 않습니다.')
		}

	} catch (e) {
		vAlertError('전산매체 생성 중 오류가 발생했습니다.')
		console.error(e)
	}
}

const initialize = () => {
	searchForm.yy = String(currentYear)
	searchForm.gisu = '1'
	searchForm.gbn = '1'
	if (taxUnitOptions.value.length > 0) searchForm.taxunit = taxUnitOptions.value[0].code
}

onMounted(() => {
	fetchOptions()
})
</script>

<style scoped>
.erp-table-dense th { width: 150px; font-weight: 600; font-size: 13px; }
.erp-table-dense td { font-size: 13px; }
.main-content-wrapper { min-height: 0; }
</style>
