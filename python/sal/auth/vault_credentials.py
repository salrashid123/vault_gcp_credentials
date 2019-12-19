"""Google Cloud Vault credentials.

This module provides authentication for applications where a VAULT_TOKEN is use
to acquire a google Credential object.

"""

import base64
import copy
from datetime import datetime
import json

import six
from six.moves import http_client

from google.auth import _helpers
from google.auth import credentials
from google.auth import exceptions
from google.auth import jwt

import hvac
from hvac.exceptions import Forbidden

_REFRESH_ERROR = "Unable to acquire impersonated credentials"

#  export REQUESTS_CA_BUNDLE=/apps/vault/CA_crt.pem

class Credentials(credentials.Credentials):


    vault_token = None
    vault_host = None
    roleset = None

    def __init__(
        self,
        vault_token,
        vault_host,
        roleset
    ):

        super(Credentials, self).__init__()

        if (vault_token == None):
            raise exceptions.GoogleAuthError(
                "vault_token, vault_host, roleset must be provided"
            )
        self._vault_token = vault_token
        self._vault_host = vault_host
        self._roleset = roleset
        self.token = None
        self.expiry = _helpers.utcnow()

    @_helpers.copy_docstring(credentials.Credentials)
    def refresh(self, request):
        client = hvac.Client(url=self._vault_host, token=self._vault_token)

        if client.is_authenticated() == False:
           raise exceptions.GoogleAuthError("Unable to connect to Vault Server" )
        try:
          token_response = client.secrets.gcp.generate_oauth2_access_token(self._roleset)
          self.token = token_response["data"]["token"]
          self.expiry =  datetime.fromtimestamp(token_response["data"]["expires_at_seconds"])

        except (Forbidden) as ex:
          raise exceptions.GoogleAuthError("Unable to get Vault Token:" + ex )

    @property
    def expired(self):
        return _helpers.utcnow() >= self.expiry

