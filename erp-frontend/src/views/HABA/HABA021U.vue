<!--
	=============================================================
	프로그램명 : 재무제표 양식편집 (HABA021U)
	작성일자	: 2025.03.14
	설명        : 재무제표 구성 항목 및 계산 공식 설정 (표준 UI 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showalert" :error="showerror" :message="alertmessage" />
  <Modal v-model:visible="modalvisible" :modalProps="modalprops" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- [헤더] 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-calculator-fill me-2 text-primary" style="font-size: 18px;"></i>
        기본정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">재무제표 양식편집 (HABA021U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="frmReset">신규</button>
        <button class="btn-erp btn-search" @click="fetchleftgrid">조회</button>
        <button class="btn-erp btn-save" @click="frmSubmit">저장</button>
      </div>
    </div>

    <!-- [콘텐츠] 2. 메인 콘텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [검색] Search 필터 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-2 bg-white">
          <table class="erp-table-dense w-100">
            <tbody>
              <tr>
                <th class="bg-light text-center" style="width: 100px;">적용연도</th>
                <td style="width: 150px;">
                  <select v-model="searchform.yyyy" class="form-select form-select-sm fw-bold text-center" @change="fetchleftgrid">
                    <option v-for="y in yearoptions" :key="y" :value="y">{{ y }}년</option>
                  </select>
                </td>
                <th class="bg-light text-center border-start" style="width: 100px;">재무제표</th>
                <td style="width: 200px;">
                  <select v-model="searchform.gubun" class="form-select form-select-sm" @change="fetchleftgrid">
                    <option v-for="opt in statementoptions" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                  </select>
                </td>
                <td class="px-3">
                    <button class="btn btn-sm btn-secondary px-3" @click="fetchleftgrid"><i class="bi bi-search me-1"></i>조회</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [입력] frmBody 입력 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense w-100">
            <colgroup>
              <col style="width: 120px;" /><col style="width: 38%;" />
              <col style="width: 120px;" /><col />
            </colgroup>
            <tbody>
              <tr>
                <th class="bg-light text-center">재무제표</th>
                <td class="bg-light-yellow">
                  <div class="d-flex align-items-center gap-1">
                    <input v-model="searchform.yyyy" type="text" class="form-control form-control-sm text-center bg-light-fix fw-bold" style="max-width: 80px;" readonly />
                    <span class="small me-1">년</span>
                    <select v-model="searchform.gubun" class="form-select form-select-sm" disabled>
                      <option v-for="opt in statementoptions" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                    </select>
                  </div>
                </td>
                <th class="bg-light text-center border-start">집계계정</th>
                <td class="bg-light-yellow">
                  <div class="d-flex align-items-center gap-1">
                    <input v-model="masterdata.upacct" type="text" class="form-control form-control-sm text-center bg-light-fix fw-bold" style="max-width: 100px;" readonly />
                    <input v-model="masterdata.upacct_t" type="text" class="form-control form-control-sm bg-light-fix flex-grow-1" readonly />
                  </div>
                </td>
              </tr>
              <tr>
                <th class="bg-light text-center">계정과목</th>
                <td class="bg-light-yellow">
                  <div class="input-group input-group-sm">
                    <input v-model="masterdata.acctcd" type="text" class="form-control text-center bg-light-fix fw-bold" style="max-width: 80px;" readonly />
                    <input v-model="masterdata.acctcd_t" type="text" class="form-control" placeholder="계정명 입력 또는 검색" @keydown.enter="openhelp('acct')" />
                    <button class="btn btn-outline-secondary px-2" @click="openhelp('acct')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="bg-light text-center border-start">계산타입</th>
                <td class="bg-light-yellow">
                  <select v-model="masterdata.caltype" class="form-select form-select-sm">
                    <option value="000">선택</option>
                    <option v-for="opt in calctypeoptions" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                  </select>
                </td>
              </tr>
              <tr>
                <th class="bg-light text-center">계산방식</th>
                <td class="bg-light-yellow">
                  <select v-model="masterdata.calgagam" class="form-select form-select-sm">
                    <option value="000">선택</option>
                    <option v-for="opt in formulaoptions" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                  </select>
                </td>
                <th class="bg-light text-center border-start">사용여부</th>
                <td class="bg-light-yellow">
                  <div class="form-check form-check-inline m-0 ms-2 d-flex align-items-center h-100">
                    <input v-model="masterdata.useyn" class="form-check-input mt-0" type="checkbox" true-value="Y" false-value="N" id="useSwitch021">
                    <label class="form-check-label ms-2 small fw-bold" for="useSwitch021">사용</label>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [그리드] Grid 영역 (2분할) -->
      <div class="d-flex gap-2 flex-grow-1 overflow-hidden" style="min-height: 0;">

        <!-- ⬅️ 좌측: 집계계정 리스트 -->
        <div class="card border shadow-sm d-flex flex-column overflow-hidden" style="width: 40%;">
          <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center">
            <i class="bi bi-list-ul me-2 text-secondary"></i>
            <span class="fw-bold small text-dark">재무제표 집계계정</span>
          </div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="leftgridref" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>

        <!-- ➡️ 우측: 세부 구성 계정 리스트 -->
        <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column">
          <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center">
            <i class="bi bi-table me-2 text-primary"></i>
            <span class="fw-bold small text-dark">세부 구성 계정 리스트</span>
          </div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="rightgridref" class="tabulator-instance flex-grow-1"></div>
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
import AppAlert from '@/components/AppAlert.vue'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { useCommonHelp } from '@/composables/useCommonHelp'

const authstore = useAuthStore()
const { showAlert: showalert, showError: showerror, alertMessage: alertmessage, vAlert: valert, vAlertError: valerterror } = useAlerts()
const { resetForm: resetform } = useFormReset()
const { modalVisible: modalvisible, modalProps: modalprops, openHelp: commonopenhelp } = useCommonHelp()

// [1] 데이터 모델링
const yearoptions = ref<string[]>([])
const searchform = reactive({ yyyy: '', gubun: '010' })

const masterdata = reactive<any>({
  actkind: 'I1', cmpycd: authstore.cmpycd, yyyy: '', gubun: '',
  upacct: '', upacct_t: '', acctcd: '', acctcd_t: '', arowno: '',
  caltype: '000', calgagam: '000', useyn: 'Y', updyn: 'Y'
})

const statementoptions = ref<any[]>([]); const calctypeoptions = ref<any[]>([]); const formulaoptions = ref<any[]>([])
const leftgridref = ref<HTMLDivElement | null>(null); const rightgridref = ref<HTMLDivElement | null>(null)
let leftgrid: Tabulator | null = null; let rightgrid: Tabulator | null = null

// [2] 주요 함수
const fetchleftgrid = async () => {
    if (!searchform.yyyy) return;
	try {
		const res = await api.post('/haba/HABA_021U_STR', { actkind: 'S3', cmpycd: authstore.cmpycd, yyyy: searchform.yyyy, gubun: searchform.gubun })
		leftgrid?.setData(res.data || []);
        rightgrid?.clearData();
        frmReset();
	} catch (e) { valerterror('집계계정 조회 실패') }
}

const fetchrightgrid = async (row: any) => {
	try {
		const res = await api.post('/haba/HABA_021U_STR', { actkind: 'SR', cmpycd: authstore.cmpycd, yyyy: searchform.yyyy, gubun: row.gubun, upacct: row.upacct })
		rightgrid?.setData((res.data || []).map((n: any) => ({
            ...n,
            caltype_nm: n.caltypenm,
            calgagam_nm: n.calgagamnm
        })));
	} catch (e) { valerterror('상세 내역 조회 실패') }
}

const frmSubmit = async () => {
	if (masterdata.updyn !== 'Y') return valerterror('수정할 수 없습니다.')
	if (!searchform.gubun || searchform.gubun === '0') return valerterror('재무제표 종류를 선택해주십시오.')
	if (!masterdata.upacct) return valerterror('집계계정을 선택해주십시오.')
	if (!masterdata.acctcd) return valerterror('계정코드를 선택해주십시오.')
	if (masterdata.caltype === '000') return valerterror('계산타입을 선택해 주십시오.')
	if (masterdata.calgagam === '000') return valerterror('계산방식을 선택해 주십시오.')

	try {
		masterdata.yyyy = searchform.yyyy; masterdata.gubun = searchform.gubun
		const res = await api.post('/haba/HABA_021U_SAVE', { ...masterdata, cmpycd: authstore.cmpycd, userid: authstore.userid })
        const resdata = res.data?.[0] || {};
		if (resdata.ret_yn === 'N') { valerterror(resdata.ret_msg) }
        else { valert('저장되었습니다.'); fetchrightgrid({ gubun: masterdata.gubun, upacct: masterdata.upacct }); frmReset(); }
	} catch (e) { valerterror('저장 실패') }
}

const frmReset = () => {
    const curupacct = masterdata.upacct; const curupacctt = masterdata.upacct_t; const curupdyn = masterdata.updyn;
    resetform(masterdata);
    Object.assign(masterdata, {
        actkind: 'I1', cmpycd: authstore.cmpycd, useyn: 'Y',
        upacct: curupacct, upacct_t: curupacctt, updyn: curupdyn,
        caltype: '000', calgagam: '000'
    });
}

function openhelp(type: string) {
    if (type === 'acct') {
        commonopenhelp('ACCT', (d) => {
            masterdata.acctcd = d.acctcd; masterdata.acctcd_t = d.acctnm;
        }, { gubun: searchform.gubun });
    }
}

// [3] 초기화
onMounted(async () => {
    await nextTick();

    // Grids 초기화
    if (leftgridref.value) {
        leftgrid = new Tabulator(leftgridref.value, {
            layout: 'fitColumns', height: '100%', selectable: 1,
            columnDefaults: { headerSort: false, headerHozAlign: "center", hozAlign: "center", vertAlign: "middle" },
            columns: [
                { title: "계정코드", field: "upacct", width: 100, cssClass: "fw-bold text-primary border-end" },
                {
                    title: "항목명", field: "acctnm", hozAlign: "left", widthGrow: 1,
                    formatter: (cell) => {
                        const d = cell.getData(); const code = String(d.upacct);
                        // ASP Indent Logic 재현
                        let indent = 0;
                        if (code.substring(1, 7) === "000000" || ["1990000", "2980000", "3980000", "3990000"].includes(code)) { indent = 0; }
                        else if (code.substring(2, 7) === "00000") { indent = 10; }
                        else if (code.substring(3, 7) === "0000") { indent = 20; }
                        else if (code.substring(5, 7) === "00") { indent = 25; }
                        else { indent = 30; }

                        const color = d.inyn === 'Y' ? '#0d6efd' : '#212529';
                        return `<div style="padding-left: ${indent}px; color: ${color};">${indent > 0 ? '· ' : ''}${cell.getValue()}</div>`
                    }
                },
                { title: "구분", field: "gubun_nm", width: 100 }
            ]
        });
        leftgrid.on('rowClick', (e, row) => {
            const d = row.getData();
            masterdata.upacct = d.upacct; masterdata.upacct_t = d.acctnm; masterdata.updyn = d.updyn;
            fetchrightgrid(d); frmReset();
        });
    }

    if (rightgridref.value) {
        rightgrid = new Tabulator(rightgridref.value, {
            layout: 'fitColumns', height: '100%', selectable: 1,
            columnDefaults: { headerSort: false, headerHozAlign: "center", hozAlign: "center", vertAlign: "middle" },
            columns: [
                { title: "계정과목", field: "acctcd", width: 100, cssClass: "fw-bold text-primary border-end" },
                { title: "계정명", field: "acctnm", minWidth: 150, widthGrow: 1, hozAlign: "left" },
                { title: "계산타입", field: "caltype_nm", width: 100 },
                { title: "계산방식", field: "calgagam_nm", width: 100 },
                { title: "사용", field: "useyn", width: 80, hozAlign: "center",
                  formatter: (cell) => {
                    const val = String(cell.getValue() || '').trim().toUpperCase();
                    return val === 'Y' ? '<b class="text-primary">사용</b>' : '';
                  }
                }
            ]
        });
        rightgrid.on("rowClick", (e, row) => {
            const d = row.getData(); Object.assign(masterdata, d);
            masterdata.acctcd_t = d.acctnm; masterdata.actkind = 'U1';
        });
    }

    // 초기 데이터 로드
	try {
        const yRes = await api.post('/haba/HABA_021U_STR', { actkind: 'S4', cmpycd: authstore.cmpycd });
        if (yRes.data && yRes.data.length > 0) {
            yearoptions.value = yRes.data.map((r: any) => String(r.yyyy));
            searchform.yyyy = String(yRes.data[0].yyyy);
        }

		const resgbn = await api.post('/ha00/HA00_00P_STR', { gubun: 'E0', gbncd: '070', cmpycd: authstore.cmpycd })
		statementoptions.value = resgbn.data || []
        if (statementoptions.value.length > 0) searchform.gubun = statementoptions.value[0].codecd;

		const restype = await api.post('/ha00/HA00_00P_STR', { gubun: 'E0', gbncd: '080', cmpycd: authstore.cmpycd })
		calctypeoptions.value = restype.data || []

		const resformula = await api.post('/ha00/HA00_00P_STR', { gubun: 'E0', gbncd: '090', cmpycd: authstore.cmpycd })
		formulaoptions.value = resformula.data || []

        nextTick(() => fetchleftgrid());
	} catch (e) { console.error('초기 데이터 로드 실패') }
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
:deep(.tabulator-row-selected) { background-color: #e7f1ff !important; }
.bg-light-yellow { background-color: #f9f6e7 !important; }
.bg-light-fix { background-color: #f8f9fa !important; }
</style>
