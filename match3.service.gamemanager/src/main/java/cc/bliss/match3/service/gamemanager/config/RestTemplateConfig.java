package cc.bliss.match3.service.gamemanager.config;

import bliss.lib.framework.util.JSONUtil;
import cc.bliss.match3.service.gamemanager.ent.persistence.match3.ConfigEnt;
import cc.bliss.match3.service.gamemanager.service.system.ConfigService;
import cc.bliss.match3.service.gamemanager.util.GoogleUtils;
import com.google.gson.JsonObject;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Map;

@Configuration
public class RestTemplateConfig {

    @Autowired
    private ConfigService configService;

    public static final Map<String, JsonObject> k8sConfigMap = new NonBlockingHashMap<>();

    @Bean
    public RestTemplate restTemplate() throws Exception {
        SSLContext sslContext = getSslContext();

        HttpClient httpClient = HttpClients.custom()
                .setSSLContext(sslContext)
                .build();

        ClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        return new RestTemplate(requestFactory);
    }

    public SSLContext getSslContext() throws Exception {
        String gkeConfig = "GKE_" + GoogleUtils.getRegionName();
        ConfigEnt configEnt = configService.getConfig(gkeConfig);
        JsonObject data = JSONUtil.DeSerialize(configEnt.getValue(), JsonObject.class);
        k8sConfigMap.put(gkeConfig, data);
        String certificateContent = data.get("certificate").getAsString();
        InputStream certInputStream = new ByteArrayInputStream(certificateContent.getBytes());

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);

        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        Certificate cert = certificateFactory.generateCertificate(certInputStream);
        keyStore.setCertificateEntry("k8s-certificate", cert);


        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagerFactory.getTrustManagers(), new java.security.SecureRandom());
        return sslContext;
    }
}
