

import com.hazelcast.config.Config;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.instance.HazelcastInstanceFactory;
import com.hazelcast.spi.properties.GroupProperty;

/**
 * Hazelcast Hello world!
 */
public class App {

    public static void main(String[] args) {
        System.setProperty("java.net.preferIPv4Stack", "true");
        Config config = new Config();
        
        NetworkConfig networkConfig = config.getNetworkConfig();
        networkConfig.getInterfaces().addInterface("127.0.0.1");
        config.setProperty(GroupProperty.KERBEROS_ENABLED.getName(), "true");
        networkConfig.getJoin().getMulticastConfig().setEnabled(false);
        networkConfig.getJoin().getTcpIpConfig().setEnabled(true).addMember("127.0.0.1");
        try {
            Hazelcast.newHazelcastInstance(config);
            config.setProperty(GroupProperty.KERBEROS_SERVERNAME.getName(), "server2");
            Hazelcast.newHazelcastInstance(config);
        } finally {
            HazelcastInstanceFactory.terminateAll();
        }
    }
}
