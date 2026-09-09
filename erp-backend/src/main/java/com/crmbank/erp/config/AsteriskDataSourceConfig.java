package com.crmbank.erp.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import com.crmbank.erp.global.handler.MapKeyLowerWrapperFactory;

import javax.sql.DataSource;

@Slf4j
@Configuration
@MapperScan(
    basePackages = {
        "com.crmbank.erp.asterisk.mapper",
        "com.crmbank.erp.hgpa.mapper"
    },
    sqlSessionFactoryRef = "asteriskSqlSessionFactory"
)
public class AsteriskDataSourceConfig {

    @Autowired
    private Environment env;

    @Bean(name = "asteriskDataSource")
    public DataSource asteriskDataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        
        // 🚀 [쾌속 기동 설정] 내부망 주소 우선 사용
        String host = env.getProperty("ASTERISK_DB_HOST", "127.0.0.1");
        String port = env.getProperty("ASTERISK_DB_PORT", "3306");
        String dbName = env.getProperty("ASTERISK_DB_NAME", "asterisk");
        String username = env.getProperty("ASTERISK_DB_USERNAME", "root");
        String password = env.getProperty("ASTERISK_DB_PASSWORD", "gkdldhs12#$");
        
        String url = String.format("jdbc:mysql://%s:%s/%s?serverTimezone=Asia/Seoul&useSSL=false&allowPublicKeyRetrieval=true",
                                   host, port, dbName);
        
        log.info("🔌 [Asterisk DB Direct Connect]: {}", url);
        
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        
        // 💡 [지연 시간 단축] 연결이 안 되면 빠르게 포기하고 재시도하게 설정
        dataSource.setConnectionTimeout(3000); // 3초 (기존 30초)
        dataSource.setValidationTimeout(1000);
        dataSource.setPoolName("AsteriskPool");
        dataSource.setMaximumPoolSize(5);
        
        return dataSource;
    }

    @Bean(name = "asteriskSqlSessionFactory")
    public SqlSessionFactory asteriskSqlSessionFactory(@Qualifier("asteriskDataSource") DataSource dataSource) throws Exception {
        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        sessionFactory.setMapperLocations(resolver.getResources("classpath*:/com/crmbank/erp/asterisk/**/*.xml"));
        
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setCallSettersOnNulls(true);
        configuration.setObjectWrapperFactory(new MapKeyLowerWrapperFactory());
        sessionFactory.setConfiguration(configuration);

        return sessionFactory.getObject();
    }

    @Bean(name = "asteriskSqlSessionTemplate")
    public SqlSessionTemplate asteriskSqlSessionTemplate(@Qualifier("asteriskSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Bean(name = "asteriskTransactionManager")
    public PlatformTransactionManager asteriskTransactionManager(@Qualifier("asteriskDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
