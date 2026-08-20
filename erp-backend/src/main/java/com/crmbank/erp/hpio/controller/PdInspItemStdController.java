package com.crmbank.erp.hpio.controller;

import com.crmbank.erp.comm.dto.ApiResponse;
import com.crmbank.erp.hpio.dto.PdInspItemStdDto;
import com.crmbank.erp.hpio.service.PdInspItemStdService;

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

@RestController
@RequiredArgsConstructor
@RequestMapping("/product/insp-item-std")
public class PdInspItemStdController {

    private final PdInspItemStdService pdInspItemStdService;

    /**
     * 품목별 검사항목 목록 조회
     */
    @GetMapping("/list")
    public ResponseEntity<List<PdInspItemStdDto>> getPdInspItemStdList(@RequestParam Map<String, Object> params, HttpSession session) {
        String cmpycd = (String) session.getAttribute("cmpycd");
        params.put("cmpycd", cmpycd);
        return ResponseEntity.ok(pdInspItemStdService.getPdInspItemStdList(params));
    }

    /**
     * 품목별 검사항목 저장 (등록/수정/삭제 통합)
     */
    @PostMapping("/save")
    public ResponseEntity<ApiResponse<?>> savePdInspItemStd(@RequestBody List<PdInspItemStdDto> dtoList, HttpSession session) {
        try {
            String cmpycd = (String) session.getAttribute("cmpycd");
            String userId = (String) session.getAttribute("userid");
            pdInspItemStdService.savePdInspItemStd(dtoList, cmpycd, userId);
            return ResponseEntity.ok(ApiResponse.success("S", "성공적으로 저장되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(500, e.getMessage()));
        }
    }
}
