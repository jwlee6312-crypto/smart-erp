package com.crmbank.erp.hppl.mapper;

import com.crmbank.erp.hppl.dto.PdPlanDto;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface PdPlanMapper {
    /**
     * 양산/외주 요청 자료 조회 (전체 내역)
     */
    List<PdPlanDto> getPdRequestList(Map<String, Object> params);

    void insertPdRequest(PdPlanDto dto);
    void updatePdRequest(PdPlanDto dto);
    void deletePdRequest(PdPlanDto dto);

    /**
     * 주간생산계획 대상(주문/양산) 조회
     */
    List<PdPlanDto> getPdPlanTargetList(Map<String, Object> params);

    /**
     * 주간생산계획 확정 목록 조회
     */
    List<PdPlanDto> getPdPlanList(Map<String, Object> params);

    /**
     * 월간 생산계획 조정용 목록 조회
     */
    List<PdPlanDto> getPdPlanMonthlyList(Map<String, Object> params);

    /**
     * 개별 계획 조회 (이동 전 데이터 복사용)
     */
    PdPlanDto selectPdPlan(Map<String, Object> params);

    /**
     * 계획 저장 (Insert)
     */
    void insertPdPlan(PdPlanDto dto);

    /**
     * 개별 계획 단건 저장 (Insert Single)
     */
    void insertPdPlanSingle(PdPlanDto dto);

    /**
     * 계획 수정 (Update)
     */
    void updatePdPlan(PdPlanDto dto);

    /**
     * 지시수량 확인
     */
    BigDecimal selectOrdQty(Map<String, Object> params);

    /**
     * 삭제
     */
    void deletePdPlan(Map<String, Object> params);

    /**
     * 다음 순번 조회
     */
    String selectNextSer(Map<String, Object> params);

    /**
     * 등록자료 존재 확인
     */
    int selectPdPlanCount(Map<String, Object> params);

    /**
     * 휴일 정보 조회
     */
    Map<String, Object> selectHolidayInfo(Map<String, Object> params);

    /**
     * 품목/라인별 BOM 및 공정 정보 통합 조회
     */
    List<Map<String, Object>> selectBomAndRouting(Map<String, Object> params);
    /**
     * 월간 날짜별 Capa 및 계획 요약 조회
     */
    List<Map<String, Object>> getPdPlanMonthlySummary(Map<String, Object> params);
}
