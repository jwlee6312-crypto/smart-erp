<!--	=============================================================
	프로그램명	: 매입매출총괄표 (부가가치세 신고서)
	작성일자	: 2025.02.24
	작성자	    : AI Assistant
	설명        : 일반과세자 부가가치세 신고서 형식의 매입/매출 총괄 현황 조회 (HSOD100U 표준 UI 적용)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-2 px-3 sticky-top shadow-sm flex-shrink-0">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-file-earmark-ruled me-2 text-primary" style="font-size: 18px;"></i>
        세무관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">매입매출총괄표 (HATX100S)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-2">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="search">조회</button>
        <button class="btn-erp btn-print" @click="handlePrint">인쇄</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 조회 필터 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense" width="100%">
            <colgroup>
                <col style="width: 8%" /><col style="width: 25%" />
                <col style="width: 8%" /><col style="width: 59%" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light required">사업장</th>
                <td>
                  <select v-model="searchForm.taxunit" class="form-select form-select-sm ms-1" style="width: 200px;">
                    <option value="000">전체</option>
                    <option v-for="opt in taxUnitOptions" :key="opt.code" :value="opt.code">{{ opt.codenm }}</option>
                  </select>
                </td>
                <th class="text-center bg-light required">기 간</th>
                <td>
                  <div class="d-flex align-items-center gap-1 px-1">
                    <select v-model="searchForm.YY" class="form-select form-select-sm" style="width: 100px;">
                      <option v-for="y in yearOptions" :key="y" :value="y">{{ y }}년</option>
                    </select>
                    <select v-model="searchForm.FMM" class="form-select form-select-sm" style="width: 80px;">
                      <option v-for="m in monthOptions" :key="m" :value="m">{{ m }}월</option>
                    </select>
                    <span class="mx-1 small">~</span>
                    <select v-model="searchForm.TMM" class="form-select form-select-sm" style="width: 80px;">
                      <option v-for="m in monthOptions" :key="m" :value="m">{{ m }}월</option>
                    </select>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 신고서 레포트 영역 -->
      <div class="card border shadow-sm flex-grow-1 overflow-auto d-flex flex-column bg-white p-4">
        <div class="report-document mx-auto border p-5 shadow-sm" style="width: 210mm; min-height: 297mm; background: #fff;">
          <div class="d-flex justify-content-between mb-2 small text-muted">
            <span>[별지 제12호서식]</span>
            <span>(1장앞쪽)</span>
          </div>

          <div class="text-center mb-4">
            <h3 class="fw-bolder">일반과세자 부가가치세 신고서</h3>
            <div class="d-flex justify-content-center gap-3 mt-2">
              <div class="form-check form-check-inline">
                <input class="form-check-input" type="checkbox" :checked="isYejung" disabled>
                <label class="form-check-label">예정</label>
              </div>
              <div class="form-check form-check-inline">
                <input class="form-check-input" type="checkbox" :checked="isHakjung" disabled>
                <label class="form-check-label">확정</label>
              </div>
            </div>
            <div class="mt-2 fw-bold">
              신고기간: {{ searchForm.YY }}년 {{ searchForm.FMM }}월 01일 ~ {{ searchForm.TMM }}월 {{ lastDayOfTmm }}일
            </div>
          </div>

          <!-- 사업자 정보 -->
          <table class="table table-bordered report-table small">
            <tbody>
              <tr>
                <th rowspan="3" class="text-center align-middle bg-light" style="width: 40px;">사업자</th>
                <th class="bg-light" style="width: 120px;">상호(법인명)</th>
                <td style="width: 200px;">{{ bizInfo.LTDNM }}</td>
                <th class="bg-light" style="width: 120px;">성명(대표자)</th>
                <td>{{ bizInfo.BOSSNM }}</td>
              </tr>
              <tr>
                <th class="bg-light">사업자등록번호</th>
                <td>{{ formatSaupNo(bizInfo.SAUPNO) }}</td>
                <th class="bg-light">전화번호</th>
                <td>{{ bizInfo.TELNO }}</td>
              </tr>
              <tr>
                <th class="bg-light">사업장주소</th>
                <td colspan="3">{{ bizInfo.ADDRESS }}</td>
              </tr>
            </tbody>
          </table>

          <!-- 신고 내용 -->
          <div class="mt-3 fw-bold small mb-1">① 신 고 내 용</div>
          <table class="table table-bordered report-table small">
            <thead class="bg-light text-center">
              <tr>
                <th colspan="3">구 분</th>
                <th style="width: 150px;">금 액</th>
                <th style="width: 60px;">세율</th>
                <th style="width: 150px;">세 액</th>
              </tr>
            </thead>
            <tbody>
              <!-- 매출세액 -->
              <tr>
                <th rowspan="4" class="text-center align-middle bg-light" style="width: 40px;">과세<br>표준<br>및<br>매출<br>세액</th>
                <th rowspan="4" class="text-center align-middle bg-light" style="width: 40px;">과세</th>
                <td>세금계산서 발급분 (1)</td>
                <td class="text-end pe-2">{{ nf(report.amt1) }}</td>
                <td class="text-center">10/100</td>
                <td class="text-end pe-2">{{ nf(report.tax1) }}</td>
              </tr>
              <tr>
                <td>매입자 발행 세금계산서 (2)</td>
                <td class="text-end pe-2">{{ nf(report.amt11) }}</td>
                <td class="text-center">10/100</td>
                <td class="text-end pe-2">{{ nf(report.tax11) }}</td>
              </tr>
              <tr>
                <td>신용카드·현금영수증 발행분 (3)</td>
                <td class="text-end pe-2">{{ nf(report.amt2_1) }}</td>
                <td class="text-center">10/100</td>
                <td class="text-end pe-2">{{ nf(report.tax2_1) }}</td>
              </tr>
              <tr>
                <td>기타(영수증 외 매출분) (4)</td>
                <td class="text-end pe-2">{{ nf(report.amt2) }}</td>
                <td class="text-center">10/100</td>
                <td class="text-end pe-2">{{ nf(report.tax2) }}</td>
              </tr>
              <tr class="fw-bold bg-light-blue">
                <th colspan="3" class="text-center">합 계 (9)</th>
                <td class="text-end pe-2">{{ nf(totalSalesAmt) }}</td>
                <td class="text-center">㉮</td>
                <td class="text-end pe-2">{{ nf(totalSalesTax) }}</td>
              </tr>

              <!-- 매입세액 -->
              <tr>
                <th rowspan="4" class="text-center align-middle bg-light">매입<br>세액</th>
                <th rowspan="2" class="text-center align-middle bg-light">세금<br>계산서<br>수취분</th>
                <td>일반매입 (10)</td>
                <td class="text-end pe-2">{{ nf(report.amt6) }}</td>
                <td class="bg-light"></td>
                <td class="text-end pe-2">{{ nf(report.tax6) }}</td>
              </tr>
              <tr>
                <td>고정자산 매입 (11)</td>
                <td class="text-end pe-2">{{ nf(report.amt7) }}</td>
                <td class="bg-light"></td>
                <td class="text-end pe-2">{{ nf(report.tax7) }}</td>
              </tr>
              <tr>
                <th colspan="2" class="text-center bg-light">공제받지 못할 매입세액 (16)</th>
                <td class="text-end pe-2">{{ nf(report.amt8) }}</td>
                <td class="bg-light"></td>
                <td class="text-end pe-2 text-danger">{{ nf(report.tax8) }}</td>
              </tr>
              <tr class="fw-bold bg-light-green">
                <th colspan="2" class="text-center">차 감 계 (17 = 15 - 16)</th>
                <td class="text-end pe-2">{{ nf(totalPurchaseAmt) }}</td>
                <td class="text-center">㉯</td>
                <td class="text-end pe-2">{{ nf(totalPurchaseTax) }}</td>
              </tr>

              <!-- 납부세액 -->
              <tr class="fw-bold" style="background-color: #fff9c4;">
                <td colspan="4" class="text-center align-middle p-2">납부(환급)세액 (매출세액㉮ - 매입세액㉯)</td>
                <td class="text-center">㉰</td>
                <td class="text-end pe-2 fs-6 text-primary">{{ nf(payableTax) }}</td>
              </tr>
            </tbody>
          </table>

          <!-- 하단 서명란 -->
          <div class="mt-5 pt-4 text-center">
            <p>부가가치세법 제18조· 제19조 또는 제24조와 국세기본법 제 45조의 3의 규정에 따라 신고하며<br>
               위 내용을 충분히 검토하였고 신고인이 알고 있는 사실 그대로를 정확하게 기재하였음을 확인합니다.</p>

            <div class="mt-4 fs-5 fw-bold">{{ searchForm.YY }}년 {{ searchForm.TMM }}월 25일</div>

            <div class="d-flex justify-content-center gap-5 mt-4 fs-5">
              <span>신고인: <strong>{{ bizInfo.BOSSNM }}</strong></span>
              <span class="text-muted small">(서명 또는 인)</span>
            </div>

            <div class="mt-5 text-end pe-5 fs-4 fw-bold text-dark">
              {{ bizInfo.TAXOFFINM || '세무서장' }} 귀하
            </div>
          </div>

        </div>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, nextTick } from 'vue'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import AppAlert from '@/components/AppAlert.vue'

const authStore = useAuthStore()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()

// [1] 데이터 모델링
const currentYear = new Date().getFullYear()
const currentMonth = (new Date().getMonth() + 1).toString().padStart(2, '0')
const yearOptions = Array.from({ length: 6 }, (_, i) => String(currentYear - i))
const monthOptions = Array.from({ length: 12 }, (_, i) => String(i + 1).padStart(2, '0'))

const searchForm = reactive({
  taxunit: '000',
  YY: String(currentYear),
  FMM: currentMonth,
  TMM: currentMonth
})

const taxUnitOptions = ref<any[]>([])

const bizInfo = reactive({
    SAUPNO: '', LTDNM: '', BOSSNM: '', ADDRESS: '', TELNO: '', TAXOFFINM: '', LEGALNO: ''
})

const report = reactive({
    amt1: 0, tax1: 0, amt11: 0, tax11: 0, amt2_1: 0, tax2_1: 0, amt2: 0, tax2: 0,
    amt3: 0, tax3: 0, amt4: 0, tax4: 0, amt5: 0, tax5: 0, daevat200: 0,
    amt6: 0, tax6: 0, amt7: 0, tax7: 0, amt9: 0, tax9: 0, amt91: 0, tax91: 0, amt92: 0, tax92: 0,
    amt8: 0, tax8: 0, daeamt100: 0, payamt: 0
})

// [2] 계산 프로퍼티
const isYejung = computed(() => ['01','02','03','07','08','09'].includes(searchForm.TMM))
const isHakjung = computed(() => ['04','05','06','10','11','12'].includes(searchForm.TMM))

const lastDayOfTmm = computed(() => {
    const m = parseInt(searchForm.TMM);
    if ([1, 3, 5, 7, 8, 10, 12].includes(m)) return 31;
    if ([4, 6, 9, 11].includes(m)) return 30;
    if (m === 2) return 28;
    return 30;
})

const totalSalesAmt = computed(() => report.amt1 + report.amt11 + report.amt2_1 + report.amt2 + report.amt3 + report.amt4 + report.amt5)
const totalSalesTax = computed(() => report.tax1 + report.tax11 + report.tax2_1 + report.tax2 + report.tax5 + report.daevat200)

const totalPurchaseAmt = computed(() => report.amt6 + report.amt7 + report.amt9 + report.amt91 + report.amt92 - report.amt8)
const totalPurchaseTax = computed(() => report.tax6 + report.tax7 + report.tax9 + report.tax91 + report.tax92 - report.tax8)

const payableTax = computed(() => totalSalesTax.value - totalPurchaseTax.value)

// [3] 비즈니스 로직
const fetchOptions = async () => {
  try {
    const res = await api.post('/ha00/HA00_00P_STR', { gubun: 'SA', cmpycd: authStore.cmpycd, search: ' ' });
    taxUnitOptions.value = res.data || [];
  } catch (e) { console.error(e) }
}

async function search() {
  try {
    const ymfr = searchForm.YY + searchForm.FMM;
    const ymto = searchForm.YY + searchForm.TMM;

    // 1. 사업장 정보 조회
    const bizRes = await api.post('/haba/HABA_030U_STR', { actkind: 'TX', cmpycd: authStore.cmpycd, unitcd: searchForm.taxunit });
    if (bizRes.data?.length) Object.assign(bizInfo, bizRes.data[0]);

    // 2. 부가세 합계 데이터 조회 (iogbn '51' - 신고서 메인)
    const res51 = await api.post('/hatx/HATX_110S_STR', {
      cmpycd: authStore.cmpycd, iogbn: '51', taxunit: searchForm.taxunit, ymfr: ymfr, ymto: ymto
    });

    if (res51.data?.length) {
        const d = res51.data[0];
        // ASP rs(idx) 순서에 따른 매핑
        Object.assign(report, {
            amt1: Number(d.col0 || 0), tax1: Number(d.col1 || 0),
            amt2: Number(d.col2 || 0), tax2: Number(d.col3 || 0),
            amt3: Number(d.col4 || 0), tax3: Number(d.col5 || 0),
            amt4: Number(d.col6 || 0), tax4: Number(d.col7 || 0),
            amt5: Number(d.col8 || 0), tax5: Number(d.col9 || 0),
            amt6: Number(d.col10 || 0), tax6: Number(d.col11 || 0),
            amt7: Number(d.col12 || 0), tax7: Number(d.col13 || 0),
            amt8: Number(d.col14 || 0), tax8: Number(d.col15 || 0),
            amt11: Number(d.col16 || 0), tax11: Number(d.col17 || 0),
            amt9: Number(d.col18 || 0), tax9: Number(d.col19 || 0),
            amt91: Number(d.col20 || 0), tax91: Number(d.col21 || 0),
            amt92: Number(d.col22 || 0), tax92: Number(d.col23 || 0),
            amt2_1: Number(d.col24 || 0), tax2_1: Number(d.col25 || 0),
            daevat200: Number(d.col28 || 0), daeamt100: Number(d.col29 || 0),
            payamt: Number(d.col27 || 0)
        });
    }

    vAlert('조회되었습니다.');
  } catch (e) { vAlertError('조회 실패'); }
}

const initialize = () => {
    searchForm.taxunit = '000';
    searchForm.YY = String(currentYear);
    searchForm.FMM = currentMonth;
    searchForm.TMM = currentMonth;
}

const handlePrint = () => {
    const params = new URLSearchParams({
        taxunit: searchForm.taxunit, YY: searchForm.YY, FMM: searchForm.FMM, TMM: searchForm.TMM, PRTGU: 'Print'
    }).toString();
    window.open(`/hatx/HATX_100P?${params}`, 'TaxReportPrint', 'width=1000,height=800,scrollbars=yes');
}

const nf = (v: any) => v ? Number(v).toLocaleString() : '0'
const formatSaupNo = (v: string) => v && v.length === 10 ? `${v.slice(0,3)}-${v.slice(3,5)}-${v.slice(5)}` : v

onMounted(() => {
  fetchOptions();
  search();
})
</script>

<style scoped>
.report-document { font-family: "GulimChe", "Gulim", "Courier New", monospace; color: #000; line-height: 1.2; }
.report-table { border-collapse: collapse; border: 2px solid #000 !important; }
.report-table th, .report-table td { border: 1px solid #000 !important; padding: 4px 6px; }
.bg-light-blue { background-color: #e3f2fd; }
.bg-light-green { background-color: #f1f8e9; }
</style>
