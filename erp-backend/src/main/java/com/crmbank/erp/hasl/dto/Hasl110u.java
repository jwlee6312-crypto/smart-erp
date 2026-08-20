package com.crmbank.erp.hasl.dto;

import lombok.Data;

/**
 * 경리전표 마스터 DTO (HASL_110U_STR)
 */
@Data
public class Hasl110u {
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
    private String updemp;
}
