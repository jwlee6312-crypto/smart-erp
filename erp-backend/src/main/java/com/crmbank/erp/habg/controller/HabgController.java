package com.crmbank.erp.habg.controller;

import com.crmbank.erp.comm.dto.UserSession;
import com.crmbank.erp.habg.mapper.HabgMapper;
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
@RequestMapping("/habg")
@RequiredArgsConstructor
public class HabgController {

    private final HabgMapper habgMapper;
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

            String actkind = Objects.requireNonNullElse(params.get("actkind"), "").toString().toUpperCase();
            if (proc.length() >= 9 && proc.charAt(8) == 'U' && (actkind.startsWith("A") || actkind.startsWith("U"))) {
                String validationMsg = validateParameters(proc, params);
                if (validationMsg != null) {
                    return ResponseEntity.badRequest().body(Map.of(
                        "status", "VALIDATION_ERROR",
                        "message", "🛠 [PROGRAM VALID ALARM]\n" + validationMsg
                    ));
                }
            }

            log.info("📋 [habg] 실행 요청: {}", proc);

            List<Map<String, Object>> result;
            if (proc.endsWith("U_STR") && (actkind.startsWith("A") || actkind.startsWith("U"))) {
                result = executeJdbcQuery(proc, params);
            } else {
                result = switch (proc) {
                    case "HABG_010U_STR" -> habgMapper.HABG_010U_STR(params);
                    case "HABG_020S_STR" -> habgMapper.HABG_020S_STR(params);
                    case "HABG_030U_STR" -> habgMapper.HABG_030U_STR(params);
                    case "HABG_050U_STR" -> habgMapper.HABG_050U_STR(params);
                    case "HABG_060U_STR" -> habgMapper.HABG_060U_STR(params);
                    case "HABG_070S_STR" -> habgMapper.HABG_070S_STR(params);
                    case "HABG_110U_STR" -> habgMapper.HABG_110U_STR(params);
                    case "HABG_120U_STR" -> habgMapper.HABG_120U_STR(params);
                    case "HABG_210S_STR" -> habgMapper.HABG_210S_STR(params);
                    case "HABG_220S_STR" -> habgMapper.HABG_220S_STR(params);
                    case "HABG_230S_STR" -> habgMapper.HABG_230S_STR(params);
                    default -> null;
                };
                if (result == null) {
                    log.warn("❌ [habg] Unregistered procedure: {}", proc);
                    return ResponseEntity.notFound().build();
                }
            }

            if (result.isEmpty()) {
                result = List.of(Map.of("res", "OK"));
            }
            
            // 🚀 모든 결과를 소문자로 강제 변환하여 프론트엔드 표준 준수
            return ResponseEntity.ok(convertToLowerCaseKeys(result));

        } catch (Exception e) {
            log.error("❌ [habg] executeProcedure Error ({}): {}", proc, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    private List<Map<String, Object>> executeJdbcQuery(String proc, Map<String, Object> params) {
        String positionalSql = buildPositionalSql(proc, params);
        log.info("📋 [ASP 스타일 실행] SQL: {}", positionalSql);

        return jdbcTemplate.query(positionalSql, (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            List<Object> values = new ArrayList<>();
            int colCount = rs.getMetaData().getColumnCount();
            for (int i = 1; i <= colCount; i++) {
                Object val = rs.getObject(i);
                String colName = rs.getMetaData().getColumnLabel(i); 
                if (colName == null || colName.isEmpty()) colName = "col_" + (i-1);
                row.put(colName.toLowerCase(), val == null ? "" : val);
                values.add(val == null ? "" : val);
            }
            row.put("returnkeyvalue", values); 
            return row;
        });
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
            String statementId = HabgMapper.class.getName() + "." + proc;
            if (!sqlSession.getConfiguration().hasStatement(statementId)) return;
            MappedStatement ms = sqlSession.getConfiguration().getMappedStatement(statementId);
            BoundSql boundSql = ms.getBoundSql(params);

            for (ParameterMapping pm : boundSql.getParameterMappings()) {
                String prop = pm.getProperty();
                if (prop != null && !prop.startsWith("_") && !prop.contains(".")) {
                    params.putIfAbsent(prop.trim(), "");
                }
            }
        } catch (Exception e) { log.warn("🛠 누락 파라미터 보정 중 알림 ({}): {}", proc, e.getMessage()); }
    }

    private String buildPositionalSql(String proc, Map<String, Object> params) {
        try {
            String statementId = HabgMapper.class.getName() + "." + proc;
            if (!sqlSession.getConfiguration().hasStatement(statementId)) return "EXEC " + proc;
            MappedStatement ms = sqlSession.getConfiguration().getMappedStatement(statementId);
            BoundSql boundSql = ms.getBoundSql(params);
            List<String> values = new ArrayList<>();

            for (ParameterMapping pm : boundSql.getParameterMappings()) {
                Object val = params.get(pm.getProperty().trim());
                String valStr = (val == null || "null".equals(String.valueOf(val))) ? "''" : "N'" + val.toString().replace("'", "''").trim() + "'";
                values.add(valStr);
            }
            return String.format("EXEC %s %s", proc, String.join(", ", values));
        } catch (Exception e) { return "EXEC " + proc; }
    }

    private String validateParameters(String proc, Map<String, Object> vueParams) {
        try {
            String statementId = HabgMapper.class.getName() + "." + proc;
            if (!sqlSession.getConfiguration().hasStatement(statementId)) return null;
            MappedStatement ms = sqlSession.getConfiguration().getMappedStatement(statementId);
            BoundSql boundSql = ms.getBoundSql(vueParams);
            List<ParameterMapping> xmlMappings = boundSql.getParameterMappings();
            Set<String> xmlKeys = new LinkedHashSet<>();
            for (ParameterMapping pm : xmlMappings) {
                String prop = pm.getProperty();
                if (prop != null && !prop.startsWith("_") && !prop.contains(".")) xmlKeys.add(prop);
            }
            if (vueParams.size() < xmlKeys.size()) {
                return String.format("📍 [PARAM SHORTAGE] XML:%d > VUE:%d\n📋 [REQUIRED]: %s", xmlKeys.size(), vueParams.size(), xmlKeys);
            }
            return null;
        } catch (Exception e) { return "VALIDATION ERROR: " + e.getMessage(); }
    }
}
