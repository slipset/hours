--name: user-projects-by-importance
select c.name as client_name, p.id as project_id, p.name as project_name, sum(h.period_end - h.period_start) as worked from workday_period h, workday_project p, workday_client c where c.workday_user_id = (:user_id)::uuid and p.workday_client_id = c.id and h.workday_project_id = p.id group by client_name, project_id, project_name order by worked desc
--name: user-projects
select * from workday_project p, workday_client c where c.workday_user_id = (:user_id)::uuid and p.workday_client_id = c.id

--name: user-project
select * from workday_project p, workday_client c where c.workday_user_id = (:user_id)::uuid and p.workday_client_id = c.id and p.id = (:project_id)::uuid

--name: by-name
select * from workday_project p, workday_client c where c.workday_user_id = (:user_id)::uuid and p.workday_client_id = c.id and p.name = :name

--name: user-client-projects
select * from workday_project p, workday_client c where c.workday_user_id = (:user_id)::uuid and p.workday_client_id = c.id and c.id = (:client_id)::uuid

--name: add<!
insert into workday_project (name, workday_client_id, color) values (:name, (:client_id)::uuid, :color)

--name: update!
update workday_project set name = :name, color = :color where id = (:project_id)::uuid
