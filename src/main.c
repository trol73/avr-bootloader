#include <stddef.h>
#include <stdlib.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>


#include <avr/io.h>
#include <avr/interrupt.h>
#include <avr/pgmspace.h>
#include <avr/sleep.h>
#include <avr/eeprom.h>
#include <avr/wdt.h>
#include <avr/boot.h>



#include <util/delay.h>


#include "uart.h"
#include "commands.h"


#if defined (SPMCSR)
	#define SPM_REG SPMCSR
#elif defined (SPMCR)
	#define SPM_REG SPMCR
#else
	#error "AVR processor does not provide bootloader support!"
#endif

#define APP_END (FLASHEND - (BOOTLOADER_SIZE * 2))


static uint16_t uartWaitWord() {
	uint16_t w = (uint16_t)uartWaitChar() << 8;
	return w + uartWaitChar();
}	


static inline uint16_t readFlashBlock(uint16_t address, uint16_t size) {
	do {
		uint16_t data;
#if READ_PROTECT_BOOTLOADER
		// don't read bootloader
		if (address < APP_END) {
		#if defined(RAMPZ)
			data = pgm_read_word_far(address);
		#else
			data = pgm_read_word_near(address);
		#endif
		} else {
			data = 0xFFFF; // fake empty
		}
#else
	#if defined(RAMPZ)
		data = pgm_read_word_far(address);
	#else
		data = pgm_read_word_near(address);
	#endif
#endif
		uartPutChar(data);			// send LSB
		size--;
		address++;
		if (!size) {
			break;
		}
		uartPutChar(data >> 8);		// send MSB
		address++;
		size--;
	} while (size);
	return address;
}

static inline uint16_t readEEpromBlock(uint16_t address, uint16_t size) {
	do {
		uartPutChar(eeprom_read_byte((uint8_t*)address));
		address++;
		size--;
	} while (size);

	return address;
}


#if ENABLE_READ_FUSELOCK
static uint8_t read_fuse_lock(uint16_t addr) {
	uint8_t mode = _BV(BLBSET) | _BV(SPMEN);
	uint8_t retval;

	asm volatile(
		"movw r30, %3\n\t"		/* Z to addr */ \
		"sts %0, %2\n\t"		/* set mode in SPM_REG */ \
		"lpm\n\t"			/* load fuse/lock value into r0 */ \
		"mov %1,r0\n\t"			/* save return value */ \
		: "=m" (SPM_REG),
		  "=r" (retval)
		: "r" (mode),
		  "r" (addr)
		: "r30", "r31", "r0"
	);
	return retval;
}
#endif

static inline void eraseFlash() {
	// erase only main section (bootloader protection)
	uint32_t addr = 0;
	while (APP_END > addr) {
		boot_page_erase(addr);		// Perform page erase
		boot_spm_busy_wait();		// Wait until the memory is erased.
		addr += SPM_PAGESIZE;
	}
	boot_rww_enable();
}


static void cmdSync() {
	uint8_t val = uartWaitChar();
	uartPutChar(val);
}

static void cmdAbout() {
	
}

static void cmdReadFlash() {
	uint16_t address = uartWaitWord();
	uint16_t size = uartWaitWord();
	readFlashBlock(address, size);
}

static void cmdReadEeprom() {
	uint16_t address = uartWaitWord();
	uint16_t size = uartWaitWord();
	
	readEEpromBlock(address, size);
}

static void cmdReadFuses() {
	
}

void main() {
	uartInit(UART_BAUD_SELECT(UART_BAUD_RATE, F_CPU));
	while (true) {
		uint8_t cmd = uartWaitChar();
		
		switch (cmd) {
			case CMD_SYNC:
				cmdSync();
				break; 
			case CMD_ABOUT:
				cmdAbout();
				break;
			case CMD_READ_FLASH:
				cmdReadFlash();
				break;
			case CMD_READ_EEPROM:
				cmdReadEeprom();
				break;
			case CMD_READ_FUSES:
				cmdReadFuses();
				break;
		}
	}
}


void reset(void) __attribute__((naked,section(".vectors")));
void reset(void){
    asm("clr r1");
    SP = RAMEND;
    SREG = 0;
    asm("jmp __dtors_end");
}
void jmp_main(void) __attribute__((naked,section(".init9")));
void jmp_main(void){
    asm("jmp main");
}