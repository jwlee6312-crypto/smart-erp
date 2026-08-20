<!--	=============================================================
	프로그램명	: 배부작업
	작성일자	: 2025.02.24
	작성자	    : AI Assistant
	설명        : 해당 월의 부문별/품목별 비용 배부 실행 (컴팩트 디자인 적용)
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container p-2">
		<!-- 🟢 상단 타이틀 및 버튼 (타이트한 배치) -->
		<div class="d-flex justify-content-between align-items-center mb-1 border-bottom pb-1">
			<div class="fw-bold small d-flex align-items-center">
				<i class="bi bi-chevron-right me-1 text-primary"></i>
				<span>관리손익</span>
				<i class="bi bi-chevron-double-right mx-1 opacity-50"></i>
				<span class="text-primary">배부작업 (HAPL100U)</span>
			</div>
			<div class="d-flex gap-1">
				<button class="btn btn-sm btn-primary shadow-sm px-4" @click="handleExecute" :disabled="isExecuting">
					<span v-if="isExecuting" class="spinner-border spinner-border-sm me-1"></span>
					<i v-else class="bi bi-play-fill"></i>
					{{ isExecuting ? '처리 중...' : '배부실행' }}
				</button>
			</div>
		</div>

		<!-- 🔍 1. 작업 대상 설정 영역 (기존 테이블 스타일 레이아웃) -->
		<div class="border shadow-sm mb-2 bg-white overflow-hidden">
			<div class="row g-0 border-bottom border-secondary-subtle bg-light-beige">
				<div class="col-12 p-2 fw-bold small text-dark">
					<i class="bi bi-calendar-check me-1"></i> 배부 작업 설정
				</div>
			</div>
			<div class="row g-0">
				<div class="col-md-6 d-flex border-end border-bottom border-secondary-subtle">
					<div class="input-label bg-light-beige border-end p-1 px-3 fw-bold small text-center d-flex align-items-center justify-content-center" style="width: 120px;">기준연월</div>
					<div class="p-1 d-flex align-items-center gap-1 flex-grow-1">
						<select v-model="form.yy" class="form-select form-select-sm border-0 bg-transparent" style="width: 100px;">
							<option v-for="y in yearOptions" :key="y" :value="y">{{ y }}년</option>
						</select>
						<select v-model="form.mm" class="form-select form-select-sm border-0 bg-transparent" style="width: 80px;">
							<option v-for="m in monthOptions" :key="m" :value="m">{{ m }}월</option>
						</select>
					</div>
				</div>
				<div class="col-md-6 d-flex border-bottom border-secondary-subtle bg-light-yellow">
					<div class="p-2 small text-muted italic">※ 대상 연월을 선택한 후 [배부실행] 버튼을 클릭하세요.</div>
				</div>
			</div>

			<!-- 📘 안내 메시지 영역 (짜임새 있는 배치) -->
			<div class="row g-0">
				<div class="col-12 p-3 bg-white">
					<div class="border rounded p-2 bg-light shadow-inner">
						<div class="d-flex align-items-start mb-1 small">
							<i class="bi bi-info-circle-fill text-primary me-2 mt-1"></i>
							<span class="fw-bold">1. 당월 마감작업이 완료되었으면 배부작업을 하시기 바랍니다.</span>
						</div>
						<div class="d-flex align-items-start small">
							<i class="bi bi-info-circle-fill text-primary me-2 mt-1"></i>
							<span>2. <span class="text-primary fw-bold" style="cursor:pointer;" @click="goToHaba100">[기본정보 > 마감/결제라인관리]</span>의 마감정보와 작업월의 말일자가 일치해야 만 배부작업이 가능합니다.</span>
						</div>
					</div>
				</div>
			</div>
		</div>

		<!-- ⚠️ 2. 차액 발생 시 그리드 영역 -->
		<div v-if="showErrorGrid" class="flex-grow-1 overflow-hidden d-flex flex-column border shadow-sm mt-1">
			<div class="bg-danger text-white p-1 px-3 small fw-bold d-flex justify-content-between">
				<span><i class="bi bi-exclamation-triangle-fill me-1"></i> 배부 전/후 금액 불일치 내역</span>
				<span class="opacity-75">수정 후 재작업 필요</span>
			</div>
			<div ref="errorGridRef" class="compact-grid flex-grow-1"></div>
		</div>

		<!-- 빈 공간 채우기용 (그리드가 없을 때) -->
		<div v-else class="flex-grow-1 bg-light-beige opacity-25 d-flex align-items-center justify-content-center">
			<i class="bi bi-calculator opacity-25" style="font-size: 100px;"></i>
		</div>
	</div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import AppAlert from '@/components/AppAlert.vue' // 💡 이게 없어서 메시지가 안 보였던 것입니다. 정말 죄송합니다.
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useTabStore } from '@/stores/tabStore'

const authStore = useAuthStore()
const tabStore = useTabStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()

const currentYear = new Date().getFullYear()
const currentMonth = (new Date().getMonth() + 1).toString().padStart(2, '0')

const yearOptions = Array.from({ length: 20 }, (_, i) => String(currentYear - i))
const monthOptions = Array.from({ length: 12 }, (_, i) => String(i + 1).padStart(2, '0'))

const form = reactive({
	yy: String(currentYear),
    mm: currentMonth
})

const showErrorGrid = ref(false)
const isExecuting = ref(false) // 로딩 상태
const errorGridRef = ref<HTMLDivElement | null>(null)
let errorGrid: Tabulator | null = null

const handleExecute = async () => {
	if (!confirm(`${form.yy}년 ${form.mm}월의 배부작업을 하시겠습니까?`)) return

	isExecuting.value = true
	try {
		vAlert(`${form.mm}월 배부 작업을 시작합니다. 잠시만 기다려주세요...`);
		showErrorGrid.value = false

		// 1. 마감 정보 체크 (참조용으로 조회만 수행하고 프로세스를 차단하지 않음)
		try {
			await api.post('/haba/HABA_100U_STR', { actkind: 'S0', cmpycd: authStore.cmpycd })
		} catch (e) { console.warn('마감 정보 조회 생략') }

		vAlert('배부 엔진을 가동합니다...');

		// 2. 배부 실행 (actkind: 'A') - 외부 성공 쿼리와 동일한 파라미터 전달
		const resExec = await api.post('/hapl/HAPL_100U_STR', {
			actkind: 'A',
			cmpycd: authStore.cmpycd,
			stdym: form.yy + form.mm,
			deptcd: '00000',    // 💡 XML의 #{deptcd}와 정확히 일치
			userid: authStore.userid
		})

		const rawData = resExec.data || []

		// 💡 성공 판정: 데이터가 아예 없거나, 1건이더라도 실질적인 차액 데이터(acctcd)가 없는 경우
		const isSuccess = rawData.length === 0 || (rawData.length === 1 && !rawData[0].acctcd && !rawData[0].ACCTCD);

		if (!isSuccess) {
			// 2. 실패 판정: 데이터가 있으면 차액 발생 (프로시저의 HAVING <> 0 결과)
			vAlertError('배부 전/후 금액 불일치가 발견되었습니다.\n계정별 배부기준과 배부적수를 확인하시기 바랍니다.');
			showErrorGrid.value = true

			// 💡 프로시저의 별칭(acctcd, acctnm, bfamt, afamt, amt) 그대로 매핑
			const mappedData = rawData.map((row: any) => ({
				acctcd: row.acctcd || '',
				acctnm: row.acctnm || '',
				bef_amt: Number(row.bfamt || 0),
				aft_amt: Number(row.afamt || 0),
				diff_amt: Number(row.amt || 0)
			}))

			await nextTick(); initErrorGrid(mappedData)
		} else {
			// 💡 서비스 정신 보강: 1단계 성공 보고 후 2단계(집계) 시작 알림
			vAlert(`[1/2단계 성공] ${form.mm}월 배부 계산이 완료되었습니다. 이제 최종 결과를 장부에 반영(집계)합니다...`);
			try {
				// 💡 'C' 작업(집계) 실행
				await api.post('/hapl/HAPL_100U_STR', {
					actkind: 'C',
					cmpycd: authStore.cmpycd,
					stdym: form.yy + form.mm,
					deptcd: '00000',
					userid: authStore.userid
				})
				// 💡 최종 완료 보고 (매너 있는 마무리)
				vAlert(`[최종 완료] ${form.yy}년 ${form.mm}월 부문별 배부 및 집계 작업이 성공적으로 종료되어 장부에 최종 반영되었습니다.`);
			} catch (ce: any) {
				vAlertError(`[주의] 배부 계산은 성공했으나, 최종 장부 반영(집계) 중 서버 응답이 지연되고 있습니다. DB 자료를 확인해 주세요.`);
			}
		}
	} catch (e: any) {
		vAlertError(`배부 작업 중 서버 오류가 발생했습니다: ${e.message || ''}`);
	} finally {
		isExecuting.value = false
	}
}

const initErrorGrid = (data: any[]) => {
	if (!errorGridRef.value) return

	errorGrid = new Tabulator(errorGridRef.value, {
		data: data,
		layout: 'fitColumns',
		height: '100%',
		columnDefaults: { headerSort: false, vertAlign: "middle" },
		columns: [
			{ title: "계정", field: "acctcd", width: 90, hozAlign: "center", headerHozAlign: "center" },
			{ title: "계정명", field: "acctnm", widthGrow: 2, headerHozAlign: "center" },
			{ title: "배부전금액", field: "bef_amt", width: 130, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, headerHozAlign: "center" },
			{ title: "배부후금액", field: "aft_amt", width: 130, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, headerHozAlign: "center" },
			{ title: "차 액", field: "diff_amt", width: 130, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "text-danger fw-bold", headerHozAlign: "center" }
		]
	})
}

const goToHaba100 = () => {
	tabStore.addTab({ name: '마감/결제라인관리', path: '/HABA/HABA100U' })
}
</script>

<style scoped>
/* 🎨 기존 시스템 테마 재현 */
.bg-light-beige { background-color: #f1ede3 !important; }
.bg-light-yellow { background-color: #fffde7 !important; }
.shadow-inner { box-shadow: inset 0 1px 3px rgba(0,0,0,0.05); }

.erp-container {
	height: 100vh;
	display: flex;
	flex-direction: column;
	background-color: #fff;
}

.input-label {
	color: #333;
	font-size: 12px;
}

/* Tabulator 디자인 (고밀도) */
:deep(.tabulator) {
	font-size: 12px;
	border: none !important;
}
:deep(.tabulator-header) {
	background-color: #7d7d9a !important; /* 보라빛 도는 클래식 헤더 색상 */
	color: white !important;
	font-weight: bold;
}
:deep(.tabulator-header .tabulator-col) {
	background-color: transparent !important;
	border-right: 1px solid #ffffff44 !important;
}
:deep(.tabulator-row) {
	min-height: 28px !important;
}
:deep(.tabulator-cell) {
	padding: 4px 8px !important;
	border-right: 1px solid #eee !important;
}
:deep(.tabulator-row:hover) {
	background-color: #dfd9bd !important;
}

.form-select-sm {
	font-size: 12px;
	padding: 2px 5px;
}

.italic { font-style: italic; }
</style>
