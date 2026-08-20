<!--
	=============================================================
	프로그램명	: 생산실적등록 (HPIO300U)
	작성일자	: 2025.02.24
	설명        : 생산 공정별 실적 등록 및 투입 자재 상세 관리
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-box-seam-fill me-2 text-primary" style="font-size: 18px;"></i>
        생산정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">생산실적등록 (HPIO300U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchProcesses">조회</button>
        <button class="btn-erp btn-save" @click="savePerformance">실적저장</button>
        <button class="btn-erp btn-save" style="background-color: #059669 !important;" @click="saveMaterials" :disabled="!selectedProduct.prodid">자재저장</button>
      </div>
    </div>

    <!-- 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- 조회 필터 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
                <col style="width: 10%" /><col style="width: 23%" />
                <col style="width: 10%" /><col style="width: 23%" />
                <col style="width: 10%" /><col style="width: 24%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light required">생산라인</th>
                <td>
                  <select v-model="searchForm.linecd" class="form-select form-select-sm" @change="onLineChange" style="font-size: 12px;">
                    <option value="">라인 선택</option>
                    <option v-for="opt in lineOptions" :key="opt.linecd" :value="opt.linecd">
                      [{{ opt.linecd }}] {{ opt.linenm }}
                    </option>
                  </select>
                </td>
                <th class="text-center bg-light required">지시일자</th>
                <td>
                  <input v-model="ordymd_f" type="date" class="form-control form-control-sm" @change="onFilterChange" style="font-size: 12px;" />
                </td>
                <th class="bg-light"></th><td></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 3단 레이아웃 영역 -->
      <div class="d-flex gap-2 flex-grow-1 overflow-hidden" style="min-height: 0;">

        <!-- 좌측: 공정 목록 (grid1) -->
        <div class="card border shadow-sm d-flex flex-column overflow-hidden grid-container-left" style="width: 250px; min-width: 250px;">
          <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark text-center">생산공정</div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="tableRef1" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>

        <!-- 우측: 실적 및 자재 -->
        <div class="flex-grow-1 d-flex flex-column gap-2 overflow-hidden">

          <!-- 우측 상단: 제품 생산 실적 (grid2) -->
          <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column grid-container-right">
            <div class="bg-white border-bottom">
                <table class="erp-table-dense" width="100%">
                    <colgroup>
                        <col style="width: 10%" /><col style="width: 23.33%" />
                        <col style="width: 10%" /><col style="width: 23.33%" />
                        <col style="width: 10%" /><col style="width: 23.34%" />
                    </colgroup>
                    <tbody>
                        <tr style="background-color: #f0f7ff;">
                            <th class="text-center bg-primary bg-opacity-10">라인/공정</th>
                            <td class="px-2 py-1">
                                <div class="d-flex gap-1">
                                    <input type="text" class="form-control form-control-sm bg-light" :value="searchForm.linenm" readonly style="font-size: 12px;">
                                    <input type="text" class="form-control form-control-sm bg-light" :value="selectedProg.prognm" readonly style="font-size: 12px;">
                                </div>
                            </td>
                            <th class="text-center bg-primary bg-opacity-10 required">출고창고</th>
                            <td class="px-2 py-1">
                                <select v-model="masterForm.whcd" class="form-select form-select-sm" style="font-size: 12px;">
                                    <option v-for="opt in whOptions" :key="opt.whcd" :value="opt.whcd">{{ opt.whnm }}</option>
                                </select>
                            </td>
                            <th class="text-center bg-primary bg-opacity-10">지시일자</th>
                            <td class="px-2 py-1">
                                <input v-model="ordymd_f" type="date" class="form-control form-control-sm bg-light" readonly style="font-size: 12px;">
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
              <span class="fw-bold small text-dark" style="font-size: 12px;">
                <i class="bi bi-list-check me-2 text-primary"></i>제품 생산 실적
              </span>
              <button class="btn btn-sm btn-outline-primary py-0 px-2 fw-bold" @click="addPerformanceRow" :disabled="!selectedProg.progcd" style="font-size: 11px;">+ 실적추가</button>
            </div>
            <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
              <div ref="tableRef2" class="tabulator-instance flex-grow-1"></div>
            </div>
          </div>

          <!-- 상태 안내 바 -->
          <div class="alert alert-info py-1 px-3 m-0 d-flex align-items-center justify-content-center border-0 rounded-0 bg-opacity-10" style="min-height: 28px;">
            <i class="bi bi-info-circle-fill me-2 text-info" style="font-size: 14px;"></i>
            <span class="fw-bold text-info" style="font-size: 11px;">
              {{ selectedProduct.prodid ? '실적 등록 완료! 하단 자재 내역을 확인하고 필요시 수정 후 [자재저장] 하세요.' : '제품 실적을 선택하거나 등록하십시오.' }}
            </span>
          </div>

          <!-- 우측 하단: 투입 자재 상세 (grid3) -->
          <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column grid-container-right">
            <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
              <span class="fw-bold small text-dark" style="font-size: 12px;">
                <i class="bi bi-box-arrow-in-right me-2 text-success"></i>투입 자재 상세
                <span v-if="selectedProduct.itemnm" class="badge bg-success-subtle text-success border border-success-subtle ms-2">{{ selectedProduct.itemnm }}</span>
              </span>
              <button class="btn btn-sm btn-outline-primary py-0 px-2 fw-bold" @click="addMaterialRow" :disabled="!selectedProduct.prodid" style="font-size: 11px;">+ 자재추가</button>
            </div>
            <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
              <div ref="tableRef3" class="tabulator-instance flex-grow-1"></div>
            </div>
          </div>

        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, computed, nextTick } from 'vue'
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

const authStore = useAuthStore()
const { today } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps } = useCommonHelp()

const initymd = today.replace(/-/g, '')
const searchForm = reactive({ linecd: '010', linenm: '', ordymd: initymd })
const masterForm = reactive({ whcd: '300' })

const lineOptions = ref<any[]>([])
const whOptions = ref<any[]>([])
const selectedProg = reactive({ progcd: '', prognm: '' })
const selectedProduct = reactive<any>({ itemcd: '', itemnm: '', prodid: 0 })

const ordymd_f = computed({
  get: () => searchForm.ordymd && searchForm.ordymd.length === 8 ? `${searchForm.ordymd.substring(0, 4)}-${searchForm.ordymd.substring(4, 6)}-${searchForm.ordymd.substring(6, 8)}` : '',
  set: (v) => { if (v) searchForm.ordymd = v.replace(/-/g, '') }
})

const tableRef1 = ref<HTMLDivElement | null>(null)
const tableRef2 = ref<HTMLDivElement | null>(null)
const tableRef3 = ref<HTMLDivElement | null>(null)
let grid1: Tabulator | null = null
let grid2: Tabulator | null = null
let grid3: Tabulator | null = null

const isNotReceived = (cell: any) => {
  const d = cell.getData();
  return !(Number(d.inqty || 0) > 0);
}

const initGrids = () => {
  grid1 = new Tabulator(tableRef1.value!, {
    layout: "fitColumns", height: "100%", placeholder: "라인 선택", selectable: 1,
    columnDefaults: { headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "No", formatter: "rownum", width: 40, hozAlign: "center" },
      { title: "공정명", field: "prognm", hozAlign: "left" }
    ],
  });
  grid1.on("rowClick", (e, row) => onProcessSelect(row.getData()));

  grid2 = new Tabulator(tableRef2.value!, {
    layout: "fitColumns", height: "100%", placeholder: "공정을 선택하세요.", selectable: 1,
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "상태", field: "_status", width: 50, hozAlign: "center", formatter: (c) => {
          const d = c.getData();
          if (Number(d.inqty || 0) > 0) return '<i class="bi bi-lock-fill text-secondary" title="입고완료"></i>';
          const v = c.getValue();
          if (!v) return '';
          if (v === '입력') return '<span class="badge bg-primary">입력</span>';
          if (v === '수정') return '<span class="badge bg-warning text-dark">수정</span>';
          if (v === '삭제') return '<span class="badge bg-danger">삭제</span>';
          return '';
      }, frozen: true },
      { title: "제품명", field: "itemnm", minWidth: 200, widthGrow: 1, cssClass: 'fw-bold text-primary',
        cellClick: (e, cell) => {
            const d = cell.getData();
            if (d._state === 'NEW') handleOpenHelp('ITEM_PROD', cell.getRow());
        }
      },
      { title: "규격", field: "itsize", width: 150 },
      { title: "단위", field: "unit", width: 60, hozAlign: "center" },
      { title: "생산일자", field: "proymd", width: 130, hozAlign: "center",
        editor: "input", editorParams: { elementAttributes: { type: "date" } },
        editable: isNotReceived,
        formatter: (c) => formatDate(c.getValue()),
        cellEdited: (cell) => {
            const val = cell.getValue();
            if (val && val.includes('-')) {
                cell.getRow().update({ proymd: val.replace(/-/g, '') });
            }
            const d = cell.getData();
            if (d._state === 'EXIST') {
                const isOrig = (d.proymd_o && d.proymd_o.trim() !== '') || Number(d.prdqty_o || 0) > 0;
                cell.getRow().update({ _status: isOrig ? '수정' : '입력' });
            }
        }
      },
      { title: "지시량", field: "ordqty", width: 90, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "금회생산", field: "prdqty", width: 90, hozAlign: "right", editor: "number", cssClass: "bg-light-yellow fw-bold",
        editable: isNotReceived,
        cellEdited: (cell) => {
            const d = cell.getData();
            let prdqty = Number(d.prdqty || 0);
            const ordqty = Number(d.ordqty || 0);
            const errqty = Number(d.errqty || 0);

            // [체크] 지시량 초과 입력 제한
            if (prdqty > ordqty) {
                alert(`생산량이 지시량(${ordqty})을 초과할 수 없습니다.`);
                prdqty = ordqty;
                cell.getRow().update({ prdqty: prdqty });
            }

            cell.getRow().update({ godqty: prdqty - errqty });

            if (d._state === 'EXIST') {
                const isOrig = (d.proymd_o && d.proymd_o.trim() !== '') || Number(d.prdqty_o || 0) > 0;
                cell.getRow().update({ _status: isOrig ? '수정' : '입력' });
            }
        }
      },
      { title: "불량", field: "errqty", width: 70, hozAlign: "right", editor: "number",
        editable: isNotReceived,
        cellEdited: (cell) => {
            const d = cell.getData();
            const prdqty = Number(d.prdqty || 0);
            const errqty = Number(d.errqty || 0);
            cell.getRow().update({ godqty: prdqty - errqty });

            if (d._state === 'EXIST') {
                const isOrig = (d.proymd_o && d.proymd_o.trim() !== '') || Number(d.prdqty_o || 0) > 0;
                cell.getRow().update({ _status: isOrig ? '수정' : '입력' });
            }
        }
      },
      { title: "양품수량", field: "godqty", width: 90, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "bg-light" },
      { title: "작업시간(분)", field: "workmm", width: 90, hozAlign: "right", editor: "number",
        editable: isNotReceived,
        cellEdited: (cell) => {
            const d = cell.getData();
            cell.getRow().update({ manmm: Number(d.workmm || 0) * Number(d.mancnt || 0) });
            if (d._state === 'EXIST') {
                const isOrig = (d.proymd_o && d.proymd_o.trim() !== '') || Number(d.prdqty_o || 0) > 0;
                cell.getRow().update({ _status: isOrig ? '수정' : '입력' });
            }
        }
      },
      { title: "인원", field: "mancnt", width: 60, hozAlign: "right", editor: "number",
        editable: isNotReceived,
        cellEdited: (cell) => {
            const d = cell.getData();
            cell.getRow().update({ manmm: Number(d.workmm || 0) * Number(d.mancnt || 0) });
            if (d._state === 'EXIST') {
                const isOrig = (d.proymd_o && d.proymd_o.trim() !== '') || Number(d.prdqty_o || 0) > 0;
                cell.getRow().update({ _status: isOrig ? '수정' : '입력' });
            }
        }
      },
      { title: "투입공수(분)", field: "manmm", width: 90, hozAlign: "right", editor: "number",
        editable: isNotReceived,
        cellEdited: (cell) => {
            const d = cell.getData();
            if (d._state === 'EXIST') {
                const isOrig = (d.proymd_o && d.proymd_o.trim() !== '') || Number(d.prdqty_o || 0) > 0;
                cell.getRow().update({ _status: isOrig ? '수정' : '입력' });
            }
        }
      },
      { title: "삭제", width: 40, hozAlign: "center", formatter: () => "<i class='bi bi-trash text-danger cursor-pointer'></i>", cellClick: (e, cell) => handleRowAction(cell.getRow()) }
    ]
  });
  grid2.on("rowClick", (e, row) => {
      const d = row.getData();
      if (d.prodid && Number(d.prodid) > 0) {
          fetchMaterialData(d);
      } else {
          grid3?.clearData();
          selectedProduct.itemcd = '';
          selectedProduct.itemnm = '';
          selectedProduct.prodid = 0;
      }
  });

  grid3 = new Tabulator(tableRef3.value!, {
    layout: "fitColumns", height: "100%", placeholder: "제품을 선택하세요.", selectable: true,
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "상태", field: "_status", width: 50, hozAlign: "center", formatter: (c) => {
          const v = c.getValue();
          if (!v) return '';
          if (v === '입력') return '<span class="badge bg-primary">입력</span>';
          if (v === '수정') return '<span class="badge bg-warning text-dark">수정</span>';
          if (v === '삭제') return '<span class="badge bg-danger">삭제</span>';
          return '';
      }},
      { title: "투입자재명", field: "mitemnm", minWidth: 200, widthGrow: 1, cssClass: 'fw-bold text-success', cellClick: (e, cell) => handleOpenHelp('MATERIAL', cell.getRow()) },
      { title: "규격", field: "mitsize", width: 150 },
      { title: "단위", field: "munit", width: 60, hozAlign: "center" },
      { title: "재고량", field: "jaego", width: 90, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "소요량", field: "soqty", width: 90, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "투입량", field: "inqty", width: 100, hozAlign: "right", editor: "number", cssClass: "bg-light-green fw-bold",
        cellEdited: (cell) => {
            const d = cell.getData();
            if (d._state === 'EXIST') {
                const isOrig = Number(d.inqty_o || 0) > 0;
                cell.getRow().update({ _status: isOrig ? '수정' : '입력' });
            }
        }
      },
      { title: "전공정명", field: "prognm", width: 130, cellClick: (e, cell) => handleOpenHelp('befprog', cell.getRow()) },
      { title: "삭제", width: 40, hozAlign: "center", formatter: () => "<i class='bi bi-trash text-danger cursor-pointer'></i>", cellClick: (e, cell) => handleRowAction(cell.getRow()) }
    ]
  });
}

const fetchLineOptions = async () => {
    try {
        const res = await api.get('/hp00/HP00_000S_STR', { params: { gubun: 'L0', cmpycd: authStore.cmpycd, gbncd: 'Y' } })
        lineOptions.value = res.data.map((i: any) => ({ linecd: i.linecd, linenm: i.linenm }))
        const currentLine = lineOptions.value.find(o => o.linecd === searchForm.linecd);
        if (currentLine) {
            searchForm.linenm = currentLine.linenm;
        } else if (lineOptions.value.length > 0) {
            searchForm.linecd = lineOptions.value[0].linecd;
            searchForm.linenm = lineOptions.value[0].linenm;
        }
    } catch (e) {}
}

const fetchProcesses = async () => {
  if (!searchForm.linecd) return vAlertError('생산라인을 먼저 선택하세요.');
  try {
    const res = await api.get('/hp00/HP00_000S_STR', { params: { gubun: 'G0', cmpycd: authStore.cmpycd, gbncd: searchForm.linecd, code: '' } })
    const cleanData = (res.data || []).filter((i: any) => i.progcd && i.progcd.trim() !== '');
    grid1?.setData(cleanData);
    grid2?.clearData(); grid3?.clearData();
    selectedProg.progcd = ''; selectedProg.prognm = '';
    selectedProduct.itemcd = ''; selectedProduct.itemnm = '';
  } catch (e) {}
}

const onProcessSelect = (prog: any) => {
  selectedProg.progcd = prog.progcd; selectedProg.prognm = prog.prognm;
  fetchPerfData()
}

const fetchPerfData = async (keepDetail = false) => {
  try {
    const res = await api.post('/hpio/HPIO_300U_STR', {
        actkind: 'S0',
        prodid: 0,
        cmpycd: authStore.cmpycd,
        linecd: searchForm.linecd,
        progcd: selectedProg.progcd,
        ordymd: searchForm.ordymd,
        lotymd: '', lotno: '', ordym: '', ordno: '', proymd: '', itemcd: '', itsize: '', unit: '',
        prdqty: 0, godqty: 0, errqty: 0, workmm: 0,
        mancnt: 0,
        manmm: 0,
        whcd: '', custcd: '', equpcd: '', prodcd: '', wkgbn: '', bigo: '', useyn: 'Y'
    })
    const validData = (res.data || []).filter((i: any) => i.itemcd && i.ordymd);
    grid2?.setData(validData.map((i: any) => ({
        ...i,
        _state: 'EXIST',
        _status: '',
        prdqty_o: i.prdqty || 0,
        proymd_o: i.proymd || '',
        proymd: i.proymd || initymd,
        workmm: i.workmm || 0,
        mancnt: i.mancnt || 0,
        manmm: i.manmm || 0
    })))
    if (!keepDetail) {
        grid3?.clearData();
        selectedProduct.itemcd = '';
        selectedProduct.itemnm = '';
        selectedProduct.prodid = 0;
    }
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('실적 조회 실패') }
}

const fetchMaterialData = async (product: any) => {
  Object.assign(selectedProduct, product)
  try {
    const res = await api.post('/hpio/HPIO_301U_STR', {
        actkind: 'S0',
        prodid: product.prodid || 0,
        matlid: 0,
        cmpycd: authStore.cmpycd,
        linecd: searchForm.linecd,
        progcd: selectedProg.progcd,
        itemcd: product.itemcd,
        proymd: product.proymd,
        ordymd: '', lotymd: '', lotno: '', ordym: '', ordno: '',
        mitemcd: '', mitsize: '', munit: '', soqty: product.prdqty || 0, inqty: 0,
        useyn: 'Y', whcd: masterForm.whcd, befprog: '', astkind: '', updemp: authStore.userid
    })
    const mappedData = (res.data || []).map((i: any) => {
        const item: any = {};
        Object.keys(i).forEach(k => { item[k.toLowerCase()] = i[k]; });
        const d = {
            ...item,
            _state: 'EXIST',
            _status: '',
            inqty_o: item.inqty || 0
        };
        if (Number(d.inqty || 0) === 0) d.inqty = d.soqty || 0;
        return d;
    })
    grid3?.setData(mappedData)
  } catch (e) { vAlertError('자재 조회 실패') }
}

const savePerformance = async () => {
  const details = grid2?.getData().filter((r: any) => r._status) || []
  if (details.length === 0) return vAlertError('저장할 실적 변경 항목이 없습니다.')
  if (!confirm('제품 생산 실적을 저장하시겠습니까?')) return
  try {
    for (const item of details) {
      if (Number(item.inqty || 0) > 0) {
        return vAlertError(`[${item.itemnm}] 항목은 이미 입고 처리되어 수정할 수 없습니다.`);
      }
      const isOrig = (item.proymd_o && item.proymd_o.trim() !== '') || Number(item.prdqty_o || 0) > 0;
      const actkind = isOrig ? (item._status === '삭제' ? 'D0' : 'U0') : 'A0';
      const res = await api.post('/hpio/HPIO_300U_STR', {
        ...item,
        actkind,
        cmpycd: authStore.cmpycd,
        linecd: searchForm.linecd,
        progcd: selectedProg.progcd,
        whcd: masterForm.whcd,
        prodcd: '100', wkgbn: '100',
        prdqty: Number(item.prdqty || 0),
        errqty: Number(item.errqty || 0),
        godqty: Number(item.prdqty || 0) - Number(item.errqty || 0),
        updemp: authStore.userid,
        useyn: item._status === '삭제' ? 'N' : 'Y'
      })
      const out = res.data?.[0];
      if (out?.ret_yn === 'Y' || out?.result === 'Y') {
          return vAlertError(out?.msg || '처리 중 오류가 발생했습니다.');
      }
      const returnedId = out?.result || out?.prodid;
      if (actkind === 'A0' && returnedId) {
          const prodidInt = parseInt(returnedId);
          item.prodid = prodidInt;
          selectedProduct.prodid = prodidInt;
          selectedProduct.itemcd = item.itemcd;
          selectedProduct.itemnm = item.itemnm;
          selectedProduct.proymd = item.proymd;
          selectedProduct.prdqty = item.prdqty;
          await nextTick();
          await fetchMaterialData(selectedProduct);
      }
    }
    vAlert('실적이 저장되었습니다.');
    await fetchPerfData(true);
  } catch (e) { vAlertError('실적 저장 실패') }
}

const saveMaterials = async () => {
  const details = grid3?.getData().filter((r: any) => r._status) || []
  if (details.length === 0) return vAlertError('저장할 자재 변경 항목이 없습니다.')
  if (!confirm('투입 자재 상세 내역을 저장하시겠습니까?')) return
  try {
    for (const item of details) {
      const actkind = item._status === '입력' ? 'A0' : (item._status === '삭제' ? 'D0' : 'U0')
      const res = await api.post('/hpio/HPIO_301U_STR', {
        ...item,
        actkind,
        prodid: selectedProduct.prodid,
        cmpycd: authStore.cmpycd,
        linecd: searchForm.linecd,
        progcd: selectedProg.progcd,
        whcd: masterForm.whcd,
        updemp: authStore.userid,
        useyn: item._status === '삭제' ? 'N' : 'Y'
      })
      if (res.data?.[0]?.result === 'Y') return vAlertError(res.data[0].msg);
    }
    vAlert('자재 정보가 저장되었습니다.'); fetchMaterialData(selectedProduct)
  } catch (e) { vAlertError('자재 저장 실패') }
}

const handleOpenHelp = (type: string, row: any) => {
  const commonPath = '/ha00/HA00_00P_STR'
  const hpPath = '/hp00/HP00_000S_STR'

  switch (type) {
    case 'REASON': // 불량원인 선택
      Object.assign(modalProps, {
        title: '불량원인 선택',
        path: commonPath,
        defaultField: 'cdnm',
        data: { gubun: 'E0', cmpycd: authStore.cmpycd, gbncd: '210' },
        columns: [
          { title: '코드', field: 'code', width: 100, hozAlign: 'center' },
          { title: '원인명', field: 'cdnm', minWidth: 200 }
        ],
        onConfirm: (data: any) => {
          row.update({ wonincd: data.code, woninnm: data.cdnm })
        }
      })
      break

    case 'MATERIAL': // 투입자재 선택
      Object.assign(modalProps, {
        title: '자재 선택',
        path: hpPath,
        defaultField: 'itemnm',
        large: true,
        data: { gubun: 'I1', cmpycd: authStore.cmpycd, gbncd: 'A', code: '', remark: '' },
        columns: [
          { title: '품목코드', field: 'itemcd', width: 120, hozAlign: 'center' },
          { title: '품목명', field: 'itemnm', minWidth: 250, widthGrow: 1 },
          { title: '규격', field: 'itsize', width: 150 },
          { title: '단위', field: 'unit', width: 80, hozAlign: 'center' }
        ],
        onConfirm: (data: any) => {
          row.update({
            mitemcd: data.itemcd,
            mitemnm: data.itemnm,
            mitsize: data.itsize,
            munit: data.unit,
            soqty: 0,
            inqty: 0,
            _status: '입력',
            _state: 'NEW'
          })
        }
      })
      break

    case 'befprog': // 전공정 선택
      Object.assign(modalProps, {
        title: '이전공정 선택',
        path: hpPath,
        defaultField: 'prognm',
        data: { gubun: 'G0', cmpycd: authStore.cmpycd, gbncd: searchForm.linecd, code: '' },
        columns: [
          { title: '코드', field: 'progcd', width: 100, hozAlign: 'center' },
          { title: '공정명', field: 'prognm', minWidth: 200 }
        ],
        onConfirm: (data: any) => {
          row.update({ befprog: data.progcd, prognm: data.prognm })
          if (row.getData()._state === 'EXIST') {
            row.update({ _status: '수정' })
          }
        }
      })
      break

    case 'ITEM_PROD': // 제품 선택
      Object.assign(modalProps, {
        title: '제품 선택',
        path: hpPath,
        defaultField: 'itemnm',
        large: true,
        data: { gubun: 'I1', cmpycd: authStore.cmpycd, gbncd: 'A', code: '', remark: '' },
        columns: [
          { title: '제품코드', field: 'itemcd', width: 120, hozAlign: 'center' },
          { title: '제품명', field: 'itemnm', minWidth: 250, widthGrow: 1 },
          { title: '규격', field: 'itsize', width: 150 },
          { title: '단위', field: 'unit', width: 80, hozAlign: 'center' }
        ],
        onConfirm: (data: any) => {
          row.update({
            itemcd: data.itemcd,
            itemnm: data.itemnm,
            itsize: data.itsize,
            unit: data.unit,
            ordqty: 0,
            prdqty: 0
          })
        }
      })
      break
  }
  modalVisible.value = true
}

const fetchWhOptions = async () => {
  try {
    const res = await api.post('/hs00/HS00_000S_STR', { gubun: 'W0', cmpycd: authStore.cmpycd, gbncd: '', code: '', codenm: '', etcval: '' })
    whOptions.value = res.data.map((i: any) => ({ whnm: i.whnm, whcd: i.whcd }));
  } catch (e) {}
}

const addPerformanceRow = () => {
  if (!selectedProg.progcd) return vAlertError('먼저 공정을 선택하세요.');
  grid2?.addRow({ _status: '입력', _state: 'NEW', linenm: searchForm.linenm, prognm: selectedProg.prognm, whcd: masterForm.whcd, ordymd: searchForm.ordymd, proymd: initymd, prdqty: 0, errqty: 0, useyn: 'Y' }, true);
}

const handleRowAction = (row: any) => {
  const d = row.getData();
  if (Number(d.inqty || 0) > 0) {
    return vAlertError('이미 입고 처리된 실적입니다. 수정을 원하시면 입고 취소를 먼저 진행하십시오.');
  }
  if (d._state === 'NEW') row.delete();
  else row.update({ _status: d._status === '삭제' ? '' : '삭제' });
}

const addMaterialRow = () => {
  if (!selectedProduct.itemcd) return vAlertError('먼저 제품을 선택하세요.');
  grid3?.addRow({ soqty: 0, inqty: 0, _status: '입력', _state: 'NEW', useyn: 'Y' }, true);
}

const onLineChange = () => {
    const line = lineOptions.value.find(o => o.linecd === searchForm.linecd);
    searchForm.linenm = line ? line.linenm : '';
    fetchProcesses();
}
const onFilterChange = () => { if (selectedProg.progcd) fetchPerfData() }

const initialize = () => {
  resetForm(searchForm);
  Object.assign(searchForm, { linecd: '010', ordymd: initymd });
  masterForm.whcd = '300';
  grid1?.clearData(); grid2?.clearData(); grid3?.clearData();
  selectedProg.progcd = ''; selectedProg.prognm = '';
  selectedProduct.itemcd = ''; selectedProduct.itemnm = '';
  fetchLineOptions();
}

const formatDate = (v: any) => v && v.length === 8 ? `${v.substring(0, 4)}-${v.substring(4, 6)}-${v.substring(6, 8)}` : v;
onMounted(() => { fetchLineOptions(); fetchWhOptions(); nextTick(initGrids); })
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; font-size: 12px; }
.grid-container-left, .grid-container-right { border-bottom: 3px solid #005a9f !important; }
.required::after { content: " *"; color: red; }

/* 좌측 공정 그리드 짝홀수 색상 제거 및 선택 색상 강조 */
.grid-container-left :deep(.tabulator-row-even) { background-color: #fff !important; }
.grid-container-left :deep(.tabulator-row.tabulator-selected) { background-color: #005a9f !important; color: #fff !important; }
.grid-container-left :deep(.tabulator-row:hover) { background-color: #f0f7ff !important; cursor: pointer; }
</style>
