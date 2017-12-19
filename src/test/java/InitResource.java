
import org.exemodel.session.impl.JdbcSessionFactory;
import org.exemodel.cache.impl.RedisTemplate;
import com.alibaba.druid.pool.DruidDataSource;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Created by zp on 16/9/27.
 */
public class InitResource  {

    public static DruidDataSource dataSource;

    public InitResource() {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxIdle(100);
        jedisPoolConfig.setMaxTotal(300);
        jedisPoolConfig.setTestOnBorrow(true);
        RedisTemplate redisTemplate = new RedisTemplate(jedisPoolConfig, "192.168.1.100", 6378, 2000, "Superid123",0,false);

        dataSource = new DruidDataSource();
        dataSource.setDriverClassName("com.mysql.jdbc.Driver");
        dataSource.setUsername("superid");
        dataSource.setPassword("superid");
        dataSource.setUrl("jdbc:mysql://rm-bp1943x791y4e3z21.mysql.rds.aliyuncs.com/jpa");
        dataSource.setInitialSize(5);
        dataSource.setMinIdle(1);
        dataSource.setMaxActive(1000);

        new JdbcSessionFactory(dataSource,redisTemplate);
    }

}
