Add jade lib manually:

```
mvn install:install-file -Dfile="./lib/jade.jar" -DgroupId="jade" -DartifactId="jade" -Dversion="4.5.0" -Dpackaging="jar"
```

Build and run agent

```
mvn clean install

mvn exec:java -DMAIN_HOST="192.168.10.4"
```

> **Remark:** Replace `192.168.10.4` with the actual IP address of your machine.