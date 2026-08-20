package com.crmbank.erp.hafa.mapper;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface HafaMapper {
    /**
     * 자산이력관리 조회 및 처리 (HAFA_010U_STR)
     */
    List<Map<String, Object>> HAFA_010U_STR(Map<String, Object> params);

    /**
     * 자산이력조회 (HAFA_020S_STR)
     */
    List<Map<String, Object>> HAFA_020S_STR(Map<String, Object> params);

    /**
     * 고정자산대장 조회 (HAFA_040S_STR)
     */
    List<Map<String, Object>> HAFA_040S_STR(Map<String, Object> params);

    /**
     * 고정자산관리 조회 및 처리 (HAFA_050U_STR)
     */
    List<Map<String, Object>> HAFA_050U_STR(Map<String, Object> params);

    /**
     * 감가상각계산 처리 (HAFA_090U_STR)
     */
    List<Map<String, Object>> HAFA_090U_STR(Map<String, Object> params);

    /**
     * 감가상각명세서 조회 (HAFA_120S_STR)
     */
    List<Map<String, Object>> HAFA_120S_STR(Map<String, Object> params);

    /**
     * 감가상각집계표 조회 (HAFA_130S_STR)
     */
    List<Map<String, Object>> HAFA_130S_STR(Map<String, Object> params);

    /**
     * 월별 감가상각명세서 헤더 조회 (HA00_150S_STR)
     */
    List<Map<String, Object>> HA00_150S_STR(Map<String, Object> params);

    /**
     * 월별 감가상각명세서 조회 (HAFA_140S_STR)
     */
    List<Map<String, Object>> HAFA_140S_STR(Map<String, Object> params);

    /**
     * 감가상각전표 대상 조회 (HAFA_150U_STR)
     */
    List<Map<String, Object>> HAFA_150U_STR(Map<String, Object> params);

    /**
     * 감가상각계정과목 설정 (HAFA_900U_STR)
     */
    List<Map<String, Object>> HAFA_900U_STR(Map<String, Object> params);

}
