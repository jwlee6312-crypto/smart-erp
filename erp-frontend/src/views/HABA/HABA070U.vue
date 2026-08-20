<!--
	=============================================================
	프로그램명	: 프로젝트관리 (haba070u)
	작성일자	: 2025.03.14
	설명        : 프로젝트 기본 정보 및 투입 인원 관리 (완전 소문자 원칙 적용)
	=============================================================
-->

<template>
	<appalert :show="showalert" :error="showerror" :message="alertmessage" />

	<div class="erp-container">
		<!-- 🚀 상단 액션 바 -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-folder2-open me-2 text-primary" style="font-size: 18px;"></i>
				기본정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
				<span class="text-primary fw-bolder">프로젝트관리 (haba070u)</span>
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
				<button v-if="masterdata.actkind === 'U'" class="btn-erp btn-danger" @click="deletedata">
					<i class="bi bi-trash"></i> 삭제
				</button>
			</div>
		</div>

		<!-- 🔍 검색 조건 영역 -->
		<div class="p-2 pb-0 flex-shrink-0">
			<div class="card border shadow-sm overflow-hidden bg-light">
				<table class="erp-table-full" style="table-layout: fixed;">
					<colgroup>
						<col style="width: 100px;" /><col style="width: 300px;" />
						<col />
					</colgroup>
					<tbody>
						<tr>
							<th class="text-center border-end">프로젝트</th>
							<td class="bg-white border-end px-2">
								<div class="input-group input-group-sm">
									<input v-model="searchform.prjcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
									<input v-model="searchform.prjnm" type="text" class="form-control" placeholder="프로젝트 선택" @keydown.enter="openhelp('search_prj')" />
									<button class="btn btn-outline-secondary px-2" @click="openhelp('search_prj')"><i class="bi bi-search"></i></button>
								</div>
							</td>
							<td class="bg-white px-3 text-muted small">
								<i class="bi bi-info-circle me-1"></i> 관리하실 프로젝트를 선택해 주십시요.
							</td>
						</tr>
					</tbody>
				</table>
			</div>
		</div>

		<!-- 📝 마스터 정보 입력 영역 -->
		<div class="p-2 pb-0 flex-shrink-0">
			<div class="card border shadow-sm bg-white overflow-hidden">
				<div class="card-header py-1 px-2 bg-light border-bottom">
					<span class="small fw-bold text-secondary"><i class="bi bi-pencil-square me-1"></i> 프로젝트 기본 정보</span>
				</div>
				<table class="erp-table-full small border-0">
					<colgroup>
						<col style="width: 100px;" /><col style="width: 20%;" />
						<col style="width: 100px;" /><col style="width: 25%;" />
						<col style="width: 100px;" /><col />
					</colgroup>
					<tbody>
						<tr>
							<th class="text-center bg-light-subtle border-end">프로젝트</th>
							<td class="bg-white border-end px-2 py-1">
								<input v-model="masterdata.prjcd" type="text" class="form-control form-control-sm text-center" maxlength="4" :readonly="masterdata.actkind === 'U'" />
							</td>
							<th class="text-center bg-light-subtle border-end border-top">프로젝트명</th>
							<td class="bg-white border-end border-top px-2 py-1">
								<input v-model="masterdata.prjnm" type="text" class="form-control form-control-sm" maxlength="50" />
							</td>
							<th class="text-center bg-light-subtle border-end border-top">완료여부</th>
							<td class="bg-white border-top px-3">
								<div class="form-check form-switch m-0 pt-1">
									<input v-model="masterdata.useyn" class="form-check-input" type="checkbox" id="prjuseyn" true-value="n" false-value="y">
									<label class="form-check-label small fw-bold" for="prjuseyn">완료</label>
								</div>
							</td>
						</tr>
						<tr>
							<th class="text-center bg-light-subtle border-end border-top">시&nbsp;작&nbsp;일</th>
							<td class="bg-white border-end border-top px-2 py-1">
								<input v-model="masterdata.fromdt" type="date" class="form-control form-control-sm" />
							</td>
							<th class="text-center bg-light-subtle border-end border-top">종&nbsp;료&nbsp;일</th>
							<td class="bg-white border-end border-top px-2 py-1">
								<input v-model="masterdata.todt" type="date" class="form-control form-control-sm" />
							</td>
							<th class="text-center bg-light-subtle border-end border-top">출현순서</th>
							<td class="bg-white border-top px-2 py-1">
								<input v-model="masterdata.dspord" type="number" class="form-control form-control-sm text-end" style="max-width: 80px;" />
							</td>
						</tr>
						<tr>
							<th class="text-center bg-light-subtle border-end border-top">개&nbsp;&nbsp;&nbsp;&nbsp;요</th>
							<td colspan="5" class="bg-white border-top px-2 py-1">
								<textarea v-model="masterdata.bigo" class="form-control form-control-sm" rows="2" maxlength="200"></textarea>
							</td>
						</tr>
					</tbody>
				</table>
			</div>
		</div>

		<!-- 📊 투입 인원 그리드 영역 -->
		<div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column bg-light">
			<div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white">
				<div class="card-header py-1 px-2 bg-light border-bottom d-flex justify-content-between align-items-center">
					<span class="small fw-bold text-secondary"><i class="bi bi-people me-1"></i> 투입 인원 상세</span>
					<div class="btn-group btn-group-sm">
						<button class="btn btn-outline-primary py-0" @click="addrow"><i class="bi bi-plus"></i> 추가</button>
					</div>
				</div>
                <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
                  <div ref="detailgridref" class="tabulator-instance flex-grow-1"></div>
                </div>
			</div>
		</div>
	</div>

	<Modal v-model:visible="modalvisible" :modalProps="modalprops" />
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import Modal from '@/components/Modal.vue'
import AppAlert from '@/components/AppAlert.vue'
import { useCommonHelp } from '@/composables/useCommonHelp'
import type { ModalProps } from '@/types/modal'
import { getDate } from '@/composables/useDate'

const authstore = useAuthStore()
const { firstDay, today } = getDate()
const { showAlert: showalert, showError: showerror, alertMessage: alertmessage, vAlert: valert, vAlertError: valerterror } = useAlerts()
const { resetForm: resetform } = useFormReset()
const { modalVisible: modalvisible, modalProps: modalprops } = useCommonHelp()

// 🔍 검색 데이터
const searchform = reactive({
	prjcd: '',
	prjnm: ''
})

// 📝 마스터 데이터
const masterdata = reactive({
	actkind: 'A',
	prjcd: '',
	prjnm: '',
	fromdt: firstDay,
	todt: today,
	bigo: '',
	dspord: 0,
	useyn: 'Y'
})

const detailgridref = ref<HTMLDivElement | null>(null)
let detailgrid: Tabulator | null = null

const formatymd = (v: string) => v && v.length === 8 ? `${v.substring(0, 4)}-${v.substring(4, 6)}-${v.substring(6, 8)}` : v

const search = async () => {
	if (!searchform.prjcd) return valert('프로젝트를 선택하시기 바랍니다.')
	try {
		// 1. 마스터 조회
		const resmaster = await api.post('/haba/HABA_070U_STR', {
			actkind: 'S0',
			cmpycd: authstore.cmpycd,
			prjcd: searchform.prjcd
		})

		if (resmaster.data && resmaster.data.length > 0) {
			const m = resmaster.data[0];
			Object.assign(masterdata, {
				actkind: 'U',
				prjcd: m.prjcd,
				prjnm: m.prjnm,
				fromdt: formatymd(m.fromdt),
				todt: formatymd(m.todt),
				bigo: m.bigo,
				dspord: Number(m.dspord || 0),
				useyn: m.useyn
			})
		}

		// 2. 디테일(투입인원) 조회
		const resdetail = await api.post('/haba/HABA_071U_STR', {
			actkind: 'S0',
			cmpycd: authstore.cmpycd,
			prjcd: searchform.prjcd
		})

		const processeddetails = (resdetail.data || []).map((n: any) => {
            return {
                upkind: '',
                rowno: n.rowno,
                userid: n.userid,
                usernm: n.usernm,
                fromdt: formatymd(n.fromdt),
                todt: formatymd(n.todt),
                bigo: n.bigo
            }
		})

		detailgrid?.setData(processeddetails)
		valert('조회되었습니다.')
	} catch (e) { valerterror('조회 중 오류 발생') }
}

const save = async () => {
	if (!masterdata.prjcd) return valert('프로젝트 코드를 입력하시기 바랍니다.')
	if (!masterdata.prjnm) return valert('프로젝트 명을 입력하시기 바랍니다.')
	if (!masterdata.fromdt || !masterdata.todt) return valert('일자를 입력하시기 바랍니다.')

    if (!confirm('저장하시겠습니까?')) return
	try {
		// 1. 마스터 저장
		const masterpayload = {
			...masterdata,
			actkind: masterdata.actkind + '0',
			cmpycd: authstore.cmpycd,
			userid: authstore.userid,
			fromdt: masterdata.fromdt.replace(/-/g, ''),
			todt: masterdata.todt.replace(/-/g, ''),
            bigo: masterdata.bigo
		}

		const resmaster = await api.post('/haba/HABA_070U_STR', masterpayload)
        const resmdata = resmaster.data?.[0];
		if (resmdata.ret_yn === 'Y' || resmdata.result === 'N') return valerterror(resmdata.ret_msg || resmdata.msg)

		// 2. 디테일 저장
		const griddata = detailgrid?.getData() || []
		for (const n of griddata) {
            let kind = n.upkind || masterdata.actkind
			if (!n.userid) continue

			const detailpayload = {
				actkind: kind + '0',
				cmpycd: authstore.cmpycd,
				prjcd: masterdata.prjcd,
				rowno: n.rowno || '',
				userid: n.userid,
				fromdt: (n.fromdt || '').replace(/-/g, ''),
				todt: (n.todt || '').replace(/-/g, ''),
				bigo: n.bigo || '',
				mgr_userid: authstore.userid
			}
			await api.post('/haba/HABA_071U_STR', detailpayload)
		}

		valert('정상으로 작업이 되었습니다.')
		search()
	} catch (e) { valerterror('저장 실패') }
}

const deletedata = async () => {
	if (!confirm('해당 자료를 삭제하시겠습니까?')) return
	try {
		await api.post('/haba/HABA_070U_STR', {
			actkind: 'D0',
			cmpycd: authstore.cmpycd,
			prjcd: masterdata.prjcd
		})
		valert('삭제되었습니다.')
		initialize()
	} catch (e) { valerterror('삭제 실패') }
}

const initialize = () => {
	resetform(masterdata)
	masterdata.actkind = 'A'
	masterdata.fromdt = firstDay
	masterdata.todt = today
	masterdata.useyn = 'Y'
    masterdata.cmpycd = authstore.cmpycd
	detailgrid?.setData([])
	searchform.prjcd = ''
	searchform.prjnm = ''
}

const addrow = () => {
	detailgrid?.addRow({ upkind: 'A', fromdt: masterdata.fromdt, todt: masterdata.todt }, true)
}

let activerow: any = null

async function openhelp(type: string, row?: any) {
	if (type === 'search_prj') {
		// 🚀 프로젝트 도움창 직접 조회 패턴
		const res = await api.post('/haba/HABA_070U_STR', {
			actkind: 'S0',
			cmpycd: authstore.cmpycd,
			prjcd: ''
		});
		Object.assign(modalprops, {
			title: '프로젝트 선택',
			data: res.data || [],
			columns: [
				{ title: '코드', field: 'prjcd', width: 100, hozAlign: 'center' },
				{ title: '프로젝트명', field: 'prjnm', width: 250 }
			],
			onConfirm: (n: any) => {
				searchform.prjcd = n.prjcd;
				searchform.prjnm = n.prjnm;
				search();
			},
			type: 'table'
		});
		modalvisible.value = true;
	} else if (type === 'user') {
		activerow = row;
		const currentData = activerow.getData();

		// 🚀 요청하신 사용자 도움창 직접 조회 패턴 적용 (HA00_00P_STR 'SD','COIT','','')
		const res = await api.post('/ha00/HA00_00P_STR', {
			gubun: 'SD',
			cmpycd: authstore.cmpycd,
			gbncd: '',
			code: ''
		});

		Object.assign(modalprops, {
			title: '투입인원 선택',
			data: res.data || [],
			columns: [
				{ title: '아이디', field: 'userid', width: 100, hozAlign: 'center' },
				{ title: '성명', field: 'usernm', width: 120, hozAlign: 'center' },
				{ title: '부서', field: 'deptnm', width: 150 }
			],
			onConfirm: (n: any) => {
				activerow.update({
					userid: n.userid || n.codecd,
					usernm: n.usernm || n.codenm,
					upkind: currentData.rowno ? 'U' : 'A'
				});
			},
			type: 'table'
		});
		modalvisible.value = true;
	}
}

onMounted(() => {
	if (detailgridref.value) {
		detailgrid = new Tabulator(detailgridref.value, {
			layout: 'fitColumns',
			height: '100%',
			columnDefaults: { headerSort: false, vertAlign: "middle", headerHozAlign: "center", hozAlign: "center" },
			columns: [
				{ title: "no", field: "rowno", width: 60 },
				{
					title: "투입인원", field: "usernm", width: 250,
					formatter: (cell) => {
						const d = cell.getData()
						return `<div class="d-flex align-items-center gap-1">
									<span class="badge bg-light text-dark border">${d.userid || ''}</span>
									<span>${d.usernm || ''}</span>
									<i class="bi bi-search ms-auto text-primary cursor-pointer"></i>
								</div>`
					},
					cellClick: (e, cell) => {
						if ((e.target as HTMLElement).classList.contains('bi-search')) {
							openhelp('user', cell.getRow())
						}
					}
				},
				{ title: "투입일", field: "fromdt", width: 130, editor: "date" },
				{ title: "종료일", field: "todt", width: 130, editor: "date" },
				{ title: "비고", field: "bigo", editor: "input", hozAlign: "left" },
				{
					title: "삭제", width: 60,
					formatter: () => '<i class="bi bi-x-circle text-danger cursor-pointer"></i>',
					cellClick: async (e, cell) => {
						const d = cell.getData()
						if (d.rowno) {
							if (confirm('이 인원을 프로젝트에서 삭제하시겠습니까?')) {
								try {
									await api.post('/haba/HABA_071U_STR', {
										actkind: 'D0',
										cmpycd: authstore.cmpycd,
										prjcd: masterdata.prjcd,
										rowno: d.rowno,
										mgr_userid: authstore.userid
									})
									cell.getRow().delete()
									valert('삭제되었습니다.')
								} catch (e) { valerterror('삭제 실패') }
							}
						} else {
							cell.getRow().delete()
						}
					}
				}
			]
		})
	}
})
</script>

<style scoped>
.cursor-pointer { cursor: pointer; }
</style>
