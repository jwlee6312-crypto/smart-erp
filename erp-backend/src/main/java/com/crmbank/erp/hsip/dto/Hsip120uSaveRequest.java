package com.crmbank.erp.hsip.dto;

import lombok.Data;
import java.util.List;

/**
 * 통관/입고 통합 저장 요청 DTO
 */
@Data
public class Hsip120uSaveRequest {
    private Hsip120u mst;        // 통관/입고 마스터 데이터
    private List<Hsip121u> dtl;  // 선택된 품목 리스트
}
