"""Simula a telemetria (odômetro) de um veículo conectado via MQTT sobre TLS mútuo.

Uso: python publicar_telemetria.py [VIN] [TOPICO]
"""
import json
import random
import ssl
import sys
import time

import paho.mqtt.client as mqtt

VIN = sys.argv[1] if len(sys.argv) > 1 else "9BFTESTE000000001"
TOPIC = sys.argv[2] if len(sys.argv) > 2 else f"vehicles/{VIN}/telemetry"
CERTS = "../certs/out"

client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2, client_id=f"veiculo-{VIN}")
client.tls_set(ca_certs=f"{CERTS}/ca.crt",
               certfile=f"{CERTS}/veiculo-{VIN}.crt",
               keyfile=f"{CERTS}/veiculo-{VIN}.key",
               tls_version=ssl.PROTOCOL_TLS_CLIENT)
client.connect("localhost", 8883)
client.loop_start()

km = random.randint(10_000, 60_000)
for _ in range(5):
    km += random.randint(5, 40)
    payload = json.dumps({"vin": VIN, "odometerKm": km, "ts": int(time.time())})
    client.publish(TOPIC, payload, qos=1).wait_for_publish()
    print(f"publicado em {TOPIC}: {payload}")
    time.sleep(1)

client.loop_stop()
client.disconnect()
