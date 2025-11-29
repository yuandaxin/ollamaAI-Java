package com.example.chat_demo.controller;

import com.example.chat_demo.entity.Message;
import com.example.chat_demo.service.IMessageService;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;

@RestController
public class MessageController {

    @Autowired
    private OllamaChatModel ollamaChatModel;

    @Autowired
    private IMessageService messageService;

    @GetMapping("/messages")
    public List<Message> getMessages(){
        return messageService.list();
    }

    @GetMapping("/stream")
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
}
