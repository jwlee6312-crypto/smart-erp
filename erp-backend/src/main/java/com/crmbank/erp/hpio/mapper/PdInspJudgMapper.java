package com.crmbank.erp.hpio.mapper;

import com.crmbank.erp.hpio.dto.PdInspJudgDto;
import com.crmbank.erp.hpio.dto.PdInspReqDto;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface PdInspJudgMapper {
    /**
     * 검사 의뢰 목록 조회 (좌측 그리드)
     */
    List<PdInspReqDto> getPdInspReqList(Map<String, Object> params);

    /**
     * 부적합 처리 결과 목록 조회 (우측 그리드)
     */
    List<PdInspJudgDto> getPdInspJudgList(Map<String, Object> params);

    /**
     * 처리 일련번호 채번
     */
    String selectNewRsltSer(Map<String, Object> params);

    /**
     * 부적합 처리 저장 (Insert)
     */
    void insertPdInspJudg(PdInspJudgDto dto);

    /**
     * 부적합 처리 수정 (Update)
     */
    void updatePdInspJudg(PdInspJudgDto dto);

    /**
     * 부적합 처리 삭제
     */
    void deletePdInspJudg(Map<String, Object> params);

    /**
     * 처리 수량 합계 조회
     */
    BigDecimal selectTotalRsltQty(Map<String, Object> params);

    /**
     * 검사 의뢰 판정 결과 업데이트 (PD_INSP_REQ)
     */
    void updatePdInspReqJudg(Map<String, Object> params);

    /**
     * 생산실적 양품/불량 수량 업데이트 (PD_PRODUCTION)
     */
    void updatePdProductionQty(Map<String, Object> params);

    /**
     * 입고 여부 확인
     */
    int selectIpgoCount(Map<String, Object> params);
}
