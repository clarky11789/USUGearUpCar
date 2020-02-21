import random
from random import randint
import json
import requests
import time

def main():
    outputs = ["spe", "cha", "cur", "vol"]
    carId = 1
    while True:
        try:
            outputData = [carId, outputs[randint(0, 3)], randint(25, 75)]
            print(outputData)
            requests.post(
                url = "http://localhost:3000/update", 
                data = json.dumps(outputData), 
                headers = {'Content-type': 'application/json', 'Accept': 'text/plain'}
            )

            time.sleep(3)
        except Error:
            pass

if __name__ == "__main__":
    main()  