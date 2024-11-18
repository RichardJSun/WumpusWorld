from nanonav import BLE, NanoBot
import time

### test motors and encoders ###

# Create a NanoBot object
robot = NanoBot()

# Move forward for 2 seconds
print(f'encoder 1 start: {robot.get_enc1()}')
robot.m1_forward(30)
robot.m2_forward(30)
time.sleep(2)
print(f'encoder 1 end: {robot.get_enc1()}')

# Stop
robot.stop()
time.sleep(2)

# Move backward for 2 seconds
print(f'encoder 2 start: {robot.get_enc2()}')
robot.m1_backward(30)
robot.m2_backward(30)
time.sleep(2)
print(f'encoder 2 end: {robot.get_enc2()}')

# Stop
robot.stop()

### test Bluetooth ###

# Create a Bluetooth object
ble = BLE(name="NanoNav")

ble.send(43)
response = ble.read()
# wait until something changes, indicating a response
while response == 43:
    response = ble.read()
    time.sleep(0.5)

print("Received: ", response)

### test ir sensors ###
while True:
    print(f'left: {robot.ir_left()}    right: {robot.ir_right()}')
    time.sleep(0.5)
