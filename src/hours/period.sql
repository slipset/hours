-- name: start<!
insert into workday_period (workday_user_id, workday_project_id, description, period_start)
values ((:user_id)::uuid, (:project_id)::uuid, :description, :start)

--name: end!
update workday_period set period_end = :end where id = (:id)::uuid and workday_user_id = (:user_id)::uuid

-- name: by-id
select * from workday_period p1, workday_project p2 where p1.id = (:id)::uuid and p1.workday_user_id = (:user_id)::uuid and p1.workday_project_id = p2.id

-- name: by-user
select * from workday_period p1, workday_project p2, workday_client c where p1.workday_user_id = (:user_id)::uuid and p1.workday_project_id = p2.id and p2.workday_client_id = c.id and p1.period_start > current_date - interval '7 days' order by p1.period_start desc

--name: update!
update workday_period
set period_end = :end, period_start = :start, workday_project_id = (:project_id)::uuid, description = :description
where id = (:id)::uuid and workday_user_id = (:user_id)::uuid

--name: delete!
delete from workday_period where id = (:id)::uuid and workday_user_id = (:user_id)::uuid

--name: unstopped
select * from workday_period p1, workday_project p2
where p1.workday_project_id = p2.id and p1.workday_user_id = (:user_id)::uuid
and p1.period_end is null order by p1.period_start desc

