CREATE DATABASE  IF NOT EXISTS `movie_db` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `movie_db`;
-- MySQL dump 10.13  Distrib 8.0.40, for Win64 (x86_64)
--
-- Host: localhost    Database: movie_db
-- ------------------------------------------------------
-- Server version	8.0.40

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `comment`
--

DROP TABLE IF EXISTS `comment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `comment` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `movie_id` bigint unsigned NOT NULL COMMENT '电影ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `content` text NOT NULL COMMENT '评论内容',
  `score` decimal(2,1) unsigned DEFAULT NULL COMMENT '评分(1.0-5.0)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_movie_id` (`movie_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `fk_comment_movie` FOREIGN KEY (`movie_id`) REFERENCES `movie` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_comment_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评论表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `comment`
--

LOCK TABLES `comment` WRITE;
/*!40000 ALTER TABLE `comment` DISABLE KEYS */;
INSERT INTO `comment` VALUES (1,1,1,'这部电影改变了我的人生观',5.0,'2025-06-20 15:38:56'),(2,1,2,'经典中的经典，值得反复观看',5.0,'2025-06-20 15:38:56'),(3,2,1,'视觉效果震撼，剧情也很棒',4.5,'2025-06-20 15:38:56'),(4,3,3,'张国荣的表演堪称完美',5.0,'2025-06-20 15:38:56'),(5,1,8,'加油最棒的！\r\n',5.0,'2026-02-16 20:46:42'),(6,6,8,'这部电影真不错',5.0,'2026-02-18 16:35:00'),(7,7,8,'111',4.0,'2026-03-05 21:39:30'),(8,27,8,'豪堪\r\n',5.0,'2026-03-05 22:14:21'),(9,5,8,'111',5.0,'2026-03-05 22:19:57');
/*!40000 ALTER TABLE `comment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `favorite`
--

DROP TABLE IF EXISTS `favorite`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `favorite` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `movie_id` bigint unsigned NOT NULL COMMENT '电影ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_user_movie` (`user_id`,`movie_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_movie_id` (`movie_id`),
  CONSTRAINT `fk_fav_movie` FOREIGN KEY (`movie_id`) REFERENCES `movie` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_fav_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='收藏表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `favorite`
--

LOCK TABLES `favorite` WRITE;
/*!40000 ALTER TABLE `favorite` DISABLE KEYS */;
INSERT INTO `favorite` VALUES (1,1,1,'2025-06-20 15:38:57'),(2,1,2,'2025-06-20 15:38:57'),(3,2,1,'2025-06-20 15:38:57'),(4,3,3,'2025-06-20 15:38:57');
/*!40000 ALTER TABLE `favorite` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `movie`
--

DROP TABLE IF EXISTS `movie`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `movie` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '电影ID',
  `title` varchar(100) NOT NULL COMMENT '电影标题',
  `cover` varchar(255) DEFAULT NULL COMMENT '封面图URL',
  `description` text COMMENT '电影描述',
  `release_date` date DEFAULT NULL COMMENT '上映日期',
  `duration` int unsigned DEFAULT '0' COMMENT '时长(分钟)',
  `region` varchar(50) DEFAULT NULL COMMENT '地区',
  `type` varchar(50) DEFAULT NULL COMMENT '类型',
  `score` decimal(3,1) unsigned DEFAULT '0.0' COMMENT '评分',
  `views` int unsigned DEFAULT '0' COMMENT '观看次数',
  `is_vip` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否VIP电影(0-否 1-是)',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态(0-下架 1-上架)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_title` (`title`),
  KEY `idx_region` (`region`),
  KEY `idx_type` (`type`),
  KEY `idx_score` (`score`),
  KEY `idx_views` (`views`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=30 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='电影表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `movie`
--

LOCK TABLES `movie` WRITE;
/*!40000 ALTER TABLE `movie` DISABLE KEYS */;
INSERT INTO `movie` VALUES (1,'肖申克的救赎','shawshank.jpg','两个被囚禁在肖申克监狱的男人...','1994-09-23',142,'美国','剧情',9.7,1500016,0,1,'2025-06-20 15:38:56','2026-03-05 23:08:05'),(2,'阿凡达','avatar.jpg','在未来世界中，人类为获取资源...','2009-12-18',162,'美国','科幻',8.8,1200004,1,1,'2025-06-20 15:38:56','2026-02-16 20:45:58'),(3,'霸王别姬','farewell.jpg','两位京剧表演艺术家半个世纪的悲欢离合...','1993-01-01',171,'中国','剧情',9.6,980007,0,1,'2025-06-20 15:38:56','2026-02-18 15:29:17'),(4,'盗梦空间','inception.jpg','一群能够进入他人梦境的小偷...','2010-07-16',148,'美国','科幻',9.3,1100003,1,1,'2025-06-20 15:38:56','2026-02-18 15:57:13'),(5,'三傻大闹宝莱坞','3idiots.jpg','三个大学生在校园中成长的故事...','2009-12-25',171,'印度','喜剧,剧情',9.2,1250023,0,1,'2026-02-18 15:54:21','2026-03-05 23:24:37'),(6,'何以为家','capernaum.jpg','黎巴嫩男孩扎因控诉父母的故事...','2018-09-20',126,'黎巴嫩','剧情',9.1,980012,1,1,'2026-02-18 15:54:21','2026-03-05 23:24:24'),(7,'大话西游之月光宝盒','chinese_odyssey.jpg','至尊宝与紫霞仙子的爱情故事...','1995-01-21',88,'中国香港','喜剧,爱情',9.0,1560006,0,1,'2026-02-18 15:54:21','2026-03-05 23:24:29'),(8,'寻梦环游记','coco.jpg','男孩米格穿越到亡灵国度的冒险...','2017-11-22',105,'美国','动画,家庭',9.1,2100002,1,1,'2026-02-18 15:54:21','2026-03-05 21:14:32'),(9,'摔跤吧！爸爸','dangal.jpg','父亲培养女儿成为摔跤冠军的故事...','2016-12-23',161,'印度','剧情,家庭',9.0,1890001,0,1,'2026-02-18 15:54:21','2026-03-05 22:19:49'),(10,'我不是药神','dying_to_survive.jpg','程勇代购印度仿制药的故事...','2018-07-05',117,'中国大陆','剧情,犯罪',9.0,2350000,0,1,'2026-02-18 15:54:21','2026-02-18 15:54:21'),(11,'阿甘正传','forrest_gump.jpg','阿甘一生充满传奇的经历...','1994-07-06',142,'美国','剧情,爱情',9.5,2500002,1,1,'2026-02-18 15:54:21','2026-03-05 22:19:46'),(12,'教父','godfather.jpg','黑手党柯里昂家族的传奇故事...','1972-03-24',175,'美国','犯罪,剧情',9.3,1980000,1,1,'2026-02-18 15:54:21','2026-02-18 15:54:21'),(13,'绿皮书','green_book.jpg','司机与钢琴家的跨种族友谊...','2018-11-16',130,'美国','剧情,喜剧',8.9,1230000,0,1,'2026-02-18 15:54:21','2026-02-18 15:54:21'),(14,'海蒂和爷爷','heidi.jpg','孤儿海蒂与爷爷在阿尔卑斯山的生活...','2015-12-10',111,'德国','家庭,剧情',9.3,890000,0,1,'2026-02-18 15:54:21','2026-02-18 15:54:21'),(15,'星际穿越','interstellar.jpg','库珀穿越宇宙寻找人类新家园...','2014-11-07',169,'美国','科幻,剧情',9.4,2150000,1,1,'2026-02-18 15:54:21','2026-02-18 15:54:21'),(16,'小丑','joker.jpg','亚瑟在哥谭市走向疯狂的故事...','2019-10-04',122,'美国','犯罪,剧情',8.4,1500000,1,1,'2026-02-18 15:54:21','2026-02-18 15:54:21'),(17,'这个杀手不太冷','leon.jpg','杀手莱昂与小女孩玛蒂尔达的故事...','1994-09-14',110,'法国','动作,犯罪',9.4,1800000,0,1,'2026-02-18 15:54:21','2026-02-18 15:54:21'),(18,'放牛班的春天','les_choristes.jpg','音乐老师用音乐改变问题少年...','2004-03-17',96,'法国','剧情,音乐',9.3,760000,0,1,'2026-02-18 15:54:21','2026-02-18 15:54:21'),(19,'让子弹飞','let_the_bullets_fly.jpg','张麻子与黄四郎在鹅城的较量...','2010-12-16',132,'中国大陆','喜剧,动作',9.0,1680000,0,1,'2026-02-18 15:54:21','2026-02-18 15:54:21'),(20,'少年派的奇幻漂流','life_of_pi.jpg','少年派与老虎在海上漂流的故事...','2012-11-21',127,'美国','剧情,冒险',9.1,1450000,1,1,'2026-02-18 15:54:21','2026-02-18 15:54:21'),(21,'指环王3：王者无敌','lotr3.jpg','魔戒远征队摧毁魔戒的最终决战...','2003-12-17',201,'美国','动作,奇幻',9.6,2050001,1,1,'2026-02-18 15:54:21','2026-02-18 15:57:56'),(22,'哪吒之魔童降世','nezha.jpg','哪吒逆天改命的成长故事...','2019-07-26',110,'中国大陆','动画,喜剧',8.4,3200004,0,1,'2026-02-18 15:54:21','2026-02-18 16:45:22'),(23,'寄生虫','parasite.jpg','金家寄生在富豪家庭的故事...','2019-05-30',132,'韩国','剧情,犯罪',8.8,1750000,1,1,'2026-02-18 15:54:21','2026-02-18 15:54:21'),(24,'辛德勒的名单','schindlers_list.jpg','辛德勒拯救犹太人的故事...','1993-12-15',195,'美国','历史,剧情',9.5,1300000,1,1,'2026-02-18 15:54:21','2026-02-18 15:54:21'),(25,'肖申克的救赎','shawshank.jpg','安迪在监狱中坚持希望的故事...','1994-09-23',142,'美国','犯罪,剧情',9.7,2600001,1,1,'2026-02-18 15:54:21','2026-03-05 22:13:55'),(26,'熔炉','silenced.jpg','揭露聋哑学校性侵案的故事...','2011-09-22',125,'韩国','剧情,犯罪',8.9,920000,1,1,'2026-02-18 15:54:21','2026-02-18 15:54:21'),(27,'千与千寻','spirited_away.jpg','千寻在神灵世界的成长冒险...','2001-07-20',125,'日本','动画,冒险',9.4,2400002,0,1,'2026-02-18 15:54:21','2026-03-05 22:14:21'),(28,'泰坦尼克号','titanic.jpg','杰克与露丝的爱情悲剧...','1997-12-19',194,'美国','爱情,剧情',9.5,2800002,1,1,'2026-02-18 15:54:21','2026-02-18 16:45:26'),(29,'你的名字。','your_name.jpg','男女高中生在梦中互换身体的故事...','2016-08-26',106,'日本','动画,爱情',8.5,2100001,0,1,'2026-02-18 15:54:21','2026-02-18 15:57:20');
/*!40000 ALTER TABLE `movie` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `movie_person`
--

DROP TABLE IF EXISTS `movie_person`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `movie_person` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `movie_id` bigint unsigned NOT NULL COMMENT '电影ID',
  `person_id` bigint unsigned NOT NULL COMMENT '电影人ID',
  `role_type` tinyint(1) NOT NULL DEFAULT '0' COMMENT '角色类型(0-演员 1-导演)',
  `role_name` varchar(50) DEFAULT NULL COMMENT '角色名称(如饰演的角色名)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_movie_person` (`movie_id`,`person_id`,`role_type`),
  KEY `idx_movie_id` (`movie_id`),
  KEY `idx_person_id` (`person_id`),
  KEY `idx_role_type` (`role_type`),
  CONSTRAINT `fk_mp_movie` FOREIGN KEY (`movie_id`) REFERENCES `movie` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_mp_person` FOREIGN KEY (`person_id`) REFERENCES `person` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='电影-电影人关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `movie_person`
--

LOCK TABLES `movie_person` WRITE;
/*!40000 ALTER TABLE `movie_person` DISABLE KEYS */;
INSERT INTO `movie_person` VALUES (1,1,1,0,'安迪·杜佛兰','2025-06-20 15:38:56'),(2,1,2,0,'艾利斯·波伊德·瑞德','2025-06-20 15:38:56'),(3,2,3,1,'导演','2025-06-20 15:38:56'),(4,3,4,0,'程蝶衣','2025-06-20 15:38:56'),(5,4,5,1,'导演','2025-06-20 15:38:56');
/*!40000 ALTER TABLE `movie_person` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `order_info`
--

DROP TABLE IF EXISTS `order_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order_info` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no` varchar(50) NOT NULL COMMENT '订单编号',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `amount` decimal(10,2) NOT NULL COMMENT '金额',
  `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '状态(0-未支付 1-已支付 2-已取消)',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `pay_type` tinyint(1) DEFAULT NULL COMMENT '支付类型(1-支付宝 2-微信)',
  `pay_no` varchar(100) DEFAULT NULL COMMENT '第三方支付单号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_order_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `order_info`
--

LOCK TABLES `order_info` WRITE;
/*!40000 ALTER TABLE `order_info` DISABLE KEYS */;
INSERT INTO `order_info` VALUES (1,'ORD20250001',1,29.90,1,NULL,1,'20250001','2025-06-20 15:38:57','2025-06-20 15:38:57'),(2,'ORD20250002',3,99.90,1,NULL,2,'20250002','2025-06-20 15:38:57','2025-06-20 15:38:57');
/*!40000 ALTER TABLE `order_info` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `person`
--

DROP TABLE IF EXISTS `person`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `person` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `name` varchar(50) NOT NULL COMMENT '姓名',
  `photo` varchar(255) DEFAULT NULL COMMENT '照片URL',
  `type` tinyint(1) NOT NULL DEFAULT '0' COMMENT '类型(0-演员 1-导演)',
  `description` text COMMENT '简介',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_name` (`name`),
  KEY `idx_type` (`type`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='电影人表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `person`
--

LOCK TABLES `person` WRITE;
/*!40000 ALTER TABLE `person` DISABLE KEYS */;
INSERT INTO `person` VALUES (1,'蒂姆·罗宾斯','tim_robbins.jpg',0,'美国著名演员','2025-06-20 15:38:56','2025-06-20 15:38:56'),(2,'摩根·弗里曼','morgan_freeman.jpg',0,'奥斯卡获奖演员','2025-06-20 15:38:56','2025-06-20 15:38:56'),(3,'詹姆斯·卡梅隆','james_cameron.jpg',1,'著名导演','2025-06-20 15:38:56','2025-06-20 15:38:56'),(4,'张国荣','leslie.jpg',0,'香港传奇演员','2025-06-20 15:38:56','2025-06-20 15:38:56'),(5,'克里斯托弗·诺兰','nolan.jpg',1,'英国著名导演','2025-06-20 15:38:56','2025-06-20 15:38:56');
/*!40000 ALTER TABLE `person` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `play_record`
--

DROP TABLE IF EXISTS `play_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `play_record` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `movie_id` bigint unsigned NOT NULL COMMENT '电影ID',
  `play_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '播放时间',
  `duration` int unsigned DEFAULT '0' COMMENT '播放时长(秒)',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_movie_id` (`movie_id`),
  KEY `idx_play_time` (`play_time`),
  CONSTRAINT `fk_play_movie` FOREIGN KEY (`movie_id`) REFERENCES `movie` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_play_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='播放记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `play_record`
--

LOCK TABLES `play_record` WRITE;
/*!40000 ALTER TABLE `play_record` DISABLE KEYS */;
INSERT INTO `play_record` VALUES (1,1,1,'2025-06-20 15:38:57',8520),(2,1,2,'2025-06-20 15:38:57',9720),(3,2,1,'2025-06-20 15:38:57',4260),(4,3,3,'2025-06-20 15:38:57',10260);
/*!40000 ALTER TABLE `play_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(100) NOT NULL COMMENT '密码',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
  `is_vip` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否是VIP(0-否 1-是)',
  `vip_expire_time` datetime DEFAULT NULL COMMENT 'VIP过期时间',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态(0-禁用 1-正常)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_username` (`username`),
  UNIQUE KEY `uniq_email` (`email`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (1,'user1','111','user1@example.com',NULL,NULL,1,'2025-12-31 23:59:59',1,'2025-06-20 15:38:56','2025-06-20 15:38:56'),(2,'user2','222','user2@example.com',NULL,NULL,0,NULL,1,'2025-06-20 15:38:56','2025-06-20 15:38:56'),(3,'vip_user','vip','vip@example.com',NULL,NULL,1,'2026-01-01 00:00:00',1,'2025-06-20 15:38:56','2025-06-20 15:38:56'),(7,'111','698d51a19d8a121ce581499d7b701668','111@qq.com','13679728917',NULL,0,NULL,1,'2025-06-20 19:37:22','2025-06-20 19:37:22'),(8,'222','bcbe3365e6ac95ea2c0343a2395834dd','123456@163.com','13679728917',NULL,1,NULL,1,'2025-06-20 23:38:58','2025-06-20 23:38:58'),(10,'333','310dcbbf4cce62f762a2aaa148d556bd','1234565@163.com','13679728917',NULL,1,NULL,1,'2025-06-21 00:00:51','2025-06-21 00:00:51');
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-05-31 14:12:14
