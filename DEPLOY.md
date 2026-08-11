# 发票管理系统 · 云端部署指南

> 目标场景：**海外单机 VPS + Docker Compose**，个人/内部使用。
> 前置：已有可用的 `docker-compose.yml`（三服务：mysql + backend + nginx，全镜像化）。

---

## 1. 部署架构

```
[浏览器] --HTTPS(443)--> [Caddy/nginx] --反向代理--> [nginx:8088 容器] --/api--> [backend:8080 容器] --> [mysql:3306 容器]
                              │                                              │
                              └─ 静态前端 dist                               ├─ LLM 兜底 → https://opencode.ai/zen/v1
                                                                              └─ 命名卷: mysql-data / backend-data / backend-logs
```

- 前端构建产物（`web/Dockerfile` 多阶段）与后端 jar（`server/Dockerfile`）都已进镜像
- 单机 `docker compose up --build` 一次拉起，不依赖宿主机预构建产物

---

## 2. 服务器准备

### 2.1 购买 VPS

| 项 | 建议 |
|---|---|
| 地域 | 海外（目标已定，海外云） |
| 规格 | **1C1G**（已调优，见 §8 附录；预计容器 ~450MiB + 系统 ~200MiB） |
| 系统 | Ubuntu 22.04/24.04 LTS |
| 存储 | 40GB SSD（镜像 + 卷 + 备份） |

> 1C1G 是下限。若预算允许，2C2G 更从容（Java 默认堆 + MySQL 默认 buffer pool，免调优）。

### 2.2 SSH 登录

```bash
ssh root@<服务器IP>
# 或创建普通用户 + sudo，生产不建议 root 直连
```

### 2.3 安装 Docker

```bash
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER   # 重新登录生效
```

### 2.4 安装 Caddy（HTTPS，推荐）

Caddy 自动申请/续期 Let's Encrypt 证书，比 nginx+certbot 省事：

```bash
sudo apt install -y debian-keyring debian-archive-keyring apt-transport-https curl
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/gpg.key' | sudo gpg --dearmor -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt' | sudo tee /etc/apt/sources.list.d/caddy-stable.list
sudo apt update && sudo apt install -y caddy
```

---

## 3. 代码与配置上传

### 3.1 本地构建镜像并传输（推荐，省云端拉取时间）

云端拉 `maven:3.9-eclipse-temurin-25` / `node:22-alpine` 基础镜像较慢，本地先构建好：

```bash
# 本地
docker compose build
docker save invoice-backend:1.0.0 invoice-web:1.0.0 | gzip > images.tar.gz
scp images.tar.gz root@<服务器IP>:/root/
```

```bash
# 云端
docker load < images.tar.gz
```

> 基础镜像（mysql/nginx/temurin/node）在云端 `docker compose up` 时按需拉取。

### 3.2 上传项目文件

只需 3 个文件（源码不需要上云）：

```bash
scp docker-compose.yml root@<服务器IP>:/opt/invoice/
scp server/ddl/schema.sql root@<服务器IP>:/opt/invoice/server/ddl/
scp web/nginx.conf root@<服务器IP>:/opt/invoice/web/
# .env 手动创建（含密码，勿 scp 明文传输，见下）
```

---

## 4. 环境变量配置

### 4.1 云端 `.env`（手动创建，chmod 600）

```bash
cd /opt/invoice
touch .env && chmod 600 .env
vi .env
```

```ini
# 云端 .env —— 勿提交，勿与本地 .env 混用
DB_PASSWORD=更换为强密码
MYSQL_ROOT_PASSWORD=更换为强密码
LLM_API_KEY=你的新 key
APP_LLM_MODEL=big-pickle
APP_LLM_BASE_URL=https://opencode.ai/zen/v1
```

**安全要求**：
- `DB_PASSWORD` 别用 `Invoice123!`，生成随机强密码：`openssl rand -base64 18`
- 云上 `chmod 600 .env`
- 本地/云端 `.env` 都是 gitignored，**绝不提交仓库**

### 4.2 与本地 `.env` 的区别

| 值 | 本地（根 `.env`） | 云端 `.env` |
|---|---|---|
| `DB_PASSWORD` | `Invoice123!`（开发） | 随机强密码 |
| `APP_LLM_BASE_URL` | `https://opencode.ai/zen/v1` | 相同（海外可达） |
| `LLM_API_KEY` | 你的 key | 同 key 或云上新 key |

---

## 5. 公网访问与 HTTPS

> **本项目无域名**。推荐 **Cloudflare Tunnel**（免费、真 HTTPS、零端口暴露、无需 DNS A 记录）。备选方案见 5.4。

### 5.1 Cloudflare Tunnel（推荐，无域名首选）

原理：VPS 装 `cloudflared` 客户端 → 连 Cloudflare 边缘 → 边缘给一个 `*.trycloudflare.com` 随机子域或自定义子域。VPS **不需要公网开放端口**，安全组只需 22。

```bash
# 1. VPS 安装 cloudflared
curl -L --output cloudflared.deb https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64.deb
sudo dpkg -i cloudflared.deb

# 2. 快速通道（免注册，临时 URL，每次重启变）
cloudflared tunnel --url http://localhost:8088
# 输出: https://xxx-random-123.trycloudflare.com  ← 浏览器访问这个

# 3. 正式通道（固定域名，需注册 cloudflare + 添加站点）
cloudflared tunnel login
cloudflared tunnel create invoice
cloudflared tunnel route dns invoice your-subdomain.your-site.tld
cloudflared tunnel run --url http://localhost:8088 invoice
# 或写 /etc/cloudflared/config.yml 用 systemd 常驻
```

**优点**：自动 HTTPS、免公网端口、隐藏 VPS IP。**缺点**：走 Cloudflare 边缘，国内访问慢/不稳（本项目目标海外，无碍）。

> 无 Cloudflare 账号？用快速通道（第 2 步）即可先跑起来，URL 随机但 HTTPS 加密可用。

### 5.2 有域名：Caddy（备选）

DNS：域名 A 记录 → VPS IP。`/etc/caddy/Caddyfile`：

```
invoice.example.com {
    reverse_proxy localhost:8088
}
```

```bash
sudo systemctl restart caddy
```

自动申请证书，`https://invoice.example.com` 可用。端口 80/443 在安全组放行。

### 5.3 什么都没有：IP + HTTP/自签

```bash
# 直接访问，无加密（公网 IP 明文传发票数据，个人临时可用，不建议长期）
http://<VPS-IP>:8088
```

或 Caddy 自签（有加密但有浏览器警告）：

```
:443 {
    tls internal
    reverse_proxy localhost:8088
}
```

---

## 6. 启动与验证

```bash
cd /opt/invoice
docker compose up -d
docker compose ps          # 三个服务应 healthy
curl -s http://localhost:8088/api/invoices   # → {"content":[],...}
```

浏览器访问：
- 无域名 → Cloudflare Tunnel 给的 `https://xxx.trycloudflare.com`（5.1）
- 有域名 → `https://invoice.example.com`（5.2）

验证项：
1. 列表页正常渲染
2. 上传样例 PDF → 解析 → 入库 → 列表出现
3. 预览/下载/删除可用

**验证 LLM 兜底**（可选）：上传一张正则解析不全的发票，`docker compose logs backend | grep "LLM fill"` 应有调用记录。

---

## 7. 数据备份（必须）

### 7.1 MySQL 定时备份（cron）

```bash
crontab -e
```

```cron
# 每天 3 点备份 MySQL 数据卷 + 上传文件卷
0 3 * * * docker exec invoice-mysql mysqldump -u root -p'密码' invoice_db | gzip > /opt/backup/invoice-$(date +\%F).sql.gz
0 3 * * * docker run --rm -v invoice-manager_backend-data:/data -v /opt/backup:/backup alpine tar czf /backup/uploads-$(date +\%F).tar.gz /data
```

### 7.2 保留策略

```bash
find /opt/backup -name "*.gz" -mtime +30 -delete   # 保留 30 天
```

### 7.3 恢复演练（重要，备份没验证=没备份）

```bash
# 恢复 DB
gunzip < invoice-2026-08-11.sql.gz | docker exec -i invoice-mysql mysql -u root -p'密码' invoice_db
# 恢复上传文件
docker run --rm -v invoice-manager_backend-data:/data -v /opt/backup:/backup alpine tar xzf /backup/uploads-2026-08-11.tar.gz -C /
```

---

## 8. 运维命令速查

```bash
docker compose ps                    # 状态
docker compose logs -f backend       # 后端日志（含 LLM fill）
docker compose restart backend       # 重启单服务
docker compose up -d --build         # 更新（改代码后）
docker compose down -v               # 停+清卷（⚠️ 删数据，先备份）
```

**更新部署流程**：
```bash
# 本地
docker compose build && docker save invoice-backend:1.0.0 invoice-web:1.0.0 | gzip > images.tar.gz
scp images.tar.gz root@<服务器IP>:/root/
# 云端
docker load < images.tar.gz && cd /opt/invoice && docker compose up -d
```

---

## 9. 安全清单（部署前逐项确认）

- [ ] `.env` `chmod 600`，强密码，未进仓库
- [ ] MySQL 端口**未**暴露公网（compose 无 `3306:3306` 映射）
- [ ] HTTPS 已启用（Cloudflare Tunnel 或 Caddy）
- [ ] backend/nginx 有 `restart: unless-stopped`
- [ ] 容器加 `TZ=Asia/Shanghai`（否则 `created_at` 差 8 小时）
- [ ] 定时备份 + 恢复演练完成
- [ ] 防火墙/安全组：Cloudflare Tunnel 只放行 22；Caddy 方案放行 22/80/443

---

## 10. 已知局限

- **单机无高可用**：VPS 宕机即服务中断。个人/内部用可接受；需 HA 则上 K8s（超出本文范围）。
- **LLM 依赖外网**：`opencode.ai` 不可达时 LLM 兜底失败，但上传不中断（null 保留）。
- **备份是 crontab 非异地**：VPS 挂掉备份同在机上。重要数据应异地同步（如 `rclone` 到对象存储）。

---

## 附录：1C1G 内存调优（已在 compose 内）

`docker-compose.yml` 已内置以下调优，无需手动改：

```yaml
backend:
  environment:
    - TZ=Asia/Shanghai
    - JAVA_OPTS=-Xmx256m -Xms128m -XX:+UseSerialGC -XX:MaxRAMPercentage=50
mysql:
  command: --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci \
           --innodb-buffer-pool-size=64M --performance-schema=OFF
```

| 调优项 | 值 | 效果 |
|---|---|---|
| JVM 堆 | `-Xmx256m` | Java 峰值内存从 ~500MB 压到 ~250MB |
| GC | `UseSerialGC` | 单核下比默认 G1 更省内存 |
| MySQL buffer pool | `64M` | 默认 128MB 减半 |
| `performance-schema=OFF` | 关 | MySQL 省 ~50MB 内存 |

> `JAVA_OPTS` 经 `server/Dockerfile` 的 `ENTRYPOINT ["sh","-c","java $JAVA_OPTS ..."]` 传入。改堆参数只需改 compose 的 `JAVA_OPTS`，无需重建镜像（Dockerfile 已支持 env 展开）。

预计部署后内存：backend ~250MiB + mysql ~200MiB + nginx ~10MiB ≈ **460MiB**，系统留 ~500MiB。

**实测监控**：
```bash
docker stats --no-stream    # 看容器内存
free -m                     # 看系统内存余量
```

**仍紧张时**：MySQL 还可加 `--table-open-cache=400 --thread-cache-size=16`；或升 2C2G（最省心）。
