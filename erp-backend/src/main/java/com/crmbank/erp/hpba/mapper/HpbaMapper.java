package com.crmbank.erp.hpba.mapper;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface HpbaMapper {
    List<Map<String, Object>> HPBA_100U_STR(Map<String, Object> params);
    List<Map<String, Object>> HPBA_130U_STR(Map<String, Object> params);
    List<Map<String, Object>> HPBA_200U_STR(Map<String, Object> params);
    List<Map<String, Object>> HPBA_210U_STR(Map<String, Object> params);
    List<Map<String, Object>> HPBA_800U_STR(Map<String, Object> params);
    List<Map<String, Object>> HPBA_810U_STR(Map<String, Object> params);
    List<Map<String, Object>> HPBA_820U_STR(Map<String, Object> params);
    List<Map<String, Object>> HPBA_830U_STR(Map<String, Object> params);
    List<Map<String, Object>> HPBA_840U_STR(Map<String, Object> params);
    List<Map<String, Object>> HPBA_900U_STR(Map<String, Object> params);
}
