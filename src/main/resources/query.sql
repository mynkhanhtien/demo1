SELECT  u.FirstName as first_name,
        u.LastName as last_name,
        u.MemberId as member_id,
        u.BirthYear as date_of_birth,
        u.Gender as gender_id,
        u.Address1 as address_1,
        u.Address2 as address_2,
        u.City as city,
        u.State as state,
        u.Zip as postal_code,
        u.Email as email_address
FROM dbo.CallUsers u