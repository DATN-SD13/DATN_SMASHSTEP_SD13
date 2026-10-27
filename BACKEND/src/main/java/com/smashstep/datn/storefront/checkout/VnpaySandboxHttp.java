package com.smashstep.datn.storefront.checkout;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.*;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.time.Duration;
import java.util.Locale;

/** Windows uses its trusted CA store if the JDK CA store cannot validate the Sandbox chain. */
final class VnpaySandboxHttp {
    private static final URI ENDPOINT = URI.create("https://sandbox.vnpayment.vn/merchant_webapi/api/transaction");
    private static volatile HttpClient windowsClient;
    private VnpaySandboxHttp() {}

    static HttpResponse<InputStream> send(HttpClient standard, HttpRequest request) throws IOException, InterruptedException {
        if (!ENDPOINT.equals(request.uri()) || !"POST".equals(request.method()))
            throw new IllegalArgumentException("Only VNPAY Sandbox QueryDr is permitted");
        var trusted = windowsClient;
        if (trusted != null) return trusted.send(request, HttpResponse.BodyHandlers.ofInputStream());
        try { return standard.send(request, HttpResponse.BodyHandlers.ofInputStream()); }
        catch (SSLHandshakeException original) {
            if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows")
                    || !certificatePathFailure(original)) throw original;
            try { return windowsClient().send(request, HttpResponse.BodyHandlers.ofInputStream()); }
            catch (GeneralSecurityException e) { throw new IOException("Windows trusted certificates are unavailable", e); }
        }
    }

    private static boolean certificatePathFailure(Throwable error) {
        for (var cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof java.security.cert.CertPathBuilderException
                    || cause instanceof java.security.cert.CertPathValidatorException) return true;
        }
        return false;
    }

    private static HttpClient windowsClient() throws GeneralSecurityException, IOException {
        synchronized (VnpaySandboxHttp.class) {
            if (windowsClient == null) {
                var roots = KeyStore.getInstance("Windows-ROOT");
                roots.load(null, null);
                var managers = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                managers.init(roots);
                var tls = SSLContext.getInstance("TLS");
                tls.init(null, managers.getTrustManagers(), null);
                // Standard certificate validity and HTTPS hostname checks stay enabled.
                windowsClient = HttpClient.newBuilder().sslContext(tls).connectTimeout(Duration.ofSeconds(4))
                        .followRedirects(HttpClient.Redirect.NEVER).version(HttpClient.Version.HTTP_1_1).build();
            }
            return windowsClient;
        }
    }
}
