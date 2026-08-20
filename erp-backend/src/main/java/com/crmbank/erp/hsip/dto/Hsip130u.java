package com.crmbank.erp.hsip.dto;

import lombok.Data;

/**
 * 수입비용 마스터 DTO (HSIP_130U_STR)
 */
@Data
public class Hsip130u {
    private String actkind;
    private String cmpycd;
    private String fileno;
    private String docno;
    private String deptcd;
    private String pubymd;
    private String bigo;
    private String updemp;
    
    // 추가 필드 (필요시)
    private String deptnm;
}
