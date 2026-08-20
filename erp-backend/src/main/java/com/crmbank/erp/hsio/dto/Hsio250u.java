package com.crmbank.erp.hsio.dto;
import lombok.Data;
import java.math.BigDecimal;

/**
 * 매입등록 마스터 DTO (HSIO_500U_STR)
 */
@Data
public class Hsio250u {
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
    private String addres;
    private String remark;
    private String cfmyn;
    private String gubun;
    private String totsum;
    private String updemp;
}
