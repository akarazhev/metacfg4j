import com.github.akarazhev.metaconfig.api.MetaConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

import static com.github.akarazhev.metaconfig.Constants.Settings.DB_DIALECT;
import static com.github.akarazhev.metaconfig.Constants.Settings.POSTGRE;

public class Main {

    public static void main(String[] args) {
        final MetaConfig metaConfig = metaConfig();
        System.out.println(metaConfig.getNames().count());
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

    private static Map<String, Object> dbSettings() {
        final Map<String, Object> settings = new HashMap<>();
        settings.put(DB_DIALECT, POSTGRE);
        return settings;
    }

    public static MetaConfig metaConfig() {

        return new MetaConfig.Builder().dataSource(dataSource()).dbSettings(dbSettings()).build();
    }
}
