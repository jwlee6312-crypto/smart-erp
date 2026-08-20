package com.crmbank.erp.hppl.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@ToString
@AllArgsConstructor
public class PdPlanDto {
    private String cmpycd;     // 회사코드
    private String gubun;       // 주문 100/양산 200/외주 300
    private String yymmdd;      // 계획일자
    private String ser;         // 순번
    private String reqym;       // 요청년월
    private String reqymd;      // 요청일자
    private String reqno;       // 요청번호
    private String ordym;       // 주문년월 (계획 연계용)
    private String ordymd;      // 주문일자
    private String ordno;       // 주문번호
    private String orowno;      // 주문순번
    private String custcd;     // 거래처코드
    private String custnm;     // 거래처명 (조회용)
    private String addrcd;     // 주소지코드
    private String itemcd;     // 품목코드
    private String itemnm;     // 품목명 (조회용)
    private String itsize;     // 규격
    private String unit;       // 단위 (조회용)
    private String linecd;      // 라인코드
    private String linenm;      // 라인명 (조회용)
    private String progcd;      // 공정코드
    private String prognm;      // 공정명 (조회용)
    private BigDecimal ordqty;  // 지시수량
    private BigDecimal planqty; // 계획수량
    private BigDecimal suppamt; // 공급가액
    private String napgiymd;    // 납기일자
    private String dlvhopedt;   // 배송희망일
    private String pumymd;     // 품의일자 (추가)
    private String pumno;      // 품의번호 (추가)
    private String day_nm;      // 요일 (조회용)
    private String sordgbn;     // 주문구분코드
    private String sordgbnnm;   // 주문구분명
    private String altrn_mtrl_gbn; // 대체자재구분
    private String bigo;        // 비고
    private LocalDateTime addtime; 
    private LocalDateTime updtime; 
    private String updemp;      

    // 🚀 UI 표준 플래그
    private String _status;
    private String _state;

    private String state;       // 기존 호환용 (C, U, D)

    // 💡 공정별 표준 Capa 정보 추가 (조회용)
    private BigDecimal gadrate;    // 가동율 (%)
    private BigDecimal jungrate;   // 양품율 (%)
    private BigDecimal capahh;     // 개당 소요시간 (h)
    private BigDecimal gadtmdd;    // 가동시간
    private BigDecimal pqtytt;     // 일생산량
    private BigDecimal unit_capa;  // 일생산능력 (pqtydd)
}
