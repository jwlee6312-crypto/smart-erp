package com.crmbank.erp.hsip.controller;

import com.crmbank.erp.comm.dto.ApiResponse;
import com.crmbank.erp.comm.dto.UserSession;
import com.crmbank.erp.hsip.dto.Hsip100uRequest;
import com.crmbank.erp.hsip.dto.Hsip120uSaveRequest;
import com.crmbank.erp.hsip.dto.Hsip140uSaveRequest;
import com.crmbank.erp.hsip.dto.Hsip145uSaveRequest;
import com.crmbank.erp.hsip.dto.Hsip150uCancelRequest;
import com.crmbank.erp.hsip.mapper.HsipMapper;
import com.crmbank.erp.hsip.service.HsipService;
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
 * [HSIP] 수입관리 통합 컨트롤러 (사용자 정의 최종 표준형)
 */
@SuppressWarnings("unused")
@Slf4j
@RestController
@RequestMapping("/hsip")
@RequiredArgsConstructor
public class HsipController {

    private final HsipMapper hsipMapper;
    private final HsipService hsipService;
    private final SqlSession sqlSession;
    private final JdbcTemplate jdbcTemplate;

    @PostMapping("/HSIP_120U_SAVE")
    public ResponseEntity<?> saveCustomsStock(@RequestBody Hsip120uSaveRequest request, HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user == null) return ResponseEntity.status(401).build();
        try {
            String userId = user.getUserid();
            String cmpycd = user.getCmpycd();
            if (request.getMst() != null) {
                request.getMst().setCmpycd(cmpycd);
                request.getMst().setUpdemp(userId);
            }
            Map<String, Object> result = hsipService.saveCustomsStock(request, userId);
            return ResponseEntity.ok(ApiResponse.success(result, "성공적으로 저장되었습니다."));
        } catch (Exception e) {
            log.error("❌ [hsip] Customs Save Error: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(ApiResponse.serverError(e.getMessage()));
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @PostMapping("/HSIP_100U_SAVE")
    public ResponseEntity<?> saveImportOrder(@RequestBody Hsip100uRequest request, HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user == null) return ResponseEntity.status(401).build();

        try {
            String userId = user.getUserid();
            String cmpycd = user.getCmpycd();

            if (request.getMst() != null) {
                request.getMst().setCmpycd(cmpycd);
                request.getMst().setUpdemp(userId);
            }
            Map<String, Object> result = hsipService.saveImportOrder(request, userId);
            return ResponseEntity.ok(ApiResponse.success(result, "성공적으로 저장되었습니다."));
        } catch (Exception e) {
            log.error("❌ [hsip] Save Error: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(ApiResponse.serverError(e.getMessage()));
        }
    }

    @PostMapping("/HSIP_140U_SAVE")
    public ResponseEntity<?> generateSlip140(@RequestBody Hsip140uSaveRequest request, HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user == null) return ResponseEntity.status(401).build();
        try {
            Map<String, Object> result = hsipService.generateSlip140(request, user.getUserid());
            return ResponseEntity.ok(ApiResponse.success(result, "전표가 성공적으로 발행되었습니다."));
        } catch (Exception e) {
            log.error("❌ [hsip] generateSlip140 Error: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(ApiResponse.serverError(e.getMessage()));
        }
    }

    @PostMapping("/HSIP_150U_CANCEL")
    public ResponseEntity<?> cancelSlips150(@RequestBody Hsip150uCancelRequest request, HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user == null) return ResponseEntity.status(401).build();
        try {
            Map<String, Object> result = hsipService.cancelSlips150(request, user.getUserid());
            return ResponseEntity.ok(ApiResponse.success(result, "전표 취소가 완료되었습니다."));
        } catch (Exception e) {
            log.error("❌ [hsip] cancelSlips150 Error: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(ApiResponse.serverError(e.getMessage()));
        }
    }

    @PostMapping("/HSIP_145U_SAVE")
    public ResponseEntity<?> saveExternalSlip145(@RequestBody Hsip145uSaveRequest request, HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user == null) return ResponseEntity.status(401).build();
        try {
            Map<String, Object> result = hsipService.saveExternalSlip145(request, user.getUserid(), user.getCmpycd(), user.getDeptcd());
            return ResponseEntity.ok(ApiResponse.success(result, "정산 작업이 완료되었습니다."));
        } catch (Exception e) {
            log.error("❌ [hsip] saveExternalSlip145 Error: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(ApiResponse.serverError(e.getMessage()));
        }
    }

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

            List<Map<String, Object>> resultList;

            if (params.get("items") instanceof List<?> items) {
                resultList = new ArrayList<>();
                for (Object itemObj : items) {
                    if (itemObj instanceof Map<?, ?> item) {
                        Map<String, Object> p = new HashMap<>(params);
                        item.forEach((k, v) -> p.put(String.valueOf(k), v));
                        p.remove("items");
                        resultList.addAll(executeInternal(proc, p));
                    }
                }
            } else {
                resultList = executeInternal(proc, params);
            }

            return ResponseEntity.ok(convertToLowerCaseKeys(resultList));

        } catch (Exception e) {
            log.error("❌ [HSIP] {} Error: {}", proc, e.getMessage());
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
        List<Map<String, Object>> result = switch (proc) {
            case "HSIP_100U_STR" -> hsipMapper.HSIP_100U_STR(params);
            case "HSIP_101U_STR" -> hsipMapper.HSIP_101U_STR(params);
            case "HSIP_110U_STR" -> hsipMapper.HSIP_110U_STR(params);
            case "HSIP_111U_STR" -> hsipMapper.HSIP_111U_STR(params);
            case "HSIP_112U_STR" -> hsipMapper.HSIP_112U_STR(params);
            case "HSIP_120U_STR" -> hsipMapper.HSIP_120U_STR(params);
            case "HSIP_121U_STR" -> hsipMapper.HSIP_121U_STR(params);
            case "HSIP_122U_STR" -> hsipMapper.HSIP_122U_STR(params);
            case "HSIP_130U_STR" -> hsipMapper.HSIP_130U_STR(params);
            case "HSIP_131U_STR" -> hsipMapper.HSIP_131U_STR(params);
            case "HSIP_140U_STR" -> hsipMapper.HSIP_140U_STR(params);
            case "HSIP_145U_STR" -> hsipMapper.HSIP_145U_STR(params);
            case "HSIP_150U_STR" -> hsipMapper.HSIP_150U_STR(params);
            case "HSIP_155U_STR" -> hsipMapper.HSIP_155U_STR(params);
            case "HSIP_160U_STR" -> hsipMapper.HSIP_160U_STR(params);
            case "HSIP_180U_STR" -> hsipMapper.HSIP_180U_STR(params);
            case "HSIP_200S_STR" -> hsipMapper.HSIP_200S_STR(params);
            case "HSIP_210S_STR" -> hsipMapper.HSIP_210S_STR(params);
            default -> {
                List<Map<String, Object>> mappedResult = invokeMapper(proc, params);
                yield (mappedResult != null) ? mappedResult : executeDirectSql(proc, params);
            }
        };
        return result != null ? result : new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> invokeMapper(String proc, Map<String, Object> params) {
        try {
            Method method = HsipMapper.class.getMethod(proc, Map.class);
            return (List<Map<String, Object>>) method.invoke(hsipMapper, params);
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
        }
        // 👇 이 부분이 수정할 catch 블록입니다!
        catch (Exception e) {
            String msg = e.getMessage();
            // 💡 한글/영문 "결과 집합 없음" 메시지를 모두 체크하여 정상 처리로 간주
            if (msg != null && (msg.contains("No ResultSet") ||
                    msg.contains("did not return a result set") ||
                    msg.contains("결과 집합을 반환하지 않았습니다"))) {
                log.info("ℹ️ [HSIP] {} 실행 완료 (결과셋 없음 - 정상)", proc);
                return new ArrayList<>();
            }
            throw e;
        }
    }

    private String buildPositionalSql(String proc, Map<String, Object> params) {
        try {
            String statementId = HsipMapper.class.getName() + "." + proc;
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
