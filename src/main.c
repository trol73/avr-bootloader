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
#include <avr/signature.h>


#include <util/delay.h>


#include "uart.h"
#include "commands.h"
#include "version.h"


#if defined (SPMCSR)
	#define SPM_REG SPMCSR
#elif defined (SPMCR)
	#define SPM_REG SPMCR
#else
	#error "AVR processor does not provide bootloader support!"
#endif

#define APP_END (FLASHEND - (BOOTLOADER_SIZE * 2))


uint8_t gPageBuffer[SPM_PAGESIZE];


static uint16_t uartWaitWord() {
	uint16_t w = (uint16_t)uartWaitChar() << 8;
	return w + uartWaitChar();
}	


static inline uint16_t readFlashBlock(uint32_t address, uint16_t size) {
	do {
		//uint16_t data;
		uint8_t data;
#if READ_PROTECT_BOOTLOADER
		// don't read bootloader
		if (address < APP_END) {
//		#if defined(RAMPZ)
//			data = pgm_read_word_far(address);
//		#else
//			data = pgm_read_word_near(address);
//		#endif
		#if defined(RAMPZ)
			data = pgm_read_byte_far(address);
		#else
			data = pgm_read_byte_near(address);
		#endif
			
		} else {
			//data = 0xFFFF; // fake empty
			data = 0xFF; // fake empty
		}
#else
//	#if defined(RAMPZ)
//		data = pgm_read_word_far(address);
//	#else
//		data = pgm_read_word_near(address);
//	#endif
	#if defined(RAMPZ)
		data = pgm_read_byte_far(address);
	#else
		data = pgm_read_byte_near(address);
	#endif
		
#endif
		uartPutChar(data);			// send LSB
		size--;
		address++;
		if (!size) {
			break;
		}
//		uartPutChar(data >> 8);		// send MSB
//		address++;
//		size--;
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

static inline uint16_t writeFlashPage(uint16_t waddr, uint16_t size) {
	uint32_t pagestart = (uint32_t)waddr << 1;
	uint32_t baddr = pagestart;
	uint8_t *tmp = gPageBuffer;

	do {
		uint16_t data = *tmp++;
		data |= *tmp++ << 8;
		boot_page_fill(baddr, data);	// call asm routine.

		baddr += 2;			// Select next word in memory
		size -= 2;			// Reduce number of bytes to write by two
	} while (size);

	boot_page_write(pagestart);
	boot_spm_busy_wait();
	boot_rww_enable();		// Re-enable the RWW section

	return baddr>>1;
}

static inline uint16_t writeEEpromPage(uint16_t address, uint16_t size) {
	uint8_t *tmp = gPageBuffer;

	do {
		eeprom_write_byte( (uint8_t*)address, *tmp++ );
		address++;			// Select next byte
		size--;				// Decrease number of bytes to write
	} while (size);				// Loop until all bytes written

	// eeprom_busy_wait();

	return address;
}



static void cmdSync() {
	uint8_t val = uartWaitChar();
	uartPutChar(val);
}

static void cmdAbout() {
	// TODO use PGMEM array
	
	// Bootloader signature		4b
	uartPutChar('T');
	uartPutChar('S');
	uartPutChar('B');
	uartPutChar('L');
	// Version code				2b 
	uartPutChar(VERSION_CODE >> 8);
	uartPutChar(VERSION_CODE & 0xff);
	// Bootloader start			4b
	uartPutChar(((uint32_t)BOOTLOADER_START >> 24) & 0xff);
	uartPutChar(((uint32_t)BOOTLOADER_START >> 16) & 0xff);
	uartPutChar(((uint32_t)BOOTLOADER_START >> 8) & 0xff);
	uartPutChar((uint32_t)BOOTLOADER_START & 0xff);
	// Bootloader size			2b
	uartPutChar(BOOTLOADER_SIZE >> 8);
	uartPutChar(BOOTLOADER_SIZE & 0xff);
	// Page size					2b
	uartPutChar(PAGE_SIZE >> 8);
	uartPutChar(PAGE_SIZE & 0xff);
	// Device signature, 3b
	uartPutChar(SIGNATURE_2);
	uartPutChar(SIGNATURE_1);
	uartPutChar(SIGNATURE_0);
}

static void cmdReadFlash() {
	uint32_t address = uartWaitWord();	// addres in 16 bytes paragraphs
	uint16_t size = uartWaitWord();
	address <<= 4;
	readFlashBlock(address, size);
}

static void cmdReadEeprom() {
	uint16_t address = uartWaitWord();
	uint16_t size = uartWaitWord();
	
	readEEpromBlock(address, size);
}

static void cmdReadFuses() {
	uint8_t lo = boot_lock_fuse_bits_get(GET_LOW_FUSE_BITS);
	uint8_t ex = boot_lock_fuse_bits_get(GET_EXTENDED_FUSE_BITS);
	uint8_t hi = boot_lock_fuse_bits_get(GET_HIGH_FUSE_BITS);
	
	uartPutChar(lo);
	uartPutChar(ex);
	uartPutChar(hi);
}

static void cmdEraseFlashPage() {
	uint16_t page = uartWaitWord();
	uint32_t address = page;
	address *= PAGE_SIZE;
	boot_page_erase(address);
	//boot_spm_busy_wait();		// Wait until the memory is erased.
}

volatile bool need_reenable_rww = false;

static void cmdWriteFlashPage() {
	uint16_t page  = uartWaitWord();
	uint32_t address = page;
	address *= PAGE_SIZE;
	
	uint8_t sreg = SREG;

	boot_spm_busy_wait();		// Wait until the memory is erased.
	if (need_reenable_rww) {
		boot_rww_enable();		// Re-enable the RWW section
		need_reenable_rww = false;
	}
	
	uint8_t *buf = gPageBuffer; 
	for (uint16_t i = 0; i < PAGE_SIZE; i += 2) {
        uint16_t w = *buf++;
        w += (*buf++) << 8;
    
        boot_page_fill(address + i, w);
    }
	
	boot_page_write(address);
	need_reenable_rww = true;
//	boot_spm_busy_wait();
//	boot_rww_enable();		// Re-enable the RWW section
	
	SREG = sreg;	// TODO возможно, тут это не надо
	
	uartPutChar(0);
}

static void cmdReadPageData() {
	for (uint16_t offset = 0; offset < PAGE_SIZE; offset++) {
		gPageBuffer[offset] = uartWaitChar();
	}
}


static void (*jump_to_app)(void) = 0x0000;

static void cmdJumpToApp() {
	if (need_reenable_rww) {
		boot_rww_enable();		// Re-enable the RWW section
		need_reenable_rww = false;
	}
	jump_to_app();	
}



void main() {
	//uartInit(UART_BAUD_SELECT(UART_BAUD_RATE, F_CPU));
	uartInit(UART_BAUD_SELECT_DOUBLE_SPEED(UART_BAUD_RATE, F_CPU));
	
	cli();
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
			case CMD_START_APP:
				cmdJumpToApp();
				break;
			case CMD_ERASE_PAGE:
				cmdEraseFlashPage();
				break;
			case CMD_WRITE_FLASH_PAGE:
				cmdWriteFlashPage();
				break;
			case CMD_TRANSFER_PAGE:
				cmdReadPageData();
				break;
		}
	}
}


void reset(void) __attribute__((naked,section(".vectors")));
void reset(void) {
    asm("clr r1");
    SP = RAMEND;
    SREG = 0;
#if __AVR_ARCH__ == 4
	 asm("rjmp __dtors_end");
#else 
	 asm("jmp __dtors_end");
#endif	 
    
}
void jmp_main(void) __attribute__((naked,section(".init9")));
void jmp_main(void) {
#if __AVR_ARCH__ == 4
	asm("rjmp main");
#else	
    asm("jmp main");
#endif	 
}