package com.crmbank.erp.hsio.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 매출전표 통합 저장 요청 DTO (HSIO530U)
 */
@Data
public class Hsio530uSaveRequest {
    private Map<String, Object> mst;
    private List<Map<String, Object>> items; // dtl -> items 원상복구
}
