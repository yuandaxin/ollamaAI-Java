package com.example.chat_demo.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.chat_demo.entity.Message;
import com.example.chat_demo.mapper.MessageMapper;
import org.springframework.stereotype.Service;

@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements IMessageService{
}
