-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements.  See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership.  The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliance
-- with the License.  You may obtain a copy of the License at
--
--   http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing,
-- software distributed under the License is distributed on an
-- "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
-- KIND, either express or implied.  See the License for the
-- specific language governing permissions and limitations
-- under the License.

--;
-- Schema upgrade from 4.18.0.0 to 4.18.1.0
--;

-- create_public_parameter_on_roles. #6960
ALTER TABLE `cloud`.`roles` ADD COLUMN `public_role` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'Indicates whether the role will be visible to all users (public) or only to root admins (private). If this parameter is not specified during the creation of the role its value will be defaulted to true (public).';

CREATE TABLE IF NOT EXISTS `cloud_usage`.`usage_vpc` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `vpc_id` bigint(20) unsigned NOT NULL,
  `zone_id` bigint(20) unsigned NOT NULL,
  `account_id` bigint(20) unsigned NOT NULL,
  `domain_id` bigint(20) unsigned NOT NULL,
  `state` varchar(100) DEFAULT NULL,
  `created` datetime NOT NULL,
  `removed` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB CHARSET=utf8;

ALTER TABLE `cloud_usage`.`cloud_usage` ADD state VARCHAR(100) DEFAULT NULL;