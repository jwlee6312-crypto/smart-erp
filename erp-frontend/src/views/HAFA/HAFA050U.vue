<!--
	=============================================================
	프로그램명 : 고정자산 마스터 관리(HAFA050U)
	작성일자	: 2025.02.24
	작성자    : AI Assistant
	설명        : 고정자산 마스터 정보 등록 및 관리(HAFA010U 스타일 로직 방식)
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container d-flex flex-column h-100 bg-white">
		<!-- [헤더] 1. 상단 액션 바 -->
		<div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
			<div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-box-seam me-2 text-primary" style="font-size: 18px;"></i>
				고정자산 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
				기초관리<i class="bi bi-chevron-right mx-1 small opacity-50"></i>
				<span class="text-primary fw-bolder">고정자산관리(HAFA050U)</span>
			</div>
			<div class="btn-group-erp d-flex gap-1 pe-3">
				<button class="btn-erp btn-init" @click="initialize">초기화</button>
				<button class="btn-erp btn-search" @click="search">조회</button>
				<button class="btn-erp btn-save" @click="save">저장</button>
			</div>
		</div>

		<!-- [콘텐츠] 2. 메인 콘텐츠 영역 -->
		<div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

			<!-- [조회] 조회 필터 영역 -->
			<div class="card border shadow-sm flex-shrink-0 overflow-hidden">
				<div class="card-body p-0 bg-white">
					<table class="erp-table-dense" width="100%">
						<colgroup>
							<col style="width: 10%" /><col style="width: 40%" />
							<col style="width: 10%" /><col style="width: 40%" />
						</colgroup>
						<tbody>
							<tr>
								<th class="text-center bg-light">계정과목</th>
								<td>
									<div class="input-group input-group-sm" style="width: 300px;">
										<input v-model="searchForm.acctcd" type="text" class="form-control text-center bg-light fw-bold" style="max-width: 65px;" readonly />
										<input v-model="searchForm.acctnm" type="text" class="form-control" @keydown.enter="openHelp('ACCT', 'search')" placeholder="계정 선택" />
										<button class="btn btn-outline-secondary" @click="openHelp('ACCT', 'search')"><i class="bi bi-search"></i></button>
									</div>
								</td>
								<th class="text-center bg-light">기준년월</th>
								<td>
									<div class="d-flex align-items-center gap-1">
										<select v-model="searchForm.yy" class="form-select form-select-sm" style="width: 100px;">
											<option v-for="y in yearOptions" :key="y" :value="y">{{ y }}년</option>
										</select>
										<input v-model="searchForm.mm" type="text" class="form-control form-control-sm text-center" style="width: 50px;" maxlength="2" />
										<span class="small fw-bold ms-1 text-secondary">월 현재</span>
									</div>
								</td>
							</tr>
						</tbody>
					</table>
				</div>
			</div>

			<!-- [입력] 입력/상세 정보 영역 -->
			<div class="card border shadow-sm flex-shrink-0 overflow-hidden">
				<div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
					<div class="fw-bold small text-dark"><i class="bi bi-pencil-square me-2 text-primary"></i>자산 상세 정보 [{{ formData.actkind === 'A' ? '신규' : '수정' }}]</div>
					<div class="form-check form-switch m-0">
						<input v-model="formData.useyn" class="form-check-input" type="checkbox" id="useynCheck050" true-value="N" false-value="Y">
						<label class="form-check-label text-danger fw-bold small" for="useynCheck050">폐기</label>
					</div>
				</div>
				<div class="card-body p-0 bg-white">
					<table class="erp-table-full">
						<colgroup>
							<col style="width: 110px;" /><col />
							<col style="width: 110px;" /><col />
							<col style="width: 110px;" /><col />
						</colgroup>
						<tbody>
							<tr>
								<th class="bg-light">자산코드</th>
								<td><input v-model="formData.asetcd" type="text" class="form-control form-control-sm text-center bg-light fw-bold" readonly placeholder="자동부여" /></td>
								<th class="required bg-light">자산명</th>
								<td><input v-model="formData.asetnm" type="text" class="form-control form-control-sm" placeholder="자산 명칭 입력" /></td>
								<th class="required bg-light">계정과목</th>
								<td>
									<div class="input-group input-group-sm">
										<input v-model="formData.acctcd" type="text" class="form-control text-center bg-light fw-bold" style="max-width: 65px;" readonly />
										<input v-model="formData.acctnm" type="text" class="form-control" @keydown.enter="openHelp('ACCT', 'form')" />
										<button class="btn btn-outline-secondary" @click="openHelp('ACCT', 'form')"><i class="bi bi-search"></i></button>
									</div>
								</td>
							</tr>
							<tr>
								<th class="required bg-light">취득일</th>
								<td><input v-model="formData.pchymd" type="date" class="form-control form-control-sm" /></td>
								<th class="required bg-light">취득수량</th>
								<td><input v-model="formData.pchqty" type="number" class="form-control form-control-sm text-end" /></td>
								<th class="required bg-light">취득가액</th>
								<td><input v-model="formattedPchamt" type="text" class="form-control form-control-sm text-end fw-bold text-primary" placeholder="0" @input="e => e.target.value = e.target.value.replace(/[^0-9,]/g, '')" /></td>
							</tr>
							<tr>
								<th class="required bg-light">상각방법</th>
								<td>
									<select v-model="formData.dprstype" class="form-select form-select-sm">
										<option v-for="item in dprstypeOptions" :key="item.codecd" :value="item.codecd">{{ item.codenm }}</option>
									</select>
								</td>
								<th class="required bg-light">내용연수</th>
								<td>
									<div class="input-group input-group-sm">
										<input v-model="formData.legalyy" type="number" class="form-control form-control-sm text-end" />
										<button class="btn btn-outline-secondary" @click="openHelp('rate')"><i class="bi bi-list-ul"></i></button>
									</div>
								</td>
								<th class="required bg-light">상각율</th>
								<td>
									<div class="d-flex gap-1 align-items-center">
										<input v-model="formData.asetrate" type="number" step="0.001" class="form-control form-control-sm text-end fw-bold" style="width: 100px;" />
										<span class="small text-muted ms-1">가감</span>
										<input v-model="formData.gagamyy" type="number" class="form-control form-control-sm text-end" style="width: 50px;" />
									</div>
								</td>
							</tr>
							<tr>
								<th class="required bg-light">비용구분</th>
								<td>
									<select v-model="formData.costtype" class="form-select form-select-sm">
										<option v-for="item in costtypeOptions" :key="item.codecd" :value="item.codecd">{{ item.codenm }}</option>
									</select>
								</td>
								<th class="required bg-light">보유부서</th>
								<td>
									<div class="input-group input-group-sm">
										<input v-model="formData.deptcd" type="text" class="form-control text-center bg-light fw-bold" style="max-width: 65px;" readonly />
										<input v-model="formData.deptnm" type="text" class="form-control" @keydown.enter="openHelp('DEPT')" />
										<button class="btn btn-outline-secondary" @click="openHelp('DEPT')"><i class="bi bi-search"></i></button>
									</div>
								</td>
								<th class="bg-light">비고</th>
								<td><input v-model="formData.remark" type="text" class="form-control form-control-sm" placeholder="참조 사항 입력" /></td>
							</tr>
							<tr>
								<th class="required bg-light">기초가액</th>
								<td><input v-model="formattedBaseamt" type="text" class="form-control form-control-sm text-end" @input="e => e.target.value = e.target.value.replace(/[^0-9,]/g, '')" /></td>
								<th class="required bg-light">전기상각누계</th>
								<td><input v-model="formattedDprssum" type="text" class="form-control form-control-sm text-end" @input="e => e.target.value = e.target.value.replace(/[^0-9,]/g, '')" /></td>
								<td colspan="2" class="bg-white"></td>
							</tr>
						</tbody>
					</table>
				</div>
			</div>

			<!-- [그리드] 데이터 그리드 영역 -->
			<div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white">
				<div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
					<div ref="mainGridRef" class="tabulator-instance flex-grow-1"></div>
				</div>
			</div>
		</div>
	</div>

	<Modal v-model:visible="modalVisible" :modalProps="modalProps" />
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import AppAlert from '@/components/AppAlert.vue'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import Modal from '@/components/Modal.vue'
import type { ModalProps } from '@/types/modal'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()

const today = new Date().toISOString().slice(0, 10)
const currentYear = new Date().getFullYear()
const currentMonth = (new Date().getMonth() + 1).toString().padStart(2, '0')
const yearOptions = Array.from({ length: 5 }, (_, i) => String(currentYear - i))

const searchForm = reactive({ acctcd: '', acctnm: '', yy: String(currentYear), mm: currentMonth })
const formData = reactive({ actkind: 'A', asetcd: '', asetnm: '', acctcd: '', acctnm: '', pchymd: today, pchqty: 0, pchamt: 0, dprstype: '010', legalyy: 0, gagamyy: 0, asetrate: 0, costtype: '010', deptcd: '', deptnm: '', remark: '', baseamt: 0, dprssum: 0, useyn: 'Y' })

const dprstypeOptions = ref<any[]>([]); const costtypeOptions = ref<any[]>([])
const mainGridRef = ref<HTMLDivElement | null>(null); let mainGrid: Tabulator | null = null

// 공통 코드 데이터를 표준 형식으로 매핑 (codecd, codenm 등 대응)
const mapToStandard = (data: any[]) => {
  return (data || []).map((i: any) => ({
    codecd: i.codecd || i.code || '',
    codenm: i.codenm || i.cdnm || ''
  }));
};

// 금액 천단위 콤마 포맷팅 Computed
const formattedPchamt = computed({
  get: () => {
    if (formData.pchamt === undefined || formData.pchamt === null || formData.pchamt === '') return '';
    return formData.pchamt.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
  },
  set: (val: string) => {
    const num = Number(val.replace(/,/g, ''));
    formData.pchamt = isNaN(num) ? 0 : num;
  }
});

const formattedBaseamt = computed({
  get: () => {
    if (formData.baseamt === undefined || formData.baseamt === null || formData.baseamt === '') return '';
    return formData.baseamt.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
  },
  set: (val: string) => {
    const num = Number(val.replace(/,/g, ''));
    formData.baseamt = isNaN(num) ? 0 : num;
  }
});

const formattedDprssum = computed({
  get: () => {
    if (formData.dprssum === undefined || formData.dprssum === null || formData.dprssum === '') return '';
    return formData.dprssum.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
  },
  set: (val: string) => {
    const num = Number(val.replace(/,/g, ''));
    formData.dprssum = isNaN(num) ? 0 : num;
  }
});

function initialize() {
	Object.assign(formData, { actkind: 'A', asetcd: '', asetnm: '', acctcd: searchForm.acctcd, acctnm: searchForm.acctnm, pchymd: today, pchqty: 0, pchamt: 0, dprstype: '010', legalyy: 0, gagamyy: 0, asetrate: 0, costtype: '010', deptcd: '', deptnm: '', remark: '', baseamt: 0, dprssum: 0, useyn: 'Y' })
}

const fetchOptions = async () => {
	try {
		const [resD, resC] = await Promise.all([
			api.post('/ha00/HA00_00P_STR', { gubun: 'E0', cmpycd: ' ', gbncd: '250' }),
			api.post('/ha00/HA00_00P_STR', { gubun: 'E0', cmpycd: ' ', gbncd: '110' })
		]);
		dprstypeOptions.value = mapToStandard(resD.data);
		costtypeOptions.value = mapToStandard(resC.data);
	} catch (e) { console.error('코드 로드 실패:', e) }
}

async function search() {
	if (!searchForm.acctcd) return vAlertError('계정과목을 선택하세요.')
	try {
		const res = await api.post('/hafa/HAFA_050U_STR', { actkind: 'S', cmpycd: authStore.cmpycd, yy: searchForm.yy, mm: searchForm.mm, acctcd: searchForm.acctcd })
		const data = (res.data || []).map((r: any) => ({ ...r, pchymd: r.pchymd ? `${r.pchymd.slice(0,4)}-${r.pchymd.slice(4,6)}-${r.pchymd.slice(6,8)}` : '' }))
		mainGrid?.setData(data); vAlert('조회되었습니다.'); initialize()
	} catch (e) { vAlertError('조회 중 오류 발생') }
}

async function save() {
	if (!formData.acctcd) return vAlertError('계정과목을 선택하세요.')
	if (!formData.asetnm) return vAlertError('자산명을 입력하세요.')
	try {
		const res = await api.post('/hafa/HAFA_050U_STR', { ...formData, cmpycd: authStore.cmpycd, yy: searchForm.yy, mm: searchForm.mm, pchymd: formData.pchymd.replace(/-/g, ''), userid: authStore.userid })
		if (res.data && res.data[0]?.col0 === 'Y') { vAlertError(res.data[0].col1) } else { vAlert('저장되었습니다.'); search() }
	} catch (e) { vAlertError('저장 중 오류 발생') }
}

const modalVisible = ref(false); const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })
function openHelp(type: string, target?: string) {
	if (type === 'ACCT') {
		Object.assign(modalProps, { title: '계정과목 선택', path: '/ha00/HA00_00P_STR', data: { gubun: 'A8', cmpycd: authStore.cmpycd, gbncd: '020', code: '' }, columns: [{ title: '코드', field: 'acctcd', width: 80 }, { title: '계정명', field: 'acctnm', width: 200 }],
			onConfirm: (rawData: any) => {
				const d = rawData;
				const code = d.acctcd || d.code;
				const name = d.acctnm || d.cdnm || d.name;
				if (target === 'search') { searchForm.acctcd = code; searchForm.acctnm = name; search() } else { formData.acctcd = code; formData.acctnm = name }
			}
		})
	} else if (type === 'DEPT') {
		Object.assign(modalProps, { title: '부서 선택', path: '/ha00/HA00_00P_STR', data: { gubun: 'D0', cmpycd: authStore.cmpycd, code: formData.deptnm }, columns: [{ title: '코드', field: 'deptcd', width: 80 }, { title: '부서명', field: 'deptnm', width: 180 }],
			onConfirm: (rawData: any) => {
				const d = rawData;
				formData.deptcd = d.deptcd || d.code;
				formData.deptnm = d.deptnm || d.cdnm || d.name;
			}
		})
	} else if (type === 'rate') {
		Object.assign(modalProps, { title: '상각률 선택', path: '/ha00/HA00_00P_STR', data: { gubun: 'J1', cmpycd: authStore.cmpycd, gbncd: formData.dprstype }, columns: [{ title: '내용연수', field: 'legalyy', width: 100 }, { title: '상각률', field: 'asetrate', width: 100 }],
			onConfirm: (rawData: any) => {
				const d = rawData;
				formData.legalyy = d.legalyy;
				formData.asetrate = d.rate95 || d.asetrate;
			}
		})
	}
	modalVisible.value = true
}

onMounted(() => {
	fetchOptions()
	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns', height: '100%', columnDefaults: { headerSort: false, vertAlign: "middle" },
			columns: [
				{ title: "자산코드", field: "asetcd", width: 100, hozAlign: "center", cssClass: "fw-bold text-primary" },
				{ title: "자산명", field: "asetnm", minWidth: 180, cssClass: "fw-bold" },
				{ title: "취득일", field: "pchymd", width: 150, hozAlign: "center" },
				{ title: "취득가액", field: "pchamt", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
				{ title: "상각방법", field: "dprstypenm", width: 150, hozAlign: "center" },
				{ title: "내용연수", field: "legalyy", width: 150, hozAlign: "center" },
				{ title: "상각률", field: "asetrate", width: 150, hozAlign: "center" },
				{ title: "보유부서", field: "deptnm", width: 200 },
                { title: "상태", field: "useyn", width: 80, hozAlign: "center",
                  formatter: (cell) => {
                    const val = String(cell.getValue() || '').trim().toUpperCase();
                    return val === 'Y' ? '<b class="text-primary">사용</b>' : '<span class="text-danger">폐기</span>';
                  }
                }
			]
		})
		mainGrid.on("rowClick", (e, row) => {
			const d = row.getData();
			Object.assign(formData, d);
			formData.actkind = 'U'
		})
	}
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
