package com.crmbank.erp.hpio.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Data
@NoArgsConstructor
@ToString
@Setter
@Getter
@AllArgsConstructor
public class PdInspItemStdDto {
    private String cmpycd;     // 회사코드
    private String itemcd;     // 품목코드
    private String insp_gb;     // 검사구분
    private String inspcd;     // 검사항목코드
    private String insp_nm;     // 검사항목명 (조회용)
    private String insp_ord;    // 표시순서
    private String bigo;        // 비고
    private LocalDateTime addtime; // 등록시간
    private LocalDateTime updtime; // 수정시간
    private String updemp;      // 수정자

    private String state;       // 그리드 상태 (C, U, D)
}
