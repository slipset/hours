-- name: start<!
insert into workday_period (workday_user_id, workday_project_id, period_start)
values ((:user_id)::uuid, (:project_id)::uuid, :start)

--name: end!
update workday_period set period_end = :end where id = (:id)::uuid and workday_user_id = (:user_id)::uuid

-- name: by-id
select * from workday_period where id = (:id)::uuid

-- name: by-user
select * from workday_period p1, workday_project p2, workday_client c where p1.workday_user_id = (:user_id)::uuid and p1.workday_project_id = p2.id and p2.workday_client_id = c.id
