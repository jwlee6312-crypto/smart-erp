package com.crmbank.erp.hsio.dto;

import lombok.Data;

/**
 * 매출정산 마스터/상세 DTO (HSIO_510U_STR)
 */
@Data
public class Hsio510u {
    private String actkind;
    private String cmpycd;
    private String iogbn;
    private String fromdt;   // 💡 ioymdfr -> fromdt로 변경
    private String todt;     // 💡 ioymdto -> todt로 변경
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
    private String updemp;
}
