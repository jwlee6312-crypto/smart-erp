package com.crmbank.erp.config;

import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import com.crmbank.erp.global.handler.MapKeyLowerWrapperFactory;
import com.zaxxer.hikari.HikariDataSource;

@Slf4j
@Configuration
@MapperScan(
    basePackages = "com.crmbank.erp",
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com\\.crmbank\\.erp\\.(asterisk|hgpa)\\..*"
    ),
    sqlSessionFactoryRef = "erpSqlSessionFactory"
)
public class ErpDataSourceConfig {

    @Autowired
    private Environment env;

    @Bean(name = "erpDataSource")
    @Primary
    public DataSource erpDataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        
        // 🚀 [쾌속 기동 설정]
        String host = env.getProperty("ERP_DB_HOST", "127.0.0.1");
        String port = env.getProperty("ERP_DB_PORT", "1433");
        String dbName = env.getProperty("ERP_DB_NAME", "smartdb");
        String username = env.getProperty("ERP_DB_USERNAME", "sa");
        String password = env.getProperty("ERP_DB_PASSWORD", "crmbank");
        
        String jdbcUrl = String.format(
                "jdbc:sqlserver://%s:%s;databaseName=%s;encrypt=false;trustServerCertificate=true;sendStringParametersAsUnicode=false;loginTimeout=5",
                host, port, dbName);
        
        log.info("🔌 [ERP DB Direct Connect]: {}", jdbcUrl);
        
        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        
        // 💡 [지연 시간 단축]
        dataSource.setConnectionTimeout(3000); 
        dataSource.setValidationTimeout(1000);
        dataSource.setPoolName("ErpPool");
        dataSource.setMaximumPoolSize(10);
        
        return dataSource;
    }

    @Bean(name = "erpJdbcTemplate")
    public JdbcTemplate erpJdbcTemplate(@Qualifier("erpDataSource") DataSource erpDataSource) {
        return new JdbcTemplate(erpDataSource);
    }

    @Bean(name = "erpSqlSessionFactory")
    @Primary
    public SqlSessionFactory erpSqlSessionFactory(@Qualifier("erpDataSource") DataSource dataSource) throws Exception {
        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        sessionFactory.setMapperLocations(resolver.getResources("classpath*:/mapper/**/*.xml"));
        
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setCallSettersOnNulls(true);
        configuration.setObjectWrapperFactory(new MapKeyLowerWrapperFactory());
        sessionFactory.setConfiguration(configuration);
        return sessionFactory.getObject();
    }

    @Bean(name = "erpSqlSessionTemplate")
    @Primary
    public SqlSessionTemplate erpSqlSessionTemplate(@Qualifier("erpSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Bean(name = "erpTransactionManager")
    @Primary
    public PlatformTransactionManager erpTransactionManager(@Qualifier("erpDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
