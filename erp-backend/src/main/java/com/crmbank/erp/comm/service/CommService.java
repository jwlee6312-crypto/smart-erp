package com.crmbank.erp.comm.service;

import com.crmbank.erp.comm.mapper.CommMapper;
import com.crmbank.erp.comm.dto.UserSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommService {

    private final CommMapper commMapper;

    @Transactional("erpTransactionManager")
    public UserSession login(String cmpycd, String userid, String passwd, String ip) throws Exception {
        Map<String, Object> param = new HashMap<>();
        param.put("cmpycd", cmpycd.trim());
        param.put("userid", userid.trim());

        Map<String, Object> companyInfoRaw = commMapper.GET_COMPANY_INFO(param);
        if (companyInfoRaw == null) throw new Exception("등록되지 않은 회사아이디 입니다.");
        Map<String, Object> companyInfo = normalize(companyInfoRaw);
        
        Map<String, Object> userInfoRaw = commMapper.GET_USER_INFO(param);
        if (userInfoRaw == null) throw new Exception("사용자 정보를 찾을 수 없습니다.");
        Map<String, Object> userInfo = normalize(userInfoRaw);

        // 🚀 비밀번호 체크 (대소문자 무시 비교)
        String dbPw = String.valueOf(userInfo.getOrDefault("pw", "")).trim();
        if (dbPw.isEmpty()) dbPw = String.valueOf(userInfo.getOrDefault("passwd", "")).trim();
        
        String inputPwEnc = com.crmbank.erp.comm.util.SecurityUtil.encryptSha256(passwd);

        if (!inputPwEnc.equalsIgnoreCase(dbPw)) {
            log.warn("❌ [비밀번호 불일치] ID: {}, Input: {}, DB: {}", userid, inputPwEnc, dbPw);
            throw new Exception("입력하신 비밀번호가 틀립니다.");
        }

        UserSession session = new UserSession();
        session.setCmpycd(String.valueOf(companyInfo.getOrDefault("cmpycd", cmpycd)));
        session.setCmpynm(String.valueOf(companyInfo.getOrDefault("cmpynm", "")));
        session.setUserid(String.valueOf(userInfo.getOrDefault("userid", userid)));
        session.setUsernm(String.valueOf(userInfo.getOrDefault("usernm", "")));
        session.setInner_no(String.valueOf(userInfo.getOrDefault("inner_no", "")));
        session.setHpno(String.valueOf(userInfo.getOrDefault("hpno", "")));
        session.setDeptcd(String.valueOf(userInfo.getOrDefault("deptcd", "")));
        session.setDeptnm(String.valueOf(userInfo.getOrDefault("deptnm", "")));
        session.setUsergrp(String.valueOf(userInfo.getOrDefault("usergrp", "")));
        session.setEmail(String.valueOf(userInfo.getOrDefault("email", "")));
        session.setStatus(String.valueOf(userInfo.getOrDefault("status", "10")));
        session.setRouting_mode(String.valueOf(userInfo.getOrDefault("routing_mode", "20")));

        param.put("iogbn", "I");
        param.put("ip", ip);
        commMapper.INSERT_LOGIN_HISTORY(param);

        return session;
    }

    private Map<String, Object> normalize(Map<String, Object> map) {
        Map<String, Object> res = new HashMap<>();
        if (map != null) {
            for (String key : map.keySet()) {
                String newKey = key.toLowerCase();
                // 🚀 'A.ITEMCD' 형태의 별칭 제거 (itemcd 로 정규화)
                int dotIdx = newKey.lastIndexOf('.');
                if (dotIdx != -1) newKey = newKey.substring(dotIdx + 1);
                res.put(newKey, map.get(key));
            }
        }
        return res;
    }

    public List<Map<String, Object>> getTopMenus() { return commMapper.GET_TOP_MENU_LIST(); }

    public List<Map<String, Object>> getTopMenusMobile() { return commMapper.GET_TOP_MENU_MOBILE_LIST(); }

    public List<Map<String, Object>> getLeftMenus(String cmpycd, String userid, String upmucd, String usergrp) {
        Map<String, Object> param = new HashMap<>();
        param.put("cmpycd", cmpycd); param.put("userid", userid);
        param.put("upmucd", upmucd); param.put("usergrp", usergrp);
        
        List<Map<String, Object>> rawList = commMapper.HA00_200S_STR(param);
        List<Map<String, Object>> resultList = new ArrayList<>();
        
        if (rawList != null) {
            for (Map<String, Object> row : rawList) {
                resultList.add(normalize(row));
            }
        }
        return resultList;
    }

    public List<Map<String, Object>> getProgramList(Map<String, Object> param) { return commMapper.GET_PROGRAM_LIST(param); }
}
