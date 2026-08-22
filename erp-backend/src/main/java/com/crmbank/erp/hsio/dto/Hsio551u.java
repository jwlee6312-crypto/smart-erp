package com.crmbank.erp.hsio.dto;

import lombok.Data;

/**
 * 출고 상세 DTO (HSIO550U/551U)
 * hsio_551u_str 파라미터 규격 준수
 */
@Data
public class Hsio551u {
    private String actkind;
    private String cmpycd;
    private String iogbn;
    private String ioym;
    private String iono;
    private String iorowno;
    private String deptcd;
    private String custcd;
    private String whcd;
    private String area;
    private String userid;
    private String ioymd;
    private String iotype;
    private String itemcd;
    private String itsize;
    private String unit;
    private String ioqty;
    private String ioamt;
    private String iovat;
    private String balym;
    private String balno;
    private String browno;
    private String cfmyn;
    private String updemp;

    // UI용 필드
    private String itemnm;
}
