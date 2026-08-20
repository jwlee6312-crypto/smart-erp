package com.crmbank.erp.hsip.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 수입비용 상세 DTO (HSIP_131U_STR)
 */
@Data
public class Hsip131u {
    private String actkind;
    private String cmpycd;
    private String fileno;
    private String docno;
    private String rowno;
    private String costcd;
    private String deptcd;
    private String shipseq;
    private String passseq;
    private String pubymd;
    private BigDecimal costamt;
    private String currcd;
    private BigDecimal frgnrate;
    private BigDecimal frgnamt;
    private String paycust;
    private String mgtno;
    private String bigo;
    private String slipymd;
    private String slipno;
    private String srowno;
    private String updemp;

    // UI 상태 관리용
    private String state;
    private String costnm;
}
