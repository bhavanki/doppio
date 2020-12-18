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

Then, create a properties file. Use [doppio-example.properties](doppio-example.properties) as an example. Only the host name is required; it must match the name on the server certificate.

Finally, run the JAR.

```
$ java -Djavax.net.ssl.keyStore=doppio.jks -Djavax.net.ssl.keyStorePassword=doppio \
  -jar target/doppio-*.jar doppio.properties
```

To support TLS client authentication, create a truststore containing imported, trusted certificates of the authorities that sign client certificates (or the client certificates themselves).

```
$ keytool -importcert -file trustedcert.pem -alias trustedcert \
  -keystore doppiots.jks
```

Then, specify the truststore as well when running the JAR.

```
... -Djavax.net.ssl.trustStore=doppiots.jks -Djavax.net.ssl.trustStorePassword=doppio ...
```

## Static File Support

Place static resources in the configured root directory. Resource content is streamed to clients exactly as it is in its resource. Notably, Doppio does not convert line endings in text files, including those that use CR ('\r') alone.

Content type is detected using Java's built-in mechanism, with additional support for recognizing _.gmi_ and _.gemini_ files as text/gemini. There is no charset detection for text files.

When a directory is requested, Doppio looks for an index file, ending with any supported filename suffix for text/gemini files (e.g., _index.gmi_), and returns the first one it finds. Otherwise, it returns a 51 (not found) response.

## CGI Support

Place CGI scripts in the configured CGI directory. If no directory is configured, Doppio does not run scripts, even if they reside in a directory that seems like a valid CGI directory (e.g., some "cgi-bin" directory).

Doppio tries to follow [RFC 3875](https://tools.ietf.org/html/rfc3875) in its CGI support. Here are salient limitations and variations from the standard in the implementation.

* UTF-8 is expected for script response headers.
* Not all meta-variables (environment variables) are supported. The standard variable REQUEST_METHOD is not applicable to Gemini. (More info to come here.)
* Request bodies are not supported, since Gemini does not support them. The URI query string is the only input mechanism.
* NPH (Non-Parsed Header) scripts are not supported.
* Local redirect responses are not supported.
* Doppio does not check if a client redirect response is well-formed in terms of response headers.
* Client redirect responses with document are not supported, because Gemini does not permit response bodies in redirects.
* Status codes 20 and 30 are used as defaults for successful responses and redirects, instead of (HTTP) 200 and 302. The "bad request" status code is 59 instead of (HTTP) 400.

## Secure Directories

Place resources (static or CGI) which should require client authentication in one of the configured secure directories. If no directories are configured, then Doppio itself does not enforce authentication, but CGI scripts may do so on their own.

Doppio validates a client certificate used to authenticate a request only when authentication is required for the requested resource.

_This feature is very basic and may be expanded in the future._

## License

[GNU Affero General Public License v3](LICENSE)
