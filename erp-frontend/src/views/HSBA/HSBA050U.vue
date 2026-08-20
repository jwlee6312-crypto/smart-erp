<!--
	=============================================================
	프로그램명	: 창고정보관리 (HSBA050U)
	작성일자	: 2025.02.24
	설명        : 창고 코드 및 명칭 관리 (HSOD100U 디자인 표준 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-house-door-fill me-2 text-primary" style="font-size: 18px;"></i>
        기본정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        기초관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">창고정보관리 (HSBA050U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-save" @click="save">저장</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 상세 입력 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
          <span class="fw-bold small text-dark"><i class="bi bi-pencil-square me-2 text-secondary"></i>창고 상세 정보</span>
          <div v-if="formData.actkind === 'U0'" class="badge bg-warning text-dark px-2">수정 중</div>
          <div v-else class="badge bg-primary text-white px-2">신규 등록</div>
        </div>
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
              <col style="width: 100px;" /><col style="width: 150px;" />
              <col style="width: 100px;" /><col />
              <col style="width: 100px;" /><col style="width: 100px;" />
              <col style="width: 80px;" /><col style="width: 80px;" />
            </colgroup>
            <tbody>
              <tr>
                <th class="required bg-light text-center">창고코드</th>
                <td>
                  <input v-model="formData.whcd" type="text" class="form-control form-control-sm fw-bold text-primary text-center" maxlength="3" placeholder="000" :disabled="formData.actkind === 'U0'"/>
                </td>
                <th class="required bg-light text-center">창고명칭</th>
                <td>
                  <input v-model="formData.whnm" type="text" class="form-control form-control-sm" maxlength="30" />
                </td>
                <th class="bg-light text-center">정렬순서</th>
                <td>
                  <input v-model="formData.dspord" type="number" class="form-control form-control-sm text-end" />
                </td>
                <th class="bg-light text-center">사용여부</th>
                <td class="text-center">
                  <div class="form-check form-switch d-inline-block">
                    <input v-model="formData.useyn" class="form-check-input" type="checkbox" true-value="Y" false-value="N">
                  </div>
                </td>
              </tr>
              <tr>
                <th class="bg-light text-center">비고</th>
                <td colspan="7">
                  <input v-model="formData.remark" type="text" class="form-control form-control-sm" maxlength="100" placeholder="창고 관련 특이사항 입력" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 데이터 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>창고 마스터 리스트 ({{ activeItemCount }} 건)</span>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="gridElement" class="tabulator-instance flex-grow-1"></div>
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

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

const formData = reactive<any>({
	actkind: 'S0', whcd: '', whnm: '', dspord: '1', useyn: 'Y', remark: '',
	cmpycd: authStore.cmpycd, userid: authStore.userid
})

const gridElement = ref<HTMLDivElement | null>(null)
let grid: Tabulator | null = null
const activeItemCount = ref(0)

async function search() {
	try {
		const res = await api.post('/hsba/HSBA_050U_STR', {
    		actkind: 'S0',
    		cmpycd: authStore.cmpycd
		})

		grid?.setData(res.data)
		activeItemCount.value = (res.data || []).length
		vAlert('조회되었습니다.')
	} catch (e) { vAlertError('데이터 조회 실패') }
}

async function save() {
	if (!formData.whcd || !formData.whnm) return vAlertError('코드와 명칭은 필수 입력 항목입니다.')
	if (!confirm('저장하시겠습니까?')) return

	try {
		const act = formData.actkind === 'S0' ? 'A0' : 'U0';
		const res = await api.post('/hsba/HSBA_050U_STR', { ...formData, actkind: act })

        const resData = res.data[0] || {};
		if (resData.result === 'N' || resData.res === 'N') {
            vAlertError(resData.msg || '저장 실패')
        } else {
            vAlert('저장되었습니다.');
            search();
            initialize();
        }
	} catch (e) { vAlertError('저장 처리 중 오류 발생') }
}

function initialize() {
	resetForm(formData);
    Object.assign(formData, { actkind: 'S0', useyn: 'Y', dspord: '1', cmpycd: authStore.cmpycd, userid: authStore.userid });
}

onMounted(() => {
	if (gridElement.value) {
		grid = new Tabulator(gridElement.value, {
			layout: 'fitColumns', height: '100%', selectable: 1,
			placeholder: '데이터가 없습니다.',
			columnDefaults: { headerSort: false, headerHozAlign: 'center', vertAlign: "middle" },
			columns: [
				{ title: '코드', field: 'whcd', hozAlign: 'center', width: 150, cssClass: 'fw-bold text-primary border-end' },
				{ title: '창고 명칭', field: 'whnm', minWidth: 250, widthGrow: 1, cssClass: 'fw-bold', hozAlign: 'left' },
				{ title: '순서', field: 'dspord', hozAlign: 'center', width: 150 },
				{ title: '사용', field: 'useyn', hozAlign: 'center', width: 150, formatter: (c) => c.getValue() === 'Y' ? '<span class="text-success fw-bold">O</span>' : '<span class="text-danger fw-bold">X</span>' },
				{ title: '비고', field: 'remark', minWidth: 250, widthGrow: 1, hozAlign: 'left' }
			]
		})
		grid.on('rowClick', (e, row) => {
			const d = row.getData();
			Object.assign(formData, d);
			formData.actkind = 'U0'
		})
	}
	nextTick(search);
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
