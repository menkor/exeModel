SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS  `role`;
CREATE TABLE `role` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) DEFAULT NULL,
  `title` varchar(32) DEFAULT NULL,
  `details` varchar(255) DEFAULT NULL,
  `permissions` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2247 DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS  `user`;
CREATE TABLE `user` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(32) DEFAULT NULL,
  `age` int(11) DEFAULT NULL,
  `details` text,
  `id_card` varchar(32) DEFAULT NULL,
  `money` decimal(10,0) DEFAULT '0',
  `image` blob,
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=21459 DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;

