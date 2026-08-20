package com.crmbank.erp.hsio.dto;

import lombok.Data;

/**
 * 매출정산 상세 DTO (HSIO_511U_STR)
 */
@Data
public class Hsio511u {
    private String actkind;
    private String cmpycd;
    private String iogbn;
    private String yymm;
    private String sungb;
    private String custcd;
    private String deptcd;
    private String ioym;
    private String iono;
    private String iorowno;
    private String taxunit;
    private String vattype;
    private String jsanym;
    private String jsanno;
    private String jsanymd;
    private String jsanqty;
    private String jsanamt;
    private String jsanvat;
    private String fromdt; // 💡 통일성을 위해 추가
    private String todt;   // 💡 통일성을 위해 추가
    private String updemp;
}
