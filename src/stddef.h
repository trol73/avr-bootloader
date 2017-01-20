//#ifndef _STDDEF_H_
//#define _STDDEF_H_


#define TRUE	1
#define FALSE	0

/**
	Возвращает старший байт слова
*/
#define HI_WORD(x) (((x) >> 8) & 0xFF)			

/**
	Возвращает младший байт слова
*/
#define LO_WORD(x) ((x) & 0xFF)

#define GLUE(a, b)     a##b
#define PORT(x)        GLUE(PORT, x)
#define PIN(x)         GLUE(PIN, x)
#define DDR(x)         GLUE(DDR, x)


#include <avr/io.h>

#if defined (SPMCSR)
#define SPM_REG SPMCSR
#elif defined (SPMCR)
#define SPM_REG SPMCR
#else
#error "AVR processor does not provide bootloader support!"
#endif

#define APP_END (FLASHEND - (BOOTSIZE * 2))

#if (SPM_PAGESIZE > UINT8_MAX)
typedef uint16_t pagebuf_t;
#else
typedef uint8_t pagebuf_t;
#endif


//#endif // _STDDEF_H_
