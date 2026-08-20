package com.crmbank.erp.hafn.controller;

import com.crmbank.erp.comm.dto.ApiResponse;
import com.crmbank.erp.comm.dto.UserSession;
import com.crmbank.erp.hafn.mapper.HafnMapper;
import com.crmbank.erp.hafn.service.HafnService;
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
@RequestMapping("/hafn")
@RequiredArgsConstructor
public class HafnController {

    private final HafnMapper hafnMapper;
    private final HafnService hafnService;
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

        injectSession(params, session);
        String proc = procedure.toUpperCase();

        // 🚀 특수 업무 로직 엔드포인트 연동 (Java 호출 인지 및 통제 강화)
        switch (proc) {
            case "HAFN_610U_SAVE": return saveHafn610(params, session);
            case "HAFN_620U_SAVE": return saveHafn620(params, session);
            case "HAFN_630U_SAVE": return saveHafn630(params, session);
        }

        String actkind = String.valueOf(params.getOrDefault("actkind", "")).toUpperCase();
        
        try {
            fillMissingParameters(proc, params);

            log.info("🚀 [hafn] 실행 요청: {}", proc);
            
            List<Map<String, Object>> result;
            if (proc.endsWith("U_STR") && (actkind.startsWith("A") || actkind.startsWith("U"))) {
                String positionalSql = buildPositionalSql(proc, params);
                log.info("📋 [ASP 스타일 실행] SQL: {}", positionalSql);

                result = jdbcTemplate.query(positionalSql, (rs, rowNum) -> {
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
                log.info("🎯 [무결성 직접 수신 성공] 데이터: {}", result);
            } else {
                switch (proc) {
                    case "HAFN_010S_STR": result = hafnMapper.HAFN_010S_STR(params); break;
                    case "HAFN_110S_STR": result = hafnMapper.HAFN_110S_STR(params); break;
                    case "HAFN_120S_STR": result = hafnMapper.HAFN_120S_STR(params); break;
                    case "HAFN_210S_STR": result = hafnMapper.HAFN_210S_STR(params); break;
                    case "HAFN_310S_STR": result = hafnMapper.HAFN_310S_STR(params); break;
                    case "HAFN_410S_STR": result = hafnMapper.HAFN_410S_STR(params); break;
                    case "HAFN_420S_STR": result = hafnMapper.HAFN_420S_STR(params); break;
                    case "HAFN_430S_STR": result = hafnMapper.HAFN_430S_STR(params); break;
                    case "HAFN_510S_STR": result = hafnMapper.HAFN_510S_STR(params); break;
                    case "HAFN_520S_STR": result = hafnMapper.HAFN_520S_STR(params); break;
                    case "HAFN_610U_STR": result = hafnMapper.HAFN_610U_STR(params); break;
                    case "HAFN_620U_STR": result = hafnMapper.HAFN_620U_STR(params); break;
                    case "HAFN_630U_STR": result = hafnMapper.HAFN_630U_STR(params); break;
                    case "HAFN_670S_STR": result = hafnMapper.HAFN_670S_STR(params); break;
                    case "HAFN_680S_STR": result = hafnMapper.HAFN_680S_STR(params); break;
                    case "HAFN_690S_STR": result = hafnMapper.HAFN_690S_STR(params); break;
                    default:
                        return ResponseEntity.notFound().build();
                }
            }

            if (result == null || result.isEmpty()) {
                result = List.of(Map.of("res", "OK"));
            }
            return ResponseEntity.ok(convertToLowerCaseKeys(result));
        } catch (Exception e) {
            log.error("❌ [hafn] executeProcedure Error ({}): {}", proc, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
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

    @PostMapping("/HAFN_610U_SAVE")
    public ResponseEntity<ApiResponse<?>> saveHafn610(@RequestBody Map<String, Object> payload, HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user == null) return ResponseEntity.status(401).build();
        try {
            Map<String, Object> result = hafnService.saveHafn610(payload, user.getCmpycd(), user.getUserid());
            return ResponseEntity.ok(ApiResponse.success(result, "성공적으로 저장되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.serverError(e.getMessage()));
        }
    }

    @PostMapping("/HAFN_620U_SAVE")
    public ResponseEntity<ApiResponse<?>> saveHafn620(@RequestBody Map<String, Object> payload, HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user == null) return ResponseEntity.status(401).build();
        try {
            Map<String, Object> result = hafnService.saveHafn620(payload, user.getCmpycd(), user.getUserid());
            return ResponseEntity.ok(ApiResponse.success(result, "성공적으로 저장되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.serverError(e.getMessage()));
        }
    }

    @PostMapping("/HAFN_630U_SAVE")
    public ResponseEntity<ApiResponse<?>> saveHafn630(@RequestBody Map<String, Object> payload, HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user == null) return ResponseEntity.status(401).build();
        try {
            Map<String, Object> result = hafnService.saveHafn630(payload, user.getCmpycd(), user.getUserid());
            return ResponseEntity.ok(ApiResponse.success(result, "성공적으로 저장되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.serverError(e.getMessage()));
        }
    }

    private void injectSession(Map<String, Object> params, HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user != null) {
            if (params.get("cmpycd") == null || params.get("cmpycd").toString().trim().isEmpty()) {
                params.put("cmpycd", user.getCmpycd());
            }
            if (params.get("userid") == null || params.get("userid").toString().trim().isEmpty()) {
                params.put("userid", user.getUserid());
            }
            params.put("updemp", user.getUserid());
        }
    }

    private void fillMissingParameters(String proc, Map<String, Object> params) {
        try {
            String statementId = HafnMapper.class.getName() + "." + proc;
            if (!sqlSession.getConfiguration().hasStatement(statementId)) return;
            MappedStatement ms = sqlSession.getConfiguration().getMappedStatement(statementId);
            BoundSql boundSql = ms.getBoundSql(params);

            for (ParameterMapping pm : boundSql.getParameterMappings()) {
                String prop = pm.getProperty();
                if (prop != null && !prop.startsWith("_") && !prop.contains(".")) {
                    String cleanProp = prop.trim();
                    if (!params.containsKey(cleanProp) || params.get(cleanProp) == null || params.get(cleanProp).toString().trim().isEmpty()) {
                        params.put(cleanProp, "");
                    }
                    if (!cleanProp.equals(prop)) params.put(prop, params.get(cleanProp));
                }
            }
        } catch (Exception e) { log.warn("🛠 누락 파라미터 보정 중 알림 ({}): {}", proc, e.getMessage()); }
    }

    private String buildPositionalSql(String proc, Map<String, Object> params) {
        try {
            // 💡 [주의] 이 부분만 해당 컨트롤러의 매퍼 클래스명으로 수정하세요 (예: HsodMapper.class)
            String statementId = HafnMapper.class.getName() + "." + proc;

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

}
