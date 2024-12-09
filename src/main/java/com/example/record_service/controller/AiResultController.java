package com.example.record_service.controller;

import com.example.record_service.dto.AiResultDto;
import com.example.record_service.entity.Record;
import com.example.record_service.service.RecordService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/records/ai")
@RequiredArgsConstructor
public class AiResultController {
    private final SimpMessagingTemplate messagingTemplate;
    private final RecordService recordService;
    private static final Logger log = LoggerFactory.getLogger(AiResultController.class);


    // Ai 모델에게서 해당 HTTP 요청을 보내서 결과값 받아옴
    @PostMapping("/results")
    public ResponseEntity<?> receiveAIResult(@RequestBody AiResultDto resultDto) {
        try {
            String recordIdx = resultDto.getRecordIdx(); // 기록 식별
            String result = resultDto.getResult(); // AI 결과값

            Boolean isHuman = resultDto.getIsHuman();
            String text = isHuman ? resultDto.getText() : null; // isHuman일 경우에만 text 값 저장

            // Record 업데이트
            if (!recordService.updateRecordWithAIResult(recordIdx, result, isHuman, text)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Record with recordIdx " + recordIdx + " not found.");
            }

            Optional<Record> optionalRecord = recordService.getRecordByRecordIdx(recordIdx);
            String userIdx = optionalRecord.get().getUserIdx();

            // WebSocket 경로로 메시지 전송
            String destination = "/socket/" + userIdx;
            messagingTemplate.convertAndSend(destination, result);

            log.info("WebSocket message sent to {}: {}", destination, result);
            return ResponseEntity.ok("Message sent to WebSocket");
        } catch (RuntimeException e) {
            log.error("Error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error while sending WebSocket message: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to send WebSocket message");
        }
    }
}
