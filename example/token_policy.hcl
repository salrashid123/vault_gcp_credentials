
path "auth/token/lookup-self" {
  capabilities = ["read"]
}

path "auth/token/renew" {
  capabilities = ["update", "create"]
}

path "auth/token/lookup-accessor" {
  capabilities = [ "read", "update" ]
}

path "auth/approle/role/observatory/secret-id" {
  capabilities = ["read", "create", "update", "list"]
}

path "gcp/token/my-token-roleset" {
    capabilities = ["read"]
}