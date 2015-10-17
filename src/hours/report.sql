-- name: by-dates
select *
from workday_period p1, workday_project p2, workday_client p3
where p1.workday_user_id = (:user_id)::uuid and p1.workday_project_id = p2.id
and p2.workday_client_id = p3.id and (p1.period_start, period_end) overlaps (:period_start, :period_end)
