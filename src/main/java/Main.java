import static com.github.akarazhev.metaconfig.Constants.Settings.DB_DIALECT;
import static com.github.akarazhev.metaconfig.Constants.Settings.FETCH_SIZE;
import static com.github.akarazhev.metaconfig.Constants.Settings.POSTGRE;

import com.github.akarazhev.metaconfig.Constants;
import com.github.akarazhev.metaconfig.api.Config;
import com.github.akarazhev.metaconfig.api.MetaConfig;
import com.github.akarazhev.metaconfig.api.Property;
import com.github.akarazhev.metaconfig.engine.web.server.Server;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        final MetaConfig metaConfig = metaConfig();
        System.out.println(metaConfig.getNames().count());
        System.in.read();
    }

    private static DataSource dataSource() {
        final HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName("org.postgresql.ds.PGSimpleDataSource");
        dataSource.setJdbcUrl("jdbc:postgresql://localhost:5432/postgres");
        dataSource.setMaximumPoolSize(Runtime.getRuntime().availableProcessors() * 2 + 1);
        dataSource.addDataSourceProperty("user", "postgres2");
        dataSource.addDataSourceProperty("password", "root");
        return dataSource;
    }

    private static DataSource getDataSource() {
        final HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setJdbcUrl("jdbc:h2:mem:test");
        dataSource.setUsername("SA");
        dataSource.setPassword("");
        return dataSource;
    }

    private static Map<String, Object> dbSettings() {
        final Map<String, Object> settings = new HashMap<>();
        settings.put(DB_DIALECT, POSTGRE);
        return settings;
    }

    public static MetaConfig metaConfig() {

        final HashMap<String, String> dataMapping=new HashMap<String, String>();
        dataMapping.put(Constants.Mapping.CONFIGS_TABLE,"CONFIGS");
        dataMapping.put(Constants.Mapping.CONFIG_ATTRIBUTES_TABLE,"CONFIG_ATTRIBUTES");
        dataMapping.put(Constants.Mapping.PROPERTIES_TABLE,"PROPERTIES");
        dataMapping.put(Constants.Mapping.PROPERTY_ATTRIBUTES_TABLE,"PROPERTY_ATTRIBUTES");
        // Set a fetch size
        final HashMap<String, Object> settings= new HashMap<>();
        settings.put(FETCH_SIZE,100);
        settings.put(DB_DIALECT, POSTGRE);
        // Create the web server config
        final Config webServer=new Config.Builder(Server.Settings.CONFIG_NAME,
                                                     Arrays.asList(
                                                 new Property.Builder(Server.Settings.HOSTNAME, "localhost").build(),
                                                 new Property.Builder(Constants.Endpoints.ACCEPT_CONFIG, "accept_config").build(),
                                                 new Property.Builder(Constants.Endpoints.CONFIG_NAMES,"config_names").build(),
                                                 new Property.Builder(Constants.Endpoints.CONFIG,"config").build(),
                                                 new Property.Builder(Server.Settings.PORT,8000).build(),
                                                 new Property.Builder(Server.Settings.BACKLOG,0).build(),
                                                 new Property.Builder(Server.Settings.KEY_STORE_FILE,"./data/metacfg4j.keystore").build(),
                                                 new Property.Builder(Server.Settings.ALIAS,"alias").build(),
                                                 new Property.Builder(Server.Settings.STORE_PASSWORD,"password").build(),
                                                 new Property.Builder(Server.Settings.KEY_PASSWORD,"password").build())
        ).build();
        // Create the meta configuration
        return new MetaConfig.Builder().
          webServer(webServer).
          dataSource(dataSource()).
          dataMapping(dataMapping).
          dbSettings(settings).
          build();
    }
}
