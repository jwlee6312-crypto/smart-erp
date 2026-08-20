package com.crmbank.erp.hsio.dto;
import lombok.Data;
import java.math.BigDecimal;

/**
 * 매입등록 마스터 DTO (HSIO_500U_STR)
 */
@Data
public class Hsio730u {
    private String actkind;
    private String cmpycd;
    private String fromdt;
    private String todt;
    private String deptnm;
    private String iogbn;
    private String ioym;
    private String iono;
    private String deptcd;
    private String whcd;
    private String ioymd;
    private String remark;
    private String updemp;
}
