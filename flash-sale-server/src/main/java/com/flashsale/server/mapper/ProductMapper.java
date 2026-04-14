package com.flashsale.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashsale.server.entity.Product;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}
