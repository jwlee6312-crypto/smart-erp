package com.crmbank.erp.hsod.controller;

import com.crmbank.erp.comm.dto.ApiResponse;
import com.crmbank.erp.comm.dto.UserSession;
import com.crmbank.erp.hsod.dto.Hsod100uRequest;
import com.crmbank.erp.hsod.mapper.HsodMapper;
import com.crmbank.erp.hsod.service.HsodService;
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
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/hsod")
@RequiredArgsConstructor
public class HsodController {

    private final HsodMapper hsodMapper;
    private final HsodService hsodService;
    private final SqlSession sqlSession;
    private final JdbcTemplate jdbcTemplate;

    @Transactional(rollbackFor = Exception.class)
    @PostMapping("/HSOD_100U_SAVE")
    public ResponseEntity<?> saveOrder(@RequestBody Hsod100uRequest request, HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user == null) return ResponseEntity.status(401).build();

        try {
            String userId = user.getUserid();
            String cmpycd = user.getCmpycd();

            if (request.getMst() != null) {
                request.getMst().setCmpycd(cmpycd);
                request.getMst().setUpdemp(userId);
            }
            Map<String, Object> result = hsodService.saveOrder(request, userId);
            return ResponseEntity.ok(ApiResponse.success(result, "Successfully saved."));
        } catch (Exception e) {
            log.error("❌ [hsod] Save Error: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(ApiResponse.serverError(e.getMessage()));
        }
    }

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

            String actkind = String.valueOf(params.getOrDefault("actkind", "")).toUpperCase();
            if (proc.length() >= 9 && proc.charAt(8) == 'U' && (actkind.startsWith("A") || actkind.startsWith("U"))) {
                String validationMsg = validateParameters(HsodMapper.class, proc, params);
                if (validationMsg != null) {
                    return ResponseEntity.badRequest().body(Map.of(
                        "status", "VALIDATION_ERROR",
                        "message", "🛠 [PROGRAM VALID ALARM]\n" + validationMsg
                    ));
                }
            }

            log.info("📋 [hsod] 실행 요청: {}", proc);
            
            List<Map<String, Object>> resultList;
            if (proc.endsWith("U_STR") && (actkind.startsWith("A") || actkind.startsWith("U"))) {
                String positionalSql = buildPositionalSql(proc, params);
                log.info("📋 [ASP 스타일 실행] SQL: {}", positionalSql);

                resultList = jdbcTemplate.query(positionalSql, (rs, rowNum) -> {
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
                    row.put("returnkeyvalue", values); // 💡 시스템 표준에 맞춰 소문자로 지정
                    return row;
                });
                log.info("🎯 [무결성 직접 수신 성공] 데이터: {}", resultList);
            } else {
                switch (proc) {
                    case "HSOD_100U_STR": resultList = hsodMapper.HSOD_100U_STR(params); break;
                    case "HSOD_101U_STR": resultList = hsodMapper.HSOD_101U_STR(params); break;
                    case "HSOD_110S_STR": resultList = hsodMapper.HSOD_110S_STR(params); break;
                    case "HSOD_120U_STR": resultList = hsodMapper.HSOD_120U_STR(params); break;
                    case "HSOD_200U_STR": resultList = hsodMapper.HSOD_200U_STR(params); break;
                    case "HSOD_210U_STR": resultList = hsodMapper.HSOD_210U_STR(params); break;
                    case "HSOD_300U_STR": resultList = hsodMapper.HSOD_300U_STR(params); break;
                    default:
                        return ResponseEntity.notFound().build();
                }
            }

            if (resultList == null || resultList.isEmpty()) {
                if (actkind.startsWith("S")) {
                    resultList = new ArrayList<>();
                } else {
                    resultList = List.of(Map.of("res", "OK"));
                }
            }

            // 🚀 [Pilot 적용] 모든 결과를 소문자로 강제 변환하여 프론트엔드 표준 준수
            return ResponseEntity.ok(convertToLowerCaseKeys(resultList));

        } catch (Exception e) {
            log.error("❌ [hsod] executeProcedure Error ({}): {}", proc, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Map의 모든 Key를 소문자로 변환하여 일관성 보장
     */
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
            String statementId = HsodMapper.class.getName() + "." + proc;
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
            String statementId = HsodMapper.class.getName() + "." + proc;

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

    private String validateParameters(Class<?> mapperClass, String proc, Map<String, Object> vueParams) {
        try {
            String statementId = mapperClass.getName() + "." + proc;
            if (!sqlSession.getConfiguration().hasStatement(statementId)) return null;
            MappedStatement ms = sqlSession.getConfiguration().getMappedStatement(statementId);
            BoundSql boundSql = ms.getBoundSql(vueParams);
            List<ParameterMapping> xmlMappings = boundSql.getParameterMappings();
            Set<String> xmlKeys = new LinkedHashSet<>();
            for (ParameterMapping pm : xmlMappings) {
                String prop = pm.getProperty();
                if (prop != null && !prop.startsWith("_") && !prop.contains(".")) xmlKeys.add(prop);
            }
            Set<String> vueKeys = vueParams.keySet();
            if (vueKeys.size() < xmlKeys.size()) {
                return String.format("📍 [PARAM SHORTAGE] XML:%d > VUE:%d\n📋 [REQUIRED]: %s",
                    xmlKeys.size(), vueKeys.size(), xmlKeys);
            }
            return null;
        } catch (Exception e) { return "VALIDATION ERROR: " + e.getMessage(); }
    }
}
