from nanonav import NanoBot
import time

# Create a NanoBot object
robot = NanoBot()

# Define the movement speed
speed = 20

while True:
    left_ir = robot.ir_left()
    right_ir = robot.ir_right()
    print(f'left: {left_ir}    right: {right_ir}')
    time.sleep(0.5)

    while not left_ir and not right_ir:
        print("IR sensor only detected black so far")
        robot.m1_backward(speed)
        robot.m2_backward(speed)

        left_ir = robot.ir_left()
        right_ir = robot.ir_right()
        time.sleep(0.1)

    print("IR sensors detected white.")

    robot.m1_backward(speed)
    robot.m2_backward(speed)
    time.sleep(0.5)

    robot.stop()

    print("Robot has stopped in the middle of the tile.")
    break
