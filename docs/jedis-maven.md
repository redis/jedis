## Use Jedis as a maven dependency:

### Official Releases

```xml
<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
    <version>8.0.0</version>
</dependency>
```

### Snapshots

```xml
<repositories>
    <repository>
        <id>central-snapshots</id>
        <url>https://central.sonatype.com/repository/maven-snapshots/</url>
        <releases>
            <enabled>false</enabled>
        </releases>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>
```

and

```xml
<dependencies>
    <dependency>
        <groupId>redis.clients</groupId>
        <artifactId>jedis</artifactId>
        <version>8.0.2-SNAPSHOT</version>
    </dependency>
</dependencies>
```
