package com.crmbank.erp.hpio.controller;

import com.crmbank.erp.comm.dto.UserSession;
import com.crmbank.erp.hpio.mapper.HpioMapper;
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
@RequestMapping("/hpio")
@RequiredArgsConstructor
public class HpioController {

    private final HpioMapper hpioMapper;
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

            String actkind = String.valueOf(params.getOrDefault("actkind", "")).toUpperCase();
            if (proc.length() >= 9 && proc.charAt(8) == 'U' && (actkind.startsWith("A") || actkind.startsWith("U"))) {
                String validationMsg = validateParameters(proc, params);
                if (validationMsg != null) {
                    return ResponseEntity.badRequest().body(Map.of(
                        "status", "VALIDATION_ERROR",
                        "message", "🛠 [PROGRAM VALID ALARM]\n" + validationMsg
                    ));
                }
            }

            log.info("📋 [hpio] 실행 요청: {}", proc);
            
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
                        
                        // 🚀 [지시사항] HSIO 패턴: 프로시저별 리턴 필드명(Alias) 강제 지정
                        if (colName == null || colName.isEmpty() || colName.toLowerCase().startsWith("col")) {
                            if (proc.equals("HPIO_290U_STR")) {
                                if (i == 1) colName = "pumym";
                                else if (i == 2) colName = "pumno";
                                else if (i == 3) colName = "rtn_msg";
                            }

                            if (proc.equals("HPIO_340U_STR")) {
                                if (i == 1) colName = "outym";
                                else if (i == 2) colName = "outno";
                                else if (i == 3) colName = "inno";
                            }
                            if (proc.equals("HPIO_110U_STR")) {
                                if (i == 1) colName = "jsanym";
                                else if (i == 2) colName = "jsanno";
                            }
                            if (proc.equals("HPIO_500U_STR")) {
                                if (i == 1) colName = "ioym";
                                else if (i == 2) colName = "iono";
                                else if (i == 3) colName = "inno";
                            }
                            if (proc.equals("HPIO_510U_STR")) {
                                if (i == 1) colName = "ioym";
                                else if (i == 2) colName = "iono";
                                else if (i == 3) colName = "ino";
                            }
                            if (proc.equals("HPIO_520U_STR")) {
                                if (i == 1) colName = "ioym";
                                else if (i == 2) colName = "iono";
                            }
                        }
                        if (colName == null || colName.isEmpty()) colName = "col_" + (i-1);
                        row.put(colName.toLowerCase(), val == null ? "" : val);
                        values.add(val == null ? "" : val);
                    }
                    row.put("returnkeyvalue", values); // 💡 시스템 표준에 맞춰 소문자로 지정
                    return row;
                });
                log.info("🎯 [succ] data: {}", result);
            } else {
                switch (proc) {
                    case "HPIO_110U_STR": result = hpioMapper.HPIO_110U_STR(params); break;
                    case "HPIO_200U_STR": result = hpioMapper.HPIO_200U_STR(params); break;
                    case "HPIO_210U_STR": result = hpioMapper.HPIO_210U_STR(params); break;
                    case "HPIO_230S_STR": result = hpioMapper.HPIO_230S_STR(params); break;
                    case "HPIO_250U_STR": result = hpioMapper.HPIO_250U_STR(params); break;
                    case "HPIO_251U_STR": result = hpioMapper.HPIO_251U_STR(params); break;
                    case "HPIO_290U_STR": result = hpioMapper.HPIO_290U_STR(params); break;
                    case "HPIO_291U_STR": result = hpioMapper.HPIO_291U_STR(params); break;
                    case "HPIO_292U_STR": result = hpioMapper.HPIO_292U_STR(params); break;
                    case "HPIO_300U_STR": result = hpioMapper.HPIO_300U_STR(params); break;
                    case "HPIO_301U_STR": result = hpioMapper.HPIO_301U_STR(params); break;
                    case "HPIO_340U_STR": result = hpioMapper.HPIO_340U_STR(params); break;
                    case "HPIO_340U_POPUP": result = hpioMapper.HPIO_340U_POPUP(params); break;
                    case "HPIO_341U_STR": result = hpioMapper.HPIO_341U_STR(params); break;
                    case "HPIO_350U_STR": result = hpioMapper.HPIO_350U_STR(params); break;
                    case "HPIO_351U_STR": result = hpioMapper.HPIO_351U_STR(params); break;
                    case "HPIO_360S_STR": result = hpioMapper.HPIO_360S_STR(params); break;
                    case "HPIO_370S_STR": result = hpioMapper.HPIO_370S_STR(params); break;
                    case "HPIO_380S_STR": result = hpioMapper.HPIO_380S_STR(params); break;
                    case "HPIO_390S_STR": result = hpioMapper.HPIO_390S_STR(params); break;
                    case "HPIO_400U_STR": result = hpioMapper.HPIO_400U_STR(params); break;
                    case "HPIO_410U_STR": result = hpioMapper.HPIO_410U_STR(params); break;
                    case "HPIO_420S_STR": result = hpioMapper.HPIO_420S_STR(params); break;
                    case "HPIO_430S_STR": result = hpioMapper.HPIO_430S_STR(params); break;
                    case "HPIO_500U_STR": result = hpioMapper.HPIO_500U_STR(params); break;
                    case "HPIO_501U_STR": result = hpioMapper.HPIO_501U_STR(params); break;
                    case "HPIO_510U_STR": result = hpioMapper.HPIO_510U_STR(params); break;
                    case "HPIO_511U_STR": result = hpioMapper.HPIO_511U_STR(params); break;
                    case "HPIO_520U_STR": result = hpioMapper.HPIO_520U_STR(params); break;
                    case "HPIO_521U_STR": result = hpioMapper.HPIO_521U_STR(params); break;
                    case "HPIO_640S_STR": result = hpioMapper.HPIO_640S_STR(params); break;
                    case "HPIO_650S_STR": result = hpioMapper.HPIO_650S_STR(params); break;
                    case "HPIO_660S_STR": result = hpioMapper.HPIO_660S_STR(params); break;
                    case "HPIO_710S_STR": result = hpioMapper.HPIO_710S_STR(params); break;
                    case "HPIO_720S_STR": result = hpioMapper.HPIO_720S_STR(params); break;
                    case "HPIO_850S_STR": result = hpioMapper.HPIO_850S_STR(params); break;
                    case "HPIO_870U_STR": result = hpioMapper.HPIO_870U_STR(params); break;
                    case "HPIO_250U_POP": result = hpioMapper.HPIO_250U_POP(params); break;
                    case "HPIO_251S_STR": result = hpioMapper.HPIO_251S_STR(params); break;
                    case "HPIO_252S_STR": result = hpioMapper.HPIO_252S_STR(params); break;
                    case "HPIO_253U_STR": result = hpioMapper.HPIO_253U_STR(params); break;
                    default:
                        return ResponseEntity.notFound().build();
                }
            }

            if (result == null || result.isEmpty()) {
                if (actkind.startsWith("S") || actkind.startsWith("L") || actkind.startsWith("P") || actkind.isEmpty()) {
                    result = new ArrayList<>();
                } else {
                    result = List.of(Map.of("res", "OK"));
                }
            }
            
            // 🚀 모든 결과를 소문자로 강제 변환하여 프론트엔드 표준 준수
            return ResponseEntity.ok(convertToLowerCaseKeys(result));

        } catch (Exception e) {
            log.error("❌ [hpio] executeProcedure Error ({}): {}", proc, e.getMessage());
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
            String statementId = HpioMapper.class.getName() + "." + proc;
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
        } catch (Exception e) { log.warn("🛠 missing parameter alarm ({}): {}", proc, e.getMessage()); }
    }

    private String validateParameters(String proc, Map<String, Object> vueParams) {
        try {
            String statementId = HpioMapper.class.getName() + "." + proc;
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

    private String buildPositionalSql(String proc, Map<String, Object> params) {
        try {
            // 💡 [주의] 이 부분만 해당 컨트롤러의 매퍼 클래스명으로 수정하세요 (예: HsodMapper.class)
            String statementId = HpioMapper.class.getName() + "." + proc;

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
