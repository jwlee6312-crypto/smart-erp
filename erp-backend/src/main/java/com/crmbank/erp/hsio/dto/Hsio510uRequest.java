package com.crmbank.erp.hsio.dto;

import lombok.Data;
import java.util.List;

/**
 * 매출정산 통합 저장 요청 DTO
 */
@Data
public class Hsio510uRequest {
    private Hsio510u mst;        // 정산 마스터 (HSIO_510U_STR A0)
    private List<Hsio510u> dtl;  // 정산 상세 (HSIO_510U_STR U0)
}
