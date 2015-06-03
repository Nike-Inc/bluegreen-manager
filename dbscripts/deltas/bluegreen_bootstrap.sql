-- This is just an example of how you would bootstrap the blue/green data model
-- to describe your first environment.  You would need to change all these values
-- to make them specific for you.

insert into ENVIRONMENT (ENV_ID, ENV_NAME) values (1, 'env1');

insert into APPLICATION_VM (APPVM_ID, APPVM_HOSTNAME, APPVM_IP_ADDRESS, FK_ENV_ID) values
(1, 'vm0100.example.com', '10.111.222.100', 1);

insert into APPLICATION (APP_ID, APP_SCHEME, APP_HOSTNAME, APP_PORT, APP_URL_PATH, APP_USERNAME, APP_PASSWORD, FK_APPVM_ID) values
(1, 'http', 'vm0100.example.com', 8080, '/rest/bluegreen', 'appAdminUser', 'the-password', 1);

insert into LOGICAL_DATABASE (LOGICAL_ID, LOGICAL_NAME, FK_ENV_ID) values
(1, 'the_datasource', 1);

insert into PHYSICAL_DATABASE (PHYSICAL_ID, PHYSICAL_TYPE, DRIVER_CLASS_NAME, PHYSICAL_INST_NAME,
                               IS_LIVE, URL, USERNAME, `PASSWORD`, FK_LOGICAL_ID) values
(1, 'RDS', 'com.mysql.jdbc.Driver', 'my-rds-instance', 1,
 'jdbc:mysql://my-rds-instance.wwcckkadf.us-west-2.rds.amazonaws.com:3306/dbname', 'db_admin', 'abc123', 1);
