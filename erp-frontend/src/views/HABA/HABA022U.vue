<!--
	=============================================================
	프로그램명	: 재무제표 차감수식 (HABA022U)
	작성일자	: 2025.03.14
	설명        : 재무제표 충당금 계정의 차감 연산 수식 설정 및 관리 (HABA021U 표준 준수)
	=============================================================
-->

<template>
  <AppAlert :show="showalert" :error="showerror" :message="alertmessage" />
  <Modal v-model:visible="modalvisible" :modalProps="modalprops" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-calculator-fill me-2 text-primary" style="font-size: 18px;"></i>
        기본정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        장부관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">재무제표 차감수식 (HABA022U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchleftgrid">조회</button>
        <button class="btn-erp btn-save" @click="save">저장</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 조회 필터 영역 -->
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
                <td class="px-3 text-muted small border-0 text-end pe-4">
                    <i class="bi bi-info-circle me-1"></i> 충당금 계정을 선택하여 수식을 설정하십시오.
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 레이아웃 영역 (2분할) -->
      <div class="d-flex gap-2 flex-grow-1 overflow-hidden" style="min-height: 0;">

        <!-- ⬅️ 좌측: 집계항목 목록 (HSOD100U 참조: width 400px로 조정) -->
        <div class="card border shadow-sm d-flex flex-column overflow-hidden grid-container-left" style="width: 400px; min-width: 400px;">
          <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center">
            <i class="bi bi-list-ul me-2 text-secondary"></i>
            <span class="fw-bold small text-dark">재무제표 집계항목</span>
          </div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="leftgridref" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>

        <!-- ➡️ 우측: 수식 편집 폼 + 상세 리스트 -->
        <div class="flex-grow-1 d-flex flex-column gap-2 overflow-hidden">
          <!-- ... (수식 편집 폼 생략) ... -->
          <!-- 상세 리스트 그리드 -->
          <div class="card border shadow-sm flex-grow-1 d-flex flex-column overflow-hidden grid-container-right">
            <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center">
              <i class="bi bi-table me-2 text-primary"></i>
              <span class="fw-bold small text-dark">세부 구성 수식 리스트</span>
            </div>
            <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
              <div ref="rightgridref" class="tabulator-instance flex-grow-1"></div>
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
import Modal from '@/components/Modal.vue'
import AppAlert from '@/components/AppAlert.vue'
import { useCommonHelp } from '@/composables/useCommonHelp'

const authstore = useAuthStore()
const { showAlert: showalert, showError: showerror, alertMessage: alertmessage, vAlert: valert, vAlertError: valerterror } = useAlerts()
const { resetForm: resetform } = useFormReset()
const { modalVisible: modalvisible, modalProps: modalprops, openHelp: commonopenhelp } = useCommonHelp()

const yearoptions = ref<string[]>([])
const searchform = reactive({ yyyy: new Date().getFullYear().toString(), gubun: '010' })
const masterdata = reactive<any>({
    actkind: 'I1', yyyy: '', gubun: '', upacct: '', upacct_t: '',
    acctcd: '', acctcd_t: '', arowno: '', caltype: '000', calgagam: '000', useyn: 'Y', updyn: 'Y', rstyn: 'N'
})

const statementoptions = ref<any[]>([]); const calctypeoptions = ref<any[]>([]); const formulaoptions = ref<any[]>([])
const leftgridref = ref<HTMLDivElement | null>(null); const rightgridref = ref<HTMLDivElement | null>(null)
let leftgrid: Tabulator | null = null; let rightgrid: Tabulator | null = null

const loadinitdata = async () => {
	try {
      try {
        const yRes = await api.post('/haba/HABA_022U_STR', {
            actkind: 'S4', cmpycd: authstore.cmpycd,
            yyyy: '', gubun: '', upacct: '', upacct_t: '',
            acctcd: '', acctcd_t: '', arowno: '', caltype: '000', calgagam: '000', useyn: 'Y', updyn: 'Y', rstyn: 'N'
        });
        if (yRes.data && yRes.data.length > 0) {
            yearoptions.value = yRes.data.map((r: any) => String(r.yyyy));
            searchform.yyyy = String(yRes.data[0].yyyy);
        }
      } catch (e) { console.error('Year List Load Failed') }

		const resgbn = await api.post('/ha00/HA00_00P_STR', { gubun: 'E0', gbncd: '070', cmpycd: authstore.cmpycd })
		statementoptions.value = resgbn.data || []
        if (statementoptions.value.length > 0) searchform.gubun = statementoptions.value[0].codecd;

		const restype = await api.post('/ha00/HA00_00P_STR', { gubun: 'E0', gbncd: '080', cmpycd: authstore.cmpycd })
		calctypeoptions.value = restype.data || []

		const resformula = await api.post('/ha00/HA00_00P_STR', { gubun: 'E0', gbncd: '090', cmpycd: authstore.cmpycd })
		formulaoptions.value = resformula.data || []

        nextTick(() => fetchleftgrid());
	} catch (e) { console.error('초기 데이터 로드 실패') }
}
const fetchleftgrid = async () => {
	try {
		const res = await api.post('/haba/HABA_022U_STR', { actkind: 'S3', cmpycd: authstore.cmpycd, yyyy: searchform.yyyy, gubun: searchform.gubun })
		const data = (res.data || []).map((n: any) => {
            return {
                ...n,
                gubun_nm: n.gubun_nm || (n.actgbn === '1' ? '집계' : '상세')
            }
		});

        // HSOD100U 표준: 불필요한 redraw 없이 데이터 설정
        leftgrid?.setData(data);
        rightgrid?.clearData();
        initialize();
	} catch (e) { valerterror('목록 조회 실패') }
}

const fetchrightgrid = async (row: any) => {
	try {


		const res = await api.post('/haba/HABA_022U_STR', {
            actkind: 'SR',
            cmpycd: authstore.cmpycd,
            yyyy: searchform.yyyy,
            gubun: row.gubun,
            upacct: row.upacct
		})
		rightgrid?.setData((res.data || []).map((n: any) => {
            return {
                ...n,
                caltype_nm: n.caltypenm,
                calgagam_nm: n.calgagamnm
            }
        }));
	} catch (e) { valerterror('수식 내역 조회 실패') }
}
const save = async () => {
	if (masterdata.updyn !== 'Y') return valerterror('수정할 수 없는 항목입니다.')
	if (masterdata.rstyn !== 'Y') return valerterror('충당금계정(결과) 항목만 등록 가능합니다.')
	if (!masterdata.upacct) return valerterror('좌측 집계항목을 선택하십시오.')
	if (!masterdata.acctcd) return valerterror('계정과목을 선택하십시오.')
	if (masterdata.caltype === '000') return valerterror('연산대상을 선택하십시오.')
	if (masterdata.calgagam === '000') return valerterror('연산수식을 선택하십시오.')

	try {
		masterdata.yyyy = searchform.yyyy; masterdata.gubun = searchform.gubun
		const res = await api.post('/haba/HABA_022U_SAVE', { ...masterdata, cmpycd: authstore.cmpycd, userid: authstore.userid })
        const resdata = res.data?.[0] || {};
		if (resdata.ret_yn === 'N') { valerterror(resdata.ret_msg) }
        else { valert('저장되었습니다.'); fetchrightgrid({ gubun: masterdata.gubun, upacct: masterdata.upacct }); initialize(); }
	} catch (e) { valerterror('저장 실패') }
}

const initialize = () => {
    const curupacct = masterdata.upacct; const curupacctt = masterdata.upacct_t;
    const currstyn = masterdata.rstyn; const curupdyn = masterdata.updyn; const curgubun = masterdata.gubun;
    resetform(masterdata);
    Object.assign(masterdata, {
        actkind: 'I1', cmpycd: authstore.cmpycd, useyn: 'Y',
        upacct: curupacct, upacct_t: curupacctt, rstyn: currstyn, updyn: curupdyn, gubun: curgubun,
        caltype: '000', calgagam: '000'
    });
}

function openhelp(type: string) {
    if (type === 'acct') {
        commonopenhelp('ACCT', (d) => {
            masterdata.acctcd = d.acctcd; masterdata.acctcd_t = d.acctnm;
        });
    }
}
onMounted(async () => {
    await nextTick();
    if (leftgridref.value) {
        // HSOD100U grid1 구성 참조
        leftgrid = new Tabulator(leftgridref.value, {
            layout: "fitColumns", height: "100%", placeholder: "데이터 없음",
            columns: [
                { title: "No", formatter: "rownum", width: 40, hozAlign: "center", headerSort: false },
                { title: "코드", field: "upacct", width: 90, hozAlign: "center", headerSort: false, cssClass: "fw-bold text-primary border-end" },
                {
                    title: "집계계정명", field: "acctnm", hozAlign: "left", headerSort: false,
                    formatter: (cell) => {
                        const d = cell.getData();
                        const code = String(d.upacct || '');
                        let indent = code.endsWith("00000") ? 0 : code.endsWith("0000") ? 15 : code.endsWith("00") ? 30 : 45;
                        const color = d.rstyn === 'Y' ? '#0d6efd' : '#212529';
                        const val = cell.getValue() || '';
                        return `<div style="padding-left: ${indent}px; color: ${color}; font-weight: ${d.rstyn === 'Y' ? '800' : 'normal'}; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">
                                    ${indent > 0 ? '· ' : ''}${val}
                                </div>`
                    }
                },
                { title: "구분", field: "gubun_nm", width: 70, hozAlign: "center", headerSort: false }
            ]
        });
        leftgrid.on('rowClick', (e, row) => {
            const d = row.getData();
            masterdata.upacct = d.upacct; masterdata.upacct_t = d.acctnm; masterdata.gubun = d.gubun; masterdata.updyn = d.updyn; masterdata.rstyn = d.rstyn;
            fetchrightgrid(d); initialize();
        });
    }
    if (rightgridref.value) {
        // HSOD100U grid2 구성 참조
        rightgrid = new Tabulator(rightgridref.value, {
            layout: "fitColumns", height: "100%", placeholder: "내역 없음", selectable: 1,
            columnDefaults: { headerSort: false, headerHozAlign: "center", hozAlign: "center", vertAlign: "middle" },
            columns: [
                { title: "No", formatter: "rownum", width: 40 },
                { title: "계정코드", field: "acctcd", width: 110, cssClass: "fw-bold text-primary border-end" },
                { title: "계정과목명", field: "acctnm", minWidth: 180, widthGrow: 1, hozAlign: "left" },
                { title: "연산대상", field: "caltypenm", width: 100 },
                { title: "연산수식", field: "calgagamnm", width: 100 },
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
            masterdata.acctcd_t = d.acctcd_t; masterdata.actkind = 'U1';
        });
    }
    await loadinitdata();
})
</script>

<style scoped>
.tabulator-instance {
  width: 100% !important;
  background-color: #fff;
}
:deep(.tabulator-row-selected) {
  background-color: #e7f1ff !important;
}
</style>
