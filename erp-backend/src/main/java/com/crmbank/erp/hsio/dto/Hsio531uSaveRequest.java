package com.crmbank.erp.hsio.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 외부매출전표 통합 저장 요청 DTO (HSIO531U)
 */
@Data
public class Hsio531uSaveRequest {
    private Map<String, Object> mst;
    private List<Map<String, Object>> items; // dtl -> items 원상복구
}
