package com.flashsale.server.controller;

import com.flashsale.server.common.result.Result;
import com.flashsale.server.service.ProductService;
import com.flashsale.server.vo.ProductCardVO;
import com.flashsale.server.vo.ProductDetailVO;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public Result<List<ProductCardVO>> listProducts() {
        return Result.success(productService.listProducts());
    }

    @GetMapping("/{id}")
    public Result<ProductDetailVO> getProductDetail(@PathVariable @Positive(message = "product id must be greater than 0") Long id) {
        return Result.success(productService.getProductDetail(id));
    }
}
