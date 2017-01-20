# -*- coding: utf-8 -*-

__author__ = 'Trol'


def hex2str(val, length):
    if length == 1:
        return nibble2hex((val >> 4) & 0x0f) + nibble2hex(val & 0x0f)
    elif length == 2:
        return hex2str((val >> 8) & 0xff, 1) + hex2str(val & 0xff, 1)
    else:
        return '!!'


def nibble2hex(v):
    if v < 10:
        return chr(ord('0') + v)
    elif v < 16:
        return chr(ord('A') + v - 10)
    return '!'


def parse_nibble(ch):
    o = ord(ch)
    if ord('0') <= o <= ord('9'):
        return o - ord('0')
    elif ord('a') <= o <= ord('f'):
        return o - ord('a') + 10
    return -1


def hex2byte(s):
    if len(s) != 2:
        return -1
    hi = parse_nibble(s[0])
    lo = parse_nibble(s[1])
    if hi < 0 or lo < 0:
        return -1
    return (hi << 4) + lo


def hex2word(s):
    if len(s) != 4:
        return -1
    hi = hex2byte(s[0:2])
    lo = hex2byte(s[2:4])
    if hi < 0 or lo < 0:
        return -1
    return (hi << 8) + lo


