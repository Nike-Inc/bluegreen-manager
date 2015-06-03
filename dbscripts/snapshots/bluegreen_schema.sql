/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `APPLICATION`
--

DROP TABLE IF EXISTS `APPLICATION`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `APPLICATION` (
  `APP_ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `APP_HOSTNAME` varchar(128) NOT NULL,
  `APP_PORT` int(11) DEFAULT NULL,
  `APP_SCHEME` varchar(10) NOT NULL,
  `APP_URL_PATH` varchar(255) NOT NULL,
  `APP_USERNAME` varchar(64) NOT NULL,
  `APP_PASSWORD` varchar(64) NOT NULL,
  `FK_APPVM_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`APP_ID`),
  KEY `FK_j84aa8inrh1lcjrad3ijcporj` (`FK_APPVM_ID`),
  CONSTRAINT `FK_j84aa8inrh1lcjrad3ijcporj` FOREIGN KEY (`FK_APPVM_ID`) REFERENCES `APPLICATION_VM` (`APPVM_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `APPLICATION_VM`
--

DROP TABLE IF EXISTS `APPLICATION_VM`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `APPLICATION_VM` (
  `APPVM_ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `APPVM_HOSTNAME` varchar(128) NOT NULL,
  `APPVM_IP_ADDRESS` varchar(20) NOT NULL,
  `FK_ENV_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`APPVM_ID`),
  KEY `FK_63dydgc9c25015dqn9wp6gh50` (`FK_ENV_ID`),
  CONSTRAINT `FK_63dydgc9c25015dqn9wp6gh50` FOREIGN KEY (`FK_ENV_ID`) REFERENCES `ENVIRONMENT` (`ENV_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ENVIRONMENT`
--

DROP TABLE IF EXISTS `ENVIRONMENT`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ENVIRONMENT` (
  `ENV_ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `ENV_NAME` varchar(32) NOT NULL,
  PRIMARY KEY (`ENV_ID`),
  UNIQUE KEY `UK_pt48ej5vl68yt0wv93r3tctrw` (`ENV_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `JOB_HISTORY`
--

DROP TABLE IF EXISTS `JOB_HISTORY`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `JOB_HISTORY` (
  `JOBHIST_ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `JOBHIST_CMDLINE` longtext NOT NULL,
  `JOBHIST_END_TIME` datetime DEFAULT NULL,
  `JOBHIST_ENV1` varchar(32) NOT NULL,
  `JOBHIST_ENV2` varchar(32) DEFAULT NULL,
  `JOBHIST_NAME` varchar(64) NOT NULL,
  `JOBHIST_START_TIME` datetime NOT NULL,
  `JOBHIST_STATUS` varchar(20) NOT NULL,
  PRIMARY KEY (`JOBHIST_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `LOGICAL_DATABASE`
--

DROP TABLE IF EXISTS `LOGICAL_DATABASE`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `LOGICAL_DATABASE` (
  `LOGICAL_ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `LOGICAL_NAME` varchar(32) NOT NULL,
  `FK_ENV_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`LOGICAL_ID`),
  UNIQUE KEY `UK_4e96fku1a9x755bupvayelanq` (`FK_ENV_ID`,`LOGICAL_NAME`),
  CONSTRAINT `FK_sly5rtv7aejengum46rg1aku0` FOREIGN KEY (`FK_ENV_ID`) REFERENCES `ENVIRONMENT` (`ENV_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `PHYSICAL_DATABASE`
--

DROP TABLE IF EXISTS `PHYSICAL_DATABASE`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `PHYSICAL_DATABASE` (
  `PHYSICAL_ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `PHYSICAL_TYPE` varchar(20) NOT NULL,
  `DRIVER_CLASS_NAME` varchar(32) NOT NULL,
  `PHYSICAL_INST_NAME` varchar(64) NOT NULL,
  `IS_LIVE` bit(1) NOT NULL,
  `PASSWORD` varchar(32) NOT NULL,
  `URL` varchar(255) NOT NULL,
  `USERNAME` varchar(32) NOT NULL,
  `FK_LOGICAL_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`PHYSICAL_ID`),
  KEY `FK_fpvs8xxh6tclh9ddktlqmhyhd` (`FK_LOGICAL_ID`),
  CONSTRAINT `FK_fpvs8xxh6tclh9ddktlqmhyhd` FOREIGN KEY (`FK_LOGICAL_ID`) REFERENCES `LOGICAL_DATABASE` (`LOGICAL_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `TASK_HISTORY`
--

DROP TABLE IF EXISTS `TASK_HISTORY`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `TASK_HISTORY` (
  `TASKHIST_ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `TASKHIST_END_TIME` datetime DEFAULT NULL,
  `TASKHIST_POSITION` int(11) NOT NULL,
  `TASKHIST_START_TIME` datetime NOT NULL,
  `TASKHIST_STATUS` varchar(20) NOT NULL,
  `TASKHIST_NAME` varchar(64) NOT NULL,
  `FK_JOBHIST_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`TASKHIST_ID`),
  KEY `FK_h8v90lry1jla3fe4t6qo6w854` (`FK_JOBHIST_ID`),
  CONSTRAINT `FK_h8v90lry1jla3fe4t6qo6w854` FOREIGN KEY (`FK_JOBHIST_ID`) REFERENCES `JOB_HISTORY` (`JOBHIST_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

