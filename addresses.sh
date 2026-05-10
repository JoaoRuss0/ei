get_terraform_dns() {
    terraform state show aws_instance.quarkus_instance \
        | grep public_dns \
        | sed 's/public_dns//g' \
        | sed 's/=//g' \
        | sed 's/"//g' \
        | sed 's/ //g' \
        | sed "s/\e\[[0-9;]*m//g"
}

echo "- Database:" "$(cd terraform/rds && terraform state show aws_db_instance.example | grep address | sed "s/address//g" | sed "s/=//g" | sed "s/\"//g" |sed "s/ //g" | sed "s/\e\[[0-9;]*m//g" )"
echo "- Kafka Cluster:" "$(cd terraform/kafka && terraform output -json publicdnslist | jq -r 'map("\(.):9092") | join(",")')"
echo "- Ollama: http://""$(cd terraform/microservices/ollama && terraform state show -no-color aws_instance.ollama_instance | awk -F\" '/^    public_dns/ {print $2}' )"":11434/api/generate"

echo "- Asset: http://""$(cd terraform/microservices/asset && get_terraform_dns)"":8080/q/swagger-ui"
echo "- AssetLink: http://""$(cd terraform/microservices/asset_link && get_terraform_dns)"":8080/q/swagger-ui"
echo "- EnergyAnalytics: http://""$(cd terraform/microservices/energy_analytics && get_terraform_dns)"":8080/q/swagger-ui"
echo "- FlexibilityEmission: http://""$(cd terraform/microservices/flexibility_emission && get_terraform_dns)"":8080/q/swagger-ui"
echo "- GridBalancingRecommendation: http://""$(cd terraform/microservices/grid_balancing && get_terraform_dns)"":8080/q/swagger-ui"
echo "- GridCell: http://""$(cd terraform/microservices/grid_cell && get_terraform_dns)"":8080/q/swagger-ui"
echo "- Prosumer: http://""$(cd terraform/microservices/prosumer && get_terraform_dns)"":8080/q/swagger-ui"
echo "- Telemetry: http://""$(cd terraform/microservices/telemetry && get_terraform_dns)"":8080/q/swagger-ui"
echo "- UtilityOperator: http://""$(cd terraform/microservices/utility_operator && get_terraform_dns)"":8080/q/swagger-ui"
