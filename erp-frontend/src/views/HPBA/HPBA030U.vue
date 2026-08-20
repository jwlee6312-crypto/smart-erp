<!--
	=============================================================
	프로그램명	: 자재 중분류 관리 (HPBA030U)
	작성일자	: 2025.02.24
	설명        : 자재 대분류별 중분류 체계 등록 및 사용 여부 관리 (HPIO210U 표준 패턴 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-folder2-open me-2 text-primary" style="font-size: 18px;"></i>
        기본정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">자재 중분류 관리 (HPBA030U)</span>
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
                <th class="text-center bg-light required">중분류코드</th>
                <td>
                  <input v-model="formData.bgrpcd" class="form-control text-center fw-bold" maxlength="3" :readonly="formData.actkind === 'U0'" placeholder="3자리" />
                </td>
                <th class="text-center bg-light required">중분류명</th>
                <td>
                  <input v-model="formData.bgrpnm" class="form-control" placeholder="분류명 입력" />
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

      <!-- [하단] 투-그리드 레이아웃 영역 -->
      <div class="d-flex gap-2 flex-grow-1 overflow-hidden" style="min-height: 0;">

        <!-- ⬅️ 좌측: 대분류 목록 (grid1) -->
        <div class="card border shadow-sm d-flex flex-column overflow-hidden grid-container-left" style="width: 300px; min-width: 300px;">
          <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark text-center">대분류 목록</div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="tableRef1" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>

        <!-- ➡️ 우측: 상세 중분류 그리드 (grid2) -->
        <div class="flex-grow-1 d-flex flex-column gap-2 overflow-hidden">
          <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column grid-container-right">
            <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
              <span class="fw-bold small text-dark">
                <i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i>상세 중분류 리스트
                <span v-if="selectedLarge.cdnm" class="badge bg-primary-subtle text-primary border border-primary-subtle ms-2">{{ selectedLarge.cdnm }}</span>
              </span>
              <span class="text-muted small">목록 클릭 시 수정 모드로 전환</span>
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
import { reactive, ref, onMounted, nextTick } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { useCommonHelp } from '@/composables/useCommonHelp'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps, openHelp } = useCommonHelp()

// [1] 데이터 모델링
const formData = reactive<any>({
  actkind: 'S0', cmpycd: authStore.cmpycd,
  astkind: '', agrpcd: '', bgrpcd: '', bgrpnm: '', useyn: 'Y'
})

const astOptions = ref<any[]>([])
const selectedLarge = reactive({ code: '', cdnm: '' })

const tableRef1 = ref<HTMLDivElement | null>(null)
const tableRef2 = ref<HTMLDivElement | null>(null)
let grid1: Tabulator | null = null
let grid2: Tabulator | null = null

// [2] 그리드 초기화
const initGrids = () => {
  // Left: 대분류
  grid1 = new Tabulator(tableRef1.value!, {
    layout: "fitColumns", height: "100%", placeholder: "데이터 없음", selectable: 1,
    columns: [
      { title: "No", formatter: "rownum", width: 40, hozAlign: "center", headerSort: false },
      { title: "대분류명", field: "cdnm", hozAlign: "left", cssClass: "fw-bold text-primary" },
      { title: "코드", field: "code", width: 70, hozAlign: "center" }
    ],
  });
  grid1.on("rowClick", (e, row) => onLargeGroupSelect(row.getData()));

  // Right: 중분류
  grid2 = new Tabulator(tableRef2.value!, {
    layout: "fitColumns", height: "100%", placeholder: "대분류를 선택하세요.", selectable: 1,
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { title: "No", formatter: "rownum", width: 40, hozAlign: "center" },
      { title: "중분류 코드", field: "bgrpcd", width: 120, hozAlign: "center", cssClass: "fw-bold text-primary" },
      { title: "중분류명", field: "bgrpnm", minWidth: 250, widthGrow: 1, cssClass: "fw-bold" },
      { title: "사용", field: "useyn", width: 80, hozAlign: "center",
        formatter: (cell) => {
          const val = String(cell.getValue() || '').trim().toUpperCase();
          return val === 'Y' ? '<b class="text-primary">사용</b>' : '';
        }
      }
    ],
  });
  grid2.on("rowClick", (e, row) => fetchDetail(row.getData()));
}

// [3] 로직
const fetchAstOptions = async () => {
  try {
    const res = await api.get('/hp00/HP00_000S_STR', { params: { gubun: 'E0', cmpycd: authStore.cmpycd, gbncd: '100' } });
    astOptions.value = res.data;
    if (astOptions.value.length > 0) formData.astkind = astOptions.value[0].code;
  } catch (e) {}
}

async function search() {
  if (!formData.astkind) return;
  try {
    const res = await api.post('/hpba/HPBA_030U_STR', { actkind: 'S1', cmpycd: authStore.cmpycd, astkind: formData.astkind });
    grid1?.setData(res.data);
    grid2?.clearData();
    selectedLarge.code = ''; selectedLarge.cdnm = '';
  } catch (e) { vAlertError('조회 실패'); }
}

const onLargeGroupSelect = (row: any) => {
  selectedLarge.code = row.code;
  selectedLarge.cdnm = row.cdnm;
  formData.agrpcd = row.code;
  fetchMiddleGroups();
  resetInputForm();
}

async function fetchMiddleGroups() {
  try {
    const res = await api.post('/hpba/HPBA_030U_STR', { actkind: 'S0', cmpycd: authStore.cmpycd, astkind: formData.astkind, agrpcd: selectedLarge.code });
    grid2?.setData(res.data);
  } catch (e) {}
}

function fetchDetail(row: any) {
  Object.assign(formData, { ...row, actkind: 'U0' });
}

async function save() {
  if (!formData.astkind || !formData.agrpcd || !formData.bgrpcd || !formData.bgrpnm) return vAlertError('필수 항목을 입력하세요.');
  if (!confirm('저장하시겠습니까?')) return

  try {
    await api.post('/hpba/HPBA_030U_STR', {
      ...formData,
      actkind: formData.actkind === 'U0' ? 'U0' : 'A0',
      userid: authStore.userid
    });
    vAlert('처리되었습니다.');
    fetchMiddleGroups();
    resetInputForm();
  } catch (e) { vAlertError('저장 오류'); }
}

const resetInputForm = () => {
  formData.actkind = 'S0';
  formData.bgrpcd = ''; formData.bgrpnm = ''; formData.useyn = 'Y';
}

const initialize = () => {
  const currentAst = formData.astkind;
  resetForm(formData);
  formData.astkind = currentAst; formData.cmpycd = authStore.cmpycd;
  resetInputForm();
  grid1?.clearData(); grid2?.clearData();
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
.grid-container-left, .grid-container-right { border-bottom: 3px solid #005a9f !important; }
</style>
