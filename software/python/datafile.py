# -*- coding: utf-8 -*-
import sys

import strutils

__author__ = 'Trol'


class DataFile:

    MODE_HEX = 0
    MODE_BINARY = 1

    def __init__(self, data):
        print data
        if type(data) is list:
            self.data = data
        elif type(data) is str:
            self.data = []
            self.load(data)
        self.mode = None

    def save(self, filename):
        if filename.lower().endswith('.bin'):
            self.mode = DataFile.MODE_BINARY
        elif filename.lower().endswith('.hex'):
            self.mode = DataFile.MODE_HEX

        if self.mode == DataFile.MODE_BINARY:
            self._save_binary(filename)
        elif self.mode == DataFile.MODE_HEX:
            self._save_hex(filename)
        else:
            print 'Wrong file mode', self.mode
            sys.exit()

    def _save_binary(self, filename):
        with open(filename, "wb") as f:
            for b in self.data:
                f.write(chr(b))

    def _save_hex(self, filename):
        offset = 0
        length = len(self.data)
        with open(filename, "wb") as f:
            while offset < length:
                l = 16
                if offset + l > length:
                    l = length - offset
                crc = 0
                s = ':'
                s += strutils.hex2str(l, 1)
                crc += l
                s += strutils.hex2str(offset, 2)
                crc += (offset >> 8) & 0xFF
                crc += offset & 0xFF
                s += strutils.hex2str(0, 1)
                for i in range(0, l):
                    s += strutils.hex2str(self.data[offset+i], 1)
                    crc += self.data[offset+i]
                crc = - crc
                s += strutils.hex2str(crc, 1)
                s += "\x0D\x0A"
                offset += l
                f.write(s)

    def load(self, filename):
        if filename.lower().endswith('.bin'):
            self.mode = DataFile.MODE_BINARY
        elif filename.lower().endswith('.hex'):
            self.mode = DataFile.MODE_HEX

        if self.mode == DataFile.MODE_BINARY:
            self._load_binary(filename)
        elif self.mode == DataFile.MODE_HEX:
            self._load_hex(filename)
        else:
            print 'Wrong file mode', self.mode
            sys.exit()

    def _load_binary(self, filename):
        with open(filename, "rb") as f:
            self.data = []
            while True:
                b = f.read(1)
                if b == "":
                    break
                self.data.append(ord(b))

    def _load_hex(self, filename):
        base_address = 0
        linear_mode = True
        with open(filename, "rb") as f:
            lines = f.readlines()
            dt = {}
            for line in range(0, len(lines)):
                s = lines[line].strip().lower()
                if len(s) == 0:
                    continue
                if s[0] != ':':
                    print filename, 'error in line', line+1, '":" expected'
                    sys.exit(-1)
                if len(s) < 11:
                    print filename, 'error in line', line + 1
                    sys.exit(-1)
                pos = 1

                l = strutils.hex2byte(s[pos:pos+2])
                pos += 2
                address = strutils.hex2word(s[pos:pos+4])
                pos += 4
                s_type = strutils.hex2byte(s[pos:pos+2])
                pos += 2

                # check CRC
                crc = 0
                for i in range(1, len(s), 2):
                    b = strutils.hex2byte(s[i:i+2])
                    if b < 0:
                        print filename, 'error in line', line+1, 'column', i+1
                        sys.exit(-1)
                    crc += b
                    crc &= 0xff
                if crc != 0:
                    print filename, 'CRC error at line', line+1, crc % 0xff
                    sys.exit(-1)
                if s_type == 0:
                    if linear_mode:
                        for i in range(0, l):
                            b = strutils.hex2byte(s[pos:pos+2])
                            pos += 2
                            dt[address+i] = b
                    else:
                        for i in range(0, l):
                            b = strutils.hex2byte(s[pos:pos+2])
                            pos += 2
                            dt[base_address+address+i] = b
                elif s_type == 1:
                    break
                elif s_type == 2:
                    linear_mode = False
                    if address != 0:
                        print filename, 'invalid offset (not zero) at line', line+1
                    base_address = strutils.hex2word(s[pos:pos+4]) * 16
                    pos += 4
                elif s_type == 3:
                    pass
                else:
                    print filename, 'unsupported record type', line+1, crc % 0xff
                    sys.exit(-1)

        sz = 0
        for o in dt.keys():
            if o >= sz:
                sz = o + 1

        self.data = sz*[None]
        for o in dt.keys():
            self.data[o] = dt[o]



