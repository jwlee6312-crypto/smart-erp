package com.crmbank.erp.hppl.controller;

import com.crmbank.erp.comm.dto.ApiResponse;
import com.crmbank.erp.hppl.dto.PdCapaDto;
import com.crmbank.erp.hppl.service.PdCapaService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/product/basicInfo")
public class PdCapaController {

    private final PdCapaService pdCapaService;

    /**
     * 제품 품목 목록 조회 (좌측 그리드)
     */
    @PostMapping("/selectProductItemList")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> selectProductItemList(@RequestBody Map<String, Object> params,
                                                                                        HttpSession session) {
        // 모든 키를 소문자로 변환하여 매퍼와 일치시킴
        Map<String, Object> lowerParams = params.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().toLowerCase(), Map.Entry::getValue));
        lowerParams.put("cmpycd", session.getAttribute("cmpycd").toString());
        
        List<Map<String, Object>> list = pdCapaService.getProductItemList(lowerParams);
        return ResponseEntity.ok(ApiResponse.success(list, "조회되었습니다."));
    }

    /**
     * 제품별 표준공정도 목록 조회 (우측 그리드)
     */
    @PostMapping("/selectProductCapaList")
    public ResponseEntity<ApiResponse<List<PdCapaDto>>> selectProductCapaList(@RequestBody Map<String, Object> params,
                                                                              HttpSession session) {
        // 모든 키를 소문자로 변환 (itemcd, linecd 보장)
        Map<String, Object> lowerParams = params.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().toLowerCase(), Map.Entry::getValue));
        lowerParams.put("cmpycd", session.getAttribute("cmpycd").toString());
        
        log.info("🔍 표준공정도 조회 파라미터: {}", lowerParams);
        List<PdCapaDto> list = pdCapaService.getProductCapaList(lowerParams);
        return ResponseEntity.ok(ApiResponse.success(list, "조회되었습니다."));
    }

    @PostMapping("/saveProductCapa")
    public ResponseEntity<ApiResponse<String>> saveProductCapa(@RequestBody Map<String, List<PdCapaDto>> payload, HttpSession session) {
        String cmpycd = session.getAttribute("cmpycd").toString();
        String userId = session.getAttribute("userid").toString();
        pdCapaService.saveProductCapa(payload.get("capaList"), cmpycd, userId);
        return ResponseEntity.ok(ApiResponse.success("저장되었습니다."));
    }

    @PostMapping("/deleteProductCapa")
    public ResponseEntity<ApiResponse<String>> deleteProductCapa(@RequestBody Map<String, List<PdCapaDto>> payload, HttpSession session) {
        String cmpycd = session.getAttribute("cmpycd").toString();
        pdCapaService.deleteProductCapa(payload.get("capaList"), cmpycd);
        return ResponseEntity.ok(ApiResponse.success("삭제되었습니다."));
    }
}
