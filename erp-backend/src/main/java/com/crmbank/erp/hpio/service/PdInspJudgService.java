package com.crmbank.erp.hpio.service;

import com.crmbank.erp.hpio.dto.PdInspJudgDto;
import com.crmbank.erp.hpio.dto.PdInspReqDto;
import com.crmbank.erp.hpio.mapper.PdInspJudgMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PdInspJudgService {

    private final PdInspJudgMapper pdInspJudgMapper;

    /**
     * 검사 의뢰 목록 조회 (좌측 그리드)
     */
    public List<PdInspReqDto> getPdInspReqList(Map<String, Object> params) {
        return pdInspJudgMapper.getPdInspReqList(params);
    }

    /**
     * 부적합 처리 결과 목록 조회 (우측 그리드)
     */
    public List<PdInspJudgDto> getPdInspJudgList(Map<String, Object> params) {
        return pdInspJudgMapper.getPdInspJudgList(params);
    }

    /**
     * 부적합 처리 일괄 저장
     */
    @Transactional
    public void savePdInspJudgList(List<PdInspJudgDto> list, String cmpycd, String userId) {
        if (list == null || list.isEmpty()) return;

        // 1. 입고 여부 확인 (첫 번째 항목 기준)
        PdInspJudgDto first = list.get(0);
        Map<String, Object> checkParams = new HashMap<>();
        checkParams.put("cmpycd", cmpycd);
        checkParams.put("linecd", first.getLinecd());
        checkParams.put("progcd", first.getProgcd());
        checkParams.put("ordymd", first.getOrdymd());
        checkParams.put("proymd", first.getProymd());
        checkParams.put("lotno", first.getLotno());
        checkParams.put("itemcd", first.getItemcd());

        int ipgoCount = pdInspJudgMapper.selectIpgoCount(checkParams);
        if (ipgoCount > 0) {
            throw new RuntimeException("이미 생산입고 처리된 항목은 수정할 수 없습니다.");
        }

        // 2. 부적합 처리 내역 저장/수정/삭제
        for (PdInspJudgDto dto : list) {
            dto.setCmpycd(cmpycd);
            dto.setUpdemp(userId);

            if ("C".equals(dto.getState())) {
                String newSer = pdInspJudgMapper.selectNewRsltSer(Map.of(
                    "cmpycd", cmpycd, "insp_gb", dto.getInsp_gb(), "insp_req_dt", dto.getInsp_req_dt(),
                    "insp_req_no", dto.getInsp_req_no(), "linecd", dto.getLinecd(), "progcd", dto.getProgcd(),
                    "ordymd", dto.getOrdymd(), "proymd", dto.getProymd(), "lotno", dto.getLotno(), "itemcd", dto.getItemcd()
                ));
                dto.setRslt_ser(newSer);
                pdInspJudgMapper.insertPdInspJudg(dto);
            } else if ("U".equals(dto.getState())) {
                pdInspJudgMapper.updatePdInspJudg(dto);
            } else if ("D".equals(dto.getState())) {
                Map<String, Object> delParams = new HashMap<>();
                delParams.put("cmpycd", cmpycd);
                delParams.put("insp_gb", dto.getInsp_gb());
                delParams.put("insp_req_dt", dto.getInsp_req_dt());
                delParams.put("insp_req_no", dto.getInsp_req_no());
                delParams.put("linecd", dto.getLinecd());
                delParams.put("progcd", dto.getProgcd());
                delParams.put("ordymd", dto.getOrdymd());
                delParams.put("proymd", dto.getProymd());
                delParams.put("lotno", dto.getLotno());
                delParams.put("itemcd", dto.getItemcd());
                delParams.put("rslt_ser", dto.getRslt_ser());
                pdInspJudgMapper.deletePdInspJudg(delParams);
            }
        }

        // 3. 관련 수량 및 판정 결과 업데이트
        updateRelatedQuantities(first, cmpycd);
    }

    /**
     * 검사판정 삭제
     */
    @Transactional
    public void deletePdInspJudg(Map<String, Object> params) {
        // 입고 여부 확인
        int ipgoCount = pdInspJudgMapper.selectIpgoCount(params);
        if (ipgoCount > 0) {
            throw new RuntimeException("이미 생산입고 처리된 항목은 삭제할 수 없습니다.");
        }
        pdInspJudgMapper.deletePdInspJudg(params);
        
        // 수량 재계산이 필요한 경우를 위해 DTO 더미 생성 후 업데이트 호출 (필요시 상세 구현)
    }

    private void updateRelatedQuantities(PdInspJudgDto info, String cmpycd) {
        Map<String, Object> params = new HashMap<>();
        params.put("cmpycd", cmpycd);
        params.put("prodid", info.getProdid()); // 💡 생산실적 고유 ID 추가 (업데이트 핵심 키)
        params.put("insp_gb", info.getInsp_gb());
        params.put("insp_req_dt", info.getInsp_req_dt());
        params.put("insp_req_no", info.getInsp_req_no());
        params.put("linecd", info.getLinecd());
        params.put("progcd", info.getProgcd());
        params.put("ordymd", info.getOrdymd());
        params.put("proymd", info.getProymd());
        params.put("lotno", info.getLotno());
        params.put("itemcd", info.getItemcd());

        BigDecimal totalErrQty = pdInspJudgMapper.selectTotalRsltQty(params);
        BigDecimal godQty = info.getPrdqty().subtract(totalErrQty);
        
        params.put("godqty", godQty);
        params.put("errqty", totalErrQty);
        
        // 🚀 수정: 프론트엔드에서 전달된 판정결과(judg_cd)를 우선 사용하고, 없으면 자동 계산
        if (info.getJudg_cd() != null && !info.getJudg_cd().isEmpty()) {
            params.put("judg_cd", info.getJudg_cd());
        } else {
            params.put("judg_cd", totalErrQty.compareTo(BigDecimal.ZERO) > 0 ? "200" : "100");
        }

        pdInspJudgMapper.updatePdInspReqJudg(params);
        pdInspJudgMapper.updatePdProductionQty(params);
    }
}
