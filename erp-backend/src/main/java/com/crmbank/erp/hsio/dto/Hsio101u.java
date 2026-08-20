package com.crmbank.erp.hsio.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 입고 상세 DTO (HSIO_101U_STR)
 */
@Data
public class Hsio101u {
    private String actkind;
    private String cmpycd;
    private String iogbn;
    private String ioym;
    private String iono;
    private String iorowno;
    private String deptcd;
    private String custcd;
    private String whcd;
    private String ioymd;
    private String iotype;
    private String itemcd;
    private String itsize;
    private String unit;
    private String pkunit;
    private BigDecimal ioqty;
    private BigDecimal ioamt;
    private BigDecimal iovat;
    private String cfmyn;
    private String scustcd;
    private String scustnm;
    private String updemp;
    
    // 추가 필드 (필요시)
    private String balym;
    private String balno;
    private String browno;
    private String ordym;
    private String ordno;
}
