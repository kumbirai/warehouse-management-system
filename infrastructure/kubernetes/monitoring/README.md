# Monitoring Configuration

Monitoring configurations for the Warehouse Management System.

## Components

- **Prometheus** - Metrics collection
- **Grafana** - Metrics visualization
- **AlertManager** - Alerting

## Configuration Files

- `prometheus-config.yaml` - Prometheus scrape configuration

## Setup

1. Deploy Prometheus operator (if using Prometheus Operator)
2. Apply monitoring configurations
3. Configure Grafana dashboards
4. Set up alerting rules

## Metrics Endpoints

All services expose metrics at `/actuator/prometheus` endpoint.

