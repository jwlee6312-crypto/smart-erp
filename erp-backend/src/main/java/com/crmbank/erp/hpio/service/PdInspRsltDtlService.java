package com.crmbank.erp.hpio.service;

import com.crmbank.erp.hpio.dto.PdInspReqDto;
import com.crmbank.erp.hpio.dto.PdInspRsltDtlDto;
import com.crmbank.erp.hpio.dto.PdInspRsltDto;
import com.crmbank.erp.hpio.mapper.PdInspRsltDtlMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PdInspRsltDtlService {

    private final PdInspRsltDtlMapper pdInspRsltDtlMapper;

    /**
     * 검사 의뢰 목록 조회 (좌측 그리드)
     */
    public List<PdInspReqDto> getPdInspReqList(Map<String, Object> params) {
        return pdInspRsltDtlMapper.getPdInspReqList(params);
    }

    /**
     * 검사 결과 마스터 목록 조회 (중간 그리드)
     */
    public List<PdInspRsltDto> getPdInspRsltMstList(Map<String, Object> params) {
        return pdInspRsltDtlMapper.getPdInspRsltMstList(params);
    }

    /**
     * 검사 불량 상세 목록 조회 (우측 그리드)
     */
    public List<PdInspRsltDtlDto> getPdInspRsltDtlList(Map<String, Object> params) {
        return pdInspRsltDtlMapper.getPdInspRsltDtlList(params);
    }

    /**
     * 검사 불량 상세 삭제
     */
    @Transactional
    public void deletePdInspRsltDtl(Map<String, Object> params) {
        pdInspRsltDtlMapper.deletePdInspRsltDtl(params);
    }

    /**
     * 검사 불량 상세 일괄 저장 (Insert/Update/Delete)
     */
    @Transactional
    public void savePdInspRsltDtlList(List<PdInspRsltDtlDto> list, String cmpycd, String userId) {
        for (PdInspRsltDtlDto dto : list) {
            dto.setCmpycd(cmpycd);
            dto.setUpdemp(userId);

            if ("C".equals(dto.getState())) {
                // 일련번호 채번
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
                
                String newSer = pdInspRsltDtlMapper.selectNewInspSer(params);
                dto.setInsp_ser(newSer);
                
                pdInspRsltDtlMapper.insertPdInspRsltDtl(dto);
            } else if ("U".equals(dto.getState())) {
                pdInspRsltDtlMapper.updatePdInspRsltDtl(dto);
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
                params.put("insp_ser", dto.getInsp_ser());
                
                pdInspRsltDtlMapper.deletePdInspRsltDtl(params);
            }
        }
    }
}
