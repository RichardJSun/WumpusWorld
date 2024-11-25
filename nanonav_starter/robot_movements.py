#  - By: sathya - Mon Nov 25 2024

from nanonav import NanoBot
import time

class Robot:
    def __init__(self, speed = 20, counts_per_rotation = 2000, tile_size = 30, debug = False):
        print("### INITIALIZING ROBOT ###")
        self.robot = NanoBot()
        # Initializing infrared sensors
        self.left_ir = self.robot.ir_left()
        self.right_ir = self.robot.ir_right()
        #Initializing encoders
        self.enc1 = self.robot.get_enc1()
        self.enc2 = self.robot.get_enc2()
        self.speed = speed
        self.counts_per_rotation = counts_per_rotation
        self.tile_size = tile_size
        self.debug = debug
        print("### COMPLETE INITIALIZING ROBOT ###")


    def moveForward(self, squares):
        for i in range(squares):
            # Resetting infrared sensors
            self.left_ir = self.robot.ir_left()
            self.right_ir = self.robot.ir_right()
            while not self.left_ir and not self.right_ir:
                if self.debug:
                    print("DEBUG: IR sensor only detected black so far")
                self.robot.m1_forward(self.speed)
                self.robot.m2_forward(self.speed)
                self.left_ir = self.robot.ir_left()
                self.right_ir = self.robot.ir_right()
                time.sleep(0.1)

            if self.debug:
                print("DEBUG: IR sensors detected white.")

            self.moveToMiddle(self.tile_size)
            self.robot.stop()

    def moveBackward(self, squares):
        for i in range(squares):
            # Resetting infrared sensors
            self.left_ir = self.robot.ir_left()
            self.right_ir = self.robot.ir_right()
            while not self.left_ir and not self.right_ir:
                if self.debug:
                    print("DEBUG: IR sensor only detected black so far")
                self.robot.m1_backward(self.speed)
                self.robot.m2_backward(self.speed)
                self.left_ir = self.robot.ir_left()
                self.right_ir = self.robot.ir_right()
                time.sleep(0.1)

            if self.debug:
                print("DEBUG: IR sensors detected white.")

            self.moveToMiddle(self.tile_size)
            self.robot.stop()

    def moveLeft(self, rotations):
        target_encoder_count = rotations * 180
        self.robot.set_enc1(0)
        self.robot.set_enc2(0)
        time.sleep(0.1)

        while abs(self.robot.get_enc1()) < target_encoder_count and abs(self.robot.get_enc2()) < target_encoder_count:
           self.robot.m1_backward(self.speed)
           self.robot.m2_forward(self.speed)
           time.sleep(0.1)

        self.robot.stop()

    def moveRight(self, rotations):
        target_encoder_count = rotations * 180
        self.robot.set_enc1(0)
        self.robot.set_enc2(0)
        time.sleep(0.1)

        while abs(self.robot.get_enc1()) < target_encoder_count and abs(self.robot.get_enc2()) < target_encoder_count:
           self.robot.m1_forward(self.speed)
           self.robot.m2_backward(self.speed)
           time.sleep(0.1)

        self.robot.stop()


    def moveToMiddle(self, tile_size):
        distance = tile_size // 2
        target_encoder_count = distance * self.counts_per_rotation / 100

        # Zero encoders
        self.robot.set_enc1(0)
        self.robot.set_enc2(0)
        time.sleep(0.1)

        while abs(self.robot.get_enc1()) < target_encoder_count and abs(self.robot.get_enc2()) < target_encoder_count:
           self.robot.m1_forward(self.speed)
           self.robot.m2_forward(self.speed)
           time.sleep(0.1)

        self.robot.stop()

if __name__ == "__main__":
    robot = Robot(speed = 30, tile_size = 20, debug = True)
    print("Moving forward 1 time")
    robot.moveForward(1)
    robot.moveLeft(1)
    robot.moveForward(1)
    robot.moveLeft(1)
    robot.moveForward(1)
    robot.moveLeft(1)
    robot.moveForward(1)





