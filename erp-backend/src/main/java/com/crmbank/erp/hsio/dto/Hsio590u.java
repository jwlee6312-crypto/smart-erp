package com.crmbank.erp.hsio.dto;

import lombok.Data;

/**
 * 일괄매출정산 DTO (HSIO_590U_STR)
 */
@Data
public class Hsio590u {
    private String actkind;
    private String cmpycd;
    private String iogbn;
    private String fromdt;   // @iIOYMDFR
    private String todt;     // @iIOYMDTO
    private String deptcd;
    private String custcd;
    private String salsemp;
    private String taxunit;
    private String vattype;
    private String jsanymd;
    private String jsanamt;
    private String jsanvat;
    private String updemp;
}
