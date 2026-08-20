<!--기본정보/환경설정 [ERP 프리미엄 고밀도 표준 - 최종 보정안] -->
<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container">
		<!-- 📢 시스템 공지 바 (작업 중 안내) -->
		<div class="alert alert-warning m-0 py-1 px-3 border-0 border-bottom rounded-0 d-flex align-items-center shadow-sm" style="font-size: 12px; background-color: #fff9db;">
			<i class="bi bi-exclamation-triangle-fill me-2 text-warning"></i>
			<span class="fw-bold text-dark">시스템 고도화 작업 안내:</span>
			<span class="ms-2">현재 환경설정 기능 표준화 작업이 진행 중입니다. 일부 기능 사용 시 주의하시기 바랍니다.</span>
		</div>

		<!-- 🚀 1. 상단 액션 바 -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 shadow-sm sticky-top">
			<div class="fw-bold ps-3 text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-gear-wide-connected me-2 text-primary" style="font-size: 18px;"></i>
				기본정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
				<span class="text-primary fw-bolder">결재 및 회사 환경설정 (HABA100U)</span>
			</div>
			<div class="btn-group-erp pe-3">
				<button class="btn-erp btn-init" @click="initialize">초기화</button>
				<button class="btn-erp btn-search" @click="fetchConfig">조회</button>
				<button class="btn-erp btn-save" @click="save">저장</button>
			</div>
		</div>

		<!-- 💡 2. 메인 설정 영역 (3열 밸런스 배치) -->
		<div class="flex-grow-1 overflow-auto p-3 d-flex flex-column gap-3">

			<!-- 🅰️ 결재 및 마감 통제 -->
			<div class="card border-0 shadow-sm overflow-hidden">
				<div class="card-header bg-white py-2 px-3 border-bottom d-flex align-items-center">
					<i class="bi bi-pen-fill me-2 text-secondary"></i>
					<span class="fw-bold small text-dark">결재 및 마감 통제 설정</span>
				</div>
				<div class="card-body p-0">
					<table class="erp-table-full border-0">
						<colgroup>
							<col style="width: 120px;" /><col />
							<col style="width: 120px;" /><col />
							<col style="width: 120px;" /><col />
						</colgroup>
						<tbody>
							<tr>
								<th class="required">마감 기준일</th>
								<td>
									<div class="d-flex align-items-center gap-1 flex-nowrap">
										<input v-model="uiDate.yy" type="text" class="form-control text-center fw-bold" style="width: 70px;" maxlength="4" />
										<span class="small">년</span>
										<input v-model="uiDate.mm" type="text" class="form-control text-center fw-bold" style="width: 50px;" maxlength="2" />
										<span class="small">월</span>
										<input v-model="uiDate.dd" type="text" class="form-control text-center fw-bold" style="width: 50px;" maxlength="2" />
										<span class="small">일</span>
									</div>
								</td>
								<th>장부 반영</th>
								<td>
									<select v-model="formData.slipyn" class="form-select">
										<option value="N">미사용 (승인 후 반영)</option>
										<option value="Y">즉시반영 (자동 발행 시)</option>
									</select>
								</td>
								<th>비용예산 통제</th>
								<td>
									<select v-model="formData.bgtype" class="form-select">
										<option value="000">관리안함</option>
										<option v-for="opt in bgOptions" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
									</select>
								</td>
							</tr>
							<tr>
								<th>결재라인 명칭</th>
								<td colspan="5">
									<div class="d-flex gap-2 flex-nowrap">
										<div class="input-group input-group-sm flex-nowrap" v-for="n in 5" :key="n">
											<span class="input-group-text bg-light">{{ n }}차</span>
											<input v-model="formData['gline'+n]" type="text" class="form-control" placeholder="명칭" />
										</div>
									</div>
								</td>
							</tr>
						</tbody>
					</table>
				</div>
			</div>

			<!-- 🅱️ 물류 및 운영 로직 -->
			<div class="card border-0 shadow-sm overflow-hidden">
				<div class="card-header bg-white py-2 px-3 border-bottom d-flex align-items-center">
					<i class="bi bi-box-seam-fill me-2 text-secondary"></i>
					<span class="fw-bold small text-dark">운영 로직 및 물류 환경설정</span>
				</div>
				<div class="card-body p-0">
					<table class="erp-table-full border-0">
						<colgroup>
							<col style="width: 120px;" /><col />
							<col style="width: 120px;" /><col />
							<col style="width: 120px;" /><col />
						</colgroup>
						<tbody>
							<tr>
								<th>재고 평가방법</th>
								<td>
									<select v-model="formData.stkgbn" class="form-select">
										<option value="100">총평균법</option>
										<option value="200">이동평균법</option>
										<option value="300">선입선출법</option>
									</select>
								</td>
								<th>매출 단가기준</th>
								<td>
									<select v-model="formData.pricegbn" class="form-select">
										<option value="1">공급가액 기준</option>
										<option value="2">공급가+부가세 합산</option>
									</select>
								</td>
								<th>재고부족 통제</th>
								<td>
									<select v-model="formData.stokyn" class="form-select">
										<option value="Y">부족시 차단</option>
										<option value="N">마이너스 허용</option>
									</select>
								</td>
							</tr>
							<tr>
								<th>생산 연동</th>
								<td>
									<select v-model="formData.mnfyn" class="form-select">
										<option value="N">제조안함</option>
										<option value="Y">제조업체</option>
									</select>
								</td>
								<th>회계 연동</th>
								<td>
									<select v-model="formData.outacctyn" class="form-select">
										<option value="N">자체회계</option>
										<option value="Y">외부연동</option>
									</select>
								</td>
								<th>여신 확인</th>
								<td>
									<div class="form-check form-switch m-0 d-flex align-items-center justify-content-center h-100">
										<input v-model="formData.yeosinyn" class="form-check-input mt-0" type="checkbox" true-value="Y" false-value="N" id="yeosinSwitch">
										<label class="form-check-label ms-2 small fw-bold" for="yeosinSwitch">사용</label>
									</div>
								</td>
							</tr>
							<tr>
								<th>기타 옵션</th>
								<td colspan="5">
									<div class="d-flex gap-4 px-2 h-100 align-items-center">
										<div class="form-check">
											<input v-model="formData.iocnfmyn" type="checkbox" class="form-check-input" true-value="Y" false-value="N" id="ioCheck" />
											<label for="ioCheck" class="small fw-bold">출고확정 단계 필수</label>
										</div>
									</div>
								</td>
							</tr>
						</tbody>
					</table>
				</div>
			</div>

			<!-- 💡 3. 하단 이미지 영역: 로고 및 직인 -->
			<div class="card border-0 shadow-sm overflow-hidden flex-shrink-0">
				<div class="card-header bg-white py-2 px-3 border-bottom d-flex align-items-center">
					<i class="bi bi-image me-2 text-secondary"></i>
					<span class="fw-bold small text-dark">회사 인장 및 로고 관리</span>
				</div>
				<div class="card-body p-0 bg-white">
					<table class="erp-table-full border-0">
						<colgroup>
							<col style="width: 120px;" /><col />
							<col style="width: 120px;" /><col />
						</colgroup>
						<tbody>
							<tr>
								<th>회사 로고</th>
								<td>
									<div class="d-flex align-items-center gap-2 flex-nowrap">
										<input type="file" class="form-control" style="max-width: 300px;" @change="e => onFileChange(e, 'logoimg')" />
										<div v-if="formData.logoimg" class="border rounded p-1 bg-white">
											<img :src="getImageUrl(formData.logoimg, 'logoimg')" height="25" />
										</div>
									</div>
								</td>
								<th>공인 직인</th>
								<td>
									<div class="d-flex align-items-center gap-2 flex-nowrap">
										<input type="file" class="form-control" style="max-width: 300px;" @change="e => onFileChange(e, 'stampimg')" />
										<div v-if="formData.stampimg" class="border rounded p-1 bg-white">
											<img :src="getImageUrl(formData.stampimg, 'stampimg')" height="50" />
										</div>
									</div>
								</td>
							</tr>
						</tbody>
					</table>
				</div>
			</div>
		</div>
	</div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import AppAlert from '@/components/AppAlert.vue'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { API_URL } from '@/config/api'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

const uiDate = reactive({ yy: '',mm: '', dd: '' })
const formData = reactive<any>({
	actkind: 'S0', cmpycd: authStore.cmpycd, userid: authStore.userid,
	gline1: '', gline2: '', gline3: '', gline4: '', gline5: '',
	bgtype: '000', cardcust: '', mnfyn: 'N', stkgbn: '100',
	stokyn: 'Y', pricegbn: '1', slipyn: 'N', yeosinyn: 'N', iocnfmyn: 'N',
	balcnfmyn: 'N', outacctyn: 'N', logoimg: '', stampimg: ''
})

const getImageUrl = (filename: string, type: string) => {
	if (!filename) return ''
	const baseUrl = API_URL || window.location.origin
	// 🚀 단순화: 리소스 핸들러 경로와 매핑
	return `${baseUrl}/Upload_Images/${authStore.cmpycd}/${type}/${filename}`.replace(/([^:]\/)\/+/g, "$1")
}

const bgOptions = ref<any[]>([])

async function fetchConfig() {
	try {
		const res = await api.post('/haba/HABA_100U_STR', {
			actkind: 'S0',
			cmpycd: authStore.cmpycd
		})

		if (res.data && res.data.length > 0) {
			const d = res.data[0]
			console.log('📡 [HABA100U] Config Loaded:', d)

			// 🚀 [복구] 가공 없이 백엔드 응답을 그대로 formData에 할당 (정확한 필드명 매칭)
			Object.assign(formData, d)

			const rawClsymd = formData.clsymd || ''
			if (rawClsymd && rawClsymd.length >= 8) {
				uiDate.yy = rawClsymd.substring(0, 4)
				uiDate.mm = rawClsymd.substring(4, 6)
				uiDate.dd = rawClsymd.substring(6, 8)
			}
			vAlert('환경설정 정보를 성공적으로 로드했습니다.')
		} else {
			vAlert('조회된 설정 정보가 없습니다.')
		}
	} catch (e) { vAlertError('설정 로드 실패') }
}

async function save() {
	if (!uiDate.yy || !uiDate.mm || !uiDate.dd) return vAlertError('마감 기준일을 입력하십시오.')
	try {
		// 🚀 [표준] XML 21개 파라미터 스펙에 맞춰 하나하나 명시적으로 나열하여 전송
		const param = {
			actkind: 'U0',
			cmpycd: authStore.cmpycd,
			clsymd: `${uiDate.yy}${uiDate.mm}${uiDate.dd}`,
			gline1: formData.gline1,
			gline2: formData.gline2,
			gline3: formData.gline3,
			gline4: formData.gline4,
			gline5: formData.gline5,
			bgtype: formData.bgtype,
			stkgbn: formData.stkgbn,
			mnfyn: formData.mnfyn,
			stokyn: formData.stokyn,
			slipyn: formData.slipyn,
			pricegbn: formData.pricegbn,
			cardcust: formData.cardcust,
			logoimg: formData.logoimg,
			stampimg: formData.stampimg,
			yeosinyn: formData.yeosinyn,
			iocnfmyn: formData.iocnfmyn,
			outacctyn: formData.outacctyn,
			updemp: authStore.userid
		}

		await api.post('/haba/HABA_100U_STR', param)
		vAlert('환경설정이 저장되었습니다.')
		fetchConfig()
	} catch (e) { vAlertError('저장 실패') }
}

function initialize() {
	resetForm(formData); formData.actkind = 'S0'; formData.cmpycd = authStore.cmpycd;
}

const onFileChange = async (e: any, target: string) => {
	const file = e.target.files[0]
	if (!file) return

	const data = new FormData()
	data.append('file', file)
	data.append('cmpycd', authStore.cmpycd)
	data.append('type', target) // logoimg or stampimg

	try {
		vAlert(target === 'logoimg' ? '로고 업로드 중...' : '직인 업로드 중...')
		const res = await api.post('/comm/upload/company', data, {
			headers: { 'Content-Type': 'multipart/form-data' }
		})

		// 🚀 백엔드 응답 키값(filename)과 일치 여부 확인 및 보정
		const filename = res.data.filename || res.data.fileName
		if (filename) {
			formData[target] = filename
			vAlert('✅ 업로드 성공. [저장] 버튼을 눌러 확정해 주세요.')
		}
	} catch (err) {
		console.error('❌ 업로드 실패:', err)
		vAlertError('파일 업로드에 실패했습니다.')
	}
}

onMounted(async () => {
	try {
		// 💡 403 Forbidden 방지를 위해 POST로 변경
		const res = await api.post('/ha00/HA00_00P_STR', { gubun: 'E0', gbncd: '320', cmpycd: authStore.cmpycd })
		bgOptions.value = res.data.map((i: any) => ({ codecd: String(i.codecd || i.codecd).trim(), codenm: String(i.codenm || i.codenm).trim() }))
	} catch (e) { console.error('코드 로드 실패') }
	fetchConfig()
})
</script>
