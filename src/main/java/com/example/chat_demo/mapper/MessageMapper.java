package com.example.chat_demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.chat_demo.entity.Message;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {

}
