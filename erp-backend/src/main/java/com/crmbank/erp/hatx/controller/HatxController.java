package com.crmbank.erp.hatx.controller;

import com.crmbank.erp.comm.dto.UserSession;
import com.crmbank.erp.hatx.mapper.HatxMapper;
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
@RequestMapping("/hatx")
@RequiredArgsConstructor
public class HatxController {

    private final HatxMapper hatxMapper;
    private final SqlSession sqlSession;
    private final JdbcTemplate jdbcTemplate;

    // -----------------------------------------------------------------------
    // [1] 매입부가세 통합 저장 (010U)
    // -----------------------------------------------------------------------
    @Transactional(rollbackFor = Exception.class)
    @PostMapping("/save-purchase")
    public ResponseEntity<?> savePurchaseVat(@RequestBody Map<String, Object> payload, HttpSession session) {
        if (session.getAttribute("user_session") == null) return ResponseEntity.status(401).build();
        UserSession user = (UserSession) session.getAttribute("user_session");

        try {
            Map<String, Object> master = (Map<String, Object>) payload.get("master");
            List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");
            String slipYn = String.valueOf(payload.getOrDefault("slipyn", "N"));
            String cmpycd = user.getCmpycd();
            String userid = user.getUserid();

            String slipymd = String.valueOf(master.getOrDefault("slipymd", "00000000")).replace("-", "");
            String slipno = String.valueOf(master.getOrDefault("slipno", ""));
            String srowno = "";

            if ("Y".equals(slipYn)) {
                String slipAct = (slipno == null || slipno.trim().isEmpty() || "000".equals(slipno)) ? "A" : "U";
                Map<String, Object> m = new HashMap<>();
                m.put("actkind", slipAct); m.put("cmpycd", cmpycd); m.put("slipymd", slipymd); m.put("slipno", slipno);
                m.put("deptcd", master.get("deptcd")); m.put("usernm", user.getUsernm()); m.put("slipgu", "010");
                m.put("remark", master.get("custnm") + " 매입건(공급가:" + master.get("supyamt") + " 부가세:" + master.get("vatamt") + ")");
                m.put("userid", userid);
                List<Map<String, Object>> mstRes = hatxMapper.HATX_01AU_STR(m);
                if (mstRes != null && !mstRes.isEmpty()) slipno = String.valueOf(mstRes.get(0).get("col_0"));

                double supyAmt = Double.parseDouble(String.valueOf(master.getOrDefault("supyamt", "0")).replace(",", ""));
                double vatAmt = Double.parseDouble(String.valueOf(master.getOrDefault("vatamt", "0")).replace(",", ""));

                // 차변 (매입액)
                Map<String, Object> db1 = new HashMap<>(master);
                db1.put("actkind", "A"); db1.put("cmpycd", cmpycd); db1.put("slipymd", slipymd); db1.put("slipno", slipno);
                db1.put("dbamt", String.valueOf((long)supyAmt)); db1.put("cramt", "0"); db1.put("updemp", userid);
                hatxMapper.HATX_01BU_STR(db1);

                // 차변 (매입부가세 - 1275)
                Map<String, Object> db2 = new HashMap<>(master);
                db2.put("actkind", "A"); db2.put("cmpycd", cmpycd); db2.put("slipymd", slipymd); db2.put("slipno", slipno);
                db2.put("acctcd", "1275"); db2.put("dbamt", String.valueOf((long)vatAmt)); db2.put("cramt", "0");
                db2.put("docno1", master.get("taxunit")); db2.put("docno2", master.get("custcd"));
                db2.put("docno3", master.get("taxtype")); db2.put("docno4", master.get("pubymd"));
                db2.put("docno5", master.get("supyamt")); db2.put("docno6", master.get("pubymd"));
                db2.put("updemp", userid);
                List<Map<String, Object>> detRes = hatxMapper.HATX_01BU_STR(db2);
                if (detRes != null && !detRes.isEmpty()) srowno = String.valueOf(detRes.get(0).get("col_0"));

                // 대변 (지불)
                for (int i = 1; i <= 2; i++) {
                    String payCndt = String.valueOf(master.getOrDefault("cpaycndt" + i, "000"));
                    if (payCndt.length() > 3) {
                        double inAmt = Double.parseDouble(String.valueOf(master.getOrDefault("cinamt" + i, "0")).replace(",", ""));
                        Map<String, Object> cr = new HashMap<>();
                        cr.put("actkind", master.getOrDefault("crowact" + i, "A"));
                        cr.put("cmpycd", cmpycd); cr.put("slipymd", slipymd); cr.put("slipno", slipno); cr.put("srowno", srowno);
                        cr.put("acctcd", master.get("cacctcd" + i)); cr.put("deptcd", master.get("deptcd"));
                        cr.put("custcd", master.get("custcd")); cr.put("mgtno", master.get("cmgtno" + i));
                        cr.put("dbamt", (supyAmt < 0 && inAmt > 0) ? String.valueOf((long)inAmt) : "0");
                        cr.put("cramt", (supyAmt < 0 && inAmt > 0) ? "0" : String.valueOf((long)inAmt));
                        cr.put("remark", master.get("descnm")); cr.put("paycndt", master.get("cpaytype" + i));
                        cr.put("payymd", String.valueOf(master.getOrDefault("cpayymd" + i, "00000000")).replace("-", ""));
                        cr.put("docno6", String.valueOf(master.getOrDefault("cstdymd" + i, "00000000")).replace("-", ""));
                        cr.put("docno7", String.valueOf(master.getOrDefault("cendymd" + i, "00000000")).replace("-", ""));
                        cr.put("docno8", String.valueOf((long)inAmt)); cr.put("docno9", master.get("custcd"));
                        cr.put("updemp", userid);
                        hatxMapper.HATX_01BU_STR(cr);
                    }
                }
            }

            Map<String, Object> vatMstParams = new HashMap<>(master);
            vatMstParams.put("cmpycd", cmpycd); vatMstParams.put("taxkind", "100"); vatMstParams.put("userid", userid);
            vatMstParams.put("slipymd", slipymd); vatMstParams.put("slipno", slipno); vatMstParams.put("srowno", srowno);
            vatMstParams.put("pubymd", String.valueOf(master.get("pubymd")).replace("-", ""));
            List<Map<String, Object>> vatRes = hatxMapper.HATX_010U_STR(vatMstParams);
            String taxym = String.valueOf(vatRes.get(0).get("col_0"));
            String taxno = String.valueOf(vatRes.get(0).get("col_1"));

            for (Map<String, Object> item : items) {
                if (item.get("itemnm") != null && !String.valueOf(item.get("itemnm")).trim().isEmpty()) {
                    item.put("actkind", "I1"); item.put("cmpycd", cmpycd); item.put("taxkind", "100");
                    item.put("taxym", taxym); item.put("taxno", taxno);
                    item.put("ymd", String.valueOf(item.get("ymd")).replace("-", ""));
                    hatxMapper.HATX_011U_STR(item);
                }
            }
            return ResponseEntity.ok(Map.of("res", "OK", "taxym", taxym, "taxno", taxno));
        } catch (Exception e) {
            log.error("❌ [savePurchase] Error: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // -----------------------------------------------------------------------
    // [2] 매출부가세 통합 저장 (020U)
    // -----------------------------------------------------------------------
    @Transactional(rollbackFor = Exception.class)
    @PostMapping("/save-sales")
    public ResponseEntity<?> saveSalesVat(@RequestBody Map<String, Object> payload, HttpSession session) {
        if (session.getAttribute("user_session") == null) return ResponseEntity.status(401).build();
        UserSession user = (UserSession) session.getAttribute("user_session");

        try {
            Map<String, Object> master = (Map<String, Object>) payload.get("master");
            List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");
            String slipYn = String.valueOf(payload.getOrDefault("slipyn", "N"));
            String cmpycd = user.getCmpycd();
            String userid = user.getUserid();

            String slipymd = String.valueOf(master.getOrDefault("slipymd", "00000000")).replace("-", "");
            String slipno = String.valueOf(master.getOrDefault("slipno", ""));
            String srowno = "";

            if ("Y".equals(slipYn)) {
                String slipAct = (slipno == null || slipno.trim().isEmpty() || "000".equals(slipno)) ? "A" : "U";
                Map<String, Object> m = new HashMap<>();
                m.put("actkind", slipAct); m.put("cmpycd", cmpycd); m.put("slipymd", slipymd); m.put("slipno", slipno);
                m.put("deptcd", master.get("deptcd")); m.put("usernm", user.getUsernm()); m.put("slipgu", "010");
                m.put("remark", master.get("custnm") + " 매출건(공급가:" + master.get("supyamt") + " 부가세:" + master.get("vatamt") + ")");
                m.put("userid", userid);
                List<Map<String, Object>> mstRes = hatxMapper.HATX_01AU_STR(m);
                if (mstRes != null && !mstRes.isEmpty()) slipno = String.valueOf(mstRes.get(0).get("col_0"));

                double supyAmt = Double.parseDouble(String.valueOf(master.getOrDefault("supyamt", "0")).replace(",", ""));
                double vatAmt = Double.parseDouble(String.valueOf(master.getOrDefault("vatamt", "0")).replace(",", ""));

                // 차변 (입금 1, 2)
                for (int i = 1; i <= 2; i++) {
                    String payCndt = String.valueOf(master.getOrDefault("cpaycndt" + i, "000"));
                    if (payCndt.length() > 3) {
                        double inAmt = Double.parseDouble(String.valueOf(master.getOrDefault("cinamt" + i, "0")).replace(",", ""));
                        Map<String, Object> db = new HashMap<>();
                        db.put("actkind", master.getOrDefault("crowact" + i, "A"));
                        db.put("cmpycd", cmpycd); db.put("slipymd", slipymd); db.put("slipno", slipno);
                        db.put("acctcd", master.get("cacctcd" + i)); db.put("deptcd", master.get("deptcd"));
                        db.put("custcd", master.get("custcd")); db.put("prjcd", master.get("prjcd")); db.put("mgtno", master.get("cmgtno" + i));
                        db.put("dbamt", (supyAmt < 0 && inAmt > 0) ? "0" : String.valueOf((long)inAmt));
                        db.put("cramt", (supyAmt < 0 && inAmt > 0) ? String.valueOf((long)inAmt) : "0");
                        db.put("remark", master.get("descnm"));
                        db.put("docno6", String.valueOf(master.getOrDefault("cstdymd" + i, "00000000")).replace("-", ""));
                        db.put("docno7", String.valueOf(master.getOrDefault("cendymd" + i, "00000000")).replace("-", ""));
                        db.put("docno8", String.valueOf((long)inAmt)); db.put("docno9", master.get("custcd"));
                        db.put("updemp", userid);
                        hatxMapper.HATX_01BU_STR(db);
                    }
                }

                // 대변 (매출액)
                Map<String, Object> cr1 = new HashMap<>(master);
                cr1.put("actkind", "A"); cr1.put("cmpycd", cmpycd); cr1.put("slipymd", slipymd); cr1.put("slipno", slipno);
                cr1.put("dbamt", supyAmt < 0 ? String.valueOf((long)Math.abs(supyAmt)) : "0");
                cr1.put("cramt", supyAmt < 0 ? "0" : String.valueOf((long)supyAmt)); cr1.put("updemp", userid);
                hatxMapper.HATX_01BU_STR(cr1);

                // 대변 (매출부가세 - 2145)
                Map<String, Object> cr2 = new HashMap<>(master);
                cr2.put("actkind", "A"); cr2.put("cmpycd", cmpycd); cr2.put("slipymd", slipymd); cr2.put("slipno", slipno);
                cr2.put("acctcd", "2145");
                cr2.put("dbamt", vatAmt < 0 ? String.valueOf((long)Math.abs(vatAmt)) : "0");
                cr2.put("cramt", vatAmt < 0 ? "0" : String.valueOf((long)vatAmt));
                cr2.put("remark", master.get("descnm") + "(" + master.get("custnm") + ")");
                cr2.put("updemp", userid);
                List<Map<String, Object>> detRes = hatxMapper.HATX_01BU_STR(cr2);
                if (detRes != null && !detRes.isEmpty()) srowno = String.valueOf(detRes.get(0).get("col_0"));
            }

            Map<String, Object> v = new HashMap<>(master);
            v.put("cmpycd", cmpycd); v.put("taxkind", "200"); v.put("userid", userid);
            v.put("slipymd", slipymd); v.put("slipno", slipno); v.put("srowno", srowno);
            v.put("pubymd", String.valueOf(master.get("pubymd")).replace("-", ""));
            v.put("frgnrate", String.valueOf(master.getOrDefault("frgnrate", "0")).replace(",", ""));
            v.put("frgnamt", String.valueOf(master.getOrDefault("frgnamt", "0")).replace(",", ""));
            
            List<Map<String, Object>> vatRes = hatxMapper.HATX_010U_STR(v);
            String taxym = String.valueOf(vatRes.get(0).get("col_0"));
            String taxno = String.valueOf(vatRes.get(0).get("col_1"));

            for (Map<String, Object> item : items) {
                if (item.get("itemnm") != null && !String.valueOf(item.get("itemnm")).trim().isEmpty()) {
                    item.put("actkind", "I1"); item.put("cmpycd", cmpycd); item.put("taxkind", "200");
                    item.put("taxym", taxym); item.put("taxno", taxno);
                    item.put("ymd", String.valueOf(item.get("ymd")).replace("-", ""));
                    hatxMapper.HATX_011U_STR(item);
                }
            }
            return ResponseEntity.ok(Map.of("res", "OK", "taxym", taxym, "taxno", taxno));
        } catch (Exception e) {
            log.error("❌ [saveSales] Error: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // -----------------------------------------------------------------------
    // [3] 수정세금계산서 통합 저장 (210U)
    // -----------------------------------------------------------------------
    @Transactional(rollbackFor = Exception.class)
    @PostMapping("/save-corrected-sales")
    public ResponseEntity<?> saveCorrectedSalesVat(@RequestBody Map<String, Object> payload, HttpSession session) {
        if (session.getAttribute("user_session") == null) return ResponseEntity.status(401).build();
        UserSession user = (UserSession) session.getAttribute("user_session");

        try {
            Map<String, Object> master = (Map<String, Object>) payload.get("master");
            List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");
            String slipYn = String.valueOf(payload.getOrDefault("slipyn", "N"));
            String cmpycd = user.getCmpycd();
            String userid = user.getUserid();

            String slipymd = String.valueOf(master.getOrDefault("slipymd", "00000000")).replace("-", "");
            String slipno = String.valueOf(master.getOrDefault("slipno", ""));
            String srowno = "";

            if ("Y".equals(slipYn)) {
                // 수정세금계산서 전표 생성 로직 (일반 매출과 유사하되 마이너스 전표 가능)
                // ... 필요 시 보강 ...
            }

            Map<String, Object> v = new HashMap<>(master);
            v.put("cmpycd", cmpycd); v.put("taxkind", "200"); v.put("userid", userid);
            v.put("pubymd", String.valueOf(master.get("pubymd")).replace("-", ""));
            v.put("bfymd", String.valueOf(master.get("bfymd")).replace("-", ""));
            
            List<Map<String, Object>> vatRes = hatxMapper.HATX_210U_STR(v);
            String taxym = String.valueOf(vatRes.get(0).get("col_0"));
            String taxno = String.valueOf(vatRes.get(0).get("col_1"));

            for (Map<String, Object> item : items) {
                if (item.get("itemnm") != null && !String.valueOf(item.get("itemnm")).trim().isEmpty()) {
                    item.put("actkind", "I1"); item.put("cmpycd", cmpycd); item.put("taxkind", "200");
                    item.put("taxym", taxym); item.put("taxno", taxno);
                    item.put("ymd", String.valueOf(item.get("ymd")).replace("-", ""));
                    hatxMapper.HATX_011U_STR(item);
                }
            }
            return ResponseEntity.ok(Map.of("res", "OK", "taxym", taxym, "taxno", taxno));
        } catch (Exception e) {
            log.error("❌ [saveCorrected] Error: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
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

        injectSession(params, session);
        String proc = procedure.toUpperCase();
        String actkind = String.valueOf(params.getOrDefault("actkind", "")).toUpperCase();

        try {
            fillMissingParameters(proc, params);
            log.info("🚀 [hatx] 실행 요청: {}", proc);
            
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
            } else {
                switch (proc) {
                    case "HATX_010U_STR": result = hatxMapper.HATX_010U_STR(params); break;
                    case "HATX_011U_STR": result = hatxMapper.HATX_011U_STR(params); break;
                    case "HATX_030S_STR": result = hatxMapper.HATX_030S_STR(params); break;
                    case "HATX_040S_STR": result = hatxMapper.HATX_040S_STR(params); break;
                    case "HATX_110S_STR": result = hatxMapper.HATX_110S_STR(params); break;
                    case "HATX_130S_STR": result = hatxMapper.HATX_130S_STR(params); break;
                    case "HATX_140S_STR": result = hatxMapper.HATX_140S_STR(params); break;
                    case "HATX_150S_STR": result = hatxMapper.HATX_150S_STR(params); break;
                    case "HATX_160S_STR": result = hatxMapper.HATX_160S_STR(params); break;
                    case "HATX_170S_STR": result = hatxMapper.HATX_170S_STR(params); break;
                    case "HATX_210U_STR": result = hatxMapper.HATX_210U_STR(params); break;
                    case "HATX_600S_STR": result = hatxMapper.HATX_600S_STR(params); break;
                    case "HATX_01AU_STR": result = hatxMapper.HATX_01AU_STR(params); break;
                    case "HATX_01BU_STR": result = hatxMapper.HATX_01BU_STR(params); break;
                    case "HATX_060U_STR": result = hatxMapper.HATX_060U_STR(params); break;
                    case "HATX_080U_STR": result = hatxMapper.HATX_080U_STR(params); break;
                    case "HATX_050U_STR": result = hatxMapper.HATX_050U_STR(params); break;
                    case "HATX_500S_STR": result = hatxMapper.HATX_500S_STR(params); break;
                    default:
                        return ResponseEntity.notFound().build();
                }
            }

            if (result == null || result.isEmpty()) {
                result = List.of(Map.of("res", "OK"));
            }
            return ResponseEntity.ok(convertToLowerCaseKeys(result));
        } catch (Exception e) {
            log.error("❌ [hatx] executeProcedure Error ({}): {}", proc, e.getMessage());
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
            String statementId = "com.crmbank.erp.hatx.mapper.HatxMapper." + proc;
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
            String statementId = HatxMapper.class.getName() + "." + proc;
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
