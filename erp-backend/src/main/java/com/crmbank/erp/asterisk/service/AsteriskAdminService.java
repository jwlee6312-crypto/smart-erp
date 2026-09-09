package com.crmbank.erp.asterisk.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.crmbank.erp.asterisk.mapper.AsteriskMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsteriskAdminService {

    private final AsteriskMapper asteriskMapper;

    // 1. PJSIP 내선번호 저장
    @Transactional
    public void savePjsip(List<Map<String, Object>> list) {
        for (Map<String, Object> data : list) {
            asteriskMapper.insertPjsipAuth(data);
            asteriskMapper.insertPjsipAor(data);
            asteriskMapper.insertPjsipEndpoint(data);
        }
    }

    // 2. 수신그룹(Queue) 저장
    @Transactional
    public void saveQueues(List<Map<String, Object>> list) {
        for (Map<String, Object> data : list) {
            asteriskMapper.upsertQueue(data);
        }
    }

    // 3. 수신그룹 멤버 저장
    @Transactional
    public void saveQueueMembers(String queueName, List<Map<String, Object>> members) {
        asteriskMapper.deleteQueueMembers(queueName);
        for (Map<String, Object> member : members) {
            member.put("queue_name", queueName);
            asteriskMapper.insertQueueMember(member);
        }
    }

    // 4. 다이얼플랜 저장
    @Transactional
    public void saveExtensions(List<Map<String, Object>> list) {
        for (Map<String, Object> data : list) {
            asteriskMapper.deleteExtensions(data);
            asteriskMapper.insertExtension(data);
        }
    }

    // 5. ARS 스크립트 저장 및 TTS 생성
    @Transactional
    public void saveArsScripts(List<Map<String, Object>> list, String userId) {
        for (Map<String, Object> data : list) {
            data.put("upd_user", userId);
            asteriskMapper.upsertArsScript(data);
            
            // 💡 실시간 TTS 생성 트리거 (성공 시 파일이 /var/lib/asterisk/sounds/custom/에 생성됨)
            String scriptId = String.valueOf(data.get("id"));
            String text = String.valueOf(data.get("script_text"));
            generateTtsFile(scriptId, text);
        }
    }

    private void generateTtsFile(String scriptId, String text) {
        /*
         * [서버 환경별 TTS 생성 스크립트 실행 지침]
         * 1. WINDOWS: wsl 명령어를 통해 프로젝트 내 스크립트 실행
         * 2. UNIX/LINUX: 시스템 python3를 직접 호출하여 실행
         * 💡 향후 스크립트 경로를 찾지 못하는 문제가 발생하면 linuxPath 변수의 
         *    절대 경로를 서버 환경에 맞게 직접 수정하거나 별도 프로퍼티로 관리하세요.
         */
        try {
            String[] command;
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                // ============================================================
                // [CASE 1] WINDOWS (로컬 테스트 - WSL 연동)
                // ============================================================
                command = new String[]{"wsl", "python3", "/mnt/d/erp.crmbank.co.kr/scripts/generate_tts.py", scriptId, text};
            } else {
                // ============================================================
                // [CASE 2] UNIX / LINUX (실제 운영 서버 - Docker 컨테이너 내부 경로)
                // ============================================================
                String linuxPath = "/app/scripts/generate_tts.py";
                command = new String[]{"python3", linuxPath, scriptId, text};
            }

            log.info("🎙️ TTS 생성 시도: {} {}", scriptId, text);
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.inheritIO(); // 로그 확인을 위해 출력 스트림 연결
            pb.start();
        } catch (Exception e) {
            log.error("❌ TTS 생성 실패: {}", e.getMessage());
        }
    }

    @Value("${backend.internal.url:http://erp-backend:8080}")
    private String backendInternalUrl;

    /**
     * 💡 [테스트 최적화] 3자리 내선번호(101, 103) 환경을 위한 표준 IVR 시나리오
     * 내선 간 통화, IVR 진입(700), 콜백(4) 연동을 포함합니다.
     */
    public List<Map<String, Object>> getStandardIvrTemplate() {
        List<Map<String, Object>> template = new ArrayList<>();

        // [0] from-internal: 103번 내선이 999를 눌렀을 때의 진입점
        addExten(template, "from-internal", "999", 1, "NoOp", "### [HQ] Head Office IVR Entry ###");
        addExten(template, "from-internal", "999", 2, "Answer", "");
        addExten(template, "from-internal", "999", 3, "Goto", "ivr-main,s,1");
        
        // 🚀 기존 내선번호(_1XX) 지능형 라우팅 유지
        addExten(template, "from-internal", "_1XX", 1, "Set", String.format("ROUTE_RES=${CURL(%s/api/crm/inbound/asterisk/check-routing?exten=${EXTEN}&cmpycd=COIT)}", backendInternalUrl));
        addExten(template, "from-internal", "_1XX", 2, "NoOp", "### Routing Result: ${ROUTE_RES} ###");
        addExten(template, "from-internal", "_1XX", 3, "GotoIf", "$[\"${CUT(ROUTE_RES,:,1)}\" = \"BLOCK\"]?invalid-agent,s,1");
        addExten(template, "from-internal", "_1XX", 4, "GotoIf", "$[\"${CUT(ROUTE_RES,:,1)}\" = \"MOBILE_DIRECT\"]?fwd-mobile,s,1");
        addExten(template, "from-internal", "_1XX", 5, "GotoIf", "$[\"${CUT(ROUTE_RES,:,1)}\" = \"OFFICE_FIRST\"]?office-hybrid,s,1");
        addExten(template, "from-internal", "_1XX", 6, "Dial", "PJSIP/${EXTEN},20,tT"); 
        addExten(template, "from-internal", "_1XX", 7, "Hangup", "");

        // [1] ivr-main: 본사 안내 및 부서 선택
        addExten(template, "ivr-main", "s", 1, "Answer", "");
        addExten(template, "ivr-main", "s", 2, "Background", "custom/hq_welcome"); // "본사입니다. 영업은 1번, 기술은 2번..."
        addExten(template, "ivr-main", "s", 3, "WaitExten", "5");

        // 1번: 영업팀 대기열(Queue) 연결 -> 이 시점에 CTI 팝업 발생
        addExten(template, "ivr-main", "1", 1, "NoOp", "### Connecting to Sales Queue ###");
        addExten(template, "ivr-main", "1", 2, "Queue", "sales_group,tT");
        addExten(template, "ivr-main", "1", 3, "Hangup", "");

        // 2번: 기술지원 대기열 연결
        addExten(template, "ivr-main", "2", 1, "Queue", "support_group,tT");
        addExten(template, "ivr-main", "2", 2, "Hangup", "");

        // [기타 컨텍스트 생략... 기존 로직 유지]

        // 2번/3번: 부서별 대기열(Queue) 연결
        addExten(template, "ivr-main", "2", 1, "Queue", "tech_queue");
        addExten(template, "ivr-main", "3", 1, "Queue", "admin_queue");

        // 4번: 콜백 서비스 (윈도우 백엔드 연동)
        addExten(template, "ivr-main", "4", 1, "NoOp", "### [TEST] Callback Requested ###");
        addExten(template, "ivr-main", "4", 2, "Playback", "custom/05_callback_confirm");
        // 💡 backendInternalUrl: Docker 내부 네트워크 혹은 로컬 호스트 접근용
        addExten(template, "ivr-main", "4", 3, "System", String.format("curl -X POST %s/api/crm/inbound/log-callback -H \"Content-Type: application/json\" -d '{\"interaction_id\":\"CB_$${UNIQUEID}\", \"keyword\":\"$${CALLERID(num)}\", \"media_type\":\"callback\", \"cmpycd\":\"HAIONNET\"}'", backendInternalUrl));
        addExten(template, "ivr-main", "4", 4, "Playback", "custom/06_thank_you_bye");
        addExten(template, "ivr-main", "4", 5, "Hangup", "");

        // 5번: 정보 안내
        addExten(template, "ivr-main", "5", 1, "Playback", "custom/07_company_info");
        addExten(template, "ivr-main", "5", 2, "Goto", "ivr-main,s,1");

        // 내선번호 직접 연결 (3자리: 101, 103 등)
        addExten(template, "ivr-main", "_1XX", 1, "NoOp", "### [TEST] IVR Direct Dial to ${EXTEN} ###");
        addExten(template, "ivr-main", "_1XX", 2, "Dial", "PJSIP/${EXTEN},20,tT");
        addExten(template, "ivr-main", "_1XX", 3, "Playback", "vm-nobodyavail");
        addExten(template, "ivr-main", "_1XX", 4, "Hangup", "");

        // 예외 처리
        addExten(template, "ivr-main", "i", 1, "Playback", "pbx-invalid");
        addExten(template, "ivr-main", "i", 2, "Goto", "ivr-main,s,1");
        addExten(template, "ivr-main", "t", 1, "Goto", "ivr-main,s,1");

        // [3] busy-handling: 상담원 통화 중 안내
        addExten(template, "busy-handling", "s", 1, "Playback", "custom/04_calling_busy");
        addExten(template, "busy-handling", "s", 2, "Wait", "1");
        addExten(template, "busy-handling", "s", 3, "Goto", "ivr-main,s,1");

        // [4] after-hours: 야간 및 휴일
        addExten(template, "after-hours", "s", 1, "Playback", "custom/08_night_greeting");
        addExten(template, "after-hours", "9", 1, "Playback", "custom/09_transfer_to_emergency");
        addExten(template, "after-hours", "9", 2, "Dial", "PJSIP/01032043901@PJSIP/my-trunk,30");

        return template;
    }

    private void addExten(List<Map<String, Object>> list, String ctx, String ext, int pri, String app, String arg) {
        Map<String, Object> row = new HashMap<>();
        row.put("context", ctx);
        row.put("exten", ext);
        row.put("priority", pri);
        row.put("app", app);
        row.put("appdata", arg);
        list.add(row);
    }
}
