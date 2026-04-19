package com.flashsale.server.application.product;

import com.flashsale.server.common.enums.ResultCode;
import com.flashsale.server.common.exception.BusinessException;
import com.flashsale.server.entity.Product;
import com.flashsale.server.infrastructure.seckill.SeckillCacheService;
import com.flashsale.server.vo.ProductCardVO;
import com.flashsale.server.vo.ProductDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductApplicationService {

    private final SeckillCacheService seckillCacheService;

    public List<ProductCardVO> listProducts() {
        return seckillCacheService.listProducts().stream().map(this::toCardVO).toList();
    }

    public ProductDetailVO getProductDetail(Long id) {
        Product product = seckillCacheService.getProductById(id);
        if (product == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "product not found");
        }
        return toDetailVO(product);
    }

    private ProductCardVO toCardVO(Product product) {
        ProductCardVO vo = new ProductCardVO();
        vo.setId(product.getId());
        vo.setName(product.getName());
        vo.setOriginalPrice(product.getOriginalPrice());
        vo.setImageUrl(buildImageUrl(product.getId()));
        vo.setStock(product.getStock());
        return vo;
    }

    private ProductDetailVO toDetailVO(Product product) {
        ProductDetailVO vo = new ProductDetailVO();
        vo.setId(product.getId());
        vo.setName(product.getName());
        vo.setDescription(product.getDescription());
        vo.setOriginalPrice(product.getOriginalPrice());
        vo.setImageUrl(buildImageUrl(product.getId()));
        vo.setStock(product.getStock());
        vo.setStatus(product.getStatus());
        return vo;
    }

    private String buildImageUrl(Long productId) {
        return "https://picsum.photos/seed/product-" + productId + "/400/400";
    }
}
