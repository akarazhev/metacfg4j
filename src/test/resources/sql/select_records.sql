-- Select a config
SELECT `C`.`ID`,
       `C`.`NAME`,
       `C`.`DESCRIPTION`,
       `C`.`VERSION`,
       `C`.`UPDATED`,
       `CA`.`KEY`,
       `CA`.`VALUE`,
       `P`.`ID`,
       `P`.`PROPERTY_ID`,
       `P`.`NAME`,
       `P`.`CAPTION`,
       `P`.`DESCRIPTION`,
       `P`.`TYPE`,
       `P`.`VALUE`,
       `P`.`UPDATED`,
       `PA`.`KEY`,
       `PA`.`VALUE`
FROM `CONFIGS` AS `C`
         LEFT JOIN `PROPERTIES` AS `P` ON `C`.`ID` = `P`.`CONFIG_ID`
         LEFT JOIN `CONFIG_ATTRIBUTES` AS `CA` ON `C`.`ID` = `CA`.`CONFIG_ID`
         LEFT JOIN `PROPERTY_ATTRIBUTES` AS `PA` ON `P`.`ID` = `PA`.`PROPERTY_ID`
WHERE `C`.`NAME` = 'Simple Config';
-- Select a config
SELECT DISTINCT `C`.`NAME`
FROM `CONFIGS` AS `C`
         INNER JOIN `CONFIG_ATTRIBUTES` AS `CA` ON `C`.`ID` = `CA`.`CONFIG_ID`
WHERE (`C`.`NAME` LIKE '%Config%') AND
      (`CA`.`KEY` LIKE '%key_1%'
          AND `CA`.`VALUE` LIKE '%value_1%')
   OR (`CA`.`KEY` LIKE '%key_2%'
    AND `CA`.`VALUE` LIKE '%value_2%')
ORDER BY `C`.`NAME`;
-- Select a config
SELECT COUNT(DISTINCT `C`.`NAME`)
FROM `CONFIGS` AS `C`
         INNER JOIN `CONFIG_ATTRIBUTES` AS `CA` ON `C`.`ID` = `CA`.`CONFIG_ID`
WHERE (`C`.`NAME` LIKE '%Config%') AND
      (`CA`.`KEY` LIKE '%key_1%'
          AND `CA`.`VALUE` LIKE '%value_1%')
   OR (`CA`.`KEY` LIKE '%key_2%'
    AND `CA`.`VALUE` LIKE '%value_2%');