<!--
	=============================================================
	프로그램명	: 제조원가계산작업 (HFMF211U)
	작성일자	: 2025.02.24
	설명        : 제조원가계산작업 (HSOD100U 표준 그리드 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-calculator-fill me-2 text-primary" style="font-size: 18px;"></i>
        원가관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        원가결산 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">제조원가계산작업 (HFMF211U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="handleExecute">계산실행</button>
        <button class="btn-erp btn-danger" @click="handleCancel">계산취소</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 조회 필터 및 조건 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
              <col style="width: 15%" /><col style="width: 35%" />
              <col style="width: 15%" /><col style="width: 35%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light">최종원가작업년월</th>
                <td>
                  <input :value="clsInfo.close_yymm" class="form-control form-control-sm bg-light text-center fw-bold w-50" readonly />
                </td>
                <th class="text-center bg-light required">제조원가작업년월</th>
                <td>
                  <div class="d-flex align-items-center gap-2">
                    <input v-model="searchForm.ym" type="month" class="form-control form-control-sm w-50" @change="handleSearch" />
                    <span class="text-danger small fw-bold"><i class="bi bi-exclamation-circle me-1"></i>배부작업 후 진행</span>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [안내] 작업 안내 바 -->
      <div class="alert alert-info py-2 px-3 mb-0 small d-flex align-items-center border-0 shadow-sm flex-shrink-0">
          <i class="bi bi-info-circle-fill me-2 fs-6"></i>
          <div>
            <span class="fw-bold me-2 text-primary">작업순서:</span>
            재료비 생성 → 노무비/제조경비 배부 → 외주가공 → 재공품 원가 생성 → 제품별 원가 생성 (순차 자동 처리)
          </div>
      </div>

      <!-- [하단] 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column grid-container-right">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>공정별 배부 전/후 원가 결과</span>
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
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import AppAlert from '@/components/AppAlert.vue'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

const clsInfo = reactive({ close_yymm: '', wclsym: '' })
const searchForm = reactive({ ym: new Date().toISOString().substring(0, 7) })

const mainGridRef = ref<HTMLElement | null>(null)
let mainGrid: Tabulator | null = null

const loadInitData = async () => {
	try {
		const [res, cls] = await Promise.all([
			api.post('/hfmf/FMF2110U_STR', {
                cmpycd: authStore.cmpycd, actkind: 'S0', ym: '', jobord: '', userid: authStore.userid, MsgYn: '', MsgText: ''
            }),
			api.get('/hp00/HP00_000S_STR', { params: { gubun: 'CL', cmpycd: authStore.cmpycd } })
		])

		if (res.data && res.data[0]) {
            const d = res.data[0];
            const v = Object.values(d);
			clsInfo.close_yymm = String(d.close_yymm || v[0] || '');
		}
        if (cls.data?.length > 0) {
            clsInfo.wclsym = cls.data[0].wclsym || cls.data[0].codecd || '';
        }
	} catch (e) { console.error(e) }
}

const handleSearch = async () => {
	try {
		const ym = searchForm.ym.replace('-', '')
		const { data } = await api.post('/hfmf/FMF2110U_STR', {
            cmpycd: authStore.cmpycd, actkind: 'S2', ym: ym, jobord: '', userid: authStore.userid, MsgYn: '', MsgText: ''
        })
		mainGrid?.setData(data)
	} catch (e) { vAlertError('조회 실패') }
}

const handleExecute = async () => {
	const ym = searchForm.ym.replace('-', '')
	if (clsInfo.wclsym && clsInfo.wclsym >= ym) return vAlertError('원가마감 되었습니다. 마감 취소 후 작업하세요.')

	if (!confirm('제조원가계산 작업을 시작하시겠습니까?')) return

	try {
        const steps = [
            { val: '1', msg: '재료비 생성' }, { val: '2', msg: '노무비/제조경비 배부' },
            { val: '3', msg: '외주가공비 집계' }, { val: '4', msg: '재공품 원가 생성' }, { val: '5', msg: '제품별 원가 생성' }
        ];

        for (const step of steps) {
            const res = await api.post('/hfmf/FMF2110U_STR', {
                cmpycd: String(authStore.cmpycd), actkind: 'A0', ym: String(ym), jobord: String(step.val),
                userid: String(authStore.userid), MsgYn: '', MsgText: ''
            });
            const v = Object.values(res.data[0]);
            if (v[0]?.toString().toUpperCase() !== 'Y') throw new Error(`${step.msg} 실패: ${v[1]}`);
        }
		vAlert('제조원가계산 작업이 완료되었습니다.'); handleSearch();
	} catch (e: any) { vAlertError(e.message || '작업 중 오류 발생'); }
}

const handleCancel = async () => {
	const ym = searchForm.ym.replace('-', '')
	if (clsInfo.wclsym && clsInfo.wclsym >= ym) return vAlertError('원가마감 되었습니다.')
	if (!confirm('제조원가계산 내용을 취소하시겠습니까?')) return

	try {
		await api.post('/hfmf/FMF2110U_STR', {
            cmpycd: authStore.cmpycd, actkind: 'D0', ym: ym, jobord: '', userid: authStore.userid, MsgYn: '', MsgText: ''
        })
		vAlert('취소되었습니다.'); mainGrid?.clearData();
	} catch (e) { vAlertError('취소 중 오류 발생') }
}

const initialize = () => { resetForm(searchForm); loadInitData(); mainGrid?.clearData(); }

onMounted(() => {
	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns', height: '100%',
			columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
			columns: [
				{ title: '공정', field: 'linenm', hozAlign: 'center', width: 150, frozen: true, cssClass: "fw-bold text-primary" },
                {
                    title: '배부 전',
                    columns: [
                        { title: '재료비', field: 'matlcost', hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 } },
                        { title: '노무비', field: 'mancost', hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 } },
                        { title: '제조경비', field: 'expcost', hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 } },
                        { title: '외주비', field: 'outcost', hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 } },
                        { title: '합계', field: 'tot', hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 }, cssClass: 'bg-light' }
                    ]
                },
                {
                    title: '배부 후',
                    columns: [
                        { title: '재료비', field: 'bmatlcost', hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 } },
                        { title: '노무비', field: 'bmancost', hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 } },
                        { title: '제조경비', field: 'bexpcost', hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 } },
                        { title: '외주비', field: 'boutcost', hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 } },
                        { title: '합계', field: 'btot', hozAlign: 'right', formatter: 'money', formatterParams: { precision: 0 }, cssClass: 'bg-info-subtle fw-bold' }
                    ]
                }
			]
		})
	}
    loadInitData().then(() => handleSearch())
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
