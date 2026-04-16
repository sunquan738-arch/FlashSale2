package com.flashsale.server.service;

import com.flashsale.server.vo.OrderItemVO;

import java.util.List;

public interface OrderService {

    List<OrderItemVO> listMyOrders();
}
