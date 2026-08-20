package com.crmbank.erp.hpio.mapper;

import com.crmbank.erp.hpio.dto.PdInspReqDto;
import com.crmbank.erp.hpio.dto.PdInspRsltDtlDto;
import com.crmbank.erp.hpio.dto.PdInspRsltDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface PdInspRsltDtlMapper {
    /**
     * 검사 의뢰 목록 조회 (좌측 그리드)
     */
    List<PdInspReqDto> getPdInspReqList(Map<String, Object> params);

    /**
     * 검사 결과 마스터 목록 조회 (중간 그리드)
     */
    List<PdInspRsltDto> getPdInspRsltMstList(Map<String, Object> params);

    /**
     * 검사 불량 상세 목록 조회 (우측 그리드)
     */
    List<PdInspRsltDtlDto> getPdInspRsltDtlList(Map<String, Object> params);

    /**
     * 불량 일련번호 채번
     */
    String selectNewInspSer(Map<String, Object> params);

    /**
     * 저장 (Insert)
     */
    void insertPdInspRsltDtl(PdInspRsltDtlDto dto);

    /**
     * 수정 (Update)
     */
    void updatePdInspRsltDtl(PdInspRsltDtlDto dto);

    /**
     * 삭제 (전체 또는 개별)
     */
    void deletePdInspRsltDtl(Map<String, Object> params);
}
