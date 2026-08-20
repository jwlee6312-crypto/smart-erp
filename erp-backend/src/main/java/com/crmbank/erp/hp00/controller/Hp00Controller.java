package com.crmbank.erp.hp00.controller;

import com.crmbank.erp.comm.dto.UserSession;
import com.crmbank.erp.hp00.mapper.Hp00Mapper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/hp00")
@RequiredArgsConstructor
public class Hp00Controller {

    private final Hp00Mapper hp00Mapper;

    private void injectSession(Map<String, Object> params, HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user != null) {
            params.putIfAbsent("cmpycd", user.getCmpycd());
            params.putIfAbsent("userid", user.getUserid());
        }
        // 💡 HP00_000S_STR 명세 보장 (6개 변수)
        params.putIfAbsent("gubun", "");
        params.putIfAbsent("cmpycd", "");
        params.putIfAbsent("gbncd", "");
        params.putIfAbsent("code", "");
        params.putIfAbsent("codenm", "");
        params.putIfAbsent("etcval", "");
    }

    @GetMapping("/{procedure}")
    public ResponseEntity<List<Map<String, Object>>> executeGet(
            @PathVariable String procedure, @RequestParam Map<String, Object> params, HttpSession session) {
        return execute(procedure, new HashMap<>(params), session);
    }

    @PostMapping("/{procedure}")
    public ResponseEntity<List<Map<String, Object>>> executePost(
            @PathVariable String procedure, @RequestBody Map<String, Object> params, HttpSession session) {
        return execute(procedure, params, session);
    }

    private ResponseEntity<List<Map<String, Object>>> execute(String procedure, Map<String, Object> params, HttpSession session) {
        injectSession(params, session);
        log.info("📋 [HP00] SSMS 실행용: {}", buildSsmsLog(procedure, params));
        
        try {
            List<Map<String, Object>> result;
            if ("HP00_000S_STR".equalsIgnoreCase(procedure)) {
                result = hp00Mapper.HP00_000S_STR(params);
            } else {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("❌ [HP00] 에러: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    private String buildSsmsLog(String procedure, Map<String, Object> params) {
        String[] keys = {"gubun", "cmpycd", "gbncd", "code", "codenm", "etcval"};
        String values = java.util.Arrays.stream(keys)
                .map(key -> {
                    Object val = params.get(key);
                    return val == null ? "''" : "'" + val.toString().trim() + "'";
                })
                .collect(Collectors.joining(", "));
        return String.format("EXEC %s %s", procedure.toUpperCase(), values);
    }
}
