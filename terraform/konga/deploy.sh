#!/bin/bash
echo "Starting..."
sudo yum install -y docker
sudo service docker start

sudo mkdir -p /opt/konga-seed

cat <<'EOF' | sudo tee /opt/konga-seed/users.json >/dev/null
[
  {
    "username": "admin",
    "email": "admin@example.com",
    "firstName": "Admin",
    "lastName": "User",
    "node_id": "",
    "admin": true,
    "active": true,
    "password": "adminadminadmin"
  }
]
EOF

cat <<EOF | sudo tee /opt/konga-seed/kong_node.json >/dev/null
[
  {
    "name": "default",
    "type": "default",
    "kong_admin_url": "${KONG_ADMIN_URL}"
  }
]
EOF

sudo docker pull pantsel/konga
sudo docker run -d --name konga \
  -p 1337:1337 \
  -v /opt/konga-seed:/data \
  -e "KONGA_SEED_USER_DATA_SOURCE_FILE=/data/users.json" \
  -e "KONGA_SEED_KONG_NODE_DATA_SOURCE_FILE=/data/kong_node.json" \
  pantsel/konga
echo "Finished."