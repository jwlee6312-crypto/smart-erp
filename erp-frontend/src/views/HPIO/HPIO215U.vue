<!--
	=============================================================
	프로그램명	: 작업지시전개 (HPIO215U)
	작성일자	: 2025.03.14
	설명        : 생산계획을 공정별 작업지시로 전개 및 확정 (표준 디자인 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-list-check me-2 text-primary" style="font-size: 18px;"></i>
        생산관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        작업지시 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">작업지시전개 (HPIO215U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-save" @click="saveData">저장</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 (2단 그리드) -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 조회 조건 -->
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
                <th class="required bg-light text-center">계획일자</th>
                <td>
                  <input v-model="searchForm.yymmdd" type="date" class="form-control form-control-sm" style="width: 150px;" @change="search" />
                </td>
                <th class="required bg-light text-center border-start">생산라인</th>
                <td>
                  <select v-model="searchForm.linecd" class="form-select form-select-sm" style="width: 150px;" @change="search">
                    <option v-for="opt in lineOptions" :key="opt.linecd" :value="opt.linecd">{{ opt.linenm }}</option>
                  </select>
                </td>
                <td colspan="2" class="text-muted small ps-3">
                  <i class="bi bi-info-circle me-1"></i> 상단 <b>주문번호</b>,<b>공정별지시</b> 클릭 하단 세부 공정 지시 생성.
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [중간] 작업지시 대상 (계획) 그리드 -->
      <div class="card border shadow-sm h-50 overflow-hidden d-flex flex-column">
        <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark d-flex align-items-center">
          <i class="bi bi-collection-play me-2 text-primary"></i> 1. 작업지시 대상 (미지시 생산계획)
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="targetGridRef" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>

      <!-- [하단] 작업지시 내역 그리드 -->
      <div class="card border shadow-sm h-50 overflow-hidden d-flex flex-column">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark"><i class="bi bi-check2-square me-2 text-primary"></i> 2. 작업지시 확정 내역</span>
          <button class="btn btn-sm btn-outline-danger py-0 px-2 fw-bold" @click="delRow" style="font-size: 11px;">지시취소</button>
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
import { TabulatorFull as Tabulator, type RowComponent } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import AppAlert from '@/components/AppAlert.vue'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

const today = new Date().toISOString().split('T')[0]
const searchForm = reactive({ yymmdd: today, linecd: '' })
const lineOptions = ref<any[]>([])

const targetGridRef = ref<HTMLElement | null>(null); const mainGridRef = ref<HTMLElement | null>(null)
let targetGrid: Tabulator | null = null; let mainGrid: Tabulator | null = null

const initGrids = () => {
  // 1. 계획 그리드
  targetGrid = new Tabulator(targetGridRef.value!, {
    layout: 'fitColumns', height: '100%', selectable: 1,
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: '순번', field: 'ser', width: 60, hozAlign: 'center' },
      { title: '주문번호', field: 'ordno', width: 150, cellClick: (e, cell) => addOrder(cell.getData()), cssClass: 'text-primary cursor-pointer fw-bold' },
      { title: '거래처', field: 'custnm', widthGrow: 1 },
      { title: '품목코드', field: 'itemcd', width: 100, cssClass: 'fw-bold' },
      { title: '품목명', field: 'itemnm', widthGrow: 1, hozAlign: 'left' },
      { title: '공정', field: 'prognm', width: 150 },
      { title: '계획수량', field: 'planqty', width: 120, hozAlign: 'right', formatter: 'money' },
      { title: '이미지시', field: 'ordqty', width: 120, hozAlign: 'right', formatter: 'money' },
      { title: '전개', width: 150, hozAlign: 'center', formatter: () => '<button class="btn btn-xs btn-primary py-0 px-2">공정별지시</button>',
        cellClick: (e, cell) => { addOrder(cell.getData()); }
      }
    ]
  })

  // 2. 지시 그리드
  mainGrid = new Tabulator(mainGridRef.value!, {
    layout: 'fitColumns', height: '100%', selectable: true,
    columnDefaults: {
        headerHozAlign: 'center', headerSort: false, vertAlign: "middle",
        cellEdited: (cell) => {
            const row = cell.getRow(); const d = row.getData();
            if (d._state === 'EXIST' && d._status !== '삭제') row.update({ _status: '수정' });
        }
    },
    columns: [
      { title: "선택", width: 40, hozAlign: "center", formatter: "rowSelection", titleFormatter: "rowSelection" },
      { title: "상태", field: "_status", width: 60, hozAlign: "center", formatter: (c) => {
          const v = c.getValue();
          if (v === '입력') return '<span class="badge bg-primary">입력</span>';
          if (v === '수정') return '<span class="badge bg-warning text-dark">수정</span>';
          if (v === '삭제') return '<span class="badge bg-danger">삭제</span>';
          return '';
      }},
      { title: '지시일자', field: 'ordymd', width: 150, hozAlign: 'center' },
      { title: 'LOT번호', field: 'lotno', width: 200, hozAlign: 'center', cssClass: 'fw-bold text-primary' },
      { title: '품목명', field: 'itemnm', widthGrow: 1, hozAlign: 'left' },
      { title: '규격', field: 'itsize', width: 200, hozAlign: 'center' },
      { title: '단위', field: 'unit', width: 100, hozAlign: 'center' },
      { title: '공정명', field: 'prognm', width: 200 },
      { title: '지시수량', field: 'ordqty', width: 150, hozAlign: 'right', editor: 'number', cssClass: 'bg-light-yellow fw-bold' },
      { title: '생산여부', field: 'updyn', width: 150, hozAlign: 'center', formatter: (c) => c.getValue() === 'N' ? '완료' : '-' }
    ]
  })
}

const fetchLineOptions = async () => {
    try {
        const res = await api.get('/hp00/HP00_000S_STR', { params: { gubun: 'L0', cmpycd: authStore.cmpycd, gbncd: 'Y' } })
        lineOptions.value = res.data.map((i: any) => ({ linecd: i.linecd, linenm: i.linenm }))
        if (lineOptions.value.length > 0 && !searchForm.linecd) {
            searchForm.linecd = lineOptions.value[0].linecd;
        }
    } catch (e) {}
}

const search = async () => {
  const ymd = searchForm.yymmdd.replace(/-/g, '');
  try {
    const params = {
        cmpycd: authStore.cmpycd,
        linecd: searchForm.linecd,
        yymmdd: ymd
    };

    const resT = await api.get('/product/pdorder/targetlist', { params })
    targetGrid?.setData(resT.data || []);

    const resM = await api.get('/product/pdorder/list', { params })
    mainGrid?.setData((resM.data || []).map((i: any) => ({ ...i, _state: 'EXIST', _status: '' })));
    vAlert('조회되었습니다.');
  } catch (e) { vAlertError('조회 실패') }
}

const addOrder = (data: any) => {
    const exists = mainGrid?.getData().some((d: any) => d.yymmdd === data.yymmdd && d.ser === data.ser && d.progcd === data.progcd);
    if (exists) return vAlertError('이미 지시 목록에 포함되었습니다.');

    mainGrid?.addRow({
        ...data, _status: '입력', _state: 'NEW',
        ordymd: today.replace(/-/g, ''),
        ordqty: data.planqty - (data.ordqty || 0),
        lotno: '자동채번'
    }, true);
}

const delRow = () => {
    mainGrid?.getSelectedRows().forEach(row => {
        const d = row.getData();
        if (d.updyn === 'N') return vAlertError('생산실적이 있는 지시는 취소할 수 없습니다.');
        if (d._state === 'NEW') row.delete();
        else row.update({ _status: d._status === '삭제' ? '' : '삭제' });
    });
}

const saveData = async () => {
    const changes = mainGrid?.getData().filter((r: any) => r._status) || [];
    if (changes.length === 0) return vAlertError('변경사항이 없습니다.');
    if (!confirm('작업지시를 저장/수정/취소 하시겠습니까?')) return

    try {
        const res = await api.post('/product/pdorder/save', changes);
        const out = res.data[0];
        if (out.result === 'OK') {
            vAlert(out.msg || '저장되었습니다.');
            search();
        }
        else { vAlertError(out.msg || '저장 실패'); }
    } catch (e) { vAlertError('처리 중 오류 발생') }
}

const initialize = () => { resetForm(searchForm); searchForm.yymmdd = today; targetGrid?.clearData(); mainGrid?.clearData(); }

const formatDate = (v: any) => v && v.length === 8 ? `${v.substring(0, 4)}-${v.substring(4, 6)}-${v.substring(6, 8)}` : v;

onMounted(async () => {
    await fetchLineOptions();
    await nextTick();
    initGrids();
    setTimeout(() => { search(); }, 200); // 🚀 그리드 완전 렌더링 후 조회 실행
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
.bg-light-yellow { background-color: #fffdf0 !important; }
.btn-xs { padding: 1px 6px; font-size: 0.75rem; }
</style>
