package com.crmbank.erp.crm.controller;

import jakarta.servlet.http.HttpSession;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.crmbank.erp.crm.dto.CallMstDto;
import com.crmbank.erp.crm.dto.TotalCallLogDto;
import com.crmbank.erp.comm.dto.UserSession;
import com.crmbank.erp.crm.service.InboundService;
import com.crmbank.erp.crm.service.GeminiAiService;
import com.crmbank.erp.crm.mapper.inbound.InboundMapper;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.stream.Collectors;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/crm/inbound")
@RequiredArgsConstructor
public class InboundController {

    private final InboundService inboundService;
    private final GeminiAiService geminiAiService;
    private final InboundMapper inboundMapper;

    /**
     * 📞 ARS 콜백 요청 로그 기록
     */
    /**
     * 💡 Asterisk 지능형 하이브리드 라우팅 상태 체크 API
     * 리턴값 형식: [명령]:[값1]:[값2]
     */
    @GetMapping("/asterisk/check-routing")
    public ResponseEntity<String> checkRouting(@RequestParam String exten, @RequestParam(required = false) String cmpycd) {
        String finalCmpycd = (cmpycd == null || cmpycd.isEmpty()) ? "COIT" : cmpycd;
        String today = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd").format(java.time.LocalDate.now());

        // 1. 휴일 체크 (PD_HOLIDAY 연동)
        Map<String, Object> hParam = new HashMap<>();
        hParam.put("cmpycd", finalCmpycd);
        hParam.put("yymmdd", today);
        if (inboundMapper.checkHoliday(hParam) != null) {
            return getDutyRedirect(finalCmpycd, "HOLIDAY");
        }

        // 2. 업무 시간 체크 (평일 09:00 - 18:00)
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.DayOfWeek dayOfWeek = now.getDayOfWeek();
        if (dayOfWeek == java.time.DayOfWeek.SATURDAY || dayOfWeek == java.time.DayOfWeek.SUNDAY) {
            return getDutyRedirect(finalCmpycd, "WEEKEND");
        }
        
        java.time.LocalTime time = now.toLocalTime();
        if (time.isBefore(java.time.LocalTime.of(9, 0)) || time.isAfter(java.time.LocalTime.of(18, 0))) {
            return getDutyRedirect(finalCmpycd, "OFF_HOURS");
        }

        // 3. 상담원 마스터 상태 체크 (haba920t_tbl 연동)
        Map<String, Object> aParam = new HashMap<>();
        aParam.put("cmpycd", finalCmpycd);
        aParam.put("exten", exten);
        Map<String, Object> agent = inboundMapper.checkAgentStatus(aParam);

        if (agent == null || "N".equals(agent.get("useyn"))) {
            return ResponseEntity.ok("BLOCK:INVALID_AGENT");
        }

        String status = String.valueOf(agent.get("status")); // 10:정상, 20:외출, 30:휴가, 40:퇴근
        String routingMode = String.valueOf(agent.get("routing_mode")); // 10:앱, 20:하이브리드, 30:휴대폰
        String mobileNo = String.valueOf(agent.get("mobile_no"));

        // 4. 세부 상태별 분기
        if ("30".equals(status)) return ResponseEntity.ok("BLOCK:VACATION");
        if ("20".equals(status) || "30".equals(routingMode)) {
            return ResponseEntity.ok("MOBILE_DIRECT:" + mobileNo);
        }
        if ("40".equals(status)) return getDutyRedirect(finalCmpycd, "AGENT_OFF");

        // 5. 기본: 사무실 우선 (MicroSIP 호출 후 미응답 시 휴대폰 백업)
        return ResponseEntity.ok("OFFICE_FIRST:" + exten + ":" + mobileNo);
    }

    private ResponseEntity<String> getDutyRedirect(String cmpycd, String reason) {
        Map<String, Object> dParam = new HashMap<>();
        dParam.put("cmpycd", cmpycd);
        Map<String, Object> duty = inboundMapper.getDutyAgent(dParam);
        if (duty != null) {
            return ResponseEntity.ok("DUTY_DIRECT:" + duty.get("mobile_no") + ":" + reason);
        }
        return ResponseEntity.ok("BLOCK:" + reason);
    }

    @PostMapping("/log-callback")
    public ResponseEntity<Map<String, Object>> logCallback(@RequestBody Map<String, Object> params) {
        try {
            log.info("📞 [ARS CALLBACK] 요청 수신: {}", params);

            TotalCallLogDto logDto = TotalCallLogDto.builder()
                    .uniqueid(String.valueOf(params.get("interaction_id")))
                    .keyword(String.valueOf(params.get("keyword")))
                    .media_type(String.valueOf(params.get("media_type")))
                    .cmpycd(String.valueOf(params.get("cmpycd")))
                    .direction("in")
                    .start_time(LocalDateTime.now())
                    .callback_yn("Y")
                    .callback_no(String.valueOf(params.get("keyword")))
                    .callback_req_time(LocalDateTime.now())
                    .callback_retry_cnt(0)
                    .build();

            inboundService.insertTotalInteractionLog(logDto);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.error("❌ 콜백 로그 저장 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 💡 미결/콜백 리스트 조회 (HGOA200U)
     */
    @GetMapping("/pending-list")
    public List<Map<String, Object>> getPendingList(HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("user_session");
        String cmpycd = user != null ? user.getCmpycd() : "";
        Map<String, Object> params = new HashMap<>();
        params.put("cmpycd", cmpycd);
        // 💡 [혁신] 콜백 전용 리스트와 미결 상담 리스트를 통합하여 제공
        return toLowerCase(inboundMapper.selectCallbackList(params));
    }

    /**
     * 💡 콜백 통합 관리 리스트 조회 (HGOA110U)
     */
    @GetMapping("/callback-list")
    public ResponseEntity<?> getCallbackList(@RequestParam Map<String, Object> params, HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user == null) return ResponseEntity.status(401).build();

        params.put("cmpycd", user.getCmpycd());
        return ResponseEntity.ok(toLowerCase(inboundMapper.selectCallbackList(params)));
    }

    /**
     * 💡 콜백 응대 결과 저장 (HGOA110U / MHGOA110U)
     */
    @PostMapping("/interaction/save-response")
    public ResponseEntity<Map<String, Object>> saveCallbackResponse(@RequestBody Map<String, Object> params, HttpSession session) {
        try {
            UserSession user = (UserSession) session.getAttribute("user_session");
            String userid = user != null ? user.getUserid() : "system";

            // TOTAL_INTERACTION_LOG 업데이트
            Map<String, Object> updateParam = new HashMap<>();
            updateParam.put("uniqueid", params.get("INTERACTION_ID"));
            updateParam.put("result_cd", params.get("rslt_cd"));
            updateParam.put("call_memo", params.get("remark"));
            updateParam.put("callback_agent_id", userid);
            updateParam.put("status", "300"); // 완료 상태

            inboundMapper.updateCallbackResult(updateParam);

            return ResponseEntity.ok(Map.of("success", true, "message", "처리가 완료되었습니다."));
        } catch (Exception e) {
            log.error("콜백 결과 저장 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    private List<Map<String, Object>> toLowerCase(List<Map<String, Object>> list) {
        if (list == null) return new java.util.ArrayList<>();
        return list.stream()
            .filter(Objects::nonNull)
            .map(map -> {
                Map<String, Object> lowerMap = new HashMap<>();
                map.forEach((k, v) -> lowerMap.put(k != null ? k.toLowerCase() : null, v));
                return lowerMap;
            }).collect(Collectors.toList());
    }

    /**
     * 💡 상담 통합 저장 및 자동 STT/요약
     */
    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> save(@RequestBody SaveRequest request, HttpSession session) {
        try {
            // 💡 [소문자 표준화] session key: "user_session"
            UserSession user = (UserSession) session.getAttribute("user_session");
            String cmpycd = user != null ? user.getCmpycd() : "";
            String userid = user != null ? user.getUserid() : "system";
            String deptcd = user != null ? user.getDeptcd() : "";

            CallMstDto dto = request.getDto();
            dto.setCmpycd(cmpycd);
            dto.setConsultid(userid);
            dto.setUpdemp(userid);
            dto.setDeptcd(deptcd);
            dto.setHappycall_yn("N");

            if (dto.getInteraction_id() == null || dto.getInteraction_id().isEmpty()) {
                dto.setInteraction_id("IN_" + UUID.randomUUID().toString().substring(0, 8));
            }

            dto.setEnd_time(LocalDateTime.now());

            if (request.getRecordings() != null && !request.getRecordings().isEmpty()) {
                String lastFile = request.getRecordings().get(request.getRecordings().size() - 1);
                String fullPath = "/var/spool/asterisk/monitor/" + lastFile;
                Map<String, String> aiResult = geminiAiService.analyzeAudio(fullPath);
                dto.setAns_ment(aiResult.get("stt"));
                dto.setAi_summary(aiResult.get("summary"));
                dto.setRec_file(lastFile);
            }

            String svcymd = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String svcno = inboundService.saveCallMst(dto, request.getRecordings(), svcymd, deptcd);

            return ResponseEntity.ok(Map.of("success", true, "svcno", svcno));
        } catch (Exception e) {
            log.error("상담 저장 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/ai-summarize")
    public Map<String, String> aiSummarize(@RequestBody Map<String, String> params) {
        String trbMent = params.get("trb_ment");
        String ansMent = params.get("ans_ment");
        String chatLog = "문의내용: " + trbMent + "\n답변내용: " + ansMent;
        String summary = geminiAiService.summarizeText(chatLog);

        Map<String, String> result = new HashMap<>();
        result.put("summary", summary);
        result.put("deptcd", "");
        return result;
    }

    @GetMapping("/play-recording")
    public ResponseEntity<Resource> playRecording(@RequestParam String file) {
        // 🚀 Windows 호스트에서 WSL(Ubuntu) 파일 시스템에 접근하기 위한 정규화된 경로 사용
        String wslBase = "\\\\wsl.localhost\\Ubuntu";
        
        // 슬래시 방향을 윈도우 스타일로 통일하여 경로 충돌 방지
        String safeFile = file.replace("/", "\\");
        if (safeFile.startsWith("\\")) safeFile = safeFile.substring(1);
        
        // 1. 기본 녹취 경로 (MixMonitor)
        File monitorFile = new File(wslBase + "\\var\\spool\\asterisk\\monitor\\" + safeFile);
        // 2. ARS 음원 및 콜백 메세지 경로
        File soundFile = new File(wslBase + "\\var\\lib\\asterisk\\sounds\\" + safeFile);

        File targetFile = monitorFile.exists() ? monitorFile : (soundFile.exists() ? soundFile : null);

        if (targetFile == null) {
            log.warn("🔈 [재생 실패] 파일을 찾을 수 없음: {} (검색: {}, {})", 
                     file, monitorFile.getAbsolutePath(), soundFile.getAbsolutePath());
            return ResponseEntity.notFound().build();
        }

        log.info("🔈 [음원 재생] 파일 발견: {}", targetFile.getAbsolutePath());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/wav"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + targetFile.getName() + "\"")
                .body(new FileSystemResource(targetFile));
    }

    @GetMapping("/status-list")
    public List<Map<String, Object>> getStatusList(
            @RequestParam String fromdt,
            @RequestParam String todt,
            @RequestParam(required = false) String custnm,
            HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("user_session");
        String cmpycd = user != null ? user.getCmpycd() : "";
        return inboundService.getStatusList(cmpycd, fromdt, todt, custnm);
    }

    @GetMapping("/customer-detail")
    public Map<String, Object> getCustomerDetail(@RequestParam String custcd, HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("user_session");
        String cmpycd = user != null ? user.getCmpycd() : "";
        return inboundService.getCustomerByCustCd(cmpycd, custcd);
    }

    @GetMapping("/item-list")
    public List<Map<String, Object>> getItemList(@RequestParam String custcd, HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("user_session");
        String cmpycd = user != null ? user.getCmpycd() : "";
        return inboundService.getItemList(cmpycd, custcd);
    }

    @GetMapping("/call-history")
    public List<Map<String, Object>> getCallHistory(@RequestParam String custcd, HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("user_session");
        String cmpycd = user != null ? user.getCmpycd() : "";
        return inboundService.getCallHistory(cmpycd, custcd);
    }

    @GetMapping("/service-history")
    public List<Map<String, Object>> getServiceHistory(@RequestParam String custcd, HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("user_session");
        String cmpycd = user != null ? user.getCmpycd() : "";
        return inboundService.getServiceHistory(cmpycd, custcd);
    }

    @GetMapping("/settle-history")
    public List<Map<String, Object>> getSettleHistory(@RequestParam String custcd, HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("user_session");
        String cmpycd = user != null ? user.getCmpycd() : "";
        return inboundService.getSettleHistory(cmpycd, custcd);
    }

    @Data public static class SaveRequest { private CallMstDto dto; private List<String> recordings; }
}