package poker.connection.client;

import poker.connection.protocol.channels.ClientChannel;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.KeyStore;

public class ClientChannelFactory {
    public ClientChannel createChannel(String host, int port) throws Exception {
        SSLContext sslContext = getSSLContext();

        SSLSocketFactory socketFactory = sslContext.getSocketFactory();
        SSLSocket socket = (SSLSocket) socketFactory.createSocket();

        SocketAddress socketAddress = new InetSocketAddress(host, port);
        try {
            socket.connect(socketAddress);
        } catch (Exception e) {
            throw new ConnectException("Server is currently offline");
        }

        return new ClientChannel(socket);
    }

    private SSLContext getSSLContext() throws Exception {
        InputStream trustStoreInputStream = getClass().getClassLoader().getResourceAsStream("client_truststore.jks");
        KeyStore trustStore = KeyStore.getInstance("JKS");
        trustStore.load(trustStoreInputStream, "client_truststore".toCharArray());

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
        return sslContext;
    }
}
