-- name: all-clients
select * from workday_client

-- name: user-clients
select * from workday_client where workday_user_id :workday_user_id
