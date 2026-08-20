package com.crmbank.erp.hsio.dto;
import lombok.Data;
import java.math.BigDecimal;

/**
 * 매입등록 마스터 DTO (HSIO_500U_STR)
 */
@Data
public class Hsio571u {
    private String       actkind;
    private String       cmpycd;
    private String       iogbn;
    private String       ioym;
    private String       iono;
    private String       iorowno;
    private String       deptcd;
    private String       custcd;
    private String       whcd;
    private String       userid;
    private String       ioymd;
    private String       iotype;
    private String       itemcd;
    private String       itsize;
    private String       unit;
    private String       ino;
    private String       itemcd_t;
    private String       itsize_t;
    private String       unit_t;
    private BigDecimal   ioqty;
    private BigDecimal   ioamt;
    private BigDecimal   iovat;
    private String       usedept;
    private String       cfmyn;
    private String       updemp;
}
