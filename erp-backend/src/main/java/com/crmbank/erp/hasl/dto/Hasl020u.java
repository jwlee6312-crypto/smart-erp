package com.crmbank.erp.hasl.dto;

import lombok.Data;

/**
 * 전표 자동발행/확정 DTO (HASL_020U_STR)
 */
@Data
public class Hasl020u {
    private String actkind;
    private String cmpycd;
    private String slipymd;
    private String acctymd;
    private String slipno;
    private String deptcd;
    private String slipkind;
    private String slipyn;
    private String cofmyn;
    private String cofmemp;
    private String empnm;
    private String updemp;
}
