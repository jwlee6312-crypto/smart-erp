package com.crmbank.erp.hsio.dto;

import lombok.Data;

/**
 * 매입등록 마스터 DTO (HSIO_500U_STR)
 */
@Data
public class Hsio500u {
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
    private String saleuserid;
    private String trnemp;
    private String trancd;
    private String postno;    // 🚀 추가: 우편번호
    private String address;   // 🚀 addres -> address로 수정 (매퍼와 일치)
    private String d_address; // 🚀 추가: 상세주소
    private String remark;
    private String cfmyn;
    private String totsum;
    private String ordym;
    private String ordno;
    private String updemp;
}
