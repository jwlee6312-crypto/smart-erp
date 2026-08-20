<!--
	=============================================================
	프로그램명	: 수입전표취소(대체) [디자인 표준 통합]
	작성일자	: 2025.02.24
	설명        : HSOD100U 디자인 패턴 이식 및 ASP 패턴 기반 순차 취소 로직 적용
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom bg-white py-2 px-3 sticky-top shadow-sm">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-file-earmark-minus-fill me-2 text-danger" style="font-size: 18px;"></i>
        수입관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">수입전표취소(대체) (HSIP155U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-2">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchList">조회</button>
        <button class="btn-erp btn-save" @click="save">전표 취소</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- 🔍 [상단] 조회 필터 영역 (HSOD100U 디자인 패턴) -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
              <col style="width: 10%" />
              <col style="width: 40%" />
              <col style="width: 10%" />
              <col style="width: 40%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light">발생부서</th>
                <td>
                  <div class="input-group input-group-sm w-75">
                    <input v-model="searchForm.deptcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
                    <input v-model="searchForm.deptnm" type="text" class="form-control" placeholder="부서 선택" />
                    <button class="btn btn-outline-secondary px-2" @click="openHelp('DEPT')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="text-center bg-light">발생일자</th>
                <td class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                  <DateForm v-model:fromdt="searchForm.ioymdfr" v-model:todt="searchForm.ioymdto" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 📊 3. 취소 대상 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white grid-container-right">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark d-flex align-items-center">
            <i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i> 취소 대상 수입 전표(대체) 목록
          </span>
          <span class="text-muted" style="font-size: 11px;">※ 행 선택 후 '전표 취소' 버튼을 누르세요. (더존 전송 건은 취소 불가)</span>
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
import { ref, reactive, onMounted } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { useCommonHelp } from '@/composables/useCommonHelp'
import { getDate } from '@/composables/useDate'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import DateForm from '@/components/DateForm.vue'

const authStore = useAuthStore()
const { firstDay, today } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps, openHelp: openCommonHelp } = useCommonHelp()

const searchForm = reactive({
  deptcd: authStore.deptcd, deptnm: authStore.deptnm,
  ioymdfr: firstDay,
  ioymdto: today
})

const mainGridRef = ref<HTMLDivElement | null>(null); let mainGrid: Tabulator | null = null

const fetchList = async () => {
  try {
    const params = {
      actkind: 'S0',
      cmpycd: authStore.cmpycd,
      deptcd: searchForm.deptcd,
      fromdt: searchForm.ioymdfr.replace(/-/g, ''),
      todt: searchForm.ioymdto.replace(/-/g, '')
    }
    const res = await api.post('/hsip/HSIP_155U_STR', params)
    mainGrid?.setData(res.data || [])
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

/**
 * 🚀 전표 취소 로직 (ASP 루프 패턴 완벽 이식)
 */
const save = async () => {
  const selected = mainGrid?.getSelectedData()
  if (!selected || selected.length === 0) return vAlertError('취소할 항목을 선택하세요.')

  // 💡 ASP 로직: 전송 완료된 건이 하나라도 있으면 취소 불가
  for (const item of selected) {
    if ((item.sendyn || item.SENDYN) === 'Y') {
      return vAlertError(`전표번호 ${item.slipno}는 이미 전송 완료된 건이므로 취소할 수 없습니다.`)
    }
  }

  if (!confirm('선택한 항목들의 전표를 취소하시겠습니까?')) return

  try {
    // 🔄 선택된 각 항목에 대해 순차적으로 프로시저 호출 (ASP 패턴)
    for (const item of selected) {
      const slipYmd = (item.slipymd || '').replace(/-/g, '')

      // 💡 ASP 로직: 전표일자가 있는 경우만 실행
      if (slipYmd > '00000000') {
        const params = {
          actkind: 'D0',
          cmpycd: authStore.cmpycd,
          fromdt: searchForm.ioymdfr.replace(/-/g, ''),
          todt: searchForm.ioymdto.replace(/-/g, ''),
          deptcd: item.deptcd || searchForm.deptcd,
          slipymd: slipYmd,
          slipno: item.slipno,
          updemp: authStore.userid
        }

        const res = await api.post('/hsip/HSIP_155U_STR', params)
        const resData = res.data?.[0]

        // ASP 에러 체크: rs(0) = "Y" 이면 오류
        if (resData && (resData.status === 'Y' || resData.erryn === 'Y' || resData.STATUS === 'Y' || resData.ERRYN === 'Y')) {
          throw new Error(resData.msg || resData.MSG || '취소 처리 중 업무 오류가 발생했습니다.')
        }
      }
    }

    vAlert('정상적으로 작업이 되었습니다.')
    fetchList()
  } catch (e: any) {
    console.error('Cancellation error:', e)
    vAlertError(e.message || '취소 실패')
  }
}

const initialize = () => {
  resetForm(searchForm);
  mainGrid?.clearData();
}

function openHelp(type: string) {
  if (type === 'DEPT') {
    openCommonHelp('DEPT', (d) => { searchForm.deptcd = d.deptcd; searchForm.deptnm = d.deptnm })
  }
}

onMounted(() => {
  if (mainGridRef.value) {
    mainGrid = new Tabulator(mainGridRef.value, {
      layout: 'fitColumns', height: '100%', selectable: true,
      columnDefaults: { headerSort: false, headerHozAlign: "center", hozAlign: "center", vertAlign: "middle", minWidth: 100 },
      columns: [
        { title: "선택", formatter: "rowSelection", titleFormatter: "rowSelection", width: 50, hozAlign: "center" },
        { title: "전표번호", field: "slipno", width: 140, cssClass: "fw-bold text-primary", formatter: (c) => {
            const d = c.getData(); return d.slipymd && d.slipno ? `${d.slipymd}-${d.slipno}` : '';
        }},
        { title: "전송여부", field: "sendyn", width: 100, formatter: (c) => c.getValue() === 'Y' ? '전송완료' : '미전송' },
        { title: "PO No.", field: "fileno", width: 150 },
        { title: "비용종류", field: "costnm", width: 150, hozAlign: "left" },
        { title: "발생일", field: "slipymd", width: 120, formatter: (c) => {
            const v = c.getValue(); return v && v.length === 8 ? `${v.substring(0,4)}.${v.substring(4,6)}.${v.substring(6,8)}` : v;
        }},
        { title: "적요", field: "business", minWidth: 200, widthGrow: 1, hozAlign: "left" },
        { title: "차변금액", field: "dbamt", hozAlign: "right", width: 120, formatter: "money", formatterParams: { precision: 0 } },
        { title: "대변금액", field: "cramt", hozAlign: "right", width: 120, formatter: "money", formatterParams: { precision: 0 } },
        { title: "발행처", field: "custnm", width: 250, hozAlign: "left" }
      ]
    })
  }

  fetchList()
})
</script>

<style scoped>
.main-content-wrapper { padding-bottom: 0vh !important; }
.grid-container-right { border-bottom: 3px solid #005a9f !important; }
.erp-table-dense th, .erp-table-dense td {
  height: 32px !important; padding: 0 8px !important; font-size: 12px; vertical-align: middle; border: 1px solid #dee2e6;
}
.erp-table-dense .form-control, .erp-table-dense .form-select, .erp-table-dense .btn {
  height: 26px !important; font-size: 12px !important; border-radius: 2px;
}
.erp-table-dense th { font-weight: 600; color: #495057; }
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
