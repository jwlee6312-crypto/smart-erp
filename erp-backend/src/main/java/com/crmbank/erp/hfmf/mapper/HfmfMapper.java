package com.crmbank.erp.hfmf.mapper;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface HfmfMapper {
    List<Map<String, Object>> FMF1020U_STR(Map<String, Object> params);
    List<Map<String, Object>> FMF1040U_STR(Map<String, Object> params);
    List<Map<String, Object>> FMF1050U_STR(Map<String, Object> params);
    List<Map<String, Object>> FMF2010U_STR(Map<String, Object> params);
    List<Map<String, Object>> FMF2060R_STR(Map<String, Object> params);
    List<Map<String, Object>> FMF2020U_STR(Map<String, Object> params);
    List<Map<String, Object>> FMF2070U_STR(Map<String, Object> params);
    List<Map<String, Object>> FMF2110U_STR(Map<String, Object> params);
    List<Map<String, Object>> FMF2120R_STR(Map<String, Object> params);
    List<Map<String, Object>> FMF2140R_STR(Map<String, Object> params);
    List<Map<String, Object>> FMF2150R_STR(Map<String, Object> params);
    List<Map<String, Object>> FMF2160R_STR(Map<String, Object> params);
    List<Map<String, Object>> FMF2180R_STR(Map<String, Object> params);
    List<Map<String, Object>> FMF2190R_STR(Map<String, Object> params);
    List<Map<String, Object>> FMF2210R_STR(Map<String, Object> params);
    List<Map<String, Object>> FMF3010U_STR(Map<String, Object> params);


}
