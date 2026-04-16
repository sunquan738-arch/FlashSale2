package com.flashsale.server.controller;

import com.flashsale.server.common.result.Result;
import com.flashsale.server.service.OrderService;
import com.flashsale.server.vo.OrderItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/my")
    public Result<List<OrderItemVO>> myOrders() {
        return Result.success(orderService.listMyOrders());
    }
}
