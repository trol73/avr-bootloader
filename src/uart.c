#include "uart.h"

void uartInit(unsigned int baudrate) {
#if USE_SECOND_UART
	#ifdef UART_TEST
	#ifndef UART1_BIT_U2X
	#warning "UART1_BIT_U2X not defined"
	#endif
	#ifndef UART1_UBRRH
	#warning "UART1_UBRRH not defined"
	#endif
	#ifndef UART1_CONTROLC
	#warning "UART1_CONTROLC not defined"
	#endif
	#if defined(URSEL) || defined(URSEL1)
	#ifndef UART1_BIT_URSEL
	#warning "UART1_BIT_URSEL not defined"
	#endif
	#endif
	#endif

		 // Set baud rate
		 if (baudrate & 0x8000) {
			  #if UART1_BIT_U2X
			UART1_STATUS = (1<<UART1_BIT_U2X);  //Enable 2x speed
			  #endif
		 }
		 UART1_UBRRH = (unsigned char)((baudrate >> 8) & 0x80);
		 UART1_UBRRL = (unsigned char) baudrate;

		 // Enable USART receiver and transmitter
		 UART1_CONTROL = _BV(UART1_BIT_RXCIE)|_BV(UART1_BIT_RXEN)|_BV(UART1_BIT_TXEN);

		 // Set frame format: asynchronous, 8data, no parity, 1stop bit
		 #ifdef UART1_BIT_URSEL
		 UART1_CONTROLC = _BV(UART1_BIT_URSEL) | _BV(UART1_BIT_UCSZ1) | _BV(UART1_BIT_UCSZ0);
		 #else
		 UART1_CONTROLC = _BV(UART1_BIT_UCSZ1)|_BV(UART1_BIT_UCSZ0);
		 #endif

#else

	#ifdef UART_TEST
	#ifndef UART0_BIT_U2X
	#warning "UART0_BIT_U2X not defined"
	#endif
	#ifndef UART0_UBRRH
	#warning "UART0_UBRRH not defined"
	#endif
	#ifndef UART0_CONTROLC
	#warning "UART0_CONTROLC not defined"
	#endif
	#if defined(URSEL) || defined(URSEL0)
	#ifndef UART0_BIT_URSEL
	#warning "UART0_BIT_URSEL not defined"
	#endif
	#endif
	#endif

		 // Set baud rate
		 if (baudrate & 0x8000) {
			  #if UART0_BIT_U2X
			  UART0_STATUS = _BV(UART0_BIT_U2X);  // Enable 2x speed
			  #endif
		 } else {
			 #if UART0_BIT_U2X
			 UART0_STATUS &= ~_BV(UART0_BIT_U2X);	// Disable 2x speed
			 #endif
		 }
		 #if defined(UART0_UBRRH)
		 UART0_UBRRH = (unsigned char)((baudrate>>8)&0x80) ;
		 #endif
		 UART0_UBRRL = (unsigned char) (baudrate & 0x00FF);

		 // Enable USART receiver and transmitter
		 UART0_CONTROL = _BV(UART0_BIT_RXEN)|_BV(UART0_BIT_TXEN);

		 // Set frame format: asynchronous, 8data, no parity, 1stop bit
		 #ifdef UART0_CONTROLC
		 #ifdef UART0_BIT_URSEL
		 UART0_CONTROLC = _BV(UART0_BIT_URSEL)|_BV(UART0_BIT_UCSZ1)|_BV(UART0_BIT_UCSZ0);
		 #else
		 UART0_CONTROLC = _BV(UART0_BIT_UCSZ1)|_BV(UART0_BIT_UCSZ0);
		 #endif
		 #endif
#endif
}



void uartPutChar(uint8_t data) {
	while (!(UART_STATUS & _BV(UART_TXREADY)));
	UART_DATA = data;
}

uint8_t uartWaitChar() {
	while (!(UART_STATUS & _BV(UART_RXREADY)));
	return UART_DATA;
}

#if ENABLE_PROXY
void proxyUartPutChar(uint8_t data) {
	while (!(UART_STATUS & _BV(UART_TXREADY)));
	UART_DATA = data;
}

uint8_t proxyUartWaitChar() {
	while (!(UART_STATUS & _BV(UART_RXREADY)));
	return UART_DATA;
}
#endif