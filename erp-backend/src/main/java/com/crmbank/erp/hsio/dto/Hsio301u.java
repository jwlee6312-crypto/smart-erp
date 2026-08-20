package com.crmbank.erp.hsio.dto;

import lombok.Data;

/**
 * 매출건별 상계처리 DTO (HSIO_301U_STR)
 * HSIO305U 화면에서 입금과 매출건을 상계할 때 사용
 */
@Data
public class Hsio301u {
    private String actkind;   // 액션코드 (A1, U1, D1 등)
    private String cmpycd;    // 회사코드
    private String custcd;    // 거래처코드
    private String iogbn;     // 입출고구분
    private String ioym;      // 입출고년월
    private String iono;      // 입출고번호
    private String imym;      // 입금년월
    private String imno;      // 입금번호
    private String imrowno;   // 입금순번
    private String imymd;     // 입금일자
    private String maeamt;    // 매출금액
    private String imamt;     // 상계금액
    private String deptcd;    // 부서코드
    private String userid;    // 사용자ID
}
