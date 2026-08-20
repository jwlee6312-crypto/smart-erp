package com.crmbank.erp.hafa.controller;

import com.crmbank.erp.comm.dto.ApiResponse;
import com.crmbank.erp.comm.dto.UserSession;
import com.crmbank.erp.hafa.mapper.HafaMapper;
import com.crmbank.erp.hafa.service.HafaService;
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
@RequestMapping("/hafa")
@RequiredArgsConstructor
public class HafaController {

    private final HafaMapper hafaMapper;
    private final HafaService hafaService;
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

        // 🚀 특수 엔드포인트 수동 매핑 (Java 호출 인지 및 통제 강화)
        if ("HAFA_150U_SAVE".equals(proc)) {
            return saveHafa150U(params, session);
        }

        injectSession(params, session);
        String actkind = Objects.requireNonNullElse(params.get("actkind"), "").toString().toUpperCase();

        try {
            fillMissingParameters(proc, params);
            log.info("📋 [hafa] 실행 요청: {}", proc);

            List<Map<String, Object>> result;
            if (proc.endsWith("U_STR") && (actkind.startsWith("A") || actkind.startsWith("U"))) {
                result = executeJdbcQuery(proc, params);
            } else {
                result = switch (proc) {
                    case "HAFA_010U_STR" -> hafaMapper.HAFA_010U_STR(params);
                    case "HAFA_020S_STR" -> hafaMapper.HAFA_020S_STR(params);
                    case "HAFA_040S_STR" -> hafaMapper.HAFA_040S_STR(params);
                    case "HAFA_050U_STR" -> hafaMapper.HAFA_050U_STR(params);
                    case "HAFA_090U_STR" -> hafaMapper.HAFA_090U_STR(params);
                    case "HAFA_120S_STR" -> hafaMapper.HAFA_120S_STR(params);
                    case "HAFA_130S_STR" -> hafaMapper.HAFA_130S_STR(params);
                    case "HA00_150S_STR" -> hafaMapper.HA00_150S_STR(params);
                    case "HAFA_140S_STR" -> hafaMapper.HAFA_140S_STR(params);
                    case "HAFA_150U_STR" -> hafaMapper.HAFA_150U_STR(params);
                    case "HAFA_900U_STR" -> hafaMapper.HAFA_900U_STR(params);
                    default -> null;
                };
                if (result == null) return ResponseEntity.notFound().build();
            }

            if (result.isEmpty()) {
                result = List.of(Map.of("res", "OK"));
            }
            
            return ResponseEntity.ok(convertToLowerCaseKeys(result));

        } catch (Exception e) {
            log.error("❌ [hafa] executeProcedure Error ({}): {}", proc, e.getMessage());
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
                row.put(colName.toLowerCase(), Objects.requireNonNullElse(val, ""));
                values.add(Objects.requireNonNullElse(val, ""));
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

    @PostMapping("/HAFA_150U_SAVE")
    public ResponseEntity<ApiResponse<?>> saveHafa150U(@RequestBody Map<String, Object> params, HttpSession session) {
        if (session.getAttribute("user_session") == null) {
            return ResponseEntity.status(401).build();
        }
        injectSession(params, session);
        try {
            Map<String, Object> result = hafaService.saveDepreciationSlip(params);
            return ResponseEntity.ok(ApiResponse.success(result, "성공"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.serverError(e.getMessage()));
        }
    }

    private void injectSession(Map<String, Object> params, HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user != null) {
            params.putIfAbsent("cmpycd", user.getCmpycd());
            params.putIfAbsent("userid", user.getUserid());
            params.put("updemp", user.getUserid());
            params.putIfAbsent("usernm", user.getUsernm());
        }
    }

    private void fillMissingParameters(String proc, Map<String, Object> params) {
        try {
            String statementId = HafaMapper.class.getName() + "." + proc;
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
            String statementId = HafaMapper.class.getName() + "." + proc;
            if (!sqlSession.getConfiguration().hasStatement(statementId)) return "EXEC " + proc;
            BoundSql boundSql = sqlSession.getConfiguration().getMappedStatement(statementId).getBoundSql(params);
            List<String> values = new ArrayList<>();

            for (ParameterMapping pm : boundSql.getParameterMappings()) {
                Object val = params.get(pm.getProperty().trim());
                String valStr = (val == null || "null".equals(String.valueOf(val))) ? "''" : "N'" + val.toString().replace("'", "''").trim() + "'";
                values.add(valStr);
            }
            return String.format("EXEC %s %s", proc, String.join(", ", values));
        } catch (Exception e) { return "EXEC " + proc; }
    }
}
