<!--관리정보/기초관리/사업장정보관리 [ERP 프리미엄 고밀도 표준 - 1열 배치] -->
<template>
	<AppAlert :show="showalert" :error="showerror" :message="alertmessage" />

	<div class="erp-container">
		<!-- 🚀 1. 상단 액션 바 -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 shadow-sm sticky-top">
			<div class="fw-bold ps-3 text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-geo-alt-fill me-2 text-primary" style="font-size: 18px;"></i>
				기본정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
				기초관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
				<span class="text-primary fw-bolder">사업장정보관리 (haba030u)</span>
			</div>
			<div class="btn-group-erp pe-3">
				<button class="btn-erp btn-init" @click="initialize">초기화</button>
				<button class="btn-erp btn-search" @click="search">조회</button>
				<button class="btn-erp btn-save" @click="save">저장</button>
			</div>
		</div>

		<!-- 💡 메인 컨텐츠 영역 -->
		<div class="flex-grow-1 overflow-hidden d-flex flex-column gap-3 p-3">

			<!-- 💡 2. 상세 입수정 영역 (1열 수평 배치) -->
			<div class="card border-0 shadow-sm overflow-hidden flex-shrink-0">
				<div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
					<div class="fw-bold small text-dark"><i class="bi bi-pencil-square me-2 text-secondary"></i>사업장 상세 정보</div>
					<div v-if="formdata.actkind === 'U0'" class="badge bg-warning text-dark px-2">수정 중</div>
					<div v-else class="badge bg-primary text-white px-2">신규 등록</div>
				</div>
				<div class="card-body p-0 bg-white">
					<table class="erp-table-full border-0">
						<colgroup>
                            <col style="width: 70px;" /><col style="width: 120px;" /> <!-- 코드 -->
                            <col style="width: 70px;" /><col />                       <!-- 명칭 -->
                            <col style="width: 80px;" /><col style="width: 140px;" /> <!-- 사업자번호 -->
                            <col style="width: 70px;" /><col style="width: 120px;" /> <!-- 대표자 -->
                            <col style="width: 60px;" /><col style="width: 100px;" /> <!-- 순서 -->
                            <col style="width: 50px;" /><col style="width: 80px;" />  <!-- 사용 -->
                        </colgroup>
						<tbody>
							<tr>
								<th class="required">코드</th>
								<td><input v-model="formdata.taxunit" type="text" class="form-control fw-bold text-primary text-center" maxlength="3" placeholder="000" :disabled="formdata.actkind === 'U0'"/></td>
								<th class="required">사업장명</th>
								<td><input v-model="formdata.unitnm" type="text" class="form-control" maxlength="30" /></td>
								<th>사업자번호</th>
								<td><input v-model="formdata.saupno" type="text" class="form-control text-center" maxlength="10" placeholder="숫자만" /></td>
								<th>대표자</th>
								<td><input v-model="formdata.bossnm" type="text" class="form-control" maxlength="20" /></td>
								<th>순서</th>
								<td><input v-model="formdata.dspord" type="number" class="form-control text-end" /></td>
								<th>사용</th>
								<td>
									<div class="form-check form-switch m-0 d-flex align-items-center justify-content-center h-100">
										<input v-model="formdata.useyn" class="form-check-input mt-0" type="checkbox" true-value="Y" false-value="N" id="useyn030">
									</div>
								</td>
							</tr>
							<tr>
								<th>주소</th>
								<td colspan="11">
                  <AddressPopupForm
                    v-model:postno="formdata.postno"
                    v-model:address="formdata.address"
                    v-model:d_address="formdata.d_address"
                  />
								</td>
							</tr>
						</tbody>
					</table>
				</div>
			</div>

			<!-- 📊 3. 하단 그리드 영역 -->
			<div class="card border-0 shadow-sm flex-grow-1 overflow-hidden">
				<div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center">
					<i class="bi bi-table me-2 text-secondary"></i>
					<span class="fw-bold small text-dark">사업장 리스트 ({{ activeitemcount }} 건)</span>
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
import AppAlert from '@/components/AppAlert.vue'
import AddressPopupForm from '@/components/AddressPopupForm.vue'

const authstore = useAuthStore()
const { showAlert: showalert, showError: showerror, alertMessage: alertmessage, vAlert: valert, vAlertError: valerterror } = useAlerts()
const { resetForm: resetform } = useFormReset()

const formdata = reactive({
	actkind: 'sr', taxunit: '', unitnm: '', saupno: '', bossnm: '', postno: '', address: '', d_address: '', dspord: '1', useyn: 'Y',
	cmpycd: authstore.cmpycd, userid: authstore.userid
})

const maingridelement = ref<HTMLDivElement | null>(null)
let maingrid: Tabulator | null = null
const activeitemcount = ref(0)

async function search() {
	try {
		const res = await api.post('/haba/haba_030u_str', { actkind: 'sr', cmpycd: authstore.cmpycd })
		const processed = res.data || [];
		maingrid?.setData(processed)
		activeitemcount.value = processed.length
		valert('조회되었습니다.')
	} catch (e) { valerterror('데이터 로드 실패') }
}

async function save() {
	if (!formdata.taxunit || !formdata.unitnm) return valerterror('코드와 명칭은 필수입니다.')
	if (!confirm('저장하시겠습니까?')) return
	try {
		const act = formdata.actkind === 'sr' ? 'ar' : 'ur';
    const payload = {
      ...formdata,
      actkind: act,
      address_det: formdata.d_address // 서버 필드명에 맞춤 (추측)
    }
		const res = await api.post('/haba/haba_030u_str', payload)
		const resdata = res.data?.[0] || {};
		if (resdata.result === 'N') valerterror(resdata.msg || '저장 실패')
		else { valert('저장되었습니다.'); search(); initialize() }
	} catch (e) { valerterror('저장 실패') }
}

function initialize() {
	resetform(formdata); Object.assign(formdata, { actkind: 'sr', useyn: 'Y', dspord: '1', cmpycd: authstore.cmpycd, userid: authstore.userid });
}

onMounted(() => {
	if (maingridelement.value) {
		maingrid = new Tabulator(maingridelement.value, {
			layout: 'fitColumns', height: '100%', selectable: 1,
			placeholder: '데이터가 없습니다.',
			columnDefaults: { headerSort: false, headerHozAlign: 'center', vertAlign: "middle" },
			columns: [
				{ title: '코드', field: 'taxunit', hozAlign: 'center', width: 80, cssClass: 'fw-bold text-primary' },
				{ title: '사업장 명칭', field: 'unitnm', minWidth: 200, widthGrow: 1, cssClass: 'fw-bold' },
				{ title: '사업자번호', field: 'saupno', hozAlign: 'center', width: 140 },
				{ title: '대표자', field: 'bossnm', hozAlign: 'center', width: 100 },
				{ title: '순서', field: 'dspord', hozAlign: 'center', width: 70 },
				{ title: '사용', field: 'useyn', hozAlign: 'center', width: 70, formatter: (c) => c.getValue() === 'Y' ? 'O' : 'X' }
			]
		})
		maingrid.on('rowClick', (e, row) => {
			const d = row.getData();
      if (d.address_det) d.d_address = d.address_det;
			Object.assign(formdata, d);
			formdata.actkind = 'ur'
		})
	}
	nextTick(search);
})
</script>
