package com.crmbank.erp.hafn.mapper;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface HafnMapper {
    List<Map<String, Object>> HAFN_010S_STR(Map<String, Object> params);
    List<Map<String, Object>> HAFN_110S_STR(Map<String, Object> params);
    List<Map<String, Object>> HAFN_120S_STR(Map<String, Object> params);
    List<Map<String, Object>> HAFN_210S_STR(Map<String, Object> params);
    List<Map<String, Object>> HAFN_310S_STR(Map<String, Object> params);
    List<Map<String, Object>> HAFN_410S_STR(Map<String, Object> params);
    List<Map<String, Object>> HAFN_420S_STR(Map<String, Object> params);
    List<Map<String, Object>> HAFN_430S_STR(Map<String, Object> params);
    List<Map<String, Object>> HAFN_510S_STR(Map<String, Object> params);
    List<Map<String, Object>> HAFN_520S_STR(Map<String, Object> params);
    List<Map<String, Object>> HAFN_610U_STR(Map<String, Object> params);
    List<Map<String, Object>> HAFN_620U_STR(Map<String, Object> params);
    List<Map<String, Object>> HAFN_630U_STR(Map<String, Object> params);
    List<Map<String, Object>> HAFN_670S_STR(Map<String, Object> params);
    List<Map<String, Object>> HAFN_680S_STR(Map<String, Object> params);
    List<Map<String, Object>> HAFN_690S_STR(Map<String, Object> params);
}
