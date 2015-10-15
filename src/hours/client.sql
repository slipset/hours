-- name: all-clients
select * from workday_client

-- name: user-clients
select * from workday_client where workday_user_id = (:user_id)::uuid

-- name: add-client<!
insert into workday_client (name, workday_user_id) values (:name, (:user_id)::uuid)

-- name: user-client
select * from workday_client where id = (:client_id)::uuid and workday_user_id = (:user_id)::uuid
