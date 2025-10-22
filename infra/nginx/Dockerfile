FROM node:18-alpine AS build

WORKDIR /app

COPY finlab-client/package*.json ./
RUN npm ci

COPY finlab-client/ ./
RUN npm run build -- --configuration production

FROM nginx:alpine

RUN apk add --no-cache openssl

RUN mkdir -p /etc/nginx/ssl && \
    openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
    -keyout /etc/nginx/ssl/key.pem \
    -out /etc/nginx/ssl/cert.pem \
    -subj "/C=US/ST=State/L=City/O=FinLab/CN=localhost"

COPY --from=build /app/dist/finlab-client /usr/share/nginx/html

COPY infra/nginx/nginx.conf /etc/nginx/nginx.conf

EXPOSE 443

CMD ["nginx", "-g", "daemon off;"]
