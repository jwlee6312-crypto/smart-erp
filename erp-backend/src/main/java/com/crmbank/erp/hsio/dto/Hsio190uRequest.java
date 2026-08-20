package com.crmbank.erp.hsio.dto;

import java.util.List;

import lombok.Data;

@Data
public class Hsio190uRequest {
    private Hsio190u mst;      // 마스터 정보
    private List<Hsio191u> dtl; // 상세 리스트
}
