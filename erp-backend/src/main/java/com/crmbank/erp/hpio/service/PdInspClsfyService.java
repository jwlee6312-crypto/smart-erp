package com.crmbank.erp.hpio.service;

import com.crmbank.erp.hpio.dto.PdInspClsfyDto;
import com.crmbank.erp.hpio.mapper.PdInspClsfyMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PdInspClsfyService {

    private final PdInspClsfyMapper pdInspClsfyMapper;

    /**
     * 검사분류 항목 목록 조회
     */
    public List<PdInspClsfyDto> getPdInspClsfyList(Map<String, Object> params) {
        return pdInspClsfyMapper.getPdInspClsfyList(params);
    }

    /**
     * 검사분류 항목 저장 (등록/수정/삭제 통합)
     */
    @Transactional
    public void savePdInspClsfy(List<PdInspClsfyDto> dtoList, String cmpycd, String userId) {
        for (PdInspClsfyDto dto : dtoList) {
            dto.setCmpycd(cmpycd);
            dto.setUpdemp(userId);

            if ("C".equals(dto.getState())) {
                pdInspClsfyMapper.insertPdInspClsfy(dto);
            } else if ("U".equals(dto.getState())) {
                pdInspClsfyMapper.updatePdInspClsfy(dto);
            } else if ("D".equals(dto.getState())) {
                Map<String, Object> params = new HashMap<>();
                params.put("cmpycd", cmpycd);
                params.put("insp_gb", dto.getInsp_gb());
                params.put("inspcd", dto.getInspcd());
                pdInspClsfyMapper.deletePdInspClsfy(params);
            }
        }
    }
}
