#!/bin/bash
dnf install -y docker
systemctl enable --now docker

# Install Ollama (creates systemd service listening on 127.0.0.1 by default)
curl -fsSL https://ollama.com/install.sh | sh

# Override to bind on all interfaces so external traffic on port 11434 works
mkdir -p /etc/systemd/system/ollama.service.d
printf '[Service]\nEnvironment="OLLAMA_HOST=0.0.0.0"\n' > /etc/systemd/system/ollama.service.d/override.conf
systemctl daemon-reload
systemctl restart ollama
sleep 30

# Pull the model
ollama pull llama3.2:1b

# FlexibilityForecasting docker commands are injected by DeploymentAutomation-macOS.sh
sudo docker login -u "antoniomiguelpalma9999" -p "Sabes912.."
sudo docker pull antoniomiguelpalma9999/flexibility-forecasting:1.0.0-SNAPSHOT
sudo docker run -d --name flexibility-forecasting -p 8080:8080 antoniomiguelpalma9999/flexibility-forecasting:1.0.0-SNAPSHOT
