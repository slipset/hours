-- name: user-by-email
select * from workday_user where email = :email

-- name: add-user<!
insert into workday_user (first_name, last_name, email) values (:first_name, :last_name, :email)
