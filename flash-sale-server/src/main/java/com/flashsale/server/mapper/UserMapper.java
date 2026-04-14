package com.flashsale.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashsale.server.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
