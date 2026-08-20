package com.crmbank.erp.haba.controller;

import com.crmbank.erp.comm.dto.UserSession;
import com.crmbank.erp.haba.mapper.HabaMapper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.SqlSession;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/haba")
@RequiredArgsConstructor
public class HabaController {

    private final HabaMapper habaMapper;
    private final SqlSession sqlSession;
    private final JdbcTemplate jdbcTemplate;

    @Transactional(rollbackFor = Exception.class)
    @PostMapping("/{procedure}")
    public ResponseEntity<?> executeProcedure(
            @PathVariable String procedure,
            @RequestBody Map<String, Object> params,
            HttpSession session) {

        if (session.getAttribute("user_session") == null) {
            return ResponseEntity.status(401).build();
        }

        String proc = procedure.toUpperCase();
        try {
            injectSession(params, session);
            fillMissingParameters(proc, params);

            log.info("📋 [haba] 실행 요청: {}", proc);

            List<Map<String, Object>> result = switch (proc) {
                case "HABA_YYYY_S" -> habaMapper.HABA_YYYY_S(params);
                case "HABA_010U_STR" -> habaMapper.HABA_010U_STR(params);
                case "HABA_020U_STR" -> habaMapper.HABA_020U_STR(params);
                case "HABA_021U_STR" -> habaMapper.HABA_021U_STR(params);
                case "HABA_022U_STR" -> habaMapper.HABA_022U_STR(params);
                case "HABA_030U_STR" -> habaMapper.HABA_030U_STR(params);
                case "HABA_040U_STR" -> habaMapper.HABA_040U_STR(params);
                case "HABA_050U_STR" -> habaMapper.HABA_050U_STR(params);
                case "HABA_060U_STR" -> habaMapper.HABA_060U_STR(params);
                case "HABA_070U_STR" -> habaMapper.HABA_070U_STR(params);
                case "HABA_080U_STR" -> habaMapper.HABA_080U_STR(params);
                case "HABA_090U_STR" -> habaMapper.HABA_090U_STR(params);
                case "HABA_100U_STR" -> habaMapper.HABA_100U_STR(params);
                case "HABA_110U_STR" -> habaMapper.HABA_110U_STR(params);
                case "HABA_120U_STR" -> habaMapper.HABA_120U_STR(params);
                case "HABA_130U_STR" -> habaMapper.HABA_130U_STR(params);
                case "HABA_140U_STR" -> habaMapper.HABA_140U_STR(params);
                case "HABA_150U_STR" -> habaMapper.HABA_150U_STR(params);
                case "HABA_160U_STR" -> habaMapper.HABA_160U_STR(params);
                case "HABA_170U_STR" -> habaMapper.HABA_170U_STR(params);
                case "HABA_180U_STR" -> habaMapper.HABA_180U_STR(params);
                case "HABA_190S_STR" -> habaMapper.HABA_190S_STR(params);
                case "HABA_210U_STR" -> habaMapper.HABA_210U_STR(params);
                case "HABA_220U_STR" -> habaMapper.HABA_220U_STR(params);
                case "HABA_230U_STR" -> habaMapper.HABA_230U_STR(params);
                case "HABA_240U_STR" -> habaMapper.HABA_240U_STR(params);
                case "HABA_250U_STR" -> habaMapper.HABA_250U_STR(params);
                case "HABA_260U_STR" -> habaMapper.HABA_260U_STR(params);
                case "HABA_510U_STR" -> habaMapper.HABA_510U_STR(params);
                case "HABA_900U_STR" -> habaMapper.HABA_900U_STR(params);
                case "HABA_910U_STR" -> habaMapper.HABA_910U_STR(params);
                case "HABA_920U_STR" -> habaMapper.HABA_920U_STR(params);
                case "HABA_935U_STR" -> habaMapper.HABA_935U_STR(params);
                default -> null;
            };

            if (result == null) return ResponseEntity.notFound().build();
            return ResponseEntity.ok(convertToLowerCaseKeys(result));
        } catch (Exception e) {
            log.error("❌ [haba] Error: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    private List<Map<String, Object>> convertToLowerCaseKeys(List<Map<String, Object>> list) {
        List<Map<String, Object>> newList = new ArrayList<>();
        for (Map<String, Object> map : list) {
            Map<String, Object> newMap = new LinkedHashMap<>();
            map.forEach((k, v) -> newMap.put(k.toLowerCase(), v));
            newList.add(newMap);
        }
        return newList;
    }

    private void injectSession(Map<String, Object> params, HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user != null) {
            params.putIfAbsent("cmpycd", user.getCmpycd());
            params.putIfAbsent("userid", user.getUserid());
            params.put("updemp", user.getUserid());
        }
    }

    private void fillMissingParameters(String proc, Map<String, Object> params) {
        try {
            String statementId = HabaMapper.class.getName() + "." + proc;
            if (!sqlSession.getConfiguration().hasStatement(statementId)) return;
            MappedStatement ms = sqlSession.getConfiguration().getMappedStatement(statementId);
            BoundSql boundSql = ms.getBoundSql(params);
            for (ParameterMapping pm : boundSql.getParameterMappings()) {
                String prop = pm.getProperty();
                if (prop != null && !prop.startsWith("_") && !prop.contains(".")) {
                    params.putIfAbsent(prop.trim(), "");
                }
            }
        } catch (Exception ignored) {}
    }

    @Transactional
    @PostMapping("/update-my-status")
    public ResponseEntity<?> updateMyStatus(@RequestBody Map<String, Object> data, HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user == null) return ResponseEntity.status(401).build();

        String cmpycd = user.getCmpycd();
        String userid = user.getUserid();
        String status = String.valueOf(data.get("status"));
        String routingMode = String.valueOf(data.get("routing_mode"));

        jdbcTemplate.update("UPDATE HABA920T_TBL SET status = ?, routing_mode = ?, updtime = GETDATE(), updemp = ? WHERE cmpycd = ? AND userid = ?",
                status, routingMode, userid, cmpycd, userid);

        user.setStatus(status);
        user.setRouting_mode(routingMode);
        session.setAttribute("user_session", user);

        return ResponseEntity.ok(Map.of("success", true));
    }
}
