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
public class PdInspReqDto {
    private String cmpycd;      // 회사코드
    private String insp_gb;      // 검사구분 (300: 제품검사)
    private String insp_req_dt;  // 검사의뢰일자
    private String insp_req_no;  // 검사의뢰번호
    private int prodid;          // 생산실적번호
    private String linecd;       // 라인코드
    private String progcd;       // 공정코드
    private String ordymd;       // 지시일자
    private String lotno;        // LOT번호
    private String proymd;       // 생산일자
    private String itemcd;      // 품목코드
    private String insp_how;     // 검사방법
    private BigDecimal prdqty;  // 생산수량
    private BigDecimal godqty;   // 양품수량
    private BigDecimal errqty;   // 불량수량
    private String judg_cd;      // 💡 최종판정결과 추가
    private LocalDateTime addtime; 
    private LocalDateTime updtime; 
    private String updemp;      

    // 조회용 확장 필드
    private String itemnm;      // 품목명
    private String prognm;      // 공정명
    private String state;       // 그리드 상태 (C, U, D)
}
