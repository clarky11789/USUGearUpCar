from bluetooth import *
import random
from random import randint
import time
import serial

server_sock = BluetoothSocket( RFCOMM )

server_sock.bind(("", PORT_ANY))
server_sock.listen(1)
port = server_sock.getsockname()[1]

print("Waiting for device...")

client_sock, client_info = server_sock.accept(2)
print("Accepted connection from ", client_info)

#vars
outputNames = [ "speed: ", "charge: ", "current: ", "voltage: " ]
random.seed()
while True:
    try:
        outputString = outputNames[randint(0, 3)] + str(randint(25, 75)) + "\n"
        client_sock.send(ouputString)
        time.sleep(3)
    except IOErrer as ioerr:
        print(ioerr)
        pass

client_sock.close()
server_sock.close()
