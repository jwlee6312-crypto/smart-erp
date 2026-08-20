<!--
	=============================================================
	프로그램명	: 경리전표입력 (HASL110U)
	작성일자	: 2025.02.24
	설명        : 재무 부서용 회계 전표 관리
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-journal-check me-2 text-primary" style="font-size: 18px;"></i>
        전표관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">경리전표입력 (HASL110U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-2">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="searchSlips">조회</button>
        <button class="btn-erp btn-save" @click="save">저장</button>
        <button v-if="masterForm.slipno" class="btn-erp btn-delete" @click="deleteData">삭제</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
                <col style="width: 10%" /><col style="width: 40%" />
                <col style="width: 10%" /><col style="width: 40%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light">발행기간</th>
                <td class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                  <DateForm v-model:fromdt="searchParams.fromdt" v-model:todt="searchParams.todt" />
                </td>
                <th class="text-center bg-light">거래내역</th>
                <td>
                  <input v-model="searchParams.business" class="form-control form-control-sm" placeholder="검색어 입력" @keyup.enter="searchSlips" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="d-flex gap-2 flex-grow-1 overflow-hidden" style="min-height: 0;">

        <div class="card border shadow-sm d-flex flex-column overflow-hidden grid-container-left" style="width: 350px; min-width: 350px;">
          <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark">전표 목록</div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="slipListGridRef" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>

        <div class="flex-grow-1 d-flex flex-column gap-2 overflow-hidden">

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
                    <th class="required bg-light text-center">발행부서</th>
                    <td>
                      <div class="input-group input-group-sm">
                        <input v-model="masterForm.deptnm" class="form-control" readonly />
                        <button class="btn btn-outline-secondary px-2" @click="handleOpenHelp('DEPT')"><i class="bi bi-search"></i></button>
                      </div>
                    </td>
                    <th class="bg-light text-center">전표번호</th>
                    <td>
                      <div class="d-flex gap-1">
                        <input v-model="masterForm.slipymd" type="date" class="form-control" />
                        <input v-model="masterForm.slipno" class="form-control text-center bg-light fw-bold" readonly style="width: 60px;" />
                      </div>
                    </td>
                    <th class="required bg-light text-center">회계일자</th>
                    <td><input v-model="masterForm.acctymd" type="date" class="form-control" /></td>
                  </tr>
                  <tr>
                    <th class="required bg-light text-center border-top">거래내역</th>
                    <td colspan="3" class="border-top">
                        <input v-model="masterForm.business" class="form-control fw-bold text-primary" placeholder="전표 거래내역 입력" @input="syncRemarkToRows" />
                    </td>
                    <th class="bg-light text-center border-top">상태</th>
                    <td class="text-center border-top">
                      <span :class="masterForm.slipgu === '020' ? 'badge bg-success' : 'badge bg-primary'">
                        {{ masterForm.slipgu === '020' ? '미확정' : '확정' }}
                      </span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <div class="flex-grow-1 d-flex gap-2 overflow-hidden">

            <div class="card border shadow-sm flex-grow-1 d-flex flex-column overflow-hidden grid-container-right">
              <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
                <span class="fw-bold small text-dark"><i class="bi bi-list-columns me-2 text-primary"></i>분개 상세</span>
                <button class="btn btn-sm btn-primary py-0 px-2 fw-bold" @click="addRow" style="font-size: 11px;">+ 행추가</button>
              </div>
              <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
                <div ref="mainGridRef" class="tabulator-instance flex-grow-1"></div>
              </div>
              <div class="card-footer p-0 border-top bg-dark text-white">
                <div class="row g-0 text-end fw-bold small py-1 px-3">
                  <div class="col-4 text-center border-end border-secondary">분개 합계</div>
                  <div class="col-2 text-info border-end border-secondary pe-2">{{ formatMoney(totalDebit) }}</div>
                  <div class="col-2 text-warning border-end border-secondary pe-2">{{ formatMoney(totalCredit) }}</div>
                  <div class="col-4" :class="balance === 0 ? 'text-success' : 'text-danger'">차액: {{ formatMoney(balance) }}</div>
                </div>
              </div>
            </div>

            <div class="card border shadow-sm flex-shrink-0 d-flex flex-column bg-white overflow-hidden side-panel-wrapper" style="width: 440px;" v-if="selectedRow">
              <div class="card-header py-2 px-3 bg-secondary text-white small fw-bold d-flex justify-content-between align-items-center">
                 <span><i class="bi bi-gear-fill me-2"></i>상세 관리항목 설정</span>
                 <span class="badge" :class="String(selectedRow.dbcr || '').trim().toUpperCase() === 'D' ? 'bg-primary' : 'bg-danger'">{{ String(selectedRow.dbcr || '').trim().toUpperCase() === 'D' ? '차변' : '대변' }}</span>
              </div>
              <div class="card-body p-0 overflow-auto custom-scrollbar flex-grow-1">

                 <div class="p-2 bg-light border-bottom d-flex align-items-center gap-2 small sticky-top">
                    <span class="badge bg-white text-dark border px-2 py-1">{{ selectedRow.acctcd }}</span>
                    <b class="text-primary">{{ selectedRow.acctnm || '계정 미선택' }}</b>
                 </div>

                 <table class="erp-table-dense side-detail-table w-100 border-0">
                    <colgroup>
                        <col style="width: 100px;" /><col />
                    </colgroup>
                    <tbody>
                        <template v-if="String(selectedRow.typeacct || '').trim() === '100'">
                            <tr>
                                <th class="bg-light border-bottom text-end pe-3 required">유형</th>
                                <td class="border-bottom">
                                    <select v-model="selectedRow.docno3" class="form-select form-select-sm border-0">
                                        <option v-for="opt in entTypeOptions" :key="opt.code" :value="opt.code">{{ opt.name }}</option>
                                    </select>
                                </td>
                            </tr>
                            <tr>
                                <th class="bg-light border-bottom text-end pe-3 required">접대일자</th>
                                <td class="border-bottom">
                                    <input v-model="selectedRow.docno6" type="date" class="form-control form-control-sm border-0" />
                                </td>
                            </tr>
                            <tr>
                                <th class="bg-light border-bottom text-end pe-3">사업번호</th>
                                <td class="border-bottom">
                                    <input v-model="selectedRow.docno2" class="form-control form-control-sm border-0" placeholder="숫자만 입력" />
                                </td>
                            </tr>
                            <tr>
                                <th class="bg-light border-bottom text-end pe-3 text-primary">상호</th>
                                <td class="border-bottom">
                                    <div class="input-group input-group-sm">
                                        <input v-model="selectedRow.docno4" class="form-control border-0" placeholder="상호명 입력" />
                                        <button class="btn btn-outline-secondary border-0 border-start px-2" @click="handleOpenHelp('vat_cust')"><i class="bi bi-search"></i></button>
                                    </div>
                                </td>
                            </tr>
                            <tr v-if="String(selectedRow.docno3 || '').trim() === '01'">
                                <th class="bg-light border-bottom text-end pe-3 text-primary fw-bold">카드</th>
                                <td class="border-bottom">
                                    <div class="input-group input-group-sm">
                                        <input v-model="selectedRow.docno5" class="form-control border-0 text-primary" placeholder="카드 선택" />
                                        <button class="btn btn-outline-secondary border-0 border-start px-2" @click="handleOpenHelp('CARD')"><i class="bi bi-credit-card"></i></button>
                                    </div>
                                </td>
                            </tr>
                            <tr>
                                <th class="bg-light border-bottom text-end pe-3 fw-bold text-primary">순접대비</th>
                                <td class="border-bottom">
                                    <input v-model="selectedRow.docno8" type="number" class="form-control form-control-sm border-0 text-end fw-bold" placeholder="0" />
                                </td>
                            </tr>
                            <tr>
                                <th class="bg-light border-bottom text-end pe-3">봉사료</th>
                                <td class="border-bottom">
                                    <input v-model="selectedRow.docno9" type="number" class="form-control form-control-sm border-0 text-end" placeholder="0" />
                                </td>
                            </tr>
                        </template>

                        <template v-else-if="String(selectedRow.typeacct || '').trim() === '090'">
                            <template v-if="String(selectedRow.typedc || '').trim().toUpperCase() === String(selectedRow.dbcr || '').trim().toUpperCase()">
                                <tr>
                                    <th class="bg-light border-bottom text-end pe-3 required">사업장</th>
                                    <td class="border-bottom">
                                        <select v-model="selectedRow.docno1" class="form-select form-select-sm border-0">
                                            <option value="">-- 선택 --</option>
                                            <option v-for="opt in bizPlaceOptions" :key="opt.code" :value="opt.code">{{ opt.name }}</option>
                                        </select>
                                    </td>
                                </tr>
                                <tr>
                                    <th class="bg-light border-bottom text-end pe-3 required">세무유형</th>
                                    <td class="border-bottom">
                                        <select v-model="selectedRow.docno3" class="form-select form-select-sm border-0">
                                            <option value="">유형 선택</option>
                                            <option v-for="opt in dynamicVatOptions" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                                        </select>
                                    </td>
                                </tr>
                                <tr>
                                    <th class="bg-light border-bottom text-end pe-3 required">거래처</th>
                                    <td class="border-bottom">
                                        <div class="input-group input-group-sm">
                                            <input v-model="selectedRow.docno2nm" class="form-control border-0" readonly placeholder="도움창 선택" />
                                            <button class="btn btn-outline-secondary border-0 border-start px-2" @click="handleOpenHelp('vat_cust')"><i class="bi bi-search"></i></button>
                                        </div>
                                    </td>
                                </tr>
                                <tr>
                                    <th class="bg-light border-bottom text-end pe-3 required">발행일</th>
                                    <td class="border-bottom">
                                        <input v-model="selectedRow.docno6" type="date" class="form-control form-control-sm border-0" />
                                    </td>
                                </tr>
                                <tr>
                                    <th class="bg-light border-bottom text-end pe-3 fw-bold text-primary">공급가</th>
                                    <td class="border-bottom">
                                        <input v-model="selectedRow.docno8" type="number" class="form-control form-control-sm border-0 text-end fw-bold" @input="calcVat" />
                                    </td>
                                </tr>
                                <tr>
                                    <th class="bg-light border-bottom text-end pe-3 fw-bold text-danger">부가세</th>
                                    <td class="border-bottom">
                                        <input v-model="selectedRow.docno9" type="number" class="form-control form-control-sm border-0 text-end fw-bold" />
                                    </td>
                                </tr>
                                <tr v-if="String(selectedRow.dbcr || '').trim().toUpperCase() === 'D'">
                                    <th class="bg-light border-bottom text-end pe-3 text-primary fw-bold">카드</th>
                                    <td class="border-bottom">
                                        <div class="input-group input-group-sm">
                                            <input v-model="selectedRow.docno4" class="form-control border-0 text-primary" placeholder="카드번호 선택" />
                                            <button class="btn btn-outline-secondary border-0 border-start px-2" @click="handleOpenHelp('CARD')"><i class="bi bi-credit-card"></i></button>
                                        </div>
                                    </td>
                                </tr>
                            </template>
                        </template>

                        <template v-else-if="['050', '060', '070'].includes(String(selectedRow.typeacct || '').trim())">
                            <tr>
                                <th class="bg-light border-bottom text-end pe-3 required">어음번호</th>
                                <td class="border-bottom">
                                    <div class="input-group input-group-sm">
                                        <input v-model="selectedRow.mgtno" class="form-control border-0" @keydown.enter="handleOpenHelp('MGT')" placeholder="번호 입력" />
                                        <button class="btn btn-outline-secondary border-0 border-start px-2" @click="handleOpenHelp('MGT')"><i class="bi bi-search"></i></button>
                                    </div>
                                </td>
                            </tr>
                            <tr v-if="(String(selectedRow.typeacct || '').trim() === '050' && String(selectedRow.dbcr || '').trim().toUpperCase() === 'D') || (String(selectedRow.typeacct || '').trim() === '060' && String(selectedRow.dbcr || '').trim().toUpperCase() === 'C')">
                                <th class="bg-light border-bottom text-end pe-3">지급/수취처</th>
                                <td class="border-bottom">
                                    <div class="input-group input-group-sm">
                                        <input v-model="selectedRow.docno9nm" class="form-control border-0" readonly placeholder="도움창 선택" />
                                        <button class="btn btn-outline-secondary border-0 border-start px-2" @click="handleOpenHelp('vat_cust')"><i class="bi bi-search"></i></button>
                                    </div>
                                </td>
                            </tr>
                            <tr v-if="(String(selectedRow.typeacct || '').trim() === '050' && String(selectedRow.dbcr || '').trim().toUpperCase() === 'D') || (String(selectedRow.typeacct || '').trim() === '060' && String(selectedRow.dbcr || '').trim().toUpperCase() === 'C')">
                                <th class="bg-light border-bottom text-end pe-3">만기일자</th>
                                <td class="border-bottom">
                                    <input v-model="selectedRow.docno7" type="date" class="form-control form-control-sm border-0" />
                                </td>
                            </tr>
                        </template>

                        <template v-else>
                            <tr v-if="String(selectedRow.typemgt || '').trim() > '000'">
                                <th class="bg-light border-bottom text-end pe-3 text-primary">
                                    {{
                                        String(selectedRow.typemgt || '').trim() === '010' ? '구좌번호' :
                                        String(selectedRow.typemgt || '').trim() === '020' ? '차입금' :
                                        String(selectedRow.typemgt || '').trim() === '030' ? '유가증권' :
                                        String(selectedRow.typemgt || '').trim() === '040' ? '법인카드' : (selectedRow.titmgt || selectedRow.typemgtnm || '관리항목')
                                    }}
                                </th>
                                <td class="border-bottom">
                                    <div class="input-group input-group-sm">
                                        <input v-model="selectedRow.mgtno" class="form-control border-0" @keydown.enter="handleOpenHelp('MGT')" :placeholder="(String(selectedRow.typemgt || '').trim() >= '010' && String(selectedRow.typemgt || '').trim() <= '040' ? '코드' : '번호') + ' 입력'" />
                                        <button class="btn btn-outline-secondary border-0 border-start px-2" @click="handleOpenHelp('MGT')"><i class="bi bi-search"></i></button>
                                    </div>
                                    <div class="small text-primary mt-1 px-1 fw-bold" v-if="selectedRow.mgtnm">{{ selectedRow.mgtnm }}</div>
                                </td>
                            </tr>
                            <tr v-if="String(selectedRow.typesub || '').trim() > '000' && (String(selectedRow.typemgt || '').trim() <= '000' || String(selectedRow.typemgt || '').trim() === '090')">
                                <th class="bg-light border-bottom text-end pe-3 text-primary">{{ selectedRow.titsub || selectedRow.typesubnm || '거래처' }}</th>
                                <td class="border-bottom">
                                    <div class="input-group input-group-sm">
                                        <input v-model="selectedRow.subnm" class="form-control border-0" readonly placeholder="도움창 선택" />
                                        <button class="btn btn-outline-secondary border-0 border-start px-2" @click="handleOpenHelp('SUB')"><i class="bi bi-search"></i></button>
                                    </div>
                                </td>
                            </tr>
                        </template>

                        <template v-if="String(selectedRow.acctcd || '').trim() > '40000'">
                            <tr>
                                <th class="bg-light border-bottom text-end pe-3">프로젝트</th>
                                <td class="border-bottom">
                                    <div class="input-group input-group-sm">
                                        <input v-model="selectedRow.prjnm" class="form-control border-0" readonly />
                                        <button class="btn btn-outline-secondary border-0 border-start px-2" @click="handleOpenHelp('PRJ')"><i class="bi bi-search"></i></button>
                                    </div>
                                </td>
                            </tr>
                            <tr v-if="String(selectedRow.typebugt || '').trim() > '000' && String(selectedRow.dbcr || '').trim().toLowerCase() === 'd'">
                                <th class="bg-light border-bottom text-end pe-3">예산과목</th>
                                <td class="border-bottom">
                                    <div class="input-group input-group-sm">
                                        <input v-model="selectedRow.bugtnm" class="form-control border-0" readonly />
                                        <button class="btn btn-outline-secondary border-0 border-start px-2" @click="handleOpenHelp('BUGT')"><i class="bi bi-search"></i></button>
                                    </div>
                                </td>
                            </tr>
                        </template>
                    </tbody>
                 </table>
              </div>

              <div class="card-footer bg-light p-2 border-top">
                 <button class="btn btn-xs btn-primary w-100 py-1 fw-bold shadow-sm" @click="updateAutoRemark(true)">
                    <i class="bi bi-magic me-1"></i> 적요 자동 조합
                 </button>
              </div>
            </div>

            <div class="card border shadow-sm flex-shrink-0 d-flex align-items-center justify-content-center bg-white" style="width: 440px;" v-else>
               <div class="text-center opacity-50">
                  <i class="bi bi-cursor-fill mb-2" style="font-size: 2rem;"></i>
                  <div class="small">분개 행을 선택하면<br/>상세 입력창이 나타납니다.</div>
               </div>
            </div>

          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, nextTick, watch } from 'vue'
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
import DateForm from '@/components/DateForm.vue'

const authStore = useAuthStore()
const { firstDay, today } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps, openHelp } = useCommonHelp()

const searchParams = reactive({ fromdt: firstDay, todt: today, business: '' })
const masterForm = reactive<any>({
  cmpycd: authStore.cmpycd, deptcd: authStore.deptcd, deptnm: authStore.deptnm,
  slipymd: today, slipno: '', empnm: authStore.usernm, business: '',
  acctymd: today, slipgu: '020'
})

const slipListGridRef = ref<HTMLElement | null>(null)
const mainGridRef = ref<HTMLElement | null>(null)
let grid1: Tabulator | null = null
let grid2: Tabulator | null = null
const selectedRow = ref<any>(null)
const activeRowComponent = ref<any>(null)

const purchaseVatOptions = ref<any[]>([]); const salesVatOptions = ref<any[]>([])
const bizPlaceOptions = ref<any[]>([]);
const entTypeOptions = [{code:'01', name:'카드'}, {code:'02', name:'현금'}, {code:'03', name:'세금계산서'}]

const dynamicVatOptions = computed(() => {
  if (!selectedRow.value) return []
  return (selectedRow.value.dbcr||'').toLowerCase() === 'd' ? purchaseVatOptions.value : salesVatOptions.value
})

const totalDebit = ref(0); const totalCredit = ref(0)
const balance = computed(() => totalDebit.value - totalCredit.value)
const formatMoney = (val: any) => Number(val || 0).toLocaleString()

const updateRowStatus = (row: any) => {
    const d = row.getData();
    if (d._state === 'EXIST' && !d._status) row.update({ _status: '수정' });
}

watch(selectedRow, (newVal) => {
    if (newVal && activeRowComponent.value) {
        activeRowComponent.value.update(newVal);
        updateRowStatus(activeRowComponent.value);
        updateAutoRemark();
    }
}, { deep: true });

function updateAutoRemark(force = false) {
    const row = selectedRow.value; if (!row) return;
    let prefix = "";
    if (row.subnm) prefix = `[${row.subnm}] `;
    else if (row.docno4) prefix = `[${row.docno4}] `;
    else if (row.mgtnm) prefix = `[${row.mgtnm}] `;
    else if (row.acctnm) prefix = `[${row.acctnm}] `;
    if (force || !row.remark || row.remark.startsWith('[') || row.remark === masterForm.business) {
        row.remark = prefix + (masterForm.business || "");
    }
}

function syncRemarkToRows() {
    grid2?.getRows().forEach(row => {
        const d = row.getData();
        if (!d.remark || d.remark.startsWith('[') || d.remark === "") {
            let prefix = d.subnm ? `[${d.subnm}] ` : (d.mgtnm ? `[${d.mgtnm}] ` : (d.acctnm ? `[${d.acctnm}] ` : ""));
            row.update({ remark: prefix + masterForm.business });
        }
    });
}

const initGrids = () => {
  grid1 = new Tabulator(slipListGridRef.value!, {
    layout: "fitColumns", height: "100%", placeholder: "데이터 없음",
    columns: [
      { title: "No", formatter: "rownum", width: 40, hozAlign: "center" },
      { title: "발행일", field: "slipymd", width: 95, hozAlign: "center", formatter: (c) => {
          const v = c.getValue() || ''; return v.length === 8 ? `${v.substring(0,4)}-${v.substring(4,6)}-${v.substring(6,8)}` : v;
      }},
      { title: "거래내역", field: "business", hozAlign: "left", headerSort: false }
    ],
  });
  grid1.on("rowClick", (e, row) => fetchDetail(row.getData()));

  grid2 = new Tabulator(mainGridRef.value!, {
    layout: "fitColumns", height: "100%", selectable: 1,
    columnDefaults: {
        headerHozAlign: 'center', headerSort: false, vertAlign: "middle",
        cellClick: (e, cell) => {
            cell.getRow().select();
            selectedRow.value = cell.getRow().getData();
            activeRowComponent.value = cell.getRow();
        }
    },
    columns: [
      { title: "No", field: "srowno", width: 40, hozAlign: "center" },
      { title: "상태", field: "_status", width: 50, hozAlign: "center", formatter: (c) => {
          const v = c.getValue() || '';
          if (v === '입력') return '<span class="badge bg-primary">신규</span>';
          if (v === '수정') return '<span class="badge bg-warning text-dark">수정</span>';
          if (v === '삭제') return '<span class="badge bg-danger">삭제</span>';
          return '';
      }},
      { title: "차/대", field: "dbcr", width: 70, hozAlign: "center", editor: "list", editorParams: { values: { "d": "차변", "c": "대변" } },
        formatter: (c) => {
            const v = String(c.getValue() || '').trim().toLowerCase();
            return v === 'd' ? '<b class="text-primary">차변</b>' : (v === 'c' ? '<b class="text-danger">대변</b>' : v);
        }
      },
      { title: "계정과목", field: "acctnm", width: 160, cellClick: (e, cell) => handleOpenHelp('ACCT', cell.getRow()), cssClass: "cursor-pointer text-primary fw-bold" },
      { title: "적요", field: "remark", minWidth: 200, editor: "input" },
      { title: "금액", field: "amount", width: 110, hozAlign: "right", editor: "number", formatter: "money", formatterParams: { precision: 0 } },
      { title: "삭제", width: 40, hozAlign: "center", formatter: () => '<i class="bi bi-trash text-danger"></i>', cellClick: (e, cell) => handleRowAction(cell.getRow()) }
    ]
  });
  grid2.on("cellEdited", (cell) => {
    selectedRow.value = cell.getRow().getData();
    activeRowComponent.value = cell.getRow();
    updateRowStatus(activeRowComponent.value);
    updateTotals();
  });
}

const updateTotals = () => {
  const data = grid2?.getData() || []
  totalDebit.value = data.reduce((s, r: any) => s + (String(r.dbcr || '').trim().toLowerCase() === 'd' ? Number(r.amount || 0) : 0), 0)
  totalCredit.value = data.reduce((s, r: any) => s + (String(r.dbcr || '').trim().toLowerCase() === 'c' ? Number(r.amount || 0) : 0), 0)
}

async function searchSlips() {
  try {
    const res = await api.post('/hasl/HASL_110U_STR', { actkind: 'F', cmpycd: authStore.cmpycd, fromdt: searchParams.fromdt.replace(/-/g, ''), todt: searchParams.todt.replace(/-/g, ''), keyword: searchParams.business });
    grid1?.setData(res.data);
  } catch (e) { vAlertError('조회 실패'); }
}

async function fetchDetail(row: any) {
  const master = row;
  Object.assign(masterForm, master);
  if (masterForm.slipymd && masterForm.slipymd.length === 8) {
    const d = masterForm.slipymd; masterForm.slipymd = `${d.substring(0,4)}-${d.substring(4,6)}-${d.substring(6,8)}`;
  }
  if (masterForm.acctymd && masterForm.acctymd.length === 8) {
    const d = masterForm.acctymd; masterForm.acctymd = `${d.substring(0,4)}-${d.substring(4,6)}-${d.substring(6,8)}`;
  }
  try {
    const resD = await api.post('/hasl/HASL_111U_STR', { actkind: 'S', cmpycd: authStore.cmpycd, slipymd: String(masterForm.slipymd).replace(/-/g,''), slipno: masterForm.slipno });
    grid2?.setData(resD.data || []); updateTotals();
  } catch (e) { vAlertError('상세 조회 실패'); }
}

function handleOpenHelp(type: string, target?: any) {
  if (type === 'DEPT') openHelp('DEPT', (d) => { masterForm.deptcd = d.deptcd; masterForm.deptnm = d.deptnm });
  else if (type === 'ACCT') {
    openHelp('ACCT', (res: any) => {
        target.update({ acctcd: res.acctcd, acctnm: res.acctnm, typeacct: res.typeacct, typemgt: res.typemgt, typesub: res.typesub, typedc: res.typedc, typebugt: res.bugtcd });
        selectedRow.value = target.getData();
        activeRowComponent.value = target;
    });
  } else if (type === 'MGT' || type === 'CARD') {
      const mgtgbn = type === 'CARD' ? '040' : selectedRow.value.typemgt;
      openHelp('MGT', (res: any) => {
          if (type === 'CARD') selectedRow.value.docno4 = res.mgtno;
          else { selectedRow.value.mgtno = res.mgtno; selectedRow.value.mgtnm = res.mgtnm; }
      }, { acctcd: selectedRow.value.acctcd, mgtgbn: mgtgbn });
  } else if (type === 'SUB') {
      openHelp('CUST', (res: any) => { selectedRow.value.subcd = res.custcd; selectedRow.value.subnm = res.custnm; });
  } else if (type === 'vat_cust') {
      openHelp('CUST', (res: any) => { selectedRow.value.docno2 = res.busino || res.custcd; selectedRow.value.docno2nm = res.custnm; });
  } else if (type === 'PRJ') {
      openHelp('PRJ', (d) => { selectedRow.value.prjcd = d.prjcd; selectedRow.value.prjnm = d.prjnm });
  } else if (type === 'BUGT') {
      openHelp('BUGT', (d) => { selectedRow.value.bugtcd = d.bugtcd; selectedRow.value.bugtnm = d.bugtnm }, { acctcd: selectedRow.value.acctcd });
  }
}

const calcVat = () => { if (String(selectedRow.value?.typeacct || '').trim() === '090') selectedRow.value.docno9 = Math.floor(Number(selectedRow.value.docno8 || 0) * 0.1); }

async function save() {
  if (balance.value !== 0) return vAlertError('차/대변 금액이 일치하지 않습니다.');
  if (!masterForm.business) return vAlertError('거래내역을 입력하십시오.');
  const allData = grid2?.getData() || [];
  try {
    const payload = {
      actkind: masterForm.slipno ? 'U' : 'A',
      master: { ...masterForm, slipymd: masterForm.slipymd.replace(/-/g, ''), acctymd: masterForm.acctymd.replace(/-/g, '') },
      details: allData.map(d => {
        const item = { ...d };
        ['docno6', 'docno7', 'payymd'].forEach(key => { if (item[key]) item[key] = item[key].replace(/-/g, ''); });
        return {
            ...item,
            custcd: item.subcd,
            dbamt: (item.dbcr || '').toLowerCase() === 'd' ? (item.amount || 0) : 0,
            cramt: (item.dbcr || '').toLowerCase() === 'c' ? (item.amount || 0) : 0,
            upkind: item._status === '입력' ? 'A' : (item._status === '삭제' ? 'D' : 'U')
        };
      })
    };
    const res = await api.post('/hasl/HASL_110U_SAVE', payload);
    if (res.data?.[0]?.result === 'N') return vAlertError(res.data[0].msg || '저장 실패');
    vAlert('저장되었습니다.'); searchSlips();
  } catch (e) { vAlertError('저장 실패'); }
}

const initialize = () => {
    resetForm(masterForm);
    Object.assign(masterForm, { cmpycd: authStore.cmpycd, deptcd: authStore.deptcd, deptnm: authStore.deptnm, slipymd: today, acctymd: today, empnm: authStore.usernm, slipgu: '020' });
    grid2?.clearData(); selectedRow.value = null; totalDebit.value = 0; totalCredit.value = 0;
}

const addRow = () => {
    const data = grid2?.getData() || [];
    const lastRow = data.length > 0 ? data[data.length - 1] : null;
    const newDbcr = lastRow ? (String(lastRow.dbcr).toLowerCase() === 'd' ? 'c' : 'd') : 'd';
    grid2?.addRow({ dbcr: newDbcr, remark: masterForm.business, amount: 0, srowno: data.length + 1, _status: '입력', _state: 'NEW' }, false).then(row => {
        nextTick(() => { row.select(); });
    });
}

const handleRowAction = (row: any) => {
    const d = row.getData();
    if (d._state === 'NEW') row.delete();
    else { row.update({ _status: d._status === '삭제' ? '' : '삭제' }); }
}

const deleteData = async () => {
    if (!confirm('삭제하시겠습니까?')) return;
    try {
        const res = await api.post('/hasl/HASL_110U_SAVE', { actkind: 'D', master: { ...masterForm, slipymd: masterForm.slipymd.replace(/-/g, '') } });
        if (res.data?.[0]?.result === 'N') return vAlertError(res.data[0].msg || '삭제 실패');
        vAlert('삭제되었습니다.'); initialize(); searchSlips();
    } catch (e) { vAlertError('삭제 실패'); }
}

onMounted(() => {
  api.post('/ha00/HA00_00P_STR', { gubun: 'E0', gbncd: '120' }).then(r => purchaseVatOptions.value = r.data);
  api.post('/ha00/HA00_00P_STR', { gubun: 'E0', gbncd: '130' }).then(r => salesVatOptions.value = r.data);
  api.post('/ha00/HA00_00P_STR', { gubun: 'SA', cmpycd: authStore.cmpycd }).then(r => bizPlaceOptions.value = (r.data || []).map(n => { return { code: n.taxunit || n.code || '', name: n.unitnm || n.codenm || '' }; }));
  initGrids(); searchSlips();
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
.side-detail-table th { background-color: #f8f9fa; padding: 2px 10px !important; font-size: 12px; font-weight: 600; color: #555; border: 1px solid #eee; text-align: right; height: 25px; white-space: nowrap; }
.side-detail-table td { padding: 1px 6px !important; border: 1px solid #eee; vertical-align: middle; }
</style>
