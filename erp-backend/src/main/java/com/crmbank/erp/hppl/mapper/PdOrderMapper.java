package com.crmbank.erp.hppl.mapper;

import com.crmbank.erp.hppl.dto.PdOrderDto;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface PdOrderMapper {
    /**
     * 작업지시 대상(계획) 조회
     */
    List<PdOrderDto> getPdOrderTargetList(Map<String, Object> params);

    /**
     * 작업지시 상세 목록 조회
     */
    List<PdOrderDto> getPdOrderList(Map<String, Object> params);

    /**
     * 신규 LOT 번호 채번
     */
    String selectNewLotNo(Map<String, Object> params);

    /**
     * 작업지시 상세 저장
     */
    void insertPdOrder(PdOrderDto dto);

    /**
     * 작업지시 마스터(LOT) 저장
     */
    void insertPdLotNo(PdOrderDto dto);

    /**
     * 작업지시 상세 수정
     */
    void updatePdOrder(PdOrderDto dto);

    /**
     * 작업지시 마스터(LOT) 수정
     */
    void updatePdLotNo(PdOrderDto dto);

    /**
     * 계획 테이블 지시수량 업데이트
     */
    void updatePlanOrdQty(Map<String, Object> params);

    /**
     * 작업지시 삭제
     */
    void deletePdOrder(PdOrderDto dto);

    /**
     * 작업지시 마스터 삭제
     */
    void deletePdLotNo(PdOrderDto dto);

    /**
     * 특정 지시 데이터 단건 조회
     */
    PdOrderDto selectPdOrderOne(Map<String, Object> params);

    /**
     * 생산 완료 수량 조회 (삭제 체크용)
     */
    java.math.BigDecimal selectProdQty(Map<String, Object> params);
}
