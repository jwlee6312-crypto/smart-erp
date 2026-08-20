package com.crmbank.erp.hscl.mapper;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface HsclMapper {
    List<Map<String, Object>> HSCL_200S_STR(Map<String, Object> params);
    List<Map<String, Object>> HSCL_100U_STR(Map<String, Object> params);
    List<Map<String, Object>> HSCL_110U_STR(Map<String, Object> params);
    List<Map<String, Object>> HSCL_115U_STR(Map<String, Object> params);
    List<Map<String, Object>> HSCL_210S_STR(Map<String, Object> params);
    List<Map<String, Object>> HSCL_220S_STR(Map<String, Object> params);
    List<Map<String, Object>> HSCL_270S_STR(Map<String, Object> params);
    List<Map<String, Object>> HSCL_290S_STR(Map<String, Object> params);
    List<Map<String, Object>> HSCL_310S_STR(Map<String, Object> params);
    List<Map<String, Object>> HSCL_520S_STR(Map<String, Object> params);
}
