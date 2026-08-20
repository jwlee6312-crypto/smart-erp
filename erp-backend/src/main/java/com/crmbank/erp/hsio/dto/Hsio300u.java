package com.crmbank.erp.hsio.dto;
import lombok.Data;
import java.math.BigDecimal;

/**
 * 입금등록 DTO (HSIO_300U_STR)
 */
@Data
public class Hsio300u {
    private String actkind;
    private String cmpycd;
    private String fromdt;
    private String todt;
    private String custnm;
    private String imym;
    private String imno;
    private String imrowno;
    private String deptcd;
    private String custcd;
    private String imymd;
    private String remark;
    private String imtype;
    private String imamt;
    private String mgtno;
    private String pubymd;
    private String endymd;
    private String billgbn;
    private String billamt;
    private String pubbank;
    private String pubman;
    private String slipyn;
    private String slipymd;
    private String slipno;
    private String updemp;
}
