-- 发票管理系统 DDL
-- MySQL 8+
-- 执行: mysql -u root -p < ddl/schema.sql

CREATE DATABASE IF NOT EXISTS invoice_db
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE invoice_db;

CREATE TABLE IF NOT EXISTS invoice (
  id              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  invoice_number  VARCHAR(20)   NOT NULL                COMMENT '发票号码',
  invoice_date    DATE          NULL                    COMMENT '开票日期',
  buyer_name      VARCHAR(128)  NULL                    COMMENT '购买方名称',
  buyer_tax_id    VARCHAR(20)   NULL                    COMMENT '购买方纳税人识别号',
  seller_name     VARCHAR(128)  NULL                    COMMENT '销售方名称',
  seller_tax_id   VARCHAR(20)   NULL                    COMMENT '销售方纳税人识别号',
  total_amount    DECIMAL(10,2) NULL                    COMMENT '金额合计(不含税)',
  tax_amount      DECIMAL(10,2) NULL                    COMMENT '税额合计',
  total_with_tax  DECIMAL(10,2) NULL                    COMMENT '价税合计(小写)',
  category        VARCHAR(64)   NULL                    COMMENT '项目名称，如 *餐饮服务*餐饮服务',
  drawer          VARCHAR(64)   NULL                    COMMENT '开票人',
  file_path       VARCHAR(255)  NOT NULL                COMMENT 'PDF 磁盘相对路径',
  created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_invoice_number (invoice_number),
  KEY idx_invoice_date (invoice_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT ='发票表';

CREATE TABLE IF NOT EXISTS app_user (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  username      VARCHAR(64)  NOT NULL                COMMENT '登录名',
  password_hash VARCHAR(100) NOT NULL                COMMENT 'BCrypt 密码散列',
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT ='登录用户表';
