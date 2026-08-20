<!--관리정보/기초관리/은행정보관리 [ERP 프리미엄 고밀도 표준] -->
<template>
	<appalert :show="showalert" :error="showerror" :message="alertmessage" />

	<div class="erp-container">
		<!-- 🚀 1. 상단 액션 바 -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 shadow-sm sticky-top">
			<div class="fw-bold ps-3 text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-bank2 me-2 text-primary" style="font-size: 18px;"></i>
				기본정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
				기초관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
				<span class="text-primary fw-bolder">은행정보관리 (haba050u)</span>
			</div>
			<div class="btn-group-erp pe-3">
				<button class="btn-erp btn-init" @click="initialize">초기화</button>
				<button class="btn-erp btn-search" @click="search">조회</button>
				<button class="btn-erp btn-save" @click="save">저장</button>
			</div>
		</div>

		<!-- 💡 메인 컨텐츠 영역 -->
		<div class="flex-grow-1 overflow-hidden d-flex flex-column gap-3 p-3 bg-light">

			<!-- 💡 2. 상세 입수정 영역 -->
			<div class="card border-0 shadow-sm overflow-hidden flex-shrink-0">
				<div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
					<div class="fw-bold small text-dark"><i class="bi bi-pencil-square me-2 text-secondary"></i>은행 상세 정보</div>
					<div v-if="masterform.actkind === 'U0'" class="badge bg-warning text-dark px-2">수정 중</div>
					<div v-else class="badge bg-primary text-white px-2">신규 등록</div>
				</div>
				<div class="card-body p-0 bg-white">
					<table class="erp-table-full border-0">
						<colgroup>
                            <col style="width: 80px;" /><col style="width: 120px;" />
                            <col style="width: 80px;" /><col />
                            <col style="width: 60px;" /><col style="width: 100px;" />
                            <col style="width: 50px;" /><col style="width: 80px;" />
                        </colgroup>
						<tbody>
							<tr>
								<th class="required">은행코드</th>
								<td><input v-model="masterform.bankcd" type="text" class="form-control fw-bold text-primary text-center" maxlength="3" placeholder="000" :disabled="masterform.actkind === 'U0'"/></td>
								<th class="required">은행명</th>
								<td><input v-model="masterform.banknm" type="text" class="form-control" maxlength="30" /></td>
								<th>순서</th>
								<td><input v-model="masterform.dspord" type="number" class="form-control text-end" /></td>
								<th>사용</th>
								<td>
									<div class="form-check form-switch m-0 d-flex align-items-center justify-content-center h-100">
										<input v-model="masterform.useyn" class="form-check-input mt-0" type="checkbox" true-value="Y" false-value="N" id="useyn050">
									</div>
								</td>
							</tr>
							<tr>
								<th>비고</th>
								<td colspan="7"><input v-model="masterform.remark" type="text" class="form-control" maxlength="50" /></td>
							</tr>
						</tbody>
					</table>
				</div>
			</div>

			<!-- 📊 3. 하단 그리드 영역 -->
			<div class="card border-0 shadow-sm flex-grow-1 overflow-hidden">
				<div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center">
					<i class="bi bi-table me-2 text-secondary"></i>
					<span class="fw-bold small text-dark">은행 리스트 ({{ activeitemcount }} 건)</span>
				</div>
                <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
                    <div ref="maingridelement" class="tabulator-instance flex-grow-1"></div>
                </div>
			</div>
		</div>
	</div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, nextTick } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useAlerts } from '@/composables/useAlerts'
import { useFormReset } from '@/composables/useFormReset'
import appalert from '@/components/AppAlert.vue'

const authstore = useAuthStore()
const { showAlert: showalert, showError: showerror, alertMessage: alertmessage, vAlert: valert, vAlertError: valerterror } = useAlerts()
const { resetForm: resetform } = useFormReset()

const masterform = reactive({
	actkind: 'S0', bankcd: '', banknm: '', remark: '', dspord: '1', useyn: 'Y',
	cmpycd: authstore.cmpycd, userid: authstore.userid
})

const maingridelement = ref<HTMLDivElement | null>(null)
let maingrid: Tabulator | null = null
const activeitemcount = ref(0)

async function search() {
	try {
		// 🚀 HabaController의 fillMissingParameters 기능을 테스트하기 위해
		// bankcd, banknm 등 공백 파라미터를 생략하고 actkind만 전달합니다.
		const res = await api.post('/haba/HABA_050U_STR', {
		    actkind: 'S0'
		})

		const processed = res.data || [];
		maingrid?.setData(processed)
		activeitemcount.value = processed.length
		valert('조회되었습니다.')
	} catch (e) { valerterror('데이터 로드 실패') }
}

async function save() {
	if (!masterform.bankcd || !masterform.banknm) return valerterror('코드와 명칭은 필수입니다.')
	if (!confirm('저장하시겠습니까?')) return
	try {
		const act = masterform.actkind === 'S0' ? 'A0' : 'U0';
		const res = await api.post('/haba/HABA_050U_STR', { ...masterform, actkind: act })
		const resdata = res.data?.[0] || {};
		if (resdata.result === 'N') valerterror(resdata.msg || '저장 실패')
		else { valert('저장되었습니다.'); search(); initialize() }
	} catch (e) { valerterror('저장 실패') }
}

function initialize() {
	resetform(masterform); Object.assign(masterform, { actkind: 'S0', useyn: 'Y', dspord: '1', cmpycd: authstore.cmpycd, userid: authstore.userid });
}

onMounted(() => {
	if (maingridelement.value) {
		maingrid = new Tabulator(maingridelement.value, {
			layout: 'fitColumns', height: '100%', selectable: 1,
			placeholder: '데이터가 없습니다.',
			columnDefaults: { headerSort: false, headerHozAlign: 'center', vertAlign: "middle" },
			columns: [
				{ title: '코드', field: 'bankcd', hozAlign: 'center', width: 80, cssClass: 'fw-bold text-primary' },
				{ title: '은행 명칭', field: 'banknm', minWidth: 200, widthGrow: 1, cssClass: 'fw-bold', hozAlign: 'left' },
				{ title: '비고', field: 'remark', minWidth: 200, hozAlign: 'left' },
				{ title: '순서', field: 'dspord', hozAlign: 'center', width: 70 },
				{ title: '사용', field: 'useyn', hozAlign: 'center', width: 70, formatter: (c) => c.getValue() === 'Y' ? 'O' : 'X' }
			]
		})
		maingrid.on('rowClick', (e, row) => {
			const d = row.getData();
			Object.assign(masterform, d);
			masterform.actkind = 'U0'
		})
	}
	nextTick(search);
})
</script>
