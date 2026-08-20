package com.crmbank.erp.hsip.mapper;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface HsipMapper {
    // 수입 발주 (통합 저장용 Object 지원)
    List<Map<String, Object>> HSIP_100U_STR(Object params);
    List<Map<String, Object>> HSIP_101U_STR(Object params);
    
    // 선적 통지
    List<Map<String, Object>> HSIP_110U_STR(Map<String, Object> params);
    List<Map<String, Object>> HSIP_111U_STR(Map<String, Object> params);
    List<Map<String, Object>> HSIP_112U_STR(Map<String, Object> params);
    
    // 통관 입고 (DTO 지원을 위해 Object로 변경)
    List<Map<String, Object>> HSIP_120U_STR(Object params);
    List<Map<String, Object>> HSIP_121U_STR(Object params);
    List<Map<String, Object>> HSIP_122U_STR(Map<String, Object> params);
    
    // 수입 비용
    List<Map<String, Object>> HSIP_130U_STR(Map<String, Object> params);
    List<Map<String, Object>> HSIP_131U_STR(Map<String, Object> params);
    
    // 전표 및 정산
    List<Map<String, Object>> HSIP_140U_STR(Map<String, Object> params);
    List<Map<String, Object>> HSIP_145U_STR(Map<String, Object> params);
    List<Map<String, Object>> HSIP_150U_STR(Map<String, Object> params);
    List<Map<String, Object>> HSIP_155U_STR(Map<String, Object> params);
    
    // 비용 배부 및 원가
    List<Map<String, Object>> HSIP_160U_STR(Map<String, Object> params);
    List<Map<String, Object>> HSIP_161S_STR(Map<String, Object> params);
    List<Map<String, Object>> HSIP_180U_STR(Map<String, Object> params);
    
    // 현황 조회
    List<Map<String, Object>> HSIP_200S_STR(Map<String, Object> params);
    List<Map<String, Object>> HSIP_210S_STR(Map<String, Object> params);
}
