# Build stage
FROM golang:1.25-alpine AS builder

# Install git for version info
RUN apk add --no-cache git

WORKDIR /app

# Copy go mod files first for better caching
COPY go.mod go.sum* ./
RUN go mod download

# Copy source code
COPY . .

# Build arguments
ARG VERSION=dev
ARG COMMIT=unknown
ARG BUILD_DATE=unknown

# Build with CGO disabled for static binary
RUN CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build \
    -trimpath \
    -ldflags "-s -w -X main.Version=${VERSION} -X main.Commit=${COMMIT} -X main.BuildDate=${BUILD_DATE}" \
    -o /osb-cmdb \
    ./cmd/osb-cmdb

# Runtime stage - use distroless for minimal attack surface
FROM gcr.io/distroless/static-debian12:nonroot

# Copy binary from builder
COPY --from=builder /osb-cmdb /osb-cmdb

# Use non-root user
USER nonroot:nonroot

# Expose default port
EXPOSE 8080

# Run the broker
ENTRYPOINT ["/osb-cmdb"]
