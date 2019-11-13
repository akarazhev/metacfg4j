-- Insert a config
INSERT INTO `CONFIGS` (`ID`, `NAME`, `DESCRIPTION`, `VERSION`, `UPDATED`) VALUES (1, 'Simple Config', 'Simple Description', 1, 1);
-- Insert config attributes
INSERT INTO `CONFIG_ATTRIBUTES` (`ID`, `CONFIG_ID`, `KEY`, `VALUE`) VALUES (1, 1, 'key_1', 'value_1');
INSERT INTO `CONFIG_ATTRIBUTES` (`ID`, `CONFIG_ID`, `KEY`, `VALUE`) VALUES (2, 1, 'key_2', 'value_2');
INSERT INTO `CONFIG_ATTRIBUTES` (`ID`, `CONFIG_ID`, `KEY`, `VALUE`) VALUES (3, 1, 'key_3', 'value_3');
-- Insert a property
INSERT INTO `PROPERTIES` (`ID`, `CONFIG_ID`, `NAME`, `CAPTION`, `DESCRIPTION`, `TYPE`, `VALUE`, `VERSION`) VALUES (1, 1, 'Simple Property', 'Simple Caption', 'Simple Description', 'BOOL', 'true', 1);