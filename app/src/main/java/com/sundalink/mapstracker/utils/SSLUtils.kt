package com.sundalink.mapstracker.utils

import android.content.Context
import java.security.KeyStore
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory
import java.security.cert.CertificateFactory

object SSLUtils {
    fun getSocketFactory(context: Context, fileName: String): SSLSocketFactory {
        // Load certificate from assets
        val certificate = context.assets.open(fileName).use { inputStream ->
            CertificateFactory.getInstance("X.509").generateCertificate(inputStream)
        }

        // Create a KeyStore and load the certificate
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, null)
            setCertificateEntry("ca", certificate)
        }

        // Create a TrustManagerFactory and initialize with the KeyStore
        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
            init(keyStore)
        }

        // Create an SSLContext and initialize it with the TrustManager
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, trustManagerFactory.trustManagers, null)
        }

        return sslContext.socketFactory
    }
}
