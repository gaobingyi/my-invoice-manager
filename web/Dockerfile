# Build stage
FROM node:22-alpine AS build
WORKDIR /build
COPY web/package.json web/package-lock.json ./
RUN npm ci
COPY web/ ./
RUN npm run build

# Runtime stage
FROM nginx:1.27-alpine
COPY --from=build /build/dist /usr/share/nginx/html
# nginx.conf / cf-allow.conf / certs 由 docker-compose 挂载覆盖
EXPOSE 8088
CMD ["nginx", "-g", "daemon off;"]
