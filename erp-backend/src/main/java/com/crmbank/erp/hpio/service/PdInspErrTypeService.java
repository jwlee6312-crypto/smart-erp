package com.crmbank.erp.hpio.service;

import com.crmbank.erp.hpio.dto.PdInspClsfyDto;
import com.crmbank.erp.hpio.dto.PdInspErrTypeDto;
import com.crmbank.erp.hpio.mapper.PdInspErrTypeMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PdInspErrTypeService {

    private final PdInspErrTypeMapper pdInspErrTypeMapper;

    /**
     * 검사항목 목록 조회 (좌측 그리드용)
     */
    public List<PdInspClsfyDto> getPdInspClsfyList(Map<String, Object> params) {
        return pdInspErrTypeMapper.getPdInspClsfyList(params);
    }

    /**
     * 검사항목별 불량유형 목록 조회 (우측 그리드용)
     */
    public List<PdInspErrTypeDto> getPdInspErrTypeList(Map<String, Object> params) {
        return pdInspErrTypeMapper.getPdInspErrTypeList(params);
    }

    /**
     * 불량유형 일괄 저장 (Insert/Delete)
     * 불량유형 관리는 일반적으로 추가/삭제로 이루어지므로 수정(Update) 로직은 생략하거나 필요시 추가합니다.
     */
    @Transactional
    public void savePdInspErrTypeList(List<PdInspErrTypeDto> list, String cmpycd, String userId) {
        for (PdInspErrTypeDto dto : list) {
            dto.setCmpycd(cmpycd);
            dto.setUpdemp(userId);

            if ("C".equals(dto.getState())) {
                // 중복 체크 후 저장
                Map<String, Object> checkParams = new HashMap<>();
                checkParams.put("cmpycd", cmpycd);
                checkParams.put("insp_gb", dto.getInsp_gb());
                checkParams.put("inspcd", dto.getInspcd());
                checkParams.put("err_type", dto.getErr_type());
                
                int count = pdInspErrTypeMapper.selectPdInspErrTypeCnt(checkParams);
                if (count == 0) {
                    pdInspErrTypeMapper.insertPdInspErrType(dto);
                }
            } else if ("D".equals(dto.getState())) {
                Map<String, Object> deleteParams = new HashMap<>();
                deleteParams.put("cmpycd", cmpycd);
                deleteParams.put("insp_gb", dto.getInsp_gb());
                deleteParams.put("inspcd", dto.getInspcd());
                deleteParams.put("err_type", dto.getErr_type());
                pdInspErrTypeMapper.deletePdInspErrType(deleteParams);
            }
        }
    }
}
