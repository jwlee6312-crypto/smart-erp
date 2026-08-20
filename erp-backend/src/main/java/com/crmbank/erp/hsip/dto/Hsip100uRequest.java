package com.crmbank.erp.hsip.dto;

import lombok.Data;
import java.util.List;

@Data
public class Hsip100uRequest {
    private Hsip100u mst;      // 마스터 정보
    private List<Hsip101u> dtl; // 상세 리스트
}
