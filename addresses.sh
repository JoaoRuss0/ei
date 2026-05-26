get_terraform_dns() {
    terraform state show "module.$1.aws_instance.quarkus_instance" \
        | grep public_dns \
        | sed 's/public_dns//g' \
        | sed 's/=//g' \
        | sed 's/"//g' \
        | sed 's/ //g' \
        | sed "s/\e\[[0-9;]*m//g"
}

echo "[FETCHING ADDRESSES]..."

get_instance_dns() {
    terraform state show -no-color "$1" \
        | awk -F\" '/^    public_dns/ {print $2}'
}

export DB_ADDRESS=$(cd terraform/rds && terraform state show aws_db_instance.example | grep address | sed "s/address//g" | sed "s/=//g" | sed "s/\"//g" |sed "s/ //g" | sed "s/\e\[[0-9;]*m//g" )
export KAFKA_CLUSTER=$(cd terraform/kafka && terraform output -json publicdnslist | jq -r 'map("\(.):9092") | join(",")')
export OLLAMA_URL="$(cd terraform/microservices/ollama && get_instance_dns aws_instance.ollama_instance):11434"
export CAMUNDA_URL="$(cd terraform/camunda && get_instance_dns aws_instance.camunda_engine_instance):8080"
export KONG_URL="$(cd terraform/kong && get_instance_dns aws_instance.kong_instance):8000"
export KONG_ADMIN_URL="$(cd terraform/kong && get_instance_dns aws_instance.kong_instance):8001"
export KONGA_URL="$(cd terraform/konga && get_instance_dns aws_instance.konga_instance):1337"
export ASSET_URL="$(cd terraform/microservices/asset && get_terraform_dns "asset"):8080"
export ASSET_LINK_URL="$(cd terraform/microservices/asset_link && get_terraform_dns "assetlink"):8080"
export ENERGY_ANALYTICS_URL="$(cd terraform/microservices/energy_analytics && get_terraform_dns "energyanalytics"):8080"
export FLEXIBILITY_EMISSION_URL="$(cd terraform/microservices/flexibility_emission && get_terraform_dns "flexibilityemission"):8080"
export GRID_BALANCING_URL="$(cd terraform/microservices/grid_balancing && get_terraform_dns "gridbalancing"):8080"
export GRID_CELL_URL="$(cd terraform/microservices/grid_cell && get_terraform_dns "gridcell"):8080"
export PROSUMER_URL="$(cd terraform/microservices/prosumer && get_terraform_dns "prosumer"):8080"
export TELEMETRY_URL="$(cd terraform/microservices/telemetry && get_terraform_dns "telemetry"):8080"
export UTILITY_OPERATOR_URL="$(cd terraform/microservices/utility_operator && get_terraform_dns "utilityoperator"):8080"

echo "- Database: $DB_ADDRESS"
echo "- Kafka Cluster: $KAFKA_CLUSTER"
echo "- Ollama: http://$OLLAMA_URL"
echo "- Camunda: http://$CAMUNDA_URL/operate"
echo "- Kong: http://$KONG_URL"
echo "- Kong Admin: http://$KONG_ADMIN_URL"
echo "- Konga: http://$KONGA_URL"
echo "- Asset: http://$ASSET_URL"
echo "- AssetLink: http://$ASSET_LINK_URL"
echo "- EnergyAnalytics: http://$ENERGY_ANALYTICS_URL"
echo "- FlexibilityEmission: http://$FLEXIBILITY_EMISSION_URL"
echo "- GridBalancingRecommendation: http://$GRID_BALANCING_URL"
echo "- GridCell: http://$GRID_CELL_URL"
echo "- Prosumer: http://$PROSUMER_URL"
echo "- Telemetry: http://$TELEMETRY_URL"
echo "- UtilityOperator: http://$UTILITY_OPERATOR_URL"