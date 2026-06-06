CREATE DATABASE IF NOT EXISTS smart_wms DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE smart_wms;

CREATE TABLE IF NOT EXISTS products (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  sku VARCHAR(255) NOT NULL UNIQUE,
  name VARCHAR(255) NOT NULL,
  category VARCHAR(255),
  model_spec VARCHAR(255),
  unit_name VARCHAR(255),
  supplier VARCHAR(255),
  safety_stock INT NOT NULL DEFAULT 0,
  current_stock INT NOT NULL DEFAULT 0,
  image_url VARCHAR(255),
  remark VARCHAR(1000),
  created_at DATETIME,
  updated_at DATETIME
);

CREATE TABLE IF NOT EXISTS warehouses (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(255) NOT NULL UNIQUE,
  name VARCHAR(255) NOT NULL,
  address VARCHAR(255),
  manager VARCHAR(255),
  remark VARCHAR(255),
  created_at DATETIME,
  updated_at DATETIME
);

CREATE TABLE IF NOT EXISTS warehouse_zones (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(255),
  name VARCHAR(255),
  warehouse_id BIGINT NOT NULL,
  created_at DATETIME,
  updated_at DATETIME,
  CONSTRAINT fk_zone_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)
);

CREATE TABLE IF NOT EXISTS shelves (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(255),
  name VARCHAR(255),
  zone_id BIGINT NOT NULL,
  created_at DATETIME,
  updated_at DATETIME,
  CONSTRAINT fk_shelf_zone FOREIGN KEY (zone_id) REFERENCES warehouse_zones(id)
);

CREATE TABLE IF NOT EXISTS storage_locations (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(255) NOT NULL UNIQUE,
  warehouse_id BIGINT NOT NULL,
  shelf_id BIGINT NULL,
  capacity INT DEFAULT 0,
  occupied INT DEFAULT 0,
  status VARCHAR(32) DEFAULT 'ENABLED',
  created_at DATETIME,
  updated_at DATETIME,
  CONSTRAINT fk_location_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
  CONSTRAINT fk_location_shelf FOREIGN KEY (shelf_id) REFERENCES shelves(id)
);

CREATE TABLE IF NOT EXISTS stocks (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id BIGINT NOT NULL,
  warehouse_id BIGINT NOT NULL,
  location_id BIGINT NOT NULL,
  quantity INT DEFAULT 0,
  created_at DATETIME,
  updated_at DATETIME,
  UNIQUE KEY uk_stock_account (product_id, warehouse_id, location_id),
  CONSTRAINT fk_stock_product FOREIGN KEY (product_id) REFERENCES products(id),
  CONSTRAINT fk_stock_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
  CONSTRAINT fk_stock_location FOREIGN KEY (location_id) REFERENCES storage_locations(id)
);

CREATE TABLE IF NOT EXISTS stock_movements (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id BIGINT NOT NULL,
  warehouse_id BIGINT NOT NULL,
  location_id BIGINT NOT NULL,
  type VARCHAR(32),
  quantity INT,
  before_quantity INT,
  after_quantity INT,
  source_no VARCHAR(255),
  operator_name VARCHAR(255),
  movement_time DATETIME,
  created_at DATETIME,
  updated_at DATETIME,
  CONSTRAINT fk_movement_product FOREIGN KEY (product_id) REFERENCES products(id),
  CONSTRAINT fk_movement_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
  CONSTRAINT fk_movement_location FOREIGN KEY (location_id) REFERENCES storage_locations(id)
);

CREATE TABLE IF NOT EXISTS inbound_orders (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(255),
  type VARCHAR(32),
  status VARCHAR(32),
  operator_name VARCHAR(255),
  completed_at DATETIME,
  remark VARCHAR(255),
  created_at DATETIME,
  updated_at DATETIME
);

CREATE TABLE IF NOT EXISTS inbound_order_items (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  warehouse_id BIGINT NOT NULL,
  location_id BIGINT NOT NULL,
  quantity INT,
  received_quantity INT DEFAULT 0,
  tracking_no VARCHAR(255),
  created_at DATETIME,
  updated_at DATETIME,
  CONSTRAINT fk_in_item_order FOREIGN KEY (order_id) REFERENCES inbound_orders(id),
  CONSTRAINT fk_in_item_product FOREIGN KEY (product_id) REFERENCES products(id),
  CONSTRAINT fk_in_item_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
  CONSTRAINT fk_in_item_location FOREIGN KEY (location_id) REFERENCES storage_locations(id)
);

CREATE TABLE IF NOT EXISTS outbound_orders LIKE inbound_orders;

CREATE TABLE IF NOT EXISTS outbound_order_items (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  warehouse_id BIGINT NOT NULL,
  location_id BIGINT NOT NULL,
  quantity INT,
  created_at DATETIME,
  updated_at DATETIME,
  CONSTRAINT fk_out_item_order FOREIGN KEY (order_id) REFERENCES outbound_orders(id),
  CONSTRAINT fk_out_item_product FOREIGN KEY (product_id) REFERENCES products(id),
  CONSTRAINT fk_out_item_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
  CONSTRAINT fk_out_item_location FOREIGN KEY (location_id) REFERENCES storage_locations(id)
);

CREATE TABLE IF NOT EXISTS inventory_checks (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  check_no VARCHAR(255),
  status VARCHAR(32),
  operator_name VARCHAR(255),
  confirmed_at DATETIME,
  remark VARCHAR(255),
  created_at DATETIME,
  updated_at DATETIME
);

CREATE TABLE IF NOT EXISTS inventory_check_items (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  check_task_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  warehouse_id BIGINT NOT NULL,
  location_id BIGINT NOT NULL,
  book_quantity INT,
  actual_quantity INT,
  diff_quantity INT,
  created_at DATETIME,
  updated_at DATETIME,
  CONSTRAINT fk_check_item_task FOREIGN KEY (check_task_id) REFERENCES inventory_checks(id),
  CONSTRAINT fk_check_item_product FOREIGN KEY (product_id) REFERENCES products(id),
  CONSTRAINT fk_check_item_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
  CONSTRAINT fk_check_item_location FOREIGN KEY (location_id) REFERENCES storage_locations(id)
);

CREATE TABLE IF NOT EXISTS stock_alerts (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id BIGINT NOT NULL,
  status VARCHAR(32),
  message VARCHAR(255),
  created_at DATETIME,
  updated_at DATETIME,
  CONSTRAINT fk_alert_product FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE IF NOT EXISTS ai_reports (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(255),
  structured_data_json LONGTEXT,
  analysis_text LONGTEXT,
  pdf_url VARCHAR(255),
  created_at DATETIME,
  updated_at DATETIME
);

CREATE TABLE IF NOT EXISTS app_users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(255),
  display_name VARCHAR(255),
  role VARCHAR(64),
  enabled BIT DEFAULT 1,
  created_at DATETIME,
  updated_at DATETIME
);

INSERT IGNORE INTO warehouses (id, code, name, address, manager, created_at, updated_at)
VALUES (1, 'WH-SH-01', '上海主仓', '上海市浦东新区', '仓库管理员', NOW(), NOW());

INSERT IGNORE INTO storage_locations (id, code, warehouse_id, capacity, occupied, status, created_at, updated_at)
VALUES (1, 'A01-01-01', 1, 1000, 0, 'ENABLED', NOW(), NOW());

INSERT IGNORE INTO products (id, sku, name, category, model_spec, unit_name, supplier, safety_stock, current_stock, created_at, updated_at)
VALUES
  (1, 'SKU-1001', '扫码枪', '设备', '无线款', '台', '默认供应商', 20, 0, NOW(), NOW()),
  (2, 'SKU-2001', '包装纸箱', '耗材', '40x30x20cm', '个', '默认供应商', 200, 0, NOW(), NOW());
