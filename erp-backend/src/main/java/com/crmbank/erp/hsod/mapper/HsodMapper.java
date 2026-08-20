package com.crmbank.erp.hsod.mapper;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface HsodMapper {
    List<Map<String, Object>> HSOD_100U_STR(Object params);
    List<Map<String, Object>> HSOD_101U_STR(Object params);
    List<Map<String, Object>> HSOD_110S_STR(Map<String, Object> params);
    List<Map<String, Object>> HSOD_120U_STR(Map<String, Object> params);
    List<Map<String, Object>> HSOD_200U_STR(Map<String, Object> params);
    List<Map<String, Object>> HSOD_210U_STR(Map<String, Object> params);
    List<Map<String, Object>> HSOD_300U_STR(Map<String, Object> params);
}
