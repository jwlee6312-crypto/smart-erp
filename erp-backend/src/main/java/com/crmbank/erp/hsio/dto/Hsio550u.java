package com.crmbank.erp.hsio.dto;

import lombok.Data;

/**
 * 출고 마스터 DTO (HSIO550U)
 * hsio_550u_str 파라미터 규격 준수
 */
@Data
public class Hsio550u {
    private String actkind;
    private String cmpycd;
    private String iogbn;
    private String fromdt;
    private String todt;
    private String custcd;
    private String deptcd;
    private String ioym;
    private String iono;
    private String ioymd;
    private String iotype;
    private String whcd;
    private String area;
    private String userid;
    private String trnemp;
    private String trancd;
    private String addres;
    private String d_address;
    private String remark;
    private String cfmyn;
    private String totsum;
    private String updemp;

    // UI용 필드
    private String custnm;
}
