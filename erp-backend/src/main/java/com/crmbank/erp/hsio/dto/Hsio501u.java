package com.crmbank.erp.hsio.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * 매입등록 상세 DTO (HSIO_501U_STR)
 */
@Data
public class Hsio501u {
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
    private String saleuserid;
    private String ioymd;
    private String iotype;
    private String itemcd;
    private String itsize;
    private String unit;
    
    @JsonAlias("qty")
    private String ioqty;
    
    @JsonAlias({"amtsum", "amt"})
    private String ioamt;
    
    @JsonAlias("vat")
    private String iovat;
    
    private String cfmyn;
    private String updemp;

    // 프론트엔드 계산 및 표시용 (DB 전송 제외)
    private String price;
}
