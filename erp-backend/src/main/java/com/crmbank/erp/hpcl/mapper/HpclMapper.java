package com.crmbank.erp.hpcl.mapper;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface HpclMapper {
    List<Map<String, Object>> HPCL_100U_STR(Map<String, Object> params);
    List<Map<String, Object>> HPCL_110U_STR(Map<String, Object> params);
    List<Map<String, Object>> HPCL_200S_STR(Map<String, Object> params);
    List<Map<String, Object>> HPCL_210S_STR(Map<String, Object> params);
    List<Map<String, Object>> HPCL_220S_STR(Map<String, Object> params);
    List<Map<String, Object>> HPCL_230S_STR(Map<String, Object> params);
    List<Map<String, Object>> HPCL_240S_STR(Map<String, Object> params);
}
