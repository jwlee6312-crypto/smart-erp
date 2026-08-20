<!--
	=============================================================
	프로그램명	: 최종부적합 처리 (HPIO950U)
	작성일자	: 2025.02.24
	설명        : 제품 검사 판정 및 부적합 사항에 대한 최종 처리 (조회 결함 완벽 해결)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-shield-exclamation me-2 text-danger" style="font-size: 18px;"></i>
        품질관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        검사결과 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">최종부적합 처리 (HPIO950U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-save" @click="save">처리저장</button>
      </div>
    </div>

    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup><col style="width: 10%" /><col style="width: 23%" /><col style="width: 10%" /><col style="width: 23%" /><col style="width: 10%" /><col style="width: 24%" /></colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light required">검사분류</th>
                <td><select v-model="searchForm.insp_gb" class="form-select form-select-sm" @change="search"><option v-for="opt in inspGbOptions" :key="opt.code" :value="opt.code">{{ opt.cdnm }}</option></select></td>
                <th class="text-center bg-light required">의뢰일자</th>
                <td><div class="input-group input-group-sm"><input v-model="searchForm.insp_req_dt_f" type="date" class="form-control" /><span class="input-group-text">~</span><input v-model="searchForm.insp_req_dt_t" type="date" class="form-control" /></div></td>
                <th class="text-center bg-light required">라인</th>
                <td><select v-model="searchForm.linecd" class="form-select form-select-sm"><option v-for="item in lineData" :key="item.linecd" :value="item.linecd">{{ item.linenm }}</option></select></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="flex-grow-1 d-flex gap-2 overflow-hidden">
        <div class="card border shadow-sm d-flex flex-column overflow-hidden grid-container-left" style="width: 450px; min-width: 450px;">
          <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark"><i class="bi bi-list-task me-2 text-primary"></i>부적합 처리 대기 목록</div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column"><div ref="leftGridRef" class="tabulator-instance flex-grow-1"></div></div>
        </div>

        <div class="flex-grow-1 d-flex flex-column gap-2 overflow-hidden">
          <div class="card border shadow-sm flex-shrink-0 overflow-hidden border-start border-4 border-info">
            <div class="card-body p-0 bg-white">
              <table class="erp-table-dense w-100">
                <colgroup><col style="width: 100px;" /><col style="width: 25%;" /><col style="width: 100px;" /><col style="width: 25%;" /><col style="width: 100px;" /><col /></colgroup>
                <tbody>
                  <tr style="background-color: #f0faff;">
                    <th class="text-center bg-info bg-opacity-10">의뢰번호</th>
                    <td class="px-2 py-1"><input :value="displayReqNo" class="form-control form-control-sm bg-light text-center fw-bold" readonly /></td>
                    <th class="text-center bg-info bg-opacity-10">LOT No</th>
                    <td class="px-2 py-1"><input :value="displayLotNo" class="form-control form-control-sm bg-light text-center fw-bold text-danger" readonly /></td>
                    <th class="text-center bg-info bg-opacity-10">라인명</th>
                    <td class="px-2 py-1"><select v-model="detailForm.linecd" class="form-select form-select-sm bg-light" disabled><option v-for="item in lineData" :key="item.linecd" :value="item.linecd">{{ item.linenm }}</option></select></td>
                  </tr>
                  <tr>
                    <th class="text-center bg-light">제품명</th>
                    <td class="px-2 py-1" colspan="3"><input v-model="detailForm.itemnm" class="form-control form-control-sm bg-light fw-bold" readonly /></td>
                    <th class="text-center bg-light">생산수량</th>
                    <td class="px-2 py-1"><input v-model="detailForm.prdqty" class="form-control form-control-sm bg-light text-end" readonly /></td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <div class="card border shadow-sm flex-shrink-0 overflow-hidden border-start border-4 border-primary">
            <div class="card-body p-0 bg-white">
              <table class="erp-table-dense w-100">
                <colgroup><col style="width: 100px;" /><col style="width: 25%;" /><col style="width: 100px;" /><col style="width: 25%;" /><col style="width: 100px;" /><col /></colgroup>
                <tbody>
                  <tr style="background-color: #f0faff;">
                    <th class="text-center bg-primary bg-opacity-10">검사일자</th>
                    <td class="px-2 py-1"><input v-model="resultForm.insp_dt" type="date" class="form-control form-control-sm bg-light" readonly /></td>
                    <th class="text-center bg-primary bg-opacity-10">담당자</th>
                    <td class="px-2 py-1"><input v-model="resultForm.user_nm" class="form-control form-control-sm bg-light" readonly /></td>
                    <th class="text-center bg-primary bg-opacity-10 required">판정결과</th>
                    <td class="px-2 py-1"><select v-model="resultForm.judg_cd" class="form-select form-select-sm fw-bold text-primary"><option v-for="opt in judgCdOptions" :key="opt.code" :value="opt.code">{{ opt.cdnm }}</option></select></td>
                  </tr>
                  <tr>
                    <th class="text-center bg-light">의뢰수량</th>
                    <td class="px-2 py-1"><input v-model="resultForm.sample_qty" class="form-control form-control-sm bg-light text-end" readonly /></td>
                    <th class="text-center bg-light text-danger">불량수(검사)</th>
                    <td class="px-2 py-1"><input v-model="resultForm.errqty" class="form-control form-control-sm bg-light text-end text-danger fw-bold" readonly /></td>
                    <th class="text-center bg-light text-success">양품수</th>
                    <td class="px-2 py-1"><input v-model="resultForm.godqty" class="form-control form-control-sm bg-light text-end text-success fw-bold" readonly /></td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <div class="card border shadow-sm flex-grow-1 d-flex flex-column overflow-hidden grid-container-right">
            <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
              <span class="fw-bold small text-dark"><i class="bi bi-tools me-2 text-primary"></i>실물 부적합 처리 상세</span>
              <div class="d-flex gap-1"><button class="btn btn-sm btn-outline-primary py-0 px-2 fw-bold" @click="addRow">+ 행추가</button><button class="btn btn-sm btn-outline-danger py-0 px-2 fw-bold" @click="delRow">- 행삭제</button></div>
            </div>
            <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column"><div ref="mainGridRef" class="tabulator-instance flex-grow-1"></div></div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick, computed } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import AppAlert from '@/components/AppAlert.vue'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()

const now = new Date(); const firstDay = new Date(now.getFullYear(), now.getMonth(), 1).toISOString().substring(0, 10); const today = now.toISOString().substring(0, 10)
const searchForm = reactive({ insp_gb: '300', insp_req_dt_f: firstDay, insp_req_dt_t: today, linecd: '010' })
const detailForm = reactive({ insp_req_dt: '', insp_req_no: '', prodid: null, lotno: '', linecd: '', itemnm: '', prdqty: 0, proymd: '', insp_gb: '' })
const resultForm = reactive({ insp_dt: '', user_nm: '', judg_cd: '', sample_qty: 0, errqty: 0, godqty: 0 })

const displayReqNo = computed(() => detailForm.insp_req_no ? `${detailForm.insp_req_dt}-${detailForm.insp_req_no}` : '')
const displayLotNo = computed(() => detailForm.proymd ? `${detailForm.proymd}-${detailForm.lotno}` : detailForm.lotno)

const inspGbOptions = ref<any[]>([]); const lineData = ref<any[]>([]); const judgCdOptions = ref<any[]>([]); const rsltCdOptions = ref<Record<string, string>>({})
const leftGridRef = ref<HTMLElement | null>(null); const mainGridRef = ref<HTMLElement | null>(null)
let leftGrid: Tabulator | null = null; let mainGrid: Tabulator | null = null

const mapLower = (list: any[]) => (list || []).map(i => {
    const n: any = {}; for (let k in i) n[k.toLowerCase()] = i[k]; return n;
})

const initialize = () => {
	searchForm.insp_req_dt_f = firstDay; searchForm.insp_req_dt_t = today
	if (inspGbOptions.value.length > 0) searchForm.insp_gb = inspGbOptions.value[0].code
	if (lineData.value.length > 0) searchForm.linecd = lineData.value[0].linecd
	Object.assign(detailForm, { insp_req_dt: '', insp_req_no: '', prodid: null, lotno: '', linecd: '', itemnm: '', prdqty: 0, proymd: '', insp_gb: '' })
	Object.assign(resultForm, { insp_dt: '', user_nm: '', judg_cd: '', sample_qty: 0, errqty: 0, godqty: 0 })
	leftGrid?.clearData(); mainGrid?.clearData()
}

const search = async () => {
	if (!searchForm.insp_gb || !searchForm.linecd) return vAlertError('조회 조건을 확인하세요.')
	try {
		const params = { ...searchForm, cmpycd: authStore.cmpycd, insp_req_dt_f: searchForm.insp_req_dt_f.replaceAll('-', ''), insp_req_dt_t: searchForm.insp_req_dt_t.replaceAll('-', '') }
		const { data } = await api.get('/product/insp-judg/req-list', { params })
		leftGrid?.setData(mapLower(data))
	} catch (e) { vAlertError('조회 실패') }
}

const selectJudgList = async (rowData: any) => {
    if (!rowData.insp_req_no) return;
	try {
        // 💡 쿼리에서 가져온 rowData에는 이제 insp_gb가 포함되어 있습니다.
		const { data } = await api.get('/product/insp-judg/judg-list', {
            params: {
                cmpycd: authStore.cmpycd,
                insp_gb: rowData.insp_gb,
                insp_req_dt: rowData.insp_req_dt,
                insp_req_no: rowData.insp_req_no
            }
        })
		mainGrid?.setData(mapLower(data).map(n => ({ ...n, _status: '' })))

        const n = mapLower([rowData])[0];
		Object.assign(resultForm, {
			insp_dt: n.insp_dt ? n.insp_dt.replace(/(\d{4})(\d{2})(\d{2})/, '$1-$2-$3') : today,
			user_nm: n.updemp || authStore.usernm, judg_cd: n.judg_cd || '100',
			sample_qty: n.prdqty || 0, errqty: n.errqty || 0, godqty: Number(n.prdqty || 0) - Number(n.errqty || 0)
		})
	} catch (e) {}
}

const save = async () => {
	const all = mainGrid?.getData() || [];
    const targets = all.filter((d: any) => d._status)

    // 💡 변경사항 체크: 그리드 변화가 없더라도 판정결과가 있으면 저장 허용
    if (targets.length === 0 && !resultForm.judg_cd) return vAlertError('변경사항이 없습니다.')

    // 불량수와 처리수량 합계 검증 (불합격인 경우에만)
    const totalProcessQty = all.filter(d => d._status !== '삭제').reduce((acc, cur) => acc + Number(cur.rslt_qty || 0), 0)
    if (resultForm.judg_cd === '200' && Number(resultForm.errqty) !== totalProcessQty) {
        return vAlertError(`처리수량 합계 불일치 (불량:${resultForm.errqty}, 처리:${totalProcessQty})`)
    }

	if (!confirm('저장하시겠습니까?')) return
	try {
        let payload = []
        if (targets.length > 0) {
            payload = targets.map(d => ({ ...d, cmpycd: authStore.cmpycd, insp_gb: detailForm.insp_gb, judg_cd: resultForm.judg_cd, updemp: authStore.userid, state: d._status === '입력' ? 'C' : (d._status === '삭제' ? 'D' : 'U') }))
        } else {
            // 💡 그리드 변경이 없어도 판정결과 업데이트를 위해 헤더 정보 전송
            payload = [{ ...detailForm, ...resultForm, cmpycd: authStore.cmpycd, insp_gb: detailForm.insp_gb, judg_cd: resultForm.judg_cd, updemp: authStore.userid, state: 'U' }]
        }

		const { code } = await api.post('/product/insp-judg/save', payload)
		if (code === 'S') { vAlert('저장되었습니다.'); search(); selectJudgList(detailForm); }
	} catch (e) { vAlertError('저장 실패') }
}

const addRow = () => {
	if (!detailForm.insp_req_no) return vAlertError('의뢰를 선택하세요.')
	mainGrid?.addRow({ _status: '입력', rslt_qty: 0, rslt_cd: '', ...detailForm, cmpycd: authStore.cmpycd }, true)
}

const delRow = () => {
	const sel = mainGrid?.getSelectedRows()
	if (!sel?.length) return vAlertError('삭제할 행을 선택하세요.')
	sel.forEach(row => { const d = row.getData(); if (d._status === '입력') row.delete(); else { row.update({ _status: d._status === '삭제' ? '' : '삭제' }) } })
}

const initGrids = () => {
    if (!leftGridRef.value || !mainGridRef.value) return;
	leftGrid = new Tabulator(leftGridRef.value, {
		layout: 'fitColumns', height: '100%', selectable: 1,
		columnDefaults: { headerHozAlign: 'center', headerSort: false },
		columns: [
            { title: '상태', field: 'judg_cd', width: 85, hozAlign: 'center', formatter: (cell) => {
                const v = cell.getValue();
                if (v === '100') return '<span class="badge bg-success">합격</span>';
                if (v === '200') return '<span class="badge bg-danger">불합격</span>';
                if (v === '300') return '<span class="badge bg-warning text-dark">특채</span>';
                return '<span class="badge bg-secondary">대기</span>';
            }},
            { title: '의뢰일자', field: 'insp_req_dt', width: 110, hozAlign: 'center', formatter: (c) => { const v = c.getValue(); return v && v.length === 8 ? `${v.substring(0,4)}-${v.substring(4,6)}-${v.substring(6,8)}` : v; } },
			{ title: '의뢰번호', field: 'insp_req_no', width: 80, hozAlign: 'center', cssClass: 'fw-bold text-primary' },
			{ title: '제품명', field: 'itemnm', hozAlign: 'left' }
		]
	})
	leftGrid.on('rowClick', (e, row) => { const d = row.getData(); Object.assign(detailForm, d); selectJudgList(d) })
	mainGrid = new Tabulator(mainGridRef.value, {
        layout: 'fitColumns', height: '100%', selectable: true,
        columns: [
            { title: '선택', width: 40, hozAlign: 'center', headerHozAlign: 'center', formatter: "rowSelection", titleFormatter: "rowSelection" },
            { title: "상태", field: "_status", width: 70, hozAlign: "center", headerHozAlign: "center", formatter: (cell) => { const v = cell.getValue(); if (v === '입력') return '<span class="badge bg-primary">입력</span>'; if (v === '수정') return '<span class="badge bg-warning text-dark">수정</span>'; if (v === '삭제') return '<span class="badge bg-danger">삭제</span>'; return ''; } },
            { title: '처리방법', field: 'rslt_cd', widthGrow: 2, hozAlign: 'center', headerHozAlign: 'center', editor: 'list', editorParams: { values: rsltCdOptions.value }, formatter: (cell) => rsltCdOptions.value[cell.getValue()] || cell.getValue() },
            { title: '수량', field: 'rslt_qty', width: 120, hozAlign: 'right', headerHozAlign: 'center', editor: 'number', formatter: 'money', formatterParams: { precision: 0 } },
            { title: '비고', field: 'bigo', widthGrow: 3, hozAlign: 'left', headerHozAlign: 'center', editor: 'input' }
        ]
    })
	mainGrid.on('cellEdited', (cell) => { if (cell.getRow().getData()._status !== '입력') cell.getRow().update({ _status: '수정' }) })
}

onMounted(async () => {
    try {
        const resLine = await api.post('/hp00/HP00_000S_STR', { gubun: 'L0', cmpycd: authStore.cmpycd, gbncd: 'Y' })
        lineData.value = mapLower(resLine.data)
        const resGb = await api.post('/hp00/HP00_000S_STR', { gubun: 'E0', cmpycd: authStore.cmpycd, gbncd: '820' })
        inspGbOptions.value = mapLower(resGb.data)
        const resJudg = await api.post('/hp00/HP00_000S_STR', { gubun: 'E0', cmpycd: authStore.cmpycd, gbncd: '860' })
        judgCdOptions.value = mapLower(resJudg.data)
        const resRslt = await api.post('/hp00/HP00_000S_STR', { gubun: 'E0', cmpycd: authStore.cmpycd, gbncd: '870' })
        rsltCdOptions.value = mapLower(resRslt.data).reduce((acc: any, cur: any) => ({ ...acc, [cur.code]: cur.cdnm }), {})
    } catch (e) {}
    nextTick(initGrids);
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; font-size: 12px; }
:deep(.tabulator-cell[tabulator-field="rslt_qty"]) { justify-content: flex-end !important; }
.required::after { content: " *"; color: red; }
</style>
