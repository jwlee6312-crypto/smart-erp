package com.crmbank.erp.hppl.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PdCapaDto {
    private String cmpycd;     // 회사코드
    private String itemcd;     // 품목코드
    private String itemnm;     // 품목명 (조회용)
    private String linecd;      // 라인코드
    private String progcd;      // 공정코드
    private String prognm;      // 공정명 (조회용)
    private String progord;     // 공정순서
    private String itsize;      // 규격
    private String unit;        // 단위
    private BigDecimal capahh;   // 시간당캐파
    private BigDecimal gadrate;  // 가동율
    private BigDecimal pqtytt;   // 이론생산량
    private BigDecimal pqtydd;   // 일생산량
    private BigDecimal gadtmdd;  // 일가동시간
    private BigDecimal jungrate; // 정미율
    private LocalDateTime addtime; // 등록시간
    private LocalDateTime updtime; // 수정시간
    private String updemp;      // 수정자

    private String addyn;       // 등록여부 (y/n)
    private boolean chk = false; // 💡 화면 체크박스 상태값 (기본값 false로 'x' 방지)
    private String state;       // 그리드 상태 (C, U, D)
    private String dspord;      // 기본표시순서 (조회용)
}
