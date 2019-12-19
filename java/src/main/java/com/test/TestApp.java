package com.test;

import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.sal.auth.oauth2.VaultCredentials;

import java.util.Collection;
import java.util.Iterator;
import java.io.FileInputStream;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.auth.oauth2.ComputeEngineCredentials;
import com.google.cloud.ServiceOptions;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.api.gax.paging.Page;

public class TestApp {
	public static void main(String[] args) {
		TestApp tc = new TestApp();
	}

	public TestApp() {
		try {

			String projectId = ServiceOptions.getDefaultProjectId();
			System.out.println(projectId);

			VaultCredentials vc = VaultCredentials.newBuilder().setVaultToken("s.Mp3to4vHdFuaYBJVdY50saUB")
					.setVaultHost("https://grpc.domain.com:8200").setVaultCA("/apps/vault/crt_vault.pem")
					.setRoleSet("gcp/token/my-token-roleset").build();

			Storage service = StorageOptions.newBuilder().setCredentials(vc).build().getService();

			Bucket bucket = service.get("clamav-241815");
			Page<Blob> blobs = bucket.list();
			for (Blob blob : blobs.iterateAll()) {
			  System.out.println(blob.getName());
			}

		} catch (Exception ex) {
			System.out.println("Error:  " + ex);
		}
	}

}
