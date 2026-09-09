package com.crmbank.erp.crm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.crmbank.erp.crm.handler.CtiWebSocketHandler;
import com.crmbank.erp.crm.mapper.inbound.InboundMapper;
import org.asteriskjava.manager.ManagerConnection;
import org.asteriskjava.manager.ManagerEventListener;
import org.asteriskjava.manager.action.*;
import org.asteriskjava.manager.event.*;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsteriskService implements ManagerEventListener {

    private final ManagerConnection managerConnection;
    private final InboundMapper inboundMapper; 
    private final CtiWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 💡 [소문자 표준화] 멤버 변수명 소문자 변경
    private final Map<String, String> exten_channel_map = new ConcurrentHashMap<>(); 
    private final Map<String, String> exten_caller_channel = new ConcurrentHashMap<>(); 
    private final Map<String, String> exten_linked_id = new ConcurrentHashMap<>(); 
    private final Map<String, Set<String>> session_channels = new ConcurrentHashMap<>(); 
    private final Set<String> answering_lock = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<String> registered_extens = Collections.newSetFromMap(new ConcurrentHashMap<>());

    // 💡 Getter 메서드 표준 카멜케이스 적용
    public Map<String, String> getExtenChannelMap() { return exten_channel_map; }
    public Map<String, String> getExtenLinkedId() { return exten_linked_id; }
    public Map<String, Set<String>> getSessionChannels() { return session_channels; }
    public List<String> getRegisteredExtens() { return new ArrayList<>(registered_extens); }

    @Override
    public void onManagerEvent(ManagerEvent event) {
        // 💡 모든 중요 이벤트를 실시간으로 INFO 로그에 남겨 추적성을 확보합니다.
        if (!(event instanceof org.asteriskjava.manager.event.RtcpReceivedEvent)) {
             log.info("📡 [AMI EVENT] Type: {}", event.getClass().getSimpleName());
        }

        if (event instanceof NewChannelEvent) {
            handleNewChannel((NewChannelEvent) event);
        } else if (event instanceof AgentCalledEvent) {
            // [CASE 1] 대기열 수신 시 팝업 (표준)
            handleAgentCalled((AgentCalledEvent) event);
        } else if (event instanceof NewStateEvent) {
            // [CASE 2] 직통 전화 벨울림 시 팝업 (백업)
            handleNewState((NewStateEvent) event);
        } else if (event instanceof BridgeEnterEvent) {
            handleBridgeEnter((BridgeEnterEvent) event);
        } else if (event instanceof HangupEvent) {
            handleHangup((HangupEvent) event);
        } else if (event instanceof ContactStatusEvent) {
            handleContactStatus((ContactStatusEvent) event);
        }
    }

    private void handleContactStatus(ContactStatusEvent e) {
        String exten = e.getEndpointName();
        if (exten == null) exten = extractNumberOnly(e.getAor());

        if (isValidExt(exten)) {
            String status = e.getContactStatus();
            if ("Reachable".equalsIgnoreCase(status) || "Registered".equalsIgnoreCase(status)) {
                log.info("🟢 [PJSIP ONLINE] 내선번호: {} (Status: {})", exten, status);
                registered_extens.add(exten);
            } else if ("Unreachable".equalsIgnoreCase(status) || "Removed".equalsIgnoreCase(status) || "Unknown".equalsIgnoreCase(status)) {
                log.info("🔴 [PJSIP OFFLINE] 내선번호: {} (Status: {})", exten, status);
                registered_extens.remove(exten);
            }
        }
    }

    private void handleNewChannel(NewChannelEvent event) {
        String channel = event.getChannel();
        String uniqueId = event.getUniqueId();
        String ext = extractNumberOnly(channel);
        if (isValidExt(ext)) exten_channel_map.put(ext, channel);
        session_channels.computeIfAbsent(uniqueId, k -> Collections.synchronizedSet(new HashSet<>())).add(channel);
    }

    private void handleNewState(NewStateEvent e) {
        // 💡 [수정] 발신번호 추출 로직 강화 (강제 세팅된 번호 대응)
        String state = e.getChannelStateDesc();
        String channel = e.getChannel();
        String ext = extractNumberOnly(channel);
        String callerId = e.getCallerIdNum(); // 01032043901 예상

        if ("Ringing".equals(state) && isValidExt(ext)) {
            // 발신자 본인 제외 필터링
            if (callerId != null && !channel.contains(callerId)) {
                log.info("📢 [CTI 팝업] 수신: {}, 발신: {}, 상태: {}", ext, callerId, state);
                sendInboundPopup(ext, callerId, channel, e.getUniqueId());
            }
        }
    }

    private void handleAgentCalled(AgentCalledEvent e) {
        String agentExt = extractNumberOnly(e.getInterface());
        if (isValidExt(agentExt)) {
            exten_caller_channel.put(agentExt, e.getChannel());
            exten_linked_id.put(agentExt, e.getLinkedId());
            if (e.getDestinationChannel() != null) exten_channel_map.put(agentExt, e.getDestinationChannel());
            
            // 💡 [수정] 벨이 울리는 즉시 팝업 신호(INBOUND_CALL) 발송
            log.info("📢 [CTI 팝업 신호 전송] 내선: {}, 고객: {}", agentExt, e.getCallerIdNum());
            sendInboundPopup(agentExt, e.getCallerIdNum(), e.getChannel(), e.getLinkedId());
        }
    }

    private void handleBridgeEnter(BridgeEnterEvent e) {
        String exten = extractNumberOnly(e.getChannel());
        if (isValidExt(exten)) {
            answering_lock.remove(exten);
            sendCtiEvent(exten, "CALL_CONNECTED", e.getChannel(), null);
        }
    }

    private void handleHangup(HangupEvent e) {
        String exten = extractNumberOnly(e.getChannel());
        if (isValidExt(exten)) {
            if (answering_lock.contains(exten)) return;
            // 💡 [핵심] 종료 시 녹취 파일명(LinkedID)을 반드시 전송하여 상담원이 볼 수 있게 함
            String recFile = exten_linked_id.get(exten);
            sendCtiEvent(exten, "CALL_HANGUP", e.getChannel(), recFile);
            
            exten_channel_map.remove(exten);
            exten_caller_channel.remove(exten);
            exten_linked_id.remove(exten);
        }
    }

    public void answerCall(String exten) {
        String callerChannel = exten_caller_channel.get(exten);
        String agentChannel = exten_channel_map.get(exten);
        if (callerChannel == null) return;

        answering_lock.add(exten);
        sendCtiEvent(exten, "STOP_RINGTONE", null, null);

        new Thread(() -> {
            try {
                if (agentChannel != null && !agentChannel.equals("UNKNOWN")) {
                    managerConnection.sendAction(new HangupAction(agentChannel));
                    Thread.sleep(600);
                }
                managerConnection.sendAction(new SetVarAction(callerChannel, "AGENT_EXTEN", exten));
                managerConnection.sendAction(new RedirectAction(callerChannel, "cti-answer-force", "s", 1));
                Thread.sleep(4000);
                answering_lock.remove(exten);
            } catch (Exception ex) { answering_lock.remove(exten); }
        }).start();
    }

    public void hangupCall(String exten, String ch) {
        try {
            if (ch != null) managerConnection.sendAction(new HangupAction(ch));
            String myCh = exten_channel_map.get(exten);
            if (myCh != null) managerConnection.sendAction(new HangupAction(myCh));
            String callerCh = exten_caller_channel.get(exten);
            if (callerCh != null) managerConnection.sendAction(new HangupAction(callerCh));
        } catch (Exception ex) {}
    }

    public void transferCall(String exten, String target) {
        String myChannel = exten_channel_map.get(exten);
        if (myChannel != null) {
            try { managerConnection.sendAction(new RedirectAction(myChannel, "from-internal", target, 1)); } catch (Exception e) {}
        }
    }

    public boolean checkAmiConnection() {
        return managerConnection != null && managerConnection.getState() == org.asteriskjava.manager.ManagerConnectionState.CONNECTED;
    }

    private void sendInboundPopup(String exten, String callerId, String channel, String linkedId) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("type", "INBOUND_CALL");
            data.put("callerid", callerId);
            data.put("exten", exten);
            data.put("linkedid", linkedId);

            // 💡 [수정] 하드코딩 제거: WebSocket 세션에서 상담원의 회사코드를 실시간으로 획득
            String cmpycd = webSocketHandler.getCmpyCd(exten);
            data.put("cmpycd", cmpycd); 
            
            Map<String, Object> params = new HashMap<>();
            params.put("cmpycd", cmpycd); // 🚀 획득한 세션 회사코드 주입
            params.put("phone", callerId);
            
            // 기존 매퍼를 활용하여 고객 정보 조회
            Map<String, Object> customer = inboundMapper.findCustomerByPhoneMap(params);
            
            if (customer != null) {
                log.info("🎯 [CTI] 팝업 전송 (회사: {}, 고객: {})", cmpycd, customer.get("custnm"));
                customer.forEach((k, v) -> data.put(k.toLowerCase(), v));
            } else {
                data.put("custnm", "미등록 고객");
            }

            webSocketHandler.sendMessage(exten, objectMapper.writeValueAsString(data));
        } catch (Exception e) {
            log.error("❌ [CTI] 팝업 처리 중 오류: {}", e.getMessage());
        }
    }

    private void sendCtiEvent(String exten, String type, String channel, String recFile) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("type", type); data.put("exten", exten);
            if (channel != null) data.put("channel", channel);
            if (recFile != null) data.put("recordingFile", recFile + ".wav");
            webSocketHandler.sendMessage(exten, objectMapper.writeValueAsString(data));
        } catch (Exception e) {}
    }

    private boolean isValidExt(String s) { return s != null && s.matches("^\\d{3,4}$"); }
    private String extractNumberOnly(String s) {
        if (s == null) return null;
        Matcher m = Pattern.compile("(\\d{3,4})").matcher(s);
        return m.find() ? m.group(1) : null;
    }

    @PostConstruct
    public void init() {
        new Thread(() -> {
            while (true) {
                try {
                    if (managerConnection.getState() != org.asteriskjava.manager.ManagerConnectionState.CONNECTED) {
                        log.info("🔐 [AMI] Asterisk 연결 시도 중...");
                        managerConnection.addEventListener(this);
                        managerConnection.login();
                        log.info("✅ [AMI] Asterisk 연결 및 로그인 성공!");
                    }
                } catch (Exception e) {
                    log.error("❌ [AMI] 연결 실패, 5초 후 재시도: {}", e.getMessage());
                }
                try { Thread.sleep(5000); } catch (InterruptedException e) { break; }
            }
        }).start();
    }
    @PreDestroy public void cleanup() { try { if (managerConnection != null) managerConnection.logoff(); } catch (Exception ex) {} }
}
