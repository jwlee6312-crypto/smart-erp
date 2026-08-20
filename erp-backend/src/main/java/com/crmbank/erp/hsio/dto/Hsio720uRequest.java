package com.crmbank.erp.hsio.dto;

import lombok.Data;
import java.util.List;

/**
 * 매입등록 통합 저장 요청 DTO (HSIO500U)
 */
@Data
public class Hsio720uRequest {
    private Hsio720u mst;      // 마스터 정보
    private List<Hsio721u> dtl; // 상세 리스트
}
