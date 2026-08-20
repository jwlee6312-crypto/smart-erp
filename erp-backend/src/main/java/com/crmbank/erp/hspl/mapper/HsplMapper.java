package com.crmbank.erp.hspl.mapper;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface HsplMapper {
    List<Map<String, Object>> HSPL_100U_STR(Map<String, Object> params);
    List<Map<String, Object>> HSPL_110U_STR(Map<String, Object> params);
    List<Map<String, Object>> HSPL_200S_STR(Map<String, Object> params);
    List<Map<String, Object>> HSPL_210S_STR(Map<String, Object> params);
    List<Map<String, Object>> HSPL_220S_STR(Map<String, Object> params);
}
