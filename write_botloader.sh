CHIP=m328p

avrdude -s -c usbasp -p $CHIP -U "flash:w:build/avr-bootloader.hex:a"
#avrdude -U "lfuse:w:0x62:m" -U "hfuse:w:0xDF:m" -U "efuse:w:0xF9:m" -U "lock:w:0xFF:m" -s -c usbasp -p $CHIP