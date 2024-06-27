package main

import (
	"log"
	"net/http"

	"context"

	"golang.org/x/oauth2"

	vaultcredentials "github.com/salrashid123/vault_gcp_credentials"

	"google.golang.org/api/iterator"

	"cloud.google.com/go/storage"
	"google.golang.org/api/option"
)

const ()

var ()

func main() {

	log.Printf("======= Init  ========")

	tokenSource, err := vaultcredentials.VaultTokenSource(
		&vaultcredentials.VaultTokenConfig{
			VaultToken:  "s.mwkBs0T0jt9rfBZ61mmxzRYi",
			VaultPath:   "gcp/token/my-token-roleset",
			VaultCAcert: "certs/tls-ca-chain.pem",
			VaultAddr:   "https://vault.domain.com:8200",
		},
	)

	client := &http.Client{
		Transport: &oauth2.Transport{
			Source: tokenSource,
		},
	}

	url := "https://storage.googleapis.com/storage/v1/b/core-eso-bucket/o"
	resp, err := client.Get(url)
	if err != nil {
		log.Fatal(err)
	}
	log.Printf("Response: %v", resp.Status)

	ctx := context.Background()
	sclient, err := storage.NewClient(ctx, option.WithTokenSource(tokenSource))
	if err != nil {
		log.Fatal(err)
	}

	it := sclient.Bucket("core-eso-bucket").Objects(ctx, nil)
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
}
