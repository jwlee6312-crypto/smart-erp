<!--
	=============================================================
	프로그램명	: 제품입고(자가생산) (HPIO400U)
	작성일자	: 2025.02.24
	설명        : 생산 공정별 완료 항목의 입고 처리 관리 (균등배분 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-box-arrow-in-down me-2 text-primary" style="font-size: 18px;"></i>
        생산정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">제품입고(자가생산) (HPIO400U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchProcesses">조회</button>
        <button class="btn-erp btn-save" @click="saveReceipt" :disabled="!selectedProg.progcd">입고저장</button>
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
                <col style="width: 10%" /><col style="width: 50%" />
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
                <th class="text-center bg-light required">생산일자</th>
                <td class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                  <input v-model="fromdt" type="date" class="form-control form-control-sm" style="width: 150px; font-size: 12px;" />
                  <span class="px-1 opacity-50">~</span>
                  <input v-model="todt" type="date" class="form-control form-control-sm" style="width: 150px; font-size: 12px;" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 투-그리드 레이아웃 영역 -->
      <div class="d-flex gap-2 flex-grow-1 overflow-hidden" style="min-height: 0;">

        <!-- ⬅️ 좌측: 공정 목록 (grid1) -->
        <div class="card border shadow-sm d-flex flex-column overflow-hidden grid-container-left" style="width: 250px; min-width: 250px;">
          <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark text-center">공정 목록</div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="tableRef1" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>

        <!-- ➡️ 우측: 입고 대상 그리드 (grid2) -->
        <div class="flex-grow-1 d-flex flex-column gap-2 overflow-hidden">
          <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column grid-container-right">

            <!-- 🛠 그리드 상단 공통 입력 항목 (균등 배분 적용) -->
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
                            <th class="text-center bg-primary bg-opacity-10 required">입고창고</th>
                            <td class="px-2 py-1">
                                <select v-model="masterForm.whcd" class="form-select form-select-sm border-primary" style="font-size: 12px;">
                                    <option v-for="opt in whOptions" :key="opt.whcd" :value="opt.whcd">{{ opt.whnm }}</option>
                                </select>
                            </td>
                            <th class="text-center bg-primary bg-opacity-10 required">입고일자</th>
                            <td class="px-2 py-1">
                                <input v-model="inymd_f" type="date" class="form-control form-control-sm border-primary" style="font-size: 12px;">
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
              <span class="fw-bold small text-dark" style="font-size: 12px;">
                <i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>입고 대상 내역
              </span>
              <div class="small text-muted" style="font-size: 11px;">※ 항목 선택 후 상단 '입고저장'을 클릭하세요.</div>
            </div>
            <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
              <div ref="tableRef2" class="tabulator-instance flex-grow-1"></div>
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
import { getDate } from '@/composables/useDate'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'

const authStore = useAuthStore()
const { today, firstDay } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

// [1] 데이터 모델링
const initymd = today.replace(/-/g, '')
const initfromdt = firstDay.replace(/-/g, '')

const searchForm = reactive({ linecd: '010', linenm: '', fromdt: initfromdt, todt: initymd })
const masterForm = reactive({ whcd: '200', inymd: initymd })

const lineOptions = ref<any[]>([]);
const whOptions = ref<any[]>([])
const selectedProg = reactive({ progcd: '', prognm: '' })
const closingInfo = reactive({ clsymd: '', sclsym: '' })

// 포맷팅 헬퍼
const fromdt = computed({ get: () => formatDate(searchForm.fromdt), set: (v) => { if(v) searchForm.fromdt = v.replace(/-/g, '') } })
const todt = computed({ get: () => formatDate(searchForm.todt), set: (v) => { if(v) searchForm.todt = v.replace(/-/g, '') } })
const inymd_f = computed({ get: () => formatDate(masterForm.inymd), set: (v) => { if(v) masterForm.inymd = v.replace(/-/g, '') } })

const tableRef1 = ref<HTMLDivElement | null>(null)
const tableRef2 = ref<HTMLDivElement | null>(null)
let grid1: Tabulator | null = null
let grid2: Tabulator | null = null

const initGrids = () => {
  grid1 = new Tabulator(tableRef1.value!, {
    layout: "fitColumns", height: "100%", placeholder: "라인 선택", selectable: 1,
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "No", formatter: "rownum", width: 40, hozAlign: "center" },
      { title: "공정명", field: "prognm", hozAlign: "left" },
      { title: "코드", field: "progcd", hozAlign: "center", width: 80, cssClass: "fw-bold text-primary" }
    ],
  });
  grid1.on("rowClick", (e, row) => onProcessSelect(row.getData()));

  grid2 = new Tabulator(tableRef2.value!, {
    layout: "fitColumns", height: "100%", placeholder: "공정을 선택하세요.", selectable: true,
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "선택", formatter: "rowSelection", titleFormatter: "rowSelection", width: 40, hozAlign: "center", frozen: true },
      { title: "생산라인", field: "linenm", width: 100 },
      { title: "생산공정", field: "prognm", width: 100 },
      { title: "생산일자", field: "proymd", width: 90, hozAlign: "center", formatter: (c) => formatDate(c.getValue()) },
      { title: "제품명", field: "itemnm", minWidth: 180, widthGrow: 1, cssClass: "fw-bold text-primary" },
      { title: "규격", field: "itsize", width: 120 },
      { title: "단위", field: "unit", width: 50, hozAlign: "center" },
      { title: "생산량", field: "prdqty", width: 80, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "양품", field: "godqty", width: 80, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "text-success fw-bold" },
      { title: "불량", field: "errqty", width: 60, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "text-danger" },
      { title: "기입고", field: "db_inqty", width: 80, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
      { title: "미입고", field: "ninqty", width: 80, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, cssClass: "text-primary fw-bold" },
      { title: "입고량", field: "inqty", width: 90, hozAlign: "right", editor: "number", cssClass: "bg-light-green fw-bold" },
      { title: "비고", field: "work_info", width: 120, hozAlign: "left" }
    ]
  });
}

const fetchLineOptions = async () => {
    try {
        const res = await api.get('/hp00/HP00_000S_STR', { params: { gubun: 'L0', cmpycd: authStore.cmpycd, gbncd: 'Y' } })
        lineOptions.value = res.data.map((i: any) => ({ linecd: i.linecd, linenm: i.linenm }))

        // 명시적으로 현재 선택된 라인명 설정
        const currentLine = lineOptions.value.find(o => o.linecd === searchForm.linecd);
        if (currentLine) {
            searchForm.linenm = currentLine.linenm;
        } else if (lineOptions.value.length > 0) {
            searchForm.linecd = lineOptions.value[0].linecd;
            searchForm.linenm = lineOptions.value[0].linenm;
        }
    } catch (e) {}
}

const fetchWhOptions = async () => {
  try {
    const res = await api.post('/hs00/HS00_000S_STR', { gubun: 'W0', cmpycd: authStore.cmpycd, gbncd: '', code: '', codenm: '', etcval: '' })
    whOptions.value = res.data.map((i: any) => ({ whcd: i.whcd, whnm: i.whnm }));
  } catch (e) {}
}

const fetchProcesses = async () => {
  if (!searchForm.linecd) return vAlertError('라인을 선택하세요.');
  try {
    const res = await api.get('/hp00/HP00_000S_STR', { params: { gubun: 'G2', cmpycd: authStore.cmpycd, gbncd: searchForm.linecd, code: '' } })
    // 공통코드 조회 결과의 필드명(progcd/code) 무관하게 매핑
    const cleanData = (res.data || []).map((i: any) => ({
        progcd: i.progcd,
        prognm: i.prognm
    })).filter((i: any) => i.progcd.trim() !== '');

    grid1?.setData(cleanData)
    grid2?.clearData(); selectedProg.progcd = ''; selectedProg.prognm = '';
  } catch (e) {}
}

const onProcessSelect = (prog: any) => {
  selectedProg.progcd = prog.progcd; selectedProg.prognm = prog.prognm
  fetchGridData()
}

const fetchGridData = async () => {
  try {
    const res = await api.post('/hpio/HPIO_400U_STR', {
      actkind: 'S0',
      cmpycd: authStore.cmpycd,
      linecd: searchForm.linecd,
      progcd: selectedProg.progcd,
      fromdt: searchForm.fromdt,
      todt: searchForm.todt,
      prodid: 0, inqty: 0, iorowno: 0,
      ioymd: '', whcd: '', deptcd: '', iogbn: '', ioym: '', iono: '', updemp: authStore.userid
    })

    const mapped = res.data.map((item: any) => {
        const dbInQty = Number(item.inqty || 0); // 기입고량
        const godQty = Number(item.godqty || 0); // 양품수량
        const ninQty = Math.max(0, godQty - dbInQty); // 미입고량

        return {
            ...item,
            linenm: searchForm.linenm,
            prognm: selectedProg.prognm,
            db_inqty: dbInQty,
            ninqty: ninQty,
            inqty: ninQty, // 💡 미입고량만큼 자동 표시
            work_info: item.prodcd === '100' ? item.worknm : (item.custnm || '')
        }
    })
    grid2?.setData(mapped)
    vAlert('조회되었습니다.')
  } catch (e) { vAlertError('조회 실패') }
}

const saveReceipt = async () => {
  const selected = grid2?.getSelectedData() || []
  if (selected.length === 0) return vAlertError('입고 처리할 항목을 선택하세요.')

  // 0보다 큰 입고량만 필터링
  const targets = selected.filter((d: any) => Number(d.inqty || 0) > 0)
  if (targets.length === 0) return vAlertError('입고량이 0인 항목은 처리할 수 없습니다.')

  if (!confirm(`선택한 ${targets.length}건의 내역을 입고 처리하시겠습니까?`)) return

  try {
    const inYmd = masterForm.inymd.replace(/-/g, '')
    const targetWhcd = masterForm.whcd
    if (inYmd <= closingInfo.clsymd) return vAlertError(`회계 마감된 일자(${inYmd})입니다.`)
    if (inYmd.substring(0, 6) <= closingInfo.sclsym) return vAlertError(`영업 마감된 월입니다.`)

    for (const item of targets) {
      // 1. 입고 마스터 저장 (HSIO_060U_STR)
      const masterRes = await api.post('/hsio/HSIO_060U_STR', {
          actkind: 'A0', cmpycd: authStore.cmpycd, iogbn: '100', iotype: '100', custcd: '',
          fromdt: '', todt: '', deptcd: authStore.deptcd , whcd: targetWhcd, ioymd: inYmd,
          remark: `생산입고-${masterForm.inymd}`, cfmyn: 'Y', userid: authStore.userid
      })

      const ioYm = masterRes.data[0].ioym;
      const ioNo = masterRes.data[0].iono;

      // 2. 입고 상세 저장 (HSIO_061U_STR)
      const detailRes = await api.post('/hsio/HSIO_061U_STR', {
          actkind: 'A0', cmpycd: authStore.cmpycd, iogbn: '100', iotype: '100', custcd: '',
          ioym: ioYm, iono: ioNo, deptcd: authStore.deptcd, whcd: targetWhcd, ioymd: inYmd,
          itemcd: item.itemcd, unit: item.unit, itsize: item.itsize,
          ioqty: item.inqty, ioamt: 0, iovat: 0, balym: '', balno: '', browno: '', cfmyn: 'Y',
          userid: authStore.userid, ordym: item.ordym, ordno: item.ordno
      })

      const ioRowNo = detailRes.data[0].iorowno

      // 3. 생산 실적에 입고 정보 업데이트 (HPIO_400U_STR - A0)
      await api.post('/hpio/HPIO_400U_STR', {
          actkind: 'A0', cmpycd: authStore.cmpycd, linecd: searchForm.linecd,
          progcd: selectedProg.progcd, iogbn: '100',
          ioymd: inYmd, deptcd: authStore.deptcd, whcd: targetWhcd,
          inqty: item.inqty, ioym: ioYm, iono: ioNo, iorowno: ioRowNo,
          ordym: item.ordym, ordno: item.ordno,
          prodid: item.prodid // 생산실적 ID 추가
      })
    }
    vAlert('정상적으로 입고 처리되었습니다.'); fetchGridData()
  } catch (e) { vAlertError('입고 처리 중 오류 발생') }
}

const onLineChange = () => {
    const line = lineOptions.value.find(o => o.linecd === searchForm.linecd);
    if (line) searchForm.linenm = line.linenm;
    fetchProcesses();
}

const initialize = () => {
  resetForm(searchForm);
  Object.assign(searchForm, { linecd: '010', fromdt: initfromdt, todt: initymd });
  Object.assign(masterForm, { whcd: '200', inymd: initymd });
  grid1?.clearData(); grid2?.clearData();
  selectedProg.progcd = ''; selectedProg.prognm = '';
  fetchLineOptions();
}

const formatDate = (v: any) => v && v.length === 8 ? `${v.substring(0, 4)}-${v.substring(4, 6)}-${v.substring(6, 8)}` : v;

onMounted(async () => {
  api.get('/hp00/HP00_000S_STR', { params: { gubun: 'CL', cmpycd: authStore.cmpycd } }).then(r => {
    if (r.data?.length) { closingInfo.clsymd = r.data[0].clsymd; closingInfo.sclsym = r.data[0].sclsym; }
  })
  fetchLineOptions(); fetchWhOptions(); nextTick(initGrids);
})
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
