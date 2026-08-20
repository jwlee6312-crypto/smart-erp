package com.crmbank.erp.hsio.dto;
import lombok.Data;
import java.math.BigDecimal;

/**
 * 매입등록 마스터 DTO (HSIO_500U_STR)
 */
@Data
public class Hsio580u {
    private String        actkind;
    private String        cmpycd;
    private String        fromdt;
    private String        todt;
    private String        iogbn;
    private String        ioym;
    private String        iono;
    private String        ioymd;
    private String        odeptcd;
    private String        owhcd;
    private String        ideptcd;
    private String        iwhcd;
    private String        remark;
    private String        updemp;
}
