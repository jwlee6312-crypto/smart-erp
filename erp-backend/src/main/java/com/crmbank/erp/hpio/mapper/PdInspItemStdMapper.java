package com.crmbank.erp.hpio.mapper;

import com.crmbank.erp.hpio.dto.PdInspItemStdDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface PdInspItemStdMapper {
    /**
     * 품목별 검사항목 목록 조회
     */
    List<PdInspItemStdDto> getPdInspItemStdList(Map<String, Object> params);

    /**
     * 데이터 존재 여부 확인
     */
    int selectPdInspItemStdCnt(Map<String, Object> params);

    /**
     * 품목별 검사항목 저장 (Insert)
     */
    void insertPdInspItemStd(PdInspItemStdDto dto);

    /**
     * 품목별 검사항목 수정 (Update)
     */
    void updatePdInspItemStd(PdInspItemStdDto dto);

    /**
     * 품목별 검사항목 삭제
     */
    void deletePdInspItemStd(Map<String, Object> params);
}
