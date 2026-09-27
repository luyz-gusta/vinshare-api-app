#!/usr/bin/env bash
# CA de laboratório, certificado do broker e um certificado por veículo/serviço.
# O CN do certificado do veículo é o VIN, usado como identidade na ACL.
set -euo pipefail
# Git Bash (Windows) converteria "/CN=..." em caminho de arquivo; no Linux não tem efeito.
export MSYS_NO_PATHCONV=1
cd "$(dirname "$0")"
mkdir -p out && cd out

openssl req -x509 -newkey rsa:3072 -sha256 -days 365 -nodes \
  -keyout ca.key -out ca.crt -subj "/CN=VinShare IoT Lab CA"

issue() { # $1 = arquivo, $2 = CN, $3 = opções extras do openssl x509
  openssl req -newkey rsa:2048 -nodes -keyout "$1.key" -out "$1.csr" -subj "/CN=$2"
  # shellcheck disable=SC2086 # $3 carrega opções extras e precisa ser dividido em palavras
  openssl x509 -req -in "$1.csr" -CA ca.crt -CAkey ca.key -CAcreateserial \
    -out "$1.crt" -days 365 -sha256 ${3:-}
}

printf "subjectAltName=DNS:localhost,DNS:mosquitto\n" > server.ext
issue server mosquitto "-extfile server.ext"
issue veiculo-9BFTESTE000000001 9BFTESTE000000001
issue veiculo-9BFTESTE000000002 9BFTESTE000000002
issue ingestor ingestor
rm -f ./*.csr server.ext
echo "Certificados gerados em $(pwd)"
