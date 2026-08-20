package com.crmbank.erp.hsod.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * 주문 상세 DTO
 * 프로시저 HSOD_101U_STR 파라미터 정의와 1:1 매핑 (15개)
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Hsod101u {
    private String actkind;  // @iACTKIND
    private String cmpycd;   // @iCMPYCD
    private String ordym;    // @iORDYM
    private String ordno;    // @iORDNO
    private String orowno;   // @iOROWNO
    private String ordymd;   // @iORDYMD
    private String deptcd;   // @iDEPTCD
    private String custcd;   // @iCUSTCD
    private String itemcd;   // @iITEMCD
    private String unit;     // @iUNIT
    private String ordqty;   // @iORDQTY
    private String pctype;   // @iPCTYPE
    private String ordamt;   // @iORDAMT
    private String ordvat;   // @iORDVAT
    private String updemp;   // @iUPDEMP

    // 프론트엔드 수신용 (DB 파라미터 아님)
    private String price;

    @JsonAlias("amtsum")
    public void setAmtsum(String amtsum) {
        this.ordamt = amtsum;
    }
}
