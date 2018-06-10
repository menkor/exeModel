
# ExeModel

    微服务成为了趋势，每个服务一般都有多个实例，此时需要考虑到分布式缓存问题了。ExeModel目前实现了以Redis作为缓存层使用，支持分库建设置，提供丰富的数据库SQL操作。
## Features ##

* Entity自动映射数据库表，能够直接进行CRUD操作
* builder模式构造常用SQL，可以返回指定类的实例或者实例列表，也支持原生语句
* 支持分布式缓存，开发者不需要感知到缓存
* 可以自定义缓存字段，将常用字段进行缓存，而不是整表，默认的缓存实现基于redis hmap结构，当有字段更新时，通过lua脚本，直接更新，不需要获取原实体对象实例。存储到Redis时为字节流，有压缩处理，节省内存
* 支持缓存批量操作，减少与缓存服务器的请求次数
* 方便与


## Quick Start ##

Download the jar through Maven:

```xml
<dependency>
  <groupId>com.menkor</groupId>
  <artifactId>jpa</artifactId>
  <version>${version}</version>
</dependency>
```

The simple Spring ExeModel configuration with SpringBoot looks like this:
```java
@Configuration
@EnableTransactionManagement
@Order(1)
public class DataSourceConfig {
    @Bean
    @ConfigurationProperties(prefix = "druid.datasource")
    public DataSource DataSource() {
        return new DruidDataSource();
    }

    @Bean
    public JedisPoolConfig JedisPoolConfig(
            @Value("${redis.pool.min-idle}") int minIdle,
            @Value("${redis.pool.max-idle}") int maxIdle,
            @Value("${redis.pool.max-wait}") int maxWaitMillis,
            @Value("${redis.pool.block-when-exhausted}") boolean blockWhenExhausted,
            @Value("${redis.pool.max-total}") int maxTotal) {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMinIdle(minIdle);
        config.setMaxIdle(maxIdle);
        config.setMaxWaitMillis(maxWaitMillis);
        config.setMaxTotal(maxTotal);
        config.setBlockWhenExhausted(blockWhenExhausted);
        return config;
    }

    @Bean
    public ICache Cache(
            @Qualifier("JedisPoolConfig") JedisPoolConfig config,
            @Value("${redis.host}") String host,
            @Value("${redis.port}") int port,
            @Value("${redis.password}") String password,
            @Value("${redis.timeout}") int timeout,
            @Value("${redis.database}") int database,
            @Value("${redis.ssl}") boolean ssl) {
        if(StringUtil.isEmpty(password)){
            password = null;
        }
        return new RedisTemplate(config, host, port, timeout, password, database, ssl);
    }


    @Bean
    public JdbcSessionFactory JdbcSessionFactory(
            @Qualifier("DataSource") DataSource dataSource,
            @Qualifier("Cache") ICache cache) {
        return new JdbcSessionFactory(dataSource, cache);
    }

    @Bean
    public PlatformTransactionManager transactionManager(
            @Qualifier("JdbcSessionFactory")JdbcSessionFactory jdbcSessionFactory) {
        TransactionManager transactionManager = new TransactionManager();
        transactionManager.setSessionFactory(jdbcSessionFactory);
        return transactionManager;
    }
}
```

Create an entity:

```java
@Cacheable(key = "trl")
public class Role extends ExecutableModel{
    @Id
    private Integer id;
    @PartitionId //UserId作为分库建
    @CacheField//UserId将被缓存
    private int userId;
    @CacheField //角色名将被缓存
    private String title;
    private String details;
    private String permissions;

  // Getters and setters
  // (Firstname, Lastname)-constructor and noargs-constructor
  // equals / hashcode

}
```


Write a test client

```java
@Test
    public void testSave(){
        User user = new User();
        user.setName("zp");
        user.setGender(Gender.MAN);
        user.setAge(18);
        user.save();
        Assert.assertTrue(user.getName().equals(findById(user.getId()).getName()));
    }

    @Test
    public void testUpdate() {
        User user = saveUser();
        user.setAge(25);
        user.setName("HanMeiMei");
        user.setGender(Gender.FEMALE);
        PublicInfoDTO publicInfoDTO = new PublicInfoDTO();
        publicInfoDTO.setAddress(true);
        user.setPublicInfo(publicInfoDTO);
        user.update();
        User _user = CustomStatement.build(User.class).findById(user.getId());
        Assert.assertTrue(_user.getAge() == 25);
        Assert.assertTrue(_user.getPublicInfo().isAddress());
        Assert.assertFalse(_user.getPublicInfo().isGender());
    }


    @Test
    public void testDelete() {
        User user = new User();
        user.setName("src/test");
        user.setAge(18);
        user.save();
        user.delete();
        User _user = CustomStatement.build(User.class).findById(user.getId());
        Assert.assertTrue(_user == null);
    }


    @Test
    public void testFindList() {
        String sql = "select * from user where name=?";
        List<User> list = CustomStatement.build(User.class).findListByNativeSql(sql, "zp");
        Assert.assertTrue(list != null);
    }

    @Test
    public void testFindOne() {
        saveUser();
        String sql = "select id,name from user where name=? and age =? limit 1 ";
        User user = CustomStatement.build(User.class).findOneByNativeSql(sql, "zp", 18);
        Assert.assertTrue(user != null);

        UserVO userVO = CustomStatement.build(User.class).eq("name","zp").selectOne(UserVO.class,"id"," name as username");
        Assert.assertTrue(userVO!=null);
    }

    @Test
    public void testSelectOne() {
        saveUser();
        User user = CustomStatement.build(User.class).eq("name", "zp").selectOne("id", "name");
        Assert.assertTrue(user != null);
    }


    @Test
    public void testSelectList() {
        List<User> users = CustomStatement.build(User.class).eq("name", "zp")
                .asc("age")
                .selectList("id", "name");

        Assert.assertTrue(users != null);
        List<Long> ids = CustomStatement.build(User.class).eq("name", "zp")
                .asc("age")
                .selectList(Long.class, "id");
        Assert.assertTrue(ids != null);

    }


    @Test
    public void testOrConditions() {
        List<User> users1 = CustomStatement.build(User.class).or(Expr.eq("name", "zp"), Expr.eq("name", "xxf")).selectList();
        String[] names = {"zp", "xxf"};
        List<User> users2 = CustomStatement.build(User.class).in("name", names).selectList();
        Assert.assertTrue(users1.size() == users2.size());

    }

    @Test
    public void testRemove() {
        User user = new User();
        user.setAge(33);
        user.setName("tms");
        user.save();
        CustomStatement.build(User.class).eq("name", "tms").remove();
        Assert.assertTrue(findById(user.getId()) == null);


    }


    @Test
    public void testSet() {
        User user = new User();
        user.setAge(33);
        user.setName("tms");
        user.save();
        CustomStatement.build(User.class).eq("name", "tms").set("name", "xxf", "age", 38);//把tms改成xxf，年龄改为38
        User _user = CustomStatement.build(User.class).findById(user.getId());
        Assert.assertTrue(_user.getAge() == 38);

        CustomStatement.build(User.class).eq("name", "xxf").set(" age = age + 1 ", null);
        Object __user = CustomStatement.build(User.class).findById(user.getId());
        Assert.assertTrue(((User) __user).getAge() == 39);

    }

    @Test
    public void testTransaction() {
        Session session = AbstractSession.currentSession();
        session.begin();
        CustomStatement.build(User.class).eq("name", "tms").set("name", "xxf", "age", 38);//把tms改成xxf，年龄改为38
        CustomStatement.build(User.class).eq("name", "xxf").set(" age = age + 1 ", null);
        session.commit();

    }


    @Test
    public void testExecute() {
        User.executeUpdate("update user set name=? where name='zp'", new ParameterBindings("jzy"));
        Assert.assertTrue(CustomStatement.build(User.class).eq("name", "zp").selectOne() == null);
    }


    @Test
    public void testCopy() {
        UserAddForm userAddForm = new UserAddForm();
        userAddForm.setName("zp");
        userAddForm.setAge(10);
        User user = new User();
        user.copyPropertiesFrom(userAddForm);
        Assert.assertTrue(user.getName().equals("zp") && user.getAge() == 10);

        UserAddForm test2 = new UserAddForm();
        user.copyPropertiesTo(test2);
        Assert.assertFalse(test2.getName().equals("zp") && test2.getAge() == null);

    }

    @Test
    public void testPagination() {
        Pagination pagination = new Pagination();
        pagination.setPage(1);
        pagination.setSize(20);
        List<User> users = CustomStatement.build(User.class).eq("name", "xxf")
                .asc("age")
                .selectByPagination(pagination);

        Assert.assertFalse(users.size() > pagination.getTotal());

    }

    @Test
    public void testUpdateWithPartition() {
        User user = saveUser();
        Role role = new Role();
        role.setTitle("开发人员");
        try {
            role.save();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof JdbcRuntimeException);
        }

        role.setUserId(user.getId());
        role.save();
        Assert.assertTrue(role.getUserId() == user.getId());
    }

    @Test
    public void testDeleteWithPartition() {
        User user = saveUser();
        Role role = new Role();
        role.setTitle("开发人员");
        role.setUserId(user.getId());
        try {
            role.delete();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof JdbcRuntimeException);
        }

        role.save();

        try {
            CustomStatement.build(Role.class).findById(role.getId());
        } catch (Exception e) {
            Assert.assertTrue(e instanceof JdbcRuntimeException);
            e.printStackTrace();
        }
        Role _role = CustomStatement.build(Role.class).findById(role.getId(), user.getId());
        Assert.assertTrue(_role.getTitle().equals("开发人员"));

        role.delete();

    }


    @Test
    public void testExecuteBatch() {
        List<ExecutableModel> users = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            User user = new User();
            user.setName("zp" + i);
            users.add(user);
        }
        try (Session session = User.getSession()) {
            session.saveBatch(users);//批量保存userlist
        } catch (Exception e) {
            e.printStackTrace();
        }

        try (Session session = User.getSession()) {
            session.startBatch();
            for (int i = 0; i < 10; i++) {
                User.executeUpdate("update user set name=? where name=?", new ParameterBindings("jzy" + i, "zp" + i));//批量修改
            }

            session.executeBatch();
        }
    }

    @Test
    public void testUnusualType(){
        String testStr = "ElonMusk";
        BigDecimal money = new BigDecimal("666.66");
        BigInteger no = new BigInteger("1212121212121212121");
        byte[] bytes = BinaryUtil.getBytes(testStr);
        User user = new User();
        user.setName("zp");
        user.setAge(18);
        user.setPwd(bytes);
        user.setMoney(money);
        user.setSerialNo(no);
        user.save();

        User _user = findById(user.getId());
        Assert.assertTrue(BinaryUtil.toString(_user.getPwd()).equals(testStr));
        Assert.assertTrue(_user.getMoney().equals(money));
        Assert.assertTrue(_user.getSerialNo().equals(no));
    }

    @Test
    public void testInputStream() throws Exception{
        InputStream inputStream = UserDaoTest.class.getResourceAsStream("/test.sql");
        User user = new User();
        user.setName("zp");
        user.setAge(18);
        user.setImage(inputStream);
        user.save();

        User _user = findById(user.getId());
        InputStream inputStream1 = _user.getImage();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream1,"utf-8"));
        String s;
        while ( (s = bufferedReader.readLine())!=null){
            System.out.println(s);
        }
    }

    @Test
    public void testMutilResultSetOfProcedure() throws SQLException{
        TestVO testVO = AbstractSession.currentSession()
                .callProcedure(TestVO.class," call test_mutil_result_set(?)",new ParameterBindings("zp"));
    }



    private User saveUser() {
        User user = new User();
        user.setName("zp");
        user.setGender(Gender.MAN);
        user.setAge(18);
        user.save();
        return user;
    }



    private User findById(int userId) {
        return CustomStatement.build(User.class).findById(userId);
    }
```


#操作缓存
```java
@Test
    public void testFindCache() {
        Role role = addRole();
        Role role1 = CustomStatement.build(Role.class).findCache(role.getId());
        Assert.assertTrue(role.getUserId() == role1.getUserId());

        Role role2 = CustomStatement.build(Role.class).id(role.getId()).selectOne("userId");
        Assert.assertTrue(role.getUserId() == role2.getUserId());

        Role role3 = CustomStatement.build(Role.class).findCache(role.getId());
        Assert.assertTrue(role3.getDetails() == null);//because details not cache

    }

    @Test
    public void testSet() {
        String newTitle = "Software Test";
        Role role = addRole();
        CustomStatement.build(Role.class).id(role.getId()).set("title", newTitle);

        Role role1 = CustomStatement.build(Role.class).findById(role.getId());
        Assert.assertTrue(role1.getTitle().equals(newTitle));
    }

    @Test
    public void testUpdate() {
        String newTitle = "CTO";
        Role role = addRole();
        role.setTitle(newTitle);
        role.update();

        Role role1 = CustomStatement.build(Role.class).findCache(role.getId());
        Assert.assertTrue(role1.getTitle().equals(newTitle));

        role1 = CustomStatement.build(Role.class).id(role.getId()).selectOne("title", "permissions");
        Assert.assertTrue(role1.getTitle().equals(newTitle));

    }



    @Test
    public void testBatch() {
        try (Session session = AbstractSession.currentSession()) {//try with material
            List<Role> roleList = getList(session);

            String tmp = "dog_fxxx";
            for (Role role : roleList) {
                role.setPermissions(tmp);
            }
            session.updateBatch(roleList);

            Map<Integer, Role> roleMap = session.getCache().batchGet(roleList, new MapTo<Integer, Role>() {
                @Override
                public Integer apply(Role role) {
                    return role.getId();
                }
            }, Role.class);

            Integer[] ids = new Integer[10];
            int i = 0;
            for (Role role : roleList) {
                ids[i++] = role.getId();
            }

            Map<Integer, Role> roleMap1 = session.getCache().batchGet(ids, Role.class);
            for(i=0;i<10;i++){
                Assert.assertTrue( roleMap.get(ids[i]).getTitle().equals(roleMap1.get(ids[i]).getTitle()));
            }

            List<Role> res = new ArrayList<>();
            session.startCacheBatch();
            for (Object id : ids) {
                Role role = CustomStatement.build(Role.class).id(id).selectOne("permissions");
                res.add(role);
                Role role1 = CustomStatement.build(Role.class).findCache(id);
                res.add(role1);
            }
            session.executeCacheBatch();


            for (Role role : res) {
                Assert.assertTrue(role != null);
            }

            Role role0 = roleList.get(0);
            session.startCacheBatch();
            CustomStatement.build(Role.class).id(role0.getId()).set("permissions", "fuck_the_wildest_dog");
            CustomStatement.build(Role.class).id(role0.getId()).set("userId", 20);
            role0 = CustomStatement.build(Role.class).findCache(role0.getId());
            session.executeCacheBatch();
            Assert.assertTrue(role0.getUserId() == 20);}
    }

    @Test
    public void onValid() {
        try (Session session = AbstractSession.currentSession()) {
            List<Role> roleList = getList(session);
            session.startCacheBatch();
            for (Role role : roleList) {
                final Role role1 = CustomStatement.build(Role.class).findCache(role.getId());
                role1.onValid(new Function<Role>() {
                    @Override
                    public void apply(Role o) {
                        System.out.println(role1.getPermissions());
                    }
                });
            }
            session.executeCacheBatch();

        }

    }


    @Test
    public void testBatchGet() {
        Integer[] ids = {1, 2, 1001};
        Map<Integer, Role> map = AbstractSession.currentSession().getCache().batchGet(ids, Role.class);
        System.out.println(map);
    }


    @Test
    public void testSetByObject() {
        Role role = addRole();
        RoleUpdateForm roleUpdateForm = new RoleUpdateForm();
        roleUpdateForm.setPermissions("set_by_object");
        roleUpdateForm.setNotExists(0);

        CustomStatement.build(Role.class).id(role.getId()).setByObject(roleUpdateForm);
        Role role1 = CustomStatement.build(Role.class).findCache(role.getId());
        Assert.assertTrue(role1.getPermissions().equals("set_by_object"));
        Assert.assertTrue(role1.getTitle().equals(role.getTitle()));

    }


    private Role addRole() {
        Role role = new Role();
        role.setTitle("Software Test");
        role.setUserId(10);
        role.setPermissions("admin");
        role.setDetails("starking");
        role.save();
        return role;
    }

    private List<Role> getList(Session session) {
        List<Role> roleList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Role role = new Role();
            role.setPermissions("fxxx dog");
            role.setTitle("jyz_FxxxDog");
            role.setUserId(10 + i);
            roleList.add(role);
        }
        session.saveBatch(roleList);
        return roleList;
    }
```
