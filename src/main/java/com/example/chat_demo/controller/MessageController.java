package com.example.chat_demo.controller;

import com.example.chat_demo.entity.Message;
import com.example.chat_demo.service.IMessageService;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
public class MessageController {

    @Autowired
    private OllamaChatModel ollamaChatModel;

    @Autowired
    private IMessageService messageService;

    @GetMapping("/api/messages")
    public List<Message> getMessages(){
        return messageService.list();
    }

    @DeleteMapping("/api/delete")
    public void deleteAllMessages(){
        messageService.remove(null);
    }

    @GetMapping("/api/stream")
    public Flux<ServerSentEvent<String>> chat(String prompt){
        // 用户消息
        Message umsg = new Message();
        umsg.setCreatedAt(LocalDateTime.now());
        umsg.setContent(prompt);
        umsg.setSender("user");
        messageService.save(umsg);

        StringBuilder aiResponeBuilder = new StringBuilder();

        // token
        return ollamaChatModel.stream(prompt).map(token->{
            aiResponeBuilder.append(token);
           return ServerSentEvent.builder(token).event("message").build();
        }).doOnComplete(()->{
            // AI消息
            Message aimsg = new Message();
            aimsg.setCreatedAt(LocalDateTime.now());
            aimsg.setContent(aiResponeBuilder.toString());
            aimsg.setSender("bot");
            messageService.save(aimsg);
        });
    }


    @PostMapping(value = "/api/getChatResult", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getchat(@RequestParam("prompt") String prompt) {

        // 1. 异步保存用户消息（避免阻塞主线程）
        Message umsg = new Message();
        umsg.setCreatedAt(LocalDateTime.now());
        umsg.setContent(prompt);
        umsg.setSender("user");
        CompletableFuture.runAsync(() -> messageService.save(umsg));

        // 2. 调用AI模型，同步获取思考后的完整结果
        try {
            String fullResult = ollamaChatModel.call(prompt);

            // 3. 保存AI消息（包含完整结果）
            Message aimsg = new Message();
            aimsg.setCreatedAt(LocalDateTime.now());
            aimsg.setContent(fullResult);
            aimsg.setSender("bot");
            messageService.save(aimsg);

            // 核心改动2：将结果包装成标准的 JSON 字符串返回，完美适配模板的 toGJson
            return "{\"content\":\"" + fullResult.replace("\"", "\\\"") + "\"}";

        } catch (Exception e) {
            // 异常信息也包装成 JSON 返回
            return "{\"error\":\"发生错误：" + e.getMessage().replace("\"", "\\\"") + "\"}";
        }
    }
}
