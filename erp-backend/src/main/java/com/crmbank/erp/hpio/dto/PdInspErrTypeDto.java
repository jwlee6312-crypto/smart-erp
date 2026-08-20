package com.crmbank.erp.hpio.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PdInspErrTypeDto {
    private String cmpycd;     // 회사코드
    private String insp_gb;     // 검사구분
    private String inspcd;     // 검사항목코드
    private String insp_nm;     // 검사항목명 (조회용)
    private String err_type;    // 불량유형코드
    private String err_type_nm; // 불량유형명 (조회용)
    private LocalDateTime addtime;
    private LocalDateTime updtime;
    private String updemp;

    private String state;       // 그리드 상태 (C, U, D)
}
