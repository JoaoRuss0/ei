package org.acme;

import lombok.Getter;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import org.apache.kafka.common.errors.WakeupException;
import org.json.*;

@Getter
public class DynamicTopicConsumer extends Thread {
    private String topic;
    private Consumer<String, String> consumer;

    io.vertx.mutiny.mysqlclient.MySQLPool client;

    public DynamicTopicConsumer(String topic_received, String kafka_servers_received, io.vertx.mutiny.mysqlclient.MySQLPool client_received) {
        topic = topic_received;
        client = client_received;

        Properties properties = new Properties();
        properties.put("bootstrap.servers", kafka_servers_received);
        properties.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put("auto.offset.reset", "earliest");
        properties.put("group.id", topic_received);

        consumer = new KafkaConsumer<>(properties);
    }

    public DynamicTopicConsumer(String topic_received, Consumer<String, String> mock_consumer_received, io.vertx.mutiny.mysqlclient.MySQLPool client_received) {
        this.topic = topic_received;
        this.consumer = mock_consumer_received;
        this.client = client_received;
    }

    public void run() {
        try {
            consumer.subscribe(Collections.singletonList(topic));

            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, String> record : records) {

                    System.out.printf("topic = %s, partition = %s, offset = %d,key = %s, value = %s\n",
                            record.topic(), record.partition(), record.offset(),
                            record.key(), record.value());

                    String jsonString = record.value();
                    JSONObject obj = new JSONObject(jsonString);
                    String timeStamp = obj.getString("timeStamp");
                    String asset_type = obj.getString("asset_type");
                    String asset_id = obj.getString("asset_id");
                    String grid_cell_id = obj.getString("grid_cell_id");

                    switch (asset_type) {
                        case "BATTERY" -> {
                            Float soc_percent = obj.getJSONObject("payload").getFloat("soc_percent");
                            Float energy_available_kwh = obj.getJSONObject("payload").getFloat("energy_available_kwh");
                            Float active_power_kw = obj.getJSONObject("payload").getFloat("active_power_kw");
                            Float max_discharge_power_kw = obj.getJSONObject("payload").getFloat("max_discharge_power_kw");
                            Float soh_percent = obj.getJSONObject("payload").getFloat("soh_percent");
                            String connection_status = obj.getJSONObject("payload").getString("connection_status");

                            String query = "INSERT INTO Telemetry (timeStamp, asset_id, asset_type, grid_cell_id, State_of_Charge, Available_Energy, Current_Output, Max_Capacity, State_of_Health, Status) VALUES ("
                                    + "'" + timeStamp + "',"
                                    + "'" + asset_id + "',"
                                    + "'" + asset_type + "',"
                                    + "'" + grid_cell_id + "',"
                                    + "'" + soc_percent + "',"
                                    + "'" + energy_available_kwh + "',"
                                    + "'" + active_power_kw + "',"
                                    + "'" + max_discharge_power_kw + "',"
                                    + "'" + soh_percent + "',"
                                    + "'" + connection_status + "'"
                                    + ")";

                            client.query(query).execute().await().indefinitely();

                        }
                        case "SOLAR" -> {
                            Float generation_kw = obj.getJSONObject("payload").getFloat("generation_kw");
                            Float daily_yield_kwh = obj.getJSONObject("payload").getFloat("daily_yield_kwh");
                            Float ac_voltage_v = obj.getJSONObject("payload").getFloat("ac_voltage_v");
                            Float grid_frequency_hz = obj.getJSONObject("payload").getFloat("grid_frequency_hz");

                            String query = "INSERT INTO Telemetry (timeStamp, asset_id, asset_type, grid_cell_id, Current_Generation, Daily_Total, Grid_Voltage, Frequency) VALUES ("
                                    + "'" + timeStamp + "',"
                                    + "'" + asset_id + "',"
                                    + "'" + asset_type + "',"
                                    + "'" + grid_cell_id + "',"
                                    + "'" + generation_kw + "',"
                                    + "'" + daily_yield_kwh + "',"
                                    + "'" + ac_voltage_v + "',"
                                    + "'" + grid_frequency_hz + "'"
                                    + ")";

                            client.query(query).execute().await().indefinitely();

                        }
                        case "EV_CHARGER" -> {
                            String connector_status = obj.getJSONObject("payload").getString("connector_status");
                            Float charging_power_kw = obj.getJSONObject("payload").getFloat("charging_power_kw");
                            Float session_energy_kwh = obj.getJSONObject("payload").getFloat("session_energy_kwh");
                            Float ev_soc_percent = obj.getJSONObject("payload").getFloat("ev_soc_percent");

                            String query = "INSERT INTO Telemetry (timeStamp, asset_id, asset_type, grid_cell_id, Plug_Status, Charging_Rate, Session_Energy, EV_SoC) VALUES ("
                                    + "'" + timeStamp + "',"
                                    + "'" + asset_id + "',"
                                    + "'" + asset_type + "',"
                                    + "'" + grid_cell_id + "',"
                                    + "'" + connector_status + "',"
                                    + "'" + charging_power_kw + "',"
                                    + "'" + session_energy_kwh + "',"
                                    + "'" + ev_soc_percent + "'"
                                    + ")";

                            client.query(query).execute().await().indefinitely();
                        }
                    }
                }
            }
        } catch (WakeupException e){
            System.out.println("Exiting on wakeup...");
        } catch (Exception e) {
            System.err.println("Exception is caught:" + e);
        } finally {
            consumer.close();
        }
    }
}
