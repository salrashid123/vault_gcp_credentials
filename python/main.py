from sal.auth import vault_credentials

from google.cloud import storage

#  export REQUESTS_CA_BUNDLE=/apps/vault/CA_crt.pem

target_credentials = vault_credentials.Credentials(
          vault_token="s.Mp3to4vHdFuaYBJVdY50saUB",
          vault_host="https://grpc.domain.com:8200",
          roleset="my-token-roleset")

client = storage.Client(credentials=target_credentials)

blobs = client.list_blobs('clamav-241815')
for blob in blobs:
    print(blob.name)