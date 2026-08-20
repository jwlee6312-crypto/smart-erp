<!--
	=============================================================
	프로그램명	: 외부매출전표 [디자인 원칙 13가지 + 검색영역/등록영역 분리]
	작성일자	: 2025.02.24
	작성자	    : AI Assistant
	설명        : 외부 매출 확정 내역에 대한 전표 생성 및 조회/등록 영역 분리 UI 적용
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container">
		<!-- 🚀 1. 상단 액션 바 -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-file-earmark-ruled me-2 text-primary" style="font-size: 18px;"></i>
				영업정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				매출관리 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				<span class="text-primary fw-bolder">외부매출전표 (HSIO531U)</span>
			</div>
			<div class="btn-group-erp d-flex gap-1">
				<button class="btn-erp btn-init" @click="initialize">
					<i class="bi bi-arrow-clockwise"></i> 초기화
				</button>
				<button class="btn-erp btn-search" @click="search">
					<i class="bi bi-search"></i> 조회
				</button>
				<button class="btn-erp btn-save" @click="issueSlip">
					<i class="bi bi-file-earmark-check"></i> 전표 발행
				</button>
			</div>
		</div>

		<!-- 💡 2. 메인 컨텐츠 영역 -->
		<div class="flex-grow-1 overflow-auto p-2 d-flex flex-column gap-2">
			<!-- 🅰️ 조회 조건 영역 -->
			<div class="card border shadow-sm overflow-hidden">
				<div class="card-header bg-light py-1 px-3 border-bottom d-flex align-items-center">
					<span class="fw-bold small text-dark"><i class="bi bi-search me-1"></i> 조회 조건</span>
				</div>
				<div class="card-body p-0">
					<table class="erp-table-full">
						<colgroup>
							<col style="width: 10%" />
							<col style="width: 23%" />
							<col style="width: 10%" />
							<col style="width: 23%" />
							<col style="width: 34%" />
						</colgroup>
						<tbody>
							<tr>
								<th class="required">판매부서</th>
								<td>
									<div class="input-group input-group-sm flex-nowrap" style="max-width: 220px;">
										<input v-model="searchData.deptcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
										<input v-model="searchData.deptnm" type="text" class="form-control" placeholder="부서 선택" @keyup.enter="openHelp('SEARCH_DEPT')" />
										<button class="btn btn-outline-secondary px-2" @click="openHelp('SEARCH_DEPT')"><i class="bi bi-search"></i></button>
									</div>
								</td>
								<th class="required">정산일자</th>
								<td>
									<div class="d-flex align-items-center gap-1 flex-grow-1" style="max-width: 260px;">
										<input v-model="fromdt" type="date" class="form-control form-control-sm" />
										<span class="text-muted">~</span>
										<input v-model="todt" type="date" class="form-control form-control-sm" />
									</div>
								</td>
								<td></td>
							</tr>
						</tbody>
					</table>
				</div>
			</div>

			<!-- 🅱️ 전표 발행 설정 (등록 영역) -->
			<div class="card border shadow-sm overflow-hidden">
				<div class="card-header py-1 px-3 border-bottom d-flex align-items-center" style="background-color: #f0f7ff !important;">
					<span class="fw-bold small text-primary"><i class="bi bi-pencil-square me-1"></i> 전표 발행 정보 (등록용)</span>
				</div>
				<div class="card-body p-0">
					<table class="erp-table-full">
						<colgroup>
							<col style="width: 10%" />
							<col style="width: 23%" />
							<col style="width: 10%" />
							<col style="width: 23%" />
							<col style="width: 34%" />
						</colgroup>
						<tbody>
							<tr>
								<th class="required bg-light-primary">발행부서</th>
								<td>
									<div class="input-group input-group-sm flex-nowrap" style="max-width: 220px;">
										<input v-model="registerData.deptcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
										<input v-model="registerData.deptnm" type="text" class="form-control" placeholder="발행부서 선택" @keyup.enter="openHelp('REG_DEPT')" />
										<button class="btn btn-outline-secondary px-2" @click="openHelp('REG_DEPT')"><i class="bi bi-search"></i></button>
									</div>
								</td>
								<th class="required bg-light-primary">발행일자</th>
								<td>
									<input v-model="uislipymd" type="date" class="form-control form-control-sm" style="max-width: 140px;" />
								</td>
								<td></td>
							</tr>
						</tbody>
					</table>
				</div>
			</div>

			<!-- Ⓒ 데이터 그리드 영역 -->
			<div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column">
				<div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
					<span class="fw-bold small text-dark d-flex align-items-center">
						<i class="bi bi-list-check me-2 text-primary"></i> 전표 발행 대기 목록
					</span>
				</div>
				<div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
					<div ref="gridElement" class="tabulator-instance flex-grow-1"></div>
				</div>
				<!-- 🚀 하단 요약 정보 (Footer) -->
				<div class="card-footer bg-light border-top py-1 px-3 d-flex justify-content-between align-items-center flex-shrink-0">
					<div class="small text-muted">
						[ 선택: <span class="fw-bold text-primary">{{ activeItemCount }}</span> 건 ]
					</div>
					<div class="d-flex gap-4 small">
						<div class="d-flex align-items-center">
							<span class="me-2 text-muted">공급가:</span>
							<span class="fw-bold text-dark">{{ formatNumber(sumData.spysum) }}</span>
						</div>
						<div class="d-flex align-items-center border-start ps-4">
							<span class="me-2 text-muted text-info">부가세:</span>
							<span class="fw-bold text-info">{{ formatNumber(sumData.vatsum) }}</span>
						</div>
						<div class="d-flex align-items-center border-start ps-4">
							<span class="me-2 text-muted text-danger">합계금액:</span>
							<span class="fw-bold text-danger">{{ formatNumber(sumData.totsum) }}</span>
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>

	<Modal v-model:visible="modalVisible" :modalProps="modalProps" />
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, computed, nextTick } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import type { ModalProps } from '@/types/modal'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

const now = new Date()
const initymd = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}`
const initfromdt = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}01`

// 1. 상태 관리
const searchData = reactive({
  deptcd: authStore.deptcd, deptnm: authStore.deptnm,
  fromdt: initfromdt, todt: initymd, salsemp: '000'
})

const registerData = reactive({
  slipymd: initymd, deptcd: authStore.deptcd, deptnm: authStore.deptnm,
  clsymd: '', sclsym: ''
})

const sumData = reactive({ spysum: 0, vatsum: 0, totsum: 0 })

const fromdt = computed({ get: () => formatDate(searchData.fromdt, '-'), set: (v) => searchData.fromdt = v.replace(/-/g, '') })
const todt = computed({ get: () => formatDate(searchData.todt, '-'), set: (v) => searchData.todt = v.replace(/-/g, '') })
const uislipymd = computed({ get: () => formatDate(registerData.slipymd, '-'), set: (v) => registerData.slipymd = v.replace(/-/g, '') })

const gridElement = ref<HTMLElement | null>(null)
const grid = ref<Tabulator | null>(null)
const activeItemCount = ref(0)

// 2. 그리드 초기화
const initGrid = () => {
  if (!gridElement.value) return
  grid.value = new Tabulator(gridElement.value, {
    layout: "fitColumns", height: "100%", selectable: true,
    columnDefaults: { headerSort: false, headerHozAlign: "center", hozAlign: "center", vertAlign: "middle", minWidth: 100 },
    columns: [
      {
        title: "선택", formatter: "rowSelection", titleFormatter: "rowSelection", width: 40, hozAlign: "center",
        headerClick: () => toggleAllRows()
      },
      { title: "발행일", field: "jsanymd", width: 150, formatter: (c) => formatDate(c.getValue(), '-') },
      { title: "부서", field: "deptnm", width: 250 },
      { title: "거래처", field: "custnm", minWidth: 200, hozAlign: "left", cssClass: "fw-bold" },
      { title: "사업장", field: "unitnm", width: 200 },
      { title: "유형", field: "vattypenm", width: 150 },
      { title: "공급가", field: "spyamt", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "부가세", field: "vatamt", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "합계", field: "jsansum", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "text-primary fw-bold" }
    ]
  })

  grid.value.on('rowSelectionChanged', (data) => {
    activeItemCount.value = data.length
    sumData.spysum = data.reduce((acc, cur: any) => acc + (Number(cur.spyamt) || 0), 0)
    sumData.vatsum = data.reduce((acc, cur: any) => acc + (Number(cur.vatamt) || 0), 0)
    sumData.totsum = data.reduce((acc, cur: any) => acc + (Number(cur.jsansum) || 0), 0)
  })
}

const toggleAllRows = () => {
	if (!grid.value) return
	const rows = grid.value.getRows()
	const allSelected = grid.value.getSelectedRows().length === rows.length
	if (allSelected) grid.value.deselectRow()
	else grid.value.selectRow()
}

// 3. 기능 구현
async function search() {
  if (!searchData.deptcd) return vAlertError('판매부서를 선택하세요.')
  try {
    const res = await api.post('/hsio/HSIO_531U_STR', {
      actkind: 'S0',
      cmpycd: authStore.cmpycd,
      iogbn: '200',
      fromdt: searchData.fromdt,
      todt: searchData.todt,
      deptcd: searchData.deptcd,
      salsemp: searchData.salsemp === '000' ? '' : searchData.salsemp
    })
    if (grid.value) {
      const data = (res.data || []).map((item: any) => {
          item.jsansum = (Number(item.spyamt) || 0) + (Number(item.vatamt) || 0)
          return item
      })
      grid.value.setData(data)
      vAlert('조회되었습니다.')
    }
  } catch (e) { vAlertError('조회 실패') }
}

/**
 * 🚀 전표 발행 처리 (ASP 로직 반영 + 소문자 통일)
 */
async function issueSlip() {
  const selected = grid.value?.getSelectedData()
  if (!selected || selected.length === 0) return vAlertError('전표발행 대상을 선택하시기 바랍니다.')

  // 마감 체크
  if (registerData.slipymd <= registerData.clsymd) return vAlertError(`회계 마감된 일자입니다. (마감: ${registerData.clsymd})`)
  if (registerData.sclsym && registerData.sclsym >= registerData.slipymd.substring(0, 6)) return vAlertError(`영업 마감된 월입니다. (마감: ${registerData.sclsym})`)

  // 데이터 검증
  for (const item of selected) {
    if (!item.outcustcd) return vAlertError('외부 거래처 등록에서 등록 후 처리 바랍니다.')
    if (!item.custcd) return vAlertError('거래처코드가 없는 항목이 있습니다.')
    if (Number(item.jsansum) === 0) return vAlertError('합계금액이 0인 항목은 발행할 수 없습니다.')
  }

  if (!confirm('전표를 발행 하시겠습니까?')) return

  try {
    const slipymd = registerData.slipymd
    const business = `${slipymd.substring(0, 4)}년 ${slipymd.substring(4, 6)}월 매출 건`

    const payload = {
        mst: {
            slipymd: slipymd,
            cmpycd: authStore.cmpycd,
            deptcd: registerData.deptcd,
            fromdt: searchData.fromdt,
            todt: searchData.todt,
            business: business
        },
        items: selected.map(item => ({
            deptcd: item.deptcd,
            jsanym: item.jsanym,
            jsanno: item.jsanno,
            jsanymd: item.jsanymd,
            spyamt: item.spyamt,
            vatamt: item.vatamt,
            custcd: item.custcd,
            taxunit: item.taxunit,
            vattype: item.vattype
        }))
    }

    const res = await api.post('/hsio/HSIO_531U_SAVE', payload)
    const { slipno, res: status } = res.data

    if (status === 'OK') {
        vAlert('정상적으로 발행되었습니다.')
        // 전표 인쇄 팝업 (ASP 로직 반영)
        const printUrl = `../HASL/HASL_SLIP_PRINT_OUT.asp?slipgu=010&slipymd=${slipymd}&slipno=${slipno}&deptcd=${registerData.deptcd}`
        window.open(printUrl, '전표인쇄', 'left=10,top=10,width=700,height=650,scrollbars=yes')
        search()
    } else {
        vAlertError('전표 발행 중 오류가 발생했습니다.')
    }
  } catch (e: any) {
    vAlertError(e.response?.data?.message || e.message || '전표 발행 실패')
  }
}

function initialize() {
  resetForm(searchData)
  Object.assign(searchData, { deptcd: authStore.deptcd, deptnm: authStore.deptnm, fromdt: initfromdt, todt: initymd, salsemp: '000' })
  Object.assign(registerData, { slipymd: initymd, deptcd: authStore.deptcd, deptnm: authStore.deptnm })
  if (grid.value) grid.value.clearData()
}

// 4. 팝업 및 기초정보
const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function openHelp(type: string) {
  if (type.includes('DEPT')) {
    Object.assign(modalProps, {
      title: '부서 선택', path: '/ha00/HA00_00P_STR', defaultField: 'deptnm', large: false,
      data: { gubun: 'D0', cmpycd: authStore.cmpycd },
      columns: [{ title: '코드', field: 'deptcd', width: 80 }, { title: '부서명', field: 'deptnm', width: 180 }],
      onConfirm: (data: any) => {
        if (type === 'SEARCH_DEPT') { searchData.deptcd = data.deptcd; searchData.deptnm = data.deptnm }
        else { registerData.deptcd = data.deptcd; registerData.deptnm = data.deptnm }
      }
    })
    modalVisible.value = true
  }
}

const formatDate = (v: any, sep: string) => v && v.length === 8 ? `${v.substring(0, 4)}${sep}${v.substring(4, 6)}${sep}${v.substring(6, 8)}` : v
const formatNumber = (val: any) => new Intl.NumberFormat().format(Number(val) || 0)

onMounted(async () => {
  api.get('/hp00/HP00_000S_STR', { params: { gubun: 'cl', cmpycd: authStore.cmpycd } }).then(r => {
    if (r.data?.length) {
      const d = r.data[0]
      registerData.clsymd = String(d.clsymd || '').trim()
      registerData.sclsym = String(d.sclsym || '').trim()
    }
  })
  nextTick(() => initGrid())
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; border-bottom: 3px solid #005a9f !important; }
.bg-light-primary { background-color: #f8fbff !important; }
</style>
