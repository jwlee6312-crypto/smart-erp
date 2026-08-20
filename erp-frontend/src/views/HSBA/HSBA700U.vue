<!--기본정보/재무관리/재고자산계정등록 [ERP 프리미엄 표준] -->
<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-1 px-3 sticky-top shadow-sm flex-shrink-0">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 13px;">
        <i class="bi bi-calculator-fill me-2 text-primary"></i>
        기본정보 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        재고자산계정 등록 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
        <span class="text-primary fw-bolder">재고자산 계정관리 (HSBA700U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1">
        <button class="btn-erp btn-init" @click="fetchList">초기화(새로고침)</button>
        <button class="btn-erp btn-save" @click="handleBulkSave">일괄 저장</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2">
      <!-- 🅰️ 안내 문구 -->
      <div class="alert alert-primary py-1 px-3 mb-0 d-flex align-items-center shadow-sm" style="font-size: 12px; border-left: 4px solid #005a9f;">
        <i class="bi bi-info-circle-fill me-2 text-primary"></i>
        재고자산별 매입/매출 계정코드를 설정하십시오. 돋보기 아이콘을 클릭하여 계정을 검색할 수 있습니다.
      </div>

      <!-- 🅱️ 데이터 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column">
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="gridElement" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>
    </div>
    <!-- 💡 계정 도움창 모달 -->
    <Modal v-model:visible="modalVisible" :modalProps="modalProps" />
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, nextTick } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import type { ModalProps } from '@/types/modal'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()

// 1. 상태 관리
const gridElement = ref<HTMLElement | null>(null)
const grid = ref<Tabulator | null>(null)
const activeItemCount = ref(0)

// 2. 그리드 컬럼 정의
const accountSearchIcon = () => "<i class='bi bi-search text-primary' style='cursor:pointer;'></i>"

const initGrid = () => {
  if (!gridElement.value) return
  grid.value = new Tabulator(gridElement.value, {
    layout: "fitColumns",
    height: "100%",
    placeholder: "데이터를 조회 중입니다...",
    columnHeaderVertAlign: "middle",
    columnDefaults: { headerSort: false },
    columns: [
      { title: "재고자산 유형", field: "cdnm", width: 150, hozAlign: "center", cssClass: "fw-bold bg-light", frozen: true },
      { title: "코드", field: "code", visible: false },
      {
        title: "매입 (Purchase)",
        headerHozAlign: "center",
        columns: [
          { title: "차변계정", field: "imacctnm", widthGrow: 1,
            formatter: (cell) => cell.getData().imacctcd ? `[${cell.getData().imacctcd || ''}] ${cell.getValue() || ''}` : ""
          },
          { title: " ", field: "searchim", width: 35, hozAlign: "center", formatter: accountSearchIcon,
            cellClick: (e, cell) => openAccountHelp('im', cell)
          },
          { title: "대변계정", field: "ipacctnm", widthGrow: 1,
            formatter: (cell) => cell.getData().ipacctcd ? `[${cell.getData().ipacctcd || ''}] ${cell.getValue() || ''}` : ""
          },
          { title: " ", field: "searchip", width: 35, hozAlign: "center", formatter: accountSearchIcon,
            cellClick: (e, cell) => openAccountHelp('ip', cell)
          },
          { title: "선급부가세", field: "ivacctnm", widthGrow: 1,
            formatter: (cell) => cell.getData().ivacctcd ? `[${cell.getData().ivacctcd || ''}] ${cell.getValue() || ''}` : ""
          },
          { title: " ", field: "searchiv", width: 35, hozAlign: "center", formatter: accountSearchIcon,
            cellClick: (e, cell) => openAccountHelp('iv', cell)
          },
        ]
      },

      // 매출(Sales) 그룹
      {
        title: "매출 (Sales)",
        headerHozAlign: "center",
        columns: [
          { title: "차변계정", field: "omacctnm", widthGrow: 1,
            formatter: (cell) => cell.getData().omacctcd ? `[${cell.getData().omacctcd || ''}] ${cell.getValue() || ''}` : ""
          },
          { title: " ", field: "searchom", width: 35, hozAlign: "center", formatter: accountSearchIcon,
            cellClick: (e, cell) => openAccountHelp('om', cell)
          },
          { title: "대변계정", field: "opacctnm", widthGrow: 1,
            formatter: (cell) => cell.getData().opacctcd ? `[${cell.getData().opacctcd || ''}] ${cell.getValue() || ''}` : ""
          },
          { title: " ", field: "searchop", width: 35, hozAlign: "center", formatter: accountSearchIcon,
            cellClick: (e, cell) => openAccountHelp('op', cell)
          },
          { title: "예수부가세", field: "ovacctnm", widthGrow: 1,
            formatter: (cell) => cell.getData().ovacctcd ? `[${cell.getData().ovacctcd || ''}] ${cell.getValue() || ''}` : ""
          },
          { title: " ", field: "searchov", width: 35, hozAlign: "center", formatter: accountSearchIcon,
            cellClick: (e, cell) => openAccountHelp('ov', cell)
          },
        ]
      }
    ]
  })
}

// 3. 데이터 로드 (S0)
async function fetchList() {
  try {
    const res = await api.post('/hsba/HSBA_700U_STR', { actkind: 'S0', cmpycd: authStore.cmpycd })
    if (grid.value) {
      grid.value.setData(res.data || [])
      activeItemCount.value = (res.data || []).length
    }
  } catch (e) { vAlertError('계정 설정 정보 조회 실패') }
}

// 4. 저장 (일괄 처리)
async function handleBulkSave() {
  if (!confirm('그리드에 표시된 모든 변경 사항을 저장하시겠습니까?')) return

  const data = grid.value?.getData() || []
  let successCount = 0

  try {
    for (const row of data) {
      // ASP 로직: 하나라도 계정이 있으면 저장(A0), 모두 없으면 삭제(D0)
      const hasAccount = row.imacctcd || row.ipacctcd || row.ivacctcd || row.omacctcd || row.opacctcd || row.ovacctcd
      const act = hasAccount ? 'A0' : 'D0'

      await api.post('/hsba/HSBA_700U_STR', {
        actkind: act,
        cmpycd: authStore.cmpycd,
        astkind: row.code,
        astkindnm: row.cdnm,
        imacctcd: row.imacctcd || '',
        ipacctcd: row.ipacctcd || '',
        ivacctcd: row.ivacctcd || '',
        omacctcd: row.omacctcd || '',
        opacctcd: row.opacctcd || '',
        ovacctcd: row.ovacctcd || '',
        userid: authStore.user_id
      })
      successCount++
    }
    vAlert(`${successCount}개 유형의 계정 설정이 완료되었습니다.`)
    fetchList()
  } catch (e) { vAlertError('일괄 저장 처리 중 오류 발생') }
}

// 5. 도움창(모달) 연동
const modalVisible = ref(false)
const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })

function openAccountHelp(type: string, cell: any) {
  const row = cell.getRow()

  // ASP의 HELP_acctcd_LTD.asp gbn 파라미터 규격 적용
  let gbn = '112'
  if (type === 'im') gbn = '112';
  else if (type === 'ip') gbn = '210';
  else if (type === 'iv') gbn = '1118';
  else if (type === 'om') gbn = '1114';
  else if (type === 'op') gbn = '51';
  else if (type === 'ov') gbn = '2104';

  // HA00_00P_STR 'A0','haionnet','','112'
  Object.assign(modalProps, {
    title: '계정과목 선택',
    path: '/ha00/HA00_00P_STR',
    data: { gubun: 'A0', gbncd: '', code: gbn, cmpycd: authStore.cmpycd },
    defaultField: 'acctnm',
    columns: [
      { title: '계정코드', field: 'acctcd', width: 100 },
      { title: '계정과목명', field: 'acctnm', width: 200 }
    ],
    onConfirm: (selected: any) => {
      const updateData: any = {}
      updateData[`${type}acctcd`] = selected.acctcd
      updateData[`${type}acctnm`] = selected.acctnm
      row.update(updateData)
    }
  })
  modalVisible.value = true
}

onMounted(() => {
  nextTick(() => {
    initGrid()
    fetchList()
  })
})
</script>

