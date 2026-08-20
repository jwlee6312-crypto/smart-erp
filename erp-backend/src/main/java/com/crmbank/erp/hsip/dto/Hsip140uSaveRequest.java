package com.crmbank.erp.hsip.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 수입비용 전표대체 통합 저장 요청 DTO (HSIP140U)
 */
@Data
public class Hsip140uSaveRequest {
    private Map<String, Object> mst;      // 전표 기본 정보 (payymd, deptcd 등)
    private Map<String, Object> vat;      // 부가세 정보 (taxunit, custcd, vattype, vatamt)
    private List<Map<String, Object>> costs;    // 선택된 비용 목록 (fileno, docno, crowno, costamt 등)
    private List<Map<String, Object>> payments; // 지불 정보 목록 (acctcd, payamt, bankcd, custcd, mgtno 등)
}
