package com.crmbank.erp.hpio.mapper;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface HpioMapper {
    List<Map<String, Object>> HPIO_110U_STR(Map<String, Object> params);
    List<Map<String, Object>> HPIO_200U_STR(Map<String, Object> params);
    List<Map<String, Object>> HPIO_210U_STR(Map<String, Object> params);
    List<Map<String, Object>> HPIO_230S_STR(Map<String, Object> params);
    List<Map<String, Object>> HPIO_250U_STR(Map<String, Object> params);
    List<Map<String, Object>> HPIO_251U_STR(Map<String, Object> params);
    List<Map<String, Object>> HPIO_290U_STR(Map<String, Object> params);
    List<Map<String, Object>> HPIO_291U_STR(Map<String, Object> params);
    List<Map<String, Object>> HPIO_292U_STR(Map<String, Object> params);
    List<Map<String, Object>> HPIO_300U_STR(Map<String, Object> params);
    List<Map<String, Object>> HPIO_301U_STR(Map<String, Object> params);
    List<Map<String, Object>> HPIO_340U_STR(Map<String, Object> params);

    List<Map<String, Object>> HPIO_340U_POPUP(Map<String, Object> params);
    List<Map<String, Object>> HPIO_341U_STR(Map<String, Object> params);
    List<Map<String, Object>> HPIO_350U_STR(Map<String, Object> params);
    List<Map<String, Object>> HPIO_351U_STR(Map<String, Object> params);
    List<Map<String, Object>> HPIO_360S_STR(Map<String, Object> params);
    List<Map<String, Object>> HPIO_370S_STR(Map<String, Object> params);
    List<Map<String, Object>> HPIO_380S_STR(Map<String, Object> params);
    List<Map<String, Object>> HPIO_390S_STR(Map<String, Object> params);
    List<Map<String, Object>> HPIO_400U_STR(Map<String, Object> params);
    List<Map<String, Object>> HPIO_410U_STR(Map<String, Object> params);
    List<Map<String, Object>> HPIO_420S_STR(Map<String, Object> params);
    List<Map<String, Object>> HPIO_430S_STR(Map<String, Object> params);
    List<Map<String, Object>> HPIO_500U_STR(Map<String, Object> params);
    List<Map<String, Object>> HPIO_501U_STR(Map<String, Object> params);
    List<Map<String, Object>> HPIO_510U_STR(Map<String, Object> params);
    List<Map<String, Object>> HPIO_511U_STR(Map<String, Object> params);
    List<Map<String, Object>> HPIO_520U_STR(Map<String, Object> params);
    List<Map<String, Object>> HPIO_521U_STR(Map<String, Object> params);
    List<Map<String, Object>> HPIO_640S_STR(Map<String, Object> params);
    List<Map<String, Object>> HPIO_650S_STR(Map<String, Object> params);
    List<Map<String, Object>> HPIO_660S_STR(Map<String, Object> params);
    List<Map<String, Object>> HPIO_710S_STR(Map<String, Object> params);
    List<Map<String, Object>> HPIO_720S_STR(Map<String, Object> params);
    List<Map<String, Object>> HPIO_850S_STR(Map<String, Object> params);
    List<Map<String, Object>> HPIO_870U_STR(Map<String, Object> params);
    List<Map<String, Object>> HPIO_250U_POP(Map<String, Object> params);
    List<Map<String, Object>> HPIO_251S_STR(Map<String, Object> params);
    List<Map<String, Object>> HPIO_252S_STR(Map<String, Object> params);
    List<Map<String, Object>> HPIO_253U_STR(Map<String, Object> params);
}
