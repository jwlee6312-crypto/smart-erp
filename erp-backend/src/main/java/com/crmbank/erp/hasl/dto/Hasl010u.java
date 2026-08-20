package com.crmbank.erp.hasl.dto;

import lombok.Data;

/**
 * 전표 마스터/상세 DTO (HASL_010U_STR)
 */
@Data
public class Hasl010u {
    private String actkind;
    private String cmpycd;
    private String fromdt;
    private String todt;
    private String keyword;
    private String slipymd;
    private String slipno;
    private String acctymd;
    private String deptcd;
    private String empnm;
    private String slipgu;
    private String business;
    private String salsemp;
    private String imsum;
    private String updemp;
}
