package com.crmbank.erp.hpio.service;

import com.crmbank.erp.hpio.dto.PdInspReqDto;
import com.crmbank.erp.hpio.dto.PdInspRsltDto;
import com.crmbank.erp.hpio.mapper.PdInspRsltMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PdInspRsltService {

    private final PdInspRsltMapper pdInspRsltMapper;

    /**
     * 검사 의뢰 목록 조회 (좌측 그리드)
     */
    public List<PdInspReqDto> getPdInspReqList(Map<String, Object> params) {
        return pdInspRsltMapper.getPdInspReqList(params);
    }

    /**
     * 검사 결과 목록 조회 (우측 그리드)
     */
    public List<PdInspRsltDto> getPdInspRsltList(Map<String, Object> params) {
        return pdInspRsltMapper.getPdInspRsltList(params);
    }

    /**
     * 검사 결과 일괄 저장 (Insert/Update/Delete)
     */
    @Transactional
    public void savePdInspRsltList(List<PdInspRsltDto> list, String cmpycd, String userId) {
        for (PdInspRsltDto dto : list) {
            dto.setCmpycd(cmpycd);
            dto.setUpdemp(userId);

            if ("C".equals(dto.getState())) {
                pdInspRsltMapper.insertPdInspRslt(dto);
            } else if ("U".equals(dto.getState())) {
                pdInspRsltMapper.updatePdInspRslt(dto);
            } else if ("D".equals(dto.getState())) {
                Map<String, Object> params = new HashMap<>();
                params.put("cmpycd", cmpycd);
                params.put("insp_gb", dto.getInsp_gb());
                params.put("insp_req_dt", dto.getInsp_req_dt());
                params.put("insp_req_no", dto.getInsp_req_no());
                params.put("linecd", dto.getLinecd());
                params.put("progcd", dto.getProgcd());
                params.put("ordymd", dto.getOrdymd());
                params.put("proymd", dto.getProymd());
                params.put("lotno", dto.getLotno());
                params.put("itemcd", dto.getItemcd());
                params.put("insp_ord", dto.getInsp_ord());
                params.put("inspcd", dto.getInspcd());
                
                this.deletePdInspRslt(params);
            }
        }
    }

    /**
     * 검사 결과 삭제
     */
    @Transactional
    public void deletePdInspRslt(Map<String, Object> params) {
        pdInspRsltMapper.deletePdInspRslt(params);
    }
}
