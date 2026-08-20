<!--
	=============================================================
	프로그램명	: 외주(임)가공계약등록 (HPIO290U)
	작성일자	: 2025.03.11
	설명        : 외주 가공 계약 마스터/상세/자재 관리 (표준화 적용)
	=============================================================
-->
<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-pencil-square me-2 text-primary" style="font-size: 18px;"></i>
        생산정보 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">외주(임)가공계약등록 (HPIO290U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchList">조회</button>
        <button class="btn-erp btn-save" @click="saveAll">저장</button>
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
                <th class="text-center bg-light">품의기간</th>
                <td class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                  <DateForm v-model:fromdt="form_01.fromdt" v-model:todt="form_01.todt" />
                </td>
                <th class="text-center bg-light">거래처명</th>
                <td>
                  <input v-model="form_01.custnm" class="form-control form-control-sm" placeholder="거래처명 검색" @keyup.enter="fetchList" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 투-그리드 레이아웃 영역 (좌/우 2단 구성) -->
      <div class="d-flex flex-row flex-grow-1 overflow-hidden gap-2" style="min-height: 0;">

        <!-- 🅰️ 좌측: 계약 목록 리스트 -->
        <div class="d-flex flex-column gap-2" style="width: 480px;">
          <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column">
          <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between">
            <span class="fw-bold small text-dark"><i class="bi bi-list-ul me-2 text-primary"></i>계약 목록</span>
            <span class="badge bg-primary-subtle text-primary border border-primary-subtle" style="font-size: 10px;">LO</span>
          </div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="listTableRef" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>
      </div>

      <!-- 🅱️ 우측: 상세 내용 및 그리드 -->
      <div class="flex-grow-1 d-flex flex-column gap-2 overflow-hidden">
        <!-- 마스터 정보 폼 -->
        <div class="card border shadow-sm flex-shrink-0">
          <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center">
            <span class="fw-bold small text-dark"><i class="bi bi-file-earmark-text me-2 text-primary"></i>계약 상세 정보</span>
          </div>
          <div class="card-body p-0 bg-white">
            <table class="erp-table-dense w-100">
              <colgroup>
                <col style="width: 120px;" /><col />
                <col style="width: 120px;" /><col />
                <col style="width: 120px;" /><col />
              </colgroup>
              <tbody>
                <tr>
                  <th class="required bg-light text-center">품의번호</th>
                  <td>
                    <div class="d-flex align-items-center gap-1 px-1">
                      <input v-model="pumym_f" type="month" class="form-control form-control-sm" style="max-width: 120px;" />
                      <input v-model="form_02.pumno" type="text" class="form-control form-control-sm text-center bg-light fw-bold text-primary" style="width: 80px;" readonly />
                    </div>
                  </td>
                  <th class="required bg-light text-center">품의일자</th>
                  <td><div class="px-1"><input v-model="pumymd_f" type="date" class="form-control form-control-sm" /></div></td>
                  <th class="required bg-light text-center">품의부서</th>
                  <td>
                    <div class="input-group input-group-sm px-1">
                      <input v-model="form_02.deptcd" type="text" class="form-control text-center bg-light" style="max-width: 80px;" readonly />
                      <input v-model="form_02.deptnm" type="text" class="form-control" readonly />
                      <button class="btn btn-outline-secondary px-2" @click="handleOpenHelp('DEPT')"><i class="bi bi-search"></i></button>
                    </div>
                  </td>
                </tr>
                <tr>
                  <th class="required bg-light text-center">거 래 처</th>
                  <td>
                    <div class="input-group input-group-sm px-1">
                      <input v-model="form_02.custcd" type="text" class="form-control text-center bg-light" style="max-width: 80px;" readonly />
                      <input v-model="form_02.custnm" type="text" class="form-control" readonly />
                      <button class="btn btn-outline-secondary px-2" @click="handleOpenHelp('CUST')"><i class="bi bi-search"></i></button>
                    </div>
                  </td>
                  <th class="required bg-light text-center">계약기간</th>
                  <td colspan="3">
                    <div class="d-flex align-items-center gap-1 px-1">
                      <input v-model="frymd_f" type="date" class="form-control form-control-sm" style="max-width: 150px;" />
                      <span class="small mx-1">~</span>
                      <input v-model="toymd_f" type="date" class="form-control form-control-sm" style="max-width: 150px;" />
                      <span class="ms-3 small fw-bold text-muted" style="min-width: 60px;">특기사항:</span>
                      <input v-model="form_02.remark" class="form-control form-control-sm flex-grow-1" placeholder="계약 관련 중요 메모" />
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- 하단: 제품 및 자재 그리드 -->
        <div class="d-flex flex-column gap-2 flex-grow-1 overflow-hidden">
          <!-- 제품 그리드 -->
          <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column">
            <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
              <span class="fw-bold small text-dark"><i class="bi bi-box-seam me-2 text-primary"></i>계약 대상 제품 리스트</span>
              <div class="d-flex gap-1">
                <button class="btn btn-sm btn-outline-primary py-0 px-2 fw-bold" @click="addRow('PROD')" style="font-size: 11px;">+ 제품추가</button>
                <button class="btn btn-sm btn-outline-danger py-0 px-2 fw-bold" @click="deleteRows('PROD')" style="font-size: 11px;">- 삭제</button>
              </div>
            </div>
            <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
              <div ref="prodTableRef" class="tabulator-instance flex-grow-1"></div>
            </div>
          </div>

          <!-- 자재 그리드 -->
          <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column">
            <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
              <span class="fw-bold small text-dark">
                <i class="bi bi-tools me-2 text-success"></i>투입 자재 소요량 상세
                <span v-if="selectedProduct.itemnm" class="badge bg-success-subtle text-success border border-success-subtle ms-2">{{ selectedProduct.itemnm }}</span>
              </span>
              <div class="d-flex gap-1">
                <button class="btn btn-sm btn-outline-primary py-0 px-2 fw-bold" @click="addRow('MAT')" :disabled="!selectedProduct.itemcd" style="font-size: 11px;">+ 자재추가</button>
                <button class="btn btn-sm btn-outline-danger py-0 px-2 fw-bold" @click="deleteRows('MAT')" :disabled="!selectedProduct.itemcd" style="font-size: 11px;">- 삭제</button>
              </div>
            </div>
            <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
              <div ref="matTableRef" class="tabulator-instance flex-grow-1"></div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, computed, nextTick } from 'vue'
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
const { today, firstDay } = getDate()
const { showAlert, showError, alertMessage, vAlert, vAlertError } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps, openHelp } = useCommonHelp()

// [1] 데이터 모델링
const form_01 = reactive({ fromdt: firstDay, todt: today, custnm: '' })

const form_02 = reactive<any>({
  actkind: 'S0', cmpycd: authStore.cmpycd,
  deptcd: authStore.deptcd, deptnm: authStore.deptnm,
  pumym: today.replace(/-/g, '').substring(0, 6),
  pumno: '000',
  pumymd: today.replace(/-/g, ''),
  frymd: today.replace(/-/g, ''),
  toymd: today.replace(/-/g, ''),
  custcd: '', custnm: '', remark: '', pumgbn: '200'
})

const selectedProduct = reactive<any>({ itemcd: '', itemnm: '' })

// 포맷팅 헬퍼
const pumym_f = computed({ get: () => form_02.pumym ? `${form_02.pumym.substring(0, 4)}-${form_02.pumym.substring(4, 6)}` : '', set: (v) => { if (v) form_02.pumym = v.replace(/-/g, '') } })
const pumymd_f = computed({ get: () => formatDate(form_02.pumymd), set: (v) => { if (v) form_02.pumymd = v.replace(/-/g, '') } })
const frymd_f = computed({ get: () => formatDate(form_02.frymd), set: (v) => { if (v) form_02.frymd = v.replace(/-/g, '') } })
const toymd_f = computed({ get: () => formatDate(form_02.toymd), set: (v) => { if (v) form_02.toymd = v.replace(/-/g, '') } })

const listTableRef = ref<HTMLDivElement | null>(null)
const prodTableRef = ref<HTMLDivElement | null>(null)
const matTableRef = ref<HTMLDivElement | null>(null)

let listGrid: Tabulator | null = null
let prodGrid: Tabulator | null = null
let matGrid: Tabulator | null = null

// [2] 그리드 초기화
const initGrids = () => {
  listGrid = new Tabulator(listTableRef.value!, {
    layout: "fitColumns", height: "100%", placeholder: "목록 없음", selectable: 1,
    columns: [
      { title: "번호", field: "pumno", width: 70, hozAlign: "center", headerHozAlign: "center" },
      { title: "거래처명", field: "custnm", minWidth: 150, headerHozAlign: "center" },
      { title: "품의일", field: "pumymd", width: 100, hozAlign: "center", headerHozAlign: "center", formatter: (c) => formatDate(c.getValue()) }
    ]
  });
  listGrid.on("rowClick", (e, row) => fetchDetail(row.getData()));

  prodGrid = new Tabulator(prodTableRef.value!, {
    layout: "fitColumns", height: "100%", selectable: true,
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { formatter: "rowSelection", titleFormatter: "rowSelection", width: 40, hozAlign: "center", headerSort: false },
      { title: "상태", field: "_status", width: 50, hozAlign: "center", formatter: (c) => {
          const v = c.getValue();
          if (v === '입력') return '<span class="badge bg-primary">신규</span>';
          if (v === '수정') return '<span class="badge bg-warning text-dark">수정</span>';
          if (v === '삭제') return '<span class="badge bg-danger">삭제</span>';
          return '';
      }},
      { title: "제품명", field: "itemnm", minWidth: 250, widthGrow: 1, cellClick: (e, cell) => { if (!cell.getData().itemcd) handleOpenHelp('ITEM', cell.getRow()) } },
      { title: "규격", field: "itsize", width: 200 },
      { title: "단가", field: "price", width: 180, hozAlign: "right", editor: "number", cellEdited: (c) => markEdit(c.getRow()) }
    ]
  });

  // 단가(price) 셀을 클릭할 때는 자재 조회를 하지 않도록 cellClick으로 변경
  prodGrid.on("cellClick", (e, cell) => {
    if (cell.getField() !== "price") {
      fetchMaterials(cell.getRow().getData());
    }
  });

  matGrid = new Tabulator(matTableRef.value!, {
    layout: "fitColumns", height: "100%", placeholder: "자재 내역 없음", selectable: true,
    columnDefaults: { headerHozAlign: 'center', headerSort: false, vertAlign: "middle" },
    columns: [
      { formatter: "rowSelection", titleFormatter: "rowSelection", width: 40, hozAlign: "center", headerSort: false },
      { title: "상태", field: "_status", width: 50, hozAlign: "center", formatter: (c) => {
          const v = c.getValue();
          if (v === '입력') return '<span class="badge bg-primary">신규</span>';
          if (v === '수정') return '<span class="badge bg-warning text-dark">수정</span>';
          if (v === '삭제') return '<span class="badge bg-danger">삭제</span>';
          return '';
      }},
      { title: "자재명", field: "mitemnm", minWidth: 250, cellClick: (e, cell) => { if (!cell.getData().mitemcd) handleOpenHelp('MAT', cell.getRow()) } },
      { title: "규격", field: "mitsize", width: 180 },
      { title: "소요량", field: "soqty", width: 180, hozAlign: "right", editor: "number", cellEdited: (c) => markEdit(c.getRow()) }
    ]
  });
}

// [3] 비즈니스 로직
const convertToLower = (list: any[]) => list.map(i => {
  const obj: any = {};
  Object.keys(i).forEach(k => { obj[k.toLowerCase()] = i[k]; });
  return obj;
});

async function fetchList() {
  try {
    const res = await api.post('/hpio/HPIO_290U_STR', {
      actkind: 'L0',
      cmpycd: authStore.cmpycd,
      pumgbn: '200',
      fromdt: form_01.fromdt.replace(/-/g, ''),
      todt: form_01.todt.replace(/-/g, '')
    });
    console.log('res.data:', res.data);
    listGrid?.setData(convertToLower(res.data || []));
    vAlert('조회되었습니다.');
  } catch (e) { vAlertError('목록 조회 실패'); }
}

async function fetchDetail(row: any) {
  Object.assign(form_02, row);
  try {
    const resP = await api.post('/hpio/HPIO_291U_STR', {
        actkind: 'S0',
        cmpycd: authStore.cmpycd,
        pumym: form_02.pumym,
        pumno: form_02.pumno,
        price: 0
     });
    const data = convertToLower(resP.data || []).map(i => ({ ...i, _status: '' }));
    await prodGrid?.setData(data);

    if (data.length > 0) {
      // 🚀 데이터가 로드되면 즉시 첫 번째 제품의 자재를 조회
      const firstItem = data[0];
      selectedProduct.itemcd = firstItem.itemcd;
      selectedProduct.itemnm = firstItem.itemnm;

      fetchMaterials(firstItem, true);

      // UI 선택 표시
      nextTick(() => {
        const rows = prodGrid?.getRows();
        if (rows && rows.length > 0) rows[0].select();
      });
    } else {
      matGrid?.clearData();
      selectedProduct.itemcd = '';
      selectedProduct.itemnm = '';
    }
  } catch (e) {
    console.error('Detail Fetch Error:', e);
  }
}

async function fetchMaterials(prod: any, force = false) {
  if (!prod.itemcd || (!force && prod.itemcd === selectedProduct.itemcd)) return;

  selectedProduct.itemcd = prod.itemcd;
  selectedProduct.itemnm = prod.itemnm;

  if (form_02.pumno === '000' || !form_02.pumno) {
    matGrid?.clearData();
    return;
  }

  try {
    const resM = await api.post('/hpio/HPIO_292U_STR', {
        actkind: 'S',
        cmpycd: authStore.cmpycd,
        pumym: form_02.pumym,
        pumno: form_02.pumno,
        itemcd: prod.itemcd,
        mitemcd: '0', // 🚀 빈 문자열 대신 '0' 전달 (Numeric 변환 에러 방지)
        mitsize: '',
        munit: '',
        soqty: 0,
        remark: '',
        updemp: authStore.userid
     });
    matGrid?.setData(convertToLower(resM.data || []));
  } catch (e) {}
}

async function saveAll() {
  if (!form_02.custcd) return vAlertError('거래처를 선택하세요.');
  const prods = prodGrid?.getData().filter((r: any) => r._status) || [];
  const mats = matGrid?.getData().filter((r: any) => r._status) || [];

  if (form_02.pumno === '000' && prods.length === 0) {
    return vAlertError('저장할 제품 내역이 없습니다.');
  }

  if (!confirm('저장하시겠습니까?')) return

  try {
    // 🚀 [보정] 현재 선택된 제품 정보 보관
    const lastItemCd = selectedProduct.itemcd;
    const lastItemNm = selectedProduct.itemnm;

    const isNewContract = form_02.pumno === '000' || !form_02.pumno;

    // 1. 마스터 저장
    const resM = await api.post('/hpio/HPIO_290U_STR', {
      ...form_02,
      fromdt: form_01.fromdt.replace(/-/g, ''),
      todt: form_01.todt.replace(/-/g, ''),
      actkind: isNewContract ? 'A0' : 'U0',
      userid: authStore.userid
    });

    // 🚀 서버 응답에서 PUMYM, PUMNO 추출 (대소문자 무관하게 처리)
    let pumNo = '';
    let pumYm = '';
    let rtnMsg = '';

    const resData = resM.data;
    const resList = Array.isArray(resData) ? resData : (resData ? [resData] : []);

    if (resList.length > 0) {
      const firstRow = resList[0];
      for (const key in firstRow) {
        const lowerKey = key.toLowerCase();
        if (lowerKey === 'pumno') pumNo = String(firstRow[key]);
        if (lowerKey === 'pumym') pumYm = String(firstRow[key]);
        if (lowerKey === 'rtn_msg') rtnMsg = String(firstRow[key]);
      }
    }

    // 채번된 번호를 즉시 반영
    if (pumNo && pumNo !== '000') {
      form_02.pumno = pumNo;
      if (pumYm) form_02.pumym = pumYm;
    } else if (isNewContract) {
      return vAlertError('품의번호(PUMNO)가 생성되지 않았습니다. 관리자에게 문의하세요.');
    }

    const keyYM = form_02.pumym;
    const keyNO = form_02.pumno;

    if (!keyNO || keyNO === '000') {
      return vAlertError('품의번호가 유효하지 않습니다 (PUMNO: ' + keyNO + '). 저장을 중단합니다.');
    }

    // 2. 제품 저장
    for (const p of prods) {
      const pAct = p._status === '입력' ? 'A0' : (p._status === '삭제' ? 'D0' : 'U0');
      await api.post('/hpio/HPIO_291U_STR', {
        ...p,
        actkind: pAct,
        cmpycd: authStore.cmpycd,
        pumym: keyYM,
        pumno: keyNO,
        updemp: authStore.userid
      });
    }

    // 3. 자재 저장 (제품 코드 'lastItemCd' 등 필수자료 11개 매칭)
    for (const m of mats) {
      const mAct = m._status === '입력' ? 'A' : (m._status === '삭제' ? 'D' : 'U');
      await api.post('/hpio/HPIO_292U_STR', {
        actkind: mAct,
        cmpycd: authStore.cmpycd,
        pumym: keyYM,
        pumno: keyNO,
        itemcd: lastItemCd, // 상단에서 선택된 제품코드
        mitemcd: m.mitemcd,
        mitsize: m.mitsize || '',
        munit: m.munit || '',
        soqty: m.soqty || 0,
        remark: m.remark || '',
        updemp: authStore.userid
      });
    }

    vAlert(rtnMsg || '저장되었습니다.');

    // 4. 모든 저장이 끝난 후 최종 재조회 (등록된 데이터를 가져옴)
    await fetchList();
    await fetchDetail({ ...form_02 });

    if (lastItemCd) {
      fetchMaterials({ itemcd: lastItemCd, itemnm: lastItemNm }, true);
    }

  } catch (e: any) {
    console.error('Save Error:', e);
    vAlertError(e.message || '저장 중 오류가 발생했습니다.');
  }
}

const handleOpenHelp = (type: string, target?: any) => {
  const props: any = { title: '', path: '', data: { cmpycd: authStore.cmpycd }, columns: [], onConfirm: () => {} };
  if (type === 'DEPT') {
    props.title = '부서'; props.path = '/ha00/HA00_00P_STR'; props.data.gubun = 'D0';
    props.columns = [{ title: '코드', field: 'deptcd', width: 80 }, { title: '부서명', field: 'deptnm' }];
    props.onConfirm = (d: any) => { form_02.deptcd = d.deptcd; form_02.deptnm = d.deptnm };
  } else if (type === 'CUST') {
    props.title = '거래처'; props.path = '/ha00/HA00_00P_STR'; props.data.gubun = 'C4';
    props.columns = [{ title: '코드', field: 'custcd', width: 80 }, { title: '거래처명', field: 'custnm' }];
    props.onConfirm = (d: any) => { form_02.custcd = d.custcd; form_02.custnm = d.custnm };
  } else if (type === 'ITEM') {
    props.title = '제품 선택'; props.path = '/hp00/HP00_000S_STR';
    props.data.gubun = 'I1';
    props.data.gbncd = 'A';
    props.columns = [{ title: '코드', field: 'itemcd', width: 100 }, { title: '제품명', field: 'itemnm' }, { title: '규격', field: 'itsize' }];
    props.onConfirm = (d: any) => target.update({ itemcd: d.itemcd, itemnm: d.itemnm, itsize: d.itsize, unit: d.unit, _status: '입력' });
  } else if (type === 'MAT') {
    props.title = '자재 선택'; props.path = '/hp00/HP00_000S_STR';
    props.data.gubun = 'I0';
    props.data.gbncd = 'A';
    props.columns = [{ title: '코드', field: 'itemcd', width: 100 }, { title: '자재명', field: 'itemnm' }, { title: '규격', field: 'itsize' }];
    props.onConfirm = (d: any) => target.update({ mitemcd: d.itemcd, mitemnm: d.itemnm, mitsize: d.itsize, munit: d.unit, _status: '입력' });
  }
  Object.assign(modalProps, props); modalVisible.value = true;
}

const markEdit = (row: any) => { if (!row.getData()._status) row.update({ _status: '수정' }); }
const handleRowAction = (row: any) => {
  const d = row.getData();
  if (d._status === '입력') row.delete();
  else row.update({ _status: d._status === '삭제' ? '' : '삭제' });
}
const addRow = (type: string) => {
  if (type === 'PROD') prodGrid?.addRow({ price: 0, _status: '입력' }, true);
  else matGrid?.addRow({ soqty: 0, _status: '입력' }, true);
}
const deleteRows = (type: string) => {
  const g = type === 'PROD' ? prodGrid : matGrid;
  g?.getSelectedRows().forEach(r => handleRowAction(r));
}
const initialize = () => {
  resetForm(form_02);
  Object.assign(form_01, { fromdt: firstDay, todt: today, custnm: '' });
  Object.assign(form_02, {
      cmpycd: authStore.cmpycd,
      pumym: today.replace(/-/g, '').substring(0, 6),
      pumno: '000', pumymd: today.replace(/-/g, ''),
      frymd: today.replace(/-/g, ''),
      toymd: today.replace(/-/g, ''),
      deptcd: authStore.deptcd,
      deptnm: authStore.deptnm,
      pumgbn: '200'
  });
  prodGrid?.clearData(); matGrid?.clearData(); listGrid?.clearData();
}
const formatDate = (v: any) => v && v.length === 8 ? `${v.substring(0, 4)}-${v.substring(4, 6)}-${v.substring(6, 8)}` : v;
onMounted(() => nextTick(initGrids));
</script>

<style scoped>
.tabulator-instance { width: 100% !important; background-color: #fff; font-size: 12px; }
.erp-table-dense th { font-size: 11px; padding: 4px; }
</style>