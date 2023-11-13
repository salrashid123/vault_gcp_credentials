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


### Local Vault Setup

```bash
export PROJECT_ID=`gcloud config get-value core/project`
export VAULT_SERVICE_ACCOUNT=vault-svc-account@$PROJECT_ID.iam.gserviceaccount.com

gcloud iam service-accounts create vault-svc-account --display-name "Vault Service Account"
gcloud iam service-accounts keys create vault-svc.json --iam-account=$VAULT_SERVICE_ACCOUNT 

gcloud projects add-iam-policy-binding $PROJECT_ID --member=serviceAccount:$VAULT_SERVICE_ACCOUNT --role=roles/iam.serviceAccountAdmin
gcloud projects add-iam-policy-binding $PROJECT_ID --member=serviceAccount:$VAULT_SERVICE_ACCOUNT --role=roles/iam.serviceAccountKeyAdmin
gcloud projects add-iam-policy-binding $PROJECT_ID --member=serviceAccount:$VAULT_SERVICE_ACCOUNT --role=roles/storage.admin

export GOOGLE_APPLICATION_CREDENTIALS=/path/to/vault-svc.json

vault server -config=server.conf 

## in a new window
export VAULT_ADDR='https://vault.domain.com:8200'
export VAULT_CACERT=certs/tls-ca-chain.pem
vault operator init

export VAULT_TOKEN=[Initial Root Token]
## unseal with three keyshares
vault  operator unseal 

## enable GCP
vault secrets enable gcp

## create binding
vault policy write token-policy  token_policy.hcl

gsutil mb gs://$PROJECT_ID-bucket

cat <<EOF > gcs.hcl
resource "buckets/$PROJECT_ID-bucket" {
        roles = ["roles/storage.objectViewer"]
}
EOF

vault write gcp/roleset/my-token-roleset \
   project="$PROJECT_ID"    secret_type="access_token" \
   token_scopes="https://www.googleapis.com/auth/cloud-platform"    bindings=@gcs.hcl

## get a VAULT_TOKEN and embed it into the go `VaultTokenConfig` below
vault token create -policy=token-policy 
```

### Golang

Golang library Hashicorp provides is one of the few "official" libraries for Vault and is quite robust.  The following snippet uses that library internally in a repo maintained here which gives various GCP `TokenSources` (of various degrees of usefulness)

- [Sals' unofficial TokenSource](https://github.com/salrashid123/oauth2#usage-vaulttokensource)



```golang
import (
        sal "github.com/salrashid123/oauth2/vault"
)

	tokenSource, err := sal.VaultTokenSource(
		&sal.VaultTokenConfig{
			VaultToken:  "s.ENYXI72vwdZ9rTs02PWpi8pS",
			VaultPath:   "gcp/token/my-token-roleset",
			VaultCAcert: "../certs/tls-ca-chain.pem",
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
