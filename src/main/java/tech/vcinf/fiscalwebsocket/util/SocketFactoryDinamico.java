package tech.vcinf.fiscalwebsocket.util;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.SecureRandom;

public class SocketFactoryDinamico implements SecureProtocolSocketFactory {

    private final SSLSocketFactory sslSocketFactory;

    public SocketFactoryDinamico(KeyStore keyStore, String alias, String senha,
                                 KeyStore cacert, String sslProtocol) throws Exception {
        // Configurar KeyManagerFactory com o certificado do emitente
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(keyStore, senha.toCharArray());

        // Configurar TrustManagerFactory com o cacert
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(cacert);

        // Criar SSLContext
        SSLContext sslContext = SSLContext.getInstance(sslProtocol);
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());

        this.sslSocketFactory = sslContext.getSocketFactory();
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        return sslSocketFactory.createSocket(host, port);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localAddress, int localPort) throws IOException, UnknownHostException {
        return sslSocketFactory.createSocket(host, port, localAddress, localPort);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localAddress, int localPort, HttpConnectionParams params) throws IOException, UnknownHostException, ConnectTimeoutException {
        if (params == null) {
            throw new IllegalArgumentException("Parameters may not be null");
        }
        int timeout = params.getConnectionTimeout();
        if (timeout == 0) {
            return createSocket(host, port, localAddress, localPort);
        } else {
            Socket socket = sslSocketFactory.createSocket();
            socket.bind(new java.net.InetSocketAddress(localAddress, localPort));
            socket.connect(new java.net.InetSocketAddress(host, port), timeout);
            return socket;
        }
    }

    @Override
    public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
        return sslSocketFactory.createSocket(socket, host, port, autoClose);
    }
}
