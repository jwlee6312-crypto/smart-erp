package com.crmbank.erp.hsip.dto;
import lombok.Data;
import java.math.BigDecimal;

/**
 * 매입등록 마스터 DTO (HSIO_500U_STR)
 */
@Data
public class Hsip100u {
    private String       actkind;
    private String       cmpycd;
    private String       fromdt;
    private String       todt;
    private String       custnm;
    private String       fileno;
    private String       deptcd;
    private String       offerno;
    private String       supergbn;
    private String       imptgbn;
    private String       issymd;
    private String       custcd;
    private String       currcd;
    private BigDecimal   frgnrate;
    private String       nacd;
    private String       shipport;
    private String       pricond;
    private String       pritext;
    private String       arvport;
    private String       paycond;
    private String       payterm;
    private BigDecimal   lcamt;
    private BigDecimal   wonamt;
    private BigDecimal   frgnamt;
    private BigDecimal   xtamt;
    private String       bigo;
    private String       inymd;
    private String       apvyn;
    private String       apvymd;
    private String       apvemp;
    private String       updemp;
}
