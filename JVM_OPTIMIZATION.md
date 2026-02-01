# JVM Optimization for 8GB RAM Laptop

This document provides JVM arguments optimized for running the PetBuddyCentral microservices on a machine with limited RAM (8GB total).

## Recommended JVM Arguments

### profile-rescue-svc (Port 9090)

```bash
java -Xms128m -Xmx256m \
     -XX:+UseSerialGC \
     -XX:+TieredCompilation \
     -XX:TieredStopAtLevel=1 \
     -XX:MaxMetaspaceSize=128m \
     -jar profile-rescue-svc.jar
```

### api-gateway (Port 8080)

```bash
java -Xms128m -Xmx256m \
     -XX:+UseSerialGC \
     -XX:+TieredCompilation \
     -XX:TieredStopAtLevel=1 \
     -XX:MaxMetaspaceSize=128m \
     -jar api-gateway.jar
```

## Explanation of Flags

| Flag                        | Purpose                                                    |
| --------------------------- | ---------------------------------------------------------- |
| `-Xms128m`                  | Initial heap size of 128MB                                 |
| `-Xmx256m`                  | Maximum heap size of 256MB                                 |
| `-XX:+UseSerialGC`          | Use Serial Garbage Collector (lowest memory footprint)     |
| `-XX:+TieredCompilation`    | Enable tiered compilation for faster startup               |
| `-XX:TieredStopAtLevel=1`   | Stop at C1 compilation level (faster startup, less memory) |
| `-XX:MaxMetaspaceSize=128m` | Limit metaspace to prevent unbounded growth                |

## For Development with Gradle

### profile-rescue-svc

```bash
cd profile-rescue-svc/demo
./gradlew bootRun -Dspring-boot.run.jvmArguments="-Xms128m -Xmx256m -XX:+UseSerialGC"
```

### api-gateway

```bash
cd api-gateway
./gradlew bootRun -Dspring-boot.run.jvmArguments="-Xms128m -Xmx256m -XX:+UseSerialGC"
```

## Memory Budget (8GB Total)

| Component                  | Allocation |
| -------------------------- | ---------- |
| Windows/OS                 | ~2GB       |
| IDE (VS Code/IntelliJ)     | ~1.5GB     |
| PostgreSQL/Supabase Client | ~256MB     |
| profile-rescue-svc         | ~256MB     |
| api-gateway                | ~256MB     |
| Other processes            | ~1GB       |
| **Total Reserved**         | **~5.3GB** |
| **Available Buffer**       | **~2.7GB** |

## Alternative: G1GC with Limited Regions

For slightly better performance at cost of more memory:

```bash
java -Xms192m -Xmx384m \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -XX:G1HeapRegionSize=1m \
     -jar <service>.jar
```

## Monitoring Memory Usage

```bash
# Check Java process memory
jcmd <pid> VM.native_memory summary

# Watch memory in real-time (Windows)
tasklist /FI "IMAGENAME eq java.exe" /FO TABLE

# Watch memory in real-time (PowerShell)
Get-Process -Name java | Select-Object Name, @{Name="Memory(MB)";Expression={$_.WorkingSet64 / 1MB}}
```
