<!--
	=============================================================
	프로그램명	: 수입비용입력 [디자인 표준 통합]
	작성일자	: 2025.02.24
	설명        : 공통 모듈 배제 및 로컬 도움창 로직 적용 (트랜잭션 보장형 구조)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom bg-white py-2 px-3 sticky-top shadow-sm">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-cash-coin me-2 text-primary" style="font-size: 18px;"></i>
        수입관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">수입비용입력 (HSIP130U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-2">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchList">조회</button>
        <button class="btn-erp btn-save" @click="save">저장</button>
        <button class="btn-erp btn-delete" @click="deleteData" :disabled="!formData.docno">삭제</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- 🔍 [상단] 조회 필터 영역 -->
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
                <th class="text-center bg-light">발생일자</th>
                <td class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                  <DateForm v-model:fromdt="searchForm.fromdt" v-model:todt="searchForm.todt" />
                </td>
                <th class="text-center bg-light">발생부서</th>
                <td>
                  <div class="input-group input-group-sm w-75">
                    <input v-model="searchForm.deptcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
                    <input v-model="searchForm.deptnm" type="text" class="form-control" placeholder="부서 선택" readonly />
                    <button class="btn btn-outline-secondary px-2" @click="openHelp('S_DEPT')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 투-그리드 레이아웃 영역 -->
      <div class="d-flex gap-2 flex-grow-1 overflow-hidden" style="min-height: 0;">

        <!-- ⬅️ 좌측: 비용 마스터 목록 -->
        <div class="card border shadow-sm d-flex flex-column overflow-hidden grid-container-left" style="width: 380px; min-width: 380px;">
          <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark">비용 내역 목록</div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="leftGridRef" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>

        <!-- ➡️ 우측: 마스터 상세 폼 + 상세 품목 그리드 -->
        <div class="flex-grow-1 d-flex flex-column gap-2 overflow-hidden">

          <!-- 상세 정보 마스터 폼 -->
          <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
            <div class="card-body p-0 bg-white">
              <table class="erp-table-dense w-100">
                <colgroup>
                  <col style="width: 100px;" /><col />
                  <col style="width: 100px;" /><col />
                  <col style="width: 100px;" /><col />
                </colgroup>
                <tbody>
                  <tr>
                    <th class="required bg-light text-center">PO No</th>
                    <td>
                      <div class="input-group input-group-sm">
                        <input v-model="formData.fileno" type="text" class="form-control fw-bold text-primary" placeholder="PO 번호" @keyup.enter="fetchDetail()" />
                        <button class="btn btn-outline-secondary px-2" @click="openHelp('PO')"><i class="bi bi-search"></i></button>
                      </div>
                    </td>
                    <th class="required bg-light text-center">비용번호</th>
                    <td><input v-model="formData.docno" class="form-control bg-light fw-bold text-center" readonly placeholder="자동부여" /></td>
                    <th class="required bg-light text-center">발생일자</th>
                    <td><input v-model="formData.pubymd" type="date" class="form-control" /></td>
                  </tr>
                  <tr>
                    <th class="bg-light text-center">발생부서</th>
                    <td>
                      <div class="input-group input-group-sm">
                        <input v-model="formData.deptcd" type="text" class="form-control text-center bg-light" style="max-width: 60px;" readonly />
                        <input v-model="formData.deptnm" type="text" class="form-control" readonly />
                        <button class="btn btn-outline-secondary px-2" @click="openHelp('DEPT')"><i class="bi bi-search"></i></button>
                      </div>
                    </td>
                    <th class="bg-light text-center">특기사항</th>
                    <td colspan="3"><input v-model="formData.remark" class="form-control" placeholder="비용 관련 참고사항" /></td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- 📊 3. 상세 품목 그리드 영역 -->
          <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white grid-container-right">
            <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
              <span class="fw-bold small text-dark d-flex align-items-center">
                <i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i> 수입 제비용 상세 내역
              </span>
              <button class="btn btn-sm btn-outline-primary py-0 px-2 fw-bold" @click="addRow" style="font-size: 12px;">+ 행추가</button>
            </div>
            <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
              <div ref="mainGridRef" class="tabulator-instance flex-grow-1"></div>
            </div>
          </div>
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
import { getDate } from '@/composables/useDate'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import DateForm from '@/components/DateForm.vue'

const authStore = useAuthStore()
const { firstDay, today } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

// [로컬 상태 정의] 공백 제거 헬퍼 포함
const clean = (val: any) => String(val || '').trim()

const searchForm = reactive({
  fromdt: firstDay,
  todt: today,
  deptcd: authStore.deptcd,
  deptnm: authStore.deptnm
})

const formData = reactive<any>({
  cmpycd: authStore.cmpycd, fileno: '',
  pubymd: today, remark: '',
  deptcd: authStore.deptcd, deptnm: authStore.deptnm, docno: ''
})

// [도움창 상태 정의]
const modalVisible = ref(false)
const modalProps = reactive<any>({
  title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table'
})

const leftGridRef = ref<HTMLDivElement | null>(null); let leftGrid: Tabulator | null = null
const mainGridRef = ref<HTMLDivElement | null>(null); let mainGrid: Tabulator | null = null

const fetchList = async () => {
  try {
    const res = await api.post('/hsip/HSIP_130U_STR', {
      actkind: 'L0',
      cmpycd: authStore.cmpycd,
      fromdt: searchForm.fromdt.replace(/-/g, ''),
      todt: searchForm.todt.replace(/-/g, ''),
      deptcd: '',
      deptnm: '', fileno: '', docno: '', pubymd: '', bigo: '',
      updemp: authStore.userid
    })
    leftGrid?.setData(res.data || [])
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

const fetchDetail = async (fileno?: string, docno?: string) => {
  const fno = fileno || formData.fileno
  const dno = docno || formData.docno
  if (!fno) return vAlertError('PO No를 입력하세요.')

  try {
    const res = await api.post('/hsip/HSIP_130U_STR', {
      actkind: 'S0',
      cmpycd: authStore.cmpycd,
      fileno: fno,
      docno: dno || '',
      fromdt: '', todt: '', deptnm: '', deptcd: '', pubymd: '', bigo: '',
      updemp: authStore.userid
    })

    if (res.data && res.data.length > 0) {
      const mst = res.data[0];
      Object.assign(formData, mst)
      formData.remark = mst.bigo || mst.bigo || '';
      if (formData.pubymd && formData.pubymd.length === 8) {
        formData.pubymd = `${formData.pubymd.substring(0, 4)}-${formData.pubymd.substring(4, 6)}-${formData.pubymd.substring(6, 8)}`
      }

      const resItems = await api.post('/hsip/HSIP_131U_STR', {
        actkind: 'S0',
        cmpycd: authStore.cmpycd,
        fileno: formData.fileno,
        docno: formData.docno,
        crowno: '', costcd: '', deptcd: '', shipseq: '', passseq: '', pubymd: '', costamt: 0,
        currcd: '', frgnrate: 0, frgnamt: 0, paycust: '', mgtno: '', bigo: '',
        slipymd: '', slipno: '', srowno: '', updemp: authStore.userid
      })
      mainGrid?.setData(resItems.data || [])
    } else {
      mainGrid?.setData([])
      vAlertError('조회 결과가 없습니다.')
    }
  } catch (e) { vAlertError('조회 실패') }
}

const save = async () => {
  if (!formData.fileno) return vAlertError('PO No를 입력하세요.')
  const items = mainGrid?.getData().filter((r: any) => clean(r.costcd) !== '' && (r.state || !r.rowno)) || [];
  if (!items.length && !formData.docno) return vAlertError('저장할 항목이 없습니다.')

  if (!confirm('저장하시겠습니까?')) return

  try {
    const act = formData.docno ? 'U0' : 'A0'
    const resMst = await api.post('/hsip/HSIP_130U_STR', {
      actkind: act,
      cmpycd: authStore.cmpycd,
      fileno: formData.fileno,
      docno: formData.docno || '',
      deptcd: formData.deptcd,
      pubymd: formData.pubymd.replace(/-/g, ''),
      bigo: formData.remark || '',
      updemp: authStore.userid
    })

    if (!resMst.data?.length) throw new Error('마스터 저장 실패')
    const keyDocno = clean(resMst.data[0].docno) || formData.docno

    for (const item of items) {
      const itemRowno = clean(item.crowno)
      const detailAct = itemRowno === '' ? 'A0' : (item.state === 'D' ? 'D0' : 'U0');

      await api.post('/hsip/HSIP_131U_STR', {
        actkind: detailAct,
        cmpycd: authStore.cmpycd,
        fileno: formData.fileno,
        docno: keyDocno,
        rowno: itemRowno,
        costcd: item.costcd,
        deptcd: formData.deptcd,
        shipseq: item.shipseq || '10',
        passseq: item.passseq || '10',
        pubymd: formData.pubymd.replace(/-/g, ''),
        costamt: String(item.costamt || '0').replace(/,/g, ''),
        currcd: '',
        frgnrate: 0,
        frgnamt: 0,
        paycust: '',
        mgtno: '',
        bigo: item.bigo || '',
        updemp: authStore.userid
      })
    }

    vAlert('정상적으로 저장되었습니다.')
    formData.docno = keyDocno
    fetchList();
    fetchDetail();
  } catch (e: any) { vAlertError(e.message || '저장 중 오류 발생') }
}

const deleteData = async () => {
  if (!formData.docno) return
  if (!confirm('정말 삭제하시겠습니까?')) return
  try {
    await api.post('/hsip/HSIP_130U_STR', { ...formData, actkind: 'D0', cmpycd: authStore.cmpycd, updemp: authStore.userid })
    vAlert('삭제되었습니다.'); initialize(); fetchList();
  } catch (e) { vAlertError('삭제 실패') }
}

/** 🚀 로컬 도움창 핸들러 */
const openHelp = (type: string, cell?: any) => {
  if (type === 'S_DEPT' || type === 'DEPT') {
    Object.assign(modalProps, {
      title: '부서 선택', path: '/ha00/HA00_00P_STR', defaultField: 'deptnm',
      data: { gubun: 'D0', cmpycd: authStore.cmpycd, gbncd: '', code: '', remark: '' },
      columns: [
        { title: '부서코드', field: 'deptcd', width: 100, hozAlign: 'center' },
        { title: '부서명', field: 'deptnm', width: 200 }
      ],
      onConfirm: (d: any) => {
        if (type === 'S_DEPT') { searchForm.deptcd = d.deptcd; searchForm.deptnm = d.deptnm }
        else { formData.deptcd = d.deptcd; formData.deptnm = d.deptnm }
      }
    });
  } else if (type === 'PO') {
    Object.assign(modalProps, {
      title: 'PO 번호 선택', path: '/hs00/HS00_000S_STR', defaultField: 'fileno',
      data: { gubun: 'F0', cmpycd: authStore.cmpycd, gbncd: '1', code: '', codenm: '', etcval: '' },
      columns: [
        { title: 'PO No', field: 'fileno', width: 150, hozAlign: 'center' },
        { title: '거래처', field: 'custnm', width: 200 },
        { title: '발행일', field: 'issymd', width: 100, hozAlign: 'center' }
      ],
      onConfirm: (d: any) => {
        formData.fileno = d.fileno;  
        formData.docno = d.docno;
      }
    });
  } else if (type === 'COST') {
    Object.assign(modalProps, {
      title: '비용코드 선택', path: '/hs00/HS00_000S_STR', defaultField: 'cdnm',
      data: { gubun: 'C0', cmpycd: authStore.cmpycd, gbncd: '', code: '', codenm: '', etcval: '' },
      columns: [
        { title: '코드', field: 'costcd', width: 100, hozAlign: 'center' },
        { title: '비용명칭', field: 'costnm', width: 150 },
        { title: '배부기준', field: 'costgbn', width: 100 },
        { title: '배부기준명', field: 'costgbnm', width: 150 }
      ],
      onConfirm: (d: any) => {
        const updateData: any = {
          costcd: d.costcd,
          costnm: d.costnm,
          costgbn: d.costgbn,
          state: cell.getRow().getData().crowno ? 'U' : 'C'
        }
        // 배부기준(costgbn)에 따른 초기값 설정
        if (d.costgbn === '100') { updateData.shipseq = ''; updateData.passseq = '' }
        else if (d.costgbn === '200') { updateData.passseq = '' }

        cell.getRow().update(updateData)
      }
    });
  }
  modalVisible.value = true
}

const addRow = () => { mainGrid?.addRow({ costcd: '', costnm: '', costamt: 0, shipseq: '10', passseq: '10', bigo: '', state: 'C', costgbn: '300' }, true) }

const initialize = () => {
  resetForm(formData);
  Object.assign(formData, {
    cmpycd: authStore.cmpycd,
    pubymd: today,
    deptcd: authStore.deptcd, deptnm: authStore.deptnm,
    docno: ''
  });
  mainGrid?.clearData();
}

onMounted(() => {
  if (leftGridRef.value) {
    leftGrid = new Tabulator(leftGridRef.value, {
      layout: 'fitColumns', height: '100%',
      columnDefaults: { headerSort: false, headerHozAlign: "center", vertAlign: "middle" },
      columns: [
        { title: 'No', formatter: 'rownum', width: 40, hozAlign: 'center' },
        { title: 'PO No', field: 'fileno', width: 120, hozAlign: 'center', cssClass: 'fw-bold text-primary' },
        { title: '발행일', field: 'pubymd', width: 100, hozAlign: 'center', formatter: (c) => {
          const v = c.getValue(); return (v && v.length === 8) ? `${v.substring(0,4)}-${v.substring(4,6)}-${v.substring(6,8)}` : v;
        }},
        { title: '부서명', field: 'deptnm', minWidth: 100, hozAlign: 'left' }
      ]
    })
    leftGrid.on('rowClick', (e, row) => {
      const d = row.getData()
      fetchDetail(d.fileno, d.docno)
    })
  }

  if (mainGridRef.value) {
    mainGrid = new Tabulator(mainGridRef.value, {
      layout: 'fitColumns', height: '100%',
      columnDefaults: { headerSort: false, headerHozAlign: "center", hozAlign: "center", vertAlign: "middle", minWidth: 100 },
      columns: [
        { title: 'No', formatter: 'rownum', width: 60 },
        {
          title: '비용코드', field: 'costcd', width: 120,
          formatter: (cell) => {
            return `<div class='d-flex align-items-center justify-content-between w-100 px-2'>
                      <span>${cell.getValue() || ''}</span>
                      <i class='bi bi-search text-primary cursor-pointer'></i>
                    </div>`
          },
          cellClick: (e, cell) => { if ((e.target as HTMLElement).classList.contains('bi-search')) openHelp('COST', cell) }
        },

        { title: '비용 명칭', field: 'costnm', minWidth: 200, widthGrow: 1.5, cssClass: 'fw-bold', hozAlign: 'left' },
        { title: '비용(원화)', field: 'costamt', hozAlign: 'right', width: 150, editor: 'number', formatter: "money", formatterParams: { precision: 0 },
          cellEdited: (c) => { if (c.getRow().getData().crowno) c.getRow().update({ state: 'U' }) }
        },
        { field: 'costgbn', visible: false },  // 100: 선적차수,통관차수 등록안함, 200:선적차수만 등록, 300:선적차수,통관차수 등록
        {
          title: '선적차수', field: 'shipseq', width: 100,
          editable: (cell: any) => {
            const d = cell.getRow().getData();
            // 🚀 값이 없거나(신규), 200/300인 경우 편집 허용
            return !d.costgbn || d.costgbn === '200' || d.costgbn === '300';
          },
          editor: "select", editorParams: { values: { "10": "1차", "20": "2차", "30": "3차", "40": "4차", "50": "5차" } },
          formatter: "lookup", formatterParams: { "10": "1차", "20": "2차", "30": "3차", "40": "4차", "50": "5차" },
          cellEdited: (c) => { if (c.getRow().getData().crowno) c.getRow().update({ state: 'U' }) }
        },
        {
          title: '통관차수', field: 'passseq', width: 100,
          editable: (cell: any) => {
            const d = cell.getRow().getData();
            // 🚀 값이 없거나(신규), 300인 경우 편집 허용
            return !d.costgbn || d.costgbn === '300';
          },
          editor: "select", editorParams: { values: { "10": "1차", "20": "2차", "30": "3차", "40": "4차", "50": "5차" } },
          formatter: "lookup", formatterParams: { "10": "1차", "20": "2차", "30": "3차", "40": "4차", "50": "5차" },
          cellEdited: (c) => { if (c.getRow().getData().crowno) c.getRow().update({ state: 'U' }) }
        },
        { title: '상세 적요', field: 'bigo', minWidth: 200, widthGrow: 1.5, editor: 'input', hozAlign: 'left',
          cellEdited: (c) => { if (c.getRow().getData().crowno) c.getRow().update({ state: 'U' }) }
        },
        {
          title: "삭제", width: 60,
          formatter: () => "<i class='bi bi-trash text-danger cursor-pointer'></i>",
          cellClick: (e, c) => {
             const row = c.getRow(); const data = row.getData();
             if (data.rowno) {
                if (confirm('이 항목을 즉시 삭제하시겠습니까?')) {
                   api.post('/hsip/HSIP_131U_STR', { ...data, actkind: 'D1', cmpycd: authStore.cmpycd, updemp: authStore.userid }).then(() => {
                      vAlert('삭제되었습니다.'); row.delete();
                   });
                }
             } else row.delete();
          }
        },
        { title: 'crowno', field: 'crowno', width: 30, visible: false }
      ]
    })
  }
  fetchList();
})
</script>

<style scoped>
.main-content-wrapper { padding-bottom: 0vh !important; }
.grid-container-left, .grid-container-right { border-bottom: 3px solid #005a9f !important; }
.erp-table-dense th, .erp-table-dense td { height: 32px !important; padding: 0 8px !important; font-size: 12px; border: 1px solid #dee2e6; }
.erp-table-dense .form-control, .erp-table-dense .btn { height: 26px !important; font-size: 12px !important; }
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
