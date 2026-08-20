package com.crmbank.erp.hsio.dto;

import com.crmbank.erp.hasl.dto.Hasl010u;
import lombok.Data;
import java.util.List;

/**
 * 입금전표 발행 요청 DTO
 */
@Data
public class Hsio320uSaveRequest {
    private Hasl010u mst;        // 전표 마스터 정보
    private List<Hsio320u> dtl;  // 선택된 입금 내역 목록
}
