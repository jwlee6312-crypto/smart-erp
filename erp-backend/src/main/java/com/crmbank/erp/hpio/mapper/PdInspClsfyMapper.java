package com.crmbank.erp.hpio.mapper;

import com.crmbank.erp.hpio.dto.PdInspClsfyDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface PdInspClsfyMapper {
    /**
     * 검사분류 항목 목록 조회
     */
    List<PdInspClsfyDto> getPdInspClsfyList(Map<String, Object> params);

    /**
     * 검사분류 항목 저장 (Insert)
     */
    void insertPdInspClsfy(PdInspClsfyDto dto);

    /**
     * 검사분류 항목 수정 (Update)
     */
    void updatePdInspClsfy(PdInspClsfyDto dto);

    /**
     * 검사분류 항목 삭제
     */
    void deletePdInspClsfy(Map<String, Object> params);
}
