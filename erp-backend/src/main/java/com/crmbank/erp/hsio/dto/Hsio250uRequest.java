package com.crmbank.erp.hsio.dto;


import java.util.List;

import lombok.Data;

@Data
public class Hsio250uRequest {
    private Hsio250u mst;      // 마스터 정보
    private List<Hsio251u> dtl; // 상세 리스트
}
