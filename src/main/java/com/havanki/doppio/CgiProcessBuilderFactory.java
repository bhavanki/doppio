package com.havanki.doppio;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.util.Map;

/**
 * A factory for {@code ProcessBuilder} objects that can run CGI scripts.
 */
public class CgiProcessBuilderFactory {

  private static final String GATEWAY_INTERFACE = "CGI/1.1";
  private static final String SERVER_PROTOCOL = "GEMINI";  // probably right
  private static final String SERVER_SOFTWARE = "Doppio";  // TBD add version

  /**
   * Creates a {@code ProcessBuilder} for a CGI script. This includes setting
   * expected environment variables.
   *
   * @param  resourceFile script file
   * @param  uri          original request URI
   * @param  socket       client socket
   * @param  serverProps  server properties
   * @return              process builder
   * @throws IOException  if the canonical path for the script file cannot be
   *                      determined
   */
  public ProcessBuilder createCgiProcessBuilder(File resourceFile, URI uri,
                                                Socket socket,
                                                ServerProperties serverProps)
    throws IOException {
    // Run the resource file as the command. Combine standard output and
    // standard error so they are fed back together in the server response.
    String command = resourceFile.getCanonicalPath();
    ProcessBuilder pb = new ProcessBuilder()
      .command(command)
      .directory(resourceFile.getParentFile())
      .redirectErrorStream(true);

    // Set CGI environment variables.
    Map<String, String> pbenv = pb.environment();
    pbenv.put("GATEWAY_INTERFACE", GATEWAY_INTERFACE);
    // pbenv.put("PATH_INFO", ...); TBD
    // pbenv.put("PATH_TRANSLATED", ...); TBD
    if (uri.getQuery() != null) {
      pbenv.put("QUERY_STRING", uri.getQuery());
    }

    InetSocketAddress remoteSocketAddress =
      (InetSocketAddress) socket.getRemoteSocketAddress();
    if (remoteSocketAddress != null) {
      pbenv.put("REMOTE_ADDR", remoteSocketAddress.getAddress().getHostAddress());
      pbenv.put("REMOTE_HOST", remoteSocketAddress.getHostString());
    }

    // pbenv.put("REMOTE_IDENT", ...); TBD
    // pbenv.put("REMOTE_USER", ...); TBD
    // REQUEST_METHOD is not applicable to Gemini
    pbenv.put("SCRIPT_NAME", uri.getPath()); // TBD: adjust with PATH_INFO
    pbenv.put("SERVER_NAME", serverProps.getHost());
    pbenv.put("SERVER_PORT", Integer.toString(serverProps.getPort()));
    pbenv.put("SERVER_PROTOCOL", SERVER_PROTOCOL);
    pbenv.put("SERVER_SOFTWARE", SERVER_SOFTWARE);

    return pb;
  }
}
