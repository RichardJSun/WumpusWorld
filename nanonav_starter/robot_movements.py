#  robot_movements.py By: sathya - Mon Nov 25 2024

from nanonav import NanoBot
import time
import math

# Custom python file imports
import constants


class Robot:

    def __init__(self, speed, counts_per_rotation, wheel_diameter, wheelbase, tile_size, debug = False):

        print("### INITIALIZING ROBOT ###")

        self.robot = NanoBot()

        # Initializing infrared sensors
        self.left_ir = self.robot.ir_left()
        self.right_ir = self.robot.ir_right()

        # Initializing and resetting encoders
        self.enc1 = self.robot.get_enc1()
        self.enc2 = self.robot.get_enc2()
        self.enc1.set_enc1(0)
        self.enc2.set_enc2(0)

        self.speed = speed
        self.counts_per_rotation = counts_per_rotation
        self.wheel_diameter = wheel_diameter
        self.wheelbase = wheelbase
        self.tile_size = tile_size
        self.debug = debug

        time.sleep(0.1)
        print("### COMPLETE INITIALIZING ROBOT ###")


    def moveForward(self, squares):

        # Iterates until the number of squares argument
        for i in range(squares):

            # Resetting infrared sensors
            self.left_ir = self.robot.ir_left()
            self.right_ir = self.robot.ir_right()

            # Iterates until white color is detected
            # Note: ir returns true if white is detected
            while not self.left_ir and not self.right_ir:

                if self.debug:
                    print("DEBUG: IR sensor only detected black so far")

                # Motors spin to move directly forward
                self.robot.m1_forward(self.speed)
                self.robot.m2_forward(self.speed)

                # Fetch ir sensor values to update throughout iteration
                self.left_ir = self.robot.ir_left()
                self.right_ir = self.robot.ir_right()

                time.sleep(0.1)

            if self.debug:
                print("DEBUG: IR sensors detected white.")

            # After white is detected, robot should move to the middle of the tile
            # Setting forward argument as true to move forwards to the middle of the previous square
            self.moveToMiddle(forward = True)
            self.robot.stop()

    def moveBackward(self, squares):

        # Iterates until the number of squares argument
        for i in range(squares):

            # Resetting infrared sensors
            self.left_ir = self.robot.ir_left()
            self.right_ir = self.robot.ir_right()

            # Iterates until white color is detected
            # Note: ir returns true if white is detected
            while not self.left_ir and not self.right_ir:

                if self.debug:
                    print("DEBUG: IR sensor only detected black so far")

                # Motors spin to move directly backward
                self.robot.m1_backward(self.speed)
                self.robot.m2_backward(self.speed)

                # Fetch ir sensor values to update throughout iteration
                self.left_ir = self.robot.ir_left()
                self.right_ir = self.robot.ir_right()
                time.sleep(0.1)

            if self.debug:
                print("DEBUG: IR sensors detected white.")

            # After white is detected, robot should move to the middle of the tile
            # Setting forward argument as false to move backwards to the middle of the previous square
            self.moveToMiddle(forward = False)
            self.robot.stop()

    def moveLeft(self, rotations = 1):

        # Target encoder count is the number of encoder ticks required for the robot to complete a specific turn
        turn_circumference = math.pi * self.wheelbase
        turn_distance = (rotations * 90 / 360) * turn_circumference

        # Calculating the number of rotations to achieve the turn distance
        wheel_circumference = math.pi * self.wheel_diameter
        turn_rotations = turn_distance / wheel_circumference
        target_encoder_count = turn_rotations * self.counts_per_rotation

        # Resetting encoders to prepare for turning
        self.robot.set_enc1(0)
        self.robot.set_enc2(0)

        time.sleep(0.1)

        # Iterates until both encoder values are at the target encoder count
        while abs(self.robot.get_enc1()) < target_encoder_count and abs(self.robot.get_enc2()) < target_encoder_count:

           # Sets motor 1 to move the opposite direction as motor 2
           self.robot.m1_backward(self.speed)
           self.robot.m2_forward(self.speed)

           time.sleep(0.1)

        self.robot.stop()

    def moveRight(self, rotations = 1):

        # Target encoder count is the number of encoder ticks required for the robot to complete a specific turn
        turn_circumference = math.pi * self.wheelbase
        turn_distance = (rotations * 90 / 360) * turn_circumference

        # Calculating the number of rotations to achieve the turn distance
        wheel_circumference = math.pi * self.wheel_diameter
        turn_rotations = turn_distance / wheel_circumference
        target_encoder_count = turn_rotations * self.counts_per_rotation

        # Resetting encoders to prepare for turning
        self.robot.set_enc1(0)
        self.robot.set_enc2(0)

        time.sleep(0.1)

        # Iterates until both encoder values are at the target encoder count
        while abs(self.robot.get_enc1()) < target_encoder_count and abs(self.robot.get_enc2()) < target_encoder_count:

           # Sets motor 1 to move the opposite direction as motor 2
           self.robot.m1_forward(self.speed)
           self.robot.m2_backward(self.speed)

           time.sleep(0.1)

        self.robot.stop()


    def moveToMiddle(self, forward = True):

        # Calculating constants to move to the middle of the tile
        distance = self.tile_size // 2
        wheel_circumference = math.pi * self.wheel_diameter

        # Multiplies the number of rotations the wheel needs to make by the encoder ticks per rotation
        target_encoder_count = (distance // wheel_circumference) * self.counts_per_rotation

        # Zero encoders
        self.robot.set_enc1(0)
        self.robot.set_enc2(0)

        time.sleep(0.1)

        # Iterates until the target encoder count threshold is met
        while abs(self.robot.get_enc1()) < target_encoder_count and abs(self.robot.get_enc2()) < target_encoder_count:

            # If the robot is moving forward and needs to reach the middle
            if forward:
               self.robot.m1_forward(self.speed)
               self.robot.m2_forward(self.speed)

            # If the robot is moving backward and needs to reach the previous middle
            else:
                self.robot.m1_backward(self.speed)
                self.robot.m2_backward(self.speed)

            time.sleep(0.1)


        self.robot.stop()

    def calibrateEncoders(self, duration = 2):

        print("### STARTING ENCODER CALIBRATION ###")

        # Zero encoders
        self.robot.set_enc1(0)
        self.robot.set_enc2(0)

        print("Rotate the wheel")

        while True:

            # Getting encoder counts
            enc1_count = abs(self.robot.get_enc1())
            enc2_count = abs(self.robot.get_enc2())

            # Average the counts as there might be small deviations
            average = (enc1_count + enc2_count) // 2
            print(f"Enc1: {enc1_count}, Enc2: {enc2_count}, Avg: {average}")


if __name__ == "__main__":
    wumpusBot = Robot(
        speed = constants.RobotConstants.SPEED,
        counts_per_rotation = constants.RobotConstants.COUNTS_PER_ROTATION,
        wheel_diameter = constants.RobotConstants.WHEEL_DIAMETER,
        wheel_base = constants.RobotConstants.WHEELBASE,
        tile_size = constants.GridConstants.TILE_SIZE,
        debug = True
    )

    wumpusBot.calibrateEncoders()








