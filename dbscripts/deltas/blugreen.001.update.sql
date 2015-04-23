-- PT-2012: interoperate with bluegreen-discovery db

SET autocommit = 0;

USE blugreen;

INSERT INTO ENVIRONMENT (ENV_ID, ENV_NAME)
VALUES
-- teamcity won't substitute %%
  (1, '%%env.name%%'),
  (2, 'idev'),
  (3, 'iqa'),
  (4, 'iprd'),
  (5, 'krakendev'),
  (6, 'krakenqa-blue');

INSERT INTO LOGICAL_DATABASE (LOGICAL_ID, LOGICAL_NAME, FK_ENV_ID)
VALUES
  (1, 'krakendb', 1),
  (2, 'krakendb', 2),
  (3, 'krakendb', 3),
  (4, 'krakendb', 4),
  (5, 'krakendb', 5),
  (6, 'krakendb', 6);

-- FILL THIS IN WITH REAL PASSWORDS BEFORE RUNNING
INSERT INTO PHYSICAL_DATABASE (PHYSICAL_ID, PHYSICAL_TYPE, PHYSICAL_INST_NAME, IS_LIVE, DRIVER_CLASS_NAME, URL, USERNAME, `PASSWORD`, FK_LOGICAL_ID)
VALUES
  (1, 'RDS', 'krakendevtc', 1, 'com.mysql.jdbc.Driver',
   'jdbc:mysql://teamcity-does-not-care.com:3306/krakendev?zeroDateTimeBehavior=convertToNull', 'admin', 'password', 1),
  (2, 'RDS', 'krakendev', 1, 'com.mysql.jdbc.Driver',
   'jdbc:mysql://krakendevdb.nikedev.com:3306/krakendev?zeroDateTimeBehavior=convertToNull', 'admin', 'password', 2),
  (3, 'RDS', 'krakenqa', 1, 'com.mysql.jdbc.Driver',
   'jdbc:mysql://krakenqadbtmp.nikedev.com:3306/krakenqa?zeroDateTimeBehavior=convertToNull', 'kraken_admin',
   'password', 3),
  (4, 'RDS', 'kraken', 1, 'com.mysql.jdbc.Driver',
   'jdbc:mysql://krakendb.nikedev.com:3306/kraken?zeroDateTimeBehavior=convertToNull',
   'kraken_user', 'password', 4),
  (5, 'RDS', 'krakendev', 1, 'com.mysql.jdbc.Driver', 'does-not-exist-yet', 'nobody', 'password', 5),
  (6, 'RDS', 'krakenqa-blue', 1, 'com.mysql.jdbc.Driver',
   'jdbc:mysql://krakenqadb.nikedev.com:3306/krakenqa?zeroDateTimeBehavior=convertToNull', 'krakenqa', 'password', 6);

INSERT INTO APPLICATION_VM (APPVM_ID, APPVM_HOSTNAME, APPVM_IP_ADDRESS, FK_ENV_ID)
VALUES
  (1, 'fakeHostname-%%env.name%%', 'fakeip-%%env.name%%', 1),
  (2, 'aws-233-54.nike.com', '10.194.233.54', 2),
  (3, 'aws-233-36.nike.com', '10.194.233.36', 3),
  (4, 'aws-233-56.nike.com', '10.194.233.56', 4),
  (5, 'fakeHostname-krakendev', 'fakeip-krakendev', 5),
  (6, 'aws-233-169.nike.com', '10.194.233.169', 6);

INSERT INTO APPLICATION (APP_ID, FK_APPVM_ID, APP_SCHEME, APP_HOSTNAME, APP_PORT, APP_URL_PATH)
VALUES
  (1, 1, 'https', 'unknown', NULL, '/rest/administration'),
  (2, 2, 'https', 'idev-kraken.nikedev.com', NULL, '/rest/administration'),
  (3, 3, 'https', 'iqa-kraken.nikedev.com', NULL, '/rest/administration'),
  (4, 4, 'https', 'kraken.nikedev.com', NULL, '/rest/administration'),
  (5, 5, 'https', 'dev-kraken.nikedev.com', NULL, '/rest/administration'),
  (6, 6, 'https', 'qa-kraken.nikedev.com', NULL, '/rest/administration');

COMMIT;

SET autocommit = 1;
