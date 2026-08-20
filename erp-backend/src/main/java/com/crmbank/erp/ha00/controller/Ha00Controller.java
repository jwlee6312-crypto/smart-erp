package com.crmbank.erp.ha00.controller;

import com.crmbank.erp.comm.dto.UserSession;
import com.crmbank.erp.ha00.mapper.Ha00Mapper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/ha00")
@RequiredArgsConstructor
public class Ha00Controller {

    private final Ha00Mapper ha00Mapper;

    @Transactional(rollbackFor = Exception.class)
    @PostMapping("/{procedure}")
    public ResponseEntity<List<Map<String, Object>>> executePost(
            @PathVariable String procedure, @RequestBody Map<String, Object> params, HttpSession session) {
        
        if (session.getAttribute("user_session") == null) {
            return ResponseEntity.status(401).build();
        }

        injectSession(params, session);
        
        try {
            String proc = procedure.toUpperCase();
            List<Map<String, Object>> result;
            
            if ("HA00_00P_STR".equals(proc)) {
                result = ha00Mapper.HA00_00P_STR(params);
            } else if ("HA00_010S_STR".equals(proc)) {
                result = ha00Mapper.HA00_010S_STR(params);
            } else {
                return ResponseEntity.notFound().build();
            }

            // 🚀 [원복] 조회성 API는 순수 데이터 리스트만 반환 (그리드 바인딩 깨짐 방지)
            return ResponseEntity.ok(result != null ? result : new ArrayList<>());
            
        } catch (Exception e) {
            log.error("❌ [HA00] 에러: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{procedure}")
    public ResponseEntity<List<Map<String, Object>>> executeGet(
            @PathVariable String procedure, @RequestParam Map<String, Object> params, HttpSession session) {
        return executePost(procedure, new HashMap<>(params), session);
    }

    private void injectSession(Map<String, Object> params, HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user != null) {
            params.putIfAbsent("cmpycd", user.getCmpycd());
            params.putIfAbsent("userid", user.getUserid());
        }
    }
}
