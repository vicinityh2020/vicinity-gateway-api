# HOW TO - Generate Private/Key pair to authenticate your gateway

# Generate your RSA private key
openssl genrsa -out platform-key.pem 2048
# Convert your key to DER encoding (BINARY) so JAVA can work with it
openssl pkcs8 -topk8 -inform PEM -outform DER -in platform-key.pem -out platform-key.der -nocrypt
# Generate your public key in DER encoding for JAVA
openssl rsa -in platform-key.pem -pubout -outform DER -out platform-pubkey.der
# Encode your public key as PEM (ASCII) for uploading to the Platform
openssl rsa -in platform-key.pem -pubout -outform PEM -out platform-pubkey.pem
