package com.crmbank.erp.hsst.mapper;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface HsstMapper {

    List<Map<String, Object>> HSST_100S_STR(Map<String, Object> params);
    List<Map<String, Object>> HSST_120S_STR(Map<String, Object> params);
    List<Map<String, Object>> HSST_130S_STR(Map<String, Object> params);
    List<Map<String, Object>> HSST_150S_STR(Map<String, Object> params);
    List<Map<String, Object>> HSST_180S_STR(Map<String, Object> params);
    List<Map<String, Object>> HSST_200S_STR(Map<String, Object> params);
    List<Map<String, Object>> HSST_210S_STR(Map<String, Object> params);
    List<Map<String, Object>> HSST_300S_STR(Map<String, Object> params);
    List<Map<String, Object>> HSST_320S_STR(Map<String, Object> params);
    List<Map<String, Object>> HSST_340S_STR(Map<String, Object> params);
    List<Map<String, Object>> HSST_360S_STR(Map<String, Object> params);
    List<Map<String, Object>> HSST_510S_STR(Map<String, Object> params);
    List<Map<String, Object>> HSST_520S_STR(Map<String, Object> params);
    List<Map<String, Object>> HSST_570S_STR(Map<String, Object> params);
    List<Map<String, Object>> HSST_600S_STR(Map<String, Object> params);
    List<Map<String, Object>> HSST_610S_STR(Map<String, Object> params);

}
