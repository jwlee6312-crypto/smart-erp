package com.crmbank.erp.hsip.dto;

import lombok.Data;

/**
 * 통관 마스터 DTO (HSIP_120U_STR)
 */
@Data
public class Hsip120u {
    private String actkind;
    private String cmpycd;
    private String fileno;
    private String shipseq;
    private String passseq;
    private String passymd;
    private String taxorg;
    private String passno;
    private String whcd;
    private String deptcd;
    
    // 거래처 정보
    private String custcd;
    private String custnm;
    
    // 입고 연계 키
    private String ioym;
    private String iono;
    
    // 추가 필드 (HSIO 연계용)
    private String iogbn;
    private String iotype;
    private String address;
    private String remark;
    private String cfmyn;
    private String gubun;
    private String totsum;

    private String updemp;
}
