package com.crmbank.erp.hsio.dto;

import lombok.Data;
import java.util.List;

/**
 * 일반발주 통합 저장 요청 DTO (HSIO_052U)
 */
@Data
public class Hsio052uRequest {
    private Hsio052u mst;      // 마스터 정보
    private List<Hsio051u> dtl; // 상세 리스트 (HSIO051U 사용)
}
