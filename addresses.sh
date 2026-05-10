get_terraform_dns() {
    terraform state show aws_instance.quarkus_instance \
        | grep public_dns \
        | sed 's/public_dns//g' \
        | sed 's/=//g' \
        | sed 's/"//g' \
        | sed 's/ //g' \
        | sed "s/\e\[[0-9;]*m//g"
}

echo "[FETCHING ADDRESSES]..."

export DB_ADDRESS=$(cd terraform/rds && terraform state show aws_db_instance.example | grep address | sed "s/address//g" | sed "s/=//g" | sed "s/\"//g" |sed "s/ //g" | sed "s/\e\[[0-9;]*m//g" )
export KAFKA_CLUSTER=$(cd terraform/kafka && terraform output -json publicdnslist | jq -r 'map("\(.):9092") | join(",")')
export OLLAMA_URL="$(cd terraform/microservices/ollama && terraform state show -no-color aws_instance.ollama_instance | awk -F\" '/^    public_dns/ {print $2}'):11434"
export ASSET_URL="$(cd terraform/microservices/asset && get_terraform_dns):8080"
export ASSET_LINK_URL="$(cd terraform/microservices/asset_link && get_terraform_dns):8080"
export ENERGY_ANALYTICS_URL="$(cd terraform/microservices/energy_analytics && get_terraform_dns):8080"
export FLEXIBILITY_EMISSION_URL="$(cd terraform/microservices/flexibility_emission && get_terraform_dns):8080"
export GRID_BALANCING_URL="$(cd terraform/microservices/grid_balancing && get_terraform_dns):8080"
export GRID_CELL_URL="$(cd terraform/microservices/grid_cell && get_terraform_dns):8080"
export PROSUMER_URL="$(cd terraform/microservices/prosumer && get_terraform_dns):8080"
export TELEMETRY_URL="$(cd terraform/microservices/telemetry && get_terraform_dns):8080"
export UTILITY_OPERATOR_URL="$(cd terraform/microservices/utility_operator && get_terraform_dns):8080"

echo "- Database: $DB_ADDRESS"
echo "- Kafka Cluster: $KAFKA_CLUSTER"
echo "- Ollama: http://$OLLAMA_URL"
echo "- Asset: http://$ASSET_URL"
echo "- AssetLink: http://$ASSET_LINK_URL"
echo "- EnergyAnalytics: http://$ENERGY_ANALYTICS_URL"
echo "- FlexibilityEmission: http://$FLEXIBILITY_EMISSION_URL"
echo "- GridBalancingRecommendation: http://$GRID_BALANCING_URL"
echo "- GridCell: http://$GRID_CELL_URL"
echo "- Prosumer: http://$PROSUMER_URL"
echo "- Telemetry: http://$TELEMETRY_URL"
echo "- UtilityOperator: http://$UTILITY_OPERATOR_URL"