package com.crmbank.erp.crm.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiAiService {

    @Value("${google.ai.gemini.api-key}")
    private String apiKey;

    @Value("${google.ai.gemini.model-name:gemini-1.5-flash}")
    private String modelName;

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @PostConstruct
    public void init() {
        // UTF-8 보장
        restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        log.info("🔍 [AI Config] Gemini AI Service 초기화 - 모델: {}", modelName);
    }

    /**
     * 🤖 통합 상담 분석 (텍스트 또는 오디오)
     */
    public Map<String, String> analyze(String chatLog, String audioPath) {
        try {
            if (audioPath != null && !audioPath.trim().isEmpty()) {
                return analyzeAudio(audioPath);
            } else if (chatLog != null && !chatLog.trim().isEmpty()) {
                Map<String, String> result = new HashMap<>();
                result.put("stt", chatLog);
                result.put("summary", summarizeText(chatLog));
                return result;
            }
        } catch (Exception e) {
            log.error("AI Analysis Error: {}", e.getMessage());
        }
        return Map.of("stt", chatLog != null ? chatLog : "", "summary", "분석 실패");
    }

    /**
     * 💬 텍스트 요약
     */
    public String summarizeText(String chatLog) {
        if (chatLog == null || chatLog.trim().isEmpty()) return "요약할 내용 없음";
        try {
            String prompt = "다음 상담 내역을 한국어로 3줄 요약해줘. JSON 형식 {\"summary\": \"...\"}으로만 응답해:\n\n" + chatLog;
            return parseSimpleField(callGeminiApi(prompt, null), "summary");
        } catch (Exception e) {
            return "요약 실패 (" + e.getMessage() + ")";
        }
    }

    /**
     * 🎙️ 오디오 분석
     */
    public Map<String, String> analyzeAudio(String filePath) {
        Map<String, String> result = new HashMap<>();
        try {
            byte[] fileContent = Files.readAllBytes(Paths.get(filePath));
            String base64Content = Base64.getEncoder().encodeToString(fileContent);
            String prompt = "이 오디오를 한국어로 STT 및 3줄 요약해줘. JSON 형식 {\"stt\": \"...\", \"summary\": \"...\"}으로만 응답해.";
            Map<String, Object> inlineData = Map.of("mime_type", "audio/mpeg", "data", base64Content);
            String rawResponse = callGeminiApi(prompt, inlineData);
            result.put("stt", parseSimpleField(rawResponse, "stt"));
            result.put("summary", parseSimpleField(rawResponse, "summary"));
        } catch (Exception e) {
            result.put("stt", "STT 실패");
            result.put("summary", "오류 (" + e.getMessage() + ")");
        }
        return result;
    }

    /**
     * 🚀 Gemini API 호출 로직 (URI 객체 사용으로 404 방지)
     */
    private String callGeminiApi(String prompt, Map<String, Object> inlineData) throws Exception {
        String key = (apiKey != null) ? apiKey.trim() : "";
        String model = (modelName != null) ? modelName.trim().replace("models/", "") : "gemini-1.5-flash";

        // 💡 URI 객체를 생성하여 RestTemplate의 콜론(:) 오인식을 방지합니다.
        String urlStr = "https://generativelanguage.googleapis.com/v1/models/" + model + ":generateContent?key=" + key;
        URI uri = URI.create(urlStr);

        Map<String, Object> requestBody = Map.of("contents", List.of(Map.of("parts", List.of(
                inlineData != null ? Map.of("text", prompt, "inline_data", inlineData) : Map.of("text", prompt)
        ))));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(uri, new HttpEntity<>(requestBody, headers), Map.class);
            if (response.getBody() != null) {
                List<Map> candidates = (List<Map>) response.getBody().get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    Map content = (Map) candidates.get(0).get("content");
                    List<Map> responseParts = (List<Map>) content.get("parts");
                    return (String) responseParts.get(0).get("text");
                }
            }
            throw new RuntimeException("API 응답 구조 이상");
        } catch (HttpStatusCodeException e) {
            log.error("❌ Gemini API 에러: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException(e.getStatusCode().value() + " " + e.getStatusText());
        }
    }

    /**
     * 🧩 JSON 파싱
     */
    private String parseSimpleField(String rawText, String field) {
        try {
            int start = rawText.indexOf("{");
            int end = rawText.lastIndexOf("}");
            if (start != -1 && end != -1) {
                String json = rawText.substring(start, end + 1);
                Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {});
                return String.valueOf(map.getOrDefault(field, rawText));
            }
            return rawText;
        } catch (Exception e) {
            return rawText;
        }
    }
}
