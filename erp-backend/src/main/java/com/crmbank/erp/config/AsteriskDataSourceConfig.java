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
/**
 * 💡 Asterisk (MySQL) 데이터 소스 설정
 */
@MapperScan(
    basePackages = "com.crmbank.erp.asterisk.mapper",
    sqlSessionFactoryRef = "asteriskSqlSessionFactory"
)
public class AsteriskDataSourceConfig {

    @Autowired
    private Environment env;

    @Bean(name = "asteriskDataSource")
    public DataSource asteriskDataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        
        String defaultUrl = "jdbc:mysql://localhost:3306/asterisk?serverTimezone=Asia/Seoul&useSSL=false&allowPublicKeyRetrieval=true";
        String url = env.getProperty("ASTERISK_DB_URL", defaultUrl);
        
        log.info("🔌 [Asterisk MySQL 연동] 접속 시도: {}", url);
        
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(env.getProperty("ASTERISK_DB_USERNAME", "root"));
        dataSource.setPassword(env.getProperty("ASTERISK_DB_PASSWORD", "gkdldhs12#$"));
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
        configuration.setMapUnderscoreToCamelCase(false); 
        
        // 💡 [소문자 표준화] 모든 Map 결과의 Key를 강제로 소문자로 변환하는 Factory 등록
        configuration.setObjectWrapperFactory(new MapKeyLowerWrapperFactory());

        sessionFactory.setConfiguration(configuration);

        return sessionFactory.getObject();
    }

    @Bean(name = "asteriskSqlSessionTemplate")
    public SqlSessionTemplate asteriskSqlSessionTemplate(
            @Qualifier("asteriskSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Bean(name = "asteriskTransactionManager")
    public PlatformTransactionManager asteriskTransactionManager(@Qualifier("asteriskDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
