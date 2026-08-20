package com.crmbank.erp.hsst.controller;

import com.crmbank.erp.comm.dto.UserSession;
import com.crmbank.erp.hsst.mapper.HsstMapper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.SqlSession;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.*;

/**
 * [HSST] 현황조회 통합 컨트롤러 (사용자 정의 최종 표준형)
 */
@Slf4j
@RestController
@RequestMapping("/hsst")
@RequiredArgsConstructor
public class HsstController {

    private final HsstMapper hsstMapper;
    private final SqlSession sqlSession;
    private final JdbcTemplate jdbcTemplate;

    @Transactional(rollbackFor = Exception.class)
    @PostMapping("/{procedure}")
    public ResponseEntity<?> executeProcedure(@PathVariable String procedure, @RequestBody Map<String, Object> params, HttpSession session) {
        String proc = procedure.toUpperCase();
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user == null) return ResponseEntity.status(401).build();

        try {
            // 💡 표준 파라미터 강제 세팅 (updemp 포함)
            params.put("cmpycd", user.getCmpycd());
            params.put("userid", user.getUserid());
            params.put("updemp", user.getUserid());

            List<Map<String, Object>> resultList = new ArrayList<>();

            if (params.get("items") instanceof List<?> items) {
                for (Object itemObj : items) {
                    if (itemObj instanceof Map<?, ?> item) {
                        Map<String, Object> p = new HashMap<>(params);
                        p.putAll((Map<String, Object>) item);
                        p.remove("items");
                        resultList.addAll(executeInternal(proc, p));
                    }
                }
            } else {
                resultList = executeInternal(proc, params);
            }

            return ResponseEntity.ok(convertToLowerCaseKeys(resultList));

        } catch (Exception e) {
            log.error("❌ [HSST] {} Error: {}", proc, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    private List<Map<String, Object>> executeInternal(String proc, Map<String, Object> params) {
        String actkind = String.valueOf(params.getOrDefault("actkind", "")).toUpperCase().trim();

        // 💡 표준: 쓰기 액션(A, U, D, DR) 및 집계(C)인 경우 무결성 수신을 위해 직접 실행 및 로깅
        if (proc.endsWith("U_STR") && (actkind.startsWith("A") || actkind.startsWith("U") || actkind.startsWith("D") || actkind.equals("DR") || actkind.equals("C"))) {
            return executeDirectSql(proc, params);
        }

        // 💡 사용자 정의 표준: 명시적인 Switch-Case 호출
        List<Map<String, Object>> result;
        switch (proc) {
            case "HSST_100S_STR": result = hsstMapper.HSST_100S_STR(params); break;
            case "HSST_120S_STR": result = hsstMapper.HSST_120S_STR(params); break;
            case "HSST_130S_STR": result = hsstMapper.HSST_130S_STR(params); break;
            case "HSST_150S_STR": result = hsstMapper.HSST_150S_STR(params); break;
            case "HSST_180S_STR": result = hsstMapper.HSST_180S_STR(params); break;
            case "HSST_200S_STR": result = hsstMapper.HSST_200S_STR(params); break;
            case "HSST_210S_STR": result = hsstMapper.HSST_210S_STR(params); break;
            case "HSST_300S_STR": result = hsstMapper.HSST_300S_STR(params); break;
            case "HSST_320S_STR": result = hsstMapper.HSST_320S_STR(params); break;
            case "HSST_340S_STR": result = hsstMapper.HSST_340S_STR(params); break;
            case "HSST_360S_STR": result = hsstMapper.HSST_360S_STR(params); break;
            case "HSST_510S_STR": result = hsstMapper.HSST_510S_STR(params); break;
            case "HSST_520S_STR": result = hsstMapper.HSST_520S_STR(params); break;
            case "HSST_570S_STR": result = hsstMapper.HSST_570S_STR(params); break;
            case "HSST_600S_STR": result = hsstMapper.HSST_600S_STR(params); break;
            case "HSST_610S_STR": result = hsstMapper.HSST_610S_STR(params); break;
            default:
                result = invokeMapper(proc, params);
                if (result == null) result = executeDirectSql(proc, params);
                break;
        }
        return result != null ? result : new ArrayList<>();
    }

    private List<Map<String, Object>> invokeMapper(String proc, Map<String, Object> params) {
        try {
            Method method = HsstMapper.class.getMethod(proc, Map.class);
            return (List<Map<String, Object>>) method.invoke(hsstMapper, params);
        } catch (NoSuchMethodException e) {
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private List<Map<String, Object>> executeDirectSql(String proc, Map<String, Object> params) {
        String sql = buildPositionalSql(proc, params);
        log.info("==>  Direct Executing: {}", sql);
        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                Map<String, Object> row = new LinkedHashMap<>();
                int colCount = rs.getMetaData().getColumnCount();
                for (int k = 1; k <= colCount; k++) {
                    String label = rs.getMetaData().getColumnLabel(k).toLowerCase();
                    row.put(label, rs.getObject(k) == null ? "" : rs.getObject(k));
                }
                row.put("returnkeyvalue", new ArrayList<>());
                return row;
            });
        } catch (Exception e) {
            if (e.getMessage() != null && (e.getMessage().contains("No ResultSet") || e.getMessage().contains("did not return a result set"))) {
                return new ArrayList<>();
            }
            throw e;
        }
    }

    private String buildPositionalSql(String proc, Map<String, Object> params) {
        try {
            // 💡 [주의] 이 부분만 해당 컨트롤러의 매퍼 클래스명으로 수정하세요 (예: HsodMapper.class)
            String statementId = HsstMapper.class.getName() + "." + proc;

            if (!sqlSession.getConfiguration().hasStatement(statementId)) return "EXEC " + proc;
            BoundSql boundSql = sqlSession.getConfiguration().getMappedStatement(statementId).getBoundSql(params);
            List<String> values = new ArrayList<>();

            for (ParameterMapping pm : boundSql.getParameterMappings()) {
                // XML에 정의된 #{이름}과 100% 일치하는 값만 추출 (VUE 순서 상관없음)
                Object val = params.get(pm.getProperty().trim());

                // NULL/공백 치환 및 유니코드(N) 처리하여 왜곡 차단
                String valStr = (val == null || "null".equals(String.valueOf(val))) ? "''" : "N'" + val.toString().replace("'", "''").trim() + "'";
                values.add(valStr);
            }
            return String.format("EXEC %s %s", proc, String.join(", ", values));
        } catch (Exception e) { return "EXEC " + proc; }
    }

    private List<Map<String, Object>> convertToLowerCaseKeys(List<Map<String, Object>> list) {
        if (list == null) return new ArrayList<>();
        List<Map<String, Object>> newList = new ArrayList<>();
        for (Map<String, Object> map : list) {
            Map<String, Object> newMap = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                newMap.put(entry.getKey().toLowerCase(), entry.getValue());
            }
            newList.add(newMap);
        }
        return newList;
    }
}
