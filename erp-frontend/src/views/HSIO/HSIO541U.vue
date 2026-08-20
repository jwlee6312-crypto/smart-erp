<!--
	=============================================================
	프로그램명	: 매출전표취소 [디자인 원칙 13가지 + 그리드 표준화]
	작성일자	: 2025.02.24
	작성자	    : AI Assistant
	설명        : 발행된 매출 전표 조회 및 취소 처리 UI 적용
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="erp-container">
		<!-- 🚀 1. 상단 액션 바 -->
		<div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
			<div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
				<i class="bi bi-file-earmark-x me-2 text-danger" style="font-size: 18px;"></i>
				영업정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				매출관리 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
				<span class="text-primary fw-bolder">매출전표취소 (HSIO541U)</span>
			</div>
			<div class="btn-group-erp d-flex gap-1">
				<button class="btn-erp btn-init" @click="initialize">
					<i class="bi bi-arrow-clockwise"></i> 초기화
				</button>
				<button class="btn-erp btn-search" @click="search">
					<i class="bi bi-search"></i> 조회
				</button>
				<button class="btn-erp btn-danger" @click="deleteSlips">
					<i class="bi bi-trash"></i> 전표 삭제
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
										<input v-model="searchdata.deptcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
										<input v-model="searchdata.deptnm" type="text" class="form-control" placeholder="부서 선택" @keyup.enter="openHelp('SEARCH_DEPT')" />
										<button class="btn btn-outline-secondary px-2" @click="openHelp('SEARCH_DEPT')"><i class="bi bi-search"></i></button>
									</div>
								</td>
								<th class="required">발행일자</th>
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

			<!-- Ⓒ 데이터 그리드 영역 -->
			<div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column">
				<div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
					<span class="fw-bold small text-dark d-flex align-items-center">
						<i class="bi bi-list-check me-2 text-primary"></i> 전표 발행 내역
					</span>
				</div>
				<div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
					<div ref="gridElement" class="tabulator-instance flex-grow-1"></div>
				</div>
				<!-- 🚀 하단 요약 정보 (Footer) -->
				<div class="card-footer bg-light border-top py-1 px-3 d-flex justify-content-between align-items-center flex-shrink-0">
					<div class="small text-muted">
						[ 선택: <span class="fw-bold text-primary">{{ activeitemcount }}</span> 건 ]
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
const searchdata = reactive({
  deptcd: authStore.deptcd, deptnm: authStore.deptnm,
  fromdt: initfromdt, todt: initymd
})

const closinginfo = reactive({ clsymd: '', sclsym: '' })
const autoslip = ref('N')
const sumData = reactive({ spysum: 0, vatsum: 0, totsum: 0 })

const fromdt = computed({ get: () => formatDate(searchdata.fromdt, '-'), set: (v) => searchdata.fromdt = v.replace(/-/g, '') })
const todt = computed({ get: () => formatDate(searchdata.todt, '-'), set: (v) => searchdata.todt = v.replace(/-/g, '') })

const gridElement = ref<HTMLElement | null>(null)
const grid = ref<Tabulator | null>(null)
const activeitemcount = ref(0)

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
      { title: "전표번호", field: "slip_full", width: 200, hozAlign: "center", cssClass: "fw-bold" },
      {
        title: "외부회계전송", field: "sendyn", width: 120, hozAlign: "center",
        formatter: (c) => (c.getValue() === 'Y') ? '<span class="badge bg-success">전송완료</span>' : '<span class="badge bg-secondary">미전송</span>'
      },
      { title: "발행부서", field: "deptnm", width: 250 },
      { title: "거래처", field: "custnm", minWidth: 200, hozAlign: "left", cssClass: "fw-bold" },
      { title: "유형", field: "vattypenm", width: 150 },
      { title: "공급가", field: "spyamt", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "부가세", field: "vatamt", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "합계", field: "jsansum", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "text-primary fw-bold" }
    ]
  })

  grid.value.on('rowSelectionChanged', (data) => {
    // 마감 체크 필터링 (선택 시점에 알림은 생략하고 실제 삭제 시 체크)
    activeitemcount.value = data.length
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

const checkCanCancel = (row: any) => {
    const slipymd = row.slipymd || ''
    const sendyn = row.sendyn || 'N'
    const cfmyn = row.cfmyn || 'N'

    if (cfmyn === 'Y' || sendyn === 'Y') return "전송완료된 전표입니다. 취소할 수 없습니다."
    if (slipymd === '00000000' || !slipymd) return "전표가 발행되지 않은 자료입니다."

    const slipym = slipymd.substring(0, 6)
    if (closinginfo.sclsym && slipym <= closinginfo.sclsym) return `영업 마감된 월입니다. (마감: ${closinginfo.sclsym})`
    if (closinginfo.clsymd && slipymd <= closinginfo.clsymd) return `회계 마감된 일자입니다. (마감: ${closinginfo.clsymd})`
    return true
}

// 3. 기능 구현
async function search() {
  if (!searchdata.deptcd) return vAlertError('판매부서를 선택하세요.')
  try {
    const res = await api.post('/hsio/HSIO_541U_STR', {
      actkind: 'S0', cmpycd: authStore.cmpycd, iogbn: '200',
      fromdt: searchdata.fromdt, todt: searchdata.todt,
      deptcd: searchdata.deptcd
    })
    if (grid.value) {
      // 🚀 백엔드 공통 처리에서 데이터가 없을 때 반환하는 [{res: 'OK'}] 필터링
      const rawData = (res.data || []).filter((row: any) => !(row.res === 'OK' || row.RES === 'OK'))

      const mappedData = rawData.map((item: any) => {
          item.jsansum = (Number(item.spyamt) || 0) + (Number(item.vatamt) || 0)
          item.slip_full = item.slipymd > '00000000' ? `${formatDate(item.slipymd, '-')}-${item.slipno}` : ''
          return item
      })
      grid.value.setData(mappedData)
      vAlert(mappedData.length > 0 ? '조회되었습니다.' : '조회된 내역이 없습니다.')
    }
  } catch (e) { vAlertError('조회 실패') }
}

/**
 * 🚀 전표 삭제 처리 (ASP 로직 반영 + 소문자 통일)
 */
async function deleteSlips() {
  const selected = grid.value?.getSelectedData()
  if (!selected || selected.length === 0) return vAlertError('취소할 전표를 선택하세요.')

  // 마감 체크
  for (const item of selected) {
      const can = checkCanCancel(item)
      if (can !== true) return vAlertError(can)
  }

  if (!confirm('선택한 전표를 삭제하시겠습니까?')) return

  try {
    const payload = {
        cmpycd: authStore.cmpycd,
        fromdt: searchdata.fromdt,
        todt: searchdata.todt,
        autoSlip: autoslip.value,
        items: selected.map(item => ({
            slipymd: item.slipymd,
            slipno: item.slipno,
            deptcd: item.deptcd
        }))
    }

    const res = await api.post('/hsio/HSIO_541U_CANCEL', payload)
    const { res: status } = res.data

    if (status === 'OK') {
        vAlert('정상적으로 처리되었습니다.')
        search()
    } else {
        vAlertError('전표 삭제 중 오류가 발생했습니다.')
    }
  } catch (e: any) {
      vAlertError(e.response?.data?.message || e.message || '전표 삭제 중 오류 발생')
  }
}

function initialize() {
  resetForm(searchdata)
  Object.assign(searchdata, { deptcd: authStore.deptcd, deptnm: authStore.deptnm, fromdt: initfromdt, todt: initymd })
  if (grid.value) grid.value.clearData()
}

// 4. 팝업 및 기초정보
const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function openHelp(type: string) {
  if (type === 'SEARCH_DEPT') {
    Object.assign(modalProps, {
      title: '부서 선택', path: '/ha00/HA00_00P_STR', defaultField: 'deptnm', large: false,
      data: { gubun: 'D0', cmpycd: authStore.cmpycd },
      columns: [{ title: '코드', field: 'deptcd', width: 80 }, { title: '부서명', field: 'deptnm', width: 180 }],
      onConfirm: (data: any) => { searchdata.deptcd = data.deptcd; searchdata.deptnm = data.deptnm }
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
        closinginfo.clsymd = String(d.clsymd || d.CLSYMD || '').trim()
        closinginfo.sclsym = String(d.sclsym || d.SCLSYM || '').trim()
    }
  })
  api.get('/ha00/HA00_010S_STR', { params: { gubun: 'p1', cmpycd: authStore.cmpycd } }).then(r => {
    if (r.data?.length) {
        const d = r.data[0]
        autoslip.value = (d.slipyn || d.slipyn || 'N').toLowerCase()
    }
  })
  nextTick(() => initGrid())
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; border-bottom: 3px solid #005a9f !important; }
</style>
