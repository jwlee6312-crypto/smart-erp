package com.crmbank.erp.hsio.dto;
import lombok.Data;
import java.math.BigDecimal;

/**
 * 매입등록 마스터 DTO (HSIO_500U_STR)
 */
@Data
public class Hsio731u {
    private String               actkind;
    private String               cmpycd;
    private String               iogbn;
    private String               ioym;
    private String               ino;
    private String               iorowno;
    private String               ono;
    private String               deptcd;
    private String               whcd;
    private String               ioymd;
    private String               itemcd;
    private String               unit;
    private BigDecimal           ioqty;
    private BigDecimal           ioamt;
    private String               updemp;
}
