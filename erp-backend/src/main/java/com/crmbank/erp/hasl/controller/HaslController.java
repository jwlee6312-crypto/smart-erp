package com.crmbank.erp.hasl.controller;

import com.crmbank.erp.comm.dto.ApiResponse;
import com.crmbank.erp.comm.dto.UserSession;
import com.crmbank.erp.hasl.mapper.HaslMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/hasl")
@RequiredArgsConstructor
public class HaslController {

    private final HaslMapper haslMapper;
    private final SqlSession sqlSession;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

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

        if (proc.equals("HASL_010U_SAVE")) return saveSlip010(params, session);
        if (proc.equals("HASL_110U_SAVE")) return saveSlip110(params, session);

        try {
            fillMissingParameters(proc, params);

            String actkind = String.valueOf(params.getOrDefault("actkind", "")).toUpperCase();
            log.info("🚀 [hasl] 실행 요청: {}", proc);
            
            List<Map<String, Object>> result;
            if (proc.endsWith("U_STR") && (actkind.startsWith("A") || actkind.startsWith("U"))) {
                String positionalSql = buildPositionalSql(proc, params);
                log.info("📋 [ASP 스타일 실행] SQL: {}", positionalSql);

                result = jdbcTemplate.execute(positionalSql, (java.sql.PreparedStatement ps) -> {
                    boolean hasResults = ps.execute();
                    List<Map<String, Object>> rows = new ArrayList<>();
                    if (hasResults) {
                        try (java.sql.ResultSet rs = ps.getResultSet()) {
                            while (rs.next()) {
                                Map<String, Object> row = new LinkedHashMap<>();
                                int colCount = rs.getMetaData().getColumnCount();
                                for (int i = 1; i <= colCount; i++) {
                                    String colName = rs.getMetaData().getColumnLabel(i);
                                    if (colName == null || colName.isEmpty()) colName = "col_" + (i - 1);
                                    row.put(colName.toLowerCase(), rs.getObject(i) == null ? "" : rs.getObject(i));
                                }
                                rows.add(row);
                            }
                        }
                    }
                    return rows;
                });
                log.info("🎯 [무결성 직접 수신 성공] 데이터: {}", result);
            } else {
                switch (proc) {
                    case "HASL_010U_STR": result = haslMapper.HASL_010U_STR(params); break;
                    case "HASL_011U_STR": result = haslMapper.HASL_011U_STR(params); break;
                    case "HASL_020U_STR": result = haslMapper.HASL_020U_STR(params); break;
                    case "HASL_030S_STR": result = haslMapper.HASL_030S_STR(params); break;
                    case "HASL_040S_STR":
                        String rawkeyword = String.valueOf(params.getOrDefault("keyword", "")).trim();
                        if (!rawkeyword.isEmpty() && !rawkeyword.equalsIgnoreCase("null")) {
                            params.put("keywords", modernTokenize(rawkeyword));
                        }
                        result = haslMapper.HASL_040S_STR(params);
                        break;
                    case "HASL_050U_MASTER": result = haslMapper.HASL_050U_MASTER(params); break;
                    case "HASL_050U_STR": result = haslMapper.HASL_050U_STR(params); break;
                    case "HASL_110U_STR": result = haslMapper.HASL_110U_STR(params); break;
                    case "HASL_111U_STR": result = haslMapper.HASL_111U_STR(params); break;
                    case "HASL_120S_STR": result = haslMapper.HASL_120S_STR(params); break;
                    case "HASL_130S_STR": result = haslMapper.HASL_130S_STR(params); break;
                    case "HASL_510S_STR": result = haslMapper.HASL_510S_STR(params); break;
                    case "HASL_520S_STR": result = haslMapper.HASL_520S_STR(params); break;
                    case "HASL_530S_STR": result = haslMapper.HASL_530S_STR(params); break;
                    case "HASL_540S_STR": result = haslMapper.HASL_540S_STR(params); break;
                    case "HASL_550S_STR": result = haslMapper.HASL_550S_STR(params); break;
                    case "HASL_560S_STR": result = haslMapper.HASL_560S_STR(params); break;
                    case "HASL_610S_STR": result = haslMapper.HASL_610S_STR(params); break;
                    case "HASL_620S_STR": result = haslMapper.HASL_620S_STR(params); break;
                    case "HASL_630S_STR": result = haslMapper.HASL_630S_STR(params); break;
                    case "HASL_710S_STR": result = haslMapper.HASL_710S_STR(params); break;
                    default:
                        return ResponseEntity.notFound().build();
                }
            }

            if (result == null || result.isEmpty()) {
                result = List.of(Map.of("res", "OK"));
            }
            return ResponseEntity.ok(convertToLowerCaseKeys(result));
        } catch (Exception e) {
            log.error("❌ [hasl] executeProcedure Error ({}): {}", proc, e.getMessage());
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
            String statementId = HaslMapper.class.getName() + "." + proc;
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
            String statementId = HaslMapper.class.getName() + "." + proc;

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

    private List<String> modernTokenize(String query) {
        if (query == null || query.trim().isEmpty()) return List.of();
        String cleaned = query.replaceAll("[.,?!()\\[\\]]", " ")
                             .replaceAll("\\d", "")
                             .replace("원", "")
                             .replaceAll("\\s+", " ").trim();
        String particleregex = "(은|는|이|가|을|를|과|와|로|으로|에서|에게|의|도|만|까지|부터|하고|했다|하며|하였다|했으며|하였습니다|하였으며|이다|입니다|습니까|니까|함|했고|있다|았다|었다|들과|들의|같이)$";
        Pattern pattern = Pattern.compile(particleregex);
        return Arrays.stream(cleaned.split(" "))
                .map(word -> {
                    String prev = ""; String curr = word;
                    while(!prev.equals(curr)) { prev = curr; curr = pattern.matcher(curr).replaceAll(""); }
                    return curr;
                })
                .filter(word -> !word.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    private String extractFirstValue(List<Map<String, Object>> result) {
        if (result == null || result.isEmpty()) return "";
        Map<String, Object> firstRow = result.getFirst();
        if (firstRow == null || firstRow.isEmpty()) return "";
        return String.valueOf(firstRow.values().iterator().next()).trim();
    }

    @Transactional
    public ResponseEntity<ApiResponse<?>> saveSlip010(Map<String, Object> payload, HttpSession session) {
        try {
            Map<String, Object> master = objectMapper.convertValue(payload.get("master"), new TypeReference<>() {});
            List<Map<String, Object>> details = objectMapper.convertValue(payload.get("details"), new TypeReference<>() {});
            String actkind = String.valueOf(payload.getOrDefault("actkind", "A")).toUpperCase();

            injectSession(master, session);
            master.put("actkind", actkind);
            
            String sql = buildPositionalSql("HASL_010U_STR", master);
            List<Map<String, Object>> masterresult = jdbcTemplate.query(sql, (rs, rowNum) -> {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                    row.put(rs.getMetaData().getColumnLabel(i).toLowerCase(), rs.getObject(i));
                }
                return row;
            });

            if (masterresult.isEmpty()) throw new RuntimeException("마스터 저장 실패");
            
            // 💡 결과셋에서 slipno 추출 (첫번째 행의 첫번째 컬럼)
            String slipno = extractFirstValue(masterresult);
            
            // 💡 에러 체크 (마스터 SP가 실패 시 000000 등을 반환하는 경우 대비)
            if ("000000".equals(slipno)) {
                Map<String, Object> firstRow = masterresult.getFirst();
                String msg = firstRow.containsKey("msg") ? String.valueOf(firstRow.get("msg")) : "저장 중 오류가 발생했습니다.";
                throw new RuntimeException(msg);
            }

            if (details != null) {
                for (Map<String, Object> detail : details) {
                    injectSession(detail, session);
                    detail.put("slipymd", master.get("slipymd"));
                    detail.put("slipno", slipno);
                    detail.put("acctymd", master.get("acctymd"));
                    
                    // 💡 상세 actkind 결정: 삭제('D')면 'D', 그 외엔 개별 upkind(없으면 마스터 actkind)
                    String dtlAct = actkind.equals("D") ? "D" : 
                                   String.valueOf(detail.getOrDefault("upkind", actkind)).toUpperCase();
                    detail.put("actkind", dtlAct);
                    
                    haslMapper.HASL_011U_STR(detail);
                }
            }
            Map<String, Object> response = new HashMap<>();
            response.put("slipno", slipno);
            return ResponseEntity.ok(ApiResponse.success(response, "성공적으로 저장되었습니다."));
        } catch (Exception e) {
            log.error("❌ [hasl] saveSlip010 Error: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(ApiResponse.serverError(e.getMessage()));
        }
    }

    @Transactional
    public ResponseEntity<ApiResponse<?>> saveSlip110(Map<String, Object> payload, HttpSession session) {
        try {
            Map<String, Object> master = objectMapper.convertValue(payload.get("master"), new TypeReference<>() {});
            List<Map<String, Object>> details = objectMapper.convertValue(payload.get("details"), new TypeReference<>() {});
            String actkind = (String) payload.get("actkind");
            injectSession(master, session);
            master.put("actkind", actkind);
            
            String sql = buildPositionalSql("HASL_110U_STR", master);
            List<Map<String, Object>> masterresult = jdbcTemplate.query(sql, (rs, rowNum) -> {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                    row.put(rs.getMetaData().getColumnLabel(i).toLowerCase(), rs.getObject(i));
                }
                return row;
            });

            if (masterresult.isEmpty()) throw new RuntimeException("마스터 저장 실패");
            
            String slipno = extractFirstValue(masterresult);
            if ("000000".equals(slipno)) {
                List<Object> mstValues = new ArrayList<>(masterresult.getFirst().values());
                throw new RuntimeException(String.valueOf(mstValues.get(1)));
            }

            String slipymd = String.valueOf(master.get("slipymd"));

            if (details != null) {
                for (Map<String, Object> detail : details) {
                    injectSession(detail, session);
                    detail.put("slipymd", slipymd); detail.put("slipno", slipno);
                    detail.put("acctymd", master.get("acctymd"));
                    detail.put("actkind", "d".equals(actkind) ? "d" : detail.getOrDefault("upkind", "a"));
                    haslMapper.HASL_111U_STR(detail);
                }
            }
            Map<String, Object> response = new HashMap<>();
            response.put("slipno", slipno);
            return ResponseEntity.ok(ApiResponse.success(response, "성공적으로 저장되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.serverError(e.getMessage()));
        }
    }
}
