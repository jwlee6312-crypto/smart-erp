package com.crmbank.erp.hsip.dto;
import lombok.Data;
import java.math.BigDecimal;

/**
 * 매입등록 마스터 DTO (HSIO_500U_STR)
 */
@Data
public class Hsip101u {
    private String actkind;
    private String cmpycd;
    private String fileno;
    private String prowno;
    private String itemcd;
    private String itsize;
    private String unit;
    private BigDecimal qty;
    private BigDecimal amt;
    private String updemp;
}
