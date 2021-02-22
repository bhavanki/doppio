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

Gemini requires a server certificate, so generate one using keytool. This example command generates an elliptic curve (EC) key and corresponding certificate.

```
$ keytool -genkeypair -keystore doppio.jks -storepass doppio -keyalg EC
```

Alternatively, use openssl (again, this example generates an EC key).

```
$ openssl ecparam -genkey -name prime256v1 -out doppio.key
$ openssl req -new -key doppio.key -x509 -out doppio.crt
$ openssl pkcs12 -export -inkey doppio.key -in doppio.crt -out doppio.p12
```

Use a common name that matches your hostname.

Then, create a properties file. Use [doppio-example.properties](doppio-example.properties) as an example. Set the following properties:

* `host` must match the name on the server certificate.
* `keystore` must point to the keystore (e.g., JKS or PKCS#12 file) containing the server's private key.
* `keystorePassword` must contain the password for the keystore.

Finally, run the JAR.

```
$ java -jar target/doppio-*.jar doppio.properties
```

Doppio listens on two ports:

* a server port for Gemini connections: configuration property `port`, default 1965
* a control port for control commands: configuration property `controlPort`, default 31965

Connections to the control port may only be made from the loopback address. If the `controlPort` configuration property is set to -1, then Doppio does not open a control port.

## Control Commands

A control command is a single line of text.

* `shutdown`: gracefully shuts down the server

An easy way to send control commands is with netcat.

```
$ nc localhost 31965
shutdown
```

## Static File Support

Place static resources in the configured root directory. By default, resource content is streamed to clients exactly as it is in its resource. To force the conversion of line endings in text resources to canonical form (CRLF or "\r\n"), set the `forceCanonicalText` server property to `true`.

Content type is detected using Java's built-in mechanism, with additional support for recognizing a configurable set of file suffixes for text/gemini resources (defaults _.gmi_ and _.gemini_).

Charset for text resources is optionally detected using [juniversalchardet](https://github.com/albfernandez/juniversalchardet). By default, charset detection is disabled. Caveats:

* Charset detection is always based on heuristics, so the detector may guess incorrectly sometimes.
* Detection requires reading the text resource an additional time before it is served, negatively affecting performance.

When a directory is requested, Doppio looks for an index file, ending with any supported filename suffix for text/gemini files (e.g., _index.gmi_), and returns the first one it finds. Otherwise, it returns a 51 (not found) response.

## Favicon Support

Set the `favicon` server property to an emoji to configure a favicon. Doppio then serves a favicon document in accordance with the [favicon RFC](gemini://mozz.us/files/rfc_gemini_favicon.gmi). If the property is not set, a literal favicon document may still be created at and served from _/favicon.txt_.

## CGI Support

Place CGI scripts in the configured CGI directory. If no directory is configured, Doppio does not run scripts, even if they reside in a directory that seems like a valid CGI directory (e.g., some "cgi-bin" directory).

Doppio tries to follow [RFC 3875](https://tools.ietf.org/html/rfc3875) in its CGI support. Here are salient limitations and variations from the standard in the implementation.

* UTF-8 is expected for script response headers.
* The following meta-variables (environment variables) are not supported, since they are not applicable to Gemini: CONTENT_LENGTH, CONTENT_TYPE. The REMOTE_IDENT meta-variable is not implemented. REQUEST_METHOD is set to an empty string.
* Request bodies are not supported, since Gemini does not support them. The URI query string and extra path information are the only input mechanisms.
* NPH (Non-Parsed Header) scripts are not supported.
* Doppio does not check if a client redirect response is well-formed in terms of response headers.
* Client redirect responses with document are not supported, because Gemini does not permit response bodies in redirects.
* Status codes 20 and 30 are used as defaults for successful responses and redirects, instead of (HTTP) 200 and 302. The "bad request" status code is 59 instead of (HTTP) 400.

Local redirects *are* supported, up to the maximum per request configured with the `maxLocalRedirects` server property.

The following TLS-related meta-variables are also supported. Those in the middle column are always set when applicable. Those in the right column, derived from [Apache mod_ssl](https://httpd.apache.org/docs/current/mod/mod_ssl.html), are only set when the `useModSslCgiMetaVars` server configuration property is set to `true`.

<table>
  <tr>
    <th>definition</th>
    <th>TLS meta-variable</th>
    <th>SSL (mod_ssl) meta-variable</th>
  </tr>
  <tr>
    <td>cipher name</td>
    <td>TLS_CIPHER</td>
    <td>SSL_CIPHER</td>
  </tr>
  <tr>
    <td>TLS protocol version</td>
    <td>TLS_VERSION</td>
    <td>SSL_PROTOCOL</td>
  </tr>
  <tr>
    <td>session ID</td>
    <td>TLS_SESSION_ID</td>
    <td>SSL_SESSION_ID</td>
  </tr>
  <tr>
    <td>client certificate serial number</td>
    <td>TLS_CLIENT_SERIAL</td>
    <td>SSL_CLIENT_M_SERIAL</td>
  </tr>
  <tr>
    <td>client certificate version</td>
    <td>TLS_CLIENT_VERSION</td>
    <td>SSL_CLIENT_M_VERSION</td>
  </tr>
  <tr>
    <td>client certificate fingerprint</td>
    <td>TLS_CLIENT_HASH</td>
    <td>-</td>
  </tr>
  <tr>
    <td>client certificate issuer DN</td>
    <td>TLS_CLIENT_ISSUER</td>
    <td>SSL_CLIENT_I_DN</td>
  </tr>
  <tr>
    <td>client certificate subject DN</td>
    <td>TLS_CLIENT_SUBJECT</td>
    <td>SSL_CLIENT_S_DN</td>
  </tr>
  <tr>
    <td>client certificate validity start timestamp</td>
    <td>TLS_CLIENT_NOT_BEFORE</td>
    <td>SSL_CLIENT_V_START</td>
  </tr>
  <tr>
    <td>client certificate validity end timestamp</td>
    <td>TLS_CLIENT_NOT_AFTER</td>
    <td>SSL_CLIENT_V_END</td>
  </tr>
  <tr>
    <td>client certificate validity remaining time, in days</td>
    <td>TLS_CLIENT_REMAIN</td>
    <td>SSL_CLIENT_V_REMAIN</td>
  </tr>
</table>

Text output from CGI scripts is subject to line ending conversion if the `forceCanonicalText` server property is set to `true`. Because CGI scripts emit their own response headers, Doppio does not detect content type or charset for them.

## Secure Domains

_Note: This feature replaces "secure directories" in earlier versions of Doppio._

A secure domain is a combination of:

* a resource directory
* an optional truststore

Place resources (static or CGI) which should require client authentication in the directory of one of the configured secure domains. If a secure domain has a truststore, then Doppio authorizes client certificates against that truststore. So, the truststore may contain individual self-signed certificates, or the root certificate for trusted authorities, or both.

If a secure domain has no truststore, then Doppio still requires client authentication, but accepts any client certificate.

Doppio validates a client certificate (e.g., checks its valid date range) only when authentication is required for a requested resource.

CGI scripts may roll their own client authentication and authorization code instead of relying on a secure domain.

To add a certificate to a new or existing truststore, use the [example script](etc/add-to-truststore.sh) or `keytool` directly:

```
$ keytool -importcert -file trustedcert.pem -alias trustedcert \
  -keystore domaints.jks
```

## License

[GNU Affero General Public License v3](LICENSE)

The [example systemd service file](etc/doppio.service) is separately available under the [MIT License](https://opensource.org/licenses/MIT).
