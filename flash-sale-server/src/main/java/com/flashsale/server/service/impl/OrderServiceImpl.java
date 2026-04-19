package com.flashsale.server.service.impl;

import com.flashsale.server.application.order.OrderApplicationService;
import com.flashsale.server.service.OrderService;
import com.flashsale.server.vo.OrderItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderApplicationService orderApplicationService;

    @Override
    public List<OrderItemVO> listMyOrders() {
        return orderApplicationService.listMyOrders();
    }
}
