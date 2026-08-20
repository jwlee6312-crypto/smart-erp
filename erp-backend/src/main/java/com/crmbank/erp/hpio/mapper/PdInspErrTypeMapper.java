package com.crmbank.erp.hpio.mapper;

import com.crmbank.erp.hpio.dto.PdInspClsfyDto;
import com.crmbank.erp.hpio.dto.PdInspErrTypeDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface PdInspErrTypeMapper {
    /**
     * 검사항목 목록 조회 (좌측 그리드용)
     */
    List<PdInspClsfyDto> getPdInspClsfyList(Map<String, Object> params);

    /**
     * 검사항목별 불량유형 목록 조회 (우측 그리드용)
     */
    List<PdInspErrTypeDto> getPdInspErrTypeList(Map<String, Object> params);

    /**
     * 불량유형 등록 여부 확인
     */
    int selectPdInspErrTypeCnt(Map<String, Object> params);

    /**
     * 불량유형 저장 (Insert)
     */
    void insertPdInspErrType(PdInspErrTypeDto pdInspErrTypeDto);

    /**
     * 불량유형 삭제
     */
    void deletePdInspErrType(Map<String, Object> params);
}
