-- Insert a config
INSERT INTO `CONFIGS` (`ID`, `NAME`, `DESCRIPTION`, `VERSION`, `UPDATED`) VALUES (1, 'Simple Config', 'Simple Description', 1, 1);
-- Insert config attributes
INSERT INTO `CONFIG_ATTRIBUTES` (`ID`, `CONFIG_ID`, `KEY`, `VALUE`) VALUES (1, 1, 'key_1', 'value_1');
INSERT INTO `CONFIG_ATTRIBUTES` (`ID`, `CONFIG_ID`, `KEY`, `VALUE`) VALUES (2, 1, 'key_2', 'value_2');
INSERT INTO `CONFIG_ATTRIBUTES` (`ID`, `CONFIG_ID`, `KEY`, `VALUE`) VALUES (3, 1, 'key_3', 'value_3');
-- Insert a property
INSERT INTO `PROPERTIES` (`ID`, `CONFIG_ID`, `NAME`, `CAPTION`, `DESCRIPTION`, `TYPE`, `VALUE`, `VERSION`) VALUES
  (1, 1, 'Property', 'Caption', 'Description', 'BOOL', 'true', 1);
-- Insert config attributes
INSERT INTO `PROPERTY_ATTRIBUTES` (`ID`, `PROPERTY_ID`, `KEY`, `VALUE`) VALUES (1, 1, 'key_1', 'value_1');
INSERT INTO `PROPERTY_ATTRIBUTES` (`ID`, `PROPERTY_ID`, `KEY`, `VALUE`) VALUES (2, 1, 'key_2', 'value_2');
INSERT INTO `PROPERTY_ATTRIBUTES` (`ID`, `PROPERTY_ID`, `KEY`, `VALUE`) VALUES (3, 1, 'key_3', 'value_3');
-- Insert a property
INSERT INTO `PROPERTIES` (`ID`, `PROPERTY_ID`, `CONFIG_ID`, `NAME`, `CAPTION`, `DESCRIPTION`, `TYPE`, `VALUE`, `VERSION`) VALUES
  (2, 1, 1, 'Sub-Property-1', 'Sub-Caption-1', 'Sub-Description-1', 'BOOL', 'true', 1);
-- Insert config attributes
INSERT INTO `PROPERTY_ATTRIBUTES` (`ID`, `PROPERTY_ID`, `KEY`, `VALUE`) VALUES (4, 2, 'key_1', 'value_1');
INSERT INTO `PROPERTY_ATTRIBUTES` (`ID`, `PROPERTY_ID`, `KEY`, `VALUE`) VALUES (5, 2, 'key_2', 'value_2');
INSERT INTO `PROPERTY_ATTRIBUTES` (`ID`, `PROPERTY_ID`, `KEY`, `VALUE`) VALUES (6, 2, 'key_3', 'value_3');
-- Insert a property
INSERT INTO `PROPERTIES` (`ID`, `PROPERTY_ID`, `CONFIG_ID`, `NAME`, `CAPTION`, `DESCRIPTION`, `TYPE`, `VALUE`, `VERSION`) VALUES
  (3, 2, 1, 'Sub-Property-2', 'Sub-Caption-2', 'Sub-Description-2', 'BOOL', 'true', 1);
-- Insert config attributes
INSERT INTO `PROPERTY_ATTRIBUTES` (`ID`, `PROPERTY_ID`, `KEY`, `VALUE`) VALUES (7, 3, 'key_1', 'value_1');
INSERT INTO `PROPERTY_ATTRIBUTES` (`ID`, `PROPERTY_ID`, `KEY`, `VALUE`) VALUES (8, 3, 'key_2', 'value_2');
INSERT INTO `PROPERTY_ATTRIBUTES` (`ID`, `PROPERTY_ID`, `KEY`, `VALUE`) VALUES (9, 3, 'key_3', 'value_3');