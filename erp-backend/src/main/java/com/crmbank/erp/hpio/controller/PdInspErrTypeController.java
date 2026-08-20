package com.crmbank.erp.hpio.controller;

import com.crmbank.erp.comm.dto.ApiResponse;
import com.crmbank.erp.hpio.dto.PdInspClsfyDto;
import com.crmbank.erp.hpio.dto.PdInspErrTypeDto;
import com.crmbank.erp.hpio.service.PdInspErrTypeService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/product/insp-err-type")
public class PdInspErrTypeController {

    private final PdInspErrTypeService pdInspErrTypeService;

    /**
     * 검사항목 목록 조회 (좌측 그리드용)
     */
    @GetMapping("/insp-list")
    public ResponseEntity<List<PdInspClsfyDto>> getPdInspClsfyList(@RequestParam Map<String, Object> params, HttpSession session) {
        String cmpycd = session.getAttribute("cmpycd").toString();
        params.put("cmpycd", cmpycd);
        log.info("🔍 검사항목 목록 조회 - 파라미터: {}", params);
        return ResponseEntity.ok(pdInspErrTypeService.getPdInspClsfyList(params));
    }

    /**
     * 검사항목별 불량유형 목록 조회 (우측 그리드용)
     */
    @GetMapping("/err-list")
    public ResponseEntity<List<PdInspErrTypeDto>> getPdInspErrTypeList(@RequestParam Map<String, Object> params, HttpSession session) {
        String cmpycd = session.getAttribute("cmpycd").toString();
        params.put("cmpycd", cmpycd);
        log.info("🔍 검사항목별 불량유형 목록 조회 - 파라미터: {}", params);
        return ResponseEntity.ok(pdInspErrTypeService.getPdInspErrTypeList(params));
    }

    /**
     * 불량유형 일괄 저장
     */
    @PostMapping("/save")
    public ResponseEntity<ApiResponse<String>> savePdInspErrType(@RequestBody List<PdInspErrTypeDto> list, HttpSession session) {
        String cmpycd = session.getAttribute("cmpycd").toString();
        String userId = session.getAttribute("userid").toString();
        log.info("💾 불량유형 일괄 저장 - 건수: {}", list.size());

        pdInspErrTypeService.savePdInspErrTypeList(list, cmpycd, userId);
        return ResponseEntity.ok(ApiResponse.success("S", "성공적으로 저장되었습니다."));
    }
}
