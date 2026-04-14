package com.flashsale.server.service;

import com.flashsale.server.vo.ProductCardVO;
import com.flashsale.server.vo.ProductDetailVO;

import java.util.List;

public interface ProductService {

    List<ProductCardVO> listProducts();

    ProductDetailVO getProductDetail(Long id);
}
