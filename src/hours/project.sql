--name: user-projects
select * from workday_project p, workday_client c where c.workday_user_id = (:user_id)::uuid and p.workday_client_id = c.id

--name: user-client-projects
select * from workday_project p, workday_client c where c.workday_user_id = (:user_id)::uuid and p.workday_client_id = c.id and c.id = (:client_id)::uuid

--name: add-project<!
insert into workday_project (name, workday_client_id) values (:name, (:client_id)::uuid)
