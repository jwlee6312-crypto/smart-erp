package com.crmbank.erp.hsio.dto;

import lombok.Data;

/**
 * 매출반품 마스터 DTO (HSIO_490U_STR)
 */
@Data
public class Hsio490u {
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
    private String area;
    private String userid;
    private String trnemp;
    private String trancd;
    private String address; // XML의 #{addres} 매핑에 맞춤
    private String remark;
    private String cfmyn;
    private String totsum;
    private String updemp;
}
