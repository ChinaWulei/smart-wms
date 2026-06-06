package com.example.wms.service;

import com.example.wms.common.BizException;
import com.example.wms.domain.Product;
import com.example.wms.repository.ProductRepository;
import com.example.wms.repository.StockMovementRepository;
import com.example.wms.repository.StockRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final StockMovementRepository movementRepository;

    public ProductService(ProductRepository productRepository, StockRepository stockRepository, StockMovementRepository movementRepository) {
        this.productRepository = productRepository;
        this.stockRepository = stockRepository;
        this.movementRepository = movementRepository;
    }

    public List<Product> search(String keyword, String category) {
        if (StringUtils.hasText(keyword)) {
            return productRepository.findByNameContainingIgnoreCaseOrSkuContainingIgnoreCaseOrBarcodeContainingIgnoreCase(
                    keyword, keyword, keyword);
        }
        if (StringUtils.hasText(category)) {
            return productRepository.findByCategory(category);
        }
        return productRepository.findAll();
    }

    @Transactional
    public Product save(Product product) {
        if (product.getSafetyStock() == null || product.getSafetyStock() < 0) {
            throw new BizException("安全库存不能小于0");
        }
        if (product.getCurrentStock() == null || product.getCurrentStock() < 0) {
            product.setCurrentStock(0);
        }
        return productRepository.save(product);
    }

    @Transactional
    public void delete(Long id) {
        Product product = productRepository.findById(id).orElseThrow(() -> new BizException("商品不存在"));
        if (stockRepository.sumQuantityByProductId(id) > 0) {
            throw new BizException("商品存在库存，不能删除");
        }
        if (movementRepository.existsByProductId(id)) {
            throw new BizException("商品存在库存流水，不能删除");
        }
        productRepository.delete(product);
    }
}
