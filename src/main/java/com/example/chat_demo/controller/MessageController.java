package com.example.chat_demo.controller;

import com.example.chat_demo.entity.Message;
import com.example.chat_demo.service.IMessageService;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

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


    @GetMapping("/api/getChatResult")
    public String getchat(String prompt) {
        // 1. 补充定义StringBuilder，用于累积AI生成的完整结果
        StringBuilder aiResponseBuilder = new StringBuilder();

        // 2. 异步保存用户消息（避免阻塞主线程）
        Message umsg = new Message();
        umsg.setCreatedAt(LocalDateTime.now());
        umsg.setContent(prompt);
        umsg.setSender("user");
        CompletableFuture.runAsync(() -> messageService.save(umsg));

        // 3. 调用AI模型，同步获取思考后的完整结果
        try {
            // 假设ollamaChatModel.chat()返回完整的AI响应字符串（内部已包含“思考”逻辑）
            String fullResult = ollamaChatModel.call(prompt); // 同步调用，获取完整结果

            // 4. 保存AI消息（包含完整结果）
            Message aimsg = new Message();
            aimsg.setCreatedAt(LocalDateTime.now());
            aimsg.setContent(fullResult);
            aimsg.setSender("bot");
            messageService.save(aimsg);

            // 5. 返回完整结果
            return fullResult;
        } catch (Exception e) {
            // 6. 错误处理：捕获异常并记录日志，返回错误提示
            return "发生错误：" + e.getMessage();
        }
    }
}
