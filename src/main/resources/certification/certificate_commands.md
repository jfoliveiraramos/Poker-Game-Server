# Generate Server certificate
## Generate a private key for the server
```shell
openssl genpkey -algorithm RSA -out server_private_key.pem -pkeyopt rsa_keygen_bits:2048
```

## Generate a certificate signing request for the server
```shell
openssl req -new -key server_private_key.pem -out server_certificate_request.csr
```

## Generate a self-signed certificate for the server
```shell
openssl x509 -req -days 365 -in server_certificate_request.csr -signkey server_private_key.pem -out server_certificate.pem
```

# Generate server keystore
```shell
openssl pkcs12 -export -in server_certificate.pem -inkey server_private_key.pem -out server_keystore.p12
```

# Generate client truststore
## Convert the server certificate to DER format (.cer)
```shell
openssl x509 -outform der -in server_certificate.pem -out server_certificate.cer
```

## Import the server certificate into the client truststore
```shell
keytool -importcert -file server_certificate.cer -alias server-cert -keystore client_truststore.jks -storetype JKS
```
