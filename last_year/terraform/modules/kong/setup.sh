#!/bin/bash
echo "Waiting for Kong proxy (port 8000) to be open..."
until curl -s -o /dev/null -w "%%{http_code}" http://${publicdns}:8000/ | grep -q "404"; do
  sleep 2
done
echo "Kong proxy (port 8000) is open!"