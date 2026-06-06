package com.example.wms.config;

import com.example.wms.domain.AppUser;
import com.example.wms.domain.InboundOrder;
import com.example.wms.domain.InboundOrderItem;
import com.example.wms.domain.OutboundOrder;
import com.example.wms.domain.OutboundOrderItem;
import com.example.wms.domain.Product;
import com.example.wms.domain.Stock;
import com.example.wms.domain.StorageLocation;
import com.example.wms.domain.Warehouse;
import com.example.wms.domain.enums.InboundType;
import com.example.wms.domain.enums.OrderStatus;
import com.example.wms.domain.enums.OutboundType;
import com.example.wms.domain.enums.Role;
import com.example.wms.repository.AppUserRepository;
import com.example.wms.repository.InboundOrderRepository;
import com.example.wms.repository.OutboundOrderRepository;
import com.example.wms.repository.ProductRepository;
import com.example.wms.repository.StockRepository;
import com.example.wms.repository.StorageLocationRepository;
import com.example.wms.repository.WarehouseRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DemoDataInitializer implements CommandLineRunner {
    private final AppUserRepository userRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final StorageLocationRepository locationRepository;
    private final StockRepository stockRepository;
    private final InboundOrderRepository inboundOrderRepository;
    private final OutboundOrderRepository outboundOrderRepository;

    public DemoDataInitializer(AppUserRepository userRepository, ProductRepository productRepository,
                               WarehouseRepository warehouseRepository, StorageLocationRepository locationRepository,
                               StockRepository stockRepository, InboundOrderRepository inboundOrderRepository,
                               OutboundOrderRepository outboundOrderRepository) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.locationRepository = locationRepository;
        this.stockRepository = stockRepository;
        this.inboundOrderRepository = inboundOrderRepository;
        this.outboundOrderRepository = outboundOrderRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        user("admin", "123456", "系统管理员", Role.SYSTEM_ADMIN);
        user("warehouse", "123456", "仓库管理员", Role.WAREHOUSE_MANAGER);
        user("buyer", "123456", "采购人员", Role.PURCHASING_OPERATIONS);

        Product p1 = product("P001", "697000000001", "医用口罩", "防护用品", "50只/盒", "盒", "默认供应商", 100, 80);
        Product p2 = product("P002", "697000000002", "一次性手套", "防护用品", "100只/盒", "盒", "默认供应商", 50, 0);
        Product p3 = product("P003", "697000000003", "矿泉水", "办公物资", "24瓶/箱", "箱", "默认供应商", 100, 300);
        Product p4 = product("P004", "697000000004", "打印纸", "办公物资", "A4 70g", "包", "默认供应商", 50, 20);
        Product p5 = product("P005", "697000000005", "记号笔", "办公物资", "黑色", "支", "默认供应商", 30, 0);
        product("P006", "697000000006", "电脑配件", "IT耗材", "通用", "件", "默认供应商", 20, 0);
        product("P007", "697000000007", "清洁工具", "清洁用品", "套装", "套", "默认供应商", 10, 0);

        Warehouse ulhkg = warehouse("ULHKG", "A", "WH-SH-01");
        warehouse("USLAX", "B");
        StorageLocation demoStockLocation = location("LT-DEMO-1-1-1", ulhkg, 1000);
        stock(p1, ulhkg, demoStockLocation, 80);
        stock(p3, ulhkg, demoStockLocation, 300);
        stock(p4, ulhkg, demoStockLocation, 20);

        inboundOrder(p1, p4, p5, ulhkg, demoStockLocation);
        outboundOrder(p1, p3, ulhkg, demoStockLocation);
    }

    private void user(String username, String password, String displayName, Role role) {
        userRepository.findByUsername(username).orElseGet(() -> {
            AppUser user = new AppUser();
            user.setUsername(username);
            user.setPassword(password);
            user.setDisplayName(displayName);
            user.setRole(role);
            user.setEnabled(true);
            return userRepository.save(user);
        });
    }

    private Product product(String sku, String barcode, String name, String category, String model, String unit,
                            String supplier, int safetyStock, int currentStock) {
        Product product = productRepository.findBySku(sku).orElseGet(Product::new);
        product.setSku(sku);
        product.setBarcode(barcode);
        product.setName(name);
        product.setCategory(category);
        product.setModelSpec(model);
        product.setUnitName(unit);
        product.setSupplier(supplier);
        product.setSafetyStock(safetyStock);
        product.setCurrentStock(currentStock);
        return productRepository.save(product);
    }

    private Warehouse warehouse(String code, String... legacyCodes) {
        Warehouse warehouse = warehouseRepository.findByCode(code).orElse(null);
        for (String legacyCode : legacyCodes) {
            if (warehouse == null) warehouse = warehouseRepository.findByCode(legacyCode).orElse(null);
        }
        if (warehouse == null) warehouse = new Warehouse();
        warehouse.setCode(code);
        warehouse.setName(code);
        return warehouseRepository.save(warehouse);
    }

    private StorageLocation location(String code, Warehouse warehouse, int capacity) {
        return locationRepository.findByCode(code).orElseGet(() -> {
            StorageLocation location = new StorageLocation();
            location.setCode(code);
            location.setWarehouse(warehouse);
            location.setCapacity(capacity);
            location.setOccupied(0);
            return locationRepository.save(location);
        });
    }

    private void stock(Product product, Warehouse warehouse, StorageLocation location, int quantity) {
        Stock stock = stockRepository.findByProductIdAndWarehouseIdAndLocationId(product.getId(), warehouse.getId(), location.getId())
                .orElseGet(Stock::new);
        stock.setProduct(product);
        stock.setWarehouse(warehouse);
        stock.setLocation(location);
        stock.setQuantity(quantity);
        stockRepository.save(stock);
    }

    private void inboundOrder(Product p1, Product p4, Product p5, Warehouse warehouse, StorageLocation location) {
        inboundOrderRepository.findByOrderNo("IN202606010001").orElseGet(() -> {
            InboundOrder order = new InboundOrder();
            order.setOrderNo("IN202606010001");
            order.setType(InboundType.PURCHASE);
            order.setStatus(OrderStatus.IN_QUEUE);
            order.setOperatorName("默认供应商");
            inboundItem(order, p1, warehouse, location, 10);
            inboundItem(order, p4, warehouse, location, 10);
            inboundItem(order, p5, warehouse, location, 10);
            return inboundOrderRepository.save(order);
        });
    }

    private void inboundItem(InboundOrder order, Product product, Warehouse warehouse, StorageLocation location, int quantity) {
        InboundOrderItem item = new InboundOrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setWarehouse(warehouse);
        item.setLocation(location);
        item.setQuantity(quantity);
        item.setReceivedQuantity(0);
        order.getItems().add(item);
    }

    private void outboundOrder(Product p1, Product p3, Warehouse warehouse, StorageLocation location) {
        outboundOrderRepository.findByOrderNo("OUT202606010001").orElseGet(() -> {
            OutboundOrder order = new OutboundOrder();
            order.setOrderNo("OUT202606010001");
            order.setType(OutboundType.SALE);
            order.setStatus(OrderStatus.IN_QUEUE);
            order.setOperatorName("演示客户");
            outboundItem(order, p1, warehouse, location, 5);
            outboundItem(order, p3, warehouse, location, 5);
            return outboundOrderRepository.save(order);
        });
    }

    private void outboundItem(OutboundOrder order, Product product, Warehouse warehouse, StorageLocation location, int quantity) {
        OutboundOrderItem item = new OutboundOrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setWarehouse(warehouse);
        item.setLocation(location);
        item.setQuantity(quantity);
        order.getItems().add(item);
    }
}
