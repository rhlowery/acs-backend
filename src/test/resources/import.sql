-- Initial Groups
INSERT INTO acs_groups (id, name, description, persona) VALUES ('admins', 'Administrator Group', 'Users with full administrative privileges', 'ADMIN');
INSERT INTO acs_groups (id, name, description, persona) VALUES ('data-governors', 'Data Governance Group', 'Users responsible for data quality and access approval', 'APPROVER');
INSERT INTO acs_groups (id, name, description, persona) VALUES ('finance-leads', 'Finance Leads Group', 'Users managing financial data access', null);
INSERT INTO acs_groups (id, name, description, persona) VALUES ('standard-users', 'Standard User Group', 'Default user group', null);
INSERT INTO acs_groups (id, name, description, persona) VALUES ('governance-team', 'Mandatory Governance Group', 'The final signature required for all access', null);

-- Initial Users
INSERT INTO acs_users (id, name, email, role, persona) VALUES ('alice', 'Alice Smith', 'alice@example.com', 'STANDARD_USER', null);
INSERT INTO acs_users (id, name, email, role, persona) VALUES ('bob', 'Bob Jones', 'bob@example.com', 'ADMIN', 'ADMIN');
INSERT INTO acs_users (id, name, email, role, persona) VALUES ('charlie', 'Charlie Brown', 'charlie@example.com', 'STANDARD_USER', null);
INSERT INTO acs_users (id, name, email, role, persona) VALUES ('david', 'David Miller', 'david@example.com', 'STANDARD_USER', null);
INSERT INTO acs_users (id, name, email, role, persona) VALUES ('eve', 'Eve Davis', 'eve@example.com', 'STANDARD_USER', 'APPROVER');

-- User Group Memberships
INSERT INTO user_groups (user_id, group_id) VALUES ('alice', 'standard-users');
INSERT INTO user_groups (user_id, group_id) VALUES ('bob', 'admins');
INSERT INTO user_groups (user_id, group_id) VALUES ('bob', 'data-governors');
INSERT INTO user_groups (user_id, group_id) VALUES ('bob', 'finance-leads');
INSERT INTO user_groups (user_id, group_id) VALUES ('bob', 'governance-team');
INSERT INTO user_groups (user_id, group_id) VALUES ('charlie', 'standard-users');
INSERT INTO user_groups (user_id, group_id) VALUES ('david', 'standard-users');
INSERT INTO user_groups (user_id, group_id) VALUES ('eve', 'sensitive-approvers');
