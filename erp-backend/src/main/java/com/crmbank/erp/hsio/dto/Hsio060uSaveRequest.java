package com.crmbank.erp.hsio.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 입고처리 통합 저장 요청 DTO (HSIO060U)
 */
@Data
public class Hsio060uSaveRequest {
    private Map<String, Object> mst;
    private List<Map<String, Object>> items; // 원래 필드명인 items로 복구
}
