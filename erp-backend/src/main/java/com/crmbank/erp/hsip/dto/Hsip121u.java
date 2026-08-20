package com.crmbank.erp.hsip.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 통관 품목 상세 DTO (HSIP_121U_STR)
 */
@Data
public class Hsip121u {
    private String actkind;
    private String cmpycd;
    private String fileno;
    private String shipseq;
    private String passseq;
    private String prowno;
    private String itemcd;
    private String itemnm; 
    private String itsize;
    private String unit;
    private BigDecimal gqty; // 감모량
    private BigDecimal iqty; // 통관량 (Vue 그리드 매핑용)
    private BigDecimal qty;  // MyBatis #{qty} 매핑용
    private BigDecimal amt;
    private String ioym;
    private String iono;
    private String iorowno;
    private String updemp;
    private String srowno;
}
