package com.crmbank.erp.hsio.dto;
import lombok.Data;

/**
 * 매입반품 마스터 DTO (HSIO_190U_STR)
 */
@Data
public class Hsio190u {
    private String actkind;
    private String cmpycd;
    private String fromdt;
    private String todt;
    private String custnm;
    private String iogbn;
    private String ioym;
    private String iono;
    private String ioymd;
    private String iotype;
    private String custcd;
    private String deptcd;
    private String whcd;
    private String address; // XML 매핑에 맞춤
    private String remark;
    private String cfmyn;
    private String gubun;
    private String totsum;
    private String updemp;
}
