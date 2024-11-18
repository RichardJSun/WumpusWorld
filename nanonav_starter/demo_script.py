from nanonav import NanoBot
import time

# Create a NanoBot object
robot = NanoBot()

# Move forward for 2 seconds
robot.m1_forward(30)
robot.m2_forward(30)

# Printing encoder values
print("Encoder 1 Value:", robot.get_enc1())
print("Encoder 2 Value:", robot.get_enc2())

time.sleep(2)

# Stop
robot.stop()
robot.sleep(2)

# Move backward for 2 seconds
robot.m1_backward(30)
robot.m2_backward(30)
time.sleep(2)

# Stop
robot.stop()
