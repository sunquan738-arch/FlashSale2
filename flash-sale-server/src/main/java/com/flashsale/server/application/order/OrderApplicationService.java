package com.flashsale.server.application.order;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flashsale.server.entity.Order;
import com.flashsale.server.mapper.OrderMapper;
import com.flashsale.server.utils.UserContext;
import com.flashsale.server.vo.OrderItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderApplicationService {

    private final OrderMapper orderMapper;

    public List<OrderItemVO> listMyOrders() {
        Long userId = UserContext.getUserId();
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreateTime);

        return orderMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    private OrderItemVO toVO(Order order) {
        OrderItemVO vo = new OrderItemVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setOrderStatus(order.getOrderStatus());
        vo.setResultStatus(mapResultStatus(order.getOrderStatus()));
        vo.setCreateTime(order.getCreateTime());
        return vo;
    }

    private String mapResultStatus(Integer orderStatus) {
        if (orderStatus == null || orderStatus == 0) {
            return "进行中";
        }
        if (orderStatus == 1 || orderStatus == 3) {
            return "成功";
        }
        return "失败";
    }
}
