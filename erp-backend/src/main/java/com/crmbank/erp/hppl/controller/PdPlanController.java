package com.crmbank.erp.hppl.controller;

import com.crmbank.erp.hppl.dto.PdPlanDto;
import com.crmbank.erp.hppl.service.PdPlanService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/product/pdplan")
public class PdPlanController {

    private final PdPlanService pdPlanService;

    @GetMapping("/targetlist")
    public ResponseEntity<List<PdPlanDto>> getPdPlanTargetList(@RequestParam Map<String, Object> params, HttpSession session) {
        String cmpycd = session.getAttribute("cmpycd").toString();
        params.put("cmpycd", cmpycd);
        return ResponseEntity.ok(pdPlanService.getPdPlanTargetList(params));
    }

    @GetMapping("/list")
    public ResponseEntity<List<PdPlanDto>> getPdPlanList(@RequestParam Map<String, Object> params, HttpSession session) {
        String cmpycd = session.getAttribute("cmpycd").toString();
        params.put("cmpycd", cmpycd);
        return ResponseEntity.ok(pdPlanService.getPdPlanList(params));
    }

    @GetMapping("/monthly-list")
    public ResponseEntity<List<PdPlanDto>> getPdPlanMonthlyList(@RequestParam Map<String, Object> params, HttpSession session) {
        String cmpycd = session.getAttribute("cmpycd").toString();
        params.put("cmpycd", cmpycd);
        return ResponseEntity.ok(pdPlanService.getPdPlanMonthlyList(params));
    }

    /**
     * 월간 날짜별 Capa 및 계획 요약 조회
     */
    @GetMapping("/monthly-summary")
    public ResponseEntity<List<Map<String, Object>>> getPdPlanMonthlySummary(@RequestParam Map<String, Object> params, HttpSession session) {
        String cmpycd = session.getAttribute("cmpycd").toString();
        params.put("cmpycd", cmpycd);
        return ResponseEntity.ok(pdPlanService.getPdPlanMonthlySummary(params));
    }

    /**
     * 양산/외주 요청 자료 조회 (전체 내역)
     */
    @GetMapping("/request-list")
    public ResponseEntity<List<Map<String, Object>>> getPdRequestList(@RequestParam Map<String, Object> params, HttpSession session) {
        String cmpycd = session.getAttribute("cmpycd").toString();
        params.put("cmpycd", cmpycd);
        return ResponseEntity.ok(pdPlanService.getPdRequestList(params));
    }

    /**
     * 양산/외주 요청 자료 일괄 저장
     */
    @PostMapping("/request-save")
    public ResponseEntity<List<Map<String, Object>>> savePdRequest(@RequestBody List<PdPlanDto> list, HttpSession session) {
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> statusMap = new HashMap<>();
        try {
            String cmpycd = session.getAttribute("cmpycd").toString();
            String userId = session.getAttribute("userid").toString();
            pdPlanService.savePdRequestList(list, cmpycd, userId);
            statusMap.put("result", "OK");
            statusMap.put("msg", "저장되었습니다.");
        } catch (Exception e) {
            statusMap.put("result", "ERR");
            statusMap.put("msg", e.getMessage());
        }
        result.add(statusMap);
        return ResponseEntity.ok(result);
    }

    /**
     * 주간생산계획 일괄 저장 (표준 OK/ERR 리턴 구조 적용)
     */
    @PostMapping("/save")
    public ResponseEntity<List<Map<String, Object>>> savePdPlan(@RequestBody List<PdPlanDto> list, HttpSession session) {
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> statusMap = new HashMap<>();
        
        try {
            String cmpycd = session.getAttribute("cmpycd").toString();
            String userId = session.getAttribute("userid").toString();
            
            pdPlanService.savePdPlanList(list, cmpycd, userId);
            
            statusMap.put("result", "OK");
            statusMap.put("msg", "성공적으로 저장되었습니다.");
        } catch (Exception e) {
            log.error("❌ 생산계획 저장 오류: {}", e.getMessage());
            statusMap.put("result", "ERR");
            statusMap.put("msg", e.getMessage());
        }
        
        result.add(statusMap);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/move")
    public ResponseEntity<List<Map<String, Object>>> movePdPlanDate(@RequestBody Map<String, Object> params, HttpSession session) {
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> statusMap = new HashMap<>();
        
        try {
            String cmpycd = session.getAttribute("cmpycd").toString();
            String userId = session.getAttribute("userid").toString();
            params.put("cmpycd", cmpycd);
            params.put("updemp", userId);
            
            pdPlanService.movePdPlanDate(params);
            
            statusMap.put("result", "OK");
            statusMap.put("msg", "일자가 변경되었습니다.");
        } catch (Exception e) {
            statusMap.put("result", "ERR");
            statusMap.put("msg", e.getMessage());
        }
        
        result.add(statusMap);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/delete")
    public ResponseEntity<List<Map<String, Object>>> deletePdPlan(@RequestBody Map<String, Object> params, HttpSession session) {
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> statusMap = new HashMap<>();
        
        try {
            String cmpycd = session.getAttribute("cmpycd").toString();
            params.put("cmpycd", cmpycd);
            pdPlanService.deletePdPlan(params);
            
            statusMap.put("result", "OK");
            statusMap.put("msg", "삭제되었습니다.");
        } catch (Exception e) {
            statusMap.put("result", "ERR");
            statusMap.put("msg", e.getMessage());
        }
        
        result.add(statusMap);
        return ResponseEntity.ok(result);
    }
}
