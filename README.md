# Doppio

A [Gemini](https://gemini.circumlunar.space/) server.

## Building

Use [Apache Maven](https://maven.apache.org/).

```
$ mvn package
```

The result is a shaded executable JAR.

## Running

Gemini requires a server certificate, so generate one using keytool.

```
$ keytool -genkeypair -keystore doppio.jks -storepass doppio -keyalg EC
```

Use a common name that matches your hostname.

Then, create a properties file. Use [doppio-example.properties](doppio-example.properties) as an example. Only the root directory is required.

Finally, run the JAR.

```
$ java -Djavax.net.ssl.keyStore=doppio.jks -Djavax.net.ssl.keyStorePassword=doppio \
  -jar target/doppio-*.jar doppio.properties
```

## License

[GNU Affero General Public License v3](LICENSE)
