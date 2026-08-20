package com.crmbank.erp.hpio.mapper;

import com.crmbank.erp.hpio.dto.PdInspReqDto;
import com.crmbank.erp.hpio.dto.PdInspRsltDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface PdInspRsltMapper {
    /**
     * 검사 의뢰 목록 조회 (좌측 그리드)
     */
    List<PdInspReqDto> getPdInspReqList(Map<String, Object> params);

    /**
     * 검사 결과 목록 조회 (우측 그리드)
     */
    List<PdInspRsltDto> getPdInspRsltList(Map<String, Object> params);

    /**
     * 검사 결과 저장 (Insert)
     */
    void insertPdInspRslt(PdInspRsltDto dto);

    /**
     * 검사 결과 수정 (Update)
     */
    void updatePdInspRslt(PdInspRsltDto dto);

    /**
     * 검사 결과 삭제
     */
    void deletePdInspRslt(Map<String, Object> params);
}
