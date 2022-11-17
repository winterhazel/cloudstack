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
-- Schema upgrade from 4.16.0.10 to 4.16.0.11
--;

CREATE OR REPLACE
ALGORITHM = UNDEFINED VIEW cloud_usage.quota_summary_view AS
select
    cloud_usage.quota_account.account_id AS account_id,
    cloud_usage.quota_account.quota_balance AS quota_balance,
    cloud_usage.quota_account.quota_balance_date AS quota_balance_date,
    cloud_usage.quota_account.quota_enforce AS quota_enforce,
    cloud_usage.quota_account.quota_min_balance AS quota_min_balance,
    cloud_usage.quota_account.quota_alert_date AS quota_alert_date,
    cloud_usage.quota_account.quota_alert_type AS quota_alert_type,
    cloud_usage.quota_account.last_statement_date AS last_statement_date,
    cloud.account.uuid AS account_uuid,
    cloud.account.account_name AS account_name,
    cloud.account.state AS account_state,
    cloud.account.removed AS account_removed,
    cloud.domain.id AS domain_id,
    cloud.domain.uuid AS domain_uuid,
    cloud.domain.name AS domain_name,
    cloud.domain.path AS domain_path,
    cloud.domain.removed AS domain_removed,
    cloud.projects.uuid AS project_uuid,
    cloud.projects.name AS project_name,
    cloud.projects.removed AS project_removed
from
    cloud_usage.quota_account
        join cloud.account on (cloud.account.id = cloud_usage.quota_account.account_id)
        join cloud.domain on (cloud.domain.id = cloud.account.domain_id)
        left join cloud.projects on (cloud.account.type = 5 and cloud.account.id = cloud.projects.project_account_id);

CREATE TABLE `cloud_usage`.`quota_email_configuration`(
`account_id` int(11) NOT NULL,
`email_template_id` bigint(20) NOT NULL,
`enabled` int(1) UNSIGNED NOT NULL,
PRIMARY KEY (`account_id`, `email_template_id`),
CONSTRAINT `FK_quota_email_configuration_account_id` FOREIGN KEY (`account_id`) REFERENCES `cloud_usage`.`quota_account`(`account_id`),
CONSTRAINT `FK_quota_email_configuration_email_te1mplate_id` FOREIGN KEY (`email_template_id`) REFERENCES `cloud_usage`.`quota_email_templates`(`id`));
