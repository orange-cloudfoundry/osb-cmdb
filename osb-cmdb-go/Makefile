# Makefile for osb-cmdb-go

# Variables
BINARY_NAME := osb-cmdb
VERSION ?= $(shell git describe --tags --always --dirty 2>/dev/null || echo "dev")
COMMIT := $(shell git rev-parse --short HEAD 2>/dev/null || echo "unknown")
BUILD_DATE := $(shell date -u +"%Y-%m-%dT%H:%M:%SZ")

# Go settings
GO := go
GOFLAGS := -trimpath
LDFLAGS := -s -w \
	-X main.Version=$(VERSION) \
	-X main.Commit=$(COMMIT) \
	-X main.BuildDate=$(BUILD_DATE)

# Directories
BUILD_DIR := bin
CMD_DIR := cmd/osb-cmdb

.PHONY: all build test lint clean fmt vet tidy help

# Default target
all: lint test build

# Build the binary
build:
	@echo "Building $(BINARY_NAME) $(VERSION)..."
	@mkdir -p $(BUILD_DIR)
	$(GO) build $(GOFLAGS) -ldflags "$(LDFLAGS)" -o $(BUILD_DIR)/$(BINARY_NAME) ./$(CMD_DIR)

# Build for Linux (useful for Docker)
build-linux:
	@echo "Building $(BINARY_NAME) for linux/amd64..."
	@mkdir -p $(BUILD_DIR)
	CGO_ENABLED=0 GOOS=linux GOARCH=amd64 $(GO) build $(GOFLAGS) -ldflags "$(LDFLAGS)" -o $(BUILD_DIR)/$(BINARY_NAME)-linux-amd64 ./$(CMD_DIR)

# Run tests
test:
	@echo "Running tests..."
	$(GO) test -v -race -coverprofile=coverage.out ./...

# Run tests with short flag (skip integration tests)
test-short:
	@echo "Running short tests..."
	$(GO) test -v -short -race ./...

# Generate coverage report
coverage: test
	@echo "Generating coverage report..."
	$(GO) tool cover -html=coverage.out -o coverage.html

# Run linter
lint:
	@echo "Running linter..."
	@which golangci-lint > /dev/null || (echo "Installing golangci-lint..." && go install github.com/golangci/golangci-lint/cmd/golangci-lint@latest)
	golangci-lint run ./...

# Format code
fmt:
	@echo "Formatting code..."
	$(GO) fmt ./...
	@which goimports > /dev/null || (echo "Installing goimports..." && go install golang.org/x/tools/cmd/goimports@latest)
	goimports -w .

# Run go vet
vet:
	@echo "Running vet..."
	$(GO) vet ./...

# Tidy dependencies
tidy:
	@echo "Tidying dependencies..."
	$(GO) mod tidy

# Download dependencies
deps:
	@echo "Downloading dependencies..."
	$(GO) mod download

# Clean build artifacts
clean:
	@echo "Cleaning..."
	rm -rf $(BUILD_DIR)
	rm -f coverage.out coverage.html

# Run the broker locally
run: build
	@echo "Running $(BINARY_NAME)..."
	./$(BUILD_DIR)/$(BINARY_NAME)

# Docker build
docker-build:
	@echo "Building Docker image..."
	docker build -t osb-cmdb-go:$(VERSION) .

# Help target
help:
	@echo "Available targets:"
	@echo "  all          - Run lint, test, and build (default)"
	@echo "  build        - Build the binary"
	@echo "  build-linux  - Build for linux/amd64"
	@echo "  test         - Run all tests with coverage"
	@echo "  test-short   - Run short tests only"
	@echo "  coverage     - Generate HTML coverage report"
	@echo "  lint         - Run golangci-lint"
	@echo "  fmt          - Format code with gofmt and goimports"
	@echo "  vet          - Run go vet"
	@echo "  tidy         - Tidy go.mod"
	@echo "  deps         - Download dependencies"
	@echo "  clean        - Remove build artifacts"
	@echo "  run          - Build and run locally"
	@echo "  docker-build - Build Docker image"
	@echo "  help         - Show this help"
