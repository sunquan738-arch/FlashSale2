package com.flashsale.server.service.impl;

import com.flashsale.server.application.product.ProductApplicationService;
import com.flashsale.server.service.ProductService;
import com.flashsale.server.vo.ProductCardVO;
import com.flashsale.server.vo.ProductDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductApplicationService productApplicationService;

    @Override
    public List<ProductCardVO> listProducts() {
        return productApplicationService.listProducts();
    }

    @Override
    public ProductDetailVO getProductDetail(Long id) {
        return productApplicationService.getProductDetail(id);
    }
}
