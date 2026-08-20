package com.crmbank.erp.hsio.dto;
import lombok.Data;
import java.math.BigDecimal;

/**
 * 매입등록 마스터 DTO (HSIO_500U_STR)
 */
@Data
public class Hsio251u {
    private String        actkind;
    private String        cmpycd;
    private String        iogbn;
    private String        ioym;
    private String        iono;
    private String        iorowno;
    private String        deptcd;
    private String        custcd;
    private String        whcd;
    private String        ioymd;
    private String        iotype;
    private String        itemcd;
    private String        itsize;
    private String        unit;
    private String        pkunit;
    private BigDecimal    ioqty;
    private BigDecimal    ioamt;
    private BigDecimal    iovat;
    private String        cfmyn;
    private String        scustcd;
    private String        scustnm;
    private String        updemp;
}
