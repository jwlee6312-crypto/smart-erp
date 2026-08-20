<!--
	=============================================================
	프로그램명	: 작업장배부적수관리 (HFMF105U)
	작성일자	: 2025.02.24
	설명        : 작업장별 배부율 등록 및 관리 (HSOD100U 표준 디자인 및 인터셉터 표준화 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-gear-wide-connected me-2 text-primary" style="font-size: 18px;"></i>
        원가관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        기준정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">작업장배부적수관리 (HFMF105U)</span>
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
                <th class="text-center bg-light required">공정배부기준</th>
                <td>
                  <select v-model="searchForm.divstd" class="form-select form-select-sm" @change="handleSearch">
                    <option v-for="opt in divideOptions" :key="opt.code" :value="opt.code">{{ opt.cdnm }}</option>
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
          <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>작업장별 배부율 목록</span>
          <div class="text-muted small">
            <i class="bi bi-info-circle me-1"></i>배부율 수정 후 저장 버튼을 클릭하세요.
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
const divideOptions = ref<any[]>([])
const searchForm = reactive({
	ym: new Date().toISOString().substring(0, 7),
	divstd: ''
})

const mainGridRef = ref<HTMLElement | null>(null)
let mainGrid: Tabulator | null = null
const selectedRowCount = ref(0)

const loadInitData = async () => {
	try {
		const [cls, divOpts] = await Promise.all([
			api.get('/hp00/HP00_000S_STR', { params: { gubun: 'CL', cmpycd: authStore.cmpycd } }),
			api.post('/hfba/SELECT_DIVIDE_LIST', { cmpycd: authStore.cmpycd, cdkd: '1030' })
		])
        if (cls.data?.length > 0) {
            clsInfo.wclsym = cls.data[0].wclsym || cls.data[0].codecd || ''
        }
		divideOptions.value = (divOpts.data || []).filter((opt: any) => opt.code !== '3010')
        if (divideOptions.value.length > 0) searchForm.divstd = divideOptions.value[0].code
	} catch (e) { vAlertError('기초 데이터 로드 실패') }
}

const initialize = () => {
  resetForm(searchForm)
  searchForm.ym = new Date().toISOString().substring(0, 7)
  if (divideOptions.value.length > 0) searchForm.divstd = divideOptions.value[0].code
  mainGrid?.clearData()
  selectedRowCount.value = 0
}

const handleSearch = async () => {
    if(!searchForm.divstd) return;
	try {
		const ym = searchForm.ym.replace('-', '')
		const { data } = await api.post('/hfmf/FMF1050U_STR', {
            cmpycd: authStore.cmpycd, actkind: 'S0', ym: ym, costcd: '10000',
			linecd: '', divstd: String(searchForm.divstd), rate: '0', remark: '', userid: authStore.userid
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
			await api.post('/hfmf/FMF1050U_STR', {
				cmpycd: authStore.cmpycd, actkind: 'A0', ym: ym, costcd: '10000',
				linecd: String(row.linecd || ''), divstd: String(searchForm.divstd),
				rate: String(row.rate || '0'), remark: String(row.bigo || ''), userid: authStore.userid
			})
		}
		vAlert('저장되었습니다.'); handleSearch();
	} catch (e) { vAlertError('저장 실패') }
}

const deleteData = async () => {
    const ym = searchForm.ym.replace('-', '')
	if (clsInfo.wclsym && clsInfo.wclsym >= ym) return vAlertError('원가마감 되었습니다.')

	const selectedData = mainGrid?.getSelectedData() || []
	if (!confirm('선택한 자료를 삭제하시겠습니까?')) return

	try {
		for (const row of selectedData) {
			await api.post('/hfmf/FMF1050U_STR', {
				cmpycd: authStore.cmpycd, actkind: 'D0', ym: ym, costcd: '10000',
				linecd: String(row.linecd || ''), divstd: String(searchForm.divstd),
				rate: '0', remark: '', userid: authStore.userid
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
				{ title: '배부적수', field: 'PRoqty', hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 } },
				{ title: '적수계산배부율', field: 'juksu_rate', hozAlign: 'right', formatter: 'money', formatterParams: { precision: 2 } },
				{ title: '배부율 (%)', field: 'rate', hozAlign: 'right', editor: 'number', formatter: 'money', formatterParams: { precision: 2 }, cssClass: 'bg-light-yellow',
                  cellEdited: (cell) => {
                    cell.getRow().update({ _status: '수정' });
                    cell.getRow().select();
                  }
                },
				{ title: '비고', field: 'bigo', widthGrow: 2, hozAlign: 'left', editor: 'input' }
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
