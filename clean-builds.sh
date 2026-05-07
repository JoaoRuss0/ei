find . -name ".terraform" -exec rm -r .terraform '{}' \;

cd microservices/Telemetry	
mvn clean
cd ../..

cd microservices/Prosumer	
mvn clean
cd ../..

cd microservices/AssetLink	
mvn clean
cd ../..

cd microservices/UtilityOperator
mvn clean
cd ../..

cd microservices/Asset
mvn clean
cd ../..

cd microservices/GridCell
mvn clean
cd ../..