package com.crmbank.erp.hsio.dto;

import lombok.Data;
import java.util.List;

/**
 * 일괄매출정산 요청 DTO
 */
@Data
public class Hsio590uRequest {
    private List<Hsio590u> list; // 원래 필드명인 list로 복구
}
