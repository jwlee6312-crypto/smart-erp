package com.crmbank.erp.hpio.service;

import com.crmbank.erp.hpio.dto.PdInspReqDto;
import com.crmbank.erp.hpio.mapper.PdInspReqMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PdInspReqService {

    private final PdInspReqMapper pdInspReqMapper;

    /**
     * 제품검사의뢰 대상 조회 (생산실적 기반)
     */
    public List<PdInspReqDto> getPdInspReqTargetList(Map<String, Object> params) {
        return pdInspReqMapper.getPdInspReqTargetList(params);
    }

    /**
     * 제품검사의뢰 일괄 저장
     */
    @Transactional
    public void savePdInspReqList(List<PdInspReqDto> list, String cmpycd, String userId) {
        for (PdInspReqDto dto : list) {
            dto.setCmpycd(cmpycd);
            dto.setUpdemp(userId);

            if ("C".equals(dto.getState())) {
                pdInspReqMapper.insertPdInspReq(dto);
            } else if ("D".equals(dto.getState())) {
                Map<String, Object> params = new HashMap<>();
                params.put("cmpycd", cmpycd);
                params.put("insp_req_dt", dto.getInsp_req_dt());
                params.put("insp_req_no", dto.getInsp_req_no());
                this.deletePdInspReq(params);
            }
        }
    }

    /**
     * 제품검사의뢰 삭제
     */
    @Transactional
    public void deletePdInspReq(Map<String, Object> params) {
        // 검사진행 여부 확인
        int count = pdInspReqMapper.selectInspResultCount(params);
        if (count > 0) {
            throw new RuntimeException("이미 검사가 진행된 항목은 취소할 수 없습니다. (의뢰번호: " + params.get("insp_req_no") + ")");
        }
        pdInspReqMapper.deletePdInspReq(params);
    }
}
