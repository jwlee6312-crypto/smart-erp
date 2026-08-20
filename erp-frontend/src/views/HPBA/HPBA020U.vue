<!--
	=============================================================
	프로그램명	: 자재 대분류 관리 (HPBA020U)
	작성일자	: 2025.02.24
	설명        : 재고자산별 원자재, 제품 등의 대분류 체계 관리 (HPIO210U 표준 패턴 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-folder-fill me-2 text-primary" style="font-size: 18px;"></i>
        기본정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">자재 대분류 관리 (HPBA020U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-save" @click="save">저장</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 조회 및 입력 필터 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
                <col style="width: 10%" /><col style="width: 15%" />
                <col style="width: 10%" /><col style="width: 15%" />
                <col style="width: 10%" /><col style="width: 20%" />
                <col style="width: 10%" /><col style="width: 10%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light required">재고자산</th>
                <td>
                  <select v-model="formData.astkind" class="form-select" @change="search">
                    <option v-for="opt in astOptions" :key="opt.code" :value="opt.code">
                      [{{ opt.code }}] {{ opt.cdnm }}
                    </option>
                  </select>
                </td>
                <th class="text-center bg-light required">대분류코드</th>
                <td>
                  <input v-model="formData.agrpcd" class="form-control text-center fw-bold" maxlength="3" :readonly="formData.actkind === 'U0'" placeholder="3자리" />
                </td>
                <th class="text-center bg-light required">대분류명</th>
                <td>
                  <input v-model="formData.agrpnm" class="form-control" placeholder="분류명 입력" />
                </td>
                <th class="text-center bg-light">사용여부</th>
                <td>
                  <div class="form-check form-switch d-flex justify-content-center">
                    <input v-model="formData.useyn" class="form-check-input" type="checkbox" true-value="Y" false-value="N">
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column grid-container-right">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark"><i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>자재 대분류 목록</span>
          <span class="text-muted small">목록 클릭 시 수정 모드로 전환</span>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="tableRef" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, nextTick } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import AppAlert from '@/components/AppAlert.vue'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()

// [1] 데이터 모델링
const formData = reactive<any>({
  actkind: 'S0', cmpycd: authStore.cmpycd,
  astkind: '', agrpcd: '', agrpnm: '', useyn: 'Y'
})

const astOptions = ref<any[]>([])
const tableRef = ref<HTMLDivElement | null>(null)
let grid: Tabulator | null = null

// [2] 그리드 초기화
const initGrids = () => {
  grid = new Tabulator(tableRef.value!, {
    layout: "fitColumns", height: "100%", placeholder: "데이터 없음",
    columns: [
      { title: "No", formatter: "rownum", width: 40, hozAlign: "center", headerSort: false },
      { title: "대분류 코드", field: "agrpcd", width: 120, hozAlign: "center", cssClass: "fw-bold text-primary" },
      { title: "대분류명", field: "agrpnm", minWidth: 250, widthGrow: 1, cssClass: "fw-bold" },
        { title: "사용", field: "useyn", width: 80, hozAlign: "center",
          formatter: (cell) => {
            const val = String(cell.getValue() || '').trim().toUpperCase();
            return val === 'Y' ? '<b class="text-primary">사용</b>' : '';
          }
        }
    ],
  });
  grid.on("rowClick", (e, row) => fetchDetail(row.getData()));
}

// [3] 로직
const fetchAstOptions = async () => {
  try {
    const res = await api.get('/hp00/HP00_000S_STR', { params: { gubun: 'E0', cmpycd: authStore.cmpycd, gbncd: '100' } });
    astOptions.value = res.data.map((i: any) => ({ code: i.code || i.code, cdnm: i.cdnm }));
    if (astOptions.value.length > 0) formData.astkind = astOptions.value[0].code;
  } catch (e) {}
}

async function search() {
  if (!formData.astkind) return;
  try {
    const res = await api.post('/hpba/HPBA_020U_STR', { actkind: 'S0', cmpycd: authStore.cmpycd, astkind: formData.astkind });
    grid?.setData(res.data);
    vAlert('조회되었습니다.');
  } catch (e) { vAlertError('조회 실패'); }
}

function fetchDetail(row: any) {
  Object.assign(formData, { ...row, actkind: 'U0' });
}

async function save() {
  if (!formData.astkind || !formData.agrpcd || !formData.agrpnm) return vAlertError('필수 항목을 입력하세요.');
  if (!confirm('저장하시겠습니까?')) return

  try {
    await api.post('/hpba/HPBA_020U_STR', {
      ...formData,
      actkind: formData.actkind === 'U0' ? 'U0' : 'A0',
      userid: authStore.userid
    });
    vAlert('처리되었습니다.');
    search();
    resetInputForm();
  } catch (e) { vAlertError('저장 오류'); }
}

const resetInputForm = () => {
  formData.actkind = 'S0';
  formData.agrpcd = ''; formData.agrpnm = ''; formData.useyn = 'Y';
}

const initialize = () => {
  const currentAst = formData.astkind;
  resetForm(formData);
  formData.astkind = currentAst; formData.cmpycd = authStore.cmpycd;
  resetInputForm();
  grid?.clearData();
  search();
}

onMounted(async () => {
  await fetchAstOptions();
  nextTick(() => {
    initGrids();
    search();
  });
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
.grid-container-right { border-bottom: 3px solid #005a9f !important; }
</style>
