package com.crmbank.erp.hppl.dto;

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
public class PdOrderDto {
    private String cmpycd;     // 회사코드
    private String linecd;      // 라인코드
    private String progcd;      // 공정코드
    private String lotno;       // LOT번호
    private String ordym;       // 지시년월 (추가)
    private String ordno;       // 지시번호 (추가)
    private String ordymd;      // 지시일자
    private String lotymd;      // LOT일자
    private String itemcd;     // 품목코드
    private String itsize;      // 규격
    private String unit;        // 단위
    private BigDecimal lotsize; // LOT수량
    private BigDecimal ordqty;  // 지시수량
    private BigDecimal planqty; // 계획수량 (추가)
    private BigDecimal prodqty; // 생산수량
    private String bigo;        // 비고
    private String useyn;       // 사용여부
    private String ordyn;     // 지시구분
    private String gubun;       // 계획구분
    private String yymmdd;      // 계획일자
    private String ser;         // 계획순번
    private LocalDateTime addtime; 
    private LocalDateTime updtime; 
    private String updemp;      

    // 🚀 UI 표준 플래그
    private String _status;
    private String _state;

    // 조회용 확장 필드2222
    private String itemnm;     // 품목명
    private String linenm;      // 라인명
    private String prognm;      // 공정명
    private String custcd;     // 거래처코드
    private String custnm;     // 거래처명
    private String day_nm;      // 요일
    private String state;       // 그리드 상태
    private String updyn;       // 수정가능여부
}
