package com.crmbank.erp.hsio.dto;

import lombok.Data;

/**
 * 요청등록 마스터 DTO (HSIO_010U_STR)
 */
@Data
public class Hsio010u {
    private String actkind;
    private String cmpycd;
    private String fromdt;
    private String todt;
    private String deptnm;
    private String reqym;
    private String reqno;
    private String asgbn;
    private String reqymd;
    private String inymd;
    private String whcd;
    private String deptcd;
    private String remark;
    private String ordym;
    private String ordno;
    private String updemp;
}
