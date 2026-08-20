package com.crmbank.erp.hsio.dto;

import lombok.Data;

/**
 * 입금전표 조회/매핑 DTO (HSIO_320U_STR)
 */
@Data
public class Hsio320u {
    private String actkind;
    private String cmpycd;
    private String deptcd;
    private String imymdfr;
    private String imymdto;
    private String salsemp;
    private String userid;
    private String imym;
    private String imno;
    private String imymd;
    private String slipymd;
    private String slipno;
    private String updemp;

    // 조회 결과 추가 필드
    private String custcd;
    private String custnm;
    private String deptnm;
    private String imamt;
    private String rowcnt;
}
