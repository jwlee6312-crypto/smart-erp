package com.crmbank.erp.hsio.dto;

import lombok.Data;
import java.util.List;

/**
 * 매출반품 통합 저장 요청 DTO (HSIO490U)
 */
@Data
public class Hsio490uRequest {
    private Hsio490u mst;
    private List<Hsio491u> dtl;
}
