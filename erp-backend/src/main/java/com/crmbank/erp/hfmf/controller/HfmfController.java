package com.crmbank.erp.hfmf.controller;

import com.crmbank.erp.comm.dto.UserSession;
import com.crmbank.erp.hfmf.mapper.HfmfMapper;
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
@RequestMapping("/hfmf")
@RequiredArgsConstructor
public class HfmfController {

    private final HfmfMapper hfmfMapper;
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
        String actkind = String.valueOf(params.getOrDefault("actkind", "")).toUpperCase();

        try {
            fillMissingParameters(proc, params);
            log.info("🚀 [hfmf] 실행 요청: {}", proc);

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
                    case "FMF1020U_STR": result = hfmfMapper.FMF1020U_STR(params); break;
                    case "FMF1040U_STR": result = hfmfMapper.FMF1040U_STR(params); break;
                    case "FMF1050U_STR": result = hfmfMapper.FMF1050U_STR(params); break;
                    case "FMF2010U_STR": result = hfmfMapper.FMF2010U_STR(params); break;
                    case "FMF2020U_STR": result = hfmfMapper.FMF2020U_STR(params); break;
                    case "FMF2060R_STR": result = hfmfMapper.FMF2060R_STR(params); break; 
                    case "FMF2070U_STR": result = hfmfMapper.FMF2070U_STR(params); break;
                    case "FMF2110U_STR": result = hfmfMapper.FMF2110U_STR(params); break;
                    case "FMF2120R_STR": result = hfmfMapper.FMF2120R_STR(params); break;
                    case "FMF2140R_STR": result = hfmfMapper.FMF2140R_STR(params); break;
                    case "FMF2150R_STR": result = hfmfMapper.FMF2150R_STR(params); break;
                    case "FMF2160R_STR": result = hfmfMapper.FMF2160R_STR(params); break;
                    case "FMF2180R_STR": result = hfmfMapper.FMF2180R_STR(params); break;
                    case "FMF2190R_STR": result = hfmfMapper.FMF2190R_STR(params); break;
                    case "FMF2210R_STR": result = hfmfMapper.FMF2210R_STR(params); break;
                    case "FMF3010U_STR": result = hfmfMapper.FMF3010U_STR(params); break;
                    default:
                        return ResponseEntity.notFound().build();
                }
            }

            if (result == null || result.isEmpty()) {
                result = List.of(Map.of("res", "OK"));
            }
            return ResponseEntity.ok(convertToLowerCaseKeys(result));
        } catch (Exception e) {
            log.error("❌ [hfmf] executeProcedure Error ({}): {}", proc, e.getMessage());
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
            String statementId = "com.crmbank.erp.hfmf.mapper.HfmfMapper." + proc;
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
            String statementId = HfmfMapper.class.getName() + "." + proc;

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
