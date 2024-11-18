from ble_advertising import advertising_payload
import bluetooth
from machine import Pin, PWM, ADC, freq
import machine
from micropython import const
import rp2

import time

# Define BLE constants (these are not packaged in bluetooth for space efficiency)
_IO_CAPABILITY_DISPLAY_ONLY = const(0)
_FLAG_READ = const(0x0002)
_FLAG_WRITE = const(0x0008)
_IRQ_CENTRAL_CONNECT = const(1)
_IRQ_CENTRAL_DISCONNECT = const(2)
_IRQ_GATTS_WRITE = const(3)

class BLE:
    def __init__(self, ble=bluetooth.BLE(), name="NANO RP2040"):
        # Setup bluetooth low energy communication service
        _SERVICE_UUID = bluetooth.UUID(0x1523) # unique service id for the communication
        _NanoNav_CHAR_UUID = (bluetooth.UUID(0x1525), _FLAG_WRITE | _FLAG_READ) # characteristic
        _NanoNav_SERVICE = (_SERVICE_UUID, (_NanoNav_CHAR_UUID,),) # service to provide the characteristic

        self._ble = ble
        self._ble.active(True)
        self._ble.config(
            bond=True,
            mitm=True,
            le_secure=True,
            io=_IO_CAPABILITY_DISPLAY_ONLY
        )
        self._ble.irq(self._irq)
        ((self._handle,),) = self._ble.gatts_register_services((_NanoNav_SERVICE,))
        self._connections = set()
        self._payload = advertising_payload(name=name, services=[_SERVICE_UUID])
        self._advertise()
        self.value = b'a'

    def _advertise(self, interval_us=500000):
        self._ble.gap_advertise(interval_us, adv_data=self._payload)

    def _irq(self, event, data):
        # handle bluetooth event
        if event == _IRQ_CENTRAL_CONNECT:
            # handle succesfull connection
            conn_handle, addr_type, addr = data
            self._connections.add(conn_handle)

            self.on_connected()

        elif event == _IRQ_CENTRAL_DISCONNECT:
            # handle disconnect
            conn_handle, _, _ = data
            self._connections.remove(conn_handle)
            self._advertise()

            self.on_disconnected()

        elif event == _IRQ_GATTS_WRITE:
            conn_handle, value_handle = data
            if conn_handle in self._connections:
                # Value has been written to the characteristic
                self.value = self._ble.gatts_read(value_handle)

    def on_connected(self):
        pass

    def on_disconnected(self):
        pass

    def send(self, value):
        if not isinstance(value, bytes):
            if isinstance(value, int):
                value = value.to_bytes(1, "big")
            elif isinstance(value, str):
                value = value.encode('utf-8')
            else:
                raise ValueError("send value should be type int, bytes, or string")
        self.value = value
        self._ble.gatts_write(self._handle, value)

    def read(self):
        #use the last value written to characteristic
        value = self.value
        try:
            return int.from_bytes(value, "big")
        except Exception as e:
            return None

class NanoBot:
    def __init__(self, saturated_duty=22000, *args, **kwargs):
        # turn ir sensor pin on (inactive because it's active low)
        self.ir_right_sensor = Pin(28, Pin.OUT)
        self.ir_right_sensor.on()

        time.sleep(0.5)

        # ir sensors
        self.ir_left_sensor = ADC(Pin(29, Pin.IN))
        self.ir_right_sensor = ADC(Pin(28, Pin.IN))

        # initialize frequency
        machine.freq(100000000)

        # initialize motors
        m1pin1 = Pin(4)
        m1pin2 = Pin(5)
        m2pin1 = Pin(18)
        m2pin2 = Pin(17)

        self.m1pwm1 = PWM(m1pin1)
        self.m1pwm2 = PWM(m1pin2)
        self.m2pwm1 = PWM(m2pin1)
        self.m2pwm2 = PWM(m2pin2)

        # initialize motor constants
        self.max_duty = 65535 # constant
        self.saturated_duty = saturated_duty # choice for max speed
        assert(0 <= self.saturated_duty <= self.max_duty)
        self.turn90ticks = 120
        self.turn_error = 5
        self.block_delay = 1550

        # PID controller constants
        self.battery_scaling = 1.05
        self.kp = 0.8 * self.battery_scaling
        self.ki = 0.08 * self.battery_scaling
        self.kd = 0.04 * self.battery_scaling

        # initialize encoder variables
        self.encpins = (15, 25, 7, 27)
        self.enc1p1 = Pin(self.encpins[0], Pin.IN)
        self.enc1p2 = Pin(self.encpins[1], Pin.IN)
        self.enc2p1 = Pin(self.encpins[2], Pin.IN)
        self.enc2p2 = Pin(self.encpins[3], Pin.IN)

        self.enc1 = 0
        self.enc2 = 0
        self.enc1dir = 1
        self.enc2dir = 1

        # add interrupt callbacks to track encoder ticks
        self.enc1p1.irq(lambda pin: self.enc_pin_high(self.encpins[0]), Pin.IRQ_RISING)
        self.enc1p2.irq(lambda pin: self.enc_pin_high(self.encpins[1]), Pin.IRQ_RISING)
        self.enc2p1.irq(lambda pin: self.enc_pin_high(self.encpins[2]), Pin.IRQ_RISING)
        self.enc2p2.irq(lambda pin: self.enc_pin_high(self.encpins[3]), Pin.IRQ_RISING)

        self.setup()

    def enc_pin_high(self, pin):
        if pin == self.encpins[0] or pin == self.encpins[1]:
            if self.enc1p1.value() == 1 and self.enc1p2.value() == 1:
                self.enc1 += 1 * self.enc1dir
            elif self.enc1p1.value() == 1:
                self.enc1dir = 1
            else:
                self.enc1dir = -1
        if pin == self.encpins[2] or pin == self.encpins[3]:
            if self.enc2p1.value() == 1 and self.enc2p2.value() == 1:
                self.enc2 += 1 * self.enc2dir
            elif self.enc2p1.value() == 1:
                self.enc2dir = -1
            else:
                self.enc2dir = 1

    def calc_duty(self, duty_100):
        return int(duty_100 * self.max_duty / 100)

    def m1_forward(self, duty_cycle):
        self.m1pwm1.duty_u16(min(self.calc_duty(duty_cycle), self.saturated_duty))
        self.m1pwm2.duty_u16(0)

    def m1_backward(self, duty_cycle):
        self.m1pwm1.duty_u16(0)
        self.m1pwm2.duty_u16(min(self.calc_duty(duty_cycle), self.saturated_duty))

    def m1_signed(self, duty_cycle):
        if duty_cycle >= 0:
            self.m1_forward(duty_cycle)
        else:
            self.m2_backward(-duty_cycle)

    def m2_forward(self, duty_cycle):
        self.m2pwm1.duty_u16(min(self.calc_duty(duty_cycle), self.saturated_duty))
        self.m2pwm2.duty_u16(0)

    def m2_backward(self, duty_cycle):
        self.m2pwm1.duty_u16(0)
        self.m2pwm2.duty_u16(min(self.calc_duty(duty_cycle), self.saturated_duty))

    def m2_signed(self, duty_cycle):
        if duty_cycle >= 0:
            self.m2_forward(duty_cycle)
        else:
            self.m2_backward(-duty_cycle)

    def stop(self):
        # set all duty cycles to 0
        self.m1pwm1.duty_u16(0)
        self.m1pwm2.duty_u16(0)
        self.m2pwm1.duty_u16(0)
        self.m2pwm2.duty_u16(0)

    def setup(self):
        # initialize frequencies
        self.m1pwm1.freq(1000)
        self.m1pwm2.freq(1000)
        self.m2pwm1.freq(1000)
        self.m2pwm2.freq(1000)

    def ir_left(self):
        return self.ir_left_sensor.read_u16() < 65535 // 2

    def ir_right(self):
        return self.ir_right_sensor.read_u16() < 65535 // 2

    def get_enc1(self):
        return self.enc1

    def get_enc2(self):
        return self.enc2

    def set_enc1(self, value):
        self.enc1 = value

    def set_enc2(self, value):
        self.enc2 = value
