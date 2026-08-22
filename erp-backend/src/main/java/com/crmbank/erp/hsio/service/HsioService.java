package com.crmbank.erp.hsio.service;

import com.crmbank.erp.hasl.mapper.HaslMapper;
import com.crmbank.erp.hsio.mapper.HsioMapper;
import com.crmbank.erp.hsip.mapper.HsipMapper;
import com.crmbank.erp.ha00.mapper.Ha00Mapper;
import com.crmbank.erp.hsio.dto.*;
import com.crmbank.erp.hasl.dto.Hasl010u;
import com.crmbank.erp.hasl.dto.Hasl020u;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class HsioService {

    private final HsioMapper hsioMapper;
    private final HaslMapper haslMapper;
    private final HsipMapper hsipMapper;
    private final Ha00Mapper ha00Mapper;
    private final ObjectMapper objectMapper;


    @Transactional(value = "erpTransactionManager", rollbackFor = Exception.class)
    public Map<String, Object> saveRequest(Hsio010uRequest request, String userId) throws Exception {
        Hsio010u mst = request.getMst();
        List<Map<String, Object>> res = hsioMapper.HSIO_010U_STR(mst);
        if (res == null || res.isEmpty()) throw new Exception("No response from Master procedure.");
        Map<String, Object> mstRow = convertMapToLowerCase(res.getFirst());
        String reqym = nvl(mstRow.get("reqym"));
        String reqno = nvl(mstRow.get("reqno"));

        if (request.getDtl() != null) {
            for (Hsio011u dtl : request.getDtl()) {
                dtl.setReqym(reqym); dtl.setReqno(reqno);
                dtl.setCmpycd(mst.getCmpycd());
                dtl.setUpdemp(userId);
                hsioMapper.HSIO_011U_STR(dtl);
            }
        }
        return mstRow;
    }

    @Transactional(value = "erpTransactionManager", rollbackFor = Exception.class)
    public Map<String, Object> saveOrder(Hsio050uRequest request, String userId) throws Exception {
        Hsio050u mst = request.getMst();
        List<Map<String, Object>> res = hsioMapper.HSIO_050U_STR(mst);
        if (res == null || res.isEmpty()) throw new Exception("No response from Master procedure.");
        Map<String, Object> mstRow = convertMapToLowerCase(res.getFirst());
        String balym = nvl(mstRow.get("balym"));
        String balno = nvl(mstRow.get("balno"));

        if (request.getDtl() != null) {
            for (Hsio051u dtl : request.getDtl()) {
                dtl.setBalym(balym); dtl.setBalno(balno);
                dtl.setCmpycd(mst.getCmpycd());
                dtl.setUpdemp(userId);
                hsioMapper.HSIO_051U_STR(dtl);
            }
        }
        return mstRow;
    }

    @Transactional(value = "erpTransactionManager", rollbackFor = Exception.class)
    public Map<String, Object> saveGeneralOrder(Hsio052uRequest request, String userId) throws Exception {
        Hsio052u mst = request.getMst();
        List<Map<String, Object>> res = hsioMapper.HSIO_052U_STR(mst);
        if (res == null || res.isEmpty()) throw new Exception("No response from Master procedure.");
        Map<String, Object> mstRow = convertMapToLowerCase(res.getFirst());
        String balym = nvl(mstRow.get("balym"));
        String balno = nvl(mstRow.get("balno"));

        if (request.getDtl() != null) {
            for (Hsio051u dtl : request.getDtl()) {
                dtl.setBalym(balym); dtl.setBalno(balno);
                dtl.setCmpycd(mst.getCmpycd());
                dtl.setUpdemp(userId);
                hsioMapper.HSIO_051U_STR(dtl);
            }
        }
        return mstRow;
    }

    @Transactional(value = "erpTransactionManager", rollbackFor = Exception.class)
    public Map<String, Object> saveOtherIn(Hsio190uRequest request, String userId) throws Exception {
        Hsio190u mst = request.getMst();
        List<Map<String, Object>> res = hsioMapper.HSIO_190U_STR(mst);
        if (res == null || res.isEmpty()) throw new Exception("No response from Master procedure.");
        Map<String, Object> mstRow = convertMapToLowerCase(res.getFirst());
        String ioym = nvl(mstRow.get("ioym"));
        String iono = nvl(mstRow.get("iono"));

        if (request.getDtl() != null) {
            for (Hsio191u dtl : request.getDtl()) {
                dtl.setIoym(ioym); dtl.setIono(iono);
                dtl.setCmpycd(mst.getCmpycd());
                dtl.setIogbn(mst.getIogbn());
                dtl.setUpdemp(userId);
                hsioMapper.HSIO_191U_STR(dtl);
            }
        }
        return mstRow;
    }

    @Transactional(value = "erpTransactionManager", rollbackFor = Exception.class)
    public Map<String, Object> saveOtherOut(Hsio250uRequest request, String userId) throws Exception {
        Hsio250u mst = request.getMst();
        List<Map<String, Object>> res = hsioMapper.HSIO_250U_STR(mst);
        if (res == null || res.isEmpty()) throw new Exception("No response from Master procedure.");
        Map<String, Object> mstRow = convertMapToLowerCase(res.getFirst());
        String ioym = nvl(mstRow.get("ioym"));
        String iono = nvl(mstRow.get("iono"));

        if (request.getDtl() != null) {
            for (Hsio251u dtl : request.getDtl()) {
                dtl.setIoym(ioym); dtl.setIono(iono);
                dtl.setCmpycd(mst.getCmpycd());
                dtl.setIogbn(mst.getIogbn());
                dtl.setUpdemp(userId);
                hsioMapper.HSIO_251U_STR(dtl);
            }
        }
        return mstRow;
    }

    @Transactional(value = "erpTransactionManager", rollbackFor = Exception.class)
    public Map<String, Object> saveDeposit(Hsio300uRequest request, String userId) throws Exception {
        Hsio300u mst = request.getMst();
        List<Map<String, Object>> res = hsioMapper.HSIO_300U_STR(mst);
        if (res == null || res.isEmpty()) throw new Exception("No response from Master procedure.");
        Map<String, Object> mstRow = convertMapToLowerCase(res.getFirst());
        String imym = nvl(mstRow.get("imym"));
        String imno = nvl(mstRow.get("imno"));

        if (request.getDtl() != null) {
            for (Hsio300u dtl : request.getDtl()) {
                dtl.setImym(imym); dtl.setImno(imno);
                dtl.setCmpycd(mst.getCmpycd());
                dtl.setCustcd(mst.getCustcd());
                dtl.setUpdemp(userId);
                hsioMapper.HSIO_300U_STR(dtl);
            }
        }
        return mstRow;
    }

    @Transactional(value = "erpTransactionManager", rollbackFor = Exception.class)
    public Map<String, Object> saveDepositSlip(Hsio320uSaveRequest request, String userId) throws Exception {
        Hasl010u slipMst = request.getMst();
        List<Hsio320u> dtlList = request.getDtl();
        if (slipMst == null || dtlList == null || dtlList.isEmpty()) throw new Exception("발행할 데이터가 없습니다.");

        String cmpycd = slipMst.getCmpycd();
        String slipymd = nvl(slipMst.getSlipymd()).replace("-", "");
        String imsum = nvl(slipMst.getImsum(), "0");

        String autoSlipYn = "N";
        List<Map<String, Object>> p1Res = ha00Mapper.HA00_010S_STR(Map.of("cmpycd", cmpycd, "gubun", "P1"));
        if (p1Res != null && !p1Res.isEmpty()) {
            autoSlipYn = nvl(convertMapToLowerCase(p1Res.getFirst()).get("slipyn"), "N");
        }

        String acctymd = "Y".equals(autoSlipYn) ? slipymd : "";
        String slipKind = "040";

        if (nvl(slipMst.getBusiness()).isEmpty() && slipymd.length() == 8) {
            String business = String.format("%s년 %s월 %s일 외상매출금 입금 건(입금액:%s)",
                    slipymd.substring(0, 4), slipymd.substring(4, 6), slipymd.substring(6, 8), imsum);
            slipMst.setBusiness(business);
        }

        String currentSlipNo = slipMst.getSlipno();
        int proCnt = 0;
        for (Hsio320u item : dtlList) {
            if (proCnt % 50 == 0) {
                slipMst.setActkind("A");
                slipMst.setSlipymd(slipymd);
                slipMst.setAcctymd(acctymd);
                slipMst.setSlipgu(slipKind);
                slipMst.setUpdemp(userId);

                Map<String, Object> mstMap = objectMapper.convertValue(slipMst, new TypeReference<>() {});
                List<Map<String, Object>> mstRes = haslMapper.HASL_010U_STR(mstMap);
                if (mstRes == null || mstRes.isEmpty()) throw new Exception("전표 마스터 생성 실패");
                currentSlipNo = nvl(convertMapToLowerCase(mstRes.getFirst()).get("slipno"));
            }

            item.setActkind("U0");
            item.setCmpycd(cmpycd);
            item.setDeptcd(slipMst.getDeptcd());
            item.setImymdfr(nvl(slipMst.getFromdt()).replace("-", ""));
            item.setImymdto(nvl(slipMst.getTodt()).replace("-", ""));
            item.setSalsemp(nvl(slipMst.getSalsemp()));
            item.setSlipymd(slipymd);
            item.setSlipno(currentSlipNo);
            item.setUpdemp(userId);

            List<Map<String, Object>> itemRes = hsioMapper.HSIO_320U_STR(item);
            if (itemRes != null && !itemRes.isEmpty()) {
                Map<String, Object> row = convertMapToLowerCase(itemRes.getFirst());
                if ("Y".equals(nvl(row.get("erryn")))) {
                    throw new Exception(nvl(row.get("msg"), "입금 처리 중 오류"));
                }
            }
            proCnt++;
        }

        if ("Y".equals(autoSlipYn)) {
            Hasl020u confirmDto = new Hasl020u();
            confirmDto.setActkind("A0");
            confirmDto.setCmpycd(cmpycd);
            confirmDto.setSlipymd(slipymd);
            confirmDto.setAcctymd(acctymd);
            confirmDto.setSlipno(currentSlipNo);
            confirmDto.setDeptcd(slipMst.getDeptcd());
            confirmDto.setSlipkind(slipKind);
            confirmDto.setSlipyn("N");
            confirmDto.setCofmyn("Y");
            confirmDto.setUpdemp(userId);
            confirmDto.setEmpnm(slipMst.getEmpnm());

            haslMapper.HASL_020U_STR(objectMapper.convertValue(confirmDto, new TypeReference<>() {}));
        }

        return Map.of("slipymd", slipymd, "slipno", currentSlipNo, "res", "OK");
    }

    @Transactional(value = "erpTransactionManager", rollbackFor = Exception.class)
    public Map<String, Object> saveSettlement(Hsio510uRequest request, String userId) throws Exception {
        Hsio510u mst = request.getMst();
        List<Hsio510u> dtlList = request.getDtl();
        if (mst == null) throw new Exception("Request master data is null.");

        // 1. [A0] 마스터 저장 및 채번
        mst.setActkind("A0");
        mst.setIogbn("200");
        mst.setUpdemp(userId);
        
        List<Map<String, Object>> res = hsioMapper.HSIO_510U_STR(mst);
        if (res == null || res.isEmpty()) throw new Exception("마스터 저장 실패 (응답 없음)");

        Map<String, Object> mstRow = convertMapToLowerCase(res.get(0));
        String jsanym = nvl(mstRow.get("jsanym"));
        String jsanno = nvl(mstRow.get("jsanno"));

        if ("000000".equals(jsanym)) throw new Exception(nvl(jsanno, "마스터 저장 업무 오류"));
        if (jsanym.isEmpty() || jsanno.isEmpty()) throw new Exception("정산 채번 실패");

        // 2. [U0] 상세 내역 루프 저장
        if (dtlList != null) {
            log.info("📝 [매출정산] 상세 저장 시작 - 총 {}건", dtlList.size());
            for (int i = 0; i < dtlList.size(); i++) {
                Hsio510u dtl = dtlList.get(i);
                dtl.setActkind("U0");
                dtl.setCmpycd(mst.getCmpycd());
                dtl.setIogbn("200");
                dtl.setJsanym(jsanym); 
                dtl.setJsanno(jsanno);
                dtl.setUpdemp(userId);
                
                // 🚀 [해결] 마스터 데이터 완벽 상속 (상세 저장 시 누락 방지)
                dtl.setCustcd(mst.getCustcd());
                dtl.setDeptcd(mst.getDeptcd());
                dtl.setTaxunit(mst.getTaxunit());
                dtl.setVattype(mst.getVattype());
                dtl.setJsanymd(nvl(mst.getJsanymd()).replace("-", ""));
                dtl.setFromdt(nvl(mst.getFromdt()).replace("-", ""));
                dtl.setTodt(nvl(mst.getTodt()).replace("-", ""));
                
                // 원본 출고 정보 보존 및 데이터 정규화
                dtl.setIoym(nvl(dtl.getIoym()));
                dtl.setIono(nvl(dtl.getIono()));
                dtl.setIorowno(nvl(dtl.getIorowno()));
                dtl.setJsanqty(nvl(dtl.getJsanqty(), "0"));
                dtl.setJsanamt(nvl(dtl.getJsanamt(), "0"));
                dtl.setJsanvat(nvl(dtl.getJsanvat(), "0"));

                log.info("   ▶ [{}/{}] 상세 파라미터: {}", (i + 1), dtlList.size(), dtl);

                List<Map<String, Object>> dtlRes = hsioMapper.HSIO_510U_STR(dtl);
                if (dtlRes != null && !dtlRes.isEmpty()) {
                    Map<String, Object> dtlRow = convertMapToLowerCase(dtlRes.get(0));
                    // 🚀 [해결] 일관된 jsanym 키 기반 무결성 체크
                    String dtlStatus = nvl(dtlRow.get("jsanym"));
                    String dtlMsg = nvl(dtlRow.get("jsanno"));

                    if ("000000".equals(dtlStatus)) {
                        throw new Exception(dtlMsg.isEmpty() ? "상세 저장 업무 오류" : dtlMsg);
                    }
                }
            }
        }

        // 3. [V0] 최종 세금계산서 생성 확정
        mst.setActkind("V0");
        mst.setJsanym(jsanym);
        mst.setJsanno(jsanno);
        List<Map<String, Object>> vRes = hsioMapper.HSIO_510U_STR(mst);
        if (vRes != null && !vRes.isEmpty()) {
            Map<String, Object> vRow = convertMapToLowerCase(vRes.get(0));
            // 🚀 [해결] 최종 단계도 jsanym 키로 무결성 체크
            String vStatus = nvl(vRow.get("jsanym"));
            String vMsg = nvl(vRow.get("jsanno"));

            if ("000000".equals(vStatus)) {
                throw new Exception(vMsg.isEmpty() ? "최종 확정 업무 오류" : vMsg);
            }
        }

        return mstRow;
    }

    /**
     * Map의 모든 Key를 소문자로 변환 (HSOD100U 표준)
     */
    private Map<String, Object> convertMapToLowerCase(Map<String, Object> map) {
        if (map == null) return new HashMap<>();
        Map<String, Object> newMap = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            newMap.put(entry.getKey().toLowerCase(), entry.getValue());
        }
        return newMap;
    }

    @Transactional(value = "erpTransactionManager", rollbackFor = Exception.class)
    public Map<String, Object> savePurchase(Hsio500uRequest request, String userId) throws Exception {
        Hsio500u mst = request.getMst();
        List<Map<String, Object>> res = hsioMapper.HSIO_500U_STR(mst);
        if (res == null || res.isEmpty()) throw new Exception("No response from Master procedure.");
        Map<String, Object> mstRow = convertMapToLowerCase(res.getFirst());
        String ioym = nvl(mstRow.get("ioym"));
        String iono = nvl(mstRow.get("iono"));

        if (request.getDtl() != null) {
            for (Hsio501u dtl : request.getDtl()) {
                dtl.setIoym(ioym); dtl.setIono(iono);
                dtl.setCmpycd(mst.getCmpycd());
                dtl.setIogbn(mst.getIogbn());
                dtl.setUpdemp(userId);
                hsioMapper.HSIO_501U_STR(dtl);
            }
        }
        return mstRow;
    }

    @Transactional(value = "erpTransactionManager", rollbackFor = Exception.class)
    public Map<String, Object> saveWarehouseTransfer(Hsio580uRequest request, String userId) throws Exception {
        Hsio580u mst = request.getMst();
        List<Map<String, Object>> res = hsioMapper.HSIO_580U_STR(mst);
        if (res == null || res.isEmpty()) throw new Exception("No response from Master procedure.");
        Map<String, Object> mstRow = convertMapToLowerCase(res.getFirst());
        String ioym = nvl(mstRow.get("ioym"));
        String iono = nvl(mstRow.get("iono"));

        if (request.getDtl() != null) {
            for (Hsio581u dtl : request.getDtl()) {
                dtl.setIoym(ioym); dtl.setIono(iono);
                dtl.setCmpycd(mst.getCmpycd());
                dtl.setIogbn(mst.getIogbn());
                dtl.setUpdemp(userId);
                hsioMapper.HSIO_581U_STR(dtl);
            }
        }
        return mstRow;
    }

    @Transactional(value = "erpTransactionManager", rollbackFor = Exception.class)
    public Map<String, Object> saveStockAdjustment(Hsio720uRequest request, String userId) throws Exception {
        Hsio720u mst = request.getMst();
        List<Map<String, Object>> res = hsioMapper.HSIO_720U_STR(mst);
        if (res == null || res.isEmpty()) throw new Exception("No response from Master procedure.");
        Map<String, Object> mstRow = convertMapToLowerCase(res.getFirst());
        String ioym = nvl(mstRow.get("ioym"));
        String iono = nvl(mstRow.get("iono"));

        if (request.getDtl() != null) {
            for (Hsio721u dtl : request.getDtl()) {
                dtl.setIoym(ioym); dtl.setIno(iono);
                dtl.setCmpycd(mst.getCmpycd());
                dtl.setUpdemp(userId);
                hsioMapper.HSIO_721U_STR(dtl);
            }
        }
        return mstRow;
    }

    @Transactional(value = "erpTransactionManager", rollbackFor = Exception.class)
    public Map<String, Object> saveInventoryAdjustment(Hsio730Request request, String userId) throws Exception {
        Hsio730u mst = request.getMst();
        List<Map<String, Object>> res = hsioMapper.HSIO_730U_STR(mst);
        if (res == null || res.isEmpty()) throw new Exception("No response from Master procedure.");
        Map<String, Object> mstRow = convertMapToLowerCase(res.getFirst());
        String ioym = nvl(mstRow.get("ioym"));
        String iono = nvl(mstRow.get("iono"));

        if (request.getDtl() != null) {
            for (Hsio731u dtl : request.getDtl()) {
                dtl.setIoym(ioym); dtl.setIno(iono);
                dtl.setCmpycd(mst.getCmpycd());
                dtl.setUpdemp(userId);
                hsioMapper.HSIO_731U_STR(dtl);
            }
        }
        return mstRow;
    }

    @Transactional(value = "erpTransactionManager", rollbackFor = Exception.class)
    public Map<String, Object> saveStoreInout(Hsio570uRequest request, String userId) throws Exception {
        Hsio570u mst = request.getMst();
        List<Map<String, Object>> res = hsioMapper.HSIO_570U_STR(mst);
        if (res == null || res.isEmpty()) throw new Exception("No response from Master procedure.");
        Map<String, Object> mstRow = convertMapToLowerCase(res.getFirst());
        String ioym = nvl(mstRow.get("ioym"));
        String iono = nvl(mstRow.get("iono"));

        if (request.getDtl() != null) {
            for (Hsio571u dtl : request.getDtl()) {
                dtl.setIoym(ioym); dtl.setIono(iono);
                dtl.setCmpycd(mst.getCmpycd());
                dtl.setIogbn(mst.getIogbn());
                dtl.setUpdemp(userId);
                hsioMapper.HSIO_571U_STR(dtl);
            }
        }
        return mstRow;
    }

    @Transactional(value = "erpTransactionManager", rollbackFor = Exception.class)
    public Map<String, Object> saveBatchSettlement(Hsio590uRequest request, String userId) {
        List<Hsio590u> items = request.getList();
        if (items == null) return Map.of("res", "OK");
        for (Hsio590u item : items) {
            item.setActkind("U0");
            item.setUpdemp(userId);
            hsioMapper.HSIO_590U_STR(item);
        }
        return Map.of("res", "OK");
    }

    @Transactional(value = "erpTransactionManager", rollbackFor = Exception.class)
    public Map<String, Object> saveSalesReturn(Hsio490uRequest request, String userId) throws Exception {
        Hsio490u mst = request.getMst();
        List<Map<String, Object>> res = hsioMapper.HSIO_490U_STR(mst);
        if (res == null || res.isEmpty()) throw new Exception("No response from Master procedure.");
        Map<String, Object> mstRow = convertMapToLowerCase(res.getFirst());
        String ioym = nvl(mstRow.get("ioym"));
        String iono = nvl(mstRow.get("iono"));

        if (request.getDtl() != null) {
            for (Hsio491u dtl : request.getDtl()) {
                dtl.setIoym(ioym); dtl.setIono(iono);
                dtl.setCmpycd(mst.getCmpycd());
                dtl.setIogbn(mst.getIogbn());
                dtl.setUpdemp(userId);
                hsioMapper.HSIO_491U_STR(dtl);
            }
        }
        return mstRow;
    }

    @Transactional(value = "erpTransactionManager", rollbackFor = Exception.class)
    public Map<String, Object> saveHSIO600U(Map<String, Object> params) {
        List<Map<String, Object>> res = hsioMapper.HSIO_600U_STR(params);
        if (res != null && !res.isEmpty()) return convertMapToLowerCase(res.getFirst());
        return Map.of("res", "OK");
    }

    @Transactional(value = "erpTransactionManager", rollbackFor = Exception.class)
    public Map<String, Object> saveOutbound550(Hsio550uSaveRequest request, String userId) throws Exception {
        Hsio550u mst = request.getMst();
        List<Hsio551u> dtlList = request.getDtl();
        
        if (mst == null) throw new Exception("Master data is null.");
        
        mst.setActkind("A0");
        mst.setUpdemp(userId);
        mst.setIogbn("200");
        
        String ioymd = nvl(mst.getIoymd()).replace("-", "");
        if (ioymd.length() > 8) ioymd = ioymd.substring(0, 8);
        mst.setIoymd(ioymd);
        
        mst.setFromdt(nvl(mst.getFromdt()).replace("-", ""));
        mst.setTodt(nvl(mst.getTodt()).replace("-", ""));
        
        List<Map<String, Object>> resM = hsioMapper.HSIO_550U_STR(mst);
        if (resM == null || resM.isEmpty()) throw new Exception("출고 마스터 저장 실패 (응답 없음)");
        
        Map<String, Object> mstRow = convertMapToLowerCase(resM.get(0));
        String ioym = nvl(mstRow.get("ioym"));
        String iono = nvl(mstRow.get("iono"));

        if ("000000".equals(ioym)) {
            throw new Exception(nvl(iono, "출고 마스터 업무 오류"));
        }
        
        if (ioym.isEmpty() || iono.isEmpty()) {
            throw new Exception("출고 번호 채번 실패");
        }

        // 2. Detail Save
        if (dtlList != null) {
            log.info("📝 [주문출고] 상세 저장 시작 - 총 {}건", dtlList.size());
            for (int i = 0; i < dtlList.size(); i++) {
                Hsio551u dtl = dtlList.get(i);
                dtl.setActkind("A0");
                dtl.setCmpycd(mst.getCmpycd());
                dtl.setIoym(ioym);
                dtl.setIono(iono);
                dtl.setIoymd(mst.getIoymd());
                dtl.setIogbn("200");
                dtl.setUpdemp(userId);
                
                // 🚀 [해결] 마스터 데이터 완벽 상속 (custcd, whcd, iotype)
                dtl.setCustcd(mst.getCustcd());
                dtl.setWhcd(mst.getWhcd());
                dtl.setIotype(nvl(mst.getIotype(), "100"));
                
                // 데이터 정규화
                dtl.setIoqty(nvl(dtl.getIoqty(), "0"));
                dtl.setIoamt(nvl(dtl.getIoamt(), "0"));
                dtl.setIovat(nvl(dtl.getIovat(), "0"));
                
                String balym = nvl(dtl.getBalym()).replace("-", "");
                if (balym.length() > 6) balym = balym.substring(0, 6);
                dtl.setBalym(balym);

                log.info("   ▶ [{}/{}] 상세 파라미터: {}", (i + 1), dtlList.size(), dtl);
                
                List<Map<String, Object>> resD = hsioMapper.HSIO_551U_STR(dtl);
                if (resD != null && !resD.isEmpty()) {
                    Map<String, Object> dtlRow = convertMapToLowerCase(resD.get(0));
                    // 🚀 [해결] 상세 저장 시에도 ioym 키로 000000 에러 체크
                    String dtlStatus = nvl(dtlRow.get("ioym"));
                    String dtlMsg = nvl(dtlRow.get("iono"));

                    if ("000000".equals(dtlStatus)) {
                        throw new Exception(dtlMsg.isEmpty() ? "상세 저장 업무 오류" : dtlMsg);
                    }
                }
            }
        }
        
        return mstRow;
    }

    @Transactional(value = "erpTransactionManager", rollbackFor = Exception.class)
    public void transferExternalSlip(Map<String, Object> params, String userId) throws Exception {
        String cmpycd = nvl(params.get("cmpycd"));
        String slipymd = nvl(params.get("slipymd")).replace("-", "");
        String slipno = nvl(params.get("slipno"));

        List<Map<String, Object>> details = hsioMapper.getSlipDetailsForTransfer(Map.of("cmpycd", cmpycd, "slipymd", slipymd, "slipno", slipno));
        if (details == null || details.isEmpty()) throw new Exception("전송할 전표 상세 내역이 없습니다.");

        for (Map<String, Object> row : details) {
            Map<String, Object> lowerRow = convertMapToLowerCase(row);
            log.info("🚀 [THEJONE 전송 대기] SLIP: {}-{}, ACCT: {}, DEAL: {}", 
                slipymd, slipno, lowerRow.get("acctcd"), lowerRow.get("custcd"));
        }

        Map<String, Object> upParams = new HashMap<>();
        upParams.put("actkind", "U1");
        upParams.put("cmpycd", cmpycd);
        upParams.put("slipymd", slipymd);
        upParams.put("slipno", slipno);
        upParams.put("updemp", userId);
        hsioMapper.HSIO_990U_STR(upParams);
    }

    @Transactional(value = "erpTransactionManager", rollbackFor = Exception.class)
    public Map<String, Object> saveInbound060(Hsio060uSaveRequest request, String userId) throws Exception {
        Map<String, Object> mst = request.getMst();
        List<Map<String, Object>> items = request.getItems();
        String iogbn = nvl(mst.get("iogbn"), "100");
        String cmpycd = nvl(mst.get("cmpycd"));

        Map<String, Object> mParams = new HashMap<>(mst);
        mParams.put("actkind", "A0"); mParams.put("updemp", userId);
        List<Map<String, Object>> resM = hsioMapper.HSIO_060U_STR(mParams);
        if (resM == null || resM.isEmpty()) throw new Exception("입고 마스터 생성 실패");
        Map<String, Object> rowM = convertMapToLowerCase(resM.getFirst());
        String ioym = nvl(rowM.get("ioym"));
        String iono = nvl(rowM.get("iono"));

        for (Map<String, Object> item : items) {
            Map<String, Object> dParams = new HashMap<>(item);

            // 🚀 발주금액/수량 비례 입고금액/부가세 계산
            double balqty = Double.parseDouble(nvl(item.get("balqty"), "0"));
            double balamt = Double.parseDouble(nvl(item.get("balamt"), "0"));
            double balvat = Double.parseDouble(nvl(item.get("balvat"), "0"));
            double ioqty = Double.parseDouble(nvl(item.get("ioqty"), "0"));

            if (balqty > 0) {
                long ioamt = Math.round((balamt / balqty) * ioqty);
                long iovat = Math.round((balvat / balqty) * ioqty);
                dParams.put("ioamt", ioamt);
                dParams.put("iovat", iovat);
            }

            dParams.put("actkind", "A0");
            dParams.put("cmpycd", cmpycd);
            dParams.put("iogbn", iogbn);
            dParams.put("ioym", ioym);
            dParams.put("iono", iono);
            dParams.put("updemp", userId);
            hsioMapper.HSIO_061U_STR(dParams);
        }
        return Map.of("ioym", ioym, "iono", iono, "res", "OK");
    }

    @Transactional(value = "erpTransactionManager", rollbackFor = Exception.class)
    public Map<String, Object> generateSlip130(Hsio130uSaveRequest request, String userId) throws Exception {
        Map<String, Object> mst = request.getMst();
        List<Map<String, Object>> items = request.getItems();

        String slipymd = nvl(mst.get("pubymd")).replace("-", "");
        String cmpycd = nvl(mst.get("cmpycd"));
        String deptcd = nvl(mst.get("deptcd"));
        String usernm = nvl(mst.get("usernm"));

        String autoSlip = "N";
        List<Map<String, Object>> resSet = hsipMapper.HSIP_112U_STR(Map.of("cmpycd", cmpycd, "gbn", "P1")); 
        if (resSet != null && !resSet.isEmpty()) autoSlip = nvl(convertMapToLowerCase(resSet.getFirst()).get("slipyn"), "N");
        String acctymd = "Y".equals(autoSlip) ? slipymd : "";

        Map<String, Object> mParams = createParamMap();
        safePut(mParams, "actkind", "A");
        safePut(mParams, "cmpycd", cmpycd);
        safePut(mParams, "slipymd", slipymd);
        safePut(mParams, "acctymd", acctymd);
        safePut(mParams, "deptcd", deptcd);
        safePut(mParams, "empnm", usernm);
        safePut(mParams, "slipgu", "030");
        safePut(mParams, "slipkind", "030");
        safePut(mParams, "business", slipymd.substring(0, 4) + "년 " + slipymd.substring(4, 6) + "월 매입 건");
        safePut(mParams, "updemp", userId);

        List<Map<String, Object>> resMst = haslMapper.HASL_010U_STR(mParams);
        if (resMst == null || resMst.isEmpty()) throw new Exception("전표 마스터 생성 실패");
        String slipno = nvl(convertMapToLowerCase(resMst.getFirst()).get("slipno"));

        for (Map<String, Object> item : items) {
            Map<String, Object> dParams = new HashMap<>();
            dParams.put("actkind", "U0");
            dParams.put("cmpycd", cmpycd);
            dParams.put("iogbn", "100");
            dParams.put("fromdt", nvl(mst.get("fromdt")).replace("-", ""));
            dParams.put("todt", nvl(mst.get("todt")).replace("-", ""));
            dParams.put("deptcd", nvl(item.get("deptcd")).isEmpty() ? deptcd : nvl(item.get("deptcd")));
            dParams.put("jsanym", nvl(item.get("jsanym")));
            dParams.put("jsanno", nvl(item.get("jsanno")));
            dParams.put("jsanymd", nvl(item.get("jsanymd")).replace("-", ""));
            dParams.put("spyamt", nvl(item.get("spyamt"), "0").replace(",", ""));
            dParams.put("vatamt", nvl(item.get("vatamt"), "0").replace(",", ""));
            dParams.put("custcd", nvl(item.get("custcd")));
            dParams.put("taxunit", nvl(mst.get("taxunit")));
            dParams.put("vattype", nvl(mst.get("vattype"), "010"));
            dParams.put("slipymd", slipymd);
            dParams.put("slipno", slipno);
            dParams.put("updemp", userId);

            List<Map<String, Object>> resD = hsioMapper.HSIO_130U_STR(dParams);
            if (resD != null && !resD.isEmpty()) {
                Map<String, Object> rowD = convertMapToLowerCase(resD.getFirst());
                if ("Y".equals(nvl(rowD.get("erryn")))) {
                    throw new Exception(nvl(rowD.get("msg"), "정산 연동 실패"));
                }
            }
        }

        if ("Y".equals(autoSlip)) {
            Map<String, Object> cParams = createParamMap();
            safePut(cParams, "actkind", "A0");
            safePut(cParams, "cmpycd", cmpycd);
            safePut(cParams, "slipymd", slipymd);
            safePut(cParams, "acctymd", acctymd);
            safePut(cParams, "slipno", slipno);
            safePut(cParams, "deptcd", deptcd);
            safePut(cParams, "slipkind", "030");
            safePut(cParams, "slipyn", "N");
            safePut(cParams, "cofmyn", "Y");
            safePut(cParams, "cofmemp", usernm);
            safePut(cParams, "updemp", userId);
            haslMapper.HASL_020U_STR(cParams);
        }

        return Map.of("slipno", slipno, "res", "OK");
    }

    @Transactional(value = "erpTransactionManager", rollbackFor = Exception.class)
    public Map<String, Object> generateSlip131(Hsio131uSaveRequest request, String userId) throws Exception {
        Map<String, Object> mst = request.getMst();
        List<Map<String, Object>> items = request.getItems();
        if (items == null || items.isEmpty()) throw new Exception("발행할 데이터가 없습니다.");

        String slipymd = nvl(mst.get("pubymd")).replace("-", "");
        String cmpycd = nvl(mst.get("cmpycd"));
        String deptcd = nvl(mst.get("deptcd"));
        String ioymdfr = nvl(mst.get("fromdt")).replace("-", "");
        String ioymdto = nvl(mst.get("todt")).replace("-", "");

        for (Map<String, Object> item : items) {
            String costcd = nvl(item.get("costcd"));
            String itemFileno = nvl(item.get("fileno"));
            String docno = nvl(item.get("docno"));
            String crowno = nvl(item.get("crowno"));
            String slipKind = "200".equals(costcd) ? "031" : "030";
            String itemDeptcd = nvl(item.get("deptcd"), deptcd);

            Map<String, Object> baseParams = new HashMap<>();
            baseParams.put("cmpycd", cmpycd);
            baseParams.put("iogbn", "100");
            baseParams.put("costcd", costcd);
            baseParams.put("fromdt", ioymdfr);
            baseParams.put("todt", ioymdto);
            baseParams.put("deptcd", itemDeptcd);
            baseParams.put("fileno", itemFileno);
            baseParams.put("docno", docno);
            baseParams.put("crowno", crowno);
            baseParams.put("jsanymd", nvl(item.get("jsanymd")).replace("-", ""));
            baseParams.put("spyamt", nvl(item.get("spyamt"), "0").replace(",", ""));
            baseParams.put("vatamt", nvl(item.get("vatamt"), "0").replace(",", ""));
            baseParams.put("custcd", nvl(item.get("custcd")));
            baseParams.put("taxunit", nvl(item.get("taxunit"), "100"));
            baseParams.put("vattype", nvl(item.get("vattype"), "010"));
            baseParams.put("slipymd", slipymd);
            baseParams.put("slipno", "");
            baseParams.put("slipkind", slipKind);
            baseParams.put("hdeptcd", itemDeptcd);
            baseParams.put("business", itemFileno + "-" + nvl(item.get("bigo")));
            baseParams.put("updemp", userId);

            baseParams.put("actkind", "A0");
            List<Map<String, Object>> resA = hsioMapper.HSIO_131U_STR(baseParams);
            if (resA == null || resA.isEmpty()) throw new Exception("전표번호 채번 실패");
            Map<String, Object> rowA = convertMapToLowerCase(resA.getFirst());
            String slipno = nvl(rowA.get("slipno"));

            baseParams.put("actkind", "U0");
            baseParams.put("slipno", slipno);
            List<Map<String, Object>> resU = hsioMapper.HSIO_131U_STR(baseParams);
            if (resU != null && !resU.isEmpty()) {
                Map<String, Object> rowU = convertMapToLowerCase(resU.getFirst());
                if ("00000000".equals(nvl(rowU.get("status")))) {
                    throw new Exception("정산 저장 실패: " + nvl(rowU.get("msg"), "업무 오류"));
                }
            }
        }
        return Map.of("res", "OK");
    }

    @Transactional(value = "erpTransactionManager", rollbackFor = Exception.class)
    public Map<String, Object> cancelSlips140(Hsio140uCancelRequest request, String userId) throws Exception {
        String cmpycd = request.getCmpycd();
        String autoSlip = nvl(request.getAutoSlip(), "N");
        String fromdt = nvl(request.getFromdt()).replace("-", "");
        String todt = nvl(request.getTodt()).replace("-", "");

        for (Map<String, Object> item : request.getItems()) {
            String slipymd = nvl(item.get("slipymd"));
            String slipno = nvl(item.get("slipno"));
            String udeptcd = nvl(item.get("deptcd"));

            if ("Y".equals(autoSlip)) {
                Map<String, Object> haslParams = createParamMap();
                safePut(haslParams, "actkind", "A0");
                safePut(haslParams, "cmpycd", cmpycd);
                safePut(haslParams, "slipymd", slipymd);
                safePut(haslParams, "acctymd", slipymd);
                safePut(haslParams, "slipno", slipno);
                safePut(haslParams, "deptcd", udeptcd);
                safePut(haslParams, "slipkind", "030");
                safePut(haslParams, "slipyn", "Y");
                safePut(haslParams, "cofmyn", "N");
                safePut(haslParams, "updemp", userId);
                haslMapper.HASL_020U_STR(haslParams);
            }

            Map<String, Object> hsioParams = new HashMap<>();
            hsioParams.put("actkind", "D0");
            hsioParams.put("cmpycd", cmpycd);
            hsioParams.put("fromdt", fromdt);
            hsioParams.put("todt", todt);
            hsioParams.put("deptcd", udeptcd);
            hsioParams.put("slipymd", slipymd);
            hsioParams.put("slipno", slipno);
            hsioParams.put("updemp", userId);

            List<Map<String, Object>> res = hsioMapper.HSIO_140U_STR(hsioParams);
            if (res != null && !res.isEmpty()) {
                Map<String, Object> row = convertMapToLowerCase(res.getFirst());
                if ("Y".equals(nvl(row.get("erryn")))) {
                    throw new Exception("전표[" + slipno + "] 취소 실패: " + nvl(row.get("msg")));
                }
            }
        }
        return Map.of("res", "OK");
    }

    @Transactional(value = "erpTransactionManager", rollbackFor = Exception.class)
    public Map<String, Object> generateSlip530(Hsio530uSaveRequest request, String userId) throws Exception {
        Map<String, Object> mst = request.getMst();
        List<Map<String, Object>> items = request.getItems();

        String slipymd = nvl(mst.get("slipymd")).replace("-", "");
        String cmpycd = nvl(mst.get("cmpycd"));
        String deptcd = nvl(mst.get("deptcd"));
        String usernm = nvl(mst.get("usernm"));

        String autoSlip = "N";
        List<Map<String, Object>> resSet = hsipMapper.HSIP_112U_STR(Map.of("cmpycd", cmpycd, "gbn", "P1"));
        if (resSet != null && !resSet.isEmpty()) autoSlip = nvl(convertMapToLowerCase(resSet.getFirst()).get("slipyn"), "N");
        String acctymd = "Y".equals(autoSlip) ? slipymd : "";

        Map<String, Object> mParams = createParamMap();
        safePut(mParams, "actkind", "A");
        safePut(mParams, "cmpycd", cmpycd);
        safePut(mParams, "slipymd", slipymd);
        safePut(mParams, "acctymd", acctymd);
        safePut(mParams, "deptcd", deptcd);
        safePut(mParams, "empnm", usernm);
        safePut(mParams, "slipgu", "040");
        safePut(mParams, "slipkind", "040");
        safePut(mParams, "business", slipymd.substring(0, 4) + "년 " + slipymd.substring(4, 6) + "월 상품 매출 건");
        safePut(mParams, "updemp", userId);

        List<Map<String, Object>> resMst = haslMapper.HASL_010U_STR(mParams);
        if (resMst == null || resMst.isEmpty()) throw new Exception("전표 마스터 생성 실패");
        Map<String, Object> rowM = convertMapToLowerCase(resMst.getFirst());
        String slipno = nvl(rowM.get("slipno"));

        for (Map<String, Object> item : items) {
            Map<String, Object> dParams = new HashMap<>();
            dParams.put("actkind", "U0");
            dParams.put("cmpycd", cmpycd);
            dParams.put("iogbn", "200");
            dParams.put("fromdt", nvl(mst.get("fromdt")).replace("-", ""));
            dParams.put("todt", nvl(mst.get("todt")).replace("-", ""));
            dParams.put("deptcd", nvl(item.get("udeptcd"), nvl(item.get("deptcd"), deptcd)));
            dParams.put("jsanym", nvl(item.get("jsanym")));
            dParams.put("jsanno", nvl(item.get("jsanno")));
            dParams.put("jsanymd", nvl(item.get("jsanymd")).replace("-", ""));
            dParams.put("spyamt", nvl(item.get("spyamt"), "0").replace(",", ""));
            dParams.put("vatamt", nvl(item.get("vatamt"), "0").replace(",", ""));
            dParams.put("custcd", nvl(item.get("custcd")));
            dParams.put("taxunit", nvl(item.get("taxunit")));
            dParams.put("vattype", nvl(item.get("vattype"), "010"));
            dParams.put("slipymd", slipymd);
            dParams.put("slipno", slipno);
            dParams.put("updemp", userId);

            List<Map<String, Object>> resD = hsioMapper.HSIO_530U_STR(dParams);
            if (resD != null && !resD.isEmpty()) {
                Map<String, Object> rowD = convertMapToLowerCase(resD.getFirst());
                if ("Y".equals(nvl(rowD.get("erryn")))) {
                    throw new Exception(nvl(rowD.get("msg"), "정산 연동 실패"));
                }
            }
        }

        if ("Y".equals(autoSlip)) {
            Map<String, Object> cParams = createParamMap();
            safePut(cParams, "actkind", "A0");
            safePut(cParams, "cmpycd", cmpycd);
            safePut(cParams, "slipymd", slipymd);
            safePut(cParams, "acctymd", acctymd);
            safePut(cParams, "slipno", slipno);
            safePut(cParams, "deptcd", deptcd);
            safePut(cParams, "slipkind", "040");
            safePut(cParams, "slipyn", "N");
            safePut(cParams, "cofmyn", "Y");
            safePut(cParams, "cofmemp", usernm);
            safePut(cParams, "updemp", userId);
            haslMapper.HASL_020U_STR(cParams);
        }

        return Map.of("slipno", slipno, "res", "OK");
    }

    @Transactional(value = "erpTransactionManager", rollbackFor = Exception.class)
    public Map<String, Object> cancelSlips540(Hsio540uCancelRequest request, String userId) throws Exception {
        String cmpycd = request.getCmpycd();
        String autoSlip = nvl(request.getAutoSlip(), "N");
        String fromdt = nvl(request.getFromdt()).replace("-", "");
        String todt = nvl(request.getTodt()).replace("-", "");

        for (Map<String, Object> item : request.getItems()) {
            String slipymd = nvl(item.get("slipymd"));
            String slipno = nvl(item.get("slipno"));
            String udeptcd = nvl(item.get("deptcd"));

            if ("Y".equals(autoSlip)) {
                Map<String, Object> haslParams = createParamMap();
                safePut(haslParams, "actkind", "A0");
                safePut(haslParams, "cmpycd", cmpycd);
                safePut(haslParams, "slipymd", slipymd);
                safePut(haslParams, "acctymd", slipymd);
                safePut(haslParams, "slipno", slipno);
                safePut(haslParams, "deptcd", udeptcd);
                safePut(haslParams, "slipkind", "040");
                safePut(haslParams, "slipyn", "Y");
                safePut(haslParams, "cofmyn", "N");
                safePut(haslParams, "updemp", userId);
                haslMapper.HASL_020U_STR(haslParams);
            }

            Map<String, Object> hsioParams = new HashMap<>();
            hsioParams.put("actkind", "D0");
            hsioParams.put("cmpycd", cmpycd);
            hsioParams.put("iogbn", "200");
            hsioParams.put("fromdt", fromdt);
            hsioParams.put("todt", todt);
            hsioParams.put("deptcd", udeptcd);
            hsioParams.put("slipymd", slipymd);
            hsioParams.put("slipno", slipno);
            hsioParams.put("updemp", userId);

            List<Map<String, Object>> res = hsioMapper.HSIO_540U_STR(hsioParams);
            if (res != null && !res.isEmpty()) {
                Map<String, Object> row = convertMapToLowerCase(res.getFirst());
                if ("Y".equals(nvl(row.get("erryn")))) {
                    throw new Exception("전표[" + slipno + "] 취소 실패: " + nvl(row.get("msg")));
                }
            }
        }
        return Map.of("res", "OK");
    }

    @Transactional(value = "erpTransactionManager", rollbackFor = Exception.class)
    public Map<String, Object> generateSlip531(Hsio531uSaveRequest request, String userId) throws Exception {
        Map<String, Object> mst = request.getMst();
        List<Map<String, Object>> items = request.getItems();
        if (items == null || items.isEmpty()) throw new Exception("발행할 데이터가 없습니다.");

        String slipymd = nvl(mst.get("slipymd")).replace("-", "");
        String cmpycd = nvl(mst.get("cmpycd"));
        String deptcd = nvl(mst.get("deptcd"));
        String ioymdfr = nvl(mst.get("fromdt")).replace("-", "");
        String ioymdto = nvl(mst.get("todt")).replace("-", "");
        String business = nvl(mst.get("business"));

        Map<String, Object> mParams = new HashMap<>();
        mParams.put("actkind", "A0");
        mParams.put("cmpycd", cmpycd);
        mParams.put("iogbn", "200");
        mParams.put("fromdt", ioymdfr);
        mParams.put("todt", ioymdto);
        mParams.put("deptcd", deptcd);
        mParams.put("slipymd", slipymd);
        mParams.put("slipno", "");
        mParams.put("slipkind", "040");
        mParams.put("business", business);
        mParams.put("userid", userId);

        List<Map<String, Object>> resM = hsioMapper.HSIO_531U_STR(mParams);
        if (resM == null || resM.isEmpty()) throw new Exception("전표 마스터 발행 실패 (번호 채번 오류)");
        Map<String, Object> rowM = convertMapToLowerCase(resM.getFirst());
        String slipno = nvl(rowM.get("slipno"));

        for (Map<String, Object> item : items) {
            Map<String, Object> dParams = new HashMap<>();
            dParams.put("actkind", "U0");
            dParams.put("cmpycd", cmpycd);
            dParams.put("iogbn", "200");
            dParams.put("fromdt", ioymdfr);
            dParams.put("todt", ioymdto);
            dParams.put("deptcd", nvl(item.get("deptcd")));
            dParams.put("jsanym", nvl(item.get("jsanym")));
            dParams.put("jsanno", nvl(item.get("jsanno")));
            dParams.put("jsanymd", nvl(item.get("jsanymd")).replace("-", ""));
            dParams.put("spyamt", nvl(item.get("spyamt"), "0").replace(",", ""));
            dParams.put("vatamt", nvl(item.get("vatamt"), "0").replace(",", ""));
            dParams.put("custcd", nvl(item.get("custcd")));
            dParams.put("taxunit", nvl(item.get("taxunit")));
            dParams.put("vattype", nvl(item.get("vattype")));
            dParams.put("slipymd", slipymd);
            dParams.put("slipno", slipno);
            dParams.put("slipkind", "040");
            dParams.put("hdeptcd", deptcd);
            dParams.put("business", business);
            dParams.put("userid", userId);

            List<Map<String, Object>> resD = hsioMapper.HSIO_531U_STR(dParams);
            if (resD != null && !resD.isEmpty()) {
                Map<String, Object> rowD = convertMapToLowerCase(resD.getFirst());
                if ("Y".equals(nvl(rowD.get("erryn")))) {
                    throw new Exception(nvl(rowD.get("msg"), "전표 상세 연동 실패"));
                }
            }
        }
        return Map.of("slipno", slipno, "res", "OK");
    }

    @Transactional(value = "erpTransactionManager", rollbackFor = Exception.class)
    public Map<String, Object> cancelSlips541(Hsio541uCancelRequest request, String userId) throws Exception {
        String cmpycd = request.getCmpycd();
        String autoSlip = nvl(request.getAutoSlip(), "N");
        String fromdt = nvl(request.getFromdt()).replace("-", "");
        String todt = nvl(request.getTodt()).replace("-", "");

        for (Map<String, Object> item : request.getItems()) {
            String slipymd = nvl(item.get("slipymd"));
            String slipno = nvl(item.get("slipno"));
            String udeptcd = nvl(item.get("deptcd"));

            if ("Y".equals(autoSlip)) {
                Map<String, Object> haslParams = createParamMap();
                safePut(haslParams, "actkind", "A0");
                safePut(haslParams, "cmpycd", cmpycd);
                safePut(haslParams, "slipymd", slipymd);
                safePut(haslParams, "acctymd", slipymd);
                safePut(haslParams, "slipno", slipno);
                safePut(haslParams, "deptcd", udeptcd);
                safePut(haslParams, "slipkind", "040");
                safePut(haslParams, "slipyn", "Y");
                safePut(haslParams, "cofmyn", "N");
                safePut(haslParams, "updemp", userId);
                haslMapper.HASL_020U_STR(haslParams);
            }

            Map<String, Object> hsioParams = new HashMap<>();
            hsioParams.put("actkind", "D0");
            hsioParams.put("cmpycd", cmpycd);
            hsioParams.put("iogbn", "200");
            hsioParams.put("fromdt", fromdt);
            hsioParams.put("todt", todt);
            hsioParams.put("deptcd", udeptcd);
            hsioParams.put("slipymd", slipymd);
            hsioParams.put("slipno", slipno);
            hsioParams.put("updemp", userId);

            List<Map<String, Object>> res = hsioMapper.HSIO_541U_STR(hsioParams);
            if (res != null && !res.isEmpty()) {
                Map<String, Object> row = convertMapToLowerCase(res.getFirst());
                if ("Y".equals(nvl(row.get("erryn")))) {
                    throw new Exception("전표[" + slipno + "] 취소 실패: " + nvl(row.get("msg")));
                }
            }
        }
        return Map.of("res", "OK");
    }

    @Transactional(value = "erpTransactionManager", rollbackFor = Exception.class)
    public Map<String, Object> generateSlip325(Hsio325uSaveRequest request, String userId) throws Exception {
        Map<String, Object> mst = request.getMst();
        List<Map<String, Object>> items = request.getItems();
        if (items == null || items.isEmpty()) throw new Exception("발행할 데이터가 없습니다.");

        String slipymd = nvl(mst.get("slipymd")).replace("-", "");
        String cmpycd = nvl(mst.get("cmpycd"));
        String deptcd = nvl(mst.get("deptcd"));
        String usernm = nvl(mst.get("usernm"));

        String autoSlip = "N";
        List<Map<String, Object>> resSet = ha00Mapper.HA00_010S_STR(Map.of("cmpycd", cmpycd, "gubun", "P1"));
        if (resSet != null && !resSet.isEmpty()) autoSlip = nvl(convertMapToLowerCase(resSet.getFirst()).get("slipyn"), "N");
        String acctymd = "Y".equals(autoSlip) ? slipymd : "";

        for (Map<String, Object> item : items) {
            String imym = nvl(item.get("imym"));
            String imno = nvl(item.get("imno"));
            String itemDeptcd = nvl(item.get("deptcd"), deptcd);
            String remark = String.format("입금번호:%s-%s 입금전표 건", imym, imno);

            Map<String, Object> params = new HashMap<>();
            params.put("cmpycd", cmpycd);
            params.put("deptcd", itemDeptcd);
            params.put("fromdt", nvl(mst.get("fromdt")).replace("-", ""));
            params.put("todt", nvl(mst.get("todt")).replace("-", ""));
            params.put("userid", userId);
            params.put("iogbn", "200");
            params.put("imym", imym);
            params.put("imno", imno);
            params.put("slipkind", "040");
            params.put("slipymd", slipymd);
            params.put("imamt", nvl(item.get("imamt"), "0").replace(",", ""));
            params.put("remark", remark);
            params.put("custcd", nvl(item.get("custcd")));
            params.put("custnm", nvl(item.get("custnm")));
            params.put("updemp", userId);

            params.put("actkind", "A");
            params.put("slipno", "");
            List<Map<String, Object>> resA = hsioMapper.HSIO_325U_STR(params);
            if (resA == null || resA.isEmpty()) throw new Exception("전표 번호 채번 실패");
            Map<String, Object> rowA = convertMapToLowerCase(resA.getFirst());
            String slipno = nvl(rowA.get("slipno"));

            params.put("actkind", "U");
            params.put("slipno", slipno);
            List<Map<String, Object>> resU = hsioMapper.HSIO_325U_STR(params);
            if (resU != null && !resU.isEmpty()) {
                Map<String, Object> rowU = convertMapToLowerCase(resU.getFirst());
                if ("Y".equals(nvl(rowU.get("erryn")))) {
                    throw new Exception("정산 저장 실패: " + nvl(rowU.get("msg"), "업무 오류"));
                }
            }

            if ("Y".equals(autoSlip)) {
                Map<String, Object> cParams = createParamMap();
                safePut(cParams, "actkind", "A0");
                safePut(cParams, "cmpycd", cmpycd);
                safePut(cParams, "slipymd", slipymd);
                safePut(cParams, "acctymd", slipymd);
                safePut(cParams, "slipno", slipno);
                safePut(cParams, "deptcd", itemDeptcd);
                safePut(cParams, "slipkind", "040");
                safePut(cParams, "slipyn", "N");
                safePut(cParams, "cofmyn", "Y");
                safePut(cParams, "cofmemp", usernm);
                safePut(cParams, "updemp", userId);
                haslMapper.HASL_020U_STR(cParams);
            }
        }
        return Map.of("res", "OK");
    }

    @Transactional(value = "erpTransactionManager", rollbackFor = Exception.class)
    public Map<String, Object> cancelSlips325(Hsio325uCancelRequest request, String userId) throws Exception {
        String cmpycd = request.getCmpycd();
        String autoSlip = "N";
        List<Map<String, Object>> resSet = ha00Mapper.HA00_010S_STR(Map.of("cmpycd", cmpycd, "gubun", "P1"));
        if (resSet != null && !resSet.isEmpty()) autoSlip = nvl(convertMapToLowerCase(resSet.getFirst()).get("slipyn"), "N");

        for (Map<String, Object> item : request.getItems()) {
            String slipymd = nvl(item.get("slipymd")).replace("-", "");
            String slipno = nvl(item.get("slipno"));
            String udeptcd = nvl(item.get("deptcd"));

            if ("Y".equals(autoSlip)) {
                Map<String, Object> haslParams = createParamMap();
                safePut(haslParams, "actkind", "A0");
                safePut(haslParams, "cmpycd", cmpycd);
                safePut(haslParams, "slipymd", slipymd);
                safePut(haslParams, "acctymd", slipymd);
                safePut(haslParams, "slipno", slipno);
                safePut(haslParams, "deptcd", udeptcd);
                safePut(haslParams, "slipkind", "040");
                safePut(haslParams, "slipyn", "Y");
                safePut(haslParams, "cofmyn", "N");
                safePut(haslParams, "updemp", userId);
                haslMapper.HASL_020U_STR(haslParams);
            }

            Map<String, Object> hsioParams = new HashMap<>();
            hsioParams.put("actkind", "D");
            hsioParams.put("cmpycd", cmpycd);
            hsioParams.put("deptcd", udeptcd);
            hsioParams.put("imym", nvl(item.get("imym")));
            hsioParams.put("imno", nvl(item.get("imno")));
            hsioParams.put("slipymd", slipymd);
            hsioParams.put("slipno", slipno);
            hsioParams.put("userid", userId);

            List<Map<String, Object>> res = hsioMapper.HSIO_325U_STR(hsioParams);
            if (res != null && !res.isEmpty()) {
                Map<String, Object> row = convertMapToLowerCase(res.getFirst());
                if ("Y".equals(nvl(row.get("erryn")))) {
                    throw new Exception("전표[" + slipno + "] 취소 실패: " + nvl(row.get("msg")));
                }
            }
        }
        return Map.of("res", "OK");
    }

    private void safePut(Map<String, Object> map, String key, Object value) {
        map.put(key, value == null ? "" : String.valueOf(value).trim());
    }

    private String nvl(Object val) {
        return val == null ? "" : String.valueOf(val).trim();
    }

    private String nvl(Object val, String def) {
        String s = nvl(val);
        return s.isEmpty() ? def : s;
    }

    private Map<String, Object> createParamMap() {
        Map<String, Object> map = new HashMap<>();
        String[] keys = { "actkind", "cmpycd", "slipymd", "slipno", "srowno", "acctymd", "acctcd", "deptcd", "custcd", "bugtcd", "prjcd", "mgtno", "sslipno", "dbamt", "cramt", "remark", "paycndt", "payymd", "docno1", "docno2", "docno3", "docno4", "docno5", "docno6", "docno7", "docno8", "docno9", "updemp", "frgnkind", "frgnrate", "frgnamt", "ret_yn", "taxunit", "custcd2", "taxtype", "slipymd2", "supyamt", "vatamt" };
        for (String k : keys) map.put(k, "");
        return map;
    }
}
