package com.crmbank.erp.hacl.mapper;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface HaclMapper {

    List<Map<String, Object>> HACL_010S_STR(Map<String, Object> params);
    List<Map<String, Object>> HACL_020S_STR(Map<String, Object> params);
    List<Map<String, Object>> HACL_030S_STR(Map<String, Object> params);
    List<Map<String, Object>> HACL_040S_STR(Map<String, Object> params);
    List<Map<String, Object>> HACL_050S_STR(Map<String, Object> params);
    List<Map<String, Object>> HACL_060S_STR(Map<String, Object> params);
    List<Map<String, Object>> HACL_070S_STR(Map<String, Object> params);
    List<Map<String, Object>> HACL_080S_STR(Map<String, Object> params);

    /**
     * 월 마감작업 처리 및 오류 전표 조회 (HACL_800U_STR)
     */
    List<Map<String, Object>> HACL_800U_STR(Map<String, Object> params);

    /**
     * 차기이월 작업 처리 (HACL_900U_STR)
     */
    List<Map<String, Object>> HACL_900U_STR(Map<String, Object> params);
}
