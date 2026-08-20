package com.crmbank.erp.hsio.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 외부입금전표 통합 발행 요청 DTO (HSIO325U)
 */
@Data
public class Hsio325uSaveRequest {
    private Map<String, Object> mst;
    private List<Map<String, Object>> items; // dtl -> items 원상복구
}
