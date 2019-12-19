

package com.sal.auth.oauth2;

import static com.google.common.base.MoreObjects.firstNonNull;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

import com.bettercloud.vault.SslConfig;
import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.auth.http.HttpTransportFactory;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.common.base.MoreObjects;

public class VaultCredentials extends GoogleCredentials {

    private static final long serialVersionUID = -2133257318957488431L;
    private static final String RFC3339 = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    private static final String VAULT_TOKEN_EMPTY_ERROR = "VAULT_TOKEN cannot be null";
    private static final String ROLESET_EMPTY_ERROR = "RoleSet cannot be null";

    private static final String CLOUD_PLATFORM_SCOPE = "https://www.googleapis.com/auth/cloud-platform";

    private String vaultToken;
    private String vaultHost;
    private String vaultCA;
    private String roleSet;
    private final String transportFactoryClassName;

    private transient HttpTransportFactory transportFactory;

    static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    private VaultConfig config;

    public static VaultCredentials create(String vaultToken, String vaultHost, String vaultCA, String roleSet,
            HttpTransportFactory transportFactory) {
        return VaultCredentials.newBuilder().setVaultToken(vaultToken).setVaultHost(vaultHost).setVaultCA(vaultCA)
                .setRoleSet(roleSet).build();
    }

    public static VaultCredentials create(String vaultToken, String vaultHost, String vaultCA, String roleSet) {
        return VaultCredentials.newBuilder().setVaultToken(vaultToken).setVaultHost(vaultHost).setVaultCA(vaultCA)
                .setRoleSet(roleSet).build();
    }

    private VaultCredentials(Builder builder) {
        this.vaultToken = builder.getVaulToken();
        this.vaultHost = builder.getVaultHost();
        this.vaultCA = builder.getVaultCA();
        this.roleSet = builder.getRoleSet();
        this.transportFactory = firstNonNull(builder.getHttpTransportFactory(),
                getFromServiceLoader(HttpTransportFactory.class, new DefaultHttpTransportFactory()));
        this.transportFactoryClassName = this.transportFactory.getClass().getName();

    }

    @Override
    public AccessToken refreshAccessToken() throws IOException {
        String tok = "";
        String strDate = "";
        Date date = new Date();
        try {
            config = new VaultConfig().address(this.vaultHost).token(this.vaultToken)
                    .sslConfig(new SslConfig().pemFile(new File(this.vaultCA)).build()).build();

            Vault vault = new Vault(config, 2);

            Map<String, String> dat = vault.logical().read(this.roleSet).getData();
            System.out.println(dat);
            strDate = dat.get("expires_at_seconds");
            DateFormat format = new SimpleDateFormat(RFC3339);

            date = format.parse(strDate);
        } catch (Exception vex) {
            new IOException("Unable to connect to Vault " + vex.toString());
        }
        return new AccessToken(tok, date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vaultToken, vaultHost, vaultCA, roleSet);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("vaultToken", vaultToken).add("vaultHost", vaultHost)
                .add("vaultCA", vaultCA).add("roleSet", roleSet).toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof VaultCredentials)) {
            return false;
        }
        VaultCredentials other = (VaultCredentials) obj;
        return Objects.equals(this.vaultHost, other.vaultHost) && Objects.equals(this.vaultToken, other.vaultToken);
    }

    public Builder toBuilder() {
        return new Builder();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends GoogleCredentials.Builder {

        private String vaultToken;
        private String vaultHost;
        private String vaultCA;
        private String roleSet;

        private HttpTransportFactory transportFactory;

        protected Builder() {
        }

        public Builder setVaultToken(String vaultToken) {
            this.vaultToken = vaultToken;
            return this;
        }

        public String getVaulToken() {
            return this.vaultToken;
        }

        public Builder setVaultHost(String vaultHost) {
            this.vaultHost = vaultHost;
            return this;
        }

        public String getVaultHost() {
            return this.vaultHost;
        }

        public Builder setVaultCA(String vaultCA) {
            this.vaultCA = vaultCA;
            return this;
        }

        public String getVaultCA() {
            return this.vaultCA;
        }

        public Builder setRoleSet(String roleSet) {
            this.roleSet = roleSet;
            return this;
        }

        public String getRoleSet() {
            return this.roleSet;
        }

        public Builder setHttpTransportFactory(HttpTransportFactory transportFactory) {
            this.transportFactory = transportFactory;
            return this;
        }

        public HttpTransportFactory getHttpTransportFactory() {
            return transportFactory;
        }

        public VaultCredentials build() {
            return new VaultCredentials(this);
        }
    }

    static class DefaultHttpTransportFactory implements HttpTransportFactory {
        public HttpTransport create() {
            return HTTP_TRANSPORT;
        }
    }
}