package com.crmbank.erp.hfba.mapper;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface HfbaMapper {
    List<Map<String, Object>> FBA1010U_STR(Map<String, Object> params);
    List<Map<String, Object>> FBA1040U_STR(Map<String, Object> params);
    List<Map<String, Object>> FBA1060U_STR(Map<String, Object> params);
    List<Map<String, Object>> FBA2010U_STR(Map<String, Object> params);
    List<Map<String, Object>> FBA2020U_SEL(Map<String, Object> params);
    List<Map<String, Object>> FBA2020U_MOD(Map<String, Object> params);
    List<Map<String, Object>> FBA2020U_DEL(Map<String, Object> params);
    List<Map<String, Object>> FBA3010U_STR(Map<String, Object> params);

    List<Map<String, Object>> selectAcctList(Map<String, Object> params);
    List<Map<String, Object>> selectDivideList(Map<String, Object> params);
    List<Map<String, Object>> selectDivideJuksuList(Map<String, Object> params);

}
