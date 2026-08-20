package com.crmbank.erp.hpio.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Data
@NoArgsConstructor
@ToString
@Setter
@Getter
@AllArgsConstructor
public class PdInspRsltDto {
    private String cmpycd;      // 회사코드
    private String insp_gb;      // 검사구분
    private String insp_req_dt;  // 검사의뢰일자
    private String insp_req_no;  // 검사의뢰번호
    private Integer prodid;      // 💡 생산실적 고유 ID 추가
    private String linecd;       // 라인코드
    private String progcd;       // 공정코드
    private String ordymd;       // 지시일자
    private String proymd;       // 생산일자
    private String lotno;        // LOT번호
    private String itemcd;      // 품목코드
    private String itemnm;      // 품목명 (조회용)
    private String insp_ord;     // 검사순서
    private String inspcd;      // 검사코드
    private String insp_nm;      // 검사항목명 (조회용)
    private BigDecimal prdqty;  // 생산수량 (의뢰정보)
    private BigDecimal sample_cnt; // 시료수
    private BigDecimal errqty;     // 불량수량
    private String insp_dt;        // 💡 검사일자 추가
    private LocalDateTime addtime; 
    private LocalDateTime updtime; 
    private String updemp;      

    private String state;        // 그리드 상태 (C, U, D)
}
