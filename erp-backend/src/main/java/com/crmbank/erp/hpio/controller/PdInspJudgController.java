package com.crmbank.erp.hpio.controller;

import com.crmbank.erp.comm.dto.ApiResponse;
import com.crmbank.erp.hpio.dto.PdInspJudgDto;
import com.crmbank.erp.hpio.dto.PdInspReqDto;
import com.crmbank.erp.hpio.service.PdInspJudgService;

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
@RequestMapping("/product/insp-judg")
public class PdInspJudgController {

    private final PdInspJudgService pdInspJudgService;

    /**
     * 검사 의뢰 목록 조회 (좌측 그리드)
     */
    @GetMapping("/req-list")
    public ResponseEntity<List<PdInspReqDto>> getPdInspReqList(@RequestParam Map<String, Object> params, HttpSession session) {
        String cmpycd = session.getAttribute("cmpycd").toString();
        params.put("cmpycd", cmpycd);
        log.info("🔍 검사 의뢰 목록 조회 - 파라미터: {}", params);
        return ResponseEntity.ok(pdInspJudgService.getPdInspReqList(params));
    }

    /**
     * 부적합 처리 결과 목록 조회 (우측 그리드)
     */
    @GetMapping("/judg-list")
    public ResponseEntity<List<PdInspJudgDto>> getPdInspJudgList(@RequestParam Map<String, Object> params, HttpSession session) {
        String cmpycd = session.getAttribute("cmpycd").toString();
        params.put("cmpycd", cmpycd);
        log.info("🔍 부적합 처리 목록 조회 - 파라미터: {}", params);
        return ResponseEntity.ok(pdInspJudgService.getPdInspJudgList(params));
    }

    /**
     * 부적합 처리 일괄 저장
     */
    @PostMapping("/save")
    public ResponseEntity<ApiResponse<?>> savePdInspJudg(@RequestBody List<PdInspJudgDto> list, HttpSession session) {
        try {
            String cmpycd = session.getAttribute("cmpycd").toString();
            String userId = session.getAttribute("userid").toString();
            log.info("💾 부적합 처리 일괄 저장 - 건수: {}", list.size());

            pdInspJudgService.savePdInspJudgList(list, cmpycd, userId);
            return ResponseEntity.ok(ApiResponse.success("S", "성공적으로 처리되었습니다."));
        } catch (Exception e) {
            log.error("❌ 부적합 처리 저장 오류: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.error(500, e.getMessage()));
        }
    }

    /**
     * 검사판정 삭제
     */
    @PostMapping("/delete")
    public ResponseEntity<ApiResponse<?>> deletePdInspJudg(@RequestBody Map<String, Object> params, HttpSession session) {
        try {
            String cmpycd = session.getAttribute("cmpycd").toString();
            params.put("cmpycd", cmpycd);
            log.info("🗑️ 검사판정 삭제 요청 - 파라미터: {}", params);
            pdInspJudgService.deletePdInspJudg(params);
            return ResponseEntity.ok(ApiResponse.success("S", "삭제되었습니다."));
        } catch (Exception e) {
            log.error("❌ 검사판정 삭제 오류", e);
            return ResponseEntity.ok(ApiResponse.error(500, e.getMessage()));
        }
    }
}
