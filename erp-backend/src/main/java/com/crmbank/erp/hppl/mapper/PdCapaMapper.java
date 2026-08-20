package com.crmbank.erp.hppl.mapper;

import com.crmbank.erp.hppl.dto.PdCapaDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface PdCapaMapper {
    /**
     * 제품 품목 목록 조회 (좌측 그리드)
     */
    List<Map<String, Object>> getProductItemList(Map<String, Object> params);

    /**
     * 제품별 표준공정도 목록 조회 (우측 그리드)
     */
    List<PdCapaDto> getProductCapaList(Map<String, Object> params);

    /**
     * 특정 공정의 CAPA 정보 조회
     */
    PdCapaDto selectPdCapa(Map<String, Object> params);

    /**
     * 표준공정 존재 여부 확인 (Upsert용)
     */
    int selectPdCapaCnt(Map<String, Object> params);

    /**
     * 표준공정 저장 (Insert)
     */
    void insertPdCapa(PdCapaDto dto);

    /**
     * 표준공정 수정 (Update)
     */
    void updatePdCapa(PdCapaDto dto);

    /**
     * 표준공정 삭제
     */
    void deletePdCapa(Map<String, Object> params);
}
