#define CMD_SYNC						0	// (uint8_t val)
#define CMD_ABOUT						1	// ()
#define CMD_READ_FLASH				2	// (uint16_t address, uint16_t size)
#define CMD_READ_EEPROM				3	// (uint16_t address, uint16_t size)
#define CMD_READ_FUSES				4	// ()
#define CMD_START_APP				5	// ()

#define CMD_ERASE_PAGE				6	// (uint16_t page)
#define CMD_WRITE_FLASH_PAGE		7	// (uint16_t page, data[page_size])
#define CMD_TRANSFER_PAGE			8

#define CMD_START_PROXY				9
#define CMD_FINISH_PROXY				10