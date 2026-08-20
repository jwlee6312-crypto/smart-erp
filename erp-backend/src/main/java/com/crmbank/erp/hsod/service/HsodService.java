package com.crmbank.erp.hsod.service;

import com.crmbank.erp.hsod.mapper.HsodMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import com.crmbank.erp.hsod.dto.Hsod100uRequest;
import com.crmbank.erp.hsod.dto.Hsod100u;
import com.crmbank.erp.hsod.dto.Hsod101u;

@Service
@RequiredArgsConstructor
public class HsodService {

    private final HsodMapper hsodMapper;

    /**
     * 🚀 Integrated Order Save
     * 모든 리턴 키는 소문자로 강제 변환하여 처리 (시스템 표준 준수)
     */
    @Transactional(value = "erpTransactionManager", rollbackFor = Exception.class)
    public Map<String, Object> saveOrder(Hsod100uRequest request, String userId) throws Exception {
        // 1. Master Save
        Hsod100u mst = request.getMst();
        if (mst == null) throw new Exception("Request master data is null.");
        
        // NULL 방지 및 데이터 정규화
        mst.setArea(nvl(mst.getArea()));
        mst.setWhcd(nvl(mst.getWhcd()));
        mst.setTrancd(nvl(mst.getTrancd()));
        mst.setRemark(nvl(mst.getRemark()));
        mst.setOrdemp(nvl(mst.getOrdemp()));
        mst.setPaycndt(nvl(mst.getPaycndt()));
        mst.setOutymd(nvl(mst.getOutymd()));
        mst.setPostno(nvl(mst.getPostno()));
        mst.setAddress(nvl(mst.getAddress()));
        mst.setD_address(nvl(mst.getD_address()));
        mst.setTotsum(nvl(mst.getTotsum(), "0"));
        mst.setSts(nvl(mst.getSts(), "Y"));
        mst.setFromdt(nvl(mst.getFromdt()));
        mst.setTodt(nvl(mst.getTodt()));
        mst.setCustnm(nvl(mst.getCustnm()));
        mst.setUpdemp(userId);

        List<Map<String, Object>> res = hsodMapper.HSOD_100U_STR(mst);
        if (res == null || res.isEmpty()) {
            throw new Exception("Master Save: No response from database.");
        }

        // 🚀 [표준 적용] 리턴된 맵의 키를 소문자로 변환
        Map<String, Object> mstRow = convertMapToLowerCase(res.getFirst());
        
        // 🚀 이제 무조건 소문자 키로만 접근
        String ordym = nvl(mstRow.get("ordym"));
        String ordno = nvl(mstRow.get("ordno"));

        // 비즈니스 에러 체크 (000000)
        if ("000000".equals(ordym)) {
            throw new Exception(nvl(ordno, "마스터 저장 업무 오류"));
        }

        if (ordym.isEmpty() || ordno.isEmpty()) {
            throw new Exception("정산 채번 실패: 반환된 주문번호(ordym, ordno)를 확인할 수 없습니다.");
        }

        // 2. Detail Save
        if (request.getDtl() != null) {
            for (Hsod101u dtl : request.getDtl()) {
                dtl.setOrdym(ordym);
                dtl.setOrdno(ordno);
                dtl.setCmpycd(mst.getCmpycd());
                dtl.setOrdymd(mst.getOrdymd());
                dtl.setDeptcd(mst.getDeptcd());
                dtl.setCustcd(mst.getCustcd());
                dtl.setUpdemp(userId);
                
                dtl.setUnit(nvl(dtl.getUnit()));
                dtl.setPctype(nvl(dtl.getPctype(), "0"));
                dtl.setOrdamt(nvl(dtl.getOrdamt(), "0"));
                dtl.setOrdvat(nvl(dtl.getOrdvat(), "0"));
                dtl.setOrdqty(nvl(dtl.getOrdqty(), "0"));
                dtl.setItemcd(nvl(dtl.getItemcd()));

                if (dtl.getOrowno() == null || "0".equals(dtl.getOrowno())) {
                    dtl.setOrowno("");
                }

                List<Map<String, Object>> dtlRes = hsodMapper.HSOD_101U_STR(dtl);
                if (dtlRes != null && !dtlRes.isEmpty()) {
                    Map<String, Object> dtlRow = convertMapToLowerCase(dtlRes.getFirst());
                    
                    // 첫 번째 컬럼값이 000000이면 에러로 간주하는 기존 로직 유지
                    List<Object> dtlValues = new ArrayList<>(dtlRow.values());
                    if ("000000".equals(nvl(dtlValues.getFirst()))) {
                        throw new Exception(nvl(dtlValues.get(1), "상세 저장 업무 오류"));
                    }
                }
            }
        }
        return mstRow;
    }

    /**
     * Map의 모든 Key를 소문자로 변환 (시스템 표준화 도구)
     */
    private Map<String, Object> convertMapToLowerCase(Map<String, Object> map) {
        if (map == null) return new HashMap<>();
        Map<String, Object> newMap = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            newMap.put(entry.getKey().toLowerCase(), entry.getValue());
        }
        return newMap;
    }

    private String nvl(Object val) {
        return val == null ? "" : String.valueOf(val).trim();
    }

    private String nvl(Object val, String def) {
        String s = nvl(val);
        return s.isEmpty() ? def : s;
    }
}
