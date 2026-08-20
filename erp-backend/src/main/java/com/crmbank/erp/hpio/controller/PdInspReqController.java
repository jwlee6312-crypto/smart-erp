package com.crmbank.erp.hpio.controller;

import com.crmbank.erp.comm.dto.ApiResponse;
import com.crmbank.erp.hpio.dto.PdInspReqDto;
import com.crmbank.erp.hpio.service.PdInspReqService;

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
@RequestMapping("/product/insp-req")
public class PdInspReqController {

    private final PdInspReqService pdInspReqService;

    /**
     * 제품검사의뢰 대상 조회 (생산실적 기반)
     */
    @GetMapping("/target-list")
    public ResponseEntity<List<PdInspReqDto>> getPdInspReqTargetList(@RequestParam Map<String, Object> params, HttpSession session) {
        String cmpycd = session.getAttribute("cmpycd").toString();
        params.put("cmpycd", cmpycd);
        log.info("🔍 제품검사의뢰 대상 조회 - 파라미터: {}", params);
        return ResponseEntity.ok(pdInspReqService.getPdInspReqTargetList(params));
    }

    /**
     * 제품검사의뢰 일괄 저장 (의뢰 및 취소)
     */
    @PostMapping("/save")
    public ResponseEntity<ApiResponse<?>> savePdInspReq(@RequestBody List<PdInspReqDto> list, HttpSession session) {
        try {
            String cmpycd = session.getAttribute("cmpycd").toString();
            String userId = session.getAttribute("userid").toString();
            log.info("💾 제품검사의뢰 일괄 처리 - 건수: {}", list.size());

            pdInspReqService.savePdInspReqList(list, cmpycd, userId);
            return ResponseEntity.ok(ApiResponse.success("S", "성공적으로 처리되었습니다."));
        } catch (Exception e) {
            log.error("❌ 제품검사의뢰 처리 오류: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.error(500, e.getMessage()));
        }
    }

    /**
     * 제품검사의뢰 삭제
     */
    @PostMapping("/delete")
    public ResponseEntity<ApiResponse<?>> deletePdInspReq(@RequestBody Map<String, Object> params, HttpSession session) {
        try {
            String cmpycd = session.getAttribute("cmpycd").toString();
            params.put("cmpycd", cmpycd);
            log.info("🗑️ 제품검사의뢰 삭제 요청 - 파라미터: {}", params);
            pdInspReqService.deletePdInspReq(params);
            return ResponseEntity.ok(ApiResponse.success("S", "삭제되었습니다."));
        } catch (Exception e) {
            log.error("❌ 제품검사의뢰 삭제 오류", e);
            return ResponseEntity.ok(ApiResponse.error(500, e.getMessage()));
        }
    }
}
