package com.crmbank.erp.hapl.mapper;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface HaplMapper {
    List<Map<String, Object>> HAPL_010U_STR(Map<String, Object> params);
    List<Map<String, Object>> HAPL_020U_STR(Map<String, Object> params);
    List<Map<String, Object>> HAPL_030U_STR(Map<String, Object> params);
    List<Map<String, Object>> HAPL_040U_STR(Map<String, Object> params);
    List<Map<String, Object>> HAPL_050U_STR(Map<String, Object> params);
    List<Map<String, Object>> HAPL_100U_STR(Map<String, Object> params);
    List<Map<String, Object>> HAPL_110S_STR(Map<String, Object> params);
    List<Map<String, Object>> HAPL_110S_INIT(Map<String, Object> params);
    List<Map<String, Object>> HAPL_120S_STR(Map<String, Object> params);
    List<Map<String, Object>> HAPL_140S_STR(Map<String, Object> params);
    List<Map<String, Object>> HAPL_200U_STR(Map<String, Object> params);
    List<Map<String, Object>> HAPL_210S_STR(Map<String, Object> params);
    List<Map<String, Object>> HAPL_220S_STR(Map<String, Object> params);
}
