package com.flashsale.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashsale.server.entity.outbox.OutboxEvent;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OutboxEventMapper extends BaseMapper<OutboxEvent> {
}
