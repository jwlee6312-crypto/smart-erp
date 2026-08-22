package com.crmbank.erp.hsio.dto;

import lombok.Data;
import java.util.List;

/**
 * 출고처리 통합 저장 요청 DTO (HSIO550U)
 */
@Data
public class Hsio550uSaveRequest {
    private Hsio550u mst;
    private List<Hsio551u> dtl;
}
