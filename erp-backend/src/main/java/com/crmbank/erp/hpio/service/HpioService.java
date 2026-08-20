package com.crmbank.erp.hpio.service;

import com.crmbank.erp.hpio.mapper.HpioMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class HpioService {

    private final HpioMapper hpioMapper;

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> saveHpio250U(Map<String, Object> payload) {
        String cmpycd = String.valueOf(payload.get("cmpycd"));
        String updemp = String.valueOf(payload.get("updemp"));

        Map<String, Object> mst = (Map<String, Object>) payload.get("mst");
        List<Map<String, Object>> midlist = (List<Map<String, Object>>) payload.get("midlist");
        List<Map<String, Object>> dtllist = (List<Map<String, Object>>) payload.get("dtllist");

        if (mst == null) throw new RuntimeException("MST_DATA_MISSING");

        try {
            // 1. [A0] 마스터 저장
            setCommon(mst, cmpycd, updemp);
            List<Map<String, Object>> mstRes = hpioMapper.HPIO_250U_STR(mst);
            
            String outym = ""; String outno = "";
            if (mstRes != null && !mstRes.isEmpty()) {
                Map<String, Object> res = mstRes.get(0);
                outym = String.valueOf(res.getOrDefault("outym", ""));
                outno = String.valueOf(res.getOrDefault("outno", ""));
            }
            if (outno.isEmpty()) throw new RuntimeException("MASTER_SAVE_FAILED");

            // 2. [A1] 제품 리스트 저장
            if (midlist != null && !midlist.isEmpty()) {
                for (Map<String, Object> mid : midlist) {
                    if (toDouble(mid.get("outqty")) <= 0) continue;
                    mid.put("actkind", "A1");
                    mid.put("outym", outym);
                    mid.put("outno", outno);
                    setCommon(mid, cmpycd, updemp);
                    hpioMapper.HPIO_250U_STR(mid);
                }
            }

            // 3. 자재 상세 처리 (집계 테이블 HPIO253T 활용)
            if (dtllist != null && !dtllist.isEmpty()) {
                // 사용자가 화면에서 직접 편집한 경우 (A0, U0, D0)
                for (Map<String, Object> dtl : dtllist) {
                    dtl.put("outym", outym);
                    dtl.put("outno", outno);
                    setCommon(dtl, cmpycd, updemp);
                    hpioMapper.HPIO_253U_STR(dtl);
                }
            } else if (midlist != null && !midlist.isEmpty()) {
                // 🚀 [S2] 제품 정보를 기반으로 자동 BOM 전개 및 집계 테이블 저장
                for (Map<String, Object> mid : midlist) {
                    if (toDouble(mid.get("outqty")) <= 0) continue;

                    Map<String, Object> bomParam = new HashMap<>(mid);
                    bomParam.put("actkind", "S2");
                    bomParam.put("outym", outym);
                    bomParam.put("outno", outno);
                    setCommon(bomParam, cmpycd, updemp);
                    
                    List<Map<String, Object>> bomItems = hpioMapper.HPIO_250U_STR(bomParam);
                    if (bomItems != null) {
                        for (Map<String, Object> bom : bomItems) {
                            double soqty = toDouble(bom.get("soqty"));
                            if (soqty <= 0) continue;

                            Map<String, Object> dtl = new HashMap<>();
                            dtl.put("actkind", "A0"); // 집계 테이블 추가
                            dtl.put("cmpycd", cmpycd);
                            dtl.put("outym", outym);
                            dtl.put("outno", outno);
                            dtl.put("mitemcd", bom.get("mitemcd"));
                            dtl.put("mitsize", bom.get("mitsize"));
                            dtl.put("munit",   bom.get("munit"));
                            dtl.put("reqqty",  soqty);
                            dtl.put("updemp",  updemp);

                            hpioMapper.HPIO_253U_STR(dtl);
                        }
                    }
                }
            }

            // 4. 최종 결과 리프레시를 위한 재조회
            List<Map<String, Object>> finalMid = hpioMapper.HPIO_250U_STR(Map.of("actkind", "S1", "cmpycd", cmpycd, "outym", outym, "outno", outno, "outymd", mst.get("outymd")));
            List<Map<String, Object>> finalDtl = hpioMapper.HPIO_253U_STR(Map.of("actkind", "S0", "cmpycd", cmpycd, "outym", outym, "outno", outno));

            return Map.of(
                "res", "OK", "outno", outno, "outym", outym,
                "midlist", finalMid != null ? finalMid : new ArrayList<>(),
                "dtllist", finalDtl != null ? finalDtl : new ArrayList<>()
            );

        } catch (Exception e) {
            log.error("[ERROR] Transaction failed: {}", e.getMessage());
            throw new RuntimeException("HPIO_SAVE_FAILED: " + e.getMessage());
        }
    }

    private void setCommon(Map<String, Object> p, String cmpycd, String updemp) {
        p.put("cmpycd", cmpycd);
        p.put("updemp", updemp);
        if (p.get("outymd") != null) p.put("outymd", String.valueOf(p.get("outymd")).replace("-", ""));
        
        String[] fields = {"gubun", "ordym", "ordno", "lotymd", "lotno", "deptcd", "whcd", "itemcd", "mitemcd", "ritemcd", "orowno", "mitsize"};
        for (String f : fields) {
            if (p.get(f) == null) p.put(f, "");
        }

        String[] nums = {"ordqty", "outqty", "soqty", "reqqty"};
        for (String n : nums) {
            p.put(n, toDouble(p.get(n)));
        }
    }

    private double toDouble(Object v) {
        if (v == null || v.toString().trim().isEmpty() || "null".equalsIgnoreCase(v.toString())) return 0.0;
        try { return Double.parseDouble(v.toString()); } catch (Exception e) { return 0.0; }
    }
}
