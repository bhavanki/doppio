# Doppio

<img src="src/main/resources/doppio.png" alt="Doppio logo" width="200px">

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

Then, create a properties file. Use [doppio-example.properties](doppio-example.properties) as an example. Set the following properties:

* `host` must match the name on the server certificate.
* `keystore` must point to the server private key file.
* `keystorePassword` must contain the password for the keystore.

Finally, run the JAR.

```
$ java -jar target/doppio-*.jar doppio.properties
```

To support TLS client authentication, create a truststore containing imported, trusted certificates of the authorities that sign client certificates (or the client certificates themselves).

```
$ keytool -importcert -file trustedcert.pem -alias trustedcert \
  -keystore doppiots.jks
```

Then, specify the truststore in the server properties file, and its password, using the `truststore` and `truststorePassword` properties, respectively.

## Static File Support

Place static resources in the configured root directory. By default, resource content is streamed to clients exactly as it is in its resource. To force the conversion of line endings in text resources to canonical form (CRLF or "\r\n"), set the `forceCanonicalText` server property to `true`.

Content type is detected using Java's built-in mechanism, with additional support for recognizing _.gmi_ and _.gemini_ files as text/gemini. There is no charset detection for text files.

When a directory is requested, Doppio looks for an index file, ending with any supported filename suffix for text/gemini files (e.g., _index.gmi_), and returns the first one it finds. Otherwise, it returns a 51 (not found) response.

## CGI Support

Place CGI scripts in the configured CGI directory. If no directory is configured, Doppio does not run scripts, even if they reside in a directory that seems like a valid CGI directory (e.g., some "cgi-bin" directory).

Doppio tries to follow [RFC 3875](https://tools.ietf.org/html/rfc3875) in its CGI support. Here are salient limitations and variations from the standard in the implementation.

* UTF-8 is expected for script response headers.
* The following meta-variables (environment variables) are not supported, since they are not applicable to Gemini: CONTENT_LENGTH, CONTENT_TYPE, REQUEST_METHOD. The REMOTE_IDENT meta-variable is not implemented.
* Request bodies are not supported, since Gemini does not support them. The URI query string is the only input mechanism.
* NPH (Non-Parsed Header) scripts are not supported.
* Doppio does not check if a client redirect response is well-formed in terms of response headers.
* Client redirect responses with document are not supported, because Gemini does not permit response bodies in redirects.
* Status codes 20 and 30 are used as defaults for successful responses and redirects, instead of (HTTP) 200 and 302. The "bad request" status code is 59 instead of (HTTP) 400.

Local redirects *are* supported, up to the maximum per request configured with the `maxLocalRedirects` server property.

The following meta-variables, copied from
[Apache mod_ssl](https://httpd.apache.org/docs/current/mod/mod_ssl.html), are
also supported.

* SSL_CIPHER
* SSL_CLIENT_I_DN
* SSL_CLIENT_M_SERIAL
* SSL_CLIENT_M_VERSION
* SSL_CLIENT_S_DN
* SSL_CLIENT_V_START
* SSL_CLIENT_V_END
* SSL_PROTOCOL
* SSL_SESSION_ID

Text output from CGI scripts is subject to line ending conversion if the `forceCanonicalText` server property is set to `true`.

## Secure Directories

Place resources (static or CGI) which should require client authentication in one of the configured secure directories. If no directories are configured, then Doppio itself does not enforce authentication, but CGI scripts may do so on their own.

Doppio validates a client certificate used to authenticate a request only when authentication is required for the requested resource.

_This feature is very basic and may be expanded in the future._

## License

[GNU Affero General Public License v3](LICENSE)
