<!--
	=============================================================
	프로그램명	: 현업전표등록 (HASL010U)
	작성일자	: 2025.02.24
	설명        : 실무 부서용 회계 전표 관리
	=============================================================
-->

<template>
  <AppAlert :show="showalert" :error="showerror" :message="alertmessage" />
  <Modal v-model:visible="modalvisible" :modalProps="modalprops" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-journal-plus me-2 text-primary" style="font-size: 18px;"></i>
        전표관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">현업전표등록 (HASL010U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-2">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="searchslips">조회</button>
        <button class="btn-erp btn-save" @click="save">저장</button>
        <button v-if="masterform.slipno" class="btn-erp btn-delete" @click="deletedata">삭제</button>
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
                <th class="text-center bg-light">발행기간</th>
                <td class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                  <DateForm v-model:fromdt="searchparams.fromdt" v-model:todt="searchparams.todt" />
                </td>
                <th class="text-center bg-light">거래내역</th>
                <td>
                  <input v-model="searchparams.business" class="form-control form-control-sm" placeholder="검색어 입력" @keyup.enter="searchslips" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 메인 레이아웃 -->
      <div class="d-flex gap-2 flex-grow-1 overflow-hidden" style="min-height: 0;">

        <!-- ⬅️ 좌측: 전표 목록 -->
        <div class="card border shadow-sm d-flex flex-column overflow-hidden grid-container-left" style="width: 320px; min-width: 320px;">
          <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark">전표 목록</div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="sliplistgridref" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>

        <!-- ➡️ 우측: 전표 상세 내용 -->
        <div class="flex-grow-1 d-flex flex-column gap-2 overflow-hidden">

          <!-- 상세 마스터 폼 -->
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
                        <input v-model="masterform.deptnm" class="form-control" readonly />
                        <button class="btn btn-outline-secondary px-2" @click="handleopenhelp('dept')"><i class="bi bi-search"></i></button>
                      </div>
                    </td>
                    <th class="bg-light text-center">전표번호</th>
                    <td>
                      <div class="d-flex gap-1">
                        <input v-model="masterform.slipymd" type="date" class="form-control" />
                        <input v-model="masterform.slipno" class="form-control text-center bg-light fw-bold" readonly style="width: 60px;" />
                      </div>
                    </td>
                    <th class="bg-light text-center">발행인</th>
                    <td><input v-model="masterform.empnm" class="form-control bg-light" readonly /></td>
                  </tr>
                  <tr>
                    <th class="required bg-light text-center border-top">거래내역</th>
                    <td colspan="3" class="border-top">
                        <input v-model="masterform.business" class="form-control fw-bold text-primary" placeholder="전표 거래내역 입력" @input="syncremarktorows" />
                    </td>
                    <th class="bg-light text-center border-top">회계일자</th>
                    <td class="border-top"><input v-model="masterform.acctymd" class="form-control bg-light" readonly /></td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- 분개 그리드 & 관리항목 사이드 패널 -->
          <div class="flex-grow-1 d-flex gap-2 overflow-hidden">

            <!-- (중앙) 분개 그리드 -->
            <div class="card border shadow-sm flex-grow-1 d-flex flex-column overflow-hidden grid-container-right">
              <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
                <span class="fw-bold small text-dark"><i class="bi bi-list-columns me-2 text-primary"></i>분개 상세</span>
                <button class="btn btn-sm btn-primary py-0 px-2 fw-bold" @click="addrow" style="font-size: 11px;">+ 행추가</button>
              </div>
              <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
                <div ref="maingridref" class="tabulator-instance flex-grow-1"></div>
              </div>
              <div class="card-footer p-0 border-top bg-dark text-white">
                <div class="row g-0 text-end fw-bold small py-1 px-3">
                  <div class="col-4 text-center border-end border-secondary">분개 합계</div>
                  <div class="col-2 text-info border-end border-secondary pe-2">{{ formatmoney(totaldebit) }}</div>
                  <div class="col-2 text-warning border-end border-secondary pe-2">{{ formatmoney(totalcredit) }}</div>
                  <div class="col-4" :class="balance === 0 ? 'text-success' : 'text-danger'">차액: {{ formatmoney(balance) }}</div>
                </div>
              </div>
            </div>

            <!-- (우측) 상세 관리항목 설정 사이드 바 -->
            <div class="card border shadow-sm flex-shrink-0 d-flex flex-column bg-white overflow-hidden side-panel-wrapper" style="width: 440px;" v-if="selectedrow">
              <div class="card-header py-2 px-3 bg-secondary text-white small fw-bold d-flex justify-content-between align-items-center">
                 <span><i class="bi bi-gear-fill me-2"></i>상세 관리항목 설정</span>
                 <span class="badge" :class="String(selectedrow.dbcr || '').trim().toLowerCase() === 'd' ? 'bg-primary' : 'bg-danger'">{{ String(selectedrow.dbcr || '').trim().toLowerCase() === 'd' ? '차변' : '대변' }}</span>
              </div>
              <div class="card-body p-0 overflow-auto custom-scrollbar flex-grow-1 bg-light">

                 <div class="p-2 bg-white border-bottom d-flex align-items-center gap-2 small sticky-top">
                    <span class="badge bg-light text-dark border px-2 py-1">{{ selectedrow.acctcd }}</span>
                    <b class="text-primary">{{ selectedrow.acctnm || '계정 미선택' }}</b>
                 </div>

                 <table class="erp-table-dense side-detail-table w-100 border-0 bg-white">
                    <colgroup>
                        <col style="width: 100px;" /><col />
                    </colgroup>
                    <tbody>
                        <!-- [1] 접대비 상세 (typeacct: 100) -->
                        <template v-if="String(selectedrow.typeacct || '').trim() === '100'">
                            <tr>
                                <th class="bg-light border-bottom text-end pe-3 required">유형</th>
                                <td class="border-bottom">
                                    <select v-model="selectedrow.docno3" class="form-select form-select-sm border-0">
                                        <option v-for="opt in enttypeoptions" :key="opt.code" :value="opt.code">{{ opt.name }}</option>
                                    </select>
                                </td>
                            </tr>
                            <tr>
                                <th class="bg-light border-bottom text-end pe-3 required">접대일자</th>
                                <td class="border-bottom">
                                    <input v-model="selectedrow.docno6" type="date" class="form-control form-control-sm border-0" />
                                </td>
                            </tr>
                            <tr>
                                <th class="bg-light border-bottom text-end pe-3">사업번호</th>
                                <td class="border-bottom">
                                    <input v-model="selectedrow.docno2" class="form-control form-control-sm border-0" placeholder="숫자만 입력" />
                                </td>
                            </tr>
                            <tr>
                                <th class="bg-light border-bottom text-end pe-3 text-primary">상호</th>
                                <td class="border-bottom">
                                    <div class="input-group input-group-sm">
                                        <input v-model="selectedrow.docno4" class="form-control border-0" placeholder="상호명 직접입력 또는 검색" />
                                        <button class="btn btn-outline-secondary border-0 border-start px-2" @click="handleopenhelp('vat_cust')"><i class="bi bi-search"></i></button>
                                    </div>
                                </td>
                            </tr>
                            <tr v-if="String(selectedrow.docno3 || '').trim() === '01'">
                                <th class="bg-light border-bottom text-end pe-3 text-primary fw-bold">카드</th>
                                <td class="border-bottom">
                                    <div class="input-group input-group-sm">
                                        <input v-model="selectedrow.docno4" class="form-control border-0 text-primary" placeholder="카드번호 도움창 선택" />
                                        <button class="btn btn-outline-secondary border-0 border-start px-2" @click="handleopenhelp('card')"><i class="bi bi-credit-card"></i></button>
                                    </div>
                                </td>
                            </tr>
                            <tr>
                                <th class="bg-light border-bottom text-end pe-3 fw-bold text-primary">접대비</th>
                                <td class="border-bottom">
                                    <input v-model="selectedrow.docno8" type="number" class="form-control form-control-sm border-0 text-end fw-bold" placeholder="0" />
                                </td>
                            </tr>
                            <tr>
                                <th class="bg-light border-bottom text-end pe-3">봉사료</th>
                                <td class="border-bottom">
                                    <input v-model="selectedrow.docno9" type="number" class="form-control form-control-sm border-0 text-end" placeholder="0" />
                                </td>
                            </tr>
                        </template>

                        <!-- [2] 부가세 상세 (typeacct: 090) -->
                        <template v-else-if="String(selectedrow.typeacct || '').trim() === '090'">
                            <template v-if="String(selectedrow.typedc || '').trim().toLowerCase() === String(selectedrow.dbcr || '').trim().toLowerCase()">
                                <tr>
                                    <th class="bg-light border-bottom text-end pe-3 required">사업장</th>
                                    <td class="border-bottom">
                                        <select v-model="selectedrow.docno1" class="form-select form-select-sm border-0">
                                            <option value="">사업장 선택</option>
                                            <option v-for="opt in bizplaceoptions" :key="opt.code" :value="opt.code">{{ opt.name }}</option>
                                        </select>
                                    </td>
                                </tr>
                                <tr>
                                    <th class="bg-light border-bottom text-end pe-3 required">유형</th>
                                    <td class="border-bottom">
                                        <select v-model="selectedrow.docno3" class="form-select form-select-sm border-0">
                                            <option value="">유형 선택</option>
                                            <option v-for="opt in dynamicvatoptions" :key="opt.codecd" :value="opt.codecd">{{ opt.codenm }}</option>
                                        </select>
                                    </td>
                                </tr>
                                <tr>
                                    <th class="bg-light border-bottom text-end pe-3 required">거래처</th>
                                    <td class="border-bottom">
                                        <div class="input-group input-group-sm">
                                            <input v-model="selectedrow.docno2nm" class="form-control border-0" readonly placeholder="거래처 도움창 선택" />
                                            <button class="btn btn-outline-secondary border-0 border-start px-2" @click="handleopenhelp('vat_cust')"><i class="bi bi-search"></i></button>
                                        </div>
                                    </td>
                                </tr>
                                <tr>
                                    <th class="bg-light border-bottom text-end pe-3 required">발행일</th>
                                    <td class="border-bottom">
                                        <input v-model="selectedrow.docno6" type="date" class="form-control form-control-sm border-0" />
                                    </td>
                                </tr>
                                <tr>
                                    <th class="bg-light border-bottom text-end pe-3 fw-bold text-primary">공급가</th>
                                    <td class="border-bottom">
                                        <input v-model="selectedrow.docno8" type="number" class="form-control form-control-sm border-0 text-end fw-bold" @input="calcvat" />
                                    </td>
                                </tr>
                                <tr>
                                    <th class="bg-light border-bottom text-end pe-3 fw-bold text-danger">부가세</th>
                                    <td class="border-bottom">
                                        <input v-model="selectedrow.docno9" type="number" class="form-control form-control-sm border-0 text-end fw-bold" />
                                    </td>
                                </tr>
                                <tr v-if="String(selectedrow.dbcr || '').trim().toLowerCase() === 'd'">
                                    <th class="bg-light border-bottom text-end pe-3 text-primary fw-bold">카드</th>
                                    <td class="border-bottom">
                                        <div class="input-group input-group-sm">
                                            <input v-model="selectedrow.docno4" class="form-control border-0 text-primary" placeholder="카드번호 선택" />
                                            <button class="btn btn-outline-secondary border-0 border-start px-2" @click="handleopenhelp('card')"><i class="bi bi-credit-card"></i></button>
                                        </div>
                                    </td>
                                </tr>
                            </template>
                        </template>

                        <!-- [3] 어음 상세 (050, 060, 070) -->
                        <template v-else-if="['050', '060', '070'].includes(String(selectedrow.typeacct || '').trim())">
                            <tr>
                                <th class="bg-light border-bottom text-end pe-3 required">어음번호</th>
                                <td class="border-bottom">
                                    <div class="input-group input-group-sm">
                                        <input v-model="selectedrow.mgtno" class="form-control border-0" @keydown.enter="handleopenhelp('mgt')" placeholder="번호 입력" />
                                        <button class="btn btn-outline-secondary border-0 border-start px-2" @click="handleopenhelp('mgt')"><i class="bi bi-search"></i></button>
                                    </div>
                                </td>
                            </tr>
                            <tr v-if="(String(selectedrow.typeacct || '').trim() === '050' && String(selectedrow.dbcr || '').trim().toLowerCase() === 'd') || (String(selectedrow.typeacct || '').trim() === '060' && String(selectedrow.dbcr || '').trim().toLowerCase() === 'c')">
                                <th class="bg-light border-bottom text-end pe-3">지급/수취처</th>
                                <td class="border-bottom">
                                    <div class="input-group input-group-sm">
                                        <input v-model="selectedrow.docno9nm" class="form-control border-0" readonly placeholder="도움창 선택" />
                                        <button class="btn btn-outline-secondary border-0 border-start px-2" @click="handleopenhelp('vat_cust')"><i class="bi bi-search"></i></button>
                                    </div>
                                </td>
                            </tr>
                            <tr v-if="(String(selectedrow.typeacct || '').trim() === '050' && String(selectedrow.dbcr || '').trim().toLowerCase() === 'd') || (String(selectedrow.typeacct || '').trim() === '060' && String(selectedrow.dbcr || '').trim().toLowerCase() === 'c')">
                                <th class="bg-light border-bottom text-end pe-3">만기일자</th>
                                <td class="border-bottom">
                                    <input v-model="selectedrow.docno7" type="date" class="form-control form-control-sm border-0" />
                                </td>
                            </tr>
                        </template>

                        <!-- [4] 일반 관리항목 / 거래처 -->
                        <template v-else>
                            <tr v-if="String(selectedrow.typemgt || '').trim() > '000'">
                                <th class="bg-light border-bottom text-end pe-3 text-primary">
                                    {{
                                        String(selectedrow.typemgt || '').trim() === '010' ? '구좌번호' :
                                        String(selectedrow.typemgt || '').trim() === '020' ? '차입금' :
                                        String(selectedrow.typemgt || '').trim() === '030' ? '유가증권' :
                                        String(selectedrow.typemgt || '').trim() === '040' ? '법인카드' : (selectedrow.titmgt || selectedrow.typemgtnm || '관리항목')
                                    }}
                                </th>
                                <td class="border-bottom">
                                    <div class="input-group input-group-sm">
                                        <input v-model="selectedrow.mgtno" class="form-control border-0" @keydown.enter="handleopenhelp('mgt')" :placeholder="(String(selectedrow.typemgt || '').trim() >= '010' && String(selectedrow.typemgt || '').trim() <= '040' ? '코드' : '번호') + ' 입력'" />
                                        <button class="btn btn-outline-secondary border-0 border-start px-2" @click="handleopenhelp('mgt')"><i class="bi bi-search"></i></button>
                                    </div>
                                    <div class="small text-primary mt-1 px-1 fw-bold" v-if="selectedrow.mgtnm">{{ selectedrow.mgtnm }}</div>
                                </td>
                            </tr>
                            <tr v-if="String(selectedrow.typesub || '').trim() > '000' && (String(selectedrow.typemgt || '').trim() <= '000' || String(selectedrow.typemgt || '').trim() === '090')">
                                <th class="bg-light border-bottom text-end pe-3 text-primary">{{ selectedrow.titsub || selectedrow.typesubnm || '거래처' }}</th>
                                <td class="border-bottom">
                                    <div class="input-group input-group-sm">
                                        <input v-model="selectedrow.subnm" class="form-control border-0" readonly placeholder="도움창 선택" />
                                        <button class="btn btn-outline-secondary border-0 border-start px-2" @click="handleopenhelp('sub')"><i class="bi bi-search"></i></button>
                                    </div>
                                </td>
                            </tr>
                        </template>

                        <!-- [5] 프로젝트/예산 공통 -->
                        <template v-if="String(selectedrow.acctcd || '').trim() > '40000'">
                            <tr>
                                <th class="bg-light border-bottom text-end pe-3">프로젝트</th>
                                <td class="border-bottom">
                                    <div class="input-group input-group-sm">
                                        <input v-model="selectedrow.prjnm" class="form-control border-0" readonly />
                                        <button class="btn btn-outline-secondary border-0 border-start px-2" @click="handleopenhelp('prj')"><i class="bi bi-search"></i></button>
                                    </div>
                                </td>
                            </tr>
                            <tr v-if="String(selectedrow.typebugt || '').trim() > '000' && String(selectedrow.dbcr || '').trim().toLowerCase() === 'd'">
                                <th class="bg-light border-bottom text-end pe-3">예산과목</th>
                                <td class="border-bottom">
                                    <div class="input-group input-group-sm">
                                        <input v-model="selectedrow.bugtnm" class="form-control border-0" readonly />
                                        <button class="btn btn-outline-secondary border-0 border-start px-2" @click="handleopenhelp('bugt')"><i class="bi bi-search"></i></button>
                                    </div>
                                </td>
                            </tr>
                        </template>
                    </tbody>
                 </table>
              </div>

              <div class="card-footer bg-white p-2 border-top">
                 <div class="d-flex flex-column gap-1">
                    <button class="btn btn-xs btn-primary w-100 py-1 fw-bold shadow-sm" @click="updateautoremark(true)">
                        <i class="bi bi-magic me-1"></i> 적요 자동 조합
                    </button>
                 </div>
              </div>
            </div>

            <div class="card border shadow-sm flex-shrink-0 d-flex align-items-center justify-content-center bg-white" style="width: 440px;" v-else>
               <div class="text-center opacity-30">
                  <i class="bi bi-cursor-fill mb-2" style="font-size: 2rem;"></i>
                  <div class="small fw-bold">분개 행을 선택하면<br/>상세 입력창이 나타납니다.</div>
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

const authstore = useAuthStore()
const { firstDay, today } = getDate()
const { showAlert: showalert, showError: showerror, alertMessage: alertmessage, vAlert: valert, vAlertError: valerterror } = useAlerts()
const { resetForm: resetform } = useFormReset()
const { modalVisible: modalvisible, modalProps: modalprops, openHelp: commonopenhelp } = useCommonHelp()

const searchparams = reactive({ fromdt: firstDay, todt: today, business: '' })
const masterform = reactive<any>({
  cmpycd: authstore.cmpycd, deptcd: authstore.deptcd, deptnm: authstore.deptnm,
  slipymd: today, slipno: '', empnm: authstore.usernm, business: '',
  acctymd: today, slipgu: '010'
})

const sliplistgridref = ref<HTMLElement | null>(null)
const maingridref = ref<HTMLElement | null>(null)
let grid1: Tabulator | null = null
let grid2: Tabulator | null = null
const selectedrow = ref<any>(null)
let activecomponent: any = null

const purchasevatoptions = ref<any[]>([]); const salesvatoptions = ref<any[]>([])
const bizplaceoptions = ref<any[]>([]);
const enttypeoptions = [{code:'01', name:'카드'}, {code:'02', name:'현금'}, {code:'03', name:'세금계산서'}]

const dynamicvatoptions = computed(() => {
  if (!selectedrow.value) return []
  const dbcr = String(selectedrow.value.dbcr || '').trim().toLowerCase();
  return dbcr === 'd' ? purchasevatoptions.value : salesvatoptions.value
})

const totaldebit = ref(0); const totalcredit = ref(0)
const balance = computed(() => totaldebit.value - totalcredit.value)
const formatmoney = (val: any) => Number(val || 0).toLocaleString()

const updateRowStatus = (row: any) => {
    const d = row.getData();
    if (d._state === 'EXIST' && !d._status) row.update({ _status: '수정' });
}

watch(selectedrow, (newval) => {
    if (newval && activecomponent) {
        activecomponent.update(newval);
        updateRowStatus(activecomponent);
        updateautoremark();
    }
}, { deep: true });

function updateautoremark(force = false) {
    const row = selectedrow.value; if (!row) return;
    let prefix = "";
    if (row.subnm) prefix = `[${row.subnm}] `;
    else if (row.docno4) prefix = `[${row.docno4}] `;
    else if (row.mgtnm) prefix = `[${row.mgtnm}] `;
    else if (row.acctnm) prefix = `[${row.acctnm}] `;

    const currentremark = (row.remark || "").trim();
    if (force || !currentremark || currentremark.startsWith('[') || currentremark === masterform.business) {
        row.remark = prefix + (masterform.business || "");
    }
}

function syncremarktorows() {
    grid2?.getRows().forEach(row => {
        const d = row.getData();
        if (!d.remark || d.remark.startsWith('[') || d.remark === "") {
            let prefix = d.subnm ? `[${d.subnm}] ` : (d.mgtnm ? `[${d.mgtnm}] ` : (d.acctnm ? `[${d.acctnm}] ` : ""));
            row.update({ remark: prefix + masterform.business });
        }
    });
}

const initgrids = () => {
  grid1 = new Tabulator(sliplistgridref.value!, {
    layout: "fitColumns", height: "100%", placeholder: "데이터 없음",
    columns: [
      { title: "No", formatter: "rownum", width: 40, hozAlign: "center" },
      { title: "발행일", field: "slipymd", width: 95, hozAlign: "center", formatter: (c) => {
          const v = c.getValue() || ''; return v.length === 8 ? `${v.substring(0,4)}-${v.substring(4,6)}-${v.substring(6,8)}` : v;
      }},
      { title: "거래내역", field: "business", hozAlign: "left", headerSort: false }
    ],
  });
  grid1.on("rowClick", (e, row) => fetchdetail(row.getData()));

  grid2 = new Tabulator(maingridref.value!, {
    layout: "fitColumns", height: "100%", selectable: 1,
    columnDefaults: {
        headerHozAlign: 'center', headerSort: false, vertAlign: "middle",
        cellClick: (e, cell) => {
            cell.getRow().select();
            selectedrow.value = cell.getRow().getData();
            activecomponent = cell.getRow();
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
      { title: "계정과목", field: "acctnm", width: 160, cellClick: (e, cell) => handleopenhelp('acct', cell.getRow()), cssClass: "cursor-pointer text-primary fw-bold" },
      { title: "적요", field: "remark", minWidth: 200, editor: "input" },
      { title: "금액", field: "amount", width: 110, hozAlign: "right", editor: "number", formatter: "money", formatterParams: { precision: 0 } },
      { title: "삭제", width: 40, hozAlign: "center", formatter: () => '<i class="bi bi-trash text-danger"></i>', cellClick: (e, cell) => handlerowaction(cell.getRow()) }
    ]
  });

  grid2.on("cellEdited", (cell) => {
    selectedrow.value = cell.getRow().getData();
    activecomponent = cell.getRow();
    updateRowStatus(activecomponent);
    updatetotals();
  });
}

const updatetotals = () => {
  const data = grid2?.getData() || []
  totaldebit.value = data.reduce((s, r: any) => s + (String(r.dbcr || '').trim().toLowerCase() === 'd' ? Number(r.amount || 0) : 0), 0)
  totalcredit.value = data.reduce((s, r: any) => s + (String(r.dbcr || '').trim().toLowerCase() === 'c' ? Number(r.amount || 0) : 0), 0)
}

async function searchslips() {
  try {
    const res = await api.post('/hasl/HASL_010U_STR', { actkind: 'F', cmpycd: authstore.cmpycd, fromdt: searchparams.fromdt.replace(/-/g, ''), todt: searchparams.todt.replace(/-/g, ''), keyword: searchparams.business });
    grid1?.setData(res.data);
  } catch (e) { valerterror('조회 실패'); }
}

async function fetchdetail(row: any) {
  const master = row;
  Object.assign(masterform, master);
  if (masterform.slipymd && masterform.slipymd.length === 8) {
    const d = masterform.slipymd; masterform.slipymd = `${d.substring(0,4)}-${d.substring(4,6)}-${d.substring(6,8)}`;
  }
  try {
    const resd = await api.post('/hasl/HASL_011U_STR', { actkind: 'S', cmpycd: authstore.cmpycd, slipymd: String(masterform.slipymd).replace(/-/g,''), slipno: masterform.slipno });
    grid2?.setData(resd.data || []);
    updatetotals();
  } catch (e) { valerterror('상세 조회 실패'); }
}

function handleopenhelp(type: string, target?: any) {
  if (type === 'dept') commonopenhelp('DEPT', (d) => { masterform.deptcd = d.deptcd; masterform.deptnm = d.deptnm });
  else if (type === 'acct') {
    commonopenhelp('ACCT', (d: any) => {
        const res = d;
        target.update({ acctcd: res.acctcd, acctnm: res.acctnm, typeacct: res.typeacct, typemgt: res.typemgt, typesub: res.typesub, typedc: res.typedc, typebugt: res.bugtcd });
        selectedrow.value = target.getData();
        activecomponent = target;
    });
  } else if (type === 'mgt' || type === 'card') {
      const mgtgbn = type === 'card' ? '040' : selectedrow.value.typemgt;
      commonopenhelp('MGT', (res: any) => {
          if (type === 'card' && String(selectedrow.value.typeacct || '').trim() === '090') selectedrow.value.docno4 = res.mgtno;
          else { selectedrow.value.mgtno = res.mgtno; selectedrow.value.mgtnm = res.mgtnm; }
      }, { acctcd: selectedrow.value.acctcd, mgtgbn: mgtgbn });
  } else if (type === 'sub' || type === 'vat_cust') {
      commonopenhelp('CUST', (res: any) => {
        selectedrow.value.subcd = res.custcd; selectedrow.value.subnm = res.custnm;
        selectedrow.value.docno2 = res.busino || res.custcd;
        selectedrow.value.docno2nm = res.custnm;
      });
  } else if (type === 'prj') {
      commonopenhelp('PRJ', (n: any) => { selectedrow.value.prjcd = n.prjcd; selectedrow.value.prjnm = n.prjnm });
  } else if (type === 'bugt') {
      commonopenhelp('BUGT', (n: any) => { selectedrow.value.bugtcd = n.bugtcd; selectedrow.value.bugtnm = n.bugtnm }, { acctcd: selectedrow.value.acctcd });
  }
}

const calcvat = () => { if (String(selectedrow.value?.typeacct || '').trim() === '090') selectedrow.value.docno9 = Math.floor(Number(selectedrow.value.docno8 || 0) * 0.1); }

async function save() {
  if (balance.value !== 0) return valerterror('차/대변 금액이 일치하지 않습니다.');
  if (!masterform.business) return valerterror('거래내역을 입력하십시오.');

  const allData = grid2?.getData() || [];
  const detailsToSave = allData.filter((r: any) => r._status);

  if (detailsToSave.length === 0 && !masterform.slipno) return valerterror('저장할 내역이 없습니다.');

  try {
    const payload = {
      actkind: masterform.slipno ? 'U' : 'A',
      master: { ...masterform, slipymd: masterform.slipymd.replace(/-/g, ''), acctymd: masterform.acctymd.replace(/-/g, '') },
      details: allData.map(item => {
        return {
            ...item,
            custcd: item.subcd,
            dbamt: String(item.dbcr).toLowerCase() === 'd' ? (item.amount || 0) : 0,
            cramt: String(item.dbcr).toLowerCase() === 'c' ? (item.amount || 0) : 0,
            upkind: item._status === '입력' ? 'A' : (item._status === '삭제' ? 'D' : 'U')
        };
      })
    };
    const res = await api.post('/hasl/HASL_010U_SAVE', payload);
    if (res.data?.[0]?.result === 'N') return valerterror(res.data[0].msg || '저장 실패');
    valert('저장되었습니다.'); searchslips();
  } catch (e) { valerterror('저장 실패'); }
}

const initialize = () => {
    resetform(masterform);
    Object.assign(masterform, { cmpycd: authstore.cmpycd, deptcd: authstore.deptcd, deptnm: authstore.deptnm, slipymd: today, acctymd: today, empnm: authstore.usernm, slipgu: '010' });
    grid2?.clearData(); selectedrow.value = null; totaldebit.value = 0; totalcredit.value = 0;
}

const addrow = () => {
    const data = grid2?.getData() || [];
    const lastrow = data.length > 0 ? data[data.length - 1] : null;
    const newdbcr = lastrow ? (String(lastrow.dbcr).toLowerCase() === 'd' ? 'c' : 'd') : 'd';
    grid2?.addRow({ dbcr: newdbcr, remark: masterform.business, amount: 0, srowno: data.length + 1, _status: '입력', _state: 'NEW' }, false).then(row => {
        nextTick(() => { row.select(); selectedrow.value = row.getData(); activecomponent = row; });
    });
}

const handlerowaction = (row: any) => {
    const d = row.getData();
    if (d._state === 'NEW') row.delete();
    else {
        const newstatus = d._status === '삭제' ? '' : '삭제';
        row.update({ _status: newstatus });
    }
}

async function deletedata() {
  if (!confirm('정말 삭제하시겠습니까?')) return;
  try {
    const res = await api.post('/hasl/HASL_010U_SAVE', { actkind: 'D', master: { ...masterform, slipymd: masterform.slipymd.replace(/-/g, '') }, details: [] });
    if (res.data?.[0]?.result === 'N') return valerterror(res.data[0].msg || '삭제 실패');
    valert('삭제되었습니다.'); initialize(); searchslips();
  } catch (e) { valerterror('삭제 실패'); }
}

onMounted(() => {
  api.post('/ha00/HA00_00P_STR', { gubun: 'E0', gbncd: '120' }).then(r => { purchasevatoptions.value = r.data; });
  api.post('/ha00/HA00_00P_STR', { gubun: 'E0', gbncd: '130' }).then(r => { salesvatoptions.value = r.data; });
  api.post('/ha00/HA00_00P_STR', { gubun: 'SA', cmpycd: authstore.cmpycd }).then(r => {
    bizplaceoptions.value = (r.data || []).map(n => { return { code: n.taxunit || n.code || '', name: n.unitnm || n.codenm || '' }; });
  });
  nextTick(() => { initgrids(); searchslips(); });
})
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; }
.side-detail-table th { padding: 2px 10px !important; font-size: 12px; font-weight: 600; color: #555; background-color: #f8f9fa; border-right: 1px solid #eee; border-bottom: 1px solid #eee !important; white-space: nowrap; height: 25px; }
.side-detail-table td { padding: 1px 6px !important; border-bottom: 1px solid #eee !important; }
.side-detail-table th.required::after { content: " *"; color: #dc3545; }
</style>
