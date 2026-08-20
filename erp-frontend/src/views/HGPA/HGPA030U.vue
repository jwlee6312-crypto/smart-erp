<!--
	=============================================================
	프로그램명  : 다이얼플랜 관리 (Dialplan Management)
    프로그램 ID	: HGPA030U
	작성일자	    : 2025.03.14
	작성자      : AI Assistant
    설명        : 교환기 다이얼플랜(Extensions) 설정 및 관리 (표준 UI 적용)
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
	
    <div class="erp-container d-flex flex-column h-100 bg-white">
        <!-- 🚀 1. 상단 액션 바 -->
        <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom bg-white py-2 px-3 sticky-top shadow-sm">
            <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
                <i class="bi bi- Mortarboard-fill me-2 text-primary" style="font-size: 18px;"></i>
                시스템관리 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
                통신관리 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
                <span class="text-primary fw-bolder">다이얼플랜 관리 (HGPA030U)</span>
            </div>
            <div class="btn-group-erp d-flex gap-1 pe-3">
                <button class="btn-erp btn-init" @click="initialize">초기화</button>
                <button class="btn-erp btn-search" @click="search">조회</button>
                <button class="btn-erp btn-save" @click="save">저장</button>
                <button class="btn-erp btn-delete" @click="deleteSelected">삭제</button>
            </div>
        </div>

        <!-- 💡 2. 메인 컨텐츠 영역 -->
        <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">
            <!-- [상단] 조회 필터 -->
            <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
                <div class="card-body p-2 bg-white">
                    <div class="d-flex align-items-center flex-wrap gap-3 small">
                        <div class="d-flex align-items-center">
                            <span class="erp-label"><i class="bi bi-dot"></i>컨텍스트</span>
                            <input v-model="searchForm.context" class="form-control form-control-sm" style="width: 150px;" placeholder="Context" @keyup.enter="search" />
                        </div>
                        <div class="d-flex align-items-center">
                            <span class="erp-label"><i class="bi bi-dot"></i>번호(Exten)</span>
                            <input v-model="searchForm.exten" class="form-control form-control-sm" style="width: 150px;" placeholder="Extension" @keyup.enter="search" />
                        </div>
                    </div>
                </div>
            </div>

            <!-- [하단] 레이아웃 영역 -->
            <div class="row g-1 flex-grow-1 overflow-hidden">
                <!-- ⬅️ 좌측: 다이얼플랜 리스트 -->
                <div class="col-md-9 h-100 d-flex flex-column">
                    <div class="card border shadow-sm h-100 d-flex flex-column overflow-hidden">
                        <div class="card-header bg-white py-1 px-3 border-bottom d-flex justify-content-between align-items-center">
                            <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-1"></i> Extensions List</span>
                            <button class="btn btn-xs btn-outline-primary fw-bold py-0" @click="addRow">행추가</button>
                        </div>
                        <div class="card-body p-0 flex-grow-1 bg-white position-relative overflow-hidden d-flex flex-column">
                            <div ref="tableRef" class="tabulator-instance flex-grow-1" />
                        </div>
                    </div>
                </div>

                <!-- ➡️ 우측: 가이드/설명 -->
                <div class="col-md-3 h-100 overflow-auto">
                    <div class="card border shadow-sm h-100 overflow-hidden">
                        <div class="card-header bg-white py-1 px-3 border-bottom">
                            <span class="fw-bold small text-dark">Dialplan Guide</span>
                        </div>
                        <div class="card-body p-3 bg-white small text-start">
                            <div class="mb-3">
                                <p class="mb-1 text-primary fw-bold"><i class="bi bi-telephone-outbound-fill me-1"></i>내선 통화 (Dial)</p>
                                <code class="d-block p-2 bg-light border text-dark rounded">app: Dial, data: pjsip/101</code>
                                <p class="text-muted mt-1" style="font-size: 11px;">지정한 내선번호로 전화를 거는 기본 명령입니다.</p>
                            </div>
                            <div class="mb-3">
                                <p class="mb-1 text-primary fw-bold"><i class="bi bi-volume-up-fill me-1"></i>안내 재생 (Playback)</p>
                                <code class="d-block p-2 bg-light border text-dark rounded">app: Playback, data: custom/welcome</code>
                                <p class="text-muted mt-1" style="font-size: 11px;">준비된 음성 파일을 재생합니다.</p>
                            </div>
                            <div class="alert alert-info py-2 px-3 border-0" style="font-size: 11px;">
                                <i class="bi bi-info-circle-fill me-1"></i> Priority는 실행 순서를 의미하며, 동일한 Exten 내에서 낮은 숫자부터 실행됩니다.
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import AppAlert from '@/components/AppAlert.vue'
import { api } from '@/utils/axios'

const { showAlert, showError, vAlert, vAlertError, alertMessage } = useAlerts()

const searchForm = reactive({ context: '', exten: '' })
const tableRef = ref<HTMLDivElement | null>(null)
let tableInstance: Tabulator | null = null


const initTable = () => {
	if (!tableRef.value) return
    if (tableInstance) tableInstance.destroy();
	tableInstance = new Tabulator(tableRef.value, {
		placeholder: '데이터가 없습니다.',
		layout: 'fitColumns',
		selectable: true,
		height: '100%',
        columnDefaults: { headerSort: false, headerHozAlign: 'center', vertAlign: 'middle' },
		columns: [
			{ formatter: "rowSelection", titleFormatter: "rowSelection", hozAlign: "center", width: 40 },
			{ title: '컨텍스트', field: 'context', editor: 'input', width: 150, cssClass: 'fw-bold text-primary' },
			{ title: '번호 (Exten)', field: 'exten', editor: 'input', width: 120 },
			{ title: '순위', field: 'priority', editor: 'number', width: 80, hozAlign: 'center' },
			{ title: '명령어', field: 'app', editor: 'input', width: 150 },
			{ title: '파라미터', field: 'appdata', editor: 'input', hozAlign: 'left', minWidth: 200 },
		],
	})
}

async function search() {
	try {
		const { data } = await api.get('/crm/asterisk/extension/search', { params: searchForm })
        if (Array.isArray(data)) {
            await tableInstance?.setData(data)
            if(data.length > 0) vAlert(`${data.length}건이 조회되었습니다.`)
        }
	} catch (error) { vAlertError('조회 중 오류가 발생했습니다.') }
}

function addRow() {
    tableInstance?.addRow({
        context: 'from-internal',
        exten: '',
        priority: 1,
        app: '',
        appdata: ''
    }, true)
}

async function save() {
    const data = tableInstance?.getData()
    if (!data || data.length === 0) return vAlertError('저장할 데이터가 없습니다.');
    if (!confirm('저장하시겠습니까?')) return

    try {
        const res = await api.post('/crm/asterisk/extension/save', data);
        if (res.data?.[0]?.result === 'N') return vAlertError(res.data[0].msg || '저장 실패');
        vAlert('성공적으로 저장되었습니다.');
        search();
    } catch (e) { vAlertError('저장 중 오류가 발생했습니다.') }
}

async function deleteSelected() {
    const selectedData = tableInstance?.getSelectedData();
    if (!selectedData || selectedData.length === 0) return vAlertError('삭제할 행을 선택하세요.');
    if (confirm('정말로 삭제하시겠습니까?')) {
        try {
            const res = await api.post('/crm/asterisk/extension/delete', selectedData);
            if (res.data?.[0]?.result === 'N') return vAlertError(res.data[0].msg || '삭제 실패');
            vAlert('삭제되었습니다.');
            search();
        } catch (e) { vAlertError('삭제 중 오류가 발생했습니다.') }
    }
}

function initialize() {
    Object.assign(searchForm, { context: '', exten: '' });
    tableInstance?.clearData();
    vAlert('초기화되었습니다.');
}

onMounted(() => {
    nextTick(() => {
        initTable()
        search()
    })
})

onUnmounted(() => { if (tableInstance) tableInstance.destroy(); })
</script>

<style scoped>
.erp-container { font-family: 'Pretendard', sans-serif; letter-spacing: -0.02rem; }
.tabulator-instance { width: 100% !important; background-color: #fff; font-size: 12px; }
</style>
