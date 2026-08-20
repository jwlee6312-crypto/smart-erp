package com.crmbank.erp.hsio.dto;

import java.util.List;
import lombok.Data;

@Data
public class Hsio300uRequest {
    private Hsio300u mst;           // 마스터 정보 (A0, U0, D0)
    private List<Hsio300u> dtl;      // 입금 상세 리스트 (A1, U1, D1 / A2)
    private List<Hsio301u> setDtl;   // 매출 상계 리스트 (HSIO305U 전용)
}
