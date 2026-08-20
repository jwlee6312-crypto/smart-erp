package com.crmbank.erp.hsio.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 매출전표 취소 통합 요청 DTO (HSIO540U)
 */
@Data
public class Hsio540uCancelRequest {
    private String cmpycd;
    private String fromdt;
    private String todt;
    private String autoSlip;
    private List<Map<String, Object>> items; // dtl -> items 원상복구
}
