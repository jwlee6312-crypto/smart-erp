package com.crmbank.erp.hpio.controller;

import com.crmbank.erp.comm.dto.ApiResponse;
import com.crmbank.erp.hpio.dto.PdInspReqDto;
import com.crmbank.erp.hpio.dto.PdInspRsltDtlDto;
import com.crmbank.erp.hpio.dto.PdInspRsltDto;
import com.crmbank.erp.hpio.service.PdInspRsltDtlService;

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
@RequestMapping("/product/insp-rslt-dtl")
public class PdInspRsltDtlController {

    private final PdInspRsltDtlService pdInspRsltDtlService;

    /**
     * 검사 의뢰 목록 조회 (좌측 그리드)
     */
    @GetMapping("/req-list")
    public ResponseEntity<List<PdInspReqDto>> getPdInspReqList(@RequestParam Map<String, Object> params, HttpSession session) {
        String cmpycd = session.getAttribute("cmpycd").toString();
        params.put("cmpycd", cmpycd);
        log.info("🔍 검사 의뢰 목록 조회 - 파라미터: {}", params);
        return ResponseEntity.ok(pdInspRsltDtlService.getPdInspReqList(params));
    }

    /**
     * 검사 결과 마스터 목록 조회 (중간 그리드)
     */
    @GetMapping("/mst-list")
    public ResponseEntity<List<PdInspRsltDto>> getPdInspRsltMstList(@RequestParam Map<String, Object> params, HttpSession session) {
        String cmpycd = session.getAttribute("cmpycd").toString();
        params.put("cmpycd", cmpycd);
        log.info("🔍 검사 결과 마스터 조회 - 파라미터: {}", params);
        return ResponseEntity.ok(pdInspRsltDtlService.getPdInspRsltMstList(params));
    }

    /**
     * 검사 불량 상세 목록 조회 (우측 그리드)
     */
    @GetMapping("/list")
    public ResponseEntity<List<PdInspRsltDtlDto>> getPdInspRsltDtlList(@RequestParam Map<String, Object> params, HttpSession session) {
        String cmpycd = session.getAttribute("cmpycd").toString();
        params.put("cmpycd", cmpycd);
        log.info("🔍 검사 불량 상세 조회 - 파라미터: {}", params);
        return ResponseEntity.ok(pdInspRsltDtlService.getPdInspRsltDtlList(params));
    }

    /**
     * 검사 불량 상세 일괄 저장
     */
    @PostMapping("/save")
    public ResponseEntity<ApiResponse<?>> savePdInspRsltDtl(@RequestBody List<PdInspRsltDtlDto> list, HttpSession session) {
        try {
            String cmpycd = session.getAttribute("cmpycd").toString();
            String userId = session.getAttribute("userid").toString();
            log.info("💾 검사 불량 상세 일괄 저장 - 건수: {}", list.size());

            pdInspRsltDtlService.savePdInspRsltDtlList(list, cmpycd, userId);
            return ResponseEntity.ok(ApiResponse.success("S", "성공적으로 저장되었습니다."));
        } catch (Exception e) {
            log.error("❌ 검사 불량 상세 저장 오류: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.error(500, e.getMessage()));
        }
    }

    /**
     * 검사 불량 상세 삭제
     */
    @PostMapping("/delete")
    public ResponseEntity<ApiResponse<?>> deletePdInspRsltDtl(@RequestBody Map<String, Object> params, HttpSession session) {
        try {
            String cmpycd = session.getAttribute("cmpycd").toString();
            params.put("cmpycd", cmpycd);
            
            log.info("🗑️ 검사 불량 상세 삭제 요청 - 파라미터: {}", params);
            pdInspRsltDtlService.deletePdInspRsltDtl(params);
            
            return ResponseEntity.ok(ApiResponse.success("S", "삭제되었습니다."));
        } catch (Exception e) {
            log.error("❌ 검사 불량 상세 삭제 오류", e);
            return ResponseEntity.ok(ApiResponse.error(500, e.getMessage()));
        }
    }
}
