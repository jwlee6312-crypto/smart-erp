package com.crmbank.erp.hppl.mapper;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface HpplMapper {
    List<Map<String, Object>> HPPL_100U_STR(Map<String, Object> params);
    List<Map<String, Object>> HPPL_110S_STR(Map<String, Object> params);
    List<Map<String, Object>> HPPL_120U_STR(Map<String, Object> params);
    List<Map<String, Object>> HPPL_150U_STR(Map<String, Object> params);
}
