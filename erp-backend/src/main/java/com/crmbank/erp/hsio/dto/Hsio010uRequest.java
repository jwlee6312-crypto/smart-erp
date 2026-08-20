package com.crmbank.erp.hsio.dto;

import java.util.List;

import lombok.Data;

@Data
public class Hsio010uRequest {
    private Hsio010u mst;      // 마스터 정보
    private List<Hsio011u> dtl; // 상세 리스트
}
