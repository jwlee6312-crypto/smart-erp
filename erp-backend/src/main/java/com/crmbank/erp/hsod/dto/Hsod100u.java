package com.crmbank.erp.hsod.dto;

import lombok.Data;

/**
 * 주문 마스터 DTO
 * 표준 원칙: HsodMapper.xml의 파라미터 전달 순서와 1:1 일치 (25개 파라미터)
 */
@Data
public class Hsod100u {
    // 1~8
    private String actkind;
    private String cmpycd;
    private String fromdt;   // SP: @iYMDFR
    private String todt;     // SP: @iYMDTO
    private String custnm;
    private String ordym;
    private String ordno;
    private String ordkind;

    // 9~13
    private String deptcd;
    private String custcd;
    private String whcd;
    private String area;
    private String ordymd;

    // 14~20
    private String ordemp;
    private String paycndt;
    private String outymd;
    private String trancd;
    private String postno;
    private String address;
    private String d_address;

    // 21~25
    private String remark;
    private String totsum;
    private String sts;
    private String updemp;
    private String tranamt;

    // DB 파라미터 순서 외 필드 (UI/로직용)
    private String deptnm;
}
