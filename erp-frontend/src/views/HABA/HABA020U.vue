<!--
	=============================================================
	프로그램명	: 재무제표설정 (HABA020U)
	작성일자	: 2025.03.14
	설명        : 재무제표 양식 및 그룹 설정 (HSOD100U 디자인 표준 준수)
	=============================================================
-->

<template>
  <AppAlert :show="showalert" :error="showerror" :message="alertmessage" />
  <Modal v-model:visible="modalvisible" :modalProps="modalprops" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-list-columns-reverse me-2 text-primary" style="font-size: 18px;"></i>
        시스템 관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        기본정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">재무제표설정 (HABA020U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
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
                    <select v-model="searchform.yyyy" class="form-select form-select-sm fw-bold text-center" @change="search">
                      <!-- y 대신 opt 객체의 yyyy 속성을 바인딩 -->
                      <option v-for="opt in yearoptions" :key="opt.yyyy" :value="opt.yyyy">{{ opt.yyyy }}년</option>
                    </select>
                </td>
                <th class="bg-light text-center border-start" style="width: 100px;">재무제표</th>
                <td style="width: 200px;">
                  <select v-model="searchform.gubun" class="form-select form-select-sm" @change="search">
                    <option v-for="opt in statementoptions" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                  </select>
                </td>
                <td class="small text-muted ps-3"><i class="bi bi-info-circle me-1"></i>연도와 종류를 선택하여 항목을 관리하십시오.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [중간] 상세 정보 입력 영역 -->
      <div class="card border shadow-sm overflow-hidden flex-shrink-0">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
          <div class="fw-bold small text-dark"><i class="bi bi-pencil-square me-2 text-secondary"></i>재무제표 항목 상세 관리</div>
          <div v-if="masterform.actkind === 'U1'" class="badge bg-primary-subtle text-primary border border-primary-subtle px-2">수정 중</div>
          <div v-else class="badge bg-success-subtle text-success border border-success-subtle px-2">신규 등록</div>
        </div>
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense w-100">
            <colgroup>
              <col style="width: 100px;" /><col style="width: 150px;" />
              <col style="width: 100px;" /><col style="width: 250px;" />
              <col style="width: 100px;" /><col style="width: 150px;" />
              <col style="width: 100px;" /><col />
            </colgroup>
            <tbody>
              <tr>
                <th class="required bg-light text-center">코    드</th>
                <td><input v-model="masterform.upacct" type="text" class="form-control form-control-sm text-center fw-bold" maxlength="10" :readonly="masterform.actkind === 'U1'" /></td>
                <th class="required bg-light text-center border-start">명    칭</th>
                <td><input v-model="masterform.acctnm" type="text" class="form-control form-control-sm" maxlength="50" /></td>
                <th class="bg-light text-center border-start">차대구분</th>
                <td>
                  <select v-model="masterform.dbcr" class="form-select form-select-sm text-center">
                    <option value="">선택</option>
                    <option value="D">차변</option>
                    <option value="C">대변</option>
                  </select>
                </td>
                <th class="bg-light text-center border-start">표시위치</th>
                <td>
                  <select v-model="masterform.dsprl" class="form-select form-select-sm text-center">
                    <option value="">선택</option>
                    <option value="L">왼쪽</option>
                    <option value="R">오른쪽</option>
                  </select>
                </td>
              </tr>
              <tr>
                <th class="bg-light text-center border-top">출력순서</th>
                <td class="border-top"><input v-model="masterform.dspord" type="number" class="form-control form-control-sm text-center" /></td>
                <th class="bg-light text-center border-start border-top">결과여부</th>
                <td class="border-top">
                  <div class="form-check form-switch m-0 d-flex align-items-center justify-content-center h-100">
                    <input v-model="masterform.rstyn" class="form-check-input mt-0" type="checkbox" true-value="Y" false-value="N" id="rstYnSwitch">
                    <label class="form-check-label ms-2 small fw-bold" for="rstYnSwitch">{{ masterform.rstyn === 'Y' ? 'Y' : 'N' }}</label>
                  </div>
                </td>
                <th class="bg-light text-center border-start border-top">사용여부</th>
                <td class="border-top">
                  <div class="form-check form-switch m-0 d-flex align-items-center justify-content-center h-100">
                    <input v-model="masterform.useyn" class="form-check-input mt-0" type="checkbox" true-value="Y" false-value="N" id="useYnSwitch020">
                    <label class="form-check-label ms-2 small fw-bold" for="useYnSwitch020">{{ masterform.useyn === 'Y' ? '사용' : '중지' }}</label>
                  </div>
                </td>
                <th class="bg-light text-center border-start border-top">수정가능</th>
                <td class="border-top">
                  <div class="form-check form-switch m-0 d-flex align-items-center justify-content-center h-100">
                    <input v-model="masterform.updyn" class="form-check-input mt-0" type="checkbox" true-value="Y" false-value="N" id="updYnSwitch">
                    <label class="form-check-label ms-2 small fw-bold" for="updYnSwitch">{{ masterform.updyn === 'Y' ? 'Y' : 'N' }}</label>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center">
          <i class="bi bi-table me-2 text-primary"></i>
          <span class="fw-bold small text-dark">재무제표 구성 목록 (연도: {{ searchform.yyyy }})</span>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="maingridelement" class="tabulator-instance flex-grow-1"></div>
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

const authstore = useAuthStore()
const { showAlert: showalert, showError: showerror, alertMessage: alertmessage, vAlert: valert, vAlertError: valerterror } = useAlerts()
const { resetForm: resetform } = useFormReset()

const searchform = reactive({ yyyy: '', gubun: '010' })
const yearoptions = ref<string[]>([])
const statementoptions = ref<any[]>([])

const masterform = reactive<any>({
  actkind: 'I1', cmpycd: authstore.cmpycd, gubun: '010', yyyy: '',
  upacct: '', acctnm: '', dbcr: '', actgbn: '', dspord: '', dsprl: '', rstyn: 'N', useyn: 'Y', updyn: 'Y'
})

const maingridelement = ref<HTMLDivElement | null>(null)
let maingrid: Tabulator | null = null

const search = async () => {
  if (!searchform.yyyy) return;
  try {
    const res = await api.post('/haba/HABA_020U_STR', {
        actkind: 'S2', cmpycd: authstore.cmpycd, gubun: searchform.gubun, yyyy: searchform.yyyy,
        upacct: '', acctnm: '', dbcr: '', actgbn: '', dspord: '', dsprl: '', rstyn: 'N', useyn: 'Y'    })
    maingrid?.setData(res.data || []);
  } catch (e) { valerterror('조회 실패') }
}

const save = async () => {
  if (!masterform.upacct || !masterform.acctnm) return valerterror('코드와 명칭은 필수입니다.');
  try {
    masterform.yyyy = searchform.yyyy;
    const res = await api.post('/haba/HABA_020U_STR', { ...masterform, cmpycd: authstore.cmpycd, updemp: authstore.userid })
    const resdata = res.data?.[0] || {};
    if (resdata.res === 'OK' || resdata.ret_yn === 'Y') {
      valert('성공적으로 저장되었습니다.');
      search(); initialize();
    } else { valerterror(resdata.msg || resdata.ret_msg || '저장 실패'); }
  } catch (e) { valerterror('저장 실패') }
}

const initialize = () => {
  const curyyyy = searchform.yyyy;
  const curgubun = searchform.gubun;
  resetform(masterform);
  Object.assign(masterform, {
    actkind: 'I1', cmpycd: authstore.cmpycd, gubun: curgubun, yyyy: curyyyy,
    rstyn: 'N', useyn: 'Y', updyn: 'Y'
  });
}

onMounted(async () => {
    // 🚀 1. 적용년도 목록 로드 (표준 actkind: 'S4' 사용)
    try {

        const yRes = await api.post('/haba/HABA_020U_STR', {
            actkind: 'S4',
            cmpycd: authstore.cmpycd,
            yyyy: '', gubun: '', upacct: '', acctnm: '', dbcr: '',
            actgbn: '', dspord: '', dsprl: '', rstyn: '', useyn: '', updyn: '', updemp: ''
        });

        if (yRes.data && yRes.data.length > 0) {
            // 템플릿의 opt.yyyy 접근을 위해 객체 배열 형태로 저장
            yearoptions.value = yRes.data;
            // 초기 선택값 설정 (첫 번째 연도를 문자열로 할당)
            searchform.yyyy = String(yearoptions.value[0].yyyy);
        }

    } catch (e) { console.error('Year List Load Failed') }

    // 2. 재무제표 종류 로드
    api.post('/ha00/HA00_00P_STR', { gubun: 'E0', gbncd: '070', cmpycd: authstore.cmpycd }).then(r => {
        if (r.data) {
            statementoptions.value = (r.data || [])
              .filter((n: any) => n.codecd <= '030');
        }
    });

    if (maingridelement.value) {
        maingrid = new Tabulator(maingridelement.value, {
            layout: "fitColumns", height: "100%", selectable: 1,
            columnDefaults: { headerSort: false, headerHozAlign: 'center', hozAlign: 'center', vertAlign: "middle" },
            columns: [
                { title: "No", formatter: "rownum", width: 40 },
                { title: "코드", field: "upacct", width: 120, cssClass: "fw-bold text-primary border-end" },
                { title: "항목 명칭", field: "acctnm", minWidth: 200, hozAlign: "left" },
                { title: "차대", field: "dbcr", width: 60, formatter: (c) => String(c.getValue() || '').toUpperCase() === 'D' ? '차변' : (String(c.getValue() || '').toUpperCase() === 'C' ? '대변' : '') },
                { title: "순서", field: "dspord", width: 60 },
                { title: "위치", field: "dsprl", width: 80, formatter: (c) => {
                    const v = String(c.getValue() || '').toUpperCase();
                    return v === 'L' ? '왼쪽' : (v === 'R' ? '오른쪽' : '');
                }},
                { title: "결과", field: "rstyn", width: 60 },
                { title: "사용", field: "useyn", width: 80, hozAlign: "center",
                  formatter: (cell) => {
                    const val = String(cell.getValue() || '').trim().toUpperCase();
                    return val === 'Y' ? '<b class="text-primary">사용</b>' : '';
                  }
                }
            ]
        });
        maingrid.on("rowClick", (e, row) => {
            const data = row.getData();
            Object.assign(masterform, data);
            masterform.actkind = 'U1';
        });
    }
    nextTick(() => search());
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
:deep(.tabulator-row-selected) { background-color: #e7f1ff !important; }
</style>
