<!--
	=============================================================
	프로그램명: 카드지불처리 (HAFN620U)
	작성일자	: 2025.03.14
	작성자    : AI Assistant
	설명        : 카드사별 미지급 내역을 조회하여 선택 결제 및 전표 발행
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom bg-white py-2 px-3 sticky-top shadow-sm">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-credit-card me-2 text-primary" style="font-size: 18px;"></i>
        자금관리 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        <span class="text-primary fw-bolder">카드지불처리 (HAFN620U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-save" @click="save" :disabled="selectedRows.length === 0">저장 (전표발행)</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 조회 필터 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
              <col style="width: 10%;" /><col style="width: 23%;" />
              <col style="width: 10%;" /><col style="width: 23%;" />
              <col style="width: 10%;" /><col style="width: 24%;" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light border-end">기준일자</th>
                <td class="border-end px-2">
                  <input v-model="searchForm.todt" type="date" class="form-control form-control-sm" style="max-width: 150px;" />
                </td>
                <th class="text-center bg-light border-end">카드사</th>
                <td class="border-end px-2">
                  <div class="input-group input-group-sm">
                    <input v-model="searchForm.custcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
                    <input v-model="searchForm.custnm" type="text" class="form-control" placeholder="카드사 선택" @keydown.enter="openHelp('SEARCH_CUST')" />
                    <button class="btn btn-outline-secondary px-2" @click="openHelp('SEARCH_CUST')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="text-center bg-light border-end">카드번호</th>
                <td class="px-2">
                  <div class="input-group input-group-sm">
                    <input v-model="searchForm.mgtno" type="text" class="form-control text-center bg-light" style="max-width: 150px;" readonly />
                    <input v-model="searchForm.mgtnm" type="text" class="form-control" placeholder="카드 선택" @keydown.enter="openHelp('SEARCH_MGT')" />
                    <button class="btn btn-outline-secondary px-2" @click="openHelp('SEARCH_MGT')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [중간] 지불 마스터 정보 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense w-100">
            <colgroup>
              <col style="width: 110px;" /><col />
              <col style="width: 110px;" /><col />
              <col style="width: 110px;" /><col />
            </colgroup>
            <tbody>
              <tr>
                <th class="bg-light text-center border-end">발행부서</th>
                <td class="border-end px-2">
                  <div class="input-group input-group-sm">
                    <input v-model="voucherForm.deptcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
                    <input v-model="voucherForm.deptnm" type="text" class="form-control" @keydown.enter="openHelp('DEPT')" />
                    <button class="btn btn-outline-secondary px-2" @click="openHelp('DEPT')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="bg-light text-center border-end fw-bold text-info">지불일자</th>
                <td class="border-end px-2">
                  <input v-model="voucherForm.payymd" type="date" class="form-control form-control-sm" />
                </td>
                <th class="bg-light text-center border-end text-primary fw-bold">지급수수료</th>
                <td class="px-2">
                  <input v-model="voucherForm.jiamt" type="number" class="form-control form-control-sm text-end fw-bold" @input="updateTotalAmount" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 d-flex flex-column overflow-hidden">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0 text-secondary">
          <span class="fw-bold small"><i class="bi bi-list-check me-1 text-primary"></i> 카드 미지급 내역 리스트</span>
          <div class="d-flex gap-3 align-items-center fw-bold">
            <span class="text-muted small">결제구좌: {{ voucherForm.gujano || '조회 후 표시' }}</span>
            <span class="text-primary">총 결제금액: {{ formatMoney(totalPayAmt) }}</span>
          </div>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="mainGridRef" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>
    </div>
  </div>

  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, nextTick } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import type { ModalProps } from '@/types/modal'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

const today = new Date().toISOString().substring(0, 10)
const firstDay = new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString().substring(0, 10)

// 검색 조건
const searchForm = reactive({
	todt: today,
	custcd: '',
	custnm: '',
	mgtno: '',
	mgtnm: ''
})

// 전표 발행 정보
const voucherForm = reactive({
	deptcd: authStore.deptcd,
	deptnm: authStore.deptnm,
	payymd: today,
	jiamt: 0,
	gujano: '',
	clsymd: '00000000'
})

const mainGridRef = ref<HTMLDivElement | null>(null)
let mainGrid: Tabulator | null = null
const selectedRows = ref<any[]>([])

const totalPayAmt = computed(() => {
	const subtotal = selectedRows.value.reduce((acc, row) => acc + (Number(row.janamt) || 0), 0)
	return subtotal + Number(voucherForm.jiamt || 0)
})

const formatMoney = (val: any) => Number(val || 0).toLocaleString()

const search = async () => {
	if (!searchForm.custcd) return vAlert('카드사를 선택하십시오.')
	try {
		const res = await api.post('/hafn/HAFN_620U_STR', {
			actkind: 'S0',
			cmpycd: authStore.cmpycd,
			fromdt: firstDay.replace(/-/g, ''),
			todt: searchForm.todt.replace(/-/g, ''),
			custcd: searchForm.custcd,
			mgtno: searchForm.mgtno
		})

		const data = (res.data || []).map((item: any) => {
            return {
                ...item,
                slip_key: `${item.slipymd}-${item.slipno}-${item.srowno}`,
                cardno: item.mgtno,
                cardnm: item.cardnm,
                unpaid_amt: Number(item.upyamt || 0),
                paid_amt: Number(item.payamt || 0),
                janamt: Number(item.janamt || 0),
                gujano: item.gujano,
                SELECT: true
            }
        })

		if (data.length > 0) {
			voucherForm.gujano = data[0].gujano
		} else {
			voucherForm.gujano = ''
		}

		mainGrid?.setData(data)
		updateTotalAmount()
		vAlert('조회되었습니다.')
	} catch (e) { vAlertError('조회 실패') }
}

const updateTotalAmount = () => {
	const data = mainGrid?.getData() || []
	selectedRows.value = data.filter((r: any) => r.SELECT)
}

const save = async () => {
	if (voucherForm.payymd.replace(/-/g, '') <= voucherForm.clsymd) {
		return vAlert('회계 마감된 일자입니다.')
	}
	if (!voucherForm.gujano) return vAlert('결제구좌 정보가 없습니다. (기초 정보 확인 필요)')

	if (!confirm('선택한 내역에 대해 결제 전표를 발행하시겠습니까?')) return

	try {
		const details = []

		// 1. 차변 (Debits): 카드 미지급금 반제
		selectedRows.value.forEach(row => {
			details.push({
				upkind: 'A', dbcr: 'D',
				acctcd: row.acctcd,
				remark: `${searchForm.custnm} 카드대금 지급`,
				amount: row.janamt,
				custcd: searchForm.custcd,
				mgtno: row.cardno,
				sslipno: `${row.slipymd}${row.slipno}${row.srowno}`
			})
		})

		// 지급수수료 차변 추가
		if (voucherForm.jiamt > 0) {
			details.push({
				upkind: 'A', dbcr: 'D',
				acctcd: '6355',
				remark: '카드대금 수수료',
				amount: voucherForm.jiamt,
				custcd: searchForm.custcd
			})
		}

		// 2. 대변 (Credits): 보통예금 출금
		details.push({
			upkind: 'A', dbcr: 'C',
			acctcd: '1120',
			remark: `${searchForm.custnm} 카드 출금결제`,
			amount: totalPayAmt.value,
			mgtno: voucherForm.gujano
		})

		const payload = {
			actkind: 'A',
			MASTER: {
				cmpycd: authStore.cmpycd,
				slipymd: voucherForm.payymd.replace(/-/g, ''),
				acctymd: voucherForm.payymd.replace(/-/g, ''),
				deptcd: voucherForm.deptcd,
				business: `${searchForm.custnm}(${searchForm.mgtno}) 카드 결제 건`,
				slipgu: '010'
			},
			DETAILS: details
		}

		const res = await api.post('/hasl/HASL_010U_SAVE', payload)
		vAlert('전표가 발행되었습니다.')
		if (res.data.slipno) {
			window.open(`/hasl/HASL_SLIP_PRINT?slipgu=010&slipymd=${payload.MASTER.slipymd}&slipno=${res.data.slipno}&deptcd=${voucherForm.deptcd}`)
		}
		initialize()
	} catch (e) { vAlertError('저장 실패') }
}

const initialize = () => {
	resetForm(searchForm)
	resetForm(voucherForm)
	searchForm.todt = today
	voucherForm.payymd = today
	voucherForm.deptcd = authStore.deptcd
	voucherForm.deptnm = authStore.deptnm
	mainGrid?.clearData()
	selectedRows.value = []
}

// 팝업 설정
const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function openHelp(type: string) {
	if (type === 'SEARCH_CUST') {
		Object.assign(modalProps, {
			title: '카드사 선택', path: '/ha00/HA00_00P_STR',
			data: { gubun: 'C3', custgbn: '020', cmpycd: authStore.cmpycd },
			columns: [{ title: '코드', field: 'bankcd', width: 80 }, { title: '카드사명', field: 'banknm', width: 180 }],
			onConfirm: (item: any) => {
                searchForm.custcd = item.bankcd; searchForm.custnm = item.banknm; searchForm.mgtno = ''; searchForm.mgtnm = '';
            }
		})
	} else if (type === 'SEARCH_MGT') {
		if (!searchForm.custcd) return vAlert('카드사를 먼저 선택하십시오.')
		Object.assign(modalProps, {
			title: '카드번호 선택', path: '/ha00/HA00_00P_STR',
			data: { gubun: 'M0', cmpycd: authStore.cmpycd, gbncd: '040', remark: searchForm.custcd },
			columns: [{ title: '카드번호', field: 'mgtno', width: 150 }, { title: '카드명', field: 'mgtnm', width: 150 }],
			onConfirm: (item: any) => {
                searchForm.mgtno = item.mgtno; searchForm.mgtnm = item.mgtnm
            }
		})
	} else if (type === 'DEPT') {
		Object.assign(modalProps, {
			title: '부서 선택', path: '/ha00/HA00_00P_STR',
			data: { gubun: 'D0', cmpycd: authStore.cmpycd, search: voucherForm.deptnm },
			columns: [{ title: '코드', field: 'deptcd', width: 80 }, { title: '부서명', field: 'deptnm', width: 180 }],
			onConfirm: (item: any) => {
                voucherForm.deptcd = item.deptcd; voucherForm.deptnm = item.deptnm
            }
		})
	}
	modalVisible.value = true
}

onMounted(() => {
	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns', height: '100%',
			columnDefaults: { headerSort: false, vertAlign: "middle" },
			columns: [
				{ title: "선택", field: "SELECT", width: 40, hozAlign: "center", formatter: "tickCross", editor: true, cellClick: (e, cell) => { cell.setValue(!cell.getValue()); updateTotalAmount() } },
				{
					title: "발생전표번호", field: "slip_key", width: 130, hozAlign: "center",
					formatter: (cell) => { const d = cell.getData(); return `${d.slipymd}-${d.slipno}-${d.srowno}` }
				},
				{ title: "적요", field: "remark", minWidth: 200 },
				{ title: "카드번호", field: "cardno", width: 150, hozAlign: "center" },
				{ title: "카드명", field: "cardnm", width: 120 },
				{ title: "미지불액", field: "unpaid_amt", width: 110, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
				{ title: "잔액", field: "janamt", width: 110, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "fw-bold text-danger" }
			]
		})
	}

	// 마감 정보 로드
	api.post('/ha00/HA00_010S_STR', { gubun: 'P1', cmpycd: authStore.cmpycd }).then(res => {
		if (res.data && res.data.length > 0) {
            const item = res.data[0]
			voucherForm.clsymd = item.clsymd || '00000000'
		}
	})
})
</script>

<style scoped>
.erp-label { min-width: 80px; font-weight: 500; font-size: 13px; }
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
