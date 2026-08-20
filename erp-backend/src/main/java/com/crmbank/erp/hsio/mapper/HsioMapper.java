package com.crmbank.erp.hsio.mapper;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface HsioMapper {
    // 🚀 DTO와 Map 모두 지원하기 위해 파라미터 타입을 Object로 통일합니다.
    List<Map<String, Object>> HSIO_010U_STR(Object params);
    List<Map<String, Object>> HSIO_011U_STR(Object params);
    List<Map<String, Object>> HSIO_020U_STR(Object params);
    List<Map<String, Object>> HSIO_021U_STR(Object params);
    List<Map<String, Object>> HSIO_050U_STR(Object params);
    List<Map<String, Object>> HSIO_051U_STR(Object params);
    List<Map<String, Object>> HSIO_052U_STR(Object params);
    List<Map<String, Object>> HSIO_060U_STR(Object params);
    List<Map<String, Object>> HSIO_061U_STR(Object params);
    List<Map<String, Object>> HSIO_070U_STR(Object params);
    List<Map<String, Object>> HSIO_080S_STR(Object params);
    List<Map<String, Object>> HSIO_082S_STR(Object params);
    List<Map<String, Object>> HSIO_085S_STR(Object params);
    List<Map<String, Object>> HSIO_100U_STR(Object params);
    List<Map<String, Object>> HSIO_101U_STR(Object params);
    List<Map<String, Object>> HSIO_110U_STR(Object params);
    List<Map<String, Object>> HSIO_120U_STR(Object params);
    List<Map<String, Object>> HSIO_130U_STR(Object params);
    List<Map<String, Object>> HSIO_131U_STR(Object params);
    List<Map<String, Object>> HSIO_140U_STR(Object params);
    List<Map<String, Object>> HSIO_141U_STR(Object params);
    List<Map<String, Object>> HSIO_160U_STR(Object params);
    List<Map<String, Object>> HSIO_170U_STR(Object params);
    List<Map<String, Object>> HSIO_171U_STR(Object params);
    List<Map<String, Object>> HSIO_180U_STR(Object params);
    List<Map<String, Object>> HSIO_181U_STR(Object params);
    List<Map<String, Object>> HSIO_190U_STR(Object params);
    List<Map<String, Object>> HSIO_191U_STR(Object params);
    List<Map<String, Object>> HSIO_200S_STR(Object params);
    List<Map<String, Object>> HSIO_210S_STR(Object params);
    List<Map<String, Object>> HSIO_215S_STR(Object params);
    List<Map<String, Object>> HSIO_220S_STR(Object params);
    List<Map<String, Object>> HSIO_250U_STR(Object params);
    List<Map<String, Object>> HSIO_251U_STR(Object params);
    List<Map<String, Object>> HSIO_300U_STR(Object params);
    List<Map<String, Object>> HSIO_301U_STR(Object params);
    List<Map<String, Object>> HSIO_320U_STR(Object params);
    List<Map<String, Object>> HSIO_325U_STR(Object params);
    List<Map<String, Object>> HSIO_410S_STR(Object params);
    List<Map<String, Object>> HSIO_470S_STR(Object params);
    List<Map<String, Object>> HSIO_490U_STR(Object params);
    List<Map<String, Object>> HSIO_491U_STR(Object params);
    List<Map<String, Object>> HSIO_500U_STR(Object params);
    List<Map<String, Object>> HSIO_501U_STR(Object params);
    List<Map<String, Object>> HSIO_510U_STR(Object params);
    List<Map<String, Object>> HSIO_511U_STR(Object params);
    List<Map<String, Object>> HSIO_520U_STR(Object params);
    List<Map<String, Object>> HSIO_521U_STR(Object params);
    List<Map<String, Object>> HSIO_530U_STR(Object params);
    List<Map<String, Object>> HSIO_531U_STR(Object params);
    List<Map<String, Object>> HSIO_540U_STR(Object params);
    List<Map<String, Object>> HSIO_541U_STR(Object params);
    List<Map<String, Object>> HSIO_550U_STR(Object params);
    List<Map<String, Object>> HSIO_551U_STR(Object params);
    List<Map<String, Object>> HSIO_560U_STR(Object params);
    List<Map<String, Object>> HSIO_570U_STR(Object params);
    List<Map<String, Object>> HSIO_571U_STR(Object params);
    List<Map<String, Object>> HSIO_580U_STR(Object params);
    List<Map<String, Object>> HSIO_581U_STR(Object params);
    List<Map<String, Object>> HSIO_590U_STR(Object params);
    List<Map<String, Object>> HSIO_600S_STR(Object params);
    List<Map<String, Object>> HSIO_610S_STR(Object params);
    List<Map<String, Object>> HSIO_620S_STR(Object params);
    List<Map<String, Object>> HSIO_640S_STR(Object params);
    List<Map<String, Object>> HSIO_650S_STR(Object params);
    List<Map<String, Object>> HSIO_660S_STR(Object params);
    List<Map<String, Object>> HSIO_680S_STR(Object params);
    List<Map<String, Object>> HSIO_690S_STR(Object params);
    List<Map<String, Object>> HSIO_720U_STR(Object params);
    List<Map<String, Object>> HSIO_721U_STR(Object params);
    List<Map<String, Object>> HSIO_730U_STR(Object params);
    List<Map<String, Object>> HSIO_731U_STR(Object params);

    List<Map<String, Object>> HSIO_990U_STR(Object params);

    List<Map<String, Object>> getSlipDetailsForTransfer(Map<String, Object> params);

    // 🚀 생산불출 연동 (사용자 정의 12파라미터)
    List<Map<String, Object>> HSIO_600U_STR(Map<String, Object> params);
}
