package com.crmbank.erp.hsio.dto;
import lombok.Data;

/**
 * 일반발주 등록 마스터 DTO (HSIO_052U_STR)
 */
@Data
public class Hsio052u {
    private String actkind;
    private String cmpycd;
    private String fromdt;
    private String todt;
    private String custnm;
    private String balym;
    private String balno;
    private String balymd;
    private String reqymd;
    private String custcd;
    private String deptcd;
    private String remark;
    private String updemp;

}
