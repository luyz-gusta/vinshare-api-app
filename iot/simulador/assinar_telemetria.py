"""Serviço de ingestão: assina a telemetria da frota (somente leitura pela ACL)."""
import ssl

import paho.mqtt.client as mqtt

CERTS = "../certs/out"


def on_connect(client, userdata, flags, reason_code, properties):
    print("conectado:", reason_code)
    client.subscribe("vehicles/+/telemetry", qos=1)


def on_message(client, userdata, msg):
    print(f"{msg.topic}: {msg.payload.decode()}")


client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2, client_id="ingestor")
client.tls_set(ca_certs=f"{CERTS}/ca.crt",
               certfile=f"{CERTS}/ingestor.crt",
               keyfile=f"{CERTS}/ingestor.key",
               tls_version=ssl.PROTOCOL_TLS_CLIENT)
client.on_connect = on_connect
client.on_message = on_message
client.connect("localhost", 8883)
client.loop_forever()
