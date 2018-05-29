SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS  `role`;
CREATE TABLE `role` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) DEFAULT NULL,
  `title` varchar(32) DEFAULT NULL,
  `details` varchar(255) DEFAULT NULL,
  `permissions` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2402 DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS  `user`;
CREATE TABLE `user` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(32) DEFAULT NULL,
  `age` int(11) DEFAULT NULL,
  `public_info` int(11) DEFAULT NULL,
  `gender` int(11) DEFAULT NULL,
  `details` text,
  `id_card` varchar(32) DEFAULT NULL,
  `money` decimal(15,2) DEFAULT '0.00',
  `image` blob,
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `serial_no` bigint(30) DEFAULT NULL,
  `pwd` varbinary(50) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=21516 DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;

/* PROCEDURES */;
DROP PROCEDURE IF EXISTS `test_mutil_result_set`;
DELIMITER $$
CREATE PROCEDURE `test_mutil_result_set`(
  IN name VARCHAR (255)
)
  BEGIN
    select * from user u where u.name=name limit 1;
    SELECT name;
    select  r.id,u.name,u.money,r.title,r.permissions from user u join role r on u.id=r.user_id where u.name =name;
  END
$$
DELIMITER ;
