package com.crmbank.erp.hppl.controller;

import com.crmbank.erp.hppl.dto.PdOrderDto;
import com.crmbank.erp.hppl.service.PdOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/product/pdorder")
public class PdOrderController {

    private final PdOrderService pdOrderService;

    /**
     * 작업지시 대상(계획) 조회 - 순수 데이터 리스트 반환
     */
    @GetMapping("/targetlist")
    public ResponseEntity<List<PdOrderDto>> getPdOrderTargetList(@RequestParam Map<String, Object> params, HttpSession session) {
        log.info("🔍 작업지시 대상 조회 파라미터: {}", params);
        
        // 세션에서 회사코드 안전하게 추출
        Object cmpycd = session.getAttribute("cmpycd");
        if (cmpycd == null) {
            com.crmbank.erp.comm.dto.UserSession user = (com.crmbank.erp.comm.dto.UserSession) session.getAttribute("user_session");
            if (user != null) cmpycd = user.getCmpycd();
        }
        
        if (cmpycd != null) {
            params.put("cmpycd", cmpycd.toString());
        }
        
        return ResponseEntity.ok(pdOrderService.getPdOrderTargetList(params));
    }

    /**
     * 작업지시 상세 목록 조회 - 순수 데이터 리스트 반환
     */
    @GetMapping("/list")
    public ResponseEntity<List<PdOrderDto>> getPdOrderList(@RequestParam Map<String, Object> params, HttpSession session) {
        Object cmpycd = session.getAttribute("cmpycd");
        if (cmpycd == null) {
            com.crmbank.erp.comm.dto.UserSession user = (com.crmbank.erp.comm.dto.UserSession) session.getAttribute("user_session");
            if (user != null) cmpycd = user.getCmpycd();
        }
        
        if (cmpycd != null) {
            params.put("cmpycd", cmpycd.toString());
        }
        return ResponseEntity.ok(pdOrderService.getPdOrderList(params));
    }

    /**
     * 작업지시 일괄 저장 - 표준 OK/ERR 리스트 반환 (현재 작업 범위에만 적용)
     */
    @PostMapping("/save")
    public ResponseEntity<List<Map<String, Object>>> savePdOrder(@RequestBody List<PdOrderDto> list, HttpSession session) {
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> statusMap = new HashMap<>();
        
        try {
            String cmpycd = session.getAttribute("cmpycd").toString();
            String userId = session.getAttribute("userid").toString();
            
            pdOrderService.savePdOrderList(list, cmpycd, userId);
            
            statusMap.put("result", "OK");
            statusMap.put("msg", "성공적으로 저장되었습니다.");
        } catch (Exception e) {
            log.error("❌ 작업지시 저장 오류: {}", e.getMessage());
            statusMap.put("result", "ERR");
            statusMap.put("msg", e.getMessage());
        }
        
        result.add(statusMap);
        return ResponseEntity.ok(result);
    }
}
