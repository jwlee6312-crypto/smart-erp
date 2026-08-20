package com.crmbank.erp.hsip.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 외부수입전표 발행 통합 저장 요청 DTO (HSIP145U)
 */
@Data
public class Hsip145uSaveRequest {
    private String fileno;
    private String pubymd;
    private String ioymdfr;
    private String ioymdto;
    private List<Map<String, Object>> items; // 선택된 정산 항목 리스트
}
