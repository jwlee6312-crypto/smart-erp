package com.crmbank.erp.hsod.dto;

import lombok.Data;
import java.util.List;

/**
 * 주문등록 저장 요청 DTO
 * 소문자 표준화 및 프론트엔드 키값(mst, dtl) 일치화
 */
@Data
public class Hsod100uRequest {
    private Hsod100u mst;      // 마스터 정보 (기존 hsod100u)
    private List<Hsod101u> dtl; // 디테일 리스트 (기존 hsod101u)
}
