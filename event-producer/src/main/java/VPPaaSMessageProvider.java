import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Map;
import java.util.List;
import java.util.Iterator;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.PartitionInfo;


public class VPPaaSMessageProvider {

    static String DEFAULT_BROKER_LIST_ADDRESS = "localhost:9092";
    static int DEFAULT_THROUGHPUT = 10;
    static String DEFAULT_FILTER_PREFIX = "";
    static String DEFAULT_ASSET_ID = null;
    static String DEFAULT_GRID_CELL_ID = null;
    static String DEFAULT_TOPIC = null;
    static String DEFAULT_ASSET_TYPE = null;       // BATTERY | SOLAR | EV; null = random
    static Double DEFAULT_SOC = null;              // null = random
    static Double DEFAULT_CURRENT_OUTPUT = null;   // kW; BATTERY only; +discharge / -charge; null = random.
                                                   // If --status is not also set, Status is forced to ONLINE so
                                                   // grid-balancing actually uses the value.
    static String DEFAULT_STATUS = null;           // BATTERY only; ONLINE | OFFLINE | FAULT | MAINTENANCE; null = random
    static Double DEFAULT_CHARGING_RATE = null;    // kW; EV only; +charging draws from grid; null = random.
                                                   // If --plug-status is not also set, PlugStatus is forced to CHARGING
                                                   // so grid-balancing actually uses the value.
    static String DEFAULT_PLUG_STATUS = null;      // EV only; AVAILABLE | OCCUPIED | CHARGING | FAULTED; null = random
    static Double DEFAULT_CURRENT_GENERATION = null; // kW; SOLAR only; positive = generating; null = random.
                                                     // Note: grid-balancing treats SOLAR as -Current_Generation
                                                     // (it lowers load) — cannot trigger overload on its own.
    static String DEFAULT_TIMESTAMP = null;        // ISO e.g. 2026-05-27T10:30:00; null = now-per-message

    static Map<String, List<PartitionInfo>> topics;

    private static String RandomTopic() {
        if (DEFAULT_TOPIC != null) return DEFAULT_TOPIC;
        String Topic = new String("");
        int index = (new Random()).nextInt(topics.size());
        Set<String> keys = topics.keySet();
        Iterator<String> it = keys.iterator();
        for (int idx = 0; idx < index; idx++) it.next();
        Topic = (String) it.next();
        System.out.println("Topic randomized: " + Topic);
        return Topic;
    }

    private static Message CreateMessage(String topicToSend, Timestamp ts) {
        if (!topicToSend.contains("-")) return null;

        String[] GridCells = {
                "TOKYO-NW", "BERLIN-CE", "AUSTIN-DT", "LONDON-SE", "MUMBAI-WP",
                "SYDNEY-HB", "MADRID-NE", "DENVER-MT", "SEOUL-IT", "LAGOS-VI",
                "PARIS-RG", "MEXICO-SZ", "DUBAI-MR", "CAIRO-ND", "LISBON-EX",
                "TORONTO-QU", "VIENNA-BZ", "SANTOS-IN", "OSLO-FJ", "SINGA-GP"};

        String assetId = DEFAULT_ASSET_ID != null
                ? DEFAULT_ASSET_ID
                : topicToSend.substring(0, topicToSend.indexOf('-'));

        String gridCellId = DEFAULT_GRID_CELL_ID != null
                ? DEFAULT_GRID_CELL_ID
                : GridCells[new Random().nextInt(GridCells.length)];

        int typeIndex;
        if (DEFAULT_ASSET_TYPE != null) {
            switch (DEFAULT_ASSET_TYPE) {
                case "BATTERY": typeIndex = 0; break;
                case "SOLAR":   typeIndex = 1; break;
                case "EV":      typeIndex = 2; break;
                default:
                    System.out.println("Unknown --asset-type, falling back to random: " + DEFAULT_ASSET_TYPE);
                    typeIndex = ThreadLocalRandom.current().nextInt(0, 3);
            }
        } else {
            typeIndex = ThreadLocalRandom.current().nextInt(0, 3);
        }

        Message newMessage = null;

        switch (typeIndex) {
            case 0:
                double soc = DEFAULT_SOC != null
                        ? DEFAULT_SOC
                        : ThreadLocalRandom.current().nextDouble(0.0, 100.0);
                double currentOutput = DEFAULT_CURRENT_OUTPUT != null
                        ? DEFAULT_CURRENT_OUTPUT
                        : ThreadLocalRandom.current().nextDouble(-10.0, 10.0);
                // Resolution order for Status:
                //   1. explicit --status
                //   2. ONLINE if --current-output is set (otherwise the value is silently ignored
                //      by grid-balancing, which short-circuits on non-ONLINE batteries)
                //   3. random
                BatteryEnergyStorage.Level status;
                if (DEFAULT_STATUS != null) {
                    try {
                        status = BatteryEnergyStorage.Level.valueOf(DEFAULT_STATUS);
                    } catch (IllegalArgumentException e) {
                        System.out.println("Unknown --status, falling back to random: " + DEFAULT_STATUS);
                        status = new BatteryEnergyStorage().randomLevel();
                    }
                } else if (DEFAULT_CURRENT_OUTPUT != null) {
                    status = BatteryEnergyStorage.Level.ONLINE;
                } else {
                    status = new BatteryEnergyStorage().randomLevel();
                }
                newMessage = new BatteryEnergyStorage(ts.toLocalDateTime(),
                        assetId,
                        gridCellId,
                        soc,
                        ThreadLocalRandom.current().nextDouble(0.0, 20.0),
                        currentOutput,
                        ThreadLocalRandom.current().nextDouble(0.0, 5.0),
                        ThreadLocalRandom.current().nextDouble(0.0, 100.0),
                        status);
                break;
            case 1:
                double currentGeneration = DEFAULT_CURRENT_GENERATION != null
                        ? DEFAULT_CURRENT_GENERATION
                        : ThreadLocalRandom.current().nextDouble(0.0, 7.5);
                newMessage = new SolarInverter(ts.toLocalDateTime(),
                        assetId,
                        gridCellId,
                        currentGeneration,
                        ThreadLocalRandom.current().nextDouble(0.0, 150),
                        ThreadLocalRandom.current().nextDouble(245, 255),
                        ThreadLocalRandom.current().nextDouble(49.5, 50.5));
                break;
            case 2:
                double chargingRate = DEFAULT_CHARGING_RATE != null
                        ? DEFAULT_CHARGING_RATE
                        : ThreadLocalRandom.current().nextDouble(0.0, 20.5);
                // Resolution order for PlugStatus:
                //   1. explicit --plug-status
                //   2. CHARGING if --charging-rate is set (otherwise the value is silently ignored
                //      by grid-balancing, which short-circuits on non-CHARGING EVs)
                //   3. random
                EVCharger.Level plugStatus;
                if (DEFAULT_PLUG_STATUS != null) {
                    try {
                        plugStatus = EVCharger.Level.valueOf(DEFAULT_PLUG_STATUS);
                    } catch (IllegalArgumentException e) {
                        System.out.println("Unknown --plug-status, falling back to random: " + DEFAULT_PLUG_STATUS);
                        plugStatus = new EVCharger().randomLevel();
                    }
                } else if (DEFAULT_CHARGING_RATE != null) {
                    plugStatus = EVCharger.Level.CHARGING;
                } else {
                    plugStatus = new EVCharger().randomLevel();
                }
                newMessage = new EVCharger(ts.toLocalDateTime(),
                        assetId,
                        gridCellId,
                        chargingRate,
                        ThreadLocalRandom.current().nextDouble(0.0, 17.8),
                        ThreadLocalRandom.current().nextDouble(0, 100),
                        plugStatus);
                break;
        }
        return newMessage;
    }

    private static void CheckArguments() {
        System.out.println(
                "--broker-list=" + DEFAULT_BROKER_LIST_ADDRESS + "\n" +
                        "--throughput=" + DEFAULT_THROUGHPUT + "\n" +
                        "--filterprefix=" + DEFAULT_FILTER_PREFIX + "\n" +
                        "--asset-id=" + (DEFAULT_ASSET_ID == null ? "(random from topic)" : DEFAULT_ASSET_ID) + "\n" +
                        "--grid-cell-id=" + (DEFAULT_GRID_CELL_ID == null ? "(random)" : DEFAULT_GRID_CELL_ID) + "\n" +
                        "--topic=" + (DEFAULT_TOPIC == null ? "(random discovered)" : DEFAULT_TOPIC) + "\n" +
                        "--asset-type=" + (DEFAULT_ASSET_TYPE == null ? "(random)" : DEFAULT_ASSET_TYPE) + "\n" +
                        "--soc=" + (DEFAULT_SOC == null ? "(random)" : DEFAULT_SOC.toString()) + "\n" +
                        "--current-output=" + (DEFAULT_CURRENT_OUTPUT == null ? "(random)" : DEFAULT_CURRENT_OUTPUT.toString() + " kW") + "\n" +
                        "--status=" + (DEFAULT_STATUS == null
                                ? (DEFAULT_CURRENT_OUTPUT == null ? "(random)" : "ONLINE (auto, because --current-output is set)")
                                : DEFAULT_STATUS) + "\n" +
                        "--charging-rate=" + (DEFAULT_CHARGING_RATE == null ? "(random)" : DEFAULT_CHARGING_RATE.toString() + " kW") + "\n" +
                        "--plug-status=" + (DEFAULT_PLUG_STATUS == null
                                ? (DEFAULT_CHARGING_RATE == null ? "(random)" : "CHARGING (auto, because --charging-rate is set)")
                                : DEFAULT_PLUG_STATUS) + "\n" +
                        "--current-generation=" + (DEFAULT_CURRENT_GENERATION == null ? "(random)" : DEFAULT_CURRENT_GENERATION.toString() + " kW") + "\n" +
                        "--timestamp=" + (DEFAULT_TIMESTAMP == null ? "(now per message)" : DEFAULT_TIMESTAMP));
    }

    private static boolean VerifyArgs(String[] cabecalho) {
        for (int i = 0; i < cabecalho.length; i = i + 2) {
            if (cabecalho[i].compareTo("--broker-list") == 0) DEFAULT_BROKER_LIST_ADDRESS = cabecalho[i + 1];
            else if (cabecalho[i].compareTo("--throughput") == 0) DEFAULT_THROUGHPUT = Integer.valueOf(cabecalho[i + 1]).intValue();
            else if (cabecalho[i].compareTo("--filterprefix") == 0) DEFAULT_FILTER_PREFIX = cabecalho[i + 1];
            else if (cabecalho[i].compareTo("--asset-id") == 0) DEFAULT_ASSET_ID = cabecalho[i + 1];
            else if (cabecalho[i].compareTo("--grid-cell-id") == 0) DEFAULT_GRID_CELL_ID = cabecalho[i + 1];
            else if (cabecalho[i].compareTo("--topic") == 0) DEFAULT_TOPIC = cabecalho[i + 1];
            else if (cabecalho[i].compareTo("--asset-type") == 0) DEFAULT_ASSET_TYPE = cabecalho[i + 1].toUpperCase();
            else if (cabecalho[i].compareTo("--soc") == 0) DEFAULT_SOC = Double.valueOf(cabecalho[i + 1]);
            else if (cabecalho[i].compareTo("--current-output") == 0) DEFAULT_CURRENT_OUTPUT = Double.valueOf(cabecalho[i + 1]);
            else if (cabecalho[i].compareTo("--status") == 0) DEFAULT_STATUS = cabecalho[i + 1].toUpperCase();
            else if (cabecalho[i].compareTo("--charging-rate") == 0) DEFAULT_CHARGING_RATE = Double.valueOf(cabecalho[i + 1]);
            else if (cabecalho[i].compareTo("--plug-status") == 0) DEFAULT_PLUG_STATUS = cabecalho[i + 1].toUpperCase();
            else if (cabecalho[i].compareTo("--current-generation") == 0) DEFAULT_CURRENT_GENERATION = Double.valueOf(cabecalho[i + 1]);
            else if (cabecalho[i].compareTo("--timestamp") == 0) DEFAULT_TIMESTAMP = cabecalho[i + 1];
            else {
                System.out.println("Bad argument name: " + cabecalho[i]);
                return false;
            }
        }

        if (DEFAULT_BROKER_LIST_ADDRESS.length() == 0) System.out.println("Broker-list argument is mandatory!");
        else return true;

        return false;
    }

    private static void SendMessage(Message msg, KafkaProducer<String, String> prd, String topicTarget) {
        System.out.println("This is the message to send = " + msg.toStringAsJSON());
        String seqkey = msg.getSeqkey().toString();
        System.out.println("Sending new message to Kafka, to the topic = " + topicTarget + ", with key=" + seqkey);
        ProducerRecord<String, String> record = new ProducerRecord<>(topicTarget, seqkey, msg.toStringAsJSON());
        prd.send(record);
        System.out.print("Sent...Fire-and-forget stopped...");
    }

    private static void CheckTopicsAvailable() {
        Properties props = new Properties();
        props.put("bootstrap.servers", DEFAULT_BROKER_LIST_ADDRESS);
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        topics = consumer.listTopics();
        consumer.close();

        topics.remove("__consumer_offsets");
        System.out.print("Topics discovered = { ");
        for (String topicName : topics.keySet()) System.out.print(topicName + " ; ");
        System.out.println(" } ");
    }

    public static void main(String[] args) {

        String usage = "The usage of the Message Producer for VPPaaS 2026, for Enterprise Integration 2026 course, is the following.\n\n" +
                "VPPaaSSimulator --broker-list <brokers> --throughput <value> --filterprefix <value> [--asset-id <id>] [--grid-cell-id <id>] [--topic <topic>] [--asset-type BATTERY|SOLAR|EV] [--soc <value>] [--current-output <value>] [--status ONLINE|OFFLINE|FAULT|MAINTENANCE] [--charging-rate <value>] [--plug-status AVAILABLE|OCCUPIED|CHARGING|FAULTED] [--current-generation <value>] [--timestamp <iso>]\n\n" +
                "--broker-list: broker list with ports (default localhost:9092)\n" +
                "--throughput: messages per minute (default 10)\n" +
                "--filterprefix: only topics starting with this prefix are used\n" +
                "--asset-id: force this asset_id for every message (default: derived from topic)\n" +
                "--grid-cell-id: force this grid_cell_id for every message (default: random from list)\n" +
                "--topic: force this single topic, bypassing discovery / random selection\n" +
                "--asset-type: force BATTERY | SOLAR | EV (default: random)\n" +
                "--soc: force State of Charge value (only applies to BATTERY; default: random)\n" +
                "--current-output: force battery active power in kW (only applies to BATTERY; positive = discharging to grid, negative = charging from grid). Grid-balancing reads this. If --status is not also set, Status is forced to ONLINE so the value is actually counted.\n" +
                "--status: force battery connection status: ONLINE | OFFLINE | FAULT | MAINTENANCE (only applies to BATTERY; default: random, or ONLINE when --current-output is set without --status)\n" +
                "--charging-rate: force EV charging power in kW (only applies to EV; positive = drawing from grid). Grid-balancing reads this. If --plug-status is not also set, PlugStatus is forced to CHARGING so the value is actually counted.\n" +
                "--plug-status: force EV plug status: AVAILABLE | OCCUPIED | CHARGING | FAULTED (only applies to EV; default: random, or CHARGING when --charging-rate is set without --plug-status)\n" +
                "--current-generation: force solar generation in kW (only applies to SOLAR). Grid-balancing reads this as -Current_Generation, so it can only LOWER load — SOLAR cannot trigger an overload on its own.\n" +
                "--timestamp: force this ISO timestamp for every message, e.g. 2026-05-27T10:30:00 (default: current time per message)\n";

        Properties kafkaProps = new Properties();
        if (args.length == 0) System.out.println(usage);
        else {
            if (VerifyArgs(args)) {
                System.out.println("The following arguments are accepted:");
                CheckArguments();
                System.out.println("------- Processing starting -------");

                kafkaProps.put("bootstrap.servers", DEFAULT_BROKER_LIST_ADDRESS);
                kafkaProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
                kafkaProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
                KafkaProducer<String, String> producer = new KafkaProducer<>(kafkaProps);

                if (DEFAULT_TOPIC == null) CheckTopicsAvailable();

                Timestamp mili;

                while (true) {
                    try {
                        mili = DEFAULT_TIMESTAMP != null
                                ? Timestamp.valueOf(LocalDateTime.parse(DEFAULT_TIMESTAMP))
                                : new Timestamp(System.currentTimeMillis());

                        if (DEFAULT_TOPIC != null || !topics.isEmpty()) {
                            String topic_to_send = RandomTopic();

                            if (topic_to_send.startsWith(DEFAULT_FILTER_PREFIX)) {
                                Message messageToSend = CreateMessage(topic_to_send, mili);
                                if (messageToSend != null) SendMessage(messageToSend, producer, topic_to_send);
                            } else
                                System.out.println("Topic = " + topic_to_send + " has been filtered. Therefore, not sending message.");
                        } else
                            System.out.println("Empty list of Topics. Therefore, no message to send.");

                        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                        System.out.println("...Time spent for sending: " + (timestamp.getTime() - mili.getTime()));
                        Thread.sleep(60000 / DEFAULT_THROUGHPUT);
                        if (DEFAULT_TOPIC == null) CheckTopicsAvailable();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else
                System.out.println("Application Arguments bad usage.\n\nPlease check syntax.\n\n" + usage);
        }
    }
}