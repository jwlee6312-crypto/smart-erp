package com.crmbank.erp.hsip.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 수입전표 취소 통합 요청 DTO (HSIP150U)
 */
@Data
public class Hsip150uCancelRequest {
    private String cmpycd;
    private String fromdt;
    private String todt;
    private String autoSlip; // Y/N
    private List<Map<String, Object>> items; // 선택된 전표 목록
}
