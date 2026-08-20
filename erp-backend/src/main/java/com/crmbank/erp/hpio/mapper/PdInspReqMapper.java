package com.crmbank.erp.hpio.mapper;

import com.crmbank.erp.hpio.dto.PdInspReqDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface PdInspReqMapper {
    /**
     * 제품검사의뢰 대상 조회 (생산실적 기반)
     */
    List<PdInspReqDto> getPdInspReqTargetList(Map<String, Object> params);

    /**
     * 제품검사의뢰 저장 (Insert)
     */
    void insertPdInspReq(PdInspReqDto dto);

    /**
     * 검사진행 여부 확인
     */
    int selectInspResultCount(Map<String, Object> params);

    /**
     * 제품검사의뢰 삭제 (의뢰 취소)
     */
    void deletePdInspReq(Map<String, Object> params);
}
