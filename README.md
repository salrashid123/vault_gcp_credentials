# Google Credentials from VAULT_TOKEN

Utility libraries that derives Google Cloud Credentials from a `VAULT_TOKEN`.

[Vault](https://www.vaultproject.io/) Secrets for Google Cloud can return the raw google `access_token`  which can be used to access GCP resource as specified by the token's capabilities.

Developers are left with a raw `access_token` which then must be plumbed into language specific artifiacts like [StaticTokenSource](https://godoc.org/golang.org/x/oauth2#StaticTokenSource) in golang or the deprecated oauth2client's [AccessTokenCredentials](https://oauth2client.readthedocs.io/en/latest/source/oauth2client.client.html#oauth2client.client.AccessTokenCredentials)...or just inject header manually into requests..

This repo provides a shortcut which will acquire the `access_token` from Vault and then construct `Credential` objects that the google cloud client libraries understand.  You will provide the library below the `VAULT_TOKEN`, `RoleSet` and `VAULT_HOST` to connect to and out pops a GCP Credential...its magic


## References

- [Vault Secrets: GCP access_token](https://www.vaultproject.io/docs/secrets/gcp/index.html#access-tokens)
- [Vault Secrets on GCP Samples](https://github.com/salrashid123/vault_gcp#accesstoken)
- [Vault client library support](https://www.vaultproject.io/api/libraries.html)

>> This repo is _NOT_ supported by Google; _caveat emptor_


TODO: 
* Account for `VAULT_TOKEN` expiration checks, renew count somehow.  golang does a bit of that now; python will not refresh vault_token
* Make custom CA usage easier: you have to specify the CA directly inline in golang, java;
* Support env-var based `VAULT_CONFIG` (most of the libraries cited below do that already; i just got lazy)

yes, there's a lot; i'll gladly take PRs (or take and use in your own code..)


---

I'm going to assume youv'e setup Vault with GCP access_token secrets already...and have confirmed you can get a token given a `VAULT_TOKEN`


### Golang

Golang library Hashicorp provides is one of the few "official" libraries for Vault and is quite robust.  The follwoing snippet uses that library internally in a repo maintained here which gives various GCP `TokenSources` (of various degress of usefulness)

- [Sals' unofficial TokenSource](https://github.com/salrashid123/oauth2#usage-vaulttokensource)


```golang
import (
        sal "github.com/salrashid123/oauth2/google"
)

	tokenSource, err := sal.VaultTokenSource(
		&sal.VaultTokenConfig{
			VaultToken:  "s.Mp3to4vHdFuaYBJVdY50saUB",
			VaultPath:   "gcp/token/my-token-roleset",
			VaultCAcert: "/path/to/your/vaultCA/crt_vault.pem",
			VaultAddr:   "https://vault.domain.com:8200",
		},
	)

	ctx := context.Background()
	sclient, err := storage.NewClient(ctx, option.WithTokenSource(tokenSource))
	if err != nil {
		log.Fatal(err)
	}

	it := sclient.Bucket("clamav-241815").Objects(ctx, nil)
	for {
		attrs, err := it.Next()
		if err == iterator.Done {
			break
		}
		if err != nil {
			log.Fatal(err)
		}
		log.Printf(attrs.Name)
	}
```

### Python

For python, the following uses [https://hvac.readthedocs.io/](https://hvac.readthedocs.io/en/stable/overview.html) which does support gcp secrets:

First you must setup the trust store for python which internally uses python-requests and honors the env vars.  You'll def need to bundle the certs together as its documentd:

```bash
cp "$(python -c 'import certifi;print certifi.where();')" /tmp/bundle.pem
cat /apps/vault/CA_crt.pem >> /tmp/bundle.pem
export REQUESTS_CA_BUNDLE=/tmp/bundle.pem
```

```python
from sal.auth import vault_credentials
from google.cloud import storage


target_credentials = vault_credentials.Credentials(
          vault_token="s.Mp3to4vHdFuaYBJVdY50saUB",
          vault_host="https://vault.domain.com:8200",
          roleset="my-token-roleset")

client = storage.Client(credentials=target_credentials)

blobs = client.list_blobs('clamav-241815')
for blob in blobs:
    print(blob.name)
```


### Java

>> do not use the java stuff in this repo; its incomplete..

The java library for vault does not support GCP secrets...but if it did, the baseline i've got here in the repo should the start of it

```java
import com.bettercloud.vault.Vault;

        VaultCredentials vc = VaultCredentials.newBuilder().setVaultToken("s.Mp3to4vHdFuaYBJVdY50saUB")
                .setVaultHost("https://vault.domain.com:8200").setVaultCA("/path/to/your/vaultCA/crt_vault.pem")
                .setRoleSet("gcp/token/my-token-roleset").build();
        Storage service = StorageOptions.newBuilder().setCredentials(vc).build().getService();

        Bucket bucket = service.get("clamav-241815");
        Page<Blob> blobs = bucket.list();
        for (Blob blob : blobs.iterateAll())
        System.out.println(blob.getName());
```

## Anything else, and java

you can ofcourse just use a raw REST interface to Vault, get a token, then construct the Credential as normal.  THe framework is already there..you'll just need to setup the credential object as expected by gcp's library set.   If you really need java, pls file a PR (i'll just drop the library i used and make one up in HTTPClient)