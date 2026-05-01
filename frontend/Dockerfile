FROM oven/bun:1.3.11 AS builder
WORKDIR /app
COPY package.json bun.lock ./
RUN bun install --frozen-lockfile
COPY . .
RUN bun run build

FROM oven/bun:1.3.11-alpine
WORKDIR /app
COPY --from=builder /app/.output ./.output
EXPOSE 3000
ENTRYPOINT ["bun", "run", ".output/server/index.mjs"]
