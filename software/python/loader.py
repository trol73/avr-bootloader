# -*- coding: utf-8 -*-
import serial

import strutils
from datafile import DataFile

__author__ = 'Trol'


# Установка:
# python -m pip install pyserial


def _bytes(i):
    return divmod(i, 0x100)


class Bootloader:
    CMD_SYNC = 0
    CMD_ABOUT = 1
    CMD_READ_FLASH = 2
    CMD_READ_EEPROM = 3
    CMD_READ_FUSES = 4
    CMD_START_APP = 5

    def __init__(self, port_name, bauds):
        self.serial = serial.serial_for_url(port_name, baudrate=bauds, timeout=1.0)
        res1 = self.sync(1)
        if res1 != 1 and res1 > 0:
            cnt = 1
            while True:
                try:
                    self._read()
                    cnt += 1
                except:
                    break
            print 'skip', cnt, 'bytes'
        self.sync(100)

    def close(self):
        self.serial.close()

    def sync(self, val):
        self._cmd(Bootloader.CMD_SYNC, val)
        try:
            return self._read()
        except:
            return -1

    def get_about(self):
        pass  # TODO

    def read_flash(self, addr16, size16):
        self._cmd(Bootloader.CMD_READ_FLASH, _bytes(addr16), _bytes(size16))
        result = []
        for i in range(0, size16):
            v = self._read()
            result.append(v)
        return result

    def read_eeprom(self, addr16, size16):
        self._cmd(Bootloader.CMD_READ_EEPROM, _bytes(addr16), _bytes(size16))
        result = []
        for i in range(0, size16):
            v = self._read()
            result.append(v)
        return result

    def read_fuses(self):
        pass

    def start_app(self):
        self._cmd(Bootloader.CMD_START_APP)

    def _read_all(self):
        while True:
            try:
                self.serial.read()
                return
            except:
                return

    def _write(self, b):
        self.serial.write(chr(b))

    def _cmd(self, *args):
        for a in args:
            if type(a) is tuple:
                for v in a:
                    self._write(v)
            else:
                self._write(a)

    def _read(self):
        b = ord(self.serial.read())
        return b


def print_dump(lst):
    s = ''
    i = 0;
    for v in lst:
        vs = hex(v)[2:]
        i += 1
        if len(vs) == 1:
            vs = '0' + vs
        s += vs + ' '
        if (i % 16) == 0:
            print s
            s = ''


df = DataFile([])
df.load('flash.hex')
df.save('flash2.hex')
1/0

dev = Bootloader('/dev/tty.wchusbserial14230', 57600)
print dev.sync(10)
eeprom = dev.read_eeprom(0, 1024)
# flash = dev.read_flash(0x7000, 0x1000)
flash = dev.read_flash(0x0000, 32*1024)
# print_dump(flash)
print_dump(flash)
df = DataFile(flash)
df.save('flash.hex')
dev.start_app()
