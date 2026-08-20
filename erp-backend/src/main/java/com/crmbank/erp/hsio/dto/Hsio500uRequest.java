package com.crmbank.erp.hsio.dto;

import lombok.Data;
import java.util.List;

/**
 * 매입등록 통합 저장 요청 DTO (HSIO500U)
 */
@Data
public class Hsio500uRequest {
    private Hsio500u mst;      // 마스터 정보
    private List<Hsio501u> dtl; // 상세 리스트
}
