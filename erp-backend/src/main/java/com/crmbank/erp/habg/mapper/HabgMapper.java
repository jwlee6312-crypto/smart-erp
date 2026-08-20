package com.crmbank.erp.habg.mapper;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface HabgMapper {
    // 예산신청 조회 및 처리 (HABG_010U)
    List<Map<String, Object>> HABG_010U_STR(Map<String, Object> params);

    // 예산신청서 조회 (HABG_020S)
    List<Map<String, Object>> HABG_020S_STR(Map<String, Object> params);

    // 예산조정 조회 및 처리 (HABG_030U)
    List<Map<String, Object>> HABG_030U_STR(Map<String, Object> params);

    // 예산배정 조회 및 처리 (HABG_050U)
    List<Map<String, Object>> HABG_050U_STR(Map<String, Object> params);

    // 예산일괄배정 처리 (HABG_060U)
    List<Map<String, Object>> HABG_060U_STR(Map<String, Object> params);

    // 예산배정서 조회 (HABG_070S)
    List<Map<String, Object>> HABG_070S_STR(Map<String, Object> params);

    // 추가/조정신청 조회 및 처리 (HABG_110U)
    List<Map<String, Object>> HABG_110U_STR(Map<String, Object> params);

    // 추가/조정신청 처리 (HABG_120U)
    List<Map<String, Object>> HABG_120U_STR(Map<String, Object> params);

    // 예산현황 조회 (HABG_210S)
    List<Map<String, Object>> HABG_210S_STR(Map<String, Object> params);

    // 예산상세현황 조회 (HABG_220S)
    List<Map<String, Object>> HABG_220S_STR(Map<String, Object> params);

    // 예산실적서 조회 (HABG_230S)
    List<Map<String, Object>> HABG_230S_STR(Map<String, Object> params);
}
