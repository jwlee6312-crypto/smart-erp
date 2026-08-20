package com.crmbank.erp.hsio.dto;
import lombok.Data;
import java.math.BigDecimal;

/**
 * 매입등록 마스터 DTO (HSIO_500U_STR)
 */
@Data
public class Hsio581u {
    private String        actkind;
    private String        cmpycd;
    private String        iogbn;
    private String        ioym;
    private String        iono;
    private String        iorowno;
    private String        ino;
    private String        odeptcd;
    private String        owhcd;
    private String        ioymd;
    private String        ideptcd;
    private String        iwhcd;
    private String        itemcd;
    private String        unit;
    private BigDecimal    ioqty;
    private String        updemp;
}
