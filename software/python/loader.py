# -*- coding: utf-8 -*-
from asyncore import write

import serial
import sys
import time

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
    CMD_ERASE_PAGE = 6
    CMD_WRITE_FLASH_PAGE = 7
    CMD_TRANSFER_PAGE = 8

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
        self._cmd(Bootloader.CMD_ABOUT)
        info = {
            'signature': self._read_char() + self._read_char() + self._read_char() + self._read_char(),
            'version': self._read_word(),
            'bootloader_start': self._read_dword(),
            'bootloader_size': self._read_word(),
            'page_size': self._read_word(),
            'device_signature': (self._read() << 16) + (self._read() << 8) + self._read()
        }
        return info

    def read_flash(self, addr_in_16_byte_pages, size16):
        self._cmd(Bootloader.CMD_READ_FLASH, _bytes(addr_in_16_byte_pages), _bytes(size16))
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

    def erase_page(self, page_number):
        self._cmd(Bootloader.CMD_ERASE_PAGE, _bytes(page_number))

    def write_flash_page(self, page_number):
        self._cmd(Bootloader.CMD_WRITE_FLASH_PAGE, _bytes(page_number))
        self._read()    # TODO == 0 ???

    def transfer_page(self, page_data):
        self._cmd(Bootloader.CMD_TRANSFER_PAGE, page_data)

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
            elif type(a) is list:
                for v in a:
                    self._write(v)
            else:
                self._write(a)

    def _read(self):
        b = ord(self.serial.read())
        return b

    def _read_char(self):
        return self.serial.read()

    def _read_word(self):
        return (self._read() << 8) + self._read()

    def _read_dword(self):
        return (self._read_word() << 16) + self._read_word()





class Loader:
    def __init__(self, port, baudrate):
        self.dev = Bootloader(port, baudrate)
        if self.dev.sync(123) != 123:
            print "Can't connect to bootloader"
            sys.exit(-1)
        about = self.dev.get_about()
        if about['signature'] != 'TSBL':
            print "Wrong bootloader signature"
            sys.exit(-1)
        self.page_size = about['page_size']
        self.bootloader_size = about['bootloader_size']
        self.bootloader_start = about['bootloader_start']
        self.firmware_pages = self.bootloader_start / self.page_size
        print 'page sizes', self.page_size
        print 'pages', self.firmware_pages

    def read_all_flash(self, with_loader):
        start = time.time()
        size = self.bootloader_start
        if with_loader:
            size += self.bootloader_size
        #return self.read_flash(0, size)
        ret = self.read_flash(0, size)
        tm = time.time() - start
        print 'read flash time', tm, 'speed=', 1e6*tm/size, 'us/byte'
        return ret

    def read_flash(self, offset, size):
        result = []
        while size > 0:
            read_size = size if size < 0xffff else 0xffff
            dt = self.dev.read_flash(offset >> 4, read_size)
            result.extend(dt)
            offset += read_size
            size -= read_size
        return result

    def _find_changed_pages(self, data):
        if len(data) > self.bootloader_start:
            data = data[0:self.bootloader_start]
        pages_in_data = len(data) / self.page_size
        if pages_in_data*self.page_size < len(data):
            pages_in_data += 1
        print 'pages_in_data', pages_in_data
        read = self.read_flash(0, pages_in_data*self.page_size)

        changed_pages = pages_in_data*[False]
        changes_count = 0
        # TODO detect if page is empty !!!
        for page in range(0, pages_in_data):
            data_page_is_empty = True
            for o in range(0, self.page_size):
                if data[page * self.page_size + o] != 0xff:
                    data_page_is_empty = False
                    break
            if data_page_is_empty:
                continue
            for o in range(0, self.page_size):
                if data[page * self.page_size + o] != read[page * self.page_size + o]:
                    changed_pages[page] = True
                    print '! offset', o, 'page', page, '->', hex(page * self.page_size + o), '    data =', hex(data[page * self.page_size + o]), 'vs readed =', hex(read[page * self.page_size + o])
                    changes_count += 1
                    break
        if changes_count == 0:
            print 'No changes'
        else:
            print 'changed pages', changes_count, 'from', len(changed_pages)

#        print changed_pages
        return changed_pages

    def write_flash(self, data):
        while len(data) < self.firmware_pages * self.page_size:
            data.append(0xff)

        changed_pages = self._find_changed_pages(data)

        start = time.time()
        write_counter = 0
        for page in range(0, len(changed_pages)):
            if not changed_pages[page]:
                continue
            self.dev.transfer_page(data[page*self.page_size:page*self.page_size + self.page_size])
            #print 'erase', page
            self.dev.erase_page(page)
            #print 'write', page
            self.dev.write_flash_page(page)
            write_counter += 1
        tm = time.time() - start
        if write_counter > 0:
            print 'write flash time', tm, 1e6*tm/write_counter/self.page_size, 'us/byte'


    def read_and_save_flash(self, filename, with_bootloader):
        _df = DataFile([])
        _df.data = self.read_all_flash(with_bootloader)
        print 'Read', len(_df.data), 'bytes'
        _df.save(filename)





def print_dump(lst):
    s = ''
    i = 0
    for v in lst:
        vs = hex(v)[2:]
        i += 1
        if len(vs) == 1:
            vs = '0' + vs
        s += vs + ' '
        if (i % 16) == 0:
            print s
            s = ''

#fw = DataFile('/Users/trol/Projects/radio/avr-lcd-module-128x128/build/avr-lcd-module-128x128.hex')
fw = DataFile('/Users/trol/Projects/radio/avr-ic-tester-v2/firmware/tester/build/ic-tester-main.hex')


# read 230400       44.0383300884 us/byte
#write 234000       256.21552423 us/byte
#                   255.434597666 us/byte
#l = Loader('/dev/tty.wchusbserial14230', 57600)
#l = Loader('/dev/tty.wchusbserial14230', 230400)
l = Loader('/dev/tty.wchusbserial14220', 153600)
print l.dev.get_about()


l.read_and_save_flash('flash_with_loader.hex', True)
l.read_and_save_flash('flash_without_loader.hex', False)
l.write_flash(fw.data)
l.dev.start_app()
1/0

# df = DataFile([])
#
# df.load('flash.hex')
# df.save('flash2.hex')

dev = Bootloader('/dev/tty.wchusbserial14230', 57600)
print dev.sync(10)
print dev.get_about()

eeprom = dev.read_eeprom(0, 1024)
# flash = dev.read_flash(0x7000, 0x1000)
flash = dev.read_flash(0x0000, 32*1024)
# print_dump(flash)
print_dump(flash)
df = DataFile(flash)
df.save('flash.hex')
dev.start_app()
