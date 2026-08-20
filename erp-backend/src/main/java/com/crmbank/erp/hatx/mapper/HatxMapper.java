package com.crmbank.erp.hatx.mapper;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface HatxMapper {
    /**
     * 매입부가세관리 조회 및 처리 (HATX_010U_STR)
     */
    List<Map<String, Object>> HATX_010U_STR(Map<String, Object> params);

    /**
     * 매입부가세 품목 상세 처리 (HATX_011U_STR)
     */
    List<Map<String, Object>> HATX_011U_STR(Map<String, Object> params);

    /**
     * 매입장 조회 (HATX_030S_STR)
     */
    List<Map<String, Object>> HATX_030S_STR(Map<String, Object> params);

    /**
     * 매출장 조회 (HATX_040S_STR)
     */
    List<Map<String, Object>> HATX_040S_STR(Map<String, Object> params);


    /**
     * 세금계산서합계표 조회 (HATX_110S_STR)
     */
    List<Map<String, Object>> HATX_110S_STR(Map<String, Object> params);

    /**
     * 신용카드매출전표수취명세서 조회 (HATX_130S_STR)
     */
    List<Map<String, Object>> HATX_130S_STR(Map<String, Object> params);

    /**
     * 신용카드매출전표등발행금액집계표 조회 (HATX_140S_STR)
     */
    List<Map<String, Object>> HATX_140S_STR(Map<String, Object> params);

    List<Map<String, Object>> HATX_150S_STR(Map<String, Object> params);
    List<Map<String, Object>> HATX_160S_STR(Map<String, Object> params);

    List<Map<String, Object>> HATX_170S_STR(Map<String, Object> params);

    List<Map<String, Object>> HATX_210U_STR(Map<String, Object> params);


    /**
     * 수출실적명세서 조회 (HATX_600S_STR)
     */
    List<Map<String, Object>> HATX_600S_STR(Map<String, Object> params);

    /**
     * 전표 마스터 처리 (HATX_01AU_STR)
     */
    List<Map<String, Object>> HATX_01AU_STR(Map<String, Object> params);

    /**
     * 전표 상세 처리 (HATX_01BU_STR)
     */
    List<Map<String, Object>> HATX_01BU_STR(Map<String, Object> params);

    /**
     * 매출부가세접수 처리 (HATX_060U_STR)
     */
    List<Map<String, Object>> HATX_060U_STR(Map<String, Object> params);

    /**
     * 전자세금계산서발송 처리 (HATX_080U_STR)
     */
    List<Map<String, Object>> HATX_080U_STR(Map<String, Object> params);

    /**
     * 부가세전산매체 생성 (HATX_050U_STR)
     */
    List<Map<String, Object>> HATX_050U_STR(Map<String, Object> params);

    /**
     * 접대비명세서 조회 (HATX_500S_STR)
     */
    List<Map<String, Object>> HATX_500S_STR(Map<String, Object> params);


}
