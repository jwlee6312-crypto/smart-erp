<!--
	=============================================================
	프로그램명	: 공정별제조비용 조정 (HFMF207U)
	작성일자	: 2025.02.24
	설명        : 공정별 제조비용 조정 관리 (HSOD100U 표준 디자인 및 인터셉터 표준화 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-sliders me-2 text-primary" style="font-size: 18px;"></i>
        원가관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        원가결산 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">공정제조비용관리 (HFMF207U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="handleSearch">조회</button>
        <button class="btn-erp btn-save" @click="save">저장</button>
        <button class="btn-erp btn-delete" @click="deleteData" :disabled="!selectedRowCount">삭제</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 조회 필터 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
                <col style="width: 10%" /><col style="width: 30%" />
                <col style="width: 10%" /><col style="width: 30%" />
                <col style="width: 20%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light required">조회년월</th>
                <td>
                  <input v-model="searchForm.ym" type="month" class="form-control form-control-sm" style="max-width: 150px;" @change="handleSearch" />
                </td>
                <th class="text-center bg-light required">계정과목</th>
                <td>
                  <select v-model="searchForm.acct" class="form-select form-select-sm" @change="handleSearch">
                    <option v-for="opt in acctOptions" :key="opt.acct" :value="opt.acct">{{ opt.acctnm }}</option>
                  </select>
                </td>
                <td class="text-end pe-3 border-start-0">
                  <span v-if="clsInfo.wclsym" class="badge bg-danger-subtle text-danger border border-danger-subtle">마감월: {{ clsInfo.wclsym }}</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column grid-container-right">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>공정별 비용 조정 목록</span>
          <div class="text-muted small">
            <i class="bi bi-info-circle me-1"></i>조정액 입력 후 저장 버튼을 클릭하세요.
          </div>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="mainGridRef" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { useCommonHelp } from '@/composables/useCommonHelp'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps } = useCommonHelp()

const clsInfo = reactive({ wclsym: '' })
const acctOptions = ref<any[]>([])
const searchForm = reactive({
	ym: new Date().toISOString().substring(0, 7),
	acct: ''
})

const mainGridRef = ref<HTMLElement | null>(null)
let mainGrid: Tabulator | null = null
const selectedRowCount = ref(0)

const loadInitData = async () => {
	try {
		const [cls, accts] = await Promise.all([
			api.get('/hp00/HP00_000S_STR', { params: { gubun: 'CL', cmpycd: authStore.cmpycd } }),
			api.post('/hfba/SELECT_ACCT_LIST', { cmpycd: authStore.cmpycd })
		])
        if (cls.data?.length > 0) {
            clsInfo.wclsym = cls.data[0].wclsym || cls.data[0].codecd || ''
        }
		acctOptions.value = (accts.data || []).map((item: any) => ({
			acct: String(item.acct || '').trim(),
			acctnm: String(item.acctnm || '').trim()
		}))
		if (acctOptions.value.length > 0) searchForm.acct = acctOptions.value[0].acct
	} catch (e) { vAlertError('기초 데이터 로드 실패') }
}

const initialize = () => {
  resetForm(searchForm)
  searchForm.ym = new Date().toISOString().substring(0, 7)
  if (acctOptions.value.length > 0) searchForm.acct = acctOptions.value[0].acct
  mainGrid?.clearData()
  selectedRowCount.value = 0
}

const handleSearch = async () => {
	if (!searchForm.acct) return
	try {
		const ym = searchForm.ym.replace('-', '')
		const { data } = await api.post('/hfmf/FMF2070U_STR', {
			cmpycd: String(authStore.cmpycd), actkind: 'S0', ym: String(ym), costcd: '10000',
            acct: String(searchForm.acct), linecd: '', adstamt: '0', userid: String(authStore.userid)
		})
		mainGrid?.setData(data.map((i: any) => ({ ...i, _status: '' })))
        selectedRowCount.value = 0
        vAlert('조회되었습니다.')
	} catch (e) { vAlertError('조회 실패') }
}

const save = async () => {
	const ym = searchForm.ym.replace('-', '')
	if (clsInfo.wclsym && clsInfo.wclsym >= ym) return vAlertError('원가마감 되었습니다.')

	const selectedData = mainGrid?.getSelectedData() || []
	if (selectedData.length === 0) return vAlertError('저장할 행을 선택하세요.')

	if (!confirm('저장하시겠습니까?')) return

	try {
		for (const row of selectedData) {
			await api.post('/hfmf/FMF2070U_STR', {
				cmpycd: String(authStore.cmpycd), actkind: 'A0', ym: String(ym), costcd: '10000',
				acct: String(searchForm.acct), linecd: String(row.linecd || ''),
				adstamt: String(row.adstamt || '0'), userid: String(authStore.userid)
			})
		}
		vAlert('저장되었습니다.'); handleSearch();
	} catch (e) { vAlertError('저장 실패') }
}

const deleteData = async () => {
    const ym = searchForm.ym.replace('-', '')
	if (clsInfo.wclsym && clsInfo.wclsym >= ym) return vAlertError('원가마감 되었습니다.')

	const selectedData = mainGrid?.getSelectedData() || []
	if (!confirm('선택한 조정 정보를 삭제하시겠습니까?')) return

	try {
		for (const row of selectedData) {
			await api.post('/hfmf/FMF2070U_STR', {
				cmpycd: String(authStore.cmpycd), actkind: 'D0', ym: String(ym), costcd: '10000',
				acct: String(searchForm.acct), linecd: String(row.linecd || ''),
				adstamt: '0', userid: String(authStore.userid)
			})
		}
		vAlert('삭제되었습니다.'); handleSearch();
	} catch (e) { vAlertError('삭제 실패') }
}

const initGrid = () => {
	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns', height: '100%', selectable: true,
			columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
			columns: [
				{ title: "선택", width: 40, hozAlign: "center", formatter: "rowSelection", titleFormatter: "rowSelection" },
                { title: "상태", field: "_status", width: 60, hozAlign: "center", formatter: (c) => {
                    const v = c.getValue();
                    if (v === '수정') return '<span class="badge bg-warning text-dark">수정</span>';
                    return '';
                }},
				{ title: '공정코드', field: 'linecd', hozAlign: 'center', width: 120 },
				{ title: '공정명', field: 'linenm', widthGrow: 2, hozAlign: 'left', cssClass: 'fw-bold text-primary' },
				{
					title: '금액', field: 'dircamt_sum',
					hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 },
                    mutatorData: (value, data) => (Number(data.DIRcamt || data.dircamt || 0) + Number(data.IDIRcamt || data.idircamt || 0))
				},
				{
					title: '조정액', field: 'adstamt',
					hozAlign: 'right', editor: 'number', formatter: 'money', formatterParams: { precision: 0 }, cssClass: 'bg-light-yellow',
                    cellEdited: (cell) => {
                      cell.getRow().update({ _status: '수정' });
                      cell.getRow().select();
                    }
				},
				{
					title: '합계', field: 'tot_amt',
					hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 },
                    cssClass: 'fw-bold text-primary bg-light',
                    mutatorData: (value, data) => (Number(data.DIRcamt || data.dircamt || 0) + Number(data.IDIRcamt || data.idircamt || 0) + Number(data.adstamt || 0))
				}
			]
		})
    mainGrid.on('rowSelectionChanged', (data) => selectedRowCount.value = data.length)
	}
}

onMounted(async () => {
	await loadInitData()
	nextTick(() => {
    initGrid()
    handleSearch()
  })
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
