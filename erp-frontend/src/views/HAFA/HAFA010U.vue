<!--
	=============================================================
	프로그램명	: 자산이력관리 (HAFA010U)
	작성일자	: 2025.02.24
	설명        : 고정자산 취득, 변동, 처분 이력 관리 (금액 콤마 및 팝업 자동 매핑 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-pci-card me-2 text-primary" style="font-size: 18px;"></i>
        고정자산 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        이력관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">자산이력관리 (HAFA010U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-save" @click="save">저장</button>
        <button class="btn-erp btn-delete" @click="deleteData" :disabled="formData.actkind !== 'U'">삭제</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 조회 필터 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
              <col style="width: 10%" /><col style="width: 40%" />
              <col style="width: 10%" /><col style="width: 40%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light">계정과목</th>
                <td>
                  <div class="input-group input-group-sm" style="width: 300px;">
                    <input v-model="searchForm.acctcd" type="text" class="form-control text-center bg-light fw-bold" style="max-width: 65px;" readonly />
                    <input v-model="searchForm.acctnm" type="text" class="form-control" @keydown.enter="openHelp('ACCT', 'search')" placeholder="계정 선택" />
                    <button class="btn btn-outline-secondary" @click="openHelp('ACCT', 'search')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="text-center bg-light">발생일자</th>
                <td>
                  <div class="d-flex align-items-center gap-1">
                    <input v-model="searchForm.fromdt" type="date" class="form-control form-control-sm" style="width: 140px;" />
                    <span class="text-muted small fw-bold">~</span>
                    <input v-model="searchForm.todt" type="date" class="form-control form-control-sm" style="width: 140px;" />
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [상세] 정보 입력 창 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
          <div class="fw-bold small text-dark"><i class="bi bi-pencil-square me-2 text-primary"></i>자산 상세 정보 [{{ formData.actkind === 'A' ? '신규' : '수정' }}]</div>
          <div class="d-flex align-items-center gap-3">
            <div v-if="formData.actkind === 'U'" class="badge bg-primary-subtle text-primary border border-primary-subtle px-2" style="font-size: 10px;">수정모드</div>
            <div class="form-check form-switch m-0">
              <input v-model="formData.useyn" class="form-check-input" type="checkbox" id="useynCheck010" true-value="N" false-value="Y">
              <label class="form-check-label text-danger fw-bold small" for="useynCheck010">폐기</label>
            </div>
          </div>
        </div>
        <div class="card-body p-0 bg-white">
          <table class="erp-table-full">
            <colgroup>
              <col style="width: 110px;" /><col />
              <col style="width: 110px;" /><col />
              <col style="width: 110px;" /><col />
            </colgroup>
            <tbody>
              <tr>
                <th class="required bg-light">처리구분</th>
                <td>
                  <select v-model="formData.proctype" class="form-select form-select-sm">
                    <option v-for="item in proctypeOptions" :key="item.codecd" :value="item.codecd">{{ item.codenm }}</option>
                  </select>
                </td>
                <th class="bg-light">자산코드</th>
                <td>
                  <div class="input-group input-group-sm">
                    <input v-model="formData.asetcd" type="text" class="form-control text-center bg-light fw-bold" readonly />
                    <button class="btn btn-outline-secondary" @click="openHelp('ASET')" :disabled="formData.proctype === '010' || !formData.acctcd">
                      <i class="bi bi-search"></i>
                    </button>
                  </div>
                </td>
                <th class="required bg-light">자산명</th>
                <td><input v-model="formData.asetnm" type="text" class="form-control form-control-sm" placeholder="자산 명칭" /></td>
              </tr>
              <tr>
                <th class="required bg-light">계정과목</th>
                <td>
                  <div class="input-group input-group-sm">
                    <input v-model="formData.acctcd" type="text" class="form-control text-center bg-light fw-bold" style="max-width: 65px;" readonly />
                    <input v-model="formData.acctnm" type="text" class="form-control" @keydown.enter="openHelp('ACCT', 'form')" />
                    <button class="btn btn-outline-secondary" @click="openHelp('ACCT', 'form')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="required bg-light">발생일</th>
                <td><input v-model="formData.procymd" type="date" class="form-control form-control-sm" /></td>
                <th class="required bg-light">수량/금액</th>
                <td>
                  <div class="d-flex gap-1 align-items-center">
                    <input v-model="formData.procqty" type="number" class="form-control form-control-sm text-end" style="width: 70px;" />
                    <input v-model="formattedProcamt" type="text" class="form-control form-control-sm text-end fw-bold text-primary" placeholder="0" @input="e => (e.target as HTMLInputElement).value = (e.target as HTMLInputElement).value.replace(/[^0-9,]/g, '')" />
                  </div>
                </td>
              </tr>
              <tr>
                <th class="required bg-light">상각방법</th>
                <td>
                  <select v-model="formData.dprstype" class="form-select form-select-sm">
                    <option v-for="item in dprstypeOptions" :key="item.codecd" :value="item.codecd">{{ item.codenm }}</option>
                  </select>
                </td>
                <th class="required bg-light">내용연수</th>
                <td>
                  <div class="input-group input-group-sm">
                    <input v-model="formData.legalyy" type="number" class="form-control form-control-sm text-end" />
                    <button class="btn btn-outline-secondary" @click="openHelp('rate')"><i class="bi bi-list-ul"></i></button>
                  </div>
                </td>
                <th class="required bg-light">상각율</th>
                <td>
                  <div class="d-flex gap-1 align-items-center px-1">
                    <input v-model="formData.asetrate" type="number" step="0.001" class="form-control form-control-sm text-end fw-bold" style="width: 100px;" />
                    <span class="small text-muted ms-1">가감</span>
                    <input v-model="formData.gagamyy" type="number" class="form-control form-control-sm text-end" style="width: 50px;" />
                  </div>
                </td>
              </tr>
              <tr>
                <th class="required bg-light">비용구분</th>
                <td>
                  <select v-model="formData.costtype" class="form-select form-select-sm">
                    <option v-for="item in costtypeOptions" :key="item.codecd" :value="item.codecd">{{ item.codenm }}</option>
                  </select>
                </td>
                <th class="required bg-light">보유부서</th>
                <td>
                  <div class="input-group input-group-sm">
                    <input v-model="formData.deptcd" type="text" class="form-control text-center bg-light fw-bold" style="max-width: 65px;" readonly />
                    <input v-model="formData.deptnm" type="text" class="form-control" @keydown.enter="openHelp('DEPT')" />
                    <button class="btn btn-outline-secondary" @click="openHelp('DEPT')"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="bg-light">비고</th>
                <td><input v-model="formData.remark" type="text" class="form-control form-control-sm" placeholder="참조 사항 입력" /></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 그리드 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column bg-white">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
          <div class="fw-bold small text-dark"><i class="bi bi-list-ul me-2 text-primary"></i>자산 변동이력 리스트</div>
          <span class="text-muted small" style="font-size: 11px;">항목 클릭 시 수정 모드로 전환됩니다.</span>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="mainGridRef" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>
    </div>
  </div>

  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import AppAlert from '@/components/AppAlert.vue'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import Modal from '@/components/Modal.vue'
import type { ModalProps } from '@/types/modal'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()

const today = new Date().toISOString().slice(0, 10)
const firstDay = today.slice(0, 8) + '01'

const searchForm = reactive({ acctcd: '', acctnm: '', fromdt: firstDay, todt: today })
const formData = reactive({ actkind: 'A', ym: '', rowno: '', proctype: '010', asetcd: '', asetnm: '', acctcd: '', acctnm: '', procymd: today, procqty: 0, procamt: 0, dprstype: '010', legalyy: 0, gagamyy: 0, asetrate: 0, costtype: '010', deptcd: '', deptnm: '', remark: '', useyn: 'Y' })

const proctypeOptions = ref<any[]>([]); const dprstypeOptions = ref<any[]>([]); const costtypeOptions = ref<any[]>([])
const mainGridRef = ref<HTMLDivElement | null>(null); let mainGrid: Tabulator | null = null

// 공통 코드 표준 매핑 (codecd, codenm)
const mapToStandard = (data: any[]) => {
  return (data || []).map((i: any) => ({
    codecd: i.codecd || i.code || '',
    codenm: i.codenm || i.cdnm || ''
  }));
};

// 금액 천단위 콤마 포맷팅 Computed
const formattedProcamt = computed({
  get: () => {
    if (formData.procamt === undefined || formData.procamt === null || formData.procamt === '') return '';
    return formData.procamt.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
  },
  set: (val: string) => {
    const num = Number(val.replace(/,/g, ''));
    formData.procamt = isNaN(num) ? 0 : num;
  }
});

function initialize() {
	Object.assign(formData, { actkind: 'A', ym: '', rowno: '', proctype: '010', asetcd: '', asetnm: '', acctcd: searchForm.acctcd, acctnm: searchForm.acctnm, procymd: today, procqty: 0, procamt: 0, dprstype: '010', legalyy: 0, gagamyy: 0, asetrate: 0, costtype: '010', deptcd: '', deptnm: '', remark: '', useyn: 'Y' })
}

const fetchOptions = async () => {
	try {
		const [resP, resD, resC] = await Promise.all([
			api.post('/ha00/HA00_00P_STR', { gubun: 'E0', cmpycd: ' ', gbncd: '260' }),
			api.post('/ha00/HA00_00P_STR', { gubun: 'E0', cmpycd: ' ', gbncd: '250' }),
			api.post('/ha00/HA00_00P_STR', { gubun: 'E0', cmpycd: ' ', gbncd: '110' })
		]);
		proctypeOptions.value = mapToStandard(resP.data);
		dprstypeOptions.value = mapToStandard(resD.data);
		costtypeOptions.value = mapToStandard(resC.data);
	} catch (e) { console.error('코드 로드 실패:', e) }
}

async function search() {
	if (!searchForm.acctcd) return vAlertError('계정과목을 선택하세요.')
	try {
		const res = await api.post('/hafa/HAFA_010U_STR', { actkind: 'S', cmpycd: authStore.cmpycd, fromdt: searchForm.fromdt.replace(/-/g, ''), todt: searchForm.todt.replace(/-/g, ''), acctcd: searchForm.acctcd })
    const data = (res.data || []).map((r: any) => ({
      ...r,
      procymd: r.procymd ? `${r.procymd.slice(0,4)}-${r.procymd.slice(4,6)}-${r.procymd.slice(6,8)}` : ''
    }));
		mainGrid?.setData(data);
    vAlert('조회되었습니다.');
    initialize();
	} catch (e) { vAlertError('조회 중 오류 발생') }
}

async function save() {
	if (!formData.acctcd) return vAlertError('계정과목을 선택하세요.')
	if (formData.proctype === '010' && !formData.asetnm) return vAlertError('자산명을 입력하세요.')
	if (formData.proctype !== '010' && !formData.asetcd) return vAlertError('자산코드를 선택하세요.')
	try {
		const res = await api.post('/hafa/HAFA_010U_STR', { ...formData, cmpycd: authStore.cmpycd, procymd: formData.procymd.replace(/-/g, ''), userid: authStore.userid })
		if (res.data && res.data[0]?.col0 === 'Y') { vAlertError(res.data[0].col1) } else { vAlert('저장되었습니다.'); search() }
	} catch (e) { vAlertError('저장 중 오류 발생') }
}

async function deleteData() {
	if (!confirm('정말 삭제하시겠습니까?')) return
	try {
		await api.post('/hafa/HAFA_010U_STR', { ...formData, actkind: 'D', cmpycd: authStore.cmpycd, userid: authStore.userid })
		vAlert('삭제되었습니다.'); search()
	} catch (e) { vAlertError('삭제 중 오류 발생') }
}

const modalVisible = ref(false); const modalProps = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })
function openHelp(type: string, target?: string) {
	if (type === 'ACCT') {
		Object.assign(modalProps, { title: '계정과목 선택', path: '/ha00/HA00_00P_STR', data: { gubun: 'A8', cmpycd: authStore.cmpycd, gbncd: '020', code: '' }, columns: [{ title: '코드', field: 'acctcd', width: 80 }, { title: '계정명', field: 'acctnm', width: 200 }],
			onConfirm: (rawData: any) => {
        const d = rawData;
        if (target === 'search') { searchForm.acctcd = d.acctcd; searchForm.acctnm = d.acctnm; search() } else { formData.acctcd = d.acctcd; formData.acctnm = d.acctnm }
      }
		})
	} else if (type === 'ASET') {
		if (!formData.acctcd) {
			vAlertError('계정과목을 먼저 선택해 주세요.');
			return;
		}
		Object.assign(modalProps, { title: '자산 선택', path: '/ha00/HA00_00P_STR', data: { gubun: 'J0', cmpycd: authStore.cmpycd, gbncd: formData.acctcd, code: formData.asetnm }, columns: [{ title: '코드', field: 'asetcd', width: 100 }, { title: '자산명', field: 'asetnm', width: 200 }],
			onConfirm: (rawData: any) => {
        const d = rawData;
        Object.assign(formData, {
          asetcd: d.asetcd,
          asetnm: d.asetnm,
          legalyy: d.legalyy,
          deptcd: d.deptcd,
          deptnm: d.deptnm,
          costtype: d.costtype,
          dprstype: d.dprstype,
          asetrate: d.asetrate,
          procqty: d.pchqty,
          procamt: d.pchamt
        })
      }
		})
	} else if (type === 'DEPT') {
		Object.assign(modalProps, { title: '부서 선택', path: '/ha00/HA00_00P_STR', data: { gubun: 'D0', cmpycd: authStore.cmpycd, code: formData.deptnm }, columns: [{ title: '코드', field: 'deptcd', width: 80 }, { title: '부서명', field: 'deptnm', width: 180 }],
			onConfirm: (rawData: any) => {
        const d = rawData;
        formData.deptcd = d.deptcd; formData.deptnm = d.deptnm;
      }
		})
	} else if (type === 'rate') {
		Object.assign(modalProps, { title: '상각율 선택', path: '/ha00/HA00_00P_STR', data: { gubun: 'J1', cmpycd: authStore.cmpycd, gbncd: formData.dprstype }, columns: [{ title: '내용연수', field: 'legalyy', width: 100 }, { title: '상각율', field: 'asetrate', width: 100 }],
			onConfirm: (rawData: any) => {
        const d = rawData;
        formData.legalyy = d.legalyy; formData.asetrate = d.rate95;
      }
		})
	}
	modalVisible.value = true
}

onMounted(() => {
	fetchOptions()
	if (mainGridRef.value) {
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns', height: '100%', columnDefaults: { headerSort: false, vertAlign: "middle" },
			columns: [
				{ title: "발생일", field: "procymd", width: 120, hozAlign: "center" },
				{ title: "구분", field: "procnm", width: 100, hozAlign: "center", cssClass: "text-primary fw-bold" },
				{ title: "자산명", field: "asetnm", minWidth: 200, cssClass: "fw-bold" },
				{ title: "금액", field: "procamt", width: 150, hozAlign: "right", formatter: "money", formatterParams: { precision: 0 } },
				{ title: "상각방법", field: "dprstypenm", width: 150, hozAlign: "center" },
				{ title: "내용연수", field: "legalyy", width: 150, hozAlign: "center" },
				{ title: "상각율", field: "asetrate", width: 150, hozAlign: "center" },
				{ title: "보유부서", field: "deptnm", width: 200 },
                { title: "상태", field: "useyn", width: 80, hozAlign: "center",
                  formatter: (cell) => {
                    const val = String(cell.getValue() || '').trim().toUpperCase();
                    return val === 'Y' ? '<b class="text-primary">사용</b>' : '<span class="text-danger">폐기</span>';
                  }
                }
			]
		})
		mainGrid.on("rowClick", (e, row) => {
      const d = row.getData();
      Object.assign(formData, d);
      formData.actkind = 'U'
    })
	}
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
</style>
