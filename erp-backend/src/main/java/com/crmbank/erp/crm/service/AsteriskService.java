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

    // 💡 Getter 메서드 표준 카멜케이스 적용
    public Map<String, String> getExtenChannelMap() { return exten_channel_map; }
    public Map<String, String> getExtenLinkedId() { return exten_linked_id; }
    public Map<String, Set<String>> getSessionChannels() { return session_channels; }

    @Override
    public void onManagerEvent(ManagerEvent event) {
        if (event instanceof NewChannelEvent) {
            String channel = ((NewChannelEvent) event).getChannel();
            String uniqueId = ((NewChannelEvent) event).getUniqueId();
            String ext = extractNumberOnly(channel);
            if (isValidExt(ext)) exten_channel_map.put(ext, channel);
            session_channels.computeIfAbsent(uniqueId, k -> Collections.synchronizedSet(new HashSet<>())).add(channel);
        } else if (event instanceof AgentCalledEvent) {
            handleAgentCalled((AgentCalledEvent) event);
        } else if (event instanceof BridgeEnterEvent) {
            handleBridgeEnter((BridgeEnterEvent) event);
        } else if (event instanceof HangupEvent) {
            handleHangup((HangupEvent) event);
        }
    }

    private void handleAgentCalled(AgentCalledEvent e) {
        String agentExt = extractNumberOnly(e.getInterface());
        if (isValidExt(agentExt)) {
            exten_caller_channel.put(agentExt, e.getChannel());
            exten_linked_id.put(agentExt, e.getLinkedId());
            if (e.getDestinationChannel() != null) exten_channel_map.put(agentExt, e.getDestinationChannel());
            
            if (!answering_lock.contains(agentExt)) {
                sendInboundPopup(agentExt, e.getCallerIdNum(), e.getChannel(), e.getLinkedId());
            }
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
            Map<String, Object> params = new HashMap<>();
            params.put("cmpycd", ""); // 💡 [교정] 하드코딩 제거 (향후 DID별 매핑 로직 필요)
            params.put("phone", callerId);
            Map<String, Object> customer = inboundMapper.findCustomerByPhoneMap(params);
            if (customer != null) customer.forEach((k, v) -> data.put(k.toLowerCase(), v));
            else data.put("custnm", "미등록 고객");
            webSocketHandler.sendMessage(exten, objectMapper.writeValueAsString(data));
        } catch (Exception e) {}
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

    @PostConstruct public void init() { new Thread(() -> { try { managerConnection.addEventListener(this); if (!checkAmiConnection()) managerConnection.login(); } catch (Exception e) {} }).start(); }
    @PreDestroy public void cleanup() { try { if (managerConnection != null) managerConnection.logoff(); } catch (Exception ex) {} }
}
