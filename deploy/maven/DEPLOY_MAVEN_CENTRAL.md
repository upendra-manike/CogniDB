# 🍃 Publishing SyntricDB to Maven Central (`com.syntricdb`)

This guide explains how to deploy SyntricDB's Java artifacts (`syntricdb-engine`, `syntricdb-jdbc`) to **Maven Central Portal / Sonatype OSSRH** so Java & Spring Boot developers can include SyntricDB natively via Maven or Gradle.

---

## 🛠️ Prerequisites

1. Sonatype Central Account at [central.sonatype.com](https://central.sonatype.com).
2. GPG Keypair generated for signing release artifacts (`gpg --gen-key`).
3. `~/.m2/settings.xml` configured with Sonatype credentials & GPG passphrase.

---

## ⚙️ Maven Settings Configuration (`~/.m2/settings.xml`)

```xml
<settings>
  <servers>
    <server>
      <id>central</id>
      <username>YOUR_SONATYPE_TOKEN_USER</username>
      <password>YOUR_SONATYPE_TOKEN_PASSWORD</password>
    </server>
  </servers>
  <profiles>
    <profile>
      <id>gpg</id>
      <properties>
        <gpg.passphrase>YOUR_GPG_PASSPHRASE</gpg.passphrase>
      </properties>
    </profile>
  </profiles>
</settings>
```

---

## 🚀 One-Click Deploy Command

```bash
mvn clean deploy -P release -DskipTests
```

After deployment, Java & Spring Boot developers can use SyntricDB in their `pom.xml`:

```xml
<dependency>
    <groupId>com.syntricdb</groupId>
    <artifactId>syntricdb-jdbc</artifactId>
    <version>1.0.0</version>
</dependency>
```
