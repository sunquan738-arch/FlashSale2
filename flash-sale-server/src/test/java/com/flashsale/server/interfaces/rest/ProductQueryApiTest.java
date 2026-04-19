package com.flashsale.server.interfaces.rest;

import com.flashsale.server.common.handler.GlobalExceptionHandler;
import com.flashsale.server.service.ProductService;
import com.flashsale.server.vo.ProductCardVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductQueryApiTest {

    private final ProductService productService = Mockito.mock(ProductService.class);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ProductController controller = new ProductController(productService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listProductsShouldReturnData() throws Exception {
        ProductCardVO vo = new ProductCardVO();
        vo.setId(1L);
        vo.setName("Keyboard");
        vo.setOriginalPrice(BigDecimal.valueOf(199));

        Mockito.when(productService.listProducts()).thenReturn(List.of(vo));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value(1));
    }
}
