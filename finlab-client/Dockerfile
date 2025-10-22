# Step 1: Build Angular App
FROM node:20-alpine AS builder
WORKDIR /app
COPY package.json ./
RUN npm i --no-package-lock
COPY . .
RUN npm run build -- --configuration production
 
# Step 2: Serve with Nginx
FROM nginx:alpine
RUN rm -rf /usr/share/nginx/html/*
COPY --from=builder /app/dist/finlab-client /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
 
CMD ["nginx", "-g", "daemon off;"]