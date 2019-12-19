package main

import (
	"log"
	"net/http"

	"context"

	"golang.org/x/oauth2"

	sal "github.com/salrashid123/oauth2/google"

	"github.com/golang/glog"

	"google.golang.org/api/iterator"

	"cloud.google.com/go/storage"
	"google.golang.org/api/option"
)

const ()

var ()

func main() {

	glog.V(2).Infof("======= Init  ========")

	tokenSource, err := sal.VaultTokenSource(
		&sal.VaultTokenConfig{
			VaultToken:  "s.Mp3to4vHdFuaYBJVdY50saUB",
			VaultPath:   "gcp/token/my-token-roleset",
			VaultCAcert: "/apps/vault/crt_vault.pem",
			VaultAddr:   "https://grpc.domain.com:8200",
		},
	)

	// tok, err := kmsTokenSource.Token()
	// if err != nil {
	// 	log.Fatal(err)
	// }
	// log.Printf("Token: %v", tok.AccessToken)
	client := &http.Client{
		Transport: &oauth2.Transport{
			Source: tokenSource,
		},
	}

	url := "https://storage.googleapis.com/storage/v1/b/clamav-241815/o"
	resp, err := client.Get(url)
	if err != nil {
		glog.Fatal(err)
	}
	log.Printf("Response: %v", resp.Status)

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
}
