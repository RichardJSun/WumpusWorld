from nanonav import NanoBot
import time

# Create a NanoBot object
robot = NanoBot()

# Move forward for 2 seconds
robot.m1_forward(30)
robot.m2_forward(30)

# Print encoder values every second for 2 seconds
start_time = time.time()
while time.time() - start_time < 2:
    # Print encoder values
    print("\nEncoder 1 Value:", robot.get_enc1())
    print("Encoder 2 Value:", robot.get_enc2())
    time.sleep(1)  # Wait for 1 second before printing again

# Stop
robot.stop()

# Move backward for 2 seconds
robot.m1_backward(30)
robot.m2_backward(30)

# Print encoder values every second for 2 seconds
start_time = time.time()
while time.time() - start_time < 2:
    # Print encoder values
    print("\nEncoder 1 Value:", robot.get_enc1())
    print("Encoder 2 Value:", robot.get_enc2())
    time.sleep(1)  # Wait for 1 second before printing again

# Stop
robot.stop()
