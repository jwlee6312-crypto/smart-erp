package com.crmbank.erp.hasl.mapper;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface HaslMapper {
    List<Map<String, Object>> HASL_010U_STR(Map<String, Object> params);
    List<Map<String, Object>> HASL_011U_STR(Map<String, Object> params);
    List<Map<String, Object>> HASL_020U_STR(Map<String, Object> params);
    List<Map<String, Object>> HASL_110U_STR(Map<String, Object> params);
    List<Map<String, Object>> HASL_111U_STR(Map<String, Object> params);
    List<Map<String, Object>> HASL_030S_STR(Map<String, Object> params);
    List<Map<String, Object>> HASL_120S_STR(Map<String, Object> params);
    List<Map<String, Object>> HASL_130S_STR(Map<String, Object> params);
    List<Map<String, Object>> HASL_040S_STR(Map<String, Object> params);
    List<Map<String, Object>> HASL_050U_MASTER(Map<String, Object> params);
    List<Map<String, Object>> HASL_050U_STR(Map<String, Object> params);
    List<Map<String, Object>> HASL_510S_STR(Map<String, Object> params);
    List<Map<String, Object>> HASL_520S_STR(Map<String, Object> params);
    List<Map<String, Object>> HASL_530S_STR(Map<String, Object> params);
    List<Map<String, Object>> HASL_540S_STR(Map<String, Object> params);
    List<Map<String, Object>> HASL_550S_STR(Map<String, Object> params);
    List<Map<String, Object>> HASL_560S_STR(Map<String, Object> params);
    List<Map<String, Object>> HASL_610S_STR(Map<String, Object> params);
    List<Map<String, Object>> HASL_620S_STR(Map<String, Object> params);
    List<Map<String, Object>> HASL_630S_STR(Map<String, Object> params);
    List<Map<String, Object>> HASL_710S_STR(Map<String, Object> params);
}
